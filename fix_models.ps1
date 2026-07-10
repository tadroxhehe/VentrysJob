# Script to fix Blockbench models: add cullface: "down" to all "down" faces and fix texture references

function Fix-Model {
    param(
        [string]$InputFile,
        [string]$OutputFile,
        [string]$TextureKey,
        [string]$TextureValue
    )
    
    $json = Get-Content $InputFile -Raw -Encoding UTF8 | ConvertFrom-Json
    
    # Fix textures
    if ($TextureKey -and $TextureValue) {
        $json.textures.$TextureKey = $TextureValue
        $json.textures.particle = $TextureValue
    }
    
    # Add cullface: "down" to all "down" faces
    foreach ($element in $json.elements) {
        if ($element.faces.down) {
            if (-not $element.faces.down.PSObject.Properties['cullface']) {
                $element.faces.down | Add-Member -MemberType NoteProperty -Name "cullface" -Value "down" -Force
            } else {
                $element.faces.down.cullface = "down"
            }
        }
    }
    
    # Convert back to JSON with proper formatting
    $jsonString = $json | ConvertTo-Json -Depth 100
    # Fix formatting to use tabs
    $jsonString = $jsonString -replace '  ', "`t"
    $jsonString = $jsonString -replace '": ', '": '
    $jsonString = $jsonString -replace ', "', ",`n`t`t`""
    $jsonString = $jsonString -replace '": "', '": "'
    
    # Write output
    [System.IO.File]::WriteAllText($OutputFile, $jsonString, [System.Text.Encoding]::UTF8)
    Write-Host "Fixed: $OutputFile"
}

# Fix artisan_table.json (use table_menuisier.json with table_menuisier3 texture)
Fix-Model -InputFile "json_textures/table artisanat/table_menuisier.json" -OutputFile "src/main/resources/assets/ventrysjob/models/block/artisan_table.json" -TextureKey "2" -TextureValue "ventrysjob:block/table_menuisier3"

# Fix metier_tisser.json (use couturier.json with couturier texture)
Fix-Model -InputFile "json_textures/metier_à_tisser/couturier.json" -OutputFile "src/main/resources/assets/ventrysjob/models/block/metier_tisser.json" -TextureKey "0" -TextureValue "ventrysjob:block/couturier"

# Fix existing models: add cullface to down faces
Fix-Model -InputFile "src/main/resources/assets/ventrysjob/models/block/apothicaire_table.json" -OutputFile "src/main/resources/assets/ventrysjob/models/block/apothicaire_table.json"
Fix-Model -InputFile "src/main/resources/assets/ventrysjob/models/block/forgeron_table.json" -OutputFile "src/main/resources/assets/ventrysjob/models/block/forgeron_table.json"
Fix-Model -InputFile "src/main/resources/assets/ventrysjob/models/block/cuisinier_table.json" -OutputFile "src/main/resources/assets/ventrysjob/models/block/cuisinier_table.json"

Write-Host "All models fixed!"

