/**
 * Divise par 2 les coûts des blocs constructibles artisan
 * (bois planche+clou, pierre) → minecraft / westerosblocks / ventrys_blocs.
 * Exclut stations, pantins, chariots, métiers.
 *
 * Usage : node scripts/halve-constructible-costs.cjs
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.join(__dirname, '..');
const JOBS = path.join(ROOT, 'src/main/resources/data/ventrysjob/jobs.json');

const PLANCHE = /^ventrysitem:res_planche_/;
const CLOU = 'ventrysitem:res_clou';
const PIERRE = 'ventrysitem:res_pierre_fragmente';

const ALLOWED_OUTPUT_NS = new Set(['minecraft', 'westerosblocks', 'ventrys_blocs']);

function half(n) {
  if (typeof n !== 'number' || n < 1) return n;
  return Math.max(1, Math.floor(n / 2));
}

function hasInput(recipe, pred) {
  return (recipe.inputs || []).some((i) =>
    typeof pred === 'string' ? i.itemId === pred : pred.test(i.itemId)
  );
}

function outputId(recipe) {
  const o = recipe.output || (recipe.outputs && recipe.outputs[0]);
  return o && o.itemId ? o.itemId : '';
}

function outputNs(recipe) {
  const id = outputId(recipe);
  const i = id.indexOf(':');
  return i >= 0 ? id.slice(0, i) : '';
}

function isConstructibleBlock(recipe) {
  if (!ALLOWED_OUTPUT_NS.has(outputNs(recipe))) {
    return false;
  }
  const wood = hasInput(recipe, PLANCHE) && hasInput(recipe, CLOU);
  const stone = hasInput(recipe, PIERRE);
  return wood || stone;
}

function halveRecipe(recipe) {
  if (!isConstructibleBlock(recipe)) {
    return false;
  }
  let changed = false;
  for (const input of recipe.inputs || []) {
    const next = half(input.count);
    if (next !== input.count) {
      input.count = next;
      changed = true;
    }
  }
  return changed;
}

function processRecipeList(recipes) {
  let n = 0;
  for (const r of recipes) {
    if (halveRecipe(r)) n++;
  }
  return n;
}

// --- jobs.json ---
const jobs = JSON.parse(fs.readFileSync(JOBS, 'utf8'));
const artisan = jobs.jobs.find((j) => j.id === 'artisan');
if (!artisan) {
  console.error('artisan introuvable');
  process.exit(1);
}
const nJobs = processRecipeList(artisan.recipes);
fs.writeFileSync(JOBS, JSON.stringify(jobs, null, 4) + '\n');
console.log(`jobs.json artisan : ${nJobs} recettes divisées /2`);

// --- artisan-part*.json ---
let partTotal = 0;
for (let i = 1; i <= 20; i++) {
  const fn = path.join(__dirname, `artisan-part${String(i).padStart(2, '0')}.json`);
  if (!fs.existsSync(fn)) continue;
  const part = JSON.parse(fs.readFileSync(fn, 'utf8'));
  if (!Array.isArray(part)) {
    console.warn('skip (pas un tableau):', fn);
    continue;
  }
  const n = processRecipeList(part);
  if (n > 0) {
    fs.writeFileSync(fn, JSON.stringify(part, null, 4) + '\n');
    console.log(`${path.basename(fn)} : ${n} recettes`);
    partTotal += n;
  }
}
console.log(`parts total : ${partTotal}`);

// sanity samples
const samples = ['art_oak_planks', 'art_oak_stairs', 'art_cobblestone', 'art_chaisechene'];
for (const id of samples) {
  const r = artisan.recipes.find((x) => x.id === id);
  if (!r) continue;
  console.log(
    id,
    '→',
    r.inputs.map((i) => `${i.itemId.split(':')[1]}x${i.count}`).join(' + ')
  );
}
