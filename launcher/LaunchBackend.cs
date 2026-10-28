using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using System.Web.Script.Serialization;

internal static class LaunchBackend
{
    // 2026-10-28: the client hash gate was removed on purpose - the local client is 282.6, not the pinned
    // 282.5 build, so the launcher no longer compares MapleStory.exe against a fixed SHA-256. The starter
    // helper is still verified (StarterHash, rewritten by build.ps1 after every helper build).
    const string StarterHash = "E80A7983C83072BED1B2691296729F606BDE38D85BCFE6AAD6B3B8F32B5BCD10";
    const string LocalHost = "127.0.0.1";
    // 2026-10-08: the game connects to the loopback alias, relayed to the server on 127.0.0.1 (LoopbackBridge)
    const string GameHost = LoopbackBridge.AliasHost;
    static LoopbackBridge bridge;
    static readonly object bridgeLock = new object();

    /** Alias + relay up (kept while the game runs; StopBridge when it ends or the launcher closes). */
    static void StartBridge()
    {
        lock (bridgeLock) {
            if (bridge != null && bridge.Running) return;
            if (bridge != null) bridge.Dispose();
            bridge = null;
            try { bridge = LoopbackBridge.Open(); }
            catch (InvalidOperationException) { throw; }
            catch (Exception e) { throw Error("無法建立本機相容連線（" + e.Message + "）。"); }
        }
    }

    public static void StopBridge()
    {
        lock (bridgeLock) {
            if (bridge != null) bridge.Dispose();
            bridge = null;
        }
    }
    static string Folder { get { return AppDomain.CurrentDomain.BaseDirectory; } }
    static InvalidOperationException Error(string message) { return new InvalidOperationException(message); }

    public static async Task<int> LoginAndStartAsync(string gamePath, string username, string password)
    {
        ValidateCredentials(username, password);
        if (String.IsNullOrWhiteSpace(gamePath) || !Path.IsPathRooted(gamePath)
            || !String.Equals(Path.GetFileName(gamePath), "MapleStory.exe", StringComparison.OrdinalIgnoreCase)
            || !File.Exists(gamePath)) throw Error("請選擇遊戲資料夾內的 MapleStory.exe。");
        gamePath = Path.GetFullPath(gamePath);
        if (Process.GetProcessesByName("MapleStory").Length != 0) throw Error("遊戲已在執行，請先關閉遊戲。");
        byte[] starter;
        try {
            starter = await Task.Run(() => VerifiedFile("TmsLocalStarter.exe", StarterHash)).ConfigureAwait(false);
        } catch (InvalidOperationException) { throw; }
        catch { throw Error("無法讀取遊戲或啟動元件，請確認檔案完整。"); }
        await PingServerAsync().ConfigureAwait(false);
        await Task.Run(() => StartBridge()).ConfigureAwait(false);
        // The 282.5 game authenticates BeanFun account/password via CP0074; HTTP tokens are not game launch tokens.
        return await AuthenticateAndStartAsync(gamePath, username, password,
            body => RequestAsync("/api/v2/account", body), input => RunStarterAsync(starter, input)).ConfigureAwait(false);
    }

    public static async Task<string> PingServerAsync()
    {
        var status = Parse(await RequestAsync("/api/v2/launcher", "{\"action\":\"status\"}").ConfigureAwait(false));
        if (!True(status, "ok") || !True(status, "running")) throw Error("伺服器尚未啟動，請先開啟 server 的 launch.bat。");
        // 2026-10-28: the server reports the build it is configured for ("282.5" / "282.6"). Accept any TMS 282.x
        // instead of pinning one minor, so switching the server build does not require a rebuilt launcher.
        string protocol = Text(status, "protocol");
        if (protocol == null || !protocol.StartsWith("282.", StringComparison.Ordinal)) throw Error("目前連線的伺服器不是 282.x，請啟動此 server 資料夾。");
        return protocol;
    }

    public static async Task<string> RegisterAsync(string username, string password, string pin)
    {
        ValidateRegistration(username, password, pin);
        await PingServerAsync().ConfigureAwait(false);
        return await RegisterAccountAsync(username, password, pin, body => RequestAsync("/api/v2/account", body)).ConfigureAwait(false);
    }

    static void ValidateRegistration(string username, string password, string pin)
    {
        ValidateCredentials(username, password);
        if (pin == null || !Regex.IsMatch(pin, "\\A[0-9]{6,16}\\z") || pin == password)
            throw Error("第二組密碼需為 6～16 位數字，且與登入密碼不同。");
    }

