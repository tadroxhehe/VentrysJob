# Intégration avec EcoVentrys et VentrysItem

Ce document explique comment VentrysJob s'intègre avec les mods **EcoVentrys** et **VentrysItem**.

## EcoVentrys - Système d'énergie

### Description
EcoVentrys est un mod de survie réaliste qui ajoute des stats de survie aux joueurs, notamment :
- Faim (hunger)
- Hydratation (hydration)
- **Énergie (energy)** ← Utilisé par VentrysJob
- Température corporelle
- Taille du joueur

### Intégration dans VentrysJob

#### 1. Accès à l'énergie du joueur

```java
import com.tadrox.ecoventrys.capabilities.SurvivalDataCapability;

// Récupérer l'énergie
player.getCapability(SurvivalDataCapability.SURVIVAL_DATA).ifPresent(data -> {
    float energy = data.getEnergy(); // Valeur entre 0 et 100
    
    // Consommer de l'énergie
    data.addEnergy(-10); // Retire 10 points d'énergie
    
    // Ajouter de l'énergie
    data.addEnergy(5); // Ajoute 5 points d'énergie
});
```

#### 2. Utilisation dans le HUD

Le HUD de VentrysJob affiche en temps réel l'énergie du joueur :

```java
// JobHudOverlay.java
private static float getEnergyPercentage(Minecraft mc) {
    if (mc.player != null) {
        return mc.player.getCapability(SurvivalDataCapability.SURVIVAL_DATA)
            .map(data -> data.getEnergy() / 100.0f)
            .orElse(0.75f);
    }
    return 0.75f;
}
```

#### 3. Vérification avant craft

Avant de permettre un craft, VentrysJob vérifie si le joueur a suffisamment d'énergie :

```java
// CraftJobRecipePacket.java
if (recipe.getEnergyCost() > 0) {
    player.getCapability(SurvivalDataCapability.SURVIVAL_DATA).ifPresent(data -> {
        if (data.getEnergy() < recipe.getEnergyCost()) {
            player.sendMessage(new TextComponent("§cÉnergie insuffisante!"));
            return;
        }
        data.addEnergy(-recipe.getEnergyCost());
    });
}
```

### Configuration des recettes avec énergie

```json
{
  "id": "advanced_craft",
  "name": "Craft avancé",
  "energyCost": 25,  // Requiert 25 points d'énergie
  "inputs": [...],
  "output": {...}
}
```

**Notes** :
- `energyCost: 0` = Pas de consommation d'énergie
- `energyCost: 1-100` = Coût en points d'énergie
- Si le joueur n'a pas assez d'énergie, le craft est bloqué

---

## VentrysItem - Items personnalisés

### Description
VentrysItem est un mod qui ajoute des items RP médiévaux :
- **Items alimentaires** : fruits, légumes, viandes, plats préparés
- **Ressources** : lingots, minerais bruts, planches, etc.
- **Outils** : briquets, gourdes, etc.

### Structure des items

#### Items spéciaux (avec classes Java)
- `item_pomme`, `item_carotte`, `item_tomate`, etc.
- `item_pain`, `item_bol_boeuf_bourguignon`, etc.
- `item_bronze_briquet`, `item_gourde_en_cuir_eau`, etc.

#### Ressources (définis en JSON)
- `res_bronze_lingot`, `res_bronze_pepite`
- `res_acier_lingot`, `res_acier_pepite`
- `res_cuivre_lingot`, `res_fer_brut`
- `res_diamant_taille`, `res_emeraude_gemme`
- `res_ble`, `res_orge`, `res_farine`
- etc. (50+ ressources)

### Intégration dans VentrysJob

#### 1. Utilisation des items dans les recettes

```json
{
  "id": "bread_craft",
  "name": "Pain artisanal",
  "inputs": [
    { "itemId": "ventrysitem:res_farine", "count": 2 },
    { "itemId": "ventrysitem:res_sel", "count": 1 },
    { "itemId": "minecraft:water_bucket", "count": 1 }
  ],
  "output": { "itemId": "ventrysitem:item_pain", "count": 3 }
}
```

