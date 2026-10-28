package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.world.kit.CharacterKit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * {@code char.kit} (admin console, 角色 tab): copies an official character's look and gear onto an online character
 * ({@link CharacterKit}). The console downloads the source (a MapleKit character JSON) into
 * {@code data/kit-sources/<file>.json} first; this action only reads a file of that folder.
 * <ul>
 *     <li>{@code {file, preview:true}}: what the kit holds - source name, level, job, counts, the pieces that could not
 *     be mapped. Changes nothing.</li>
 *     <li>{@code {file, character, levelAndJob}}: applies it to the online character and answers what was done and
 *     what failed. The character's own gear is moved to its bags.</li>
 * </ul>
 */
public final class KitAdmin {
    public static final String FOLDER = "kit-sources";
    private static final Pattern FILE = Pattern.compile("[A-Za-z0-9_\\-]{1,64}");
    private static final ObjectMapper JSON = new ObjectMapper();

    private KitAdmin() {
    }

    public static final class Kit implements AdminAction {
        @Override
        public String name() {
            return "char.kit";
        }

        @Override
        public boolean mutating() {
            return true;
        }

        @Override
        public String auditSummary(JsonNode request) {
            return (request != null && request.path("preview").asBoolean() ? "preview " : "apply ") + "file=" + AdminParams.text(request, "file")
                    + " character=" + AdminParams.text(request, "character");
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
            String file = AdminParams.text(request, "file");
            if (!FILE.matcher(file).matches()) {
                return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "file");
            }
            Path path = ctx.env().resolve("data", FOLDER, file + ".json");
            if (!Files.isRegularFile(path) || Files.size(path) > 4L << 20) {
                return LauncherResult.fail(AdminCodes.NOT_FOUND).with("problem", "file");
            }
            JsonNode source = JSON.readTree(Files.readAllBytes(path));
            if (!source.path("itemEquipment").path("item_equipment").isArray() || !source.path("basic").isObject()) {
                return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "content");
            }
            CharacterKit.Kit kit = CharacterKit.build(source);
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("source", kit.source());
            summary.put("level", kit.level());
            summary.put("job", kit.job());
            summary.put("gender", kit.gender() == 1 ? "女" : kit.gender() == 0 ? "男" : "?");
            summary.put("hair", kit.hair());
            summary.put("face", kit.face());
            summary.put("skin", kit.skin());
            summary.put("equips", kit.equips().size());
            summary.put("cash", kit.cash().size());
            summary.put("skipped", kit.skipped());
            List<String> unmapped = new ArrayList<>();
            for (CharacterKit.Piece p : kit.equips()) {
                unmapped.addAll(unmappedLines(p, source));
            }
            summary.put("unmappedPotentials", unmapped.size());
            if (request.path("preview").asBoolean()) {
                return LauncherResult.ok(AdminCodes.OK).with("kit", summary);
            }
            Char chr = OpsAdmin.online(AdminParams.text(request, "character"));
            if (chr == null) {
                return LauncherResult.fail(AdminCodes.NOT_FOUND).with("problem", "character");
            }
            CharacterKit.Report report = CharacterKit.apply(chr, kit, !request.has("levelAndJob") || request.path("levelAndJob").asBoolean(true));
            return LauncherResult.ok(AdminCodes.OK).with("kit", summary).with("character", chr.getId()).with("name", chr.getName())
                    .with("done", report.done()).with("failed", report.failed());
        }

        /** Potential lines of the source that found no option (shown as a count: they are simply left empty). */
        private static List<String> unmappedLines(CharacterKit.Piece p, JsonNode source) {
            List<String> out = new ArrayList<>();
            for (JsonNode it : source.path("itemEquipment").path("item_equipment")) {
                if (!p.name().equals(it.path("item_name").asText("")) || !p.slot().equals(it.path("item_equipment_slot").asText(""))) {
                    continue;
                }
                for (int i = 0; i < 3; i++) {
                    if (p.pot()[i] == 0 && !it.path("potential_option_" + (i + 1)).asText("").isBlank()) {
                        out.add(p.name() + " " + it.path("potential_option_" + (i + 1)).asText());
                    }
                    if (p.bonus()[i] == 0 && !it.path("additional_potential_option_" + (i + 1)).asText("").isBlank()) {
                        out.add(p.name() + " " + it.path("additional_potential_option_" + (i + 1)).asText());
                    }
                }
            }
            return out;
        }
    }
}
