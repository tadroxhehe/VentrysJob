/**
 * Rééquilibrage économie Forgeron / Artisan (plan validé).
 * Réécrit data/ventrysjob/jobs.json + synchronise artisan-part*.json / fragment.
 */
const fs = require("fs");
const path = require("path");

const ROOT = path.join(__dirname, "..");
const JOBS_PATH = path.join(ROOT, "src/main/resources/data/ventrysjob/jobs.json");

const WOOD_KEYS = ["planche", "planks"];
const NAIL_KEYS = ["clou"];
const STICK_KEYS = ["minecraft:stick"];
const METAL_KEYS = [
  "minecraft:iron_ingot",
  "ventrysitem:res_bronze_lingot",
  "ventrysitem:res_acier_lingot",
  "minecraft:gold_ingot",
  "minecraft:copper_ingot",
];

function scaleQty(count, divisor) {
  if (count == null || count <= 0) return count;
  let result;
  if (count < 4) {
    result = Math.ceil(count / divisor);
  } else if (count > 10) {
    result = Math.floor(count / divisor);
  } else {
    result = Math.round(count / divisor);
  }
  return Math.max(1, result);
}

function blob(r) {
  const out = r.output?.itemId || "";
  const outs = (r.outputs || []).map((o) => o.itemId).join(" ");
  return `${r.id || ""} ${r.name || ""} ${out} ${outs}`.toLowerCase();
}

/** @returns {'structure_volume'|'structure_variant'|'meuble_courant'|'meuble_riche'|'etabli'|'deco'|'pierre'|'autre'} */
function classifyArtisan(r) {
  const b = blob(r);
  const id = (r.id || "").toLowerCase();

  if (id === "art_artisan_table" || /etabli|table d'artisan|job.?table|meule|four/.test(b)) {
    return "etabli";
  }
  if (/comode.?double|armoire|banc_riche|banc riche|lit_noble|noble_.*bed/.test(b)) {
    return "meuble_riche";
  }
  if (
    /table|chaise|banc|coffre|etager|étager|shelf|fauteuil|tabouret|commode|buffet|dressoir|lit(?!_noble)/.test(
      b
    ) ||
    /ventrys_blocs:/.test(b)
  ) {
    // ventrys_blocs furniture often
    if (/comode|armoire|banc_riche/.test(id)) return "meuble_riche";
    if (
      /table|chaise|banc|coffre|etager|commode|armoire|lit|fauteuil|tabouret|buffet/.test(id) ||
      /ventrys_blocs:/.test(b)
    ) {
      if (/riche|double|noble/.test(b)) return "meuble_riche";
      return "meuble_courant";
    }
  }
  if (/carpet|tapis|candle|torch|banner|deco/.test(b) && !/planks|planche/.test(b)) {
    return "deco";
  }
  if (/carpet.*plank|tapis planche|vertical_planks_carpet/.test(b)) {
    return "deco";
  }
  if (
    /granite|cobble|stone|brick|calcaire|pierre|timber|frame|enduit|chaux|argile|sand/.test(b) &&
    !/planks|planche_chene|planche_sapin|planche_bouleau/.test(b)
  ) {
    return "pierre";
  }
  if (/_tip|_hopper|panelling_wall|_wall\b|muret/.test(id) || /\btip\b|\bhopper\b|muret/.test(b)) {
    if (/plank|planche|panelling|vertical/.test(b)) return "structure_variant";
  }
  if (
    /planks|planche|stairs|slab|fence|door|trapdoor|panelling|vertical_planks|oak_stairs|spruce_stairs|birch_stairs|thin_.*_log/.test(
      b
    )
  ) {
    return "structure_volume";
  }
  // carts / big wood props
  if (/cart|chariot|tonneau|barrel|wheel|roue/.test(b)) {
    return "meuble_riche";
  }
  return "autre";
}

const ARTISAN_DIVISOR = {
  structure_volume: 8,
  structure_variant: 6,
  meuble_courant: 4,
  meuble_riche: 3,
  etabli: 3,
  deco: 4,
  pierre: 2.5,
  autre: 3,
};

function isWoodInput(itemId) {
  const id = itemId.toLowerCase();
  return WOOD_KEYS.some((k) => id.includes(k)) || id.includes("res_planche");
}

function isNailInput(itemId) {
  return itemId.toLowerCase().includes("clou");
}

function isStickInput(itemId) {
  return STICK_KEYS.includes(itemId);
}

