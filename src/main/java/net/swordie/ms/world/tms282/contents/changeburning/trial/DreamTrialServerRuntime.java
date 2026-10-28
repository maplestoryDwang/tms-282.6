package net.swordie.ms.world.tms282.contents.changeburning.trial;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Instance;
import net.swordie.ms.world.field.fieldeffect.FieldEffect;
import net.swordie.ms.world.tms282.bossrun.BossRunMaps;
import net.swordie.ms.world.tms282.bossrun.BossRunService;
import net.swordie.ms.world.tms282.contents.changeburning.ChangeBurning;
import net.swordie.ms.world.tms282.contents.changeburning.dream.DreamService;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

/** {@link DreamTrialService.Runtime} on the live server (instances, mobs, ChangeBurning, 夢幻的裝備, boss-run pqlog). */
final class DreamTrialServerRuntime implements DreamTrialService.Runtime {

    @Override
    public boolean tms282() {
        try {
            return Protocol.active().version() == ProtocolVersion.TMS_282_3;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    @Override
    public boolean eventActive() {
        return ChangeBurning.get().eventActive();
    }

    @Override
    public boolean designated(Char chr) {
        return ChangeBurning.get().isDesignated(chr);
    }

    @Override
    public int lucidLevel(Char chr) {
        return ChangeBurning.lucidLevel(chr);
    }

    @Override
    public String soloBlock(Char chr) {
        Party p = chr.getParty();
        if (p != null && p.getMembers().size() > 1) return "夢幻的試煉只能單人挑戰，請先退出隊伍。";
        if (chr.getInstance() != null) return "請先離開目前的副本再挑戰。";
        return null;
    }

    @Override
    public boolean fieldExists(int fieldId) {
        return BossRunMaps.exists(fieldId);
    }

    @Override
    public int used(Char chr, int logKey) {
        BossRunService s = BossRunService.get();
        return s.store().pqCount(chr.getId(), logKey, s.now().toLocalDate());
    }

    @Override
    public void count(Char chr, int logKey) {
        BossRunService s = BossRunService.get();
        s.store().pqAdd(chr.getId(), logKey, s.now().toLocalDate(), 1);
    }

    @Override
    public void reset(Char chr, int logKey) {
        BossRunService.get().store().pqReset(chr.getId(), logKey);
    }

    @Override
    public Object enter(Char chr, int fieldId, int exitField, int timeLimitSeconds, int revives) {
        chr.getScriptManager().warpInstanceIn(chr, fieldId, false); // solo instance (partyAllowed = false)
        Instance inst = chr.getInstance();
        if (inst == null) return null;
        inst.setForcedReturn(exitField);
        inst.setForcedReturnPortalId(-1);
        inst.setTimeout(timeLimitSeconds, true); // CLOCK (LP030F) = the proven boss timer
        chr.setDeathCount(revives);
        chr.showDeathCount(revives);
        return inst;
    }

    @Override
    public boolean inside(Char chr, Object instance) {
        return instance instanceof Instance inst && chr.getInstance() == inst && Server.get().getInstances().contains(inst);
    }

    @Override
    public int fieldId(Char chr) {
        return chr.getField() == null ? 0 : chr.getField().getId();
    }

    @Override
    public void warp(Char chr, int fieldId) {
        chr.warp(fieldId);
    }

    @Override
    public void leave(Char chr, int fieldId) {
        chr.setDeathCount(-1);
        chr.getScriptManager().warpInstanceOut(chr, fieldId);
    }

    @Override
    public boolean forceTransform(Char chr) {
        return ChangeBurning.get().forceTransform(chr);
    }

    @Override
    public boolean endForced(Char chr) {
        return ChangeBurning.get().endForcedTransform(chr);
    }

    @Override
    public boolean forced(Char chr) {
        return ChangeBurning.get().transformReason(chr) == ChangeBurning.FORCED_REASON;
    }

    @Override
    public void schedule(Runnable r, long delayMs) {
        Server.get().getEventTimer().addEvent(() -> {
            try {
                r.run();
            } catch (RuntimeException | LinkageError e) {
                System.out.println("[DreamTrial] scheduled step failed: " + e);
                e.printStackTrace();
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private static Field field(Object instance, int fieldId) {
        return instance instanceof Instance inst ? inst.getField(fieldId) : null;
    }

    @Override
    public void prepare(Object instance, int fieldId) {
        Field f = field(instance, fieldId);
        if (f == null) return;
        f.setDropsDisabled(true); // 官方：試煉不掉落道具
        f.removeMobs();
    }

    @Override
    public boolean spawn(Object instance, int fieldId, DreamTrialConfig.MobSpawn spawn, long hp, boolean hpTag, IntConsumer onDeath) {
        Field f = field(instance, fieldId);
        if (f == null) return false;
        Mob mob = f.spawnMob(spawn.id(), spawn.x(), spawn.y(), false, hp);
        if (mob == null) return false;
        mob.addObserver(new Observer() {
            private boolean done;

            @Override
            public synchronized void update(Observable o, Object arg) {
                if (done || mob.getHp() > 0) return;
                done = true;
                o.deleteObserver(this);
                onDeath.accept(mob.getTemplateId());
            }
        });
        if (hpTag) {
            f.broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(mob))); // FIELD_EFFECT MobHPTag (proven)
        }
        return true;
    }

    @Override
    public void removeMobs(Object instance, int fieldId) {
        Field f = field(instance, fieldId);
        if (f != null) f.removeMobs();
    }

    @Override
    public void lucid(Object instance, int fieldId, int phase, int mode, boolean on) {
        Field f = field(instance, fieldId);
        if (f == null) return;
        if (on) {
            net.swordie.ms.world.boss.Lucid.startButterflies(f, phase, mode);
        } else {
            net.swordie.ms.world.boss.Lucid.stopButterflies(f);
        }
    }

    @Override
    public boolean questDone(Char chr, int questId) {
        return chr.hasQuestCompleted(questId);
    }

    @Override
    public void completeQuest(Char chr, int questId) {
        chr.completeQuest(questId);
    }

    @Override
    public String addDust(Char chr, long dust) {
        DreamService.Result r = DreamService.get().addDust(chr, dust);
        return switch (r) {
            case OK -> null;
            case OFF -> "夢幻的裝備沒有開放";
            default -> "儲存失敗，請回報 GM";
        };
    }

    @Override
    public boolean canHold(Char chr, List<DreamTrialConfig.Item> items) {
        for (DreamTrialConfig.Item i : items) {
            if (!chr.canHold(i.id(), i.quantity())) return false;
        }
        return true;
    }

    @Override
    public void give(Char chr, List<DreamTrialConfig.Item> items) {
        net.swordie.ms.client.character.tms282.EconomyLocks.run(chr, () -> {
            for (DreamTrialConfig.Item i : items) chr.addItemToInventory(i.id(), i.quantity());
            net.swordie.ms.lifecycle.OnlineCharacterSaver.save(chr);
        });
    }

    @Override
    public void message(Char chr, String text) {
        chr.chatMessage(ChatType.SystemNotice, text);
    }

    @Override
    public void registerMechanics(DreamTrialConfig config, boolean on) {
        if (on) {
            net.swordie.ms.world.boss.Lucid.setExtraFields(config.lucidFields(1), config.lucidFields(2));
            net.swordie.ms.constants.BossConstants.setExtraVellum(config.vellumMobs());
        } else {
            net.swordie.ms.world.boss.Lucid.setExtraFields(java.util.Set.of(), java.util.Set.of());
            net.swordie.ms.constants.BossConstants.setExtraVellum(java.util.Set.of());
        }
    }
}
