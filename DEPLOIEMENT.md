# Guide de déploiement VentrysJob

## 📦 Préparation pour la distribution

### Étape 1 : Compilation des dépendances

```bash
# Compiler EcoVentrys
cd ../EcoVentrys
gradlew clean build
cd ..

# Compiler VentrysItem
cd VentrysItem
gradlew clean build
cd ..

# Compiler VentrysJob
cd VentrysJob
gradlew clean build
```

### Étape 2 : Récupération des JAR

Les fichiers JAR compilés se trouvent dans :

```
EcoVentrys/build/libs/ecoventrys-1.0.0.jar
VentrysItem/build/libs/ventrysitem-1.0.0.jar
VentrysJob/build/libs/ventrysjob-1.0.0.jar
```

### Étape 3 : Package de distribution

Créer un dossier avec tous les fichiers nécessaires :

```
VentrysJobPack/
├── README.txt
├── INSTALLATION.txt
├── mods/
│   ├── ecoventrys-1.0.0.jar
│   ├── ventrysitem-1.0.0.jar
│   └── ventrysjob-1.0.0.jar
├── config/
│   └── ventrysjob/
│       └── jobs.json (exemple)
└── docs/
    ├── GUIDE-COMPLET-FR.md
    └── INTEGRATION-MODS.md
```

---

## 🖥️ Déploiement sur serveur

### Configuration serveur vanilla → Forge

1. **Arrêter le serveur** si actif

2. **Sauvegarder** le monde et les configs :
   ```bash
   cp -r world world_backup
   cp -r config config_backup
   ```

3. **Installer Forge Server** :
   ```bash
   wget https://maven.minecraftforge.net/net/minecraftforge/forge/1.18.2-40.2.0/forge-1.18.2-40.2.0-installer.jar
   java -jar forge-1.18.2-40.2.0-installer.jar --installServer
   ```

4. **Copier les mods** :
   ```bash
   mkdir -p mods
   cp ecoventrys-1.0.0.jar mods/
   cp ventrysitem-1.0.0.jar mods/
   cp ventrysjob-1.0.0.jar mods/
   ```

5. **Configurer le démarrage** :
   
   Créer `start.sh` :
   ```bash
   #!/bin/bash
   java -Xmx4G -Xms2G -jar forge-1.18.2-40.2.0.jar nogui
   ```
   
   Ou `start.bat` (Windows) :
   ```batch
   @echo off
   java -Xmx4G -Xms2G -jar forge-1.18.2-40.2.0.jar nogui
   pause
   ```

6. **Premier lancement** :
   ```bash
   chmod +x start.sh
   ./start.sh
   ```
   
   Le fichier `config/ventrysjob/jobs.json` sera créé automatiquement.

7. **Personnaliser** `jobs.json` selon vos besoins

8. **Redémarrer** le serveur

### Mise à jour du mod

1. **Arrêter** le serveur

2. **Sauvegarder** la config :
   ```bash
   cp config/ventrysjob/jobs.json jobs.json.backup
   ```

3. **Remplacer** le JAR :
   ```bash
   rm mods/ventrysjob-*.jar
   cp ventrysjob-X.X.X.jar mods/
   ```

4. **Restaurer** la config si nécessaire

5. **Redémarrer** le serveur

---

## 👥 Distribution aux joueurs

### Package client

Créer un ZIP avec :

```
VentrysJobClient.zip
├── README-CLIENT.txt
├── mods/
│   ├── ecoventrys-1.0.0.jar
│   ├── ventrysitem-1.0.0.jar
│   └── ventrysjob-1.0.0.jar
└── GUIDE.txt
```

### Instructions pour les joueurs

**README-CLIENT.txt** :

```
==============================================
  INSTALLATION VENTRYS JOB - CLIENT
==============================================

Prérequis:
  - Minecraft 1.18.2
  - Forge 1.18.2-40.2.0 ou supérieur
  - Java 17

Installation:
  1. Installer Forge 1.18.2
  2. Ouvrir le dossier .minecraft/mods
  3. Copier les 3 fichiers JAR depuis le dossier "mods"
  4. Lancer Minecraft avec le profil Forge

Commandes en jeu:
  /jobs - Ouvrir le menu des métiers
  Touche J - Raccourci pour ouvrir le menu

Support:
  Contactez l'administrateur du serveur
==============================================
```

---

## 🔄 Workflow de développement

### Pour les développeurs

1. **Cloner le dépôt** :
   ```bash
   git clone <repo> VentrysJob
   cd VentrysJob
   ```

