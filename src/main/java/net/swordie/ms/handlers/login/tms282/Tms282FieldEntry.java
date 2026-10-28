package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282StagePackets;
import net.swordie.ms.loaders.FieldData;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.migration.FieldEntryPolicy;
import net.swordie.ms.world.migration.Gms265FieldEntry;
import net.swordie.ms.world.migration.MigrationContext;
import net.swordie.ms.world.migration.MigrationTicket;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * TMS 282.3 field entry (CP007E after the ticket was claimed), in the probe's order:
 * <ol>
 *     <li>the character belongs to the ticket's account and world ({@link #validate});</li>
 *     <li>character data is loaded (the 265 loads; a failure ends the session);</li>
 *     <li>the saved field, or 100000000 when the server data lacks it (in memory only, as the probe);</li>
 *     <li>LP02EF {@link Tms282StagePackets#setFieldFirstEntry} with the zero-based wire channel;</li>
 *     <li>LP0B16 key map and LP02EE macros ({@link Tms282KeyConfig#sendInit}), as the probe's sendKeyConfig;</li>
 *     <li>then the 265 server-side field entry (warp, timers, first-login init). Its 265 packets have no
 *     282 codec and are dropped by the fail-closed OutboundGuard; a failure there is logged, the client keeps
 *     its field.</li>
 * </ol>
 * The last character is recorded for the LP0B7F quick-login avatar response.
 */
final class Tms282FieldEntry implements FieldEntryPolicy {
    private final Charset nameCharset;
    private final Tms282KeyConfig keyConfig;

    Tms282FieldEntry(Charset nameCharset) {
        this.nameCharset = Objects.requireNonNull(nameCharset, "nameCharset");
        this.keyConfig = new Tms282KeyConfig(nameCharset);
    }

    @Override
    public int worldIdOf(MigrationTicket ticket) {
        return ticket.worldId();
    }

    @Override
    public String validate(Char chr, Account account, MigrationTicket ticket) {
        CharacterStat stat = chr.getAvatarData() == null ? null : chr.getAvatarData().getCharacterStat();
        if (stat == null) {
            return "character " + chr.getId() + " could not be loaded";
        }
        if (chr.getAccId() != ticket.accountId() || stat.getWorldIdForLog() != ticket.worldId()) {
            return "character " + chr.getId() + " is not in account " + ticket.accountId() + " world " + ticket.worldId();
        }
        return null;
    }

    @Override
    public boolean recordsLastCharacter() {
        return true;
    }

    @Override
    public void reject(Client c, String reason) {
        DataPrinter.send(DataPrinter.CLIENT_ERROR, reason);
        Tms282LoginState.of(c).terminate("CP007E " + reason);
    }

    @Override
    public void enter(MigrationContext context) {
        Client c = context.client();
        Char chr = context.chr();
        if (context.ticket() != null) {
            Tms282LoginState.of(c).inheritFileReportVerification(context.ticket().origin());
        }
        // The 265 load writes packets (e.g. quest record 333333 when a character first reaches 30) that the 282 client
        // cannot take before LP02EF; hold them and send them right after it (live crash 2026-09-24).
        c.holdOutbound();
        // step 6: the save gate of this id first, so a logout save of the previous session that is still running
        // (it no longer holds ECONOMY_MONITOR) finishes before the recovery and the load read the rows
        int loaded = net.swordie.ms.client.character.tms282.EconomyLocks.withGate(chr.getId(), () -> loadGated(c, chr, context));
        if (loaded < 0) {
            return;
        }
        if (loaded == 0) {
            c.releaseOutboundAfter((net.swordie.ms.connection.OutPacket) null);
            reject(c, "character " + chr.getId() + " data could not be loaded");
            return;
        }
        net.swordie.ms.client.character.tms282.Tms282MyHomeRuntime.beforeLoginField(chr);
        try {
            net.swordie.ms.world.tms282.newbie.NewbieFlowService.get().beforeLoginField(chr, context.account());
        } catch (java.sql.SQLException | RuntimeException e) {
            c.releaseOutboundAfter((OutPacket) null);
            reject(c, "新手資料暫時無法載入，請稍後再試。");
            System.out.println("[Newbie] entry refused for character " + chr.getId() + ": " + e);
            return;
        }
        net.swordie.ms.client.character.tms282.Tms282KinesisMp.repair(chr); // 265 kept Kinesis max MP at the PP cap
        net.swordie.ms.client.character.tms282.Tms282InnerAbility.unlockLines(chr); // lines due at 30 / 50 before [28] is sent
        CharacterStat stat = chr.getAvatarData().getCharacterStat();
        int savedField = (int) stat.getPosMap();
        if (FieldData.getFieldById(savedField) == null
                || !net.swordie.ms.connection.protocol.codec.FieldAvailability.active().isAvailable(savedField)) {
            // 993166000 (the creation map) is in both the 265 data and the 282 client; a saved map missing from the
            // server data falls back to 100000000, also present in both (start-map-282-data-audit.md).
            System.out.println("[TMS282] CHANNEL field " + savedField + " missing from server data; using "
                    + Tms282StagePackets.FALLBACK_FIELD_ID);
            stat.setPosMap(Tms282StagePackets.FALLBACK_FIELD_ID);
            stat.setPortal(0);
        }
        Field field = chr.getOrCreateFieldByCurrentInstanceType((int) stat.getPosMap());
        if (field == null) {
            c.releaseOutboundAfter((net.swordie.ms.connection.OutPacket) null);
            reject(c, "field " + stat.getPosMap() + " missing from server data");
            return;
        }
        var seeds = Tms282StagePackets.newDamageSeeds();
        var size = Tms282StagePackets.FieldSize.of(field);
        int wireChannel = context.ticket().clientChannelId();
        net.swordie.ms.client.character.social.tms282.RingRecordService.get().onLogin(chr); // social: ring records before LP02EF CharacterData is built
        OutPacket setField = Tms282StagePackets.setFieldFirstEntry(chr, wireChannel, size, seeds, nameCharset);
        // LP02EF and the key/macro init first (the verified probe order), then what the 265 load wrote before it.
        c.releaseOutboundAfter(() -> {
            c.write(setField);
            Tms282LoginState.of(c).enteredField(chr, seeds);
            System.out.println("[TMS282] SENT LP02EF SetField first entry character=" + chr.getId() + " name=" + chr.getName()
                    + " map=" + stat.getPosMap() + " portal=" + stat.getPortal() + " channel=" + wireChannel
                    + " size=" + size.width() + "x" + size.height());
            keyConfig.sendInit(Tms282LoginState.of(c), chr);
        });
        try {
            placeInField(context, field);
        } catch (RuntimeException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_MIGRATION, e);
            System.out.println("[TMS282] server-side field entry of character " + chr.getId()
                    + " failed after LP02EF (client keeps its field): " + e);
        }
        // the foothold kept its value from the last session / map until the first move (spawned-object audit 09-29):
        // the one below the spawn position, used by everything that copies the owner's foothold
        if (chr.getPosition() != null && chr.getField() != null) {
            var below = chr.getField().findFootHoldBelow(chr.getPosition());
            if (below != null) {
                chr.setFoothold((short) below.getId());
            }
        }
        sendHexaState(c, chr);
        net.swordie.ms.client.character.tms282.Tms282InnerAbility.afterEntry(c, chr); // LP0138 active ability preset
        try {
            Tms282CubeHandlers.restorePending(chr);
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Could not restore pending native cube choice",e);
        }
        net.swordie.ms.client.social.Guild.Tms282GuildRuntime.onFieldEntry(chr,nameCharset);
        net.swordie.ms.client.character.tms282.Tms282SkillRingRuntime.onFieldEntry(chr);
        net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.onFieldEntry(chr);
        net.swordie.ms.world.tms282.mesomarket.MesoMarketService.onFieldEntry(chr); // meso market: offline settlements
        sendQuestRecords(c, chr);
        if (net.swordie.ms.scripts.ScriptManagerImpl.GAME_SCRIPTS_ENABLED)
            net.swordie.ms.world.tms282.contents.changeburning.ChangeBurning.get().onLogin(chr); // 變身燃燒 skill / QR / exp table
    }

    static void placeInField(MigrationContext context, Field field) {
        Gms265FieldEntry.placeInField(context, field);
        // The first LP02EF precedes placement; the later 265 characterData SetField is deliberately dropped.
        if (context.chr().getJobHandler() instanceof net.swordie.ms.client.jobs.sengoku.Kanna kanna
                && kanna.getOniController().getControllerId() == 0) kanna.getOniController().enterField();
    }

    /**
     * The first-entry CharacterData has no QuestRecord / QuestComplete block (Tms282StagePackets.FIRST_ENTRY_FLAGS), so
     * the client started every session with an empty completed-quest map (CharacterData+0xEA3, filled by 0x8C29C0 also
     * from LP00AE type 1). The main menu greys out 聯盟 (quest 16013), 靈魂收藏 (68840) and 世界檔案館 (600136) from that
     * map (refresh 0x28785E0, 0x8C2910), and quest UIs start from nothing. Replay every record as LP00AE type 1
     * (2026-09-29): completed with its time, started with its QR value.
     */
    private void sendQuestRecords(Client c, Char chr) {
        int completed = 0, started = 0;
        try {
            for (var q : new java.util.ArrayList<>(chr.getQuests().values())) {
                if (q == null || q.getQRKey() <= 0) {
                    continue;
                }
                if (q.getStatus() == net.swordie.ms.enums.QuestStatus.Completed) {
                    var ft = q.getCompletedTime();
                    long when;
                    if (ft == null) {
                        when = net.swordie.ms.connection.packet.Tms282QuestPackets.fileTimeOf(System.currentTimeMillis());
                    } else { // exactly the 8 bytes 265 encodeFT writes (millis converted, or a client FILETIME as is)
                        OutPacket tmp = new OutPacket();
                        ft.encode(tmp);
                        when = java.nio.ByteBuffer.wrap(tmp.getData(), 0, 8).order(java.nio.ByteOrder.LITTLE_ENDIAN).getLong();
                    }
                    c.write(net.swordie.ms.connection.packet.Tms282QuestPackets.questRecordCompleted(q.getQRKey(), when));
                    completed++;
                } else if (q.getStatus() == net.swordie.ms.enums.QuestStatus.Started) {
                    String value = q.getQRValue();
                    c.write(net.swordie.ms.connection.packet.Tms282QuestPackets.questRecordStarted(q.getQRKey(), value == null ? "" : value, nameCharset));
                    started++;
                }
            }
        } catch (RuntimeException e) {
            System.out.println("[TMS282] quest record replay failed for character " + chr.getId() + ": " + e);
        }
        System.out.printf("[TMS282] quest records replayed character=%d completed=%d started=%d%n", chr.getId(), completed, started);
        // 楓之谷成就: the main menu enables it only when achievement 1 (「楓之谷成就的開始」) mission 0xFF has state 2
        // (0x2878797 -> 0x8F84E0 key 1255). LP00AE type 44 ACHIEVEMENT_INIT: i32 n, n x {i32 id, u8 mission, u8 state,
        // FILETIME, i32 kind, kind 42 ? str : i64} - 5ED27F0 -> 5F7FA70 -> 8F89A0 traced CLEAN (09-29).
        OutPacket achievement = OutPacket.forProfile(net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3, 0x00AE);
        achievement.encodeByte(44);
        achievement.encodeInt(1);
        achievement.encodeInt(1);
        achievement.encodeByte(0xFF);
        achievement.encodeByte(2);
        achievement.encodeLong(net.swordie.ms.connection.packet.Tms282QuestPackets.fileTimeOf(System.currentTimeMillis()));
        achievement.encodeInt(42);
        achievement.encodeString("");
        c.write(achievement);
    }

    /**
     * 6th job: the first-entry CharacterData leaves the HEXA skill / stat blocks out (Tms282StagePackets flags), so a
     * character that has any gets them as LP0272 / LP0273 (265 builders, Tms282HexaMatrixTranslators); the HEXA cost
     * tables (LP0274) come from 265 Char.initialize in placeInField.
     */
    private static void sendHexaState(Client c, Char chr) {
        try {
            if (!chr.getHexaSkills().isEmpty()) {
                c.write(net.swordie.ms.connection.packet.WvsContext.hexaSkillsUpdate(chr));
            }
            if (!chr.getHexaStats().isEmpty()) {
                c.write(net.swordie.ms.connection.packet.WvsContext.hexaStatsUpdate(chr));
            }
        } catch (RuntimeException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_MIGRATION, e);
            System.out.println("[TMS282] HEXA state of character " + chr.getId() + " not sent: " + e);
        }
    }

    /** Journal recovery + 265 load under the save gate: 1 loaded, 0 load failed, -1 recovery refused (rejected). */
    private int loadGated(Client c, Char chr, MigrationContext context) {
        try {
            if(context.firstLogin())net.swordie.ms.client.character.tms282.Tms282SkillRingRuntime.beforeLoad(chr);
            if(context.firstLogin())net.swordie.ms.client.character.tms282.Tms282CubeRuntime.beforeLoad(chr);
            if(context.firstLogin())net.swordie.ms.client.character.tms282.Tms282MyHomeRuntime.beforeLoad(chr);
            if(context.firstLogin())net.swordie.ms.client.social.Guild.Tms282GuildRuntime.beforeLoad(chr);
            if(context.firstLogin())net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.beforeLoad(chr);
        } catch(java.sql.SQLException | RuntimeException e) {
            c.releaseOutboundAfter((OutPacket)null);
            reject(c,"角色資料尚未完成復原，請稍後再試。");
            return -1;
        }
        return Gms265FieldEntry.loadCharacterData(context) ? 1 : 0;
    }
}
