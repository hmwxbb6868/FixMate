$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $root '.env'
if (Test-Path -LiteralPath $envPath) {
    Write-Host 'Using the existing .env file.'
    return
}

function New-FixMateSecret {
    $bytes = New-Object byte[] 32
    $random = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $random.GetBytes($bytes) } finally { $random.Dispose() }
    return [BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
}

$lines = @(
    "FIXMATE_MYSQL_ROOT_PASSWORD=$(New-FixMateSecret)"
    "FIXMATE_MYSQL_PASSWORD=$(New-FixMateSecret)"
    "FIXMATE_REDIS_PASSWORD=$(New-FixMateSecret)"
    "FIXMATE_JWT_SECRET=$(New-FixMateSecret)"
    'FIXMATE_API_BIND=127.0.0.1'
    'WECHAT_APP_ID='
    'WECHAT_APP_SECRET='
    'ALIYUN_OSS_ENDPOINT='
    'ALIYUN_OSS_BUCKET='
    'ALIYUN_OSS_ACCESS_KEY_ID='
    'ALIYUN_OSS_ACCESS_KEY_SECRET='
)
$file = [IO.File]::Open($envPath, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write)
try {
    $bytes = [Text.Encoding]::UTF8.GetBytes(($lines -join "`n") + "`n")
    $file.Write($bytes, 0, $bytes.Length)
} finally {
    $file.Dispose()
}
Write-Host 'Created .env with per-installation secrets.'
