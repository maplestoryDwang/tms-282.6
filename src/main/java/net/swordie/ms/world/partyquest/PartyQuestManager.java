package net.swordie.ms.world.partyquest;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.enums.social.Party.PartyQuestType;

public class PartyQuestManager {

    private PartyQuest partyQuest = null;

    public PartyQuest getPartyQuest() {
        return partyQuest;
    }

    public void setPartyQuest(PartyQuest partyQuest) {
        this.partyQuest = partyQuest;
    }

    private PartyQuest getPartyQuestByType(Char chr, PartyQuestType partyQuestType) {
        return switch (partyQuestType) {
            case MOON_BUNNY -> new MoonBunny(chr);
            case FIRST_TIME_TOGETHER -> new FirstTimeTogether(chr);
            case TANGYOON_COOKING -> new TangyoonCooking(chr);
            case DIMENSION_INVASION -> new DimensionInvasion(chr);
            case NETT_PYRAMID -> new DefenseEvent(chr);
            case ESCAPE -> new Escape(chr);
            case HUNGRY_MUTO -> new HungryMuto(chr);
            case XERXES_CHRYSE -> new XerxesInChryse(chr);
            case LORD_PIRATE -> new LordPirate(chr);
            case DRAGON_RIDER -> new DragonRider(chr);
            case KENTA_IN_DANGER -> new KentaInDanger(chr);
            case ROMEO -> new Romeo(chr);
            case JULIET -> new Juliet(chr);
            case ALIEN_VISITOR -> new AlienVisitor(chr);
        };
    }

    public void start(Char chr, PartyQuestType partyQuestType, byte typeEnter) {
        if (this.partyQuest == null) {
            this.partyQuest = getPartyQuestByType(chr, partyQuestType);
        }
        switch (typeEnter) {
            case 0 -> {
                if (partyQuest != null) {
                    partyQuest.start();
                }
            }
            case 1 -> {
                if (partyQuest != null) {
                    partyQuest.exit();
                }
            }
            case 2 -> {
                if (partyQuest != null) {
                    partyQuest.event();
                }
            }
        }
    }
}
