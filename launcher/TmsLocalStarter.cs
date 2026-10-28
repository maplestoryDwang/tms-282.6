using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.NetworkInformation;

using System.Runtime.InteropServices;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Web.Script.Serialization;

// This helper only starts the selected local game. It performs no injection or network setup of its own.
// 2026-10-08: the game is pointed at the loopback alias 202.80.104.26:8484 that the launcher sets up and relays to the
// server on 127.0.0.1 (LoopbackBridge.cs); the helper only checks that the relay listens there.
// 2026-10-28: the fixed 282.5 client hash check was removed - the launcher is used with a different client build here.
// 2026-10-28: when LocaleBridge (launcher side) resolved a Locale Remulator profile, it passes LRProc.exe and the
// profile GUID in two environment variables and the helper starts the game through LRProc.exe instead of calling
// CreateProcess itself, so the client sees a Taiwan code page (950) even on a Simplified Chinese system.
internal static class TmsLocalStarter
{
    // Keep in sync with LocaleBridge.ProcVariable / GuidVariable / AdminVariable; the helper is compiled on its own.
    const string LocaleProcVariable = "TMS_LRPROC";
    const string LocaleGuidVariable = "TMS_LRGUID";
    const string LocaleAdminVariable = "TMS_LRADMIN";

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    struct StartupInfo {
        public int cb; public string reserved, desktop, title;
        public int x, y, xSize, ySize, xChars, yChars, fill, flags;
        public short show, reserved2; public IntPtr reserved3, stdIn, stdOut, stdErr;
    }
    [StructLayout(LayoutKind.Sequential)] struct ProcessInfo { public IntPtr process, thread; public int pid, tid; }
    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    static extern bool CreateProcess(string app, StringBuilder args, IntPtr a, IntPtr b, bool inherit, uint flags, IntPtr env, string dir, ref StartupInfo info, out ProcessInfo process);
    [DllImport("kernel32.dll")] static extern uint ResumeThread(IntPtr thread);
    [DllImport("kernel32.dll")] static extern bool TerminateProcess(IntPtr process, uint code);
    [DllImport("kernel32.dll")] static extern bool CloseHandle(IntPtr handle);

    static int Main()
    {
        Console.OutputEncoding = new UTF8Encoding(false);
        try {
            string[] fields = new string[8];
            using (var input = new StreamReader(Console.OpenStandardInput(), new UTF8Encoding(false)))
                for (int i = 0; i < fields.Length; i++) fields[i] = input.ReadLine();
            if (fields[0] != "v1" || Array.Exists(fields, s => s == null) || fields[2] != "202.80.104.26"
                || fields[5] != "0" || fields[6] != "" || fields[7] != ""
                || !Regex.IsMatch(fields[3], "\\A[A-Za-z0-9]{3,24}\\z") || !Regex.IsMatch(fields[4], "\\A[A-Za-z0-9]{8,32}\\z"))
                return Fail("invalidInput");
            string game = fields[1];
            if (!Path.IsPathRooted(game) || !String.Equals(Path.GetFileName(game), "MapleStory.exe", StringComparison.OrdinalIgnoreCase) || !File.Exists(game))
                return Fail("gameMissing");
            string image = Path.GetFileNameWithoutExtension(game);
            if (Process.GetProcessesByName(image).Length != 0) return Fail("gameRunning");
            if (!Array.Exists(IPGlobalProperties.GetIPGlobalProperties().GetActiveTcpListeners(),
                    e => e.Port == 8484 && e.Address.Equals(IPAddress.Parse(fields[2])))) return Fail("localEndpointRequired");
            string arguments = fields[2] + " 8484 BeanFun " + fields[3] + " " + fields[4];
            string localeProc = Environment.GetEnvironmentVariable(LocaleProcVariable);
            string localeGuid = Environment.GetEnvironmentVariable(LocaleGuidVariable);
            bool localeAdmin = String.Equals(Environment.GetEnvironmentVariable(LocaleAdminVariable), "1", StringComparison.Ordinal);
            int result = (!String.IsNullOrEmpty(localeProc) && !String.IsNullOrEmpty(localeGuid) && File.Exists(localeProc))
                ? StartWithLocale(localeProc, localeGuid, localeAdmin, game, image, arguments)
                : StartSuspended(game, arguments);
            Array.Clear(fields, 0, fields.Length);
            return result;
        } catch { return Fail("launchFailed"); }
    }

    /** Direct start, suspended first so the process is fully configured before it runs. */
    static int StartSuspended(string game, string arguments)
    {
        var info = new StartupInfo(); info.cb = Marshal.SizeOf(typeof(StartupInfo));
        ProcessInfo child;
        var command = new StringBuilder("\"" + game + "\" " + arguments);
        if (!CreateProcess(game, command, IntPtr.Zero, IntPtr.Zero, false, 4, IntPtr.Zero, Path.GetDirectoryName(game), ref info, out child))
            return Fail("launchFailed");
        command.Clear();
        bool running = false;
        try {
            if (ResumeThread(child.thread) == uint.MaxValue) return Fail("launchFailed");
            running = true;
            Console.WriteLine(new JavaScriptSerializer().Serialize(new { ok = true, pid = child.pid }));
            return 0;
        } finally {
            if (!running) TerminateProcess(child.process, 1);
            CloseHandle(child.thread); CloseHandle(child.process);
        }
    }

    /**
     * Start through Locale Remulator, exactly the way LREditor's "Run" button does it
     * (LREditor/LREditor.xaml.cs, Button_Run_Click):
     *
     *   LRProc.exe <guid> "<game>" <args>      (no quotes around the guid)
     *   UseShellExecute = true, WorkingDirectory = game folder, Verb = "runas" when the profile says RunAsAdmin
     *
     * UseShellExecute=false is NOT equivalent: LRProc uses DetourCreateProcessWithDllExW and returns 0 even when it
     * fails, so the shell context (and the elevation it performs) is what makes the injection work.
     * LRProc also returns immediately, so the game pid is found by diffing the image list before/after.
     */
    static int StartWithLocale(string localeProc, string localeGuid, bool localeAdmin, string game, string image, string arguments)
    {
        HashSet<int> before = new HashSet<int>();
        foreach (var running in Process.GetProcessesByName(image)) { before.Add(running.Id); running.Dispose(); }
        var info = new ProcessStartInfo(localeProc, localeGuid + " \"" + game + "\" " + arguments) {
            UseShellExecute = true, WorkingDirectory = Path.GetDirectoryName(game)
        };
        if (localeAdmin) info.Verb = "runas";
        using (var bridge = Process.Start(info)) {
            if (bridge == null) return Fail("localeFailed");
            if (!bridge.WaitForExit(8000)) { try { bridge.Kill(); } catch { } return Fail("localeFailed"); }
            if (bridge.ExitCode != 0) return Fail("localeFailed");
        }
        int pid = 0;
        for (int attempt = 0; attempt < 60 && pid == 0; attempt++) {
            foreach (var started in Process.GetProcessesByName(image)) {
                if (!before.Contains(started.Id)) pid = started.Id;
                started.Dispose();
                if (pid != 0) break;
            }
            if (pid == 0) Thread.Sleep(200);   // up to ~12 s for the client to show up
        }
        if (pid == 0) return Fail("localeFailed");   // LRProc 回報成功但遊戲沒出現：多半是 LR 本身失效
        Console.WriteLine(new JavaScriptSerializer().Serialize(new { ok = true, pid = pid }));
        return 0;
    }

    static int Fail(string code) { Console.WriteLine(new JavaScriptSerializer().Serialize(new { ok = false, code = code })); return 2; }
}
