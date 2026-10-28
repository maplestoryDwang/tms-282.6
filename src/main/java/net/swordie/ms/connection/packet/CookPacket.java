package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.List;

public class CookPacket {

    public static OutPacket setRecipes(List<Integer> recipes) {
        OutPacket outPacket = new OutPacket(OutHeader.COOK_SET_RECIPES);

        outPacket.encodeInt(recipes.size());
        for (Integer recipeID : recipes) {
            outPacket.encodeInt(recipeID);
        }

        return outPacket;
    }
}
