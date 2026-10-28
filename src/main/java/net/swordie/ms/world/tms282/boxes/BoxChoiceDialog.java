package net.swordie.ms.world.tms282.boxes;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.scripts.ScriptInfo;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.scripts.ScriptType;

import javax.script.SimpleBindings;
import java.util.List;

/**
 * Selection menu of a choice box ("擇一" / 選擇券), the way the item scripts do it: NPC 9010000 speaks, an AskMenu
 * lists {@code #L i##v id# #t id##l}, the chosen item is given and the box consumed. Runs as an Item "script" of the
 * character's ScriptManagerImpl without a script file: an active {@link ScriptInfo} of {@link ScriptType#Item} so the
 * CP012C answer reaches it ({@code handleAction} on the last active type), and a virtual thread that waits for the
 * answer like a script thread. Off the request thread, its 265 inventory / meso packets reach the 282 client through
 * the profile's translators (Tms282InventoryOperationEncoder), as for any item script. Closing the menu consumes
 * nothing.
 */
final class BoxChoiceDialog {
    static final int SPEAKER = 9010000;
    static final String SCRIPT_NAME = "tms282_box_choice";

    /** One option: {@code count} x {@code itemId}, {@code periodDays} 0 = permanent. */
    record Option(int itemId, int count, int periodDays) {
    }

    private BoxChoiceDialog() {
    }

    static String menuText(List<Option> options) {
        StringBuilder text = new StringBuilder("請選擇想要獲得的道具：\r\n");
        for (int i = 0; i < options.size(); i++) {
            Option o = options.get(i);
            text.append("#L").append(i).append("##v").append(o.itemId()).append("# #b#t").append(o.itemId()).append("##k");
            if (o.count() > 1) {
                text.append(" x").append(o.count());
            }
            if (o.periodDays() > 0) {
                text.append(" (").append(o.periodDays()).append("天)");
            }
            text.append("#l\r\n");
        }
        return text.toString();
    }

    /** @return false (nothing started) when another dialog is open. */
    static boolean open(Char chr, Item box, List<Option> options) {
        ScriptManagerImpl sm = chr.getScriptManager();
        synchronized (sm) {
            if (sm.getLastActiveScriptType() != null && sm.getLastActiveScriptType() != ScriptType.None
                    && sm.isActive(sm.getLastActiveScriptType())) {
                chr.chatMessage(ChatType.SystemNotice, "請先結束目前的對話，未消耗此道具。");
                return false;
            }
            ScriptInfo si = new ScriptInfo(ScriptType.Item, new SimpleBindings(), box.getItemId(), SCRIPT_NAME);
            si.setActive(true);
            sm.getScripts().put(ScriptType.Item, si);
            sm.setLastActiveScriptType(ScriptType.Item);
            sm.setLastActiveScriptName(SCRIPT_NAME);
        }
        List<Option> frozen = List.copyOf(options);
        Thread.ofVirtual().name("tms282-box-choice-" + chr.getId()).start(() -> run(chr, sm, box, frozen));
        return true;
    }

    private static void run(Char chr, ScriptManagerImpl sm, Item box, List<Option> options) {
        try {
            sm.setSpeakerID(SPEAKER);
            int sel = sm.askMenu(menuText(options));
            if (sel < 0 || sel >= options.size()) {
                return;
            }
            Option o = options.get(sel);
            Item reward = BoxSupport.createItem(o.itemId(), o.count(), o.periodDays());
            if (reward == null) {
                BoxSupport.refuseNoData(chr);
                return;
            }
            BoxSupport.giveAndConsume(chr, box, List.of(reward), 0);
        } catch (NullPointerException closed) {
            // the menu was closed (ScriptManagerImpl stops the script with its intended NPE): nothing consumed
        } catch (RuntimeException e) {
            System.out.printf("[TMS282][Boxes] choice box %d failed for %s: %s%n", box.getItemId(), chr.getName(), e);
            chr.chatMessage(ChatType.SystemNotice, "道具使用失敗，請稍後再試。");
        } finally {
            sm.stop(ScriptType.Item);
            chr.dispose();
        }
    }
}
