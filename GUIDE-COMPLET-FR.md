# Guide Complet VentrysJob - Version Française

## 📋 Table des matières

1. [Vue d'ensemble](#vue-densemble)
2. [Installation](#installation)
3. [Utilisation en jeu](#utilisation-en-jeu)
4. [Configuration](#configuration)
5. [Création de métiers personnalisés](#création-de-métiers-personnalisés)
6. [Intégration avec les autres mods](#intégration-avec-les-autres-mods)
7. [Exemples de recettes](#exemples-de-recettes)
8. [Dépannage](#dépannage)

---

## 🎯 Vue d'ensemble

**VentrysJob** est un système de métiers pour Minecraft 1.18.2 (Forge) conçu pour les serveurs RP médiévaux.

### Caractéristiques principales

✅ **4 métiers par défaut** : Forgeron, Artisan, Apothicaire, Cuisinier  
✅ **Configuration JSON** : Métiers et recettes modifiables sans recompiler  
✅ **Système d'énergie** : Intégration avec EcoVentrys  
✅ **Items personnalisés** : Support complet de VentrysItem  
✅ **Multi-mods** : Utilisation d'items d'autres mods  
✅ **Interface intuitive** : GUI avec liste de recettes et crafts  
✅ **HUD en jeu** : Affichage du métier actif et de l'énergie  

### Dépendances obligatoires

- ☑️ Minecraft 1.18.2
- ☑️ Forge 40.2.0+
- ☑️ Java 17
- ☑️ **EcoVentrys** (système d'énergie)
- ☑️ **VentrysItem** (items RP)

---

## 💿 Installation

### Pour les joueurs

1. **Installer Forge 1.18.2**
   - Télécharger : https://files.minecraftforge.net
   - Exécuter l'installateur
   - Sélectionner "Install client"

2. **Copier les mods**
   - Ouvrir le dossier `.minecraft/mods`
   - Copier les 3 fichiers JAR :
     - `ecoventrys-1.0.0.jar`
     - `ventrysitem-1.0.0.jar`
     - `ventrysjob-1.0.0.jar`

3. **Lancer le jeu**
   - Sélectionner le profil Forge 1.18.2
   - Vérifier dans le menu "Mods" que les 3 mods sont présents

### Pour les serveurs

1. **Installer Forge Server**
   ```bash
   java -jar forge-1.18.2-40.2.0-installer.jar --installServer
   ```

2. **Copier les mods**
   - Placer les 3 JAR dans le dossier `mods/`

3. **Lancer le serveur**
   ```bash
   java -Xmx4G -Xms2G -jar forge-1.18.2-40.2.0.jar nogui
   ```

4. **Configurer**
   - Modifier `config/ventrysjob/jobs.json` selon vos besoins

---

## 🎮 Utilisation en jeu

### Ouvrir le menu des métiers

**3 méthodes** :
1. Appuyer sur **J** (configurable)
2. Taper `/jobs`
3. Via un NPC (si configuré)

### Interface du menu

```
┌────────────────────────────────────────┐
│  MÉTIERS            │ Détails du métier│
│                     │                  │
│ ▶ Forgeron          │ Forgeron         │
│   Artisan           │ Maître de la     │
│   Apothicaire       │ forge et métal   │
│   Cuisinier         │                  │
│                     │ Recettes:        │
│                     │ • Épée en fer    │
│                     │ • Armure...      │
│                     │                  │
│        [Activer] [Craft] [Fermer]     │
└────────────────────────────────────────┘
```

### Activer un métier

1. Cliquer sur un métier dans la liste de gauche
2. Lire la description et les recettes
3. Cliquer sur **"Activer"**
4. Le métier s'affiche dans le HUD (coin supérieur droit)

### Crafter un item

1. Métier activé → Sélectionner une recette
2. Vérifier les ingrédients requis
3. Vérifier l'énergie nécessaire
4. Cliquer sur **"Craft"**
5. Le craft consomme les ingrédients et l'énergie

### HUD en jeu

```
┌─────────────────────┐
│ Forgeron            │
│ ▓▓▓▓▓▓▓░░░ Énergie: │
│ 75%                 │
└─────────────────────┘
```

**Couleurs de la barre d'énergie** :
- 🟢 Vert : > 60% d'énergie
- 🟠 Orange : 30-60% d'énergie
- 🔴 Rouge : < 30% d'énergie

### Commandes

| Commande | Description |
|----------|-------------|
| `/jobs` | Ouvre la GUI des métiers |
| `/jobs list` | Liste tous les métiers disponibles |
| `/jobs info` | Affiche votre métier actif |
| `/jobs clear` | Désactive votre métier actif |

---

## ⚙️ Configuration

### Fichier jobs.json

**Emplacement** : `config/ventrysjob/jobs.json`

Le fichier est créé automatiquement au premier lancement avec 4 métiers par défaut.

### Structure d'un métier

```json
{
  "id": "identifiant_unique",
  "name": "Nom affiché",
  "description": "Description courte du métier",
  "recipes": [
    // Liste des recettes...
  ]
}
```

### Structure d'une recette

```json
{
  "id": "identifiant_recette",
  "name": "Nom de la recette",
  "description": "Description de la recette",
  "inputs": [
    { "itemId": "modid:item_id", "count": quantité }
  ],
  "output": { "itemId": "modid:item_id", "count": quantité },
  "energyCost": 0,
  "craftTime": 20
}
```

### Paramètres expliqués

| Paramètre | Type | Description |
|-----------|------|-------------|
| `id` | String | Identifiant unique (pas d'espaces) |
| `name` | String | Nom affiché en jeu |
| `description` | String | Description du métier/recette |
| `inputs` | Array | Liste des ingrédients requis |
| `output` | Object | Item résultat du craft |
| `energyCost` | Integer | Énergie consommée (0-100) |
| `craftTime` | Integer | Temps en ticks (20 = 1 sec) |
| `itemId` | String | ID Minecraft de l'item (format `modid:item`) |
| `count` | Integer | Quantité d'items |

---

## 🛠️ Création de métiers personnalisés

### Exemple : Métier de Mineur

```json
{
  "id": "miner",
  "name": "Mineur",
  "description": "Expert en extraction de minerais",
  "recipes": [
    {
      "id": "iron_pickaxe",
      "name": "Pioche en fer",
      "description": "Fabrique une pioche en fer robuste",
      "inputs": [
        { "itemId": "minecraft:iron_ingot", "count": 3 },
        { "itemId": "minecraft:stick", "count": 2 }
      ],
      "output": { "itemId": "minecraft:iron_pickaxe", "count": 1 },
      "energyCost": 15,
      "craftTime": 40
    },
    {
      "id": "refined_diamond",
      "name": "Diamant raffiné",
      "description": "Raffine un diamant brut",
      "inputs": [
        { "itemId": "ventrysitem:res_diamant_brut", "count": 1 },
        { "itemId": "ventrysitem:res_sel", "count": 2 }
      ],
      "output": { "itemId": "ventrysitem:res_diamant_taille", "count": 1 },
      "energyCost": 25,
      "craftTime": 60
    }
  ]
}
```

### Conseils de création

1. **IDs uniques** : Chaque métier et recette doit avoir un ID unique
2. **Équilibrage** :
   - Crafts simples : `energyCost: 0-10`
   - Crafts moyens : `energyCost: 10-30`
   - Crafts avancés : `energyCost: 30-60`
   - Crafts légendaires : `energyCost: 60-100`
3. **Temps de craft** :
   - Instant : `craftTime: 1`
   - Rapide : `craftTime: 20` (1 sec)
   - Normal : `craftTime: 40` (2 sec)
   - Lent : `craftTime: 100` (5 sec)

---

## 🔗 Intégration avec les autres mods

### EcoVentrys - Système d'énergie

**L'énergie est consommée lors des crafts** :

```json
{
  "energyCost": 25  // Consomme 25 points d'énergie
}
```

**Affichage dans le HUD** :
- Le HUD affiche en temps réel l'énergie du joueur
- La couleur change selon le niveau
- Le craft est bloqué si l'énergie est insuffisante

### VentrysItem - Items personnalisés

**Utilisation dans les recettes** :

```json
{
  "inputs": [
    { "itemId": "ventrysitem:res_bronze_lingot", "count": 2 },
    { "itemId": "ventrysitem:item_pomme", "count": 1 }
  ],
  "output": { "itemId": "ventrysitem:item_pain", "count": 3 }
}
```

**Liste des items disponibles** : Voir `INTEGRATION-MODS.md`

### Autres mods (optionnel)

**Support automatique** :

```json
{
  "itemId": "autremod:item_special", "count": 1
}
```

**Comportement** :
- ✅ Mod installé → Recette active
- ❌ Mod absent → Recette ignorée (pas de crash)

---

## 📝 Exemples de recettes

### Forgeron

#### Épée en acier
```json
{
  "id": "steel_sword",
  "name": "Épée en acier",
  "inputs": [
    { "itemId": "ventrysitem:res_acier_lingot", "count": 2 },
    { "itemId": "minecraft:stick", "count": 1 }
  ],
  "output": { "itemId": "minecraft:iron_sword", "count": 1 },
  "energyCost": 20,
  "craftTime": 40
}
```

#### Armure en bronze
```json
{
  "id": "bronze_chestplate",
  "name": "Plastron en bronze",
  "inputs": [
    { "itemId": "ventrysitem:res_bronze_lingot", "count": 8 },
    { "itemId": "ventrysitem:res_acier_maille", "count": 4 }
  ],
  "output": { "itemId": "minecraft:iron_chestplate", "count": 1 },
  "energyCost": 30,
  "craftTime": 60
}
```

### Cuisinier

#### Pain artisanal
```json
{
  "id": "artisan_bread",
  "name": "Pain artisanal",
  "inputs": [
    { "itemId": "ventrysitem:res_farine", "count": 3 },
    { "itemId": "ventrysitem:res_sel", "count": 1 },
    { "itemId": "minecraft:egg", "count": 1 }
  ],
  "output": { "itemId": "ventrysitem:item_pain", "count": 4 },
  "energyCost": 15,
  "craftTime": 30
}
```

#### Ragoût
```json
{
  "id": "stew",
  "name": "Ragoût de légumes",
  "inputs": [
    { "itemId": "ventrysitem:item_carotte", "count": 2 },
    { "itemId": "ventrysitem:item_pomme_de_terre", "count": 2 },
    { "itemId": "ventrysitem:item_oignon", "count": 1 },
    { "itemId": "ventrysitem:res_bol", "count": 1 }
  ],
  "output": { "itemId": "minecraft:suspicious_stew", "count": 1 },
  "energyCost": 10,
  "craftTime": 40
}
```

### Apothicaire

#### Potion de soin
```json
{
  "id": "healing_potion",
  "name": "Potion de soin",
  "inputs": [
    { "itemId": "minecraft:glass_bottle", "count": 1 },
    { "itemId": "ventrysitem:item_fraise_sauvage", "count": 2 },
    { "itemId": "ventrysitem:item_miel", "count": 1 }
  ],
  "output": { "itemId": "minecraft:potion", "count": 1 },
  "energyCost": 20,
  "craftTime": 50
}
```

### Artisan

#### Coffre renforcé
```json
{
  "id": "reinforced_chest",
  "name": "Coffre renforcé",
  "inputs": [
    { "itemId": "ventrysitem:res_planche_chene", "count": 8 },
    { "itemId": "ventrysitem:res_clou", "count": 16 },
    { "itemId": "ventrysitem:res_acier_cadenas", "count": 1 }
  ],
  "output": { "itemId": "minecraft:chest", "count": 1 },
  "energyCost": 25,
  "craftTime": 60
}
```

---

## 🔧 Dépannage

### Le mod ne se charge pas

**Symptôme** : Le mod n'apparaît pas dans la liste des mods

**Solutions** :
1. ✅ Vérifier que Java 17 est installé : `java -version`
2. ✅ Vérifier que Forge 1.18.2-40.2.0+ est installé
3. ✅ Vérifier que les 3 mods sont dans le dossier `mods/`
4. ✅ Consulter les logs : `.minecraft/logs/latest.log`

### Les recettes ne s'affichent pas

**Symptôme** : Le métier est vide ou n'a pas de recettes

**Solutions** :
1. ✅ Vérifier que `config/ventrysjob/jobs.json` existe
2. ✅ Vérifier la syntaxe JSON (virgules, guillemets)
3. ✅ Vérifier que les items existent (IDs corrects)
4. ✅ Consulter les logs pour voir les recettes ignorées

### L'énergie ne se consomme pas

**Symptôme** : Le craft fonctionne mais l'énergie ne diminue pas

**Solutions** :
1. ✅ Vérifier que EcoVentrys est installé
2. ✅ Vérifier que `energyCost > 0` dans la recette
3. ✅ Vérifier les logs pour erreurs EcoVentrys

### Le HUD ne s'affiche pas

**Symptôme** : Pas de HUD en haut à droite

**Solutions** :
1. ✅ Activer un métier avec `/jobs`
2. ✅ Vérifier que le HUD n'est pas caché (F1)
3. ✅ Redémarrer le jeu

### Erreur lors du craft

**Symptôme** : Message "Recette invalide" ou rien ne se passe

**Solutions** :
1. ✅ Vérifier que vous avez tous les ingrédients
2. ✅ Vérifier que vous avez assez d'énergie
3. ✅ Vérifier que le métier est bien activé
4. ✅ Consulter les logs serveur

### Erreur JSON

**Symptôme** : Erreur au chargement du fichier jobs.json

**Solutions** :
1. ✅ Valider le JSON sur https://jsonlint.com
2. ✅ Vérifier les virgules (pas de virgule après le dernier élément)
3. ✅ Vérifier les guillemets doubles `"` (pas simples `'`)
4. ✅ Supprimer le fichier pour en générer un nouveau

---

## 📚 Fichiers de documentation

- `README.md` - Vue d'ensemble du projet
- `INSTALLATION.md` - Guide d'installation détaillé
- `INTEGRATION-MODS.md` - Intégration EcoVentrys et VentrysItem
- `PROJET-COMPLET.md` - Documentation technique complète
- `GUIDE-COMPLET-FR.md` - Ce fichier

---

## 📄 Licence

All rights reserved © Ventrys Team

---

## ✉️ Support

Pour toute question ou problème :
1. Consulter cette documentation
2. Vérifier les logs du jeu/serveur
3. Contacter l'équipe Ventrys

---

**Version** : 1.0.0  
**Minecraft** : 1.18.2  
**Forge** : 40.2.0+  
**Java** : 17  

© Ventrys Team - 2025

