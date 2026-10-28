using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Xml;

// 2026-10-28: the 282.x client refuses to log in on a Simplified Chinese system (zh-CN, ACP 936).
// Locale Remulator (https://github.com/InWILL/Locale_Remulator) starts the game through LRProc.exe, which injects
// LRHookx64.dll so the target process sees a Taiwan profile (zh-TW, code page 950, LCID 1028) without changing any
// OS setting. This type resolves which LRProc.exe / profile to use and hands them to TmsLocalStarter through
// environment variables; the injection itself is done by LRProc.exe.
//
// LRProc's own contract (LRProc/LRProc.cpp upstream): "LRProc.exe GUID Path Args", profile read from LRConfig.xml
// next to LRProc.exe, and it exits 0 even when DetourCreateProcessWithDllExW fails - hence the helper only trusts
// the pid it can observe afterwards.
//
// Resolution order:
//   1. lr.txt next to SimpleLauncher.exe - line 1: LRProc.exe path, line 2 (optional): profile GUID
//   2. LocaleRemulator\LRProc.exe next to SimpleLauncher.exe (the copy shipped with this package)
//   3. LRProc.exe / Locale_Remulator\LRProc.exe next to SimpleLauncher.exe
// When no LRProc.exe is found the launcher starts the game directly, exactly as before.
internal static class LocaleBridge
{
    // Keep in sync with TmsLocalStarter.LocaleProcVariable / LocaleGuidVariable / LocaleAdminVariable.
    public const string ProcVariable = "TMS_LRPROC";
    public const string GuidVariable = "TMS_LRGUID";
    public const string AdminVariable = "TMS_LRADMIN";

    static string BaseFolder { get { return AppDomain.CurrentDomain.BaseDirectory; } }

    sealed class Profile
    {
        public string Guid;
        public string Name;
        public string Location;
        public bool Admin;
    }

    /** Adds the locale variables to the helper's environment. Throws when lr.txt or LRConfig.xml is broken. */
    public static void Apply(ProcessStartInfo info)
    {
        string proc, guid, error;
        bool admin;
        if (!TryResolve(out proc, out guid, out admin, out error)) {
            if (error != null) throw new InvalidOperationException(error);
            return;
        }
        info.EnvironmentVariables[ProcVariable] = proc;
        info.EnvironmentVariables[GuidVariable] = guid;
        info.EnvironmentVariables[AdminVariable] = admin ? "1" : "0";
    }

    /** Short description for diagnostics: "off" when LR is not installed, "error: …" when it is misconfigured. */
    public static string Describe()
    {
        string proc, guid, error;
        bool admin;
        if (!TryResolve(out proc, out guid, out admin, out error)) return error == null ? "off" : "error: " + error;
        return guid + " (admin=" + (admin ? "yes" : "no") + ") via " + proc;
    }

    static bool TryResolve(out string proc, out string guid, out bool admin, out string error)
    {
        proc = null;
        guid = null;
        admin = false;
        error = null;
        string[] config = ReadConfig();
        string configured = config.Length > 0 ? config[0] : null;
        if (configured != null && configured.Length > 0) {
            if (!File.Exists(configured)) {
                error = "找不到 Locale Remulator：lr.txt 指定的 LRProc.exe 不存在（" + configured + "）。";
                return false;
            }
            proc = configured;
        } else {
            string[] candidates = new string[] {
                Path.Combine(BaseFolder, Path.Combine("LocaleRemulator", "LRProc.exe")),
                Path.Combine(BaseFolder, "LRProc.exe"),
                Path.Combine(BaseFolder, Path.Combine("Locale_Remulator", "LRProc.exe"))
            };
            for (int i = 0; i < candidates.Length; i++)
                if (File.Exists(candidates[i])) { proc = candidates[i]; break; }
            if (proc == null) return false;   // no Locale Remulator: keep starting the game directly
        }
        string wanted = config.Length > 1 ? config[1] : null;
        List<Profile> profiles = ReadProfiles(proc);
        if (profiles.Count == 0) {
            error = "Locale Remulator 的 LRConfig.xml 讀不到 profile（" + Path.Combine(Path.GetDirectoryName(proc), "LRConfig.xml") + "）。";
            return false;
        }
        Profile chosen = null;
        if (wanted != null && wanted.Length > 0) {
            for (int i = 0; i < profiles.Count; i++)
                if (String.Equals(profiles[i].Guid, wanted, StringComparison.OrdinalIgnoreCase)) { chosen = profiles[i]; break; }
        }
        if (chosen == null) chosen = PickTaiwan(profiles);
        if (chosen == null) {
            error = "Locale Remulator 的 LRConfig.xml 找不到 zh-TW profile（" + Path.Combine(Path.GetDirectoryName(proc), "LRConfig.xml") + "）。";
            return false;
        }
        guid = chosen.Guid;
        admin = chosen.Admin;
        return true;
    }

