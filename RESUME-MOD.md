VentrysJob - Résumé Complet du Mod

Vue d'ensemble

VentrysJob est un mod de métiers pour serveur RP médiéval sur Minecraft 1.18.2 (Forge). Il ajoute un système complet de professions avec des blocs fonctionnels, des recettes personnalisables, et une intégration avec les mods EcoVentrys (système d'énergie) et VentrysItem (items personnalisés).

Architecture du Mod

Système de Métiers
- Activation/Désactivation : Chaque joueur peut activer un métier à la fois
- Configuration JSON : Tous les métiers et recettes sont définis dans config/ventrysjob/jobs.json
- Validation automatique : Les items sont vérifiés avant le craft (pas de crash si item manquant)
- Support multi-mods : Utilisation d'items d'autres mods via leur ID

Intégrations
- EcoVentrys : Système d'énergie pour les actions métiers
- VentrysItem : Support de 100+ items personnalisés
- VentrysBlocs : Support des blocs personnalisés

Blocs Disponibles

Tables de Métier
Ces blocs ouvrent l'interface des métiers lorsqu'on clique dessus :

1. Table de Forgeron (forgeron_table)
   - Ouvre le menu des métiers avec le métier "Forgeron" sélectionné

2. Table d'Artisan (artisan_table)
   - Ouvre le menu des métiers avec le métier "Artisan" sélectionné

3. Table d'Apothicaire (apothicaire_table)
   - Ouvre le menu des métiers avec le métier "Apothicaire" sélectionné

4. Table de Cuisinier (cuisinier_table)
   - Ouvre le menu des métiers avec le métier "Cuisinier" sélectionné

5. Métier à Tisser (metier_tisser)
   - Bloc de tissage (fonctionnalité en développement)

Blocs Fonctionnels

1. Four Ouvrier (ouvrier_four)
   - Four à charbon pour transformation de ressources
   - Accepte 1-4 items (planches, charbon, etc.)
   - Allumage avec briquet (feu temporaire de 60 secondes)
   - Interface avec slot unique
   - Transformation automatique des items valides

2. Four de Forgeron (forgeron_four)
   - Four de forge pour la métallurgie
   - Slots : combustible, minerai, résultat
   - Allumage avec briquet (feu temporaire de 60 secondes)
   - Fonctionne comme un four vanilla amélioré
   - 76 recettes de forge configurées

3. Meule (meule)
   - Bloc pour moudre des ressources
   - Interface avec slots d'entrée et de sortie
   - Configuration via JSON

4. Vase d'Apothicaire (vase_apothicaire)
   - Bloc pour la préparation de potions et remèdes
   - Fonctionnalité de plantation

5. Sac à Sel (sac_sel)
   - Stockage de sel
   - Interface avec inventaire

6. Nid de Poule (chicken_nest)
   - Collecte d'œufs de poules
   - Interface pour récupérer les œufs

Métiers Disponibles

1. Forgeron (forgeron)
Utilité : Spécialisé dans la forge et la métallurgie. Fabrique tous les objets métalliques : armures, armes, outils, lingots, verre, clous, briquets, et équipements de forge (enclume, alambic, marmite). Essentiel pour l'équipement militaire et les outils de travail.

2. Artisan (artisan)
Utilité : Créateur d'objets décoratifs et utilitaires en bois. Fabrique des bols, des objets de décoration et des ustensiles du quotidien. Complémentaire au travail du forgeron pour les objets non-métalliques.

3. Apothicaire (apothicaire)
Utilité : Maître des potions et remèdes. Crée des potions de soin et des remèdes pour soigner les blessures. Indispensable pour la survie et le support médical en RP.

4. Cuisinier (cuisinier)
Utilité : Expert en cuisine et nutrition. Prépare tous les plats et aliments : pain, gâteaux, soupes, ragoûts, pommes dorées, carottes dorées. Essentiel pour nourrir les joueurs et restaurer la faim.

5. Ouvrier (ouvrier)
Utilité : Spécialisé dans l'extraction et le travail des ressources naturelles. Transforme les matières premières (planches en charbon, etc.) et gère les ressources de base. Fondamental pour la production de matériaux essentiels.

6. Couturier (couturier)
Utilité : Maître du tissage et de la couture. Fabrique les vêtements et armures en cuir (casques, armures). Complémentaire au forgeron pour l'équipement en cuir et les vêtements.

7. Bâtisseur (batisseur)
Utilité : Spécialiste de la construction et de l'architecture. Permet de construire et d'aménager les bâtiments. Essentiel pour le développement des villes et des structures en RP.

Commandes Disponibles

Commandes Principales (/job)

/job me
- Description : Affiche votre métier actif
- Permission : Tous les joueurs
- Exemple : /job me

/job list
- Description : Liste tous les métiers disponibles
- Permission : Tous les joueurs
- Exemple : /job list

/job set <joueur> <id_métier>
- Description : Définit le métier d'un joueur (admin uniquement)
- Permission : Op niveau 2
- Exemple : /job set Notch forgeron

/job remove <joueur>
- Description : Retire le métier d'un joueur (admin uniquement)
- Permission : Op niveau 2
- Exemple : /job remove Notch

/job check <joueur>
- Description : Vérifie le métier d'un joueur (admin uniquement)
- Permission : Op niveau 2
- Exemple : /job check Notch

/job reset
- Description : Réinitialise votre progression de métier
- Permission : Tous les joueurs
- Exemple : /job reset

/job cleanup
- Description : Force le nettoyage des progressions (admin uniquement)
- Permission : Op niveau 2
- Exemple : /job cleanup

/job reload
- Description : Recharge le fichier jobs.json (admin uniquement)
- Permission : Op niveau 2
- Exemple : /job reload
- Note : Utile après modification du fichier de configuration

/job debug
- Description : Affiche les informations de debug sur les métiers (admin uniquement)
- Permission : Op niveau 2
- Exemple : /job debug
- Affiche : Nombre de métiers chargés, nombre de recettes par métier

Commandes de Temps (/ventrystime)

/ventrystime info
- Description : Affiche les informations de synchronisation du temps
- Permission : Op niveau 2
- Affiche :
  - Heure IRL (Paris)
  - Heure de jeu cible
  - Heure de jeu actuelle
  - Différence en ticks et secondes
  - Temps brut du monde

/ventrystime debug [true/false]
- Description : Active/désactive le mode debug du temps
- Permission : Op niveau 2
- Exemple : /ventrystime debug true
- Sans argument : Toggle le mode debug

/ventrystime sync
- Description : Force la synchronisation du temps du monde avec l'heure IRL
- Permission : Op niveau 2
- Exemple : /ventrystime sync

Interface Utilisateur

Menu des Métiers
- Ouverture : Touche J ou /job ou clic sur une table de métier
- Fonctionnalités :
  - Liste des métiers avec descriptions
  - Liste des recettes par métier (scrollable)
  - Affichage détaillé des recettes :
    - Ingrédients requis
    - Résultat
    - Coût en énergie
  - Boutons "Activer" et "Craft"
  - Bouton "Fermer"

HUD en Jeu
- Position : Coin supérieur droit
- Affichage :
  - Métier actif
  - Barre d'énergie (intégration EcoVentrys)
  - Couleur dynamique :
    - Vert : > 60%
    - Orange : 30-60%
    - Rouge : < 30%
  - Pourcentage d'énergie

Configuration

Fichier Principal
Emplacement : config/ventrysjob/jobs.json

Structure :
{
  "jobs": [
    {
      "id": "forgeron",
      "name": "Forgeron",
      "description": "Spécialisé dans la forge et la métallurgie",
      "recipes": [
        {
          "id": "alambic",
          "name": "Alambic",
          "description": "Alambic pour la distillation",
          "inputs": [
            {
              "itemId": "ventrysitem:res_cuivre_lingot",
              "count": 40
            }
          ],
          "output": {
            "itemId": "ventrys_blocs:alambic",
            "count": 1
          }
        }
      ]
    }
  ]
}

Autres Fichiers de Configuration
- config/ventrysjob/extraction_config.json : Configuration des outils d'extraction
- config/ventrysjob/meule_config.json : Configuration de la meule
- config/ventrysjob/crop_growth_config.json : Configuration de la croissance des cultures
- config/ventrysjob/mob_config.json : Configuration des animaux personnalisés

Fonctionnalités Techniques

Système de Craft
- Validation automatique : Vérifie que tous les items existent avant le craft
- Gestion d'erreurs : Pas de crash si item manquant (recette ignorée)
- Support multi-mods : Utilise les IDs complets (modid:item_id)
- Coût en énergie : Intégration avec EcoVentrys
- Temps de craft : Configurable par recette

Système de Fours
- Feu temporaire : 60 secondes après allumage avec briquet
- Limite de stack : Four ouvrier accepte max 4 items
- Shift+Clic : Gestion intelligente (place max 4, garde le reste)
- Synchronisation : État visuel synchronisé avec l'état logique

Système de Temps
- Synchronisation IRL : Le temps de jeu suit l'heure réelle (Paris)
- Conversion automatique : Heure IRL → Heure de jeu Minecraft
- Commandes de debug : Outils pour vérifier la synchronisation

Items et Entités

Items
- Farine (flour) : Item de base pour la cuisine

Entités Personnalisées
- Cochon Personnalisé (custom_pig) : Avec spawn egg
- Vache Personnalisée (custom_cow) : Avec spawn egg
- Poule Personnalisée (custom_chicken) : Avec spawn egg

Utilisation Rapide

1. Activer un métier :
   - Ouvrir le menu (touche J ou /job)
   - Sélectionner un métier
   - Cliquer sur "Activer"

2. Craft une recette :
   - Avoir un métier actif
   - Ouvrir le menu
   - Sélectionner une recette
   - Vérifier les ingrédients et l'énergie
   - Cliquer sur "Craft"

3. Utiliser un four :
   - Placer le four
   - Clic droit avec briquet pour allumer
   - Placer les items (max 4 pour four ouvrier)
   - Attendre la transformation

Dépendances

Obligatoires
- Minecraft Forge 1.18.2-40.2.0
- Java 17
- EcoVentrys : Système d'énergie
- VentrysItem : Items personnalisés

Optionnelles
- VentrysBlocs : Blocs personnalisés
- Autres mods avec items compatibles

Notes Importantes

- Localisation : Tous les messages utilisent TranslatableComponent pour un support multilingue correct
- Accents : Tous les accents sont préservés grâce aux fichiers de traduction (fr_fr.json, en_us.json)
- Sauvegarde : Les données joueur sont sauvegardées automatiquement
- Performance : Nettoyage automatique des progressions inactives

Dépannage

Problème | Solution
Mod ne charge pas | Vérifier Java 17 + Forge 1.18.2
Recette invalide | Vérifier les IDs d'items dans jobs.json
Pas d'énergie | Vérifier EcoVentrys installé
HUD invisible | Activer un métier avec /job
Four ne s'allume pas | Utiliser un briquet (flint and steel)
Items perdus au Shift+Clic | Corrigé dans la dernière version

Version : 1.0.0
Minecraft : 1.18.2
Forge : 40.2.0
Dernière mise à jour : 2024
