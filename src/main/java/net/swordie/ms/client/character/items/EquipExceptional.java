package net.swordie.ms.client.character.items;

public class EquipExceptional {
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

    public EquipExceptional() {
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

    public EquipExceptional(short except_str, short except_dex, short except_int, short except_luk, short except_pad, short except_mad, short except_pdd, short except_hp, short except_mp, short except_speed, short except_jump, short except_allStatR, short except_bossDamageR, short except_damageR, short except_reduceReqLevel) {
        this.STR = except_str;
        this.DEX = except_dex;
        this.INT_ = except_int;
        this.LUK = except_luk;
        this.PAD = except_pad;
        this.MAD = except_mad;
        this.PDD = except_pdd;
        this.HP = except_hp;
        this.MP = except_mp;
        this.Speed = except_speed;
        this.Jump = except_jump;
        this.AllStatR = except_allStatR;
        this.BossDamageR = except_bossDamageR;
        this.Damage = except_damageR;
        this.ReduceReqLevel = (byte) except_reduceReqLevel;
    }

    public EquipExceptional deepCopy() {
        EquipExceptional ret = new EquipExceptional();
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
