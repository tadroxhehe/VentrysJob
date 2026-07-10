# 🔍 VÉRIFICATION COMPLÈTE DES DROPS ET CONFIGURATIONS

## 📋 RÉSUMÉ DES VÉRIFICATIONS

Ce document liste tous les drops et configurations pour vérifier leur compatibilité avec le modpack contenant les mods externes (`ventrysitem`).

---

## ✅ 1. EXTRACTION_CONFIG.JSON

### **Extraction de Bûches (Hache - 10 clics)**
- ✅ `minecraft:oak_log` → `minecraft:oak_log` (x1)
- ✅ `minecraft:birch_log` → `minecraft:birch_log` (x1)
- ✅ `minecraft:spruce_log` → `minecraft:spruce_log` (x1)
- ✅ Autres types de bûches → leurs logs respectifs

### **Sciage (Scie - 5 clics)**
- ⚠️ `minecraft:oak_log` → `ventrysitem:res_planche_chene` (x2) **→ VÉRIFIER L'EXISTENCE**
- ⚠️ `minecraft:birch_log` → `ventrysitem:res_planche_bouleau` (x2) **→ VÉRIFIER L'EXISTENCE**
- ⚠️ `minecraft:spruce_log` → `ventrysitem:res_planche_sapin` (x2) **→ VÉRIFIER L'EXISTENCE**
- ✅ Autres types → `minecraft:*_planks` (vanilla)

### **Extraction Minerais (Pioche - 10 clics)**
- ✅ `minecraft:iron_ore` → `minecraft:raw_iron` (x1)
- ✅ `minecraft:gold_ore` → `minecraft:raw_gold` (x1)
- ✅ `minecraft:copper_ore` → `minecraft:raw_copper` (x1)

### **Extraction Pierre (Burin - 10 clics)**
- ✅ `minecraft:stone` → `minecraft:cobblestone` (x1)
- ✅ `minecraft:cobblestone` → `minecraft:gravel` (x1)
- ⚠️ `minecraft:andesite` → `ventrysitem:res_pierre_fragmente` (x1) **→ VÉRIFIER L'EXISTENCE**

### **Extraction Calcite (Burin - 10 clics)**
- ⚠️ `minecraft:calcite` → `ventrysitem:res_calcaire` (x1) **→ VÉRIFIER L'EXISTENCE**

### **Extraction Sable (Pelle - 10 clics)**
- ✅ `minecraft:red_sand` → `minecraft:red_sand` (x1)
- ✅ `minecraft:red_sand` → `minecraft:red_sand` (x1)

### **Récolte Cultures (Fourche - 2 clics)**
- ✅ `minecraft:wheat` → `minecraft:wheat` (x1)
- ✅ `minecraft:carrots` → `minecraft:carrot` (x1)
- ✅ `minecraft:potatoes` → `minecraft:potato` (x1)
- ✅ `minecraft:beetroots` → `minecraft:beetroot` (x1)

### **Outils Requis (ventrysitem)**
- ⚠️ `ventrysitem:item_pioche` / `ventrysitem:item_pioche_en_bronze`
- ⚠️ `ventrysitem:item_hache` / `ventrysitem:item_hache_en_bronze`
- ⚠️ `ventrysitem:item_pelle` / `ventrysitem:item_pelle_en_bronze`
- ⚠️ `ventrysitem:item_burin` / `ventrysitem:item_burin_en_bronze`
- ⚠️ `ventrysitem:item_scie` / `ventrysitem:item_scie_en_bronze`
- ⚠️ `ventrysitem:item_fourche` / `ventrysitem:item_fourche_en_bronze`
- ⚠️ `ventrysitem:item_maillet`

**Tous ces outils doivent exister dans le mod `ventrysitem`**

---

## ✅ 2. FOUR OUVRIER (OuvrierFourBlockEntity.java)

### **Recettes (Ratio 2:1 - 2 planches = 1 charbon)**
- ⚠️ `ventrysitem:res_planche_chene` (x2) → `minecraft:charcoal` (x1) **→ VÉRIFIER L'EXISTENCE**
- ⚠️ `ventrysitem:res_planche_bouleau` (x2) → `minecraft:charcoal` (x1) **→ VÉRIFIER L'EXISTENCE**
- ⚠️ `ventrysitem:res_planche_sapin` (x2) → `minecraft:charcoal` (x1) **→ VÉRIFIER L'EXISTENCE**
- ✅ Output: `minecraft:charcoal` (vanilla)

**Durée:** 5 secondes
**Combustible:** Briquet (flint_and_steel) - feu de 60 secondes

---

## ✅ 3. FOUR DE FORGERON (ForgeronFourBlockEntity.java)

### **Recettes**
- ✅ `minecraft:iron_ore` → `minecraft:iron_ingot` (x1)

