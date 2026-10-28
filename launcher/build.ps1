param([switch]$Publish)
$ErrorActionPreference = 'Stop'
$compiler = Join-Path $env:WINDIR 'Microsoft.NET/Framework64/v4.0.30319/csc.exe'
$output = Join-Path $PSScriptRoot 'build-output'
[void][IO.Directory]::CreateDirectory($output)
$helper = Join-Path $output 'TmsLocalStarter.exe'
$gui = Join-Path $output 'SimpleLauncher.exe'
& $compiler /nologo /target:exe /platform:x64 /reference:System.Web.Extensions.dll ("/out:" + $helper) (Join-Path $PSScriptRoot 'TmsLocalStarter.cs')
if ($LASTEXITCODE -ne 0) { throw 'Local game helper compilation failed.' }
$helperHash = (Get-FileHash -LiteralPath $helper -Algorithm SHA256).Hash
$backendPath = Join-Path $PSScriptRoot 'LaunchBackend.cs'
$backend = [IO.File]::ReadAllText($backendPath)
$backend = [regex]::Replace($backend, 'const string StarterHash = "[0-9A-F]{64}";', ('const string StarterHash = "' + $helperHash + '";'))
[IO.File]::WriteAllText($backendPath, $backend, [Text.UTF8Encoding]::new($false))
& $compiler /nologo /target:winexe /platform:x64 /reference:System.Windows.Forms.dll /reference:System.Drawing.dll /reference:System.Net.Http.dll /reference:System.Web.Extensions.dll /reference:System.Xml.dll ("/win32manifest:" + (Join-Path $PSScriptRoot 'launcher.manifest')) ("/out:" + $gui) (Join-Path $PSScriptRoot 'SimpleLauncher.cs') $backendPath (Join-Path $PSScriptRoot 'LoopbackBridge.cs') (Join-Path $PSScriptRoot 'LocaleBridge.cs')
if ($LASTEXITCODE -ne 0) { throw 'Simple launcher compilation failed.' }
# Run the same sources with an asInvoker test manifest; production EXE still requires elevation for the game.
$checkGui = Join-Path $output 'SimpleLauncher.Check.exe'
& $compiler /nologo /target:winexe /platform:x64 /reference:System.Windows.Forms.dll /reference:System.Drawing.dll /reference:System.Net.Http.dll /reference:System.Web.Extensions.dll /reference:System.Xml.dll ("/out:" + $checkGui) (Join-Path $PSScriptRoot 'SimpleLauncher.cs') $backendPath (Join-Path $PSScriptRoot 'LoopbackBridge.cs') (Join-Path $PSScriptRoot 'LocaleBridge.cs')
if ($LASTEXITCODE -ne 0) { throw 'Launcher check compilation failed.' }
function Invoke-Check([string]$arguments) {
    $info = [Diagnostics.ProcessStartInfo]::new($checkGui, $arguments)
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.StandardOutputEncoding = [Text.Encoding]::Default
    $info.StandardErrorEncoding = [Text.Encoding]::Default
    $check = [Diagnostics.Process]::Start($info)
    try {
        if (-not $check.WaitForExit(20000)) { $check.Kill(); throw 'Launcher verification timed out.' }
        if ($check.ExitCode -ne 0) { throw ('Launcher verification failed: ' + $check.StandardError.ReadToEnd()) }
    } finally { $check.Dispose() }
}
Invoke-Check '--self-check'
$previews = Join-Path $output 'preview'
Invoke-Check ('--render-check "' + $previews + '"')
if ($Publish) {
    $runtime = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../launcher'))
    [void][IO.Directory]::CreateDirectory($runtime)
    Copy-Item -LiteralPath $helper -Destination (Join-Path $runtime 'TmsLocalStarter.exe')
    Copy-Item -LiteralPath $gui -Destination (Join-Path $runtime 'SimpleLauncher.exe')
    # Locale Remulator payload (LRProc.exe + hook DLLs + LRConfig.xml with the zh-TW profile).
    $lrSource = Join-Path $PSScriptRoot 'LocaleRemulator'
    if (Test-Path $lrSource) {
        $lrDest = Join-Path $runtime 'LocaleRemulator'
        [void][IO.Directory]::CreateDirectory($lrDest)
        Get-ChildItem -LiteralPath $lrSource -File | ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination $lrDest -Force }
    }
    Write-Output ('Published EXEs only: ' + $runtime)
} else { Write-Output ('Verified build and previews: ' + $output) }
