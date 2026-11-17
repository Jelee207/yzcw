#Requires -Version 5.1
<#
  狐缘阁 · 一键启动器
  不依赖系统 PATH：自行探测 Node / JDK / Maven / MySQL。
  全程写日志到「启动日志.txt」，失败时给出具体原因。
  用法：powershell -ExecutionPolicy Bypass -File start-huyao.ps1 [-Mode all|db|backend|frontend|stop|check]
#>
param(
    [ValidateSet('all','db','backend','frontend','stop','check')]
    [string]$Mode = 'all'
)

$ErrorActionPreference = 'Continue'
$Root      = Split-Path -Parent $MyInvocation.MyCommand.Path
$Backend   = Join-Path $Root 'springBootTest'
$Frontend  = Join-Path $Root 'huyao-login-vue'
$LogPath   = Join-Path $Root '启动日志.txt'
$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)

# 日志按次追加，保留最近的运行记录；过大时清空重建
if ((Test-Path $LogPath) -and ((Get-Item $LogPath).Length -gt 200KB)) {
    [System.IO.File]::WriteAllText($LogPath, '', $Utf8NoBom)
}
[System.IO.File]::AppendAllText(
    $LogPath,
    ("`r`n=== 狐缘阁启动器 {0}  Mode={1} ===`r`n" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $Mode),
    $Utf8NoBom)

function Write-Log {
    param([string]$Text, [string]$Level = 'INFO')
    $line = '[{0}] [{1,-5}] {2}' -f (Get-Date -Format 'HH:mm:ss'), $Level, $Text
    switch ($Level) {
        'ERROR' { Write-Host $line -ForegroundColor Red }
        'WARN'  { Write-Host $line -ForegroundColor Yellow }
        'OK'    { Write-Host $line -ForegroundColor Green }
        default { Write-Host $line }
    }
    [System.IO.File]::AppendAllText($script:LogPath, $line + "`r`n", $script:Utf8NoBom)
}

function Test-Port {
    param([int]$Port)
    try {
        $c = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
        if ($c) { return $true }
    } catch { }
    $raw = netstat -ano 2>$null | Select-String -Pattern (":$Port\s+.*LISTENING")
    return [bool]$raw
}

function Wait-Port {
    param([int]$Port, [int]$Seconds, [string]$Label)
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $lastReport = -10
    while ($sw.Elapsed.TotalSeconds -lt $Seconds) {
        if (Test-Port $Port) { return $true }
        $elapsed = [int]$sw.Elapsed.TotalSeconds
        if (($elapsed - $lastReport) -ge 15) {
            Write-Log ("{0} 尚未就绪，已等待 {1}s ..." -f $Label, $elapsed)
            $lastReport = $elapsed
        }
        Start-Sleep -Milliseconds 800
    }
    return (Test-Port $Port)
}

function Resolve-Tool {
    param([string[]]$Candidates, [string]$FallbackCommand)
    foreach ($c in $Candidates) {
        if ($c -and (Test-Path $c)) { return (Resolve-Path $c).Path }
    }
    $cmd = Get-Command $FallbackCommand -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    return $null
}

function Find-NodeDir {
    $c = Get-Command node.exe -ErrorAction SilentlyContinue
    if ($c) {
        $d = Split-Path -Parent $c.Source
        if (Test-Path (Join-Path $d 'npm.cmd')) { return $d }
    }
    # WorkBuddy 内置 Node（版本目录带版本号，用通配扫描取最新）
    $base = Join-Path $env:USERPROFILE '.workbuddy\binaries\node\versions'
    if (Test-Path $base) {
        $dirs = Get-ChildItem -Path $base -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending
        foreach ($d in $dirs) {
            if ((Test-Path (Join-Path $d.FullName 'node.exe')) -and (Test-Path (Join-Path $d.FullName 'npm.cmd'))) {
                return $d.FullName
            }
        }
    }
    foreach ($d in @('C:\Program Files\nodejs', (Join-Path $env:LOCALAPPDATA 'Programs\nodejs'), 'D:\dev\nodejs')) {
        if ((Test-Path (Join-Path $d 'node.exe')) -and (Test-Path (Join-Path $d 'npm.cmd'))) { return $d }
    }
    return $null
}

