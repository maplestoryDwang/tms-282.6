# SimpleLauncher 分析與改造紀錄（TMS 282.5 本機登入器）

- 分析對象：`D:\mxd-data\282\TMS282.5\launcher\SimpleLauncher.exe`（原始發佈版）
- 原始版 SHA-256：`730236EEF4A458025170BB4934134EFE3608F918A98D1073FE5AD3256AEE6C49`
- 原始版 MD5：`0262B11725911389390CB4E4045F0F26`
- IDA 資料庫：同目錄 `SimpleLauncher.exe.i64`
- 分析日期：2026-10-28
- 本次改動：**移除客戶端（MapleStory.exe）固定雜湊校驗**，改用本機 282.6 客戶端可跑；已重新編譯、自檢、發布。

---

## 0. 一句話總結

這是一個 **.NET Framework 4.x / WinForms x64 托管程式**（不是原生 PE），是「TMS 282.5 本機私服」套件的
圖形登入器：跟本機 Server 的 HTTP API 註冊/登入、把官方位址 `202.80.104.26` 掛到回送介面並轉送到
`127.0.0.1`、校驗啟動元件、最後用一支獨立 helper 把 `MapleStory.exe` 拉起來並回報 pid。

> 這份二進位有完整 .NET metadata（160 個函式全有名稱、174 條字串），
> 而且 `TMS282.5\source\launcher\` 就是同一份 C# 原始碼。本文件以 IDA 的 IL 反組譯為主，
> 逐條與原始碼對照，兩者完全一致——行為不是猜的。

---

## 1. 二進位資訊

| 項目 | 值 |
|---|---|
| 格式 | PE32+ (x64)，**.NET / CIL**，CLR `v4.0.30319` |
| 映像基底 / 大小 | `0x140000000` / `0x14000` |
| 函式 / 字串 | 160 / 174（`#US` user-string heap） |
| Manifest | `requestedExecutionLevel level="requireAdministrator"`、`dpiAware=true` |
| 相依組件 | mscorlib、System、System.Windows.Forms、System.Drawing、System.Net.Http、**System.Web.Extensions**（`JavaScriptSerializer`） |

因為是托管程式，IDA 直接把 CIL 當指令反組譯，符號與字串全部可讀；Hex-Rays 對 CIL 無效
（`hexrays_ready: false`），所以本次用「IL → 偽 C#」方式還原（見 §12 附錄）。

### 1.1 類型清單

| 類型 | 方法 | IDA 位址 |
|---|---|---|
| `SimpleLauncher : Form` | `Main`、`.ctor`、`RegisterAccount`、`Field`、`DefaultClient`、`SelectGame`、`LoginAndStart`、`SetBusy`、`CheckGame`、`SetStatus`、`Render` | `0x10`–`0xE40` |
| `SimpleLauncher.RegistrationDialog : Form`（巢狀） | `.ctor`、`Rows`、`Heading`、`Primary`、`SetFeedback`、`ClearSecrets`、`SubmitAsync`、`ShowReceipt`、`AcknowledgeReceipt`、`SelfCheck`、`RenderChecks`、`RenderDialog` | `0xE80`–`0x1D30` |
| `LaunchBackend`（static） | `StartBridge`、`StopBridge`、`Folder`、`LoginAndStartAsync`、`PingServerAsync`、`RegisterAsync`、`ValidateRegistration`、`RegisterAccountAsync`、`ValidateCredentials`、`AuthenticateAndStartAsync`、`LoginError`、`Parse`、`Text`、`True`、`RequestAsync`、`HashFile`、`VerifiedFile`、`RunStarterAsync`、`ReadReplyAsync`、`WithTimeout`、`SelfCheck`、`ExpectError` | `0x1EF0`–`0x2900` |
| `LoopbackBridge : IDisposable` | `.ctor`、`Open`、`Running`、`AcceptLoop`、`Serve`、`Pump`、`Close`、`Dispose`、`EnsureAlias`、`RemoveAlias`、`RunPowerShell` | `0x2B80`–`0x32F0` |
| 編譯器產生 | `<LoginAndStart>d__1b`、`<SubmitAsync>d__31`、`<LoginAndStartAsync>d__b`、`<RequestAsync>d__2b`、`<RunStarterAsync>d__3a`、`<ReadReplyAsync>d__45`、`<WithTimeout>d__4f` 等狀態機、`<>c__DisplayClass*`、`<>f__AnonymousType0\`4`、`<>f__AnonymousType1\`3` | `0x3320`–`0x6010` |

`<>f__AnonymousType0\`4`（action/username/password/pin）與 `<>f__AnonymousType1\`3`（action/username/password）
就是 `new { … }` 被序列化成 JSON 的痕跡。

---

## 2. 架構

```
┌──────────────────────────────┐
│ SimpleLauncher.exe           │  requireAdministrator, x64, WinForms
│  SimpleLauncher (主視窗)      │  遊戲路徑 / 帳號 / 密碼 / 登入
│   └ RegistrationDialog       │  註冊 + 一次性復原碼
│  LaunchBackend (流程)         │  驗證、HTTP、校驗、編排
│  LoopbackBridge (背景)        │  回送別名 + TCP 轉送
└───────────┬──────────────────┘
            │ HTTP POST 127.0.0.1:8600
            ▼
┌──────────────────────────────┐
│ 本機 Server（Java）+ MariaDB   │  /api/v2/launcher、/api/v2/account
└──────────────────────────────┘
            ▲ 遊戲連 202.80.104.26:{8484,8585–8588}
            │ （LoopbackBridge 轉送到 127.0.0.1 同埠）
┌───────────┴──────────────────┐
│ MapleStory.exe（客戶端）        │  由 TmsLocalStarter.exe 拉起
└──────────────────────────────┘
```

