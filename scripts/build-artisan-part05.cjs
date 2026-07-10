/**
 * Construit scripts/artisan-part05.json depuis artisan_part2 (fin) + artisan_part3,
 * avec correctifs (IDs mods, timber westerosblocks, chaumes, meubles pierre, astikorcarts).
 */
const fs = require('fs');
const path = require('path');

const part2Tail = fs.readFileSync(path.join(__dirname, 'artisan_part2.txt'), 'utf8').split(/\r?\n/);
// Lignes 250–296 : menuisier → tabouret coussin ; puis 334+ : sable + chaux (sans le bloc chaume erroné 298–332)
const part2Slice = [...part2Tail.slice(249, 296), ...part2Tail.slice(333)].join('\n');

const part3 = fs.readFileSync(path.join(__dirname, 'artisan_part3.txt'), 'utf8');

const RAW = part2Slice + '\n\n' + part3;

/** @type {{ itemId: string; count: number }[]} */
function parseInputs(joined) {
  const m = joined.match(/^recette\s*:\s*(.+)$/i);
  if (!m) return [];
  const rest = m[1];
  const out = [];
  const re = /([a-z0-9_]+:[a-z0-9_]+)\s*\(\s*x\s*(\d+)\s*\)/gi;
  let x;
  while ((x = re.exec(rest))) {
    out.push({ itemId: x[1], count: parseInt(x[2], 10) });
  }
  return out;
}

function parseOutput(joined) {
  const m = joined.match(/résultat\s*:\s*([a-z0-9_]+:[a-z0-9_]+)\s*\(\s*x\s*(\d+)\s*\)/i);
  if (!m) return null;
  return { itemId: m[1], count: parseInt(m[2], 10) };
}

function extractBlocks(text) {
  const lines = text.split(/\r?\n/);
  const blocks = [];
  let cur = [];
  for (const line of lines) {
    const t = line.trim();
    if (!t) {
      if (cur.length) {
        blocks.push(cur.join(' '));
        cur = [];
      }
      continue;
    }
    if (/^recette\s*:/i.test(t)) {
      if (cur.length) blocks.push(cur.join(' '));
      cur = [t];
    } else if (/^résultat\s*:/i.test(t)) {
      cur.push(t);
      blocks.push(cur.join(' '));
      cur = [];
    } else if (cur.length) cur.push(t);
  }
  if (cur.length) blocks.push(cur.join(' '));
  return blocks;
}

const OUTPUT_FIX = {
  'westerosblocks:grey_granite_arrow_window': 'westerosblocks:grey_granite_arrow_slit_window',
  'westerosblocks:grey_granite_arrow_ornate': 'westerosblocks:grey_granite_arrow_slit_ornate',
  'westerosblocks:green_grey_granite_arrow_window': 'westerosblocks:green_grey_granite_arrow_slit_window',
  'westerosblocks:green_grey_granite_arrow_ornate': 'westerosblocks:green_grey_granite_arrow_slit_ornate',
  'westerosblocks:winterfell_granite_arrow_window': 'westerosblocks:winterfell_granite_arrow_slit_window',
  'westerosblocks:winterfell_granite_arrow_ornate': 'westerosblocks:winterfell_granite_arrow_slit_ornate',
};

function fixOutput(id) {
  return OUTPUT_FIX[id] || id;
}

function slug(s) {
  return s
    .replace(/^[^:]+:/, '')
    .replace(/[^a-z0-9]+/gi, '_')
    .replace(/^_|_$/g, '')
    .slice(0, 48);
}

