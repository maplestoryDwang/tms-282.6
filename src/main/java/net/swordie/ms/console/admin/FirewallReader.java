package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Reads the inbound Windows Firewall rules that matter for the server - READ ONLY: one {@code powershell.exe} run of
 * Get-NetFirewallApplicationFilter / Get-NetFirewallRule / Get-NetFirewallPortFilter / Get-NetFirewallAddressFilter
 * (only Get- cmdlets; nothing is ever added, changed or removed). Rules for java(w).exe and rules whose name mentions
 * TMS / Maple / 8484 / 8600 / Radmin are listed.
 */
final class FirewallReader {
    static final int TIMEOUT_SECONDS = 60;
    /** Only Get-* cmdlets. Output: a JSON array (UTF-8). */
    static final String SCRIPT = String.join("\n",
            "$ErrorActionPreference='SilentlyContinue'",
            "[Console]::OutputEncoding=[Text.Encoding]::UTF8",
            "$java=@{}",
            "foreach($f in Get-NetFirewallApplicationFilter -All){ if([string]$f.Program -match 'javaw?\\.exe$'){ $java[$f.InstanceID]=[string]$f.Program } }",
            "$out=New-Object System.Collections.ArrayList",
            "foreach($r in Get-NetFirewallRule -Direction Inbound){",
            "  $named=([string]$r.DisplayName) -match 'TMS|Maple|8484|8600|Radmin'",
            "  if(-not $java.ContainsKey($r.Name) -and -not $named){ continue }",
            "  $pf=$r | Get-NetFirewallPortFilter",
            "  $af=$r | Get-NetFirewallAddressFilter",
            "  $prog=$java[$r.Name]; if(-not $prog){ $prog=[string]($r | Get-NetFirewallApplicationFilter).Program }",
            "  [void]$out.Add([pscustomobject]@{name=[string]$r.DisplayName; enabled=[string]$r.Enabled; action=[string]$r.Action;",
            "    profile=[string]$r.Profile; program=$prog; protocol=[string]$pf.Protocol; localPort=(@($pf.LocalPort) -join ',');",
            "    remote=(@($af.RemoteAddress) -join ',')})",
            "}",
            "ConvertTo-Json -InputObject @($out) -Compress -Depth 3");

    record Rule(String name, String enabled, String action, String profile, String program, String protocol,
                String localPort, String remote) {
    }

    private FirewallReader() {
    }

    static List<Rule> read() throws IOException, InterruptedException {
        String encoded = Base64.getEncoder().encodeToString(SCRIPT.getBytes(StandardCharsets.UTF_16LE));
        byte[] out = output(List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                "-EncodedCommand", encoded), TIMEOUT_SECONDS);
        return parse(out);
    }

    /**
     * Runs {@code command} and returns its stdout; IOException after {@code timeoutSeconds} - the limit holds even
     * when the process hangs without writing or closing stdout (it is then ended by its own handle).
     */
    static byte[] output(List<String> command, int timeoutSeconds) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        p.getOutputStream().close();
        // stdout is read on its own thread so the time limit holds even when PowerShell hangs without output
        CompletableFuture<byte[]> output = CompletableFuture.supplyAsync(() -> {
            try (InputStream in = p.getInputStream()) {
                return readAll(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, runnable -> {
            Thread t = new Thread(runnable, "firewall-reader-stdout");
            t.setDaemon(true);
            t.start();
        });
        byte[] out;
        try {
            if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new IOException("PowerShell 讀取防火牆逾時（" + timeoutSeconds + " 秒），已結束該 PowerShell");
            }
            out = output.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new IOException("PowerShell 讀取防火牆逾時", e);
        } catch (ExecutionException e) {
            throw e.getCause() instanceof UncheckedIOException u ? u.getCause() : new IOException(e.getCause());
        } finally {
            if (p.isAlive()) {
                // only the powershell.exe started above (by its handle, never by name); the reader thread then gets EOF
                p.descendants().forEach(ProcessHandle::destroyForcibly);
                p.destroyForcibly();
            }
        }
        return out;
    }

    static List<Rule> parse(byte[] out) throws IOException {
        String text = new String(out, StandardCharsets.UTF_8).strip();
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        List<Rule> rules = new ArrayList<>();
        if (text.isEmpty()) {
            return rules;
        }
        JsonNode n = HttpAdminGateway.JSON.readTree(text);
        if (n.isObject()) {
            rules.add(rule(n));
        } else {
            for (JsonNode r : n) {
                rules.add(rule(r));
            }
        }
        return rules;
    }

    private static Rule rule(JsonNode r) {
        return new Rule(AdminUi.text(r, "name"), AdminUi.text(r, "enabled"), AdminUi.text(r, "action"), AdminUi.text(r, "profile"),
                AdminUi.text(r, "program"), AdminUi.text(r, "protocol"), AdminUi.text(r, "localPort"), AdminUi.text(r, "remote"));
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        in.transferTo(b);
        return b.toByteArray();
    }
}
