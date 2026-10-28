package net.swordie.ms.life.npc;

import net.swordie.ms.connection.OutPacket;

public class NpcScriptInfo {
    private int objectID;
    private int[] options;
    private byte speakerType = 4; // ?
    private int overrideSpeakerTemplateID;
    private short param;
    private byte color;
    private String text;
    private NpcMessageType messageType;
    private String[] images;
    private int min;
    private int max;
    private String defaultText;
    private int defaultNumber;
    private byte type;
    private int time;
    private String title;
    private String problemText;
    private String hintText;
    private int quizType;
    private int answer;
    private int correctAnswers;
    private int remaining;
    private boolean angelicBuster;
    private boolean zeroBeta;
    private int dlgType;
    private int defaultSelect;
    private String[] selectText;
    private int templateID;
    private int faceIndex;
    private int index;
    private boolean isLeft;
    private String androidName;
    private int[] androids;

    public NpcScriptInfo deepCopy() {
        NpcScriptInfo nsi = new NpcScriptInfo();
        if (options != null) {
            nsi.options = new int[options.length];
            System.arraycopy(options, 0, nsi.options, 0, options.length);
        }
        nsi.objectID = objectID;
        nsi.index = index;
        nsi.speakerType = speakerType;
        nsi.overrideSpeakerTemplateID = overrideSpeakerTemplateID;
        nsi.param = param;
        nsi.color = color;
        nsi.text = text;
        nsi.messageType = messageType;
        if (images != null) {
            nsi.images = images.clone();
        }
        nsi.min = min;
        nsi.max = max;
        nsi.defaultText = defaultText;
        nsi.defaultNumber = defaultNumber;
        nsi.type = type;
        nsi.time = time;
        nsi.title = title;
        nsi.problemText = problemText;
        nsi.hintText = hintText;
        nsi.quizType = quizType;
        nsi.answer = answer;
        nsi.correctAnswers = correctAnswers;
        nsi.remaining = remaining;
        nsi.angelicBuster = angelicBuster;
        nsi.zeroBeta = zeroBeta;
        nsi.dlgType = dlgType;
        nsi.defaultSelect = defaultSelect;
        nsi.selectText = selectText;
        nsi.templateID = templateID;
        nsi.faceIndex = faceIndex;
        nsi.isLeft = isLeft;
        return nsi;
    }

    public byte getSpeakerType() {
        return speakerType;
    }

    public void setSpeakerType(byte speakerType) {
        this.speakerType = speakerType;
    }

    public int getOverrideSpeakerTemplateID() {
        return overrideSpeakerTemplateID;
    }

    public void setOverrideSpeakerTemplateID(int overrideSpeakerTemplateID) {
        this.overrideSpeakerTemplateID = overrideSpeakerTemplateID;
    }

    public short getParam() {
        return param;
    }

    public void setParam(short param) {
        this.param = param;
    }

    public byte getColor() {
        return color;
    }

