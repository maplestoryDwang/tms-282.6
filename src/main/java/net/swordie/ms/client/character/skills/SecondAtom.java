package net.swordie.ms.client.character.skills;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.util.Position;

import java.util.ArrayList;
import java.util.List;

public class SecondAtom {
    private int charId;
    private int objectID;
    private int targetID;
    private int rotate;
    private int skillId;
    private int skilllv;
    private int dataIndex;
    private int key;
    private int customrotate;
    private int attackableCount;
    private int createDelay;
    private int enableDelay;
    private long start;
    private int expire;
    private int firstAngleRange;
    private int firstAngleStart;
    private int collisionCheck;
    private int unk = -1;
    private boolean localOnly;
    private boolean inAttackMode;
    private boolean isSpecial;
    private Position position;
    private Int2IntMap customs;

    public SecondAtom() {}

    public SecondAtom(SkillInfo.SecondAtomInfo sai, int obj, int charId, int mobObjectID, int key, int skillId, Position pos, long start) {
        this.setObjectID(obj);
        this.setCharId(charId);
        this.setTargetID(mobObjectID);
        this.setKey(key);
        this.setDataIndex(sai.getDataIndex());
        this.setSkillId(skillId);
        this.setCreateDelay(sai.getCreateDelay());
        this.setEnableDelay(sai.getEnableDelay());
        this.setRotate(sai.getRotate());
        this.setFirstAngleRange(sai.getFirstAngleRange());
        this.setFirstAngleStart(sai.getFirstAngleStart());
        this.setExpire(sai.getExpire());
        this.setAttackableCount(sai.getAttackableCount() == 0 ? 1 : sai.getAttackableCount());
        this.setPosition(pos);
        this.setLocalOnly(sai.getLocalOnly() != 0);
        this.setCustoms(new Int2IntOpenHashMap());
        this.setStart(start);
    }

    public SecondAtom(int objectID, int charId, int targetID, int rotate, int skillId, int skilllv,
                      Position position, int dataIndex, int key, int customrotate, int attackableCount, int createDelay,
                      int enableDelay, long start, int expire, boolean specSummon, Int2IntOpenHashMap customs) {
        this.objectID = objectID;
        this.charId = charId;
        this.targetID = targetID;
        this.rotate = rotate;
        this.skillId = skillId;
        this.skilllv = skilllv;
        this.position = position;
        this.dataIndex = dataIndex;
        this.key = key;
        this.customrotate = customrotate;
        this.attackableCount = attackableCount;
        this.createDelay = createDelay;
        this.enableDelay = enableDelay;
        this.start = start;
        this.expire = expire;
        this.isSpecial = specSummon;
        this.customs = customs;
    }

    public int getCharId() {
        return charId;
    }

    public void setCharId(int charId) {
        this.charId = charId;
    }

    public int getObjectID() {
        return objectID;
    }

    public void setObjectID(int oid) {
        this.objectID = oid;
    }

    public int getRotate() {
        return rotate;
    }

    public void setRotate(int rotate) {
        this.rotate = rotate;
    }

    public int getSkillId() {
        return skillId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
    }

    public int getSkilllv() {
        return skilllv;
    }

    public void setSkilllv(int lv) {
        this.skilllv = lv;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public Position getPosition() {
        return position;
    }

    public boolean isSpecialAtom() {
        return isSpecial;
    }

    public void setSpecialAtom(boolean enable) {
        this.isSpecial = enable;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public int getDataIndex() {
        return dataIndex;
    }

    public void setTargetID(int tid) {
        this.targetID = tid;
    }

    public int getTargetID() {
        return targetID;
    }

    public void setDataIndex(int dix) {
        this.dataIndex = dix;
    }

    public int getCustomRotate() {
        return customrotate;
    }

    public void setCustomrotate(int customrotate) {
        this.customrotate = customrotate;
    }

    public void setAttackableCount(int max) {
        this.attackableCount = max;
    }

    public int getAttackableCount() {
        return attackableCount;
    }

    public void setCreateDelay(int cdl) {
        this.createDelay = cdl;
    }

    public int getCreateDelay() {
        return createDelay;
    }

    public void setEnableDelay(int edl) {
        this.enableDelay = edl;
    }

    public int getEnableDelay() {
        return enableDelay;
    }

    public void setExpire(int expire) {
        this.expire = expire;
    }

    public int getExpire() {
        return expire;
    }

    public Int2IntMap getCustoms() {
        return customs;
    }

    public void setCustoms(Int2IntMap cus) {
        this.customs = cus;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public int getFirstAngleRange() {
        return firstAngleRange;
    }

    public void setFirstAngleRange(int firstAngleRange) {
        this.firstAngleRange = firstAngleRange;
    }

    public int getFirstAngleStart() {
        return firstAngleStart;
    }

    public void setFirstAngleStart(int firstAngleStart) {
        this.firstAngleStart = firstAngleStart;
    }

    public int getCollisionCheck() {
        return collisionCheck;
    }

    public void setCollisionCheck(int collisionCheck) {
        this.collisionCheck = collisionCheck;
    }

    public boolean isLocalOnly() {
        return localOnly;
    }

    public void setLocalOnly(boolean localOnly) {
        this.localOnly = localOnly;
    }

    public boolean isInAttackMode() {
        return inAttackMode;
    }

    public void setInAttackMode(boolean inAttackMode) {
        this.inAttackMode = inAttackMode;
    }

    public int getUnk() {
        return unk;
    }

    public void setUnk(int unk) {
        this.unk = unk;
    }
}
