package net.swordie.ms.client.character.achievement;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.items.ScrollUpgradeInfo;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.CharPotGrade;
import net.swordie.ms.enums.ItemGrade;
import net.swordie.ms.enums.SpellTraceScrollType;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.loaders.Etc.Achievement.AchievementInfo;
import net.swordie.ms.loaders.Etc.Achievement.AchievementInfoData;

import java.util.List;
import java.util.Map;

import static net.swordie.ms.constants.GameConstants.*;

public class AchievementHandler {

    public static void handleLevelUp(Char chr, short level) {
        switch (level) {
            case 50:
                if (!chr.hasAchiementByInfoIdAndMissionID(37)) {
                    chr.addAchieventDoneByID(37);
                }
                break;
            case 100:
                if (!chr.hasAchiementByInfoIdAndMissionID(38)) {
                    chr.addAchieventDoneByID(38);
                }
                break;
            case 150:
                if (!chr.hasAchiementByInfoIdAndMissionID(39)) {
                    chr.addAchieventDoneByID(39);
                }
                break;
            case 200:
                if (!chr.hasAchiementByInfoIdAndMissionID(40)) {
                    chr.addAchieventDoneByID(40);
                }
                if (chr.getJob() == 0 || chr.getJob() == 1000 || chr.getJob() == 3000) {
                    if (!chr.hasAchiementByInfoIdAndMissionID(47)) {
                        chr.addAchieventDoneByID(47);
                    }
                } else {
                    if (!chr.hasAchievementDoneByInfoId(36)) {
                        int jobCode = -1;
                        AchievementInfo ai = AchievementInfoData.getAchievementInfoByID(36);
                        if (ai != null) {
                            for (AchievementInfo.MissionInfo mi : ai.getMissions()) {
                                for (Integer jc : mi.getJobCodes()) {
                                    if (jc == chr.getJob()) {
                                        jobCode = mi.getId();
                                        break;
                                    }
                                }
                            }
                        }
                        if (jobCode != -1) {
                            if (!chr.hasAchiementByInfoIdAndMissionID(36)) {
                                chr.addAchievementByID(36, jobCode, 1, 1);
                            } else {
                                chr.updateAchievementByID(36, jobCode, 200);
                                if (chr.hasAchievementDoneByInfoId(36)) {
                                    chr.addAchieventDoneByID(36);
                                }
                            }
                        }
                    }
                }
                break;
            case 210:
                if (!chr.hasAchiementByInfoIdAndMissionID(41)) {
                    chr.addAchieventDoneByID(41);
                }
                break;
            case 220:
                if (!chr.hasAchiementByInfoIdAndMissionID(42)) {
                    chr.addAchieventDoneByID(42);
                }
                break;
            case 230:
                if (!chr.hasAchiementByInfoIdAndMissionID(43)) {
                    chr.addAchieventDoneByID(43);
                }
                break;
            case 240:
                if (!chr.hasAchiementByInfoIdAndMissionID(44)) {
                    chr.addAchieventDoneByID(44);
                }
                break;
            case 250:
                if (!chr.hasAchiementByInfoIdAndMissionID(45)) {
                    chr.addAchieventDoneByID(45);
                }
                if (chr.getJob() == 0 || chr.getJob() == 1000 || chr.getJob() == 3000) {
                    if (!chr.hasAchiementByInfoIdAndMissionID(48)) {
                        chr.addAchieventDoneByID(48);
                    }
                } else {
                    if (!chr.hasAchievementDoneByInfoId(46)) {
                        int jobCode = -1;
                        AchievementInfo ai = AchievementInfoData.getAchievementInfoByID(46);
                        if (ai != null) {
                            for (AchievementInfo.MissionInfo mi : ai.getMissions()) {
                                for (Integer jc : mi.getJobCodes()) {
                                    if (jc == chr.getJob()) {
                                        jobCode = mi.getId();
                                        break;
                                    }
                                }
                            }
                        }
                        if (jobCode != -1) {
                            if (!chr.hasAchiementByInfoIdAndMissionID(46)) {
                                chr.addAchievementByID(46, jobCode, 1, 1);
                            } else {
                                chr.updateAchievementByID(46, jobCode, 250);
                                if (chr.hasAchievementDoneByInfoId(46)) {
                                    chr.addAchieventDoneByID(46);
                                }
                            }
                        }
                    }
                }
                break;
            case 255:
                if (!chr.hasAchiementByInfoIdAndMissionID(50)) {
                    chr.addAchieventDoneByID(50);
                }
                break;
            case 260:
                if (!chr.hasAchiementByInfoIdAndMissionID(51)) {
                    chr.addAchieventDoneByID(51);
                }
                break;
            case 265:
                if (!chr.hasAchiementByInfoIdAndMissionID(52)) {
                    chr.addAchieventDoneByID(52);
                }
                break;
            case 270:
                if (!chr.hasAchiementByInfoIdAndMissionID(53)) {
                    chr.addAchieventDoneByID(53);
                }
                break;
            case 275:
                if (!chr.hasAchiementByInfoIdAndMissionID(54)) {
                    chr.addAchieventDoneByID(54);
                }
                if (!chr.hasAchievementDoneByInfoId(55)) {
                    int jobCode = -1;
                    AchievementInfo ai = AchievementInfoData.getAchievementInfoByID(55);
                    if (ai != null) {
                        for (AchievementInfo.MissionInfo mi : ai.getMissions()) {
                            for (Integer jc : mi.getJobCodes()) {
                                if (jc == chr.getJob()) {
                                    jobCode = mi.getId();
                                    break;
                                }
                            }
                        }
                    }
                    if (jobCode != -1) {
                        if (!chr.hasAchiementByInfoIdAndMissionID(55)) {
                            chr.addAchievementByID(55, jobCode, 1, 1);
                        } else {
                            chr.updateAchievementByID(55, jobCode, 275);
                            if (chr.hasAchievementDoneByInfoId(55)) {
                                chr.addAchieventDoneByID(55);
                            }
                        }
                    }
                }
                break;
        }
    }