const stoneFurnitureOverride = [
  { id: 'art_banc_pierre_taille', name: 'Banc pierre taillee', inputs: [
    { itemId: 'ventrysitem:res_pierre_fragmente', count: 28 },
    { itemId: 'ventrysitem:res_enduit_de_chaux', count: 6 },
  ], output: { itemId: 'ventrys_blocs:bancpierretaille', count: 1 } },
  { id: 'art_banc_riche_pierre_taille', name: 'Banc riche pierre taillee', inputs: [
    { itemId: 'ventrysitem:res_pierre_fragmente', count: 58 },
    { itemId: 'ventrysitem:res_enduit_de_chaux', count: 12 },
  ], output: { itemId: 'ventrys_blocs:banc_riche_pierre_taille', count: 1 } },
  { id: 'art_banclong_pierretaille', name: 'Banc long pierre taillee', inputs: [
    { itemId: 'ventrysitem:res_pierre_fragmente', count: 42 },
    { itemId: 'ventrysitem:res_enduit_de_chaux', count: 10 },
  ], output: { itemId: 'ventrys_blocs:banclongpierretaille', count: 1 } },
  { id: 'art_banc_riche_long_pierre_taille', name: 'Banc riche long pierre taillee', inputs: [
    { itemId: 'ventrysitem:res_pierre_fragmente', count: 72 },
    { itemId: 'ventrysitem:res_enduit_de_chaux', count: 24 },
  ], output: { itemId: 'ventrys_blocs:banc_riche_long_pierre_taille', count: 1 } },
  { id: 'art_table_pierretaille', name: 'Table pierre taillee', inputs: [
    { itemId: 'ventrysitem:res_pierre_fragmente', count: 24 },
    { itemId: 'ventrysitem:res_enduit_de_chaux', count: 6 },
  ], output: { itemId: 'ventrys_blocs:tablepierretaille', count: 1 } },
  { id: 'art_table_ronde_pierretaille', name: 'Table ronde pierre taillee', inputs: [
    { itemId: 'ventrysitem:res_pierre_fragmente', count: 24 },
    { itemId: 'ventrysitem:res_enduit_de_chaux', count: 6 },
  ], output: { itemId: 'ventrys_blocs:table_ronde_pierretaille', count: 1 } },
  { id: 'art_chaise_pierre_taille', name: 'Chaise pierre taillee', inputs: [
    { itemId: 'ventrysitem:res_pierre_fragmente', count: 14 },
    { itemId: 'ventrysitem:res_enduit_de_chaux', count: 6 },
  ], output: { itemId: 'ventrys_blocs:chaise_pierre_taille', count: 1 } },
  { id: 'art_tabouret_pierretaille', name: 'Tabouret pierre taillee', inputs: [
    { itemId: 'ventrysitem:res_pierre_fragmente', count: 12 },
    { itemId: 'ventrysitem:res_enduit_de_chaux', count: 6 },
  ], output: { itemId: 'ventrys_blocs:tabouretpierretaille', count: 1 } },
  { id: 'art_chaisehaute_pierretaille', name: 'Chaise haute pierre taillee', inputs: [
    { itemId: 'ventrysitem:res_pierre_fragmente', count: 18 },
    { itemId: 'ventrysitem:res_enduit_de_chaux', count: 6 },
  ], output: { itemId: 'ventrys_blocs:chaisehautepierretaille', count: 1 } },
];

