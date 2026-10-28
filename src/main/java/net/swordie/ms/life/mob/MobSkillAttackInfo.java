package net.swordie.ms.life.mob;

import net.swordie.ms.util.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 3/19/2018.
 */
public class MobSkillAttackInfo {
    public byte actionAndDirMask;
    public byte action;
    public boolean flip;
    public int targetInfo;
    public short skillID;
    public short slv;
    public List<Position> multiTargetForBalls = new ArrayList<>();
    public List<Short> randTimeForAreaAttacks = new ArrayList<>();
    public UnkMobMovement unkMobMovement = null;

    public static class UnkMobMovement {
        public int index;
        public int unk1;
        public int unk2;
        public int unk3;
        public int unk4;
        public int unk5;
        public int unk6;
        public int unk7;
        public int unk8;
        public int unk9;
        public int unk10;
        public int unk11;

        public UnkMobMovement(int index, int unk1, int unk2, int unk3, int unk4, int unk5, int unk6, int unk7, int unk8, int unk9, int unk10, int unk11) {
            this.index = index;
            this.unk1 = unk1;
            this.unk2 = unk2;
            this.unk3 = unk3;
            this.unk4 = unk4;
            this.unk5 = unk5;
            this.unk6 = unk6;
            this.unk7 = unk7;
            this.unk8 = unk8;
            this.unk9 = unk9;
            this.unk10 = unk10;
            this.unk11 = unk11;
        }
    }
}
