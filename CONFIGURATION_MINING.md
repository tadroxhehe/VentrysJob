# Configuration du Système de Minage - VentrysJob

## Vue d'ensemble

Le système de minage permet aux ouvriers d'extraire des minerais avec des pioches en utilisant un système de clics progressifs (10 clics pour 1 drop). Le système est compatible avec tous les mods et détecte automatiquement les pioches moddées.

## Fonctionnalités

- ✅ **10 clics pour 1 drop** de minerai
- ✅ **1 de durabilité** perdue par minerai extrait
- ✅ **Détection automatique** des pioches vanilla et moddées
- ✅ **Compatible avec tous les mods** (IDs configurables)
- ✅ **Drops configurables** par type de minerai
- ✅ **Casse normale désactivée** pour les minerais extractibles

## Minerais Configurés par Défaut

### Minerais Vanilla
- **Charbon** : `minecraft:coal_ore` → `minecraft:coal`
- **Charbon Deepslate** : `minecraft:deepslate_coal_ore` → `minecraft:coal`
- **Fer** : `minecraft:iron_ore` → `minecraft:raw_iron`
- **Fer Deepslate** : `minecraft:deepslate_iron_ore` → `minecraft:raw_iron`
- **Or** : `minecraft:gold_ore` → `minecraft:raw_gold`
- **Or Deepslate** : `minecraft:deepslate_gold_ore` → `minecraft:raw_gold`
- **Cuivre** : `minecraft:copper_ore` → `minecraft:raw_copper`
- **Cuivre Deepslate** : `minecraft:deepslate_copper_ore` → `minecraft:raw_copper`
- **Lapis** : `minecraft:lapis_ore` → `minecraft:lapis_lazuli`
- **Lapis Deepslate** : `minecraft:deepslate_lapis_ore` → `minecraft:lapis_lazuli`
- **Redstone** : `minecraft:redstone_ore` → `minecraft:redstone`
- **Redstone Deepslate** : `minecraft:deepslate_redstone_ore` → `minecraft:redstone`
- **Diamant** : `minecraft:diamond_ore` → `minecraft:diamond`
- **Diamant Deepslate** : `minecraft:deepslate_diamond_ore` → `minecraft:diamond`
- **Émeraude** : `minecraft:emerald_ore` → `minecraft:emerald`
- **Émeraude Deepslate** : `minecraft:deepslate_emerald_ore` → `minecraft:emerald`

### Minerais du Nether
- **Or du Nether** : `minecraft:nether_gold_ore` → `minecraft:gold_nugget`
- **Quartz** : `minecraft:nether_quartz_ore` → `minecraft:quartz`
- **Ancient Debris** : `minecraft:ancient_debris` → `minecraft:netherite_scrap`

## Configuration des Minerais Moddés

### Ajouter un Minerai Moddé

```java
// Dans JobActions.java, section MINING_CONFIGS
MINING_CONFIGS.put("monmod:copper_ore", new MiningConfig("monmod:copper_ingot", 1));
MINING_CONFIGS.put("autre_mod:titanium_ore", new MiningConfig("autre_mod:titanium_nugget", 2));
```

### Méthodes de Configuration Dynamique

```java
// Ajouter un minerai
JobActions.addMiningConfig("monmod:silver_ore", "monmod:silver_ingot", 1);

// Retirer un minerai
JobActions.removeMiningConfig("monmod:copper_ore");

// Obtenir la configuration d'un minerai
MiningConfig config = JobActions.getMiningConfig("minecraft:iron_ore");
```

## Détection des Pioches

### Pioches Vanilla (Détection Automatique)
- Pioche en bois
- Pioche en pierre
- Pioche en fer
- Pioche en or
- Pioche en diamant
- Pioche en netherite

### Pioches Moddées (Détection Automatique)
Le système utilise `ToolActions.PICKAXE_DIG` pour détecter automatiquement toutes les pioches moddées qui respectent les standards Forge.

## Utilisation

1. **Devenir Ouvrier** : Utilisez `/job ouvrier`
2. **Tenir une Pioche** : N'importe quelle pioche (vanilla ou moddée)
3. **Clic Droit sur un Minerai** : Commencer l'extraction
4. **10 Clics** : Terminer l'extraction
5. **Résultat** : 1 drop du minerai + 1 de durabilité perdue

## Messages

- `"Extraction: X/10"` : Progression de l'extraction
- `"Extraction reussie!"` : Extraction terminée avec succès

## Restrictions

- ❌ **Mode Créatif** : Les extractions sont désactivées
- ❌ **Casse Normale** : Les pioches ne cassent plus les minerais normalement
- ❌ **Autres Métiers** : Seuls les ouvriers peuvent extraire
- ❌ **Délai** : 1 seconde entre chaque clic

## Compatibilité Multijoueur

✅ Le système fonctionne parfaitement en multijoueur :
- Les données sont partagées entre tous les joueurs
- N'importe quel ouvrier peut extraire n'importe quel minerai
- Compatible avec les serveurs dédiés

## Exemples de Configuration

### Minerai Simple
```java
MINING_CONFIGS.put("monmod:tin_ore", new MiningConfig("monmod:tin_ingot", 1));
```

### Minerai avec Drop Multiple
```java
MINING_CONFIGS.put("monmod:platinum_ore", new MiningConfig("monmod:platinum_nugget", 3));
```

### Minerai avec Drop Moddé
```java
MINING_CONFIGS.put("monmod:cobalt_ore", new MiningConfig("monmod:cobalt_dust", 2));
```

## Dépannage

### Le Minerai n'est pas Détecté
1. Vérifiez que l'ID du bloc est correct
2. Ajoutez le minerai dans `MINING_CONFIGS`
3. Redémarrez le serveur

### La Pioche Moddée ne Fonctionne pas
1. Vérifiez que la pioche utilise `ToolActions.PICKAXE_DIG`
2. Contactez l'auteur du mod pour la compatibilité Forge

### Le Drop n'apparaît pas
1. Vérifiez que l'ID de l'item de drop est correct
2. Vérifiez que l'item existe dans le jeu
3. Testez avec un item vanilla d'abord