const ventrysjobExtras = [
  { id: 'art_meule_ble', name: 'Meule', description: '', inputs: [
    { itemId: 'ventrysitem:res_pierre_fragmente', count: 32 },
    { itemId: 'ventrysitem:res_planche_chene', count: 16 },
  ], output: { itemId: 'ventrysjob:meule', count: 1 } },
  { id: 'art_four_charbon_bois', name: 'Four a charbon de bois', description: '', inputs: [
    { itemId: 'ventrysitem:res_pierre_fragmente', count: 64 },
    { itemId: 'minecraft:charcoal', count: 16 },
  ], output: { itemId: 'ventrysjob:ouvrier_four', count: 1 } },
  { id: 'art_four_minerais', name: 'Four a minerais', description: '', inputs: [
    { itemId: 'ventrysitem:res_pierre_fragmente', count: 64 },
    { itemId: 'minecraft:coal', count: 16 },
  ], output: { itemId: 'ventrysjob:forgeron_four', count: 1 } },
  { id: 'art_vase_plantation', name: 'Vase de plantation', description: '', inputs: [
    { itemId: 'ventrysitem:res_argile_brun', count: 12 },
    { itemId: 'minecraft:dirt', count: 6 },
  ], output: { itemId: 'ventrysjob:vase_apothicaire', count: 1 } },
  { id: 'art_panier_poisson', name: 'Panier a poisson', description: '', inputs: [
    { itemId: 'ventrysitem:res_planche_chene', count: 24 },
    { itemId: 'minecraft:stick', count: 12 },
    { itemId: 'ventrysitem:res_fils', count: 36 },
  ], output: { itemId: 'ventrys_blocs:panier_poisson', count: 1 } },
  { id: 'art_abreuvoir', name: 'Abreuvoir', description: '', inputs: [
    { itemId: 'ventrysitem:res_clou', count: 40 },
    { itemId: 'ventrysitem:res_planche_chene', count: 70 },
    { itemId: 'minecraft:stick', count: 8 },
  ], output: { itemId: 'ventrys_blocs:abreuvoir', count: 1 } },
  { id: 'art_auge', name: 'Auge', description: '', inputs: [
    { itemId: 'ventrysitem:res_clou', count: 40 },
    { itemId: 'ventrysitem:res_planche_chene', count: 70 },
    { itemId: 'minecraft:stick', count: 8 },
  ], output: { itemId: 'ventrys_blocs:auge', count: 1 } },
];

const thatchRecipes = [
  { id: 'art_thatch_light_fur', name: 'Chaume fourrure claire', inputs: [{ itemId: 'minecraft:stick', count: 32 }], output: { itemId: 'westerosblocks:thatch_light_fur', count: 1 } },
  { id: 'art_thatch_light_stairs', name: 'Escalier chaume clair', inputs: [{ itemId: 'minecraft:stick', count: 24 }], output: { itemId: 'westerosblocks:thatch_light_fur_stairs', count: 1 } },
  { id: 'art_thatch_light_slab', name: 'Dalle chaume clair', inputs: [{ itemId: 'minecraft:stick', count: 16 }], output: { itemId: 'westerosblocks:thatch_light_fur_slab', count: 1 } },
  { id: 'art_thatch_light_tip', name: 'Pointe chaume clair', inputs: [{ itemId: 'minecraft:stick', count: 24 }], output: { itemId: 'westerosblocks:thatch_light_fur_tip', count: 1 } },
  { id: 'art_thatch_light_fence', name: 'Barriere chaume clair', inputs: [{ itemId: 'minecraft:stick', count: 12 }], output: { itemId: 'westerosblocks:thatch_light_fur_fence', count: 1 } },
  { id: 'art_thatch_light_carpet', name: 'Tapis chaume clair', inputs: [{ itemId: 'minecraft:stick', count: 9 }], output: { itemId: 'westerosblocks:thatch_light_fur_carpet', count: 1 } },
  { id: 'art_thatch_dark_fur', name: 'Chaume fourrure foncee', inputs: [{ itemId: 'minecraft:stick', count: 32 }], output: { itemId: 'westerosblocks:thatch_dark_fur', count: 1 } },
  { id: 'art_thatch_dark_stairs', name: 'Escalier chaume fonce', inputs: [{ itemId: 'minecraft:stick', count: 24 }], output: { itemId: 'westerosblocks:thatch_dark_fur_stairs', count: 1 } },
  { id: 'art_thatch_dark_slab', name: 'Dalle chaume fonce', inputs: [{ itemId: 'minecraft:stick', count: 16 }], output: { itemId: 'westerosblocks:thatch_dark_fur_slab', count: 1 } },
  { id: 'art_thatch_dark_tip', name: 'Pointe chaume fonce', inputs: [{ itemId: 'minecraft:stick', count: 24 }], output: { itemId: 'westerosblocks:thatch_dark_fur_tip', count: 1 } },
  { id: 'art_thatch_dark_fence', name: 'Barriere chaume fonce', inputs: [{ itemId: 'minecraft:stick', count: 12 }], output: { itemId: 'westerosblocks:thatch_dark_fur_fence', count: 1 } },
  { id: 'art_thatch_dark_carpet', name: 'Tapis chaume fonce', inputs: [{ itemId: 'minecraft:stick', count: 9 }], output: { itemId: 'westerosblocks:thatch_dark_fur_carpet', count: 1 } },
];