function scaleArtisanInputs(recipe, divisor) {
  if (!recipe.inputs) return;
  for (const inp of recipe.inputs) {
    if (!inp || !inp.itemId) continue;
    if (isWoodInput(inp.itemId) || isNailInput(inp.itemId) || isStickInput(inp.itemId)) {
      inp.count = scaleQty(inp.count, divisor);
    }
  }
}

/** Forgeron metal divisor by recipe tier */
function forgeronMetalDivisor(r) {
  const b = blob(r);
  const id = (r.id || "").toLowerCase();

  // Clous — handled separately
  if (id.includes("clou") || id.includes("nugget") || id.includes("pepite")) {
    return null;
  }
  // Job tables / etablis — ÷4 pour rester sous les armes courtes (16 Fe → 12)
  if (
    id === "enclume" ||
    /forgeron_table|apothicaire_table|cuisinier_table|etabli/.test(b) ||
    (id.includes("table") && /metier|job|forgeron|apothicaire|cuisinier/.test(b)) ||
    id === "marmite" ||
    id === "apothicaire_table" ||
    /table d'apothicaire|établi de forgeron|établi de cuisinier/.test(b)
  ) {
    return 4;
  }
  // Iron block décor
  if (id === "block_iron" || /iron_block|bloc de fer/.test(b)) {
    return 5;
  }
  // Armor HD
  if (/skin_plate|armure plaque|maille/.test(b) || id.startsWith("skin_")) {
    return 3;
  }
  // Heavy / signature weapons
  if (/flamberge|hallebarde|claymore|espadon|guisarme|barbiche|pavois/.test(b)) {
    return 3;
  }
  // Long weapons
  if (
    /epee_longue|épée longue|hache_combat_longue|hache_double_longue|masse|fleau|etoile|bec_de_corbin|bocle/.test(
      b
    )
  ) {
    return 3;
  }
  // Short weapons
  if (/epee_courte|epee_batarde|hache_combat|hache_double_courte|dagger|dague/.test(b)) {
    return 3;
  }
  // Extraction tools
  if (/pioche|hache_fer|hache_bronze|hache\b/.test(b) && !/combat|double|longue/.test(b)) {
    if (/pioche|hache_bronze|hache_fer|^hache$/.test(id) || /pioche|hache en/.test(b)) {
      return 2.5;
    }
  }
  if (/^pioche|^hache_bronze|^hache_fer/.test(id)) {
    return 2.5;
  }
  // Craft tools
  if (/burin|marteau|maillet|scie|pelle|fourche|marteau_/.test(b)) {
    return 2;
  }
  // Crossbow / misc metal
  if (/arbalete|crossbow|barreau|lanterne|brazier|clef|cadenat|trousseau|glass|bronze_lingot|clou/.test(b)) {
    return 2.5;
  }
  // Default metal crafts
  if ((r.inputs || []).some((i) => METAL_KEYS.includes(i.itemId))) {
    return 3;
  }
  return null;
}

function scaleForgeronInputs(recipe, divisor) {
  if (!recipe.inputs || divisor == null) return;
  for (const inp of recipe.inputs) {
    if (!inp || !inp.itemId) continue;
    const id = inp.itemId;
    if (
      METAL_KEYS.includes(id) ||
      id.includes("maille") ||
      id.includes("acier") ||
      id === "minecraft:iron_nugget" ||
      id.includes("bronze_pepite")
    ) {
      // Don't scale nuggets used for nail recipe (handled separately)
      inp.count = scaleQty(inp.count, divisor);
    }
    // Also scale wood/sticks on weapons lightly? Plan says metal hierarchy — scale planches on forgeron too at same divisor if present
    if (isWoodInput(id) || isStickInput(id) || id.includes("fils") || id.includes("corde")) {
      inp.count = scaleQty(inp.count, Math.min(divisor, 3));
    }
  }
}

function rebalanceNails(forgeron) {
  for (const r of forgeron.recipes || []) {
    if (r.id === "clou_from_iron" || r.id === "clou_from_bronze") {
      for (const inp of r.inputs || []) {
        if (inp.itemId.includes("pepite") || inp.itemId.includes("nugget")) {
          inp.count = 2;
        }
      }
      if (r.output) {
        r.output.itemId = "ventrysitem:res_clou";
        r.output.count = 8;
      }
    }
  }
}

