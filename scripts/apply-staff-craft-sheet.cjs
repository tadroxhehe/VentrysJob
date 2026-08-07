/**
 * Fiche craft staff — artisan / cuisinier / couturier.
 * Usage : node scripts/apply-staff-craft-sheet.cjs
 */
const fs = require('fs');
const path = require('path');

const JOBS = path.join(__dirname, '../src/main/resources/data/ventrysjob/jobs.json');
const jobs = JSON.parse(fs.readFileSync(JOBS, 'utf8'));

function job(id) {
  const j = jobs.jobs.find((x) => x.id === id);
  if (!j) throw new Error('job missing: ' + id);
  return j;
}

function setInputs(recipe, inputs) {
  recipe.inputs = inputs;
}

function setOutput(recipe, itemId, count) {
  recipe.output = { itemId, count };
  delete recipe.outputs;
}

const art = job('artisan').recipes;
const cui = job('cuisinier').recipes;
const cou = job('couturier').recipes;
let n = 0;

// --- Enduit : 1 sable + 3 chaux = 4 ---
{
  const r = art.find((x) => x.id === 'art_res_enduit_de_chaux');
  setInputs(r, [
    { itemId: 'minecraft:red_sand', count: 1 },
    { itemId: 'ventrysitem:res_chaux', count: 3 },
  ]);
  setOutput(r, 'ventrysitem:res_enduit_de_chaux', 4);
  r.name = 'Enduit de chaux';
  n++;
}

// --- Timber oak colombages : planches /2, sable = 1 ---
for (const r of art) {
  if (!r.id.startsWith('art_timber_oak_')) continue;
  for (const input of r.inputs || []) {
    if (input.itemId && input.itemId.includes('res_planche_')) {
      input.count = Math.max(1, Math.floor(input.count / 2));
    }
    if (input.itemId === 'minecraft:red_sand' || input.itemId === 'minecraft:sand') {
      input.count = 1;
    }
  }
  n++;
}

// --- Slates (brown / orange / blue) ---
const SLATE_SHAPES = {
  '': { clay: 6, calc: 3, out: 1 },
  _slab: { clay: 3, calc: 1, out: 1 },
  _stairs: { clay: 4, calc: 2, out: 1 },
  _tip: { clay: 3, calc: 1, out: 1 },
  _hopper: { clay: 3, calc: 1, out: 1 },
  _fence: { clay: 3, calc: 1, out: 2 },
  _wall: { clay: 3, calc: 1, out: 1 },
};
const SLATE_COLORS = [
  { key: 'brown', clay: 'ventrysitem:res_argile_brun', block: 'brown_slate' },
  { key: 'orange', clay: 'ventrysitem:res_argile_orange', block: 'orange_slate' },
  { key: 'blue', clay: 'ventrysitem:res_argile_bleu', block: 'blue_slate' },
];
for (const col of SLATE_COLORS) {
  for (const [suffix, cfg] of Object.entries(SLATE_SHAPES)) {
    const id = `art_${col.key}_slate${suffix}`;
    const r = art.find((x) => x.id === id);
    if (!r) {
      console.warn('missing', id);
      continue;
    }
    setInputs(r, [
      { itemId: col.clay, count: cfg.clay },
      { itemId: 'ventrysitem:res_calcaire', count: cfg.calc },
    ]);
    setOutput(r, `westerosblocks:${col.block}${suffix}`, cfg.out);
    n++;
  }
}

// --- Cobbles de base : output x4 ---
for (const id of ['art_cobblestone', 'art_dark_cobblestone', 'art_green_grey_cobblestone']) {
  const r = art.find((x) => x.id === id);
  if (!r || !r.output) continue;
  r.output.count = 4;
  n++;
}

// --- Cuisinier ---
{
  const sucr = cui.find((x) => x.id === 'cuis_sucrerie');
  setInputs(sucr, [
    { itemId: 'ventrysitem:item_caramel', count: 1 },
    { itemId: 'minecraft:stick', count: 1 },
  ]);
  setOutput(sucr, 'ventrysitem:item_sucrerie', 1);
  n++;

  const pain = cui.find((x) => x.id === 'cuis_pain_raisins');
  setInputs(pain, [
    { itemId: 'ventrysitem:res_patte_a_pain', count: 1 },
    { itemId: 'ventrysitem:item_raisin', count: 1 },
  ]);
  setOutput(pain, 'ventrysitem:item_pain_aux_raisins', 1);
  n++;
}

// --- Couturier : peaux → cuir ---
const SKIN_RECIPES = [
  { id: 'tan_peau_loup_cuir', name: 'Cuir (peau de loup)', skin: 'ventrysitem:res_peau_de_loup', count: 10 },
  { id: 'tan_peau_ours_cuir', name: 'Cuir (peau d\'ours)', skin: 'ventrysitem:res_peau_dours', count: 20 },
  { id: 'tan_peau_lion_cuir', name: 'Cuir (peau de lion)', skin: 'ventrysitem:res_peau_lion', count: 15 },
  { id: 'tan_peau_lionne_cuir', name: 'Cuir (peau de lionne)', skin: 'ventrysitem:res_peau_lionne', count: 15 },
  { id: 'tan_peau_lion_montagne_cuir', name: 'Cuir (peau de lion des montagnes)', skin: 'ventrysitem:res_peau_lion_des_montagne', count: 15 },
];
for (const spec of SKIN_RECIPES) {
  const existing = cou.find((x) => x.id === spec.id);
  const recipe = existing || { id: spec.id };
  recipe.name = spec.name;
  recipe.description = 'Transforme une peau sauvage en cuir utilisable';
  setInputs(recipe, [{ itemId: spec.skin, count: 1 }]);
  setOutput(recipe, 'minecraft:leather', spec.count);
  if (!existing) cou.push(recipe);
  n++;
}

fs.writeFileSync(JOBS, JSON.stringify(jobs, null, 4) + '\n');
console.log('jobs.json mis à jour —', n, 'recettes touchées');

// Sync artisan-part*.json si présents (même règles)
for (let i = 1; i <= 20; i++) {
  const fn = path.join(__dirname, `artisan-part${String(i).padStart(2, '0')}.json`);
  if (!fs.existsSync(fn)) continue;
  const part = JSON.parse(fs.readFileSync(fn, 'utf8'));
  if (!Array.isArray(part)) continue;
  let changed = false;
  for (const r of part) {
    const master = art.find((x) => x.id === r.id);
    if (!master) continue;
    r.inputs = JSON.parse(JSON.stringify(master.inputs));
    if (master.output) r.output = JSON.parse(JSON.stringify(master.output));
    if (master.outputs) r.outputs = JSON.parse(JSON.stringify(master.outputs));
    changed = true;
  }
  if (changed) {
    fs.writeFileSync(fn, JSON.stringify(part, null, 4) + '\n');
    console.log('sync', path.basename(fn));
  }
}
