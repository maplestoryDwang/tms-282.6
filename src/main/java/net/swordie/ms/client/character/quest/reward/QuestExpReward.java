package net.swordie.ms.client.character.quest.reward;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.ItemBuffs;
import net.swordie.ms.loaders.DatSerializable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created on 3/2/2018.
 */
public class QuestExpReward implements QuestReward {

    private long exp;
    private String expTable;

    public QuestExpReward(long exp, String expTable) {
        this.exp = exp;
        this.expTable = expTable;
    }

    public QuestExpReward() {}

    public long getExp() {
        return exp;
    }

    public String getExpTable() {
        return expTable;
    }

    @Override
    public void giveReward(Char chr) {
        long expGain = calculateExpTable(chr, getExp()) * ServerConfig.QUEST_EXP_RATE;
        chr.addExp(expGain, false);
    }

    @Override
    public void giveReward(Account account, String questName) {
        Char currentChr = account.getCurrentChr();
        if (currentChr != null) {
            long expGain = calculateExpTable(currentChr, getExp()) * ServerConfig.QUEST_EXP_RATE;
            currentChr.addExp(expGain, false);
            for (Char chr : account.getCharacters()) {
                if (chr.getId() != currentChr.getId()) {
                    if (chr.getRewardSystem() == null) {
                        chr.initRewardSystem();
                    }
                    chr.sendRewardToChar(0, 1, expGain, "角色 " + currentChr.getName() + " 已完成任務 " + questName + "。", 30);
                }
            }
        }
    }

    private long calculateExpTable(Char chr, long expGain) {
        if (getExpTable().equalsIgnoreCase("3060theme")) {
            switch (chr.getLevel()) {
                case 30: expGain = 3770; break;
                case 31: expGain = 3700; break;
                case 32: expGain = 3630; break;
                case 33: expGain = 3574; break;
                case 34: expGain = 3518; break;
                case 35: expGain = 4404; break;
                case 36: expGain = 5480; break;
                case 37: expGain = 6814; break;
                case 38: expGain = 8409; break;
                case 39: expGain = 10320; break;
                case 40: expGain = 11229; break;
                case 41: expGain = 12200; break;
                case 42: expGain = 13240; break;
                case 43: expGain = 14396; break;
                case 44: expGain = 15633; break;
                case 45: expGain = 16988; break;
                case 46: expGain = 18428; break;
                case 47: expGain = 20001; break;
                case 48: expGain = 21692; break;
                case 49: expGain = 23550; break;
                case 50: expGain = 25534; break;
                case 51: expGain = 27667; break;
                case 52: expGain = 29964; break;
                case 53: expGain = 32466; break;
                case 54: expGain = 35175; break;
                case 55: expGain = 38108; break;
                case 56: expGain = 41226; break;
                case 57: expGain = 44618; break;
                case 58: expGain = 48292; break;
                case 59: expGain = 52270; break;
                default: {
                    if (chr.getLevel() < 30) {
                        expGain = 0;
                    } else if (chr.getLevel() > 59) {
                        expGain = 52270;
                    }
                    break;
                }
            }
        }
        else if (getExpTable().equalsIgnoreCase("3060theme2")) {
            switch (chr.getLevel()) {
                case 30: expGain = 8548; break;
                case 31: expGain = 8478; break;
                case 32: expGain = 8408; break;
                case 33: expGain = 8352; break;
                case 34: expGain = 8296; break;
                case 35: expGain = 10137; break;
                case 36: expGain = 12360; break;
                case 37: expGain = 15070; break;
                case 38: expGain = 18316; break;
                case 39: expGain = 22208; break;
                case 40: expGain = 24068; break;
                case 41: expGain = 26066; break;
                case 42: expGain = 28215; break;
                case 43: expGain = 30569; break;
                case 44: expGain = 33100; break;
                case 45: expGain = 35853; break;
                case 46: expGain = 38801; break;
                case 47: expGain = 42004; break;
                case 48: expGain = 45455; break;
                case 49: expGain = 49215; break;
                case 50: expGain = 53251; break;
                case 51: expGain = 57602; break;
                case 52: expGain = 62293; break;
                case 53: expGain = 67382; break;
                case 54: expGain = 72884; break;
                case 55: expGain = 78833; break;
                case 56: expGain = 85209; break;
                case 57: expGain = 92120; break;
                case 58: expGain = 99594; break;
                case 59: expGain = 107676; break;
                default: {
                    if (chr.getLevel() < 30) {
                        expGain = 0;
                    } else if (chr.getLevel() > 59) {
                        expGain = 107676;
                    }
                    break;
                }
            }
        }
        return expGain;
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeLong(getExp());
        dos.writeUTF(getExpTable());
    }

    @Override
    public DatSerializable load(DataInputStream dis) throws IOException {
        return new QuestExpReward(dis.readLong(), dis.readUTF());
    }
}
