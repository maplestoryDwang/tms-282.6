package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.skill.MobSkillID;
import net.swordie.ms.life.mob.skill.SpiderWeb;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.container.Triple;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.field.Field;

import java.util.List;
import java.util.Set;

public class WillPacket {

    public static OutPacket setMoonGauge(int max, int min) {
        OutPacket outPacket = new OutPacket(OutHeader.WILL_SET_MOONGAUGE);

        outPacket.encodeInt(max);
        outPacket.encodeInt(min);

        return outPacket;
    }

    public static OutPacket addMoonGauge(int inc) {
        OutPacket outPacket = new OutPacket(OutHeader.WILL_MOONGAUGE);

        outPacket.encodeInt(inc);

        return outPacket;
    }

    public static OutPacket cooldownMoonGauge(int length) {
        // 282: LP086E (Will base field 3A346F0 -> 4BAEAA0 cooldown UI). With WILL_MOONGAUGE the client took the
        // cooldown milliseconds (5000/7000) as the gauge value (LP086D). 265 enum already has WILL_COOLTIME_MOONGAUGE(2105).
        // bossmech2-0930 (research/tms2823/boss-mech-0930/patches/WillPacket-cooldown.diff)
        OutPacket outPacket = new OutPacket(OutHeader.WILL_COOLTIME_MOONGAUGE);

        outPacket.encodeInt(length);

        return outPacket;
    }

    public static OutPacket createBulletEyes(int... args) {
        OutPacket outPacket = new OutPacket(OutHeader.WILL_CREATE_BULLETEYE);

        outPacket.encodeInt(args[0]);
        outPacket.encodeInt(args[1]);
        outPacket.encodeInt(args[2]);
        outPacket.encodeInt(args[3]);
        if (args[0] == 1) {
            outPacket.encodeInt(1800);
            outPacket.encodeInt(5);
            outPacket.encodeByte(true);
            outPacket.encodeInt(args[4]);
            outPacket.encodeInt(args[5]);
            outPacket.encodeInt(args[6]);
            outPacket.encodeInt(args[7]);
        }

        return outPacket;
    }

    public static OutPacket setHp(List<Integer> counts, Field field, int mobId1, int mobId2, int mobId3) {
        OutPacket outPacket = new OutPacket(OutHeader.WILL_SET_HP);

        outPacket.encodeInt(counts.size());
        for (int i : counts) {
            outPacket.encodeInt(i);
        }
        Mob life1 = (Mob) field.getLifeByTemplateId(mobId1);
        Mob life2 = (Mob) field.getLifeByTemplateId(mobId2);
        Mob life3 = (Mob) field.getLifeByTemplateId(mobId3);
        outPacket.encodeByte((life1 != null));
        if (life1 != null) {
            outPacket.encodeInt(life1.getObjectId());
            outPacket.encodeLong(life1.getHp());
            outPacket.encodeLong((long) (life1.getMaxHp() * life1.getBonusHp()));
        }
        outPacket.encodeByte((life2 != null));
        if (life2 != null) {
            outPacket.encodeInt(life2.getObjectId());
            outPacket.encodeLong(life2.getHp());
            outPacket.encodeLong((long) (life2.getMaxHp() * life2.getBonusHp()));
        }
        outPacket.encodeByte((life3 != null));
        if (life3 != null) {
            outPacket.encodeInt(life3.getObjectId());
            outPacket.encodeLong(life3.getHp());
            outPacket.encodeLong((long) (life3.getMaxHp() * life3.getBonusHp()));
        }

        return outPacket;
    }

    public static OutPacket setHp(List<Integer> counts) {
        OutPacket outPacket = new OutPacket(OutHeader.WILL_SET_HP2);

        outPacket.encodeInt(counts.size());
        for (int i : counts) {
            outPacket.encodeInt(i);
        }

        return outPacket;
    }

