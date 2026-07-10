==============================================
  VENTRYS JOB - PACKAGE DE DÉPLOIEMENT
==============================================

Version: 1.0.0
Date: 2024

PRÉREQUIS:
----------
- Minecraft 1.18.2
- Forge 1.18.2-40.3.0 ou supérieur
- Java 17

INSTALLATION:
-------------
1. Installer Forge 1.18.2-40.3.0
2. Ouvrir le dossier .minecraft/mods (client) ou mods/ (serveur)
3. Copier les 2 fichiers JAR depuis le dossier "mods" :
   - ventrysjob-1.0.0.jar
   - geckolib-forge-1.18-3.0.40.jar
4. Lancer Minecraft avec le profil Forge

CONTENU DU PACKAGE:
-------------------
mods/
  ├── ventrysjob-1.0.0.jar          (Mod principal)
  └── geckolib-forge-1.18-3.0.40.jar (Dépendance requise)

FONCTIONNALITÉS:
----------------
- Système de métiers (paysan, forgeron, etc.)
- Animaux personnalisés (cochon, vache, poule) avec animations GeckoLib
- Système de culture personnalisé (indépendant de l'humidité/météo)
- Synchronisation temps réel (heure de Paris) - OPTIMISÉ
- Nid de poule avec gestion des œufs (4 slots, 1 œuf max par slot)
- Meule pour transformation des récoltes
- Système de reproduction avec conditions nutrition/hydratation > 30%
- Production d'œufs (poule) : intervalle aléatoire 2-5 min, pause si conditions non remplies
- Production de lait (vache) : pause si conditions non remplies
- Si nid plein : les poules reset leur progression (œufs perdus)

NOUVELLES FONCTIONNALITÉS:
---------------------------
- Système de FOURCHE configurable via JSON (extraction_config.json)
  * Compatible vanilla et mods externes (IDs d'items configurables)
  * Par défaut : minecraft:yellow_dye (teinture jaune vanilla)
  * Plus besoin d'item moddé, tout est configurable via JSON
- RESTRICTIONS JOB PAYSAN :
  * Nourrir les animaux : uniquement paysan
  * Hydrater les animaux : uniquement paysan
  * Planter des récoltes : uniquement paysan
  * Labourer (till farmland) : uniquement paysan
  * Récolter avec la fourche : uniquement paysan
- Système de récolte avec la fourche :
  * 5 clics requis pour récolter (configurable)
  * Compatible avec IDs vanilla et moddés pour les outils fourche
  * Configuration des drops par culture (blé, carottes, pommes de terre, betteraves)
  * Perd 1 de durabilité par récolte
  * Ne récolte que les cultures matures (âge maximum)
- Interface nid de poule : 4 slots vanilla centrés, sans fond ni inventaire visible
- Zone interactive des slots alignée avec les textures
- Messages de progression uniformisés : tous affichent "Progression" au lieu de "Récolte", "Extraction", etc.
- Système de temps optimisé : synchronisation conditionnelle (seulement quand l'heure IRL change)
- Stripping vanilla désactivé : les bûches extractibles ne peuvent plus être épluchées (évite conflit avec extraction)

COMMANDES EN JEU:
-----------------
/ventrysjob <métier> - Activer un métier
/ventrystime info - Afficher les infos de synchronisation du temps
/ventrystime debug <true/false> - Activer/désactiver les logs détaillés
/ventrystime sync - Forcer la synchronisation immédiate

CONFIGURATION:
--------------
Les configurations sont dans data/ventrysjob/ :
- extraction_config.json : Configuration des outils et récoltes (fourche, drops, stripping désactivé)
- crop_growth.json : Configuration de la croissance des cultures
- jobs.json : Configuration des métiers et recettes
- mobs_config.json : Configuration des animaux

SUPPORT:
--------
Pour toute question ou problème, contactez l'administrateur du serveur.

==============================================