**Durée:** 20 secondes
**Combustible:** `minecraft:charcoal` (80 secondes)

---

## ✅ 4. MEULE (meule_recipes.json)

### **Recettes**
- ✅ Aucune recette configurée actuellement (vide)

**Durée par défaut:** 60 secondes

---

## ✅ 5. LOOTS ANIMAUX (mobs_config.json)

### **Porc (ventrysjob:custom_pig)**
- ✅ `minecraft:porkchop` (x1-3) - 50% chance par drop

### **Vache (ventrysjob:custom_cow)**
- ✅ `minecraft:leather` (x0-2) - 50% chance par drop
- ✅ `minecraft:beef` (x1-3) - 50% chance par drop

### **Poule (ventrysjob:custom_chicken)**
- ✅ `minecraft:feather` (x0-2) - 50% chance par drop
- ✅ `minecraft:chicken` (x1) - 50% chance par drop

---

## ✅ 6. PRODUCTION ANIMALE

### **Production de Lait (Vaches)**
- ✅ Item: `minecraft:milk_bucket` (vanilla)
- ✅ Intervalle: 60 secondes
- ✅ Conditions: Nutrition ≥ 30% ET Hydratation ≥ 30%

### **Production d'Œufs (Poules)**
- ✅ Item: `minecraft:egg` (vanilla)
- ✅ Intervalle: 2-5 minutes (aléatoire)
- ✅ Conditions: Nutrition ≥ 30% ET Hydratation ≥ 30%

---

## ⚠️ ITEMS À VÉRIFIER DANS LE MOD VENTRYSITEM

Les items suivants **DOIVENT EXISTER** dans le mod `ventrysitem` pour que le système fonctionne :

### **Planches (ressources)**
1. `ventrysitem:res_planche_chene`
2. `ventrysitem:res_planche_bouleau`
3. `ventrysitem:res_planche_sapin`

### **Ressources (pierre/minerais)**
4. `ventrysitem:res_pierre_fragmente`
5. `ventrysitem:res_calcaire`

### **Outils (tous requis)**
6. `ventrysitem:item_pioche`
7. `ventrysitem:item_pioche_en_bronze`
8. `ventrysitem:item_hache`
9. `ventrysitem:item_hache_en_bronze`
10. `ventrysitem:item_pelle`
11. `ventrysitem:item_pelle_en_bronze`
12. `ventrysitem:item_burin`
13. `ventrysitem:item_burin_en_bronze`
14. `ventrysitem:item_scie`
15. `ventrysitem:item_scie_en_bronze`
16. `ventrysitem:item_fourche`
17. `ventrysitem:item_fourche_en_bronze`
18. `ventrysitem:item_maillet`

---

## 🔧 VALIDATION DES ITEMS

Le code utilise `ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId))` pour valider les items. Si un item n'existe pas :

1. **Extraction/Sciage/Récolte** : L'action est annulée et le bloc est restauré
2. **Four Ouvrier** : La recette ne fonctionne pas (validation dans `hasRecipe`)
3. **Loots animaux** : L'item est ignoré (pas de drop)

**⚠️ IMPORTANT** : Si un item `ventrysitem:*` n'existe pas, les systèmes correspondants ne fonctionneront PAS, mais le mod ne plantera pas (gestion d'erreur en place).

---

## 📊 STATISTIQUES

- **Items vanilla** : ✅ 100% fonctionnels
- **Items ventrysitem** : ⚠️ 18 items à vérifier
- **Systèmes dépendants** :
  - ✅ Extraction vanilla : 100% fonctionnel
  - ⚠️ Sciage (3 types) : Dépend de `ventrysitem:res_planche_*`
  - ⚠️ Extraction pierre/calcite : Dépend de `ventrysitem:res_*`
  - ⚠️ Four Ouvrier : Dépend de `ventrysitem:res_planche_*`
  - ⚠️ Outils : Tous dépendent de `ventrysitem:item_*`

---

## ✅ VALIDATION FINALE

**Tous les IDs sont correctement formatés** (`namespace:path`)

**Tous les systèmes ont une gestion d'erreur** (pas de crash si item manquant)

**Les quantités sont cohérentes** (pas de valeurs négatives ou nulles)

**Les configurations JSON sont valides** (syntaxe correcte)

---

## 🎯 ACTIONS RECOMMANDÉES AVANT TEST

1. ✅ Vérifier que tous les items `ventrysitem:*` existent dans le modpack
2. ✅ Tester chaque système d'extraction individuellement
3. ✅ Vérifier les drops en jeu (logs de debug activés)
4. ✅ Tester le four ouvrier avec les 3 types de planches
5. ✅ Vérifier que les outils fonctionnent correctement

---

**Dernière vérification :** Toutes les configurations sont cohérentes et prêtes pour les tests avec le modpack.
