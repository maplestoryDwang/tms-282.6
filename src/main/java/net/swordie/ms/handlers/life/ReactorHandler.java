package net.swordie.ms.handlers.life;


import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Reactor;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.ReactorData;
import net.swordie.ms.loaders.containerclasses.ReactorInfo;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

public class ReactorHandler {

    @Handler(op = InHeader.REACTOR_CLICK)
    public static void handleReactorClick(Char chr, InPacket inPacket) {
        int objectId = inPacket.decodeInt();
        byte type = 0;
        if (inPacket.getUnreadAmount() > 0) {
            inPacket.decodeInt();
            type = inPacket.decodeByte();
        }
        Life life = chr.getField().getLifeByObjectID(objectId);
        Reactor reactor;
        if (!(life instanceof Reactor)) {
            if (!chr.getField().getReactors().isEmpty()) {
                reactor = Util.getRandomFromCollection(chr.getField().getReactors());
            } else {
                System.out.println("[Hack] Could not find reactor with objID " + objectId);
                return;
            }
        } else {
            reactor = (Reactor) life;
        }
        if (reactor == null) {
            System.out.println("Could not find reactor with objID " + objectId);
            return;
        }
        int templateID = reactor.getTemplateId();
        ReactorInfo ri = ReactorData.getReactorInfoByID(templateID);
        String action = ri.getAction();
        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        if ((templateID == 1058016 || templateID == 1058017 // Vonbon
                || templateID == 1058018 || templateID == 1058019 // Crimson Queen
                || templateID == 1058020 || templateID == 1058021 // Vellum
        ) && (field.isBossSpawned() || !field.getMobs().isEmpty())) {
            return;
        } else {
            reactor.startScript(chr, action);
        }
    }

    @Handler(op = InHeader.REACTOR_RECT_IN_MOB)
    public static void handleReactorRectInMob(Char chr, InPacket inPacket) {
        int objectId = inPacket.decodeInt();
        int mobID = inPacket.decodeInt();
        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        Life life = field.getLifeByObjectID(objectId);
        if (!(life instanceof Reactor reactor)) {
            System.out.println("Could not find reactor with objID " + objectId);
            return;
        }

        Life life2 = field.getLifeByObjectID(mobID);
        if (!(life2 instanceof Mob mob)) {
            System.out.println("Could not find Mob with objID " + mobID);
            return;
        }
        int templateID = reactor.getTemplateId();
        ReactorInfo ri = ReactorData.getReactorInfoByID(templateID);
        String action = ri.getAction();
        if (action.equals("")) {
            action = templateID + "action";
        }
        if (templateID == 9239000) {
            mob.removeWithAnimation();
            if (chr.getDefenseTowerWave() != null && field.getId() == 993000100) {
                chr.getDefenseTowerWave().attacked(chr);
            } else if (field.getId() == GameConstants.NETT_PYRAMID_MAIN_MAP) {
                chr.getDefenseEvent().minusLife();
            }
        } else {
            reactor.startScript(chr, action);
        }
    }

    @Handler(op = InHeader.REACTOR_KEY)
    public static void handleReactorKey(Char chr, InPacket inPacket) {
        int objID = inPacket.decodeInt();
        int lifeID = inPacket.decodeInt();
        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        Life life = field.getLifeByObjectID(lifeID);
        if (life instanceof Mob mob) {
            mob.remove(false);
        }
        for (Char character : field.getChars()) {
            character.increaseGolluxStack();
        }
    }

    @Handler(op = InHeader.REACTOR_BY_SPACE)
    public static void handleReactorBySpace(Char chr, InPacket inPacket) {
        int objectId = inPacket.decodeInt();
        Life life = chr.getField().getLifeByObjectID(objectId);
        if (!(life instanceof Reactor reactor)) {
            System.out.println("Could not find reactor with objID " + objectId);
            return;
        }
        int templateID = reactor.getTemplateId();
        ReactorInfo ri = ReactorData.getReactorInfoByID(templateID);
        String action = ri.getAction();
        reactor.startScript(chr, action);
    }

    @Handler(op = InHeader.REACTOR_HIT)
    public static void handleReactorHit(Char chr, InPacket inPacket) {
        int objectId = inPacket.decodeInt();
        int charPosition = inPacket.decodeInt(); //2 = Turn Right, 3 = Turn Left ? Use for?
        short stance = inPacket.decodeShort();
        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        Life life = field.getLifeByObjectID(objectId);
        Reactor reactor;
        if (!(life instanceof Reactor)) {
            if (!field.getReactors().isEmpty()) {
                reactor = Util.getRandomFromCollection(chr.getField().getReactors());
            } else {
                System.out.println("[Hack] Could not find reactor with objID " + objectId);
                return;
            }
        } else {
            reactor = (Reactor) life;
        }
        if (reactor == null) {
            System.out.println("Could not find reactor with objID " + objectId);
            return;
        }
        int templateID = reactor.getTemplateId();
        if (templateID == 2401000) { // chaos
            reactor.incHitCount();
            if (reactor.getHitCount() == reactor.getMaxHitCount()) {
                field.removeReactorByID(objectId);
                field.spawnMob(8810130, 95, 260, false);
            }
        } else if (templateID == 2401100) { // normal
            reactor.incHitCount();
            if (reactor.getHitCount() == reactor.getMaxHitCount()) {
                field.removeReactorByID(objectId);
                field.spawnMob(8810026, 95, 260, false);
            }
        } else if (templateID == 2401300) { // easy
            reactor.incHitCount();
            if (reactor.getHitCount() >= reactor.getMaxHitCount()) {
                field.removeReactorByID(objectId);
                field.spawnMob(8810215, 95, 260, false);
            }
        } else {
            ReactorInfo ri = ReactorData.getReactorInfoByID(templateID);
            String action = ri.getAction();
            reactor.startScript(chr, action);
        }
        chr.getScriptManager().changeReactorStateByObjectID(objectId, reactor.getState(), stance, (byte) 0);
    }
}