    /** First profile whose Location is zh-TW / zh-Hant; "Taiwan" and "Admin" names win ties. */
    static Profile PickTaiwan(List<Profile> profiles)
    {
        Profile best = null;
        int bestScore = -1;
        for (int i = 0; i < profiles.Count; i++) {
            Profile profile = profiles[i];
            if (profile.Location == null) continue;
            if (!profile.Location.StartsWith("zh-TW", StringComparison.OrdinalIgnoreCase)
                && !profile.Location.StartsWith("zh-Hant", StringComparison.OrdinalIgnoreCase)) continue;
            int score = 0;
            if (profile.Name != null && profile.Name.IndexOf("Taiwan", StringComparison.OrdinalIgnoreCase) >= 0) score += 2;
            if (profile.Admin) score += 1;
            if (score > bestScore) { bestScore = score; best = profile; }
        }
        return best;
    }

    /** lr.txt: blank lines, '#' and ';' comments are ignored; surrounding quotes are stripped. */
    static string[] ReadConfig()
    {
        List<string> lines = new List<string>();
        string path = Path.Combine(BaseFolder, "lr.txt");
        if (!File.Exists(path)) return lines.ToArray();
        foreach (string raw in File.ReadAllLines(path)) {
            string line = raw.Trim();
            if (line.Length == 0 || line[0] == '#' || line[0] == ';') continue;
            if (line.Length >= 2 && line[0] == '"' && line[line.Length - 1] == '"') line = line.Substring(1, line.Length - 2);
            lines.Add(line);
        }
        return lines.ToArray();
    }

    static List<Profile> ReadProfiles(string lrProc)
    {
        List<Profile> result = new List<Profile>();
        string config = Path.Combine(Path.GetDirectoryName(lrProc), "LRConfig.xml");
        if (!File.Exists(config)) return result;
        XmlDocument document = new XmlDocument();
        document.Load(config);
        XmlNodeList nodes = document.SelectNodes("//Profile");
        foreach (XmlNode node in nodes) {
            Profile profile = new Profile();
            profile.Guid = Attribute(node, "Guid");
            profile.Name = Attribute(node, "Name");
            if (profile.Guid == null || profile.Guid.Length == 0) continue;
            profile.Location = ChildText(node, "Location");
            profile.Admin = String.Equals(ChildText(node, "RunAsAdmin"), "true", StringComparison.OrdinalIgnoreCase);
            result.Add(profile);
        }
        return result;
    }

    static string Attribute(XmlNode node, string name)
    {
        if (node.Attributes == null) return null;
        XmlAttribute attribute = node.Attributes[name];
        return attribute == null ? null : attribute.Value;
    }

    static string ChildText(XmlNode node, string name)
    {
        XmlNodeList children = node.ChildNodes;
        for (int i = 0; i < children.Count; i++) {
            if (String.Equals(children[i].Name, name, StringComparison.OrdinalIgnoreCase)) return children[i].InnerText.Trim();
        }
        return null;
    }
}
