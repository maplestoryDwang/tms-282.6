package net.swordie.ms.life.npc;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.NpcPool;
import net.swordie.ms.life.Life;
import net.swordie.ms.world.field.Field;

import java.util.HashMap;
import java.util.Map;

import static net.swordie.ms.ServerConstants.version;

public class Npc extends Life {
    private boolean enabled = true;
    private int presentItemID;
    private byte presentItemState;
    private int presentItemTime = -1;
    private int noticeBoardType;
    private int noticeBoardValue;
    private int alpha; // if hideToLocalUser is true
    private String localRepeatEffect;
    private ScreenInfo screenInfo;
    private Map<Integer, String> scripts = new HashMap<>();
    private boolean move;
    private boolean isShop = false;
    private int trunkGet;
    private int trunkPut;


    public Npc(int templateId) {
        super(templateId);
    }

    public void encode(OutPacket outPacket) {
        // CNpc::Init
        outPacket.encodePosition(getPosition());
        outPacket.encodeInt(-1); // v263
        outPacket.encodeInt(-1); // v263
        outPacket.encodeByte(isMove());
        outPacket.encodeByte(!isFlip());
        outPacket.encodeShort(getFh());
        outPacket.encodeShort(getRx0()); // rgHorz.low
        outPacket.encodeShort(getRx1()); // rgHorz.high
        outPacket.encodeShort(getPosition().getY());
        outPacket.encodeShort(getPosition().getY());
        outPacket.encodeByte(isEnabled());
        outPacket.encodeInt(0); // v263
        outPacket.encodeInt(getPresentItemID());
        outPacket.encodeByte(getPresentItemState());
        outPacket.encodeInt(getPresentItemTime()); // -1
        outPacket.encodeByte(0);
        outPacket.encodeLong(368573625000000L); // v263
        outPacket.encodeInt(getNoticeBoardType());
        outPacket.encodeInt(getAlpha());
        outPacket.encodeInt(0); // v263
        outPacket.encodeString(getLocalRepeatEffect());
        outPacket.encodeByte(0); // true = avatarLook
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPresentItemID() {
        return presentItemID;
    }

    public void setPresentItemID(int presentItemID) {
        this.presentItemID = presentItemID;
    }

    public byte getPresentItemState() {
        return presentItemState;
    }

    public void setPresentItemState(byte presentItemState) {
        this.presentItemState = presentItemState;
    }

    public int getPresentItemTime() {
        return presentItemTime;
    }

    public void setPresentItemTime(int presentItemTime) {
        this.presentItemTime = presentItemTime;
    }

    public int getNoticeBoardType() {
        return noticeBoardType;
    }

    public void setNoticeBoardType(int noticeBoardType) {
        this.noticeBoardType = noticeBoardType;
    }

    public int getNoticeBoardValue() {
        return noticeBoardValue;
    }

    public void setNoticeBoardValue(int noticeBoardValue) {
        this.noticeBoardValue = noticeBoardValue;
    }

    public int getAlpha() {
        return alpha;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    public String getLocalRepeatEffect() {
        return localRepeatEffect;
    }

    public void setLocalRepeatEffect(String localRepeatEffect) {
        this.localRepeatEffect = localRepeatEffect;
    }

    public ScreenInfo getScreenInfo() {
        return screenInfo;
    }

    public void setScreenInfo(ScreenInfo screenInfo) {
        this.screenInfo = screenInfo;
    }

    @Override
    public Npc deepCopy() {
        Npc copy = new Npc(getTemplateId());
        copy.setLifeType(getLifeType());
        copy.setControllerID(getControllerID());
        copy.setX(getX());
        copy.setY(getY());
        copy.setMobTime(getMobTime());
        copy.setFlip(isFlip());
        copy.setHide(isHide());
        copy.setFh(getFh());
        copy.setCy(getCy());
        copy.setRx0(getRx0());
        copy.setRx1(getRx1());
        copy.setLimitedName(getLimitedName());
        copy.setUseDay(isUseDay());
        copy.setUseNight(isUseNight());
        copy.setHold(isHold());
        copy.setNoFoothold(isNoFoothold());
        copy.setDummy(isDummy());
        copy.setSpine(isSpine());
        copy.setMobTimeOnDie(isMobTimeOnDie());
        copy.setRegenStart(getRegenStart());
        copy.setMove(isMove());
        copy.setShop(isShop());
        copy.setMobAliveReq(getMobAliveReq());
        copy.setTrunkGet(getTrunkGet());
        copy.setTrunkPut(getTrunkPut());
        copy.getScripts().putAll(getScripts());
        return copy;
    }

    public Map<Integer, String> getScripts() {
        return scripts;
    }

    @Override
    public void broadcastSpawnPacket(Char onlyChar) {
        Field field = getField();
        field.broadcast(NpcPool.npcEnterField(this));
        for (Char chr : field.getChars()) {
            chr.write(NpcPool.npcChangeController(this, chr.getId() == getControllerID()));
        }
    }

    @Override
    public void broadcastLeavePacket() {
        Field field = getField();
        field.broadcast(NpcPool.npcLeaveField(this));
        for (Char chr : field.getChars()) {
            chr.write(NpcPool.npcChangeController(this, chr.getId() == getControllerID(), true));
        }
    }

    @Override
    public void notifyControllerChange() {
        for (Char chr : getField().getChars()) {
            chr.write(NpcPool.npcChangeController(this, chr.getId() == getControllerID()));
        }
    }

    public boolean isMove() {
        return move;
    }

    public void setMove(boolean move) {
        this.move = move;
    }

    @Override
    public String toString() {
        return super.toString() + ", Move: " + isMove();
    }

    public int getTrunkGet() {
        return trunkGet;
    }

    public void setTrunkGet(int trunkGet) {
        this.trunkGet = trunkGet;
    }

    public int getTrunkPut() {
        return trunkPut;
    }

    public void setTrunkPut(int trunkPut) {
        this.trunkPut = trunkPut;
    }

    public boolean isShop() {
        return isShop;
    }

    public void setShop(boolean shop) {
        isShop = shop;
    }
}