**核心技巧**：282.5 客戶端若直連 `127.0.0.1`，約 70 秒後在世界列表處存取違規自我結束；
連官方 IP `202.80.104.26` 才正常。所以登入器把該 IP `/32` 加到回送介面（`SkipAsSource`，
24 小時有效期），並在該 IP 上監聽 8484/8585–8588 轉送到 `127.0.0.1` 同埠——
封包不出本機，外部也連不到。

---

## 3. 入口與四種模式

`Main(string[] args)` 先 `EnableVisualStyles()` + `SetCompatibleTextRenderingDefault(false)`：

| 參數 | 行為 | 結束碼 |
|---|---|---|
| （無） | `Application.Run(new SimpleLauncher())` | 0 |
| `--self-check` | `LaunchBackend.SelfCheck()` + `RegistrationDialog.SelfCheck()`，失敗把訊息印到 stderr | 0/1 |
| `--check-server` | `PingServerAsync()` 結果印到 stdout（正常是 `282.5`） | 0/1 |
| `--render-check <dir>` | 離屏建視窗、`DrawToBitmap` 存 5 張 PNG | 0 |

`--render-check` 會把視窗移到 `(-20000,-20000)` 且不進任務欄，輸出：

```
simple-launcher.png         simple-register.png
simple-launcher-error.png   simple-register-error.png
                            simple-register-receipt.png
```

> IDA 裡兩個怪路徑 `D:\TMS\MapleStory\MapleStory.exe`、`D:\Synthetic\MapleStory.exe`
> **都是自檢/截圖用的假資料**，不是產品路徑。

---

## 4. 主視窗 `SimpleLauncher`

| 屬性 | 值 |
|---|---|
| Title | `TMS 282.5 簡易登入器` |
| Font | `Microsoft JhengHei UI, 10F` |
| BackColor / ForeColor | `(246,247,249)` / `(36,43,53)` |
| ClientSize | `480 × 440` |
| 邊框 / 起始位置 | `FixedSingle`、`CenterScreen`、`MaximizeBox=false`、`AutoScaleMode.Dpi`、Padding 24 |
| 版面 | `TableLayoutPanel` 1×8，列高 `40,26,70,64,64,28,40,60` |

| 列 | 內容 |
|---|---|
| 0 | `TMS 282.5`（20F Bold） |
| 1 | `本機測試伺服器`（灰 93,104,119） |
| 2 | `遊戲主程式` + `[路徑 TextBox][選擇…]`（右欄 64px） |
| 3 | `帳號` + TextBox（MaxLength 24） |
| 4 | `密碼` + TextBox（MaxLength 32、`UseSystemPasswordChar`） |
| 5 | ☐`顯示密碼` + 連結`建立帳號`（右上 `(358,1)`） |
| 6 | `登入並啟動`（藍 38,87,139 / 白字、Flat、無邊框、Bold） |
| 7 | 狀態列（初始 `輸入伺服器帳號與密碼。`） |

`AcceptButton=signIn`、`Timer.Interval=1000`、`Shown→username.Focus()`、
`FormClosed→gameTimer.Dispose(); LaunchBackend.StopBridge();`。

### 狀態機

```csharp
void SetBusy(bool value) {
    busy = value;
    username.Enabled = password.Enabled = gamePath.Enabled = browse.Enabled
        = showPassword.Enabled = !value && gamePid == 0;
    signIn.Enabled = register.Enabled = !value && gamePid == 0;
}
```

`LoginAndStart()`：

```
if (busy || gamePid != 0) return;                  // 重入保護
SetBusy(true); SetStatus("正在驗證帳號並啟動遊戲…")
gamePid = await LaunchBackend.LoginAndStartAsync(gamePath.Text.Trim(),
                                                 username.Text.Trim(), password.Text);  // 密碼不 Trim
password.Clear(); showPassword.Checked = false;
File.WriteAllText("<exe 目錄>\client-path.txt", gamePath.Text.Trim());   // 記住路徑
SetStatus("遊戲已啟動，請保留登入器開啟。"); signIn.Text = "遊戲執行中"; gameTimer.Start();
catch → SetStatus(e.Message, true)（紅 164,47,43）、密碼全選、聚焦
finally → SetBusy(false)
```

`CheckGame()`（每秒）：`Process.GetProcessById(gamePid).HasExited` → 結束（或 `ArgumentException`）則
`gamePid=0`、停 Timer、`StopBridge()`（**拆別名與轉送**）、按鈕還原、`SetStatus("遊戲已結束，可以重新登入。")`。

`DefaultClient()`：`client-path.txt` 存在就讀（Trim），否則 `BaseDirectory\..\..\..\MapleStory\MapleStory.exe`
（開發目錄相對路徑；散佈後通常不存在，使用者需「選擇」一次）。

`Render(path)`：`new Bitmap(Width,Height)` → `DrawToBitmap` → `Save`。

---

## 5. 註冊對話框 `RegistrationDialog`

