package net.swordie.ms.world.migration;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.jobs.JobManager;
import net.swordie.ms.client.jobs.adventurer.thief.DualBlade;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.FieldInstanceType;

import java.util.concurrent.TimeUnit;

/**
 * GMS 265 field entry: the former {@code MigrationHandler.handleMigrateIn} steps after the session was bound,
 * unchanged. The two phases are public so other profiles can reuse them around their own first-field packet.
 */
public final class Gms265FieldEntry implements FieldEntryPolicy {
    public static final Gms265FieldEntry INSTANCE = new Gms265FieldEntry();

    private Gms265FieldEntry() {
    }

    @Override
    public int worldIdOf(MigrationTicket ticket) {
        return ServerConstants.WORLD_ID;
    }

    @Override
    public String validate(Char chr, Account account, MigrationTicket ticket) {
        return null;
    }

    @Override
    public boolean recordsLastCharacter() {
        return true;
    }

    @Override
    public void reject(Client c, String reason) {
        DataPrinter.send(DataPrinter.CLIENT_ERROR, reason);
        c.close();
    }

    @Override
    public void enter(MigrationContext context) {
        if (!loadCharacterData(context)) {
            context.client().close();
            return;
        }
        net.swordie.ms.client.character.social.tms282.RingRecordService.get().onLogin(context.chr()); // social: ring records (265 path; 282 loads in Tms282FieldEntry)
        placeInField(context, null);
    }

    /**
     * Instance type, stat fixes and (first login) every character data load, then job handler and party.
     *
     * @return false when a load threw (logged to EXCEPTION_CAUGHT_MIGRATION); the caller rejects the client
     */
    public static boolean loadCharacterData(MigrationContext context) {
        Char chr = context.chr();
        boolean isFirstLogin = context.firstLogin();
        chr.setFieldInstanceType(FieldInstanceType.CHANNEL);

        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        if (cs.getExp() < 0) {
            cs.setExp(Math.abs(cs.getExp()));
        }
        chr.initKanna();
        try {
            if (isFirstLogin) {
                chr.loadCharacterData();
                chr.loadCharacterPartyData();
                chr.loadCharacterGuildData();
                chr.initBlessingSkillNames();
                chr.initSkillAlarms();
                chr.initHyperStats();
                chr.initCharacterPotentials();
                if (chr.getAccount() != null) {
                    chr.getAccount().ensureLinkSkillRows();
                }
                chr.initLinkSkills(true);
                chr.initEquips();
                chr.initCompletedSetItemID();
                chr.initRewardSystem();
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_MIGRATION, e);
            return false;
        }
        if (isFirstLogin) {
            if (chr.getJobHandler() == null) {
                chr.setJobHandler(chr.getSubJob() == 1 ? new DualBlade(chr) : JobManager.getJobById(chr.getJob(), chr));
            }
            var party = chr.getWorld().getPartyByPartyID(chr.getPartyID());
            if (chr.getPartyID() != 0 && party != null) {
                chr.setParty(party);
            }
        }
        return true;
    }

    /**
     * Warps the character into its field (265 SetField and the rest of the entry packets), schedules
     * {@code Char.initialize}, and runs the first-login initialisation.
     *
     * @param field the field to enter, or null for the saved field (HOME_MAP when none)
     */
    public static void placeInField(MigrationContext context, Field field) {
        Client c = context.client();
        Char chr = context.chr();
        boolean isFirstLogin = context.firstLogin();
        chr.setBurningFieldLevel(FieldConstants.BURNING_FIELD_LEVEL_ON_START);
        if (field == null) {
            field = chr.getOrCreateFieldByCurrentInstanceType(chr.getFieldID() <= 0 ? FieldConstants.HOME_MAP : chr.getFieldID());
        }
        chr.initAndroid(true);
        //Set this before warp for Set Field.
        chr.warp(field, true);

        Char currentChar = chr;
        chr.getTimer().addEvent(currentChar::initialize, 1500, TimeUnit.MILLISECONDS);

        chr.checkAndRemoveExpiredItems(isFirstLogin);
        if (isFirstLogin) {
            chr.initBaseStats();
            chr.initTraits();
            chr.initFatigueTimer();
        }
        chr.setOnline(true); // v195+: respect 'invisible login' setting
        net.swordie.ms.GameSettings.onChannelEnter(chr); // game.slide (no-op while empty)
        // multiplayer-0930: a relogin / channel change no longer calls warpInstanceOut (it cleared the party's boss
        // instance and warped every online member out); only this character's stale eligibility is dropped.
        net.swordie.ms.world.field.InstanceMembership.onFieldEntry(chr);
        //chr.write(WvsContext.checkProcessResult(true));
        if (ServerConfig.IP_LOG && isFirstLogin) {
            DataPrinter.send(DataPrinter.LOGIN, String.format("[IP: %s] [賬號: %s] [角色: %s] 已進入遊戲。"
                            + " | Machine ID: " + Util.readableByteArray(context.request().machineId()),
                    c.getIP(), context.user().getName(), chr.getName()));
        }
    }
}
