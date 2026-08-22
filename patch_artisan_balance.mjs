/**
 * Patch équilibrage artisan : pierre unifiée, colombage, bâtons, ruche domestique.
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const jobsPath = path.join(__dirname, "src/main/resources/data/ventrysjob/jobs.json");

const STANDARD_STONE_INPUTS = [
  { itemId: "ventrysitem:res_pierre_fragmente", count: 9 },
  { itemId: "ventrysitem:res_enduit_de_chaux", count: 1 },
];

const PLANCHE_IDS = new Set([
  "ventrysitem:res_planche_chene",
  "ventrysitem:res_planche_sapin",
  "ventrysitem:res_planche_bouleau",
]);

function isStoneConstructionRecipe(recipe) {
  const id = (recipe.id || "").toLowerCase();
  const out = (recipe.output?.itemId || "").toLowerCase();
  if (!id.startsWith("art_") || !out.startsWith("westerosblocks:")) {
    return false;
  }
  if (id.includes("enduit") || out.includes("enduit")) {
    return false;
  }
  const inputs = recipe.inputs || [];
  const hasCobbleInput = inputs.some((i) => (i.itemId || "").includes("cobblestone"));
  const hasPierreEnduit =
    inputs.some((i) => i.itemId === "ventrysitem:res_pierre_fragmente") &&
    inputs.some((i) => i.itemId === "ventrysitem:res_enduit_de_chaux");
  if (hasCobbleInput) {
    return true;
  }
  if (!hasPierreEnduit) {
    return false;
  }
  return (
    id.includes("cobblestone") ||
    id.includes("granite") ||
    id.includes("brick") ||
    id.includes("stone") ||
    id.includes("faith") ||
    id.includes("arrow") ||
    id.includes("engraved") ||
    id.includes("polished") ||
    id.includes("winterfell") ||
    id.includes("smooth") ||
    id.includes("green_grey") ||
    id.includes("dark_grey") ||
    id.includes("light_grey") ||
    id.includes("grey_")
  );
}

function stoneOutputCount(recipe) {
  const out = recipe.output?.itemId || "";
  if (out === "westerosblocks:cobblestone") {
    return 8;
  }
  return 4;
}

function isTimberColombage(recipe) {
  const id = (recipe.id || "").toLowerCase();
  const out = (recipe.output?.itemId || "").toLowerCase();
  return id.startsWith("art_timber_") || (id.includes("timber") && out.includes("timber"));
}

function patchTimber(recipe) {
  if (!isTimberColombage(recipe)) {
    return false;
  }
  let changed = false;
  for (const input of recipe.inputs || []) {
    if (PLANCHE_IDS.has(input.itemId) && input.count !== 2) {
      input.count = 2;
      changed = true;
    }
  }
  return changed;
}

function patchStone(recipe) {
  if (!isStoneConstructionRecipe(recipe)) {
    return false;
  }
  recipe.inputs = STANDARD_STONE_INPUTS.map((i) => ({ ...i }));
  const count = stoneOutputCount(recipe);
  if (recipe.output.count !== count) {
    recipe.output.count = count;
  }
  return true;
}

function patchBatons(recipe) {
  const id = recipe.id || "";
  if (!id.startsWith("art_batons")) {
    return false;
  }
  if (recipe.energyCost !== 2) {
    recipe.energyCost = 2;
    return true;
  }
  return false;
}

function ensureBeehive(artisan) {
  const id = "art_ruche_domestique";
  if (artisan.recipes.some((r) => r.id === id)) {
    return false;
  }
  artisan.recipes.push({
    id,
    name: "Ruche domestique",
    description: "Ruche en bois pour élever des abeilles",
    inputs: [
      { itemId: "minecraft:honeycomb", count: 5 },
      { itemId: "ventrysitem:res_planche_chene", count: 20 },
      { itemId: "ventrysitem:res_clou", count: 5 },
    ],
    output: {
      itemId: "minecraft:beehive",
      count: 1,
    },
    energyCost: 8,
  });
  return true;
}

const jobs = JSON.parse(fs.readFileSync(jobsPath, "utf8"));
const artisan = jobs.jobs.find((j) => j.id === "artisan");
if (!artisan) {
  throw new Error("Métier artisan introuvable");
}

let stone = 0;
let timber = 0;
let batons = 0;
for (const recipe of artisan.recipes) {
  if (patchStone(recipe)) stone++;
  if (patchTimber(recipe)) timber++;
  if (patchBatons(recipe)) batons++;
}
const beehive = ensureBeehive(artisan);

fs.writeFileSync(jobsPath, JSON.stringify(jobs, null, 4) + "\n", "utf8");
console.log(
  JSON.stringify({ stoneRecipes: stone, timberRecipes: timber, batonsRecipes: batons, beehiveAdded: beehive }, null, 2)
);
