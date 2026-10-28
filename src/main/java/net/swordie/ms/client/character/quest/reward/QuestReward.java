package net.swordie.ms.client.character.quest.reward;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.loaders.DatSerializable;

public interface QuestReward extends DatSerializable {

    void giveReward(Char chr);

    void giveReward(Account account, String questName);
}