function inputKey(inputs) {
  return inputs.map((i) => `${i.itemId}:${i.count}`).sort().join('|');
}

const blocks = extractBlocks(RAW);
const seen = new Set();
const recipes = [];

/** Recettes meuble pierre du fichier source (IDs/quantites corriges via stoneFurnitureOverride) */
const STONE_FURN_SKIP = new Set([
  'ventrys_blocs:bancpierretaille',
  'ventrys_blocs:banc_riche_pierre_taillee',
  'ventrys_blocs:banclongpierretaillee',
  'ventrys_blocs:banc_riche_long_pierre_taillee',
  'ventrys_blocs:tablepierretaillee',
  'ventrys_blocs:table_ronde_pierretaillee',
  'ventrys_blocs:chaise_pierre_taillee',
  'ventrys_blocs:chaisehautepierretaillee',
]);

for (const block of blocks) {
  if (!/résultat\s*:/i.test(block)) continue;
  const inputs = parseInputs(block);
  let out = parseOutput(block);
  if (!out || inputs.length === 0) continue;

  out = { ...out, itemId: fixOutput(out.itemId) };

  if (out.itemId.startsWith('astikocrafts:')) {
    out = { ...out, itemId: out.itemId.replace('astikocrafts:', 'astikorcarts:') };
  }

  // Retirer recette pierre (x2) -> bouton pierre (doublon incoherent)
  if (inputs.length === 1 && inputs[0].itemId === 'ventrysitem:res_pierre_fragmente' && inputs[0].count === 2 && out.itemId === 'minecraft:stone_button') {
    continue;
  }

  if (STONE_FURN_SKIP.has(out.itemId)) continue;

  // Timber: forcer namespace westerosblocks (spec jeu)
  if (out.itemId.startsWith('ventrys_blocs:timber_oak')) {
    out = { ...out, itemId: out.itemId.replace('ventrys_blocs:', 'westerosblocks:') };
  }

  // Escaliers green_grey : corriger entree cobblestone_stairs -> green_grey_cobblestone_stairs
  for (const inp of inputs) {
    if (inp.itemId === 'westerosblocks:cobblestone_stairs' && out.itemId === 'westerosblocks:green_grey_granite_stairs') {
      inp.itemId = 'westerosblocks:green_grey_cobblestone_stairs';
    }
    if (inp.itemId === 'westerosblocks:cobblestone_stairs' && out.itemId === 'westerosblocks:small_dark_grey_brick_stairs') {
      inp.itemId = 'westerosblocks:dark_cobblestone_stairs';
    }
  }

  const ik = `${out.itemId}|${inputKey(inputs)}`;
  if (seen.has(ik)) continue;
  seen.add(ik);

  const name = out.itemId.replace(/^[^:]+:/, '').replace(/_/g, ' ');
  recipes.push({
    id: `art_tmp_${recipes.length}`,
    name: name.charAt(0).toUpperCase() + name.slice(1),
    description: '',
    inputs,
    output: out,
  });
}

for (const t of thatchRecipes) {
  recipes.push({ ...t, description: '' });
}

for (const s of stoneFurnitureOverride) {
  recipes.push({ ...s, description: '' });
}

for (const v of ventrysjobExtras) {
  recipes.push(v);
}

// Renommer IDs generiques en IDs stables par sortie
const byOut = new Map();
for (const r of recipes) {
  const k = r.output.itemId;
  if (!byOut.has(k)) byOut.set(k, []);
  byOut.get(k).push(r);
}

for (const [, list] of byOut) {
  list.forEach((r, i) => {
    const base = slug(r.output.itemId) || 'recipe';
    r.id = list.length === 1 ? `art_${base}` : `art_${base}_${i + 1}`;
  });
}

fs.writeFileSync(path.join(__dirname, 'artisan-part05.json'), JSON.stringify(recipes, null, 2) + '\n');
console.log('artisan-part05.json:', recipes.length, 'recettes');
