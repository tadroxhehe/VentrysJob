# Configuration du Système d'Extraction - Métier Ouvrier

## 🔧 Configuration des Drops

### Modifier le nombre de drops
Dans `JobActions.java`, ligne 26 :
```java
private static final int OAK_EXTRACTION_DROPS = 2; // Changez ce nombre
```

### Modifier l'item de drop (compatible mods)
Dans `JobActions.java`, ligne 29 :
```java
private static final String OAK_DROP_ITEM = "minecraft:oak_log"; // Changez l'item
```

**Exemples d'items moddés :**
- `"minecraft:oak_log"` - Chêne vanilla
- `"create:oak_log"` - Chêne du mod Create
- `"modid:custom_log"` - Item personnalisé d'un autre mod
- `"thermal:rubberwood_log"` - Chêne du mod Thermal Expansion

## 🪓 Compatibilité des Haches

### Haches détectées automatiquement
Le système détecte **TOUTES** les haches qui :
1. **Supportent `ToolActions.AXE_DIG`** (méthode principale - compatible avec tous les mods)
2. **Haches vanilla** (méthode de fallback)

### Haches compatibles
- ✅ **Toutes les haches vanilla** (bois, pierre, fer, or, diamant, netherite)
- ✅ **Haches de mods** (Create, Thermal Expansion, Tinkers' Construct, etc.)
- ✅ **Haches personnalisées** (tant qu'elles supportent `ToolActions.AXE_DIG`)

## ⚙️ Configuration Avancée

### Modifier le nombre de clics requis
Dans `JobActions.java`, ligne 25 :
```java
private static final int OAK_EXTRACTION_CLICKS = 10; // Changez ce nombre
```

### Vérifier si un item moddé existe
```java
boolean exists = JobActions.doesItemExist("modid:custom_item");
```

## 🎯 Exemples de Configuration

### Configuration pour un serveur difficile
```java
private static final int OAK_EXTRACTION_CLICKS = 20; // Plus de clics
private static final int OAK_EXTRACTION_DROPS = 1;   // Moins de drops
```

### Configuration pour un serveur facile
```java
private static final int OAK_EXTRACTION_CLICKS = 5;  // Moins de clics
private static final int OAK_EXTRACTION_DROPS = 3;   // Plus de drops
```

### Configuration avec items moddés
```java
private static final String OAK_DROP_ITEM = "create:oak_log"; // Item du mod Create
```

## 🔍 Dépannage

### L'item moddé ne fonctionne pas ?
1. Vérifiez que le mod est chargé
2. Vérifiez l'ID exact avec `/give @s modid:item_name`
3. Utilisez `JobActions.doesItemExist("modid:item_name")` pour tester

### La hache moddée n'est pas détectée ?
1. Vérifiez que la hache supporte `ToolActions.AXE_DIG`
2. Contactez le développeur du mod pour ajouter le support
3. Utilisez une hache vanilla en attendant

## 📝 Notes Importantes

- **Les constantes sont `final`** : Redémarrez le serveur après modification
- **Fallback automatique** : Si l'item moddé n'existe pas, utilise `minecraft:oak_log`
- **Compatibilité maximale** : Fonctionne avec tous les mods qui respectent les standards Forge
- **Performance** : Le système est optimisé pour ne pas impacter les performances
