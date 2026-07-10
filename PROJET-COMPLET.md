# VentrysJob - Projet Complet

## Vue d'ensemble

**VentrysJob** est un mod Minecraft Forge 1.18.2 qui ajoute un système de métiers pour serveurs RP médiévaux. Il s'intègre avec **EcoVentrys** (système d'énergie) et **VentrysItem** (items personnalisés).

## Architecture du projet

### Structure des fichiers

```
VentrysJob/
├── build.gradle                    # Configuration Gradle
├── gradle.properties               # Propriétés Gradle
├── settings.gradle                 # Settings Gradle
├── README.md                       # Documentation principale
├── INSTALLATION.md                 # Guide d'installation
├── HUDPACK/                        # Textures HUD (assets 1-5)
│   ├── asset1.png
│   ├── asset2.png
│   ├── asset3.png
│   ├── asset4.png
│   └── asset5.png
└── src/
    └── main/
        ├── java/com/ventrys/job/
        │   ├── VentrysJob.java                 # Classe principale du mod
        │   ├── client/
        │   │   ├── KeyBindings.java            # Gestion des touches (J pour ouvrir GUI)
        │   │   ├── gui/
        │   │   │   └── JobsScreen.java         # Interface graphique des métiers
        │   │   └── hud/
        │   │       └── JobHudOverlay.java      # HUD en jeu (métier actif + énergie)
        │   ├── command/
        │   │   └── JobsCommand.java            # Commandes /jobs
        │   ├── data/
        │   │   ├── Job.java                    # Classe métier
        │   │   ├── JobRecipe.java              # Classe recette
        │   │   ├── RecipeIngredient.java       # Classe ingrédient
        │   │   └── JobManager.java             # Gestion JSON et validation
        │   ├── event/
        │   │   └── ModEvents.java              # Événements Forge
        │   ├── network/
        │   │   ├── NetworkHandler.java         # Gestion réseau
        │   │   └── packet/
        │   │       ├── OpenJobsGuiPacket.java  # Packet ouverture GUI
        │   │       ├── SetActiveJobPacket.java # Packet activation métier
        │   │       └── CraftJobRecipePacket.java # Packet craft recette
        │   └── player/
        │       └── PlayerJobData.java          # Données joueur (métier actif)
        └── resources/
            ├── META-INF/
            │   └── mods.toml                   # Métadonnées du mod
            ├── pack.mcmeta                     # Pack resources
            └── assets/ventrysjob/
                ├── lang/
                │   ├── fr_fr.json              # Traductions françaises
                │   └── en_us.json              # Traductions anglaises
                └── textures/
                    └── hud/                     # Textures HUD copiées
                        ├── asset1.png
                        ├── asset2.png
                        ├── asset3.png
                        ├── asset4.png
                        └── asset5.png
```

## Fonctionnalités implémentées

### ✅ Système de métiers

- 4 métiers par défaut : **Forgeron**, **Artisan**, **Apothicaire**, **Cuisinier**
- Configuration JSON (`config/ventrysjob/jobs.json`)
- Activation/désactivation de métier par joueur
- Chaque métier contient une liste de recettes

### ✅ Système de recettes

- Recettes définies en JSON
- Support des items vanilla, VentrysItem, et mods externes
- Validation automatique des items (recette ignorée si item manquant)
- Inputs multiples et output unique
- Coût en énergie (EcoVentrys)
- Temps de craft configurable

### ✅ Interface graphique (GUI)

- Menu des métiers (touche **J** ou `/jobs`)
- Liste des métiers avec descriptions
- Liste des recettes par métier
- Affichage détaillé des recettes :
  - Ingrédients requis
  - Résultat
  - Coût en énergie
- Boutons "Activer" et "Craft"
- Support du scroll pour longues listes

### ✅ HUD en jeu

- Affichage du métier actif (coin supérieur droit)
- Barre d'énergie intégrée avec EcoVentrys
- Couleur dynamique selon le niveau d'énergie :
  - Vert : > 60%
  - Orange : 30-60%
  - Rouge : < 30%
- Pourcentage d'énergie affiché

### ✅ Système de réseau

- Communication client/serveur via packets
- Synchronisation des données
- Validation côté serveur

