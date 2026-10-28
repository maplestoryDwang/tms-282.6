package net.swordie.ms.world.tms282.mail;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.util.DataPrinter;

/**
 * Field-entry hook: announces unread memos and collectable parcels. Call once per login (first field entry) from
 * the 282 field-entry path; safe to call on every field change (it only reads). Never throws.
 */
public final class MailLoginHook {
    private MailLoginHook() { }

    public static void onFirstFieldEntry(Char chr) {
        if (!net.swordie.ms.scripts.ScriptManagerImpl.GAME_SCRIPTS_ENABLED) return;
        if (chr == null) return;
        try {
            MemoService.get().onEnterField(chr);
            ParcelService.get().onEnterField(chr);
            // P7: launcher gift codes waiting for this character (launcher_reward_queue)
            net.swordie.ms.connection.api.launcher.gift.LauncherRewardDelivery.onFirstFieldEntry(chr);
        } catch (RuntimeException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        // step 7: an unfinished newbie flow opens its dialog again (lobby, or anywhere with resumeOnLogin)
        net.swordie.ms.world.tms282.newbie.NewbieFlowService.get().onLoginEntry(chr);
    }
}