| 屬性 | 值 |
|---|---|
| Title | `建立 TMS 282.5 帳號`（成功後改 `帳號已建立`） |
| ClientSize | `440 × 440` → 復原碼頁 `440 × 320` |
| 樣式 | `FixedDialog`、`MinimizeBox=MaximizeBox=false`、`ShowInTaskbar=false`、`CenterParent`、Padding 24 |
| 列高 | `32,28,56,56,56,56,26,38,44` → 復原碼頁 `32,32,46,37,34,38,34` |

輸入限制：帳號 24、密碼/確認 32、pin 16，三者皆 `UseSystemPasswordChar`。
`submit` 是 `Func<string,string,string,Task<string>>` 可注入（自檢用），預設 `LaunchBackend.RegisterAsync`。

### 提交流程

```
if (pending || receipt) return;
if (secret.Text != confirmation.Text) {
    兩次輸入的密碼不一致，請重新確認。（紅）→ confirmation 全選+聚焦; return; }
pending = true; 全部輸入/建立/取消停用; create.Text = "建立中…"; SetFeedback("正在建立帳號…")
try { code = await submit(account.Text.Trim(), secret.Text, pin.Text);
      CreatedUsername = name; ShowReceipt(code); }
catch { SetFeedback(e.Message, true);
        focusAfterError = 訊息含「第二組」? pin : 含「帳號」? account : secret; }
finally { pending = false; if (!receipt) 恢復控制項 + 焦點回 focusAfterError; }
```

### 復原碼頁 `ShowReceipt(code)`

整頁換掉（舊控制項 `Visible=false`）：`帳號已建立` 標題、說明「復原碼只顯示這一次，請複製並安全保存。」、
唯讀 TextBox（Consolas 10.5）、`複製復原碼`、☑`我已保存復原碼`、`已保存，返回登入`。

- `AcknowledgeReceipt()`：`if (!saved.Checked) return;` → `allowClose=true`、清空復原碼、`DialogResult=OK`、`Close()`。
- 關閉攔截：`pending` → 取消並提示「正在建立帳號，請稍候…」；
  `receipt && !allowClose` → 取消並提示「請先保存復原碼，再勾選已保存。」；
  `FormClosed` 清空 `secret/confirmation/pin/recovery`。

`Primary(button)`：主按鈕樣式（藍底白字、Flat、無邊框、Bold），`EnabledChanged` 切換
`(38,87,139)/白` ↔ `(221,226,233)/灰`。

---

## 6. `LaunchBackend` 主流程

### 6.1 `LoginAndStartAsync`

```
1. ValidateCredentials(username, password)
     \A[A-Za-z0-9]{3,24}\z → "帳號需為 3～24 個英文字母或數字。"
     \A[A-Za-z0-9]{8,32}\z → "密碼需為 8～32 個英文字母或數字。"
2. 路徑：非空、IsPathRooted、檔名 MapleStory.exe（忽略大小寫）、File.Exists
     否則 "請選擇遊戲資料夾內的 MapleStory.exe。"
3. gamePath = Path.GetFullPath(gamePath)
4. Process.GetProcessesByName("MapleStory").Length != 0 → "遊戲已在執行，請先關閉遊戲。"
5. await Task.Run(() => VerifiedFile("TmsLocalStarter.exe", StarterHash))   ← ［本次改動：原本這裡先比對 GameHash］
     其它例外 → "無法讀取遊戲或啟動元件，請確認檔案完整。"
6. await PingServerAsync()                 // Server 在跑且 protocol == 282.5
7. await Task.Run(() => StartBridge())     // 掛別名 + 開 8484/8585–8588 轉送
8. return await AuthenticateAndStartAsync(gamePath, username, password,
        body  => RequestAsync("/api/v2/account", body),
        input => RunStarterAsync(starter, input));
```

順序重點：**先校驗、再確認 Server、再建橋、最後才登入**；`StartBridge` 失敗會自行 `Dispose`，不留半套狀態。

### 6.2 Server 狀態

```csharp
var status = Parse(await RequestAsync("/api/v2/launcher", "{\"action\":\"status\"}"));
if (!True(status,"ok") || !True(status,"running")) throw "伺服器尚未啟動，請先開啟 server 的 launch.bat。";
if (Text(status,"protocol") != "282.5")            throw "目前連線的伺服器不是 282.5，請啟動此 server 資料夾。";
```

### 6.3 註冊

```
ValidateRegistration：ValidateCredentials + pin 非空 + \A[0-9]{6,16}\z + pin != password
     否則 "第二組密碼需為 6～16 位數字，且與登入密碼不同。"
await PingServerAsync()
POST /api/v2/account  {action:"register", username, password, pin}
recoveryCode 必須符合 \A(?:[0-9A-F]{8}-){4}[0-9A-F]{8}\z
     否則 "帳號已建立，但未收到有效復原碼，請聯絡管理者。"
```

### 6.4 登入後拉起遊戲

```
POST /api/v2/account {action:"login", …} → 必須 ok && code=="loggedIn"，否則走 LoginError
input = string.Join("\n", { "v1", gamePath, "202.80.104.26", username, password, "0", "", "" }) + "\n"
result = Parse(await start(input))
  !ok 時：gameRunning / localEndpointRequired / elevationRequired / 其它 → 各自中文訊息
pid = (int)result["pid"]；必須存在、是 int、> 0，否則 "啟動元件回應異常，請重新開啟登入器。"
```

### 6.5 HTTP 客戶端

