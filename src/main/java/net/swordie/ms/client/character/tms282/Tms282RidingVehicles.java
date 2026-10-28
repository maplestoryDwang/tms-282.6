package net.swordie.ms.client.character.tms282;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * TMS 282.3 common riding skills (80xxxxxx and beginner-book mounts) -> RideVehicle id. The server's skill data carries no {@code vehicleID}
 * (the client keeps the mapping in Skill/RidingSkillInfo.img, which the loader never read), so every riding skill
 * did nothing. {@code data/protocol/tms282.3/riding-vehicles.tsv} is generated from the 282 client's own
 * Skill/RidingSkillInfo.img (research/tms2823/skill-coverage-1006/gen_riding.py + riding282/RidingDump.java); every
 * vehicle in it exists in the client's Character/TamingMob.
 * Job mounts (e.g. the Wild Hunter jaguar, 282 vehicle 1932982) stay with their job classes. Not live-tested.
 */
public final class Tms282RidingVehicles {
    public static final Path FILE = Path.of("data", "protocol", "tms282.3", "riding-vehicles.tsv");
    private static volatile Map<Integer, Integer> vehicles;

    private Tms282RidingVehicles() {
    }

    /** The vehicle of a common riding skill on the 282 profile, else 0. */
    public static int vehicleOf(int skillId) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3
                || !Boolean.parseBoolean(System.getProperty("tms282.ridingVehicles.enabled", "true"))) {
            return 0;
        }
        return table().getOrDefault(skillId, 0);
    }

    static Map<Integer, Integer> table() {
        Map<Integer, Integer> t = vehicles;
        if (t == null) {
            synchronized (Tms282RidingVehicles.class) {
                t = vehicles;
                if (t == null) {
                    t = load(Path.of(ServerConstants.DIR).resolve(FILE));
                    vehicles = t;
                }
            }
        }
        return t;
    }

    /** Parses the table (header and {@code #} lines skipped); a missing file gives an empty table. */
    public static Map<Integer, Integer> load(Path path) {
        Map<Integer, Integer> out = new HashMap<>();
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#") || line.startsWith("skill\t")) {
                    continue;
                }
                String[] f = line.split("\t", -1);
                if (f.length < 2) {
                    continue;
                }
                try {
                    int skill = Integer.parseInt(f[0].trim()), vehicle = Integer.parseInt(f[1].trim());
                    if (skill > 0 && vehicle > 0) {
                        out.put(skill, vehicle);
                    }
                } catch (NumberFormatException ignored) {
                    // a malformed row is skipped
                }
            }
        } catch (IOException e) {
            System.out.println("[TMS282] riding-vehicles: " + path + " not readable (" + e + "); riding skills stay off");
        }
        System.out.println("[TMS282] riding-vehicles: " + out.size() + " riding skills");
        return out;
    }
}
