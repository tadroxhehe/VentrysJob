/**
 * Parse scripts/artisan_recipes_raw.txt (format "recette :" / "résultat :")
 * et fusionne dans jobs.json → métier "artisan" (conserve bowl_wood + nouvelles recettes).
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const scriptsDir = path.join(__dirname, "scripts");
const rawParts = ["artisan_part1.txt", "artisan_part1b.txt", "artisan_part2.txt", "artisan_part3.txt"];
const jobsPath = path.join(__dirname, "src/main/resources/data/ventrysjob/jobs.json");

function loadRawText() {
  const chunks = rawParts.map((name) => {
    const p = path.join(scriptsDir, name);
    if (!fs.existsSync(p)) throw new Error("Fichier manquant: " + p);
    let t = fs.readFileSync(p, "utf8");
    if (t.charCodeAt(0) === 0xfeff) t = t.slice(1);
    return t;
  });
  return chunks.join("\n");
}

function fixTypoItemId(id) {
  let s = id.trim().replace(/\s+/g, " ");
  // Typo connue dans le texte utilisateur
  if (s === "westerosblocksgreen_grey_cobblestone_stairs") {
    return "westerosblocks:green_grey_cobblestone_stairs";
  }
  // "westerosblocks:cobblestone_fence( x1 )" → id sans parenthèses collées
  const m = s.match(/^(.+?)\s*\(\s*x\s*(\d+)\s*\)\s*$/i);
  if (m) return m[1].trim();
  return s.replace(/\s*\(\s*x\s*\d+\s*\)\s*$/i, "").trim();
}

function parseIngredientPart(part) {
  const p = part.trim();
  const m = p.match(/^(.+?)\s*\(\s*x\s*(\d+)\s*\)\s*$/i);
  if (!m) return null;
  return { itemId: fixTypoItemId(m[1]), count: parseInt(m[2], 10) };
}

function parseRecipeLine(line) {
  const parts = line.split(/\s*\+\s*/);
  const inputs = [];
  for (const part of parts) {
    const ing = parseIngredientPart(part);
    if (ing) inputs.push(ing);
  }
  return inputs;
}

function parseOutputLine(line) {
  const m = line.trim().match(/^(.+?)\s*\(\s*x\s*(\d+)\s*\)\s*$/i);
  if (m) {
    return { itemId: fixTypoItemId(m[1]), count: parseInt(m[2], 10) };
  }
  // Ligne tronquée ex. "westerosblocks:table"
  return { itemId: fixTypoItemId(line), count: 1 };
}

function parseRawText(text) {
  const lines = text.replace(/\r\n/g, "\n").split("\n");
  const blocks = [];
  let i = 0;
  while (i < lines.length) {
    const L = lines[i];
    if (/^recette\s*:/i.test(L.trim())) {
      let recipeLine = L.replace(/^recette\s*:\s*/i, "").trim();
      i++;
      while (i < lines.length && !/^résultat\s*:/i.test(lines[i].trim())) {
        const cont = lines[i].trim();
        if (cont) recipeLine += " " + cont;
        i++;
      }
      if (i >= lines.length) break;
      const R = lines[i].replace(/^résultat\s*:\s*/i, "").trim();
      i++;
      const inputs = parseRecipeLine(recipeLine);
      const output = parseOutputLine(R);
      blocks.push({ inputs, output, rawIn: recipeLine, rawOut: R });
    } else {
      i++;
    }
  }
  return blocks;
}

function slug(s, max = 40) {
  return s
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-zA-Z0-9_]+/g, "_")
    .replace(/_+/g, "_")
    .replace(/^_|_$/g, "")
    .toLowerCase()
    .slice(0, max);
}

function main() {
  const raw = loadRawText();
  const linesResult = (raw.match(/résultat\s*:/gi) || []).length;
  console.log("Dans les fichiers source (lignes résultat):", linesResult);

  const blocks = parseRawText(raw);
  console.log("Blocs recette/résultat parsés:", blocks.length);

  const recipes = [];
  const seenIds = new Set(["bowl_wood"]);
  let idx = 0;
  for (const b of blocks) {
    idx++;
    if (!b.inputs.length) {
      console.warn("Recette", idx, "sans ingrédients parsés — ignorée:", b.rawIn?.slice(0, 80));
      continue;
    }
    if (b.inputs.length > 9) {
      console.warn("Recette", idx, "a", b.inputs.length, "ingrédients (>9) — ignorée:", b.rawIn?.slice(0, 100));
      continue;
    }
    if (!b.output.itemId) {
      console.warn("Recette", idx, "sortie vide — ignorée");
      continue;
    }

    let id = `art_${slug(b.output.itemId)}_${b.output.count}_${idx}`;
    if (seenIds.has(id)) id = `${id}_${idx}`;
    seenIds.add(id);

    const name = b.output.itemId.replace(/^[^:]+:/, "").replace(/_/g, " ");
    recipes.push({
      id,
      name: name.slice(0, 80) || id,
      description: "",
      inputs: b.inputs.map((x) => ({ itemId: x.itemId, count: x.count })),
      output: { itemId: b.output.itemId, count: b.output.count },
    });
  }

  let jobsRaw = fs.readFileSync(jobsPath, "utf8");
  if (jobsRaw.charCodeAt(0) === 0xfeff) jobsRaw = jobsRaw.slice(1);
  const data = JSON.parse(jobsRaw);
  const artisan = data.jobs.find((j) => j.id === "artisan");
  if (!artisan) throw new Error("métier artisan introuvable");

  const keepBowl = (artisan.recipes || []).filter((r) => r.id === "bowl_wood");
  artisan.recipes = [...keepBowl, ...recipes];

  fs.writeFileSync(jobsPath, JSON.stringify(data, null, 4) + "\n", "utf8");
  console.log("artisan: bowl_wood +", recipes.length, "recettes → total", artisan.recipes.length);
}

main();
