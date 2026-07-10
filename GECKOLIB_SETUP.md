# Configuration GeckoLib

## Problème
GeckoLib n'est pas disponible via Maven pour la compilation, mais votre modpack l'a déjà en runtime.

## Solution

### Option 1 : Utiliser le JAR depuis votre modpack (Recommandé)

1. Trouvez le JAR GeckoLib dans votre modpack (dossier `mods/` ou similaire)
2. Copiez-le dans le dossier `libs/` de ce projet
3. Renommez-le si nécessaire pour qu'il soit facilement identifiable (ex: `geckolib-forge-1.18.2.jar`)
4. Décommentez cette ligne dans `build.gradle` :
   ```groovy
   implementation files('libs/geckolib-forge-1.18.2.jar')
   ```
5. Relancez `.\gradlew build`

### Option 2 : Télécharger depuis CurseForge

1. Allez sur https://www.curseforge.com/minecraft/mc-mods/geckolib
2. Téléchargez la version compatible avec Minecraft 1.18.2
3. Placez le JAR dans le dossier `libs/`
4. Décommentez et ajustez la ligne dans `build.gradle`

## Note
Le code est déjà prêt pour GeckoLib. Une fois la dépendance ajoutée, tout devrait compiler et fonctionner.

