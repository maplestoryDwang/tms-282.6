import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.tms282.mail.MailLoginHook;
import net.swordie.ms.world.tms282.newbie.NewbieFlowService;

import javax.script.ScriptException;
import java.nio.file.Path;

/** Offline: run against the packaged JAR; any real database connection is refused. */
public class LoginOnlyScriptsCheck {
    public static void main(String[] args) throws Exception {
        System.setProperty("tms282.tests.rejectRealDatabase", "true");
        check(!ScriptManagerImpl.GAME_SCRIPTS_ENABLED, "game scripts disabled");
        Char chr = new Char();
        ScriptManagerImpl sm = chr.getScriptManager();
        for (ScriptType type : ScriptType.values()) {
            check(!sm.startScript(chr, 9000000, "missing-login-script", type), "no script/fallback: " + type);
            check(!sm.startScript(chr, 9000000, 1, "missing-login-script", type), "no object script: " + type);
            check(!sm.start(chr, 9000000, 1, "missing-login-script", type), "no direct script: " + type);
        }
        check(!sm.startJavaDialog(chr, 9000000, "login-dialog", ignored -> {
            throw new AssertionError("dialog scheduled");
        }), "Java dialog never scheduled");
        sm.runScript("missing-content-script");
        check(sm.makeEvent("missing-event") == null, "no event created");
        new ScriptManagerImpl.EventBridge().runScriptAsync(sm, "missing-event-script");
        try {
            new ScriptManagerImpl.EventBridge().load(null, Path.of("missing-event.js"));
            throw new AssertionError("event engine accepted a script");
        } catch (ScriptException e) {
            check(e.getMessage().contains("disabled"), "event refused before file/engine loading");
        }
        Field field = new Field(100000000);
        field.setFieldScript("missing-field-script");
        field.setOnFirstUserEnter("missing-first-enter-script");
        field.setOnUserEnter("missing-user-enter-script");
        field.startFieldScript();
        field.execFirstUserEnterScript(chr);
        field.execUserEnterScript(chr);
        check(!field.isUserFirstEnter(), "first-entry hook did not claim or schedule a script");
        MailLoginHook.onFirstFieldEntry(chr);
        System.setProperty("tms282.newbie.enabled", "true");
        NewbieFlowService.get().beforeLoginField(chr, null);
        NewbieFlowService.get().onLoginEntry(chr);
        check(!NewbieFlowService.get().replacesEntryScript(chr), "onboarding stays disabled for direct JVM startup");
        check(sm.getScripts().isEmpty() && sm.getLastActiveScriptType() == ScriptType.None,
                "no script state or fallback dialog created");
        check(!DatabaseManager.isInitialized(), "no database connection");
        System.out.println("PASS: login-only scripts, dialogs and field-entry hooks remain inactive (offline)");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
