# VentrysJob - Démarrage Rapide

## 🚀 Installation (5 minutes)

### 1. Compilation
```bash
cd VentrysJob
gradlew build
```

### 2. Installation
Copier les 3 JAR dans `.minecraft/mods/` :
- `ecoventrys-1.0.0.jar`
- `ventrysitem-1.0.0.jar`
- `ventrysjob-1.0.0.jar`

### 3. Lancement
Lancer Minecraft avec Forge 1.18.2

---

## 🎮 Utilisation (2 minutes)

### En jeu
- Appuyer sur **J** → Ouvrir le menu
- Cliquer sur un métier → **Activer**
- Sélectionner une recette → **Craft**

### Commandes
```
/jobs        → Ouvrir le menu
/jobs list   → Liste des métiers
/jobs info   → Métier actif
/jobs clear  → Désactiver
```

---

## ⚙️ Configuration (3 minutes)

### Fichier
`config/ventrysjob/jobs.json`

### Exemple de métier
```json
{
  "id": "forgeron",
  "name": "Forgeron",
  "description": "Maître de la forge",
  "recipes": [
    {
      "id": "epee_fer",
      "name": "Épée en fer",
      "inputs": [
        { "itemId": "minecraft:iron_ingot", "count": 2 },
        { "itemId": "minecraft:stick", "count": 1 }
      ],
      "output": { "itemId": "minecraft:iron_sword", "count": 1 },
      "energyCost": 10,
      "craftTime": 20
    }
  ]
}
```

---

## 📦 IDs des items

### Vanilla
```json
"minecraft:iron_ingot"
"minecraft:diamond"
```

### VentrysItem - Alimentaire
```json
"ventrysitem:item_pomme"
"ventrysitem:item_pain"
"ventrysitem:item_bol_boeuf_bourguignon"
```

### VentrysItem - Ressources
```json
"ventrysitem:res_bronze_lingot"
"ventrysitem:res_acier_lingot"
"ventrysitem:res_farine"
```

---

## 🔧 Dépannage

| Problème | Solution |
|----------|----------|
| Mod ne charge pas | Vérifier Java 17 + Forge 1.18.2 |
| Recette invalide | Vérifier les IDs d'items |
| Pas d'énergie | Vérifier EcoVentrys installé |
| HUD invisible | Activer un métier avec `/jobs` |

---

## 📚 Documentation complète

- `README.md` - Vue d'ensemble
- `GUIDE-COMPLET-FR.md` - Guide utilisateur
- `INTEGRATION-MODS.md` - Intégration EcoVentrys/VentrysItem
- `DEPLOIEMENT.md` - Guide serveur
- `PROJET-COMPLET.md` - Documentation technique

---

## ✨ Fonctionnalités

✅ 4 métiers : Forgeron, Artisan, Apothicaire, Cuisinier  
✅ Recettes JSON personnalisables  
✅ Système d'énergie (EcoVentrys)  
✅ Items personnalisés (VentrysItem)  
✅ GUI intuitive + HUD  
✅ Support multi-mods  

---

**Version** : 1.0.0 | **MC** : 1.18.2 | **Forge** : 40.2.0+ | **Java** : 17

© Ventrys Team - 2025

