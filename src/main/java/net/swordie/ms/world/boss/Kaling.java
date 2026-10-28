package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Instance;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/** TMS273 event routes and original TMS282 cinematics. Native Karing combat is not emulated. */
public final class Kaling {
    public static final int LOBBY = 410007025;
    private static final int[] BASES = {410008000, 410007100, 410008300, 410008600};
    private static final int[] MOBS = {8880900, 8880830, 8880930, 8880960};
    private static final String[] INTROS = {"1phase_goongi_spine", "1phase_dool_spine",
            "1phase_hondon_spine", "2phase_spine", "3phase_spine"};
    private Kaling() { }

    private static boolean enabled() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    public static int baseForDifficulty(int difficulty) {
        return switch (difficulty) {
            case 0 -> BASES[0]; case 1 -> BASES[1]; case 2 -> BASES[2]; case 4 -> BASES[3];
            default -> 0;
        };
    }

    public static int baseForField(int fieldId) {
        for (int base : BASES) {
            if (fieldId >= base && fieldId <= base + 220 && (fieldId - base) % 20 == 0) return base;
        }
        return 0;
    }

    public static boolean isField(int fieldId) { return baseForField(fieldId) != 0; }

    public static int introSequence(int fieldId) {
        int base = baseForField(fieldId);
        if (base == 0) return -1;
        return switch (fieldId - base) {
            case 20 -> 0; case 60 -> 1; case 100 -> 2; case 140 -> 3; case 180 -> 4;
            default -> -1;
        };
    }

    public static int introDuration(int fieldId) { return introSequence(fieldId) < 0 ? 0 : 8000; }

    public static void enter(Char chr) {
        if (!enabled()) return;
        if (isField(chr.getFieldID())) {
            exit(chr);
        } else if (chr.getFieldID() == LOBBY && chr.getInstance() != null) {
            if (chr.getParty() != null && chr.getParty().getOnlineChars().size() > 6) {
                chr.getScriptManager().sendSayOkay("咖凌最多可由六人組隊挑戰。");
                return;
            }
            chr.getScriptManager().startBoss();
        } else {
            chr.getScriptManager().startBossUI("Kaling");
        }
    }

    public static void exit(Char chr) {
        if (!enabled() || !isField(chr.getFieldID())) return;
        chr.getScriptManager().endBossIntro();
        if (chr.getInstance() != null) chr.getScriptManager().exitBoss();
        else chr.warp(LOBBY);
    }

    public static void enterField(Char chr) {
        if (!enabled() || !isField(chr.getFieldID()) || chr.getInstance() == null) return;
        Field field = chr.getField();
        Instance instance = chr.getInstance();
        int base = baseForField(field.getId());
        if (field.getId() == base) {
            synchronized (instance) {
                if (instance.hasProperty("KalingSelectionStarted")) return;
                instance.setProperty("KalingSelectionStarted", true);
                instance.setTimeout(35 * 60, false); // Selection allowance + battle; reset at actual battle start.
                // Native selection UI is not ported. 273 distributes after its 5 min "select" timer; bossflow-0930
                // default 10 s 【非官方】(tms282.kaling.selectSec, 300 = the 273 wait).
                field.getTimer().addEvent(() -> distribute(instance, base), selectSeconds(), TimeUnit.SECONDS);
            }
            chr.chatMessage("咖凌區域選擇介面尚未移植；" + selectSeconds() + " 秒後依隊伍順序分配初始區域。");
            return;
        }
        int stageMob = stageMob(field.getId());
        if (stageMob != 0 && spawnEnabled()) {
            // bossflow-0930: 273 initKaringEvent (native) summoned these; the 282 server summoned nothing.
            int[] pos = stagePosition(field.getId() - base);
            synchronized (field) {
                if (!field.hasProperty("KalingStageSpawned")) {
                    field.setProperty("KalingStageSpawned", true);
                    if (chr.getScriptManager().spawnBossMob(stageMob, pos[0], pos[1]) == null) {
                        field.getProperties().remove("KalingStageSpawned");
                    }
                }
            }
            return;
        }
        int sequence = introSequence(field.getId());
        if (sequence >= 0) {
            String key = "KalingIntro" + chr.getId();
            Object token = new Object();
            field.setProperty(key, token);
            var sm = chr.getScriptManager();
            sm.beginBossIntro(8000);
            sm.spineScreen(false, false, true, 0, "Effect/Direction20.img/bossKaring/" + INTROS[sequence],
                    "animation", "KalingIntro");
            chr.getTimer().addEvent(() -> {
                if (chr.getField() == field && chr.getInstance() == instance && field.getProperty(key) == token) {
                    sm.offSpineScreen("KalingIntro", 0, "", 0);
                    sm.endBossIntro();
                    chr.warp(field.getId() + 20);
                }
            }, 8000, TimeUnit.MILLISECONDS);
        } else if (field.getId() != base + 220) {
            chr.chatMessage("咖凌原生階段戰鬥機制尚未完成移植；可使用首領退出功能離場。");
        }
    }

    // ---- bossflow-0930 (research/tms2823/boss-flow-0930) ---------------------------------------------------------

    static String setting(String key, String fallback) {
        String jvm = System.getProperty(key);
        if (jvm != null) return jvm;
        try {
            String v = net.swordie.ms.console.config.ServerRuntimeConfig.stringValue(key);
            return v == null || v.isBlank() ? fallback : v;
        } catch (RuntimeException | LinkageError e) {
            return fallback;
        }
    }

    /** {@code tms282.kaling.spawn.enabled=false} -> the old behaviour (nothing summoned). */
    public static boolean spawnEnabled() {
        return !"false".equalsIgnoreCase(setting("tms282.kaling.spawn.enabled", "true").trim());
    }

