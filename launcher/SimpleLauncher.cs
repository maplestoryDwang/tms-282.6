using System;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Threading.Tasks;
using System.Windows.Forms;

internal sealed class SimpleLauncher : Form
{
    private readonly TextBox gamePath = new TextBox();
    private readonly TextBox username = new TextBox();
    private readonly TextBox password = new TextBox();
    private readonly Button browse = new Button();
    private readonly Button signIn = new Button();
    private readonly Label status = new Label();
    private readonly CheckBox showPassword = new CheckBox();
    private readonly LinkLabel register = new LinkLabel();
    private readonly Timer gameTimer = new Timer();
    private bool busy;
    private int gamePid;
    private static readonly string SettingsPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "client-path.txt");

    [STAThread]
    private static int Main(string[] args)
    {
        Application.EnableVisualStyles();
        Application.SetCompatibleTextRenderingDefault(false);
        if (args.Length == 1 && args[0] == "--self-check")
        {
            try { LaunchBackend.SelfCheck(); RegistrationDialog.SelfCheck(); return 0; }
            catch (Exception e) { Console.Error.WriteLine(e.Message); return 1; }
        }
        if (args.Length == 1 && args[0] == "--check-server")
        {
            try { Console.WriteLine(LaunchBackend.PingServerAsync().GetAwaiter().GetResult()); return 0; }
            catch (Exception e) { Console.Error.WriteLine(e.Message); return 1; }
        }
        if (args.Length == 2 && args[0] == "--render-check")
        {
            Directory.CreateDirectory(args[1]);
            using (var form = new SimpleLauncher())
            {
                form.ShowInTaskbar = false;
                form.StartPosition = FormStartPosition.Manual;
                form.Location = new Point(-20000, -20000);
                form.Show();
                Application.DoEvents();
                form.gamePath.Text = @"D:\TMS\MapleStory\MapleStory.exe";
                form.Render(Path.Combine(args[1], "simple-launcher.png"));
                form.SetStatus("帳號或密碼不正確，請重新輸入。", true);
                form.Render(Path.Combine(args[1], "simple-launcher-error.png"));
                form.Close();
            }
            RegistrationDialog.RenderChecks(args[1]);
            return 0;
        }
        Application.Run(new SimpleLauncher());
        return 0;
    }

    private SimpleLauncher()
    {
        Text = "TMS 282.5 簡易登入器";
        Font = new Font("Microsoft JhengHei UI", 10F);
        BackColor = Color.FromArgb(246, 247, 249);
        ForeColor = Color.FromArgb(36, 43, 53);
        ClientSize = new Size(480, 440);
        FormBorderStyle = FormBorderStyle.FixedSingle;
        MaximizeBox = false;
        StartPosition = FormStartPosition.CenterScreen;
        AutoScaleMode = AutoScaleMode.Dpi;
        Padding = new Padding(24);

        var layout = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 1, RowCount = 8 };
        foreach (int height in new int[] { 40, 26, 70, 64, 64, 28, 40, 60 })
            layout.RowStyles.Add(new RowStyle(SizeType.Absolute, height));
        layout.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100));
        Controls.Add(layout);
        layout.Controls.Add(new Label { Text = "TMS 282.5", Font = new Font(Font.FontFamily, 20F, FontStyle.Bold),
            AutoSize = true, Margin = Padding.Empty }, 0, 0);
        layout.Controls.Add(new Label { Text = "本機測試伺服器", AutoSize = true, ForeColor = Color.FromArgb(93, 104, 119),
            Margin = new Padding(1, 2, 0, 0) }, 0, 1);

        var clientField = new Panel { Size = new Size(432, 62), Dock = DockStyle.Fill, Margin = new Padding(0, 8, 0, 0) };
        clientField.Controls.Add(new Label { Text = "遊戲主程式", AutoSize = true, Location = Point.Empty });
        var pathRow = new TableLayoutPanel { Location = new Point(0, 24), Size = new Size(432, 29),
            Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right, ColumnCount = 2, RowCount = 1 };
        pathRow.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100));
        pathRow.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 64));
        gamePath.Dock = DockStyle.Fill;
        gamePath.Margin = new Padding(0, 0, 8, 0);
        gamePath.AccessibleName = "MapleStory.exe 路徑";
        browse.Text = "選擇…";
        browse.Dock = DockStyle.Fill;
        browse.Margin = Padding.Empty;
        browse.Click += delegate { SelectGame(); };
        pathRow.Controls.Add(gamePath, 0, 0);
        pathRow.Controls.Add(browse, 1, 0);
        clientField.Controls.Add(pathRow);
        layout.Controls.Add(clientField, 0, 2);

        username.MaxLength = 24;
        username.AccessibleName = "帳號";
        password.MaxLength = 32;
        password.UseSystemPasswordChar = true;
        password.AccessibleName = "密碼";
        layout.Controls.Add(Field("帳號", username), 0, 3);
        layout.Controls.Add(Field("密碼", password), 0, 4);
        showPassword.Text = "顯示密碼";
        showPassword.AutoSize = true;
        showPassword.Margin = Padding.Empty;
        showPassword.CheckedChanged += delegate { password.UseSystemPasswordChar = !showPassword.Checked; };
        var options = new Panel { Size = new Size(432, 28), Dock = DockStyle.Fill, Margin = Padding.Empty };
        showPassword.Location = Point.Empty;
        register.Text = "建立帳號";
        register.AutoSize = true;
        register.LinkColor = Color.FromArgb(38, 87, 139);
        register.ActiveLinkColor = Color.FromArgb(28, 65, 104);
        register.Location = new Point(358, 1);
        register.Anchor = AnchorStyles.Top | AnchorStyles.Right;
        register.TabIndex = 1;
        register.AccessibleName = "建立伺服器帳號";
        register.LinkClicked += delegate { RegisterAccount(); };
        options.Controls.Add(showPassword);
        options.Controls.Add(register);
        layout.Controls.Add(options, 0, 5);
        signIn.Text = "登入並啟動";
        signIn.Dock = DockStyle.Fill;
        signIn.Margin = Padding.Empty;
        signIn.BackColor = Color.FromArgb(38, 87, 139);
        signIn.ForeColor = Color.FromArgb(247, 249, 252);
        signIn.FlatStyle = FlatStyle.Flat;
        signIn.FlatAppearance.BorderSize = 0;
        signIn.Font = new Font(Font, FontStyle.Bold);
        signIn.Click += async delegate { await LoginAndStart(); };
        layout.Controls.Add(signIn, 0, 6);
        status.Dock = DockStyle.Fill;
        status.Margin = new Padding(0, 14, 0, 0);
        status.AccessibleName = "登入狀態";
        layout.Controls.Add(status, 0, 7);
        SetStatus("輸入伺服器帳號與密碼。", false);
        AcceptButton = signIn;
        gameTimer.Interval = 1000;
        gameTimer.Tick += delegate { CheckGame(); };
        FormClosed += delegate { gameTimer.Dispose(); LaunchBackend.StopBridge(); };
        gamePath.Text = DefaultClient();
        Shown += delegate { username.Focus(); };
    }

    private void RegisterAccount()
    {
        if (busy || gamePid != 0) return;
        using (var dialog = new RegistrationDialog())
            if (dialog.ShowDialog(this) == DialogResult.OK)
            {
                username.Text = dialog.CreatedUsername;
                password.Clear();
                showPassword.Checked = false;
                SetStatus("帳號已建立，輸入密碼即可登入。", false);
                password.Focus();
            }
    }

    private static Panel Field(string label, TextBox input)
    {
        var panel = new Panel { Size = new Size(432, 64), Dock = DockStyle.Fill, Margin = Padding.Empty };
        panel.Controls.Add(new Label { Text = label, AutoSize = true, Location = Point.Empty });
        input.Location = new Point(0, 24);
        input.Width = 432;
        input.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
        panel.Controls.Add(input);
        return panel;
    }

    private static string DefaultClient()
    {
        try
        {
            if (File.Exists(SettingsPath)) return File.ReadAllText(SettingsPath).Trim();
        }
        catch (IOException) { }
        catch (UnauthorizedAccessException) { }
        return Path.GetFullPath(Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "..", "..", "..", "MapleStory", "MapleStory.exe"));
    }

    private void SelectGame()
    {
        using (var picker = new OpenFileDialog { Title = "選擇 TMS 282.5 主程式", Filter = "MapleStory.exe|MapleStory.exe", CheckFileExists = true })
        {
            if (File.Exists(gamePath.Text)) picker.FileName = gamePath.Text;
            if (picker.ShowDialog(this) == DialogResult.OK) gamePath.Text = picker.FileName;
        }
    }

    private async Task LoginAndStart()
    {
        if (busy || gamePid != 0) return;
        SetBusy(true);
        SetStatus("正在驗證帳號並啟動遊戲…", false);
        try
        {
            gamePid = await LaunchBackend.LoginAndStartAsync(gamePath.Text.Trim(), username.Text.Trim(), password.Text);
            password.Clear();
            showPassword.Checked = false;
            try { File.WriteAllText(SettingsPath, gamePath.Text.Trim()); }
            catch (IOException) { }
            catch (UnauthorizedAccessException) { }
            SetStatus("遊戲已啟動，請保留登入器開啟。", false);
            signIn.Text = "遊戲執行中";
            gameTimer.Start();
        }
        catch (Exception e)
        {
            SetStatus(e.Message, true);
            password.SelectAll();
            password.Focus();
        }
        finally { SetBusy(false); }
    }

    private void SetBusy(bool value)
    {
        busy = value;
        username.Enabled = password.Enabled = gamePath.Enabled = browse.Enabled = showPassword.Enabled = !value && gamePid == 0;
        signIn.Enabled = register.Enabled = !value && gamePid == 0;
    }

    private void CheckGame()
    {
        try
        {
            using (var game = Process.GetProcessById(gamePid))
                if (!game.HasExited) return;
        }
        catch (ArgumentException) { }
        gamePid = 0;
        gameTimer.Stop();
        LaunchBackend.StopBridge();
        signIn.Text = "登入並啟動";
        SetBusy(false);
        SetStatus("遊戲已結束，可以重新登入。", false);
    }

    private void SetStatus(string message, bool error)
    {
        status.Text = message;
        status.ForeColor = error ? Color.FromArgb(164, 47, 43) : Color.FromArgb(93, 104, 119);
    }

    private void Render(string path)
    {
        using (var bitmap = new Bitmap(Width, Height))
        {
            DrawToBitmap(bitmap, new Rectangle(Point.Empty, Size));
            bitmap.Save(path);
        }
    }

    private sealed class RegistrationDialog : Form
    {
        private readonly TextBox account = new TextBox();
        private readonly TextBox secret = new TextBox();
        private readonly TextBox confirmation = new TextBox();
        private readonly TextBox pin = new TextBox();
        private readonly TextBox recovery = new TextBox();
        private readonly CheckBox saved = new CheckBox();
        private readonly Button create = new Button();
        private readonly Button cancel = new Button();
        private readonly Button acknowledged = new Button();
        private readonly Label feedback = new Label();
        private readonly Func<string, string, string, Task<string>> submit;
        private bool pending, receipt, allowClose;
        public string CreatedUsername { get; private set; }

        public RegistrationDialog(Func<string, string, string, Task<string>> request = null)
        {
            submit = request ?? LaunchBackend.RegisterAsync;
            Text = "建立 TMS 282.5 帳號";
            Font = new Font("Microsoft JhengHei UI", 10F);
            BackColor = Color.FromArgb(246, 247, 249);
            ForeColor = Color.FromArgb(36, 43, 53);
            ClientSize = new Size(440, 440);
            FormBorderStyle = FormBorderStyle.FixedDialog;
            MinimizeBox = MaximizeBox = false;
            ShowInTaskbar = false;
            StartPosition = FormStartPosition.CenterParent;
            AutoScaleMode = AutoScaleMode.Dpi;
            Padding = new Padding(24);
            var layout = Rows(32, 28, 56, 56, 56, 56, 26, 38, 44);
            layout.Controls.Add(Heading("建立帳號"), 0, 0);
            layout.Controls.Add(new Label { Text = "填寫登入資料與遊戲內第二組密碼。", AutoSize = true,
                ForeColor = Color.FromArgb(93, 104, 119), Margin = Padding.Empty }, 0, 1);
            account.MaxLength = 24;
            secret.MaxLength = confirmation.MaxLength = 32;
            pin.MaxLength = 16;
            secret.UseSystemPasswordChar = confirmation.UseSystemPasswordChar = pin.UseSystemPasswordChar = true;
            account.AccessibleName = "新帳號";
            secret.AccessibleName = "新密碼";
            confirmation.AccessibleName = "確認新密碼";
            pin.AccessibleName = "第二組密碼";
            layout.Controls.Add(Field("帳號（3～24 個英文字母或數字）", account), 0, 2);
            layout.Controls.Add(Field("密碼（8～32 個英文字母或數字）", secret), 0, 3);
            layout.Controls.Add(Field("確認密碼", confirmation), 0, 4);
            layout.Controls.Add(Field("第二組密碼（6～16 位數字）", pin), 0, 5);
            layout.Controls.Add(new Label { Text = "第二組密碼需與登入密碼不同。", AutoSize = true,
                ForeColor = Color.FromArgb(93, 104, 119), Margin = Padding.Empty }, 0, 6);
            var actions = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 2, RowCount = 1, Margin = Padding.Empty };
            actions.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100));
            actions.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 80));
            create.Text = "建立帳號";
            Primary(create);
            create.Margin = new Padding(0, 0, 8, 0);
            create.Click += async delegate { await SubmitAsync(); };
            cancel.Text = "取消";
            cancel.Dock = DockStyle.Fill;
            cancel.Margin = Padding.Empty;
            cancel.DialogResult = DialogResult.Cancel;
            actions.Controls.Add(create, 0, 0);
            actions.Controls.Add(cancel, 1, 0);
            layout.Controls.Add(actions, 0, 7);
            feedback.Dock = DockStyle.Fill;
            feedback.Margin = new Padding(0, 10, 0, 0);
            feedback.AccessibleName = "註冊狀態";
            layout.Controls.Add(feedback, 0, 8);
            AcceptButton = create;
            CancelButton = cancel;
            Shown += delegate { account.Focus(); };
            FormClosing += delegate(object sender, FormClosingEventArgs e) {
                if (pending) { e.Cancel = true; SetFeedback("正在建立帳號，請稍候…", false); }
                else if (receipt && !allowClose) { e.Cancel = true; SetFeedback("請先保存復原碼，再勾選已保存。", true); }
            };
            FormClosed += delegate { ClearSecrets(); recovery.Clear(); };
        }

        private TableLayoutPanel Rows(params int[] heights)
        {
            var table = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 1, RowCount = heights.Length, Margin = Padding.Empty };
            table.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100));
            foreach (int height in heights) table.RowStyles.Add(new RowStyle(SizeType.Absolute, height));
            Controls.Add(table);
            return table;
        }
        private Label Heading(string text) { return new Label { Text = text, Font = new Font(Font.FontFamily, 17F, FontStyle.Bold), AutoSize = true, Margin = Padding.Empty }; }
        private void Primary(Button button)
        {
            button.Dock = DockStyle.Fill; button.Margin = Padding.Empty;
            button.BackColor = Color.FromArgb(38, 87, 139); button.ForeColor = Color.FromArgb(247, 249, 252);
            button.FlatStyle = FlatStyle.Flat; button.FlatAppearance.BorderSize = 0; button.Font = new Font(Font, FontStyle.Bold);
            button.EnabledChanged += delegate {
                button.BackColor = button.Enabled ? Color.FromArgb(38, 87, 139) : Color.FromArgb(221, 226, 233);
                button.ForeColor = button.Enabled ? Color.FromArgb(247, 249, 252) : Color.FromArgb(93, 104, 119);
            };
        }
        private void SetFeedback(string text, bool error)
        {
            feedback.Text = text;
            feedback.ForeColor = error ? Color.FromArgb(164, 47, 43) : Color.FromArgb(93, 104, 119);
        }
        private void ClearSecrets() { secret.Clear(); confirmation.Clear(); pin.Clear(); }

        private async Task SubmitAsync()
        {
            if (pending || receipt) return;
            if (secret.Text != confirmation.Text) {
                SetFeedback("兩次輸入的密碼不一致，請重新確認。", true);
                confirmation.SelectAll(); confirmation.Focus(); return;
            }
            pending = true;
            account.Enabled = secret.Enabled = confirmation.Enabled = pin.Enabled = create.Enabled = cancel.Enabled = false;
            create.Text = "建立中…";
            SetFeedback("正在建立帳號…", false);
            TextBox focusAfterError = null;
            try {
                string name = account.Text.Trim();
                string code = await submit(name, secret.Text, pin.Text);
                CreatedUsername = name;
                ShowReceipt(code);
            } catch (Exception e) {
                SetFeedback(e.Message, true);
                focusAfterError = e.Message.Contains("第二組") ? pin : e.Message.Contains("帳號") ? account : secret;
            } finally {
                pending = false;
                if (!receipt) {
                    account.Enabled = secret.Enabled = confirmation.Enabled = pin.Enabled = create.Enabled = cancel.Enabled = true;
                    create.Text = "建立帳號";
                    if (focusAfterError != null) { focusAfterError.SelectAll(); focusAfterError.Focus(); }
                }
            }
        }

        private void ShowReceipt(string code)
        {
            ClearSecrets();
            receipt = true;
            foreach (Control previous in Controls) previous.Visible = false;
            ClientSize = new Size(440, 320);
            Text = "帳號已建立";
            var layout = Rows(32, 32, 46, 37, 34, 38, 34);
            layout.Controls.Add(Heading("帳號已建立"), 0, 0);
            layout.Controls.Add(new Label { Text = "復原碼只顯示這一次，請複製並安全保存。", AutoSize = true,
                ForeColor = Color.FromArgb(93, 104, 119), Margin = Padding.Empty }, 0, 1);
            recovery.Text = code;
            recovery.ReadOnly = true;
            recovery.Font = new Font("Consolas", 10.5F);
            recovery.Dock = DockStyle.Fill;
            recovery.Margin = new Padding(0, 0, 0, 12);
            recovery.AccessibleName = "一次性帳號復原碼";
            layout.Controls.Add(recovery, 0, 2);
            var copy = new Button { Text = "複製復原碼", Dock = DockStyle.Fill, Margin = new Padding(0, 0, 0, 6) };
            copy.Click += delegate {
                try { Clipboard.SetText(recovery.Text); SetFeedback("已複製，請另行保存。", false); }
                catch { SetFeedback("無法複製，請選取復原碼後按 Ctrl+C。", true); recovery.SelectAll(); recovery.Focus(); }
            };
            layout.Controls.Add(copy, 0, 3);
            saved.Text = "我已保存復原碼";
            saved.AutoSize = true;
            saved.Margin = new Padding(0, 3, 0, 0);
            saved.CheckedChanged += delegate { acknowledged.Enabled = saved.Checked; };
            layout.Controls.Add(saved, 0, 4);
            acknowledged.Text = "已保存，返回登入";
            Primary(acknowledged);
            acknowledged.Enabled = false;
            acknowledged.Click += delegate { AcknowledgeReceipt(); };
            layout.Controls.Add(acknowledged, 0, 5);
            feedback.Margin = new Padding(0, 10, 0, 0);
            layout.Controls.Add(feedback, 0, 6);
            SetFeedback("忘記密碼時，需使用此碼復原帳號。", false);
            AcceptButton = acknowledged;
            CancelButton = null;
            copy.Focus();
        }

        private void AcknowledgeReceipt()
        {
            if (!saved.Checked) return;
            allowClose = true;
            recovery.Clear();
            DialogResult = DialogResult.OK;
            Close();
        }

        const string DemoRecovery = "01234567-89ABCDEF-01234567-89ABCDEF-01234567";
        public static void SelfCheck()
        {
            int calls = 0;
            using (var dialog = new RegistrationDialog((name, pass, second) => {
                if (name != "Demo123" || pass != "Test9876" || second != "654321") throw new Exception("registration UI request");
                calls++; return Task.FromResult(DemoRecovery);
            })) {
                dialog.account.Text = "Demo123"; dialog.secret.Text = "Test9876"; dialog.pin.Text = "654321";
                dialog.confirmation.Text = "Mismatch9";
                dialog.SubmitAsync().GetAwaiter().GetResult();
                if (calls != 0 || !dialog.feedback.Text.Contains("不一致")) throw new Exception("confirmation gate");
                dialog.confirmation.Text = "Test9876";
                dialog.SubmitAsync().GetAwaiter().GetResult();
                if (calls != 1 || !dialog.receipt || dialog.recovery.Text != DemoRecovery || dialog.acknowledged.Enabled
                    || dialog.secret.Text.Length != 0 || dialog.confirmation.Text.Length != 0 || dialog.pin.Text.Length != 0)
                    throw new Exception("recovery receipt");
                var closing = new FormClosingEventArgs(CloseReason.UserClosing, false);
                dialog.OnFormClosing(closing);
                if (!closing.Cancel) throw new Exception("receipt save gate");
                dialog.saved.Checked = true;
                if (!dialog.acknowledged.Enabled) throw new Exception("receipt acknowledge");
                dialog.AcknowledgeReceipt();
                if (!dialog.allowClose || dialog.recovery.Text.Length != 0) throw new Exception("receipt clearing");
            }
            using (var dialog = new RegistrationDialog((name, pass, second) => { throw new InvalidOperationException("這個帳號無法使用，請換一個帳號。"); })) {
                dialog.account.Text = "Demo123"; dialog.secret.Text = dialog.confirmation.Text = "Test9876"; dialog.pin.Text = "654321";
                dialog.SubmitAsync().GetAwaiter().GetResult();
                if (dialog.pending || dialog.receipt || !dialog.create.Enabled || !dialog.account.Enabled
                    || dialog.secret.Text != "Test9876" || !dialog.feedback.Text.Contains("無法使用")) throw new Exception("registration error recovery");
            }
        }

        public static void RenderChecks(string folder)
        {
            using (var dialog = new RegistrationDialog()) {
                dialog.StartPosition = FormStartPosition.Manual;
                dialog.Location = new Point(-20000, -20000);
                dialog.Show(); Application.DoEvents();
                RenderDialog(dialog, Path.Combine(folder, "simple-register.png"));
                dialog.SetFeedback("兩次輸入的密碼不一致，請重新確認。", true);
                RenderDialog(dialog, Path.Combine(folder, "simple-register-error.png"));
                dialog.CreatedUsername = "Demo123";
                dialog.ShowReceipt(DemoRecovery); Application.DoEvents();
                RenderDialog(dialog, Path.Combine(folder, "simple-register-receipt.png"));
                dialog.allowClose = true; dialog.Close();
            }
        }
        private static void RenderDialog(Form form, string path)
        {
            using (var bitmap = new Bitmap(form.Width, form.Height)) { form.DrawToBitmap(bitmap, new Rectangle(Point.Empty, form.Size)); bitmap.Save(path); }
        }
    }
}
