package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Instance;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/** Verified routing from TMS273 boss_malitia_normal/extreme; native mechanics remain unavailable. */
public final class Malitia {
    public static final int LOBBY = 875033004;
    public static final int NORMAL = 875030000;
    public static final int EXTREME = 875031000;
    private static final int[] INTRO_DURATIONS = {7500, 7200, 9000, 12030};
    private Malitia() { }

    private static boolean enabled() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    public static boolean isField(int id) {
        return (id >= NORMAL && id <= NORMAL + 800 && (id - NORMAL) % 100 == 0)
                || (id >= EXTREME && id <= EXTREME + 800 && (id - EXTREME) % 100 == 0);
    }

    public static void enter(Char chr) {
        if (!enabled() || chr.getFieldID() != LOBBY) return;
        if (chr.getInstance() == null) {
            chr.getScriptManager().startBossUI("Malitia");
        } else if (chr.getParty() != null && chr.getParty().getOnlineChars().size() > 3) {
            chr.getScriptManager().sendSayOkay("瑪麗西亞最多可由三人組隊挑戰。");
        } else {
            chr.getScriptManager().startBoss();
        }
    }

    public static void exit(Char chr) {
        if (!enabled() || !isField(chr.getFieldID())) return;
        if (chr.getInstance() != null) chr.getScriptManager().exitBoss();
        else chr.warp(LOBBY);
    }

    public static void enterField(Char chr) {
        if (!enabled() || !isField(chr.getFieldID()) || chr.getInstance() == null) return;
        int sequence = introSequence(chr.getFieldID());
        if (sequence >= 0) {
            Field source = chr.getField();
            Instance instance = chr.getInstance();
            String key = "MalitiaIntro" + chr.getId();
            Object token = new Object();
            source.setProperty(key, token);
            // Actual TMS282 frame animations, not Map/Obj's ambient Spine assets.
            // Four sequence IDs follow the four intro maps in event order; mapping is inferred,
            // while every duration is the exact sum of original frame delay properties.
            chr.getScriptManager().beginBossIntro(introDuration(chr.getFieldID()));
            chr.getScriptManager().screen("Effect/DirectionMalitia.img/effect/screen/intro/" + sequence);
            int base = source.getId() >= EXTREME ? EXTREME : NORMAL;
            int destination = base + (sequence == 0 ? 100 : sequence == 3 ? 700 : 400);
            chr.getTimer().addEvent(() -> {
                if (chr.getField() == source && chr.getInstance() == instance && source.getProperty(key) == token) {
                    chr.getScriptManager().endBossIntro();
                    chr.warp(destination);
                }
            }, introDuration(chr.getFieldID()), TimeUnit.MILLISECONDS);
            return;
        }
        // FrameworkMob's server event is still absent. Never invent form spawns or stage clears.
        chr.chatMessage("瑪麗西亞階段戰鬥機制尚未完成移植；請使用首領退出功能離場。");
    }

    public static int introSequence(int fieldId) {
        if (!isField(fieldId)) return -1;
        int base = fieldId >= EXTREME ? EXTREME : NORMAL;
        return switch (fieldId - base) {
            case 0 -> 0;
            case 300 -> 1;
            case 500 -> 2;
            case 600 -> 3;
            default -> -1;
        };
    }

    public static int introDuration(int fieldId) {
        int sequence = introSequence(fieldId);
        return sequence < 0 ? 0 : INTRO_DURATIONS[sequence];
    }

    /** Only death IDs explicitly named in the reference event advance a stage. */
    public static int nextFieldForDeath(int fieldId, int mobId) {
        if (!isField(fieldId)) return 0;
        boolean extreme = fieldId >= EXTREME;
        int base = extreme ? EXTREME : NORMAL;
        int stage = fieldId - base;
        if ((stage == 100 || stage == 200) && mobId == (extreme ? 9602526 : 9602465)) return base + 300;
        if (stage == 400 && mobId == (extreme ? 9602528 : 9602467)) return base + 500;
        if (stage == 400 && mobId == (extreme ? 9602532 : 9602471)) return base + 600;
        if (stage == 700 && mobId == (extreme ? 9602533 : 9602472)) return base + 800;
        return 0;
    }

    public static void onMobDeath(Field field, int mobId) {
        if (!enabled()) return;
        int target = nextFieldForDeath(field.getId(), mobId);
        if (target == 0) return;
        Instance instance = field.getChars().stream().map(Char::getInstance).filter(i -> i != null).findFirst().orElse(null);
        if (instance == null) return;
        String key = "MalitiaDeathRoute" + target;
        synchronized (instance) {
            if (instance.hasProperty(key)) return;
            instance.setProperty(key, true);
        }
        int base = field.getId() >= EXTREME ? EXTREME : NORMAL;
        int delay = target == base + 300 ? 3000 : 5000;
        // Reference event delays only after an actual death, never in place of combat/animation.
        field.getTimer().addEvent(() -> {
            for (Char member : new ArrayList<>(instance.getChars().values())) {
                int current = member.getFieldID();
                boolean source = target == base + 300 ? current == base + 100 || current == base + 200
                        : current == field.getId();
                if (source && member.getInstance() == instance) member.warp(target);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
}