    public void setColor(byte color) {
        this.color = color;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public NpcMessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(NpcMessageType messageType) {
        this.messageType = messageType;
    }

    public String[] getImages() {
        return images;
    }

    public void setImages(String[] images) {
        this.images = images;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public String getDefaultText() {
        return defaultText;
    }

    public void setDefaultText(String defaultText) {
        this.defaultText = defaultText;
    }

    public int getDefaultNumber() {
        return defaultNumber;
    }

    public void setDefaultNumber(int defaultNumber) {
        this.defaultNumber = defaultNumber;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProblemText() {
        return problemText;
    }

    public void setProblemText(String problemText) {
        this.problemText = problemText;
    }

    public String getHintText() {
        return hintText;
    }

    public void setHintText(String hintText) {
        this.hintText = hintText;
    }

    public int getQuizType() {
        return quizType;
    }

    public void setQuizType(int quizType) {
        this.quizType = quizType;
    }

    public int getAnswer() {
        return answer;
    }

    public void setAnswer(int answer) {
        this.answer = answer;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public int getRemaining() {
        return remaining;
    }

    public void setRemaining(int remaining) {
        this.remaining = remaining;
    }

    public boolean isAngelicBuster() {
        return angelicBuster;
    }

    public void setAngelicBuster(boolean angelicBuster) {
        this.angelicBuster = angelicBuster;
    }

    public boolean isZeroBeta() {
        return zeroBeta;
    }

    public void setZeroBeta(boolean zeroBeta) {
        this.zeroBeta = zeroBeta;
    }

    public int getDlgType() {
        return dlgType;
    }

    public void setDlgType(int dlgType) {
        this.dlgType = dlgType;
    }

    public int getDefaultSelect() {
        return defaultSelect;
    }

    public void setDefaultSelect(int defaultSelect) {
        this.defaultSelect = defaultSelect;
    }

    public String[] getSelectText() {
        return selectText;
    }

    public void setSelectText(String[] selectText) {
        this.selectText = selectText;
    }

    public void addParam(Param param) {
        setParam((byte) (getParam() | param.getVal()));
    }

    public void removeParam(Param param) {
        if ((getParam() & param.getVal()) != 0) {
            setParam((byte) (getParam() ^ param.getVal()));
        }
    }

    public int[] getOptions() {
        return options;
    }

    public void setOptions(int[] options) {
        this.options = options;
    }

    public void resetParam() {
        setParam((byte) 0);
    }

    public void reset() {
        resetParam();
        setSpeakerType((byte) 0);
        setColor((byte) 0);
    }

    public boolean hasParam(Param param) {
        return (getParam() & param.getVal()) != 0;
    }

    public int getTemplateID() {
        return templateID;
    }

    public void setTemplateID(int templateID) {
        this.templateID = templateID;
    }

    public int getFaceIndex() {
        return faceIndex;
    }

    public void setFaceIndex(int faceIndex) {
        this.faceIndex = faceIndex;
    }

    public boolean isLeft() {
        return isLeft;
    }

    public void setLeft(boolean left) {
        isLeft = left;
    }

    public int getObjectID() {
        return objectID;
    }

    public void setObjectID(int objectID) {
        this.objectID = objectID;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getAndroidName() {
        return androidName;
    }

    public void setAndroidName(String androidName) {
        this.androidName = androidName;
    }

    public int[] getAndroids() {
        return androids;
    }

    public void setAndroids(int[] androids) {
        this.androids = androids;
    }

    public enum Param {
        NotCancellable(0x1),
        PlayerAsSpeaker(0x2),
        PlayerAsSpeakerNoEndChat(0x3),
        OverrideSpeakerID(0x4),
        FlipSpeaker(0x8),
        FlipSpeakerNoEndChat(0x9),
        PlayerAsSpeakerFlip(0x10),
        PlayerAsSpeakerFlipNoEndChat(0x11),
        BoxChat(0x20), // Standard BoxChat if Color = 1  |  Zero BoxChat if Color = 0
        BoxChatNoEndChat(0x21),
        BoxChatAsPlayer(0x22),
        BoxChatAsPlayerNoEndChat(0x23),
        BoxChatOverrideSpeaker(0x24),
        BoxChatOverrideSpeakerNoEndChat(0x25),
        FlipBoxChat(0x28),
        FlipBoxChatNoEscape(0x29),
        FlipBoxChatAsPlayer(0x30),
        FlipBoxChatAsPlayerNoEscape(0x31),
        ;

        private final int val;

        Param(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }
    }

    public void encodeGhostPark(OutPacket outPacket) {
        outPacket.encodeInt(21);
        for (int i = 0; i < 7; i++) {
            outPacket.encodeInt(1); // level
            outPacket.encodeInt(10); // incRate
            outPacket.encodeInt(2); // bonusRate
        }
        for (int i = 0; i < 7; i++) {
            outPacket.encodeInt(2); // level
            outPacket.encodeInt(30); // incRate
            outPacket.encodeInt(5); // bonusRate
        }
        for (int i = 0; i < 7; i++) {
            outPacket.encodeInt(3); // level
            outPacket.encodeInt(100); // incRate
            outPacket.encodeInt(10); // bonusRate
        }
    }
}
