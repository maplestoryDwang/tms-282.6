package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.Tms282StatRecoveryService;
import net.swordie.ms.client.character.keys.Tms282KeyMapService;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.quest.Tms282QuestService;

/**
 * Persistence of the TMS 282.3 in-field requests (strategy; {@link #SQL} is the server's, checks pass an in-memory
 * one). Methods may throw {@link RuntimeException}; the callers keep in-memory quest/key/stat changes for the
 * session. Macro replacement is transactional and publishes memory only after commit.
 */
public interface Tms282FieldStore {
    /** CP01A6: {@link Tms282QuestService#save}; true when a row was inserted. */
    boolean saveQuest(Char chr, Tms282QuestService.Started started);

    /**
     * CP01A6 complete / auto-complete: the quest row as it is now (265 {@link Quest#saveToSQL}: UPDATE by row ID,
     * INSERT when the ID is still 0). Stores that do not persist quests keep the default, which refuses; the caller
     * then keeps the change in memory (the 265 character save at disconnect writes it).
     */
    default void saveQuestState(Char chr, Quest quest) {
        throw new UnsupportedOperationException("this store does not write quest rows");
    }

    /** CP01A6 resign: removes the quest row (265 {@link Quest#deleteFromSQL}); the default refuses, as above. */
    default void deleteQuest(Char chr, Quest quest) {
        throw new UnsupportedOperationException("this store does not write quest rows");
    }

    /** CP020F type 0: {@link Tms282KeyMapService#save}. */
    Tms282KeyMapService.Saved saveKeys(Tms282KeyMapService.Applied applied);

    /** CP01B0: atomic replacement; the caller changes memory only after this succeeds. */
    default void saveMacros(Char chr, java.util.List<net.swordie.ms.client.character.Macro> macros) {
        throw new UnsupportedOperationException("this store does not write macros");
    }

    /** CP0193 periodic write: hp and mp only ({@link Tms282StatRecoveryService#saveHpMp}). */
    void saveHpMp(Char chr);

    /** The 265 database through the Tms282*Service methods (the probe's writes). */
    Tms282FieldStore SQL = new Tms282FieldStore() {
        @Override
        public boolean saveQuest(Char chr, Tms282QuestService.Started started) {
            return Tms282QuestService.save(chr, started);
        }

        @Override
        public void saveQuestState(Char chr, Quest quest) {
            if (net.swordie.ms.constants.QuestConstants.isAccountQuest(quest.getQRKey())) {
                var shared = chr.getAccount().getQuestById(quest.getQRKey());
                net.swordie.ms.client.AccountQuest.saveToSQL(java.util.Map.of(shared.getQRKey(), shared), chr.getAccount().getId());
            } else Quest.saveToSQL(java.util.Map.of(quest.getQRKey(), quest), chr.getId());
        }

        @Override
        public void deleteQuest(Char chr, Quest quest) {
            quest.deleteFromSQL();
        }

        @Override
        public Tms282KeyMapService.Saved saveKeys(Tms282KeyMapService.Applied applied) {
            return Tms282KeyMapService.save(applied);
        }

        @Override
        public void saveMacros(Char chr, java.util.List<net.swordie.ms.client.character.Macro> macros) {
            net.swordie.ms.client.character.keys.Tms282MacroService.save(chr, macros);
        }

        @Override
        public void saveHpMp(Char chr) {
            Tms282StatRecoveryService.saveHpMp(chr);
        }
    };
}
