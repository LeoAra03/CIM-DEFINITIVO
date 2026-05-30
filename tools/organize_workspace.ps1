<#
Organize workspace by moving temporary files into archive.
Usage:
  .\organize_workspace.ps1 -Root "C:\Path\To\Practica_2" [-DryRun]

DryRun default: true (no files moved). Use -DryRun:$false to perform actual moves.
#>

param(
    [string]$Root = "C:\Users\Leo\Desktop\Test Practica2\Practica_2",
    [switch]$DryRun = $true
)

Write-Host "Workspace organizer"
Write-Host "Root: $Root"
Write-Host "DryRun: $DryRun"

$archive = Join-Path $Root "archive"
if (!(Test-Path $archive)) { New-Item -ItemType Directory -Path $archive | Out-Null }

$patterns = @("*_Fixed.kt", "*Fixed.*", "*.old", "Template.rar", "*~", "*.tmp")

foreach ($pattern in $patterns) {
    Write-Host "Searching pattern: $pattern"
    $matches = Get-ChildItem -Path $Root -Recurse -Include $pattern -ErrorAction SilentlyContinue
    foreach ($m in $matches) {
        $dest = Join-Path $archive $($m.Name)
        if ($DryRun) {
            Write-Host "[DRY] Would move: $($m.FullName) -> $dest"
        } else {
            Write-Host "Moving: $($m.FullName) -> $dest"
            try { Move-Item -Path $m.FullName -Destination $dest -Force } catch { Write-Host "Error moving: $_" }
        }
    }
}

Write-Host "Done. Review archive folder: $archive"