function Stop-ByPort {
    param([int]$Port)
    try {
        $conns = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
        foreach ($c in $conns) {
            $p = Get-Process -Id $c.OwningProcess -ErrorAction SilentlyContinue
            if ($p) {
                Write-Log ("停止端口 {0} 上的进程 {1} ({2})" -f $Port, $p.Id, $p.ProcessName)
                Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue
            }
        }
    } catch {
        Write-Log ("停止端口 {0} 时出错：{1}" -f $Port, $_.Exception.Message) 'WARN'
    }
}

# ---------------------------------------------------------------- 环境探测
Write-Log '===== 环境自检 ====='
Write-Log ("项目根目录: {0}" -f $Root)

$nodeDir = Find-NodeDir
$nodeExe = if ($nodeDir) { Join-Path $nodeDir 'node.exe' } else { $null }
$npmCmd  = if ($nodeDir) { Join-Path $nodeDir 'npm.cmd'  } else { $null }
$javaExe = Resolve-Tool @('D:\dev\jdk21\jdk-21.0.13+9\bin\java.exe', 'D:\dev\jdk17\bin\java.exe') 'java.exe'
$mvnCmd  = Resolve-Tool @('D:\dev\maven-3.9.9\bin\mvn.cmd', (Join-Path $Backend 'mvnw.cmd')) 'mvn.cmd'
$mysqlHome = 'D:\dev\mysql-8.0.29-winx64'
$mysqld  = Resolve-Tool @((Join-Path $mysqlHome 'bin\mysqld.exe')) 'mysqld.exe'
$mysqlExe= Resolve-Tool @((Join-Path $mysqlHome 'bin\mysql.exe'))  'mysql.exe'
$myIni   = Join-Path $mysqlHome 'my.ini'

if ($nodeDir)  { Write-Log ("Node   : {0}" -f $nodeExe)  'OK' } else { Write-Log 'Node/npm 未找到（已查 PATH、WorkBuddy 内置目录、常见安装位置）' 'ERROR' }
if ($javaExe)  { Write-Log ("Java   : {0}" -f $javaExe)  'OK' } else { Write-Log 'java.exe 未找到' 'ERROR' }
if ($mvnCmd)   { Write-Log ("Maven  : {0}" -f $mvnCmd)   'OK' } else { Write-Log 'Maven / mvnw.cmd 未找到' 'ERROR' }
if ($mysqld)   { Write-Log ("MySQL  : {0}" -f $mysqld)   'OK' } else { Write-Log '便携 mysqld.exe 未找到（若系统 MySQL 服务已在运行则不影响）' 'WARN' }
if ($mysqlExe) { Write-Log ("客户端 : {0}" -f $mysqlExe) 'OK' } else { Write-Log 'mysql.exe 未找到，将跳过自动建库检查' 'WARN' }

# 显式补齐子进程 PATH，不依赖用户级环境变量是否刷新
$envParts = @()
if ($nodeDir)  { $envParts += $nodeDir }
if ($javaExe)  { $envParts += (Split-Path -Parent $javaExe); $env:JAVA_HOME = (Split-Path -Parent (Split-Path -Parent $javaExe)) }
if ($mvnCmd)   { $envParts += (Split-Path -Parent $mvnCmd) }
if ($mysqlExe) { $envParts += (Split-Path -Parent $mysqlExe) }
if ($envParts.Count -gt 0) { $env:PATH = (($envParts -join ';') + ';' + $env:PATH) }
# 消除工具注入的 SERVER__PORT 污染（历史坑：后端会被带偏到随机 5 位端口）
$env:SERVER_PORT   = '8081'
$env:SERVER__PORT  = '8081'
Write-Log '已固定 SERVER_PORT / SERVER__PORT = 8081' 'OK'

