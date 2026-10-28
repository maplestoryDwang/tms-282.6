using System;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.NetworkInformation;



using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using System.Web.Script.Serialization;

// This helper only starts the selected, verified local 282.5 game. It performs no injection or network setup.
// 2026-10-08: the game is pointed at the loopback alias 202.80.104.26:8484 that the launcher sets up and relays to the
// server on 127.0.0.1 (LoopbackBridge.cs); the helper only checks that the relay listens there.
internal static class TmsLocalStarter
{
    const string GameHash = "DEF89CF3C51B5D196B90776D64A68898D722C26F8CE14F97F8C3930E9C4CFC27";
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
            using (var file = File.OpenRead(game)) using (var hash = SHA256.Create())
                if (BitConverter.ToString(hash.ComputeHash(file)).Replace("-", "") != GameHash) return Fail("versionMismatch");
            if (Process.GetProcessesByName("MapleStory").Length != 0) return Fail("gameRunning");
            if (!Array.Exists(IPGlobalProperties.GetIPGlobalProperties().GetActiveTcpListeners(),
                    e => e.Port == 8484 && e.Address.Equals(IPAddress.Parse(fields[2])))) return Fail("localEndpointRequired");
            var info = new StartupInfo(); info.cb = Marshal.SizeOf(typeof(StartupInfo));
            ProcessInfo child;
            var command = new StringBuilder("\"" + game + "\" " + fields[2] + " 8484 BeanFun " + fields[3] + " " + fields[4]);
            if (!CreateProcess(game, command, IntPtr.Zero, IntPtr.Zero, false, 4, IntPtr.Zero, Path.GetDirectoryName(game), ref info, out child))
                return Fail("launchFailed");
            command.Clear(); Array.Clear(fields, 0, fields.Length);
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
        } catch { return Fail("launchFailed"); }
    }
    static int Fail(string code) { Console.WriteLine(new JavaScriptSerializer().Serialize(new { ok = false, code = code })); return 2; }
}
