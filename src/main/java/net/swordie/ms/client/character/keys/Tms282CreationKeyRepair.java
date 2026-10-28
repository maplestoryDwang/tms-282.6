package net.swordie.ms.client.character.keys;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.Tms282FuncKeyModifiedRequest.KeyChange;
import net.swordie.ms.connection.packet.Tms282FuncKeyModifiedRequest.KeyChanges;
import net.swordie.ms.constants.GameConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Explicit one-character repair planning only. No automatic login hook, mutation or persistence. */
public final class Tms282CreationKeyRepair {
    private Tms282CreationKeyRepair() {}
    public record Target(int userId, int accountId, int worldId, int characterId, int nativeType) {}
    public record Plan(Target target, List<KeyChanges> presets, List<Integer> quickSlots) {
        public Plan { presets = List.copyOf(presets); quickSlots = List.copyOf(quickSlots); }
    }

    /** Recheck against freshly loaded state before applying through Tms282KeyMapService.apply/save. */
    public static Optional<Plan> plan(Char chr, Target target) {
        if (chr == null || target.nativeType() != 1 || chr.getId() != target.characterId()
                || chr.getAccId() != target.accountId() || chr.getUser() == null
                || chr.getUser().getId() != target.userId() || chr.getAccount() == null
                || chr.getAccount().getId() != target.accountId() || chr.getAccount().getWorldId() != target.worldId()
                || chr.getAccount().getOwnerUserId() != target.userId() || chr.getFuncKeyMaps() == null
                || !chr.getFuncKeyMaps().keySet().equals(Set.of(0, 1, 2))
                || !Arrays.asList(GameConstants.QuickSlot_adv).equals(chr.getQuickslotKeys())) return Optional.empty();
        List<KeyChanges> presets = new ArrayList<>();
        for (int preset = 0; preset < 3; preset++) {
            FuncKeyMap current = chr.getFuncKeyMapByPreset(preset);
            FuncKeyMap wrong = FuncKeyMap.getDefaultMapping(chr.getId(), preset, 1);
            if (current == null || current.getKeymap() == null || current.getPreset() != preset
                    || current.getCharID() != chr.getId()) return Optional.empty();
            Set<Integer> indices = new HashSet<>();
            for (Keymapping binding : current.getKeymap()) {
                if (binding == null || !indices.add(binding.getIndex()) || binding.getIndex() < 0
                        || binding.getIndex() >= 89 || !same(binding, wrong.getMappingAt(binding.getIndex())))
                    return Optional.empty();
            }
            FuncKeyMap correct = Tms282CreationKeys.mapping(chr.getId(), preset, target.nativeType());
            List<KeyChange> changes = new ArrayList<>();
            for (int index = 0; index < 89; index++) {
                Keymapping old = current.getMappingAt(index), replacement = correct.getMappingAt(index);
                if (!same(old, wrong.getMappingAt(index))) return Optional.empty();
                if (!same(old, replacement)) changes.add(new KeyChange(index,
                        replacement == null ? 0 : replacement.getType(), replacement == null ? 0 : replacement.getVal()));
            }
            presets.add(new KeyChanges(preset, 0, changes));
        }
        return Optional.of(new Plan(target, presets, Tms282CreationKeys.quickSlots(target.nativeType())));
    }

    private static boolean same(Keymapping a, Keymapping b) {
        return (a == null ? 0 : a.getType()) == (b == null ? 0 : b.getType())
                && (a == null ? 0 : a.getVal()) == (b == null ? 0 : b.getVal());
    }
}
