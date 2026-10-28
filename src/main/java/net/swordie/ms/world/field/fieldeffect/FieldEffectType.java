package net.swordie.ms.world.field.fieldeffect;

public enum FieldEffectType {
    FromString(0),
    Tremble(1),
    ObjectStateByString(2),
    DisableEffectObject(3),
    Screen(4),
    PlaySound(7),
    MobHPTag(9),
    ChangeBGM(10),
    BGMVolumeOnly(11),
    SetBGMVolume(12),
    RewardRoulette(18),
    TopScreen(19),
    BackScreen(20),
    TopScreenEffect(21),
    ScreenEffect(23),
    ScreenFloatingEffect(24),
    Blind(25),
    TeraBlinkWarp(27),
    TeraBlinkEff(28),
    SetGrey(33),
    OnOffLayer(34),
    OverlapScreen(35),
    OverlapScreenDetail(36),
    RemoveOverlapScreen(37),
    ChangeColor(38),
    StageClearExpOnly(40),
    TopScreenWithOrigin(41),
    SpineScreen(42),
    OffSpineScreen(43),
    ;

    private final byte val;

    FieldEffectType(int val) {
        this.val = (byte) val;
    }

    public byte getVal() {
        return val;
    }
}
