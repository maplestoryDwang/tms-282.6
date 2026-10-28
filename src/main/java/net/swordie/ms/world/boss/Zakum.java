package net.swordie.ms.world.boss;

import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.BossConstants;
import net.swordie.ms.enums.WeatherEffNoticeType;
import net.swordie.ms.world.field.Field;

public class Zakum {

    public static boolean isTms282() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    public static void spawnOnEnter(Char chr) {
        if (!isTms282()) {
            return;
        }
        Field field = chr.getField();
        int mode = switch (field.getId()) {
            case BossConstants.ZAKUM_EASY_ALTAR -> 1;
            case BossConstants.ZAKUM_NORMAL_ALTAR -> 2;
            case BossConstants.ZAKUM_CHAOS_ALTAR -> 3;
            default -> 0;
        };
        if (mode != 0) {
            spawnTms282(mode, field, chr);
        }
    }

    // All entry scripts and the fire-eye route share one field-local reservation.
    private static void spawnTms282(int mode, Field field, Char chr) {
        synchronized (field) {
            if (field.getZakumStand() != 0 || mode < 1 || mode > 3) {
                return;
            }
            field.setZakumStand(1);
            field.setBossSpawned(true);
            int body = switch (mode) {
                case 1 -> BossConstants.ZAKUM_EASY_BODY;
                case 2 -> BossConstants.ZAKUM_NORMAL_BODY;
                default -> BossConstants.ZAKUM_CHAOS_BODY;
            };
            int arm = switch (mode) {
                case 1 -> BossConstants.ZAKUM_EASY_ARM;
                case 2 -> BossConstants.ZAKUM_NORMAL_ARM;
                default -> BossConstants.ZAKUM_CHAOS_ARM;
            };
            field.createZakumStand();
            for (int i = -1; i < 8; i++) {
                int id = i < 0 ? body : arm + i;
                if (chr != null) {
                    chr.getScriptManager().spawnBossMob(id, BossConstants.ZAKUM_SPAWN_X, BossConstants.ZAKUM_SPAWN_Y);
                } else {
                    field.spawnMob(id, BossConstants.ZAKUM_SPAWN_X, BossConstants.ZAKUM_SPAWN_Y, false);
                }
            }
        }
    }

    public static void spawn(int mode, Field field) {
        if (isTms282()) {
            spawnTms282(mode, field, null);
            return;
        }
        if (field.getZakumStand() == 0) {
            field.setZakumStand(1);
            final short x = BossConstants.ZAKUM_SPAWN_X;
            final short y = BossConstants.ZAKUM_SPAWN_Y;
            int zakumBody = 0;
            int zakumArm = 0;
            switch (mode) {
                case 1 -> {
                    field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossKillNotice, "[簡單] 扎昆被火焰之眼的力量召喚出來了。"));
                    zakumBody = BossConstants.ZAKUM_EASY_BODY;
                    zakumArm = BossConstants.ZAKUM_EASY_ARM;
                }
                case 2 -> {
                    field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossKillNotice, "[普通] 扎昆被火焰之眼的力量召喚出來了。"));
                    zakumBody = BossConstants.ZAKUM_NORMAL_BODY;
                    zakumArm = BossConstants.ZAKUM_NORMAL_ARM;
                }
                case 3 -> {
                    field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossKillNotice, "[混沌] 扎昆被火焰之眼的力量召喚出來了。"));
                    zakumBody = BossConstants.ZAKUM_CHAOS_BODY;
                    zakumArm = BossConstants.ZAKUM_CHAOS_ARM;
                }
            }
            field.spawnMob(zakumBody, x, y, false);
            for (int i = 0; i < 8; i++) {
                field.spawnMob(zakumArm + i, x, y, false);
            }
            field.createZakumStand();
        }
    }
}
