package net.swordie.ms.handlers.user;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.enums.SpecialHPBossType;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.fieldeffect.FieldEffect;

public class SpecialHPBossHandler {

    private static final int[][] horntailMobs = new int[][]{
            {
                    8810202, //Easy Horntail's Head A
                    8810203, //Easy Horntail's Head B
                    8810204, //Easy Horntail's Head C
                    8810205, //Easy Horntail's Left Hand
                    8810206, //Easy Horntail's Right Hand
                    8810207, //Easy Horntail's Wings
                    8810208, //Easy Horntail's Legs
                    8810209, //Easy Horntail's Left Head
            },
            {
                    8810002, //Horntail's Head A
                    8810003, //Horntail's Head B
                    8810004, //Horntail's Head C
                    8810005, //Horntail's Left Hand
                    8810006, //Horntail's Right Hand
                    8810007, //Horntail's Wings
                    8810008, //Horntail's Legs
                    8810009, //Horntail's Tails
            },
            {
                    8810102, //Chaos Horntail's Head A
                    8810103, //Chaos Horntail's Head B
                    8810104, //Chaos Horntail's Head C
                    8810105, //Chaos Horntail's Left Hand
                    8810106, //Chaos Horntail's Right Hand
                    8810107, //Chaos Horntail's Wings
                    8810108, //Chaos Horntail's Legs
                    8810109, //Chaos Horntail's Tail
            },
    };

    private static final int[][] pinkBeanMobs = new int[][]{
            {   //Stage 1,2,3,4
                    8820002, //Ariel
                    8820003, //Solomon the Wise
                    8820004, //Rex the Wise
                    8820005, //Hugin
                    8820006, //Munin
                    //Stage 5
                    8820015, //Solomon the Wise
                    8820016, //Rex the Wise
                    8820017, //Hugin
                    8820018, //Munin
            },
            {   //Stage 1,2,3,4
                    8820102, //Chaos Ariel
                    8820103, //Chaos Solomon the Wise
                    8820104, //Chaos Rex the Wise
                    8820105, //Chaos Hugin
                    8820106, //Chaos Munin
                    //Stage 5
                    8820115, //Chaos Solomon the Wise
                    8820116, //Chaos Rex the Wise
                    8820117, //Chaos Hugin
                    8820118, //Chaos Munin
            },
    };
    private static final int[] horntailOptions = new int[]{
            8810214, //Easy Horntail
            8810018, //Normal Horntail
            8810122, //Chaos Horntail
    };
    private static final int[][] pinkBeanOptions = new int[][]{
            {8820010, 8820011, 8820012, 8820013, 8820014},
            {8820300, 8820301, 8820302, 8820303, 8820304},
    };

    public static void onDamage(Char chr, Mob mob, long damage, int skillID, boolean isDotDamage) {
        SpecialHPBossType type = getType(mob.getTemplateId());
        switch (type) {
            case Easy_Horntail:
                onDamageToHorntail(chr, damage, skillID, (byte) 0, isDotDamage);
                break;
            case Normal_Horntail:
                onDamageToHorntail(chr, damage, skillID, (byte) 1, isDotDamage);
                break;
            case Chaos_Horntail:
                onDamageToHorntail(chr, damage, skillID, (byte) 2, isDotDamage);
                break;
            case Normal_PinkBean:
                onDamageToPinkBean(chr, damage, skillID, (byte) 0, isDotDamage);
                break;
            case Chaos_PinkBean:
                onDamageToPinkBean(chr, damage, skillID, (byte) 1, isDotDamage);
                break;
        }
    }

