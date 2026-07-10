/**
 * Nettoie artisan-part05.json : retire les items-résultat utilisés comme ingrédients,
 * retire les prévisualisations timber (ventrys_blocs) pour sorties westerosblocks,
 * retire les blocs "flèche" intermédiaires incohérents avec la sortie,
 * supprime art_stone_button_1 (doublon non listé par le design),
 * remplace art_menuisier par l'établi d'artisan (ventrysjob:artisan_table).
 *
 * Usage : node scripts/sanitize-artisan-part05.mjs
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const fp = path.join(__dirname, 'artisan-part05.json');

let recipes = JSON.parse(fs.readFileSync(fp, 'utf8'));

recipes = recipes.filter((r) => r.id !== 'art_stone_button_1');

function cleanRecipe(r) {
  if (r.id === 'art_menuisier') {
    return {
      ...r,
      id: 'art_artisan_table',
      name: "Établi d'artisan",
      description: r.description || '',
      inputs: [
        { itemId: 'ventrysitem:res_planche_chene', count: 52 },
        { itemId: 'ventrysitem:res_clou', count: 104 },
        { itemId: 'minecraft:stick', count: 16 },
      ],
      output: { itemId: 'ventrysjob:artisan_table', count: 1 },
    };
  }

  const out = r.output.itemId;
  let inputs = [...(r.inputs || [])];

  inputs = inputs.filter((i) => i.itemId !== out);

  if (out.startsWith('westerosblocks:timber_oak')) {
    inputs = inputs.filter((i) => !i.itemId.startsWith('ventrys_blocs:timber_oak'));
  }

  inputs = inputs.filter((i) => {
    if (i.itemId.includes('granite_arrow') && i.itemId !== out) return false;
    return true;
  });

  // Faux namespace / item fantôme (jamais défini en jeu)
  inputs = inputs.filter((i) => !i.itemId.startsWith('astikocrafts:'));

  return { ...r, inputs };
}

recipes = recipes.map(cleanRecipe);

function inputSig(inputs) {
  return [...inputs]
    .map((i) => `${i.itemId}@${i.count}`)
    .sort()
    .join('|');
}

const dupMap = new Map();
for (const r of recipes) {
  const sig = inputSig(r.inputs);
  if (!sig) continue;
  if (!dupMap.has(sig)) dupMap.set(sig, []);
  dupMap.get(sig).push(r.id);
}

console.log('--- Recettes partageant exactement les mêmes ingrédients (même signature) ---');
let dupCount = 0;
for (const [sig, ids] of dupMap) {
  if (ids.length > 1) {
    dupCount++;
    console.log(ids.join(', '));
    console.log('  ', sig.slice(0, 200) + (sig.length > 200 ? '…' : ''));
  }
}
console.log('Total groupes en doublon (signature identique):', dupCount);

fs.writeFileSync(fp, JSON.stringify(recipes, null, 2) + '\n');
console.log('Écrit', recipes.length, 'recettes dans', path.basename(fp));
