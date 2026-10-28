package net.swordie.ms.console.preview;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Composes a standing character from the client's WZ parts, the way the game does: every part carries an origin
 * and named map points (navel, neck, brow, hand); parts are chained through shared map points starting at the
 * body, drawn back-to-front by Base.wz/zmap.img, and hair layers covered by the cap's vslot (Base.wz/smap.img) are
 * hidden. Only frame 0 of stand1/stand2 is drawn (no animation, effects, pets or mounts).
 */
public final class AvatarRenderer {
    /** What to draw: skin/face/hair ids and the visible equip per body part (cash already preferred). */
    public record Look(int skin, int face, int hair, boolean elfEar, Map<Integer, Integer> equips) { }

    private record Placed(WzClientData.Part part, Point position, int source) { }

    private static final int SRC_BODY = 0, SRC_HAIR = 1, SRC_FACE = 2, SRC_EQUIP = 3;

    private final WzClientData wz;
    private List<String> zmap;
    private Map<String, String> smap;

    public AvatarRenderer(WzClientData wz) {
        this.wz = wz;
    }

    private synchronized List<String> zmap() {
        if (zmap == null) zmap = wz.childNames(wz.img("Base", "zmap.img"));
        return zmap;
    }

    private synchronized Map<String, String> smap() {
        if (smap == null) {
            Object img = wz.img("Base", "smap.img");
            Map<String, String> m = new HashMap<>();
            for (String name : wz.childNames(img)) m.put(name, wz.string(wz.child(img, name)));
            smap = m;
        }
        return smap;
    }

    /** Client directory of an equip template (Character/&lt;dir&gt;/0XXXXXXX.img). */
    static String equipDir(int itemId) {
        int prefix = itemId / 10000;
        return switch (prefix) {
            case 100 -> "Cap";
            case 101, 102, 103, 112, 113, 114, 115, 116, 118, 119 -> "Accessory";
            case 104 -> "Coat";
            case 105 -> "Longcoat";
            case 106 -> "Pants";
            case 107 -> "Shoes";
            case 108 -> "Glove";
            case 109 -> "Shield";
            case 110 -> "Cape";
            case 111 -> "Ring";
            case 120 -> "Totem";
            case 166 -> "Android";
            case 167 -> "Android"; // mechanical hearts live with the androids
            default -> prefix >= 120 && prefix < 180 ? "Weapon" : null;
        };
    }

    static String img(int id) {
        return String.format("%08d.img", id);
    }

    public BufferedImage render(Look look) {
        List<Placed> parts = new ArrayList<>();
        int weaponId = look.equips().getOrDefault(11, 0);
        Object weaponImg = weaponId > 0 ? wz.img("Character/Weapon", img(weaponId)) : null;
        Object weaponRoot = weaponRoot(weaponImg, weaponId);
        String action = action(weaponRoot);

        Object body = wz.img("Character", img(2000 + look.skin()));
        Object head = wz.img("Character", img(12000 + look.skin()));
        if (body == null) throw new IllegalStateException("找不到身體 " + img(2000 + look.skin()));
        addFrame(parts, wz.child(body, action + "/0"), SRC_BODY, n -> true);
        // The 282 head frame carries every ear variant: "humanEar", elf "ear", and "lefEar" / "highlefEar".
        if (head != null) addFrame(parts, wz.child(head, action + "/0"), SRC_BODY,
                n -> !n.endsWith("Ear") && !n.equals("ear") || n.equals(look.elfEar() ? "ear" : "humanEar"));

        Object face = look.face() > 0 ? wz.img("Character/Face", img(look.face())) : null;
        if (face != null) addFrame(parts, wz.child(face, "default"), SRC_FACE, n -> true);
        Object hair = look.hair() > 0 ? wz.img("Character/Hair", img(look.hair())) : null;
        if (hair != null) addFrame(parts, wz.child(hair, "default"), SRC_HAIR, n -> true);

        String capVslot = "";
        // An overall occupies both the top and bottom slots.
        boolean overall = look.equips().values().stream().anyMatch(id -> "Longcoat".equals(equipDir(id)));
        for (Map.Entry<Integer, Integer> e : look.equips().entrySet()) {
            int id = e.getValue();
            if (id <= 0 || e.getKey() == 11) continue;
            String dir = equipDir(id);
            if (dir == null || dir.equals("Ring") || overall && (dir.equals("Coat") || dir.equals("Pants"))) continue;
            Object eq = wz.img("Character/" + dir, img(id));
            if (eq == null) continue;
            if (dir.equals("Cap")) capVslot = Objects.toString(wz.string(wz.child(eq, "info/vslot")), "");
            Object frame = wz.child(eq, action + "/0");
            if (frame == null) frame = wz.child(eq, "default");
            addFrame(parts, frame, SRC_EQUIP, n -> true);
        }
        if (weaponRoot != null) addFrame(parts, wz.child(weaponRoot, action + "/0"), SRC_EQUIP, n -> true);

        if (!capVslot.isEmpty()) {
            String cover = capVslot;
            parts.removeIf(p -> p.source() == SRC_HAIR && covered(p.part().z(), cover));
        }
        return compose(position(parts));
    }

