package net.swordie.ms.life.npc;

import net.swordie.ms.util.Util;

public enum NpcMessageType {
    SayOk(0, false, false, ResponseType.Response),
    SayNext(0, false, true, ResponseType.Response),
    SayPrev(0, true, false, ResponseType.Response),
    Say(0, true, true, ResponseType.Response),

    SayUnk(1, ResponseType.Response),

    SayImage(2, ResponseType.Response),
    AskYesNo(3, ResponseType.Response),
    AskText(4, ResponseType.Text),
    AskNumber(5, ResponseType.Answer),
    AskMenu(6, ResponseType.Answer),
    InitialQuiz(7, ResponseType.Answer),
    InitialSpeedQuiz(8, ResponseType.Answer),
    ICQuiz(9, ResponseType.Answer),
    AskAvatar(10, ResponseType.Answer),
    AskAndroid(11, ResponseType.Answer),
    AskMannequin(12, ResponseType.Answer),
    AskPet(13, ResponseType.Answer),
    AskPetAll(14, ResponseType.Answer),
    AskActionPetEvolution(15, ResponseType.Answer),
    AskAccept2(17, ResponseType.Response),
    AskAccept(18, ResponseType.Response),
    AskBoxtext(19, ResponseType.Answer),
    AskSlideMenu(20, ResponseType.Answer),
    AskIngameDirection(21, ResponseType.Response),
    PlayMovieClip(22, ResponseType.Response),
    AskCenter(23, ResponseType.Answer),
    AskAvatar2(25, ResponseType.Answer),
    AskSelectMenu(26, ResponseType.Answer),

    AskStoryUI(27, ResponseType.Answer),

    AskAngelicBuster(29, ResponseType.Answer),
    SayIllustration(30, ResponseType.Answer),
    SayIllustrationOk(30, false, false, ResponseType.Response),
    SayIllustrationNext(30, false, true, ResponseType.Response),
    SayIllustrationPrev(30, true, false, ResponseType.Response),
    SayDualIllustration(31, ResponseType.Answer),
    AskYesNoIllustration(32, ResponseType.Answer),
    AskAcceptIllustration(33, ResponseType.Answer),
    AskMenuIllustration(34, ResponseType.Answer),
    AskYesNoDualIllustration(35, ResponseType.Answer),
    AskAcceptDualIllustration(36, ResponseType.Answer),
    AskMenuDualIllustration(37, ResponseType.Answer),

    AskAvatarZero(38, ResponseType.Answer), // not sure
    AskAvatar3(39, ResponseType.Answer), // not sure
    AskAvatar3Zero(40, ResponseType.Answer), // not sure
    AskWeaponBox(42, ResponseType.Answer), // not sure

    AskBoxTextBgImg(44, ResponseType.Answer),
    AskUserSurvey(45, ResponseType.Answer),

    Monologue(46, ResponseType.Response), // not sure
    AskMixHair(47, ResponseType.Answer), // not sure
    AskMixHairExZero(48, ResponseType.Answer), // not sure
    AskCustomMixHair(49, ResponseType.Answer), // not sure
    AskCustomMixHairAndProb(50, ResponseType.Answer), // not sure
    AskMixHairNew(51, ResponseType.Answer), // not sure
    AskMixHairNewExZero(52, ResponseType.Answer), // not sure

    AskNumberUseKeyPad(53, ResponseType.Answer),
    SpinOffGuitarRhythmGame(54, ResponseType.Answer),
    GhostParkEnter(55, ResponseType.Answer),
    GhostParkEnter2(56, ResponseType.Answer),

    None(-1, ResponseType.Answer),
    ;

    private byte val;
    private boolean prevPossible, nextPossible;
    private int delay;
    private ResponseType responseType;

    NpcMessageType(int val, ResponseType responseType) {
        this.val = (byte) val;
        prevPossible = false;
        nextPossible = false;
        this.responseType = responseType;
    }

    NpcMessageType(int val, boolean prev, boolean next, ResponseType responseType) {
        this.val = (byte) val;
        prevPossible = prev;
        nextPossible = next;
        this.responseType = responseType;
    }

    public static NpcMessageType getByVal(byte val) {
        return Util.findWithPred(values(), v -> v.getVal() == val);
    }

    public byte getVal() {
        return val;
    }

    public boolean isPrevPossible() {
        return prevPossible;
    }

    public boolean isNextPossible() {
        return nextPossible;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }


    public ResponseType getResponseType() {
        return responseType;
    }

    public enum ResponseType {
        Response, Answer, Text
    }
}
