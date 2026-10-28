param([switch]$Activate)
$ErrorActionPreference = 'Stop'
$source = $PSScriptRoot
$package = [IO.Path]::GetFullPath((Join-Path $source '..'))
$jdk = Join-Path $package 'runtime/java/bin'
$baseJar = Join-Path $package 'bin/maplestory-282.5-local-obfuscated.jar'
$work = Join-Path $source 'bin/local-build'
$classes = Join-Path $work ('classes-' + [Guid]::NewGuid().ToString('N'))
$keys = Join-Path $env:LOCALAPPDATA 'TMS2825/Signing'
$keyFile = Join-Path $keys 'local-build.p12'
$passwordFile = Join-Path $keys 'password.txt'
function Run-JavaTool([string]$Name, [string[]]$Arguments) {
    $savedPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & (Join-Path $jdk ($Name + '.exe')) @Arguments 2>&1 | ForEach-Object { Write-Host $_ }
        if ($LASTEXITCODE -ne 0) { throw "$Name failed. The current executable has not been replaced." }
    } finally { $ErrorActionPreference = $savedPreference }
}
try {
    if (!(Test-Path -LiteralPath $baseJar)) { throw 'Missing packaged server JAR.' }
    $resolvedClasses = [IO.Path]::GetFullPath($classes)
    $allowedWork = [IO.Path]::GetFullPath((Join-Path $source 'bin/local-build')) + '\'
    if (!$resolvedClasses.StartsWith($allowedWork, [StringComparison]::OrdinalIgnoreCase)) { throw 'Build output is outside the expected directory.' }
    [void][IO.Directory]::CreateDirectory($classes)
    [void][IO.Directory]::CreateDirectory($keys)
    $signerSid = [Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    & icacls.exe $keys /inheritance:r /grant:r (('*' + $signerSid) + ':(OI)(CI)F') '*S-1-5-18:(OI)(CI)F' *> $null
    if ($LASTEXITCODE -ne 0) { throw 'Cannot secure the local signing directory.' }
    if (!(Test-Path -LiteralPath $keyFile)) {
        $bytes = New-Object byte[] 32
        $random = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $random.GetBytes($bytes) } finally { $random.Dispose() }
        [IO.File]::WriteAllText($passwordFile, [Convert]::ToBase64String($bytes), [Text.UTF8Encoding]::new($false))
        Run-JavaTool 'keytool' @('-genkeypair','-alias','tms2825-local','-keyalg','RSA','-keysize','3072','-validity','3650','-dname','CN=TMS 282.5 Local Build,O=Local,C=TW','-storetype','PKCS12','-keystore',$keyFile,'-storepass:file',$passwordFile,'-keypass:file',$passwordFile)
    }
    if (!(Test-Path -LiteralPath $passwordFile)) { throw 'The local signing password file is missing.' }
    $certificate = Join-Path $work 'local-build.cer'
    Run-JavaTool 'keytool' @('-exportcert','-alias','tms2825-local','-keystore',$keyFile,'-storepass:file',$passwordFile,'-file',$certificate)
    $cert = [Security.Cryptography.X509Certificates.X509Certificate2]::new([IO.File]::ReadAllBytes($certificate))
    $sha = [Security.Cryptography.SHA256]::Create()
    try { $fingerprint = [BitConverter]::ToString($sha.ComputeHash($cert.RawData)).Replace('-','') } finally { $sha.Dispose(); $cert.Dispose() }
    $signatureSource = Join-Path $source 'src/main/java/net/swordie/ms/lifecycle/SignaturePolicy.java'
    $generated = Join-Path $work 'generated/net/swordie/ms/lifecycle/SignaturePolicy.java'
    [void][IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($generated))
    $code = [regex]::Replace([IO.File]::ReadAllText($signatureSource), '"[0-9A-F]{64}"', ('"' + $fingerprint + '"'))
    [IO.File]::WriteAllText($generated, $code, [Text.UTF8Encoding]::new($false))
    $files = @(Get-ChildItem (Join-Path $source 'src/main/java') -Filter '*.java' -Recurse -File | Where-Object { $_.FullName -ne $signatureSource.Replace('/','\') } | ForEach-Object { '"' + $_.FullName.Replace('\','/') + '"' })
    $files += '"' + $generated.Replace('\','/') + '"'
    $argFile = Join-Path $work 'sources.txt'
    [IO.File]::WriteAllLines($argFile, $files, [Text.UTF8Encoding]::new($false))
    Write-Host 'Compiling server sources offline with bundled Java 22...'
    Run-JavaTool 'javac' @('--enable-preview','--release','22','-encoding','UTF-8','-cp',$baseJar,'-d',$classes,('@' + $argFile))
    $resourceRoot = Join-Path $source 'src/main/java'
    foreach ($file in Get-ChildItem $resourceRoot -Recurse -File | Where-Object { $_.Extension -ne '.java' }) {
        $relative = $file.FullName.Substring($resourceRoot.Length + 1)
        $target = Join-Path $classes $relative
        [void][IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($target))
        Copy-Item -LiteralPath $file.FullName -Destination $target -Force
    }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $outputJar = Join-Path $work 'maplestory-282.5-local.jar'
    $temporary = Join-Path $work ([Guid]::NewGuid().ToString('N') + '.jar')
    $base = [IO.Compression.ZipFile]::OpenRead($baseJar)
    $stream = [IO.File]::Open($temporary, [IO.FileMode]::CreateNew)
    $zip = [IO.Compression.ZipArchive]::new($stream, [IO.Compression.ZipArchiveMode]::Create, $false)
    try {
        $replacementNames = @{}
        foreach ($file in Get-ChildItem $classes -Recurse -File) { $replacementNames[$file.FullName.Substring($classes.Length + 1).Replace('\','/')] = $file.FullName }
        foreach ($entry in $base.Entries) {
            if ($entry.FullName.EndsWith('/') -or $entry.FullName.StartsWith('net/swordie/ms/') -or $replacementNames.ContainsKey($entry.FullName) -or $entry.FullName -match '^META-INF/([^/]+\.(SF|RSA|DSA|EC)|MANIFEST.MF)$') { continue }
            $target = $zip.CreateEntry($entry.FullName, [IO.Compression.CompressionLevel]::Optimal)
            $input = $entry.Open(); $output = $target.Open()
            try { $input.CopyTo($output) } finally { $input.Dispose(); $output.Dispose() }
        }
        foreach ($name in $replacementNames.Keys) { [void][IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $replacementNames[$name], $name, [IO.Compression.CompressionLevel]::Optimal) }
        $entry = $zip.CreateEntry('META-INF/MANIFEST.MF')
        $output = $entry.Open()
        try { $bytes = [Text.Encoding]::ASCII.GetBytes("Manifest-Version: 1.0`r`nMain-Class: net.swordie.ms.Server`r`n`r`n"); $output.Write($bytes,0,$bytes.Length) } finally { $output.Dispose() }
    } finally { $zip.Dispose(); $stream.Dispose(); $base.Dispose() }
    Move-Item -LiteralPath $temporary -Destination $outputJar -Force
    $trust = Join-Path $work 'tms2825-trust.p12'
    if (!(Test-Path -LiteralPath $trust)) { Run-JavaTool 'keytool' @('-importcert','-noprompt','-alias','tms2825-local','-file',$certificate,'-storetype','PKCS12','-keystore',$trust,'-storepass','public-verification') }
    Run-JavaTool 'jarsigner' @('-keystore',$keyFile,'-storepass:file',$passwordFile,'-keypass:file',$passwordFile,$outputJar,'tms2825-local')
    Run-JavaTool 'jarsigner' @('-verify','-strict','-keystore',$trust,'-storepass','public-verification',$outputJar)
    # Keep the unsigned/source API build available for the existing offline checks.
    Copy-Item -LiteralPath $outputJar -Destination (Join-Path $source 'bin/maplestory-282.5-jar-with-dependencies.jar') -Force
    if ($Activate) {
        $backup = Join-Path $package ('bin/previous-build/' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
        [void][IO.Directory]::CreateDirectory($backup)
        Copy-Item -LiteralPath $baseJar -Destination $backup
        Copy-Item -LiteralPath (Join-Path $package 'certificates/tms2825-trust.p12') -Destination $backup
        Copy-Item -LiteralPath $outputJar -Destination $baseJar -Force
        Copy-Item -LiteralPath $trust -Destination (Join-Path $package 'certificates/tms2825-trust.p12') -Force
        Copy-Item -LiteralPath $certificate -Destination (Join-Path $package 'certificates/tms2825-signing.cer') -Force
        Write-Host 'Built server activated. Local signing keys remain outside the package.'
    } else { Write-Host ('Build verified: ' + $outputJar + '. The running server was not replaced.') }
} catch { Write-Host $_.Exception.Message -ForegroundColor Red; exit 1 }
