/**
 * Génère artisan-part03.json (sapin) et artisan-part04.json (bouleau)
 * à partir de artisan-part02.json (chêne).
 */
const fs = require('fs');
const path = require('path');

const srcPath = path.join(__dirname, 'artisan-part02.json');
const src = JSON.parse(fs.readFileSync(srcPath, 'utf8'));

const spruceOut = {
  'ventrys_blocs:chaisechene': 'ventrys_blocs:chaise_sapin',
  'ventrys_blocs:chaisehautechene': 'ventrys_blocs:chaisehaute_sapin',
  'ventrys_blocs:tablechene': 'ventrys_blocs:tablesapin',
  'ventrys_blocs:table_ronde_chene': 'ventrys_blocs:table_ronde_sapin',
  'ventrys_blocs:bancchene': 'ventrys_blocs:bancsapin',
  'ventrys_blocs:banc_riche_chene': 'ventrys_blocs:banc_riche_sapin',
  'ventrys_blocs:banclongchene': 'ventrys_blocs:banclongsapin',
  'ventrys_blocs:banc_riche_long_chene': 'ventrys_blocs:banc_riche_long_sapin',
  'ventrys_blocs:caissechene': 'ventrys_blocs:caisse_sapin',
  'ventrys_blocs:armoirechene': 'ventrys_blocs:armoire_sapin',
  'ventrys_blocs:tabouretchene': 'ventrys_blocs:tabouretsapin',
  'ventrys_blocs:petitecomodechene': 'ventrys_blocs:petitecomodesapin',
  'ventrys_blocs:comodechene': 'ventrys_blocs:comodesapin',
  'ventrys_blocs:comodedoublechene': 'ventrys_blocs:comodedoublesapin',
};

const birchOut = {
  'ventrys_blocs:chaisechene': 'ventrys_blocs:chaise_bouleau',
  'ventrys_blocs:chaisehautechene': 'ventrys_blocs:chaisehautebouleau',
  'ventrys_blocs:tablechene': 'ventrys_blocs:tablebouleau',
  'ventrys_blocs:table_ronde_chene': 'ventrys_blocs:table_ronde_bouleau',
  'ventrys_blocs:bancchene': 'ventrys_blocs:bancbouleau',
  'ventrys_blocs:banc_riche_chene': 'ventrys_blocs:banc_riche_bouleau',
  'ventrys_blocs:banclongchene': 'ventrys_blocs:banclongbouleau',
  'ventrys_blocs:banc_riche_long_chene': 'ventrys_blocs:banc_riche_long_bouleau',
  'ventrys_blocs:caissechene': 'ventrys_blocs:caisse_bouleau',
  'ventrys_blocs:armoirechene': 'ventrys_blocs:armoirbouleau',
  'ventrys_blocs:tabouretchene': 'ventrys_blocs:tabouretbouleau',
  'ventrys_blocs:petitecomodechene': 'ventrys_blocs:petitecomodebouleau',
  'ventrys_blocs:comodechene': 'ventrys_blocs:comodebouleau',
  'ventrys_blocs:comodedoublechene': 'ventrys_blocs:comodedoublebouelau',
};

function mapItemId(id, wood) {
  let x = id;
  if (wood === 'spruce') {
    x = x
      .replace(/ventrysitem:res_planche_chene/g, 'ventrysitem:res_planche_sapin')
      .replace(/minecraft:oak_/g, 'minecraft:spruce_')
      .replace(/westerosblocks:reach_oak_/g, 'westerosblocks:reach_spruce_')
      .replace(/westerosblocks:oak_/g, 'westerosblocks:spruce_');
    return spruceOut[x] || x;
  }
  x = x
    .replace(/ventrysitem:res_planche_chene/g, 'ventrysitem:res_planche_bouleau')
    .replace(/minecraft:oak_/g, 'minecraft:birch_')
    .replace(/westerosblocks:reach_oak_/g, 'westerosblocks:reach_birch_')
    .replace(/westerosblocks:oak_/g, 'westerosblocks:birch_');
  return birchOut[x] || x;
}

function mapRecipeId(id, wood) {
  if (wood === 'spruce') {
    let s = id
      .replace(/^art_reach_oak_/, 'art_reach_spruce_')
      .replace(/^art_oak_/, 'art_spruce_')
      .replace(/chene/gi, 'sapin');
    if (s === id) return `${id}_spruce`;
    return s;
  }
  let s = id
    .replace(/^art_reach_oak_/, 'art_reach_birch_')
    .replace(/^art_oak_/, 'art_birch_')
    .replace(/chene/gi, 'bouleau');
  if (s === id) return `${id}_birch`;
  return s;
}

function mapName(name, wood) {
  if (wood === 'spruce') {
    return name
      .replace(/Chêne/g, 'Sapin')
      .replace(/chêne/gi, 'sapin')
      .replace(/Chenes/g, 'Sapins')
      .replace(/chene/gi, 'sapin');
  }
  return name
    .replace(/Chêne/g, 'Bouleau')
    .replace(/chêne/gi, 'bouleau')
    .replace(/Chenes/g, 'Bouleaux')
    .replace(/chene/gi, 'bouleau');
}

function cloneRecipes(wood) {
  return src.map((r) => ({
    ...r,
    id: mapRecipeId(r.id, wood),
    name: mapName(r.name, wood),
    inputs: r.inputs.map((i) => ({ ...i, itemId: mapItemId(i.itemId, wood) })),
    output: { ...r.output, itemId: mapItemId(r.output.itemId, wood) },
  }));
}

const p3 = cloneRecipes('spruce');
const p4 = cloneRecipes('birch');
fs.writeFileSync(path.join(__dirname, 'artisan-part03.json'), JSON.stringify(p3, null, 2) + '\n');
fs.writeFileSync(path.join(__dirname, 'artisan-part04.json'), JSON.stringify(p4, null, 2) + '\n');
console.log('OK artisan-part03.json', p3.length, 'recettes (sapin)');
console.log('OK artisan-part04.json', p4.length, 'recettes (bouleau)');