    /** Cash weapons keep their frames under a weapon-type key (e.g. "30" for one-handed swords). */
    private Object weaponRoot(Object weaponImg, int weaponId) {
        if (weaponImg == null) return null;
        if (wz.child(weaponImg, "stand1/0") != null || wz.child(weaponImg, "stand2/0") != null) return weaponImg;
        for (String name : wz.childNames(weaponImg)) {
            if (name.matches("\\d+")) {
                Object sub = wz.child(weaponImg, name);
                if (wz.child(sub, "stand1/0") != null || wz.child(sub, "stand2/0") != null) return sub;
            }
        }
        return null;
    }

    private String action(Object weaponRoot) {
        if (weaponRoot == null) return "stand1";
        int stand = wz.integer(wz.child(weaponRoot, "info/stand"), 0);
        if (stand == 2 && wz.child(weaponRoot, "stand2/0") != null) return "stand2";
        return wz.child(weaponRoot, "stand1/0") != null ? "stand1" : "stand2";
    }

    private boolean covered(String z, String vslot) {
        if (z == null) return false;
        String slots = smap().get(z);
        if (slots == null || slots.isEmpty()) return false;
        for (int i = 0; i + 1 < slots.length(); i += 2) {
            if (!vslot.contains(slots.substring(i, i + 2))) return false;
        }
        return true;
    }

    private void addFrame(List<Placed> out, Object frame, int source, java.util.function.Predicate<String> keep) {
        if (frame == null) return;
        for (String name : wz.childNames(frame)) {
            if (!keep.test(name)) continue;
            Object node = wz.child(frame, name);
            if (!wz.isCanvas(node)) continue;
            WzClientData.Part part = wz.part(node);
            if (part != null && part.image().getWidth() > 1) out.add(new Placed(part, null, source));
        }
    }

    /** Chains parts through shared map points, starting from the body at (0,0). */
    private List<Placed> position(List<Placed> parts) {
        Map<String, Point> anchors = new HashMap<>();
        List<Placed> placed = new ArrayList<>();
        List<Placed> pending = new ArrayList<>(parts);
        Placed body = pending.stream().filter(p -> "body".equals(p.part().z())).findFirst().orElse(pending.isEmpty() ? null : pending.get(0));
        if (body == null) return placed;
        place(body, new Point(0, 0), anchors, placed);
        pending.remove(body);
        boolean progress = true;
        while (!pending.isEmpty() && progress) {
            progress = false;
            for (Iterator<Placed> it = pending.iterator(); it.hasNext(); ) {
                Placed p = it.next();
                Point at = null;
                for (Map.Entry<String, Point> m : p.part().map().entrySet()) {
                    Point a = anchors.get(m.getKey());
                    if (a != null) {
                        at = new Point(a.x - m.getValue().x, a.y - m.getValue().y);
                        break;
                    }
                }
                if (at == null && p.part().map().isEmpty()) at = new Point(0, 0);
                if (at != null) {
                    place(p, at, anchors, placed);
                    it.remove();
                    progress = true;
                }
            }
        }
        return placed;
    }

    private void place(Placed p, Point at, Map<String, Point> anchors, List<Placed> placed) {
        placed.add(new Placed(p.part(), at, p.source()));
        for (Map.Entry<String, Point> m : p.part().map().entrySet()) {
            anchors.putIfAbsent(m.getKey(), new Point(at.x + m.getValue().x, at.y + m.getValue().y));
        }
    }

    private BufferedImage compose(List<Placed> placed) {
        if (placed.isEmpty()) return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        List<String> order = zmap();
        placed.sort(Comparator.comparingInt((Placed p) -> {
            int i = p.part().z() == null ? -1 : order.indexOf(p.part().z());
            return i < 0 ? order.indexOf("body") : i;
        }).reversed());
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Placed p : placed) {
            int x = p.position().x - p.part().origin().x, y = p.position().y - p.part().origin().y;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x + p.part().image().getWidth());
            maxY = Math.max(maxY, y + p.part().image().getHeight());
        }
        BufferedImage out = new BufferedImage(maxX - minX, maxY - minY, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        for (Placed p : placed) {
            g.drawImage(p.part().image(), p.position().x - p.part().origin().x - minX, p.position().y - p.part().origin().y - minY, null);
        }
        g.dispose();
        return out;
    }
}
