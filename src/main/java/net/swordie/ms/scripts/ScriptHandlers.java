package net.swordie.ms.scripts;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.util.Randomizer;
import net.swordie.ms.util.Util;

public class ScriptHandlers {

    public static boolean handleItemScripts(Char chr, int itemID, int objID, String scriptName, ScriptType scriptType) {
        ScriptManagerImpl sm = chr.getScriptManager();
        int fieldID = chr.getFieldID();
        switch (itemID) {
            case 2431174:
                sm.chat("你獲得了 100 榮譽經驗值。");
                chr.addHonorExp(100);
                sm.consumeItem(itemID);
                sm.playExclSoundWithDownBGM("FarmSE.img/levelUp", 100);
                return true;
            case 2433103:
                sm.chat("你獲得了 1,000 榮譽經驗值。");
                chr.addHonorExp(1000);
                sm.consumeItem(itemID);
                sm.playExclSoundWithDownBGM("FarmSE.img/levelUp", 100);
                return true;
        }
        switch (scriptName) {
            case "141060000_fishing": {
                int bigFish = 4030028;
                int randomInt = Randomizer.nextInt(3);

                if (fieldID != 141060000) {
                    sm.chat("(芭芭拉說她家北邊有個很棒的釣魚地點。)");
                } else {
                    switch (randomInt) {
                        case 0: // Big Fish Caught
                            sm.chat("做得好！航海士，你釣到一條大魚了！");
                            sm.giveItem(bigFish);
                            break;
                        case 1: // Small Fish Caught
                            sm.chat("釣到一條小魚……這條魚對芭芭拉來說恐怕太小了，把牠放生吧。");
                            break;
                        case 2: // Nothing Caught
                            sm.chat("什麼都沒釣到……真可惜，再試一次吧！");
                            break;
                    }
                }
                return true;
            }
            case "cash_5130000": {
                chr.consumeItem(5130000, 1);
                sm.chat("已使用護身符，經驗值不會減少。");
                return true;
            }
            case "cash_5680015": {
                // Fatigue Reset Drink | Using this drink will reset your fatigue to 0%. You can only use it when you have more than 0% fatigue.
                if (chr.getStat(Stat.fatigue) > 0) {
                    chr.setStatAndSendPacket(Stat.fatigue, 0);
                    sm.chatScript("疲勞度已重置為 0%。");
                } else {
                    sm.chat("疲勞度大於 0% 時才能使用。");
                }
                return true;
            }
            case "cash_5680260": {
                if (sm.canHold(5000605) && sm.canHold(2120000)) {
                    sm.giveItem(5000605);
                    sm.giveItem(2120000, 1000);
                    sm.consumeItem(5680260);
                } else {
                    sm.chat("請在背包中空出空間。");
                }
                return true;
            }
            case "cash_5680330": {
                if (sm.canHold(4001832)) {
                    sm.giveItem(4001832, 500);
                    sm.consumeItem(5680330);
                } else {
                    sm.chat("請確認背包有足夠的空格！");
                }
                return true;
            }
            case "cash_5680382": {
                // 5680382 - Epic Potential Scroll 50% Coupon
                sm.giveItem(2049708, 1);
                sm.consumeItem(itemID);
                return true;
            }
            case "cash_5680405": {
                sm.giveMesos(10000000);
                sm.consumeItem(itemID);
                return true;
            }
            case "cash_5680406": {
                sm.giveMesos(55000000);
                sm.consumeItem(itemID);
                return true;
            }
            case "cash_5680407": {
                sm.giveMesos(120000000);
                sm.consumeItem(itemID);
                return true;
            }
            case "cash_5680408": {
                sm.giveMesos(390000000);
                sm.consumeItem(itemID);
                return true;
            }
            case "cash_5680409": {
                chr.addMaplePoint(3000);
                sm.consumeItem(itemID);
                return true;
            }
            case "cash_5680410": {
                chr.addMaplePoint(5000);
                sm.consumeItem(itemID);
                return true;
            }
            case "cash_5680411": {
                chr.addMaplePoint(7000);
                sm.consumeItem(itemID);
                return true;
            }
            case "cash_5680412": {
                chr.addMaplePoint(10000);
                sm.consumeItem(itemID);
                return true;
            }
            case "consume_2432493": {
                int amount = Util.getRandom(10, 50);
                sm.chat(String.format("你獲得了 %d 榮譽經驗值。", amount));
                chr.addHonorExp(amount);
                sm.consumeItem(itemID);
                sm.playExclSoundWithDownBGM("FarmSE.img/levelUp", 100);
                return true;
            }
            case "consume_2432494": {
                int amount = Util.getRandom(50, 300);
                sm.chat(String.format("你獲得了 %d 榮譽經驗值。", amount));
                chr.addHonorExp(amount);
                sm.consumeItem(itemID);
                sm.playExclSoundWithDownBGM("FarmSE.img/levelUp", 100);
                return true;
            }
            case "consume_2630389": {
                sm.chat("你獲得了 10 榮譽經驗值。");
                chr.addHonorExp(10);
                sm.consumeItem(itemID);
                sm.playExclSoundWithDownBGM("FarmSE.img/levelUp", 100);
                return true;
            }
            case "consume_2431174": {
                sm.chat("你獲得了 100 榮譽經驗值。");
                chr.addHonorExp(100);
                sm.consumeItem(itemID);
                sm.playExclSoundWithDownBGM("FarmSE.img/levelUp", 100);
                return true;
            }
            case "consume_2434637": {
                sm.chat("你獲得了 500 榮譽經驗值。");
                chr.addHonorExp(500);
                sm.consumeItem(itemID);
                sm.playExclSoundWithDownBGM("FarmSE.img/levelUp", 100);
                return true;
            }
            case "consume_2433103":
            case "consume_2434638": {
                sm.chat("你獲得了 1,000 榮譽經驗值。");
                chr.addHonorExp(1000);
                sm.consumeItem(itemID);
                sm.playExclSoundWithDownBGM("FarmSE.img/levelUp", 100);
                return true;
            }
            case "consume_2432495":
            case "consume_2434639": {
                sm.chat("你獲得了 2,000 榮譽經驗值。");
                chr.addHonorExp(2000);
                sm.consumeItem(itemID);
                sm.playExclSoundWithDownBGM("FarmSE.img/levelUp", 100);
                return true;
            }
            case "consume_2432970":
            case "consume_2433808":
            case "consume_2434021":
            case "consume_2434288":
            case "consume_2434290":
            {
                sm.chat("你獲得了 10,000 榮譽經驗值。");
                chr.addHonorExp(10000);
                sm.consumeItem(itemID);
                sm.playExclSoundWithDownBGM("FarmSE.img/levelUp", 100);
                return true;
            }
            case "consume_2435856":
            case "consume_2435857":
            case "consume_2435858":
            case "consume_2435859":
            case "consume_2435860":
            case "consume_2435861":
            case "consume_2435862":
            case "consume_2435863":
            case "consume_2435864":
            case "consume_2435865":
            case "consume_2435866":
            case "consume_2435867":
            case "consume_2435868":
            case "consume_2435869":
            case "consume_2435870":
            case "consume_2435871":
            case "consume_2435872":
            {
                chr.addChuChuRecipe(itemID);
                sm.consumeItem(itemID);
                return true;
            }
            case "consume_2434775":
            {
                sm.consumeItem(itemID);
                sm.incCharismaEXP((short) 93596);
                return true;
            }
            case "consume_2434776":
            {
                sm.consumeItem(itemID);
                sm.incInsightEXP((short) 93596);
                return true;
            }
            case "consume_2434777":
            {
                sm.consumeItem(itemID);
                sm.incWillEXP((short) 93596);
                return true;
            }
            case "consume_2434778":
            {
                sm.consumeItem(itemID);
                sm.incCraftEXP((short) 93596);
                return true;
            }
            case "consume_2434779":
            {
                sm.consumeItem(itemID);
                sm.incSenseEXP((short) 93596);
                return true;
            }
            case "consume_2434780":
            {
                sm.consumeItem(itemID);
                sm.incCharmEXP((short) 93596);
                return true;
            }
            case "consume_2436614":
            {
                sm.giveMesos(Util.getRandom(100000,3000000));
                sm.consumeItem(itemID);
                return true;
            }
            case "consume_2436615":
            {
                sm.giveMesos(Util.getRandom(500000,5000000));
                sm.consumeItem(itemID);
                return true;
            }
            case "consume_2436616":
            {
                sm.giveMesos(Util.getRandom(1000000,10000000));
                sm.consumeItem(itemID);
                return true;
            }
        }
        return false;
    }
}