    public static OutPacket spiderAttack(int objectID, int slv, int type, List<Triple<Integer, Integer, Integer>> values) {
        OutPacket outPacket = new OutPacket(OutHeader.WILL_SPIDER_ATTACK);

        outPacket.encodeInt(objectID);
        outPacket.encodeInt(MobSkillID.Will.getVal());
        outPacket.encodeInt(slv);
        switch (slv) {
            case 1, 2, 3, 14 -> {
                if (slv == 14) {
                    outPacket.encodeInt(type);
                }
                outPacket.encodeInt(slv == 14 ? 9 : 4);
                outPacket.encodeInt(1200);
                outPacket.encodeInt(slv == 14 ? 5000 : 9000);
                outPacket.encodeInt(slv == 14 && type == 2 ? -60 : -40);
                outPacket.encodeInt(-600);
                outPacket.encodeInt(slv == 14 && type == 2 ? 60 : 40);
                outPacket.encodeInt(10);
                outPacket.encodeInt(values.size());
                for (Triple<Integer, Integer, Integer> value : values) {
                    outPacket.encodeInt(value.getLeft());
                    outPacket.encodeInt(value.getMiddle());
                    outPacket.encodeInt(value.getRight());
                    outPacket.encodeInt(0);
                }
            }
            case 4 -> {
                outPacket.encodeInt(type);
                outPacket.encodeByte((type != 0));
            }
            case 5 -> {
                outPacket.encodeInt(2);
                if (type == 0) {
                    outPacket.encodeByte(0);
                    outPacket.encodeInt(-690);
                    outPacket.encodeInt(-455);
                    outPacket.encodeInt(695);
                    outPacket.encodeInt(160);
                    outPacket.encodeByte(1);
                    outPacket.encodeInt(-690);
                    outPacket.encodeInt(-2378);
                    outPacket.encodeInt(695);
                    outPacket.encodeInt(-2019);
                    break;
                }
                outPacket.encodeByte(0);
                outPacket.encodeInt(-690);
                outPacket.encodeInt(-2378);
                outPacket.encodeInt(695);
                outPacket.encodeInt(-2019);
                outPacket.encodeByte(1);
                outPacket.encodeInt(-690);
                outPacket.encodeInt(-455);
                outPacket.encodeInt(695);
                outPacket.encodeInt(160);
            }
        }

        return outPacket;
    }

    public static OutPacket useSpecial() {
        return new OutPacket(OutHeader.WILL_USE_SPECIAL);
    }

    public static OutPacket stun() {
        return new OutPacket(OutHeader.WILL_STUN);
    }

    public static OutPacket thirdOne(int objectID) {
        OutPacket outPacket = new OutPacket(OutHeader.WILL_THIRD_ONE);

        outPacket.encodeByte(true);
        outPacket.encodeInt(objectID);

        return outPacket;
    }

    public static OutPacket spider(boolean isCreate, SpiderWeb web) {
        OutPacket outPacket = new OutPacket(OutHeader.WILL_SPIDER);

        outPacket.encodeInt(isCreate ? 3 : 4);
        outPacket.encodeInt(web.getObjectId());
        outPacket.encodeInt(web.getPattern());
        outPacket.encodePositionInt(web.getPos());
        switch (web.getPattern()) {
            case 0 -> {
                outPacket.encodeInt(100);
                outPacket.encodeInt(100);
                return outPacket;
            }
            case 1 -> {
                outPacket.encodeInt(160);
                outPacket.encodeInt(160);
                return outPacket;
            }
            case 2 -> {
                outPacket.encodeInt(270);
                outPacket.encodeInt(270);
                return outPacket;
            }
        }
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket teleport() {
        OutPacket outPacket = new OutPacket(OutHeader.WILL_TELEPORT);

        outPacket.encodeInt(1);

        return outPacket;
    }

    public static OutPacket poison(Set<Char> chars, int objectID) {
        OutPacket outPacket = new OutPacket(OutHeader.WILL_POISON);

        outPacket.encodeInt(chars.size());
        for (Char chr : chars) {
            outPacket.encodeByte(true);
            outPacket.encodeInt(objectID);
            outPacket.encodeInt(chr.getId());
            outPacket.encodeByte(true);
            outPacket.encodePositionInt(chr.getPosition());
        }

        return outPacket;
    }

    public static OutPacket poison(Char chr, int objectID) {
        OutPacket outPacket = new OutPacket(OutHeader.WILL_POISON_2);

        outPacket.encodeInt(objectID);
        outPacket.encodeInt(chr.getId());
        outPacket.encodeByte(true);
        outPacket.encodePositionInt(chr.getPosition());

        return outPacket;
    }

    public static OutPacket attackPoison(Char chr, int objectID) {
        OutPacket outPacket = new OutPacket(OutHeader.WILL_POISON_ATTACK);

        outPacket.encodeInt(objectID);
        outPacket.encodeInt(chr.getId());
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket removePoison(List<Char> chrs) {
        OutPacket outPacket = new OutPacket(OutHeader.WILL_POISON_REMOVE);

        outPacket.encodeInt(chrs.size());
        for (Char chr : chrs) {
            outPacket.encodeInt(chr.getId());
        }

        return outPacket;
    }

    public static OutPacket removePoison(int charID) {
        OutPacket outPacket = new OutPacket(OutHeader.WILL_POISON_REMOVE);

        outPacket.encodeInt(1);
        outPacket.encodeInt(charID);

        return outPacket;
    }
}
