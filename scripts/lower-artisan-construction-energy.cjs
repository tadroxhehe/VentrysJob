/**
 * Fixe des energyCost bas sur les crafts artisan de construction volume
 * (stairs, slabs, planks, cobble, stone, fences…) pour les grosses commandes.
 *
 * Usage : node scripts/lower-artisan-construction-energy.cjs
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.join(__dirname, '..');
const JOBS = path.join(ROOT, 'src/main/resources/data/ventrysjob/jobs.json');

const FURNITURE_RE =
  /table|chaise|banc|armoire|coffre|nid|statue|commode|fauteuil|tabouret|buffet|chariot|tonneau|cart|lit|etager|shelf|dressoir/;

function outputOf(r) {
  const o = r.output || (r.outputs && r.outputs[0]);
  return o && o.itemId ? String(o.itemId).toLowerCase() : '';
}

function outCount(r) {
  if (r.output && r.output.count) return r.output.count;
  if (r.outputs && r.outputs[0]) return r.outputs[0].count || 1;
  return 1;
}

function isFurniture(r) {
  const id = String(r.id || '').toLowerCase();
  const out = outputOf(r);
  return FURNITURE_RE.test(id) || FURNITURE_RE.test(out);
}

function isWoodConstruction(r) {
  if (isFurniture(r)) return false;
  const out = outputOf(r);
  if (
    /plank|timber|thin_|wood_ladder|rope_ladder|panelling|wattle|thatch/.test(out)
  ) {
    return true;
  }
  const wood = /oak|spruce|birch|jungle|acacia|dark_oak|reach_oak|reach_spruce/.test(out);
  const shape =
    /stairs|slab|wall|fence|tip|hopper|log|door|trapdoor|pressure_plate|button|sign|fence_gate/.test(
      out
    );
  return wood && shape;
}

function isStoneConstruction(r) {
  if (isFurniture(r)) return false;
  const out = outputOf(r);
  return /cobble|stone|granite|slate|brick|sandstone|basalt|marble|limestone|andesite|diorite|calcaire|enduit|chaux|mud|daub|plaster|frame/.test(
    out
  );
}

function energyFor(r) {
  const n = outCount(r);
  if (isWoodConstruction(r)) {
    return n >= 4 ? 0.4 : 0.25;
  }
  if (isStoneConstruction(r)) {
    return n >= 4 ? 0.4 : 0.2;
  }
  return null;
}

function applyList(recipes) {
  let n = 0;
  for (const r of recipes) {
    const e = energyFor(r);
    if (e == null) continue;
    if (r.energyCost !== e) {
      r.energyCost = e;
      n++;
    }
  }
  return n;
}

const jobs = JSON.parse(fs.readFileSync(JOBS, 'utf8'));
const artisan = jobs.jobs.find((j) => j.id === 'artisan');
if (!artisan) {
  console.error('artisan introuvable');
  process.exit(1);
}
const changed = applyList(artisan.recipes);
fs.writeFileSync(JOBS, JSON.stringify(jobs, null, 4) + '\n');
console.log(`jobs.json : ${changed} recettes construction avec energyCost bas`);

const samples = ['art_oak_stairs', 'art_oak_slab', 'art_cobblestone', 'art_oak_planks', 'art_oak_wall'];
for (const id of samples) {
  const r = artisan.recipes.find((x) => x.id === id);
  if (!r) continue;
  console.log(`${id} → energyCost=${r.energyCost} out=${outputOf(r)} x${outCount(r)}`);
}

// Sync parts si présents
for (let i = 1; i <= 20; i++) {
  const fn = path.join(__dirname, `artisan-part${String(i).padStart(2, '0')}.json`);
  if (!fs.existsSync(fn)) continue;
  const part = JSON.parse(fs.readFileSync(fn, 'utf8'));
  if (!Array.isArray(part)) continue;
  const n = applyList(part);
  if (n > 0) {
    fs.writeFileSync(fn, JSON.stringify(part, null, 4) + '\n');
    console.log(`${path.basename(fn)} : ${n}`);
  }
}