#### 2. Validation automatique

VentrysJob vérifie automatiquement si les items existent :

```java
// JobManager.java
private static boolean itemExists(String itemId) {
    try {
        ResourceLocation resourceLocation = new ResourceLocation(itemId);
        Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
        return item != null && item != net.minecraft.world.item.Items.AIR;
    } catch (Exception e) {
        return false;
    }
}
```

**Comportement** :
- ✅ Item existe → Recette active
- ❌ Item manquant → Recette ignorée (pas de crash)

#### 3. Exemples de recettes utilisant VentrysItem

##### Forgeron - Briquet en bronze
```json
{
  "id": "bronze_lighter",
  "name": "Briquet en bronze",
  "inputs": [
    { "itemId": "ventrysitem:res_bronze_lingot", "count": 2 },
    { "itemId": "ventrysitem:res_silex", "count": 1 }
  ],
  "output": { "itemId": "ventrysitem:item_bronze_briquet", "count": 1 },
  "energyCost": 15
}
```

##### Cuisinier - Bol de bœuf bourguignon
```json
{
  "id": "beef_stew",
  "name": "Bœuf Bourguignon",
  "inputs": [
    { "itemId": "minecraft:beef", "count": 2 },
    { "itemId": "ventrysitem:item_carotte", "count": 2 },
    { "itemId": "ventrysitem:item_oignon", "count": 1 },
    { "itemId": "ventrysitem:res_bol", "count": 1 }
  ],
  "output": { "itemId": "ventrysitem:item_bol_boeuf_bourguignon", "count": 1 },
  "energyCost": 20
}
```

##### Apothicaire - Gourde d'eau
```json
{
  "id": "water_flask",
  "name": "Gourde d'eau",
  "inputs": [
    { "itemId": "ventrysitem:item_gourde_en_cuir_vide", "count": 1 },
    { "itemId": "minecraft:water_bucket", "count": 1 }
  ],
  "output": { "itemId": "ventrysitem:item_gourde_en_cuir_eau", "count": 1 },
  "energyCost": 5
}
```

##### Artisan - Planches spéciales
```json
{
  "id": "special_planks",
  "name": "Planches de chêne travaillées",
  "inputs": [
    { "itemId": "minecraft:oak_log", "count": 1 },
    { "itemId": "ventrysitem:res_clou", "count": 4 }
  ],
  "output": { "itemId": "ventrysitem:res_planche_chene", "count": 4 },
  "energyCost": 10
}
```

---

## Support des mods externes

VentrysJob peut également utiliser des items d'autres mods installés sur le serveur.

### Exemple avec un mod fictif "MedievalWeapons"

```json
{
  "id": "legendary_sword",
  "name": "Épée légendaire",
  "inputs": [
    { "itemId": "ventrysitem:res_acier_lingot", "count": 5 },
    { "itemId": "ventrysitem:res_diamant_taille", "count": 2 },
    { "itemId": "medievalweapons:ancient_gem", "count": 1 }
  ],
  "output": { "itemId": "medievalweapons:legendary_sword", "count": 1 },
  "energyCost": 50
}
```

**Comportement** :
- Si `medievalweapons` est installé → Recette active
- Si `medievalweapons` est absent → Recette ignorée

---

## Liste complète des items VentrysItem utilisables

### Items alimentaires (tab "items")
```
ventrysitem:item_pomme
ventrysitem:item_carotte
ventrysitem:item_tomate
ventrysitem:item_oignon
ventrysitem:item_pomme_de_terre
ventrysitem:item_abricot
ventrysitem:item_raisin
ventrysitem:item_betterave
ventrysitem:item_choux
ventrysitem:item_salade
ventrysitem:item_fraise_sauvage
ventrysitem:item_mousserons
ventrysitem:item_morilles

ventrysitem:item_blanc_poulet
ventrysitem:item_cotelette_porc
ventrysitem:item_saumon
ventrysitem:item_dorade
ventrysitem:item_homard
ventrysitem:item_epaule_sanglier
ventrysitem:item_filet_mignon_cerf
ventrysitem:item_gigot_lapin
ventrysitem:item_gigot_agneau

ventrysitem:item_pain
ventrysitem:item_bol_boeuf_bourguignon
ventrysitem:item_chope_bierre
ventrysitem:item_miel
ventrysitem:item_gruyere
ventrysitem:item_oeuf_poule
```

