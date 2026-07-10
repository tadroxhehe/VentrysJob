# VentrysJob

Mod de métiers pour serveur RP médiéval Minecraft 1.18.2 (Forge)

## Fonctionnalités

- **4 métiers** : Forgeron, Artisan, Apothicaire, Cuisinier
- **Système de crafts personnalisé** via JSON
- **Intégration EcoVentrys** : barre d'énergie pour les actions métiers
- **Intégration VentrysItem** : support des items personnalisés
- **Interface GUI** complète avec liste de métiers et recettes
- **HUD en jeu** affichant le métier actif et l'énergie
- **Support multi-mods** : utilisation d'items d'autres mods via leur ID

## Installation

### Dépendances obligatoires
- Minecraft Forge 1.18.2-40.2.0
- Java 17
- **EcoVentrys** (système de survie et énergie)
- **VentrysItem** (items personnalisés)

### Compilation

1. Assurez-vous que EcoVentrys et VentrysItem sont compilés :
   ```bash
   cd ../EcoVentrys
   gradlew build
   cd ../VentrysItem
   gradlew build
   ```

2. Compilez VentrysJob :
   ```bash
   cd VentrysJob
   gradlew build
   ```

3. Le JAR sera généré dans `build/libs/ventrysjob-1.0.0.jar`

## Utilisation

### Commandes

- `/jobs` - Ouvre l'interface des métiers
- `/jobs list` - Liste tous les métiers disponibles
- `/jobs info` - Affiche le métier actif
- `/jobs clear` - Désactive le métier actif

### Raccourci clavier

- **Touche J** : Ouvre le menu des métiers (configurable)

### Configuration

Le fichier `config/ventrysjob/jobs.json` est créé automatiquement au premier lancement avec 4 métiers par défaut.

#### Structure du fichier jobs.json

```json
[
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
]
```

### Ajout de recettes

#### Utilisation d'items vanilla
```json
{
  "itemId": "minecraft:diamond",
  "count": 1
}
```

#### Utilisation d'items VentrysItem
```json
{
  "itemId": "ventrysitem:item_pomme",
  "count": 3
}
```

#### Utilisation d'items d'autres mods
```json
{
  "itemId": "autremod:item_special",
  "count": 2
}
```

**Note** : Si un item d'un mod externe n'est pas présent, la recette sera automatiquement désactivée sans planter le jeu.

### Coût en énergie

Le champ `energyCost` utilise le système d'énergie d'EcoVentrys :
- 0 : Pas de coût en énergie
- 1-100 : Coût en points d'énergie

## Intégration des mods

### EcoVentrys
- Accès à la barre d'énergie du joueur
- Vérification de l'énergie avant craft
- Consommation d'énergie lors des crafts

### VentrysItem
- Accès à tous les items personnalisés
- Support des items alimentaires
- Support des ressources

## Développement

### Structure du projet

```
src/main/java/com/ventrys/job/
├── VentrysJob.java              # Classe principale
├── client/
│   ├── gui/
│   │   └── JobsScreen.java      # Interface GUI des métiers
│   ├── hud/
│   │   └── JobHudOverlay.java   # HUD en jeu
│   └── KeyBindings.java         # Raccourcis clavier
├── command/
│   └── JobsCommand.java         # Commandes du jeu
├── data/
│   ├── Job.java                 # Modèle de métier
│   ├── JobRecipe.java           # Modèle de recette
│   ├── JobManager.java          # Gestion des métiers
│   └── RecipeIngredient.java    # Modèle d'ingrédient
├── network/
│   ├── NetworkHandler.java      # Gestion réseau
│   └── packet/                  # Packets client/serveur
└── player/
    └── PlayerJobData.java       # Données joueur
```

## Version

**Version actuelle** : 1.0.0

## Licence

All rights reserved © Ventrys Team

## Auteur

Ventrys Team

