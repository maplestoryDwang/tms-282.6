package net.swordie.ms.loaders;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.items.BodyPart;
import net.swordie.ms.connection.packet.Tms282BodySlots;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.life.pet.Pet;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Compatibility extracted from the 282 client's PetEquip image root keys. */
public final class Tms282PetEquipment {
    private Tms282PetEquipment() { }
    private static final class Data {
        static final Map<Integer, Set<Integer>> ALLOWED = load();
        private static Map<Integer, Set<Integer>> load() {
            Map<Integer, Set<Integer>> result = new HashMap<>();
            try {
                for (String line : Files.readAllLines(Path.of("data/resources/tms282/pet-equipment.tsv"))) {
                    if (line.isBlank()) continue;
                    String[] pair = line.split("\t", -1);
                    Set<Integer> pets = new HashSet<>();
                    for (String id : pair[1].split(",")) if (!id.isEmpty()) pets.add(Integer.parseInt(id));
                    if (result.put(Integer.parseInt(pair[0]), Set.copyOf(pets)) != null)
                        throw new IllegalArgumentException("Duplicate pet equipment " + pair[0]);
                }
                return Map.copyOf(result);
            } catch (IOException e) { throw new IllegalStateException("Missing 282 pet equipment metadata", e); }
        }
    }

    /**
     * Pet index (0..2) of a pet equip slot, sign ignored, else -1. 282: 114 / 124 / 126 (client cash 14 / 24 / 26,
     * 79E970; equips README WI-3). 265: PetAcc1..3 114 / 130 / 138.
     */
    public static int petIndex(int slot) {
        if (Protocol.active().version() == ProtocolVersion.TMS_282_3) {
            return Tms282BodySlots.petWearIndex(slot);
        }
        slot = Math.abs(slot);
        if (slot == BodyPart.PetAcc1.getVal()) return 0;
        if (slot == BodyPart.PetAcc2.getVal()) return 1;
        if (slot == BodyPart.PetAcc3.getVal()) return 2;
        return -1;
    }

    /** The 282 worn slot (positive, server numbering) of pet {@code index}'s pet equip: 114 / 124 / 126. */
    public static int petWearSlot(int index) {
        return index >= 0 && index < Tms282BodySlots.PET_WEAR.length ? Tms282BodySlots.PET_WEAR[index] : -1;
    }

    public static boolean compatible(int itemId, int petId) {
        return Data.ALLOWED.getOrDefault(itemId, Set.of()).contains(petId);
    }

    public static boolean canWear(Char chr, Item item, int slot) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3) return true;
        int itemId = item.getItemId();
        // pet name-tag rings (1802100, 182xxxx) only go to 123 / 125 / 127 (client 23 / 25 / 27), nothing else does;
        // the client decides whether the pet must be out (not in 79E970), so no pet check here
        boolean nameTag = Tms282BodySlots.isPetNameTag(itemId);
        int tagIndex = Tms282BodySlots.petNameTagIndex(slot);
        if (nameTag || tagIndex >= 0) {
            return nameTag && tagIndex >= 0;
        }
        int index = petIndex(slot);
        boolean petEquip = itemId / 10000 == 180;
        if (index < 0) return !petEquip;
        if (!petEquip) return false;
        Pet pet = chr.getPetByIdx(index);
        return pet != null && pet.getItem().canSummon() && compatible(itemId, pet.getTemplateId());
    }
}
