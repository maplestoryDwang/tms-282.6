package net.swordie.ms.client.character.skills;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.util.Position;

import java.util.LinkedList;
import java.util.List;

public class ExtraSkill {
    public int SkillID, TriggerSkillID = 0, FaceLeft = -1, Delay = 0, Value = 0, TargetOID = 0;
    public Position Position;
    public List<Integer> MobOIDs = new LinkedList<>();
    public List<Integer> UnkList = new LinkedList<>();
    public List<Integer> UnkList1 = new LinkedList<>();
    public List<Integer> UnkList2 = new LinkedList<>();

    public ExtraSkill(int skillId, Position pos) {
        this.SkillID = skillId;
        this.Position = pos;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(TriggerSkillID);
        outPacket.encodeInt(SkillID);
        outPacket.encodePositionInt(Position);
        outPacket.encodeShort(FaceLeft);
        outPacket.encodeInt(Delay);
        outPacket.encodeInt(Value);
        outPacket.encodeInt(-1);
        outPacket.encodeInt(MobOIDs.size());
        for (int oid : MobOIDs) {
            outPacket.encodeInt(oid);
        }
        outPacket.encodeInt(UnkList.size());
        for (int un : UnkList) {
            outPacket.encodeInt(un);
        }
        outPacket.encodeInt(TargetOID);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(UnkList1.size());
        for (int un : UnkList1) {
            outPacket.encodeInt(un);
        }
        outPacket.encodeInt(UnkList2.size());
        for (int un : UnkList2) {
            outPacket.encodeInt(un);
        }
    }

    public int getDelay() {
        return Delay;
    }
}