    public static void handleMultiKO(Char chr, int multiKillMessage) {
        // [Multi KO] It's Exciting!
        if (!chr.hasAchiementByInfoIdAndMissionID(620)) {
            chr.addAchievementByID(620, 0, 1, multiKillMessage);
        } else {
            if (!chr.hasAchievementDoneByInfoId(620)) {
                long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(620, 0) + multiKillMessage;
                if (newValue >= 1000) {
                    chr.updateAchievementByID(620, 0, 1000);
                    chr.addAchieventDoneByID(620);
                } else {
                    chr.updateAchievementByID(620, 0, newValue);
                }
            } else {
                // [Multi KO] It's New!
                if (!chr.hasAchiementByInfoIdAndMissionID(621)) {
                    chr.addAchievementByID(621, 0, 1, multiKillMessage);
                } else {
                    if (!chr.hasAchievementDoneByInfoId(621)) {
                        long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(621, 0) + multiKillMessage;
                        if (newValue >= 10000) {
                            chr.updateAchievementByID(621, 0, 10000);
                            chr.addAchieventDoneByID(621);
                        } else {
                            chr.updateAchievementByID(621, 0, newValue);
                        }
                    } else {
                        // [Multi KO] It's the Best!
                        if (!chr.hasAchiementByInfoIdAndMissionID(622)) {
                            chr.addAchievementByID(622, 0, 1, multiKillMessage);
                        } else {
                            if (!chr.hasAchievementDoneByInfoId(622)) {
                                long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(622, 0) + multiKillMessage;
                                if (newValue >= 100000) {
                                    chr.updateAchievementByID(622, 0, 100000);
                                    chr.addAchieventDoneByID(622);
                                } else {
                                    chr.updateAchievementByID(622, 0, newValue);
                                }
                            }
                        }
                    }
                }
            }
        }

        // [Multi KO] 1 Hit, 10 Deaths
        if (multiKillMessage == 10) {
            if (!chr.hasAchiementByInfoIdAndMissionID(624)) {
                chr.addAchievementByID(624, 0, 1, 1);
            } else {
                if (!chr.hasAchievementDoneByInfoId(624)) {
                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(624, 0) + 1;
                    if (newValue >= 1000) {
                        chr.updateAchievementByID(624, 0, 1000);
                        chr.addAchieventDoneByID(624);
                    } else {
                        chr.updateAchievementByID(624, 0, newValue);
                    }
                }
            }
        }
    }

    public static void handleComboKill(Char chr, int comboCount) {
        // [Combo Kill] 9999 Combo
        if (!chr.hasAchiementByInfoIdAndMissionID(614)) {
            chr.addAchievementByID(614, 0, 1, comboCount);
        } else {
            if (!chr.hasAchievementDoneByInfoId(614)) {
                long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(614, 0) + comboCount;
                if (newValue >= 9999) {
                    chr.updateAchievementByID(614, 0, 9999);
                    chr.addAchieventDoneByID(614);
                } else {
                    chr.updateAchievementByID(614, 0, newValue);
                }
            }
        }
        // [Combo Kill] 9999 Combos of Fear on the Wall...
        if (comboCount == 101) {
            if (!chr.hasAchiementByInfoIdAndMissionID(615)) {
                chr.addAchievementByID(615, 0, 1, comboCount);
            } else {
                if (!chr.hasAchievementDoneByInfoId(615)) {
                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(615, 0) + comboCount;
                    if (newValue >= 99) {
                        chr.updateAchievementByID(615, 0, 99);
                        chr.addAchieventDoneByID(615);
                    } else {
                        chr.updateAchievementByID(615, 0, newValue);
                    }
                }
            }
        }
    }

    public static void handleBurningField(List<Char> chars, int level) {
        // [Burning Field] A Fire in the Belly and the Field
        int missionID = level - 1;
        for (Char chr : chars) {
            if (!chr.hasAchiementByInfoIdAndMissionID(625)) {
                chr.addAchievementByID(625, missionID, 2, 1);
            } else {
                if (!chr.hasAchievementDoneByInfoId(625)) {
                    if (chr.hasAchievementDoneByInfoId(625)) {
                        chr.addAchieventDoneByID(625);
                    } else {
                        chr.updateAchievementByID(625, missionID, 1);
                    }
                }
            }
        }
    }

    public static void handleBurningField(Char chr, int level) {
        // [Burning Field] A Fire in the Belly and the Field
        int missionID = level - 1;
        if (!chr.hasAchiementByInfoIdAndMissionID(625)) {
            chr.addAchievementByID(625, missionID, 2, 1);
        } else {
            if (!chr.hasAchievementDoneByInfoId(625)) {
                if (chr.hasAchievementDoneByInfoId(625)) {
                    chr.addAchieventDoneByID(625);
                } else {
                    chr.updateAchievementByID(625, missionID, 1);
                }
            }
        }
    }

    public static void handlePowerElixir(Char chr, int itemID) {
        // Do you remember how many potions you had before?
        if (itemID == 2000005 || itemID == 2000019 || itemID == 2000032 || itemID == 2001505
                || itemID == 2022176 || itemID == 2022457 || itemID == 2022720 || itemID == 2023236) {
            if (!chr.hasAchiementByInfoIdAndMissionID(31)) {
                chr.addAchievementByID(31, 0, 1, 1);
            } else {
                if (!chr.hasAchievementDoneByInfoId(31)) {
                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(31, 0) + 1;
                    if (newValue >= 10000) {
                        chr.updateAchievementByID(31, 0, 10000);
                        chr.addAchieventDoneByID(31);
                    } else {
                        chr.updateAchievementByID(31, 0, newValue);
                    }
                }
            }
        }
    }

    public static void handlePetFood(Char chr) {
        // Here you go! Yummy, isn't it?
        if (!chr.hasAchiementByInfoIdAndMissionID(32)) {
            chr.addAchievementByID(32, 0, 1, 1);
        } else {
            if (!chr.hasAchievementDoneByInfoId(32)) {
                long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(32, 0) + 1;
                if (newValue >= 10000) {
                    chr.updateAchievementByID(32, 0, 10000);
                    chr.addAchieventDoneByID(32);
                } else {
                    chr.updateAchievementByID(32, 0, newValue);
                }
            }
        }
    }

