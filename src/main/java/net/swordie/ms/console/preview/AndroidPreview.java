package net.swordie.ms.console.preview;

import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.life.Android;

import java.util.*;

/**
 * The character's android: the android and mechanical heart equips, the android's own equipment (body parts
 * 1200..1207) and how it looks. The server does not store the android's face/hair/skin (they are rolled from the
 * android type's costume list when it spawns), so an offline preview uses the type's first costume; an online one
 * matches the live values against the costume list.
 */
public final class AndroidPreview {
    /** Android equipment slots (BodyPart.APHat..APWeapon) and the character body part each one draws as. */
    static final int[][] SLOTS = {{1200, 1}, {1201, 9}, {1202, 2}, {1203, 5}, {1204, 6}, {1205, 7}, {1206, 8}, {1207, 11}};
    public static final int ANDROID_SLOT = 53, HEART_SLOT = 54;

    public record Snapshot(Equip android, Equip heart, Map<Integer, Equip> equips, Android live) {
        public boolean present() {
            return android != null;
        }
    }

    private AndroidPreview() { }

    static Snapshot of(Map<Integer, Equip> normal) {
        Equip android = normal.get(ANDROID_SLOT), heart = normal.get(HEART_SLOT);
        Map<Integer, Equip> equips = new TreeMap<>();
        for (int[] s : SLOTS) {
            Equip e = normal.get(s[0]);
            if (e != null) equips.put(s[0], e);
        }
        return new Snapshot(android, heart, equips, android == null ? null : android.getAndroidLife());
    }

    /** Body part for an item id (for the android's default outfit, which is listed by item only). */
    static int bodyPart(int itemId) {
        return switch (itemId / 10000) {
            case 100 -> 1;
            case 101 -> 2;
            case 102 -> 3;
            case 103 -> 4;
            case 104, 105 -> 5;
            case 106 -> 6;
            case 107 -> 7;
            case 108 -> 8;
            case 109 -> 10;
            case 110 -> 9;
            default -> itemId / 10000 >= 120 && itemId / 10000 < 180 ? 11 : -1;
        };
    }

    /** Look to render, or null when there is no android or its type has no client data. */
    public static AvatarRenderer.Look look(WzClientData wz, Snapshot s) {
        if (!s.present()) return null;
        Object type = wz.img("Etc/Android", String.format("%04d.img", s.android().getAndroid()));
        if (type == null) return null;
        List<Integer> skins = values(wz, wz.child(type, "costume/skin"));
        List<Integer> hairs = values(wz, wz.child(type, "costume/hair"));
        List<Integer> faces = values(wz, wz.child(type, "costume/face"));
        int skin = pick(skins, s.live() == null ? -1 : s.live().getSkin(), 1000);
        int hair = pick(hairs, s.live() == null ? -1 : s.live().getHair(), 10000);
        int face = pick(faces, s.live() == null ? -1 : s.live().getFace(), 10000);
        Map<Integer, Integer> equips = new TreeMap<>();
        Object basic = wz.child(type, "basic");
        for (String name : wz.childNames(basic)) {
            int id = wz.integer(wz.child(basic, name), 0);
            int part = bodyPart(id);
            if (part > 0) equips.put(part, id);
        }
        for (int[] slot : SLOTS) {
            Equip e = s.equips().get(slot[0]);
            if (e != null) equips.put(slot[1], e.getItemId());
        }
        // Skin ids in the costume list are 2000-based body ids.
        return new AvatarRenderer.Look(skin >= 2000 ? skin - 2000 : Math.max(0, skin), face, hair, false, equips);
    }

    private static List<Integer> values(WzClientData wz, Object list) {
        List<Integer> out = new ArrayList<>();
        for (String k : wz.childNames(list)) {
            int v = wz.integer(wz.child(list, k), -1);
            if (v >= 0) out.add(v);
        }
        return out;
    }

    /** The live value matched by the server's reduced form (value % modulo), else the first costume entry. */
    private static int pick(List<Integer> options, int live, int modulo) {
        if (options.isEmpty()) return live < 0 ? 0 : live;
        if (live >= 0) {
            for (int v : options) if (v % modulo == live) return v;
        }
        return options.get(0);
    }
}