### Ressources (tab "ressources")
```
# Métaux
ventrysitem:res_bronze_lingot
ventrysitem:res_bronze_pepite
ventrysitem:res_acier_lingot
ventrysitem:res_acier_pepite
ventrysitem:res_cuivre_lingot
ventrysitem:res_cuivre_fondu
ventrysitem:res_fer_fondu
ventrysitem:res_or_lingot
ventrysitem:res_etain_fondu

# Minerais bruts
ventrysitem:res_cuivre_brut
ventrysitem:res_etain_brut
ventrysitem:res_fer_brut
ventrysitem:res_or_brut
ventrysitem:res_argent_brut
ventrysitem:res_diamant_brut
ventrysitem:res_emeraude_brute
ventrysitem:res_rubis_brut
ventrysitem:res_saphir_brut

# Pierres et gemmes
ventrysitem:res_diamant_taille
ventrysitem:res_emeraude_gemme
ventrysitem:res_calcaire
ventrysitem:res_silex
ventrysitem:res_pierre_fragmente

# Bois
ventrysitem:res_planche_sapin
ventrysitem:res_planche_bouleau
ventrysitem:res_planche_chene
ventrysitem:res_charbon_bois

# Agriculture
ventrysitem:res_ble
ventrysitem:res_orge
ventrysitem:res_farine
ventrysitem:res_sel
ventrysitem:res_sucre
ventrysitem:res_tournesol

# Argile
ventrysitem:res_argile_orange
ventrysitem:res_argile_bleue
ventrysitem:res_argile_brune

# Outils et composants
ventrysitem:res_acier_cle
ventrysitem:res_acier_cadenas
ventrysitem:res_acier_maille
ventrysitem:res_clou
ventrysitem:res_bol
ventrysitem:res_sac_farine_vide
ventrysitem:res_plume_poule
ventrysitem:res_cuir
ventrysitem:res_charbon
```

---

## Dépendances dans le code

### build.gradle
```gradle
dependencies {
    minecraft 'net.minecraftforge:forge:1.18.2-40.2.0'
    
    // Dépendances obligatoires
    implementation files('../EcoVentrys/build/libs/ecoventrys-1.0.0.jar')
    implementation files('../VentrysItem/build/libs/ventrysitem-1.0.0.jar')
}
```

### mods.toml
```toml
[[dependencies.ventrysjob]]
    modId="ventrysitem"
    mandatory=true
    versionRange="[1.0.0,)"
    ordering="AFTER"
    side="BOTH"

[[dependencies.ventrysjob]]
    modId="ecoventrys"
    mandatory=true
    versionRange="[1.0.0,)"
    ordering="AFTER"
    side="BOTH"
```

### Imports Java
```java
// EcoVentrys
import com.tadrox.ecoventrys.capabilities.SurvivalDataCapability;

// VentrysItem (pas d'import direct, utilisation via ResourceLocation)
// Accès aux items via ForgeRegistries uniquement
```

---

## Résumé

| Mod | Fonction | Utilisation dans VentrysJob |
|-----|----------|----------------------------|
| **EcoVentrys** | Système d'énergie | Coût énergie des crafts, affichage HUD |
| **VentrysItem** | Items RP médiévaux | Ingrédients et résultats de recettes |

**VentrysJob** est conçu pour être **extensible** :
- ✅ Support des items vanilla
- ✅ Support des items VentrysItem
- ✅ Support d'items de mods tiers
- ✅ Pas de crash si un item manque

---

© Ventrys Team - 2025

