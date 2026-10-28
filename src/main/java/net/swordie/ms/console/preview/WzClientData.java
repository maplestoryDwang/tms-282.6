package net.swordie.ms.console.preview;

import net.swordie.ms.console.ConsoleProperties;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Read-only access to the 282 client's WZ files (images, origins, map points) for the GUI previews. The WZ parser is
 * OrzRepacker, loaded reflectively so the server has no build dependency on it. Paths come from console.properties:
 * {@code preview.client.data} (client Data folder) and {@code preview.orz.dir} (OrzRepacker folder).
 * Images live in {@code _Canvas} sub-archives; main entries point there through {@code _outlink}.
 */
public final class WzClientData {
    public static final String KEY_CLIENT_DATA = "preview.client.data";
    public static final String KEY_ORZ_DIR = "preview.orz.dir";
    private static final String DEFAULT_CLIENT_DATA = "D:/TMS/MapleStory/Data";
    private static final String DEFAULT_ORZ_DIR = "D:/GMS/OrzRepacker";
    private static final short WZ_VERSION = 282;

    /** A drawable part: image plus its origin and named map points (all relative to the origin). */
    public record Part(BufferedImage image, Point origin, Map<String, Point> map, String z) { }

    private static volatile WzClientData instance;
    /** OrzRepacker images of one archive share its file reader: parallel previews (SwingWorker threads) reading at
     * once corrupt each other's position ("Unknown property type ... pType = -128"), so every read is serialized. */
    private static final Object READ = new Object();

    private final Path data;
    private final Class<?> fileType;
    private final byte[] iv;
    private final byte[] key;
    private final Map<Path, Object> files = new ConcurrentHashMap<>();
    private final Map<String, Optional<Object>> images = new ConcurrentHashMap<>();
    private final Map<Path, List<Path>> archives = new ConcurrentHashMap<>();

    private WzClientData(Path data, Path orz) throws Exception {
        if (!Files.isDirectory(data)) throw new IllegalStateException("找不到客戶端 Data 資料夾：" + data + "（console.properties " + KEY_CLIENT_DATA + "）");
        Path jar = orz.resolve("OrzRepacker.jar");
        if (!Files.isRegularFile(jar)) throw new IllegalStateException("找不到 OrzRepacker：" + jar + "（console.properties " + KEY_ORZ_DIR + "）");
        List<URL> urls = new ArrayList<>(List.of(URI.create("jar:" + jar.toUri() + "!/BOOT-INF/classes/").toURL()));
        try (var jars = Files.list(orz.resolve("lib"))) {
            for (Path j : jars.filter(j -> j.toString().endsWith(".jar")).toList()) urls.add(j.toUri().toURL());
        }
        quietParserLog();
        ClassLoader loader = new URLClassLoader(urls.toArray(URL[]::new), WzClientData.class.getClassLoader());
        this.data = data;
        this.fileType = loader.loadClass("orange.wz.provider.WzFile");
        Class<?> aes = loader.loadClass("orange.wz.provider.WzAESConstant");
        this.iv = ((byte[]) aes.getField("WZ_EMPTY_IV").get(null)).clone();
        this.key = ((byte[]) aes.getField("DEFAULT_KEY").get(null)).clone();
    }

    /** OrzRepacker logs every image it cannot parse with a stack trace; a failed image only means "no picture". */
    private static void quietParserLog() {
        try {
            if (org.slf4j.LoggerFactory.getLogger("orange.wz") instanceof ch.qos.logback.classic.Logger l) {
                l.setLevel(ch.qos.logback.classic.Level.OFF);
            }
        } catch (Throwable ignored) {
            // another slf4j backend: leave its log as it is
        }
    }

    /** Shared instance; throws IllegalStateException with a readable reason when the client or parser is missing. */
    public static WzClientData get() {
        WzClientData d = instance;
        if (d == null) {
            synchronized (WzClientData.class) {
                d = instance;
                if (d == null) {
                    Properties p = ConsoleProperties.load();
                    try {
                        d = new WzClientData(Path.of(p.getProperty(KEY_CLIENT_DATA, DEFAULT_CLIENT_DATA)),
                                Path.of(p.getProperty(KEY_ORZ_DIR, DEFAULT_ORZ_DIR)));
                    } catch (IllegalStateException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new IllegalStateException("無法載入 WZ 讀取器：" + e, e);
                    }
                    instance = d;
                }
            }
        }
        return d;
    }

