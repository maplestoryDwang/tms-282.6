package net.swordie.ms.client.character.skills;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.world.field.Field;

public class SpecialStack {

    public Int2ObjectMap<SpecialStackInfo> targets;
    public int maxStacks;
    public int duration;
    public int originalSkillID;
    public int skillID;
    public int skillID2;
    public int unk;

    public SpecialStack(int originalSkillID, int skillID, int skillID2, int duration, int maxStacks) {
        this.originalSkillID = originalSkillID;
        this.skillID = skillID;
        this.skillID2 = skillID2;
        this.duration = duration;
        this.maxStacks = maxStacks;
        this.targets = new Int2ObjectOpenHashMap<>();
    }

    public static class SpecialStackInfo {
        public int objectId;
        public int stack;
        public int unk;
        public long start;
        public long end;

        public SpecialStackInfo(long start, int duration) {
            this.start = start;
            this.end = start + duration;
        }
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(targets.size());
        for (var entry : targets.int2ObjectEntrySet()) {
            outPacket.encodeInt(entry.getIntKey());
        }
        outPacket.encodeInt(maxStacks);
        outPacket.encodeInt(duration);
        outPacket.encodeInt(originalSkillID);
        outPacket.encodeInt(skillID);
        outPacket.encodeInt(skillID2);
        outPacket.encodeInt(0);
        outPacket.encodeInt(targets.size());
        for (var entry : targets.int2ObjectEntrySet()) {
            outPacket.encodeInt(entry.getIntKey());
            outPacket.encodeInt(entry.getValue().stack);
            outPacket.encodeInt(entry.getValue().unk);
            outPacket.encodeInt(entry.getValue().end - entry.getValue().start); // time left
        }
    }

    public static boolean prune(Char chr, SpecialStack stack, long now) {
        if (stack == null || stack.targets == null || stack.targets.isEmpty()) {
            return false;
        }

        Field field = chr.getField();
        boolean changed = false;

        var it = stack.targets.int2ObjectEntrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            int objectId = entry.getIntKey();
            SpecialStack.SpecialStackInfo info = entry.getValue();

            var life = field.getLifeByObjectID(objectId);
            if (!(life instanceof Mob mob) || mob.getHp() <= 0) {
                it.remove();
                changed = true;
                continue;
            }

            if (now >= info.end) { // timeleft <= 0
                it.remove();
                changed = true;
            }
        }
        return changed;
    }
}