```
POST http://127.0.0.1:8600 + path
Headers: X-Cat-Launcher: 1.0.0
Content: StringContent(body, UTF8 no BOM, "application/json")
HttpClientHandler{ UseProxy=false, AllowAutoRedirect=false }
Timeout 8s + CancellationTokenSource(8000) + HttpCompletionOption.ResponseHeadersRead
429        → "登入次數過多，請稍後再試。"
非 2xx     → "伺服器目前無法回應，請稍後再試。"
回應 >8192 → "伺服器回應異常，請稍後再試。"
其它例外    → "連不到本機伺服器，請先開啟 server 的 launch.bat。"
```

自家 `InvalidOperationException`（`Error(...)`）一律原樣往外拋，不會被最後那句蓋掉。

### 6.6 JSON / 校驗 / 啟動 helper

- `Parse`：長度 ≤ 8192，`JavaScriptSerializer{MaxJsonLength=8192, RecursionLimit=16}`，
  必須反序列化成 `Dictionary<string,object>`，否則「伺服器或啟動元件回應異常，請稍後再試。」
- `Text()` 只接受 `string`；`True()` 只接受 `bool == true`。
- `VerifiedFile(name, expected)`：讀 `AppDomain.BaseDirectory\name`，
  `BitConverter.ToString(SHA256(bytes)).Replace("-","")` 比對，不符則
  「啟動元件不完整或已變更，請重新放入原始登入器檔案。」
- `RunStarterAsync`：`%TEMP%\tms2825-starter-<Guid:N>` → 寫入 helper → 啟動
  （`UseShellExecute=false`、`CreateNoWindow`、`WindowStyle=Hidden`、重導 stdio、UTF8、`WorkingDirectory=stage`）
  → `WithTimeout(reply, 15000)` → 失敗 `Kill()` → `finally` 若 stage 在 `%TEMP%` 下則整目錄刪除。
- `ReadReplyAsync`：UTF8 writer 寫 stdin 後關閉；同時吃掉 stderr；讀 stdout 到 `char[8193]`（>8192 丟錯）；
  `Task.Run(WaitForExit)`；回傳 `Trim()` 後的字串。
- `WithTimeout`：`Task.WhenAny(work, Delay(ms)) != work` → 「遊戲啟動逾時，請重新開啟登入器。」

---

## 7. `LoopbackBridge`

```csharp
const string AliasHost = "202.80.104.26";
static readonly int[] Ports = { 8484, 8585, 8586, 8587, 8588 };
const int LifetimeHours = 24;
```

- `Open()`：`EnsureAlias()` → 每埠 `TcpListener(AliasHost, port)`（`ExclusiveAddressUse`、`Start(16)`）；
  `AddressNotAvailable` 最多重試 **40 次 × 250ms**（剛加的位址要等核心生效），其它錯誤直接拋；
  每埠開背景執行緒 `bridge-<port>` → `AcceptLoop`。任何例外 → `Dispose()` 後重拋
  （`StartBridge` 包成「無法建立本機相容連線（…）。」）。
- `AcceptLoop`：`while(!closed)` `AcceptSocket()` → 執行緒 `bridge-conn-<port>` → `Serve`。
- `Serve`：連 `127.0.0.1:port`（`BeginConnect` + 5 秒等待），加入 `sockets`，
  一條執行緒 `Pump(server,client)`，主執行緒 `Pump(client,server)`，結束後關閉兩端並移除。
- `Pump`：`byte[16384]`，`Receive`→寫滿 `Send`；EOF 後 `Shutdown(Send)` 半關閉。
- `Dispose()`：停 listener、關所有 socket；只有「這次自己加的」位址才 `RemoveAlias()`。

### 位址是怎麼加的（PowerShell `-EncodedCommand`，UTF-16LE Base64）

共用前置：找 `127.0.0.1` 且 `PrefixOrigin=WellKnown`、`PrefixLength=8` 的唯一介面；
檢查該介面 `Dhcp = Disabled`。

新增：
```powershell
$have=@(Get-NetIPAddress -AddressFamily IPv4 -IPAddress '202.80.104.26' -ErrorAction SilentlyContinue)
if($have.Count){ if($have.Count -eq 1 -and $have[0].InterfaceIndex -eq $idx){'exists';exit 0}
                 throw 'address is on another interface' }
$t=New-TimeSpan -Hours 24
New-NetIPAddress -InterfaceIndex $idx -AddressFamily IPv4 -IPAddress '202.80.104.26' -PrefixLength 32 `
  -SkipAsSource $true -PolicyStore ActiveStore -ValidLifetime $t -PreferredLifetime $t | Out-Null
