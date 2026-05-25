# clean-desktop-ini.ps1
# Removes desktop.ini files that Windows Explorer auto-creates.
# Android's resource compiler rejects them.
# Run once before first build: .\clean-desktop-ini.ps1

$count = 0
Get-ChildItem -Path $PSScriptRoot -Recurse -Force -Filter "desktop.ini" | ForEach-Object {
    Remove-Item $_.FullName -Force
    Write-Host "Removed: $($_.FullName)"
    $count++
}

if ($count -eq 0) {
    Write-Host "No desktop.ini files found. You're clean."
} else {
    Write-Host "`nRemoved $count desktop.ini file(s)."
}
