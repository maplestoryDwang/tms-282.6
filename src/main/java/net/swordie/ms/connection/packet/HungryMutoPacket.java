package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.world.partyquest.HungryMutoRecipes;

import java.util.List;

public class HungryMutoPacket {

    public static OutPacket unk() {
        OutPacket outPacket = new OutPacket(OutHeader.HUGNRY_MUTO_UNK);

        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket setTime(int time) {
        OutPacket outPacket = new OutPacket(OutHeader.HUGNRY_MUTO_RESULT);

        outPacket.encodeInt(1);
        outPacket.encodeInt(time);

        return outPacket;
    }

    public static OutPacket finish() {
        OutPacket outPacket = new OutPacket(OutHeader.HUGNRY_MUTO_RESULT);

        outPacket.encodeInt(2);

        return outPacket;
    }

    public static OutPacket setNewRecipe(int[] recipe, List<HungryMutoRecipes> hungryMutoRecipesList) {
        OutPacket outPacket = new OutPacket(OutHeader.HUGNRY_MUTO_RESULT);

        outPacket.encodeInt(3);
        outPacket.encodeInt(recipe[0]);
        outPacket.encodeInt(recipe[1]);
        outPacket.encodeInt(recipe[2]);
        outPacket.encodeInt(recipe[3]);
        outPacket.encodeInt(recipe[4]);
        outPacket.encodeInt(hungryMutoRecipesList.size());
        for (HungryMutoRecipes hmr : hungryMutoRecipesList) {
            if (hmr.isRecipeHidden()) {
                outPacket.encodeInt(0);
            } else {
                outPacket.encodeInt(hmr.getRecipeItem());
            }
            outPacket.encodeInt(hmr.getRecipeReq());
            outPacket.encodeInt(hmr.getRecipeCount());
        }

        return outPacket;
    }

    public static OutPacket setRecipe(int[] recipe, List<HungryMutoRecipes> hungryMutoRecipesList, Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.HUGNRY_MUTO_RESULT);

        outPacket.encodeInt(4);
        outPacket.encodeInt(recipe[0]);
        outPacket.encodeInt(recipe[1]);
        outPacket.encodeInt(recipe[2]);
        outPacket.encodeInt(hungryMutoRecipesList.size());
        for (HungryMutoRecipes hmr : hungryMutoRecipesList) {
            outPacket.encodeInt(hmr.getRecipeItem());
            outPacket.encodeInt(hmr.getRecipeReq());
            outPacket.encodeInt(hmr.getRecipeCount());
        }

        return outPacket;
    }

    public static OutPacket addItem(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.HUGNRY_MUTO_RESULT);

        outPacket.encodeInt(5);
        outPacket.encodeInt(1);
        outPacket.encodeInt(chr.getId());
        outPacket.encodeInt(chr.getRecipe().getLeft());
        outPacket.encodeInt(chr.getRecipe().getRight());

        return outPacket;
    }

}
