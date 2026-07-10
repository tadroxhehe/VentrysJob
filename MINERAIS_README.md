# Système de Blocs de Minerais

## Structure créée

Le système pour créer des blocs de minerais moddés est maintenant en place :

### Fichiers créés :

1. **`src/main/java/com/ventrys/job/block/OreBlock.java`**
   - Classe de base pour les blocs de minerais

2. **`src/main/java/com/ventrys/job/data/OreConfig.java`**
   - Système de configuration pour charger les minerais depuis JSON

3. **`src/main/resources/data/ventrysjob/ores_config.json`**
   - Fichier de configuration JSON pour définir les minerais

4. **`src/main/resources/assets/ventrysjob/models/block/ore_template.json`**
   - Modèle template pour les blocs de minerais

## Comment ajouter un nouveau minerai

### 1. Ajouter la texture

Placez votre texture PNG dans :
```
src/main/resources/assets/ventrysjob/textures/block/NOM_MINIERAIS.png
```

Par exemple, si votre texture s'appelle `minerais_iron_ore.png`, placez-la dans le dossier textures/block/

### 2. Ajouter la configuration dans `ores_config.json`

```json
{
  "id": "iron_ore",
  "name": "Minerai de Fer",
  "texture": "minerais_iron_ore",
  "hardness": 3.0,
  "requires_tool": true,
  "drops": {
    "item_id": "ventrysitem:raw_iron",
    "count": 1,
    "experience": 0
  }
}
```

### 3. Créer les fichiers de ressources

Pour chaque minerai, créez :

**Blockstate** (`src/main/resources/assets/ventrysjob/blockstates/NOM_MINIERAIS.json`):
```json
{
  "variants": {
    "": {
      "model": "ventrysjob:block/NOM_MINIERAIS"
    }
  }
}
```

**Modèle de bloc** (`src/main/resources/assets/ventrysjob/models/block/NOM_MINIERAIS.json`):
```json
{
  "parent": "block/cube_all",
  "textures": {
    "all": "ventrysjob:block/TEXTURE_NAME"
  }
}
```

**Modèle d'item** (`src/main/resources/assets/ventrysjob/models/item/NOM_MINIERAIS.json`):
```json
{
  "parent": "ventrysjob:block/NOM_MINIERAIS"
}
```

### 4. Enregistrer le bloc dans ModBlocks.java

Ajoutez dans `ModBlocks.java` :

```java
public static final RegistryObject<Block> NOM_MINIERAIS = BLOCKS.register("nom_minerais",
    () -> new OreBlock(BlockBehaviour.Properties.of(Material.STONE)
            .strength(HARDNESS)
            .requiresCorrectToolForDrops()));

public static final RegistryObject<Item> NOM_MINIERAIS_ITEM = ITEMS.register("nom_minerais",
    () -> new BlockItem(NOM_MINIERAIS.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));
```

### 5. Charger la configuration

Assurez-vous que `OreConfig.loadConfig()` est appelé dans `VentrysJob.java` dans la méthode `commonSetup`.

## Notes

- Les textures doivent être nommées selon le format `minerais_ORE.png` ou selon votre convention
- Les IDs des drops doivent utiliser le format `namespace:id` (ex: `ventrysitem:raw_iron`)
- Le système est extensible : ajoutez simplement de nouvelles entrées dans `ores_config.json`
