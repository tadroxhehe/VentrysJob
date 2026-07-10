# Script pour générer automatiquement tous les blocs de minerais depuis une liste
# Usage: .\generate_ore_blocks.ps1

$listFile = "ore_blocks_list.txt"
$targetResourcesDir = "src\main\resources\assets\ventrysjob"
$targetTexturesDir = "$targetResourcesDir\textures\block"
$targetModelsDir = "$targetResourcesDir\models\block"
$targetItemModelsDir = "$targetResourcesDir\models\item"
$targetBlockstatesDir = "$targetResourcesDir\blockstates"
$modBlocksFile = "src\main\java\com\ventrys\job\init\ModBlocks.java"

# Créer les dossiers si nécessaire
New-Item -ItemType Directory -Force -Path $targetTexturesDir | Out-Null
New-Item -ItemType Directory -Force -Path $targetModelsDir | Out-Null
New-Item -ItemType Directory -Force -Path $targetItemModelsDir | Out-Null
New-Item -ItemType Directory -Force -Path $targetBlockstatesDir | Out-Null

if (-not (Test-Path $listFile)) {
    Write-Host "ERREUR: Fichier de liste introuvable: $listFile" -ForegroundColor Red
    Write-Host "Creer le fichier avec la liste des minerais (format: id_minerai;nom_affiché)" -ForegroundColor Yellow
    exit 1
}

$oreList = Get-Content $listFile | Where-Object { $_ -notmatch "^#" -and $_.Trim() -ne "" }

if ($oreList.Count -eq 0) {
    Write-Host "Aucun minerai trouve dans $listFile" -ForegroundColor Yellow
    exit 0
}

Write-Host "`nGeneration de $($oreList.Count) bloc(s) de minerai(s)...`n" -ForegroundColor Cyan

$javaBlocks = @()
$javaItems = @()
$langEntries = @()

foreach ($line in $oreList) {
    $parts = $line -split ";"
    if ($parts.Length -lt 2) {
        Write-Host "Ligne ignoree (format invalide): $line" -ForegroundColor Yellow
        continue
    }
    
    $oreId = $parts[0].Trim()
    $oreName = $parts[1].Trim()
    
    Write-Host "  - Creation: $oreId ($oreName)" -ForegroundColor White
    
    # Créer le blockstate
    $blockstateJson = @{
        variants = @{
            "" = @{
                model = "ventrysjob:block/$oreId"
            }
        }
    }
    $blockstatePath = Join-Path $targetBlockstatesDir "$oreId.json"
    $blockstateJson | ConvertTo-Json -Depth 10 | Set-Content -Path $blockstatePath -Encoding UTF8
    
    # Créer le modèle de bloc
    $blockModelJson = @{
        parent = "block/cube_all"
        textures = @{
            all = "ventrysjob:block/$oreId"
        }
    }
    $blockModelPath = Join-Path $targetModelsDir "$oreId.json"
    $blockModelJson | ConvertTo-Json -Depth 10 | Set-Content -Path $blockModelPath -Encoding UTF8
    
    # Créer le modèle d'item
    $itemModelJson = @{
        parent = "ventrysjob:block/$oreId"
    }
    $itemModelPath = Join-Path $targetItemModelsDir "$oreId.json"
    $itemModelJson | ConvertTo-Json -Depth 10 | Set-Content -Path $itemModelPath -Encoding UTF8
    
    # Générer le nom de variable Java (SCREAMING_SNAKE_CASE)
    $javaVarName = ($oreId -split "_" | ForEach-Object { $_.ToUpper() }) -join "_"
    
    $javaBlocks += "    public static final RegistryObject<Block> ${javaVarName} = BLOCKS.register(`"$oreId`",`n        () -> new com.ventrys.job.block.OreBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.STONE)`n                .strength(3.0f)`n                .requiresCorrectToolForDrops()));"
    
    $javaItems += "    public static final RegistryObject<Item> ${javaVarName}_ITEM = ITEMS.register(`"$oreId`",`n        () -> new BlockItem(${javaVarName}.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));"
    
    $langEntries += "  `"block.ventrysjob.$oreId`": `"$oreName`","
    
    Write-Host "    OK`n" -ForegroundColor Green
}

# Générer le code Java complet
$javaCode = ($javaBlocks -join "`n`n") + "`n`n" + ($javaItems -join "`n`n")

Write-Host "=" * 80 -ForegroundColor Cyan
Write-Host "CODE JAVA A AJOUTER DANS ModBlocks.java:" -ForegroundColor Cyan
Write-Host "=" * 80 -ForegroundColor Cyan
Write-Host $javaCode -ForegroundColor White
Write-Host "`n"

Write-Host "ENTREES DE TRADUCTION A AJOUTER DANS lang/fr_fr.json:" -ForegroundColor Cyan
Write-Host "=" * 80 -ForegroundColor Cyan
$langEntries | ForEach-Object { Write-Host $_ -ForegroundColor White }

# Sauvegarder le code
$javaCode | Out-File -FilePath "ore_blocks_code.txt" -Encoding UTF8
$langEntries -join "`n" | Out-File -FilePath "ore_blocks_lang.txt" -Encoding UTF8

Write-Host "`nCode sauvegarde dans:" -ForegroundColor Green
Write-Host "  - ore_blocks_code.txt (code Java)" -ForegroundColor White
Write-Host "  - ore_blocks_lang.txt (traductions)" -ForegroundColor White

Write-Host "`nIMPORTANT: Les textures PNG doivent etre copiees manuellement dans:" -ForegroundColor Yellow
Write-Host "  $targetTexturesDir" -ForegroundColor White
Write-Host "`nFormat attendu: {ore_id}.png (ex: iron_ore.png)" -ForegroundColor White
