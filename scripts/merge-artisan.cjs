/**
 * Fusionne scripts/artisan-partXX.json dans jobs.json (métier artisan).
 * Usage : node scripts/merge-artisan.cjs
 */
const fs = require('fs');
const path = require('path');

const jobsPath = path.join(__dirname, '../src/main/resources/data/ventrysjob/jobs.json');
const jobs = JSON.parse(fs.readFileSync(jobsPath, 'utf8'));
const artisan = jobs.jobs.find((j) => j.id === 'artisan');
if (!artisan) {
  console.error('Métier artisan introuvable');
  process.exit(1);
}

const bowl = {
  id: 'bowl_wood',
  name: 'Bol en Bois',
  description: 'Un bol simple en bois',
  inputs: [{ itemId: 'minecraft:oak_planks', count: 3 }],
  output: { itemId: 'ventrysitem:res_bol', count: 4 },
};

let extra = [];
for (let i = 1; i <= 20; i++) {
  const fn = path.join(__dirname, `artisan-part${String(i).padStart(2, '0')}.json`);
  if (fs.existsSync(fn)) {
    const part = JSON.parse(fs.readFileSync(fn, 'utf8'));
    if (!Array.isArray(part)) throw new Error(fn + ' doit être un tableau JSON');
    extra = extra.concat(part);
  }
}

artisan.recipes = [bowl, ...extra];
fs.writeFileSync(jobsPath, JSON.stringify(jobs, null, 4) + '\n');
console.log('Recettes artisan :', artisan.recipes.length, '(dont bowl_wood +', extra.length, 'ajouts)');