    // ---- reflection helpers -------------------------------------------------------------------------------

    private static Object call(Object o, String name, Object... args) {
        try {
            for (Method m : o.getClass().getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == args.length) return m.invoke(o, args);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(name + ": " + e.getCause(), e);
        }
        throw new IllegalStateException("WZ API missing " + o.getClass().getSimpleName() + "." + name);
    }

    private static String type(Object node) {
        return node.getClass().getSimpleName();
    }

    private Object open(Path p) {
        return files.computeIfAbsent(p, path -> {
            try {
                Object f = fileType.getConstructor(String.class, short.class, String.class, byte[].class, byte[].class)
                        .newInstance(path.toString(), WZ_VERSION, "read-only-preview", iv, key);
                if (!(Boolean) call(f, "parse")) throw new IllegalStateException("WZ parse failed: " + path);
                return f;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("WZ open " + path + ": " + e, e);
            }
        });
    }

    private List<Path> archivesIn(Path dir) {
        return archives.computeIfAbsent(dir, d -> {
            if (!Files.isDirectory(d)) return List.of();
            try (var s = Files.list(d)) {
                return s.filter(p -> p.getFileName().toString().endsWith(".wz")).sorted().toList();
            } catch (Exception e) {
                return List.of();
            }
        });
    }

    /** The parsed .img {@code imgPath} (e.g. {@code "01302000.img"}) found in any archive of {@code dir}, or null. */
    public Object img(String dir, String imgPath) {
        String cacheKey = dir + "|" + imgPath;
        return images.computeIfAbsent(cacheKey, k -> Optional.ofNullable(findImg(data.resolve(dir), imgPath))).orElse(null);
    }

    private Object findImg(Path dir, String imgPath) {
        String[] parts = imgPath.split("/");
        for (Path p : archivesIn(dir)) {
            Object node = call(open(p), "getWzDirectory");
            for (int i = 0; i < parts.length - 1 && node != null; i++) {
                try {
                    node = call(node, "getDirectory", parts[i]);
                } catch (IllegalStateException e) {
                    node = null;
                }
            }
            if (node == null) continue;
            Object img = call(node, "getImage", parts[parts.length - 1]);
            if (img != null) {
                try {
                    synchronized (READ) {
                        call(img, "parse");
                    }
                } catch (RuntimeException e) {
                    continue; // an image OrzRepacker cannot parse (e.g. Mob/_Canvas 8870000.img): try the next source
                }
                return img;
            }
        }
        return null;
    }

    /** Child by slash path; follows UOL links. Null when absent. */
    public Object child(Object node, String path) {
        for (String part : path.split("/")) {
            if (node == null) return null;
            if (part.equals("..")) {
                node = call(node, "getParent");
                continue;
            }
            node = resolveUol(call(node, "getChild", part));
        }
        return node;
    }

    private Object resolveUol(Object node) {
        for (int guard = 0; node != null && type(node).equals("WzUOLProperty") && guard < 8; guard++) {
            String target = String.valueOf(call(node, "getValue"));
            node = child(call(node, "getParent"), target);
        }
        return node;
    }

    public List<String> childNames(Object node) {
        if (node == null) return List.of();
        List<?> kids = (List<?>) call(node, "getChildren");
        if (kids == null) return List.of();
        List<String> names = new ArrayList<>();
        for (Object k : kids) names.add(String.valueOf(call(k, "getName")));
        return names;
    }

    public String string(Object node) {
        if (node == null) return null;
        String t = type(node);
        if (!t.endsWith("StringProperty") && !t.endsWith("IntProperty") && !t.endsWith("ShortProperty")
                && !t.endsWith("LongProperty") && !t.endsWith("UOLProperty") && !t.endsWith("FloatProperty")
                && !t.endsWith("DoubleProperty")) {
            return null; // null / list / canvas / vector properties carry no scalar value
        }
        Object v = call(node, "getValue");
        return v == null ? null : v.toString();
    }

