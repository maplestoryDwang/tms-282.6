package net.swordie.ms.scripts;

import net.swordie.ms.Server;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.AccountQuest;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.*;
import net.swordie.ms.client.character.avatar.AvatarLook;
import net.swordie.ms.client.character.damage.DamageSkinSaveData;
import net.swordie.ms.client.character.damage.DamageSkinType;
import net.swordie.ms.client.character.items.*;
import net.swordie.ms.client.character.potential.CharacterPotential;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.scene.Scene;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.TimedSkills;
import net.swordie.ms.client.character.skills.matrix.MatrixCore;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatBase;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.character.union.Union;
import net.swordie.ms.client.character.union.UnionMember;
import net.swordie.ms.client.jobs.nova.Kaiser;
import net.swordie.ms.client.social.Alliance.Alliance;
import net.swordie.ms.client.social.Alliance.AllianceResult;
import net.swordie.ms.client.social.Guild.Guild;
import net.swordie.ms.client.social.Guild.GuildMember;
import net.swordie.ms.client.social.Guild.GuildResult;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.client.social.Party.PartyResult;
import net.swordie.ms.client.trunk.TrunkOpen;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.connection.protocol.codec.ScriptMessageEncoder;
import net.swordie.ms.console.config.BossRuntimeConfig;
import net.swordie.ms.constants.*;
import net.swordie.ms.enums.*;
import net.swordie.ms.enums.social.Event.InGameEventType;
import net.swordie.ms.enums.social.Guild.GuildType;
import net.swordie.ms.handlers.GlobalTimerManager;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.handlers.social.RoomHandler;
import net.swordie.ms.handlers.ui.EventNameTagHandler;
import net.swordie.ms.handlers.ui.MatrixHandler;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Reactor;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobZoneInfo;
import net.swordie.ms.life.mob.skill.MobSkill;
import net.swordie.ms.life.mob.skill.MobSkillID;
import net.swordie.ms.life.npc.Npc;
import net.swordie.ms.life.npc.NpcMessageType;
import net.swordie.ms.life.npc.NpcScriptInfo;
import net.swordie.ms.loaders.Etc.VCore.VCore;
import net.swordie.ms.loaders.Etc.VCore.VCoreData;
import net.swordie.ms.loaders.Etc.HexaCore.HexaCore;
import net.swordie.ms.loaders.*;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.loaders.containerclasses.MobSkillInfo;
import net.swordie.ms.util.*;
import net.swordie.ms.world.World;
import net.swordie.ms.world.boss.BossHelper;
import net.swordie.ms.world.boss.BossManager;
import net.swordie.ms.world.event.*;
import net.swordie.ms.world.field.*;
import net.swordie.ms.world.field.fieldeffect.FieldEffect;
import net.swordie.ms.world.field.fieldeffect.GreyFieldType;
import net.swordie.ms.world.field.obtacleatom.ObtacleAtomInfo;
import net.swordie.ms.world.field.obtacleatom.ObtacleInRowInfo;
import net.swordie.ms.world.field.obtacleatom.ObtacleRadianInfo;
import net.swordie.ms.world.gach.result.GachaponDlgType;
import net.swordie.ms.world.shop.CustomShopCurrency;
import net.swordie.ms.world.shop.CustomShopData;
import net.swordie.ms.world.shop.CustomShopItem;
import net.swordie.ms.world.shop.HypayManager;
import net.swordie.ms.world.shop.ItemWarehouseManager;
import net.swordie.ms.world.shop.NpcShopDlg;

import javax.script.*;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.enums.ChatType.*;
import static net.swordie.ms.enums.EquipBaseStat.*;
import static net.swordie.ms.enums.InvType.*;
import static net.swordie.ms.enums.InventoryOperation.*;
import static net.swordie.ms.life.mob.skill.MobSkillStat.*;
import static net.swordie.ms.life.npc.NpcMessageType.*;

public class ScriptManagerImpl implements ScriptManager {

    /** This separated server keeps login and field entry without game scripts or dialogs. */
    public static final boolean GAME_SCRIPTS_ENABLED = false;

    private static final ExecutorService service = Executors.newVirtualThreadPerTaskExecutor();

    public static final String PYTHON_SCRIPT_ENGINE_NAME = "python";
    public static final String PYTHON_SCRIPT_ENGINE_EXTENSION = ".py";
    public static final String JAVASCRIPT_SCRIPT_ENGINE_NAME = "nashorn";
    public static final String JAVASCRIPT_SCRIPT_ENGINE_EXTENSION = ".js";

    /**
     * 保留舊常量語義：現有 Python 腳本和外部調用仍默認按 .py 判斷。
     */
    public static final String SCRIPT_ENGINE_NAME = PYTHON_SCRIPT_ENGINE_NAME;
    public static final String SCRIPT_ENGINE_EXTENSION = PYTHON_SCRIPT_ENGINE_EXTENSION;
    private static final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    private static final NashornScriptEngineFactory nashornScriptEngineFactory = new NashornScriptEngineFactory();

    public static final String QUEST_START_SCRIPT_END_TAG = "s";
    public static final String QUEST_COMPLETE_SCRIPT_END_TAG = "e";

    private static final String DEFAULT_SCRIPT = "undefined";
    public static final String INTENDED_NPE_MSG = "Intended NPE by forceful script stop.";
    private static final Pattern PYTHON_ENCODING_DECLARATION = Pattern.compile(
            "(?m)^#.*coding[:=]\\s*[-\\w.]+.*$");

    private String lastActiveScriptName;
    private volatile ScriptType lastActiveScriptType;
    private ScriptMemory memory = new ScriptMemory();

    private Char chr;
    private Field field;
    private volatile Object bossIntroToken;
    private volatile Field bossIntroField;
    private NpcScriptInfo npcScriptInfo;
    private Map<ScriptType, ScriptInfo> scripts;
    private final Map<Integer, Npc> scriptNpcControllers = new java.util.concurrent.ConcurrentHashMap<>();
    private Set<ScheduledFuture> events = new HashSet<>();

    private int returnField = 0;
    private int returnPortal = 0;
    private int objectID;

    private FieldTransferInfo fieldTransferInfo;

    private boolean isField;
    private boolean curNodeEventEnd;
    private boolean inGameUILocked;

    private ScriptManagerImpl(Char chr, Field field) {
        this.chr = chr;
        this.field = field;
        this.npcScriptInfo = new NpcScriptInfo();
        this.scripts = new HashMap<>();
        this.isField = chr == null;
        this.lastActiveScriptName = "Unknown";
        this.lastActiveScriptType = ScriptType.None;
        this.fieldTransferInfo = new FieldTransferInfo();
        this.inGameUILocked = false;
    }

    public ScriptManagerImpl(Char chr) {
        this(chr, chr.getField());
    }

    public ScriptManagerImpl(Field field) {
        this(null, field);
    }

    private Bindings getBindingsByType(ScriptType scriptType) {
        ScriptInfo si = getScriptInfoByType(scriptType);
        return si == null ? null : si.getBindings();
    }

    public ScriptInfo getScriptInfoByType(ScriptType scriptType) {
        return scripts.getOrDefault(scriptType, null);
    }

    @Override
    public Char getChr() {
        return chr;
    }

    public void setCharacter(Char chr) {
        this.chr = chr;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public String getScriptNameByType(ScriptType scriptType) {
        return getScriptInfoByType(scriptType).getScriptName();
    }

    public Invocable getInvocableByType(ScriptType scriptType) {
        return getScriptInfoByType(scriptType).getInvocable();
    }

    public int getParentIDByScriptType(ScriptType scriptType) {
        return getScriptInfoByType(scriptType) != null ? getScriptInfoByType(scriptType).getParentID() : 2007;
    }

    public int getObjectIDByScriptType(ScriptType scriptType) {
        return getScriptInfoByType(scriptType) != null ? getScriptInfoByType(scriptType).getObjectID() : 0;
    }

    private boolean isQuestScriptAllowed() {
        return getLastActiveScriptType() == ScriptType.None;
    }

    private void notifyMobDeath(Mob mob) {
        if (isActive(ScriptType.FirstEnterField)) {
            getScriptInfoByType(ScriptType.FirstEnterField).addResponseMob(mob);
        } else if (isActive(ScriptType.Field)) {
            getScriptInfoByType(ScriptType.Field).addResponseMob(mob);
        }
    }

    public boolean startScript(Char chr, int parentID, String scriptName, ScriptType scriptType) {
        if (!GAME_SCRIPTS_ENABLED) return false;
        if (!BossManager.handle(chr, parentID, scriptName)) {
            if (BossManager.check(parentID, scriptName)) {
                scriptType = ScriptType.Boss;
            }
            return start(chr, parentID, 0, scriptName, scriptType);
        }
        return start(chr, parentID, 0, scriptName, scriptType);
    }

    public boolean startScript(Char chr, int parentID, int objID, String scriptName, ScriptType scriptType) {
        if (!GAME_SCRIPTS_ENABLED) return false;
        if (!BossManager.handle(chr, parentID, scriptName)) {
            if (BossManager.check(parentID, scriptName)) {
                scriptType = ScriptType.Boss;
            }
            return start(chr, parentID, objID, scriptName, scriptType);
        }
        return false;
    }

    public boolean start(Char chr, int parentID, int objID, String scriptName, ScriptType scriptType) {
        if (!GAME_SCRIPTS_ENABLED) return false;
        if (!isField()) {
            System.out.printf("%s: Starting script %s, scriptType %s.%n", chr.getName(), scriptName, scriptType);
        }
        if (scriptType == ScriptType.None || (scriptType == ScriptType.Quest && !isQuestScriptAllowed())) {
            return false;
        }
        if (scriptType == ScriptType.Item && ScriptHandlers.handleItemScripts(chr, parentID, objID, scriptName, scriptType)) {
            chr.dispose();
            return true;
        }
        ScriptFile scriptFile = resolveScriptFile(scriptType, scriptName);
        if (scriptFile == null) {
            if (scriptType == ScriptType.Portal && scriptName != null && scriptName.matches("hid\\d+")) {
                exitBoss();
                return true;
            }
            // 282 script item without a script file: apply what its item data defines (damage skin, ride skill, ...).
            if (scriptType == ScriptType.Item && chr != null
                    && net.swordie.ms.world.tms282.items.ItemDataEffects.tryApply(chr, parentID)) {
                chr.dispose();
                return true;
            }
            undefined(scriptType, parentID);
            return true;
        }
        if (isActive(scriptType) && scriptType != ScriptType.Field) { // because Field Scripts don't get disposed.
            progressMessageFont(3, 17, 7, 1000, "操作太快了！請稍後再試。");
            return false;
        }
        if (chr != null) {
            setCharacter(chr);
            setField(chr.getField());
        }
        setLastActiveScriptType(scriptType);
        setLastActiveScriptName(scriptName);
        setObjectID(objID);
        resetParam();
        Bindings bindings = createScriptBindings(chr, scriptType, parentID, objID);
        if (scriptType == ScriptType.Reactor) {
            bindings.put("reactor", chr == null ? getField().getLifeByObjectID(objID) : chr.getField().getLifeByObjectID(objID));
        }
        if (scriptType == ScriptType.Quest) {
            bindings.put("startQuest", scriptName.charAt(scriptName.length() - 1) == QUEST_START_SCRIPT_END_TAG.charAt(0)); // biggest hack eu
        }
        ScriptInfo si = new ScriptInfo(scriptType, bindings, parentID, scriptName);
        if (scriptType == ScriptType.Npc || scriptType == ScriptType.Boss) {
            getNpcScriptInfo().setTemplateID(parentID);
        }
        getNpcScriptInfo().setObjectID(objID);
        si.setObjectID(objID);
        getScripts().put(scriptType, si);
        service.execute(() -> executeScript(scriptName, si, true));
        return true;
    }

    @Override
    public void runScript(String scriptPath) {
        if (!GAME_SCRIPTS_ENABLED) return;
        if (chr == null || scriptPath == null || scriptPath.isBlank()) {
            return;
        }
        String scriptName = scriptPath.replace('\\', '/');
        if (scriptName.startsWith("/")) {
            scriptName = scriptName.substring(1);
        }
        ScriptFile scriptFile = resolveScriptFile(ScriptType.Content, scriptName);
        if (scriptFile == null) {
            chat(String.format("找不到腳本：%s", scriptPath));
            System.out.printf("Script not found: %s%n", buildScriptPath(ScriptType.Content, scriptName, SCRIPT_ENGINE_EXTENSION));
            return;
        }
        ScriptType previousType = getLastActiveScriptType();
        String previousName = getLastActiveScriptName();
        ScriptType scriptType = ScriptType.Content;
        Bindings bindings = createScriptBindings(chr, scriptType, 0, 0);
        ScriptInfo si = new ScriptInfo(scriptType, bindings, 0, scriptName);
        getScripts().put(scriptType, si);
        setLastActiveScriptType(scriptType);
        setLastActiveScriptName(scriptName);
        try {
            executeScript(scriptName, si, false);
        } finally {
            if (previousType != null && previousType != ScriptType.None) {
                setLastActiveScriptType(previousType);
                setLastActiveScriptName(previousName);
            }
        }
    }

    /**
     * Runs a server-side (Java) NPC dialog exactly like an NPC script (newbie flow, step 7): the same
     * {@link ScriptType#Npc} ScriptInfo, the speaker {@code npcTemplateId}, the same send / await methods and CP012C
     * answers, and the same end (stop, field transfer, chr.dispose). Refused (false) while any script of this
     * character is active. A closed dialog / logout ends {@code body} with the intended stop exception.
     */
    public boolean startJavaDialog(Char chr, int npcTemplateId, String name, java.util.function.Consumer<ScriptManagerImpl> body) {
        if (!GAME_SCRIPTS_ENABLED) return false;
        if (chr == null || body == null || name == null) {
            return false;
        }
        final ScriptInfo si;
        synchronized (this) {
            if (getLastActiveScriptType() != ScriptType.None || isActive(ScriptType.Npc)) {
                return false;
            }
            setCharacter(chr);
            setField(chr.getField());
            setLastActiveScriptType(ScriptType.Npc);
            setLastActiveScriptName(name);
            setObjectID(0);
            resetParam(); resetJavaDialogSpeaker(); // hotfix 7d: speakerType 4 even after an earlier stop() (see the method)
            si = new ScriptInfo(ScriptType.Npc, null, npcTemplateId, name);
            getNpcScriptInfo().setTemplateID(npcTemplateId);
            getNpcScriptInfo().setObjectID(0);
            si.setObjectID(0);
            si.setActive(true);
            getScripts().put(ScriptType.Npc, si);
        }
        System.out.printf("%s: Starting dialog %s (server-side), speaker %d.%n", chr.getName(), name, npcTemplateId);
        service.execute(() -> {
            try {
                body.accept(this);
            } catch (NullPointerException e) {
                if (e.getMessage() == null || !e.getMessage().contains(INTENDED_NPE_MSG)) {
                    DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                    unlockUIIfNeeded();
                }
            } catch (RuntimeException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                unlockUIIfNeeded();
            } finally {
                synchronized (this) {
                    // ESC can stop this run before its worker notices. Never let its late cleanup reset a newer run.
                    if (getScripts().get(ScriptType.Npc) != si) return;
                    stop(ScriptType.Npc);
                }
                FieldTransferInfo fti = getFieldTransferInfo();
                try {
                    if (!fti.isInit()) {
                        fti.warp(chr);
                    }
                    System.out.printf("%s: Completed dialog %s (server-side).%n", chr.getName(), name);
                    chr.dispose();
                } catch (RuntimeException e) {
                    DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                }
            }
        });
        return true;
    }

    private void executeScript(String name, ScriptInfo si, boolean disposeOnComplete) {
        if (si == null) {
            return;
        }
        final ScriptType scriptType = si.getScriptType();
        final ScriptFile scriptFile = resolveScriptFile(scriptType, name);
        if (scriptFile == null) {
            unlockUIIfNeeded();
            System.out.printf("Script not found: %s/%s/%s.[py|js]%n", ServerConstants.SCRIPT_DIR, scriptType.getDir().toLowerCase(), name);
            return;
        }
        si.setActive(true);
        si.setFileDir(scriptFile.path());
        CompiledScript cs;
        StringBuilder script = new StringBuilder();
        ScriptEngine se = createScriptEngine(scriptFile);
        if (se == null) {
            unlockUIIfNeeded();
            DataPrinter.send(DataPrinter.SCRIPTS,
                    String.format("無法爲腳本 [%2$s] 創建腳本引擎 [%1$s]。", scriptFile.engineName(), name), true);
            return;
        }
        // leftovers-1005: use this run's own bindings. Looking them up by type raced with dispose() (logout,
        // @dispose, a stale session's ChannelInactive) clearing the scripts map between start() and this worker:
        // CompiledScript.eval(null) then ran on the engine's empty context -> "NameError: name 'sm' is not defined".
        Bindings bindings = si.getBindings();
        if (bindings == null) {
            bindings = createScriptBindings(chr, scriptType, si.getParentID(), si.getObjectID());
            si.setBindings(bindings);
        }
        if (JAVASCRIPT_SCRIPT_ENGINE_EXTENSION.equals(scriptFile.extension())) {
            ScriptRequire scriptRequire = new ScriptRequire(se);
            bindings.put("__scriptRequire", scriptRequire);
            // modules are evaluated in the engine's own context (ScriptRequire.require -> engine.eval): without it there,
            // a module that requires another module failed with ReferenceError "__scriptRequire" (dungeon menu: fire wolf trial / party monster park)
            se.put("__scriptRequire", scriptRequire);
        }
        si.setInvocable((Invocable) se);
        try {
            String context = normalizeScriptSource(Util.readFile(scriptFile.path(), StandardCharsets.UTF_8), scriptFile.extension());
            script.append(context);
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            unlockUIIfNeeded();
        }
        try {
            if (!script.isEmpty()) {
                cs = ((Compilable) se).compile(script.toString());
                cs.eval(bindings);
                invokeJavascriptStartIfPresent(scriptFile, si);
            }
        } catch (ScriptException e) {
            if (!isIntendedStop(e)) {
                e.printStackTrace();
                Throwable cause = e.getCause();
                if (cause != null) {
                    cause.printStackTrace();
                }
                DataPrinter.send(DataPrinter.SCRIPTS,
                        String.format("無法運行腳本 [%s]！異常 [%s]，行號 %s。",
                                name, e.getMessage(), e.getLineNumber()), true);
                unlockUIIfNeeded();
                progressMessageFont(3, 20, 9, 1000,
                        "發生未知錯誤，請輸入 @sualoi 或 @dispose。");
            }
        } finally {
            stop(scriptType);
            FieldTransferInfo fti = getFieldTransferInfo();
            if (!fti.isInit()) {
                if (fti.isField()) {
                    fti.warp(field);
                } else {
                    fti.warp(chr);
                }
            }
            System.out.printf("%s: Completed script %s, scriptType %s.%n", chr != null ? chr.getName() : "Null", name, scriptType);
            if (disposeOnComplete && chr != null) {
                chr.dispose();
            }
        }
    }

    private String normalizeScriptSource(String source, String extension) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        String normalized = source;
        if (normalized.charAt(0) == '\uFEFF') {
            normalized = normalized.substring(1);
        }
        normalized = normalized.replace("\r\n", "\n").replace('\r', '\n');
        if (PYTHON_SCRIPT_ENGINE_EXTENSION.equals(extension)) {
            normalized = removePythonEncodingDeclaration(normalized);
            return "from __future__ import unicode_literals\n" + normalized;
        }
        return buildJavascriptPrelude() + normalized;
    }

    private String buildJavascriptPrelude() {
        return "if (typeof require === 'undefined') { var require = function(path) { return __scriptRequire.require(String(path)); }; }"
                + " if (!Array.prototype.includes) { Object.defineProperty(Array.prototype, 'includes', { value: function(x) { return this.indexOf(x) !== -1; } }); }"
                + " if (!String.prototype.includes) { Object.defineProperty(String.prototype, 'includes', { value: function(s) { return String(this).indexOf(s) !== -1; } }); }"
                + " if (!String.prototype.repeat) { Object.defineProperty(String.prototype, 'repeat', { value: function(n) { var s = String(this), r = ''; n = Math.floor(n); while (n-- > 0) { r += s; } return r; } }); }"
                + " if (!String.prototype.padStart) { Object.defineProperty(String.prototype, 'padStart', { value: function(len, pad) { var s = String(this); pad = pad === undefined ? ' ' : String(pad); if (s.length >= len || pad === '') return s; var fill = ''; while (fill.length < len - s.length) { fill += pad; } return fill.slice(0, len - s.length) + s; } }); }"
                + " if (!String.prototype.padEnd) { Object.defineProperty(String.prototype, 'padEnd', { value: function(len, pad) { var s = String(this); pad = pad === undefined ? ' ' : String(pad); if (s.length >= len || pad === '') return s; var fill = ''; while (fill.length < len - s.length) { fill += pad; } return s + fill.slice(0, len - s.length); } }); }"
                + " if (!String.prototype.startsWith) { Object.defineProperty(String.prototype, 'startsWith', { value: function(s, pos) { pos = pos || 0; return String(this).indexOf(s, pos) === pos; } }); }"
                + " if (!String.prototype.endsWith) { Object.defineProperty(String.prototype, 'endsWith', { value: function(s, end) { var str = String(this); if (end === undefined || end > str.length) end = str.length; return str.slice(end - String(s).length, end) === String(s); } }); }"
                + " if (!Array.prototype.find) { Object.defineProperty(Array.prototype, 'find', { value: function(cb, thisArg) { for (var i = 0; i < this.length; i++) { if (cb.call(thisArg, this[i], i, this)) return this[i]; } return undefined; } }); }"
                + " if (!Array.prototype.findIndex) { Object.defineProperty(Array.prototype, 'findIndex', { value: function(cb, thisArg) { for (var i = 0; i < this.length; i++) { if (cb.call(thisArg, this[i], i, this)) return i; } return -1; } }); }"
                + " if (!Object.values) { Object.defineProperty(Object, 'values', { value: function(o) { var r = []; for (var k in o) { if (Object.prototype.hasOwnProperty.call(o, k)) r.push(o[k]); } return r; } }); }"
                + " if (!Object.entries) { Object.defineProperty(Object, 'entries', { value: function(o) { var r = []; for (var k in o) { if (Object.prototype.hasOwnProperty.call(o, k)) r.push([k, o[k]]); } return r; } }); }"
                + " if (typeof dir === 'undefined' || typeof usr === 'undefined' || typeof eff === 'undefined') { var __noopChain = new JSAdapter({ __call__: function() { return __noopChain; }, __get__: function(name) { return function() { return __noopChain; }; } }); if (typeof dir === 'undefined') { var dir = __noopChain; } if (typeof usr === 'undefined') { var usr = __noopChain; } if (typeof eff === 'undefined') { var eff = __noopChain; } }"
                + " if (typeof console === 'undefined') { var console = { log: function(m) { print('[script] ' + m); }, error: function(m) { print('[script:ERROR] ' + m); }, warn: function(m) { print('[script:WARN] ' + m); }, info: function(m) { print('[script] ' + m); } }; }\n";
    }

    private String removePythonEncodingDeclaration(String source) {
        String[] lines = source.split("\n", -1);
        int checkedLines = Math.min(lines.length, 2);
        for (int i = 0; i < checkedLines; i++) {
            if (PYTHON_ENCODING_DECLARATION.matcher(lines[i]).matches()) {
                lines[i] = "";
            }
        }
        return String.join("\n", lines);
    }

    private Bindings createScriptBindings(Char chr, ScriptType scriptType, int parentID, int objID) {
        Bindings bindings = new SimpleBindings();
        ScriptManagerImpl activeManager = chr == null ? this : chr.getScriptManager();
        bindings.put("sm", activeManager);
        bindings.put("stage", activeManager);
        bindings.put("npc", activeManager);
        bindings.put("cm", activeManager);
        bindings.put("player", activeManager);
        bindings.put("chr", chr);
        bindings.put("field", chr == null ? getField() : chr.getField());
        bindings.put("map", createMapBinding(chr));
        bindings.put("util", new ScriptUtilBinding());
        bindings.put("parentID", parentID);
        bindings.put("scriptType", scriptType);
        bindings.put("objectID", objID);
        bindings.put("db", activeManager);
        // script-pack compatibility bindings
        bindings.put("ctx", new ScriptContextBinding(activeManager, parentID));
        bindings.put("sh", activeManager);
        bindings.put("party", chr != null && chr.getParty() != null ? new PartyBinding(chr) : null);
        activeManager.ensureCompatAccountRow();
        return bindings;
    }

    private Map<String, Object> createMapBinding(Char chr) {
        Field activeField = chr == null ? getField() : chr.getField();
        return new ScriptMapBinding(activeField);
    }

    public class ScriptRequire {
        private final ScriptEngine engine;
        private final Map<String, Object> cache = new HashMap<>();

        private ScriptRequire(ScriptEngine engine) {
            this.engine = engine;
        }

        public Object require(String requestedPath) throws ScriptException {
            String normalizedPath = normalizeRequiredPath(requestedPath);
            File moduleFile = resolveRequiredModule(normalizedPath);
            if (moduleFile == null) {
                throw new ScriptException("Cannot find script module: " + requestedPath);
            }
            String cacheKey = moduleFile.getAbsolutePath();
            if (cache.containsKey(cacheKey)) {
                return cache.get(cacheKey);
            }
            try {
                String moduleSource = normalizeScriptSource(Util.readFile(cacheKey, StandardCharsets.UTF_8), JAVASCRIPT_SCRIPT_ENGINE_EXTENSION);
                String wrapper = "(function(){ var exports = {}; var module = { exports: exports };\n"
                        + moduleSource
                        + "\n; return module.exports; })();";
                Object exports = engine.eval(wrapper);
                cache.put(cacheKey, exports);
                return exports;
            } catch (IOException e) {
                throw new ScriptException(e);
            }
        }

        private String normalizeRequiredPath(String requestedPath) {
            String normalized = requestedPath == null ? "" : requestedPath.replace('\\', '/').trim();
            while (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            if (normalized.endsWith(JAVASCRIPT_SCRIPT_ENGINE_EXTENSION)) {
                normalized = normalized.substring(0, normalized.length() - JAVASCRIPT_SCRIPT_ENGINE_EXTENSION.length());
            }
            if (normalized.contains("..")) {
                throw new IllegalArgumentException("Illegal script module path: " + requestedPath);
            }
            return normalized;
        }

        private File resolveRequiredModule(String normalizedPath) {
            String contentRoot = ServerConstants.SCRIPT_DIR + "/" + ScriptType.Content.getDir().toLowerCase();
            List<String> candidates = List.of(
                    contentRoot + "/model/" + normalizedPath + JAVASCRIPT_SCRIPT_ENGINE_EXTENSION,
                    contentRoot + "/" + normalizedPath + JAVASCRIPT_SCRIPT_ENGINE_EXTENSION,
                    contentRoot + "/expand/" + normalizedPath + JAVASCRIPT_SCRIPT_ENGINE_EXTENSION
            );
            for (String candidate : candidates) {
                File file = new File(candidate);
                if (file.exists() && file.isFile()) {
                    return file;
                }
            }
            return null;
        }
    }

    public class ScriptUtilBinding {
        public String getItemName(int itemId) {
            return StringData.getItemStringById(itemId);
        }

        public String getMapName(int mapId) {
            return StringData.getMapStringById(mapId);
        }

        public boolean isItemExist(int itemId) {
            return ItemData.getItemInfoByID(itemId) != null;
        }

        public boolean isMapAvailable(int mapId) {
            return net.swordie.ms.loaders.FieldData.getFieldById(mapId) != null
                    && net.swordie.ms.connection.protocol.codec.FieldAvailability.active().isAvailable(mapId);
        }

        public void log(String message) {
            System.out.println("[ScriptUtil] " + message);
        }

        public void log(Object message) {
            System.out.println("[ScriptUtil] " + message);
        }

        // ---- script-pack compatibility (see compat layer at the bottom of ScriptManagerImpl) ----

        public Item makeItem(int itemId) {
            return ItemData.getItemDeepCopy(itemId);
        }

        public Item makeItem(int itemId, int quantity) {
            Item item = ItemData.getItemDeepCopy(itemId);
            if (item != null && quantity > 1) {
                item.setQuantity(quantity);
            }
            return item;
        }

        public String getMobName(int mobId) {
            String name = StringData.mobStrings.get(mobId);
            return name == null ? String.valueOf(mobId) : name;
        }

        public int getItemReqLevel(int itemId) {
            try {
                Equip eq = ItemData.getEquipDeepCopyFromID(itemId, false);
                return eq == null ? 0 : eq.getrLevel();
            } catch (Exception e) {
                return 0;
            }
        }

        public boolean isCashItem(int itemId) {
            ItemInfo info = ItemData.getItemInfoByID(itemId);
            return info != null && info.isCash();
        }

        public String getServerName() {
            return "楓之谷";
        }

        public String getStringDate(String format) {
            return new java.text.SimpleDateFormat(format).format(new java.util.Date());
        }

        public String getStringDate(long epochMillis, String format) {
            return new java.text.SimpleDateFormat(format).format(new java.util.Date(epochMillis));
        }

        public long getTimeStringToLong(String time, String format) {
            try {
                return new java.text.SimpleDateFormat(format).parse(time).getTime();
            } catch (Exception e) {
                return 0L;
            }
        }

        public FileTime calcExpireDay(int days) {
            return FileTime.fromDate(java.time.LocalDateTime.now().plusDays(days));
        }

        public boolean isSameWeek(long time1, long time2) {
            java.util.Calendar c1 = java.util.Calendar.getInstance();
            java.util.Calendar c2 = java.util.Calendar.getInstance();
            c1.setFirstDayOfWeek(java.util.Calendar.MONDAY);
            c2.setFirstDayOfWeek(java.util.Calendar.MONDAY);
            c1.setTimeInMillis(time1);
            c2.setTimeInMillis(time2);
            return c1.get(java.util.Calendar.YEAR) == c2.get(java.util.Calendar.YEAR)
                    && c1.get(java.util.Calendar.WEEK_OF_YEAR) == c2.get(java.util.Calendar.WEEK_OF_YEAR);
        }

        public void broadcastChatMsg(int type, String msg) {
            ScriptManagerImpl.this.broadcastWorldChat(msg);
        }

        public void broadcastWorldChatMsg(String msg) {
            ScriptManagerImpl.this.broadcastWorldChat(msg);
        }

        public void broadcastWorldChatMsg(int type, String msg) {
            ScriptManagerImpl.this.broadcastWorldChat(msg);
        }

        public void broadcastNotice(String msg) {
            ScriptManagerImpl.this.broadcastWorldChat(msg);
        }

        public void broadcastNoticeWithoutPrefix(String msg) {
            ScriptManagerImpl.this.broadcastWorldChat(msg);
        }

        public void broadcastWeatherEffectNotice(int type, String msg, int duration) {
            ScriptManagerImpl.this.broadcastWeatherEffectNotice(type, msg, duration);
        }

        public void broadcastGachaponMsg(String who, String msg, Object item) {
            ScriptManagerImpl.this.broadcastWorldChat(who + " " + msg);
        }

        public void broadcastQuestOpen(Object... args) {
            // quest-open broadcast not ported; scripts only use it for cosmetics
        }

        public void httpPost(String url, String body) {
            ScriptManagerImpl.this.httpPost(url, body);
        }

        /** {@code util.makeDrop(itemId, quantity, ...)}: the item to hand to {@code map.dropIndieItemsEx}. */
        public Object makeDrop(Object... args) {
            if (args == null || args.length == 0 || !(args[0] instanceof Number id)) {
                return null;
            }
            Item item = ItemData.getItemDeepCopy(id.intValue());
            if (item != null && args.length > 1 && args[1] instanceof Number qty && qty.intValue() > 1
                    && !ItemConstants.isEquip(id.intValue())) {
                item.setQuantity(qty.intValue());
            }
            return item;
        }

        public boolean isSameDay(long time1, long time2) {
            java.time.ZoneId zone = java.time.ZoneId.systemDefault();
            return java.time.Instant.ofEpochMilli(time1).atZone(zone).toLocalDate()
                    .equals(java.time.Instant.ofEpochMilli(time2).atZone(zone).toLocalDate());
        }

        public String getScriptPath() {
            return "";
        }
    }

    private ScriptEngine createScriptEngine(ScriptFile scriptFile) {
        if (JAVASCRIPT_SCRIPT_ENGINE_EXTENSION.equals(scriptFile.extension())) {
            return nashornScriptEngineFactory.getScriptEngine("--language=es6");
        }
        return scriptEngineManager.getEngineByName(scriptFile.engineName());
    }

    private void invokeJavascriptStartIfPresent(ScriptFile scriptFile, ScriptInfo si) throws ScriptException {
        if (!JAVASCRIPT_SCRIPT_ENGINE_EXTENSION.equals(scriptFile.extension()) || si.getInvocable() == null) {
            return;
        }
        try {
            si.getInvocable().invokeFunction("start");
        } catch (NoSuchMethodException ignored) {
            // 兼容 TMS273 這類頂層立即執行腳本；沒有 start() 時不做額外動作。
        }
    }

    public static boolean hasScriptFile(ScriptType scriptType, String scriptName) {
        return resolveScriptFile(scriptType, scriptName) != null;
    }

    /** The file {@code scriptName} would run from (same resolution as {@link #hasScriptFile}), or null. */
    public static java.nio.file.Path scriptFilePath(ScriptType scriptType, String scriptName) {
        ScriptFile file = resolveScriptFile(scriptType, scriptName);
        return file == null ? null : java.nio.file.Path.of(file.path());
    }

    private static ScriptFile resolveScriptFile(ScriptType scriptType, String scriptName) {
        if (scriptName == null || scriptName.isBlank()) {
            return null;
        }
        String normalizedName = normalizeScriptName(scriptName);
        if ((scriptType == ScriptType.Npc || scriptType == ScriptType.Portal || scriptType == ScriptType.Boss)
                && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            String bossAlias = switch (normalizedName.toLowerCase(java.util.Locale.ROOT)) {
                case "balog_accept" -> "balrog_accept";
                case "dusk_enter" -> "gloom_accept";
                case "dunkel_enter" -> "darknell_accept";
                case "slime_entergate" -> "slime_accept";
                case "seren_entergate" -> "seren_accept";
                case "kalos_entergate" -> "kalos_accept";
                case "karing_entergate" -> "kaling_accept";
                case "pt_outrootaboss", "pt_magnusdoor", "out_cygnus", "hillah_out", "serenout", "ptkalosout",
                        "pt_bm1_bossout", "bm1_bossout", "pt_jinhillah_out", "pt_gc_out",
                        "bpreturn_dusk", "bpreturn_dunkel", "bpreturn_slime", "bpreturn_seren",
                        "bpreturn_kalos", "bpreturn_karing", "bpreturn_bm" -> "compatible_boss_exit";
                default -> null;
            };
            if (bossAlias != null) {
                return resolveExplicitScriptFile(ScriptType.Boss, bossAlias,
                        PYTHON_SCRIPT_ENGINE_EXTENSION, PYTHON_SCRIPT_ENGINE_NAME);
            }
        }
        if (normalizedName.endsWith(PYTHON_SCRIPT_ENGINE_EXTENSION)) {
            String name = stripScriptExtension(normalizedName, PYTHON_SCRIPT_ENGINE_EXTENSION);
            return resolveExplicitScriptFile(scriptType, name, PYTHON_SCRIPT_ENGINE_EXTENSION, PYTHON_SCRIPT_ENGINE_NAME);
        }
        if (normalizedName.endsWith(JAVASCRIPT_SCRIPT_ENGINE_EXTENSION)) {
            String name = stripScriptExtension(normalizedName, JAVASCRIPT_SCRIPT_ENGINE_EXTENSION);
            return resolveExplicitScriptFile(scriptType, name, JAVASCRIPT_SCRIPT_ENGINE_EXTENSION, JAVASCRIPT_SCRIPT_ENGINE_NAME);
        }
        ScriptFile pythonFile = resolveExplicitScriptFile(scriptType, normalizedName, PYTHON_SCRIPT_ENGINE_EXTENSION, PYTHON_SCRIPT_ENGINE_NAME);
        if (pythonFile != null) {
            return pythonFile;
        }
        return resolveExplicitScriptFile(scriptType, normalizedName, JAVASCRIPT_SCRIPT_ENGINE_EXTENSION, JAVASCRIPT_SCRIPT_ENGINE_NAME);
    }

    private static ScriptFile resolveExplicitScriptFile(ScriptType scriptType, String scriptName, String extension, String engineName) {
        String path = buildScriptPath(scriptType, scriptName, extension);
        if (!new File(path).exists()) {
            return null;
        }
        return new ScriptFile(scriptName, path, extension, engineName);
    }

    private static String buildScriptPath(ScriptType scriptType, String scriptName, String extension) {
        return String.format("%s/%s/%s%s", ServerConstants.SCRIPT_DIR,
                scriptType.getDir().toLowerCase(), normalizeScriptName(scriptName), extension);
    }

    private static String normalizeScriptName(String scriptName) {
        String normalizedName = scriptName.replace('\\', '/');
        while (normalizedName.startsWith("/")) {
            normalizedName = normalizedName.substring(1);
        }
        return normalizedName;
    }

    private static String stripScriptExtension(String scriptName, String extension) {
        return scriptName.substring(0, scriptName.length() - extension.length());
    }

    private record ScriptFile(String name, String path, String extension, String engineName) {
    }

    private boolean isIntendedStop(ScriptException e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains(INTENDED_NPE_MSG)) {
            return true;
        }
        Throwable t = e.getCause();
        while (t != null) {
            String m = t.getMessage();
            if (m != null && m.contains(INTENDED_NPE_MSG)) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    public synchronized void stop(ScriptType scriptType) {
        for (Map.Entry<ScriptType, ScriptInfo> entry : getScripts().entrySet()) {
            if (entry.getKey() == scriptType && entry.getValue() != null) {
                entry.getValue().reset();
            }
        }
        setSpeakerID(0);
        if (getLastActiveScriptType() == scriptType) {
            setLastActiveScriptType(ScriptType.None);
            setLastActiveScriptName(null);
        }
        getNpcScriptInfo().reset();
        getMemory().clear();
        unlockUIIfNeeded();
    }

    private void undefined(ScriptType scriptType, int parentID) {
        switch (scriptType) {
            case Item:
                chat("該道具尚未實裝，請聯絡開發者。ID: " + parentID);
                chr.dispose();
                break;
            case Portal:
                chat("該傳送點尚未實裝，請聯絡開發者。ID: " + parentID);
                chr.dispose();
                break;
            case Quest:
                chat("該任務尚未實裝，請聯絡開發者。ID: " + parentID);
                chr.dispose();
                break;
            case Reactor:
                chat("該反應物尚未實裝，請聯絡開發者。ID: " + parentID);
                chr.dispose();
                break;
            case Field:
                chat("該地圖尚未實裝，請聯絡開發者。ID: " + parentID);
                chr.dispose();
                break;
            case Npc:
                String[] dialogues = new String[]{"小心！最近那些綠水靈比平常還要強。",
                        "你有看到一隻金色的小豬跑過去嗎？我弄丟了一隻！",
                        "我剛才看到一隻石巨人在守著寶藏。小心點，那傢伙可頑固了。",
                        "喔，你看起來像是個賞金獵人。可以幫我清掉一些怪物嗎？",
                        "有幾隻刺菇菇在村子邊緣晃來晃去，希望牠們別去打擾別人。",
                        "你見過雪人嗎？牠們比我想像的還要巨大得多。",
                        "我心愛的小刀掉在幽靈樹附近了，大概是被哪隻魔法怪物拿走了吧。",
                        "你身上有帶子彈嗎？我的全都拿去打石龍用光了。",
                        "別小看那些綠水靈，一隻或許沒什麼，成群結隊就不好對付了……",
                        "你的帽子破了耶，該不會是被野豬咬的吧？",
                        "我需要幾個從綠菇菇身上掉的軟角，你可以幫我嗎？",
                        "幫我從藍熊身上找一些怪物核心吧，我會送你一份像樣的謝禮。",
                        "我不小心把魔法卷軸弄丟了，大概掉在魔法森林附近的某個地方。",
                        "你有足夠的冰塊讓我做冰淇淋嗎？那東西可不好找。",
                        "要打造這把武器，我需要 10 個水晶碎片，你可以在礦坑裡找到它們。",
                        "我還需要一些寶石。聽說只有特別的石巨人才會掉。",
                        "幫我帶一對蝙蝠翅膀來吧，我需要它來完成一項祕密任務。",
                        "你找到我弄丟的戒指了嗎？上面有一個龍的圖案。",
                        "我需要 20 根羽毛來做枕頭，你可以幫我嗎？",
                        "如果你能從龍之洞穴帶回禁咒符，我會付你一大筆錢。",
                        "你需要一把更強的劍來對付敵人嗎？我可以幫你打造一把。",
                        "你最近打算轉職嗎？這件事可要好好考慮清楚。",
                        "我調配出一種新藥水，可以在短時間內提升你的力量。",
                        "你的經驗已經足夠學習新技能了嗎？",
                        "經驗就是一切，別只顧著賺錢。",
                        "多學多練才能變得更強，這是我給你的忠告。",
                        "天分只占一小部分，努力才是最重要的。",
                        "我以前可是個有名的鐵匠，不過現在都是過去的事了。",
                        "這個世界永遠需要優秀的工匠。你想學一門手藝嗎？",
                        "升級是一段漫長的旅程，祝你好運。",
                        "你打算去維多利亞島嗎？那裡藏著很多祕密。",
                        "千萬別去維多利亞港，對你這樣的新手來說太危險了。",
                        "我聽過很多關於空中之城的故事，說不定你能在那裡有所發現。",
                        "去時空之塔的時候要小心，聽說那裡的怪物非常可怕。",
                        "你看過空中的騎士城堡嗎？聽說那裡住著傳說中的騎士們。",
                        "去冰原雪域那片雪地看看吧，那裡有位很擅長魔法的老人。",
                        "勇士之村總是擠滿了戰士，熱鬧得很。",
                        "我覺得你該去螞蟻廣場，那裡是練等的好地方。",
                        "這世界有好多地方可以探索，出發前記得把行囊準備好。",
                        "你聽過海底的水之都嗎？聽說非常漂亮。",
                        "一定是我野豬吃太多了，現在只想睡覺。",
                        "你的頭跟刺菇菇長得一模一樣耶！",
                        "我剛剛踩到綠水靈滑了一跤，真是丟臉。",
                        "我好像迷路了，你可以幫我找回去的路嗎？",
                        "我都跟你說過了，我不是發任務的 NPC！",
                        "你的帽子看起來好好笑，那是香菇帽嗎？",
                        "這裡的甜點超好吃的！你要不要嚐嚐看？",
                        "我幫不了你啦，我只是個路人而已。",
                        "我看到你掉了一樣東西……啊，不對，那只是顆石頭。",
                        "喔，我正忙著數樹上的葉子，你可以晚點再來嗎？"};
                sendOK(Util.getRandomFromCollection(dialogues), parentID);
                break;
            default:
                chr.dispose();
                break;
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof Mob mob) {
            net.swordie.ms.scripts.event.EventManager.get().onMobRemoved(mob);
            notifyMobDeath(mob);
        }
    }

    public void handleAction(ScriptType scriptType, NpcMessageType lastType, byte response, int answer, String text) {
        switch (response) {
            case -1, 5 -> stop(scriptType);
            default -> {
                ScriptMemory sm = getMemory();
                if (lastType.isPrevPossible() && response == 0) {
                    // back button pressed
                    NpcScriptInfo prev = sm.getPreviousScriptInfo();
                    chr.write(ScriptMessageEncoder.active().scriptMessage(prev, prev.getMessageType()));
                } else {
                    if (getMemory().isInMemory()) {
                        NpcScriptInfo next = sm.getNextScriptInfo();
                        chr.write(ScriptMessageEncoder.active().scriptMessage(next, next.getMessageType()));
                    } else {
                        ScriptInfo si = getScriptInfoByType(scriptType);
                        if (isActive(scriptType)) {
                            switch (lastType.getResponseType()) {
                                case Response -> si.addResponseInteger((int) response);
                                case Answer -> si.addResponseInteger(answer);
                                case Text -> si.addResponseString(text);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isActive(ScriptType scriptType) {
        return getScriptInfoByType(scriptType) != null && getScriptInfoByType(scriptType).isActive();
    }

    public NpcScriptInfo getNpcScriptInfo() {
        return npcScriptInfo;
    }

    public Map<ScriptType, ScriptInfo> getScripts() {
        return scripts;
    }

    public int getParentID() {
        int res = 0;
        for (ScriptType type : ScriptType.values()) {
            if (getScriptInfoByType(type) != null) {
                res = getScriptInfoByType(type).getParentID();
            }
        }
        return res;
    }

    public boolean isField() {
        return isField;
    }

    public Field getField() {
        if (field == null) {
            return getChr().getField();
        }
        return field;
    }

    public String getLastActiveScriptName() {
        return lastActiveScriptName;
    }

    public void setLastActiveScriptName(String lastActiveScriptName) {
        this.lastActiveScriptName = lastActiveScriptName;
    }

    public ScriptType getLastActiveScriptType() {
        return lastActiveScriptType;
    }

    public void setLastActiveScriptType(ScriptType lastActiveScriptType) {
        this.lastActiveScriptType = lastActiveScriptType;
    }

    public FieldTransferInfo getFieldTransferInfo() {
        return fieldTransferInfo;
    }

    public void setFieldTransferInfo(FieldTransferInfo fieldTransferInfo) {
        this.fieldTransferInfo = fieldTransferInfo;
    }

    // Start of the sends/asks -----------------------------------------------------------------------------------------

    @Override
    public int sendSay(String text) {
        if (getLastActiveScriptType() == ScriptType.None) {
            return 0;
        }
        return sendGeneralSay(text, Say);
    }

    public int sendSayIllustration(String text, int faceIndex, boolean isLeft) {
        if (getLastActiveScriptType() == ScriptType.None) {
            return 0;
        }
        getNpcScriptInfo().setFaceIndex(faceIndex);
        getNpcScriptInfo().setLeft(isLeft);
        return sendGeneralSay(text, SayIllustration);
    }

    /**
     * Helper function that ensures that selections have the appropriate type (AskMenu).
     *
     * @param text
     * @param nmt
     */
    private String panelText(String text) {
        ScriptInfo activeInfo = getScriptInfoByType(getLastActiveScriptType());
        return CatPanelText.format(activeInfo == null ? null : activeInfo.getScriptName(), text,
                net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3);
    }

    private int sendGeneralSay(String text, NpcMessageType nmt) throws NullPointerException {
        text = panelText(text);
        ensureDialogActive();
        NpcScriptInfo nsi = getNpcScriptInfo();
        nsi.setText(text);
        if (text.contains("#L")) {
            nmt = AskMenu;
        }
        nsi.setMessageType(nmt);
        chr.write(ScriptMessageEncoder.active().scriptMessage(nsi, nmt));
        getMemory().addMemoryInfo(nsi);
        Integer response = null;
        if (!isActive(getLastActiveScriptType())) {
            throw new NullPointerException(INTENDED_NPE_MSG);
        }
        if (isActive(getLastActiveScriptType())) {
            ScriptInfo si = getScriptInfoByType(getLastActiveScriptType());
            if (si == null) {
                throw new NullPointerException(INTENDED_NPE_MSG);
            }
            response = si.awaitResponseInteger(0);
        }
        if (response == null) {
            throw new NullPointerException(INTENDED_NPE_MSG);
        }
        return response;
    }

    @Override
    public int sendNext(String text) {
        return sendGeneralSay(text, SayNext);
    }

    @Override
    public int sendPrev(String text) {
        return sendGeneralSay(text, SayPrev);
    }

    @Override
    public int sendSayOkay(String text) {
        return sendGeneralSay(text, SayOk);
    }

    public int sendSayNextIllustration(String text, int faceIndex, boolean isLeft) {
        getNpcScriptInfo().setFaceIndex(faceIndex);
        getNpcScriptInfo().setLeft(isLeft);
        return sendGeneralSay(text, SayIllustrationNext);
    }

    public int sendSayPrevIllustration(String text, int faceIndex, boolean isLeft) {
        getNpcScriptInfo().setFaceIndex(faceIndex);
        getNpcScriptInfo().setLeft(isLeft);
        return sendGeneralSay(text, SayIllustrationPrev);
    }

    public int sendSayOkayIllustration(String text, int faceIndex, boolean isLeft) {
        getNpcScriptInfo().setFaceIndex(faceIndex);
        getNpcScriptInfo().setLeft(isLeft);
        return sendGeneralSay(text, SayIllustrationOk);
    }

    @Override
    public int sendSayImage(String image) {
        return sendSayImage(new String[]{image});
    }

    @Override
    public int sendSayImage(String[] images) {
        getNpcScriptInfo().setImages(images);
        getNpcScriptInfo().setMessageType(SayImage);
        return sendGeneralSay("", SayImage);
    }

    @Override
    public boolean sendAskYesNo(String text) {
        return sendGeneralSay(text, AskYesNo) != 0;
    }

    public int askMenu(String text) {
        return sendNext(text);
    }

    public ScriptManagerImpl meFlip() {
        flipSpeaker();
        return this;
    }

    public ScriptManagerImpl defNpc(int npcId) {
        setSpeakerID(npcId);
        return this;
    }

    public void changeMap(int mapId) {
        changeMap(mapId, 0);
    }

    /**
     * MetaMS {@code changeMap}: deferred to the end of this player's running script as before, but immediate when
     * called from an event callback or on a player that runs no script (party members moved by the leader's NPC
     * script or by an event), whose deferred warp would otherwise never happen. [eventfw]
     */
    public void changeMap(int mapId, int portalId) {
        if (chr != null && (net.swordie.ms.scripts.event.EventManager.inEventCallback() || !hasRunningScript())) {
            chr.warp(mapId, portalId);
            return;
        }
        warp(mapId, portalId);
    }

    private boolean hasRunningScript() {
        for (ScriptInfo si : new ArrayList<>(getScripts().values())) {
            if (si != null && si.isActive()) return true;
        }
        return false;
    }

    public int getMapId() {
        return getFieldID();
    }

    public int getCurrency(int type) {
        if (chr == null || chr.getUser() == null) {
            return 0;
        }
        return switch (type) {
            case 1 -> chr.getUser().getMaplePoints();
            case 2 -> chr.getUser().getDonationPoints();
            case 3 -> chr.getUser().getVotePoints();
            case 4 -> chr.getUser().getNxPrepaid();
            default -> 0;
        };
    }

    public void modifyCurrency(int type, int amount) {
        if (chr == null || chr.getUser() == null || amount == 0) {
            return;
        }
        synchronized (chr.getUser()) {
        if (type < 1 || type > 4) throw new IllegalArgumentException("Unknown point type");
        long balance = (long) getCurrency(type) + amount;
        if (balance < 0 || balance > Integer.MAX_VALUE) throw new IllegalArgumentException("點數餘額不足或超出上限。");
        switch (type) {
            case 1 -> chr.getUser().addMaplePoints(amount);
            case 2 -> chr.getUser().addDonationPoint(amount);
            case 3 -> chr.getUser().addVotePoint(amount);
            case 4 -> chr.getUser().addNXPrepaid(amount);
            default -> {
            }
        }
        }
    }

    public net.swordie.ms.world.tms282.activity.ActivityRewards.Result grantActivityRewards(String activityKey, int[] itemIds, int[] quantities) {
        return net.swordie.ms.world.tms282.activity.ActivityRewards.grant(chr, activityKey, itemIds, quantities);
    }

    // ---- revived legacy events (world/tms282/revival, docs/revival) ----

    /** One claim per account and receipt key (e.g. "rv_punch:2026-10-05"), receipt before inventory. */
    public net.swordie.ms.world.tms282.activity.ActivityRewards.Result grantRevivalReward(String receiptKey, int[] itemIds, int[] quantities) {
        return net.swordie.ms.world.tms282.activity.ActivityRewards.grantOnce(chr, receiptKey, itemIds, quantities);
    }

    public boolean isRevivalOpen(String key) {
        return net.swordie.ms.world.tms282.revival.RevivalEvents.isOpen(key);
    }

    /** Open revival events in panel order, as "key|title|summary" lines. */
    public java.util.List<String> openRevivalEvents() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (var e : net.swordie.ms.world.tms282.revival.RevivalEvents.open()) {
            var s = e.spec();
            out.add(s.key() + "|" + s.title() + "|" + s.summary());
        }
        return out;
    }

    /** Revival hub NPC dialog (HubDialog): "MENU:<text>" asks again with revivalHubSelect, "SAY:<text>" ends. */
    public String revivalHubPage(String eventKey) {
        return net.swordie.ms.world.tms282.revival.hub.RevivalHubs.page(chr, eventKey);
    }

    public boolean revivalHasHub(String eventKey) {
        return net.swordie.ms.world.tms282.revival.hub.HubCatalog.open(eventKey).isPresent();
    }

    public boolean revivalHubHasShop(String eventKey) {
        return net.swordie.ms.world.tms282.revival.hub.HubCatalog.open(eventKey).map(h -> !h.offers().isEmpty()).orElse(false);
    }

    public String revivalHubSelect(String eventKey, int selection) {
        return net.swordie.ms.world.tms282.revival.hub.RevivalHubs.select(chr, eventKey, selection);
    }

    /** 世界最強拳擊王 result (event/revival_punch_king.js): weekly best + daily tier reward; returns the summary lines. */
    public String revivalPunchResult(int broken, double damage) {
        return net.swordie.ms.world.tms282.revival.punch.PunchKing.record(chr, broken, damage);
    }

    /** 番茄節 result (event/revival_punch_king.js tomato variant): 夜之碎片 once a day per account. */
    public String revivalTomatoResult(int broken, double damage) {
        return net.swordie.ms.world.tms282.revival.punch.PunchKing.recordTomato(chr, broken, damage);
    }

    /** 動態視力修煉場 result (event/revival_annual_shooting.js): 幻想硬幣 tier once a day per account. */
    public String revivalShootingResult(int score) {
        return net.swordie.ms.world.tms282.revival.circus.AnnualShooting.record(chr, score);
    }

    /** 世界最強拳擊王 weekly ranking text for NPC 9062765. */
    public String revivalPunchRanking() {
        var day = net.swordie.ms.world.tms282.revival.RevivalEvents.today();
        StringBuilder t = new StringBuilder("#b本週排行（傷害，百萬）#k\r\n");
        int n = 1;
        for (var e : net.swordie.ms.world.tms282.revival.punch.PunchKing.weeklyTop(10, day))
            t.append(n++).append(". ").append(e.name()).append("  ").append(String.format("%,d", e.units())).append("\r\n");
        if (n == 1) t.append("本週還沒有紀錄。\r\n");
        int mine = net.swordie.ms.world.tms282.revival.punch.PunchKing.myBest(chr.getId(), day);
        t.append("\r\n我的本週最佳：").append(mine < 0 ? "暫時無法讀取" : String.format("%,d", mine));
        return t.toString();
    }

    /** Enters an open revival event; returns the player-facing result text. */
    public String enterRevivalEvent(String key) {
        if (chr == null) return net.swordie.ms.world.tms282.revival.RevivalEvents.describe(
                net.swordie.ms.world.tms282.revival.RevivalEvent.EntryResult.CLOSED);
        return net.swordie.ms.world.tms282.revival.RevivalEvents.describe(
                net.swordie.ms.world.tms282.revival.RevivalEvents.enter(key,
                        net.swordie.ms.world.tms282.revival.RevivalPlayer.of(chr)));
    }

    /** 樂豆抽獎: charge + receipt in one transaction, then the exact rolled prizes (nothing charged when they do not fit). */
    public net.swordie.ms.world.tms282.activity.PointRaffle.Result purchasePointRaffle(String raffleKey, int cost, int[] itemIds, int[] quantities) {
        return net.swordie.ms.world.tms282.activity.PointRaffle.purchase(chr, raffleKey, cost, itemIds, quantities);
    }

    /** Same as {@link #purchasePointRaffle(String, int, int[], int[])} for another wallet (see PointWallets: maple, donation, nx, hypay, rmb, money). */
    public net.swordie.ms.world.tms282.activity.PointRaffle.Result purchasePointRaffle(String wallet, String raffleKey, int cost, int[] itemIds, int[] quantities) {
        return net.swordie.ms.world.tms282.activity.PointRaffle.purchase(chr, wallet, raffleKey, cost, itemIds, quantities);
    }

    /** Paid raffle / gift pack builder: add prizes, credits and limits, then {@code purchase()} once (one transaction + receipt). */
    public net.swordie.ms.world.tms282.activity.PointOrder pointOrder(String wallet, String orderKey, int cost) {
        return net.swordie.ms.world.tms282.activity.PointOrder.of(chr, wallet, orderKey, cost);
    }

    /** Read-only balance of a PointWallets wallet (never creates rows); 0 when it cannot be read. */
    public int pointBalance(String wallet) {
        var w = net.swordie.ms.world.tms282.activity.PointWallets.of(wallet);
        if (w == null || chr == null || chr.getUser() == null || chr.getAccount() == null) return 0;
        try (java.sql.Connection c = DatabaseManager.getConnection()) {
            return (int) Math.min(Integer.MAX_VALUE, w.balance(c, chr, false));
        } catch (SQLException | RuntimeException e) {
            return 0;
        }
    }

    /** True when the item can be granted on this protocol (raffle pools skip the rest before rolling). */
    public boolean isItemGrantable(int itemId) {
        return net.swordie.ms.world.tms282.items.Tms282ItemAvailability.isGrantable(itemId);
    }

    /** Bag tab (InvType value) a granted item lands in, as Char.canHold counts it (cash equips: 6); 0 when unknown. */
    public int inventoryTabOf(int itemId) {
        if (ItemConstants.isEquip(itemId)) {
            Equip equip = ItemData.getEquipById(itemId);
            return equip == null ? 0 : equip.isCash() ? InvType.DECORATION.getVal() : InvType.EQUIP.getVal();
        }
        var info = ItemData.getItemInfoByID(itemId);
        return info == null || info.getInvType() == null ? 0 : info.getInvType().getVal();
    }

    public net.swordie.ms.world.tms282.activity.ActivityRewards.Admission admitMonsterPark() {
        return net.swordie.ms.world.tms282.activity.ActivityRewards.admitMonsterPark(chr);
    }

    public void gainMesos(long amount) {
        if (chr != null) {
            chr.addMoney(amount);
        }
    }

    public void loseMesos(long amount) {
        if (chr != null) {
            chr.deductMoney(amount);
        }
    }

    public void showSystemMessage(String message) {
        systemMessage(message);
    }

    public int say(String text) {
        return sendSayOkay(text);
    }

    public int sendOk(String text) {
        return sendSayOkay(text);
    }

    public int sendSimple(String text) {
        return sendNext(text);
    }

    public int askYesNo(String text) {
        return sendAskYesNo(text) ? 1 : 0;
    }

    /**
     * A stopped script (player pressed ESC / disposed) must never send another dialog
     * packet — the NpcScriptInfo state is already reset and the malformed SCRIPT_MESSAGE
     * makes the client report a CLIENT_ERROR, which kicks the player. Scripts that catch
     * the intended-NPE and call npc.say() again hit this guard instead.
     */
    private void ensureDialogActive() {
        if (!isActive(getLastActiveScriptType())) {
            throw new NullPointerException(INTENDED_NPE_MSG);
        }
    }

    public NumberRequest defNum(int defaultNumber) {
        return new NumberRequest(defaultNumber);
    }

    /** MetaMS chain start {@code npc.minNum(a).maxNum(b).defNum(c).askNumber(text)}; the order of the setters is free. */
    public NumberRequest minNum(int min) {
        return new NumberRequest(null).minNum(min);
    }

    public NumberRequest maxNum(int max) {
        return new NumberRequest(null).maxNum(max);
    }

    public TextRequest defText(String defaultText) {
        return new TextRequest(defaultText);
    }

    public TextRequest minLen(int min) {
        return new TextRequest("").minLen(min);
    }

    public TextRequest maxLen(int max) {
        return new TextRequest("").maxLen(max);
    }

    public class NumberRequest {
        private Integer defaultNumber;
        private int min = Integer.MIN_VALUE;
        private int max = Integer.MAX_VALUE;

        private NumberRequest(Integer defaultNumber) {
            this.defaultNumber = defaultNumber;
        }

        public NumberRequest minNum(int min) {
            this.min = min;
            return this;
        }

        public NumberRequest maxNum(int max) {
            this.max = max;
            return this;
        }

        public NumberRequest defNum(int defaultNumber) {
            this.defaultNumber = defaultNumber;
            return this;
        }

        public int askNumber(String text) {
            int def = defaultNumber != null ? defaultNumber : (min != Integer.MIN_VALUE ? min : 0);
            if (min <= max) {
                def = Math.max(min, Math.min(max, def));
            }
            return sendAskNumber(text, def, min, max);
        }
    }

    public class TextRequest {
        private String defaultText;
        private short min = 0;
        private short max = 255;

        private TextRequest(String defaultText) {
            this.defaultText = defaultText;
        }

        public TextRequest minLen(int min) {
            this.min = (short) min;
            return this;
        }

        public TextRequest maxLen(int max) {
            this.max = (short) max;
            return this;
        }

        public TextRequest defText(String defaultText) {
            this.defaultText = defaultText == null ? "" : defaultText;
            return this;
        }

        public String askText(String text) {
            return sendAskText(text, defaultText, min, max);
        }
    }

    /** MetaMS {@code npc.askText(text, default, minLen, maxLen)}. */
    public String askText(String text, String defaultText, int minLength, int maxLength) {
        return sendAskText(text, defaultText == null ? "" : defaultText, (short) minLength, (short) maxLength);
    }

    public String askText(String text) {
        return sendAskText(text, "", (short) 0, (short) 255);
    }

    /** MetaMS {@code npc.askAccept(text)}: true when the player pressed accept. */
    public boolean askAccept(String text) {
        return sendAskAccept(text);
    }

    public int sayNext(String text) {
        return sendNext(text);
    }

    /** Alias used by a few item scripts ({@code sm.sendOkay}). */
    public int sendOkay(String text) {
        return sendSayOkay(text);
    }

    @Override
    public boolean sendAskAccept(String text) {
        return sendGeneralSay(text, AskAccept) != 0;
    }

    @Override
    public String sendAskText(String text, String defaultText, short minLength, short maxLength) throws NullPointerException {
        ensureDialogActive();
        NpcScriptInfo nsi = getNpcScriptInfo();
        nsi.setMin(minLength);
        nsi.setMax(maxLength);
        nsi.setDefaultText(defaultText);
        nsi.setText(panelText(text));
        nsi.setMessageType(AskText);
        chr.write(ScriptMessageEncoder.active().scriptMessage(nsi, AskText));
        getMemory().addMemoryInfo(nsi);
        ScriptInfo si = getScriptInfoByType(getLastActiveScriptType());
        if (si == null) {
            throw new NullPointerException(INTENDED_NPE_MSG);
        }
        String response = si.awaitResponseString(0);
        if (response == null) {
            throw new NullPointerException(INTENDED_NPE_MSG);
        }
        return response;
    }

    public int sendAskStoryUI(String text, String key, byte type) {
        NpcScriptInfo nsi = getNpcScriptInfo();
        nsi.setText(text);
        nsi.setHintText(key);
        nsi.setType(type);
        return sendGeneralSay(text, AskStoryUI);
    }

    @Override
    public int sendAskNumber(String text, int defaultNum, int min, int max) {
        NpcScriptInfo nsi = getNpcScriptInfo();
        nsi.setDefaultNumber(defaultNum);
        nsi.setMin(min);
        nsi.setMax(max);
        return sendGeneralSay(text, AskNumber);
    }

    @Override
    public int sendInitialQuiz(byte type, String title, String problem, String hint, int min, int max, int time) {
        NpcScriptInfo nsi = getNpcScriptInfo();
        nsi.setType(type);
        if (type != 1) {
            nsi.setTitle(title);
            nsi.setProblemText(problem);
            nsi.setHintText(hint);
            nsi.setMin(min);
            nsi.setMax(max);
            nsi.setTime(time);
        }
        return sendGeneralSay(title, InitialQuiz);
    }

    @Override
    public int sendInitialSpeedQuiz(byte type, int quizType, int answer, int correctAnswers, int remaining, int time) {
        NpcScriptInfo nsi = getNpcScriptInfo();
        nsi.setType(type);
        if (type != 1) {
            nsi.setQuizType(quizType);
            nsi.setAnswer(answer);
            nsi.setCorrectAnswers(correctAnswers);
            nsi.setRemaining(remaining);
            nsi.setTime(time);
        }
        return sendGeneralSay("", InitialSpeedQuiz);
    }

    @Override
    public int sendICQuiz(byte type, String text, String hintText, int time) {
        NpcScriptInfo nsi = getNpcScriptInfo();
        nsi.setType(type);
        nsi.setHintText(hintText);
        nsi.setTime(time);
        return sendGeneralSay(text, ICQuiz);
    }

    @Override
    public int sendAskAvatar(String text, boolean angelicBuster, boolean zeroBeta, int... options) {
        NpcScriptInfo nsi = getNpcScriptInfo();
        nsi.setAngelicBuster(angelicBuster);
        nsi.setZeroBeta(zeroBeta);
        nsi.setOptions(options);
        return sendGeneralSay(text, AskAvatar);
    }

    public int sendAskSlideMenu(int dlgType) {
        NpcScriptInfo nsi = getNpcScriptInfo();
        nsi.setDlgType(dlgType);
        return sendGeneralSay("", AskSlideMenu);
    }

    public int sendAskSelectMenu(int dlgType, int defaultSelect) {
        return sendAskSelectMenu(dlgType, defaultSelect, new String[]{});
    }

    public int sendAskSelectMenu(int dlgType, int defaultSelect, String[] text) {
        NpcScriptInfo nsi = getNpcScriptInfo();
        nsi.setDlgType(dlgType);
        nsi.setDefaultSelect(defaultSelect);
        nsi.setSelectText(text);
        return sendGeneralSay("", AskSelectMenu);
    }

    public int sendAskCustomMixHair(String text, boolean zeroBeta) {
        NpcScriptInfo nsi = getNpcScriptInfo();
        nsi.setAngelicBuster(JobConstants.isAngelicBuster(chr.getJob()));
        nsi.setZeroBeta(zeroBeta);
        return sendGeneralSay(text, AskCustomMixHair);
    }


    // Start of param methods ------------------------------------------------------------------------------------------

    public void setParam(int param) {
        getNpcScriptInfo().setParam((short) param);
    }

    public void setColor(int color) {
        getNpcScriptInfo().setColor((byte) color);
    }

    public void resetParam() {
        getNpcScriptInfo().resetParam();
    }

    public void removeEscapeButton() {
        getNpcScriptInfo().addParam(NpcScriptInfo.Param.NotCancellable);
    }

    public void addEscapeButton() {
        if (getNpcScriptInfo().hasParam(NpcScriptInfo.Param.NotCancellable)) {
            getNpcScriptInfo().removeParam(NpcScriptInfo.Param.NotCancellable);
        }
    }

    public void flipSpeaker() {
        getNpcScriptInfo().addParam(NpcScriptInfo.Param.FlipSpeaker);
    }

    public void flipDialogue() {
        getNpcScriptInfo().addParam(NpcScriptInfo.Param.OverrideSpeakerID);
    }

    public void flipDialoguePlayerAsSpeaker() {
        getNpcScriptInfo().addParam(NpcScriptInfo.Param.PlayerAsSpeakerFlip);
    }

    public void setPlayerAsSpeaker() {
        getNpcScriptInfo().addParam(NpcScriptInfo.Param.PlayerAsSpeaker);
    }

    public void setColor(byte color) {
        getNpcScriptInfo().setColor(color);
    }

    public void setBoxChat() {
        setBoxChat(true);
    }

    public void setBoxChat(boolean color) { // true = Standard BoxChat  |  false = Zero BoxChat
        getNpcScriptInfo().setColor((byte) (color ? 1 : 0));
        getNpcScriptInfo().addParam(NpcScriptInfo.Param.BoxChat);
    }

    public void setNpcBoxChat(int npcID) {
        setSpeakerID(npcID);
        getNpcScriptInfo().setColor((byte) 1);
        getNpcScriptInfo().addParam(NpcScriptInfo.Param.BoxChatOverrideSpeakerNoEndChat);
    }

    public void setPlayerBoxChat() {
        setBoxChat();
        flipBoxChat();
        flipBoxChatPlayerAsSpeaker();
    }

    public void setNpcOverrideBoxChat(int npcID) {
        setSpeakerID(npcID);
        getNpcScriptInfo().setColor((byte) 1);
        getNpcScriptInfo().addParam(NpcScriptInfo.Param.BoxChatOverrideSpeakerNoEndChat);
    }

    public void setBoxOverrideSpeaker() {
        getNpcScriptInfo().addParam(NpcScriptInfo.Param.BoxChatOverrideSpeaker);
    }

    public void flipBoxChat() {
        getNpcScriptInfo().addParam(NpcScriptInfo.Param.FlipBoxChat);
    }

    public void boxChatPlayerAsSpeaker() {
        getNpcScriptInfo().addParam(NpcScriptInfo.Param.BoxChatAsPlayer);
    }

    public void flipBoxChatPlayerAsSpeaker() {
        getNpcScriptInfo().addParam(NpcScriptInfo.Param.FlipBoxChatAsPlayer);
    }

    public void flipBoxChatPlayerNoEscape() {
        getNpcScriptInfo().addParam(NpcScriptInfo.Param.FlipBoxChatAsPlayerNoEscape);
    }

    public final String options(final String... vals) {
        final StringBuilder menu = new StringBuilder();
        for (int i = 0; i < vals.length; i++) {
            menu.append("#L").append(i).append("#").append(vals[i]).append("#l").append(i == vals.length - 1 ? "" : "\r\n");
        }
        return menu.toString();
    }

    public void sendOK(String text, int npcTemplateID) {
        chr.write(UserLocal.setUtilDlg(text, npcTemplateID));
    }
    // Start helper methods for scripts --------------------------------------------------------------------------------

    @Override
    public synchronized void dispose() {
        clearScriptNpcControllers();
        getNpcScriptInfo().reset();
        getMemory().clear();
        stop(ScriptType.Npc);
        stop(ScriptType.Portal);
        stop(ScriptType.Item);
        stop(ScriptType.Quest);
        stop(ScriptType.Reactor);
        stop(ScriptType.Field);
        stop(ScriptType.Content);
        // Preserve main cleanup: Boss and FirstEnterField may also be awaiting a response.
        for (ScriptInfo si : getScripts().values()) {
            if (si != null) si.reset();
        }
        setCurNodeEventEnd(false);
        getScripts().clear();
    }

    public Position getPosition(int objId) {
        return chr.getField().getLifeByObjectID(objId).getPosition();
    }


    // Character Stat-related methods ----------------------------------------------------------------------------------

    @Override
    public int getJob() {
        return chr.getJob();
    }

    @Override
    public String getJobName() {
        return JobConstants.getJobEnumById(chr.getJob()).toString();
    }

    @Override
    public void setJob(short jobID) {
        chr.setJob(jobID);
        Map<Stat, Object> stats = new HashMap<>();
        stats.put(Stat.job, jobID);
        chr.sendStatsPacket(stats);
    }

    @Override
    public void maxAllSkill() {
        chr.maxSkills();
    }

    public void addSPJobAdv(short job, int amount) {
        chr.addSpToSpecificJob(job, amount);
        Map<Stat, Object> stats = new HashMap<>();
        stats.put(Stat.sp, chr.getAvatarData().getCharacterStat().getExtendSP());
        chr.sendStatsPacket(stats);
        chr.write(WvsContext.incSpMessage(chr.getJob(), (byte) amount));
    }

    @Override
    public void addSP(int amount, boolean jobAdv) {
        byte jobLevel = (byte) JobConstants.getJobLevel(chr.getJob());
        int currentSP = chr.getAvatarData().getCharacterStat().getExtendSP().getSpByJobLevel(jobLevel);
        setSP(currentSP + amount);
        if (jobAdv) {
            chr.write(WvsContext.incSpMessage(chr.getJob(), (byte) amount));
        }
    }

    @Override
    public void setSP(int amount) {
        chr.setSpToCurrentJob(amount);
        Map<Stat, Object> stats = new HashMap<>();
        stats.put(Stat.sp, chr.getAvatarData().getCharacterStat().getExtendSP());
        chr.sendStatsPacket(stats);
    }

    @Override
    public void addAP(int amount) {
        int currentAP = chr.getAvatarData().getCharacterStat().getAp();
        setAP(currentAP + amount);
    }

    @Override
    public void setAP(int amount) {
        chr.setStat(Stat.ap, (short) amount);
        Map<Stat, Object> stats = new HashMap<>();
        stats.put(Stat.ap, (short) amount);
        chr.sendStatsPacket(stats);
    }

    @Override
    public void resetAP(boolean hpmp) {
        resetAP(hpmp, (short) 0);
    }

    @Override
    public void resetAP(boolean hpmp, short jobID) {
        Map<Stat, Object> stats = new HashMap<>();
        stats.put(Stat.str, chr.getAvatarData().getCharacterStat().getStr());
        stats.put(Stat.dex, chr.getAvatarData().getCharacterStat().getDex());
        stats.put(Stat.luk, chr.getAvatarData().getCharacterStat().getLuk());
        stats.put(Stat.inte, chr.getAvatarData().getCharacterStat().getInt());
        stats.put(Stat.ap, chr.getAvatarData().getCharacterStat().getAp());

        // Identify Primary Stat, special case only exist for Thief (Xenon) & Pirates (Also including Xenon)
        Stat primaryStat = JobConstants.isWarriorEquipJob(jobID) ? Stat.str : JobConstants.isArcherEquipJob(jobID) ? Stat.dex : JobConstants.isMageEquipJob(jobID) ? Stat.inte : JobConstants.isThiefEquipJob(jobID) ? Stat.luk : Stat.ap;
        if (JobConstants.isXenon(jobID) || JobConstants.isAdventurerPirate(jobID) && !JobConstants.isBuccaneer(jobID) && !JobConstants.isCorsair(jobID) && !JobConstants.isCannoneer(jobID)) {
            primaryStat = Stat.ap; // If we are a xenon or a 1st job adventurer pirate put points back into AP
        } else if (JobConstants.isBuccaneer(jobID) || JobConstants.isThunderBreaker(jobID) || JobConstants.isShade(jobID) || JobConstants.isCannoneer(jobID)) {
            primaryStat = Stat.str; // Handle STR pirates
        } else if (JobConstants.isPirateEquipJob(jobID)) {
            primaryStat = Stat.dex; // if none of the above conditions apply & we're a pirate, only remaining choice is DEX, otherwise we leave primary stat as AP
        }
        int buffer = 0; // Difference between the stat's current value and its minimum (0 for AP, 4 for the four traditional stats)
        for (Map.Entry<Stat, Object> stat : stats.entrySet()) {
            if (stat.getKey() != primaryStat) {
                buffer = ((short) stat.getValue() - (stat.getKey() != Stat.ap ? 4 : 0));
                if (buffer > 0) {
                    stat.setValue((short) ((short) stat.getValue() - buffer));
                    stats.put(primaryStat, (short) ((short) stats.get(primaryStat) + buffer));
                    chr.setStat(stat.getKey(), (short) stats.get(stat.getKey()));
                    chr.setStat(primaryStat, (short) stats.get(primaryStat));
                }
            }
        }
        chr.sendStatsPacket(stats);
    }

    @Override
    public void setSTR(short amount) {
        chr.setStat(Stat.str, amount);
        Map<Stat, Object> stats = new HashMap<>();
        stats.put(Stat.str, amount);
        chr.sendStatsPacket(stats);
    }

    @Override
    public void setINT(short amount) {
        chr.setStat(Stat.inte, amount);
        Map<Stat, Object> stats = new HashMap<>();
        stats.put(Stat.inte, amount);
        chr.sendStatsPacket(stats);
    }

    @Override
    public void setDEX(short amount) {
        chr.setStat(Stat.dex, amount);
        Map<Stat, Object> stats = new HashMap<>();
        stats.put(Stat.dex, amount);
        chr.sendStatsPacket(stats);
    }

    @Override
    public void setLUK(short amount) {
        chr.setStat(Stat.luk, amount);
        Map<Stat, Object> stats = new HashMap<>();
        stats.put(Stat.luk, amount);
        chr.sendStatsPacket(stats);
    }

    public void incCharismaEXP(short amount) {
        chr.addTraitExp(Stat.charismaEXP, amount);
    }

    public void incInsightEXP(short amount) {
        chr.addTraitExp(Stat.insightEXP, amount);
    }

    public void incWillEXP(short amount) {
        chr.addTraitExp(Stat.willEXP, amount);
    }

    public void incCraftEXP(short amount) {
        chr.addTraitExp(Stat.craftEXP, amount);
    }

    public void incSenseEXP(short amount) {
        chr.addTraitExp(Stat.senseEXP, amount);
    }

    public void incCharmEXP(short amount) {
        chr.addTraitExp(Stat.charmEXP, amount);
    }

    public void incTraitExp(Stat stat, short amount) {
        chr.addTraitExp(stat, amount);
    }

    public void addMaxHP(int amount) {
        chr.addStatAndSendPacket(Stat.mhp, amount);
    }

    @Override
    public void setMaxHP(int amount) {
        chr.setStat(Stat.mhp, amount);
        int maxHP = chr.getMaxHP();
        chr.setStat(Stat.hp, maxHP);
        Map<Stat, Object> stats = new HashMap<>();
        stats.put(Stat.mhp, amount);
        stats.put(Stat.hp, maxHP);
        chr.sendStatsPacket(stats);
    }

    public void addMaxMP(int amount) {
        if (!JobConstants.isNoManaJob(chr.getJob())) {
            chr.addStatAndSendPacket(Stat.mmp, amount);
        }
    }

    @Override
    public void setMaxMP(int amount) {
        if (!JobConstants.isNoManaJob(chr.getJob())) {
            chr.setStat(Stat.mmp, amount);
            int maxMP = chr.getMaxMP();
            chr.setStat(Stat.mp, maxMP);
            Map<Stat, Object> stats = new HashMap<>();
            stats.put(Stat.mmp, amount);
            stats.put(Stat.mp, maxMP);
            chr.sendStatsPacket(stats);
        }
    }

    @Override
    public void jobAdvance(short jobID) {
        setJob(jobID);

        int apAmount = 0;
        int spAmount = 0;

        switch (JobConstants.getJobLevel(jobID)) {
            case 1: {
                spAmount = 5;
                if (JobConstants.isCygnusKnight(jobID)) {
                    --spAmount;
                }
                if (JobConstants.isDemonSlayer(jobID)) {
                    spAmount = 8;
                }
                if (JobConstants.isKanna(jobID)) {
                    spAmount = 6;
                }
                break;
            }
            case 2: {
                int incSP = 3 * (chr.getLevel() > 60 ? 29 : chr.getLevel() - 31);
                spAmount = 8 + incSP;
                break;
            }
            case 3: {
                int incSP = 3 * (chr.getLevel() > 100 ? 39 : chr.getLevel() - 61);
                spAmount = 8 + incSP;
                apAmount = 5;
                break;
            }
            case 4:
                spAmount = 6;
                apAmount = 10;
                break;
        }

        addAP(apAmount); //Standard added AP upon Job Advancing
        addSPJobAdv(jobID, spAmount); //Standard added SP upon Job Advancing
    }

    @Override
    public void jobAdvanceForDB(short jobID) {
        setJob(jobID);

        int apAmount = 0;
        int spAmount = 0;

        switch (JobConstants.getJobLevel(jobID)) {
            case 1: {
                spAmount = 5;
                break;
            }
            case 2: {
                spAmount = 8;
                break;
            }
            case 3: {
                spAmount = 20;
                break;
            }
            case 4:
                spAmount = 10;
                break;
            case 5:
                apAmount = 5;
                spAmount = 3;
                break;
            case 6:
                apAmount = 5;
                spAmount = 3;
                break;
        }

        addAP(apAmount); //Standard added AP upon Job Advancing
        addSPJobAdv(jobID, spAmount); //Standard added SP upon Job Advancing
        modifiedCharacter();
    }

    @Override
    public void giveExp(long expGiven) {
        chr.addExp(expGiven, true);
    }

    public void giveExpNoAffectedByExpRate(long expGiven) {
        chr.addExp(expGiven, false);
    }

    @Override
    public void giveExpNoMsg(long expGiven) {
        chr.addExpNoMsg(expGiven);
    }

    @Override
    public void changeCharacterLook(int look) {
        AvatarLook al = chr.getAvatarData().getAvatarLook();
        if (look <= ItemConstants.MAX_SKIN) { // skin
            al.setSkin(look);
            chr.setStatAndSendPacket(Stat.skin, look);
        } else if ((ItemConstants.MIN_FACE <= look && look < ItemConstants.MAX_FACE) || (ItemConstants.MIN_FACE_2 <= look && look < ItemConstants.MAX_FACE_2)) {
            if (StringData.getItemStringById(look) != null) {
                al.setFace(look);
                chr.setStatAndSendPacket(Stat.face, look);
            } else {
                System.out.printf("Tried changing a look with invalid id (%d)", look);
            }
        } else if (ItemConstants.MIN_HAIR <= look && look < ItemConstants.MAX_HAIR) {
            if (StringData.getItemStringById(look) != null) {
                al.setHair(look);
                chr.setStatAndSendPacket(Stat.hair, look);
            } else {
                System.out.printf("Tried changing a look with invalid id (%d)", look);
            }
        } else {
            System.out.printf("Tried changing a look with invalid id (%d)", look);
        }
        byte maskValue = AvatarModifiedMask.AvatarLook.getVal();
        chr.getField().broadcast(UserRemote.avatarModified(chr, maskValue, (byte) 0), chr);
        chr.dispose();
    }

    public void changeCharacterLookByCoupon() {
        AvatarLook al = chr.getAvatarData().getAvatarLook();
        Integer[] skins = {0, 1, 2, 3, 4, 5, 9, 10, 11, 12, 13};
        int skin = Util.getRandomFromCollection(skins);
        int face = Util.getRandomFromCollection(ItemData.getValidFaces());
        int hair = Util.getRandomFromCollection(ItemData.getValidHairs());
        al.setSkin(skin);
        chr.setStatAndSendPacket(Stat.skin, skin);
        al.setFace(face);
        chr.setStatAndSendPacket(Stat.face, face);
        al.setHair(hair);
        chr.setStatAndSendPacket(Stat.hair, hair);
        consumeItem(2430182);
    }

    public void giveCharacterLookByXuVang(boolean isHair) {
        setSpeakerID(9010038);
        if (isHair) {
            List<Integer> option1 = new ArrayList<>();
            List<Integer> option2 = new ArrayList<>();
            List<Integer> option3 = new ArrayList<>();
            List<Integer> option4 = new ArrayList<>();
            List<Integer> option5 = new ArrayList<>();
            List<Integer> option6 = new ArrayList<>();
            List<Integer> option7 = new ArrayList<>();
            List<Integer> option8 = new ArrayList<>();
            List<Integer> option9 = new ArrayList<>();
            List<Integer> option10 = new ArrayList<>();
            List<Integer> option11 = new ArrayList<>();
            for (int i = 35000; i <= 49999; i++) {
                if (StringData.getItemStringById(i) != null && StringData.getItemStringById(i).startsWith("Black")) {
                    if (option1.size() < 50) {
                        option1.add(i);
                    } else if (option2.size() < 50) {
                        option2.add(i);
                    } else if (option3.size() < 50) {
                        option3.add(i);
                    } else if (option4.size() < 50) {
                        option4.add(i);
                    } else if (option5.size() < 50) {
                        option5.add(i);
                    } else if (option6.size() < 50) {
                        option6.add(i);
                    } else if (option7.size() < 50) {
                        option7.add(i);
                    } else if (option8.size() < 50) {
                        option8.add(i);
                    } else if (option9.size() < 50) {
                        option9.add(i);
                    } else if (option10.size() < 50) {
                        option10.add(i);
                    } else if (option11.size() < 50) {
                        option11.add(i);
                    }
                }
            }
            int sel = sendNext("#fs13#這裏有很多髮型顏色可供選擇，請從下列分類中選擇（每個分類 50 種髮型顏色）。\r\n" +
                    "#b" +
                    "#L0# 髮型顏色分類 1。#l\r\n" +
                    "#L1# 髮型顏色分類 2。#l\r\n" +
                    "#L2# 髮型顏色分類 3。#l\r\n" +
                    "#L3# 髮型顏色分類 4。#l\r\n" +
                    "#L4# 髮型顏色分類 5。#l\r\n" +
                    "#L5# 髮型顏色分類 6。#l\r\n" +
                    "#L6# 髮型顏色分類 7。#l\r\n" +
                    "#L7# 髮型顏色分類 8。#l\r\n" +
                    "#L8# 髮型顏色分類 9。#l\r\n" +
                    "#L9# 髮型顏色分類 10。#l\r\n" +
                    "#L10# 髮型顏色分類 11。#l\r\n" +
                    "#k"
            );
            List<Integer> options = new ArrayList<>();
            if (sel == 0) {
                options.addAll(option1);
            } else if (sel == 1) {
                options.addAll(option2);
            } else if (sel == 2) {
                options.addAll(option3);
            } else if (sel == 3) {
                options.addAll(option4);
            } else if (sel == 4) {
                options.addAll(option5);
            } else if (sel == 5) {
                options.addAll(option6);
            } else if (sel == 6) {
                options.addAll(option7);
            } else if (sel == 7) {
                options.addAll(option8);
            } else if (sel == 8) {
                options.addAll(option9);
            } else if (sel == 9) {
                options.addAll(option10);
            } else if (sel == 10) {
                options.addAll(option11);
            }
            if (!options.isEmpty()) {
                int[] result = options.stream().mapToInt(Integer::intValue).toArray();
                int answer = sendAskAvatar("更換髮型顏色需要 #e10,000 金幣/次#n，請選擇你喜歡的髮型顏色！", false, false, result);
                if (answer < result.length) {
                    if (chr.getUser().getDonationPoints() >= 10000) {
                        chr.getUser().deductDonationPoints(10000);
                        changeCharacterLook(result[answer]);
                    } else {
                        sendSayOkay("你沒有足夠的 10,000 金幣來更換該髮型顏色！");
                    }
                }
            } else {
                chr.chatMessage("發生未知錯誤。");
            }
        } else {
            List<String> names = new ArrayList<>();
            List<Integer> option1 = new ArrayList<>();
            List<Integer> option2 = new ArrayList<>();
            List<Integer> option3 = new ArrayList<>();
            List<Integer> option4 = new ArrayList<>();
            for (int i = 25000; i <= 29999; i++) {
                if (StringData.getItemStringById(i) != null && !names.contains(StringData.getItemStringById(i))) {
                    names.add(StringData.getItemStringById(i));
                    if (option1.size() < 50) {
                        option1.add(i);
                    } else if (option2.size() < 50) {
                        option2.add(i);
                    } else if (option3.size() < 50) {
                        option3.add(i);
                    } else {
                        option4.add(i);
                    }
                }
            }
            int sel = sendNext("#fs13#這裏有很多眼睛顏色可供選擇，請從下列分類中選擇（每個分類 50 種眼睛顏色）。\r\n" +
                    "#b" +
                    "#L0# 眼睛顏色分類 1。#l\r\n" +
                    "#L1# 眼睛顏色分類 2。#l\r\n" +
                    "#L2# 眼睛顏色分類 3。#l\r\n" +
                    "#L3# 眼睛顏色分類 4。#l\r\n" +
                    "#k"
            );
            List<Integer> options = new ArrayList<>();
            if (sel == 0) {
                options.addAll(option1);
            } else if (sel == 1) {
                options.addAll(option2);
            } else if (sel == 2) {
                options.addAll(option3);
            } else if (sel == 3) {
                options.addAll(option4);
            }
            if (!options.isEmpty()) {
                int[] result = options.stream().mapToInt(Integer::intValue).toArray();
                int answer = sendAskAvatar("更換眼睛顏色需要 #e10,000 金幣/次#n，請選擇你喜歡的眼睛顏色！", false, false, result);
                if (answer < result.length) {
                    if (chr.getUser().getDonationPoints() >= 10000) {
                        chr.getUser().deductDonationPoints(10000);
                        changeCharacterLook(result[answer]);
                    } else {
                        sendSayOkay("你沒有足夠的 10,000 金幣來更換該眼睛顏色！");
                    }
                }
            } else {
                chr.chatMessage("發生未知錯誤。");
            }
        }
    }

    public void giveSkill(int skillId) {
        giveSkill(skillId, 1);
    }

    public void giveSkill(int skillId, int slv) {
        giveSkill(skillId, slv, slv);
    }

    @Override
    public void giveSkill(int skillId, int slv, int maxLvl) {
        Skill skill = SkillData.getSkillDeepCopyById(skillId);
        if (skill == null) {
            return;
        }
        List<Skill> skills = new ArrayList<>();
        skill.setCurrentLevel(slv);
        skill.setMasterLevel(maxLvl);
        skills.add(skill);
        chr.addSkill(skill);
        chr.getClient().write(WvsContext.changeSkillRecordResult(skills, true, false, false));
    }

    public int getSkillByItem() {
        return getSkillByItem(getParentID());
    }

    public int getSkillByItem(int itemId) {
        ItemInfo itemInfo = ItemData.getItemInfoByID(itemId);
        int skillId = itemInfo == null ? 0 : itemInfo.getSkillId();
        if (net.swordie.ms.world.tms282.items.ItemDataEffects.active()) {
            // The 265 table has no (or another) skill for many coupons: live 10-07 the permanent mount coupon 2433460
            // answered "no mount for this item" and stayed in the bag. The 282 item data names the 282 riding skill.
            var plan = net.swordie.ms.world.tms282.items.ItemDataEffects.plan(itemId);
            if (plan != null && plan.kind() == net.swordie.ms.world.tms282.items.ItemDataEffects.Kind.RIDING_SKILL
                    && plan.a() > 0) {
                return plan.a();
            }
        }
        return skillId;
    }

    @Override
    public boolean hasSkill(int skillId) {
        return chr.hasSkill(skillId);
    }

    @Override
    public int getSkillLevel(int skillId) {
        return chr.getSkillLevel(skillId);
    }

    @Override
    public void removeSkill(int skillId) {
        List<Skill> skills = new ArrayList<>();
        Skill skill = chr.getSkill(skillId);
        if (skill == null) {
            return;
        }
        skill.setCurrentLevel(-1);
        skill.setMasterLevel(-1);
        skills.add(skill);
        chr.removeSkill(skillId);
        chr.getClient().write(WvsContext.changeSkillRecordResult(skills, true, false, false));
    }

    public void heal() {
        chr.heal(chr.getMaxHP());
        chr.healMP(chr.getMaxMP());
    }

    public int getCurrentLevel() {
        return chr.getLevel();
    }

    public int getLevel() {
        return chr.getLevel();
    }

    public void addLevel(int level) {
        int curLevel = chr.getLevel();
        for (int i = curLevel + 1; i <= curLevel + level; i++) {
            chr.setStat(Stat.level, i);
            Map<Stat, Object> stats = new HashMap<>();
            stats.put(Stat.level, i);
            stats.put(Stat.exp, (long) 0);
            chr.sendStatsPacket(stats);
            chr.getJobHandler().handleLevelUp((short) i);
            chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.levelUpEffect()), chr);
        }
    }

    @Override
    public void setLevel(int targetLevel) {
        int curLevel = chr.getLevel();
        if (targetLevel > curLevel) {
            addLevel(targetLevel - curLevel);
        }
    }

    @Override
    public void setLevelDirect(int targetLevel) {
        int curLevel = chr.getLevel();
        int nextLevel = Math.min(targetLevel, GameConstants.MAX_LEVEL);
        if (nextLevel <= curLevel) {
            applyLevelDirectHpMpFloor(nextLevel);
            return;
        }
        for (int level = curLevel + 1; level <= nextLevel; level++) {
            chr.setStat(Stat.level, level);
            chr.getJobHandler().handleLevelUp((short) level);
        }
        applyLevelDirectHpMpFloor(nextLevel);
        chr.getAvatarData().getCharacterStat().setExp(0);
        Map<Stat, Object> stats = new HashMap<>();
        stats.put(Stat.level, nextLevel);
        stats.put(Stat.exp, (long) 0);
        chr.sendStatsPacket(stats);
        if (chr.getField() != null) {
            chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.levelUpEffect()), chr);
        }
        chr.getAvatarData().getCharacterStat().updateCharacterStatToSQL();
    }

    private void applyLevelDirectHpMpFloor(int targetLevel) {
        if (targetLevel < 200) {
            return;
        }
        Map<Stat, Object> stats = new HashMap<>();
        int minMaxHP = getLevelDirectMinMaxHP(targetLevel);
        if (chr.getStat(Stat.mhp) < minMaxHP) {
            chr.setStat(Stat.mhp, minMaxHP);
            stats.put(Stat.mhp, minMaxHP);
        }
        if (!JobConstants.isNoManaJob(chr.getJob())) {
            int minMaxMP = getLevelDirectMinMaxMP(targetLevel);
            if (chr.getStat(Stat.mmp) < minMaxMP) {
                chr.setStat(Stat.mmp, minMaxMP);
                stats.put(Stat.mmp, minMaxMP);
            }
        }
        if (!stats.isEmpty()) {
            chr.sendStatsPacket(stats);
            chr.heal(chr.getMaxHP(), true);
            chr.healMP(chr.getMaxMP());
        }
    }

    private int getLevelDirectMinMaxHP(int targetLevel) {
        double levelRate = Math.min(targetLevel, 300) / 300D;
        short job = chr.getJob();
        if (JobConstants.isDemonAvenger(job)) {
            return (int) (100_000 * levelRate);
        }
        BaseStat mainStat = GameConstants.getMainBaseStatForJob(job);
        if (mainStat == BaseStat.str) {
            return (int) (50_000 * levelRate);
        }
        if (mainStat == BaseStat.inte) {
            return (int) (32_000 * levelRate);
        }
        return (int) (42_000 * levelRate);
    }

    private int getLevelDirectMinMaxMP(int targetLevel) {
        double levelRate = Math.min(targetLevel, 300) / 300D;
        BaseStat mainStat = GameConstants.getMainBaseStatForJob(chr.getJob());
        if (mainStat == BaseStat.inte) {
            return (int) (42_000 * levelRate);
        }
        if (mainStat == BaseStat.str) {
            return (int) (22_000 * levelRate);
        }
        return (int) (30_000 * levelRate);
    }

    @Override
    public void addAp(int amount) {
        if (amount <= 0) {
            return;
        }
        int newAp = chr.getAvatarData().getCharacterStat().getAp() + amount;
        chr.setStat(Stat.ap, newAp);
        Map<Stat, Object> stats = new HashMap<>();
        stats.put(Stat.ap, (short) newAp);
        chr.sendStatsPacket(stats);
    }

    public boolean isNativeVMatrixEnabled() {
        return net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.enabled();
    }

    @Override
    public int fullVCore() {
        if(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.enabled())
            return net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.maxForJob(chr);
        int count = 0;
        int currentCoreCount = (int) chr.getMatrixCore().stream()
                .filter(core -> core.getState() != MatrixStateType.DISASSEMBLED)
                .count();
        List<MatrixCore> newCores = new ArrayList<>();
        for (VCoreData coreData : VCore.getSkillNodes()) {
            if (currentCoreCount + newCores.size() >= MatrixConstants.SLOT_MAX) {
                break;
            }
            if (!isLearnableVSkillNode(coreData) || hasUsableVCore(coreData.getCoreID())) {
                continue;
            }
            if (coreData.getConnectSkills().isEmpty()) {
                continue;
            }
            int maxLevel = VCore.getMaxLevel(coreData.getType());
            MatrixCore core = new MatrixCore(chr.getId(), coreData.getCoreID(), coreData.getConnectSkills().get(0), 0, 0);
            core.setMaxLevel(maxLevel);
            core.setSkillLevel(maxLevel);
            core.setExperience(0);
            newCores.add(core);
        }
        if (!newCores.isEmpty()) {
            MatrixCore.saveToSQL(newCores);
            chr.getMatrixCore().addAll(newCores);
            count += newCores.size();
        }
        for (MatrixCore core : chr.getMatrixCore()) {
            VCoreData coreData = VCore.getCore(core.getCoreID());
            if (coreData == null || core.getState() == MatrixStateType.DISASSEMBLED) {
                continue;
            }
            if (coreData.getType() != VCore.SKILL && coreData.getType() != VCore.BOOST) {
                continue;
            }
            int maxLevel = core.getMaxLevel() > 0 ? core.getMaxLevel() : VCore.getMaxLevel(coreData.getType());
            if (core.getMaxLevel() <= 0) {
                core.setMaxLevel(maxLevel);
            }
            if (core.getSkillLevel() >= maxLevel) {
                continue;
            }
            core.setSkillLevel(maxLevel);
            core.setExperience(0);
            core.saveToSQL();
            if (core.isActive()) {
                MatrixHandler.relocCoreSkill(chr, core);
            }
            count++;
        }
        if (count > 0) {
            chr.write(WvsContext.updateVMatrix(chr, false, 0, 0));
        }
        return count;
    }

    private boolean isLearnableVSkillNode(VCoreData coreData) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            return net.swordie.ms.loaders.Etc.VCore.Tms282VCoreEligibility.isLearnableSkill(coreData, chr.getJob());
        }
        if (coreData == null || coreData.getType() != VCore.SKILL) {
            return false;
        }
        if (coreData.isJobSkill(chr.getJob()) || coreData.isClassSkill(chr.getJob())) {
            return true;
        }
        return coreData.getJobs().contains("all") || coreData.getName().contains("Decent");
    }

    private boolean hasUsableVCore(int coreID) {
        return chr.getMatrixCore().stream()
                .anyMatch(core -> core.getCoreID() == coreID && core.getState() != MatrixStateType.DISASSEMBLED);
    }

    @Override
    public boolean areAllVCoresMaxed() {
        if(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.enabled())
            return net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.isMaxed(chr);
        for (VCoreData coreData : VCore.getSkillNodes()) {
            if (isLearnableVSkillNode(coreData) && !hasUsableVCore(coreData.getCoreID())) {
                return false;
            }
        }
        boolean hasCore = false;
        for (MatrixCore core : chr.getMatrixCore()) {
            VCoreData coreData = VCore.getCore(core.getCoreID());
            if (coreData == null || core.getState() == MatrixStateType.DISASSEMBLED) {
                continue;
            }
            if (coreData.getType() != VCore.SKILL && coreData.getType() != VCore.BOOST) {
                continue;
            }
            hasCore = true;
            int maxLevel = core.getMaxLevel() > 0 ? core.getMaxLevel() : VCore.getMaxLevel(coreData.getType());
            if (core.getSkillLevel() < maxLevel) {
                return false;
            }
        }
        return hasCore;
    }

    @Override
    public int advanceToFourthJobIfNeeded() {
        int count = 0;
        while (!JobConstants.isReadyForFifthJob(chr.getJob(), chr.getSubJob())) {
            Short nextJob = JobConstants.getNextJobForAdvancement(chr.getJob(), chr.getSubJob());
            if (nextJob == null || SkillData.getSkillsByJob(nextJob).isEmpty()) {
                break;
            }
            if (JobConstants.isDualBlade(nextJob)) {
                jobAdvanceForDB(nextJob);
            } else {
                jobAdvance(nextJob);
            }
            count++;
            if (count > 12) {
                break;
            }
        }
        return count;
    }

    @Override
    public boolean isReadyForFifthJob() {
        return JobConstants.isReadyForFifthJob(chr.getJob(), chr.getSubJob());
    }

    @Override
    public boolean completeJobVIfNeeded() {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            return completeUnlockQuests(java.util.stream.IntStream.range(
                    QuestConstants.V_JOB_QUEST_ADV_START, QuestConstants.V_JOB_QUEST_ADV_END).toArray());
        }
        if (chr.hasQuestCompleted(QuestConstants.FIFTH_JOB_QUEST)) {
            return false;
        }
        for (int questId = QuestConstants.V_JOB_QUEST_ADV_START; questId < QuestConstants.V_JOB_QUEST_ADV_END; questId++) {
            if (!chr.hasQuestCompleted(questId)) {
                chr.completeQuest(questId);
            }
        }
        return true;
    }

    // 六轉開通記錄：此 helper 只完成任務；起源核心初始化由明確的呼叫者政策處理。
    // 38601~38612 為主線、38618~38623 為符文收集、38624/38625 為新力量與 HEXA Matrix。
    private static final int[] SIXTH_JOB_MAINLINE_QUESTS = {
            1488,
            38601, 38602, 38603, 38604, 38605, 38606, 38607, 38608, 38609,
            38611, 38612,
            38618, 38619, 38620, 38621, 38622, 38623,
            38624, 38625
    };

    private boolean completeSixthJobMainlineQuests() {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3)
            return completeUnlockQuests(SIXTH_JOB_MAINLINE_QUESTS);
        boolean completedAny = false;
        for (int questID : SIXTH_JOB_MAINLINE_QUESTS) {
            if (!chr.hasQuestCompleted(questID)) {
                chr.completeQuest(questID, true);
                completedAny = true;
            }
        }
        return completedAny;
    }

    /** Shortcut completion: no objective checks or start/end rewards. Account rows retain their ownership. */
    private boolean completeUnlockQuests(int[] quests) {
        boolean[] changed = {false};
        net.swordie.ms.client.character.tms282.EconomyLocks.packet(chr, () -> {
            for (int quest : quests) if (!chr.hasQuestCompleted(quest)) {
                net.swordie.ms.client.character.quest.Tms282QuestService.markStarted(chr, quest);
                chr.completeQuest(quest, true);
                if (!chr.hasQuestCompleted(quest)) throw new IllegalStateException("轉職任務未完成：" + quest);
                changed[0] = true;
            }
            return null;
        });
        return changed[0];
    }

    @Override
    public boolean completeJobVIIfNeeded() {
        return completeSixthJobMainlineQuests();
    }

    @Override
    public int fullHexaCore() {
        int job = chr.getJob();
        List<Integer> coreIds = new ArrayList<>(HexaCore.getAllSkillCoreIdsForJob(job));
        SixthJobSkillConstants.OriginAscentEntry originEntry = SixthJobSkillConstants.getOriginEntryForJob(job);
        if (originEntry != null && !coreIds.contains(originEntry.originCoreId())) {
            coreIds.add(originEntry.originCoreId());
        }
        if (coreIds.isEmpty()) {
            return 0;
        }
        for (int coreID : coreIds) {
            HexaCore.HexaSkillCoreData coreData = HexaCore.getSkillCoreData(coreID);
            int maxLevel = coreData != null && coreData.getMaxLevel() > 0 ? coreData.getMaxLevel() : 30;
            chr.setHexaSkill(coreID, maxLevel);
        }
        SixthJobSkillConstants.syncSixthJobSkills(chr);
        if (originEntry != null) {
            int maxLevel = 30;
            HexaCore.HexaSkillCoreData originCoreData = HexaCore.getSkillCoreData(originEntry.originCoreId());
            if (originCoreData != null && originCoreData.getMaxLevel() > 0) {
                maxLevel = originCoreData.getMaxLevel();
            }
            SixthJobSkillConstants.grantSixthJobSkillsForJob(chr, maxLevel, maxLevel);
        }
        completeSixthJobMainlineQuests();
        chr.write(WvsContext.hexaSkillsUpdate(chr));
        SixthJobSkillConstants.sendAscentSkillStackUpdate(chr);
        return coreIds.size();
    }

    @Override
    public boolean areAllHexaCoresMaxed() {
        int job = chr.getJob();
        List<Integer> coreIds = new ArrayList<>(HexaCore.getAllSkillCoreIdsForJob(job));
        SixthJobSkillConstants.OriginAscentEntry originEntry = SixthJobSkillConstants.getOriginEntryForJob(job);
        if (originEntry != null && !coreIds.contains(originEntry.originCoreId())) {
            coreIds.add(originEntry.originCoreId());
        }
        if (coreIds.isEmpty()) {
            return false;
        }
        for (int coreID : coreIds) {
            HexaCore.HexaSkillCoreData coreData = HexaCore.getSkillCoreData(coreID);
            int maxLevel = coreData != null && coreData.getMaxLevel() > 0 ? coreData.getMaxLevel() : 30;
            if (chr.getHexaSkillLevel(coreID) < maxLevel) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getChannelId() {
        return chr.getClient().getChannel();
    }

    @Override
    public long getOnlineTimeMinutes() {
        return chr.getOnlineTime() / 60L;
    }

    @Override
    public int getInventorySlotCount(int invType) {
        InvType type = InvType.getInvTypeByVal(invType);
        if (type == null) {
            return 0;
        }
        Inventory inv = chr.getInventoryByType(type);
        return inv == null ? 0 : inv.getSlots();
    }

    @Override
    public String getCharacterName() {
        return chr.getName();
    }

    @Override
    public int getAccountId() {
        return chr.getAccount().getId();
    }

    @Override
    public int getCharacterId() {
        return chr.getId();
    }

    @Override
    public String getHpMpAnalysis() {
        return chr.getHpMpAnalysis();
    }

    @Override
    public boolean isGm() {
        return chr.isGM();
    }

    @Override
    public int getMaplePoints() {
        return chr.getUser().getMaplePoints();
    }

    @Override
    public int getNxPrepaid() {
        return chr.getUser().getNxPrepaid();
    }


    @Override
    public void addMaplePoints(int amount) {
        if (amount > 0) {
            chr.addMaplePoint(amount);
        }
    }

    @Override
    public void removeItemBySlot(int invType, int slot) {
        InvType type = InvType.getInvTypeByVal(invType);
        if (type == null || slot <= 0) {
            return;
        }
        Item item = chr.getInventoryByType(type).getItemBySlot((short) slot);
        if (item != null) {
            chr.consumeItem(item, item.getQuantity());
        }
    }

    @Override
    public boolean isEventClaimed(int questId, String key) {
        String val = chr.getQRValueByKey(questId, key);
        return val != null && val.equals("1");
    }

    @Override
    public void markEventClaimed(int questId, String key) {
        if (!chr.hasQuest(questId)) {
            chr.createQuestWithQRValue(questId, key + "=0");
        }
        chr.setQRValueByKey(questId, key, "1");
    }

    @Override
    public List<CustomShopItem> getCustomShopItems(String shopKey) {
        return CustomShopData.getItems(shopKey);
    }

    @Override
    public String getCustomShopWelcomeName(String shopKey) {
        return CustomShopData.getWelcomeName(shopKey);
    }

    @Override
    public long getCustomShopBalance(int currencyType) {
        CustomShopCurrency currency = CustomShopCurrency.getByType(currencyType);
        if (currency == null) {
            return 0;
        }
        return switch (currency) {
            case NX_PREPAID -> chr.getUser().getNxPrepaid();
            case MAPLE_POINT -> chr.getUser().getMaplePoints();
            case MESO -> chr.getMoney();
            case BOSS_COIN, WITCH_BALL, VIP_COIN -> getQuantityOfItem(currency.getTokenItemId());
            case STAR_COIN -> getHypayBalance();
        };
    }

    @Override
    public String getCustomShopCurrencyLabel(int currencyType) {
        CustomShopCurrency currency = CustomShopCurrency.getByType(currencyType);
        return currency == null ? "未知" : currency.getLabel();
    }

    @Override
    public int getHypayBalance() {
        return HypayManager.getPayBalance(chr.getUser().getName());
    }

    @Override
    public void addHypayBalance(int amount) {
        if (amount <= 0) {
            return;
        }
        HypayManager.addPay(
                chr.getUser().getName(),
                amount,
                chr.getName(),
                chr.getId(),
                chr.getAccId(),
                "腳本充值",
                "menu script"
        );
    }

    @Override
    public void addNxPrepaid(int amount) {
        if (amount == 0) {
            return;
        }
        chr.getUser().addNXPrepaid(amount);
    }

    @Override
    public void deductUnionCoin(int amount) {
        if (amount <= 0) {
            return;
        }
        Union union = chr.getUnion();
        union.decUnionCoin(amount);
        chr.write(UnionPacket.unionCoin(union.getUnionCoin()));
        chr.setQRValueByKey(QuestConstants.UNION_POINT, "point", union.getUnionCoin());
    }

    @Override
    public Object getItemBySlot(int invType, int slot) {
        InvType type = InvType.getInvTypeByVal(invType);
        if (type == null) {
            return null;
        }
        return chr.getInventoryByType(type).getItemBySlot((short) slot);
    }

    @Override
    public void updateItemBySlot(int invType, int slot) {
        InvType type = InvType.getInvTypeByVal(invType);
        if (type == null) {
            return;
        }
        Item item = chr.getInventoryByType(type).getItemBySlot((short) slot);
        if (item instanceof Equip equip) {
            equip.updateToChar(chr);
        } else if (item != null) {
            chr.write(WvsContext.inventoryOperation(true, false, UpdateQuantity,
                    (short) item.getBagIndex(), (byte) -1, 0, item));
        }
    }

    @Override
    public int getJobId() {
        return chr.getJob();
    }

    @Override
    public void resetDamageSkin() {
        DamageSkinSaveData defaultSkin = new DamageSkinSaveData(chr.getId(), 0, 0, false,
                "預設的傷害字型。", FileTime.currentTime());
        chr.setActiveDamageSkin(defaultSkin);
        chr.write(UserPacket.setActiveDamageSkin(chr));
    }

    @Override
    public void deductMaplePoints(int amount) {
        if (amount > 0) {
            chr.getUser().deductMaplePoints(amount);
        }
    }

    @Override
    public boolean convertEquipItemId(int invType, int slot, int newItemId) {
        InvType type = InvType.getInvTypeByVal(invType);
        if (type == null || newItemId <= 0) {
            return false;
        }
        Item item = chr.getInventoryByType(type).getItemBySlot((short) slot);
        if (!(item instanceof Equip equip) || ItemData.getEquipById(newItemId) == null) {
            return false;
        }
        equip.setItemId(newItemId);
        equip.updateToChar(chr);
        return true;
    }

    @Override
    public String getItemName(int itemId) {
        String name = StringData.getItemStringById(itemId);
        return name != null ? name : String.valueOf(itemId);
    }

    @Override
    public int getHypayLeiji() {
        return HypayManager.getLeijiBalance(chr.getUser().getName());
    }

    @Override
    public void changeSkillLevel(int skillId, int level) {
        TimedSkills.clear(chr, skillId);
        if (level <= 0) {
            removeSkill(skillId);
        } else {
            giveSkill(skillId, level, level);
        }
    }

    /** MetaMS overload used by ride/mount items: the skill disappears once {@code expireMs} (epoch ms) passes. */
    public void changeSkillLevel(int skillId, int level, long expireMs) {
        if (level <= 0 || expireMs <= 0) {
            changeSkillLevel(skillId, level);
            return;
        }
        if (TimedSkills.recordGrant(chr, skillId, expireMs)) {
            giveSkill(skillId, level, level);
        }
    }

    @Override
    public void broadcastWorldChat(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        Server.get().broadcastForWorld(UserLocal.chatMsg(Notice2, message));
    }

    @Override
    public int getItemIdBySlot(int invType, int slot) {
        Object item = getItemBySlot(invType, slot);
        return item instanceof Item i ? i.getItemId() : 0;
    }

    private List<ItemWarehouseManager.Entry> getWarehouseEntries() {
        return ItemWarehouseManager.getPendingItems(chr.getId());
    }

    @Override
    public int getWarehouseItemCount() {
        return getWarehouseEntries().size();
    }

    @Override
    public int getWarehouseItemId(int index) {
        List<ItemWarehouseManager.Entry> entries = getWarehouseEntries();
        if (index < 0 || index >= entries.size()) {
            return 0;
        }
        return entries.get(index).getItemId();
    }

    @Override
    public int getWarehouseItemQuantity(int index) {
        List<ItemWarehouseManager.Entry> entries = getWarehouseEntries();
        if (index < 0 || index >= entries.size()) {
            return 0;
        }
        return entries.get(index).getQuantity();
    }

    @Override
    public boolean claimWarehouseItem(int index) {
        List<ItemWarehouseManager.Entry> entries = getWarehouseEntries();
        if (index < 0 || index >= entries.size()) {
            return false;
        }
        ItemWarehouseManager.Entry entry = entries.get(index);
        if (!canHold(entry.getItemId(), entry.getQuantity())) {
            return false;
        }
        if (!ItemWarehouseManager.markClaimed(entry.getId())) {
            return false;
        }
        giveItem(entry.getItemId(), entry.getQuantity());
        return true;
    }

    private static final int[] FAMILIAR_OPTIONS = {
            40001, 40002, 40003, 40004, 40005, 40006, 40011, 40012, 40015, 40017,
            40020, 40021, 40022, 40023, 40028, 40029, 40040, 40041, 40042
    };

    private Item getInventoryItem(int invType, int slot) {
        InvType type = InvType.getInvTypeByVal(invType);
        if (type == null) {
            return null;
        }
        return chr.getInventoryByType(type).getItemBySlot((short) slot);
    }

    private String familiarOptionKey(long itemDbId, int index) {
        return "f_" + itemDbId + "_o" + index;
    }

    @Override
    public boolean isDailyEventClaimed(int questId, String key) {
        return ExpandMenuHelper.isDailyFlagSet(chr, questId, key);
    }

    @Override
    public void markDailyEventClaimed(int questId, String key) {
        ExpandMenuHelper.markDailyFlag(chr, questId, key);
    }

    @Override
    public int getPlatformarStarByStage(int floor) {
        return ExpandMenuHelper.getPlatformarStar(chr, floor);
    }

    @Override
    public int resetHexaCoreWithRefund() {
        int totalLevels = chr.clearHexaSkills();
        if (totalLevels <= 0) {
            return 0;
        }
        int souls = (int) (totalLevels * 2 * 0.7);
        int shards = (int) (totalLevels * 10 * 0.7);
        if (souls > 0 && canHold(2636884, souls)) {
            giveItem(2636884, souls);
        }
        if (shards > 0 && canHold(4009548, shards)) {
            giveItem(4009548, shards);
        }
        markEventClaimed(ExpandMenuHelper.MENU_HEXA_QUEST, "reset");
        return totalLevels;
    }

    @Override
    public void openBossUI() {
        openUI(1338);
    }

    @Override
    public int countEquippedItemsById(String itemIdsCsv) {
        if (itemIdsCsv == null || itemIdsCsv.isEmpty()) {
            return 0;
        }
        Set<Integer> ids = new HashSet<>();
        for (String part : itemIdsCsv.split(",")) {
            try {
                ids.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        int count = 0;
        for (Item item : chr.getEquippedInventory().getItems()) {
            if (ids.contains(item.getItemId())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public long getWildBossRespawnAt(int mobTemplateId) {
        return ExpandMenuHelper.getWildBossRespawnAt(chr, mobTemplateId);
    }

    @Override
    public String getWildBossKiller(int mobTemplateId) {
        return ExpandMenuHelper.getWildBossKiller(chr, mobTemplateId);
    }

    @Override
    public boolean isFamiliarCard(int invType, int slot) {
        Item item = getInventoryItem(invType, slot);
        return item != null && ItemConstants.isFamiliar(item.getItemId());
    }

    @Override
    public int getFamiliarCardOption(int invType, int slot, int index) {
        Item item = getInventoryItem(invType, slot);
        if (item == null || index < 0 || index > 2) {
            return 0;
        }
        String val = chr.getQRValueByKey(ExpandMenuHelper.MENU_FAMILIAR_QUEST,
                familiarOptionKey(item.getId(), index));
        if (val == null || val.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public boolean rerollFamiliarCard(int invType, int slot, int times) {
        // The old quest-string options are not native282 familiar options. Never charge for a no-op.
        if (!isFamiliarRerollAvailable()) {
            return false;
        }
        if (times <= 0 || times > 100) {
            return false;
        }
        Item item = getInventoryItem(invType, slot);
        if (item == null || !ItemConstants.isFamiliar(item.getItemId())) {
            return false;
        }
        int cubeId = 5743003;
        if (getQuantityOfItem(cubeId) < times) {
            return false;
        }
        consumeItem(cubeId, times);
        Random rnd = new Random();
        for (int t = 0; t < times; t++) {
            for (int i = 0; i < 3; i++) {
                int opt = FAMILIAR_OPTIONS[rnd.nextInt(FAMILIAR_OPTIONS.length)];
                chr.setQRValueByKey(ExpandMenuHelper.MENU_FAMILIAR_QUEST,
                        familiarOptionKey(item.getId(), i), String.valueOf(opt));
            }
        }
        return true;
    }

    public boolean isFamiliarRerollAvailable() {
        return net.swordie.ms.connection.protocol.Protocol.active().version()
                != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
    }

    @Override
    public boolean buyCustomShopItem(String shopKey, int index, int quantity) {
        if (quantity <= 0 || quantity > 9999) {
            return false;
        }
        List<CustomShopItem> items = CustomShopData.getItems(shopKey);
        if (index < 0 || index >= items.size()) {
            return false;
        }
        CustomShopItem shopItem = items.get(index);
        CustomShopCurrency currency = CustomShopCurrency.getByType(shopItem.getCurrencyType());
        if (currency == null) {
            return false;
        }
        long unitPrice = shopItem.getPrice();
        long totalPrice = unitPrice * quantity;
        if (totalPrice <= 0 || totalPrice > Integer.MAX_VALUE) {
            return false;
        }
        int total = (int) totalPrice;
        if (!canHold(shopItem.getItemId(), quantity)) {
            return false;
        }
        if (getCustomShopBalance(shopItem.getCurrencyType()) < total) {
            return false;
        }
        if (!deductCustomShopCurrency(currency, total, shopKey, shopItem.getItemId(), quantity)) {
            return false;
        }
        giveCustomShopItem(shopItem, quantity);
        return true;
    }

    private boolean deductCustomShopCurrency(CustomShopCurrency currency, int total, String shopKey, int itemId, int quantity) {
        if (net.swordie.ms.client.character.tms282.Tms282GmToggles.freeShop(chr)) {
            return true; // admin panel GM 開關: this GM's purchases cost nothing
        }
        switch (currency) {
            case NX_PREPAID -> {
                if (chr.getUser().getNxPrepaid() < total) {
                    return false;
                }
                chr.getUser().deductNXPrepaid(total);
                return true;
            }
            case MAPLE_POINT -> {
                if (chr.getUser().getMaplePoints() < total) {
                    return false;
                }
                chr.addMaplePoint(-total);
                return true;
            }
            case MESO -> {
                if (chr.getMoney() < total) {
                    return false;
                }
                chr.deductMoney(total);
                return true;
            }
            case BOSS_COIN, WITCH_BALL, VIP_COIN -> {
                if (getQuantityOfItem(currency.getTokenItemId()) < total) {
                    return false;
                }
                chr.consumeItem(currency.getTokenItemId(), total);
                return true;
            }
            case STAR_COIN -> {
                String itemName = String.valueOf(itemId);
                return HypayManager.deductPay(
                        chr.getUser().getName(),
                        total,
                        chr.getName(),
                        chr.getId(),
                        chr.getAccId(),
                        shopKey,
                        itemName + "x" + quantity
                );
            }
            default -> {
                return false;
            }
        }
    }

    private void giveCustomShopItem(CustomShopItem shopItem, int quantity) {
        if (shopItem.getItemFlag() == 1) {
            for (int i = 0; i < quantity; i++) {
                Equip equip = ItemData.getEquipDeepCopyFromID(shopItem.getItemId(), false);
                equip.setCuttable((short) 0);
                equip.setTitle("可剪刀0次");
                chr.addItemToInventory(equip);
            }
            return;
        }
        if (shopItem.getPeriodMs() > 0) {
            long dayMs = 24L * 60 * 60 * 1000;
            if (shopItem.getPeriodMs() % dayMs == 0) {
                giveItem(shopItem.getItemId(), quantity, "day", shopItem.getPeriodMs() / dayMs);
            } else {
                giveItem(shopItem.getItemId(), quantity, "minute", Math.max(1, shopItem.getPeriodMs() / 60000));
            }
            return;
        }
        giveItem(shopItem.getItemId(), quantity);
    }

    public void setInGameDirectionMode(boolean lockUI, boolean blackFrame, boolean forceMouseOver, boolean showUI) {
        if (chr != null) {
            chr.write(UserLocal.setInGameDirectionMode(lockUI, blackFrame, forceMouseOver, showUI));
        }
    }

    public void lockInGameUI(boolean lock) {
        lockInGameUI(lock, true);
    }

    /**
     * Removes the game's UI and completely prevents the character from moving via player input.
     * InGameDirectionEvent methods, such as forcedInput and moveCamera, won't take effect without locking the UI.
     *
     * @param lock       Whether to lock the game UI; false will unlock the UI and re-enable player input to char
     * @param blackFrame Whether black bars should appear while the UI is locked. Many older cutscenes set this to true
     */
    public void lockInGameUI(boolean lock, boolean blackFrame) {
        if (chr != null) {
            chr.write(UserLocal.setInGameDirectionMode(lock, blackFrame, false, !lock));
        }
        inGameUILocked = lock;
    }

    public void curNodeEventEnd(boolean enable) {
        setCurNodeEventEnd(enable);
        chr.write(FieldPacket.curNodeEventEnd(enable));
    }

    public void setCurNodeEventEnd(boolean curNodeEventEnd) {
        this.curNodeEventEnd = curNodeEventEnd;
    }

    public void setTemporarySkillSet(int skillSet) {
        if (chr != null) {
            chr.write(TemporarySkillMan.setTemporarySkillSet(skillSet, 0));
        }
    }

    public void setStandAloneMode(boolean enable) {

    }

    public void setDirectionMode(boolean show, int unk) {
        chr.write(UserLocal.setDirectionMode(show, unk));
    }

    public void lockUI() {
        curNodeEventEnd(true);
        setTemporarySkillSet(0);
        lockInGameUI(true, false);
    }

    public void unlockUI() {
        curNodeEventEnd(false);
        setTemporarySkillSet(0);
        lockInGameUI(false, true);
    }

    /** Boss movies outlive their entry script; script cleanup must not unlock them early. */
    public void beginBossIntro(int durationMs) {
        if (chr == null || durationMs <= 0) return;
        Object token = new Object();
        bossIntroField = chr.getField();
        bossIntroToken = token;
        lockInGameUI(true, false);
        chr.getTimer().addEvent(() -> {
            if (bossIntroToken == token) endBossIntro();
        }, durationMs + 100); // failsafe even when a movie's stage callback fails
    }

    public void endBossIntro() {
        bossIntroToken = null;
        bossIntroField = null;
        lockInGameUI(false, false);
    }

    public void cancelBossIntroIfLeaving(Field destination) {
        if (chr != null) net.swordie.ms.scripts.event.EventManager.get().onWarpRequested(chr, destination);
        if (bossIntroToken != null && bossIntroField != destination) endBossIntro();
    }

    private void unlockUIIfNeeded() {
        if (bossIntroToken != null && chr != null && bossIntroField == chr.getField()) return;
        if (inGameUILocked || curNodeEventEnd) {
            unlockUI();
        }
    }

    public void progressMessageFont(String msg) {
        progressMessageFont(3, 20, 20, 0, msg);
    }

    /**
     * @fontNameType 1 -> 9
     * @fontColorType 0: white; 1,3: Black; 2: Brown; 4: yellow; 5,6: blue; 7: Red; 9: Pink; 10: Orange; 20: Green
     */
    public void progressMessageFont(int fontNameType, int fontSize, int fontColorType, int fadeOutDelay, String message) {
        chr.write(WvsContext.progressMessageFont(fontNameType, fontSize, fontColorType, fadeOutDelay, message));
    }

    /**
     * Applies the specified emotion to the character locally.
     *
     * @param emotion      The emotion to use, from 1-44. All cash emotes adhere to wz ordering.
     *                     Order is as follows: 1-7: standard, 8-22 and 23-37: original cash emotes, 38: Ursus KO, 39-44: foreign cash emotes.
     * @param duration     The time (in ms) the emotion is active on the character if it's not overwritten
     * @param byItemOption Specifies if the emotion was triggered from using an item (e.g. Lamb Kebabs [2010048])
     */
    public void localEmotion(int emotion, int duration, boolean byItemOption) {
        chr.write(UserLocal.emotion(emotion, duration, byItemOption));
    }


    // Field-related methods -------------------------------------------------------------------------------------------

    @Override
    public void warp(int id) {
        warp(id, 0);
    }

    public void warp(int id, boolean executeAfterScript) {
        warp(id, 0, executeAfterScript, false);
    }

    @Override
    public void warp(int id, int pid) {
        warp(id, pid, true, false);
    }

    public void warp(int id, int pid, boolean instanceField) {
        warp(id, pid, true, instanceField);
    }

    public void warp(int mid, int pid, boolean executeAfterScript, boolean instanceField) {
        if (executeAfterScript) {
            FieldTransferInfo fti = getFieldTransferInfo();
            fti.setFieldId(mid);
            fti.setPortal(pid);
            fti.setIsInstanceField(instanceField);
        } else {
            chr.warp(mid, pid);
        }
    }

    public void warpParty(int id, Party party) {
        int fieldID = chr.getFieldID();
        for (Char onlineChar : party.getOnlineChars()) {
            if (onlineChar.getField().getId() == fieldID) {
                onlineChar.warp(id);
            }
        }
    }

    public void warpParty(Char chr, int id, Party party) {
        int fieldID = chr.getFieldID();
        for (Char onlineChar : party.getOnlineChars()) {
            if (onlineChar.getField().getId() == fieldID) {
                onlineChar.warp(id);
            }
        }
    }

    public void warpField(int id, Field field) {
        for (Char charInField : field.getChars()) {
            charInField.warp(id);
        }
    }

    public void changeChannelAndWarp(int channel, int fieldID, boolean executeAfterScript, boolean instanceField) {
        if (executeAfterScript) {
            FieldTransferInfo fti = getFieldTransferInfo();
            fti.setChannel(channel);
            fti.setFieldId(fieldID);
            fti.setIsInstanceField(instanceField);
        } else {
            Client c = chr.getClient();
            c.setOldChannel(c.getChannel());
            chr.changeChannelAndWarp((byte) channel, fieldID);
        }
    }

    public void warpField(int fieldId) {
        warp(fieldId, 0);
    }

    public void warpField(int fieldId, int portalId) {
        // only warp after script has ended
        FieldTransferInfo fti = getFieldTransferInfo();
        fti.setFieldId(fieldId);
        fti.setPortal(portalId);
    }

    public void changeChannelAndWarp(int channel, int fieldID) {
        changeChannelAndWarp(channel, fieldID, true, false);
    }

    @Override
    public int getFieldID() {
        return chr.getField().getId();
    }

    public void modifiedCharacter() {
        chr.write(WvsContext.characterModified(chr));
        chr.initialize();
    }

    @Override
    public void warpInstanceIn(Char chr, int id) {
        warpInstance(chr, id, true, 0, false);
    }

    public void warpInstanceIn(Char chr, int id, int portal) {
        warpInstance(chr, id, true, portal, false);
    }

    public void warpInstanceIn(Char chr, int id, boolean partyAllowed) {
        warpInstance(chr, id, true, 0, partyAllowed);
    }

    public void warpInstanceIn(Char chr, int id, int portalId, boolean partyAllowed) {
        warpInstance(chr, id, true, portalId, partyAllowed);
    }

    @Override
    public void warpInstanceOut(Char chr, int id) {
        warpInstanceOut(chr, id, 0);
    }

    public void warpInstanceOut(Char chr, int id, int portalId) {
        warpInstance(chr, id, false, portalId, false);
    }

    private void warpInstance(Char chr, int fieldId, boolean in, int portalId, boolean partyAllowed) {
        if (!in && chr.getInstance() instanceof net.swordie.ms.scripts.event.EventFieldInstance eventInstance) {
            int target = fieldId >= 0 ? fieldId : eventInstance.getForcedReturn();
            chr.warp(target, Math.max(portalId, 0), false);
            return;
        }
        Instance instance;
        Party party = chr.getParty();
        boolean SOLO = party == null;
        if (in) {
            // warp party in if there is a party and party is allowed, solo instance otherwise
            if (partyAllowed) {
                if (SOLO) {
                    instance = new Instance(chr);
                } else {
                    instance = new Instance(party);
                    party.setInstance(instance);
                }
            } else {
                instance = new Instance(chr);
            }
            // setup the instance & warp
            instance.setup(fieldId, portalId);
            DataPrinter.send(DataPrinter.INSTANCE_FIELD, "[Instance In] " + chr.getName() + " warp in Field " + fieldId);
        } else {
            // multiplayer-0930: out of the instance chr is in. Only characters standing inside that same instance
            // move (chr, and the party members inside a shared party instance); members in town, in another boss
            // run or already out are not touched. Without an instance only chr moves (before: every online party
            // member was warped, and the whole instance cleared even for members still fighting elsewhere).
            instance = chr.getInstance();
            stopEvents();
            if (instance == null) {
                chr.setInstance(null);
                chr.warp(fieldId, portalId, false);
            } else {
                int forcedReturn;
                int forcedReturnPortal;
                if (fieldId >= 0) {
                    forcedReturn = fieldId;
                    forcedReturnPortal = -1;
                } else {
                    forcedReturn = instance.getForcedReturn();
                    forcedReturnPortal = instance.getForcedReturnPortalId();
                }
                List<Char> inside = new ArrayList<>();
                inside.add(chr);
                for (Char x : net.swordie.ms.world.field.InstanceMembership.presentMembers(instance)) {
                    if (x != chr) {
                        inside.add(x);
                    }
                }
                for (Char x : inside) {
                    x.setInstance(null); // per-character cleanup (death count, debuffs, QR 102401)
                    instance.removeChar(x);
                }
                try {
                    for (Char x : inside) {
                        x.warp(forcedReturn, Math.max(forcedReturnPortal, 0), false);
                    }
                } finally {
                    net.swordie.ms.world.field.InstanceMembership.closeIfAbandoned(instance);
                }
            }
            DataPrinter.send(DataPrinter.INSTANCE_FIELD, "[Instance Out] " + chr.getName() + " warp out Field " + fieldId);
        }
    }

    public void setInstanceTime(int seconds, boolean showClock) {
        setInstanceTime(seconds, 0, 0, showClock);
    }

    public void setInstanceTime(int seconds, int forcedReturnFieldId, boolean showClock) {
        Instance instance = chr.getInstance();
        if (instance != null) {
            if (forcedReturnFieldId != 0) {
                instance.setForcedReturn(forcedReturnFieldId);
            }
            if (instance.getRemainingTime() < System.currentTimeMillis()) {
                // don't override old timeout value
                instance.setTimeout(seconds, showClock);
            }
        }
    }

    public void setInstanceTime(int seconds, int forcedReturnFieldId, int portalId, boolean showClock) {
        Instance instance = chr.getInstance();
        if (instance != null) {
            if (forcedReturnFieldId != 0) {
                instance.setForcedReturn(forcedReturnFieldId);
            }
            if (portalId != 0) {
                instance.setForcedReturnPortalId(portalId);
            }
            if (instance.getRemainingTime() < System.currentTimeMillis()) {
                // don't override old timeout value
                instance.setTimeout(seconds, showClock);
            }
        }
    }

    public void setInstanceTime(int seconds) {
        setInstanceTime(seconds, 0, 0, true);
    }

    public void setInstanceTime(int seconds, int forcedReturnFieldId) {
        setInstanceTime(seconds, forcedReturnFieldId, true);
    }

    public void setInstanceTime(int seconds, int forcedReturnFieldId, int portalId) {
        setInstanceTime(seconds, forcedReturnFieldId, portalId, true);
    }

    @Override
    public int getReturnField() {
        return returnField;
    }

    @Override
    public void setReturnField(int returnField) {
        this.returnField = returnField;
    }

    @Override
    public void setReturnField() {
        setReturnField(chr.getFieldID());
    }

    @Override
    public int getReturnPortal() {
        return returnPortal;
    }

    @Override
    public void setReturnPortal(int returnPortal) {
        this.returnPortal = returnPortal;
    }

    @Override
    public void setReturnPortal() {
        setReturnPortal(getParentID());
    }

    @Override
    public boolean hasMobsInField() {
        return hasMobsInField(chr.getFieldID());
    }

    public Mob waitForMobDeath() {
        Mob response = null;
        if (isActive(ScriptType.FirstEnterField)) {
            ScriptInfo si = getScriptInfoByType(ScriptType.FirstEnterField);
            if (si == null) {
                throw new NullPointerException(INTENDED_NPE_MSG);
            }
            response = si.awaitResponseMob(30 * 60 * 1000);
        } else if (isActive(ScriptType.Field)) {
            ScriptInfo si = getScriptInfoByType(ScriptType.Field);
            if (si == null) {
                throw new NullPointerException(INTENDED_NPE_MSG);
            }
            response = si.awaitResponseMob(30 * 60 * 1000);
        } else if (isActive(ScriptType.Quest)) {
            ScriptInfo si = getScriptInfoByType(ScriptType.Quest);
            if (si == null) {
                throw new NullPointerException(INTENDED_NPE_MSG);
            }
            response = si.awaitResponseMob(30 * 60 * 1000);
        }
        if (response == null) {
            throw new NullPointerException(INTENDED_NPE_MSG);
        }
        return (Mob) response;
    }

    public Mob waitForMobDeath(int... possibleMobs) {
        Mob mob = waitForMobDeath();
        while (true) {
            if (mob == null) {
                throw new NullPointerException(INTENDED_NPE_MSG);
            } else {
                for (int mobID : possibleMobs) {
                    if (mob.getTemplateId() == mobID) {
                        return mob;
                    }
                }
                mob = waitForMobDeath();
            }
        }
    }

    @Override
    public boolean hasMobsInField(int fieldid) {
        Field field = chr.getOrCreateFieldByCurrentInstanceType(fieldid);
        return field.getMobs().size() > 0;
    }

    public boolean hasMobsInField(int fieldid, int mobTemplateId) {
        return getAmountOfMobsInField(fieldid, mobTemplateId) > 0;
    }

    @Override
    public int getAmountOfMobsInField() {
        return getAmountOfMobsInField(chr.getFieldID());
    }

    public int getAmountOfMobsInField(int fieldid, int mobTemplateId) {
        Field field = chr.getOrCreateFieldByCurrentInstanceType(fieldid);
        return (int) field.getMobs().stream().filter(mob -> mob.getTemplateId() == mobTemplateId).count();
    }

    @Override
    public int getAmountOfMobsInField(int fieldid) {
        Field field = FieldData.getFieldById(fieldid);
        return field.getMobs().size();
    }

    public void killMobs() {
        chr.getField().killMobs();
    }

    /**
     * Kill one or all mobs with the given mob ID in the characters map
     *
     * @param mobId   mob id to kill
     * @param killAll whether or not to kill all of them or just the first one
     */
    public void killMob(int mobId, boolean killAll) {
        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        for (Mob m : field.getMobs()) {
            if (m.getTemplateId() == mobId) {
                m.remove(false);
                if (!killAll) {
                    return;
                }
            }
        }
    }

    public void showWeatherNoticeToField(String text, WeatherEffNoticeType type) {
        showWeatherNoticeToField(text, type, 7000); // 7 seconds
    }

    public void showWeatherNoticeToField(String text, WeatherEffNoticeType type, int duration) {
        Field field = chr.getField();
        field.broadcast(WvsContext.weatherEffectNotice(type, text, duration));
    }

    public void showEffectToField(String dir) {
        showEffectToField(dir, 0);
    }

    public void showEffectToField(String dir, int delay) {
        showEffectToField(dir, 4, delay);
    }

    public void showEffectToField(String dir, int placement, int delay) {
        Field field = chr.getField();
        field.broadcast(UserPacket.effect(Effect.effectFromWZ(dir, false, delay, placement, 0)));
    }

    public void showFieldEffect(String dir) {
        showFieldEffect(dir, 0);
    }

    @Override
    public void showFieldEffect(String dir, int delay) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.getFieldEffectFromWz(dir, delay)));
    }

    public void showFieldEffectToField(String dir) {
        showFieldEffectToField(dir, 0);
    }

    public void showFieldEffectToField(String dir, int delay) {
        Field field = chr.getField();
        field.broadcast(FieldPacket.fieldEffect(FieldEffect.getFieldEffectFromWz(dir, delay)));
    }

    public void showOffFieldEffect(String dir) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.getOffFieldEffectFromWz(dir, 0)));
    }

    public void showFieldBackgroundEffect(String dir) {
        showFieldBackgroundEffect(dir, 0);
    }

    public void showFieldBackgroundEffect(String dir, int delay) {
        Field field = chr.getField();
        chr.write(FieldPacket.fieldEffect(FieldEffect.getFieldBackgroundEffectFromWz(dir, delay)));
    }

    public void showFadeTransition(int duration, int fadeInTime, int fadeOutTime) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.takeSnapShotOfClient2(fadeInTime, duration, fadeOutTime, true)));
    }

    public void screen(String string) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.screen(string)));
    }

    public void showFade(int duration) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.takeSnapShotOfClient(duration)));
    }

    public void setFieldColour(GreyFieldType colorFieldType, short red, short green, short blue, int time) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.setFieldColor(colorFieldType, red, green, blue, time)));
    }

    public void setFieldGrey(GreyFieldType colorFieldType, boolean show) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.setFieldGrey(colorFieldType, show)));
    }

    public void removeOverlapScreen(int duration) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.removeOverlapScreen(duration)));
    }

    public void teraBlinkWarp(int arg1, String string, int arg2, int arg3, int arg4, int arg5, int arg6, int arg7, int arg8) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.teraBlinkWarp(arg1, string, arg2, arg3, arg4, arg5, arg6, arg7, arg8)));
    }

    public void teraBlinkEff(int arg1, String string, String string2, int arg2, int arg3) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.teraBlinkEff(arg1, string, string2, arg2, arg3)));
    }

    @Override
    public void dropItem(int itemId, int x, int y) {
        Field field = chr.getField();
        Drop drop = new Drop(field.getNewObjectID());
        drop.setItem(ItemData.getItemDeepCopy(itemId));
        Position position = new Position(x, y);
        drop.setPosition(position);
        field.drop(drop, position, true);
    }

    @Override
    public void teleportInField(Position position) {
        chr.write(FieldPacket.teleport(position, chr));
    }

    @Override
    public void teleportInField(int x, int y) {
        teleportInField(new Position(x, y));
    }

    @Override
    public void teleportToPortal(int portalId) {
        Portal portal = chr.getField().getPortalByID(portalId);
        if (portal != null) {
            Position position = new Position(portal.getX(), portal.getY());
            chr.write(FieldPacket.teleport(position, chr));
        }
    }

    public Drop getDropInRect(int itemID, Rect rect) {
        Field field = getField();
        if (field == null) {
            field = chr.getField();
        }
        return field.getDropsInRect(rect).stream()
                .filter(drop -> drop.getItem() != null && drop.getItem().getItemId() == itemID)
                .findAny().orElse(null);
    }

    @Override
    public Drop getDropInRect(int itemID, int rectRange) {
        return getDropInRect(itemID, new Rect(
                new Position(
                        chr.getPosition().getX() - rectRange,
                        chr.getPosition().getY() - rectRange),
                new Position(
                        chr.getPosition().getX() + rectRange,
                        chr.getPosition().getY() + rectRange))
        );

    }

    public void setObjectState(String objName, int curState) {
        chr.getField().broadcast(FieldPacket.setObjectState(objName, curState));
    }

    // Life-related methods --------------------------------------------------------------------------------------------


    // NPC methods
    /** Creates a script-owned actor on this character's current field and returns its real object ID. */
    public int sendNpcController(int npcId, int x, int y) {
        Field currentField = chr.getField();
        if (currentField == null) throw new IllegalStateException("NPC controller requires a field");
        clearStaleScriptNpcControllers(currentField);
        Npc npc = NpcData.getNpcDeepCopyById(npcId);
        if (npc == null) throw new IllegalArgumentException("Unknown NPC template " + npcId);
        npc.setObjectId(-1);
        npc.setPosition(new Position(x, y));
        npc.setCy(y);
        npc.setRx0(x - 50);
        npc.setRx1(x + 50);
        Foothold foothold = currentField.findFootHoldBelow(new Position(x, y - 2));
        npc.setFh(foothold == null ? 0 : foothold.getId());
        npc.setNotRespawnable(true);
        npc.setControllerID(chr.getId());
        synchronized (currentField) {
            currentField.addLife(npc);
            scriptNpcControllers.put(npc.getObjectId(), npc);
        }
        chr.write(NpcPool.npcChangeController(npc, true));
        return npc.getObjectId();
    }

    /** The two-argument form addresses object IDs, including distinct actors with the same template. */
    public void sendNpcController(int objectId, boolean controller) {
        Npc npc = scriptNpcControllers.get(objectId);
        if (npc == null) throw new IllegalArgumentException("NPC controller is not owned by this script: " + objectId);
        Field ownerField = npc.getField();
        if (ownerField != chr.getField() || ownerField.getLifeByObjectID(objectId) != npc) {
            removeScriptNpcController(npc, false);
            throw new IllegalStateException("NPC controller belongs to an expired field actor");
        }
        if (controller) {
            npc.setControllerID(chr.getId());
            chr.write(NpcPool.npcChangeController(npc, true));
        } else {
            removeScriptNpcController(npc, true);
        }
    }

    private void removeScriptNpcController(Npc npc, boolean notify) {
        if (!scriptNpcControllers.remove(npc.getObjectId(), npc)) return;
        Field ownerField = npc.getField();
        // Script-owned NPCs have no respawn/lifetime schedule; remove by identity without touching a replacement.
        if (!ownerField.getLifes().remove(npc.getObjectId(), npc)) return;
        if (notify && ownerField == chr.getField()) chr.write(NpcPool.npcChangeController(npc, false, true));
    }

    private void clearStaleScriptNpcControllers(Field currentField) {
        for (Npc npc : scriptNpcControllers.values()) {
            if (npc.getField() != currentField) removeScriptNpcController(npc, false);
        }
    }

    private void clearScriptNpcControllers() {
        for (Npc npc : scriptNpcControllers.values()) removeScriptNpcController(npc, true);
    }

    @Override
    public void spawnNpc(int npcId, int x, int y) {
        Npc npc = NpcData.getNpcDeepCopyById(npcId);
        Position position = new Position(x, y);
        npc.setPosition(position);
        npc.setCy(y);
        npc.setRx0(x + 50);
        npc.setRx1(x - 50);
        npc.setFh(chr.getField().findFootHoldBelow(new Position(x, y - 2)).getId());
        npc.setNotRespawnable(true);
        if (npc.getField() == null) {
            npc.setField(field);
        }
        chr.getField().spawnLife(npc, chr);
    }

    public void spawnNpc(int npcId, int x, int y, boolean left) {
        Npc npc = NpcData.getNpcDeepCopyById(npcId);
        Position position = new Position(x, y);
        npc.setPosition(position);
        npc.setCy(y);
        npc.setRx0(x + 50);
        npc.setRx1(x - 50);
        npc.setFh(chr.getField().findFootHoldBelow(new Position(x, y - 2)).getId());
        npc.setNotRespawnable(true);
        if (npc.getField() == null) {
            npc.setField(field);
        }
        chr.getField().spawnLife(npc, chr);
        chr.write(NpcPool.npcSetForceFlip(npc.getObjectId(), left));
    }

    @Override
    public void removeNpc(int npcId) {
        chr.getField().getNpcs().stream()
                .filter(npc -> npc.getTemplateId() == npcId)
                .findFirst()
                .ifPresent(npc -> chr.getField().removeLife(npc));
    }

    public void removeNpcs(int npcId) {
        for (Npc npc : chr.getField().getNpcs()) {
            if (npc.getTemplateId() == npcId) {
                chr.getField().removeLife(npc);
            }
        }
    }

    @Override
    public void openNpc(int npcId) {
        Npc npc = NpcData.getNpcDeepCopyById(npcId);
        String script;
        if (npc.getScripts().size() > 0) {
            script = npc.getScripts().get(0);
        } else {
            script = String.valueOf(npc.getTemplateId());
        }
        startScript(chr, npc.getTemplateId(), npcId, script, ScriptType.Npc);
    }

    @Override
    public void openShop(int shopID) {
        NpcShopDlg nsd = NpcData.getShopById(shopID);
        if (nsd != null) {
            if (shopID == 9010038) { // Donation Shop
                createQuestWithQRValue(QuestConstants.DONATION_POINT, "point=" + chr.getUser().getDonationPoints());
            } else if (shopID == 9010040) { // Vote Shop
                createQuestWithQRValue(QuestConstants.VOTE_POINT, "point=" + chr.getUser().getVotePoints());
            }
            nsd.setChar(chr);
            chr.setShop(nsd);
            chr.write(ShopDlg.openShop(0, nsd));
        } else {
            chat(String.format("找不到 NPC %d 的商店。", shopID));
            System.out.printf("Could not find shop with id %d.", shopID);
        }
    }

    // Open Shop but with different NPC ID
    public void openShop(int templateID, int shopID) {
        NpcShopDlg nsd = NpcData.getShopById(shopID);
        if (nsd != null) {
            nsd.setChar(chr);
            chr.setShop(nsd);
            nsd.setNpcTemplateID(templateID);
            chr.write(ShopDlg.openShop(0, nsd));
        } else {
            chat(String.format("找不到 NPC %d 的商店。", shopID));
            System.out.printf("Could not find shop with id %d.", shopID);
        }
    }

    @Override
    public void openTrunk(int npcTemplateID) {
        chr.write(FieldPacket.trunkDlg(new TrunkOpen(npcTemplateID, chr.getAccount().getTrunk())));
    }

    @Override
    public void setSpeakerID(int templateID) {
        NpcScriptInfo nsi = getNpcScriptInfo();
        boolean isNotCancellable = nsi.hasParam(NpcScriptInfo.Param.NotCancellable);
        nsi.resetParam();
        nsi.setOverrideSpeakerTemplateID(templateID);
        if (isNotCancellable) {
            nsi.addParam(NpcScriptInfo.Param.NotCancellable);
        }
    }

    @Override
    public void setSpeakerType(byte speakerType) {
        NpcScriptInfo nsi = getNpcScriptInfo();
        nsi.setSpeakerType(speakerType);
    }

    public void hideNpcByTemplateId(int npcTemplateId, boolean hide) {
        hideNpcByTemplateId(npcTemplateId, hide, hide);
    }

    @Override
    public void hideNpcByTemplateId(int npcTemplateId, boolean hideTemplate, boolean hideNameTag) {
        Field field = chr.getField();
        Life life = field.getLifeByTemplateId(npcTemplateId);
        if (!(life instanceof Npc)) {
            System.out.printf("npc %d is null or not an instance of Npc", npcTemplateId);
            return;
        }
        chr.write(NpcPool.npcViewOrHide(life.getObjectId(), !hideTemplate, !hideNameTag));
    }

    public void hideNpcByObjectId(int npcObjId, boolean hide) {
        hideNpcByObjectId(npcObjId, hide, hide);
    }

    @Override
    public void hideNpcByObjectId(int npcObjId, boolean hideTemplate, boolean hideNameTag) {
        Field field = chr.getField();
        Life life = field.getLifeByObjectID(npcObjId);
        if (!(life instanceof Npc)) {
            System.out.printf("npc %d is null or not an instance of Npc", npcObjId);
            return;
        }
        chr.write(NpcPool.npcViewOrHide(life.getObjectId(), !hideTemplate, !hideNameTag));
    }

    @Override
    public void moveNpcByTemplateId(int npcTemplateId, boolean left, int distance, int speed) {
        Field field = chr.getField();
        Life life = field.getLifeByTemplateId(npcTemplateId);
        if (!(life instanceof Npc)) {
            System.out.printf("npc %d is null or not an instance of Npc", npcTemplateId);
            return;
        }
        chr.write(NpcPool.npcSetForceMove(life.getObjectId(), left, distance, speed));
    }

    @Override
    public void moveNpcByObjectId(int npcObjId, boolean left, int distance, int speed) {
        Field field = chr.getField();
        Life life = field.getLifeByObjectID(npcObjId);
        if (!(life instanceof Npc)) {
            System.out.printf("npc %d is null or not an instance of Npc", npcObjId);
            return;
        }
        chr.write(NpcPool.npcSetForceMove(life.getObjectId(), left, distance, speed));
    }

    @Override
    public void flipNpcByTemplateId(int npcTemplateId, boolean left) {
        Field field = chr.getField();
        Life life = field.getLifeByTemplateId(npcTemplateId);
        if (!(life instanceof Npc)) {
            System.out.printf("npc %d is null or not an instance of Npc", npcTemplateId);
            return;
        }
        chr.write(NpcPool.npcSetForceFlip(life.getObjectId(), left));
    }

    @Override
    public void flipNpcByObjectId(int npcObjId, boolean left) {
        Field field = chr.getField();
        Life life = field.getLifeByObjectID(npcObjId);
        if (!(life instanceof Npc)) {
            System.out.printf("npc %d is null or not an instance of Npc", npcObjId);
            return;
        }
        chr.write(NpcPool.npcSetForceFlip(life.getObjectId(), left));
    }

    public void showNpcSpecialActionByTemplateId(int npcTemplateId, String effectName) {
        showNpcSpecialActionByTemplateId(npcTemplateId, effectName, 0);
    }

    @Override
    public void showNpcSpecialActionByTemplateId(int npcTemplateId, String effectName, int duration) {
        Field field = chr.getField();
        Life life = field.getLifeByTemplateId(npcTemplateId);
        if (!(life instanceof Npc)) {
            System.out.printf("npc %d is null or not an instance of Npc", npcTemplateId);
            return;
        }
        chr.write(NpcPool.npcSetSpecialAction(life.getObjectId(), effectName, duration));
    }

    public void showNpcSpecialActionByObjectId(int npcObjId, String effectName) {
        showNpcSpecialActionByObjectId(npcObjId, effectName, 0);

    }

    @Override
    public void showNpcSpecialActionByObjectId(int npcObjId, String effectName, int duration) {
        Field field = chr.getField();
        Life life = field.getLifeByObjectID(npcObjId);
        if (!(life instanceof Npc)) {
            System.out.printf("npc %d is null or not an instance of Npc", npcObjId);
            return;
        }
        chr.write(NpcPool.npcSetSpecialAction(life.getObjectId(), effectName, duration));
    }

    public void stopNpcSpecialActionByTemplateId(int npcTemplateId) {
        Field field = chr.getField();
        Life life = field.getLifeByTemplateId(npcTemplateId);
        if (!(life instanceof Npc)) {
            System.out.printf("npc %d is null or not an instance of Npc", npcTemplateId);
            return;
        }
        chr.write(NpcPool.npcResetSpecialAction(life.getObjectId()));
    }

    public int getNpcObjectIdByTemplateId(int npcTemplateId) {
        Field field = chr.getField();
        Life life = field.getLifeByTemplateId(npcTemplateId);
        if (!(life instanceof Npc)) {
            System.out.printf("npc %d is null or not an instance of Npc", npcTemplateId);
            return 0;
        }
        return life.getObjectId();
    }

    @Override
    public boolean hasFieldProperty(String key) {
        Field activeField = getField();
        return activeField != null && activeField.hasProperty(key);
    }

    @Override
    public void setFieldProperty(String key, Object value) {
        Field activeField = getField();
        if (activeField != null) {
            activeField.setProperty(key, value);
        }
    }

    @Override
    public Object getFieldProperty(String key) {
        Field activeField = getField();
        return activeField != null ? activeField.getProperty(key) : null;
    }

    // Mob methods
    @Override
    public Mob spawnMob(int id) {
        return spawnMob(id, 0, 0, false);
    }

    @Override
    public Mob spawnMob(int id, boolean respawnable) {
        return spawnMob(id, 0, 0, respawnable);
    }

    @Override
    public Mob spawnMobOnChar(int id) {
        return spawnMob(id, chr.getPosition().getX(), chr.getPosition().getY(), false);
    }

    @Override
    public Mob spawnMobOnChar(int id, boolean respawnable) {
        return spawnMob(id, chr.getPosition().getX(), chr.getPosition().getY(), respawnable);
    }

    @Override
    public Mob spawnMob(int id, int x, int y, boolean respawnable) {
        return spawnMob(id, x, y, respawnable, 0);
    }

    public Mob spawnMob(int id, int x, int y, boolean respawnable, long hp) {
        return chr.getField().spawnMob(id, x, y, respawnable, hp);
    }

    @Override
    public Mob spawnMob(int id, int x, int y, long hp) {
        // Jython 可能將 Python False 綁定到 long 重載（hp=0），此處回退到首領刷怪邏輯。
        if (hp <= 0) {
            return spawnBossMob(id, x, y);
        }
        return spawnMob(id, x, y, false, hp);
    }

    public Mob spawnMob(int id, int x, int y, boolean respawnable, long hp, long exp) {
        return chr.getField().spawnMob(id, x, y, respawnable, hp, exp);
    }

    public Mob spawnMobWithAppearType(int id, int x, int y, int appearType, int option) {
        return chr.getField().spawnMobWithAppearType(id, x, y, appearType, option);
    }

    public void spawnMobsForSubZeroHunt(Char chr) {
        if (chr.isSubZeroHunt()) {
            if (sendAskYesNo("是否要更換爲等級範圍內的其他怪物（低於或高於你 20 級以內）？")) {
                chr.getField().removeMobsByOnlyChar(chr);
                handleMobsForSubZeroHunt(chr);
            }
            return;
        }
        if (!hasItem(4001884, 1)) {
            sendSayOkay("你沒有 #b#z4001884#!#k");
            return;
        }
        boolean bool = handleMobsForSubZeroHunt(chr);
        if (bool) {
            consumeItem(4001884);
            sendSayOkay("感謝惠顧！已在你的狩獵場追加 #b3,000 隻新怪物#k！牠們正在 #b霜凍地圖#k 等著你。\r\n\r\n需要的話，我也可以在狩獵期間#b更換召喚怪物的種類#k，或依你的#b等級範圍#k調整牠們。");
        }
    }

    public boolean handleMobsForSubZeroHunt(Char chr) {
        short charLevel = chr.getLevel();
        int id = MobData.getMobByCharLevel(charLevel);
        if (id == 0) {
            sendSayOkay("未找到你等級範圍內的怪物（低於或高於你 20 級以內）。請重試！");
            return false;
        }
        final LocalDateTime now = LocalDateTime.now();
        int questID = EventConstants.SUB_ZERO_HUNT;
        if (hasQuest(questID)) {
            final String date = getQRValueByKey(questID, "date"); // yy/MM/dd
            final String count = getQRValueByKey(questID, "count");
            if (date != null && count != null) {
                DateTimeFormatter dtf_d = DateTimeFormatter.ofPattern("dd");
                int day = Integer.parseInt(dtf_d.format(now));
                int lastDay = Integer.parseInt(date.substring(6, 8));
                if (lastDay != day) {
                    chr.setSubZeroHunt(true);
                    createQuestWithQRValue(EventConstants.SUB_ZERO_HUNT, "date=" + DateTimeFormatter.ofPattern("yy/MM/dd").format(now) + ";count=0");
                } else if (Integer.parseInt(count) == 3000) {
                    sendSayOkay("你今天已達到每日上限。");
                    return false;
                } else {
                    chr.setSubZeroHunt(true);
                }
            }
        } else {
            chr.setSubZeroHunt(true);
            createQuestWithQRValue(EventConstants.SUB_ZERO_HUNT, "date=" + DateTimeFormatter.ofPattern("yy/MM/dd").format(now) + ";count=0");
        }
        // Up
        for (int i = 1698; i <= 2698; i+=200) {
            chr.getField().spawnMobForOnlyChar(chr, id, i, -81, true);
        }
        // Middle
        for (int i = 1798; i <= 2798; i+=200) {
            chr.getField().spawnMobForOnlyChar(chr, id, i, 164, true);
        }
        // Down
        for (int i = 1524; i <= 2724; i+=200) {
            chr.getField().spawnMobForOnlyChar(chr, id, i, 399, true);
        }
        return true;
    }

    @Override
    public void removeMobByObjId(int id) {
        chr.getField().removeMob(id);
    }

    @Override
    public void removeMobByTemplateId(int id) {
        Field field = chr.getField();
        Life life = field.getLifeByTemplateId(id);
        if (life == null) {
            System.out.printf("Could not find Mob by template id %d.", id);
            return;
        }
        removeMobByObjId(life.getObjectId());
    }

    public boolean isFinishedEscort(int templateID) {
        Field field = chr.getField();
        Life life = field.getLifeByTemplateId(templateID);
        if (!(life instanceof Mob mob)) {
            chr.dispose();
            return false;
        }
        boolean finished = mob.isFinishedEscort();
        if (!finished) {
            chr.dispose();
        }
        return finished;
    }

    @Override
    public void showHP(int templateID) {
        chr.getField().getMobs().stream()
                .filter(m -> m.getTemplateId() == templateID)
                .findFirst()
                .ifPresent(mob -> chr.getField().broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(mob))));
    }

    @Override
    public void showHP() {
        chr.getField().getMobs().stream()
                .filter(m -> m.getHp() > 0)
                .findFirst()
                .ifPresent(mob -> chr.getField().broadcast(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(mob))));
    }


    // Reactor methods
    @Override
    public void removeReactor() {
        Field field = chr.getField();
        int reactorID = getObjectIDByScriptType(ScriptType.Reactor);
        field.removeReactor(reactorID);
    }

    public void removeReactorByID(int reactorTemplateId) {
        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        field.removeReactorByTemplateId(reactorTemplateId);
    }

    public void removeReactorByObjectID(int objectID) {
        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        field.removeReactor(objectID);
    }

    @Override
    public void spawnReactor(int reactorId, int x, int y) {
        Field field = chr.getField();
        Reactor reactor = ReactorData.getReactorByID(reactorId);
        Position position = new Position(x, y);
        reactor.setPosition(position);
        reactor.setHomePosition(position);
        field.addLife(reactor);
        field.broadcast(ReactorPool.reactorEnterField(reactor));
    }

    public void spawnReactorInState(int reactorId, int x, int y, byte state) {
        Field field = chr.getField();
        Reactor reactor = ReactorData.getReactorByID(reactorId);
        reactor.setState(state);
        Position position = new Position(x, y);
        reactor.setPosition(position);
        reactor.setHomePosition(position);
        field.addLife(reactor);
        field.broadcast(ReactorPool.reactorEnterField(reactor));
    }

    @Override
    public boolean hasReactors() {
        Field field = chr.getField();
        return field.getReactors().size() > 0;
    }

    @Override
    public int getReactorQuantity() {
        Field field = chr.getField();
        return field.getReactors().size();
    }

    public int getReactorState(int reactorId) {
        Field field = chr.getField();
        Life life = field.getLifeByTemplateId(reactorId);
        if (life instanceof Reactor reactor) {
            return reactor.getState();
        }
        return -1;
    }

    public void changeReactorState(int reactorId, byte state) {
        changeReactorState(reactorId, state, (short) 0, (byte) 0);
    }

    public void changeReactorState(int reactorId, byte state, short delay, int stateLength) {
        Field field = chr.getField();
        Life life = field.getLifeByTemplateId(reactorId);
        if (life instanceof Reactor reactor) {
            reactor.setState(state);
            field.broadcast(ReactorPool.reactorChangeState(reactor, delay, stateLength));
        }
    }

    public void increaseReactorState(int reactorId, int stateLength) {
        chr.getField().increaseReactorState(reactorId, stateLength);
    }

    public void changeReactorStateByObjectID(int objectID, byte state, short delay, int stateLength) {
        Field field = chr.getField();
        Life life = field.getLifeByObjectID(objectID);
        if (life instanceof Reactor reactor) {
            reactor.setState(state);
            field.broadcast(ReactorPool.reactorChangeState(reactor, delay, stateLength));
        }
    }


    // Party-related methods -------------------------------------------------------------------------------------------

    @Override
    public Party getParty() {
        return chr.getParty();
    }

    @Override
    public int getPartySize() {
        return getParty().getMembers().size();
    }

    @Override
    public void setPartyField() {
        chr.setFieldInstanceType(FieldInstanceType.PARTY);
    }

    @Override
    public void setChannelField() {
        chr.setFieldInstanceType(FieldInstanceType.CHANNEL);
    }

    @Override
    public boolean isPartyLeader() {
        return chr.getParty() != null && chr.getParty().getPartyLeaderID() == chr.getId();
    }

    @Override
    public boolean checkParty() {
        Party party = chr.getParty();
        if (party == null) {
            chat("你目前沒有組隊。");
            return false;
        } else if (!isPartyLeader()) {
            chat("你不是隊長。");
            return false;
        }
        boolean res = true;
        Char leader = chr.getWorld().getCharById(party.getPartyLeaderID());
        if (leader == null) {
            chat("隊長目前不在線上。");
        } else {
            int fieldID = leader.getFieldID();
            for (PartyMember pm : party.getMembers()) {
                if (pm != null) {
                    Char pmChr = chr.getWorld().getCharById(pm.getCharID());
                    res &= pmChr != null && pmChr.isOnline() && pmChr.getField().getId() == fieldID;
                }
            }
        }
        if (!res) {
            chat("請確認所有隊員都在線上，並且與隊長在同一張地圖。");
        }
        return res;
    }

    public boolean checkPartyForPQ() {
        Party party = chr.getParty();
        if (party == null) {
            chat("你目前沒有組隊。");
            return false;
        } else if (!isPartyLeader()) {
            chat("你不是隊長。");
            return false;
        }
        int count = 0;
        Char leader = chr.getWorld().getCharById(party.getPartyLeaderID());
        if (leader == null) {
            chat("隊長目前不在線上。");
        } else {
            int fieldID = leader.getField().getId();
            for (PartyMember pm : party.getMembers()) {
                if (pm != null) {
                    Char pmChr = chr.getWorld().getCharById(pm.getCharID());
                    if (pmChr != null && pmChr.isOnline() && pmChr.getField().getId() == fieldID) {
                        count += 1;
                    }
                }
            }
        }
        if (count < 2) {
            chat("隊伍中至少需要 2 名隊員在線上，並且與隊長在同一張地圖。");
            return false;
        }
        return true;
    }

    private static boolean isPartyEligible(short lowLevel, short highLevel, Char owner, Party party, BossPartyType bpt) {
        for (Char chr : party.getPartyMembersInSameFieldWithChr(owner)) {
            if (chr.getLevel() < lowLevel || chr.getLevel() > highLevel) {
                return false;
            }
            if (bpt.getPreQuest() != 0 && !chr.getScriptManager().hasQuestCompleted(bpt.getPreQuest())) {
                return false;
            }
        }
        return true;
    }

    public List<Char> getPartyMembersInSameField(Char chr) {
        Party party = getParty();
        if (party == null) {
            return new ArrayList<>();
        }
        List<Char> list = new ArrayList<>(party.getPartyMembersInSameField(chr));
        list.add(chr);
        return new ArrayList<>(list);
    }

    public void resetPartyQRValue(int qId) {
        setPartyQRValue(qId, "0");
    }

    public void setPartyQRValue(int qId, String value) {
        for (Char c : chr.getParty().getOnlineChars()) {
            createQuestWithQRValue(c, qId, value, true);
        }
    }

    // Guild/Alliance related methods -------------------------------------------------------------------------------------------

    @Override
    public void showGuildCreateWindow() {
        chr.write(WvsContext.guildResult(GuildResult.msg(GuildType.Request_GuildNameInput)));
    }

    @Override
    public boolean checkAllianceName(String name) {
        World world = chr.getClient().getWorld();
        return world.getAlliance(name) == null;
    }

    public void incrementMaxGuildMembers(int amount) {
        Guild guild = chr.getGuild();
        guild.setMaxMembers(guild.getMaxMembers() + amount);
        guild.broadcast(WvsContext.guildResult(GuildResult.response_MaxMemberIncrease_Success(guild)));
        guild.updateGuildToSQL();
    }

    public void createAlliance(String name, Char other) {
        net.swordie.ms.client.social.Guild.Tms282GuildRuntime.requireLegacyMembershipMutation();
        Alliance alliance = new Alliance();
        alliance.setName(name);
        alliance.addGuild(chr.getGuild());
        alliance.addGuild(other.getGuild());
        GuildMember chrMember = chr.getGuild().getMemberByCharID(chr.getId());
        chrMember.setAllianceRank(1);
        GuildMember otherMember = other.getGuild().getMemberByCharID(other.getId());
        otherMember.setAllianceRank(2);
        chr.getGuild().setAlliance(alliance);
        other.getGuild().setAlliance(alliance);
        alliance.broadcast(WvsContext.allianceResult(AllianceResult.createDone(alliance)));
        chr.deductMoney(5000000);
    }


    // Chat-related methods --------------------------------------------------------------------------------------------

    @Override
    public void chat(String text) {
        chatRed(text);
    }

    @Override
    public void chatRed(String text) {
        chr.chatMessage(SystemNotice, text);
    }

    @Override
    public void chatBlue(String text) {
        chr.chatMessage(Notice2, text);
    }

    public void systemMessage(String message) {
        chr.write(WvsContext.message(MessageType.SYSTEM_MESSAGE, 0, message, (byte) 0));
    }

    @Override
    public void chatScript(String text) {
        chr.chatScriptMessage(text);
    }

    public void showWeatherNotice(String text, WeatherEffNoticeType type) {
        showWeatherNotice(text, type, 7000); // 7 seconds
    }

    @Override
    public void showWeatherNotice(String text, WeatherEffNoticeType type, int duration) {
        chr.write(WvsContext.weatherEffectNotice(type, text, duration));
    }


    // Inventory-related methods ---------------------------------------------------------------------------------------

    @Override
    public void giveMesos(long mesos) {
        chr.addMoney(mesos);
        chr.write(WvsContext.incMoneyMessage((int) mesos));
    }

    @Override
    public void deductMesos(long mesos) {
        chr.deductMoney(mesos);
        chr.write(WvsContext.incMoneyMessage((int) -mesos));
    }

    @Override
    public long getMesos() {
        return chr.getMoney();
    }

    @Override
    public void giveItem(int id) {
        giveItem(id, 1);
    }

    @Override
    public void giveItem(int id, int quantity) {
        chr.addItemToInventory(id, quantity);
    }

    public void giveItem(int id, int quantity, String expiredType, long expires) {
        chr.addItemToInventory(id, quantity, expiredType, expires);
    }

    public void giveSymbol(int itemId) {
        giveSymbol(itemId, 1, 0);
    }

    public void giveSymbol(int itemId, int quantity) {
        giveSymbol(itemId, quantity, 0);
    }

    public void giveSymbol(int itemId, int quantity, int questId) {
        giveSymbol(itemId, quantity, questId, 1);
    }

    public void giveSymbol(int itemId, int quantity, int questId, int level) {
        for (int i = 0; i < quantity; i++) {
            Equip equip = createSymbolEquipment(chr, itemId, level, 1);
            chr.addItemToInventory(equip.getInvType(), equip, false, false);
            chr.getClient().write(WvsContext.inventoryOperation(true, false, Add, (short) equip.getBagIndex(), (byte) -1, 0, equip));
            chr.write(WvsContext.dropPickupMessage(itemId, (short) 1));
        }
        if (questId != 0) {
            for (Char other : chr.getAccount().getCharacters()) {
                if (other.getId() != chr.getId()) {
                    if (other.getRewardSystem() == null) {
                        other.initRewardSystem();
                    }
                    other.sendRewardToChar(itemId, quantity, 0,"角色 " + chr.getName() + " 已完成任務 " + getQuestNameByQuestId(questId) + "。", 30);
                }
            }
        }
    }

    /** Reuses the symbol giver initialization; exp=0 creates a merge material. */
    public static Equip createSymbolEquipment(Char chr, int itemId, int level, int exp) {
        Equip equip = ItemData.getEquipDeepCopyFromID(itemId, false);
        if (equip == null) throw new IllegalArgumentException("Missing symbol template");
        equip.getSymbol().setLevel(level);
        if (exp == 0) {
            equip.setiStr((short) 0); equip.setiDex((short) 0); equip.setiInt((short) 0);
            equip.setiLuk((short) 0); equip.setiMaxHp((short) 0);
            equip.setiMaxMp((short) 0); equip.setiPad((short) 0); equip.setiMad((short) 0);
            equip.setiPDD((short) 0); equip.setiMDD((short) 0);
        }
        equip.getSymbol().setExp(exp);
        if (ItemConstants.isArcaneSymbol(itemId)) {
            equip.getSymbol().setInc((short) Math.min(220, 10 * level + 20));
        } else {
            equip.getSymbol().setInc((short) Math.min(110, 10 * level));
        }
        if (JobConstants.isDemonAvenger(chr.getJob())) {
            if (ItemConstants.isArcaneSymbol(itemId)) {
                equip.setiMaxHp((short) (itemId == 1712000 ? 4200 : 5250 + (level - 1) * 2100));
            } else {
                equip.setiMaxHp((short) (10500 + (level - 1) * 4200));
            }
        } else if (JobConstants.isXenon(chr.getJob())) {
            if (ItemConstants.isArcaneSymbol(itemId)) {
                equip.setiStr((short) (117 + (level - 1) * 48));
                equip.setiDex((short) (117 + (level - 1) * 48));
                equip.setiLuk((short) (117 + (level - 1) * 48));
            } else {
                equip.setiStr((short) (240 + (level - 1) * 96));
                equip.setiDex((short) (240 + (level - 1) * 96));
                equip.setiLuk((short) (240 + (level - 1) * 96));
            }
        } else {
            if (ItemConstants.isArcaneSymbol(itemId)) {
                equip.setBaseStat(chr.calculateMainStatForChar(), 300 + (level - 1) * 100L);
            } else {
                equip.setBaseStat(chr.calculateMainStatForChar(), 500 + (level - 1) * 200L);
            }
        }
        return equip;
    }

    public boolean exchangeSymbolCoupons(int couponId, int symbolId, int quantity) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version() != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            gainItem(symbolId, quantity); loseItem(couponId, quantity); return true;
        }
        return net.swordie.ms.world.tms282.items.SymbolCouponExchange.exchange(chr, couponId, symbolId, quantity);
    }

    public boolean isSymbolCouponExchangeUncertain() {
        return net.swordie.ms.world.tms282.items.SymbolCouponExchange.isUncertain(chr);
    }

    public void giveAndEquip(int id) {
        if (!ItemConstants.isEquip(id)) {
            giveItem(id);
        }
        Item equip = ItemData.getItemDeepCopy(id);
        if (equip == null) {
            System.out.println("Unable to give and equip null item id " + id);
            return;
        }
        // replace the old equip if there was any
        int oldChuc = 0;
        int newChuc = 0;
        Inventory equipInv = chr.getEquipInventory();
        Inventory equippedInv = chr.getEquippedInventory();
        int bodyPart = ItemConstants.getBodyPartFromItem(id, chr.getAvatarData().getAvatarLook().getGender());
        Item oldEquip = equippedInv.getItemBySlot((short) bodyPart);
        if (oldEquip != null) {
            net.swordie.ms.client.character.tms282.Tms282CubeRuntime.requireMovable(chr,oldEquip);
            if (equipInv.getEmptySlots() > 0) {
                oldEquip.setInventoryID(chr.getEquipInventory().getId());
                oldEquip.setInvType(InvType.EQUIP);
                chr.unequip(oldEquip, equipInv.getFirstOpenSlot());
                oldEquip.updateToChar(chr);
                if (oldEquip instanceof Equip) {
                    oldChuc = ((Equip) oldEquip).getChuc();
                }
            } else {
                chr.consumeItem(oldEquip);
                chr.chatMessage("裝備 " + StringData.getItemStringById(id) + " 已被刪除，因為你的裝備欄空間不足。");
            }
        }
        equip.setInventoryID(chr.getEquippedInventory().getId());
        equip.setInvType(InvType.EQUIPPED);
        chr.equip(equip, bodyPart);
        equip.updateToChar(chr);
        Equip.notifyUnionChuc(chr, newChuc - oldChuc);
    }

    @Override
    public boolean hasItem(int id) {
        return hasItem(id, 1);
    }

    @Override
    public boolean isEquipped(int id) {
        return chr.getInventoryByType(InvType.EQUIPPED).getItems().stream().anyMatch(item -> item.getItemId() == id);
    }

    @Override
    public boolean hasItem(int id, int quantity) {
        return getQuantityOfItem(id) >= quantity;
    }

    @Override
    public void consumeItem(int itemID) {
        chr.consumeItem(itemID, 1);
    }

    @Override
    public void consumeItem(int itemID, int amount) {
        chr.consumeItem(itemID, amount);
    }

    @Override
    public void useItem(int id) {
        ItemBuffs.giveItemBuffsFromItemID(chr, chr.getTemporaryStatManager(), id);
    }

    @Override
    public int getQuantityOfItem(int id) {
        int quantity = 0;
        if (ItemConstants.isEquip(id)) {
            List<Item> equips = chr.getInventoryByType(InvType.EQUIP).getItems();
            for (Item equip : equips) {
                if (equip != null) {
                    if (equip.getItemId() == id) {
                        quantity += equip.getQuantity();
                    }
                }
            }
            return quantity;
        } else {
            Item sampleItem = ItemData.getItemDeepCopy(id);
            InvType invType = sampleItem.getInvType();
            List<Item> items = chr.getInventoryByType(invType).getItems();
            for (Item item : items) {
                if (item != null) {
                    if (item.getItemId() == id) {
                        quantity += item.getQuantity();
                    }
                }
            }
            return quantity;
        }
    }

    @Override
    public boolean canHold(int id) {
        return chr.canHold(id);
    }

    @Override
    public boolean canHold(int id, int quantity) {
        return chr.canHold(id, quantity);
    }

    @Override
    public int getEmptyInventorySlots(InvType invType) {
        return chr.getInventoryByType(invType).getEmptySlots();
    }

    public int getEmptyInventorySlots(int invType) {
        return chr.getInventoryByType(InvType.getInvTypeByVal(invType)).getEmptySlots();
    }

    public int getItemQuantity(int itemId) {
        return getQuantityOfItem(itemId);
    }

    public int getFreeSlot(int invType) {
        return getEmptyInventorySlots(invType);
    }

    public void loseItem(int itemId) {
        consumeItem(itemId, 1);
    }

    public void loseItem(int itemId, int quantity) {
        consumeItem(itemId, quantity);
    }

    public void gainItem(int itemId) {
        giveItem(itemId, 1);
    }

    public void gainItem(int itemId, int quantity) {
        giveItem(itemId, quantity);
    }

    public List<Map<String, Object>> select(String sql) {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSet rs = DatabaseManager.executeQuery(sql);
        if (rs == null) {
            if (net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3)
                throw new IllegalStateException("Script database read failed");
            return rows;
        }
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String label = metaData.getColumnLabel(i);
                    if (label == null || label.isBlank()) {
                        label = metaData.getColumnName(i);
                    }
                    row.put(label, rs.getObject(i));
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            if (net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3)
                throw new IllegalStateException("Script database read failed", e);
        } finally {
            try {
                rs.close();
            } catch (SQLException ignored) {
            }
        }
        return rows;
    }

    public boolean update(String sql) {
        boolean saved = DatabaseManager.executeStatement(sql);
        if (!saved && net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3)
            throw new IllegalStateException("Script database write failed");
        return saved;
    }

    public boolean insert(String sql) {
        return update(sql);
    }

    public boolean delete(String sql) {
        return update(sql);
    }

    public boolean customSqlInsert(String sql) {
        return update(sql);
    }

    public boolean customSqlUpdate(String sql) {
        return update(sql);
    }

    // Quest-related methods -------------------------------------------------------------------------------------------
    @Override
    public void completeQuest(int questId) {
        chr.completeQuest(questId, false);
    }

    @Override
    public void completeQuestNoRewards(int questId) {
        chr.completeQuest(questId, true);
    }

    @Override
    public void startQuestNoCheck(int questId) {
        if (QuestConstants.isAccountQuest(questId)) {
            chr.getAccount().addQuest(QuestData.createAccQuestFromId(questId, chr.getAccount().getId()));
        } else {
            chr.addQuest(QuestData.createQuestFromId(questId, chr.getId()));
        }
    }

    @Override
    public void startQuest(int questId) {
        if (chr.canStartQuest(questId)) {
            if (QuestConstants.isAccountQuest(questId)) {
                chr.getAccount().addQuest(QuestData.createAccQuestFromId(questId, chr.getAccount().getId()));
            } else {
                chr.addQuest(QuestData.createQuestFromId(questId, chr.getId()));
            }
        }
    }

    public void setQuestStatus(int questId, int status) {
        if (QuestConstants.isAccountQuest(questId)) {
            AccountQuest q = chr.getAccount().getQuestById(questId);
            if (q != null) {
                q.setStatus(QuestStatus.getValByNum(status));
                chr.write(WvsContext.questRecordMessage(q));
                chr.write(WvsContext.questWorldShareMessage(q));
            } else {
                startQuest(questId);
            }
        } else {
            Quest q = chr.getQuestById(questId);
            if (q != null) {
                q.setStatus(QuestStatus.getValByNum(status));
                chr.write(WvsContext.questRecordMessage(q));
            } else {
                startQuest(questId);
            }
        }
    }

    @Override
    public boolean hasQuest(int questId) {
        return chr.hasQuestInProgress(questId);
    }

    @Override
    public boolean hasQuestCompleted(int questId) {
        return chr.hasQuestCompleted(questId);
    }

    public boolean hasHadQuest(int questId) {
        return hasQuest(questId) || hasQuestCompleted(questId);
    }

    /** Tests one QR property (for example d2=1), including an absent quest/property as false. */
    public boolean hasQuestWithValue(int questId, String expected) {
        if (expected == null) return false;
        int separator = expected.indexOf('=');
        return separator > 0 && expected.substring(separator + 1).equals(getQRValueByKey(questId, expected.substring(0, separator)));
    }

    public void createQuestWithQRValue(int questId, String qrValue, boolean ex) {
        chr.createQuestWithQRValue(questId, qrValue, ex);
    }

    public void createQuestWithQRValue(int questId, String qrValue) {
        chr.createQuestWithQRValue(questId, qrValue);
    }

    public void createQuestWithQRValue(Char chr, int questId, String qrValue, boolean ex) {
        chr.createQuestWithQRValue(chr, questId, qrValue, ex);
    }

    public void deleteQuest(int questId) {
        chr.deleteQuest(questId);
    }

    public String getQRValue(int questId) {
        return chr.getQRValue(questId);
    }

    public void setQRValue(int questId, String qrValue) {
        chr.setQRValue(questId, qrValue);
    }

    public void setQRValue(int questId, String qrValue, boolean ex) {
        chr.setQRValue(questId, qrValue, ex);
    }

    public void addQRValue(int questId, String qrValue) {
        chr.addQRValue(questId, qrValue);
    }

    public void addQRValue(int questId, String qrValue, boolean ex) {
        chr.addQRValue(questId, qrValue, ex);
    }

    public void updateQRValue(int questId, boolean ex) {
        chr.updateQRValue(questId, ex);
    }

    public String getQRValueByKey(int questId, String key) {
        return chr.getQRValueByKey(questId, key);
    }

    public void setQRValueByKey(int questId, String key, String value) {
        chr.setQRValueByKey(questId, key, value);
    }

    public String getQuestNameByQuestId(String questId) {
        int q = Integer.parseInt(questId);
        return QuestData.getQuestInfoById(q).getQuestName();
    }

    public String getQuestNameByQuestId(int questId) {
        return QuestData.getQuestInfoById(questId).getQuestName();
    }

    public String getCurrentDateAsString() {
        return FileTime.currentTime().toYYMMDD();
    }

    // Party Quest-related methods -------------------------------------------------------------------------------------

    public void givePQRewards(Party party) {
        showEffectToField(WzConstants.EFFECT_PQ_CLEAR);
        playSound(WzConstants.EFFECT_PQ_CLEAR, true);
        for (Char player : party.getPartyMembersInSameFieldWithChr(chr)) {
            ScriptManagerImpl sm = player.getScriptManager();
            sm.incWillEXP((short) 6);
            sm.incInsightEXP((short) 3);
            sm.addPartyPoint(10);
            sm.showClearStageExpWindow(getPQExp());
        }
    }

    // For Party
    public boolean checkAttempt(int entryQuest, Party party, int maximumCount) {
        DateTimeFormatter dtf_d = DateTimeFormatter.ofPattern("dd");
        final LocalDateTime now = LocalDateTime.now();
        int day = Integer.parseInt(dtf_d.format(now));
        if (party != null) {
            for (Char chr : chr.getParty().getPartyMembersInSameFieldWithChr(chr)) {
                ScriptManagerImpl sm = chr.getScriptManager();
                if (hasQuest(entryQuest)) {
                    final String date = sm.getQRValueByKey(entryQuest, "date"); // yy/MM/dd
                    if (date != null) {
                        final String count = sm.getQRValueByKey(entryQuest, "count");
                        if (count != null) {
                            int lastDay = Integer.parseInt(date.substring(6, 8));
                            if (lastDay == day) {
                                if (Integer.parseInt(count) >= maximumCount) {
                                    return false;
                                }
                            } else {
                                sm.setQRValueByKey(entryQuest, "count", "0");
                            }
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    // For Party
    public boolean checkAttempt(int entryQuest, Party party) {
        return checkAttempt(entryQuest, party, 5);
    }

    // For Party
    public void addAttempt(int entryQuest, Party party) {
        final LocalDateTime now = LocalDateTime.now();
        for (Char player : party.getOnlineChars()) {
            ScriptManagerImpl sm = player.getScriptManager();
            if (sm.hasQuest(entryQuest) || sm.hasQuestCompleted(entryQuest)) {
                sm.setQRValueByKey(entryQuest, "count", sm.getQRValueByKey(entryQuest, "count").equals("5") ? "1" : String.valueOf(Integer.parseInt(sm.getQRValueByKey(entryQuest, "count")) + 1));
                sm.setQRValueByKey(entryQuest, "date", DateTimeFormatter.ofPattern("yy/MM/dd").format(now));
            } else {
                sm.createQuestWithQRValue(entryQuest, String.format("count=%d;date=%s", 1, DateTimeFormatter.ofPattern("yy/MM/dd").format(now)));
            }
        }
    }

    // For Single Character
    public boolean checkAttempt(int entryQuest, int maximumCount) {
        if (hasQuest(entryQuest) || hasQuestCompleted(entryQuest)) {
            final String date = getQRValueByKey(entryQuest, "date"); // yy/MM/dd
            if (date != null) {
                final String count = getQRValueByKey(entryQuest, "count");
                if (count != null) {
                    DateTimeFormatter dtf_d = DateTimeFormatter.ofPattern("dd");
                    final LocalDateTime now = LocalDateTime.now();
                    int day = Integer.parseInt(dtf_d.format(now));
                    int lastDay = Integer.parseInt(date.substring(6, 8));
                    if (lastDay == day) {
                        return Integer.parseInt(count) < maximumCount;
                    }
                }
            }
        }
        return true;
    }

    // For Single Character
    public void addAttempt(int entryQuest, int maximumCount) {
        final LocalDateTime now = LocalDateTime.now();
        if (hasQuest(entryQuest) || hasQuestCompleted(entryQuest)) {
            setQRValueByKey(entryQuest, "count", getQRValueByKey(entryQuest, "count").equals("" + maximumCount) ? "1" : String.valueOf(Integer.parseInt(getQRValueByKey(entryQuest, "count")) + 1));
            setQRValueByKey(entryQuest, "date", DateTimeFormatter.ofPattern("yy/MM/dd").format(now));
        } else {
            createQuestWithQRValue(entryQuest, String.format("count=%d;date=%s", 1, DateTimeFormatter.ofPattern("yy/MM/dd").format(now)));
        }
    }

    public void addAttemptChuChuPQ(int entryQuest) {
        final LocalDateTime now = LocalDateTime.now();
        ScriptManagerImpl sm = chr.getScriptManager();
        if (sm.hasQuest(entryQuest) || hasQuestCompleted(entryQuest)) {
            sm.setQRValueByKey(entryQuest, "diff", sm.getFieldID() == GameConstants.HUNGRY_MUTO_HARD_STAGE ? "hard" : "normal");
            sm.setQRValueByKey(entryQuest, "count", sm.getQRValueByKey(entryQuest, "count").equals("3") ? "1" : String.valueOf(Integer.parseInt(sm.getQRValueByKey(entryQuest, "count")) + 1));
            sm.setQRValueByKey(entryQuest, "inGame", "1");
            sm.setQRValueByKey(entryQuest, "date", DateTimeFormatter.ofPattern("yy/MM/dd").format(now));
            sm.setQRValueByKey(entryQuest, "score", "0");
            sm.setQRValueByKey(entryQuest, "ptime", "0");
            sm.setQRValueByKey(entryQuest, "clear", "0");
        } else {
            sm.createQuestWithQRValue(entryQuest, String.format("diff=%s;count=%d;ingame=%d;date=%s;score=%d;ptime=%d;clear=%d", sm.getFieldID() == GameConstants.HUNGRY_MUTO_HARD_STAGE ? "hard" : "normal", 1, 1, DateTimeFormatter.ofPattern("yy/MM/dd").format(now), 0, 0, 0));
        }
    }

    public void sendDojoStart(int stage) {
        if (stage > 1 && getFieldID() != 925070100 && chr.getDojoStartTime() > 0) {
            recordDojoClear(stage - 1); // entering floor N means floor N-1 was cleared
        }
        if (chr.getDojoStartTime() == 0 || getFieldID() == 925070100) {
            chr.setDojoStartTime(System.currentTimeMillis());
        }
        chr.getField().broadcast(FieldPacket.clock(ClockPacket.timerInfoEx(900,
                (int) ((System.currentTimeMillis() - chr.getDojoStartTime() - chr.getDojoCoolTime()) / 1000))));
        //int entry = Integer.parseInt(String.valueOf(getQRValue(1213).charAt(4)));
        //createQuestWithQRValue(1213, "try=" + entry + 1);
        playSound("Dojang/start", 100);
        showEffectToField(WzConstants.EFFECT_DOJO_STAGE);
        showEffectToField(WzConstants.EFFECT_DOJO_STAGE_NUMBER + stage);
    }

    // TMS 273 import: dojo weekly ranking / Urus shop / accessory augment bridges ---------------------------------
    private static final int DOJO_FLOOR_PREFIX = 92507;

    /** Records a cleared dojo floor with the running dojo clock into the weekly ranking. */
    private void recordDojoClear(int stage) {
        if (chr == null || stage <= 0 || chr.getDojoStartTime() <= 0) {
            return;
        }
        long elapsed = System.currentTimeMillis() - chr.getDojoStartTime() - chr.getDojoCoolTime();
        net.swordie.ms.world.tms282.dojo.DojoRankService.get().recordClear(chr, stage, (int) Math.max(0, elapsed / 1000));
    }

    /** Called when leaving a floor through dojang_up (via addDojoPoint): records the floor if its boss is dead. */
    private void recordCurrentDojoFloorIfCleared() {
        if (chr == null || chr.getFieldID() / 10000 != DOJO_FLOOR_PREFIX || chr.getInstance() == null) {
            return;
        }
        int stage = (chr.getFieldID() % 10000) / 100;
        if (chr.getInstance().hasProperty("dojoclear" + stage)) {
            recordDojoClear(stage);
        }
    }

    /** Spawns dojo floor {@code stage} (boss + adds from floors.json) and tracks its clear. */
    public boolean spawnDojoFloor(int stage) {
        return chr != null && net.swordie.ms.world.tms282.dojo.DojoFloorService.get().spawnFloor(chr, stage);
    }

    /** Next dojo map after clearing {@code stage}: the next floor, or the exit after the final floor (80). */
    public int getDojoNextMap(int stage) {
        return net.swordie.ms.world.tms282.dojo.DojoFloorService.get().nextMap(stage);
    }

    public int getDojoFinalFloor() {
        return net.swordie.ms.world.tms282.dojo.DojoFloorService.get().finalFloor();
    }

    public java.util.List<net.swordie.ms.world.tms282.dojo.DojoRewardService.Claim> getDojoRewardClaims() {
        return net.swordie.ms.world.tms282.dojo.DojoRewardService.get().claims(chr);
    }

    public net.swordie.ms.world.tms282.dojo.DojoRewardService.Result claimDojoReward(int rewardId) {
        return net.swordie.ms.world.tms282.dojo.DojoRewardService.get().claim(chr, rewardId);
    }

    public String settleDojoWeek() {
        return net.swordie.ms.world.tms282.dojo.DojoRankService.get().settlePreviousWeek();
    }

    /** Sends DOJANG_RANKING_RESULT built from the weekly ranking (layout unverified on the 282 client). */
    public void showDojoRanking(int typeID, int listLimit) {
        try {
            chr.write(net.swordie.ms.world.tms282.dojo.DojoRankingPacket.build(typeID, chr, listLimit));
        } catch (java.sql.SQLException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public net.swordie.ms.world.tms282.urus.UrusRewardService getUrusReward() {
        return net.swordie.ms.world.tms282.urus.UrusRewardService.get();
    }

    public net.swordie.ms.world.tms282.urus.UrusShopService getUrusShop() {
        return net.swordie.ms.world.tms282.urus.UrusShopService.get();
    }

    public net.swordie.ms.world.tms282.augment.AccessoryAugmentService getAccessoryAugment() {
        return net.swordie.ms.world.tms282.augment.AccessoryAugmentService.get();
    }
    // -----------------------------------------------------------------------------------------------------------

    public void addEventPoint(int amount) {
        addPoint(QuestConstants.EVENT_POINT, amount);
        progressMessageFont("你獲得了 " + amount + " 點活動點數");
    }

    public void addDojoPoint(int amount) {
        recordCurrentDojoFloorIfCleared();
        addPoint(QuestConstants.MULUNG_POINT, amount);
        progressMessageFont("你獲得了 " + amount + " 點武陵道場點數");
    }

    public void addPartyPoint(int amount) {
        addPoint(QuestConstants.PARTY_POINT, amount);
        progressMessageFont("你獲得了 " + amount + " 點組隊任務點數");
    }

    public void addOzPoint(int amount) {
        addPoint(QuestConstants.TOWEROFOZ_POINT, amount);
        progressMessageFont("你獲得了 " + amount + " 點 oz 點數");
    }

    public void addPoint(int questID, int amount) {
        if (questID == QuestConstants.UNION_COIN) {
            addUnionCoin(amount);
        } else if (hasQuest(questID)) {
            int point = Integer.parseInt(getQRValue(questID).substring(6));
            point += amount;
            setQRValue(questID, String.format(QuestConstants.POINT_SETTING_FORMAT, point));
            updateQRValue(questID, true);
        } else {
            createQuestWithQRValue(questID, String.format(QuestConstants.POINT_SETTING_FORMAT, amount));
        }
    }

    public void decPoint(int questID, int amount) {
        if (questID == QuestConstants.DONATION_POINT) {
            chr.getUser().deductDonationPoints(amount);
        } else if (questID == QuestConstants.VOTE_POINT) {
            chr.getUser().deductVotePoints(amount);
        } else if (questID == QuestConstants.UNION_COIN) {
            addUnionCoin(-amount);
            return;
        } else if (questID == QuestConstants.STARDUST_POINT) {
            chr.getDailyCoin().decCoin(chr, amount);
        }
        int point = Integer.parseInt(getQRValue(questID).substring(6));
        int newPoint = Math.max(point - amount, 0);
        setQRValue(questID, String.format(QuestConstants.POINT_SETTING_FORMAT, newPoint));
        updateQRValue(questID, true);
    }

    public Instance getInstance() {
        if (chr.getParty() != null) {
            return chr.getParty().getInstance();
        } else {
            if (chr.getInstance() != null) {
                return chr.getInstance();
            }
        }
        return null;
    }

    public void setAchieveRatio(int ratio, boolean isParty) {
        if (getParty() != null && isParty) {
            Instance instance = getParty().getInstance();
            if (instance != null) {
                instance.setAchieveRatio(ratio);
                if (ratio == 0) {
                    getParty().broadcast((FieldPacket.setAchieveRate(getParty().getInstance().getAchieveRatio())));
                }
            }
        } else {
            Instance instance = chr.getInstance();
            if (instance != null) {
                instance.setAchieveRatio(ratio);
                if (ratio == 0) {
                    chr.write(FieldPacket.setAchieveRate(getParty().getInstance().getAchieveRatio()));
                }
            }
        }
    }

    public void setAchieveRatio(int ratio) {
        setAchieveRatio(ratio, true);
    }

    public void startGhostPark() {
        NpcScriptInfo nsi = getNpcScriptInfo();
        nsi.setMessageType(NpcMessageType.GhostParkEnter);
        chr.write(ScriptMessageEncoder.active().scriptMessage(getNpcScriptInfo(), NpcMessageType.GhostParkEnter));
    }

    public void incrementMonsterParkCount() {
        chr.setMonsterParkCount((byte) (chr.getMonsterParkCount() + 1));
    }

    public byte getMonsterParkCount() {
        return chr.getMonsterParkCount();
    }

    public String getDay() {
        return new SimpleDateFormat("EEEE", Locale.ENGLISH).format(System.currentTimeMillis());
    }

    public int getMPExpByMobId(int templateId) {
        return MonsterPark.getExpByMobId(templateId);
    }

    public int getMPReward() {
        return MonsterPark.getRewardByDay();
    }

    public long getPQExp() {
        return getPQExp(chr);
    }

    public long getPQExp(Char chr) {
        return GameConstants.PARTY_QUEST_EXP_FORMULA(chr);
    }

    public boolean rectangleStages(int stage) {
        int correctAnswer = 0;
        if (stage == 2) {
            int[][] answers = {{0, 1, 1, 1}, {1, 0, 1, 1}, {1, 1, 0, 1}, {1, 1, 1, 0}};
            int[] answer = answers[(int) Math.floor(Math.random() * answers.length)];
            int[] playerPlacement = {0, 0, 0, 0};
            Rect[] areas = {new Rect(-971, -132, -948, 42), new Rect(-800, -143, -772, 40),
                    new Rect(-662, -138, -636, 37), new Rect(-522, -142, -493, 33)};
            for (Char pm : getParty().getOnlineChars()) {
                for (int i = 0; i < areas.length; i++) {
                    if (areas[i].hasPositionInside(pm.getPosition())) {
                        playerPlacement[i] += 1;
                    }
                }
            }
            for (int i = 0; i < answer.length; i++) {
                if (playerPlacement[i] == answer[i]) {
                    correctAnswer++;
                }
            }
            if (correctAnswer == 3) {
                return true;
            } else {
                chr.chatScriptMessage("你當前選擇了 " + correctAnswer + " 條正確繩子。");
            }
        } else if (stage == 3) {
            int[][] answers = {{0, 0, 1, 1, 1}, {0, 1, 0, 1, 1}, {0, 1, 1, 0, 1}, {0, 1, 1, 1, 0}, {1, 0, 0, 1, 1},
                    {1, 0, 1, 0, 1}, {1, 0, 1, 1, 0}, {1, 1, 0, 0, 1}, {1, 1, 0, 1, 0}, {1, 1, 1, 0, 0}};
            int[] answer = answers[(int) Math.floor(Math.random() * answers.length)];
            int[] playerPlacement = {0, 0, 0, 0, 0, 0};
            Rect[] areas = {new Rect(608, -185, 737, -125), new Rect(780, -120, 918, -66),
                    new Rect(969, -183, 1096, -122), new Rect(880, -243, 1005, -186),
                    new Rect(698, -243, 826, -18)};
            for (Char pm : getParty().getOnlineChars()) {
                for (int i = 0; i < areas.length; i++) {
                    if (areas[i].hasPositionInside(pm.getPosition())) {
                        playerPlacement[i] += 1;
                    }
                }
            }
            for (int i = 0; i < answer.length; i++) {
                if (playerPlacement[i] == answer[i]) {
                    correctAnswer++;
                }
            }
            if (correctAnswer == 3) {
                return true;
            } else {
                chr.chatScriptMessage("你當前選擇了 " + correctAnswer + " 個正確平臺。");
            }
        }
        return false;
    }

    // Boss-related methods --------------------------------------------------------------------------------------------

    @Override
    public void setDeathCount(int deathCount) {
        chr.setDeathCount(deathCount);
        //chr.write(UserLocal.deathCountInfo(deathCount));
        chr.write(UserLocal.deathCountInfo(chr.getId(), deathCount));
    }

    @Override
    public void setPartyDeathCount(int deathCount) {
        Party party = chr.getParty();
        if (party != null) {
            for (Char pmChr : party.getPartyMembersInSameFieldWithChr(chr)) {
                pmChr.setDeathCount(deathCount);
            }
        }
    }

    public void createObstacleAtom(ObtacleAtomEnum oae, int key, int damage, int velocity, int amount, int proc) {
        createObstacleAtom(oae, key, damage, velocity, 0, amount, proc);
    }

    @Override
    public void createObstacleAtom(ObtacleAtomEnum oae, int key, int damage, int velocity, int angle, int amount, int proc) {
        Field field = chr.getField();
        createObstacleAtom(field, oae, key, damage, velocity, angle, amount, proc);
    }

    public void createObstacleAtom(Field field, ObtacleAtomEnum oae, int key, int damage, int velocity, int angle, int amount, int proc) {
        int xLeft = field.getVrLeft();
        int yTop = field.getVrTop();

        ObtacleInRowInfo obtacleInRowInfo = new ObtacleInRowInfo(4, false, 5000, 0, 0, 0);
        ObtacleRadianInfo obtacleRadianInfo = new ObtacleRadianInfo(4, 0, 0, 0, 0);
        Set<ObtacleAtomInfo> obtacleAtomInfosSet = new HashSet<>();

        for (int i = 0; i < amount; i++) {
            if (Util.succeedProp(proc)) {
                int randomX = new Random().nextInt(field.getWidth()) + xLeft;
                Position position = new Position(randomX, yTop);
                Foothold foothold = field.findFootHoldBelow(position);
                if (foothold != null) {
                    int footholdY = foothold.getYFromX(position.getX());
                    int height = position.getY() - footholdY;
                    height = height < 0 ? -height : height;

                    obtacleAtomInfosSet.add(new ObtacleAtomInfo(oae.getType(), key, position, new Position(), oae.getHitBox(),
                            damage, 0, 0, height, 0, velocity, height, angle));
                }
            }
        }

        field.broadcast(FieldPacket.createObtacle(ObtacleAtomCreateType.NORMAL, obtacleInRowInfo, obtacleRadianInfo, obtacleAtomInfosSet));
    }

    public void stopEvents() {
        Set<ScheduledFuture> events = getEvents();
        events.forEach(st -> st.cancel(true));
        events.clear();
        Field field;
        if (chr != null) {
            field = chr.getField();
        } else {
            field = this.field;
        }
        if (field != null) {
            field.broadcast(FieldPacket.clearObtacle());
            field.broadcast(FieldPacket.destroy());
        }
    }

    private Set<ScheduledFuture> getEvents() {
        return events;
    }

    public void addEvent(ScheduledFuture<?> event) {
        getEvents().add(event);
    }

    public void cancelTimer() {
        for (ScheduledFuture sf : getEvents()) {
            if (sf != null) {
                sf.cancel(true);
            }
        }
    }

    // Character Temporary Stat-related methods ------------------------------------------------------------------------

    @Override
    public void giveCTS(CharacterTemporaryStat cts, int nOption, int rOption, int time) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        o.nOption = nOption;
        o.rOption = rOption;
        o.tOption = time;
        tsm.sendStat(cts, o);
    }

    @Override
    public void removeCTS(CharacterTemporaryStat cts) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        tsm.removeStat(cts);
    }

    @Override
    public void removeBuffBySkill(int skillId) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        tsm.removeStatsBySkill(skillId);
    }

    @Override
    public boolean hasCTS(CharacterTemporaryStat cts) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        return tsm.hasStat(cts);
    }

    @Override
    public int getnOptionByCTS(CharacterTemporaryStat cts) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        return hasCTS(cts) ? tsm.getOption(cts).nOption : 0;
    }

    @Override
    public void rideVehicle(int mountID) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        TemporaryStatBase tsb = tsm.getTSBByTSIndex(TSIndex.RideVehicle);

        tsb.setNOption(mountID);
        tsb.setROption(Kaiser.FINAL_TRANCE);
        tsm.sendStat(RideVehicle, tsb.getOption());
    }

    public void rideVehicleExpire(int skillID, int mountID) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        TemporaryStatBase tsb = tsm.getTSBByTSIndex(TSIndex.RideVehicleExpire);
        tsb.setNOption(mountID);
        tsb.setROption(skillID);
        tsb.setExpireTerm(0);
        tsm.sendStat(RideVehicleExpire, tsb.getOption());
        o.nOption = 1;
        o.rOption = skillID;
        o.tOption = 0;
        tsm.sendStat(NewFlying, o);
    }

    // InGameDirectionEvent methods ------------------------------------------------------------------------------------

    @Override
    public int moveCamera(boolean back, int speed, int x, int y) {
        getNpcScriptInfo().setMessageType(AskIngameDirection);
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.cameraMove(back, speed, new Position(x, y))));
        ScriptInfo si = getScriptInfoByType(getLastActiveScriptType());
        if (si == null) {
            throw new NullPointerException(INTENDED_NPE_MSG);
        }
		Integer response = si.awaitResponseInteger(0);
        if (response == null) {
            throw new NullPointerException(INTENDED_NPE_MSG);
        }
        return (int) response;
    }

    public void moveCamera(int speed, int x, int y) {
        moveCamera(false, speed, x, y);
    }

    public void moveCameraBack(int speed) {
        moveCamera(true, speed, chr.getPosition().getX(), chr.getPosition().getY());
    }

    @Override
    public int zoomCamera(int inZoomDuration, int scale, int x, int y) {
        return zoomCamera(inZoomDuration, scale, 1000, x, y);
    }

    public int zoomCamera(int inZoomDuration, int scale, int timePos, int x, int y) {
        getNpcScriptInfo().setMessageType(NpcMessageType.AskIngameDirection);
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.cameraZoom(inZoomDuration, scale, timePos, new Position(x, y))));
        ScriptInfo si = getScriptInfoByType(getLastActiveScriptType());
        if (si == null) {
            throw new NullPointerException(INTENDED_NPE_MSG);
        }
        Integer response = si.awaitResponseInteger(0);
        if (response == null) {
            throw new NullPointerException(INTENDED_NPE_MSG);
        }
        return (int) response;
    }

    @Override
    public void resetCamera() {
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.cameraOnCharacter(0))); // 0 resets the Camera
    }

    public void setCameraOnNpc(int npcTemplateId) {
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.cameraOnCharacter(npcTemplateId)));
    }

    @Override
    public int sendDelay(int delay) {
        getNpcScriptInfo().setMessageType(NpcMessageType.AskIngameDirection);
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.delay(delay)));
        Object response = null;
        var lastActiveScriptType = getLastActiveScriptType();
        if (isActive(lastActiveScriptType)) {
            response = getScriptInfoByType(lastActiveScriptType).awaitResponseInteger(0);
        }
        if (response == null) {
            throw new NullPointerException(INTENDED_NPE_MSG);
        }
        return (int) response;
    }

    @Override
    public void doEventAndSendDelay(int delay, String methodName, Object... args) {
        invoke(chr.getScriptManager(), methodName, args);
        sendDelay(delay);
    }

    @Override
    public void forcedMove(boolean left, int distance) {
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.forcedMove(left, distance)));
    }

    @Override
    public void forcedFlip(boolean left) {
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.forcedFlip(left)));
    }

    @Override
    public void forcedAction(int type, int duration) {
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.forcedAction(type, duration)));
    }

    @Override
    public void forcedInput(int type) {
        ForcedInputType fit = ForcedInputType.getByVal(type);
        if (fit == null) {
            System.out.printf("Unknown Forced Input Type %d", type);
            return;
        }
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.forcedInput(type)));
    }

    public void patternInputRequest(String pattern, int act, int requestCount, int time) {
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.patternInputRequest(pattern, act, requestCount, time)));
    }

    @Override
    public void hideUser(boolean hide) {
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.vansheeMode(hide)));
    }

    public void showEffect(String path, int duration, int x, int y) {
        showEffect(path, duration, x, y, 0, 0, true, 0);
    }

    @Override
    public void showEffect(String path, int duration, int x, int y, int z, int npcIdForExtend, boolean onUser, int idk2) {
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.effectPlay(path, duration, new Position(x, y), z, npcIdForExtend, onUser, idk2)));
    }

    public void showEffectOnPosition(String path, int duration, int x, int y) {
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.effectPlay(path, duration,
                new Position(x, y), 0, 1, false, 0)));
    }

    public void showBalloonMsgOnNpc(String path, int duration, int x, int y, int templateID) {
        int objectID = getNpcObjectIdByTemplateId(templateID);
        if (objectID == 0) return;
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.effectPlay(path, duration,
                new Position(x, y), 0, objectID, false, 0)));
    }

    public void showBalloonMsgOnNpc(String path, int duration, int templateID) {
        showBalloonMsgOnNpc(path, duration, 0, -100, templateID);
    }

    public void showNpcEffectOnPosition(String path, int x, int y, int templateID) {
        int objectID = getNpcObjectIdByTemplateId(templateID);
        if (objectID == 0) return;
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.effectPlay(path, 0,
                new Position(x, y), 0, objectID, false, 0)));
    }

    public void showBalloonMsg(String path, int duration) {
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.effectPlay(path, duration,
                new Position(0, -100), 0, 0, true, 0)));
    }

    public int sayMonologue(String text, boolean isEnd) {
        getNpcScriptInfo().setMessageType(Monologue);
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.monologue(text, isEnd)));
        Object response = null;
        var lastActiveScriptType = getLastActiveScriptType();
        if (isActive(lastActiveScriptType)) {
            response = getScriptInfoByType(lastActiveScriptType).awaitResponseInteger(0);
        }
        if (response == null) {
            throw new NullPointerException(INTENDED_NPE_MSG);
        }
        return (int) response;
    }

    public void avatarLookSet(int[] equipIDs) {
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.avatarLookSet(equipIDs)));
    }

    public void removeAdditionalEffect() {
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.removeAdditionalEffect()));
    }

    public void faceOff(int faceItemID) {
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.faceOff(faceItemID)));
    }

    public void monologueScroll(String msg, boolean stayModal, short align, int updateSpeedTime, int decTic) {
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.monologueScroll(msg, stayModal, align,
                updateSpeedTime, decTic)));
    }

    public void startRoute(int fieldID) {
        int navigationQRKey = 1951;
        if (!chr.hasQuest(navigationQRKey)) {
            chr.createQuestWithQRValue(navigationQRKey, "questID=0;targetMapID=" + fieldID);
        } else {
            chr.setQRValueByKey(navigationQRKey, "targetMapID", fieldID);
        }
        chr.write(UserLocal.createRoute(fieldID));
    }

    public void startNavigation(int fieldID) {
        int navigationQRKey = 1951;
        if (!chr.hasQuest(navigationQRKey)) {
            chr.createQuestWithQRValue(navigationQRKey, "questID=0;targetMapID=" + fieldID);
        } else {
            chr.setQRValueByKey(navigationQRKey, "targetMapID", fieldID);
        }
        chr.write(WvsContext.startNavigation(fieldID, 0, ""));
    }

    public void startNavigation(int questID, int fieldID) {
        int navigationQRKey = 1951;
        if (!chr.hasQuest(navigationQRKey)) {
            chr.createQuestWithQRValue(navigationQRKey, "questID="+questID+";targetMapID=" + fieldID);
        } else {
            chr.setQRValueByKey(navigationQRKey, "questID", questID);
            chr.setQRValueByKey(navigationQRKey, "targetMapID", fieldID);
        }
        chr.write(WvsContext.startNavigation(fieldID, 0, ""));
    }

    // Clock methods ---------------------------------------------------------------------------------------------------

    public Clock createStopWatch(int seconds) {
        return new Clock(ClockType.StopWatch, chr.getField(), seconds);
    }

    public Clock createClock(int seconds) {
        return new Clock(ClockType.SecondsClock, chr.getField(), seconds);
    }

    public void createClock(int hours, int minutes, int seconds) {
        chr.write(FieldPacket.clock(ClockPacket.hmsClock((byte) hours, (byte) minutes, (byte) seconds)));
        addEvent(chr.getTimer().addEvent(this::removeClock, seconds + minutes * 60L + hours * 3600L, TimeUnit.SECONDS));
    }

    public void createClockForMultiple(int seconds, int... fieldIDs) {
        for (int fieldID : fieldIDs) {
            Field field = chr.getOrCreateFieldByCurrentInstanceType(fieldID);
            new Clock(ClockType.SecondsClock, field, seconds);
        }
    }

    public void removeClock() {
        chr.write(FieldPacket.destroy());
    }

    public Clock createTimerGauge(int seconds) {
        return new Clock(ClockType.TimerGauge, chr.getField(), seconds);
    }


    // Other methods ---------------------------------------------------------------------------------------------------

    public void sendGachaponDlg(GachaponDlgType type) {
        chr.write(chr.getGachaponManager().encode(type));
        chr.dispose();
    }

    public void openAranSkillGuide() {
        chr.write(UserLocal.openSkillGuide());
    }

    public int getActivatedDamageSkin() {
        return chr.getActiveDamageSkin().getItemID();
    }

    public boolean hasDamageSkin(int itemID) {
        return chr.getDamageSkinByItemID(itemID) != null;
    }

    @Override
    public boolean addDamageSkin(int itemID) {
        if (ItemConstants.getDamageSkinIDByItemID(itemID) < 0) { // 282-only coupon: the skin id is in its item data
            int wzSkinId = net.swordie.ms.world.tms282.items.ItemDataEffects.damageSkinId(itemID);
            if (wzSkinId >= 0) {
                return addDamageSkin(itemID, wzSkinId);
            }
        }
        DamageSkinType error = null;
        final List<DamageSkinSaveData> skins = chr.getDamageSkins().stream().filter(d -> d.getDamageSkinID() != 0).toList();
        if (skins.size() >= GameConstants.DAMAGE_SKIN_MAX_SIZE) {
            error = DamageSkinType.Res_Fail_SlotCount;
        } else if (chr.getDamageSkinByItemID(itemID) != null) {
            error = DamageSkinType.Res_Fail_AlreadyExist;
        }
        if (error != null) {
            chr.write(UserLocal.damageSkinSaveResult(DamageSkinType.Req_Reg, error, null));
        } else {
            Quest q = chr.getQuestById(QuestConstants.DAMAGE_SKIN);
            if (q == null) {
                q = new Quest(chr.getId(), QuestConstants.DAMAGE_SKIN, QuestStatus.Started);
                chr.addQuest(q);
            }
            chr.consumeItem(itemID, 1);
            DamageSkinSaveData dssd = DamageSkinSaveData.getByItemID(itemID);
            q.setQrValue(String.valueOf(dssd.getDamageSkinID()));
            dssd.setCharId(chr.getId());
            dssd.setActivateTime(FileTime.currentTime());
            chr.addDamageSkin(dssd);
            chr.setActiveDamageSkin(dssd);
            chr.write(UserLocal.damageSkinSaveResult(DamageSkinType.Req_Reg,
                    DamageSkinType.Res_Success, chr));
            chr.write(UserPacket.setActiveDamageSkin(chr));
            chr.write(WvsContext.questRecordMessage(q));
        }
        return error == null;
    }

    @Override
    public void openUI(UIType uiID) {
        int uiIDValue = uiID.getVal();
        chr.write(FieldPacket.openUI(uiIDValue));
    }

    @Override
    public void openUI(int uiID) {
        chr.write(FieldPacket.openUI(uiID));
    }

    public void openUIWithOption(int uiID, int option) {
        chr.write(FieldPacket.openUIWithOption(uiID, option, new int[0]));
    }

    public void openUIWithOption(UIType uiID, int option) {
        openUIWithOption(uiID, option, new int[0]);
    }

    public void openUIWithOption(UIType uiID, int option, int[] minigameOptions) {
        int uiIDValue = uiID.getVal();
        chr.write(FieldPacket.openUIWithOption(uiIDValue, option, minigameOptions));
    }

    public void openUIWithFocus(int uiID, int option, int unk) {
        chr.write(FieldPacket.openUIWithFocus(uiID, option, unk));
    }

    public void openUIWithFocus(UIType uiID, int option, int unk) {
        int uiIDValue = uiID.getVal();
        chr.write(FieldPacket.openUIWithFocus(uiIDValue, option, unk));
    }

    public void closeUI(UIType uiID) {
        int uiIDValue = uiID.getVal();
        chr.write(FieldPacket.closeUI(uiIDValue));
    }

    public void closeUI(int uiID) {
        chr.write(FieldPacket.closeUI(uiID));
    }

    public void makeDescOnUI(int mapID, int type, String str, String str2, int... args) {
        chr.write(UserLocal.makeDescOnUI(mapID, type, str, str2, args));
    }

    @Override
    public void showClearStageExpWindow(long expGiven) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.showClearStageExpWindow((int) expGiven)));
        giveExpNoAffectedByExpRate(expGiven);
    }

    public void showClearStageExpWindowToParty(long expGiven) {
        for (Char player : getParty().getPartyMembersInSameFieldWithChr(chr)) {
            player.write(FieldPacket.fieldEffect(FieldEffect.showClearStageExpWindow((int) expGiven)));
            player.addExp(expGiven, false);
        }
    }

    public void playSound(String sound) {
        playSound(sound, 100);
    }

    public void playSound(String sound, boolean sendField) {
        playSound(sound, 100, sendField);
    }

    public void playSound(String sound, int vol) {
        playSound(sound, vol, false);
    }

    public void playSound(String sound, int vol, boolean sendField) {
        if (sendField) {
            Field field = chr.getField();
            field.broadcast(FieldPacket.fieldEffect(FieldEffect.playSound(sound, vol)));
        } else {
            chr.write(FieldPacket.fieldEffect(FieldEffect.playSound(sound, vol)));
        }
    }

    public void playSoundToField(String sound, int vol) {

    }

    public void blind(int enable, int x, int color, int time) {
        blind(enable, x, color, 0, 0, time);
    }

    public void blind(int enable, int x, int color, int unk1, int unk2, int time) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.blind(enable, x, color, unk1, unk2, time)));
    }

    /** The existing Blind payload already has a seventh i32; preserve its value instead of dropping it. */
    public void blind(int enable, int x, int color, int unk1, int unk2, int time, int extra) {
        FieldEffect effect = FieldEffect.blind(enable, x, color, unk1, unk2, time);
        effect.setArg7(extra);
        chr.write(FieldPacket.fieldEffect(effect));
    }

    public void OnOffLayer_On(int term, String key, int x, int y, int z, String path, int origin, int unk5, int unk6, int unk7) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.OnOffLayer_On(term, key, x, y, z, path, origin, unk5, unk6, unk7)));
    }

    public void OnOffLayer_Move(int term, String key, int dx, int dy) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.OnOffLayer_Move(term, key, dx, dy)));
    }

    public void OnOffLayer_Off(int term, String key, int unk) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.OnOffLayer_Off(term, key, unk)));
    }

    public void onLayer(int duration, String key, int x, int y, int z, String origin, int org, boolean postRender, int idk, boolean repeat) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.OnOffLayer_On(duration, key, x, y, z, origin, org, postRender, idk, repeat)));
    }

    public void moveLayer(int duration, String key, int x, int y) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.OnOffLayer_Move(duration, key, x, y)));
    }

    public void offLayer(int duration, String key, boolean repeat) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.OnOffLayer_Off(duration, key, repeat)));
    }

    @Override
    public int getRandomIntBelow(int upBound) {
        return new Random().nextInt(upBound);
    }

    public void showEffect(String dir) {
        showEffect(dir, 0);
    }

    public void showEffect(String dir, int delay) {
        showEffect(dir, 4, delay);
    }

    public void showScene(String xmlPath, String sceneName, String sceneNumber) {
        Scene scene = new Scene(chr, xmlPath, sceneName, sceneNumber);
        scene.createScene();
    }

    @Override
    public void showEffect(String dir, int placement, int delay) {
        chr.write(UserPacket.effect(Effect.effectFromWZ(dir, false, delay, placement, 0)));
    }

    public void fieldItemConsumed(long expGain) {
        chr.write(UserPacket.effect(Effect.fieldItemConsumed((int) (expGain > Integer.MAX_VALUE ? Integer.MAX_VALUE : expGain))));
    }

    public void createFieldTextEffect(String msg, int letterDelay, int showTime, int clientPosition, int boxPosX,
                                      int boxPosY, int align, int lineSpace, TextEffectType type, int enterType, int leaveType) {
        chr.write(UserPacket.effect(Effect.createFieldTextEffect(msg, letterDelay, showTime, clientPosition,
                new Position(boxPosX, boxPosY), align, lineSpace, type, enterType, leaveType)));
    }

    public void createFieldTextEffect(String msg, int letterDelay, int showTime, int clientPosition, int boxPosX,
                                      int boxPosY, int align, int lineSpace, int type, int enterType, int leaveType) {
        chr.write(UserPacket.effect(Effect.createFieldTextEffect(msg, letterDelay, showTime, clientPosition,
                new Position(boxPosX, boxPosY), align, lineSpace, TextEffectType.TextNoBackground, enterType, leaveType)));
    }

    public void playPortalSound() {
        chr.write(UserPacket.effect(Effect.playPortalSound()));
    }

    public void playPortalSoundToField() {
        chr.getField().broadcast(UserPacket.effect(Effect.playPortalSound()));
    }

    public void avatarOriented(String effectPath) {
        chr.write(UserPacket.effect(Effect.avatarOriented(effectPath)));
    }

    public void reservedEffect(String effectPath) {
        chr.write(UserPacket.effect(Effect.reservedEffect(effectPath)));

        String[] splitted = effectPath.split("/");
        String sceneName = splitted[splitted.length - 2];
        String sceneNumber = splitted[splitted.length - 1];
        String xmlPath = effectPath.replace("/" + sceneName, "").replace("/" + sceneNumber, "").replace("Effect/", "Effect.wz/");

        int fieldID = new Scene(chr, xmlPath, sceneName, sceneNumber).getTransferField();
        if (fieldID != 0) {
            chr.setTransferField(fieldID);
        }
    }

    public void reservedEffectRepeat(String effectPath, boolean start) {
        chr.write(UserPacket.effect(Effect.reservedEffectRepeat(effectPath, start)));
    }

    public void reservedEffectRepeat(String effectPath) {
        reservedEffectRepeat(effectPath, true);
    }

    public void playExclSoundWithDownBGM(String soundPath, int volume) {
        chr.write(UserPacket.effect(Effect.playExclSoundWithDownBGM(soundPath, volume)));
    }

    public void blindEffect(boolean blind) {
        chr.write(UserPacket.effect(Effect.blindEffect(blind)));
    }

    public void fadeInOut(int fadeIn, int delay, int fadeOut, int alpha) {
        chr.write(UserPacket.effect(Effect.fadeInOut(fadeIn, delay, fadeOut, alpha)));
    }

    public void speechBalloon(boolean normal, int range, int nameHeight, String speech, int time, int origin, int x, int y, int z, int lineSpace, int templateID) {
        chr.write(UserPacket.effect(Effect.speechBalloon(normal, range, nameHeight, speech, time, origin, x, y, z, lineSpace, templateID, chr.getId())));
    }

    public String formatNumber(String number) {
        return Util.formatNumber(number);
    }

    private Object invoke(Object invokeOn, String methodName, Object... args) {
        try {
            List<Class<?>> classList = Arrays.stream(args).map(Object::getClass).collect(Collectors.toList());
            Class<?>[] classes = classList.stream().map(Util::convertBoxedToPrimitiveClass).toArray(Class<?>[]::new);
            Method func;
            try {
                func = getClass().getMethod(methodName, classes);
                return func.invoke(invokeOn, args);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        return null;
    }

    public void invokeForParty(String methodName, Object... args) {
        for (PartyMember pm : chr.getParty().getMembers()) {
            if (pm.getChr() == null) {
                continue;
            }
            ScriptManagerImpl sm = pm.getChr().getScriptManager();
            invoke(sm, methodName, args);
        }
    }

    public void invokeForParty(long delay, String methodName, Object... args) {
        for (PartyMember pm : chr.getParty().getMembers()) {
            if (pm.getChr() == null) {
                continue;
            }
            ScriptManagerImpl sm = pm.getChr().getScriptManager();
            sm.invokeAfterDelay(delay, methodName, args);
        }
    }

    public ScheduledFuture<?> invokeAfterDelay(long delay, String methodName, Object... args) {
        ScheduledFuture<?> sf = chr.getTimer().addEvent(() -> invoke(this, methodName, args), delay);
        addEvent(sf);
        return sf;
    }

    public ScheduledFuture<?> invokeAtFixedRate(long initialDelay, long delayBetweenExecutions,
                                                int executes, String methodName, Object... args) {
        ScheduledFuture<?> sf;
        if (executes == 0) {
            sf = chr.getTimer().addFixedRateEvent(() -> invoke(this, methodName, args),
                    initialDelay,
                    delayBetweenExecutions, false);
        } else {
            sf = chr.getTimer().addFixedRateEvent(() -> invoke(this, methodName, args),
                    initialDelay,
                    delayBetweenExecutions,
                    executes);
        }
        addEvent(sf);
        if (executes == 0) {
            if (chr != null) {
                GlobalTimerManager.addCharTimer(chr.getId(), sf);
            } else if (field != null) {
                GlobalTimerManager.addFieldTimer(field.getSN(), sf);
            }
        }
        return sf;
    }

    public int playVideoByScript(String videoPath) {
        getNpcScriptInfo().setMessageType(PlayMovieClip);
        chr.write(UserLocal.videoByScript(videoPath, true));
        Object response = null;
        var lastActiveScriptType = getLastActiveScriptType();
        if (isActive(lastActiveScriptType)) {
            response = getScriptInfoByType(lastActiveScriptType).awaitResponseInteger(0);
        }
        if (response == null) {
            throw new NullPointerException(INTENDED_NPE_MSG);
        }
        return (int) response;
    }

    public void setFuncKeyByScript(boolean add, int action, int key) {
        chr.write(UserLocal.setFuncKeyByScript(add, action, key));
        chr.getFuncKeyMap().putKeyBinding(chr.getId(), key, add ? (byte) 1 : (byte) 0, action);
    }

    public void addPopUpSay(int npcID, int duration, String message, String effect) {
        chr.write(UserLocal.addPopupSay(npcID, duration, message, effect));
    }

    public void setFieldFloating(int fieldID, int x, int y, int term) {
        chr.write(UserLocal.setFieldFloating(fieldID, x, y, term));
    }

    public void moveParticleEff(String type, int startX, int startY, int endX, int endY, int moveTime, int totalCount, int oneSprayMin, int oneSprayMax) {
        chr.write(UserLocal.moveParticleEff(type, new Position(startX, startY), new Position(endX, endY), moveTime, totalCount, oneSprayMin, oneSprayMax));
    }

    public void levelUntil(int toLevel) {
        short level = chr.getLevel();
        if (level >= toLevel) {
            return;
        }
        while (level < toLevel) {
            addLevel(1);
            level++;
        }
    }

    public void ballonMsg(String message) {
        chr.write(UserLocal.balloonMsg(message, 100, 3, null));
    }

    public void hireTutor(boolean set) {
        chr.hireTutor(set);
    }

    public void tutorAutomatedMsg(int id) {
        tutorAutomatedMsg(id, 10000);
    }

    public void tutorAutomatedMsg(int id, int duration) {
        chr.tutorAutomatedMsg(id, duration);
    }

    public void tutorCustomMsg(String message, int width, int duration) {
        chr.tutorCustomMsg(message, width, duration);
    }

    public boolean hasTutor() {
        return chr.hasTutor();
    }

    public int getMakingSkillLevel(int skillID) {
        return chr.getMakingSkillLevel(skillID);
    }

    public boolean isAbleToLevelUpMakingSkill(int skillID) {
        int neededProficiency = SkillConstants.getNeededProficiency(chr.getMakingSkillLevel(skillID));
        if (neededProficiency <= 0) {
            return false;
        }
        return chr.getMakingSkillProficiency(skillID) >= neededProficiency;
    }

    public void makingSkillLevelUp(int skillID) {
        chr.makingSkillLevelUp(skillID);
    }

    private ScriptMemory getMemory() {
        return memory;
    }

    public void changeFootHold(String footholdName, boolean show) {
        changeFootHold(footholdName, show, 0, 0);
    }

    public void changeFootHold(String footholdName, boolean show, int x, int y) {
        chr.getField().broadcast(FieldPacket.syncDynamicFootHold(footholdName, show, new Position(x, y)));
    }

    public boolean hasMobById(int mobID) {
        return chr.getField().getLifeByTemplateId(mobID) != null;
    }

    public void spawnMobRespawnable(int id, int x, int y, boolean respawnable, long hp, int respawnTime) {
        chr.getField().spawnMobRespawnable(id, x, y, respawnable, hp, respawnTime);
    }

    public void createFallingCatcherOnCharacter(String name) {
        ArrayList<Position> positions = new ArrayList<>();
        positions.add(chr.getPosition());
        chr.getField().broadcast(FieldPacket.createFallingCatcher(name, 1, 1, positions));
    }

    public void getItemsFromTrunkEmployee() {
        chr.getItemsFromEmployeeTrunk();
    }

    public void respawnLotusLaser() {
        Field field = chr.getField();
        Mob mob;
        if (field.getId() == 350060700) {
            mob = (Mob) field.getLifeByTemplateId(8950000);
        } else {
            mob = (Mob) field.getLifeByTemplateId(8950100);
        }
        if (mob != null) {
            MobSkillInfo msi = SkillData.getMobSkillInfoByIdAndLevel(MobSkillID.LaserAttack.getVal(), 1);
            MobSkill mobSkill = new MobSkill();
            mobSkill.setLevel(5); //at this level there are 4 lasers and 100% damr
            mobSkill.setSkillID(MobSkillID.LaserAttack.getVal());
            mobSkill.setFixDamR(msi.getSkillStatIntValue(fixDamR));
            mobSkill.applyEffect(mob);
        }
    }

    public void addStorageSlots(byte amount) {
        if (isNativeInventoryExpansion()) { addStorageSlots((int) amount); return; }
        chr.getAccount().getTrunk().addSlots(amount);
    }

    public void addStorageSlots(int amount) {
        if (isNativeInventoryExpansion()) {
            net.swordie.ms.client.character.tms282.Tms282InventoryExpansion.growStorage(chr, amount);
        } else {
            if (amount < 1 || amount > Byte.MAX_VALUE) throw new IllegalArgumentException("Invalid storage increment");
            addStorageSlots((byte) amount);
        }
    }

    public void addInventorySlotsByInvType(byte amount, byte type) {
        if (isNativeInventoryExpansion()) { addInventorySlotsByInvType((int) amount, (int) type); return; }
        Inventory inventory = chr.getInventoryByType(InvType.getInvTypeByVal(type));
        inventory.addSlots(amount);
        chr.write(WvsContext.expandInventory(type, (byte) inventory.getSlots()));
    }

    public void addInventorySlotsByInvType(int amount, int type) {
        if (isNativeInventoryExpansion()) {
            net.swordie.ms.client.character.tms282.Tms282InventoryExpansion.growInventory(chr, amount, type);
        } else {
            if (amount < 1 || amount > Byte.MAX_VALUE || type < 1 || type > 6) throw new IllegalArgumentException("Invalid inventory increment");
            addInventorySlotsByInvType((byte) amount, (byte) type);
        }
    }

    public boolean isNativeInventoryExpansion() {
        return net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
    }

    public int getStorageSlotsLeftToAdd() {
        if (isNativeInventoryExpansion())
            return net.swordie.ms.client.character.tms282.Tms282InventoryExpansion.storageRemaining(chr);
        return Math.max(0, GameConstants.MAX_INVENTORY_SLOTS - chr.getAccount().getTrunk().getSlotCount());
    }

    /** Free panel operation; retrying a partially completed DB save only fills the remaining capacity. */
    public int expandAllInventorySlots() {
        if (!isNativeInventoryExpansion()) throw new IllegalArgumentException("Native282 operation only");
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            synchronized (chr) {
                int added = 0;
                for (int type = 1; type <= 5; type++)
                    added += net.swordie.ms.client.character.tms282.Tms282InventoryExpansion.growInventory(chr, 128, type);
                return added + net.swordie.ms.client.character.tms282.Tms282InventoryExpansion.growStorage(chr, 128);
            }
        }
    }

    /** Only the known expansion coupons; capacity is durably increased before a coupon can be spent. */
    public boolean useInventoryExpansionCoupon(int itemId) {
        if (!isNativeInventoryExpansion()) return false;
        int type, amount;
        switch (itemId) {
            case 2430768, 2433441 -> { type = 1; amount = 8; }
            case 2430769 -> { type = 2; amount = 8; }
            case 2430770 -> { type = 3; amount = 8; }
            case 2430771 -> { type = 4; amount = 8; }
            case 2432346 -> { type = 0; amount = 8; }
            case 2434257 -> { type = 0; amount = 4; }
            default -> { return false; }
        }
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            synchronized (chr) {
                net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.requireEconomicAccess(chr);
                if (chr.getTradeRoom() != null || chr.getMiniRoom() != null) return false;
                Item coupon = chr.getConsumeInventory().getItems().stream()
                        .filter(i -> i.getItemId() == itemId && i.getQuantity() > 0
                                && i.getDateExpire() != null && !i.getDateExpire().isExpired())
                        .min(java.util.Comparator.comparingInt(Item::getBagIndex)).orElse(null);
                int remaining = type == 0 ? getStorageSlotsLeftToAdd() : getSlotsLeftToAddByInvType((byte) type);
                if (coupon == null || remaining < amount) return false;
                int added = type == 0
                        ? net.swordie.ms.client.character.tms282.Tms282InventoryExpansion.growStorage(chr, amount)
                        : net.swordie.ms.client.character.tms282.Tms282InventoryExpansion.growInventory(chr, amount, type);
                if (added != amount) return false;
                chr.consumeItem(coupon, 1);
                // The free panel can fill all these capacities too. A crash between durable capacity
                // and coupon save may leave a free increment, but cannot spend a coupon without benefit.
                chr.saveToSQL();
                return true;
            }
        }
    }

    public int getSlotsLeftToAddByInvType(byte type) {
        if (isNativeInventoryExpansion())
            return net.swordie.ms.client.character.tms282.Tms282InventoryExpansion.remaining(chr, type);
        return chr.getInventoryByType(getInvTypeByVal(type)).getEmptySlots();
    }

    // only for items with quantity
    public void dropItem(int itemId, int itemQuantity, Mob deadMob) {
        Field field = chr.getField();
        Drop drop = new Drop(field.getNewObjectID());
        drop.setItem(ItemData.getItemDeepCopy(itemId));
        if (ItemConstants.isEquip(itemId)) {
            drop.getItem().setQuantity(itemQuantity);
        }
        field.drop(drop, deadMob.getPosition());
    }

    public void dropItem(int itemId, int startPosX, int startPosY, int endPosX, int endPosY) {
        Field field = chr.getField();
        Drop drop = new Drop(field.getNewObjectID());
        drop.setItem(ItemData.getItemDeepCopy(itemId));
        Position startPos = new Position(startPosX, startPosY);
        Position endPos = new Position(endPosX, endPosY);
        field.drop(drop, startPos, endPos, true);
    }

    public void dropMeso(int mesoAmount, int startPosX, int startPosY, int endPosX, int endPosY) {
        Field field = chr.getField();
        Drop drop = new Drop(field.getNewObjectID(), mesoAmount);
        Position startPos = new Position(startPosX, startPosY);
        Position endPos = new Position(endPosX, endPosY);
        field.drop(drop, startPos, endPos, true);
    }

    public void spawnBalrog(boolean easy) {
        int[] spawns = {
                easy ? BossConstants.BALROG_EASY_BODY : BossConstants.BALROG_HARD_BODY,
                easy ? BossConstants.BALROG_EASY_LARM : BossConstants.BALROG_HARD_LARM,
                easy ? BossConstants.BALROG_EASY_RARM : BossConstants.BALROG_HARD_RARM,
                easy ? BossConstants.BALROG_EASY_DMGSINK : BossConstants.BALROG_HARD_DMGSINK,
        };

        for (int spawn : spawns) {
            long hp = BossHpConstants.getMaxHp(spawn);
            if (hp > 0) {
                spawnMob(spawn, BossConstants.BALROG_SPAWN_X, BossConstants.BALROG_SPAWN_Y, false, hp);
            } else {
                spawnMob(spawn, BossConstants.BALROG_SPAWN_X, BossConstants.BALROG_SPAWN_Y, false);
            }
        }
    }

    /**
     * 首領專用刷怪：避免 Python False 誤匹配 long 重載，並套用 BossHpConstants 標準血量。
     */
    @Override
    public Mob spawnBossMob(int id, int x, int y) {
        long hp = BossHpConstants.resolveMaxHp(id, chr);
        Mob mob;
        if (hp > 0) {
            mob = spawnMob(id, x, y, false, hp);
        } else {
            mob = spawnMob(id, x, y, false);
        }
        if (mob != null) {
            Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
            field.setBossSpawned(true);
            net.swordie.ms.world.tms282.urus.UrusRewardService.get().track(mob); // TMS 273 import: Urus coin reward
            net.swordie.ms.world.tms282.bossrun.BossRunService.track(mob); // boss-features: boss run tracking
        }
        return mob;
    }

    public void resetBossMap(int fieldId) {
        Field map = chr.getOrCreateFieldByCurrentInstanceType(fieldId);

        if (map.getClock() != null) {
            map.getClock().removeClock();
        }
        for (Mob m : map.getMobs()) {
            map.removeMob(m.getObjectId());
        }
        for (Drop d : map.getDrops()) {
            map.removeLife(d);
        }
        for (Char c : map.getChars()) {
            c.setDeathCount(0);
            c.write(UserLocal.deathCountInfo(0));
            c.write(UserLocal.deathCountInfo(c.getId(), 0));
        }
    }

    /**
     * Don't save return map on warp
     *
     * @param id  fieldId
     * @param pid portalId
     */
    public void warpNoReturn(int id, int pid) {
        chr.warp(id, pid, false);
    }

    public boolean zakumAlreadySpawned(int map) {
        field = chr.getClient().getChannelInstance().getFieldIfExists(map);
        return field != null && field.getProperties().containsKey("zakum") && field.getProperties().get("zakum").equals("1");
    }

    /**
     * ObjectID variable gets passed to script in constructor but is not accessible in this class.
     * This function solves that.
     * Doesn't work with map scripts.
     *
     * @return the map object ID of the script owner instance.
     */
    public int getObjectID() {
        return objectID;
    }

    public void setObjectID(int objectID) {
        this.objectID = objectID;
    }

    public int getObjectPositionX() {
        return chr.getField().getLifeByObjectID(getObjectID()).getX();
    }

    public int getObjectPositionY() {
        return chr.getField().getLifeByObjectID(getObjectID()).getY();
    }

    public void setDisableDropsInMap(int fieldId, boolean onoff) {
        Field map = chr.getOrCreateFieldByCurrentInstanceType(fieldId);
        map.setDropsDisabled(onoff);
    }

    public void sendAutoEventClock() {
        InGameEvent ige = InGameEventManager.getInstance().getActiveEvent();

        if (ige == null) {
            return;
        }

        if (ige.isActive() && ige.charInEvent(chr.getId())) {
            ige.sendLobbyClock(chr);
        }
    }

    public boolean isRouletteActive() {
        return InGameEventManager.getInstance().getActiveEvent() instanceof RussianRouletteEvent;
    }

    public boolean isPinkZakumActive() {
        return InGameEventManager.getInstance().getActiveEvent() instanceof PinkZakumEvent;
    }

    public boolean isOlaOlaActive() {
        return InGameEventManager.getInstance().getActiveEvent() instanceof OlaOlaEvent;
    }

    public boolean isPhysicalFitnessActive() {
        return InGameEventManager.getInstance().getActiveEvent() instanceof PhysicalFitnessEvent;
    }

    public boolean isPinkZakumOpen() {
        return isPinkZakumActive() && InGameEventManager.getInstance().getOpenEvent() instanceof PinkZakumEvent;
    }

    public boolean isOlaOlaOpen() {
        return isOlaOlaActive() && InGameEventManager.getInstance().getOpenEvent() instanceof OlaOlaEvent;
    }

    public boolean isPhysicalFitnessOpen() {
        return isPhysicalFitnessActive() && InGameEventManager.getInstance().getOpenEvent() instanceof PhysicalFitnessEvent;
    }

    public int getPreviousFieldID() {
        return chr.getPreviousFieldID();
    }

    public boolean isPinkZakumWinner() {
        PinkZakumEvent pze = ((PinkZakumEvent) InGameEventManager.getInstance().getEvent(InGameEventType.PinkZakumBattle));
        return pze.isWinner(chr) && !pze.getWinnerRewarded(chr);
    }

    public void returnPinkZakum() {
        InGameEvent e = InGameEventManager.getInstance().getActiveEvent();

        int warpMap = e instanceof PinkZakumEvent
                ? PinkZakumEvent.BATTLE_MAP
                : chr.getPreviousFieldID();

        chr.warp(warpMap, 0, false);
    }

    public int getPreviousPortalID() {
        return chr.getPreviousPortalID();
    }

    public boolean canWarpSilentCrusade(int targetFieldId) {
        return chr.getClient().getChannelInstance().tryEnterSilentCrusadePortal(chr, targetFieldId, chr.getClient().getChannelInstance().getChannelId());
    }

    public boolean canWarpAreaBoss(int targetFieldId) {
        return chr.getClient().getChannelInstance().canWarpAreaBoss(chr, targetFieldId, chr.getClient().getChannelInstance().getChannelId());
    }

    public void trySpawnAreaBoss() {
        chr.getClient().getChannelInstance().trySpawnAreaBoss(chr, getFieldID(), chr.getClient().getChannelInstance().getChannelId());
    }

    public void overrideAreaBossTimer(int targetFieldId) {
        chr.getClient().getChannelInstance().overrideAreaBossTimer(targetFieldId, chr.getClient().getChannelInstance().getChannelId());
    }

    public void openNodeWithCustomValue(int itemID, int quantity) {
        if (!hasQuestCompleted(1465)) {
            chr.write(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage("需要完成五轉任務線後才能使用。")));
            chr.dispose();
            return;
        }
        if (!hasItem(itemID, quantity)) {
            return;
        }
        int currentQuantity = chr.getInactiveMatrixCore().size();
        if (currentQuantity + quantity > MatrixConstants.SLOT_MAX) {
            chr.write(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage("你的核心背包已達到數量上限。")));
            chr.dispose();
            return;
        }
        List<MatrixCore> cores = new ArrayList<>();
        quantity = MatrixConstants.SLOT_MAX - currentQuantity;
        for (int i = 0; i < quantity; i++) {
            int coreID = 0, skillID1 = 0, skillID2 = 0, skillID3 = 0;
            List<VCoreData> coreData = new LinkedList<>();
            byte type = 0;
            if (Randomizer.isSuccess(GameConstants.SKILL_CORE_CHANCE)) {
                coreData.addAll(VCore.getJobNodes());
                type = 1;
            } else if (Randomizer.isSuccess(GameConstants.BOOST_CORE_CHANCE)) {
                coreData.addAll(VCore.getBoostNodes());
                type = 2;
            } else {
                coreData.addAll(VCore.getSpecialNodes());
                type = 3;
            }
            VCoreData vCoreData = null;
            if (Randomizer.isSuccess(GameConstants.JOB_CORE_CHANCE) && type != 3) {
                List<VCoreData> jobVCoreData = new ArrayList<>();
                for (VCoreData v : coreData) {
                    if (Short.parseShort(v.getJobs().get(0)) == chr.getJob()) {
                        jobVCoreData.add(v);
                    }
                }
                vCoreData = jobVCoreData.get(Randomizer.nextInt(jobVCoreData.size()));
            } else {
                vCoreData = coreData.get(Randomizer.nextInt(coreData.size()));
            }
            coreID = vCoreData.getCoreID();
            if (type != 3) {
                skillID1 = vCoreData.getConnectSkills().get(0);
            }
            switch (vCoreData.getType()) {
                case VCore.BOOST:
                    short jobID = Short.parseShort(vCoreData.getJobs().get(0));
                    List<Integer> boostSkills = VCore.getBoostSkillByJobID(jobID);
                    boostSkills.remove((Integer) skillID1);
                    skillID2 = boostSkills.get(Randomizer.nextInt(boostSkills.size()));
                    boostSkills.remove((Integer) skillID2);
                    skillID3 = boostSkills.get(Randomizer.nextInt(boostSkills.size()));
                    boostSkills.remove((Integer) skillID3);
                    break;
                case VCore.SKILL:
                    break;
                case VCore.SPECIAL:
                    skillID1 = 0;
                    break;
            }
            MatrixCore core = new MatrixCore(chr.getId(), coreID, skillID1, skillID2, skillID3);
            cores.add(core);
            chr.write(WvsContext.nodeStoneResult(core));
        }
        MatrixCore.saveToSQL(cores);
        for (MatrixCore add : cores) {
            chr.getMatrixCore().add(add);
        }
        chr.write(WvsContext.updateVMatrix(chr, false, 0, 0));
        chr.consumeItem(itemID, quantity);
    }

    public boolean openNodeStonesInBulk(int kind, int itemID, int quantity) {
        if (quantity <= 0) return false;

        int cur = chr.getInactiveMatrixCore().size();
        int free = MatrixConstants.SLOT_MAX - cur;
        if (free <= 0) return false;

        int openCount = Math.min(quantity, free);

        List<MatrixCore> cores = new ArrayList<>(openCount);

        for (int n = 0; n < openCount; n++) {
            int coreID = 0, skillID1 = 0, skillID2 = 0, skillID3 = 0;

            // chọn pool
            List<VCoreData> coreData;
            byte type;
            if (Randomizer.isSuccess(GameConstants.SKILL_CORE_CHANCE)) {
                coreData = VCore.getJobNodes();
                type = 1;
            } else if (Randomizer.isSuccess(GameConstants.BOOST_CORE_CHANCE)) {
                coreData = VCore.getBoostNodes();
                type = 2;
            } else {
                coreData = VCore.getSpecialNodes();
                type = 3;
            }
            if (coreData == null || coreData.isEmpty()) continue;

            // chọn vCoreData (ưu tiên job nếu trúng JOB_CORE_CHANCE và không phải special)
            VCoreData vCoreData;
            if (type != 3 && Randomizer.isSuccess(GameConstants.JOB_CORE_CHANCE)) {
                short chrJob = chr.getJob();
                int pick = -1;
                int seen = 0;

                // reservoir sampling: không tạo jobVCoreData list
                for (int i = 0; i < coreData.size(); i++) {
                    VCoreData v = coreData.get(i);
                    if (v.getJobs() == null || v.getJobs().isEmpty()) continue;
                    short job;
                    try {
                        job = Short.parseShort(v.getJobs().get(0));
                    } catch (Exception e) {
                        continue;
                    }
                    if (job != chrJob) continue;

                    seen++;
                    if (Randomizer.nextInt(seen) == 0) pick = i;
                }

                vCoreData = (pick >= 0) ? coreData.get(pick)
                        : coreData.get(Randomizer.nextInt(coreData.size()));
            } else {
                vCoreData = coreData.get(Randomizer.nextInt(coreData.size()));
            }

            coreID = vCoreData.getCoreID();
            if (type != 3 && vCoreData.getConnectSkills() != null && !vCoreData.getConnectSkills().isEmpty()) {
                skillID1 = vCoreData.getConnectSkills().get(0);
            }

            switch (vCoreData.getType()) {
                case VCore.BOOST -> {
                    short jobID = Short.parseShort(vCoreData.getJobs().get(0));
                    List<Integer> boostSkills = VCore.getBoostSkillByJobID(jobID);
                    if (boostSkills == null || boostSkills.size() < 3) break;

                    int nSize = boostSkills.size();
                    int a, b, c;

                    do { a = boostSkills.get(Randomizer.nextInt(nSize)); } while (a == skillID1);
                    do { b = boostSkills.get(Randomizer.nextInt(nSize)); } while (b == skillID1 || b == a);
                    do { c = boostSkills.get(Randomizer.nextInt(nSize)); } while (c == skillID1 || c == a || c == b);

                    skillID2 = b;
                    skillID3 = c;
                }
                case VCore.SPECIAL -> skillID1 = 0;
                case VCore.SKILL -> { /* keep */ }
            }

            MatrixCore core = new MatrixCore(chr.getId(), coreID, skillID1, skillID2, skillID3);
            cores.add(core);
        }
        if (cores.isEmpty()) {
            return false;
        }
        MatrixCore.saveToSQL(cores);
        for (MatrixCore add : cores) {
            chr.getMatrixCore().add(add);
        }
        chr.write(WvsContext.updateVMatrix(chr, false, 0, 0));
        chr.write(WvsContext.nodeStoneResultInBulk(kind, cores));
        chr.consumeItem(itemID, cores.size());
        return true;
    }

    public boolean openNodeStone(int itemID) {
        if(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.enabled())
            return net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.consumeGemstone(chr,itemID,1);
        int currentQuantity = chr.getInactiveMatrixCore().size();
        if (currentQuantity <= MatrixConstants.SLOT_MAX) {
            int coreID = 0, skillID1 = 0, skillID2 = 0, skillID3 = 0;
            List<VCoreData> coreData = new LinkedList<>();
            byte type = 0;
            if (Randomizer.isSuccess(GameConstants.SKILL_CORE_CHANCE)) {
                coreData.addAll(VCore.getJobNodes());
                type = 1;
            } else if (Randomizer.isSuccess(GameConstants.BOOST_CORE_CHANCE)) {
                coreData.addAll(VCore.getBoostNodes());
                type = 2;
            } else {
                coreData.addAll(VCore.getSpecialNodes());
                type = 3;
            }
            VCoreData vCoreData = null;
            if (Randomizer.isSuccess(GameConstants.JOB_CORE_CHANCE) && type != 3) {
                List<VCoreData> jobVCoreData = new ArrayList<>();
                for (VCoreData v : coreData) {
                    if (Short.parseShort(v.getJobs().get(0)) == chr.getJob()) {
                        jobVCoreData.add(v);
                    }
                }
                vCoreData = jobVCoreData.get(Randomizer.nextInt(jobVCoreData.size()));
            } else {
                vCoreData = coreData.get(Randomizer.nextInt(coreData.size()));
            }
            coreID = vCoreData.getCoreID();
            if (type != 3) {
                skillID1 = vCoreData.getConnectSkills().get(0);
            }
            switch (vCoreData.getType()) {
                case VCore.BOOST:
                    short jobID = Short.parseShort(vCoreData.getJobs().get(0));
                    List<Integer> boostSkills = VCore.getBoostSkillByJobID(jobID);
                    boostSkills.remove((Integer) skillID1);
                    skillID2 = boostSkills.get(Randomizer.nextInt(boostSkills.size()));
                    boostSkills.remove((Integer) skillID2);
                    skillID3 = boostSkills.get(Randomizer.nextInt(boostSkills.size()));
                    boostSkills.remove((Integer) skillID3);
                    break;
                case VCore.SKILL:
                    break;
                case VCore.SPECIAL:
                    skillID1 = 0;
                    break;
            }
            MatrixCore core = new MatrixCore(chr.getId(), coreID, skillID1, skillID2, skillID3);
            chr.addMatrixCore(core);
            core.saveToSQL();
            chr.write(WvsContext.updateVMatrix(chr, false, 0, 0));
            chr.write(WvsContext.nodeStoneResult(core));
            chr.consumeItem(itemID, 1);
            return true;
        }
        return false;
    }

    public boolean openSkillNodeStone(int itemID) {
        int currentQuantity = chr.getInactiveMatrixCore().size();
        if (currentQuantity <= MatrixConstants.SLOT_MAX) {
            int coreID = 0, skillID1 = 0, skillID2 = 0, skillID3 = 0;
            List<VCoreData> coreData = new LinkedList<>(VCore.getJobNodes());
            List<VCoreData> jobVCoreData = new ArrayList<>();
            for (VCoreData v : coreData) {
                if (Short.parseShort(v.getJobs().get(0)) == chr.getJob()) {
                    jobVCoreData.add(v);
                }
            }
            VCoreData vCoreData = jobVCoreData.get(Randomizer.nextInt(jobVCoreData.size()));
            if (vCoreData != null) {
                coreID = vCoreData.getCoreID();
                skillID1 = vCoreData.getConnectSkills().get(0);
                MatrixCore core = new MatrixCore(chr.getId(), coreID, skillID1, skillID2, skillID3);
                chr.addMatrixCore(core);
                core.saveToSQL();
                chr.write(WvsContext.updateVMatrix(chr, false, 0, 0));
                chr.write(WvsContext.nodeStoneResult(core));
                chr.consumeItem(itemID, 1);
                return true;
            }
        }
        return false;
    }

    public boolean openNodeStonesByRandomCoreIDsInBulk(int type, int itemID, int quantity, int[] coreIDs) {
        if (quantity <= 0) return false;
        if (coreIDs == null || coreIDs.length == 0) return false;
        int cur = chr.getInactiveMatrixCore().size();
        int free = MatrixConstants.SLOT_MAX - cur;
        if (free <= 0) return false;
        int openCount = Math.min(quantity, free);
        List<MatrixCore> cores = new ArrayList<>(openCount);
        for (int i = 0; i < openCount; i++) {
            int coreID = coreIDs[Util.getRandom(0, coreIDs.length - 1)];
            VCoreData vCoreData = VCore.getCore(coreID);
            if (vCoreData == null) {
                int guard = 0;
                while (vCoreData == null && guard++ < coreIDs.length * 2) {
                    coreID = coreIDs[Util.getRandom(0, coreIDs.length - 1)];
                    vCoreData = VCore.getCore(coreID);
                }
                if (vCoreData == null) break;
            }
            int skillID1 = 0;
            if (vCoreData.getConnectSkills() != null && !vCoreData.getConnectSkills().isEmpty()) {
                skillID1 = vCoreData.getConnectSkills().get(0);
            }
            MatrixCore core = new MatrixCore(chr.getId(), coreID, skillID1, 0, 0);
            cores.add(core);
        }
        if (cores.isEmpty()) {
            return false;
        }
        MatrixCore.saveToSQL(cores);
        for (MatrixCore add : cores) {
            chr.getMatrixCore().add(add);
        }
        chr.write(WvsContext.updateVMatrix(chr, false, 0, 0));
        chr.write(WvsContext.nodeStoneResultInBulk(type, cores));
        chr.consumeItem(itemID, cores.size());
        return true;
    }

    public boolean openNodeStonesByCoreIDInBulk(int type, int itemID, int quantity, int coreID) {
        if (quantity <= 0) return false;
        int cur = chr.getInactiveMatrixCore().size();
        int free = MatrixConstants.SLOT_MAX - cur;
        if (free <= 0) return false;
        int openCount = Math.min(quantity, free);
        VCoreData vCoreData = VCore.getCore(coreID);
        if (vCoreData == null) return false;
        int skillID1 = 0;
        if (vCoreData.getConnectSkills() != null && !vCoreData.getConnectSkills().isEmpty()) {
            skillID1 = vCoreData.getConnectSkills().get(0);
        }
        List<MatrixCore> cores = new ArrayList<>(openCount);
        for (int i = 0; i < openCount; i++) {
            MatrixCore core = new MatrixCore(chr.getId(), coreID, skillID1, 0, 0);
            cores.add(core);
        }
        MatrixCore.saveToSQL(cores);
        for (MatrixCore add : cores) {
            chr.getMatrixCore().add(add);
        }
        chr.write(WvsContext.updateVMatrix(chr, false, 0, 0));
        chr.write(WvsContext.nodeStoneResultInBulk(type, cores));
        chr.consumeItem(itemID, openCount);
        return true;
    }

    public boolean openNodeStoneByCoreID(int itemID, int coreID) {
        if(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.enabled())
            return net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.consumeGemstone(chr,itemID,1);
        int skillID1 = 0;
        int currentQuantity = chr.getInactiveMatrixCore().size();
        if (currentQuantity <= MatrixConstants.SLOT_MAX) {
            VCoreData vCoreData = VCore.getCore(coreID);
            if (vCoreData != null) {
                skillID1 = vCoreData.getConnectSkills().get(0);
                MatrixCore core = new MatrixCore(chr.getId(), coreID, skillID1, 0, 0);
                chr.addMatrixCore(core);
                core.saveToSQL();
                chr.write(WvsContext.updateVMatrix(chr, false, 0, 0));
                chr.write(WvsContext.nodeStoneResult(core));
                chr.consumeItem(itemID, 1);
                return true;
            }
        }
        return false;
    }

    public void setSpineObjectEffectAlpha(boolean back, String key, int alpha, int delay) {
        chr.write(MapLoadable.setSpineObjectEffectAlpha(back, key, alpha, delay));
    }

    public void setObjectEffectAlpha(String key, int alpha, int delay) {
        setSpineObjectEffectAlpha(false, key, alpha, delay);
        setSpineObjectEffectAlpha(true, key, alpha, delay);
    }

    public void setSpineObjectEffectPlay(boolean back, String key, String name, boolean loop, boolean randomStart) {
        chr.write(MapLoadable.setSpineObjectEffectPlay(back, key, name, loop, randomStart));
    }

    public void setObjectEffectPlay(String key, String name, boolean loop, boolean randomStart) {
        setSpineObjectEffectPlay(false, key, name, loop, randomStart);
        setSpineObjectEffectPlay(true, key, name, loop, randomStart);
    }

    public void setSpineObjectEffectAddPlay(boolean back, String key, String name, boolean loop) {
        chr.write(MapLoadable.setSpineObjectEffectAddPlay(back, key, name, loop));
    }

    public void setObjectEffectAddPlay(String key, String name, boolean loop) {
        setSpineObjectEffectAddPlay(false, key, name, loop);
        setSpineObjectEffectAddPlay(true, key, name, loop);
    }

    public void setSpineObjectEffectClearTracks(boolean back, String key, boolean setupPose) {
        chr.write(MapLoadable.setSpineObjectEffectClearTracks(back, key, setupPose));
    }

    public void setObjectEffectClearTracks(String key, boolean setupPose) {
        setSpineObjectEffectClearTracks(false, key, setupPose);
        setSpineObjectEffectClearTracks(true, key, setupPose);
    }

    public void setSpineObjectEffectPlayrate(boolean back, String key, int scale) {
        chr.write(MapLoadable.setSpineObjectEffectPlayrate(back, key, scale));
    }

    public void setObjectEffectPlayrate(String key, int scale) {
        setSpineObjectEffectPlayrate(false, key, scale);
        setSpineObjectEffectPlayrate(true, key, scale);
    }

    public void setSpineObjectEffectStop(boolean back, String key, boolean setupPose) {
        chr.write(MapLoadable.setSpineObjectEffectStop(back, key, setupPose));
    }

    public void setObjectEffectStop(String key, boolean setupPose) {
        setSpineObjectEffectStop(false, key, setupPose);
        setSpineObjectEffectStop(true, key, setupPose);
    }

    public void cameraSwitchNormal(String targetName, int time) {
        chr.write(UserLocal.cameraSwitchNormal(targetName, time));
    }

    public void cameraSwitchByPosition(int x, int y, int time) {
        chr.write(UserLocal.cameraSwitchByPosition(new Position(x, y), time));
    }

    public void cameraSwitchBack() {
        chr.write(UserLocal.cameraSwitchBack());
    }

    public void cameraSwitchPosByCID(int cid, boolean setCamera, int resetTime, String name) {
        chr.write(UserLocal.cameraSwitchPosByCID(cid, setCamera, resetTime, name));
    }

    public void changeBGM(String sound, int startTime, int unk) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.changeBGM(sound, startTime, unk)));
    }

    public void setPartner(boolean add, int npcID, int skillID, boolean hasScript) {
        chr.write(UserLocal.setPartner(add, npcID, skillID, hasScript));
    }

    public void sendUnityPortalDialog() {
        chr.write(UIContextPacket.unityPortalResult());
    }

    public void setBGMVolume(int bgmVolume, int fadingDuration) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.setBGMVolume(bgmVolume, fadingDuration)));
    }

    public void setBGMVolumeOnly(boolean volumeOnly) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.bgmVolumeOnly(volumeOnly)));
    }

    public void spineScreen(boolean binary, boolean loop, boolean postRender, int endDelay, String path, String animationName, String keyName) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.spineScreen(binary, loop, postRender, endDelay, path, animationName, keyName)));
    }

    public void offSpineScreen(String keyName, int type, String aniName, int alphaTime) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.offSpineScreen(keyName, type, aniName, alphaTime)));
    }

    public void objectStateByString(String name) {
        chr.write(FieldPacket.fieldEffect(FieldEffect.objectStateByString(name)));
    }

    public void setMapTaggedObjectAnimation(String tagName, int type) {
        chr.write(MapLoadable.setMapTaggedObjectAnimation(tagName, type));
    }

    public void setMapTaggedObjectVisible(String tagName, boolean visible, int manual, int delay) {
        chr.write(MapLoadable.setMapTaggedObjectVisible(tagName, visible, manual, delay));
    }

    public void setBackEffect(byte effect, int fieldID, int pageID, int duration) {
        chr.write(MapLoadable.setBackEffect(effect, fieldID, pageID, duration));
    }

    public void openEventNameTag() {
        if (hasItem(2431754, 10)) {
            EventNameTag eventNameTag = chr.getEventNameTag();
            EventNameTagType eventNameTagType;
            while (true) {
                int nCategory = Randomizer.rand(0, 4);
                int nIdx = Randomizer.rand(0, 9);
                eventNameTagType = EventNameTagType.getNameTagTypeByVal(nCategory);
                String sEventNameTag = "";
                if (eventNameTag != null) {
                    sEventNameTag = EventNameTagHandler.getStringNameTagByType(eventNameTag, eventNameTagType);
                    if (!sEventNameTag.equals("")) {
                        StringBuilder sbEventNameTag = new StringBuilder(sEventNameTag);

                        if (!eventNameTag.getsRed().contains("0") && !eventNameTag.getsBlue().contains("0") && !eventNameTag.getsYellow().contains("0") && !eventNameTag.getsGreen().contains("0") && !eventNameTag.getsPurple().contains("0")) {
                            chr.write(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage("名稱標籤欄已滿。")));
                            break;
                        } else {
                            if (sEventNameTag.charAt(nIdx) != '1') {
                                sbEventNameTag.setCharAt(nIdx, '1');
                                eventNameTag = EventNameTagHandler.setStringNameTagByType(eventNameTag, eventNameTagType, sbEventNameTag.toString());
                                chr.setEventNameTags(eventNameTag);
                                chr.write(WvsContext.acquireEventNameTag(nCategory, nIdx));
                                chr.write(WvsContext.updateEventNameTag(chr, eventNameTag.getActiveNameTags()));
                                consumeItem(2431754, 10);
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            sendSayOkay("製作標籤需要 10 個王冠的碎片。");
        }
    }

    public int getZeroWeaponType() {
        return chr.getZeroWeaponType();
    }

    public boolean getSuccessProp(int chance) {
        return Util.succeedProp(chance);
    }

    // Party Boss methods ---------------------------------------------------------------------------------------------------

    public boolean checkPartyBossAttempt(BossPartyType bpt, Party party) {
        if (ServerConfig.DEBUG_MODE) {
            return true;
        }
        if (party != null) {
            if (party.getOnlineChars().size() != party.getMembers().size()) {
                return false;
            }
            // every online member must still have an attempt (was: only the first member was checked)
            for (Char chr : party.getOnlineChars()) {
                if (chr.hasBossPartyAttempt(bpt)) return false;
            }
            return true;
        }
        return false;
    }

    public void addPartyBoss(Party party, BossPartyType bpt) {
        if (party != null) {
            for (Char chr : party.getOnlineChars()) {
                try {
                    chr.addPartyboss(bpt);
                } catch (Exception e) {
                    DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                }
            }
        }
    }

    public boolean isPartyEligible(Char chr, short lowLevel, short highLevel, Party party, BossPartyType bpt) {
        if (party.getPartyMembersInSameFieldWithChr(chr).size() != party.getOnlineChars().size()) {
            return false;
        }
        for (Char pmChr : party.getPartyMembersInSameFieldWithChr(chr)) {
            if (pmChr.getLevel() < lowLevel || pmChr.getLevel() > highLevel) {
                return false;
            }
            if (bpt.getPreQuest() != 0 && !pmChr.getScriptManager().hasQuestCompleted(bpt.getPreQuest())) {
                return false;
            }
        }
        return true;
    }

    public GolluxDifficultyType getGolluxDifficulty() {
        Map<String, Object> golluxMaps = chr.getOrCreateFieldByCurrentInstanceType(BossConstants.GOLLUX_FIRST_MAP).getProperties();
        byte difficulty = 3;
        ArrayList<Integer> golluxMainParts = new ArrayList<>();
        golluxMainParts.add(BossConstants.GOLLUX_ABDOMEN);
        golluxMainParts.add(BossConstants.GOLLUX_RIGHT_SHOULDER);
        golluxMainParts.add(BossConstants.GOLLUX_LEFT_SHOULDER);
        for (Map.Entry<String, Object> entry : golluxMaps.entrySet()) {
            if (golluxMainParts.contains(Integer.valueOf(entry.getKey())) && Integer.parseInt(entry.getValue().toString()) == 2) {
                difficulty--;
            }
        }
        return GolluxDifficultyType.getByVal(difficulty);
    }

    public void translateZeroWeaponStats(Equip oldWeapon, Equip lazuliWeapon, Equip lapisWeapon, boolean isStatTrans) {
        Equip baseWeapon = ItemData.getEquipById(oldWeapon.getItemId());
        Map<EquipBaseStat, Integer> equipBaseStats = new HashMap<>() {{
            put(tuc, (int) lazuliWeapon.getTuc() - oldWeapon.getCuc()); // Slot Available = lazuliWeapon.getTuc() - oldWeapon.getTuc() _ when newWeapon.getTuc() > oldWeapon.getTuc()
            put(cuc, (int) oldWeapon.getCuc()); // Slot Applied
            put(iuc, (int) oldWeapon.getIuc()); //Total Hammer Applied
            put(iStr, (int) oldWeapon.getiStr() - baseWeapon.getiStr());
            put(iDex, (int) oldWeapon.getiDex() - baseWeapon.getiDex());
            put(iInt, (int) oldWeapon.getiInt() - baseWeapon.getiInt());
            put(iLuk, (int) oldWeapon.getiLuk() - baseWeapon.getiLuk());
            put(iMaxHP, (int) oldWeapon.getiMaxHp() - baseWeapon.getiMaxHp());
            put(iPAD, (int) oldWeapon.getiPad() - baseWeapon.getiPad());
            put(iMAD, (int) oldWeapon.getiMad() - baseWeapon.getiMad());
            put(iPDD, (int) oldWeapon.getiPDD() - baseWeapon.getiPDD());
            put(iMDD, (int) oldWeapon.getiMDD() - baseWeapon.getiMDD());
            put(iSpeed, (int) oldWeapon.getiSpeed() - baseWeapon.getiSpeed());
            put(iJump, (int) oldWeapon.getiJump() - baseWeapon.getiJump());
        }};
        short soulOptionID = oldWeapon.getSoulOptionId();
        short soulSocketID = oldWeapon.getSoulSocketId();
        short soulOption = oldWeapon.getSoulOption();
        int soulItemID = oldWeapon.getSoulItemId();
        int setItemID = oldWeapon.getSetItemID();
        short totalStar = oldWeapon.getChuc();
        EquipFlame equipFlame = oldWeapon.getFlameStat();
        java.util.List<Integer> potentials = oldWeapon.getOptions();

        if (isStatTrans) {
            for (Map.Entry<EquipBaseStat, Integer> equipBaseStatInfo : equipBaseStats.entrySet()) {
                if (equipBaseStatInfo.getKey() == tuc) {
                    lazuliWeapon.setTuc(equipBaseStatInfo.getValue().shortValue());
                    lapisWeapon.setTuc(equipBaseStatInfo.getValue().shortValue());
                } else if (equipBaseStatInfo.getKey() == cuc) {
                    lazuliWeapon.setCuc(equipBaseStatInfo.getValue().shortValue());
                    lapisWeapon.setCuc(equipBaseStatInfo.getValue().shortValue());
                } else {
                    lazuliWeapon.addStat(equipBaseStatInfo.getKey(), equipBaseStatInfo.getValue());
                    lapisWeapon.addStat(equipBaseStatInfo.getKey(), equipBaseStatInfo.getValue());
                }
            }
            lazuliWeapon.setSoulOptionId(soulOptionID);
            lazuliWeapon.setSoulSocketId(soulSocketID);
            lazuliWeapon.setSoulOption(soulOption);
            lazuliWeapon.setSoulItemId(soulItemID);
            lazuliWeapon.setSetItemID(setItemID);
            lazuliWeapon.setChuc(totalStar, true);
            lazuliWeapon.setFlameStat(equipFlame);

            lapisWeapon.setSoulOptionId(soulOptionID);
            lapisWeapon.setSoulSocketId(soulSocketID);
            lapisWeapon.setSoulOption(soulOption);
            lapisWeapon.setSoulItemId(soulItemID);
            lapisWeapon.setSetItemID(setItemID);
            lapisWeapon.setChuc(totalStar, true);
            lapisWeapon.setFlameStat(equipFlame);
        }
        lazuliWeapon.setOptions(potentials);
        lapisWeapon.setOptions(potentials);
        lazuliWeapon.setInventoryID(chr.getEquippedInventory().getId());
        lazuliWeapon.setInvType(EQUIPPED);
        lapisWeapon.setInventoryID(chr.getEquippedInventory().getId());
        lapisWeapon.setInvType(EQUIPPED);
        chr.equip(lazuliWeapon, -11);
        chr.equip(lapisWeapon, -10);
        lazuliWeapon.updateToChar(chr);
        lapisWeapon.updateToChar(chr);
    }

    public void upgradeZeroWeapon(int lazuliWeaponID, int lapisWeaponID, boolean isStatTrans) {
        //Lazuli always -11
        //Lapis always -10
        Item lazuliWeapon = ItemData.getItemDeepCopy(lazuliWeaponID);
        Item lapisWeapon = ItemData.getItemDeepCopy(lapisWeaponID);
        if (lazuliWeapon == null || lapisWeapon == null) {
            System.out.println("Unable to give and equip null item id ");
            return;
        }
        Inventory equippedInv = chr.getEquippedInventory();
        Item currentLazuli = equippedInv.getItemBySlot((short) -11);
        Item currentLapis = equippedInv.getItemBySlot((short) -10);
        if (currentLazuli != null && currentLapis != null) {
            chr.getAvatarData().getZeroAvatarLook().getHairEquips().removeIf(ItemConstants::isBigSword);
            chr.getAvatarData().getZeroAvatarLook().getHairEquips().add(lapisWeaponID);
            chr.getAvatarData().getZeroAvatarLook().setWeaponId(lapisWeaponID);
            chr.consumeItem(currentLazuli);
            chr.consumeItem(currentLapis);
        }
        translateZeroWeaponStats((Equip) currentLazuli, (Equip) lazuliWeapon, (Equip) lapisWeapon, isStatTrans);
        chr.dispose();
    }

    public void getUpgradeZeroWeaponInfo(int selectOption) {
        Equip lazuliWeapon = null;
        Equip lapisWeapon = null;
        if (ItemConstants.isLongSword(chr.getEquippedInventory().getItemBySlot(-10).getItemId())) {
            lazuliWeapon = (Equip) chr.getEquippedInventory().getItemBySlot(-10);
            lapisWeapon = (Equip) chr.getEquippedInventory().getItemBySlot(-11);
        } else if (!ItemConstants.isLongSword(chr.getEquippedInventory().getItemBySlot(-10).getItemId())) {
            lazuliWeapon = (Equip) chr.getEquippedInventory().getItemBySlot(-11);
            lapisWeapon = (Equip) chr.getEquippedInventory().getItemBySlot(-10);
        }
        if (lazuliWeapon == null || lapisWeapon == null) {
            chr.write(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage("找不到琉或璃武器。")));
            chr.chatMessage(SystemNotice, "找不到琉或璃武器。");
            chr.dispose();
            return;
        }
        int currentLevel = -1;
        if (lazuliWeapon.getItemId() - 1572000 != lapisWeapon.getItemId() - 1562000) {
            //For Sure
            chr.write(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage("琉與璃武器的等級不一致。")));
            chr.chatMessage(SystemNotice, "琉與璃武器的等級不一致。");
            chr.dispose();
            return;
        } else if (lazuliWeapon.getItemId() - 1572000 == lapisWeapon.getItemId() - 1562000) {
            currentLevel = lazuliWeapon.getItemId() - 1572000;
            if (currentLevel < 0) {
                chr.write(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage("無法取得琉或璃武器的等級。")));
                chr.chatMessage(SystemNotice, "無法取得琉或璃武器的等級。");
                chr.dispose();
                return;
            }
            //Just For sure.
            else if (currentLevel >= 7 && selectOption == 0) {
                chr.write(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage("琉與璃武器無法使用此選項升級。")));
                chr.chatMessage(SystemNotice, "琉與璃武器無法使用此選項升級。");
                chr.dispose();
            } else if (currentLevel == 9) {
                chr.write(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage("琉與璃武器已達到最高等級。")));
                chr.chatMessage(SystemNotice, "琉與璃武器已達到最高等級。");
                chr.dispose();
                return;
            }
        }
        boolean isOpenUI = hasQuestCompleted(QuestConstants.ZERO_WEAPON_WINDOW_QUEST);
        int reqLevel = currentLevel * 10 + 110;
        boolean isEnableButton;
        if ((currentLevel >= 0 && currentLevel < 7) && selectOption == 0) {
            isEnableButton = chr.getLevel() >= reqLevel;
            chr.write(WvsContext.inheritanceInfo(isOpenUI, isEnableButton, currentLevel, currentLevel + 1, reqLevel, lazuliWeapon.getItemId() + 1, lapisWeapon.getItemId() + 1, 0, 0));
        } else if (currentLevel == 7 && selectOption == 1) {
            isEnableButton = chr.getLevel() >= reqLevel && hasItem(4310216, 1);
            chr.write(WvsContext.inheritanceInfo(isOpenUI, isEnableButton, currentLevel, 8, reqLevel, 1572008, 1562008, 4310216, 1));
        } else if (currentLevel == 8 && selectOption == 2) {
            isEnableButton = chr.getLevel() >= reqLevel && hasItem(4310217, 1);
            chr.write(WvsContext.inheritanceInfo(isOpenUI, isEnableButton, currentLevel, 9, reqLevel, 1572009, 1562009, 4310217, 1));
        }
        chr.dispose();
    }

    public void onDamienZoneHandle(Char chr, Mob mob) {
        if (mob == null) {
            if (chr.getMobZoneDebuff() != null) {
                chr.setMobZoneDebuff(null);
            }
            return;
        }
        MobZoneDebuff mobZoneDebuff = chr.getMobZoneDebuff();
        if (mobZoneDebuff == null) {
            chr.setMobZoneDebuff(new MobZoneDebuff(chr));
            return;
        }
        MobZoneInfo mobZoneInfo = mob.getMobZone();
        if (mobZoneInfo == null) {
            return;
        }
        Rect rect = mob.getMobZone().getMobZoneRect().get(mob.getCurZoneDataType());
        if (rect == null) {
            return;
        }
        for (Mob zone : chr.getField().getMobs()) {
            if (zone.getTemplateId() == 8880102) {
                if (zone.getRectAround(rect).hasPositionInside(chr.getPosition())) {
                    int hp = (int) (chr.getMaxHP() * (mobZoneInfo.getIn().getAutoDec() / 10000.0D));
                    chr.damage(hp);
                    if (mobZoneDebuff.getDamage() != mobZoneInfo.getIn().getDamage()) {
                        mobZoneDebuff.setDamage(mobZoneInfo.getIn().getDamage());
                    }
                    if (mobZoneDebuff.getHealRate() != mobZoneInfo.getIn().getHealRate()) {
                        mobZoneDebuff.setHealRate(mobZoneInfo.getIn().getHealRate());
                    }
                } else {
                    if (mobZoneDebuff.getDamage() != mobZoneInfo.getOut().getDamage()) {
                        mobZoneDebuff.setDamage(mobZoneInfo.getOut().getDamage());
                    }
                    if (mobZoneDebuff.getHealRate() != mobZoneInfo.getOut().getHealRate()) {
                        mobZoneDebuff.setHealRate(mobZoneInfo.getOut().getHealRate());
                    }
                }
            }
        }
    }

    public void gainMonsterParkReward(int itemID) {
        if (hasItem(itemID, 1)) {
            setSpeakerID(9000030);
            flipSpeaker();
            List<MonsterPark.MonsterParkReward> rewards = MonsterPark.getRewardListByItemID(itemID);
            if (rewards.isEmpty()) {
                chr.write(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage("發生未知錯誤。")));
                return;
            }
            StringBuilder dialog = new StringBuilder();
            dialog.append("#e可獲得以下獎勵：#n\r\n");
            dialog.append(String.format("#v%d# #z%d# x %d - %d\r\n\r\n", 4310020, 4310020, 15, 30));
            dialog.append("#e另外隨機獲得以下其中一項：#n\r\n");
            for (MonsterPark.MonsterParkReward reward : rewards) {
                if (reward.getItemID() == -1) {
                    dialog.append("楓幣 ");
                } else if (reward.getItemID() == -2) {
                    dialog.append("名聲值 ");
                } else {
                    dialog.append(String.format("#v%d# #z%d# ", reward.getItemID(), reward.getItemID()));
                }
                if (reward.getMinQuantity() == reward.getMaxQuantity()) {
                    dialog.append(String.format("x %d ", reward.getMaxQuantity()));
                } else {
                    dialog.append(String.format("x %d - %d ", reward.getMinQuantity(), reward.getMaxQuantity()));
                }
                if (reward.getDay() != -1) {
                    dialog.append(String.format("（%d 天）", reward.getDay()));
                }
                dialog.append("\r\n");
            }
            if (sendAskYesNo(dialog.toString())) {
                int coinQuantity = Randomizer.rand(15, 30);
                int itemQuantity = 0;
                MonsterPark.MonsterParkReward reward = rewards.get(Randomizer.rand(0, rewards.size() - 1));
                System.out.println(reward.getItemID());
                itemQuantity = Randomizer.rand(reward.getMinQuantity(), reward.getMaxQuantity());
                if (!canHold(4310020, coinQuantity)) {
                    sendSayOkay("請在背包中騰出更多空間。");
                    return;
                }
                if (ItemConstants.isEquip(reward.getItemID())) {
                    if (!canHold(reward.getItemID())) {
                        sendSayOkay("請在背包中騰出更多空間。");
                        return;
                    }
                } else {
                    if (reward.getItemID() != -1 && reward.getItemID() != -2 && getEmptyInventorySlots(ItemData.getItemInfoByID(reward.getItemID()).getInvType()) <= 0) {
                        sendSayOkay("請在背包中騰出更多空間。");
                        return;
                    }
                }
                chr.addItemToInventory(4310020, coinQuantity);
                if (reward.getItemID() == -1) {
                    chr.addMoney(itemQuantity);
                } else if (reward.getItemID() == -2) {
                    chr.addHonorExp(itemQuantity);
                } else {
                    if (reward.getDay() != -1) {
                        chr.addItemToInventory(reward.getItemID(), itemQuantity, "day", reward.getDay());
                    } else {
                        chr.addItemToInventory(reward.getItemID(), itemQuantity);
                    }
                }
                consumeItem(itemID);
            }
        }
    }

    public List<Item> getEquips() {
        List<Item> items = new ArrayList<>();
        items.addAll(chr.getEquippedInventory().getItems());
        items.addAll(chr.getEquipInventory().getItems());
        return items;
    }

    public void checkMVPStatus() {
        setSpeakerID(9010038);
        if (chr == null) return;
        User user = chr.getUser();
        if (user == null) {
            return;
        }
        chr.initMVPDailyGift();
        int xuvang = user.getDonationPoints();
        int currentVipPoints = user.getVipPoints();
        VIPGrade vipGrade = VIPGrade.getValByNum(user.getVipGrade());
        if (vipGrade == null) {
            return;
        }
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime expiredDate = user.getVipExpiredDate() == null ? now : user.getVipExpiredDate().toLocalDateTime();
        boolean canRenew = net.swordie.ms.client.MvpMembership.mayRenew(vipGrade.getVal(), expiredDate, now);
        VIPGrade nexVipGrade = VIPGrade.getValByNum(user.getVipGrade() + 1);
        String icon = "";
        switch (vipGrade) {
            case None -> icon = "";
            case Bronze -> icon = "#fUI/UIWindow4.img/dailyGift/mvpMedal/1#";
            case Silver -> icon = "#fUI/UIWindow4.img/dailyGift/mvpMedal/5#";
            case Gold -> icon = "#fUI/UIWindow4.img/dailyGift/mvpMedal/6#";
            case Diamond -> icon = "#fUI/UIWindow4.img/dailyGift/mvpMedal/7#";
        }
        int selection = sendNext("你當前的 MVP 等級爲 " + icon + " #e" + vipGrade + "#n。\r\n你當前擁有 #e" + Util.getNumberFormat(currentVipPoints) + "#n MVP 點數。\r\n" +
                "#b" +
                "#L0#我想提升 MVP 等級。#l\r\n" +
                "#L1#我想使用金幣購買 MVP 點數！#l\r\n" +
                "#L2#什麼是 MVP？MVP 有哪些福利？#l\r\n" +
                "#k");
        switch (selection) {
            case 0:
                if (canRenew) {
                    if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                        if (currentVipPoints >= 750000) {
                            if (sendAskYesNo("你確定要使用 #e#r750,0000#k#n MVP 點數（-75%）領取你的 MVP #e#r鑽石#k#n 等級獎勵嗎？")) {
                                if (getEmptyInventorySlots(InvType.CONSUME) < 1) {
                                    sendSayOkay("請在消耗欄至少空出 1 格以領取獎勵！");
                                    return;
                                }
                                if (!user.purchaseMvpMembership(750000, VIPGrade.Diamond.getVal(), FileTime.fromDate(now.toLocalDate().atStartOfDay().plusMonths(1)))) {
                                    sendSayOkay("MVP 點數或等級已變動，尚未扣款，請重新開啟選單。"); return;
                                }
                                chr.addItemToInventory(2434727, 1, "month", 1);
                                user.saveToSQL(false);
                                sendSayOkay("恭喜你成功續期 #e#r鑽石#k#n MVP 會員");
                            } else {
                                sendSayOkay("請在消耗欄至少空出 1 格以領取獎勵！");
                            }
                        } else {
                            sendSayOkay("你的 MVP 點數不足，無法升級 MVP。\r\n所需 MVP 點數：#r"
                                    + Util.getNumberFormat(currentVipPoints)
                                    + "#k/#b"
                                    + Util.getNumberFormat(750000)
                                    + "#k"
                            );
                        }
                    } else if (vipGrade.getVal() < VIPGrade.Diamond.getVal()) {
                        int reqVipPoints = 0;
                        switch (nexVipGrade) {
                            case Bronze -> reqVipPoints = 249000;
                            case Silver -> reqVipPoints = 350000;
                            case Gold -> reqVipPoints = 600000;
                            case Diamond -> reqVipPoints = 1000000;
                        }
                        if (currentVipPoints >= reqVipPoints) {
                            if (sendAskYesNo("你確定要使用 #e#r" + Util.getNumberFormat(reqVipPoints) + "#k#n MVP 點數，將 MVP 等級提升至 #e#r" + nexVipGrade + "#k#n 嗎？")) {
                                if (getEmptyInventorySlots(InvType.CONSUME) < 1) {
                                    sendSayOkay("請在消耗欄至少空出 1 格以領取獎勵！");
                                    return;
                                }
                                if (!user.purchaseMvpMembership(reqVipPoints, nexVipGrade.getVal(), FileTime.fromDate(now.toLocalDate().atStartOfDay().plusMonths(1)))) {
                                    sendSayOkay("MVP 點數或等級已變動，尚未扣款，請重新開啟選單。"); return;
                                }
                                chr.addItemToInventory(2434723 + nexVipGrade.getVal(), 1, "month", 1);
                                user.saveToSQL(false);
                                sendSayOkay("恭喜你成功續期 #e#r" + nexVipGrade + "#k#n 等級");
                            } else {
                                sendSayOkay("請在消耗欄至少空出 1 格以領取獎勵！");
                            }
                        } else {
                            sendSayOkay("你的 MVP 點數不足，無法升級 MVP。\r\n升級所需 MVP 點數：#r"
                                    + Util.getNumberFormat(currentVipPoints)
                                    + "#k/#b"
                                    + Util.getNumberFormat(reqVipPoints)
                                    + "#k"
                            );
                        }
                    } else {
                        sendSayOkay("發生未知錯誤。");
                    }
                } else {
                    LocalDateTime max = expiredDate.plusDays(5);
                    sendSayOkay("還未到下一次續期 MVP 的時間。\r\n你可以在 MVP 到期後的 #e#r5 天#k#n內續期。#e最晚續期時間爲 " + max.getYear() + " 年 " + max.getMonthValue() + " 月 " + max.getDayOfMonth() + " 日 23 時 59 分 59 秒。#n");
                }
                break;
            case 1:
                if (xuvang <= 0) { sendSayOkay("目前沒有可兌換的金幣。"); return; }
                int amount = sendAskNumber("你想購買多少 #eMVP 點數#n？（最多：#r" + Util.getNumberFormat(xuvang) + "#k 點）", 1, 1, xuvang);
                if (amount > 0 && amount <= xuvang) {
                    if (sendAskYesNo("你確定要購買 #e" + amount + "#n MVP 點數嗎？")) {
                        if (!user.exchangeDonationForVip(amount)) {
                            sendSayOkay("餘額不足或已變動，兌換未完成，請重新開啟選單。");
                            return;
                        }
                        if (EventConstants.DONATION_POINT_EVENT) {
                            if (amount >= 1000000) {
                                chr.getScriptManager().addEventPoint(100);
                                chr.sendRewardToChar(ItemConstants.VIOLET_CUBE, 20, 0, "金幣購買 1,000,000 MVP 點數活動獎勵。", 30);
                            }
                            if (amount >= 500000) {
                                chr.getScriptManager().addEventPoint(50);
                                chr.sendRewardToChar(ItemConstants.VIOLET_CUBE, 10, 0, "金幣購買 500,000 MVP 點數活動獎勵。", 30);
                            }
                            if (amount >= 100000) {
                                chr.sendRewardToChar(ItemConstants.VIOLET_CUBE, 2, 0, "金幣購買 100,000 MVP 點數活動獎勵。", 30);
                            }
                            chr.sendPacketRewards();
                        }
                        sendSayOkay("你已成功購買 #e" + amount + "#n MVP 點數！\r\n你當前擁有 #e#r" + user.getVipPoints() + "#k#n MVP 點數！");
                    }
                }
                break;
            case 2:
                sendNext("#eMVP 會員服務介紹#n\r\n" +
                        "#e1. #fUI/UIWindow4.img/dailyGift/mvpMedal/1# 青銅；#n " +
                        "#e2. #fUI/UIWindow4.img/dailyGift/mvpMedal/5# 白銀#n \r\n" +
                        "#e3. #fUI/UIWindow4.img/dailyGift/mvpMedal/6# 黃金；#n " +
                        "#e4. #fUI/UIWindow4.img/dailyGift/mvpMedal/7# 鑽石#n\r\n" +
                        "- 每個等級有效期爲一個月，到期後 5 天內可以續期以提升等級。\r\n" +
                        "- 超過續期寬限期後，下次購買會從青銅等級開始。\r\n" +
                        "- 每個等級都會獲得對應獎勵。\r\n" +
                        "- 此外還可以享受 MVP 專屬福利。");
                sendPrev("#eMVP 服務的特殊福利#n\r\n" +
                        "#e- MVP 禮包中包含高價值獎勵。#n\r\n" +
                        "#e- 增加所有首領挑戰次數：#n 僅適用於黃金及以上等級，黃金增加 1 次，鑽石增加 2 次。\r\n" +
                        "#e- 星力裝備強化：#n\r\n" +
                        "+ 強化成功率按青銅、白銀、黃金、鑽石分別提升 2.5% - 5% - 7.5% - 10%。\r\n" +
                        "+ 強化楓幣消耗按青銅、白銀、黃金、鑽石分別降低 5% - 10% - 15% - 20%。\r\n" +
                        "#e- 角色基礎楓幣掉落率按青銅、白銀、黃金、鑽石分別增加 25 - 50 - 75 - 100。\r\n" +
                        "- 拍賣場：按青銅、白銀、黃金、鑽石分別增加欄位至 2 - 4 - 6 - 8。#n");
                break;
        }
    }

    public void upgradeMechanicalHeart() {
        setSpeakerID(9010038);

        User user = chr.getUser();
        if (user == null || chr == null) {
            return;
        }
        int dp = user.getDonationPoints();
        List<Item> currentHearts = new ArrayList<>();
        for (Item item : chr.getEquipInventory().getItems()) {
            if (item.getItemId() == 1672020 || item.getItemId() == 1672027 || item.getItemId() == 1672040) {
                currentHearts.add(item);
            }
        }
        if (!currentHearts.isEmpty()) {
            currentHearts.sort(Comparator.comparingInt(Item::getItemId));
            StringBuilder choose = new StringBuilder("#r#e注意：#n 機器人心臟升級後，原有的卷軸屬性、潛在能力等所有能力值都會消失。#k\r\n#e請選擇要升級的機器人心臟：#n\r\n");
            for (int i = 0; i < currentHearts.size(); i++) {
                int itemID = currentHearts.get(i).getItemId();
                int nextItemID = itemID == 1672020 ? 1672027 : itemID == 1672027 ? 1672040 : 1672069;
                choose.append("#L").append(i).append("# #i").append(itemID).append("# #b#z").append(itemID).append("##k #e 升級為 #n ").append("#i").append(nextItemID).append("# #b#z").append(nextItemID).append("##k").append(".#l\r\n");
            }
            int selectedItem = sendNext(choose.toString());
            int selectedItemID = currentHearts.get(selectedItem).getItemId();
            int nextItemID = selectedItemID == 1672020 ? 1672027 : selectedItemID == 1672027 ? 1672040 : 1672069;
            int cost = (selectedItemID == 1672040 ? 500000 : 300000);
            if (dp >= cost) {
                if (sendAskYesNo("你確定要使用 " + (selectedItemID == 1672040 ? "500,000" : "300,000") + " 金幣將 #b#z" + selectedItemID + "##k 升級爲 #b#z" + nextItemID + "##k 嗎？")) {
                    if (canHold(nextItemID)) {
                        net.swordie.ms.client.character.tms282.Tms282CubeRuntime.requireMovable(chr,currentHearts.get(selectedItem));
                        user.deductDonationPoints(cost);
                        chr.consumeItem(currentHearts.get(selectedItem));
                        giveItem(nextItemID);
                    } else {
                        sendSayOkay("請確認你的裝備欄有足夠的空位。");
                    }
                }
            } else {
                sendSayOkay("你的金幣不足，無法升級機器人心臟。");
            }
        } else {
            sendSayOkay("你沒有可以升級的機器人心臟。");
        }
    }

    public void removeCashItems() {
        setSpeakerID(9010038);

        User user = chr.getUser();
        if (user == null || chr == null) {
            return;
        }

        long money = chr.getMoney();
        List<Item> currentCashItems = new ArrayList<>();
        for (Item item : chr.getEtcInventory().getItems()) {
            if (item.getItemId() == 4033766) {
                currentCashItems.add(item);
            }
        }
        for (Item item : chr.getEquipInventory().getItems()) {
            if (item.isCash()) {
                currentCashItems.add(item);
            }
        }
        if (!currentCashItems.isEmpty()) {
            currentCashItems.sort(Comparator.comparingInt(Item::getBagIndex));
            StringBuilder choose = new StringBuilder("#e請選擇要移除的現金道具：#n\r\n");
            for (int i = 0; i < currentCashItems.size(); i++) {
                choose.append("#L").append(i).append("# #i").append(currentCashItems.get(i).getItemId()).append("# #b#z").append(currentCashItems.get(i).getItemId()).append("##k.#l\r\n");
            }
            int selectItems = sendNext(choose.toString());
            if (money >= 50000) {
                if (sendAskYesNo("確定要移除 #i" + currentCashItems.get(selectItems).getItemId() + "# #b#z" + currentCashItems.get(selectItems).getItemId() + "##k 嗎？")) {
                    chr.deductMoney(50000);
                    chr.consumeItem(currentCashItems.get(selectItems));
                    removeCashItems();
                } else {
                    sendSayOkay("下次見！");
                }
            } else {
                sendPrev("你的楓幣不足，無法移除該道具。");
            }
        } else {
            sendSayOkay("你沒有任何可移除的現金道具。");
        }
    }

    public void handleInfernoWolf(int objectID) {
        final LocalDateTime now = LocalDateTime.now();
        if (chr.getQuestById(QuestConstants.INFERNO_WOLF_MOB_DEAD) != null) {
            int count = Integer.parseInt(getQRValueByKey(QuestConstants.INFERNO_WOLF_MOB_DEAD, "count"));
            final String date = getQRValueByKey(QuestConstants.INFERNO_WOLF_MOB_DEAD, "date"); // yy/MM/dd
            DateTimeFormatter dtf_d = DateTimeFormatter.ofPattern("dd");
            int day = Integer.parseInt(dtf_d.format(now));
            int lastDay = Integer.parseInt(date.substring(6, 8));
            if (lastDay != day) {
                if (count <= 5) {
                    setQuestStatus(QuestConstants.INFERNO_WOLF_MOB_DEAD, 0);
                    setQRValueByKey(QuestConstants.INFERNO_WOLF_MOB_DEAD, "count", "1");
                    setQRValueByKey(QuestConstants.INFERNO_WOLF_MOB_DEAD, "date", DateTimeFormatter.ofPattern("yy/MM/dd").format(now));
                    createQuestWithQRValue(QuestConstants.INFERNO_WOLF_MOB_DEAD_, "mobDead=0");
                    chr.setPreviousFieldID(chr.getFieldID());
                    warp(993000500);
                    chr.getField().removeLife(objectID, false);
                } else {
                    sendSayOkay("烈燄戰狼的洞穴每天最多可進入 5 次。");
                }
            } else if (count < 5) {
                setQuestStatus(QuestConstants.INFERNO_WOLF_MOB_DEAD, 0);
                setQRValueByKey(QuestConstants.INFERNO_WOLF_MOB_DEAD, "count", "" + (count + 1));
                createQuestWithQRValue(QuestConstants.INFERNO_WOLF_MOB_DEAD_, "mobDead=0");
                chr.setPreviousFieldID(chr.getFieldID());
                warp(993000500);
                chr.getField().removeLife(objectID, false);
            } else {
                sendSayOkay("烈燄戰狼的洞穴每天最多可進入 5 次。");
            }
        } else {
            createQuestWithQRValue(QuestConstants.INFERNO_WOLF_MOB_DEAD, "count=1;date=" + DateTimeFormatter.ofPattern("yy/MM/dd").format(now));
            createQuestWithQRValue(QuestConstants.INFERNO_WOLF_MOB_DEAD_, "mobDead=0");
            setQuestStatus(QuestConstants.INFERNO_WOLF_MOB_DEAD, 0);
            chr.setPreviousFieldID(chr.getFieldID());
            warp(993000500);
            chr.getField().removeLife(objectID, false);
        }
    }

    public Integer randomDailyQuest(int questID, int sel) {
        List<Integer> randomQuests = new ArrayList<>();
        if (questID == QuestConstants.VANISHING_JOURNEY_DAILY_QUEST) {
            List<Integer> newDailyQuests = new ArrayList<>(5);
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q1")));
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q2")));
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q3")));
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q4")));
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q5")));
            for (int i = 34130; i <= 34150; i++) {
                if (!newDailyQuests.contains(i) && newDailyQuests.get(sel) != i) {
                    randomQuests.add(i);
                }
            }
        } else if (questID == QuestConstants.CHU_CHU_DAILY_QUEST) {
            List<Integer> newDailyQuests = new ArrayList<>(3);
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q1")));
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q2")));
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q3")));
            for (int i = 39017; i <= 39032; i++) {
                if (!newDailyQuests.contains(i) && newDailyQuests.get(sel) != i) {
                    randomQuests.add(i);
                }
            }
        } else if (questID == QuestConstants.LACHELEIN_DAILY_QUEST) {
            List<Integer> newDailyQuests = new ArrayList<>(3);
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q1")));
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q2")));
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q3")));
            for (int i = 34381; i <= 34393; i++) {
                if (!newDailyQuests.contains(i) && newDailyQuests.get(sel) != i) {
                    randomQuests.add(i);
                }
            }
        } else if (questID == QuestConstants.ARCANA_DAILY_QUEST) {
            List<Integer> newDailyQuests = new ArrayList<>(3);
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q1")));
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q2")));
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q3")));
            for (int i = 39038; i <= 39048; i++) {
                if (!newDailyQuests.contains(i) && newDailyQuests.get(sel) != i) {
                    randomQuests.add(i);
                }
            }
        } else if (questID == QuestConstants.MORASS_DAILY_QUEST) {
            List<Integer> newDailyQuests = new ArrayList<>(3);
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q1")));
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q2")));
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q3")));
            for (int i = 34276; i <= 34293; i++) {
                if (!newDailyQuests.contains(i) && newDailyQuests.get(sel) != i) {
                    randomQuests.add(i);
                }
            }
        } else if (questID == QuestConstants.ESFERA_DAILY_QUEST) {
            List<Integer> newDailyQuests = new ArrayList<>(3);
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q1")));
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q2")));
            newDailyQuests.add(Integer.valueOf(getQRValueByKey(questID, "q3")));
            for (int i = 34780; i <= 34799; i++) {
                if (!newDailyQuests.contains(i) && newDailyQuests.get(sel) != i) {
                    randomQuests.add(i);
                }
            }
        }
        return Util.getRandomFromCollection(randomQuests);
    }

    public void randomDailyQuest(List<Integer> newDailyQuests, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("'min' 必須小於或等於 'max'。");
        }
        int range = max - min + 1;
        if (newDailyQuests.size() >= range) {
            // Đã có tất cả các nhiệm vụ khả thi, không thể thêm mới.
            // Bạn có thể xử lý lỗi hoặc trả về tùy ý.
            return;
        }
        int randomQuest;
        do {
            randomQuest = Util.getRandom(min, max);
        } while (newDailyQuests.contains(randomQuest));

        newDailyQuests.add(randomQuest);
    }

    public void addDailyQuestCount(int questID) {
        int max = questID == QuestConstants.VANISHING_JOURNEY_DAILY_QUEST_COUNT ? 5 : 3;
        AccountQuest q = chr.getAccount().getQuestById(questID);
        if (q == null) {
            q = new AccountQuest(chr.getAccount().getId(), questID, QuestStatus.Started);
            q.setQrValue("count=1");
            chr.getAccount().addCustomQuest(q);
            updateQRValue(questID, true);
        } else {
            q.convertQRValueToProperties();
            int count = Integer.parseInt(q.getProperty("count"));
            if (count < max) {
                count += 1;
            }
            q.setQrValue("count=" + count);
            updateQRValue(questID, true);
        }
    }

    public void openYuGardenDailyQuest() {
        final LocalDateTime now = LocalDateTime.now();
        final String date = getQRValueByKey(62150, "date"); // yy/MM/dd
        if (date == null) {
            List<Integer> newDailyQuests = new ArrayList<>(2);
            for (int i = 0; i < 2; i++) {
                randomDailyQuest(newDailyQuests, 62170, 62179);
            }
            List<Integer> newDailyQuests2 = new ArrayList<>(2);
            for (int i = 0; i < 2; i++) {
                randomDailyQuest(newDailyQuests2, 62182, 62192);
            }
            int lastQuestID = Util.getRandom(62193, 62199);
            String qrValue = "q1=" + newDailyQuests.get(0)
                    + ";q2=" + newDailyQuests.get(1)
                    + ";q3=" + newDailyQuests2.get(0)
                    + ";q4=" + newDailyQuests2.get(1)
                    + ";q5=" + lastQuestID
                    + ";date=" + DateTimeFormatter.ofPattern("yy/MM/dd").format(now);
            createQuestWithQRValue(62150, qrValue);
            modifiedCharacter();
            chr.write(UIContextPacket.openDailyQuestBoard(chr));
            return;
        }
        DateTimeFormatter dtf_d = DateTimeFormatter.ofPattern("dd");
        int day = Integer.parseInt(dtf_d.format(now));
        int lastDay = Integer.parseInt(date.substring(6, 8));
        if ((lastDay != day) || getQRValueByKey(62150, "q1") == null) {
            for (int i = 62170; i <= 62199; i++) {
                chr.removeQuest(i);
            }
            List<Integer> newDailyQuests = new ArrayList<>(2);
            for (int i = 0; i < 2; i++) {
                randomDailyQuest(newDailyQuests, 62170, 62179);
            }
            List<Integer> newDailyQuests2 = new ArrayList<>(2);
            for (int i = 0; i < 2; i++) {
                randomDailyQuest(newDailyQuests2, 62182, 62192);
            }
            int lastQuestID = Util.getRandom(62193, 62199);
            setQRValueByKey(62150, "q1", String.valueOf(newDailyQuests.get(0)));
            setQRValueByKey(62150, "q2", String.valueOf(newDailyQuests.get(1)));
            setQRValueByKey(62150, "q3", String.valueOf(newDailyQuests2.get(0)));
            setQRValueByKey(62150, "q4", String.valueOf(newDailyQuests2.get(1)));
            setQRValueByKey(62150, "q5", String.valueOf(lastQuestID));
            setQRValueByKey(62150, "date", DateTimeFormatter.ofPattern("yy/MM/dd").format(now));
            modifiedCharacter();
        }
        chr.write(UIContextPacket.openDailyQuestBoard(chr));
    }

    public void getServerInformation() {
        StringBuilder result = new StringBuilder("#fs20##e#b伺服器資訊：#k#n\r\n#fs11#");
        Duration duration = Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());
        result.append("#e1. 伺服器運作時間：#n ").append(duration.toHours()).append(" 小時 ").append(duration.toMinutes()).append(" 分 ").append(duration.toSeconds()).append(" 秒\r\n");
        result.append("#e2. 經驗值倍率：#n x").append(ServerConfig.EXP_RATE).append("\r\n");
        result.append("#e3. 楓幣倍率：#n x").append(ServerConfig.MESO_RATE).append("\r\n");
        result.append("#e4. 掉寶倍率：#n x").append(ServerConfig.DROP_RATE).append("\r\n");
        result.append("#e5. 線上玩家總數：#n ").append(Server.get().getClients().size()).append("\r\n");
        Map<Integer, Char> chars = new HashMap<>();
        int i = 0;
        for (Client client : Server.get().getClients()) {
            if (client.getChr() != null) {
                chars.put(i, client.getChr());
                result.append(String.format("#L%d#IP: #e%s#n | 名稱: #e%s#n | Lv. #e%d#n | 職業: #e%s#n | 頻道: #e%d#n | 地圖: #e#m%d##n#l\r\n\r\n", i, (client.getIP() + ":" + client.getPort()), client.getChr().getName(), client.getChr().getLevel(), JobConstants.JobEnum.getJobById(client.getChr().getJob()).name().charAt(0) + JobConstants.JobEnum.getJobById(client.getChr().getJob()).name().substring(1).toLowerCase(), client.getChr().getClient().getChannel(), client.getChr().getFieldID()));
                i++;
            }
        }
        Char player = chars.get(sendNext(result.toString()));
        if (player != null) {
            if (player.getClient().getChannel() == player.getClient().getChannel()) {
                final Field toField = player.getField();
                chr.warp(toField, toField.getPortalByName("sp"), false, true, true);
            } else {
                chr.changeChannelAndWarp(player.getClient().getChannel(), player.getFieldID());
            }
        }
    }

    public void tradeCashItems() {
        flipSpeaker();
        flipDialoguePlayerAsSpeaker();

        TradeRoom tradeRoom = chr.getTradeRoom();
        if (tradeRoom == null) {
            chr.chatMessage("你當前沒有與任何人交易。");
            return;
        }

        List<Item> currentCashItems = new ArrayList<>();
        for (Item item : chr.getEquipInventory().getItems()) {
            if (item.isCash()) {
                currentCashItems.add(item);
            }
        }
        for (Item item : chr.getCashInventory().getItems()) {
            if (item.isCash()) {
                currentCashItems.add(item);
            }
        }
        Char other = tradeRoom.getOtherChar(chr.getId());
        if (other == null) {
            tradeRoom.cancelTrade();
            chr.write(MiniroomPacket.cancelTrade());
            chr.chatMessage("你的交易對象已斷開連接。");
            return;
        }
        if (!currentCashItems.isEmpty()) {
            currentCashItems.sort(Comparator.comparingInt(Item::getItemId));
            StringBuilder choose = new StringBuilder("#e請選擇你想交易的物品：#n\r\n");
            for (int i = 0; i < currentCashItems.size(); i++) {
                choose.append("#L").append(i).append("# #i").append(currentCashItems.get(i).getItemId()).append("# #b#z").append(currentCashItems.get(i).getItemId()).append("# (數量：").append(currentCashItems.get(i).getQuantity()).append(")").append("#k.#l\r\n");
            }
            int selectItems = sendNext(choose.toString());
            Item item = currentCashItems.get(selectItems);
            if (item != null) {
                if (item.getQuantity() > 1) {
                    int amount = sendAskNumber("你想交易多少個 #e#b#z" + item.getItemId() + "#（數量：" + item.getQuantity() + "）#k#n？", 1, 1, item.getQuantity());
                    if (amount <= item.getQuantity()) {
                        RoomHandler.putItem(chr, chr.getInventoryByType(item.getInvType()), tradeRoom, tradeRoom.getTradeSlot(chr), item, (short) amount, true);
                        String msg = String.format("已放入物品 %s x %d。", StringData.getItemStringById(item.getItemId()), amount);
                        chr.write(MiniroomPacket.chat(chr, other, 1, msg));
                        other.write(MiniroomPacket.chat(chr, other,0, msg));
                    } else {
                        chr.chatMessage("發生未知錯誤。");
                    }
                } else {
                    RoomHandler.putItem(chr, chr.getInventoryByType(item.getInvType()), tradeRoom, tradeRoom.getTradeSlot(chr), item, (short) 1, true);
                    String msg = String.format("已放入物品 %s x 1。", StringData.getItemStringById(item.getItemId()));
                    chr.write(MiniroomPacket.chat(chr, other, 1, msg));
                    other.write(MiniroomPacket.chat(chr, other, 0, msg));
                }
            }
        }
    }

    public int getUnionCoin() {
        return chr.getUnion().getUnionCoin();
    }

    public void addUnionCoin(int amount) {
        Union union = chr.getUnion();
        union.addUnionCoin(amount);
        chr.write(UnionPacket.unionCoin(union.getUnionCoin()));
        chr.setQRValueByKey(QuestConstants.UNION_POINT, "point", union.getUnionCoin());
    }

    public void updateAvailableUnionCoin(int amount) {
        chr.createQuestWithQRValue(QuestConstants.UNION_COIN, "lastTime=" + FileTime.currentTime().toYYMMDDHHMMSS() + ";coin=" + amount);
    }

    public int getUnionRank() {
        return chr.getUnion().getUnionRank();
    }

    private int getUnionRankIndex() {
        int high = getUnionRank() / 100;
        int low = getUnionRank() % 100;
        return (low - 1) + (high - 1) * 5;
    }

    private boolean isMaxUnionRank() {
        return getUnionRank() == 405;
    }

    public String getUnionRankName() {
        return UnionMember.ranks[getUnionRankIndex()];
    }

    public String getUnionNextRankName() {
        return UnionMember.ranks[isMaxUnionRank() ? getUnionRankIndex() : getUnionRankIndex() + 1];
    }

    public int getUnionCoinReq() {
        return UnionMember.reqCoin[getUnionRankIndex()];
    }

    public int getUnionLevelReq() {
        return UnionMember.reqLev[getUnionRankIndex()];
    }

    public int getUnionLevel() {
        int total = 0;
        for (Char chr : chr.getAccount().getEligibleUnionChars()) {
            total += chr.getLevel();
        }
        return total;
    }

    public int getUnionCharacterCount() {
        return chr.getAccount().getEligibleUnionChars().size();
    }

    public int getUnionAssignedCharacterCount() {
        return chr.getUnion().getActiveUnionChars(chr.getActiveUnionPreset()).size();
    }

    public int getUnionAssignedMaxCharacterCount() {
        return UnionMember.attackerCount[getUnionRankIndex()];
    }

    public int getUnionAssignedNextMaxCharacterCount() {
        return UnionMember.attackerCount[isMaxUnionRank() ? getUnionRankIndex() : getUnionRankIndex() + 1];
    }

    public void incrementUnionRank() {
        chr.incrementUnionRank();
    }

    public int getUnionCoinByRaid() {
        return Integer.parseInt(getQRValueByKey(QuestConstants.UNION_RAID, "coin"));
    }

    public void spawnLinkMobsetHP(int id, int x, int y, long nowhp, long maxhp) {
        Mob mob = MobData.getMobDeepCopyById(id);
        Position pos = new Position(x, y);
        mob.setPosition(pos.deepCopy());
        mob.setPrevPos(pos.deepCopy());
        mob.setPosition(pos.deepCopy());
        mob.setHp(nowhp);
        mob.setMaxHp(maxhp);
        mob.setNotRespawnable(false);
        mob.setField(chr.getField());
        mob.setControllerID(chr.getId());
        chr.getField().spawnLife(mob, null);
    }

    public List<Equip> getEquipsForSell() {
        List<Equip> equips = new ArrayList<>();
        for (Item item : chr.getEquipInventory().getItems()) {
            if (item instanceof Equip equip) {
                if (!equip.hasAttribute(EquipAttribute.Locked)) {
                    equips.add(equip);
                }
            }
        }
        return equips;
    }

    public void initCharacterPotential(int questID) {
        final var charID = chr.getId();
        if (chr.getLevel() >= 30) {
            final var firstAbility = 12394;
            if (questID == firstAbility && !chr.hasQuestCompleted(firstAbility)) { // First Ability - The Eye Opener
                chr.getPotentials().add(new CharacterPotential(charID, 0, (byte) 1, 70000044, (byte) 5, (byte) 0));
                chr.getPotentials().add(new CharacterPotential(charID, 1, (byte) 1, 70000044, (byte) 5, (byte) 0));
                chr.getPotentials().add(new CharacterPotential(charID, 2, (byte) 1, 70000044, (byte) 5, (byte) 0));
                completeQuestNoRewards(firstAbility);
            }
        }
        if (chr.getLevel() >= 50) {
            final var secondAbility = 12395;
            if (questID == secondAbility && !chr.hasQuestCompleted(secondAbility)) { // Second Ability - Power, Strength
                chr.getPotentials().add(new CharacterPotential(charID, 0, (byte) 2, 70000044, (byte) 5, (byte) 0));
                chr.getPotentials().add(new CharacterPotential(charID, 1, (byte) 2, 70000044, (byte) 5, (byte) 0));
                chr.getPotentials().add(new CharacterPotential(charID, 2, (byte) 2, 70000044, (byte) 5, (byte) 0));
                completeQuestNoRewards(secondAbility);
            }
            final var thirdAbility = 12396;
            if (questID == thirdAbility && !chr.hasQuestCompleted(thirdAbility)) { // The Eye Opener
                chr.getPotentials().add(new CharacterPotential(charID, 0, (byte) 3, 70000044, (byte) 15, (byte) 1));
                chr.getPotentials().add(new CharacterPotential(charID, 1, (byte) 3, 70000044, (byte) 15, (byte) 1));
                chr.getPotentials().add(new CharacterPotential(charID, 2, (byte) 3, 70000044, (byte) 15, (byte) 1));
                completeQuestNoRewards(thirdAbility);
            }
        }
    }

    public String getCurrentTime() {
        return FileTime.currentTime().toYYYYMMDD_HHMMSS();
    }

    @Override
    public void setupBossDeathCount(int deathCount) {
        setupBossDeathCount(deathCount, false);
    }

    @Override
    public void setupBossDeathCount(int deathCount, boolean verusHillaDeathCount) {
        Party party = chr.getParty();
        if (party != null) {
            for (Char member : party.getPartyMembersInSameFieldWithChr(chr)) {
                applyBossDeathCount(member, deathCount, verusHillaDeathCount);
            }
            return;
        }
        applyBossDeathCount(chr, deathCount, verusHillaDeathCount);
    }

    private void applyBossDeathCount(Char member, int deathCount, boolean verusHillaDeathCount) {
        member.setDeathCount(deathCount);
        if (verusHillaDeathCount) {
            member.setVHDeathCount(new boolean[]{true, true, true, true, true});
        }
        member.showDeathCount(deathCount);
    }

    @Override
    public void showZakumPlatforms() {
        // Legacy altar script hook; Boss UI paths auto-summon zakum without platform placement.
    }

    public void startBossUI(String bossName) {
        int orderId = BossPartyType.getOrderIdByBossName(bossName);
        if (orderId != -1) {
            if (net.swordie.ms.connection.protocol.Protocol.active().version()
                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                    && Arrays.stream(BossPartyType.values()).filter(b -> b.getOrderId() == orderId)
                            .noneMatch(BossConstants::hasConfiguredBossMaps)) {
                sendSayOkay("此首領目前尚未完成支援，暫時無法開始挑戰。");
                return;
            }
            chr.write(WvsContext.startBossUI(orderId));
        }
    }

    public void initForBossing(String bossName) {
        for (Integer skillID : chr.getSkillCoolTimes().keySet()) {
            chr.addSkillCoolTime(skillID, 0);
            chr.write(UserLocal.skillCooltimeSetM(skillID, 0));
        }
        chr.write(WvsContext.clearAnnouncedQuest());
        chr.getTemporaryStatManager().removeAllDebuffs();
        var orderId = getQRValueByKey(102401, "order");
        var difficulty = getQRValueByKey(102401, "diff");
        if (orderId != null && difficulty != null) {
            BossPartyType boss = BossPartyType.getByOrderIdAndDifficulty(Integer.parseInt(orderId), Integer.parseInt(difficulty));
            if (boss != null) {
                ensureBossEntryItems(boss);
            }
        }
        addPopUpSay(0, 2000, "請做好戰鬥準備！\r\n#b" + bossName + "#k。", "");
        systemMessage("你已到達首領等待地圖。除開關類技能外，所有增益效果將被清除，技能冷卻也會重置。準備完成後，請透過傳送點或 NPC 進入首領戰鬥地圖。");
    }

    /** Revalidate the party at both lobby transfer and combat entry; lobby membership can change. */
    public boolean validateBossEntryParty(boolean requireInstance) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) return true;
        var party = chr.getParty();
        if (party == null || !party.isLeader(chr)) {
            chr.chatPopup("請讓你的隊長執行該操作。");
            return false;
        }
        var members = party.getOnlineChars();
        if (members.size() != party.getMembers().size()) {
            chr.chatPopup("所有隊伍成員都必須在線。");
            return false;
        }
        var instance = chr.getInstance();
        for (Char member : members) {
            if (member == null || member.getField() != chr.getField()
                    || (requireInstance && (instance == null || member.getInstance() != instance
                    || !instance.getChars().containsKey(member.getId())))) {
                chr.chatPopup("請讓所有隊員集合在同一個準備地圖與挑戰實例後再開始。");
                return false;
            }
        }
        return true;
    }

    public void startBoss() {
        autoUnlockBossPrerequisites();
        var orderId = getQRValueByKey(102401, "order");
        var difficulty = getQRValueByKey(102401, "diff");
        var mapR = getQRValueByKey(102401, "mapR");
        setSpeakerID(9010000);
        if (orderId == null || difficulty == null || mapR == null) {
            sendSayOkay("找不到該首領的資料。請聯絡 GM！");
            return;
        }
        BossPartyType boss = BossPartyType.getByOrderIdAndDifficulty(Integer.parseInt(orderId), Integer.parseInt(difficulty));
        if (boss == null || !BossConstants.hasConfiguredBossMaps(boss)) {
            sendSayOkay("該首領尚未設定完整的入口與戰鬥地圖，暫時無法進入。");
            return;
        }
        if (!BossRuntimeConfig.isDifficultyEnabled(boss)) {
            sendSayOkay("該首領難度暫未開放，請聯繫管理員。");
            return;
        }
        if (!ensureBossEntryItems(boss)) {
            return;
        }
        if (BossHelper.checkInstance(chr)) {
            sendSayOkay("發生未知錯誤，請離開後重新進入該首領。");
            return;
        }
        var party = createSoloPartyForBossIfNeeded();
        if (party == null) {
            chr.chatPopup("自動創建隊伍失敗，請手動創建隊伍後再繼續。");
            return;
        }
        if (!party.isLeader(chr)) {
            chr.chatPopup("請讓你的隊長執行該操作。");
            return;
        }
        if (!validateBossEntryParty(true)) return;
        int fieldID = BossConstants.getBossEntryFieldId(boss);
        if (party.getMembers().size() > BossConstants.getBossPartyLimit(boss)) {
            sendSayOkay("此首領最多可由 " + BossConstants.getBossPartyLimit(boss) + " 人組隊挑戰。");
            return;
        }
        int time = BossConstants.getBossFightingTime(boss);
        var instance = chr.getInstance();
        if (instance == null) {
            sendSayOkay("首領副本已失效，請重新進入等待地圖。");
            return;
        }
        instance.setTimeout(time, true);
        for (Char partyMember : party.getOnlineChars()) {
            partyMember.warp(fieldID);
        }
    }

    public Party createSoloPartyForBossIfNeeded() {
        Party party = chr.getParty();
        if (party != null) {
            return party;
        }
        try {
            party = Party.createNewParty((byte) 1, chr.getName() + "的隊伍", chr.getWorld());
            party.addPartyMember(chr);
            chr.write(WvsContext.partyResult(PartyResult.create(party)));
            chr.chatPopup("已自動創建單人隊伍。");
            return party;
        } catch (Exception e) {
            return chr.getParty();
        }
    }

    public void autoUnlockBossPrerequisites() {
        for (BossPartyType boss : BossPartyType.values()) {
            int preQuest = boss.getPreQuest();
            if (preQuest > 0 && !chr.hasQuestCompleted(preQuest)) {
                try {
                    completeQuestNoRewards(preQuest);
                } catch (Exception ignored) {
                }
            }
        }
        int[] extraQuestIds = new int[]{
                30007, 1562, 31179, 31833, 3521, 31152, 17523, 65851,
                34330, 34331, 34332, 34333, 34334, 34335, 34336, 34337, 34338, 34339,
                34560, 35632, 35815, 36772, 37851, 37852, 37853, 37854, 37855,
                37856, 37857, 37858, 37859, 37860, 37861, 37862, 37863, 37864,
                37865, 37866, 37867, 37868, 37869, 37870, 37871, 37900
        };
        for (int questId : extraQuestIds) {
            if (!chr.hasQuestCompleted(questId)) {
                try {
                    completeQuestNoRewards(questId);
                } catch (Exception ignored) {
                }
            }
        }
        try {
            createQuestWithQRValue(34560, "30=h1;31=h1;32=h1;33=h1;40=h1;41=h1;42=h1;44=h1;45=h1;46=h1;47=h1;48=h1;49=h1;50=h1;51=h1;52=h1;53=h1;54=h1;55=h1;56=h1;57=h1;58=h1;77=h1;78=h1;79=h1;80=h1");
            createQuestWithQRValue(37900, "01=h1");
        } catch (Exception ignored) {
        }
    }

    public boolean ensureBossEntryItems(BossPartyType boss) {
        for (int itemId : BossConstants.getRequiredEntryItems(boss)) {
            if (!giveBossEntryItemIfMissing(itemId, 1)) {
                return false;
            }
        }
        return true;
    }

    private boolean giveBossEntryItemIfMissing(int itemId, int quantity) {
        if (hasItem(itemId, quantity)) {
            return true;
        }
        giveItem(itemId, quantity);
        if (hasItem(itemId, quantity)) {
            return true;
        }
        chr.chatPopup("其他欄空間不足，無法補發首領入場道具 (#i" + itemId + "#)。");
        return false;
    }

    public void notifyBossLootWindow(String bossName) {
        BossHelper.notifyCompatibleBossLootWindow(chr.getField(), bossName);
    }

    public void scheduleBossLootWindowExit(int fieldId) {
        BossHelper.scheduleCompatibleBossExit(chr, fieldId);
    }

    public void exitBoss() {
        setSpeakerID(9010000);
        if (sendAskYesNo("你確定要退出這場戰鬥嗎？")) {
            var mapR = getQRValueByKey(102401, "mapR");
            if (mapR != null) {
                warpInstanceOut(chr, Integer.parseInt(mapR));
            } else {
                warpInstanceOut(chr, FieldConstants.HOME_MAP);
            }
        }
    }

    public void setBackGround() {
        int itemID = getParentID();
        int type = 0;
        switch (itemID) {
            case 2639943:
            case 2639944:
                type = 25;
                break;
        }
        final int qrKey = 502593;
        for (int i = 0; i < 5; i++) {
            String v = chr.getQRValueByKey(qrKey, String.valueOf(i));
            if (v != null && Integer.parseInt(v) == type) {
                chat("Bạn đã có chủ đề này rồi.");
                return;
            }
        }
        for (int i = 0; i < 5; i++) {
            String v = chr.getQRValueByKey(qrKey, String.valueOf(i));
            if (v == null || v.equals("0")) {
                chr.setQRValueByKey(qrKey, String.valueOf(i), type);
                chr.write(UserLocal.setCharacterInfoBackground(3));
                gainQuestItem(itemID, -1);
                consumeItem(itemID);
                return;
            }
        }
    }

    public void setCharacterSelectionBG() {
        int itemID = getParentID();
        int type;
        switch (itemID) {
            case 2639946 -> type = 2; // Twilight Altar
            default -> {
                return;
            }
        }
        final int qrKey = 607;
        for (int i = 0; i < 10; i++) {
            String v = chr.getQRValueByKey(qrKey, String.valueOf(i));
            if (v != null && Integer.parseInt(v) == type) {
                chat("Bạn đã có chủ đề này rồi.");
                return;
            }
        }
        for (int i = 0; i < 10; i++) {
            String v = chr.getQRValueByKey(qrKey, String.valueOf(i));
            if (v == null || v.equals("0")) {
                chr.setQRValueByKey(qrKey, String.valueOf(i), type);
                chr.write(UserLocal.setCharacterSelectionBackground(0, 1));
                gainQuestItem(itemID, -1);
                consumeItem(itemID);
                chr.getAccount().setLoginTheme(type);
                return;
            }
        }
    }

    public void gainQuestItem(int itemID, int quantity) {
        chr.write(UserPacket.effect(Effect.gainQuestItem(itemID, quantity)));
    }

    public void removeBlowWeather() {
        chr.write(FieldPacket.removeBlowWeather());
    }

    public void blowWeather(int itemID, String message, int seconds) {
        chr.write(FieldPacket.blowWeather(itemID, message, seconds, null));
    }

    public void characterSelfInfo(int unk, boolean bool) {
        chr.write(UserPacket.characterSelfInfo(unk, bool));
    }

    public void resultInstanceTable(String name, int type, int subType, boolean rightResult, int value) {
        chr.write(WvsContext.resultInstanceTable(name, type, subType, rightResult, value));
    }

    public void startFieldBooster(int itemID) {
        switch (itemID) {
            case 2638603: // HEXA Booster
                var field = chr.getField();
                if (field.getMobGens().isEmpty() || field.getBoosterMobID() != 0) {
                    chr.chatMessage("該地圖無法使用加速器。");
                    chr.dispose();
                    return;
                }
                Mob first = Util.getRandomFromCollection(field.getMobGens()).getMob();
                if (Math.abs(first.getLevel() - chr.getLevel()) <= 20) {
                    var templateID = 8645399; // Scattered Sol Erda
                    screen("Effect/ItemEff.img/2638603/startEff");
                    playSound("Sound/SoundEff.img/HasteBooster/count");
                    consumeItem(itemID);
                    chr.chatMessage("你已使用 HEXA加速器。");
                    field.setBoosterMobID(0);
                    field.setBoosterEXPMulti(0);
                    addEvent(field.getTimer().addEvent(() -> {
                        playSound("Sound/SoundEff.img/HasteBooster/start");
                        createStopWatch(100);
                        field.setBoosterMobID(templateID);
                        field.setBoosterEXPMulti(10);
                        int i = 0;
                        for (Mob mob : field.getMobs()) {
                            if (i >= 6) {
                                break;
                            }
                            spawnMob(templateID, mob.getX() + Util.getRandom(-10, 10), mob.getY(), false, mob.getMaxHp(), mob.getExp() * field.getBoosterEXPMulti());
                            i++;
                        }
                        addEvent(field.getTimer().addEvent(() -> field.setBoosterMobID(0), 100, TimeUnit.SECONDS));
                    }, 2500L));
                } else {
                    chr.chatMessage("該地圖無法使用加速器。");
                    chr.dispose();
                }
                break;
            case 2637168: // VIP Booster
            case 2637169: // VIP Booster
            case 2639826: // VIP Booster
                field = chr.getField();
                if (field.getMobGens().isEmpty() || field.getBoosterMobID() != 0) {
                    chr.chatMessage("該地圖無法使用加速器。");
                    chr.dispose();
                    return;
                }
                first = Util.getRandomFromCollection(field.getMobGens()).getMob();
                if (Math.abs(first.getLevel() - chr.getLevel()) <= 20) {
                    var templateID = 9834331; // Booster Flame
                    var useCount = chr.getQRValueByKey(101717, "useCount");
                    int count = 1;
                    if (useCount != null) {
                        count = Integer.parseInt(useCount) + 1;
                    }
                    screen("Effect/EventEffect.img/HasteBooster/startEff");
                    playSound("Sound/SoundEff.img/HasteBooster/count");
                    consumeItem(itemID);
                    createQuestWithQRValue(101717, "useCount=" + count + ";date=" + FileTime.currentTime().toYYMMDD());
                    chr.chatScriptMessage("今天你已使用 VIP加速器 " + count + "/10 次。");
                    chr.chatMessage("你已使用 VIP加速器。");
                    chr.chatMessage("你今天還可以使用 "+ (10 - count) +" 次 VIP加速器。");
                    field.setBoosterMobID(0);
                    field.setBoosterEXPMulti(0);
                    addEvent(field.getTimer().addEvent(() -> {
                        playSound("Sound/SoundEff.img/HasteBooster/start");
                        createStopWatch(100);
                        field.setBoosterMobID(templateID);
                        field.setBoosterEXPMulti(10);
                        int i = 0;
                        for (Mob mob : field.getMobs()) {
                            if (i >= 6) {
                                break;
                            }
                            spawnMob(templateID, mob.getX() + Util.getRandom(-10, 10), mob.getY(), false, mob.getMaxHp(), mob.getExp() * field.getBoosterEXPMulti());
                            i++;
                        }
                        addEvent(field.getTimer().addEvent(() -> field.setBoosterMobID(0), 100, TimeUnit.SECONDS));
                    }, 2500L));
                } else {
                    chr.chatMessage("該地圖無法使用加速器。");
                    chr.dispose();
                }
                break;
            case 2638880: // Special VIP Booster
                // TODO?
                break;
        }
    }

    // ==================================================================================
    // Script-pack compatibility layer.
    // The Chinese v265 script pack was written against a richer ScriptManager API than
    // this fork provides; these members bridge those calls onto existing functionality.
    // KV stores (QuestRecordEx / EventValue / PQLog) persist in MySQL — QuestEx tables
    // are created lazily, account_event / character_pqlog already ship with the pack.
    // ==================================================================================

    private static volatile boolean compatTablesEnsured = false;
    private static final java.net.http.HttpClient COMPAT_HTTP_CLIENT = java.net.http.HttpClient.newHttpClient();
    private static final Set<Integer> compatAccountSynced = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<String, Object> compatTempValues = new HashMap<>();

    /**
     * The script pack keeps its own `account` table (師徒系統, 入島禮物, 充值 etc. read it).
     * Rows only exist for the pack author's original accounts, so mirror one in for every
     * account the first time it runs a script.
     */
    private void ensureCompatAccountRow() {
        try {
            if (chr == null || chr.getAccount() == null) {
                return;
            }
            int accId = chr.getAccount().getId();
            if (!compatAccountSynced.add(accId)) {
                return;
            }
            if (select("SELECT id FROM account WHERE id = " + accId).isEmpty()) {
                String name = chr.getUser() != null ? chr.getUser().getName() : ("acc" + accId);
                insert("INSERT INTO account (id, name, password, create_date, last_login) VALUES ("
                        + accId + ", '" + sqlEscape(name) + "', '', now(), now())");
            }
        } catch (Exception e) {
            // never let account mirroring break script startup
        }
    }

    private static void ensureCompatTables() {
        if (compatTablesEnsured) {
            return;
        }
        synchronized (ScriptManagerImpl.class) {
            if (compatTablesEnsured) {
                return;
            }
            boolean character = DatabaseManager.executeStatement("CREATE TABLE IF NOT EXISTS character_questex (charid INT NOT NULL, questid INT NOT NULL, `key` VARCHAR(64) NOT NULL, `value` TEXT, PRIMARY KEY (charid, questid, `key`))");
            boolean account = DatabaseManager.executeStatement("CREATE TABLE IF NOT EXISTS account_questex (accountid INT NOT NULL, questid INT NOT NULL, `key` VARCHAR(64) NOT NULL, `value` TEXT, PRIMARY KEY (accountid, questid, `key`))");
            boolean world = DatabaseManager.executeStatement("CREATE TABLE IF NOT EXISTS world_share_record (questid INT NOT NULL, `key` VARCHAR(64) NOT NULL, `value` TEXT, PRIMARY KEY (questid, `key`))");
            if ((!character || !account || !world) && net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3)
                throw new IllegalStateException("Script record tables could not be initialized");
            compatTablesEnsured = true;
        }
    }

    private static String sqlEscape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("'", "''");
    }

    private static String compatValueToString(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof Number n) {
            double d = n.doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.valueOf((long) d);
            }
            return String.valueOf(d);
        }
        return String.valueOf(v);
    }

    private static int compatParseInt(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        try {
            return (int) Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long compatToEpochMillis(Object o) {
        if (o == null) {
            return 0;
        }
        if (o instanceof java.util.Date d) {
            return d.getTime();
        }
        try {
            return java.sql.Timestamp.valueOf(String.valueOf(o)).getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    // ---- QuestRecordEx (per character) / QuestRecordExShare (per account) / WorldShareRecord (global) ----

    private String questExGet(String table, String ownerCol, long ownerId, int questId, String key) {
        ensureCompatTables();
        List<Map<String, Object>> rows = select("SELECT `value` FROM " + table + " WHERE " + ownerCol
                + (ownerCol.isEmpty() ? "" : " = " + ownerId + " AND") + " questid = " + questId
                + " AND `key` = '" + sqlEscape(key) + "' LIMIT 1");
        if (rows.isEmpty()) {
            return null;
        }
        Object v = rows.get(0).get("value");
        return v == null ? null : String.valueOf(v);
    }

    private void questExSet(String table, String ownerCol, long ownerId, int questId, String key, Object value) {
        ensureCompatTables();
        if (ownerCol.isEmpty()) {
            update("REPLACE INTO " + table + " (questid, `key`, `value`) VALUES (" + questId + ", '"
                    + sqlEscape(key) + "', '" + sqlEscape(compatValueToString(value)) + "')");
        } else {
            update("REPLACE INTO " + table + " (" + ownerCol + ", questid, `key`, `value`) VALUES (" + ownerId + ", "
                    + questId + ", '" + sqlEscape(key) + "', '" + sqlEscape(compatValueToString(value)) + "')");
        }
    }

    public String getQuestRecordEx(int questId, String key) {
        return questExGet("character_questex", "charid", getCharacterId(), questId, key);
    }

    public int getIntQuestRecordEx(int questId, String key) {
        return compatParseInt(getQuestRecordEx(questId, key));
    }

    public void updateQuestRecordEx(int questId, String key, Object value) {
        questExSet("character_questex", "charid", getCharacterId(), questId, key, value);
    }

    public void updateQuestRecordEx(int questId, Object value) {
        ensureCompatTables();
        String v = compatValueToString(value);
        if (v.isEmpty()) {
            update("DELETE FROM character_questex WHERE charid = " + getCharacterId() + " AND questid = " + questId);
        } else {
            questExSet("character_questex", "charid", getCharacterId(), questId, "", v);
        }
    }

    public String getQuestRecordExShare(int questId, String key) {
        return questExGet("account_questex", "accountid", getAccountId(), questId, key);
    }

    public int getIntQuestRecordExShare(int questId, String key) {
        return compatParseInt(getQuestRecordExShare(questId, key));
    }

    public void updateQuestRecordExShare(int questId, String key, Object value) {
        questExSet("account_questex", "accountid", getAccountId(), questId, key, value);
    }

    public String getWorldShareRecord(int questId, String key) {
        return questExGet("world_share_record", "", 0, questId, key);
    }

    public int getIntWorldShareRecord(int questId, String key) {
        return compatParseInt(getWorldShareRecord(questId, key));
    }

    public void updateWorldShareRecord(int questId, String key, Object value) {
        questExSet("world_share_record", "", 0, questId, key, value);
    }

    // ---- EventValue (account_event) / PQLog (character_pqlog); semantics mirror content/model/check_log.js ----

    private static String kvResetTime(int resetDays) {
        if (resetDays < 0) {
            return "2035-01-01 00:00:00";
        }
        return java.time.LocalDate.now().plusDays(resetDays) + " 23:59:59";
    }

    private Object[] kvLogGet(String table, String ownerCol, long ownerId, String key) {
        List<Map<String, Object>> rows = select("SELECT `value`, `reset_time` FROM `" + table + "` WHERE `" + ownerCol
                + "` = " + ownerId + " AND `key` = '" + sqlEscape(key) + "' LIMIT 1");
        if (rows.isEmpty()) {
            return new Object[]{0, null};
        }
        Object resetTime = rows.get(0).get("reset_time");
        if (compatToEpochMillis(resetTime) < System.currentTimeMillis()) {
            update("DELETE FROM `" + table + "` WHERE `" + ownerCol + "` = " + ownerId + " AND `key` = '" + sqlEscape(key) + "'");
            return new Object[]{0, null};
        }
        return new Object[]{compatParseInt(String.valueOf(rows.get(0).get("value"))), String.valueOf(resetTime)};
    }

    private void kvLogAdd(String table, String ownerCol, long ownerId, String key, int value, int resetDays) {
        Object[] existing = kvLogGet(table, ownerCol, ownerId, key);
        if (existing[1] != null) {
            update("UPDATE `" + table + "` SET `value` = `value` + " + value + " WHERE `" + ownerCol + "` = " + ownerId
                    + " AND `key` = '" + sqlEscape(key) + "'");
            return;
        }
        insert("INSERT INTO `" + table + "` (`" + ownerCol + "`, `key`, `value`, `reset_time`) VALUES (" + ownerId
                + ", '" + sqlEscape(key) + "', " + value + ", '" + kvResetTime(resetDays) + "')");
    }

    public int getEventValue(Object a, Object b, String key) {
        return (int) kvLogGet("account_event", "account_id", getAccountId(), key)[0];
    }

    public Object getEventValue(Object a, Object b, String key, boolean measure) {
        Object[] combo = kvLogGet("account_event", "account_id", getAccountId(), key);
        return measure ? combo : combo[0];
    }

    public int getEventValue(String key) {
        return (int) kvLogGet("account_event", "account_id", getAccountId(), key)[0];
    }

    public void addEventValue(Object a, Object b, String key) {
        kvLogAdd("account_event", "account_id", getAccountId(), key, 1, 0);
    }

    public void addEventValue(Object a, Object b, String key, int value) {
        kvLogAdd("account_event", "account_id", getAccountId(), key, value, 0);
    }

    public void addEventValue(Object a, Object b, String key, int value, int resetDays) {
        kvLogAdd("account_event", "account_id", getAccountId(), key, value, resetDays);
    }

    public void addEventValue(String key) {
        kvLogAdd("account_event", "account_id", getAccountId(), key, 1, 0);
    }

    public void addEventValue(String key, int value) {
        kvLogAdd("account_event", "account_id", getAccountId(), key, value, 0);
    }

    public void addEventValue(String key, int value, int resetDays) {
        kvLogAdd("account_event", "account_id", getAccountId(), key, value, resetDays);
    }

    public void resetEventValue(Object a, Object b, String key) {
        update("DELETE FROM `account_event` WHERE `account_id` = " + getAccountId() + " AND `key` = '" + sqlEscape(key) + "'");
    }

    public void resetEventValue(String key) {
        resetEventValue(null, null, key);
    }

    public int getPQLog(Object a, Object b, String key) {
        return (int) kvLogGet("character_pqlog", "characters_id", getCharacterId(), key)[0];
    }

    public Object getPQLog(Object a, Object b, String key, boolean measure) {
        Object[] combo = kvLogGet("character_pqlog", "characters_id", getCharacterId(), key);
        return measure ? combo : combo[0];
    }

    public int getPQLog(String key) {
        return (int) kvLogGet("character_pqlog", "characters_id", getCharacterId(), key)[0];
    }

    public void addPQLog(Object a, Object b, String key) {
        kvLogAdd("character_pqlog", "characters_id", getCharacterId(), key, 1, 0);
    }

    public void addPQLog(Object a, Object b, String key, int value) {
        kvLogAdd("character_pqlog", "characters_id", getCharacterId(), key, value, 0);
    }

    public void addPQLog(Object a, Object b, String key, int value, int resetDays) {
        kvLogAdd("character_pqlog", "characters_id", getCharacterId(), key, value, resetDays);
    }

    public void addPQLog(String key) {
        kvLogAdd("character_pqlog", "characters_id", getCharacterId(), key, 1, 0);
    }

    public void addPQLog(String key, int value) {
        kvLogAdd("character_pqlog", "characters_id", getCharacterId(), key, value, 0);
    }

    public void addPQLog(String key, int value, int resetDays) {
        kvLogAdd("character_pqlog", "characters_id", getCharacterId(), key, value, resetDays);
    }

    public void resetPQLog(Object a, Object b, String key) {
        update("DELETE FROM `character_pqlog` WHERE `characters_id` = " + getCharacterId() + " AND `key` = '" + sqlEscape(key) + "'");
    }

    public void resetPQLog(String key) {
        resetPQLog(null, null, key);
    }

    // ---- fluent NPC-dialog decorators ----

    public ScriptManagerImpl id(int npcId) {
        setSpeakerID(npcId);
        return this;
    }

    public ScriptManagerImpl ui(int type) {
        return this;
    }

    public ScriptManagerImpl me() {
        return meFlip();
    }

    public ScriptManagerImpl next() {
        return this;
    }

    public ScriptManagerImpl noEsc() {
        return this;
    }

    public ScriptManagerImpl uiMax() {
        return this;
    }

    // ---- simple aliases and bridges ----

    public String getName() {
        return getCharacterName();
    }

    public int getId() {
        return getCharacterId();
    }

    public int getPosX() {
        return chr.getPosition().getX();
    }

    public int getPosY() {
        return chr.getPosition().getY();
    }

    public boolean hasMesos(long amount) {
        return getMesos() >= amount;
    }

    public void gainExp(long exp) {
        giveExp(exp);
    }

    public long getOnlineTime() {
        return getOnlineTimeMinutes();
    }

    public void changeChannel(int channel) {
        changeChannelAndWarp(channel, chr.getFieldID());
    }

    public boolean canGainItem(int itemId, int quantity) {
        return canHold(itemId, quantity);
    }

    public void removeItemAll(int itemId) {
        int quantity = getQuantityOfItem(itemId);
        if (quantity > 0) {
            consumeItem(itemId, quantity);
        }
    }

    public List<Map<String, Object>> customSqlResult(String sql) {
        return select(sql);
    }

    public void showTimer(int seconds) {
        createClock(seconds);
    }

    public void dropMessage(String msg) {
        chat(msg);
    }

    public void dropMessage(int type, String msg) {
        chat(msg);
    }

    public void dropAlertNotice(String msg) {
        systemMessage(msg);
    }

    public void broadcastPlayerNotice(int type, String msg) {
        broadcastWorldChat(msg);
    }

    public void showScriptProgressMsg(String msg) {
        progressMessageFont(msg);
    }

    public void showProgressMessageFont(String msg, int type, int size, int line, int duration) {
        progressMessageFont(type, size, line, duration <= 0 ? 1500 : duration, msg);
    }

    public int getCashShopCurrency(int type) {
        return switch (type) {
            case 1 -> getMaplePoints();
            case 2 -> getNxPrepaid();
            default -> 0;
        };
    }

    public void modifyCashShopCurrency(int type, int amount) {
        switch (type) {
            case 1 -> addMaplePoints(amount);
            case 2 -> addNxPrepaid(amount);
        }
    }

    public Item makeItemWithId(int itemId) {
        return ItemData.getItemDeepCopy(itemId);
    }

    public void gainItem(Item item) {
        if (item != null) {
            chr.addItemToInventory(item);
        }
    }

    public void gainGachaponItem(String title, int itemId, int quantity, int type) {
        gainItem(itemId, quantity);
        broadcastWorldChat(title + " " + getCharacterName() + " 獲得了 " + StringData.getItemStringById(itemId) + " x" + quantity + "！");
    }

    public boolean equipped(int bagIndex, int itemId) {
        for (Item item : chr.getEquippedInventory().getItems()) {
            if ((item.getBagIndex() == bagIndex || item.getBagIndex() == -bagIndex) && item.getItemId() == itemId) {
                return true;
            }
        }
        return false;
    }

    public void setSkin(int id) {
        changeCharacterLook(id);
    }

    public void setFace(int id) {
        changeCharacterLook(id);
    }

    public void setHair(int id) {
        changeCharacterLook(id);
    }

    public void modifySp(int jobLevel, int amount) {
        addSPJobAdv((short) jobLevel, amount);
    }

    public int getGuildId() {
        return chr.getGuildID();
    }

    public int getVipLevel() {
        if (chr == null || chr.getAccount() == null) return 0;
        try { return PermanentVip.level(chr.getAccount().getId()); }
        catch (SQLException e) { throw new IllegalStateException("無法讀取永久 VIP 等級，請稍後再試。", e); }
    }

    public double getPermanentVipPoints() {
        try { return PermanentVip.balance(chr.getUser().getName()).doubleValue(); }
        catch (SQLException e) { throw new IllegalStateException("無法讀取 VIP 幣餘額。", e); }
    }

    public int activatePermanentVip() {
        try { return PermanentVip.activate(chr.getUser().getId(), chr.getAccount().getId(), chr.getUser().getName()); }
        catch (SQLException e) { throw new IllegalStateException("VIP 開通未完成，請稍後再試。", e); }
    }

    public void creditPermanentVipPoints(int amount) {
        try { PermanentVip.credit(chr.getUser().getId(), chr.getAccount().getId(), chr.getUser().getName(), amount); }
        catch (SQLException e) { throw new IllegalStateException("VIP 幣充值未完成，請稍後再試。", e); }
    }

    public boolean isQuestCompleted(int questId) {
        return hasQuestCompleted(questId);
    }

    public boolean isQuestStarted(int questId) {
        return hasQuest(questId);
    }

    public void removeQuest(int questId) {
        chr.removeQuest(questId);
    }

    public int senSayOkay(String msg) {
        return sendSayOkay(msg);
    }

    public void addTempValue(String key, Object value) {
        compatTempValues.put(key, value);
    }

    public Object getTempValue(String key) {
        return compatTempValues.get(key);
    }

    public void addPopupSay(int npcId, int duration, String msg, String effect) {
        chat(msg);
    }

    public void addPopupSay(int npcId, int duration, String msg, String effect, boolean flip) {
        chat(msg);
    }

    public void setReviveCount(int count) {
        // boss revive-count system not ported
    }

    public void spawnTempNpc(int npcId, int x, int y, Object faceOrFlip) {
        spawnNpc(npcId, x, y);
    }

    public void destroyTempNpc(int npcId) {
        removeNpcs(npcId);
    }

    public void setNpcSpecialAction(Object npcId, String action, int a, boolean b) {
        // cosmetic NPC animation not ported
    }

    // ---- [eventfw] MetaMS script events: player side (stage / members[i] / npc) -------------------------------------

    /** {@code stage.getEvent()}: the event this player is bound to, or null. */
    public Object getEvent() {
        return net.swordie.ms.scripts.event.EventManager.get().getEvent(chr);
    }

    /** {@code stage.getEvent(name)}: this player's event when it is {@code name}, else null. */
    public Object getEvent(String name) {
        net.swordie.ms.scripts.event.EventInstance ev = net.swordie.ms.scripts.event.EventManager.get().getEvent(chr);
        return ev != null && name != null && name.equals(ev.getName()) ? ev : null;
    }

    /** {@code player.setEvent(event)}; null leaves the current event. */
    public void setEvent(Object event) {
        net.swordie.ms.scripts.event.EventManager.get().setEvent(chr,
                event instanceof net.swordie.ms.scripts.event.EventInstance ev ? ev : null);
    }

    /** {@code npc.makeEvent(name, onlyOne, attachment)} (same as {@code ctx.makeEvent}). */
    public Object makeEvent(Object... args) {
        return makeScriptEvent(args);
    }

    /** {@code ctx.makeEvent(name, onlyOne, attachment)}: null when the event cannot start. */
    Object makeScriptEvent(Object... args) {
        if (!GAME_SCRIPTS_ENABLED) return null;
        if (args == null || args.length == 0 || args[0] == null) {
            return null;
        }
        String name = String.valueOf(args[0]);
        boolean onlyOne = args.length > 1 && compatTruthy(args[1]);
        Object attachment = args.length > 2 ? args[2] : null;
        int channel = 1;
        java.util.function.IntFunction<Field> channelFields = null;
        if (chr != null && chr.getClient() != null && chr.getClient().getChannelInstance() != null) {
            channel = chr.getClient().getChannel();
            channelFields = chr.getClient().getChannelInstance()::getField;
        } else {
            try {
                net.swordie.ms.world.Channel ch = Server.get().getWorld().getChannelById((byte) 1);
                if (ch != null) {
                    channelFields = ch::getField;
                }
            } catch (RuntimeException ignored) {
                // tools without a world
            }
        }
        net.swordie.ms.scripts.event.EventManager em = net.swordie.ms.scripts.event.EventManager.get();
        int exit = 0;
        Field here = chr != null ? chr.getField() : getField();
        if (here != null) {
            exit = here.getId();
            net.swordie.ms.scripts.event.EventInstance cur = em.getEvent(chr);
            if (cur != null && cur.isEventMap(here.getId())) {
                exit = cur.getExitMap();
            }
        }
        return em.makeEvent(name, onlyOne, attachment, channel, chr, channelFields, exit);
    }

    private static boolean compatTruthy(Object o) {
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.doubleValue() != 0;
        if (o instanceof CharSequence s) return !s.isEmpty() && !"false".contentEquals(s) && !"0".contentEquals(s);
        return o != null;
    }

    /** Boss revive counter display of MetaMS events (the revive system itself belongs to the boss runtime). */
    public void showReviveCount() {
        if (chr != null && chr.getDeathCount() >= 0) {
            chr.write(UserLocal.deathCountInfo(chr.getId(), chr.getDeathCount()));
        }
    }

    /** MetaMS {@code cancelDebuff(skill, level)}; Swordie clears mob-skill debuffs on map change already. */
    public void cancelDebuff(int mobSkillId, int level) {
    }

    /** Full HP/MP restore (MetaMS {@code revival()}). */
    public void revival() {
        if (chr != null) {
            heal();
        }
    }

    public void showStopwatch(int seconds) {
        if (chr != null && seconds > 0) {
            chr.write(FieldPacket.clock(ClockPacket.stopWatch(seconds)));
        }
    }

    /** Platformer run timer: shown as a seconds clock ({@code seconds <= 0} removes it). */
    public void showPlatformerTimer(int type, int seconds, int unk, int questId) {
        if (chr == null) {
            return;
        }
        if (seconds > 0) {
            chr.write(FieldPacket.clock(ClockPacket.secondsClock(seconds)));
        } else {
            removeClock();
        }
    }

    /** Raw quest record value (MetaMS {@code getQuestData(questId)}). */
    public String getQuestData(int questId) {
        String v = chr == null ? null : getQRValue(questId);
        return v == null || v.isEmpty() ? "0" : v;
    }

    /** Fresh {@code party} object (e.g. after {@code npc.createParty()} in the same script). [eventfw] */
    public Object getPartyBinding() {
        return chr != null && chr.getParty() != null ? new PartyBinding(chr) : null;
    }

    /** MetaMS local emotion effect; cosmetic, not sent (no verified 282 packet). */
    public void setUserEmotionLocal(int emotion, int duration, boolean byItemOption) {
    }

    public void showWeatherEffectNotice(String msg, int type, int duration, boolean unk) {
        if (chr == null) {
            return;
        }
        WeatherEffNoticeType wt = null;
        for (WeatherEffNoticeType w : WeatherEffNoticeType.values()) {
            if (w.getVal() == type) {
                wt = w;
                break;
            }
        }
        if (wt == null) {
            chat(msg);
        } else {
            showWeatherNotice(msg, wt, duration);
        }
    }

    public List<Object> getEquipSkills(Object... args) {
        return new ArrayList<>();
    }

    public void broadcastWeatherEffectNotice(int type, String msg, int duration) {
        Server.get().broadcastForWorld(WvsContext.weatherEffectNotice(WeatherEffNoticeType.Snowflakes, msg, duration));
    }

    public void httpPost(String url, String body) {
        if (!GAME_SCRIPTS_ENABLED) return;
        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body == null ? "" : body))
                    .build();
            COMPAT_HTTP_CLIENT.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }

    public Object[] getMapleUnionInfo() {
        return new Object[]{getUnionRankName(), getUnionLevel(), getUnionCharacterCount(),
                getUnionAssignedCharacterCount(), getUnionAssignedMaxCharacterCount()};
    }

    public Object[] getMapleUnionUpgradeInfo() {
        String next = getUnionNextRankName();
        if (next == null || next.isEmpty()) {
            return null;
        }
        return new Object[]{getUnionRankName(), next, getUnionAssignedMaxCharacterCount(),
                getUnionAssignedNextMaxCharacterCount(), getUnionLevelReq(), getUnionLevel(), getUnionCoinReq()};
    }

    public void increaseEquipInvSlot(int amount) {
        increaseCompatInvSlot(InvType.EQUIP, amount);
    }

    public void increaseConsumeInvSlot(int amount) {
        increaseCompatInvSlot(InvType.CONSUME, amount);
    }

    public void increaseEtcInvSlot(int amount) {
        increaseCompatInvSlot(InvType.ETC, amount);
    }

    public void increaseInstallInvSlot(int amount) {
        increaseCompatInvSlot(InvType.INSTALL, amount);
    }

    private void increaseCompatInvSlot(InvType type, int amount) {
        if (isNativeInventoryExpansion()) {
            net.swordie.ms.client.character.tms282.Tms282InventoryExpansion.growInventory(chr, amount, type.getVal());
            return;
        }
        Inventory inv = chr.getInventoryByType(type);
        if (inv != null) {
            inv.setSlots(Math.max(inv.getSlots(), Math.min(128, inv.getSlots() + amount)));
            inv.updateSlotsToSQL();
        }
    }

    public void debug(String msg) {
        System.out.println("[ScriptDebug] " + msg);
    }

    public String getScriptName() {
        return "";
    }

    // ---- avatar-selection chain (beauty salon) ----

    private int[] compatAvatarStyles = new int[0];

    public ScriptManagerImpl showType(int type) {
        return this;
    }

    public ScriptManagerImpl styles(int[] styles) {
        compatAvatarStyles = styles == null ? new int[0] : styles;
        return this;
    }

    public ScriptManagerImpl cardId(int couponItemId) {
        return this;
    }

    public int askAvatar(String msg) {
        return sendAskAvatar(msg, false, false, compatAvatarStyles);
    }

    public int askAngelicBuster(String msg) {
        return sendAskAvatar(msg, false, true, compatAvatarStyles);
    }

    // ---- misc small bridges/stubs ----

    public void updateDamageSkin(int skinId) {
        addDamageSkin(skinId);
    }

    /**
     * Every character already gets {@link GameConstants#DAMAGE_SKIN_MAX_SIZE} slots, so there is nothing to expand.
     * Returns false so the calling item script keeps the coupon instead of consuming it for nothing.
     */
    public boolean increaseDamageSkinSlot(int amount) {
        chat("傷害字型欄位已是上限（" + GameConstants.DAMAGE_SKIN_MAX_SIZE + " 格），不需要擴充。");
        return false;
    }

    public boolean gainChatEmoticon(int emoticonId) {
        return net.swordie.ms.client.character.emoticon.EmoticonScripts.gainChatEmoticon(chr, emoticonId);
    }

    public boolean gainChatEmoticon(int emoticonId, int days) {
        return net.swordie.ms.client.character.emoticon.EmoticonScripts.gainChatEmoticon(chr, emoticonId, days);
    }

    public boolean gainChatEmoticonGroup(int groupId) {
        return net.swordie.ms.client.character.emoticon.EmoticonScripts.gainChatEmoticonGroup(chr, groupId);
    }

    public void applyItemBuff(int itemId) {
        useItem(itemId);
    }

    public void applySkillBuff(int skillId) {
        // skill buff application not ported
    }

    public void applySkillBuff(int skillId, int slv) {
        // skill buff application not ported
    }

    public void exclRequest() {
        if (chr != null && chr.getClient() != null) {
            chr.getClient().write(WvsContext.exclRequest());
        }
    }

    // ---- long-tail aliases ----

    public int getSubJob() {
        return chr.getSubJob();
    }

    public int getJobCategory() {
        return JobConstants.getJobCategory(chr.getJob());
    }

    public long getMeso() {
        return getMesos();
    }

    public int getWorldId() {
        return ServerConstants.WORLD_ID;
    }

    /** Alias for a typo'd addEventValue call in the script pack. */
    public void addventValue(Object a, Object b, String key, int value, int resetDays) {
        addEventValue(a, b, key, value, resetDays);
    }

    public void modifyAp(int amount) {
        chr.addStatAndSendPacket(Stat.ap, amount);
    }

    public void increaseMaxHp(int amount) {
        chr.addStatAndSendPacket(Stat.mhp, amount);
    }

    public void increaseMaxMp(int amount) {
        chr.addStatAndSendPacket(Stat.mmp, amount);
    }

    public boolean upgradeMapleUnionUpgrade() {
        chat("聯盟升級功能尚未實裝。");
        return false;
    }

    public void createParty() {
        // solo party creation not ported
    }

    public void disbandParty() {
        // party disband not ported
    }

    // ---- item-script bridges (MetaMS / script-pack names -> existing Char/inventory APIs) ----

    public int getFreeSlots(int invType) {
        return getFreeSlot(invType);
    }

    /** One level up with the job's level-up handling; false (no-op) at the max level. */
    public boolean levelUp() {
        if (chr.getLevel() >= GameConstants.MAX_LEVEL) {
            return false;
        }
        addLevel(1);
        return true;
    }

    /** Guild contribution from an item (bypasses the daily mob/quest contribution cap, as the item grants a fixed amount). */
    public boolean modifyGuildCommitment(int amount) {
        net.swordie.ms.client.social.Guild.Guild guild = chr.getGuild();
        if (guild == null || amount == 0) {
            return false;
        }
        net.swordie.ms.client.social.Guild.GuildMember gm = guild.getMemberByCharID(chr.getId());
        if (gm == null) {
            return false;
        }
        gm.addContribution(guild, amount);
        if (amount > 0) {
            guild.addHonorEXP(amount);
        }
        return true;
    }

    public void modifyCharisma(int amount) {
        chr.addTraitExp(Stat.charismaEXP, amount);
    }

    public void modifyInsight(int amount) {
        chr.addTraitExp(Stat.insightEXP, amount);
    }

    public void modifyWill(int amount) {
        chr.addTraitExp(Stat.willEXP, amount);
    }

    public void modifyCraft(int amount) {
        chr.addTraitExp(Stat.craftEXP, amount);
    }

    public void modifySense(int amount) {
        chr.addTraitExp(Stat.senseEXP, amount);
    }

    public void modifyCharm(int amount) {
        chr.addTraitExp(Stat.charmEXP, amount);
    }

    public void modifyHonor(int amount) {
        chr.addHonorExp(amount);
    }

    public void addStat(Stat stat, int amount) {
        chr.addStatAndSendPacket(stat, amount);
    }

    /** Swordie-pack alias: quest-ex key/value on the character (account quests go to the account). */
    public void setQuestEx(int questId, String key, String value) {
        chr.setQRValueByKey(questId, key, value);
    }

    public boolean openNodestone(int itemId) {
        return openNodeStone(itemId);
    }

    /** Takes mesos only when the character has enough; returns whether they were taken. */
    public boolean loseMeso(long amount) {
        if (amount < 0 || chr.getMoney() < amount) {
            return false;
        }
        chr.deductMoney(amount);
        return true;
    }

    public void gainMeso(long amount) {
        if (amount >= 0) {
            chr.addMoney(amount);
        } else {
            chr.deductMoney(-amount);
        }
    }

    public int getFace() {
        return chr.getAvatarData().getAvatarLook().getFace();
    }

    public int getHair() {
        return chr.getAvatarData().getAvatarLook().getHair();
    }

    public int getSkin() {
        return chr.getAvatarData().getAvatarLook().getSkin();
    }

    public boolean isFaceValid(int faceId) {
        return ItemData.isValidFaceHair(faceId, true);
    }

    public boolean isHairValid(int hairId) {
        return ItemData.isValidFaceHair(hairId, false);
    }

    public void cancelBuffStat(int skillId) {
        removeBuffBySkill(skillId);
    }

    /** Per-login script values (same store as addTempValue/getTempValue). */
    public Object getSessionValue(String key) {
        Object v = compatTempValues.get(key);
        return v == null ? 0 : v;
    }

    public void setSessionValue(String key, Object value) {
        compatTempValues.put(key, value);
    }

    /** Sol Erda energy ("breath"); 1000 = one Sol Erda (HexaMatrixConstants.solErdaStrengthMax). */
    public void gainSolErdaBreath(int amount) {
        chr.addSolErdaStrength(amount);
    }

    public void addBossClearLog(Object boss) {
        addPQLog("BossClear_" + compatValueToString(boss), 1, 0);
    }

    public int getBossClearLog(Object boss) {
        return getPQLog("BossClear_" + compatValueToString(boss));
    }

    /** Records a Mu Lung Dojo floor clear into the weekly ranking (282 dojo record store). */
    public void updateDoJangLog(int floor, int timeSec) {
        net.swordie.ms.world.tms282.dojo.DojoRankService.get().recordClear(chr, floor, timeSec);
    }

    /**
     * Grants one V core by id without consuming anything (legacy 265 matrix). The TMS 282 matrix has no loose cores
     * (gemstones grant V points), so there the call is refused; 282 gemstone scripts use {@link #openNodeStone(int)}.
     */
    public boolean gainVCore(int coreId, boolean showResult) {
        if (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.enabled()) {
            chat("新版V矩陣不使用核心道具，請改用核心寶石。");
            return false;
        }
        VCoreData vCoreData = VCore.getCore(coreId);
        if (vCoreData == null || chr.getInactiveMatrixCore().size() >= MatrixConstants.SLOT_MAX) {
            return false;
        }
        int skillID1 = vCoreData.getConnectSkills() != null && !vCoreData.getConnectSkills().isEmpty()
                ? vCoreData.getConnectSkills().get(0) : 0;
        MatrixCore core = new MatrixCore(chr.getId(), coreId, skillID1, 0, 0);
        chr.addMatrixCore(core);
        core.saveToSQL();
        chr.write(WvsContext.updateVMatrix(chr, false, 0, 0));
        if (showResult) {
            chr.write(WvsContext.nodeStoneResult(core));
        }
        return true;
    }

    public boolean gainVCore(int coreId) {
        return gainVCore(coreId, true);
    }

    /** A random skill core of the character's job (legacy 265 matrix only), without consuming anything. */
    public boolean gainRandVCore() {
        if (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.enabled()) {
            chat("新版V矩陣不使用核心道具，請改用核心寶石。");
            return false;
        }
        List<VCoreData> jobCores = new ArrayList<>();
        for (VCoreData v : VCore.getJobNodes()) {
            if (v.getJobs() != null && !v.getJobs().isEmpty() && Short.parseShort(v.getJobs().get(0)) == chr.getJob()) {
                jobCores.add(v);
            }
        }
        if (jobCores.isEmpty()) {
            return false;
        }
        return gainVCore(jobCores.get(Randomizer.nextInt(jobCores.size())).getCoreID(), true);
    }

    public boolean increaseTrunkCapacity(int amount) {
        if (amount <= 0 || getStorageSlotsLeftToAdd() < amount) {
            return false;
        }
        addStorageSlots(amount);
        return true;
    }

    /** Beauty album slot +1: 0 hair, 1 face (max 102 each, 3 free included), 2 skin (max 12). */
    public boolean increaseMannequin(int type) {
        net.swordie.ms.client.character.BeautySalon salon = chr.getBeautySalon();
        if (salon == null) {
            return false;
        }
        switch (type) {
            case 0 -> {
                if (salon.getHairSize() >= 102) return false;
                salon.setHairSize(salon.getHairSize() + 1);
            }
            case 1 -> {
                if (salon.getFaceSize() >= 102) return false;
                salon.setFaceSize(salon.getFaceSize() + 1);
            }
            case 2 -> {
                if (salon.getSkinSize() >= 12) return false;
                salon.setSkinSize(salon.getSkinSize() + 1);
            }
            default -> {
                return false;
            }
        }
        salon.saveToSQL();
        chr.write(UserLocal.beautyDataResult(salon));
        return true;
    }

    public String getAccountName() {
        return chr.getUser() == null ? "" : chr.getUser().getName();
    }

    /**
     * Selectable inventory expansion coupon (e.g. 選擇型欄位8格擴充券): grows inventory {@code type}
     * (1 equip, 2 consume, 3 install, 4 etc; 0 storage) by {@code amount} and spends one coupon, or does nothing.
     */
    public boolean useSelectableInventoryExpansionCoupon(int itemId, int type, int amount) {
        if (amount <= 0 || type < 0 || type > 4 || !hasItem(itemId)) {
            return false;
        }
        if (!isNativeInventoryExpansion()) {
            if (type == 0) {
                if (getStorageSlotsLeftToAdd() < amount) return false;
                addStorageSlots(amount);
            } else {
                Inventory inv = chr.getInventoryByType(InvType.getInvTypeByVal(type));
                if (inv == null || inv.getSlots() + amount > GameConstants.MAX_INVENTORY_SLOTS) return false;
                addInventorySlotsByInvType(amount, type);
            }
            consumeItem(itemId, 1);
            return true;
        }
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            synchronized (chr) {
                net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.requireEconomicAccess(chr);
                if (chr.getTradeRoom() != null || chr.getMiniRoom() != null) return false;
                int remaining = type == 0 ? getStorageSlotsLeftToAdd() : getSlotsLeftToAddByInvType((byte) type);
                if (remaining < amount) return false;
                int added = type == 0
                        ? net.swordie.ms.client.character.tms282.Tms282InventoryExpansion.growStorage(chr, amount)
                        : net.swordie.ms.client.character.tms282.Tms282InventoryExpansion.growInventory(chr, amount, type);
                if (added != amount) return false;
                consumeItem(itemId, 1);
                chr.saveToSQL();
                return true;
            }
        }
    }

    /** Damage skin with an explicit skin id (282 items carry info/damageSkinID the 265 tables do not know). */
    public boolean addDamageSkin(int itemID, int damageSkinID) {
        if (damageSkinID < 0) {
            return addDamageSkin(itemID);
        }
        DamageSkinType error = null;
        final List<DamageSkinSaveData> skins = chr.getDamageSkins().stream().filter(d -> d.getDamageSkinID() != 0).toList();
        if (skins.size() >= GameConstants.DAMAGE_SKIN_MAX_SIZE) {
            error = DamageSkinType.Res_Fail_SlotCount;
        } else if (chr.getDamageSkinByItemID(itemID) != null) {
            error = DamageSkinType.Res_Fail_AlreadyExist;
        }
        if (error != null) {
            chr.write(UserLocal.damageSkinSaveResult(DamageSkinType.Req_Reg, error, null));
            return false;
        }
        Quest q = chr.getQuestById(QuestConstants.DAMAGE_SKIN);
        if (q == null) {
            q = new Quest(chr.getId(), QuestConstants.DAMAGE_SKIN, QuestStatus.Started);
            chr.addQuest(q);
        }
        chr.consumeItem(itemID, 1);
        DamageSkinSaveData dssd = new DamageSkinSaveData(damageSkinID, itemID, false, StringData.getItemStringById(itemID));
        q.setQrValue(String.valueOf(damageSkinID));
        dssd.setCharId(chr.getId());
        dssd.setActivateTime(FileTime.currentTime());
        chr.addDamageSkin(dssd);
        chr.setActiveDamageSkin(dssd);
        chr.write(UserLocal.damageSkinSaveResult(DamageSkinType.Req_Reg, DamageSkinType.Res_Success, chr));
        chr.write(UserPacket.setActiveDamageSkin(chr));
        chr.write(WvsContext.questRecordMessage(q));
        return true;
    }

    // GM mail-reward broadcast of the script pack (consume_2430124): every online character gets a reward-box entry.

    private static int compatInt(Object o) {
        return o instanceof Number n ? n.intValue() : compatParseInt(o == null ? null : String.valueOf(o));
    }

    private static long compatLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        try {
            return o == null ? 0 : (long) Double.parseDouble(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<Char> onlineChars() {
        return new ArrayList<>(Server.get().getChars().values());
    }

    public int addItemReward(Object itemId, Object quantity, String title, Object days) {
        int id = compatInt(itemId), qty = Math.max(1, compatInt(quantity)), d = Math.max(1, compatInt(days));
        if (!isGm() || ItemData.getItemInfoByID(id) == null && !ItemConstants.isEquip(id)) return 0;
        List<Char> targets = onlineChars();
        targets.forEach(c -> c.sendRewardToChar(id, qty, 0, title, d));
        return targets.size();
    }

    public int addMesosReward(Object amount, String title, Object days) {
        long meso = compatLong(amount);
        int d = Math.max(1, compatInt(days));
        if (!isGm() || meso <= 0) return 0;
        List<Char> targets = onlineChars();
        targets.forEach(c -> c.sendRewardToChar(0, 0, meso, title, d));
        return targets.size();
    }

    public int addExpReward(Object amount, String title, Object days) {
        long exp = compatLong(amount);
        int d = Math.max(1, compatInt(days));
        if (!isGm() || exp <= 0) return 0;
        List<Char> targets = onlineChars();
        targets.forEach(c -> c.sendRewardToChar(0, 1, exp, title, d)); // quantity != 0 with meso -> Exp reward
        return targets.size();
    }

    public int addCSPointReward(Object amount, String title, Object days) {
        int points = compatInt(amount), d = Math.max(1, compatInt(days));
        if (!isGm() || points <= 0) return 0;
        List<Char> targets = onlineChars();
        for (Char c : targets) {
            if (c.getRewardSystem() == null) {
                c.initRewardSystem();
            }
            c.getRewardSystem().addReward(new net.swordie.ms.client.character.reward.RewardInfo(c,
                    net.swordie.ms.enums.reward.RewardItemType.MaplePoint, 0, 0, points, 0, 0, title,
                    FileTime.currentTime(), FileTime.fromDate(java.time.LocalDateTime.now().plusDays(d))));
        }
        return targets.size();
    }

    /** Typo'd {@code db.dalete(sql)} in the script pack. */
    public boolean dalete(String sql) {
        return delete(sql);
    }

    // ---- map binding: {@code map.getId()} and the field helpers of the MetaMS pack ----

    public class ScriptMapBinding extends HashMap<String, Object> {
        private final Field mapField;

        private ScriptMapBinding(Field mapField) {
            this.mapField = mapField;
            put("id", mapField == null ? 0 : mapField.getId());
        }

        public int getId() {
            return mapField == null ? 0 : mapField.getId();
        }

        public int getFieldId() {
            return getId();
        }

        public int getCharacterCount() {
            return mapField == null ? 0 : mapField.getChars().size();
        }

        /** {@code dropIndieItemsEx(?, x, y, ?, ?, ?, items)}: drops each item (from util.makeDrop) around (x, y). */
        public void dropIndieItemsEx(int unk, int x, int y, int unk2, int unk3, int unk4, Object items) {
            if (mapField == null || items == null) {
                return;
            }
            List<Item> list = new ArrayList<>();
            if (items instanceof Iterable<?> it) {
                for (Object o : it) if (o instanceof Item item) list.add(item);
            } else if (items instanceof Map<?, ?> m) { // Nashorn array converted to a map
                for (Object o : m.values()) if (o instanceof Item item) list.add(item);
            } else if (items instanceof Object[] arr) {
                for (Object o : arr) if (o instanceof Item item) list.add(item);
            }
            for (int i = 0; i < list.size(); i++) {
                Drop drop = new Drop(mapField.getNewObjectID());
                drop.setItem(list.get(i));
                Position from = new Position(x, y);
                Position to = new Position(x + (i - list.size() / 2) * 20, y);
                mapField.drop(drop, from, to, true);
            }
        }
    }

    // ---- ctx binding: item/context accessor of the original pack server ----

    public static class ScriptContextBinding {
        private final ScriptManagerImpl sm;
        private final int dataId;

        public ScriptContextBinding(ScriptManagerImpl sm, int dataId) {
            this.sm = sm;
            this.dataId = dataId;
        }

        public int getDataId() {
            return dataId;
        }

        public void consume() {
            sm.consumeItem(dataId);
        }

        public void consume(int amount) {
            sm.consumeItem(dataId, amount);
        }

        public long getExpForLevelUp() {
            int level = Math.max(1, Math.min(sm.getCurrentLevel(), GameConstants.charExp.length - 1));
            return GameConstants.charExp[level];
        }

        /** {@code ctx.getEvent()}: the caller's event. [eventfw] */
        public Object getEvent() {
            return sm.getEvent();
        }

        /** {@code ctx.getEvent(name)}: the running event {@code name} (caller's channel first). */
        public Object getEvent(String name) {
            int channel = sm.chr != null && sm.chr.getClient() != null ? sm.chr.getClient().getChannel() : 1;
            return net.swordie.ms.scripts.event.EventManager.get().find(name, channel);
        }

        /** {@code ctx.makeEvent(name, onlyOne, attachment)}. */
        public Object makeEvent(Object... args) {
            return sm.makeScriptEvent(args);
        }

        /** Running instances of event {@code name} on the caller's channel. [eventfw] */
        public List<Object> getEvents(String name) {
            int channel = sm.chr != null && sm.chr.getClient() != null ? sm.chr.getClient().getChannel() : 1;
            return new ArrayList<>(net.swordie.ms.scripts.event.EventManager.get().findAll(name, channel));
        }

        public String getEntityName() {
            return "";
        }

        public int getOption() {
            return 0;
        }

        /** Trait (personality) EXP needed to go from {@code level} to the next level. */
        public int getPersonalityExpByLevel(int level) {
            return GameConstants.getTraitExpByLevel(Math.max(0, level));
        }

        /** My-home furniture storage slots: the 282 home state has no slot-extension API yet, so nothing is granted. */
        public boolean addHomeStorageSlot(int count) {
            sm.chat("我的小屋家具欄位擴充尚未開放，本次沒有扣除道具。");
            return false;
        }

        public void setGuildCastleResearch(Object... args) {
        }

        public void addGuildCastleQuestLog(Object... args) {
        }

        public void setMyHomeName(Object... args) {
        }

        public boolean addHomeDrawing(Object... args) {
            return false;
        }
    }

    // ---- party binding: members exposed as their ScriptManagerImpl, matching pack usage ----

    public static class PartyBinding {
        private final Char owner;
        private final Integer departureLeader;

        public PartyBinding(Char owner) {
            this.owner = owner;
            this.departureLeader = null;
        }

        public PartyBinding(Char owner, int departureLeader) {
            this.owner = owner;
            this.departureLeader = departureLeader;
        }

        private net.swordie.ms.client.social.Party.Party party() {
            return owner.getParty();
        }

        public List<ScriptManagerImpl> getMembers() {
            List<ScriptManagerImpl> out = new ArrayList<>();
            if (party() == null) {
                return out;
            }
            for (net.swordie.ms.client.social.Party.PartyMember pm : party().getMembers()) {
                if (pm != null && pm.isOnline() && pm.getChr() != null) {
                    out.add(pm.getChr().getScriptManager());
                }
            }
            return out;
        }

        public List<ScriptManagerImpl> getLocalMembers() {
            List<ScriptManagerImpl> out = new ArrayList<>();
            if (party() == null) {
                return out;
            }
            for (net.swordie.ms.client.social.Party.PartyMember pm : party().getMembers()) {
                Char mc = pm == null ? null : pm.getChr();
                if (mc != null && pm.isOnline() && mc.getClient() != null && owner.getClient() != null
                        && mc.getClient().getChannel() == owner.getClient().getChannel()) {
                    out.add(mc.getScriptManager());
                }
            }
            return out;
        }

        public int getLeaderId() {
            return departureLeader != null ? departureLeader : party() == null ? -1 : party().getPartyLeaderID();
        }

        public ScriptManagerImpl getLeader() {
            if (party() == null || party().getPartyLeader() == null || party().getPartyLeader().getChr() == null) {
                return null;
            }
            return party().getPartyLeader().getChr().getScriptManager();
        }

        public int getMembersCount() {
            return party() == null ? 0 : getMembers().size();
        }
        /** Online members on {@code mapId} with level in [minLevel, maxLevel] (MetaMS party check). [eventfw] */
        public int getMembersCount(int mapId, int minLevel, int maxLevel) {
            int n = 0;
            if (party() == null) {
                return 0;
            }
            for (net.swordie.ms.client.social.Party.PartyMember pm : party().getMembers()) {
                Char mc = pm == null ? null : pm.getChr();
                if (mc != null && pm.isOnline() && mc.getField() != null && mc.getField().getId() == mapId
                        && mc.getLevel() >= minLevel && mc.getLevel() <= maxLevel) {
                    n++;
                }
            }
            return n;
        }
    }

    // ---- [eventfw] MetaMS script events: engine side ---------------------------------------------------------------

    static {
        net.swordie.ms.scripts.event.EventManager.get().setHost(new EventBridge());
    }

    /**
     * Builds the Nashorn engine of an event ({@code data/scripts/event/<name>.js}) with the globals MetaMS event
     * scripts use: {@code event}, {@code util}, {@code db}, {@code ctx}, {@code console}, {@code require}.
     */
    public static final class EventBridge implements net.swordie.ms.scripts.event.EventScriptHost {

        @Override
        public Invocable load(net.swordie.ms.scripts.event.EventInstance instance, java.nio.file.Path file)
                throws ScriptException {
            if (!GAME_SCRIPTS_ENABLED) throw new ScriptException("Game scripts are disabled in the login-only server");
            ScriptManagerImpl host = new ScriptManagerImpl((Field) null);
            ScriptEngine se = nashornScriptEngineFactory.getScriptEngine("--language=es6");
            Bindings g = se.getBindings(javax.script.ScriptContext.ENGINE_SCOPE);
            g.put("event", instance);
            g.put("util", host.new ScriptUtilBinding());
            g.put("db", host);
            g.put("ctx", new ScriptContextBinding(host, 0));
            g.put("__scriptRequire", host.new ScriptRequire(se));
            String source;
            try {
                source = host.normalizeScriptSource(Util.readFile(file.toString(), StandardCharsets.UTF_8),
                        JAVASCRIPT_SCRIPT_ENGINE_EXTENSION);
            } catch (IOException e) {
                throw new ScriptException(e);
            }
            se.put(ScriptEngine.FILENAME, "event/" + file.getFileName());
            se.eval(source);
            return (Invocable) se;
        }

        @Override
        public Object player(Char chr) {
            return chr == null ? null : chr.getScriptManager();
        }

        @Override
        public void runScriptAsync(Object player, String path) {
            if (!GAME_SCRIPTS_ENABLED) return;
            if (player instanceof ScriptManagerImpl psm) {
                service.execute(() -> {
                    try {
                        psm.runScript(path);
                    } catch (RuntimeException e) {
                        System.out.printf("[Event] script %s failed: %s%n", path, e);
                    }
                });
            }
        }

        @Override
        public Object party(Char chr) {
            return chr == null || chr.getParty() == null ? null : new PartyBinding(chr);
        }

        @Override
        public Object partyDeparture(Char chr) {
            return chr == null || chr.getParty() == null ? null : new PartyBinding(chr, chr.getParty().getPartyLeaderID());
        }

        /** Script arrays from the caller's engine become Java arrays ({@code attachment[i]}, {@code .length}). */
        @Override
        public Object importValue(Object value) {
            if (value instanceof org.openjdk.nashorn.api.scripting.ScriptObjectMirror m && m.isArray()) {
                Object[] out = new Object[m.size()];
                for (int i = 0; i < out.length; i++) {
                    out[i] = m.getSlot(i);
                }
                return out;
            }
            return value;
        }
    }

    /**
     * Hotfix 7d (2026-10-05): the server-side dialog's speaker header, as on the first dialog of a fresh session.
     * stop() (NpcScriptInfo.reset) leaves speakerType 0, so after any earlier script (e.g. a field-entry script) the
     * newbie dialog went out as LP0960 objectId 0 / speakerType 0 / no override and the 282 client crashed at once
     * (formal-channel-1791182392941-476 send-188 vs -1791168552687-3133 send-144: only this byte differs).
     * Kept at the end of the file so the nested classes keep their line numbers (smallest jar overlay).
     */
    private void resetJavaDialogSpeaker() {
        setSpeakerID(0);
        getNpcScriptInfo().setSpeakerType((byte) 4);
        getNpcScriptInfo().setColor((byte) 0);
    }
}
