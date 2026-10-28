package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.field.ClockPacket;
import net.swordie.ms.world.field.Field;

public class Gollux {

    public static void init(Char chr, int type) {
        if (BossHelper.checkInstance(chr)) {
            return;
        }
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getParty() == null) {
            return;
        }
        switch (type) {
            case 0 -> { // GiantBoss_Head
                Field field = chr.getOrCreateFieldByCurrentInstanceType(863010600);
                for (Char x : chr.getParty().getOnlineChars()) {
                    int fieldID = x.getField().getId();
                    if (fieldID != 863010600 && fieldID != 863010700 && x.getInstance() != null && fieldID >= 863010100 && fieldID <= 863010500) {
                        x.warp(field);
                    }
                }
                BossHelper.updateField(field, chr);
                //BossHelper.blockAttacks(chr);
                if (!field.isGollux_head()) {
                    BossHelper.spawnGollux(chr, 0);
                    field.spawnMobRespawnable(BossHelper.GOLLUX_RIGHT_SIDE_MOB, -650, 0, true, 1, 20);
                    field.spawnMobRespawnable(BossHelper.GOLLUX_LEFT_SIDE_MOB, -650, 0, true, 1, 20);
                    field.setGollux_head(true); // init Gollux Head
                }
                BossHelper.addCurrentField(field, chr);
                if (field.hasMobById(BossHelper.GOLLUX_SECOND_PHASE_HEAD)) {
                    field.broadcast(FieldPacket.syncDynamicFootHold("phase2-1", true, new Position(0, 0)));
                    field.broadcast(FieldPacket.syncDynamicFootHold("phase2-2", true, new Position(0, 0)));
                } else if (field.hasMobById(BossHelper.GOLLUX_THIRD_PHASE_HEAD)) {
                    field.broadcast(FieldPacket.syncDynamicFootHold("phase2-1", true, new Position(0, 0)));
                    field.broadcast(FieldPacket.syncDynamicFootHold("phase2-2", true, new Position(0, 0)));
                    field.broadcast(FieldPacket.syncDynamicFootHold("phase3", true, new Position(0, 0)));
                }
            }
            case 1 -> { // GiantBoss_LArm
                Field field = chr.getOrCreateFieldByCurrentInstanceType(863010430);
                BossHelper.openPortal(field, "phase3", 12);
                if (!field.isGollux_LArm()) {
                    field.spawnMob(BossHelper.GOLLUX_LEFT_SHOULDER, 85, 0, false);
                    field.setGollux_LArm(true);
                }
                if (!sm.hasMobsInField(863010430)) {
                    BossHelper.openPortal(field, "clear", 7);
                    BossHelper.openPortal(field, "phase3", 12);
                }
                BossHelper.addCurrentField(field, chr);
            }
            case 2 -> { // GiantBoss_RArm
                Field field = chr.getOrCreateFieldByCurrentInstanceType(863010330);
                BossHelper.openPortal(field, "phase3", 12);
                if (!field.isGollux_RArm()) {
                    field.spawnMob(BossHelper.GOLLUX_RIGHT_SHOULDER, 0, 0, false);
                    field.setGollux_RArm(true);
                }
                if (!sm.hasMobsInField(863010330)) {
                    BossHelper.openPortal(field, "clear", 7);
                    BossHelper.openPortal(field, "phase3", 12);
                }
                BossHelper.addCurrentField(field, chr);
            }
            case 3 -> { // GiantBoss_field
                int fieldID = chr.getField().getId();
                if (fieldID == 863010300
                        || fieldID == 863010200
                        || fieldID == 863010210
                        || fieldID == 863010220
                        || fieldID == 863010230
                        || fieldID == 863010310
                        || fieldID == 863010320
                        || fieldID == 863010400
                        || fieldID == 863010410
                        || fieldID == 863010420) {
                    Field field = chr.getOrCreateFieldByCurrentInstanceType(fieldID);
                    if (chr.getOrCreateFieldByCurrentInstanceType(863010600).getLifeByTemplateId(BossHelper.GOLLUX_THIRD_PHASE_HEAD) != null) {
                        field.broadcast(FieldPacket.clock(ClockPacket.timerGauge(chr.getInstance().getRemainingTime() * 1000, 90 * 1000)));
                    }
                    BossHelper.addCurrentField(field, chr);
                    if (field.getMobs().size() == 3) {
                        BossHelper.addClearedField(field, chr);
                        if (field.getId() == 863010310 || field.getId() == 863010410) {
                            BossHelper.openPortal(field, "open", 2);
                            BossHelper.openPortal(field, "clear", 1);
                        }
                    }
                }
            }
            case 4 -> { // GiantBoss_Hip
                Field field = chr.getOrCreateFieldByCurrentInstanceType(863010240);
                if (!field.isGollux_Hip()) {
                    field.spawnMob(BossHelper.GOLLUX_ABDOMEN, -4, -20, false);
                    field.spawnMobRespawnable(9390620, -694, -36, true, 1, 20);
                    field.setGollux_Hip(true);
                }
                if (!field.hasMobById(BossHelper.GOLLUX_ABDOMEN)) {
                    BossHelper.openPortal(field, "clear1", 12);
                    BossHelper.openPortal(field, "clear2", 12);
                }
                BossHelper.addCurrentField(field, chr);
            }
            case 5 -> { // onUserEnter_863010100
                Field field = chr.getOrCreateFieldByCurrentInstanceType(863010100);
                BossHelper.updateField(field, chr);
                // Triggers when you first enter the Gollux instance, dropping the right-leg
                if (field.getId() == 863010100) {
                    if (BossHelper.isAlreadyVisited(chr)) {
                        BossHelper.openPortal(field, "open", 2);
                    } else {
                        chr.getInstance().setTimeout(1800, true);
                        chr.setDeathCount(5);
                        chr.getTimer().addEvent(() -> {
                            BossHelper.openPortal(chr.getField(), "open", 1);
                            BossHelper.addClearedField(field, chr);
                        }, 4500);
                    }
                }
                BossHelper.addCurrentField(field, chr);
                // Triggers regardless of the 2 maps that utilize this script (Heart - 863010500, Road to Gollux - 863010100)
                if (chr.getOrCreateFieldByCurrentInstanceType(863010600).getLifeByTemplateId(BossHelper.GOLLUX_THIRD_PHASE_HEAD) != null) {
                    field.broadcast(FieldPacket.clock(ClockPacket.timerGauge(chr.getInstance().getRemainingTime() * 1000, 90 * 1000)));
                }
            }
            case 6 -> { // onUserEnter_863010700
                Field field = chr.getOrCreateFieldByCurrentInstanceType(863010700);
                if (field.getLifeByTemplateId(8630004) == null) {
                    if (!field.isBossSpawned()) {
                        int difficulty = sm.getGolluxDifficulty().getVal();
                        int reactorId = 8630000 + difficulty;
                        sm.spawnReactorInState(reactorId, 95, 67, (byte) 1);
                        field.setBossSpawned(true);
                    }
                } else {
                    if (!field.isBossSpawned()) {
                        sm.spawnReactorInState(8630004, 95, 67, (byte) 1);
                        field.setBossSpawned(true);
                    }
                }
                sm.chat("進入培羅德的心臟讓它平靜下來。動作要快，成功的機會稍縱即逝。");
            }
        }
    }
}