'added'
```
回 `added` → 這次是自己加的（關閉時移除）；`exists` → 本來就在（不動）；其它 → 「無法建立本機位址（…）。」

移除：只移除「同介面索引 + /32 + `SkipAsSource`」那一筆。
`RunPowerShell`：`powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -EncodedCommand <b64>`，
`WaitForExit(30000)`（逾時 Kill → 「設定本機位址逾時。」），非 0 → 「無法設定本機位址：<stderr 前 200 字>」。

---

## 8. `TmsLocalStarter.exe`（另一支 EXE）

1. `Console.OutputEncoding = UTF8(no BOM)`。
2. 從 stdin 讀 **8 行**；驗證 `v1`、無 null、host 為 `202.80.104.26`、第 6 行 `0`、7/8 行為空、
   username/password 符合同樣兩條正則 → 否則 `{"ok":false,"code":"invalidInput"}`。
3. 路徑檢查 → `gameMissing`；`Process.GetProcessesByName("MapleStory")` 非空 → `gameRunning`。
4. **［本次改動］原本這裡比對 GameHash，不符回 `versionMismatch`；已整段移除。**
5. `GetActiveTcpListeners()` 必須有 `202.80.104.26:8484` → 否則 `localEndpointRequired`。
6. `CreateProcess(game, "\"<game>\" 202.80.104.26 8484 BeanFun <user> <pass>", …, flags=4 /*SUSPENDED*/, cwd=遊戲目錄)`，
   失敗 → `launchFailed`。
7. `ResumeThread` → 印 `{"ok":true,"pid":N}` → 回 0；失敗 `TerminateProcess`。

用「掛起 + ResumeThread」而非 `Process.Start`，是為了在恢復執行前就拿到 pid、且命令列/工作目錄完全可控。

---

## 9. 協定規格

### 9.1 HTTP（登入器 → 本機 Server）

```
POST http://127.0.0.1:8600/api/v2/launcher
POST http://127.0.0.1:8600/api/v2/account
Content-Type: application/json
X-Cat-Launcher: 1.0.0
（無代理、不跟隨重導、8 秒逾時、回應上限 8192 bytes、UTF-8）
```

| 用途 | 請求 | 成功回應 |
|---|---|---|
| 狀態 | `{"action":"status"}` | `{"ok":true,"running":true,"protocol":"282.5"}` |
| 登入 | `{"action":"login","username":"…","password":"…"}` | `{"ok":true,"code":"loggedIn"}` |
| 註冊 | `{"action":"register","username":"…","password":"…","pin":"…"}` | `{"ok":true,"code":"registered","userId":12345,"recoveryCode":"XXXXXXXX-…-XXXXXXXX"}` |

錯誤：`{"ok":false,"code":"…"}`，登入可帶 `"reason":"maintenance"`；HTTP 429 視為次數過多。

### 9.2 登入錯誤碼

| 條件 | 文案 |
|---|---|
| `reason=="maintenance"` | 伺服器維護中，請稍後再試。 |
| `wrongCredentials` | 帳號或密碼錯誤。 |
| `alreadyOnline` | 此帳號已在線上，請先登出遊戲。 |
| `banned` | 此帳號目前無法登入。 |
| `busy` / `rateLimited` | 登入次數過多，請稍後再試。 |
| `invalidInput` | 帳號或密碼格式不正確。 |
| 其它 | 伺服器目前無法完成登入，請稍後再試。 |

### 9.3 註冊錯誤碼

| code | 文案 |
|---|---|
| `nameUnavailable` | 這個帳號無法使用，請換一個帳號。 |
| `invalidPin` | 第二組密碼需為 6～16 位數字，且與登入密碼不同。 |
| `setupRequired` | 伺服器尚未開放註冊，請聯絡管理者。 |
| `busy` / `rateLimited` | 註冊次數過多，請稍後再試。 |
| `invalidInput` | 帳號或密碼格式不正確。 |
| 其它 | 伺服器目前無法完成註冊，請稍後再試。 |

### 9.4 stdin 協定（登入器 → helper，v1，8 行 + 結尾換行）

```
v1
<MapleStory.exe 絕對路徑>
202.80.104.26
<username>
<password>
0
（空行）
（空行）
```

helper 回**單行 JSON**：`{"ok":true,"pid":N}` 或 `{"ok":false,"code":"…"}`；
上限 8192 字元、15 秒逾時。

---

## 10. 本次改動（2026-10-28）

需求：手上只有 **282.6 客戶端**，沒有 282.5，要能直接跑 → **移除客戶端固定雜湊校驗**。

### 10.1 `LaunchBackend.cs`

```diff
-const string GameHash = "DEF89CF3C51B5D196B90776D64A68898D722C26F8CE14F97F8C3930E9C4CFC27";
 const string StarterHash = "86C94F34…F6DA";   // 仍保留，build.ps1 每次重編後回寫
```

```diff
-starter = await Task.Run(() => {
-    if (HashFile(gamePath) != GameHash) throw Error("這個遊戲主程式不是 TMS 282.5，請重新選擇。");
-    return VerifiedFile("TmsLocalStarter.exe", StarterHash);
-}).ConfigureAwait(false);
+starter = await Task.Run(() => VerifiedFile("TmsLocalStarter.exe", StarterHash)).ConfigureAwait(false);
```

並刪除已無用的 `HashFile(string)`（`VerifiedFile` 仍用 SHA-256，`System.Security.Cryptography` 保留）。

### 10.2 `TmsLocalStarter.cs`

```diff
-using System.Security.Cryptography;
-const string GameHash = "DEF89CF3…CFC27";
-using (var file = File.OpenRead(game)) using (var hash = SHA256.Create())
-    if (BitConverter.ToString(hash.ComputeHash(file)).Replace("-", "") != GameHash)
-        return Fail("versionMismatch");
```

`versionMismatch` 這個 code 從此不再產生；登入器端本來也沒有對應分支，無需改動。
**保留**的檢查：路徑/檔名/存在性（`gameMissing`）、已在執行（`gameRunning`）、
本機埠已監聽（`localEndpointRequired`）、helper 自身 SHA-256（`StarterHash`）。

### 10.3 影響

| 項目 | 改動前 | 改動後 |
|---|---|---|
| 任意 MapleStory.exe | 必須等於 `DEF89CF3…` | 只要檔名/路徑正確即可 |
| 282.6 客戶端 | 被拒（「不是 TMS 282.5」） | 可通過校驗並啟動 |
| 防篡改 | 遊戲 + helper 雙重釘死 | 只剩 helper 釘死 |
| Server 協定檢查 | `protocol == "282.5"` | **不變**（Server 仍是 282.5 套件） |

---

## 11. 建置與驗證結果

建置指令（與原專案一致，不需要 .NET SDK／MSBuild，只用 Framework 4.x 的 `csc.exe`）：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1            # 編譯 + 自檢 + 截圖
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -Publish   # 再複製到 ..\..\launcher
```