    private static void onDamageToHorntail(Char chr, long damage, int skillID, byte mode, boolean isDotDamage) {
        Field field = chr.getField();
        Mob horntail = (Mob) field.getLifeByTemplateId(horntailOptions[mode]);
        if (horntail != null) {
            if (horntail.getHp() > 0) {
                horntail.damage(chr, damage, skillID);
                field.broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(horntail)));
            }
            if (isDotDamage) {
                Tuple<Long, Long> hpAndMaxHP = getHorntailTotalHP(field, mode);
                if (horntail.getHp() != (long) hpAndMaxHP.getLeft() && horntail.getMaxHp() == (long) hpAndMaxHP.getRight()) {
                    long hp_Un_calculate = (long) hpAndMaxHP.getLeft() - horntail.getHp();
                    horntail.damage(chr, hp_Un_calculate, skillID);
                }
            }
        }
    }

    private static void onDamageToPinkBean(Char chr, long damage, int skillID, byte mode, boolean isDotDamage) {
        Field field = chr.getField();
        for (int mobID : pinkBeanOptions[mode]) {
            Mob pinkBean = (Mob) field.getLifeByTemplateId(mobID);
            if (pinkBean != null) {
                if (pinkBean.getHp() > 0) {
                    long hpLeft = pinkBean.getHp() - damage;
                    if (hpLeft <= 0) {
                        if (pinkBean.getTemplateId() == 8820014 || pinkBean.getTemplateId() == 8820304) {
                            chr.getTimer().addEvent(() -> {
                                //Normal PinkBean: 8820000
                                //Chaos PinkBean: 8820100
                                int immortalID = pinkBean.getTemplateId() == 8820014 ? 8820000 : 8820100;
                                Life immortalPinkBean = field.getLifeByTemplateId(immortalID);
                                if (immortalPinkBean != null) {
                                    ((Mob) immortalPinkBean).remove(false);
                                }
                            }, 2000);
                        }
                    }
                    pinkBean.damage(chr, damage, skillID);
                    break;
                }
                if (isDotDamage) {
                    Tuple<Long, Long> hpAndMaxHP = getPinkBeanTotalHP(field, mode);
                    if (pinkBean.getHp() != (long) hpAndMaxHP.getLeft() && pinkBean.getMaxHp() == (long) hpAndMaxHP.getRight()) {
                        long hp_Un_calculate = (long) hpAndMaxHP.getLeft() - pinkBean.getHp();
                        pinkBean.damage(chr, hp_Un_calculate, skillID);
                    }
                }
            }
        }
    }

    public static Tuple<Long, Long> getHorntailTotalHP(Field field, int mode) {
        long hp = 0;
        long maxHP = 0;
        for (int mobID : horntailMobs[mode]) {
            Mob mob = (Mob) field.getLifeByTemplateId(mobID);
            if (mob != null) {
                hp += mob.getHp();
                maxHP += mob.getMaxHp();
            }
        }
        return new Tuple<>(hp, maxHP);
    }

    public static Tuple<Long, Long> getPinkBeanTotalHP(Field field, int mode) {
        long hp = 0;
        long maxHP = 0;
        for (int mobID : pinkBeanMobs[mode]) {
            Mob mob = (Mob) field.getLifeByTemplateId(mobID);
            if (mob != null) {
                hp += mob.getHp();
                maxHP += mob.getMaxHp();
            }
        }
        //System.out.println("hp: " + hp + " maxHP: " + maxHP);
        return new Tuple<>(hp, maxHP);
    }

    public static SpecialHPBossType getType(int mobID) {
        switch (mobID) {
            // Easy Horntail
            case 8810202: case 8810203: case 8810204: case 8810205:
            case 8810206: case 8810207: case 8810208: case 8810209:
                return SpecialHPBossType.Easy_Horntail;

            // Normal Horntail
            case 8810002: case 8810003: case 8810004: case 8810005:
            case 8810006: case 8810007: case 8810008: case 8810009:
                return SpecialHPBossType.Normal_Horntail;

            // Chaos Horntail
            case 8810102: case 8810103: case 8810104: case 8810105:
            case 8810106: case 8810107: case 8810108: case 8810109:
                return SpecialHPBossType.Chaos_Horntail;

            // Normal Pink Bean
            case 8820002: case 8820003: case 8820004: case 8820005: case 8820006:
            case 8820015: case 8820016: case 8820017: case 8820018:
                return SpecialHPBossType.Normal_PinkBean;

            // Chaos Pink Bean
            case 8820102: case 8820103: case 8820104: case 8820105: case 8820106:
            case 8820115: case 8820116: case 8820117: case 8820118:
                return SpecialHPBossType.Chaos_PinkBean;
        }
        return SpecialHPBossType.Null;
    }

    public static void onLackingGroupHPMobHandle(Char chr, int skillID) {
        Field field = chr.getField();
        switch (field.getId()) {
            case 240060300: {
                Mob horntail = (Mob) field.getLifeByTemplateId(horntailOptions[0]);
                if (horntail != null) {
                    boolean hasLiveMob = false;
                    for (Mob m : field.getMobs()) {
                        if (m.isBoss() && m.getHp() > 0) {
                            hasLiveMob = true;
                            break;
                        }
                    }
                    if (!hasLiveMob) {
                        horntail.damage(chr, horntail.getMaxHp(), skillID);
                        field.broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(horntail)));
                    }
                }
                break;
            }
            case 240060200: {
                Mob horntail = (Mob) field.getLifeByTemplateId(horntailOptions[1]);
                if (horntail != null) {
                    boolean hasLiveMob = false;
                    for (Mob m : field.getMobs()) {
                        if (m.isBoss() && m.getHp() > 0) {
                            hasLiveMob = true;
                            break;
                        }
                    }
                    if (!hasLiveMob) {
                        horntail.damage(chr, horntail.getMaxHp(), skillID);
                        field.broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(horntail)));
                    }
                }
                break;
            }
            case 240060201: {
                Mob horntail = (Mob) field.getLifeByTemplateId(horntailOptions[2]);
                if (horntail != null) {
                    boolean hasLiveMob = false;
                    for (Mob m : field.getMobs()) {
                        if (m.isBoss() && m.getHp() > 0) {
                            hasLiveMob = true;
                            break;
                        }
                    }
                    if (!hasLiveMob) {
                        horntail.damage(chr, horntail.getMaxHp(), skillID);
                        field.broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(horntail)));
                    }
                }
                break;
            }
            case 270050100: {
                for (int pinkBeanID : pinkBeanOptions[0]) {
                    Mob pinkBean = (Mob) field.getLifeByTemplateId(pinkBeanID);
                    if (pinkBean != null) {
                        boolean hasLiveMob = false;
                        for (int mobID : pinkBeanMobs[0]) {
                            Mob mob = (Mob) field.getLifeByTemplateId(mobID);
                            if (mob != null) {
                                hasLiveMob = true;
                                break;
                            }
                        }
                        if (!hasLiveMob && pinkBean.getHp() > 0) {
                            pinkBean.damage(chr, pinkBean.getMaxHp(), skillID);
                            field.broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(pinkBean)));
                        }
                    }
                }
                break;
            }
            case 270051100: {
                for (int pinkBeanID : pinkBeanOptions[1]) {
                    Mob pinkBean = (Mob) field.getLifeByTemplateId(pinkBeanID);
                    if (pinkBean != null) {
                        boolean hasLiveMob = false;
                        for (int mobID : pinkBeanMobs[1]) {
                            Mob mob = (Mob) field.getLifeByTemplateId(mobID);
                            if (mob != null) {
                                hasLiveMob = true;
                                break;
                            }
                        }
                        if (!hasLiveMob && pinkBean.getHp() > 0) {
                            pinkBean.damage(chr, pinkBean.getMaxHp(), skillID);
                            field.broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(pinkBean)));
                        }
                    }
                }
                int size = 0;
                for (Mob m : field.getMobs()) {
                    if (m.getTemplateId() >= 8820019 && m.getTemplateId() <= 8820027) {
                        continue;
                    }
                    if (m.isBoss()) {
                        size++;
                    }
                }
                if (size <= 1 && !field.isChaosPinkBeanSpawned()) {
                    field.removeMobsByTemplateID(8820114);
                    field.spawnMob(8820101, 5, -42, false);
                    field.setChaosPinkBeanSpawned(true);
                }
                break;
            }
        }
    }
}