    static async Task<string> RegisterAccountAsync(string username, string password, string pin, Func<string, Task<string>> register)
    {
        ValidateRegistration(username, password, pin);
        string body = new JavaScriptSerializer().Serialize(new { action = "register", username = username, password = password, pin = pin });
        var result = Parse(await register(body).ConfigureAwait(false));
        if (!True(result, "ok") || Text(result, "code") != "registered") {
            switch (Text(result, "code")) {
                case "nameUnavailable": throw Error("這個帳號無法使用，請換一個帳號。");
                case "invalidPin": throw Error("第二組密碼需為 6～16 位數字，且與登入密碼不同。");
                case "setupRequired": throw Error("伺服器尚未開放註冊，請聯絡管理者。");
                case "busy": case "rateLimited": throw Error("註冊次數過多，請稍後再試。");
                case "invalidInput": throw Error("帳號或密碼格式不正確。");
                default: throw Error("伺服器目前無法完成註冊，請稍後再試。");
            }
        }
        string recovery = Text(result, "recoveryCode");
        if (recovery == null || !Regex.IsMatch(recovery, "\\A(?:[0-9A-F]{8}-){4}[0-9A-F]{8}\\z"))
            throw Error("帳號已建立，但未收到有效復原碼，請聯絡管理者。");
        return recovery;
    }

    static void ValidateCredentials(string username, string password)
    {
        if (username == null || !Regex.IsMatch(username, "\\A[A-Za-z0-9]{3,24}\\z"))
            throw Error("帳號需為 3～24 個英文字母或數字。");
        if (password == null || !Regex.IsMatch(password, "\\A[A-Za-z0-9]{8,32}\\z"))
            throw Error("密碼需為 8～32 個英文字母或數字。");
    }

    static async Task<int> AuthenticateAndStartAsync(string gamePath, string username, string password,
        Func<string, Task<string>> authenticate, Func<string, Task<string>> start)
    {
        ValidateCredentials(username, password);
        string body = new JavaScriptSerializer().Serialize(new { action = "login", username = username, password = password });
        var login = Parse(await authenticate(body).ConfigureAwait(false));
        if (!True(login, "ok") || Text(login, "code") != "loggedIn") throw Error(LoginError(login));
        string input = String.Join("\n", new [] { "v1", gamePath, GameHost, username, password, "0", "", "" }) + "\n";
        var result = Parse(await start(input).ConfigureAwait(false));
        if (!True(result, "ok")) {
            if (Text(result, "code") == "gameRunning") throw Error("遊戲已在執行，請先關閉遊戲。");
            if (Text(result, "code") == "localEndpointRequired") throw Error("本機相容連線尚未就緒，請重新開啟登入器。");
            if (Text(result, "code") == "elevationRequired") throw Error("遊戲需要系統管理員權限，請以系統管理員身分重新開啟登入器。");
            if (Text(result, "code") == "localeFailed") throw Error("透過 Locale Remulator 啟動遊戲失敗：請確認 LRProc.exe 與 LRConfig.xml 完整，或先用右鍵選單測試 Locale Remulator 本身是否正常。");
            throw Error("遊戲啟動失敗，請確認遊戲資料夾完整。");
        }
        object raw;
        int pid;
        if (!result.TryGetValue("pid", out raw) || !(raw is int) || (pid = (int)raw) <= 0)
            throw Error("啟動元件回應異常，請重新開啟登入器。");
        return pid;
    }

    static string LoginError(Dictionary<string, object> result)
    {
        if (Text(result, "reason") == "maintenance") return "伺服器維護中，請稍後再試。";
        switch (Text(result, "code")) {
            case "wrongCredentials": return "帳號或密碼錯誤。";
            case "alreadyOnline": return "此帳號已在線上，請先登出遊戲。";
            case "banned": return "此帳號目前無法登入。";
            case "busy": case "rateLimited": return "登入次數過多，請稍後再試。";
            case "invalidInput": return "帳號或密碼格式不正確。";
            default: return "伺服器目前無法完成登入，請稍後再試。";
        }
    }

    static Dictionary<string, object> Parse(string value)
    {
        try {
            if (value == null || value.Length > 8192) throw new FormatException();
            var result = new JavaScriptSerializer { MaxJsonLength = 8192, RecursionLimit = 16 }.DeserializeObject(value) as Dictionary<string, object>;
            if (result == null) throw new FormatException();
            return result;
        } catch { throw Error("伺服器或啟動元件回應異常，請稍後再試。"); }
    }
    static string Text(Dictionary<string, object> value, string key) { object v; return value.TryGetValue(key, out v) ? v as string : null; }
    static bool True(Dictionary<string, object> value, string key) { object v; return value.TryGetValue(key, out v) && v is bool && (bool)v; }

