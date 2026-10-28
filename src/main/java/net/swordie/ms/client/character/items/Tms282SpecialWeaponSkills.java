package net.swordie.ms.client.character.items;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.ItemConstants;

/** Exact original282 native-positive Genesis/Destiny weapon sources, intersected with local282 templates.
 * Dynamic native lookups not yet resolved are intentionally absent; names/exItem flags alone are insufficient. */
public final class Tms282SpecialWeaponSkills {
    private Tms282SpecialWeaponSkills() { }
    private record Source(int passive, int active) { }
    private static final Set<Integer> PASSIVES = Set.of(80002632, 80003873, 80004115);
    private static final Set<Integer> ACTIVES = Set.of(80002633, 80003874, 80004116);
    private static final class Holder { static final Map<Integer, Source> SOURCES = load(); }
    private static Map<Integer, Source> load() {
        Map<Integer, Source> result = new HashMap<>();
        try {
            for (String row : Files.readAllLines(Path.of(ServerConstants.DIR,
                    "data/protocol/tms282.3/special-weapon-skills.tsv"))) {
                if (row.startsWith("item_id\t") || row.isBlank()) continue;
                String[] c = row.split("\t", -1);
                if (c.length != 5) throw new IllegalArgumentException("Malformed original282 special weapon source");
                int item = Integer.parseInt(c[0]), passive = Integer.parseInt(c[1]), active = Integer.parseInt(c[2]);
                if (!PASSIVES.contains(passive) || active != passive + 1 || !ACTIVES.contains(active)
                        || item <= 0 || result.putIfAbsent(item, new Source(passive, active)) != null)
                    throw new IllegalArgumentException("Invalid/duplicate original282 special weapon source");
            }
            if (result.isEmpty()) throw new IllegalArgumentException("Empty special weapon sources");
            return Map.copyOf(result);
        } catch (IOException e) { throw new IllegalStateException("Cannot read original282 special weapon sources", e); }
    }
    private static Source equippedSource(Equip equip) {
        // This server stores absolute bag indices; inventory type distinguishes worn slot11 from bag slot11.
        if (equip == null || equip.getInvType() != net.swordie.ms.enums.InvType.EQUIPPED
                || equip.getBagIndex() != BodyPart.Weapon.getVal() || !ItemConstants.isWeapon(equip.getItemId())
                || equip.isCash() || equip.getDateExpire() == null || equip.getDateExpire().isExpired()) return null;
        return Holder.SOURCES.get(equip.getItemId());
    }
    public static Set<Integer> allPassiveIds() { return PASSIVES; }
    public static Set<Integer> passiveIds(int itemId) {
        Source source = Holder.SOURCES.get(itemId);
        return source == null ? Set.of() : Set.of(source.passive());
    }
    public static boolean isActiveSkill(int skillId) { return ACTIVES.contains(skillId); }
    public static boolean isReservedSkill(int skillId) { return PASSIVES.contains(skillId) || ACTIVES.contains(skillId); }
    /** Passive/active records are never attack IDs; enhanced Destiny has no evidenced child burst.
     * The two supported children separately require a one-shot grant, independent of combat log policy. */
    public static boolean rejectUnimplementedAttack(int skillId) {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3
                && (isReservedSkill(skillId) || skillId == 80004117);
    }
    /** All three lifecycles use exact native CTS; only the two original cancellation bursts receive a grant. */
    public static boolean effectSupported(int skillId) { return isActiveSkill(skillId); }
    public static Map<Integer, Integer> passiveLevels(Equip equip) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3) return Map.of();
        Source source = equippedSource(equip);
        return source == null ? Map.of() : Map.of(source.passive(), 1);
    }
    /** Active liberation skills must ALREADY be learned. A weapon template does not unlock them. */
    public static boolean sourceEligible(Char chr, int skillId) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3 || chr == null || !isActiveSkill(skillId)
                || net.swordie.ms.constants.JobConstants.isZero(chr.getJob())
                || chr.getSkillLevel(skillId) != 1 || chr.getHP() <= 0) return false;
        Item item = chr.getEquippedItemByBodyPart(BodyPart.Weapon);
        if (!(item instanceof Equip equip) || chr.getEquippedInventory().getItems().stream().noneMatch(i -> i == equip)) return false;
        Source source = equippedSource(equip);
        return source != null && source.active() == skillId;
    }
    public static boolean canUse(Char chr, int skillId) { return effectSupported(skillId) && sourceEligible(chr, skillId); }
}
