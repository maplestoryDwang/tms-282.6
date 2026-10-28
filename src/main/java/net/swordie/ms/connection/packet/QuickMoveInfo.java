package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.QuickMoveType;
import net.swordie.ms.util.FileTime;

public class QuickMoveInfo {
    private int qmiID;
    private int templateID;
    private int reqQuest;
    private QuickMoveType code;
    private int levelMin;
    private String msg;
    private String script;
    private String title;
    private String desc;
    private FileTime start;
    private FileTime end;
    private boolean noInstances;

    public QuickMoveInfo(QuickMoveType code, int levelMin, String msg, String script, String title, String desc, int templateID, int reqQuest, boolean noInstances, FileTime start, FileTime end) {
        this.code = code;
        this.msg = msg;
        this.levelMin = levelMin;
        this.script = script;
        this.title = title;
        this.desc = desc;
        this.templateID = templateID;
        this.reqQuest = reqQuest;
        this.noInstances = noInstances;
        this.start = start;
        this.end = end;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getCode().getVal());
        outPacket.encodeInt(getLevelMin());
        outPacket.encodeString(getMsg());
        outPacket.encodeInt(getReqQuest());
        outPacket.encodeString(getScript());
        outPacket.encodeString(getTitle());
        outPacket.encodeInt(getTemplateID()); // 105
        outPacket.encodeInt(-1);
        outPacket.encodeString("");
        outPacket.encodeString(getDesc());
        outPacket.encodeString("");
        outPacket.encodeFT(getStart());
        outPacket.encodeFT(getEnd());
    }

    public int getQmiID() {
        return qmiID;
    }

    public void setQmiID(int qmiID) {
        this.qmiID = qmiID;
    }

    public int getTemplateID() {
        return templateID;
    }

    public void setTemplateID(int templateID) {
        this.templateID = templateID;
    }

    public QuickMoveType getCode() {
        return code;
    }

    public void setCode(QuickMoveType code) {
        this.code = code;
    }

    public int getLevelMin() {
        return levelMin;
    }

    public void setLevelMin(int levelMin) {
        this.levelMin = levelMin;
    }

    public boolean isNoInstances() {
        return noInstances;
    }

    public void setNoInstances(boolean noInstances) {
        this.noInstances = noInstances;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public FileTime getStart() {
        return start;
    }

    public void setStart(FileTime start) {
        this.start = start;
    }

    public FileTime getEnd() {
        return end;
    }

    public void setEnd(FileTime end) {
        this.end = end;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getReqQuest() {
        return reqQuest;
    }

    public void setReqQuest(int reqQuest) {
        this.reqQuest = reqQuest;
    }
}