function main() {
  const raw = fs.readFileSync(JOBS_PATH, "utf8");
  const data = JSON.parse(raw);
  const jobs = data.jobs || data;
  const stats = {
    artisanByCat: {},
    forgeronScaled: 0,
    nailsUpdated: 0,
  };

  const forgeron = jobs.find((j) => j.id === "forgeron");
  const artisan = jobs.find((j) => j.id === "artisan");
  if (!forgeron || !artisan) {
    throw new Error("forgeron/artisan missing");
  }

  // A. Clous
  const beforeClou = JSON.stringify(forgeron.recipes.find((r) => r.id === "clou_from_iron"));
  rebalanceNails(forgeron);
  const afterClou = forgeron.recipes.find((r) => r.id === "clou_from_iron");
  stats.nailsUpdated = afterClou?.output?.count === 8 ? 2 : 0;

  // B. Artisan
  for (const r of artisan.recipes || []) {
    const cat = classifyArtisan(r);
    stats.artisanByCat[cat] = (stats.artisanByCat[cat] || 0) + 1;
    const div = ARTISAN_DIVISOR[cat];
    scaleArtisanInputs(r, div);
  }

  // C. Forgeron
  for (const r of forgeron.recipes || []) {
    if (r.id === "clou_from_iron" || r.id === "clou_from_bronze") continue;
    const div = forgeronMetalDivisor(r);
    if (div != null) {
      scaleForgeronInputs(r, div);
      stats.forgeronScaled++;
    }
  }

  // Write jobs.json — preserve wrapper shape
  const out = Array.isArray(data) ? jobs : { ...data, jobs };
  fs.writeFileSync(JOBS_PATH, JSON.stringify(out, null, 4) + "\n", "utf8");

  // Sync artisan parts from live artisan recipes
  syncArtisanParts(artisan.recipes || []);

  // Verify key examples
  const oak = artisan.recipes.find((r) => r.id === "art_oak_planks");
  const enclume = forgeron.recipes.find((r) => r.id === "enclume");
  const epee = forgeron.recipes.find((r) => r.id === "epee_longue");
  const commode = artisan.recipes.find((r) => r.id === "art_comodedoublechene");

  console.log("=== REBALANCE DONE ===");
  console.log("nails recipe:", JSON.stringify(afterClou?.inputs), "->", JSON.stringify(afterClou?.output));
  console.log("was:", beforeClou);
  console.log("artisan cats:", stats.artisanByCat);
  console.log("forgeron scaled:", stats.forgeronScaled);
  console.log(
    "art_oak_planks:",
    oak?.inputs?.map((i) => i.itemId.split(":").pop() + "x" + i.count).join(", ")
  );
  console.log(
    "enclume Fe:",
    enclume?.inputs?.find((i) => i.itemId.includes("iron_ingot"))?.count
  );
  console.log(
    "epee_longue Fe:",
    epee?.inputs?.find((i) => i.itemId.includes("iron_ingot"))?.count
  );
  console.log(
    "commode:",
    commode?.inputs?.map((i) => i.itemId.split(":").pop() + "x" + i.count).join(", ")
  );
}

function syncArtisanParts(recipes) {
  const partsDir = path.join(ROOT, "scripts");
  const partFiles = [
    "artisan-part01.json",
    "artisan-part02.json",
    "artisan-part03.json",
    "artisan-part04.json",
    "artisan-part05.json",
  ];
  const byId = new Map(recipes.map((r) => [r.id, r]));

  for (const file of partFiles) {
    const p = path.join(partsDir, file);
    if (!fs.existsSync(p)) continue;
    let arr;
    try {
      arr = JSON.parse(fs.readFileSync(p, "utf8"));
    } catch {
      continue;
    }
    if (!Array.isArray(arr)) continue;
    let updated = 0;
    for (let i = 0; i < arr.length; i++) {
      const id = arr[i].id;
      if (byId.has(id)) {
        // Keep compact one-line style fields from live recipe
        const live = byId.get(id);
        arr[i] = {
          id: live.id,
          name: live.name,
          description: live.description || "",
          inputs: live.inputs,
          output: live.output,
          ...(live.outputs ? { outputs: live.outputs } : {}),
        };
        updated++;
      }
    }
    fs.writeFileSync(p, JSON.stringify(arr, null, 2) + "\n", "utf8");
    console.log("synced", file, updated, "/", arr.length);
  }

  // cuisinier fragment untouched; optional artisan fragment if exists
  const frag = path.join(ROOT, "cuisinier_recipes_fragment.json");
  // no artisan fragment at root typically
}

main();