2. **Configurer les dépendances** :
   ```bash
   # S'assurer que EcoVentrys et VentrysItem sont dans ../
   ls ../EcoVentrys/build/libs/ecoventrys-1.0.0.jar
   ls ../VentrysItem/build/libs/ventrysitem-1.0.0.jar
   ```

3. **Compiler** :
   ```bash
   gradlew build
   ```

4. **Tester en mode dev** :
   ```bash
   gradlew runClient
   ```

5. **Générer le JAR final** :
   ```bash
   gradlew build reobfJar
   # JAR dans build/reobfJar/output.jar
   ```

### Structure Git recommandée

```
.gitignore          # Ignorer build/, .gradle/, etc.
README.md           # Documentation principale
CHANGELOG.md        # Historique des versions
src/                # Code source
build.gradle        # Configuration Gradle
```

---

## 📝 Checklist de déploiement

### Avant la release

- [ ] Compiler les 3 mods sans erreurs
- [ ] Tester en solo (runClient)
- [ ] Tester sur serveur local
- [ ] Vérifier tous les métiers
- [ ] Vérifier toutes les recettes
- [ ] Tester les crafts avec/sans énergie
- [ ] Vérifier les traductions FR/EN
- [ ] Tester avec des items VentrysItem
- [ ] Vérifier les logs (pas d'erreurs)
- [ ] Créer le package de distribution
- [ ] Écrire les notes de version
- [ ] Mettre à jour CHANGELOG.md

### Pour la distribution

- [ ] Créer le ZIP client
- [ ] Créer le ZIP serveur
- [ ] Rédiger les instructions
- [ ] Tester l'installation depuis zéro
- [ ] Créer la documentation utilisateur
- [ ] Préparer le support

---

## 🐛 Tests recommandés

### Tests fonctionnels

1. **Installation** :
   - ✓ Installation propre
   - ✓ Mise à jour depuis version précédente
   - ✓ Installation avec autres mods

2. **Métiers** :
   - ✓ Activation d'un métier
   - ✓ Changement de métier
   - ✓ Désactivation de métier

3. **Crafts** :
   - ✓ Craft avec items vanilla
   - ✓ Craft avec items VentrysItem
   - ✓ Craft avec énergie suffisante
   - ✓ Craft avec énergie insuffisante
   - ✓ Craft avec items manquants

4. **Interface** :
   - ✓ Ouverture GUI (J et /jobs)
   - ✓ Affichage HUD
   - ✓ Scroll dans les listes
   - ✓ Fermeture propre

5. **Réseau** :
   - ✓ Synchronisation client/serveur
   - ✓ Crafts multiples
   - ✓ Plusieurs joueurs simultanés

### Tests de charge

- ✓ 10+ joueurs avec métiers différents
- ✓ Crafts simultanés
- ✓ Changements de métiers fréquents
- ✓ Vérifier les logs (pas de lag)

---

## 📊 Monitoring

### Logs à surveiller

**Client** : `.minecraft/logs/latest.log`
```
[VentrysJob] Chargé 4 métiers avec succès
[VentrysJob] Commandes VentrysJob enregistrées
[VentrysJob] Réseau VentrysJob enregistré
```

**Serveur** : `logs/latest.log`
```
[VentrysJob] Initialisation commune
[VentrysJob] Fichier jobs.json créé avec 4 métiers par défaut
[VentrysJob] Joueur <pseudo> a activé le métier: blacksmith
[VentrysJob] Joueur <pseudo> a crafté: Épée en fer
```

### Métriques

- Nombre de métiers chargés
- Nombre de recettes actives
- Nombre de crafts par jour
- Énergie moyenne consommée
- Métiers les plus populaires

---

## 🚀 Release

### Processus de release

1. **Finaliser le code**
   ```bash
   git checkout develop
   git pull
   gradlew clean build
   ```

2. **Tests finaux**
   - Exécuter tous les tests
   - Vérifier en jeu

3. **Versioning**
   ```bash
   # Mettre à jour build.gradle
   version = '1.0.1'
   
   git add .
   git commit -m "Release v1.0.1"
   git tag v1.0.1
   git push origin master --tags
   ```

4. **Distribution**
   - Créer les packages
   - Uploader sur le serveur
   - Notifier les utilisateurs

5. **Documentation**
   - Mettre à jour CHANGELOG.md
   - Annoncer les nouveautés

---

## 📚 Ressources

### Liens utiles

- Forge Documentation : https://docs.minecraftforge.net
- Minecraft Wiki : https://minecraft.fandom.com
- JSON Validator : https://jsonlint.com

### Outils

- **Gradle** : Compilation
- **Git** : Gestion de version
- **Eclipse/IntelliJ** : IDE
- **JSON Editor** : Édition des configs

---

© Ventrys Team - 2025

