package net.swordie.ms.world.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.swordie.ms.connection.OutPacket;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EventListData {

    public String name; // [Challenger]\nArcane Seal
    public String desc; // Undo the Arcane Seals of Challenger World!\nHelp Elwin and Lily obtain Arcane Cores with special powers!
    public String requirements; // Lv. 200+ characters created in a Challenger World\nor Zero characters that completed Story Quest Act 2
    public String period; // Until 4/21/2026 (Tue) 11:59 PM UTC
    public String fromDate; // 202511121400
    public String fromDate2; // 202511121400
    public String toDate; // 202604212359
    public int scriptType; // 2
    public String scriptName; // q102456s
    public int uiType; // 2
    public String uiName; // mysticSeal_UIOpen
    public boolean unkBool1;
    public boolean unkBool2;
    public boolean unkBool3;
    public String icon;
    public String backgrnd;
    public int unkVal;
    public int unkVal2;
    public int unkVal3;
    public String unkStr;
    public int unkVal4;
    public int unkVal5;
    public int levelReq; // 200
    public int[] rewards = new int[]{};
    public boolean unkBool;

    // ---- event-fixes-1005 (optional JSON fields; absent = the defaults below) ----------------------------------
    /** The event's 282 client quest (Quest.wz Check/0 end caps the launcher news period); 0 = none. */
    public int questId;
    /** false hides the entry from the launcher news (facilities such as the sauna); null/true = shown. */
    public Boolean launcherNews;
    /** 282-only fields of the LP 0x0175 entry (research/tms2823/eventlist-1005): i32 after unkVal3, Str where 265
     * had u8 unkVal5, then u8 and i32 after unkBool. Meaning unknown; the traced fixtures send 0 / "" / 0 / 0. */
    public int unkVal282;
    public String unkStr282 = "";
    public boolean unkBool282;
    public int unkInt282;

    public EventListData() {}

    /** Shown in the launcher news unless {@code launcherNews} is false. */
    public boolean showInLauncherNews() {
        return launcherNews == null || launcherNews;
    }

    public EventListData(String name, String desc, String requirements, String period, String fromDate, String toDate,
                         String fromDate2, int scriptType, String scriptName, int uiType, String uiName, boolean unkBool1,
                         boolean unkBool2, boolean unkBool3, int unkVal, int unkVal2, int unkVal3, String unkStr, int unkVal4,
                         int unkVal5, String icon, String backgrnd, int levelReq, int[] rewards, boolean unkBool) {
        this.name = name;
        this.desc = desc;
        this.requirements = requirements;
        this.period = period;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.fromDate2 = fromDate2;
        this.scriptType = scriptType;
        this.scriptName = scriptName;
        this.uiType = uiType;
        this.uiName = uiName;
        this.unkBool1 = unkBool1;
        this.unkBool2 = unkBool2;
        this.unkBool3 = unkBool3;
        this.icon = icon;
        this.backgrnd = backgrnd;
        this.unkVal = unkVal;
        this.unkVal2 = unkVal2;
        this.unkVal3 = unkVal3;
        this.unkStr = unkStr;
        this.unkVal4 = unkVal4;
        this.unkVal5 = unkVal5;
        this.levelReq = levelReq;
        this.rewards = rewards;
        this.unkBool = unkBool;
    }


    public void encode(OutPacket outPacket) {
        outPacket.encodeString(name);
        outPacket.encodeString(desc);
        outPacket.encodeString(requirements);
        outPacket.encodeString(period);
        outPacket.encodeString(fromDate);
        outPacket.encodeString(toDate);
        outPacket.encodeString(fromDate2);
        outPacket.encodeInt(scriptType);
        outPacket.encodeString(scriptName);
        outPacket.encodeInt(uiType);
        outPacket.encodeString(uiName);
        outPacket.encodeByte(unkBool1);
        outPacket.encodeByte(unkBool2);
        outPacket.encodeByte(unkBool3);
        outPacket.encodeInt(unkVal);
        outPacket.encodeInt(unkVal2);
        outPacket.encodeInt(unkVal3);
        outPacket.encodeString(unkStr);
        outPacket.encodeByte(unkVal4);
        outPacket.encodeByte(unkVal5);
        outPacket.encodeString(icon);
        outPacket.encodeString(backgrnd);
        outPacket.encodeInt(levelReq);
        outPacket.encodeInt(0); // size
        outPacket.encodeInt(0); // size
        outPacket.encodeInt(0); // size

        outPacket.encodeInt(0);
        outPacket.encodeByte(false);

        outPacket.encodeInt(0);

        outPacket.encodeInt(0);
        outPacket.encodeByte(false);

        outPacket.encodeInt(rewards == null ? 0 : rewards.length);
        if (rewards != null) {
            for (int rewardID : rewards) {
                outPacket.encodeInt(rewardID);
            }
        }
        outPacket.encodeByte(unkBool);
    }
}
