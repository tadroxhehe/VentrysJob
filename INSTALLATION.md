# Guide d'installation VentrysJob

## Prérequis

### Dépendances obligatoires
1. **Minecraft** 1.18.2
2. **Forge** 1.18.2-40.2.0 ou supérieur
3. **Java** 17
4. **EcoVentrys** (mod de survie et énergie)
5. **VentrysItem** (mod d'items personnalisés)

## Installation pour les joueurs

### Étape 1 : Installation de Forge

1. Téléchargez Forge 1.18.2 depuis [https://files.minecraftforge.net](https://files.minecraftforge.net)
2. Exécutez l'installateur Forge
3. Sélectionnez "Install client" et choisissez votre dossier Minecraft
4. Cliquez sur "OK" pour installer

### Étape 2 : Installation des mods

1. Ouvrez le dossier `.minecraft/mods` :
   - Windows : `%APPDATA%\.minecraft\mods`
   - macOS : `~/Library/Application Support/minecraft/mods`
   - Linux : `~/.minecraft/mods`

2. Copiez les fichiers JAR suivants dans le dossier `mods` :
   - `ecoventrys-1.0.0.jar`
   - `ventrysitem-1.0.0.jar`
   - `ventrysjob-1.0.0.jar`

3. Lancez Minecraft avec le profil Forge 1.18.2

### Étape 3 : Vérification

1. Dans le menu principal, cliquez sur "Mods"
2. Vérifiez que les 3 mods apparaissent :
   - EcoVentrys
   - VentrysItem
   - VentrysJob

## Installation pour serveur

### Étape 1 : Préparation du serveur

1. Téléchargez Forge Server 1.18.2-40.2.0
2. Installez Forge sur votre serveur :
   ```bash
   java -jar forge-1.18.2-40.2.0-installer.jar --installServer
   ```

### Étape 2 : Installation des mods

1. Copiez les 3 fichiers JAR dans le dossier `mods` du serveur :
   - `ecoventrys-1.0.0.jar`
   - `ventrysitem-1.0.0.jar`
   - `ventrysjob-1.0.0.jar`

2. Lancez le serveur :
   ```bash
   java -Xmx4G -Xms2G -jar forge-1.18.2-40.2.0.jar nogui
   ```

### Étape 3 : Configuration

1. Le fichier de configuration sera créé au premier lancement :
   - `config/ventrysjob/jobs.json`

2. Modifiez ce fichier pour personnaliser les métiers et recettes

## Utilisation

### En jeu

1. **Ouvrir le menu des métiers** :
   - Appuyez sur **J**
   - Ou tapez `/jobs`

2. **Choisir un métier** :
   - Cliquez sur un métier dans la liste
   - Cliquez sur "Activer"

3. **Crafter des items** :
   - Sélectionnez une recette
   - Cliquez sur "Craft"
   - Vérifiez que vous avez les ingrédients et l'énergie nécessaires

### Commandes

- `/jobs` - Ouvre le menu des métiers
- `/jobs list` - Liste tous les métiers
- `/jobs info` - Affiche votre métier actif
- `/jobs clear` - Désactive votre métier

## Personnalisation

### Modifier les métiers

Éditez le fichier `config/ventrysjob/jobs.json` :

```json
{
  "id": "mon_metier",
  "name": "Mon Métier",
  "description": "Description de mon métier",
  "recipes": [
    {
      "id": "ma_recette",
      "name": "Ma Recette",
      "description": "Description",
      "inputs": [
        { "itemId": "minecraft:diamond", "count": 1 }
      ],
      "output": { "itemId": "minecraft:diamond_sword", "count": 1 },
      "energyCost": 10,
      "craftTime": 20
    }
  ]
}
```

### Ajouter des recettes avec VentrysItem

```json
"inputs": [
  { "itemId": "ventrysitem:item_pomme", "count": 3 },
  { "itemId": "ventrysitem:res_bronze_lingot", "count": 2 }
],
"output": { "itemId": "ventrysitem:item_pain", "count": 1 }
```

## Dépannage

### Le mod ne se charge pas

1. Vérifiez que vous utilisez Java 17
2. Vérifiez que Forge 1.18.2-40.2.0 ou supérieur est installé
3. Vérifiez que EcoVentrys et VentrysItem sont présents

### Les recettes ne fonctionnent pas

1. Vérifiez que tous les items existent dans le jeu
2. Vérifiez que le fichier `jobs.json` est correctement formaté
3. Consultez les logs dans `.minecraft/logs/latest.log`

### L'énergie ne se consomme pas

1. Vérifiez que EcoVentrys est correctement installé
2. Vérifiez que le `energyCost` est supérieur à 0 dans la recette

## Support

Pour toute question ou problème :
- Consultez le README.md
- Vérifiez les logs du jeu
- Contactez l'équipe Ventrys

## Licence

All rights reserved © Ventrys Team

