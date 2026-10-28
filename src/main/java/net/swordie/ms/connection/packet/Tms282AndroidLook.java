package net.swordie.ms.connection.packet;

import net.swordie.ms.ServerConstants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 282 android appearance ids. 265 keeps hair/face as {@code id % 10000} (EtcData, {@code short} fields) and the 265
 * client restores them; the 282 appearance reader 8D9C20 stores hair/face raw and only the skin gets +0x7D0
 * (1D11131), so a reduced hair/face (e.g. 4783) makes the 282 client crash drawing the android. The full id is the
 * template's Etc/Android costume entry with the same remainder; without one, hair is 3xxxx and face 2xxxx.
 */
public final class Tms282AndroidLook {

    private static final Pattern LIST = Pattern.compile(
            "<imgdir name=\"(hair|face)\">(.*?)</imgdir>", Pattern.DOTALL);
    private static final Pattern VALUE = Pattern.compile("value=\"(\\d+)\"");
    private static final Map<Integer, Costume> COSTUMES = new ConcurrentHashMap<>();

    record Costume(List<Integer> hairs, List<Integer> faces) {
    }

    private Tms282AndroidLook() {
    }

    public static Tms282AndroidPackets.Appearance full(int template, Tms282AndroidPackets.Appearance look) {
        return new Tms282AndroidPackets.Appearance(look.skin(), hair(template, look.hair()), face(template, look.face()),
                look.name());
    }

    public static int hair(int template, int hair) {
        return resolve(costume(template).hairs(), hair, 30000);
    }

    public static int face(int template, int face) {
        return resolve(costume(template).faces(), face, 20000);
    }

    static int resolve(List<Integer> full, int value, int base) {
        if (value <= 0 || value >= 10000) {
            return value; // none, or already a full id
        }
        for (int id : full) {
            if (id % 10000 == value) {
                return id;
            }
        }
        return base + value;
    }

    static Costume costume(int template) {
        return COSTUMES.computeIfAbsent(template, Tms282AndroidLook::load);
    }

    static Costume load(int template) {
        List<Integer> hairs = new ArrayList<>(), faces = new ArrayList<>();
        String name = String.format("Etc.wz/Android/%04d.img.xml", template);
        // the 282 layer first (types only the 282 client has, e.g. 318 with hair 42590), then the 265 file
        java.util.List<java.io.File> layers = net.swordie.ms.loaders.overlay.DataOverlays.current().wzLayers(name);
        Path file = layers.isEmpty() ? Path.of(ServerConstants.WZ_DIR, name) : layers.get(layers.size() - 1).toPath();
        if (!Files.isRegularFile(file)) {
            file = Path.of(ServerConstants.WZ_DIR, name);
        }
        try {
            if (Files.isRegularFile(file)) {
                Matcher list = LIST.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (list.find()) {
                    List<Integer> into = list.group(1).equals("hair") ? hairs : faces;
                    Matcher value = VALUE.matcher(list.group(2));
                    while (value.find()) {
                        into.add(Integer.parseInt(value.group(1)));
                    }
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // fall back to the 3xxxx / 2xxxx ids
        }
        return new Costume(List.copyOf(hairs), List.copyOf(faces));
    }
}
