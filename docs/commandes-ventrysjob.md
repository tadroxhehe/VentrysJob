# Commandes VentrysJob — référence rapide

Toutes les commandes sont enregistrées côté **serveur**. Seul **`/ventrystime`** exige d’être **OP** (permission **2**). La commande **`/job`** est utilisable par tout joueur.

---

## `/job` — métiers joueurs

| Sous-commande | Fonction |
|---------------|----------|
| `me` | Affiche **ton** métier actuel (`jobId` ou « aucun »). |
| `list` | Liste **tous** les métiers chargés (`id` + nom affiché). |
| `set <joueur> <jobId>` | Assigne un métier à un ou plusieurs joueurs. `jobId` doit exister dans `jobs.json`. |
| `remove <joueur>` | Retire le métier des joueurs ciblés. |
| `check <joueur>` | Affiche le `jobId` du joueur (ou absence de métier). |
| `reset` | **Réinitialise ta progression** liée aux extractions / actions métier (`JobActions.resetPlayerProgress`). |
| `cleanup` | Force un nettoyage global des progressions d’extraction actives (`JobActions.forceCleanup`). |
| `reload` | Recharge **`jobs.json`** (vide le cache d’items puis `JobManager.loadJobs`). |
| `debug` | Résumé debug : jobs chargés, `jobId` + nom + **nombre de recettes** par métier. |

**Exemples :** `/job me` · `/job list` · `/job set @p artisan` · `/job reload`

---

## `/ventrystime` — temps IRL ↔ temps du monde (**OP uniquement**)

**Permission 2 requise** sur toute la commande. Sert au sync jour/nuit (`RealTimeManager`).

| Sous-commande | Fonction |
|---------------|----------|
| `info` | Heure **IRL Paris**, heure de **jeu cible**, heure **actuelle** du monde, écart en ticks, `doDaylightCycle`, ticks bruts / normalisés. |
| `debug` | Bascule le flag debug temps (sans argument : inverse l’état). |
| `debug <true\|false>` | Active ou désactive ce flag. |
| `sync` | **Force** la resynchronisation du temps du monde avec la cible temps réel. |

**Exemples :** `/ventrystime info` · `/ventrystime sync` · `/ventrystime debug true`