    public static void handleEliteMonster(Char chr) {
        // [Elite Monster] Eliminate an Elite Monster
        if (!chr.hasAchiementByInfoIdAndMissionID(626)) {
            chr.addAchievementByID(626, 0, 1, 1);
        } else {
            if (!chr.hasAchievementDoneByInfoId(626)) {
                long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(626, 0) + 1;
                if (newValue >= 100) {
                    chr.updateAchievementByID(626, 0, 1000);
                    chr.addAchieventDoneByID(626);
                } else {
                    chr.updateAchievementByID(626, 0, newValue);
                }
            } else {
                // [Elite Monster] Eliminate Many Elite Monsters
                if (!chr.hasAchiementByInfoIdAndMissionID(627)) {
                    chr.addAchievementByID(627, 0, 1, 1);
                } else {
                    if (!chr.hasAchievementDoneByInfoId(627)) {
                        long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(627, 0) + 1;
                        if (newValue >= 1000) {
                            chr.updateAchievementByID(627, 0, 1000);
                            chr.addAchieventDoneByID(627);
                        } else {
                            chr.updateAchievementByID(627, 0, newValue);
                        }
                    } else {
                        // [Elite Monster] Eliminate Copious Elite Monsters
                        if (!chr.hasAchiementByInfoIdAndMissionID(628)) {
                            chr.addAchievementByID(628, 0, 1, 1);
                        } else {
                            if (!chr.hasAchievementDoneByInfoId(628)) {
                                long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(628, 0) + 1;
                                if (newValue >= 10000) {
                                    chr.updateAchievementByID(628, 0, 10000);
                                    chr.addAchieventDoneByID(628);
                                } else {
                                    chr.updateAchievementByID(628, 0, newValue);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void handleEliteBoss(Char chr) {
        // [Elite Boss] Not That Elite, Are Ya?
        if (!chr.hasAchiementByInfoIdAndMissionID(630)) {
            chr.addAchieventDoneByID(630);
        }

        if (chr.hasAchievementDoneByInfoId(630)) {
            // [Elite Boss] Eliminate an Elite Bosses
            if (!chr.hasAchiementByInfoIdAndMissionID(631)) {
                chr.addAchievementByID(631, 0, 1, 1);
            } else {
                if (!chr.hasAchievementDoneByInfoId(631)) {
                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(631, 0) + 1;
                    if (newValue >= 10) {
                        chr.updateAchievementByID(631, 0, 10);
                        chr.addAchieventDoneByID(631);
                    } else {
                        chr.updateAchievementByID(631, 0, newValue);
                    }
                } else {
                    // [Elite Boss] Eliminate Many Elite Bosses
                    if (!chr.hasAchiementByInfoIdAndMissionID(632)) {
                        chr.addAchievementByID(632, 0, 1, 1);
                    } else {
                        if (!chr.hasAchievementDoneByInfoId(632)) {
                            long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(632, 0) + 1;
                            if (newValue >= 100) {
                                chr.updateAchievementByID(632, 0, 100);
                                chr.addAchieventDoneByID(632);
                            } else {
                                chr.updateAchievementByID(632, 0, newValue);
                            }
                        } else {
                            // [Elite Boss] Eliminate Copious Elite Bosses
                            if (!chr.hasAchiementByInfoIdAndMissionID(633)) {
                                chr.addAchievementByID(633, 0, 1, 1);
                            } else {
                                if (!chr.hasAchievementDoneByInfoId(633)) {
                                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(633, 0) + 1;
                                    if (newValue >= 1000) {
                                        chr.updateAchievementByID(633, 0, 1000);
                                        chr.addAchieventDoneByID(633);
                                    } else {
                                        chr.updateAchievementByID(633, 0, newValue);
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
    }

    public static void handleLoot(Char chr, Drop drop) {
        long money = drop.isMoney() ? 0 : drop.getMoney();
        int itemID = 0;
        boolean isEquip = false;
        boolean isUnique = false;
        if (!drop.isMoney() && drop.getItem() != null) {
            Item item = drop.getItem();
            itemID = item.getItemId();
            if (item instanceof Equip equip) {
                isEquip = true;
                if (equip.getOptionBase(0) == ItemGrade.Unique.getVal() || equip.getOptionBase(0) == ItemGrade.Legendary.getVal()) {
                    isUnique = true;
                }
            }
        }
        if (drop.isMoney()) {
            // [Meso] Pocket Change
            if (!chr.hasAchiementByInfoIdAndMissionID(845)) {
                chr.addAchievementByID(845, 0, 1, money);
            } else {
                if (!chr.hasAchievementDoneByInfoId(845)) {
                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(845, 0) + money;
                    if (newValue >= 1000000) {
                        chr.updateAchievementByID(845, 0, 1000000);
                        chr.addAchieventDoneByID(845);
                    } else {
                        chr.updateAchievementByID(845, 0, newValue);
                    }
                } else {
                    // [Meso] Lump Sum
                    if (!chr.hasAchiementByInfoIdAndMissionID(846)) {
                        chr.addAchievementByID(846, 0, 1, money);
                    } else {
                        if (!chr.hasAchievementDoneByInfoId(846)) {
                            long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(846, 0) + money;
                            if (newValue >= 100000000) {
                                chr.updateAchievementByID(846, 0, 100000000);
                                chr.addAchieventDoneByID(846);
                            } else {
                                chr.updateAchievementByID(846, 0, newValue);
                            }
                        } else {
                            // [Meso] Every Penny Counts
                            if (!chr.hasAchiementByInfoIdAndMissionID(847)) {
                                chr.addAchievementByID(847, 0, 1, money);
                            } else {
                                if (!chr.hasAchievementDoneByInfoId(847)) {
                                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(845, 0) + money;
                                    if (newValue >= 10000000000L) {
                                        chr.updateAchievementByID(847, 0, 10000000000L);
                                        chr.addAchieventDoneByID(847);
                                    } else {
                                        chr.updateAchievementByID(847, 0, newValue);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // [Meso] A Lucky Day
            if (!chr.hasAchiementByInfoIdAndMissionID(849) && money == 777) {
                chr.addAchieventDoneByID(849);
            }
        } else {
            if (ItemConstants.isArcaneSymbol(itemID)) {
                // [Arcane Symbol] It Glimmers So
                if (!chr.hasAchiementByInfoIdAndMissionID(841)) {
                    chr.addAchievementByID(841, 0, 1, 1);
                } else {
                    if (!chr.hasAchievementDoneByInfoId(841)) {
                        long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(841, 0) + 1;
                        if (newValue >= 10) {
                            chr.updateAchievementByID(841, 0, 10);
                            chr.addAchieventDoneByID(841);
                        } else {
                            chr.updateAchievementByID(841, 0, newValue);
                        }
                    } else {
                        // [Arcane Symbol] Delicious!
                        if (!chr.hasAchiementByInfoIdAndMissionID(842)) {
                            chr.addAchievementByID(842, 0, 1, 1);
                        } else {
                            if (!chr.hasAchievementDoneByInfoId(842)) {
                                long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(842, 0) + 1;
                                if (newValue >= 100) {
                                    chr.updateAchievementByID(842, 0, 100);
                                    chr.addAchieventDoneByID(842);
                                } else {
                                    chr.updateAchievementByID(842, 0, newValue);
                                }
                            } else {
                                // [Arcane Symbol] A Whole String
                                if (!chr.hasAchiementByInfoIdAndMissionID(843)) {
                                    chr.addAchievementByID(843, 0, 1, 1);
                                } else {
                                    if (!chr.hasAchievementDoneByInfoId(843)) {
                                        long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(843, 0) + 1;
                                        if (newValue >= 1000) {
                                            chr.updateAchievementByID(843, 0, 1000);
                                            chr.addAchieventDoneByID(843);
                                        } else {
                                            chr.updateAchievementByID(843, 0, newValue);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (ItemConstants.isNodeStone(itemID)) {
                // [Nodestone] Treat Them Like Gold
                if (!chr.hasAchiementByInfoIdAndMissionID(837)) {
                    chr.addAchievementByID(837, 0, 1, 1);
                } else {
                    if (!chr.hasAchievementDoneByInfoId(837)) {
                        long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(837, 0) + 1;
                        if (newValue >= 10) {
                            chr.updateAchievementByID(837, 0, 10);
                            chr.addAchieventDoneByID(837);
                        } else {
                            chr.updateAchievementByID(837, 0, newValue);
                        }
                    } else {
                        // [Nodestone] Piece of Rock
                        if (!chr.hasAchiementByInfoIdAndMissionID(838)) {
                            chr.addAchievementByID(838, 0, 1, 1);
                        } else {
                            if (!chr.hasAchievementDoneByInfoId(838)) {
                                long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(838, 0) + 1;
                                if (newValue >= 100) {
                                    chr.updateAchievementByID(838, 0, 100);
                                    chr.addAchieventDoneByID(838);
                                } else {
                                    chr.updateAchievementByID(838, 0, newValue);
                                }
                            } else {
                                // [Nodestone] Rock Rich
                                if (!chr.hasAchiementByInfoIdAndMissionID(839)) {
                                    chr.addAchievementByID(839, 0, 1, 1);
                                } else {
                                    if (!chr.hasAchievementDoneByInfoId(839)) {
                                        long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(839, 0) + 1;
                                        if (newValue >= 1000) {
                                            chr.updateAchievementByID(839, 0, 1000);
                                            chr.addAchieventDoneByID(839);
                                        } else {
                                            chr.updateAchievementByID(839, 0, newValue);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (itemID == 1122150) {
                // [Arkarium] Dominator, You're Mine!
                if (!chr.hasAchiementByInfoIdAndMissionID(854)) {
                    chr.addAchieventDoneByID(854);
                }
            } else if (itemID == 4001877) {
                // [Lotus] Black Heart, You're Mine!
                if (!chr.hasAchiementByInfoIdAndMissionID(855)) {
                    chr.addAchieventDoneByID(855);
                }
            } else if (itemID == 1099015) {
                // [Damien] Ruin Demon Aegis, You're Mine!
                if (!chr.hasAchiementByInfoIdAndMissionID(856)) {
                    chr.addAchieventDoneByID(856);
                }
            } else if (itemID == 1132308) {
                // [Lucid] Dreamy Belt, You're Mine!
                if (!chr.hasAchiementByInfoIdAndMissionID(857)) {
                    chr.addAchieventDoneByID(857);
                }
            } else if (itemID == 1402179) {
                // [Magnus] Kaiserium, You're Mine!
                if (!chr.hasAchiementByInfoIdAndMissionID(858)) {
                    chr.addAchieventDoneByID(858);
                }
            } else if (itemID == 1122430) {
                // [Verus Hilla] Source of Suffering, You're Mine!
                if (!chr.hasAchiementByInfoIdAndMissionID(859)) {
                    chr.addAchieventDoneByID(859);
                }
            } else if (itemID == 1022278) {
                // [Damien] Magic Eyepatch, You're Mine!
                if (!chr.hasAchiementByInfoIdAndMissionID(860)) {
                    chr.addAchieventDoneByID(860);
                }
            } else if (itemID == 1162080 || itemID == 1162081 || itemID == 1162082 || itemID == 1162083) {
                // [Will] Cursed Spellbook, You're Mine!
                if (!chr.hasAchiementByInfoIdAndMissionID(866)) {
                    chr.addAchieventDoneByID(866);
                }
            } else if (itemID == 1012632) {
                // [Lotus] Berserked, You're Mine!
                if (!chr.hasAchiementByInfoIdAndMissionID(867)) {
                    chr.addAchieventDoneByID(867);
                }
            } else if (itemID == 1182285) {
                // [Black Mage] Genesis Badge, You're Mine!
                if (!chr.hasAchiementByInfoIdAndMissionID(869)) {
                    chr.addAchieventDoneByID(869);
                }
            } else if (isUnique) {
                // Quick, Give Me Unique
                if (!chr.hasAchiementByInfoIdAndMissionID(861)) {
                    chr.addAchieventDoneByID(861);
                }
            } else if (itemID == BLUE_EXP_ORB_ID) {
                // [Combo Kill] Blue-Stockinged Kills
                if (!chr.hasAchiementByInfoIdAndMissionID(616)) {
                    chr.addAchievementByID(616, 0, 1, 1);
                } else {
                    if (!chr.hasAchievementDoneByInfoId(616)) {
                        long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(616, 0) + 1;
                        if (newValue >= 1000) {
                            chr.updateAchievementByID(616, 0, 1000);
                            chr.addAchieventDoneByID(616);
                        } else {
                            chr.updateAchievementByID(616, 0, newValue);
                        }
                    }
                }
            } else if (itemID == PURPLE_EXP_ORB_ID) {
                // [Combo Kill] Purple Prose Kills
                if (!chr.hasAchiementByInfoIdAndMissionID(617)) {
                    chr.addAchievementByID(617, 0, 1, 1);
                } else {
                    if (!chr.hasAchievementDoneByInfoId(617)) {
                        long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(617, 0) + 1;
                        if (newValue >= 1000) {
                            chr.updateAchievementByID(617, 0, 1000);
                            chr.addAchieventDoneByID(617);
                        } else {
                            chr.updateAchievementByID(617, 0, newValue);
                        }
                    }
                }
            } else if (itemID == RED_EXP_ORB_ID) {
                // [Combo Kill] Red-Caped Kills
                if (!chr.hasAchiementByInfoIdAndMissionID(618)) {
                    chr.addAchievementByID(618, 0, 1, 1);
                } else {
                    if (!chr.hasAchievementDoneByInfoId(618)) {
                        long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(618, 0) + 1;
                        if (newValue >= 1000) {
                            chr.updateAchievementByID(618, 0, 1000);
                            chr.addAchieventDoneByID(618);
                        } else {
                            chr.updateAchievementByID(618, 0, newValue);
                        }
                    }
                }
            } else if (itemID == YELLOW_EXP_ORB_ID) {
                // [Combo Kill] Yellow-bellied Kills
                if (!chr.hasAchiementByInfoIdAndMissionID(619)) {
                    chr.addAchievementByID(619, 0, 1, 1);
                } else {
                    if (!chr.hasAchievementDoneByInfoId(619)) {
                        long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(619, 0) + 1;
                        if (newValue >= 1000) {
                            chr.updateAchievementByID(619, 0, 1000);
                            chr.addAchieventDoneByID(619);
                        } else {
                            chr.updateAchievementByID(619, 0, newValue);
                        }
                    }
                }
            } else {
                if (isEquip) {
                    // [Obtain Equipment] Antique Shop
                    if (!chr.hasAchiementByInfoIdAndMissionID(850)) {
                        chr.addAchievementByID(850, 0, 1, 1);
                    } else {
                        if (!chr.hasAchievementDoneByInfoId(850)) {
                            long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(850, 0) + 1;
                            if (newValue >= 100) {
                                chr.updateAchievementByID(850, 0, 100);
                                chr.addAchieventDoneByID(850);
                            } else {
                                chr.updateAchievementByID(850, 0, newValue);
                            }
                        }
                    }
                    // [Obtain Equipment] Dirty Spoon
                    if (chr.hasAchievementDoneByInfoId(850)) {
                        if (!chr.hasAchiementByInfoIdAndMissionID(851)) {
                            chr.addAchievementByID(851, 0, 1, 1);
                        } else {
                            if (!chr.hasAchievementDoneByInfoId(851)) {
                                long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(851, 0) + 1;
                                if (newValue >= 1000) {
                                    chr.updateAchievementByID(851, 0, 1000);
                                    chr.addAchieventDoneByID(851);
                                } else {
                                    chr.updateAchievementByID(851, 0, newValue);
                                }
                            }
                        }
                        // [Obtain Equipment] Vacuum Cleaner
                        if (chr.hasAchievementDoneByInfoId(851)) {
                            if (!chr.hasAchiementByInfoIdAndMissionID(852)) {
                                chr.addAchievementByID(852, 0, 1, 1);
                            } else {
                                if (!chr.hasAchievementDoneByInfoId(852)) {
                                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(852, 0) + 1;
                                    if (newValue >= 10000) {
                                        chr.updateAchievementByID(852, 0, 10000);
                                        chr.addAchieventDoneByID(852);
                                    } else {
                                        chr.updateAchievementByID(852, 0, newValue);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void handleEarnHonorEXP(Char chr, int exp) {
        // [Honor EXP] First, The Fame
        if (!chr.hasAchiementByInfoIdAndMissionID(862)) {
            chr.addAchievementByID(862, 0, 1, exp);
        } else {
            if (!chr.hasAchievementDoneByInfoId(862)) {
                long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(862, 0) + exp;
                if (newValue >= 10000) {
                    chr.updateAchievementByID(862, 0, 10000);
                    chr.addAchieventDoneByID(862);
                } else {
                    chr.updateAchievementByID(862, 0, newValue);
                }
            } else {
                // [Honor EXP] Then, the Fortune
                if (!chr.hasAchiementByInfoIdAndMissionID(863)) {
                    chr.addAchievementByID(863, 0, 1, exp);
                } else {
                    if (!chr.hasAchievementDoneByInfoId(863)) {
                        long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(627, 0) + exp;
                        if (newValue >= 100000) {
                            chr.updateAchievementByID(863, 0, 100000);
                            chr.addAchieventDoneByID(863);
                        } else {
                            chr.updateAchievementByID(863, 0, newValue);
                        }
                    } else {
                        // [Honor EXP] Then Cometh the Fall
                        if (!chr.hasAchiementByInfoIdAndMissionID(864)) {
                            chr.addAchievementByID(864, 0, 1, exp);
                        } else {
                            if (!chr.hasAchievementDoneByInfoId(864)) {
                                long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(864, 0) + exp;
                                if (newValue >= 1000000) {
                                    chr.updateAchievementByID(864, 0, 1000000);
                                    chr.addAchieventDoneByID(864);
                                } else {
                                    chr.updateAchievementByID(864, 0, newValue);
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    public static void handleConsumeHonorEXP(Char chr, int exp) {
        exp *= -1;
        // [Ability] Sandy Foundation | desc: Consume cumulative 1,000,000 Honor EXP
        if (!chr.hasAchiementByInfoIdAndMissionID(105)) {
            chr.addAchievementByID(105, 0, 1, exp);
        } else {
            if (!chr.hasAchievementDoneByInfoId(105)) {
                long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(105, 0) + exp;
                if (newValue >= 1000000) {
                    chr.updateAchievementByID(105, 0, 1000000);
                    chr.addAchieventDoneByID(105);
                } else {
                    chr.updateAchievementByID(105, 0, newValue);
                }
            } else {
                // [Ability] Humility | desc: Consume cumulative 10,000,000 Honor EXP
                if (!chr.hasAchiementByInfoIdAndMissionID(106)) {
                    chr.addAchievementByID(106, 0, 1, exp);
                } else {
                    if (!chr.hasAchievementDoneByInfoId(106)) {
                        long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(106, 0) + exp;
                        if (newValue >= 10000000) {
                            chr.updateAchievementByID(106, 0, 10000000);
                            chr.addAchieventDoneByID(106);
                        } else {
                            chr.updateAchievementByID(106, 0, newValue);
                        }
                    } else {
                        // [Ability] Look Out Below! | desc: Consume cumulative 100,000,000 Honor EXP
                        if (!chr.hasAchiementByInfoIdAndMissionID(107)) {
                            chr.addAchievementByID(107, 0, 1, exp);
                        } else {
                            if (!chr.hasAchievementDoneByInfoId(107)) {
                                long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(107, 0) + exp;
                                if (newValue >= 100000000) {
                                    chr.updateAchievementByID(107, 0, 100000000);
                                    chr.addAchieventDoneByID(107);
                                } else {
                                    chr.updateAchievementByID(107, 0, newValue);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void handleQuestCompleted(Char chr, int questID) {
        Map<AchievementInfo, Integer> ais = AchievementInfoData.getAchievementInfoByQuestID(questID);
        handle(chr, ais);
    }

    public static void handleFieldEnter(Char chr, int fieldID) {
        Map<AchievementInfo, Integer> ais = AchievementInfoData.getAchievementInfoByFieldID(fieldID);
        handle(chr, ais);
    }

    public static void handleMobKilled(Char chr, int mobID) {
        Map<AchievementInfo, Integer> ais = AchievementInfoData.getAchievementInfoByMobID(mobID);
        if (!ais.isEmpty()) {
            for (Map.Entry<AchievementInfo, Integer> ai : ais.entrySet()) {
                int infoID = ai.getKey().getId();
                int missionID = ai.getValue();
                if (!chr.hasAchievementDoneByInfoId(infoID) && ai.getKey().getMissions().size() == 1 && ai.getKey().getMissions().get(0).getValue() == 1) {
                    chr.addAchieventDoneByID(infoID);
                } else {
                    long value = AchievementInfoData.getMissionValueByInfoIDAndMissionID(infoID, missionID);
                    if (!chr.hasAchiementByInfoIdAndMissionID(infoID, missionID)) {
                        chr.addAchievementByID(infoID, missionID, value > 1 ? 1 : 2, 1);
                    } else {
                        if (!chr.hasAchievementDoneByInfoId(infoID)) {
                            long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(infoID, missionID) + 1;
                            if (newValue >= value) {
                                chr.updateAchievementByID(infoID, missionID, value);
                                chr.addAchieventDoneByID(infoID);
                            } else {
                                chr.updateAchievementByID(infoID, missionID, newValue);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void handle(Char chr, Map<AchievementInfo, Integer> ais) {
        if (!ais.isEmpty()) {
            for (Map.Entry<AchievementInfo, Integer> ai : ais.entrySet()) {
                int infoID = ai.getKey().getId();
                int missionID = ai.getValue();
                if (!chr.hasAchievementDoneByInfoId(infoID) && ai.getKey().getMissions().size() == 1 && ai.getKey().getMissions().get(0).getValue() == 1) {
                    chr.addAchieventDoneByID(infoID);
                } else {
                    if (!chr.hasAchiementByInfoIdAndMissionID(infoID, missionID)) {
                        chr.addAchievementByID(infoID, missionID, 2, 1);
                    }
                    chr.handleDoneAchievementByInfo(infoID);
                }
            }
        }
    }

    public static void handleResetFirstCharPotentialRank(Char chr, int grade) {
        if (grade == CharPotGrade.Epic.ordinal()) {
            // [Ability] Epic Rank Ability detected.
            if (!chr.hasAchievementDoneByInfoId(101)) {
                chr.addAchieventDoneByID(101);
            }
        } else if (grade == CharPotGrade.Unique.ordinal()) {
            // [Ability] Unique Rank Ability detected.
            if (!chr.hasAchievementDoneByInfoId(102)) {
                chr.addAchieventDoneByID(102);
            }
        } else if (grade == CharPotGrade.Legendary.ordinal()) {
            // [Ability] Legendary Rank Ability detected.
            if (!chr.hasAchievementDoneByInfoId(103)) {
                chr.addAchieventDoneByID(103);
            }
        }
    }

    public static void handleResetCharPotentials(Char chr, int skillID, int slv) {
        // [Ability] Try getting this too! | desc: Reset Ability and have the following Ability
        switch (skillID) {
            case 70000016:
                if (slv >= 1) {
                    if (!chr.hasAchiementByInfoIdAndMissionID(104, 0)) {
                        chr.addAchievementByID(104, 0, 2, 1);
                        if (chr.hasAchievementDoneByInfoId(104)) {
                            chr.addAchieventDoneByID(104);
                        }
                    }
                }
                break;
            case 70000048:
                if (slv >= 40) {
                    if (!chr.hasAchiementByInfoIdAndMissionID(104, 1)) {
                        chr.addAchievementByID(104, 1, 2, 1);
                        if (chr.hasAchievementDoneByInfoId(104)) {
                            chr.addAchieventDoneByID(104);
                        }
                    }
                }
                break;
            case 70000012:
            case 70000013:
                if (slv >= 28) {
                    if (!chr.hasAchiementByInfoIdAndMissionID(104, 2)) {
                        chr.addAchievementByID(104, 2, 2, 1);
                        if (chr.hasAchievementDoneByInfoId(104)) {
                            chr.addAchieventDoneByID(104);
                        }
                    }
                }
                break;
            case 70000049:
                if (slv >= 39) {
                    if (!chr.hasAchiementByInfoIdAndMissionID(104, 3)) {
                        chr.addAchievementByID(104, 3, 2, 1);
                        if (chr.hasAchievementDoneByInfoId(104)) {
                            chr.addAchieventDoneByID(104);
                        }
                    }
                }
                break;
            case 70000050:
                if (slv >= 39) {
                    if (!chr.hasAchiementByInfoIdAndMissionID(104, 4)) {
                        chr.addAchievementByID(104, 4, 2, 1);
                        if (chr.hasAchievementDoneByInfoId(104)) {
                            chr.addAchieventDoneByID(104);
                        }
                    }
                }
                break;
            case 70000047:
                if (slv >= 1) {
                    if (!chr.hasAchiementByInfoIdAndMissionID(104, 5)) {
                        chr.addAchievementByID(104, 5, 2, 1);
                        if (chr.hasAchievementDoneByInfoId(104)) {
                            chr.addAchieventDoneByID(104);
                        }
                    }
                }
                break;
        }
    }

    public static void handleSpendingMesos(Char chr, long cost) {
        // [Shop] Credit OK | desc: Spend more than 100 million mesos in NPC shops
        if (!chr.hasAchiementByInfoIdAndMissionID(1062)) {
            chr.addAchievementByID(1062, 0, 1, cost);
        } else {
            if (!chr.hasAchievementDoneByInfoId(1062)) {
                long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(1062, 0) + cost;
                if (newValue >= 100000000) {
                    chr.updateAchievementByID(1062, 0, 100000000);
                    chr.addAchieventDoneByID(1062);
                } else {
                    chr.updateAchievementByID(1062, 0, newValue);
                }
            } else {
                // [Shop] Merchants' Choice | desc: Spend more than 1 billion mesos in NPC shops
                if (!chr.hasAchiementByInfoIdAndMissionID(1063)) {
                    chr.addAchievementByID(1063, 0, 1, cost);
                } else {
                    if (!chr.hasAchievementDoneByInfoId(1063)) {
                        long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(1063, 0) + cost;
                        if (newValue >= 1000000000) {
                            chr.updateAchievementByID(1063, 0, 1000000000);
                            chr.addAchieventDoneByID(1063);
                        } else {
                            chr.updateAchievementByID(1063, 0, newValue);
                        }
                    } else {
                        // [Shop] The satisfied merchants have blessed you | desc: Spend more than 10 billion mesos in NPC shops
                        if (!chr.hasAchiementByInfoIdAndMissionID(1064)) {
                            chr.addAchievementByID(1064, 0, 1, cost);
                        } else {
                            if (!chr.hasAchievementDoneByInfoId(1064)) {
                                long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(1064, 0) + cost;
                                if (newValue >= 10000000000L) {
                                    chr.updateAchievementByID(1064, 0, 10000000000L);
                                    chr.addAchieventDoneByID(1064);
                                } else {
                                    chr.updateAchievementByID(1064, 0, newValue);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void handleBuyingEquipFromShop(Char chr, int itemID, int shopID) {
        if (shopID == 2155001 || shopID == 1540894 || shopID == 3003106 || shopID == 3003537) {
            AchievementInfo ai = AchievementInfoData.getAchievementInfoByID(1065);
            if (ai != null) {
                for (AchievementInfo.MissionInfo mi : ai.getMissions()) {
                    for (Long id : mi.getItemIDs()) {
                        if (id == itemID) {
                            if (!chr.hasAchiementByInfoIdAndMissionID(1065, mi.getId())) {
                                chr.addAchievementByID(1065, mi.getId(), 2, 1);
                                if (chr.hasAchievementDoneByInfoId(1065)) {
                                    chr.addAchieventDoneByID(1065);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    public static void handleUseRuneStone(Char chr, int runeStoneID) {
        if (runeStoneID >= 0 && runeStoneID <= 10) {
            if (!chr.hasAchiementByInfoIdAndMissionID(563, runeStoneID)) {
                chr.addAchievementByID(563, runeStoneID, 2, 1);
                if (chr.hasAchievementDoneByInfoId(563)) {
                    chr.addAchieventDoneByID(563);
                }
            }
            // [Rune] Ruminating About Runes | desc: Use a Rune Successfully 100 times
            if (!chr.hasAchiementByInfoIdAndMissionID(564)) {
                chr.addAchievementByID(564, 0, 1, 1);
            } else {
                if (!chr.hasAchievementDoneByInfoId(564)) {
                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(564, 0) + 1;
                    if (newValue >= 100) {
                        chr.updateAchievementByID(564, 0, 100);
                        chr.addAchieventDoneByID(564);
                    } else {
                        chr.updateAchievementByID(564, 0, newValue);
                    }
                } else {
                    // [Rune] Use More Runes, or All is Ruined!
                    if (!chr.hasAchiementByInfoIdAndMissionID(565)) {
                        chr.addAchievementByID(565, 0, 1, 1);
                    } else {
                        if (!chr.hasAchievementDoneByInfoId(565)) {
                            long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(565, 0) + 1;
                            if (newValue >= 1000) {
                                chr.updateAchievementByID(565, 0, 1000);
                                chr.addAchieventDoneByID(565);
                            } else {
                                chr.updateAchievementByID(565, 0, newValue);
                            }
                        } else {
                            // [Rune] Ruining Your Appetite for Runes
                            if (!chr.hasAchiementByInfoIdAndMissionID(566)) {
                                chr.addAchievementByID(566, 0, 1, 1);
                            } else {
                                if (!chr.hasAchievementDoneByInfoId(566)) {
                                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(566, 0) + 1;
                                    if (newValue >= 10000) {
                                        chr.updateAchievementByID(566, 0, 10000);
                                        chr.addAchieventDoneByID(566);
                                    } else {
                                        chr.updateAchievementByID(566, 0, newValue);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void handleSpellTrace(Char chr, ScrollUpgradeInfo sui) {
        if (sui.getType() == SpellTraceScrollType.Innocence) {
            // [Spell Trace] Be Innocent | desc: Successfully Use Spell Trace Innocence Scroll 100 times
            if (!chr.hasAchiementByInfoIdAndMissionID(978)) {
                chr.addAchievementByID(978, 0, 1, 1);
            } else {
                if (!chr.hasAchievementDoneByInfoId(978)) {
                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(978, 0) + 1;
                    if (newValue >= 100) {
                        chr.updateAchievementByID(978, 0, 100);
                        chr.addAchieventDoneByID(978);
                    } else {
                        chr.updateAchievementByID(978, 0, newValue);
                    }
                }
            }
        } else if (sui.getType() == SpellTraceScrollType.CleanSlate) {
            // [Spell Trace] Cleanly, Purely, Success! | desc: Successfully Use Spell Trace Pure Clean Slate Scroll 100 times
            if (!chr.hasAchiementByInfoIdAndMissionID(980)) {
                chr.addAchievementByID(980, 0, 1, 1);
            } else {
                if (!chr.hasAchievementDoneByInfoId(980)) {
                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(980, 0) + 1;
                    if (newValue >= 100) {
                        chr.updateAchievementByID(980, 0, 100);
                        chr.addAchieventDoneByID(980);
                    } else {
                        chr.updateAchievementByID(980, 0, newValue);
                    }
                }
            }
        } else if (sui.getType() == SpellTraceScrollType.Normal && sui.getChance() == 15) {
            // [Spell Trace] High-risk, High-return | desc: Successfully Use Spell Trace 15% Scroll 100 time
            if (!chr.hasAchiementByInfoIdAndMissionID(975)) {
                chr.addAchievementByID(975, 0, 1, 1);
            } else {
                if (!chr.hasAchievementDoneByInfoId(975)) {
                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(975, 0) + 1;
                    if (newValue >= 100) {
                        chr.updateAchievementByID(975, 0, 100);
                        chr.addAchieventDoneByID(975);
                    } else {
                        chr.updateAchievementByID(975, 0, newValue);
                    }
                } else {
                    // [Spell Trace] No Pain, No Gain | desc: Successfully Use Spell Trace 15% Scroll 1,000 times
                    if (!chr.hasAchiementByInfoIdAndMissionID(976)) {
                        chr.addAchievementByID(976, 0, 1, 1);
                    } else {
                        if (!chr.hasAchievementDoneByInfoId(976)) {
                            long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(976, 0) + 1;
                            if (newValue >= 1000) {
                                chr.updateAchievementByID(976, 0, 1000);
                                chr.addAchieventDoneByID(976);
                            } else {
                                chr.updateAchievementByID(976, 0, newValue);
                            }
                        }
                    }
                }
            }
        }
        if (sui.getCost() > 0) {
            int cost = sui.getCost();
            // [Spell Trace] Collect Traces to Cast Spells
            if (!chr.hasAchiementByInfoIdAndMissionID(971)) {
                chr.addAchievementByID(971, 0, 1, cost);
            } else {
                if (!chr.hasAchievementDoneByInfoId(971)) {
                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(971, 0) + cost;
                    if (newValue >= 10000) {
                        chr.updateAchievementByID(971, 0, 10000);
                        chr.addAchieventDoneByID(971);
                    } else {
                        chr.updateAchievementByID(971, 0, newValue);
                    }
                } else {
                    // [Spell Trace] Spell-aholic | desc: Use Spell Trace x100,000 total
                    if (!chr.hasAchiementByInfoIdAndMissionID(972)) {
                        chr.addAchievementByID(972, 0, 1, cost);
                    } else {
                        if (!chr.hasAchievementDoneByInfoId(972)) {
                            long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(972, 0) + cost;
                            if (newValue >= 100000) {
                                chr.updateAchievementByID(972, 0, 100000);
                                chr.addAchieventDoneByID(972);
                            } else {
                                chr.updateAchievementByID(972, 0, newValue);
                            }
                        } else {
                            // [Spell Trace] Spell Traces, Scatter!
                            if (!chr.hasAchiementByInfoIdAndMissionID(973)) {
                                chr.addAchievementByID(973, 0, 1, cost);
                            } else {
                                if (!chr.hasAchievementDoneByInfoId(973)) {
                                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(973, 0) + cost;
                                    if (newValue >= 1000000) {
                                        chr.updateAchievementByID(973, 0, 1000000);
                                        chr.addAchieventDoneByID(973);
                                    } else {
                                        chr.updateAchievementByID(973, 0, newValue);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void handleStarForce(Char chr, long cost, int newStar, boolean isSuccess, boolean isBoom, boolean extraChanceFromMiniGame) {
        if (isSuccess) {
            // [Star Force] May the Stars Be With You | desc: Achieve Star Force Success 100 times
            if (!chr.hasAchiementByInfoIdAndMissionID(955)) {
                chr.addAchievementByID(955, 0, 1, 1);
            } else {
                if (!chr.hasAchievementDoneByInfoId(955)) {
                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(955, 0) + 1;
                    if (newValue >= 100) {
                        chr.updateAchievementByID(955, 0, 100);
                        chr.addAchieventDoneByID(955);
                    } else {
                        chr.updateAchievementByID(955, 0, newValue);
                    }
                } else {
                    // [Star Force] You're a Star | desc: Achieve Star Force Success 1,000 times
                    if (!chr.hasAchiementByInfoIdAndMissionID(956)) {
                        chr.addAchievementByID(956, 0, 1, 1);
                    } else {
                        if (!chr.hasAchievementDoneByInfoId(956)) {
                            long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(956, 0) + 1;
                            if (newValue >= 1000) {
                                chr.updateAchievementByID(956, 0, 1000);
                                chr.addAchieventDoneByID(956);
                            } else {
                                chr.updateAchievementByID(956, 0, newValue);
                            }
                        } else {
                            // [Star Force] Doesn't Everyone Do This? | desc: Achieve Star Force Success 10,000 times
                            if (!chr.hasAchiementByInfoIdAndMissionID(957)) {
                                chr.addAchievementByID(957, 0, 1, 1);
                            } else {
                                if (!chr.hasAchievementDoneByInfoId(957)) {
                                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(957, 0) + 1;
                                    if (newValue >= 10000) {
                                        chr.updateAchievementByID(957, 0, 10000);
                                        chr.addAchieventDoneByID(957);
                                    } else {
                                        chr.updateAchievementByID(957, 0, newValue);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (newStar == 5) {
                // [Star Force] Achieve 5 Stars! | desc: Successful Star Force Enhancement of 5 Stars
                if (!chr.hasAchievementDoneByInfoId(942)) {
                    chr.addAchieventDoneByID(942);
                }
            } else if (newStar == 10) {
                // [Star Force] Achieve 10 Stars! | desc: Successful Star Force Enhancement of 10 Stars
                if (!chr.hasAchievementDoneByInfoId(943)) {
                    chr.addAchieventDoneByID(943);
                }
            } else if (newStar == 15) {
                //  [Star Force] Achieve 15 Stars! | desc: Successful Star Force Enhancement of 15 Stars
                if (!chr.hasAchievementDoneByInfoId(944)) {
                    chr.addAchieventDoneByID(944);
                }
            }
        } else {
            if (isBoom) {
                // [Star Force] It's Destroyed? | desc: Perform Star Force Destruction 100 times
                if (!chr.hasAchiementByInfoIdAndMissionID(963)) {
                    chr.addAchievementByID(963, 0, 1, 1);
                } else {
                    if (!chr.hasAchievementDoneByInfoId(963)) {
                        long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(963, 0) + 1;
                        if (newValue >= 100) {
                            chr.updateAchievementByID(963, 0, 100);
                            chr.addAchieventDoneByID(963);
                        } else {
                            chr.updateAchievementByID(963, 0, newValue);
                        }
                    } else {
                        // [Star Force] Understand. Enhance. Destroy. | desc: Perform Star Force Destruction 1,000 times
                        if (!chr.hasAchiementByInfoIdAndMissionID(964)) {
                            chr.addAchievementByID(964, 0, 1, 1);
                        } else {
                            if (!chr.hasAchievementDoneByInfoId(964)) {
                                long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(964, 0) + 1;
                                if (newValue >= 1000) {
                                    chr.updateAchievementByID(964, 0, 1000);
                                    chr.addAchieventDoneByID(964);
                                } else {
                                    chr.updateAchievementByID(964, 0, newValue);
                                }
                            } else {
                                // [Star Force] God of Destruction | desc: Perform Star Force Destruction 10,000 times
                                if (!chr.hasAchiementByInfoIdAndMissionID(965)) {
                                    chr.addAchievementByID(965, 0, 1, 1);
                                } else {
                                    if (!chr.hasAchievementDoneByInfoId(965)) {
                                        long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(965, 0) + 1;
                                        if (newValue >= 10000) {
                                            chr.updateAchievementByID(965, 0, 10000);
                                            chr.addAchieventDoneByID(965);
                                        } else {
                                            chr.updateAchievementByID(965, 0, newValue);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            //  [Star Force] Failure breeds success, you know! | desc: Fail at Star Force 100 times
            if (!chr.hasAchiementByInfoIdAndMissionID(959)) {
                chr.addAchievementByID(959, 0, 1, 1);
            } else {
                if (!chr.hasAchievementDoneByInfoId(959)) {
                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(959, 0) + 1;
                    if (newValue >= 100) {
                        chr.updateAchievementByID(959, 0, 100);
                        chr.addAchieventDoneByID(959);
                    } else {
                        chr.updateAchievementByID(959, 0, newValue);
                    }
                } else {
                    // [Star Force] I Didn't See That Coming | desc: Fail at Star Force 1,000 times
                    if (!chr.hasAchiementByInfoIdAndMissionID(960)) {
                        chr.addAchievementByID(960, 0, 1, 1);
                    } else {
                        if (!chr.hasAchievementDoneByInfoId(960)) {
                            long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(960, 0) + 1;
                            if (newValue >= 1000) {
                                chr.updateAchievementByID(960, 0, 1000);
                                chr.addAchieventDoneByID(960);
                            } else {
                                chr.updateAchievementByID(960, 0, newValue);
                            }
                        } else {
                            // [Star Force] Magnificent Failure | desc: Fail at Star Force 10,000 times
                            if (!chr.hasAchiementByInfoIdAndMissionID(961)) {
                                chr.addAchievementByID(961, 0, 1, 1);
                            } else {
                                if (!chr.hasAchievementDoneByInfoId(961)) {
                                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(961, 0) + 1;
                                    if (newValue >= 10000) {
                                        chr.updateAchievementByID(961, 0, 10000);
                                        chr.addAchieventDoneByID(961);
                                    } else {
                                        chr.updateAchievementByID(961, 0, newValue);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (cost > 0) {
            // [Star Force] Meso-hungry Hippo | desc: Use 100 million mesos in your pursuit of more Star Force!
            if (!chr.hasAchiementByInfoIdAndMissionID(951)) {
                chr.addAchievementByID(951, 0, 1, cost);
            } else {
                if (!chr.hasAchievementDoneByInfoId(951)) {
                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(951, 0) + cost;
                    if (newValue >= 100000000) {
                        chr.updateAchievementByID(951, 0, 100000000);
                        chr.addAchieventDoneByID(951);
                    } else {
                        chr.updateAchievementByID(951, 0, newValue);
                    }
                } else {
                    // [Star Force] Mesos Everywhere! | desc: Use 10 billion mesos in your pursuit of more Star Force!
                    if (!chr.hasAchiementByInfoIdAndMissionID(952)) {
                        chr.addAchievementByID(952, 0, 1, cost);
                    } else {
                        if (!chr.hasAchievementDoneByInfoId(952)) {
                            long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(952, 0) + cost;
                            if (newValue >= 10000000000L) {
                                chr.updateAchievementByID(952, 0, 10000000000L);
                                chr.addAchieventDoneByID(952);
                            } else {
                                chr.updateAchievementByID(952, 0, newValue);
                            }
                        } else {
                            // [Star Force] No Fear, Just Enhance | desc: Use 1 trillion mesos in your pursuit of more Star Force!
                            if (!chr.hasAchiementByInfoIdAndMissionID(953)) {
                                chr.addAchievementByID(953, 0, 1, cost);
                            } else {
                                if (!chr.hasAchievementDoneByInfoId(953)) {
                                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(953, 0) + cost;
                                    if (newValue >= 1000000000000L) {
                                        chr.updateAchievementByID(953, 0, 1000000000000L);
                                        chr.addAchieventDoneByID(953);
                                    } else {
                                        chr.updateAchievementByID(953, 0, newValue);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (extraChanceFromMiniGame) {
            // [Star Force] Catch the Stars | desc: Successful Star Catching 100 times
            if (!chr.hasAchiementByInfoIdAndMissionID(967)) {
                chr.addAchievementByID(967, 0, 1, 1);
            } else {
                if (!chr.hasAchievementDoneByInfoId(967)) {
                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(967, 0) + 1;
                    if (newValue >= 100) {
                        chr.updateAchievementByID(967, 0, 100);
                        chr.addAchieventDoneByID(967);
                    } else {
                        chr.updateAchievementByID(967, 0, newValue);
                    }
                } else {
                    // [Star Force] I'll Do Anything for You | desc: Successful Star Catching 1,000 Times
                    if (!chr.hasAchiementByInfoIdAndMissionID(968)) {
                        chr.addAchievementByID(968, 0, 1, 1);
                    } else {
                        if (!chr.hasAchievementDoneByInfoId(968)) {
                            long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(968, 0) + 1;
                            if (newValue >= 1000) {
                                chr.updateAchievementByID(968, 0, 1000);
                                chr.addAchieventDoneByID(968);
                            } else {
                                chr.updateAchievementByID(968, 0, newValue);
                            }
                        } else {
                            // [Star Force] Catch Meeee | desc: Successful Star Catching 10,000 Times
                            if (!chr.hasAchiementByInfoIdAndMissionID(969)) {
                                chr.addAchievementByID(969, 0, 1, 1);
                            } else {
                                if (!chr.hasAchievementDoneByInfoId(969)) {
                                    long newValue = chr.getCurrentValueAchiementByInfoIDAndMissionID(969, 0) + 1;
                                    if (newValue >= 10000) {
                                        chr.updateAchievementByID(969, 0, 10000);
                                        chr.addAchieventDoneByID(969);
                                    } else {
                                        chr.updateAchievementByID(969, 0, newValue);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