    static async Task<string> RequestAsync(string path, string body)
    {
        try {
            using (var handler = new HttpClientHandler { UseProxy = false, AllowAutoRedirect = false })
            using (var client = new HttpClient(handler))
            using (var cancel = new CancellationTokenSource(8000))
            using (var request = new HttpRequestMessage(HttpMethod.Post, "http://127.0.0.1:8600" + path)) {
                client.Timeout = TimeSpan.FromSeconds(8);
                request.Headers.Add("X-Cat-Launcher", "1.0.0");
                request.Content = new StringContent(body, new UTF8Encoding(false), "application/json");
                using (var response = await client.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, cancel.Token).ConfigureAwait(false)) {
                    if (response.StatusCode == (HttpStatusCode)429) throw Error("登入次數過多，請稍後再試。");
                    if (!response.IsSuccessStatusCode) throw Error("伺服器目前無法回應，請稍後再試。");
                    using (var stream = await response.Content.ReadAsStreamAsync().ConfigureAwait(false)) {
                        var bytes = new byte[8193]; int length = 0, count;
                        while (length < bytes.Length && (count = await stream.ReadAsync(bytes, length, bytes.Length - length, cancel.Token).ConfigureAwait(false)) > 0) length += count;
                        if (length > 8192) throw Error("伺服器回應異常，請稍後再試。");
                        return new UTF8Encoding(false, true).GetString(bytes, 0, length);
                    }
                }
            }
        } catch (InvalidOperationException) { throw; }
        catch { throw Error("連不到本機伺服器，請先開啟 server 的 launch.bat。"); }
    }

    static byte[] VerifiedFile(string name, string expected)
    {
        var bytes = File.ReadAllBytes(Path.Combine(Folder, name));
        using (var hash = SHA256.Create())
            if (BitConverter.ToString(hash.ComputeHash(bytes)).Replace("-", "") != expected)
                throw Error("啟動元件不完整或已變更，請重新放入原始登入器檔案。");
        return bytes;
    }

    static async Task<string> RunStarterAsync(byte[] starter, string input)
    {
        string stage = Path.Combine(Path.GetTempPath(), "tms2825-starter-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(stage);
        try {
            string exe = Path.Combine(stage, "TmsLocalStarter.exe");
            File.WriteAllBytes(exe, starter);
            var info = new ProcessStartInfo(exe) { UseShellExecute = false, CreateNoWindow = true, WindowStyle = ProcessWindowStyle.Hidden,
                RedirectStandardInput = true, RedirectStandardOutput = true, RedirectStandardError = true,
                StandardOutputEncoding = new UTF8Encoding(false), StandardErrorEncoding = new UTF8Encoding(false), WorkingDirectory = stage };
            // 2026-10-28: hand the resolved Locale Remulator profile to the helper; it starts the game through
            // LRProc.exe so the client sees a Taiwan code page. Without LR the helper starts the game directly.
            LocaleBridge.Apply(info);
            using (var p = Process.Start(info)) {
                var reply = ReadReplyAsync(p, input);
                // 30 s: with Locale Remulator the helper also waits for the injected client to appear.
                try { return await WithTimeout(reply, 30000).ConfigureAwait(false); }
                catch { if (!p.HasExited) p.Kill(); throw; }
            }
        } catch (InvalidOperationException) { throw; }
        catch { throw Error("遊戲啟動失敗，請確認遊戲檔案及啟動元件完整。"); }
        finally {
            // Only this call's new, random temporary directory contains the staged helper.
            if (Path.GetFullPath(stage).StartsWith(Path.GetFullPath(Path.GetTempPath()), StringComparison.OrdinalIgnoreCase))
                try { Directory.Delete(stage, true); } catch { }
        }
    }

    static async Task<string> ReadReplyAsync(Process process, string input)
    {
        using (var writer = new StreamWriter(process.StandardInput.BaseStream, new UTF8Encoding(false)))
            await writer.WriteAsync(input).ConfigureAwait(false);
        var error = process.StandardError.ReadToEndAsync();
        char[] buffer = new char[8193]; int length = 0, count;
        while (length < buffer.Length && (count = await process.StandardOutput.ReadAsync(buffer, length, buffer.Length - length).ConfigureAwait(false)) > 0) length += count;
        if (length > 8192) throw Error("啟動元件回應異常，請重新開啟登入器。");
        await Task.Run(() => process.WaitForExit()).ConfigureAwait(false);
        await error.ConfigureAwait(false);
        return new string(buffer, 0, length).Trim();
    }

    static async Task<T> WithTimeout<T>(Task<T> work, int milliseconds)
    {
        if (await Task.WhenAny(work, Task.Delay(milliseconds)).ConfigureAwait(false) != work)
            throw Error("遊戲啟動逾時，請重新開啟登入器。");
        return await work.ConfigureAwait(false);
    }

    public static void SelfCheck()
    {
        VerifiedFile("TmsLocalStarter.exe", StarterHash);
        int calls = 0;
        Func<string, Task<string>> starter = input => {
            string[] lines = input.Split('\n');
            if (lines.Length != 9 || lines[0] != "v1" || lines[2] != GameHost || lines[3] != "Demo123" || lines[4] != "Test9876" || lines[5] != "0") throw new Exception("stdin protocol");
            calls++; return Task.FromResult("{\"ok\":true,\"pid\":12345}");
        };
        int pid = AuthenticateAndStartAsync(@"D:\Synthetic\MapleStory.exe", "Demo123", "Test9876", body => {
            var j = Parse(body);
            if (Text(j, "action") != "login" || Text(j, "username") != "Demo123" || Text(j, "password") != "Test9876") throw new Exception("login request");
            return Task.FromResult("{\"ok\":true,\"code\":\"loggedIn\"}");
        }, starter).GetAwaiter().GetResult();
        if (pid != 12345 || calls != 1) throw new Exception("success flow");
        ExpectError(() => AuthenticateAndStartAsync(@"D:\Synthetic\MapleStory.exe", "Demo123", "Test9876",
            body => Task.FromResult("{\"ok\":false,\"code\":\"wrongCredentials\"}"), starter).GetAwaiter().GetResult());
        if (calls != 1) throw new Exception("failed login started game");
        ExpectError(() => ValidateCredentials("Demo123\n", "Test9876"));
        ExpectError(() => ValidateCredentials("Demo123", "short"));
        ExpectError(() => Parse("not JSON"));
        ExpectError(() => WithTimeout(new TaskCompletionSource<int>().Task, 10).GetAwaiter().GetResult());
        ExpectError(() => AuthenticateAndStartAsync(@"D:\Synthetic\MapleStory.exe", "Demo123", "Test9876",
            body => Task.FromResult("{\"ok\":true,\"code\":\"loggedIn\"}"), input => Task.FromResult("{\"ok\":true,\"pid\":0}")).GetAwaiter().GetResult());
        const string demoRecovery = "01234567-89ABCDEF-01234567-89ABCDEF-01234567";
        int registrations = 0;
        string recovery = RegisterAccountAsync("Demo123", "Test9876", "654321", body => {
            var request = Parse(body);
            if (Text(request, "action") != "register" || Text(request, "username") != "Demo123"
                || Text(request, "password") != "Test9876" || Text(request, "pin") != "654321") throw new Exception("register request");
            registrations++;
            return Task.FromResult("{\"ok\":true,\"code\":\"registered\",\"userId\":12345,\"recoveryCode\":\"" + demoRecovery + "\"}");
        }).GetAwaiter().GetResult();
        if (registrations != 1 || recovery != demoRecovery) throw new Exception("registration success");
        foreach (string code in new [] { "nameUnavailable", "invalidPin", "setupRequired", "busy" })
            ExpectError(() => RegisterAccountAsync("Demo123", "Test9876", "654321",
                body => Task.FromResult("{\"ok\":false,\"code\":\"" + code + "\"}")).GetAwaiter().GetResult());
        ExpectError(() => ValidateRegistration("Demo123", "12345678", "12345678"));
        ExpectError(() => ValidateRegistration("Demo123", "Test9876", "12345"));
        ExpectError(() => ValidateRegistration("Demo123", "Test9876", "65432A"));
        ExpectError(() => RegisterAccountAsync("Demo123", "Test9876", "654321",
            body => Task.FromResult("{\"ok\":true,\"code\":\"registered\",\"recoveryCode\":\"invalid\"}")).GetAwaiter().GetResult());
    }
    static void ExpectError(Action action) { try { action(); } catch (InvalidOperationException) { return; } throw new Exception("Expected safe error"); }
}