本次實測（`build.ps1 -Publish`）：

| 檢查 | 結果 |
|---|---|
| `TmsLocalStarter.cs` 編譯 | 0 錯誤 |
| `SimpleLauncher.cs` + `LaunchBackend.cs` + `LoopbackBridge.cs` + `LocaleBridge.cs` 編譯（掛 manifest） | 0 錯誤 |
| `SimpleLauncher.Check.exe --self-check` | **exit 0**；第一行輸出 locale 解析結果 |
| `--render-check` | 5 張 PNG 全部產生 |
| `--check-server` | **exit 0，輸出 `282.5`**（本機 Server 當時在跑） |
| 產物字串掃描 | 已無 `DEF89CF3…`、已無「這個遊戲主程式不是 TMS 282.5」、含 `TMS_LRPROC`／`LRConfig.xml` |

產物（最後一次建置 2026-10-28 18:0x，含地區模擬修正）：

| 檔案 | 大小 | SHA-256 |
|---|---|---|
| `build-output\SimpleLauncher.exe`（=`launcher\SimpleLauncher.exe`） | 68,096 | `EDC2D59E89151F753E1C260BA61B73482960E824405FD3B168CF8D3100F76CD4` |
| `build-output\TmsLocalStarter.exe`（=`launcher\TmsLocalStarter.exe`） | 10,240 | `AFA0FC410BBCB51FFBCCD17FDE5311EDC7CA41C6B27F6B02F6A18EA3E4479D57` |
| `build-output\SimpleLauncher.Check.exe`（無 manifest，驗證用） | — | — |
| `build-output\preview\*.png` | — | 5 張 UI 快照 |

`build.ps1` 已自動把 `LaunchBackend.cs` 的 `StarterHash` 改寫為
`AFA0FC41…79D57`（= 新 helper 的雜湊），並把兩支 EXE 與 `LocaleRemulator\` 複製到 `TMS282.5\launcher\`。
原本的兩支發佈版 EXE 已備份到 `TMS282.5\launcher\backup-20261028-1\`。

---

## 12. 使用方式

1. 執行 `TMS282.5\開始遊戲.bat`（會檢查環境、啟動 DB 與 Server，最後以管理員權限拉起登入器），
   或直接執行 `TMS282.5\launcher\SimpleLauncher.exe`（會跳 UAC，因為要加回送位址）。
2. 第一次按「選擇…」，指定你的 `MapleStory.exe`（282.6 也可以），路徑會記在
   `launcher\client-path.txt`。
3. 「建立帳號」→ 設定帳號、密碼、第二組密碼 → **保存復原碼**（只顯示一次）。
4. 輸入帳號密碼 → 「登入並啟動」。

---

## 13. 地區模擬（Locale Remulator）

### 13.1 為什麼需要

282.x 客戶端在**簡體中文系統**（`zh-CN`、ACP 936）上會拒絕登入（「簡體系統無法登入」）。
Locale Remulator（<https://github.com/InWILL/Locale_Remulator>，本機 1.6.0）的做法是：
`LRProc.exe` 用 `CreateProcess`（掛起）建立目標，再把 `LRHookx64.dll` 注入進去，
由它鉤住 `GetSystemDefaultLCID / GetUserDefaultLCID / GetThreadLocale / GetLocaleInfoW/A`
等 API 並提供 `zh-TW`（CodePage 950、LCID 1028、Taipei 時區、Bias 480）的答案，
**完全不需要改動系統的「地區」設定**。

### 13.2 實作（三個地方）

| 檔案 | 改動 |
|---|---|
| `LocaleBridge.cs`（新增） | 解析「用哪支 LRProc.exe、用哪個 profile」，並用環境變數交給 helper |
| `LaunchBackend.cs` | `RunStarterAsync` 建 `ProcessStartInfo` 後呼叫 `LocaleBridge.Apply(info)`；helper 逾時 15s → **30s**（要等注入後的客戶端出現） |
| `TmsLocalStarter.cs` | 收到 `TMS_LRPROC` + `TMS_LRGUID` 時改走 LRProc，否則維持原本的 `CreateProcess`（掛起）路徑 |

**解析順序**（`LocaleBridge.TryResolve`）：

1. `lr.txt`（放在 `SimpleLauncher.exe` 旁）第 1 行 = `LRProc.exe` 路徑，第 2 行（可選）= profile GUID；
   空行、`#`、`;` 開頭視為註解，路徑可加引號。
2. 自動偵測 `LocaleRemulator\LRProc.exe`（本套件內建）→ `LRProc.exe` → `Locale_Remulator\LRProc.exe`。
3. 都沒有 → 回 `off`，**照舊直接啟動遊戲**（不會因為沒裝 LR 而壞掉）。

