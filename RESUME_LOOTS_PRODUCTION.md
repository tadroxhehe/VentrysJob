# Résumé des Loots et Systèmes de Production/Transformation

## 🪓 SYSTÈME D'EXTRACTION (Métier: OUVRIER)

### 1. Extraction de Bûches (Hache)
**Fichier:** `extraction_config.json` → `oak_configs`
**Clics requis:** 10
**Outils:** Haches moddées (`ventrysitem:item_hache`, `ventrysitem:item_hache_en_bronze`)

**Loots:**
- Tous les types de bûches (oak, birch, spruce, acacia, dark_oak, mangrove, cherry) → **2x bûches du même type**
- Les bûches extraites ont un tag NBT `ventrysjob:extracted` pour permettre le sciage

**Blocs extractibles:**
- `minecraft:oak_log` → `minecraft:oak_log` x2
- `minecraft:oak_wood` → `minecraft:oak_log` x2
- `minecraft:stripped_oak_log` → `minecraft:oak_log` x2
- `minecraft:stripped_oak_wood` → `minecraft:oak_log` x2
- (Même logique pour birch, spruce, acacia, dark_oak, mangrove, cherry)

---

### 2. Sciage de Bûches (Scie)
**Fichier:** `extraction_config.json` → `saw_configs`
**Clics requis:** 5
**Outils:** Scies moddées (`ventrysitem:item_scie`, `ventrysitem:item_scie_en_bronze`)
**Prérequis:** La bûche doit avoir été extraite avec une hache (tag NBT `ventrysjob:extracted`)

**Loots:**
- `minecraft:oak_log` → `minecraft:oak_planks` x4
- `minecraft:birch_log` → `minecraft:birch_planks` x4
- `minecraft:spruce_log` → `minecraft:spruce_planks` x4
- `minecraft:acacia_log` → `minecraft:acacia_planks` x4
- `minecraft:dark_oak_log` → `minecraft:dark_oak_planks` x4
- `minecraft:mangrove_log` → `minecraft:mangrove_planks` x4
- `minecraft:cherry_log` → `minecraft:cherry_planks` x4

---

### 3. Extraction de Minerais (Pioche)
**Fichier:** `extraction_config.json` → `mining_configs`
**Clics requis:** 10
**Outils:** Pioches moddées (`ventrysitem:item_pioche`, `ventrysitem:item_pioche_en_bronze`)

**Loots:**
- `minecraft:iron_ore` → `minecraft:raw_iron` x1
- `minecraft:gold_ore` → `minecraft:raw_gold` x1
- `minecraft:copper_ore` → `minecraft:raw_copper` x1

---

### 4. Extraction de Pierre (Burin)
**Fichier:** `extraction_config.json` → `stone_configs`
**Clics requis:** 10
**Outils:** Burins moddés (`ventrysitem:item_burin`, `ventrysitem:item_burin_en_bronze`)

**Loots:**
- `minecraft:stone` → `minecraft:cobblestone` x1
- `minecraft:cobblestone` → `minecraft:gravel` x1

---

### 5. Extraction de Calcite (Maillet)
**Fichier:** `extraction_config.json` → `calcite_configs`
**Clics requis:** 10
**Outils:** Maillets moddés (`ventrysitem:item_maillet`)

**Loots:**
- `minecraft:calcite` → `minecraft:calcite` x1

---

### 6. Extraction de Sable (Pelle)
**Fichier:** `extraction_config.json` → `sand_configs`
**Clics requis:** 10
**Outils:** Pelles moddées (`ventrysitem:item_pelle`, `ventrysitem:item_pelle_en_bronze`)

**Loots:**
- `minecraft:red_sand` → `minecraft:red_sand` x1
- `minecraft:red_sand` → `minecraft:red_sand` x1

---

## 🌾 SYSTÈME DE RÉCOLTE (Métier: PAYSAN)

### 7. Récolte de Cultures (Fourche)
**Fichier:** `extraction_config.json` → `crop_configs`
**Clics requis:** 5
**Outils:** Fourches moddées (`ventrysitem:item_fourche`, `ventrysitem:item_fourche_en_bronze`)

**Loots:**
- `minecraft:wheat` → `minecraft:wheat` x1
- `minecraft:carrots` → `minecraft:carrot` x1
- `minecraft:potatoes` → `minecraft:potato` x1
- `minecraft:beetroots` → `minecraft:beetroot` x1

---

## 🔥 SYSTÈMES DE TRANSFORMATION

