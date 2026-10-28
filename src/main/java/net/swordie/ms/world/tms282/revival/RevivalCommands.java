package net.swordie.ms.world.tms282.revival;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.commands.AdminCommand;
import net.swordie.ms.client.character.commands.Command;
import net.swordie.ms.connection.packet.Tms282SunnySundayBlock;
import net.swordie.ms.world.event.SunnySunday;

import static net.swordie.ms.enums.AccountType.GameMaster;
import static net.swordie.ms.enums.ChatType.Notice2;

/**
 * GM commands of the revived events (scanned by ChatHandler like AuditCommands):
 * <pre>
 * !revival                 list the events, open / closed, config state
 * !revival reload          re-read data/tms282-revival/revival-events.json and revival-rewards.json now
 * !revival go &lt;key&gt;        enter an event as a player would (ignores the period, keeps the level check)
 * !revival sunday reload   re-read the Sunny Sunday calendar (sunnysunday.tw.json)
 * !revival sunday preview  show a sample Sunny Sunday block on every SetField until "off" (display only, no bonus)
 * !revival sunday off      back to the calendar
 * </pre>
 */
public class RevivalCommands {

    @Command(names = {"revival", "活動復刻"}, requiredType = GameMaster)
    public static class Revival extends AdminCommand {
        public static void execute(Char chr, String[] args) {
            String sub = args.length > 1 ? args[1] : "";
            switch (sub) {
                case "reload" -> {
                    var st = RevivalConfigSource.shared().reload();
                    chr.chatMessage(Notice2, "[復刻活動] 設定 " + st.state() + "：" + st.detail());
                    var rw = net.swordie.ms.world.tms282.revival.rewards.RevivalRewardsSource.shared().reload();
                    chr.chatMessage(Notice2, "[復刻活動] 獎勵 " + rw.state() + "：" + rw.detail());
                }
                case "go" -> {
                    if (args.length < 3) {
                        chr.chatMessage(Notice2, "用法：!revival go <活動代號>");
                        return;
                    }
                    var e = RevivalEvents.find(args[2]).orElse(null);
                    if (e == null) {
                        chr.chatMessage(Notice2, "沒有這個活動：" + args[2]);
                        return;
                    }
                    chr.chatMessage(Notice2, "[復刻活動] " + RevivalEvents.describe(e.enter(RevivalPlayer.of(chr))));
                }
                case "sunday" -> sunday(chr, args.length > 2 ? args[2] : "");
                default -> {
                    chr.chatMessage(Notice2, "[復刻活動] 設定：" + RevivalConfigSource.shared().status().detail());
                    for (RevivalEvent e : RevivalEvents.all()) {
                        var s = e.spec();
                        chr.chatMessage(Notice2, (RevivalEvents.isOpen(s.key()) ? "[開放] " : "[關閉] ") + s.key() + " "
                                + s.title() + (s.entryMap() == 0 ? "" : " 地圖 " + s.entryMap()));
                    }
                }
            }
        }


        private static void sunday(Char chr, String what) {
            switch (what) {
                case "reload" -> {
                    SunnySunday.load();
                    var today = SunnySunday.SunnySundayLogic.get();
                    chr.chatMessage(Notice2, "[星期天楓之谷] 日曆已重新載入；今天：" + (today == null ? "沒有活動" : today.title));
                }
                case "preview" -> {
                    SunnySunday.SunnySundayEntry e = new SunnySunday.SunnySundayEntry();
                    e.uiPath = "UI/UIWindowEvent.img/sundayMaple";
                    e.uiName = "SundayMaple";
                    e.title = "[星期天楓之谷] 預覽";
                    e.desc = "#sunday# #fnArial##fs18##fc0xFFFFD800#+50%#fc0xFFFFFFFF# 怪物公園獲得經驗值！";
                    e.dateText = "#fnArial##fs15##fc0xFFB7EC00#" + RevivalEvents.today().toString().replace('-', '/');
                    e.val1 = 50;
                    e.val2 = 229;
                    Tms282SunnySundayBlock.setSource(() -> e);
                    chr.chatMessage(Notice2, "[星期天楓之谷] 預覽開啟：換地圖後顯示（只有畫面，沒有加成）。!revival sunday off 關閉。");
                }
                case "off" -> {
                    Tms282SunnySundayBlock.setSource(null);
                    chr.chatMessage(Notice2, "[星期天楓之谷] 回到日曆設定。");
                }
                default -> {
                    var now = Tms282SunnySundayBlock.current();
                    chr.chatMessage(Notice2, "用法：!revival sunday reload|preview|off；今天：" + (now == null ? "不顯示" : now.title));
                }
            }
        }
    }
}