if ($Mode -eq 'check') {
    if (-not $nodeDir -or -not $javaExe -or -not $mvnCmd) {
        Write-Log '环境自检未通过，请查看上面标红的项。' 'ERROR'
        exit 1
    }
    Write-Log ('端口现状: 3306={0} 8081={1} 5173={2}' -f (Test-Port 3306), (Test-Port 8081), (Test-Port 5173))
    Write-Log '环境自检通过。' 'OK'
    exit 0
}

if ($Mode -eq 'stop') {
    Write-Log '===== 停止服务 ====='
    Stop-ByPort 8081
    Stop-ByPort 5173
    Write-Log '已停止 8081 / 5173（MySQL 保持运行，数据不会丢）。' 'OK'
    exit 0
}

if (-not $nodeDir -or -not $javaExe -or -not $mvnCmd) {
    Write-Log '工具链不完整，启动中止。详细探测结果见上方与启动日志.txt。' 'ERROR'
    exit 1
}

# ---------------------------------------------------------------- MySQL
if ($Mode -eq 'all' -or $Mode -eq 'db') {
    Write-Log '===== [1/3] MySQL 3306 ====='
    if (Test-Port 3306) {
        Write-Log '3306 已在监听，跳过启动。' 'OK'
    } elseif (-not $mysqld -or -not (Test-Path $myIni)) {
        Write-Log ("3306 未监听且找不到便携 MySQL 或 my.ini：{0}" -f $mysqlHome) 'ERROR'
        Write-Log '请启动系统 MySQL 服务，或把便携包放回 D:\dev\mysql-8.0.29-winx64' 'ERROR'
        exit 1
    } else {
        Write-Log ("启动 mysqld: {0}" -f $mysqld)
        try {
            $p = Start-Process -FilePath $mysqld `
                -ArgumentList ("--defaults-file={0}" -f $myIni), '--console' `
                -WorkingDirectory (Split-Path -Parent $mysqld) `
                -WindowStyle Minimized -PassThru -ErrorAction Stop
            Write-Log ("mysqld 进程 PID={0}" -f $p.Id)
        } catch {
            Write-Log ("mysqld 启动失败：{0}" -f $_.Exception.Message) 'ERROR'
            exit 1
        }
        if (-not (Wait-Port 3306 90 'MySQL')) {
            $errFile = Get-ChildItem (Join-Path $mysqlHome 'data\*.err') -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
            Write-Log 'MySQL 90 秒内未就绪。' 'ERROR'
            if ($errFile) {
                Write-Log ("错误日志：{0}" -f $errFile.FullName) 'ERROR'
                Get-Content $errFile.FullName -Tail 12 | ForEach-Object { Write-Log ("  {0}" -f $_) 'ERROR' }
            }
            exit 1
        }
        Write-Log 'MySQL 已在 3306 就绪。' 'OK'
    }

    # 初始化 / 兼容旧库
    if ($mysqlExe) {
        $baseArgs = @('-uroot','-p123456','-h127.0.0.1','-P3306','--protocol=tcp')
        & $mysqlExe @baseArgs -e 'USE huyao' 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Log 'huyao 库已存在，跳过初始化（不会清空数据）。' 'OK'
            & $mysqlExe @baseArgs --force -e 'ALTER TABLE huyao.tb_user ADD COLUMN avatar MEDIUMTEXT DEFAULT NULL' 2>$null | Out-Null
        } else {
            $sqlFile = Join-Path $Backend 'sql\huyao.sql'
            if (-not (Test-Path $sqlFile)) {
                Write-Log ("huyao 库不存在，且找不到建库脚本 {0}" -f $sqlFile) 'ERROR'
                exit 1
            }
            Write-Log ("huyao 库不存在，导入 {0} ..." -f $sqlFile)
            $sql = [System.IO.File]::ReadAllText($sqlFile)
            $sql | & $mysqlExe @baseArgs --default-character-set=utf8mb4 2>$null | Out-Null
            if ($LASTEXITCODE -ne 0) {
                Write-Log '建库失败：确认 MySQL 已就绪、账号为 root / 123456。' 'ERROR'
                exit 1
            }
            Write-Log '建库完成，测试账号 admin / 123456。' 'OK'
        }
    }
}

# ---------------------------------------------------------------- 后端
if ($Mode -eq 'all' -or $Mode -eq 'backend') {
    Write-Log '===== [2/3] 后端 Spring Boot 8081 ====='
    if (Test-Port 8081) {
        Write-Log '8081 已在监听，跳过启动。' 'OK'
    } else {
        Write-Log ("Maven 现场编译并启动：{0}" -f $mvnCmd)
        try {
            $p = Start-Process -FilePath $mvnCmd `
                -ArgumentList '-f','pom.xml','-B','-DskipTests','spring-boot:run' `
                -WorkingDirectory $Backend -WindowStyle Normal -PassThru -ErrorAction Stop
            Write-Log ("后端进程 PID={0}（日志在独立窗口与 springBootTest\log\xx.log）" -f $p.Id)
        } catch {
            Write-Log ("后端启动失败：{0}" -f $_.Exception.Message) 'ERROR'
            exit 1
        }
    }
}

# ---------------------------------------------------------------- 前端
if ($Mode -eq 'all' -or $Mode -eq 'frontend') {
    Write-Log '===== [3/3] 前端 Vite 5173 ====='
    if (Test-Port 5173) {
        Write-Log '5173 已在监听，跳过启动。' 'OK'
    } else {
        $lock = Join-Path $Frontend 'node_modules\.package-lock.json'
        if (-not (Test-Path $lock)) {
            Write-Log 'node_modules 缺失，执行 npm ci（约需 40-60 秒）...'
            Push-Location $Frontend
            & $npmCmd ci --no-audit --no-fund 2>&1 | ForEach-Object { Write-Log ('  ' + $_) }
            $npmCode = $LASTEXITCODE
            Pop-Location
            if ($npmCode -ne 0) {
                Write-Log ("npm ci 失败（退出码 {0}），检查网络或 package-lock.json。" -f $npmCode) 'ERROR'
                exit 1
            }
            Write-Log '依赖安装完成。' 'OK'
        } else {
            Write-Log 'node_modules 已就绪，跳过安装。' 'OK'
        }
        try {
            $p = Start-Process -FilePath $npmCmd -ArgumentList 'run','dev' `
                -WorkingDirectory $Frontend -WindowStyle Normal -PassThru -ErrorAction Stop
            Write-Log ("前端进程 PID={0}" -f $p.Id)
        } catch {
            Write-Log ("前端启动失败：{0}" -f $_.Exception.Message) 'ERROR'
            exit 1
        }
    }
}

# ---------------------------------------------------------------- 就绪等待
if ($Mode -eq 'all') {
    Write-Log '===== 等待服务就绪（最长 180 秒） ====='
    $ok8081 = Wait-Port 8081 180 '后端 8081'
    $ok5173 = Wait-Port 5173 10  '前端 5173'
    if ($ok8081) { Write-Log '后端 8081 就绪。' 'OK' } else { Write-Log '后端 8081 未就绪，请看后端窗口日志或 springBootTest\log\xx.log' 'ERROR' }
    if ($ok5173) { Write-Log '前端 5173 就绪。' 'OK' } else { Write-Log '前端 5173 未就绪，请看前端窗口日志' 'ERROR' }

    Write-Host ''
    if ($ok8081 -and $ok5173) {
        Write-Log '启动完成！请打开 http://localhost:5173   （测试账号 admin / 123456）' 'OK'
        Write-Log '详细过程见 启动日志.txt；停止服务用 停止项目.bat' 'OK'
        exit 0
    } else {
        Write-Log '启动未完成，窗口不会自动关闭。请把 启动日志.txt 发给助手定位。' 'ERROR'
        exit 1
    }
}

exit 0