### 8. Four Ouvrier (Métier: OUVRIER)
**Bloc:** `ventrysjob:ouvrier_four`
**Fichier:** `OuvrierFourBlockEntity.java` (configuration manuelle)

**Transformations:**
- `minecraft:oak_planks` → `minecraft:charcoal` (quantité 1:1)
- `minecraft:charcoal` → `minecraft:charcoal` (récupération)
- `minecraft:coal` → `minecraft:coal` (récupération)

**Durée:** 5 secondes (100 ticks)
**Combustible:** Nécessite un briquet (flint_and_steel) pour allumer

---

### 9. Four de Forgeron (Métier: FORGERON)
**Bloc:** `ventrysjob:forgeron_four`
**Fichier:** `ForgeronFourBlockEntity.java` (configuration manuelle)

**Transformations:**
- `minecraft:iron_ore` → `minecraft:iron_ingot` x1

**Durée:** 20 secondes
**Combustible:** `minecraft:charcoal` (durée: 80 secondes)
**Prérequis:** Nécessite un briquet (flint_and_steel) pour allumer

---

### 10. Meule (Métier: PAYSAN)
**Bloc:** `ventrysjob:meule`
**Fichier:** `meule_recipes.json`

**Transformations:**
- Aucune recette configurée actuellement (flour retiré)

**Durée par défaut:** 60 secondes (60000 ms)

---

## 🐄 SYSTÈMES DE PRODUCTION ANIMALE (Métier: PAYSAN)

### 11. Production de Lait (Vaches)
**Entité:** `ventrysjob:custom_cow` (femelles uniquement)
**Fichier:** `mobs_config.json` + `CustomCow.java`

**Production:**
- **Item:** `minecraft:milk_bucket` x1
- **Intervalle:** 60 secondes (60000 ms)
- **Conditions:** Nutrition ≥ 30% ET Hydratation ≥ 30%
- **Méthode:** Clic droit avec un seau (`minecraft:bucket`)

---

### 12. Production d'Œufs (Poules)
**Entité:** `ventrysjob:custom_chicken` (femelles uniquement)
**Bloc:** `ventrysjob:chicken_nest`
**Fichier:** `CustomChicken.java` + `ChickenNestBlockEntity.java`

**Production:**
- **Item:** `minecraft:egg` x1
- **Intervalle:** 2-5 minutes (aléatoire par poule)
- **Conditions:** Nutrition ≥ 30% ET Hydratation ≥ 30%
- **Stockage:** Maximum 4 œufs par nid
- **Méthode:** Les poules pondent automatiquement dans les nids à proximité (rayon 20 blocs)

---

### 13. Loots à la Mort des Animaux
**Fichier:** `mobs_config.json` → `animals` → `drops`

**Loots (50% de chance par drop):**

**Porc (`ventrysjob:custom_pig`):**
- `minecraft:porkchop` x1-3

**Vache (`ventrysjob:custom_cow`):**
- `minecraft:leather` x0-2
- `minecraft:beef` x1-3

**Poule (`ventrysjob:custom_chicken`):**
- `minecraft:feather` x0-2
- `minecraft:chicken` x1

---

## 📋 RÉSUMÉ PAR MÉTIER

### OUVRIER
- ✅ Extraction de bûches (hache) → 2x bûches
- ✅ Sciage de bûches (scie) → 4x planches
- ✅ Extraction de minerais (pioche) → raw_iron/gold/copper
- ✅ Extraction de pierre (burin) → cobblestone/gravel
- ✅ Extraction de calcite (maillet) → calcite
- ✅ Extraction de sable (pelle) → sand/red_sand
- ✅ Four Ouvrier → planches → charbon

### PAYSAN
- ✅ Récolte de cultures (fourche) → wheat/carrot/potato/beetroot
- ✅ Production de lait (vaches) → milk_bucket
- ✅ Production d'œufs (poules) → egg
- ✅ Loots animaux à la mort → viandes, cuir, plumes

### FORGERON
- ✅ Four de Forgeron → iron_ore → iron_ingot

---

## 📊 STATISTIQUES

- **Total systèmes d'extraction:** 6 (bûches, sciage, minerais, pierre, calcite, sable)
- **Total systèmes de transformation:** 3 (Four Ouvrier, Four Forgeron, Meule)
- **Total systèmes de production animale:** 2 (lait, œufs)
- **Total types de loots:** ~30+ items différents

---

**Note:** Les crafts purs (via les tables de métier) ne sont PAS inclus dans ce résumé, uniquement les systèmes de production/transformation automatiques.
