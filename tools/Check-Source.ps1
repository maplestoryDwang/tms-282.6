$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$package = [IO.Path]::GetFullPath((Join-Path $root '..'))
$jdk = Join-Path $package 'runtime/java'
if (!(Test-Path (Join-Path $jdk 'bin/javac.exe'))) { $jdk = $env:JAVA_HOME }
if (!$jdk -or !(Test-Path (Join-Path $jdk 'bin/javac.exe'))) { throw 'JDK 22 is required.' }
$jar = Join-Path $root 'bin/maplestory-282.5-jar-with-dependencies.jar'
if (!(Test-Path $jar)) { throw 'Run build.bat first.' }
$classes = Join-Path $root 'bin/login-checks'
$work = Join-Path $root 'bin/check-runtime'
[void][IO.Directory]::CreateDirectory($classes)
foreach ($folder in @('config','data/scripts','data/protocol')) {
    [void][IO.Directory]::CreateDirectory((Join-Path $work $folder))
}
foreach ($folder in @('login','tms282.3')) {
    Copy-Item -LiteralPath (Join-Path $package ('data/protocol/' + $folder)) -Destination (Join-Path $work 'data/protocol') -Recurse -Force
}
# Public fixtures only; never copy the running server's database credentials.
$config = @('protocol.version=tms282.3','tms282.clientBuild=28206','tms282.researchDir=data/protocol/login','tms282.preauthDir=preauth','tms282.fileReference=data/protocol/login/file-report-2825.bin','tms282.fileReport.enforce=false','tms282.newbie.enabled=false')
[IO.File]::WriteAllLines((Join-Path $work 'config/server.properties'), $config, [Text.UTF8Encoding]::new($false))
$files = @(Get-ChildItem $PSScriptRoot -Filter '*.java' -File | Select-Object -ExpandProperty FullName)
& (Join-Path $jdk 'bin/javac.exe') --enable-preview --release 22 -encoding UTF-8 -cp $jar -d $classes $files
if ($LASTEXITCODE -ne 0) { throw 'Check compilation failed.' }
Push-Location $work
try {
    foreach ($check in @('LoginAssetsCheck','LoginOnlyScriptsCheck','Tms282ChannelHandshakeCheck','Tms282ReloginTicketCheck','Tms282ReloginAuthRequestCheck')) {
        & (Join-Path $jdk 'bin/java.exe') --enable-preview '-Dtms282.tests.rejectRealDatabase=true' '-Djava.awt.headless=true' -cp ($classes + ';' + $jar) $check
        if ($LASTEXITCODE -ne 0) { throw ('Failed: ' + $check) }
    }
} finally { Pop-Location }