profile GUID 未指定時，讀 `LRProc.exe` 同目錄的 `LRConfig.xml`，挑 `Location` 為
`zh-TW` / `zh-Hant` 的 `Profile`，名稱含 `Taiwan`、`Admin` 者優先。

**helper 端**（`StartWithLocale`）——與 `LREditor` 的 `Button_Run_Click` **逐行一致**：

```csharp
// LREditor/LREditor.xaml.cs, Button_Run_Click
proc.StartInfo.FileName          = LRConfig.CurrentPath + "\\LRProc.exe";
proc.StartInfo.Arguments         = profile.Guid + " " + CommandLine;   // guid 不加引號
proc.StartInfo.WorkingDirectory  = Path.GetDirectoryName(filepath);
proc.StartInfo.UseShellExecute   = true;                               // ← 關鍵
if (profile.RunAsAdmin) proc.StartInfo.Verb = "runas";                 // ← 關鍵
proc.Start();
```

```
LRProc.exe <guid> "<game 絕對路徑>" 202.80.104.26 8484 BeanFun <user> <pass>
```

> **為什麼一定要 `UseShellExecute = true`**：`LRProc.cpp` 最後一行是
> `DetourCreateProcessWithDllExW(...)`，**回傳值直接被丟掉**，失敗時仍然 `return 0`（靜默）。
> 用 `UseShellExecute=false`（`CreateProcess` + 隱藏視窗、無互動桌面）去叫它，Detour 建立行程會失敗而且
> 沒有任何錯誤——我最初就是這樣，現象正好是「LRProc 秒退、退出碼 0、遊戲沒出現」。
> 改成 shell 執行（必要時帶 `runas`）之後一次就通。
> 另外 `LRProc` 會 `wcsstr(命令行, guid)` 再 `+37` 取後半段當目標命令行，所以 guid 要原樣出現（不必加引號）。

`LRProc` **會立刻返回**（實測 ~90ms），所以遊戲 pid 不能用它的返回值，改成
「啟動前記下 `MapleStory` 進程清單 → 啟動後每 200ms 掃一次，找出新出現的 pid」（最多 60 次 ≈ 12 秒）。
`LRProc` 非 0 退出或逾時 → `localeFailed`；找不到新進程 → 也是 `localeFailed`（代表 Detour 靜默失敗）。

### 13.3 內建檔案

`launcher\LocaleRemulator\`（由 `build.ps1 -Publish` 一起複製）：

```
LRProc.exe  LRHookx64.dll  LRHookx32.dll
LRSubMenus.dll  SharpShell.dll  System.Drawing.Common.dll
LREditor.exe  LRInstaller.exe  ServerRegistrationManager.exe
LRConfig.xml   ← 只留一個 "Run in Taiwan (Admin)" profile
LICENSE  README.md
```

> `LRProc.exe` 依賴 `LRSubMenus.dll` → `SharpShell.dll` → `System.Drawing.Common.dll`。
> **少一個就會在啟動時丟 `FileNotFoundException` 直接崩潰**（我在第一次打包時漏了 `LRSubMenus.dll`，
> 事件檢視器會看到 `LRProc.exe` + `0xe0434352`）。這組檔案必須整套一起放。

`LRConfig.xml`（zh-TW profile，GUID `bd394cf4-037f-480f-a960-530852f8b6e3`）：

```xml
<Profile Name="Run in Taiwan (Admin)" Guid="bd394cf4-037f-480f-a960-530852f8b6e3">
  <Location>zh-TW</Location><CodePage>950</CodePage><LCID>1028</LCID>
  <TimeZone>Taipei Standard Time</TimeZone><Bias>480</Bias>
  <RunAsAdmin>true</RunAsAdmin><HookIME>true</HookIME><HookLCID>true</HookLCID>
