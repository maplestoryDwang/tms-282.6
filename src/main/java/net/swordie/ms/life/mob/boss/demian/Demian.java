package net.swordie.ms.life.mob.boss.demian;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.DemianFieldPacket;
import net.swordie.ms.constants.BossConstants;
import net.swordie.ms.life.mob.boss.demian.stigma.DemianStigma;
import net.swordie.ms.life.mob.boss.demian.stigma.DemianStigmaIncinerateObject;
import net.swordie.ms.world.field.Field;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Demian {

    public static ScheduledFuture<?> stigmaIncinerateObjectTimer(Field field) {
        Runnable task = () -> {
            DemianStigmaIncinerateObject o = new DemianStigmaIncinerateObject(-1);
            field.spawnLifeForTime(o, BossConstants.DEMIAN_STIGMA_INCINERATE_OBJECT_DURATION_TIME);
        };
        return field.getTimer().addFixedRateEvent(task, 5000, BossConstants.DEMIAN_STIGMA_INCINERATE_OBJECT_RESPAWN_TIME, TimeUnit.MILLISECONDS, false);
    }

    public static ScheduledFuture<?> increaseStigmaPassiveTimer(Char chr) {
        Runnable task = () -> {
            if (DemianStigma.checkStigma(chr)) {
                DemianStigma.incStigma(chr);
                chr.getParty().broadcast(DemianFieldPacket.stigmaRemainTime(BossConstants.DEMIAN_PASSIVE_STIGMA_TIME));
                chr.getTimer().addEvent(() -> chr.getParty().getInstance().setStigmaChar(null), 5, TimeUnit.SECONDS);
            }
        };
        return chr.getTimer().addFixedRateEvent(task, 2000, BossConstants.DEMIAN_PASSIVE_STIGMA_TIME, TimeUnit.MILLISECONDS, false);
    }
}
