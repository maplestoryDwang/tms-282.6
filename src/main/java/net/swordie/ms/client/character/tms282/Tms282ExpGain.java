package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.ExpIncreaseInfo;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282CombatPackets;
import net.swordie.ms.connection.packet.Tms282EffectPackets;
import net.swordie.ms.connection.packet.Tms282UserRemotePackets;
import net.swordie.ms.connection.protocol.codec.ExpGainStrategy;
import net.swordie.ms.constants.GameConstants;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * TMS 282.3 EXP gain (the {@link ExpGainStrategy} of the 282 profile): the 265 {@code Char.addExp} sends MESSAGE
 * IncEXP and STAT_CHANGED in 265 form (the fail-closed guard drops both: LP00AE and LP009D are PROFILE_CODEC) and
 * levels up through {@code Job.handleLevelUp}, whose SP/skill/effect packets are 265 too. Here the EXP and every
 * level-up go through the skill agent's {@link Tms282LevelService} (the numeric port of that 265 code), and the
 * client gets, in 265 order:
 * <ol>
 *     <li>LP00AE IncEXP ({@link Tms282CombatPackets#incExpMessage}) when the source shows a message;</li>
 *     <li>LP009D level/exp/ap/max hp/max mp/hp/mp, then LP009D SP and LP00A2 on a level-up (the level service's
 *     packets).</li>
 * </ol>
 * Writes: a gain that levels up saves characterstats (and changed skills) through the persistent store at once, as
 * 265 does for high levels; plain EXP stays in memory until the 265 character save (disconnect, periodic), so a
 * mob kill costs no database write. Not ported (265 extras): the death-penalty EXP quest 100716, guild level-up
 * notices. A level-up shows LP048E LevelUp (type 0, traced) to the other users of the field, as 265 does. The
 * idle-hunting counters are kept.
 */
public final class Tms282ExpGain implements ExpGainStrategy {
    /** A store that writes nothing (plain EXP gains). */
    static final Tms282CharacterStore IN_MEMORY = new Tms282CharacterStore() {
        @Override
        public void saveSkill(net.swordie.ms.client.character.skills.Skill skill) {
        }

        @Override
        public void saveStats(CharacterStat stat) {
        }

        @Override
        public String toString() {
            return "in-memory";
        }
    };

    private final Tms282LevelService levelUps;
    private final Tms282LevelService plainGains;
    private final Consumer<String> log;

    public Tms282ExpGain(Tms282CharacterStore store, Consumer<String> log) {
        this.levelUps = new Tms282LevelService(Objects.requireNonNull(store, "store"));
        this.plainGains = new Tms282LevelService(IN_MEMORY);
        this.log = Objects.requireNonNull(log, "log");
    }

    /** The server's: level-ups written to the 265 database. */
    public static Tms282ExpGain standard() {
        return new Tms282ExpGain(Tms282CharacterStore.SQL, System.out::println);
    }

    @Override
    public void addExp(Char chr, long amount, ExpIncreaseInfo info) {
        if (amount <= 0 || chr.getHP() <= 0) {
            return;
        }
        Tms282LevelService.Result result = (willLevelUp(chr, amount) ? levelUps : plainGains).addExp(chr, amount);
        if (info != null) {
            long shown = info.getIncEXP() > 0 ? info.getIncEXP() : amount;
            chr.write(Tms282CombatPackets.incExpMessage(info, shown)); // with the bonus lines (placeholder-audit-0929 #5)
        }
        for (OutPacket p : result.packets()) {
            chr.write(p);
        }
        if (result.toLevel() != result.fromLevel()) {
            // 265 Char.addExp: the level-up effect goes to the other users only (the client plays its own).
            if (chr.getField() != null) {
                chr.getField().broadcast(Tms282UserRemotePackets.effect(chr.getId(), Tms282EffectPackets.levelUp()), chr);
            }
            log.accept("[TMS282] LEVEL UP character=" + chr.getId() + " " + result.fromLevel() + "->" + result.toLevel()
                    + " exp=" + result.exp() + " sp+" + result.totalSpGained() + " " + String.join("; ", result.log())
                    + "; SENT LP009D" + (result.packets().size() > 1 ? " + SP/skill records" : "")
                    + "; FIELD LP048E LevelUp");
            Tms282JobAdvance.afterLevelUp(chr, result.fromLevel(), result.toLevel()); // 60 / 100 ... automatic advancement
        }
        if (chr.getIdleHunting() != null && chr.isStartedHunting()) {
            chr.getIdleHunting().incEXP(amount);
            chr.getIdleHunting().incMob(1);
        }
    }

    /** Whether {@code amount} crosses the EXP needed for the next level (the gain then saves the character). */
    static boolean willLevelUp(Char chr, long amount) {
        CharacterStat stat = chr.getAvatarData().getCharacterStat();
        int level = stat.getLevel();
        return level < GameConstants.MAX_LEVEL && Math.abs(stat.getExp()) + amount >= GameConstants.charExp[level];
    }
}
