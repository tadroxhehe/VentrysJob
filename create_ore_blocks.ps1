# Script pour créer automatiquement tous les blocs de minerais depuis les textures disponibles
# Usage: .\create_ore_blocks.ps1

$textureSourceDir = "C:\Users\carlo\OneDrive\Bureau\VentrysItem\minerais"
$targetResourcesDir = "src\main\resources\assets\ventrysjob"
$targetTexturesDir = "$targetResourcesDir\textures\block"
$targetModelsDir = "$targetResourcesDir\models\block"
$targetItemModelsDir = "$targetResourcesDir\models\item"
$targetBlockstatesDir = "$targetResourcesDir\blockstates"
$targetLangFile = "$targetResourcesDir\lang\fr_fr.json"
$modBlocksFile = "src\main\java\com\ventrys\job\init\ModBlocks.java"

# Créer les dossiers si nécessaire
New-Item -ItemType Directory -Force -Path $targetTexturesDir | Out-Null
New-Item -ItemType Directory -Force -Path $targetModelsDir | Out-Null
New-Item -ItemType Directory -Force -Path $targetItemModelsDir | Out-Null
New-Item -ItemType Directory -Force -Path $targetBlockstatesDir | Out-Null

if (-not (Test-Path $textureSourceDir)) {
    Write-Host "ERREUR: Le dossier source n'existe pas: $textureSourceDir" -ForegroundColor Red
    Write-Host "Veuillez verifier le chemin du dossier des textures." -ForegroundColor Yellow
    exit 1
}

# Récupérer toutes les textures PNG
$textureFiles = Get-ChildItem -Path $textureSourceDir -Filter "*.png" | Where-Object { $_.Name -like "*ore*" -or $_.Name -like "*minerai*" }

if ($textureFiles.Count -eq 0) {
    Write-Host "Aucune texture de minerai trouvee dans $textureSourceDir" -ForegroundColor Yellow
    exit 0
}

Write-Host "`nCreation des blocs de minerais depuis $($textureFiles.Count) texture(s)...`n" -ForegroundColor Cyan

$oreBlocks = @()
$langEntries = @()

foreach ($textureFile in $textureFiles) {
    # Extraire le nom du minerai depuis le nom de fichier
    # Format attendu: minerais_ORE.png ou ORE_ore.png -> ore
    $fileName = $textureFile.BaseName
    $oreId = $fileName -replace "minerais_", "" -replace "_ore", "" -replace "ore_", "" -replace "_", "_"
    
    # Normaliser l'ID (minuscules, underscores)
    $oreId = $oreId.ToLower()
    
    Write-Host "  - Traitement: $fileName -> $oreId" -ForegroundColor White
    
    # Copier la texture
    $targetTexturePath = Join-Path $targetTexturesDir "$oreId.png"
    Copy-Item -Path $textureFile.FullName -Destination $targetTexturePath -Force
    Write-Host "    Texture copiee: $targetTexturePath" -ForegroundColor Green
    
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
    Write-Host "    Blockstate cree: $blockstatePath" -ForegroundColor Green
    
    # Créer le modèle de bloc
    $blockModelJson = @{
        parent = "block/cube_all"
        textures = @{
            all = "ventrysjob:block/$oreId"
        }
    }
    $blockModelPath = Join-Path $targetModelsDir "$oreId.json"
    $blockModelJson | ConvertTo-Json -Depth 10 | Set-Content -Path $blockModelPath -Encoding UTF8
    Write-Host "    Modele de bloc cree: $blockModelPath" -ForegroundColor Green
    
    # Créer le modèle d'item
    $itemModelJson = @{
        parent = "ventrysjob:block/$oreId"
    }
    $itemModelPath = Join-Path $targetItemModelsDir "$oreId.json"
    $itemModelJson | ConvertTo-Json -Depth 10 | Set-Content -Path $itemModelPath -Encoding UTF8
    Write-Host "    Modele d'item cree: $itemModelPath" -ForegroundColor Green
    
    # Générer un nom lisible pour la traduction
    $readableName = $oreId -replace "_", " " -split " " | ForEach-Object { 
        $_.Substring(0,1).ToUpper() + $_.Substring(1).ToLower() 
    }
    $readableName = ($readableName -join " ")
    $readableName = "Minerai de $readableName"
    
    $oreBlocks += @{
        id = $oreId
        name = $readableName
    }
    
    $langEntries += "  `"block.ventrysjob.$oreId`": `"$readableName`","
    
    Write-Host "    OK: $oreId`n" -ForegroundColor Green
}

# Générer le code Java pour ModBlocks.java
Write-Host "`nGeneration du code Java pour ModBlocks.java...`n" -ForegroundColor Cyan

$javaCode = @"
    // Blocs de minerais
$($oreBlocks | ForEach-Object {
    $blockId = $_.id
    $upperId = ($blockId -split "_" | ForEach-Object { $_.Substring(0,1).ToUpper() + $_.Substring(1).ToLower() }) -join "_"
    $upperId = $upperId.ToUpper()
    "    public static final RegistryObject<Block> ${upperId}_ORE = BLOCKS.register(`"$blockId`"," + [Environment]::NewLine +
    "        () -> new OreBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.STONE)" + [Environment]::NewLine +
    "                .strength(3.0f)" + [Environment]::NewLine +
    "                .requiresCorrectToolForDrops()));" + [Environment]::NewLine +
    "    " + [Environment]::NewLine +
    "    public static final RegistryObject<Item> ${upperId}_ORE_ITEM = ITEMS.register(`"$blockId`"," + [Environment]::NewLine +
    "        () -> new BlockItem(${upperId}_ORE.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));"
}) -join ([Environment]::NewLine + [Environment]::NewLine)
"@

Write-Host "Code Java genere ($($oreBlocks.Count) blocs):" -ForegroundColor Yellow
Write-Host $javaCode -ForegroundColor White

Write-Host "`nEntrees de traduction generees:" -ForegroundColor Yellow
$langEntries | ForEach-Object { Write-Host $_ -ForegroundColor White }

Write-Host "`n" -NoNewline
Write-Host "=" * 80 -ForegroundColor Cyan
Write-Host "RESUME" -ForegroundColor Cyan
Write-Host "=" * 80 -ForegroundColor Cyan
Write-Host "$($oreBlocks.Count) bloc(s) de minerai(s) cree(s) avec succes!" -ForegroundColor Green
Write-Host "`nProchaines etapes:" -ForegroundColor Yellow
Write-Host "1. Copiez le code Java ci-dessus dans ModBlocks.java (avant la ligne '// Note: La fourche...')" -ForegroundColor White
Write-Host "2. Ajoutez les entrees de traduction dans lang/fr_fr.json" -ForegroundColor White
Write-Host "3. Compilez et testez en jeu!" -ForegroundColor White

# Sauvegarder le code Java dans un fichier temporaire
$javaCode | Out-File -FilePath "ore_blocks_java_code.txt" -Encoding UTF8
Write-Host "`nCode Java sauvegarde dans: ore_blocks_java_code.txt" -ForegroundColor Green
