# Configuration du Four d'Ouvrier

## Description
Le Four d'Ouvrier est un bloc spécialisé pour le métier "ouvrier" qui permet de transformer les planches en charbon.

## Fonctionnalités

### Interface
- **2 slots** : 
  - Slot gauche : Planches à transformer (entrée)
  - Slot droite : Charbon produit (sortie)

### Transformation
- **Input** : Planches (vanilla ou moddées)
- **Output** : Charbon (vanilla ou moddé)
- **Temps configurable** : Par défaut 5 secondes (100 ticks)

## Configuration des Recettes

### Ajouter une Recette
```java
// Dans le code ou via une méthode de configuration
OuvrierFourBlockEntity.addTransformationRecipe(
    "minecraft:oak_planks",      // Input: planches de chêne
    "minecraft:charcoal",        // Output: charbon
    100                          // Temps: 100 ticks (5 secondes)
);
```

### Recette Configurée
Le four a une seule recette configurée directement :
- `minecraft:oak_planks` → `minecraft:charcoal` (5 secondes)

### Support des Mods
Pour ajouter des planches de mods externes :
```java
// Exemple avec des planches d'un mod externe
OuvrierFourBlockEntity.addTransformationRecipe(
    "modname:custom_planks",     // Planches du mod
    "minecraft:charcoal",        // Charbon (peut être moddé aussi)
    80                          // Temps personnalisé
);
```

## Utilisation

### Accès
1. Seul les joueurs avec le métier "ouvrier" peuvent utiliser le four
2. Clic droit sur le four pour ouvrir l'interface

### Processus de Transformation
1. Placer des planches dans le slot gauche
2. Le four commence automatiquement la transformation
3. Le charbon apparaît dans le slot droite après le temps configuré

### Interface
- **Barre de progression** : Affiche l'avancement de la transformation
- **Slots verrouillés** : Le slot de sortie ne peut pas être rempli manuellement
- **Validation automatique** : Seules les planches configurées sont acceptées (par défaut : planches de chêne uniquement)

## Configuration Avancée

### Temps de Transformation
- **Défaut** : 100 ticks (5 secondes)
- **Personnalisable** : Via la méthode `addTransformationRecipe()`
- **Unités** : 1 tick = 1/20 seconde

### Gestion des Erreurs
- **Inventaire plein** : La transformation s'arrête si le slot de sortie est plein
- **Items invalides** : Les items non configurés sont rejetés
- **Persistance** : La progression est sauvegardée avec le chunk

## Intégration Multiplayer

### Compatibilité Serveur
- **Thread-safe** : Fonctionne en multijoueur
- **Synchronisation** : Interface synchronisée entre client/serveur
- **Performance** : Optimisé pour les serveurs avec de nombreux fours

### Restrictions
- **Métier requis** : Seuls les ouvriers peuvent utiliser le four
- **Validation côté serveur** : Toutes les validations sont faites côté serveur

## Exemples d'Utilisation

### Configuration Basique
```java
// Ajouter une nouvelle recette
OuvrierFourBlockEntity.addTransformationRecipe(
    "minecraft:oak_planks",
    "minecraft:charcoal", 
    120  // 6 secondes
);
```

### Configuration Moddée
```java
// Planches d'un mod externe
OuvrierFourBlockEntity.addTransformationRecipe(
    "biomesoplenty:cherry_planks",
    "minecraft:charcoal",
    150  // 7.5 secondes
);
```

### Suppression de Recette
```java
// Retirer une recette
OuvrierFourBlockEntity.removeTransformationRecipe("minecraft:oak_planks");
```

## Notes Techniques

### Performance
- **Tick manuel** : La transformation se déclenche manuellement (pas de tick automatique)
- **Optimisation** : Vérifications minimales pour les performances
- **Mémoire** : Gestion optimisée de la mémoire

### Compatibilité
- **Minecraft** : 1.18.2
- **Forge** : Version compatible 1.18.2
- **Mods** : Compatible avec tous les mods ajoutant des planches

### Limitations
- **2 slots maximum** : Entrée et sortie uniquement
- **1:1 ratio** : 1 planche = 1 charbon (configurable)
- **Temps fixe** : Le temps ne peut pas être modifié dynamiquement pendant la transformation