    /** 【非官方】solo / small party: a cleared beast room sends its members on to the next empty uncleared room. */
    public static boolean autoRouteEnabled() {
        return !"false".equalsIgnoreCase(setting("tms282.kaling.autoRoute.enabled", "true").trim());
    }

    /** Lobby wait before the initial rooms are assigned (273: 300 s select timer). */
    public static int selectSeconds() {
        try {
            int v = Integer.parseInt(setting("tms282.kaling.selectSec", "10").trim());
            return v <= 0 ? 300 : Math.min(v, 300);
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    /** Fight stages 40/80/120/160/200 = goongi, dool, hondon, Karing, rampaging Karing (273 boss_karing_*: 8880830..). */
    public static int stageMob(int fieldId) {
        int base = baseForField(fieldId);
        if (base == 0) return 0;
        int mobBase = 0;
        for (int i = 0; i < BASES.length; i++) if (BASES[i] == base) mobBase = MOBS[i];
        return switch (fieldId - base) {
            case 40 -> mobBase;
            case 80 -> mobBase + 1;
            case 120 -> mobBase + 2;
            case 160 -> mobBase + 7;
            case 200 -> mobBase + 12;
            default -> 0;
        };
    }

    /** Map sp portal x on the main floor (same layout for all four difficulties; wz265 fight maps). */
    public static int[] stagePosition(int stage) {
        return switch (stage) {
            case 40 -> new int[]{568, 106};
            case 80 -> new int[]{556, 405};
            case 120 -> new int[]{634, 106};
            case 160 -> new int[]{568, 106};
            case 200 -> new int[]{-545, 398};
            default -> new int[]{0, 0};
        };
    }

    /** First uncleared beast room without anyone of the instance in it (273 CheckStage order goongi, dool, hondon). */
    public static int nextEmptyRoom(Instance instance, int base) {
        int[] rooms = {40, 80, 120};
        for (int i = 0; i < rooms.length; i++) {
            if (instance.hasProperty("KalingClear" + (i + 1))) continue;
            int room = rooms[i];
            boolean occupied = false;
            for (Char c : new ArrayList<>(instance.getChars().values())) {
                int stage = c.getFieldID() - base;
                if (c.getInstance() == instance && (stage == room || stage == room - 20)) occupied = true;
            }
            if (!occupied) return base + room - 20; // the room's direction field (plays its intro, then +20)
        }
        return 0;
    }

    private static void distribute(Instance instance, int base) {
        var members = new ArrayList<>(instance.getParty() == null
                ? instance.getChars().values() : instance.getParty().getOnlineChars());
        members.removeIf(chr -> chr.getInstance() != instance || chr.getFieldID() != base);
        if (members.isEmpty()) return;
        instance.setTimeout(30 * 60, true);
        for (int i = 0; i < members.size(); i++) members.get(i).warp(base + new int[]{100, 60, 20}[i % 3]);
    }

    public static int deathStage(int fieldId, int mobId) {
        int base = baseForField(fieldId);
        if (base == 0) return 0;
        int mobBase = 0;
        for (int i = 0; i < BASES.length; i++) if (BASES[i] == base) mobBase = MOBS[i];
        int stage = fieldId - base;
        if (stage == 40 && mobId == mobBase) return 1;
        if (stage == 80 && mobId == mobBase + 1) return 2;
        if (stage == 120 && mobId == mobBase + 2) return 3;
        if (stage == 160 && mobId == mobBase + 7) return 4;
        if (stage == 200 && mobId == mobBase + 12) return 5;
        return 0;
    }

    public static void onMobDeath(Field field, int mobId) {
        if (!enabled()) return;
        int stage = deathStage(field.getId(), mobId);
        if (stage == 0 || stage == 5) return; // Final arena remains available for the shared loot window.
        Instance instance = field.getChars().stream().map(Char::getInstance).filter(i -> i != null).findFirst().orElse(null);
        if (instance == null) return;
        int base = baseForField(field.getId());
        int target = base + (stage == 4 ? 180 : 140);
        synchronized (instance) {
            instance.setProperty("KalingClear" + stage, true);
            if (stage < 4 && !(instance.hasProperty("KalingClear1") && instance.hasProperty("KalingClear2")
                    && instance.hasProperty("KalingClear3"))) {
                // bossflow-0930 【非官方】: 273 opens the portals (reactors doolPt/hondonPt...) to the other rooms; the
                // 282 server has no such portal, so members of a cleared room move to the next room nobody fights in.
                if (autoRouteEnabled()) {
                    int room = field.getId();
                    field.getTimer().addEvent(() -> {
                        int next;
                        synchronized (instance) {
                            next = nextEmptyRoom(instance, base);
                        }
                        if (next == 0) return;
                        for (Char member : new ArrayList<>(instance.getChars().values())) {
                            if (member.getInstance() == instance && member.getFieldID() == room) member.warp(next);
                        }
                    }, 3, TimeUnit.SECONDS);
                }
                return;
            }
            if (instance.hasProperty("KalingRoute" + target)) return;
            instance.setProperty("KalingRoute" + target, true);
        }
        field.getTimer().addEvent(() -> {
            for (Char member : new ArrayList<>(instance.getChars().values())) {
                int current = member.getFieldID() - base;
                boolean source = stage == 4 ? current == 160 : current == 40 || current == 80 || current == 120;
                if (source && member.getInstance() == instance) member.warp(target);
            }
        }, stage == 4 ? 5 : 3, TimeUnit.SECONDS);
    }
}
