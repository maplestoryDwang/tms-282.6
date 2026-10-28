using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;

// 2026-10-08: the 282.5 client ends itself (~70 s, access violation at the world list) when its login server is a plain
// 127.0.0.1 / LAN address; at the official address 202.80.104.26 it runs (research/tms2823/packet-audit/
// local-target-controls-20260922.md; the main launcher does the same). While the game runs this bridge:
//   - puts 202.80.104.26/32 on the Windows loopback interface (the one owning 127.0.0.1/8; SkipAsSource, ActiveStore,
//     24 h lifetime) - nothing outside this computer can reach it;
//   - relays 202.80.104.26:{8484, 8585-8588} to the server on 127.0.0.1 (same ports). The server itself still listens
//     on 127.0.0.1 only and sees every connection as coming from 127.0.0.1.
// Dispose stops the relay and removes the address if this bridge added it (an address that was already there - the main
// launcher's alias - is used and left alone). The launcher runs as administrator, which New-NetIPAddress needs.
internal sealed class LoopbackBridge : IDisposable
{
    public const string AliasHost = "202.80.104.26";
    public static readonly int[] Ports = { 8484, 8585, 8586, 8587, 8588 };
    const int LifetimeHours = 24;

    readonly List<TcpListener> listeners = new List<TcpListener>();
    readonly List<Socket> sockets = new List<Socket>();
    bool addedAlias;
    volatile bool closed;

    LoopbackBridge() { }

    public static LoopbackBridge Open()
    {
        var bridge = new LoopbackBridge();
        try {
            bridge.addedAlias = EnsureAlias();
            foreach (int port in Ports) {
                var listener = new TcpListener(IPAddress.Parse(AliasHost), port);
                listener.Server.ExclusiveAddressUse = true;
                // a just-added address can take a moment before it can be bound
                for (int attempt = 0; ; attempt++) {
                    try { listener.Start(16); break; }
                    catch (SocketException e) {
                        if (e.SocketErrorCode != SocketError.AddressNotAvailable || attempt >= 40) throw;
                        Thread.Sleep(250);
                    }
                }
                bridge.listeners.Add(listener);
                var thread = new Thread(() => bridge.AcceptLoop(listener, port)) { IsBackground = true, Name = "bridge-" + port };
                thread.Start();
            }
            return bridge;
        } catch {
            bridge.Dispose();
            throw;
        }
    }

    public bool Running { get { return !closed && listeners.Count == Ports.Length; } }

    void AcceptLoop(TcpListener listener, int port)
    {
        while (!closed) {
            Socket client;
            try { client = listener.AcceptSocket(); }
            catch { return; } // closed
            var thread = new Thread(() => Serve(client, port)) { IsBackground = true, Name = "bridge-conn-" + port };
            thread.Start();
        }
    }

