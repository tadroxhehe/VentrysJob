# Configuration du Système de Sciage

## 🪚 **Système de Sciage des Bûches**

Le métier **Ouvrier** peut maintenant scier les bûches extraites pour créer des planches.

### **📋 Prérequis :**
1. **Métier requis** : Ouvrier (`/job set ouvrier`)
2. **Outil requis** : Scie moddée (configurée dans le système)
3. **Condition** : La bûche doit avoir été extraite avec une hache avant de pouvoir être sciée

### **⚙️ Configuration des Scies Moddées**

#### **Ajouter une scie moddée :**
```java
// Dans JobActions.java, section static {}
SAW_TOOLS.put("monmod:saw", true);
SAW_TOOLS.put("autre_mod:scie", true);
SAW_TOOLS.put("monmod:diamond_saw", true);
```

#### **Ou utiliser les méthodes dynamiques :**
```java
// Ajouter une scie
JobActions.addSawTool("monmod:saw");

// Retirer une scie
JobActions.removeSawTool("monmod:saw");

// Lister toutes les scies
String[] saws = JobActions.listSawTools();
```

### **🪵 Configuration des Drops de Sciage**

#### **Modifier les drops par type de bûche :**
```java
// Dans JobActions.java, section static {}
SAW_CONFIGS.put("minecraft:oak_log", new SawConfig("minecraft:oak_planks", 4));
SAW_CONFIGS.put("minecraft:birch_log", new SawConfig("minecraft:birch_planks", 4));

// Pour des items moddés
SAW_CONFIGS.put("monmod:special_log", new SawConfig("monmod:special_planks", 6));
```

#### **Ou utiliser les méthodes dynamiques :**
```java
// Ajouter une configuration
JobActions.addSawConfig("monmod:special_log", "monmod:special_planks", 6);

// Modifier une configuration existante
JobActions.addSawConfig("minecraft:oak_log", "monmod:oak_planks", 8);

// Retirer une configuration
JobActions.removeSawConfig("minecraft:oak_log");

// Obtenir une configuration
SawConfig config = JobActions.getSawConfig("minecraft:oak_log");
```

### **🎯 Paramètres Modifiables**

#### **Nombre de clics pour scier :**
```java
// Dans JobActions.java
private static final int SAW_CLICKS = 5; // Changer cette valeur
```

#### **Délai entre les clics :**
```java
// Dans handleSawing(), ligne ~365
if (lastTime != null && (currentTime - lastTime) < 1000) { // 1000ms = 1 seconde
```

### **📝 Processus de Sciage**

1. **Extraction** : Utiliser une hache sur un arbre (10 clics)
2. **Récupération** : La bûche tombe et est marquée comme "extractable"
3. **Sciage** : Utiliser une scie sur la bûche au sol (5 clics)
4. **Résultat** : Obtention des planches configurées

### **🔧 Exemples de Configuration**

#### **Scie basique :**
```java
SAW_TOOLS.put("monmod:iron_saw", true);
```

#### **Scie avancée :**
```java
SAW_TOOLS.put("monmod:diamond_saw", true);
SAW_TOOLS.put("monmod:enchanted_saw", true);
```

#### **Drops moddés :**
```java
// Planches spéciales
SAW_CONFIGS.put("monmod:magic_log", new SawConfig("monmod:magic_planks", 8));

// Planches rares
SAW_CONFIGS.put("monmod:ancient_log", new SawConfig("monmod:ancient_planks", 12));
```

### **⚠️ Notes Importantes**

- **Compatibilité mods** : Le système détecte automatiquement les items moddés
- **Fallback** : Si un item moddé n'existe pas, l'action est annulée avec un message debug
- **Durabilité** : Les scies perdent 1 point de durabilité par sciage réussi
- **Restriction** : Seuls les ouvriers peuvent utiliser les scies
- **Sécurité** : Impossible de scier directement un arbre, extraction obligatoire

### **🐛 Debug**

Les messages de debug apparaissent dans le chat :
- `[DEBUG] Configuration de sciage introuvable pour le bloc: [ID]`
- `[DEBUG] Cette bûche n'a pas été extraite! Sciage impossible.`
- `[DEBUG] ERREUR: L'item de drop '[ID]' n'existe pas! Action annulee.`
