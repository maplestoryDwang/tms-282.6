package net.swordie.ms.client.character.items;

public class EquipFlame {
    private short STR;
    private short DEX;
    private short INT_;
    private short LUK;
    private short PAD;
    private short MAD;
    private short PDD;
    private short HP;
    private short MP;
    private short Speed;
    private short Jump;
    private short AllStatR;
    private short BossDamageR;
    private short Damage;
    private byte ReduceReqLevel;

    public EquipFlame() {
        this.STR = 0;
        this.DEX = 0;
        this.INT_ = 0;
        this.LUK = 0;
        this.PAD = 0;
        this.MAD = 0;
        this.PDD = 0;
        this.HP = 0;
        this.MP = 0;
        this.Speed = 0;
        this.Jump = 0;
        this.AllStatR = 0;
        this.BossDamageR = 0;
        this.Damage = 0;
        this.ReduceReqLevel = (byte) 0;
    }

    public EquipFlame(short flame_str, short flame_dex, short flame_int, short flame_luk, short flame_pad, short flame_mad, short flame_pdd, short flame_hp, short flame_mp, short flame_speed, short flame_jump, short flame_allStatR, short flame_bossDamageR, short flame_damageR, short flame_reduceReqLevel) {
        this.STR = flame_str;
        this.DEX = flame_dex;
        this.INT_ = flame_int;
        this.LUK = flame_luk;
        this.PAD = flame_pad;
        this.MAD = flame_mad;
        this.PDD = flame_pdd;
        this.HP = flame_hp;
        this.MP = flame_mp;
        this.Speed = flame_speed;
        this.Jump = flame_jump;
        this.AllStatR = flame_allStatR;
        this.BossDamageR = flame_bossDamageR;
        this.Damage = flame_damageR;
        this.ReduceReqLevel = (byte) flame_reduceReqLevel;
    }

    public EquipFlame deepCopy() {
        EquipFlame ret = new EquipFlame();
        ret.STR = getSTR();
        ret.DEX = getDEX();
        ret.INT_ = getINT();
        ret.LUK = getLUK();
        ret.PAD = getPAD();
        ret.MAD = getMAD();
        ret.PDD = getPDD();
        ret.HP = getHP();
        ret.MP = getMP();
        ret.Speed = getSpeed();
        ret.Jump = getJump();
        ret.AllStatR = getAllStatR();
        ret.BossDamageR = getBossDamageR();
        ret.Damage = getDamage();
        ret.ReduceReqLevel = getReduceReqLevel();
        return ret;
    }

    public void reset() {
        this.STR = 0;
        this.DEX = 0;
        this.INT_ = 0;
        this.LUK = 0;
        this.PAD = 0;
        this.MAD = 0;
        this.PDD = 0;
        this.HP = 0;
        this.MP = 0;
        this.Speed = 0;
        this.Jump = 0;
        this.AllStatR = 0;
        this.BossDamageR = 0;
        this.Damage = 0;
        this.ReduceReqLevel = 0;
    }

    public short getSTR() {
        return STR;
    }

    public void setSTR(int STR) {
        this.STR = (short) STR;
    }

    public short getDEX() {
        return DEX;
    }

    public void setDEX(int DEX) {
        this.DEX = (short) DEX;
    }

    public short getINT() {
        return INT_;
    }

    public void setINT(int INT) {
        this.INT_ = (short) INT;
    }

    public short getLUK() {
        return LUK;
    }

    public void setLUK(int LUK) {
        this.LUK = (short) LUK;
    }

    public short getPAD() {
        return PAD;
    }

    public void setPAD(int PAD) {
        this.PAD = (short) PAD;
    }

    public short getMAD() {
        return MAD;
    }

    public void setMAD(int MAD) {
        this.MAD = (short) MAD;
    }

    public short getPDD() {
        return PDD;
    }

    public void setPDD(int PDD) {
        this.PDD = (short) PDD;
    }

    public short getHP() {
        return HP;
    }

    public void setHP(int HP) {
        this.HP = (short) HP;
    }

    public short getMP() {
        return MP;
    }

    public void setMP(int MP) {
        this.MP = (short) MP;
    }

    public short getSpeed() {
        return Speed;
    }

    public void setSpeed(int Speed) {
        this.Speed = (short) Speed;
    }

    public short getJump() {
        return Jump;
    }

    public void setJump(int Jump) {
        this.Jump = (short) Jump;
    }

    public short getAllStatR() {
        return AllStatR;
    }

    public void setAllStatR(int AllStatR) {
        this.AllStatR = (short) AllStatR;
    }

    public short getBossDamageR() {
        return BossDamageR;
    }

    public void setBossDamageR(int BossDamageR) {
        this.BossDamageR = (short) BossDamageR;
    }

    public short getDamage() {
        return Damage;
    }

    public void setDamage(int Damage) {
        this.Damage = (short) Damage;
    }

    public byte getReduceReqLevel() {
        return ReduceReqLevel;
    }

    public void setReduceReqLevel(int ReduceReqLevel) {
        this.ReduceReqLevel = (byte) ReduceReqLevel;
    }
}
