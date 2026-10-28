package net.swordie.ms.world.outfit;

import java.util.List;

/**
 * One shared outfit: the visible look of a character at the moment it was shared.
 *
 * @param items         visible equips (AvatarLook hairEquips: the cash layer where one covers a normal equip)
 * @param weaponSticker the cash weapon cover, 0 when none
 * @param weapon        the weapon it was shown with (pose only; never bought or worn through the outfit)
 */
public record Outfit(int id, int userId, int charId, String charName, String name, int gender, int skin, int face,
                     int hair, int mixColor, int mixPercent, int weaponSticker, int weapon, boolean elfEar,
                     List<Integer> items, int likes, long createdAt) {

    public Outfit {
        items = List.copyOf(items);
    }

    public Outfit withId(int id) {
        return new Outfit(id, userId, charId, charName, name, gender, skin, face, hair, mixColor, mixPercent,
                weaponSticker, weapon, elfEar, items, likes, createdAt);
    }
}
