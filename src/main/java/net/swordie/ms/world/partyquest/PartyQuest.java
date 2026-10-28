package net.swordie.ms.world.partyquest;

import net.swordie.ms.client.character.Char;

public interface PartyQuest {

    void start();

    void exit();

    void end(Char chr);

    void clear(Char chr);

    void event();

    void leftOrDisband(Char chr);
}
