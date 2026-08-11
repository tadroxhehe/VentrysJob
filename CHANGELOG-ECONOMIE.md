# Changelog économie — VentrysJob

Historique des passes (du plus récent au plus ancien).

---

## 1.0.44 — Four charbon ÷10

Transformation ouvrier : **2 h → 12 min**.

## 1.0.43 — Éco vase / cultures / argile / four / livre

- Vase apo : **×2** matières (24 argile + 12 terre), `energyCost` **22**
- Cultures : durée de stade **+20 %** (~48 h → ~57,6 h total)
- Vase apo pousse : **4 h → 5 h**
- Livre vierge : **plume retirée** du craft couturier
- Argile : drop **1 → 4** / bloc
- Four forgeron : fonte **20 s → 8 s**, feu briquet **1 → 5 min**, charbon **1 → 2 min**

## 1.0.41 — Énergie bûches ↓

`EXTRACT_LOG` **0,35 → 0,15** (~10 énergie / 64 bûches au lieu de ~22). Volume bois trop cher vs demande ouvrier.

## Survival 1.0.10 — Marteaux de forge

Durabilité : bronze **30 → 150**, fer **84 → 250** (1 usure / craft enclume). Rentabilité forgeron vs coût matière du marteau.

## 1.0.38 — Bâti constructible ÷2

Coûts artisan des blocs constructibles (bois planche+clou, pierre) **divisés par 2** pour sorties `minecraft` / `westerosblocks` / `ventrys_blocs`.  
Ex. planche **6+2 → 3+1** ; cobble **18+2 → 9+1**. Stations / pantins / chariots inchangés.

## 1.0.15 — Bâti : planches / pierre ×3 (pas ×6)

Recalibrage : `res_planche_*` et `res_pierre_fragmente` constructibles à **×3** vs grille « 2+2 / cobble 6 ».  
Ex. planche bloc **6+2** ; cobble **18+2**. Stations / forgeron inchangés.

## 1.0.14 — Bâti : planches / pierre ressource ×6 (remplacé par 1.0.15)

## 1.0.13 / Survival 1.0.5 — Outils : métal ↑ + fer plus durable (−50 pts)

- Coûts lingots outils ~**+30 %** : pioche/hache/fourche **8**, pelle/scie/burin/marteau **7** (fer & bronze).
- Durabilité extract : bronze **250**, fer **340** (~+30 % fer, −50 pts vs passe précédente). Marteau : bronze 30 / fer 54.

## 1.0.12 — Four de forge chez le forgeron

Retiré de l’artisan. Craft forgeron : **40** lingots de fer → `ventrysjob:forgeron_four`.

## 1.0.11 — Établi forgeron = 100 lingots

`enclume` : **5 → 100** `minecraft:iron_ingot`.

## 1.0.10 — Armes fer + armures plaque/maille ×1,3

Métal / maille arrondis à la main (~×1,3) : armes fer, boucliers, plaque, maille.  
Outils inchangés. Ex. flamberge 31→**40**, plaque corps T3 49→**64**, maille corps 115→**150**.

## 1.0.9 — Charbon = bûches ; forgeron reste en planches

- Four ouvrier + recette `charcoal_craft` : entrée **`minecraft:*_log`** (extract), plus `res_planche_*`.
- Forgeron outils/armes : **retour planches** (`res_planche_chene` / sapin) — l’essai manches en bûches est annulé.

## 1.0.8 — Métal combat ↑, énergie paysan/apothi

### Forgeron — matières (outils / armes / armures fer & bronze)
Deux passes **~+20 %** métal / maille à la main (pas de multiplicateur global).

| Famille | Exemple actuel |
|---------|----------------|
| Outils lourds | pioche / hache / fourche **6** |
| Outils légers | pelle / scie / burin / marteau **5** |
| Dague | **8** |
| Armes courtes | **17** |
| Armes longues / pavois | **22** |
| Flamberge | **31** |
| Plaque corps T3 | **49** |
| Maille corps | **115** |

### Énergie actions (hors craft)

| | Avant (1.0.6) | Après |
|--|---------------|------:|
| Labour / semer / récolte | 0,25 / 0,20 / 0,40 | **0,30 / 0,25 / 0,50** |
| Nourrir / abreuver | 0,60 | **0,75** |
| Traire / repro | 3,5 / 9,5 | **4,2 / 11,5** |
| Vase plant / eau / harvest | 0,35 / 0,25 / 0,60 | **0,50 / 0,40 / 0,90** |
| Meule | 1,0 | **1,4** |

---

## 1.0.6 — Énergie actions ~+20 % (manuel)

Constantes `JobActionEnergyCosts` + auto-coûts crafts recalés à la main (sans `scale()` / multiplicateur).

---

## 1.0.5 — Hang chunk farmland

`FarmlandMoistureManager.trackFromChunkScan` : plus de `Level#setBlock` pendant le load (évite deadlock watchdog 60 s).

---

## 1.0.4 — Shutdown + dura 300

Flag `shuttingDown`, preload classes ; outils JSON durabilité **300**.

---

## Passes matières antérieures (rappel)

| Passe | Contenu |
|-------|---------|
| Extract ÷2 | 10→5 clics, cooldown 1 s→0,5 s |
| Cultures | 48 h **total** maturité (plus par stade) |
| Four charbon | 24 h → **2 h** |
| Clous | 2 pépites → 8 clous |
| Artisan bois/pierre | planches 2+2, cobble 6+2, etc. |
| Outils « accessibles » puis recalibrage combat | voir `PATCHNOTE-ECONOMIE.md` |

Script structure/clous historique : `scripts/rebalance-economy.cjs`