</Profile>
```

### 13.4 驗證過的事實

| 項目 | 結果 |
|---|---|
| LRProc 命令列語法 | **`LRProc.exe <guid> "<exe>" <args...>`**（`-runas`／`runas` 都無效；`runas` 只是 `RunAsAdmin` 的子字串） |
| 參數原樣轉發 | 以假 `MapleStory.exe` 實測：`ArgCount=5`、`202.80.104.26 8484 BeanFun <user> <pass>` 一字不差 |
| 目標行程編碼頁 | 注入後 `Encoding.Default.CodePage` 從 **936 → 950**（同時 `Encoding.Default` 的 ANSI 行為改變） |
| 行程名 | 目標檔名 `MapleStory.exe` → 進程名 `MapleStory`（pid 偵測可用） |
| `locale` 解析四種情況 | 內建 → `bd394cf4… (admin=yes) via …\LocaleRemulator\LRProc.exe`；移除資料夾 → `off`；`lr.txt` 指到 E:\ 安裝 → 用該支；`lr.txt` 指到不存在路徑 → `error: 找不到 Locale Remulator…` |
| **端到端（helper → LRProc → 目標）** | 假 `MapleStory.exe`（會把 ACP/參數/pid 寫檔）實測：helper 回 **`{"ok":true,"pid":30828}`**、目標 **`ACP=950`**、參數 `127.0.0.1|18484|BeanFun|Demo123|Test9876` 完整、pid 與回報一致 |
| `--self-check` | exit 0；有 LR → `locale: … (admin=yes) via …`，無 LR → `locale: off` |

`--self-check` 會把解析結果印在第一行，方便排錯：

```
locale: bd394cf4-037f-480f-a960-530852f8b6e3 via D:\...\launcher\LocaleRemulator\LRProc.exe
locale: off
locale: error: 找不到 Locale Remulator：lr.txt 指定的 LRProc.exe 不存在（…）。
```

### 13.5 已踩到的坑與結論

1. **真正的坑：用 `CreateProcess` 叫 `LRProc` → 靜默失敗**
   `LRProc.cpp` 的結尾是 `DetourCreateProcessWithDllExW(...); return 0;`——**回傳值沒檢查**。
   用 `UseShellExecute=false`（隱藏視窗、無互動桌面）呼叫時，Detour 建立行程會失敗，但 LRProc
   照樣回 0，於是看起來「什麼都沒發生」。這正是我第一版（也是你看到的那個錯誤訊息）的原因。
   改成與 `LREditor` 相同的 `UseShellExecute=true` +（必要時）`Verb="runas"` 後即通。
   > 排查過程中的「17:23 之後連 notepad 都不啟動」也是同一個 bug 的不同表現，
   > 不是 LR 壞了，也不是環境問題——這點已由端到端實測推翻。
2. **x86 目標不行**：本版 `LRProc.exe` 是 x64 建置，對 32 位目標（例如 `MapleStoryV282.6.exe`，
   PE machine `0x14C`）注入會失敗（實測：LRProc 崩在 .NET 例外，`0xe0434352`）。
   目前接的 `MapleStory.exe` 是 x64（`0x8664`），所以沒問題。
3. **`LRProc` 退出碼不可信**：它是「啟動器 + 立即返回」，成功與否只能靠「目標行程是否出現」判斷，
   所以 helper 用 pid 差異偵測，失敗一律回 `localeFailed` 並提示去測右鍵選單。

---

## 14. 已知限制與風險

1. **282.6 客戶端只是「不再被雜湊擋住」，不代表協議相容**
   遊戲命令列仍固定為
   `"<game>" 202.80.104.26 8484 BeanFun <user> <pass>`，
   Server 仍是 282.5 套件（`protocol` 檢查不變）。若 282.6 的登入參數或封包有變，
   可能出現「啟動成功但登入失敗／被踢」。這需要在實機登入一次才能確認。
   真的不相容時，要改的是 `TmsLocalStarter.cs` 第 51 行的命令列組合與 Server 端版本，
   而不是登入器其他部分。
2. **`202.80.104.26` 是全機單一資源**：兩個用同一別名的登入器不能同時跑
   （例如另一套「貓貓登入器」）。`EnsureAlias` 遇到位址在別的介面會拋錯；
   同介面已存在則沿用、關閉時不刪。
3. **需要管理員**：`New-NetIPAddress` 與遊戲啟動都需要提權，manifest 直接
   `requireAdministrator`；沒有管理員權限會在建橋階段失敗。
4. **埠佔用**：8484、8585–8588 在 `202.80.104.26` 上若被別的程序佔用，
   建橋會失敗（「無法建立本機相容連線」）；`Start(16)` 有 40×250ms 重試，只針對
   `AddressNotAvailable`。
5. **`套件驗證.txt` 已過期**：裡面記錄的 `launcher/SimpleLauncher.exe` 與
   `TmsLocalStarter.exe` SHA-256 是舊發佈版；若你要繼續用該檔做驗證，需同步更新
   為 §11 的新雜湊。
6. **`elevationRequired` 是預留分支**：helper 不會產生這個 code，登入器仍保留對應文案。
7. **`launcher\` 目錄裡的 `.id0/.id1/.nam/.til`** 是本次 IDA 分析的資料庫工作檔，
   不屬於套件內容，可安全刪除（保留 `.i64` 才能再開 IDA）。

---

## 15. 重新建置注意事項

- 只要改了 `TmsLocalStarter.cs` 就**必須**重跑 `build.ps1`：它會重新計算 helper 雜湊並
  回寫 `LaunchBackend.cs` 的 `StarterHash`。手動只編 GUI 會導致
  「啟動元件不完整或已變更」。
- `build.ps1` 會**就地改寫** `LaunchBackend.cs`（正則替換常數），這是原專案的設計，不是副作用。
- 自檢 `--self-check` 是用不帶 manifest 的 `SimpleLauncher.Check.exe` 跑的，
  避免每次都要 UAC；正式 `SimpleLauncher.exe` 仍保留 `requireAdministrator`。
- 截圖 `--render-check` 產生的 PNG 可用來比對 UI 是否走樣（本次已確認與原版一致）。

---

## 附錄：分析中間產物

| 檔案 | 內容 |
|---|---|
| `D:\mxd-data\282\_work\il_dump.txt` | 160 個方法的完整 CIL 反組譯（9,131 行） |
| `D:\mxd-data\282\_work\pseudo_cs.txt` | 由 IL 還原的可讀偽 C#（4,156 行） |
| `D:\mxd-data\282\_work\ildecomp.py` | 還原器（stack 模擬 + 跳躍標籤 + 屬性存取器摺疊） |
| `D:\mxd-data\282\_work\strings.txt` | 174 條 user string（正確解碼） |
| `D:\mxd-data\282\launcher\ANALYSIS.md` | 指向本文件的入口（工作區 `launcher\` 目錄的索引） |
