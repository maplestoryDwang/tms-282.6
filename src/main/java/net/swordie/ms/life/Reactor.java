package net.swordie.ms.life;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.ReactorPool;
import net.swordie.ms.life.drop.DropInfo;
import net.swordie.ms.loaders.ReactorData;
import net.swordie.ms.loaders.containerclasses.ReactorInfo;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.boss.RootAbyss;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Foothold;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created on 4/21/2018.
 */
public class Reactor extends Life {

    private byte state;
    private String name = "";
    private int ownerID = 0;
    private int properEventIdx;
    private int reactorTime;
    private boolean phantomForest;
    private int hitCount;
    private int maxHitCount;
    private ScheduledFuture<?> resetHitCountTimer;

    public Reactor(int templateId) {
        super(templateId);
    }

    public byte getState() {
        return state;
    }

    public void setState(byte state) {
        this.state = state;
    }

    public void increaseState() {
        this.state++;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOwnerID() {
        return ownerID;
    }

    public void setOwnerID(int ownerID) {
        this.ownerID = ownerID;
    }

    public int getProperEventIdx() {
        return properEventIdx;
    }

    public void setProperEventIdx(int properEventIdx) {
        this.properEventIdx = properEventIdx;
    }

    public int getReactorTime() {
        return reactorTime;
    }

    public void setReactorTime(int reactorTime) {
        this.reactorTime = reactorTime;
    }

    public boolean isPhantomForest() {
        return phantomForest;
    }

    public void setPhantomForest(boolean phantomForest) {
        this.phantomForest = phantomForest;
    }

    public void init() {
        ReactorInfo ri = ReactorData.getReactorInfoByID(getTemplateId());
        setState((byte) 0);
        setName(ri.getViewName().equals("") ? ri.getName() : ri.getViewName());
        setPosition(getHomePosition());
        setMaxHitCount(ri.getMaxHitCount());
    }

    @Override
    public void broadcastSpawnPacket(Char onlyChar) {
        init();
        getField().broadcast(ReactorPool.reactorEnterField(this));
    }

    @Override
    public void broadcastLeavePacket() {
        Field field = getField();
        field.broadcast(ReactorPool.reactorLeaveField(this));
        if (field.isChannelField()) {
            Reactor reactor = (Reactor) deepCopy();
            reactor.init();
            //For 1-6 Event
            if (reactor.getTemplateId() == 200014 || reactor.getTemplateId() == 100014) {
                reactor.getTimer().addEvent(() -> field.spawnLife(reactor, null), 5, TimeUnit.MINUTES);
            } else if (reactor.getTemplateId() != 9702005) {
                reactor.getTimer().addEvent(() -> field.spawnLife(reactor, null), 5, TimeUnit.SECONDS);
            }
        }
    }

    public Life deepCopy() {
        Reactor copy = new Reactor(getTemplateId());
        copy.setLifeType(getLifeType());
        copy.setX(getX());
        copy.setY(getY());
        copy.setMobTime(getMobTime());
        copy.setFlip(isFlip());
        copy.setLimitedName(getLimitedName());
        copy.setPosition(getPosition().deepCopy());
        copy.setHomePosition(getPosition().deepCopy());
        return copy;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
        if (resetHitCountTimer == null) {
            resetHitCountTimer = getTimer().addEvent(() -> setHitCount(0), 10, TimeUnit.SECONDS);
        }
    }

    public void incHitCount() {
        setHitCount(getHitCount() + 1);
    }

    public int getMaxHitCount() {
        return maxHitCount;
    }

    public void setMaxHitCount(int maxHitCount) {
        this.maxHitCount = maxHitCount;
    }

    public void die(boolean drops) { //idk sorry
        getField().removeLife(this);
        if (drops) {
            dropItems();
        }
    }

    public void dropItems() {
        int fhID = getFh();
        if (fhID == 0) {
            Position pos = getPosition();
            pos.setY(pos.getY());
            Foothold fhBelow = getField().findFootHoldBelow(pos);
            if (fhBelow != null) {
                fhID = fhBelow.getId();
            }
        }
        Set<DropInfo> dropInfoSet = ReactorData.getReactorInfoByID(getTemplateId()).getDrops();
        getField().drop(dropInfoSet, getField().getFootholdById(fhID), getPosition(), null, 100, 100, false, new ArrayList<>());
    }

    public void startScript(Char chr, String action) {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (action.equals("banbanNormal")
                || action.equals("banbanChaos")
                || action.equals("bellumNormal")
                || action.equals("bellumChaos")
                || action.equals("queenNormal")
                || action.equals("queenChaos")) {
            RootAbyss.spawn(chr, action);
            return;
        }
        if (getTemplateId() >= 9108000 && getTemplateId() <= 9108005) {
            chr.eventOnFlower(getTemplateId());
            return;
        }
        if (getTemplateId() == 2618000) {
            if (chr.hasItem(4001132) && getState() <= 6) {
                chr.consumeItem(4001132, 1);
                increaseState();
                chr.getField().broadcast(ReactorPool.reactorChangeState(this, (short) 0, 0));
            }
            return;
        }
        if (sm.isActive(ScriptType.Reactor) && sm.getParentIDByScriptType(ScriptType.Reactor) == getTemplateId()) {
            try {
                sm.getInvocableByType(ScriptType.Reactor).invokeFunction("action", 0);
            } catch (ScriptException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            } catch (NoSuchMethodException e) {
                //DataPrinter.send(DataPrinter.SCRIPTS, String.format("Không thể tìm thấy script %s/%s.", "reactor", action), true);
            }
        } else {
            if (!action.equals("")) {
                sm.startScript(chr, getTemplateId(), getObjectId(), action, ScriptType.Reactor);
            } else {
                if (getLimitedName() != null) {
                    if (!getLimitedName().equals("")) {
                        sm.startScript(chr, getTemplateId(), getObjectId(), getLimitedName(), ScriptType.Reactor);
                    }
                }
            }
        }
    }
}