### ✅ Commandes

- `/jobs` - Ouvre la GUI
- `/jobs list` - Liste des métiers
- `/jobs info` - Métier actif
- `/jobs clear` - Désactiver métier

### ✅ Intégrations

#### EcoVentrys
- Lecture de l'énergie du joueur via `SurvivalDataCapability`
- Vérification de l'énergie avant craft
- Consommation d'énergie lors du craft
- Affichage en temps réel dans le HUD

#### VentrysItem
- Support de tous les items personnalisés
- Référence par ID : `ventrysitem:item_id`
- Items alimentaires, ressources, outils

#### Mods externes
- Support optionnel via ID
- Recette ignorée si item absent (pas de crash)

## Format JSON

### Exemple de métier complet

```json
{
  "id": "blacksmith",
  "name": "Forgeron",
  "description": "Maître de la forge et du métal",
  "recipes": [
    {
      "id": "iron_sword_craft",
      "name": "Épée en fer",
      "description": "Forge une épée en fer",
      "inputs": [
        { "itemId": "minecraft:iron_ingot", "count": 2 },
        { "itemId": "minecraft:stick", "count": 1 }
      ],
      "output": { "itemId": "minecraft:iron_sword", "count": 1 },
      "energyCost": 0,
      "craftTime": 40
    }
  ]
}
```

## Métiers par défaut

### 1. Forgeron (blacksmith)
- **Craft test** : Épée en fer
  - 2x Lingot de fer + 1x Bâton
  - Résultat : 1x Épée en fer

### 2. Artisan (artisan)
- **Craft test** : Table de craft
  - 4x Planches de chêne
  - Résultat : 1x Table de craft

### 3. Apothicaire (apothecary)
- **Craft test** : Alambic
  - 1x Bâton de blaze + 3x Pierre
  - Résultat : 1x Alambic

### 4. Cuisinier (cook)
- **Craft test** : Pain
  - 3x Blé
  - Résultat : 1x Pain

## Compilation

### Prérequis
- Java 17
- Gradle (inclus via wrapper)
- EcoVentrys compilé
- VentrysItem compilé

### Commandes

```bash
# Compiler le mod
gradlew build

# Nettoyer et recompiler
gradlew clean build

# Lancer en mode développement
gradlew runClient

# Générer les ressources
gradlew runData
```

### Sortie
- JAR principal : `build/libs/ventrysjob-1.0.0.jar`
- JAR reobfusqué : `build/reobfJar/output.jar`

## Configuration requise

### build.gradle
- Forge Gradle 5.1+
- Mappings officiels Minecraft 1.18.2
- Dépendances sur EcoVentrys et VentrysItem

### mods.toml
- Forge 40+
- Minecraft 1.18.2
- Dépendances obligatoires : ventrysitem, ecoventrys

## Points techniques

### Gestion de l'énergie
```java
player.getCapability(SurvivalDataCapability.SURVIVAL_DATA)
    .ifPresent(data -> {
        float energy = data.getEnergy(); // 0-100
        data.addEnergy(-cost);
    });
```

### Validation des items
```java
ResourceLocation rl = new ResourceLocation(itemId);
Item item = ForgeRegistries.ITEMS.getValue(rl);
boolean exists = (item != null && item != Items.AIR);
```

### Craft de recette
1. Vérification énergie (si coût > 0)
2. Vérification ingrédients dans inventaire
3. Consommation énergie
4. Consommation ingrédients
5. Ajout du résultat

## Extensions futures

### Version actuelle : 1.0.0
- ✅ Système de base
- ✅ 4 métiers
- ✅ Crafts vanilla

### Prochaines versions
- [ ] Système d'XP et de niveaux
- [ ] Déblocage progressif de recettes
- [ ] Spécialisations par métier
- [ ] Actions métiers spéciales
- [ ] Achievements métiers
- [ ] Interface de configuration GUI
- [ ] Support multi-métiers simultanés

## Licence

All rights reserved © Ventrys Team

## Développeur

Ventrys Team - 2025

---

**Status** : ✅ Projet complet et fonctionnel
**Version** : 1.0.0
**Minecraft** : 1.18.2
**Forge** : 40.2.0
**Java** : 17