    void Serve(Socket client, int port)
    {
        Socket server = null;
        try {
            client.NoDelay = true;
            server = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp) { NoDelay = true };
            var connect = server.BeginConnect(IPAddress.Loopback, port, null, null);
            if (!connect.AsyncWaitHandle.WaitOne(5000)) throw new SocketException((int)SocketError.TimedOut);
            server.EndConnect(connect);
            lock (sockets) { if (closed) throw new ObjectDisposedException("bridge"); sockets.Add(client); sockets.Add(server); }
            var up = new Thread(() => Pump(client, server)) { IsBackground = true };
            up.Start();
            Pump(server, client);
            up.Join();
        } catch {
            // the server is not up or the bridge is closing: drop this connection
        } finally {
            Close(client);
            Close(server);
            lock (sockets) { sockets.Remove(client); if (server != null) sockets.Remove(server); }
        }
    }

    static void Pump(Socket from, Socket to)
    {
        var buffer = new byte[16384];
        try {
            int n;
            while ((n = from.Receive(buffer)) > 0) {
                int sent = 0;
                while (sent < n) sent += to.Send(buffer, sent, n - sent, SocketFlags.None);
            }
            to.Shutdown(SocketShutdown.Send);
        } catch {
            Close(from);
            Close(to);
        }
    }

    static void Close(Socket s)
    {
        if (s == null) return;
        try { s.Close(); } catch { }
    }

    public void Dispose()
    {
        if (closed) return;
        closed = true;
        foreach (var l in listeners) { try { l.Stop(); } catch { } }
        listeners.Clear();
        lock (sockets) { foreach (var s in sockets.ToArray()) Close(s); sockets.Clear(); }
        if (addedAlias) {
            addedAlias = false;
            try { RemoveAlias(); } catch { /* the 24 h lifetime and a reboot remove it as well */ }
        }
    }

    // ------------------------------------------------------------------ the address

    const string FindLoopback =
        "$ErrorActionPreference='Stop';" +
        "$lo=@(Get-NetIPAddress -AddressFamily IPv4 -IPAddress 127.0.0.1 | Where-Object { $_.PrefixOrigin -eq 'WellKnown' -and $_.PrefixLength -eq 8 });" +
        "if($lo.Count -ne 1){throw 'loopback interface not found'};$idx=$lo[0].InterfaceIndex;" +
        "if((Get-NetIPInterface -InterfaceIndex $idx -AddressFamily IPv4).Dhcp -ne 'Disabled'){throw 'loopback interface has DHCP'};";

    /** True when this call added the address; false when it was already on the loopback interface. */
    static bool EnsureAlias()
    {
        string output = RunPowerShell(FindLoopback +
            "$have=@(Get-NetIPAddress -AddressFamily IPv4 -IPAddress '" + AliasHost + "' -ErrorAction SilentlyContinue);" +
            "if($have.Count){ if($have.Count -eq 1 -and $have[0].InterfaceIndex -eq $idx){'exists';exit 0}; throw 'address is on another interface' };" +
            "$t=New-TimeSpan -Hours " + LifetimeHours + ";" +
            "New-NetIPAddress -InterfaceIndex $idx -AddressFamily IPv4 -IPAddress '" + AliasHost + "' -PrefixLength 32 -SkipAsSource $true -PolicyStore ActiveStore -ValidLifetime $t -PreferredLifetime $t | Out-Null;" +
            "'added'");
        if (output == "added") return true;
        if (output == "exists") return false;
        throw new InvalidOperationException("無法建立本機位址（" + output + "）。");
    }

    static void RemoveAlias()
    {
        RunPowerShell(FindLoopback +
            "@(Get-NetIPAddress -AddressFamily IPv4 -IPAddress '" + AliasHost + "' -PolicyStore ActiveStore -ErrorAction SilentlyContinue | " +
            "Where-Object { $_.InterfaceIndex -eq $idx -and $_.PrefixLength -eq 32 -and $_.SkipAsSource }) | Remove-NetIPAddress -Confirm:$false;'removed'");
    }

    static string RunPowerShell(string script)
    {
        string encoded = Convert.ToBase64String(Encoding.Unicode.GetBytes(script));
        var info = new ProcessStartInfo("powershell.exe", "-NoProfile -NonInteractive -ExecutionPolicy Bypass -EncodedCommand " + encoded) {
            UseShellExecute = false, CreateNoWindow = true, RedirectStandardOutput = true, RedirectStandardError = true,
            StandardOutputEncoding = Encoding.UTF8, StandardErrorEncoding = Encoding.UTF8 };
        using (var p = Process.Start(info)) {
            var err = p.StandardError.ReadToEndAsync();
            string output = p.StandardOutput.ReadToEnd().Trim();
            if (!p.WaitForExit(30000)) { try { p.Kill(); } catch { } throw new InvalidOperationException("設定本機位址逾時。"); }
            string error = err.Result.Trim();
            if (p.ExitCode != 0) throw new InvalidOperationException("無法設定本機位址：" + (error.Length > 200 ? error.Substring(0, 200) : error));
            return output;
        }
    }
}