    public int integer(Object node, int fallback) {
        String s = string(node);
        try {
            return s == null ? fallback : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public Point vector(Object node) {
        if (node == null || !type(node).equals("WzVectorProperty")) return null;
        return new Point(((Number) call(node, "getX")).intValue(), ((Number) call(node, "getY")).intValue());
    }

    public boolean isCanvas(Object node) {
        return node != null && type(node).equals("WzCanvasProperty");
    }

    /** Canvas pixels, following {@code _outlink} into the owning {@code _Canvas} archive. */
    public BufferedImage image(Object canvas) {
        if (!isCanvas(canvas)) return null;
        Object target = canvas;
        String link = string(call(canvas, "getChild", "_outlink"));
        if (link != null) {
            target = outlink(link);
            if (target == null) return null;
        }
        synchronized (READ) {
            return (BufferedImage) call(target, "getPngImage", false);
        }
    }

    /** e.g. {@code Character/Cap/_Canvas/01002357.img/info/icon} -> node inside Data/Character/Cap/_Canvas. */
    private Object outlink(String link) {
        String[] seg = link.split("/");
        int canvasIdx = Arrays.asList(seg).indexOf("_Canvas");
        if (canvasIdx < 0) return null;
        StringBuilder dir = new StringBuilder();
        for (int i = 0; i <= canvasIdx; i++) dir.append(i == 0 ? "" : "/").append(seg[i]);
        int imgIdx = canvasIdx + 1;
        StringBuilder imgPath = new StringBuilder();
        while (imgIdx < seg.length && !seg[imgIdx].endsWith(".img")) imgPath.append(seg[imgIdx++]).append('/');
        if (imgIdx >= seg.length) return null;
        imgPath.append(seg[imgIdx]);
        Object img = img(dir.toString(), imgPath.toString());
        return img == null ? null : child(img, String.join("/", Arrays.copyOfRange(seg, imgIdx + 1, seg.length)));
    }

    /** The canvas node's {@code _outlink} target, or null. */
    public String outlinkOf(Object canvas) {
        if (!isCanvas(canvas)) return null;
        return string(call(canvas, "getChild", "_outlink"));
    }

    /** Pixels at an outlink path such as {@code Mob/_Canvas/8880150.img/fly/0}. */
    public BufferedImage imageAt(String link) {
        Object node = link == null ? null : outlink(link);
        if (!isCanvas(node)) return null;
        synchronized (READ) {
            return (BufferedImage) call(node, "getPngImage", false);
        }
    }

    /** {@code Data/Packs/<prefix>*.ms} files, sorted. */
    public List<Path> packs(String prefix) {
        Path dir = data.resolve("Packs");
        if (!Files.isDirectory(dir)) return List.of();
        try (var s = Files.list(dir)) {
            return s.filter(p -> {
                String n = p.getFileName().toString();
                return n.startsWith(prefix) && n.endsWith(".ms");
            }).sorted().toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Decrypts one .ms pack and returns the parsed images whose entry name matches; the rest is released.
     * Heavy (a pack decrypts to about 1 GB), so callers cache what they extract. */
    public Map<String, Object> packEntries(Path pack, java.util.function.Predicate<String> wanted) {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            Class<?> ms = fileType.getClassLoader().loadClass("orange.wz.provider.ms.WzMsFile");
            List<?> entries = (List<?>) ms.getMethod("load", Path.class, byte[].class, byte[].class).invoke(null, pack, iv, key);
            for (Object e : entries) {
                String name = String.valueOf(call(e, "getEntryName"));
                if (!wanted.test(name)) continue;
                Object img = call(e, "getImage");
                if (img == null) continue;
                synchronized (READ) {
                    if (Boolean.TRUE.equals(call(img, "parse"))) out.put(name, img);
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("讀取 " + pack.getFileName() + " 失敗：" + e.getCause(), e);
        }
        return out;
    }

    /** Canvas as a positioned part (image, origin, map points, z layer). Null when it has no pixels. */
    public Part part(Object canvas) {
        BufferedImage image = image(canvas);
        if (image == null) return null;
        Point origin = Optional.ofNullable(vector(child(canvas, "origin"))).orElse(new Point(0, 0));
        Map<String, Point> map = new LinkedHashMap<>();
        Object mapNode = child(canvas, "map");
        for (String name : childNames(mapNode)) {
            Point v = vector(child(mapNode, name));
            if (v != null) map.put(name, v);
        }
        return new Part(image, origin, map, string(child(canvas, "z")));
    }
}
