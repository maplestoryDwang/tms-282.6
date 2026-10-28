package net.swordie.ms.world.tms282.newbie;

import net.swordie.ms.loaders.FieldData;
import net.swordie.ms.connection.protocol.codec.FieldAvailability;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.tms282.items.Tms282ItemAvailability;
import net.swordie.ms.world.tms282.newbie.steps.StarterGiftStep;
import net.swordie.ms.world.tms282.newbie.steps.WarpMenuStep;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The data check of a parsed configuration (the server's {@link NewbieFlowSource} refuses a file with problems):
 * <ul>
 *     <li>every gift item is in the 282 client's String.wz ({@code data/wz282/String.wz}), is not in
 *     items-removed.tsv and has a server template ({@link Tms282ItemAvailability#check} OK);</li>
 *     <li>the lobby and every warp destination load as server fields ({@link FieldData#getFieldById});</li>
 *     <li>the speaker NPC is in the 282 String.wz Npc file.</li>
 * </ul>
 * Without the 282 String.wz (a 265 test root) the client-side part is skipped.
 */
public final class NewbieFlowData {
    static final Path STRING_DIR = Path.of("data", "wz282", "String.wz");
    private static final String[] ITEM_FILES = {"Eqp", "Consume", "Cash", "Etc", "Ins", "Pet"};
    private static final Pattern ID = Pattern.compile("<imgdir name=\"(\\d{4,8})\"");
    private static volatile Set<Integer> items282;
    private static volatile Set<Integer> npcs282;

    private NewbieFlowData() {
    }

    public static List<String> problems(NewbieFlowConfig c) {
        List<String> out = new ArrayList<>();
        if (!c.enabled()) return out;
        Set<Integer> ids = new LinkedHashSet<>();
        for (NewbieStep s : c.steps()) {
            if (s instanceof StarterGiftStep g) ids.addAll(g.allItemIds());
        }
        Set<Integer> client = items282();
        for (int id : ids) {
            if (client != null && !client.contains(id)) {
                out.add("item " + id + " is not in the 282 client (String.wz)");
                continue;
            }
            Tms282ItemAvailability.Verdict v = Tms282ItemAvailability.check(id);
            if (v != Tms282ItemAvailability.Verdict.OK) out.add("item " + id + ": " + v);
        }
        Set<Integer> maps = new LinkedHashSet<>();
        maps.add(c.lobbyMap());
        for (NewbieStep s : c.steps()) {
            if (s instanceof WarpMenuStep w) w.destinations().forEach(d -> maps.add(d.map()));
            if (s instanceof net.swordie.ms.world.tms282.newbie.steps.SpawnStep sp) {
                sp.byLoginJob().values().forEach(t -> {
                    if (t > 0) maps.add(t);
                });
                if (sp.defaultTarget() > 0) maps.add(sp.defaultTarget());
            }
        }
        for (int m : maps) {
            if (!FieldAvailability.active().isAvailable(m)) {
                out.add("map " + m + " is absent from the client");
                continue;
            }
            Field f;
            try {
                f = FieldData.getFieldById(m);
            } catch (RuntimeException e) {
                f = null;
            }
            if (f == null) {
                out.add("map " + m + " cannot be loaded by the server");
                continue;
            }
            if (m == c.lobbyMap()) {
                if (f.getPortalByID(c.lobbyPortal()) == null) out.add("lobby portal " + c.lobbyPortal() + " does not exist");
                if (!f.getOnUserEnter().isEmpty() || !f.getOnFirstUserEnter().isEmpty()) out.add("lobby has an entry script");
                if (!f.getMobs().isEmpty()) out.add("lobby has monsters");
            }
            for (NewbieStep s : c.steps()) {
                if (s instanceof WarpMenuStep w) for (WarpMenuStep.Destination d : w.destinations()) {
                    if (d.map() == m && f.getPortalByID(d.portal()) == null) out.add("map " + m + " portal " + d.portal() + " does not exist");
                }
            }
        }
        Set<Integer> npcs = npcs282();
        if (npcs != null && !npcs.contains(c.speakerNpc())) out.add("speakerNpc " + c.speakerNpc() + " is not in the 282 client (String.wz Npc)");
        return out;
    }

    private static Set<Integer> items282() {
        Set<Integer> s = items282;
        if (s == null) {
            synchronized (NewbieFlowData.class) {
                if (items282 == null) items282 = scan(ITEM_FILES);
                s = items282;
            }
        }
        return s.isEmpty() ? null : s;
    }

    private static Set<Integer> npcs282() {
        Set<Integer> s = npcs282;
        if (s == null) {
            synchronized (NewbieFlowData.class) {
                if (npcs282 == null) npcs282 = scan(new String[]{"Npc"});
                s = npcs282;
            }
        }
        return s.isEmpty() ? null : s;
    }

    private static Set<Integer> scan(String[] files) {
        Set<Integer> ids = new HashSet<>();
        for (String f : files) {
            Path p = STRING_DIR.resolve(f + ".img.xml");
            if (!Files.isRegularFile(p)) continue;
            try (BufferedReader in = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                String line;
                while ((line = in.readLine()) != null) {
                    Matcher m = ID.matcher(line);
                    if (m.find()) ids.add(Integer.parseInt(m.group(1)));
                }
            } catch (IOException | RuntimeException e) {
                System.out.println("[Newbie] cannot read " + p + ": " + e);
            }
        }
        return ids;
    }
}
