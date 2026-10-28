package net.swordie.ms.world.field.fieldeffect;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.util.Util;

import java.util.ArrayList;
import java.util.List;

public class FieldEffect {

    private FieldEffectType fieldEffectType;
    private String string = "";
    private String string2 = "";
    private String string3 = "";
    private String string4 = "";
    private List<Integer> list = new ArrayList<>();
    private int arg1 = 0;
    private int arg2 = 0;
    private int arg3 = 0;
    private int arg4 = 0;
    private int arg5 = 0;
    private int arg6 = 0;
    private int arg7 = 0;
    private int arg8 = 0;
    private int arg9 = 0;
    private long arg10 = 0;
    private long arg11 = 0;
    private long arg12 = 0;
    private long arg13 = 0;

    public static FieldEffect mobHPTagFieldEffect(Mob mob) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.MobHPTag);

        fieldEffect.setArg1(mob.getTemplateId());
        int maxHP = Util.maxInt(mob.getMaxHp());
        double ratio = mob.getMaxHp() / (double) Integer.MAX_VALUE;
        fieldEffect.setArg2(ratio > 1 ? (int) (mob.getHp() / ratio) : (int) mob.getHp());
        fieldEffect.setArg3(maxHP);
        fieldEffect.setArg4(mob.getHpTagColor());
        fieldEffect.setArg5(mob.getHpTagBgcolor());

        return fieldEffect;
    }

    public static FieldEffect getFieldEffectFromWz(String dir, int delay) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.ScreenEffect);

        fieldEffect.setString(dir);
        fieldEffect.setArg1(delay);

        return fieldEffect;
    }

    public static FieldEffect getFieldFloatingEffectFromWz(String dir, int arg1) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.ScreenFloatingEffect);

        fieldEffect.setString(dir);
        fieldEffect.setArg1(arg1);
        fieldEffect.setArg2(0);

        return fieldEffect;
    }

    public static FieldEffect getFieldBackgroundEffectFromWz(String dir, int delay) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.BackScreen);

        fieldEffect.setString(dir);
        fieldEffect.setArg1(delay);

        return fieldEffect;
    }

    public static FieldEffect getOffFieldEffectFromWz(String dir, int delay) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.TopScreenEffect);

        fieldEffect.setString(dir);
        fieldEffect.setArg1(delay);

        return fieldEffect;
    }

    public static FieldEffect setFieldGrey(GreyFieldType greyFieldType, boolean setGrey) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.SetGrey);

        fieldEffect.setArg1(greyFieldType.getVal());
        fieldEffect.setArg2(setGrey ? 1 : 0);

        return fieldEffect;
    }

    public static FieldEffect setFieldColor(GreyFieldType colorFieldType, short red, short green, short blue, int time) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.ChangeColor);

        fieldEffect.setArg1(colorFieldType.getVal());
        fieldEffect.setArg2(red);
        fieldEffect.setArg3(green);
        fieldEffect.setArg4(blue);
        fieldEffect.setArg5(time);

        return fieldEffect;
    }

    public static FieldEffect showClearStageExpWindow(int expNumber) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.StageClearExpOnly);

        fieldEffect.setArg1(expNumber);

        return fieldEffect;
    }

    public static FieldEffect takeSnapShotOfClient(int duration) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.OverlapScreen);

        fieldEffect.setArg1(duration);

        return fieldEffect;
    }

    public static FieldEffect takeSnapShotOfClient2(int transitionDurationToSnapShot, int inBetweenDuration, int transitionBack, boolean someBoolean) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.OverlapScreenDetail);

        fieldEffect.setArg1(transitionDurationToSnapShot);
        fieldEffect.setArg2(inBetweenDuration);
        fieldEffect.setArg3(transitionBack);
        fieldEffect.setArg4(someBoolean ? 1 : 0);

        return fieldEffect;
    }

    public static FieldEffect screen(String string) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.Screen);

        fieldEffect.setString(string);

        return fieldEffect;
    }

    public static FieldEffect playSound(String sound, int vol) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.PlaySound);

        fieldEffect.setString(sound);
        fieldEffect.setArg1(vol);
        fieldEffect.setArg2(0);
        fieldEffect.setArg3(0);

        return fieldEffect;
    }

    public static FieldEffect topScreen(String dir) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.TopScreen);

        fieldEffect.setString(dir);

        return fieldEffect;
    }

    public static FieldEffect topScreenEffect(String dir, int delay) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.TopScreenEffect);

        fieldEffect.setString(dir);
        fieldEffect.setArg1(delay);

        return fieldEffect;
    }

    public static FieldEffect OnOffLayer_On(int term, String key, int x, int y, int z, String path, int origin, boolean postRender, int idk, boolean repeat) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.OnOffLayer);

        fieldEffect.setArg1(0);// type0
        fieldEffect.setArg2(term);
        fieldEffect.setArg3(x); //Screen X
        fieldEffect.setArg4(y); //Screen y
        fieldEffect.setArg5(z);
        fieldEffect.setArg6(origin);
        fieldEffect.setArg7(postRender ? 1 : 0);
        fieldEffect.setArg8(idk);
        fieldEffect.setArg9(repeat ? 1 : 0);
        fieldEffect.setString(key);
        fieldEffect.setString2(path);

        return fieldEffect;
    }

    public static FieldEffect OnOffLayer_On(int term, String key, int x, int y, int z, String path, int origin, int unk5, int unk6, int unk7) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.OnOffLayer);

        fieldEffect.setArg1(0);// type0
        fieldEffect.setArg2(term);
        fieldEffect.setArg3(x); //Screen X
        fieldEffect.setArg4(y); //Screen y
        fieldEffect.setArg5(z);
        fieldEffect.setArg6(origin);
        fieldEffect.setArg7(unk5);
        fieldEffect.setArg8(unk6);
        fieldEffect.setArg9(unk7);
        fieldEffect.setString(key);
        fieldEffect.setString2(path);

        return fieldEffect;
    }

    public static FieldEffect OnOffLayer_Move(int term, String key, int dx, int dy) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.OnOffLayer);

        fieldEffect.setArg1(1);
        fieldEffect.setArg2(term);
        fieldEffect.setArg3(dx);
        fieldEffect.setArg4(dy);
        fieldEffect.setString(key);

        return fieldEffect;
    }

    public static FieldEffect OnOffLayer_Off(int term, String key, boolean unk) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.OnOffLayer);

        fieldEffect.setArg1(2);// type0
        fieldEffect.setArg2(term);
        fieldEffect.setArg3(unk ? 1 : 0);
        fieldEffect.setString(key);

        return fieldEffect;
    }

    public static FieldEffect OnOffLayer_Off(int term, String key, int unk) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.OnOffLayer);

        fieldEffect.setArg1(2);// type0
        fieldEffect.setArg2(term);
        fieldEffect.setArg3(unk);
        fieldEffect.setString(key);

        return fieldEffect;
    }

    public static FieldEffect changeBGM(String sound, int startTime, int unk) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.ChangeBGM);

        fieldEffect.setString(sound);
        fieldEffect.setArg1(startTime);// type0
        fieldEffect.setArg2(unk);

        return fieldEffect;
    }

    public static FieldEffect bgmVolumeOnly(boolean volumeOnly) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.BGMVolumeOnly);

        fieldEffect.setArg1(volumeOnly ? 1 : 0);

        return fieldEffect;
    }

    public static FieldEffect tremble(int bHeavyNShortTremble, int tDelay, int unk) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.Tremble);

        fieldEffect.setArg1(bHeavyNShortTremble);
        fieldEffect.setArg2(tDelay);
        fieldEffect.setArg3(unk);

        return fieldEffect;
    }

    public static FieldEffect spineScreen(boolean binary, boolean loop, boolean postRender, int endDelay, String path,
                                          String animationName, String keyName) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.SpineScreen);

        fieldEffect.arg1 = binary ? 1 : 0;
        fieldEffect.arg2 = loop ? 1 : 0;
        fieldEffect.arg3 = postRender ? 1 : 0;
        fieldEffect.arg4 = endDelay;
        fieldEffect.string = path;
        fieldEffect.string2 = animationName;
        fieldEffect.string3 = keyName;

        return fieldEffect;
    }

    public static FieldEffect offSpineScreen(String keyName, int type, String aniName, int alphaDecayTime) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.OffSpineScreen);

        fieldEffect.string = keyName;
        fieldEffect.arg1 = type;
        fieldEffect.string2 = aniName;
        fieldEffect.arg2 = alphaDecayTime;

        return fieldEffect;
    }

    public static FieldEffect skeletonAnimation(boolean isBinary, boolean isLoop, boolean hideUI, int time,
                                                String path, String animationName, boolean isKey, String keyName) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.SpineScreen);

        fieldEffect.setArg1(isBinary ? 1 : 0);
        fieldEffect.setArg2(isLoop ? 1 : 0);
        fieldEffect.setArg3(hideUI ? 1 : 0);
        fieldEffect.setArg4(time); // Milliseconds
        fieldEffect.setString(path);
        fieldEffect.setString2(animationName);
        fieldEffect.setArg5(isKey ? 1 : 0);
        if (isKey) {
            fieldEffect.setString3(keyName);
        }

        return fieldEffect;
    }

    public static FieldEffect offSkeletonScreenImmediate(String layer) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.OffSpineScreen);

        fieldEffect.setArg1(0);// OffSpineScr_Immediate = 0x0
        fieldEffect.setString(layer);

        return fieldEffect;
    }

    public static FieldEffect offSkeletonScreenAlpha(String layer, int alpha) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.OffSpineScreen);

        fieldEffect.setArg1(1);// OffSpineScr_Alpha = 0x1
        fieldEffect.setString(layer);
        fieldEffect.setArg2(alpha);

        return fieldEffect;
    }

    public static FieldEffect offSkeletonScreenAnimation(String layer, String path) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.OffSpineScreen);

        fieldEffect.setArg1(2);// OffSpineScr_Ani = 0x2
        fieldEffect.setString(layer);
        fieldEffect.setString2(path);

        return fieldEffect;
    }

    public static FieldEffect setBGMVolume(int bgmVolume, int fadingDuration) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.SetBGMVolume);

        fieldEffect.setArg1(bgmVolume);// type0
        fieldEffect.setArg2(fadingDuration);

        return fieldEffect;
    }

    public static FieldEffect objectStateByString(String name) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.ObjectStateByString);

        fieldEffect.setString(name);

        return fieldEffect;
    }

    public static FieldEffect blind(int enable, int x, int color, int unk1, int unk2, int time) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.Blind);

        fieldEffect.setArg1(enable);
        fieldEffect.setArg2(x);
        fieldEffect.setArg3(color);
        fieldEffect.setArg4(unk1);
        fieldEffect.setArg5(unk2);
        fieldEffect.setArg6(time);

        return fieldEffect;
    }

    public static FieldEffect removeOverlapScreen(int duration) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.RemoveOverlapScreen);

        fieldEffect.setArg1(duration);

        return fieldEffect;
    }

    public static FieldEffect teraBlinkWarp(int arg1, String string, int arg2, int arg3, int arg4, int arg5, int arg6, int arg7, int arg8) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.TeraBlinkWarp);

        fieldEffect.setArg1(arg1);
        fieldEffect.setString(string);
        fieldEffect.setArg2(arg2);
        fieldEffect.setArg3(arg3);
        fieldEffect.setArg4(arg4);
        fieldEffect.setArg5(arg3);
        fieldEffect.setArg6(arg3);
        fieldEffect.setArg7(arg3);
        fieldEffect.setArg8(arg3);

        return fieldEffect;
    }

    public static FieldEffect teraBlinkEff(int arg1, String string, String string2, int arg2, int arg3) {
        FieldEffect fieldEffect = new FieldEffect();
        fieldEffect.setFieldEffectType(FieldEffectType.TeraBlinkEff);

        fieldEffect.setArg1(arg1);
        fieldEffect.setString(string);
        fieldEffect.setString2(string2);
        fieldEffect.setArg2(arg2);
        fieldEffect.setArg3(arg3);

        return fieldEffect;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeByte(getFieldEffectType().getVal());
        switch (getFieldEffectType()) {
            case FromString:
                outPacket.encodeByte(arg1);
                outPacket.encodeInt(arg2);
                outPacket.encodeInt(arg3);
                break;
            case Tremble:
                outPacket.encodeByte(arg1);
                outPacket.encodeInt(arg2);
                outPacket.encodeShort(arg3);
                break;
            case ObjectStateByString:
                outPacket.encodeString(string);// String
                break;
            case DisableEffectObject:
                outPacket.encodeString(string);// String
                outPacket.encodeByte(arg1);    // boolean: ON/OFF
                break;
            case Screen:
                outPacket.encodeString(string);
                break;
            case PlaySound:
                outPacket.encodeString(string); // Sound
                outPacket.encodeInt(arg1);      // Volume
                outPacket.encodeInt(arg2);
                outPacket.encodeInt(arg3);
                outPacket.encodeInt(arg4);
                outPacket.encodeInt(arg5);
                break;
            case MobHPTag:
                outPacket.encodeInt(arg1);      // Mob Template ID
                outPacket.encodeLong(arg2);     // Mob HP
                outPacket.encodeLong(arg3);     // Mob max HP
                outPacket.encodeByte(arg4);     // HP Tag Colour
                outPacket.encodeByte(arg5);     // HP Tab BG Colour
                break;
            case ChangeBGM:
                outPacket.encodeString(string); // sound
                outPacket.encodeInt(arg1);      // start time
                outPacket.encodeInt(arg2);
                outPacket.encodeInt(arg3);
                break;
            case BGMVolumeOnly:
                outPacket.encodeByte(arg1);     // m_bBGMVolumeOnly
                break;
            case SetBGMVolume:
                outPacket.encodeInt(arg1);      // m_uBGMVolume
                outPacket.encodeInt(arg2);      // uFadingDuration
                break;
            case RewardRoulette:
                outPacket.encodeInt(arg1);     // Reward Job ID
                outPacket.encodeInt(arg2);     // Reward Part ID
                outPacket.encodeInt(arg3);     // Reward Level ID
                break;
            case TopScreen:
                outPacket.encodeString(string);// Directory to the Effect
                break;
            case BackScreen:
                outPacket.encodeString(string); // Directory to the Effect
                outPacket.encodeInt(arg1);      // Delay in ms
                break;
            case TopScreenEffect:               // Goes over other effects
                outPacket.encodeString(string); // Directory to the Effect
                outPacket.encodeInt(arg1);      // Delay in ms
                break;
            case ScreenEffect:
                outPacket.encodeString(string); // Path to the Effect
                outPacket.encodeInt(arg1);      // Delay in ms
                outPacket.encodeInt(arg2);
                break;
            case ScreenFloatingEffect:
                outPacket.encodeString(string); // Path to the Effect
                outPacket.encodeByte(arg1);
                outPacket.encodeByte(arg2);
                break;
            case Blind:
                outPacket.encodeByte(arg1);
                outPacket.encodeShort(arg2);
                outPacket.encodeShort(arg3);
                outPacket.encodeShort(arg4);
                outPacket.encodeShort(arg5);
                outPacket.encodeInt(arg6);
                outPacket.encodeInt(arg7);
                break;
            case TeraBlinkWarp:
                outPacket.encodeInt(arg1);
                outPacket.encodeString(string);
                outPacket.encodeInt(arg2);
                outPacket.encodeInt(arg3);
                outPacket.encodeInt(arg4);
                outPacket.encodeInt(arg5);
                outPacket.encodeInt(arg6);
                outPacket.encodeInt(arg7);
                outPacket.encodeInt(arg8);
                break;
            case TeraBlinkEff:
                outPacket.encodeInt(arg1);
                outPacket.encodeString(string);
                outPacket.encodeString(string2);
                outPacket.encodeInt(arg2);
                outPacket.encodeInt(arg3);
                break;
            case SetGrey:
                outPacket.encodeShort(arg1);   // GreyField Type
                outPacket.encodeByte(arg2);    // boolean: ON/OFF
                break;
            case OnOffLayer:
                outPacket.encodeByte(arg1);// type
                outPacket.encodeInt(arg2);
                outPacket.encodeString(string);
                if (getArg1() == 0) {
                    outPacket.encodeInt(arg3);
                    outPacket.encodeInt(arg4);
                    outPacket.encodeInt(arg5);
                    outPacket.encodeString(string2);
                    outPacket.encodeInt(arg6);
                    outPacket.encodeByte(arg7);
                    outPacket.encodeInt(arg8);
                    outPacket.encodeByte(arg9);
                    outPacket.encodeInt(0);
                    outPacket.encodeInt(0);
                    outPacket.encodeByte(0);
                } else if (getArg1() == 1) {
                    outPacket.encodeInt(arg3); // nDX
                    outPacket.encodeInt(arg4); // nDY
                } else if (getArg1() == 2) {
                    outPacket.encodeByte(arg3);
                } else if (getArg1() == 3) {
                    outPacket.encodeString(string2); // pOrigin?
                    outPacket.encodeInt(arg3); // nDX?
                    outPacket.encodeInt(arg4); // nDY?
                } else if (getArg1() == 4) {
                    outPacket.encodeInt(arg3);
                    outPacket.encodeInt(arg4);
                    outPacket.encodeByte(arg5);
                    outPacket.encodeInt(arg6);
                    outPacket.encodeByte(arg7);
                    outPacket.encodeString(string2);
                } else if (getArg1() == 5) {
                    outPacket.encodeInt(arg3);
                    outPacket.encodeInt(arg4);
                    outPacket.encodeString(string2);
                } else if (getArg1() == 6) {
                    outPacket.encodeInt(arg3);
                    outPacket.encodeInt(arg4);
                    outPacket.encodeString(string2);
                }
                break;
            case OverlapScreen:                    // Takes a Snapshot of the Client and slowly fades away
                outPacket.encodeInt(arg1);     // Duration of the overlap (ms)
                break;
            case OverlapScreenDetail:
                outPacket.encodeInt(arg1);     // Fade In
                outPacket.encodeInt(arg2);     // wait time
                outPacket.encodeInt(arg3);     // Fade Out
                outPacket.encodeByte(arg4);    // some boolean
                break;
            case RemoveOverlapScreen:
                outPacket.encodeInt(arg1);     // Fade Out duration
                break;
            case ChangeColor:
                outPacket.encodeShort(arg1);   // GreyField Type (but doesn't contain Reactor
                outPacket.encodeShort(arg2);   // red      (250 is normal value)
                outPacket.encodeShort(arg3);   // green    (250 is normal value)
                outPacket.encodeShort(arg4);   // blue     (250 is normal value)
                outPacket.encodeInt(arg5);     // time in ms, that it takes to transition from old colours to the new colours
                outPacket.encodeInt(0);     // is in queue, > 0 ? = 4 => encodeInt(Id)
                if (getArg1() == 4) {
                    outPacket.encodeInt(0);
                }
                break;
            case StageClearExpOnly:
                outPacket.encodeInt(arg1);     // Exp Number given
                break;
            case TopScreenWithOrigin:
                outPacket.encodeString(string);
                outPacket.encodeByte(arg1);
                break;
            case SpineScreen:
                outPacket.encodeByte(arg1); // bBinary
                outPacket.encodeByte(arg2); // bLoop
                outPacket.encodeByte(arg3); // bPostRender
                outPacket.encodeInt(arg4); // tEndDelay
                outPacket.encodeString(string); // sPath
                outPacket.encodeString(string2); // sAniamtionName
                outPacket.encodeString(string3); // ?
                outPacket.encodeByte(false); // ?
                outPacket.encodeInt(0); // ?
                outPacket.encodeInt(0); // ?
                outPacket.encodeInt(0); // ?
                outPacket.encodeInt(0); // ?
                outPacket.encodeByte(arg5); // boolean to release sKeyName
                if (getArg5() == 1) {
                    outPacket.encodeString(string3); // sKeyName
                }
                break;
            case OffSpineScreen:
                outPacket.encodeString(string); // pLayer
                outPacket.encodeInt(arg1);
                if (getArg1() == 1) {
                    outPacket.encodeString(string2); // uWidth + 1
                } else {
                    outPacket.encodeInt(arg2); // tAphaTime
                }
                break;
        }
    }

    public FieldEffectType getFieldEffectType() {
        return fieldEffectType;
    }

    public void setFieldEffectType(FieldEffectType fieldEffectType) {
        this.fieldEffectType = fieldEffectType;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public String getString2() {
        return string2;
    }

    public void setString2(String string2) {
        this.string2 = string2;
    }

    public String getString3() {
        return string3;
    }

    public void setString3(String string3) {
        this.string3 = string3;
    }

    public String getString4() {
        return string4;
    }

    public void setString4(String string4) {
        this.string4 = string4;
    }

    public int getArg1() {
        return arg1;
    }

    public void setArg1(int arg1) {
        this.arg1 = arg1;
    }

    public int getArg2() {
        return arg2;
    }

    public void setArg2(int arg2) {
        this.arg2 = arg2;
    }

    public int getArg3() {
        return arg3;
    }

    public void setArg3(int arg3) {
        this.arg3 = arg3;
    }

    public int getArg4() {
        return arg4;
    }

    public void setArg4(int arg4) {
        this.arg4 = arg4;
    }

    public int getArg5() {
        return arg5;
    }

    public void setArg5(int arg5) {
        this.arg5 = arg5;
    }

    public int getArg6() {
        return arg6;
    }

    public void setArg6(int arg6) {
        this.arg6 = arg6;
    }

    public int getArg7() {
        return arg7;
    }

    public void setArg7(int arg7) {
        this.arg7 = arg7;
    }

    public int getArg8() {
        return arg8;
    }

    public void setArg8(int arg8) {
        this.arg8 = arg8;
    }

    public int getArg9() {
        return arg9;
    }

    public void setArg9(int arg9) {
        this.arg9 = arg9;
    }

    public long getArg10() {
        return arg10;
    }

    public void setArg10(long arg10) {
        this.arg10 = arg10;
    }

    public long getArg11() {
        return arg11;
    }

    public void setArg11(long arg11) {
        this.arg11 = arg11;
    }

    public long getArg12() {
        return arg12;
    }

    public void setArg12(long arg12) {
        this.arg12 = arg12;
    }

    public long getArg13() {
        return arg13;
    }

    public void setArg13(long arg13) {
        this.arg13 = arg13;
    }

    public List<Integer> getList() {
        return list;
    }

    public void setList(List<Integer> list) {
        this.list = list;
    }
}
