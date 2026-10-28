package net.swordie.ms.connection.api;

import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import net.swordie.ms.connection.api.launcher.LauncherActionRegistry;
import net.swordie.ms.connection.api.launcher.LauncherContext;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One JSON request on stdin, one JSON response on stdout; never starts a server.
 * <p>
 * The actions are {@link LauncherActionRegistry#standard()}, the same table HttpAuthApi serves, run with the CLI
 * channel: no sessions or rate limits; SERVER_ONLY actions answer {@code serverOnly} and SERVER_WHEN_RUNNING ones
 * {@code serverRunning} while 127.0.0.1:8484 is listening (reset is one: only the server sees the accounts it holds
 * in memory); only the local 265 database is used (265_test with {@code -Dtms.launcher.testDb=true}). Output keeps
 * every field it had (TmsAccountLauncher and cat-launcher 1.0.0 read ok / code / recoveryCode) and account outcomes
 * keep the 09-22 codes (login.maintenance answers {@code banned} + reason + message); new situations only add fields,
 * or the transport codes of the P5a rules.
 */
public final class LocalAccountCli {
    private static final ObjectMapper JSON = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    /** stdout uses the console code page while readers decode UTF-8: non-ASCII text (maintenance message) as \\uXXXX. */
    private static final ObjectMapper OUT = JsonMapper.builder().enable(JsonWriteFeature.ESCAPE_NON_ASCII).build();

    public static void main(String[] args) {
        PrintStream response = System.out;
        // Existing pool/bootstrap code writes to the console. Keep IPC and secrets out of logs.
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
        Map<String, Object> result;
        try {
            if (args.length != 0) throw new IllegalArgumentException();
            byte[] input = System.in.readNBytes(8193);
            if (input.length > 8192) throw new IllegalArgumentException();
            JsonNode request = JSON.readTree(input);
            if (request == null || !request.isObject()) throw new IllegalArgumentException();
            result = execute(request);
        } catch (IllegalArgumentException e) {
            result = result(false, "invalidInput");
        } catch (Exception e) {
            result = result(false, "unavailable");
        }
        try { response.println(OUT.writeValueAsString(result)); }
        catch (Exception ignored) { response.println("{\"ok\":false,\"code\":\"unavailable\"}"); }
    }

    /** One request through the shared registry with a fresh CLI context. */
    public static Map<String, Object> execute(JsonNode request) throws Exception {
        return execute(LauncherContext.cli(), request);
    }

    public static Map<String, Object> execute(LauncherContext ctx, JsonNode request) throws Exception {
        return LauncherActionRegistry.standard().dispatch(ctx, request).toMap();
    }

    private static Map<String, Object> result(boolean ok, String code) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", ok);
        result.put("code", code);
        return result;
    }
}
