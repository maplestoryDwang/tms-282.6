package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.BossConstants;
import net.swordie.ms.enums.BossPartyType;
import net.swordie.ms.enums.WeatherEffNoticeType;
import net.swordie.ms.life.Life;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.world.field.Field;

public class RootAbyss {

    public static void spawnPierre(Char chr, PierreMode mode) {
        if (BossHelper.checkInstance(chr)) {
            return;
        }

        if (chr.getParty() != null && !chr.getParty().isLeader(chr)) {
            chr.chatMessage("只有隊長可以召喚該 Boss。");
            return;
        }
        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        if (field.isBossSpawned() || !field.getMobs().isEmpty()) {
            return;
        }
        field.setBossSpawned(true);
        switch (mode) {
            case NORMAL -> { // pierre_Summon
                field.getTimer().addEvent(() -> {
                    field.spawnMob(BossHelper.PIERRE_1, 497, 551, false);
                    field.broadcast(UserPacket.effect(Effect.effectFromWZ("Map/Effect.img/rootabyss/firework")));
                    field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossPierre, "由衷歡迎來到茶會！"));
                }, 2000);
            }
            case CHAOS -> { // pierre_Summon1
                field.getTimer().addEvent(() -> {
                    field.spawnMob(BossHelper.CHAOS_PIERRE_1, 497, 551, false);
                    field.broadcast(UserPacket.effect(Effect.effectFromWZ("Map/Effect.img/rootabyss/firework")));
                    field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossPierre, "由衷歡迎來到茶會！"));
                }, 2000);
            }
        }
    }

    public enum PierreMode {
        NORMAL(0),
        CHAOS(1),
        ;

        private final int val;

        PierreMode(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }
    }

    public static void spawn(Char chr, String action) {
        if (BossHelper.checkInstance(chr)) {
            return;
        }

        if (chr.getParty() != null && !chr.getParty().isLeader(chr)) {
            chr.chatMessage("只有隊長可以召喚該 Boss。");
            return;
        }
        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        if (!field.getMobs().isEmpty()) {
            return;
        }
        switch (action) {
            case "banbanNormal" -> {
                final int REACTOR = 1058016;
                if (field.getMobs().isEmpty()) {
                    field.spawnMob(BossHelper.VON_BON, -135, 455, false);
                    field.setBossSpawned(true);
                    removeRootAbyssReactor(field, REACTOR);
                }
            }
            case "banbanChaos" -> {
                final int REACTOR = 1058017;
                if (field.getMobs().isEmpty()) {
                    field.spawnMob(BossHelper.CHAOS_VON_BON, -135, 455, false);
                    field.setBossSpawned(true);
                    removeRootAbyssReactor(field, REACTOR);
                }
            }
            case "bellumNormal" -> {
                final int REACTOR = 1058020;
                if (field.getMobs().isEmpty()) {
                    field.spawnMob(BossHelper.VELLUM, -200, 440, false);
                    field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossVellum, "竟敢無視我的警告？！我不會手下留情！"));
                    field.setBossSpawned(true);
                    removeRootAbyssReactor(field, REACTOR);
                }
            }
            case "bellumChaos" -> {
                final int REACTOR = 1058021;
                if (field.getMobs().isEmpty()) {
                    field.spawnMob(BossHelper.CHAOS_VELLUM, -200, 440, false);
                    field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossVellum, "竟敢無視我的警告？！我不會手下留情！"));
                    field.setBossSpawned(true);
                    removeRootAbyssReactor(field, REACTOR);
                }
            }
            case "queenNormal" -> {
                final int REACTOR = 1058018;
                if (field.getMobs().isEmpty()) {
                    field.spawnMob(BossHelper.QUEEN_1, 37, 135, false);
                    field.setBossSpawned(true);
                    removeRootAbyssReactor(field, REACTOR);
                }
            }
            case "queenChaos" -> {
                final int REACTOR = 1058019;
                if (field.getMobs().isEmpty()) {
                    field.spawnMob(BossHelper.CHAOS_QUEEN_1, 37, 135, false);
                    field.setBossSpawned(true);
                    removeRootAbyssReactor(field, REACTOR);
                }
            }
        }
    }

    private static void removeRootAbyssReactor(Field field, int reactorID) {
        field.removeReactorByTemplateId(reactorID);
    }
}
