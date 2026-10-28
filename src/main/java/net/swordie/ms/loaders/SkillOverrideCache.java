package net.swordie.ms.loaders;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.Map;

public final class SkillOverrideCache {
    private static final int MAGIC = 0x53434732; // SCG2
    private static final int VERSION = 1;
    private static final int BASE_MAX_LEVEL = 1;
    private static final int BASE_MASTER_LEVEL = 1 << 1;
    private static final int BASE_FIX_LEVEL = 1 << 2;
    private static final int BASE_V_SKILL = 1 << 3;
    private static final int BASE_HYPER = 1 << 4;
    private static final int BASE_BOOST_DAM_R = 1 << 5;
    private static final int GUI_BASE_FIELDS = BASE_MAX_LEVEL
            | BASE_MASTER_LEVEL
            | BASE_FIX_LEVEL
            | BASE_V_SKILL
            | BASE_HYPER
            | BASE_BOOST_DAM_R;

    private static final File CACHE_FILE = new File(ServerConstants.DIR + "/data/skill.cg");
    private static final Int2ObjectMap<SkillOverride> overrides = new Int2ObjectOpenHashMap<>();
    private static boolean loaded;

    private SkillOverrideCache() {
    }

    public static synchronized int applyAllToMemory() {
        ensureLoaded();
        int applied = 0;
        for (var entry : overrides.int2ObjectEntrySet()) {
            SkillInfo info = SkillData.getSkillInfoById(entry.getIntKey());
            if (info != null) {
                entry.getValue().applyTo(info);
                applied++;
            }
        }
        if (applied > 0) {
            System.out.printf("[Skill Cache] Applied %d skill overrides from data/skill.cg%n", applied);
        }
        return applied;
    }

    public static synchronized void saveSkill(SkillInfo info) throws IOException {
        ensureLoaded();
        overrides.put(info.getSkillId(), SkillOverride.from(info));
        writeAll();
    }

    public static synchronized boolean hasOverride(int skillId) {
        ensureLoaded();
        return overrides.containsKey(skillId);
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        overrides.clear();
        if (!CACHE_FILE.isFile() || CACHE_FILE.length() == 0L) {
            loaded = true;
            return;
        }
        long start = System.currentTimeMillis();
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(CACHE_FILE), 1024 * 1024))) {
            int magic = in.readInt();
            int version = in.readInt();
            if (magic != MAGIC || version != VERSION) {
                throw new IOException("Unsupported skill.cg format");
            }
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                SkillOverride override = SkillOverride.read(in);
                overrides.put(override.skillId, override);
            }
            System.out.printf("[Skill Cache] Loaded %d skill overrides from data/skill.cg in %dms%n",
                    overrides.size(), System.currentTimeMillis() - start);
        } catch (EOFException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT,
                    new IOException("data/skill.cg is incomplete; ignored to protect WZ skill data", e));
            overrides.clear();
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            overrides.clear();
        }
        loaded = true;
    }

    private static void writeAll() throws IOException {
        Util.makeDirIfAbsent(CACHE_FILE.getParent());
        File tmp = new File(CACHE_FILE.getParentFile(), CACHE_FILE.getName() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmp), 1024 * 1024))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeInt(overrides.size());
            for (var entry : overrides.int2ObjectEntrySet()) {
                entry.getValue().write(out);
            }
        }
        try {
            Files.move(tmp.toPath(), CACHE_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(tmp.toPath(), CACHE_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len < 0 || len > 1_048_576) {
            throw new IOException("Invalid skill.cg string length: " + len);
        }
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static final class SkillOverride {
        private int skillId;
        private int baseMask;
        private int maxLevel;
        private int masterLevel;
        private int fixLevel;
        private int vSkill;
        private int hyper;
        private int boostDamR;
        private final EnumMap<SkillStat, String> stats = new EnumMap<>(SkillStat.class);

        private static SkillOverride from(SkillInfo info) {
            SkillOverride override = new SkillOverride();
            override.skillId = info.getSkillId();
            override.baseMask = GUI_BASE_FIELDS;
            override.maxLevel = info.getMaxLevel();
            override.masterLevel = info.getMasterLevel();
            override.fixLevel = info.getFixLevel();
            override.vSkill = info.getVSkill();
            override.hyper = info.getHyper();
            override.boostDamR = info.getBoostDamR();
            override.stats.putAll(info.getSkillStatInfo());
            return override;
        }

        private static SkillOverride read(DataInputStream in) throws IOException {
            SkillOverride override = new SkillOverride();
            override.skillId = in.readInt();
            override.baseMask = in.readInt();
            if ((override.baseMask & BASE_MAX_LEVEL) != 0) {
                override.maxLevel = in.readInt();
            }
            if ((override.baseMask & BASE_MASTER_LEVEL) != 0) {
                override.masterLevel = in.readInt();
            }
            if ((override.baseMask & BASE_FIX_LEVEL) != 0) {
                override.fixLevel = in.readInt();
            }
            if ((override.baseMask & BASE_V_SKILL) != 0) {
                override.vSkill = in.readInt();
            }
            if ((override.baseMask & BASE_HYPER) != 0) {
                override.hyper = in.readInt();
            }
            if ((override.baseMask & BASE_BOOST_DAM_R) != 0) {
                override.boostDamR = in.readInt();
            }
            int statCount = in.readUnsignedShort();
            SkillStat[] values = SkillStat.values();
            for (int i = 0; i < statCount; i++) {
                int ordinal = in.readUnsignedShort();
                String expr = readString(in);
                if (ordinal < values.length) {
                    override.stats.put(values[ordinal], expr);
                }
            }
            return override;
        }

        private void write(DataOutputStream out) throws IOException {
            out.writeInt(skillId);
            out.writeInt(baseMask);
            if ((baseMask & BASE_MAX_LEVEL) != 0) {
                out.writeInt(maxLevel);
            }
            if ((baseMask & BASE_MASTER_LEVEL) != 0) {
                out.writeInt(masterLevel);
            }
            if ((baseMask & BASE_FIX_LEVEL) != 0) {
                out.writeInt(fixLevel);
            }
            if ((baseMask & BASE_V_SKILL) != 0) {
                out.writeInt(vSkill);
            }
            if ((baseMask & BASE_HYPER) != 0) {
                out.writeInt(hyper);
            }
            if ((baseMask & BASE_BOOST_DAM_R) != 0) {
                out.writeInt(boostDamR);
            }
            out.writeShort(stats.size());
            for (Map.Entry<SkillStat, String> entry : stats.entrySet()) {
                out.writeShort(entry.getKey().ordinal());
                writeString(out, entry.getValue());
            }
        }

        private void applyTo(SkillInfo info) {
            if ((baseMask & BASE_MAX_LEVEL) != 0) {
                info.setMaxLevel(maxLevel);
            }
            if ((baseMask & BASE_MASTER_LEVEL) != 0) {
                info.setMasterLevel(masterLevel);
            }
            if ((baseMask & BASE_FIX_LEVEL) != 0) {
                info.setFixLevel(fixLevel);
            }
            if ((baseMask & BASE_V_SKILL) != 0) {
                info.setVSkill(vSkill);
            }
            if ((baseMask & BASE_HYPER) != 0) {
                info.setHyper(hyper);
            }
            if ((baseMask & BASE_BOOST_DAM_R) != 0) {
                info.setBoostDamR(boostDamR);
            }
            for (Map.Entry<SkillStat, String> entry : stats.entrySet()) {
                info.addSkillStatInfo(entry.getKey(), entry.getValue());
            }
            info.clearCalculatedCache();
            info.load();
        }
    }
}