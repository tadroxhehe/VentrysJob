/**
 * Recettes tissage / cuir / Westeros pour le métier couturier (table metier_tisser).
 * Supprime le métier "tanneur" s'il existe (anciennes recettes déplacées ici).
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const jobsPath = path.join(__dirname, "src/main/resources/data/ventrysjob/jobs.json");
let raw = fs.readFileSync(jobsPath, "utf8");
if (raw.charCodeAt(0) === 0xfeff) raw = raw.slice(1);
const data = JSON.parse(raw);

data.jobs = data.jobs.filter((j) => j.id !== "tanneur");

const ing = (itemId, count) => ({ itemId, count });
const out = (itemId, count) => ({ itemId, count });
const R = (id, name, desc, inputs, output, outputs) => {
  const o = { id, name, description: desc || "", inputs };
  if (outputs) o.outputs = outputs;
  else o.output = output;
  return o;
};

const recipes = [
  R("cou_laine_to_wool", "Laine blanche vers laine Westeros", "", [ing("ventrysitem:res_laine_blanche", 4)], out("westerosblocks:white_wool", 1)),
  R("cou_fils_from_laine", "Fil", "", [ing("minecraft:stick", 1), ing("ventrysitem:res_laine_blanche", 1)], out("ventrysitem:res_fils", 3)),
  R("cou_tissu", "Tissu", "", [ing("ventrysitem:res_fils", 9)], out("ventrysitem:res_tissu", 1)),
  R("cou_corde", "Corde", "", [ing("ventrysitem:res_fils", 6)], out("ventrysitem:res_corde", 1)),
  R("cou_rope_horizontal", "Corde horizontale", "", [ing("ventrysitem:res_corde", 2)], out("westerosblocks:horizontal_rope", 1)),
  R("cou_rope_vertical", "Corde verticale", "", [ing("ventrysitem:res_corde", 2)], out("westerosblocks:vertical_rope", 1)),
  R("cou_hammock", "Hamac", "", [ing("ventrysitem:res_corde", 12)], out("westerosblocks:hammock", 1)),
  R(
    "cou_cuir_tanne",
    "Cuir tanne",
    "",
    [ing("minecraft:leather", 4), ing("minecraft:water_bucket", 1), ing("ventrysitem:res_sel", 1)],
    null,
    [out("ventrysitem:res_cuir_tanne", 4), out("minecraft:bucket", 1)]
  ),
  R("cou_gourde_cuir", "Gourde en cuir vide", "", [ing("ventrysitem:res_fils", 9), ing("ventrysitem:res_cuir_tanne", 3)], out("ventrysitem:item_gourde_en_cuir_vide", 1)),
  R("cou_banner", "Banniere laine blanche", "", [ing("minecraft:stick", 4), ing("minecraft:white_wool", 1)], out("westerosblocks:white_wool_banner", 1)),
  R("cou_carpet", "Tapis laine blanche", "", [ing("minecraft:white_wool", 1)], out("westerosblocks:white_wool_carpet", 4)),
  R("cou_slab_2", "Dalle laine (x2)", "", [ing("minecraft:white_wool", 1)], out("westerosblocks:white_wool_slab", 2)),
  R("cou_slab_4", "Dalles laine (x4)", "", [ing("minecraft:white_wool", 3)], out("westerosblocks:white_wool_slab", 4)),
  R(
    "cou_scelle",
    "Scelle cheval simple",
    "",
    [ing("ventrysitem:res_cuir_tanne", 40), ing("minecraft:iron_ingot", 6), ing("ventrysitem:res_fils", 18)],
    out("minecraft:saddle", 1)
  ),
  R("cou_paper", "Papier (planche chene)", "", [ing("ventrysitem:res_planche_chene", 1)], out("minecraft:paper", 1)),
  R(
    "cou_writable_book",
    "Livre vierge",
    "",
    [
      ing("minecraft:feather", 1),
      ing("minecraft:paper", 30),
      ing("minecraft:coal", 1),
      ing("minecraft:potion", 1),
      ing("ventrysitem:res_cuir_tanne", 8),
    ],
    out("minecraft:writable_book", 1)
  ),
  R("cou_lead", "Laisse", "", [ing("ventrysitem:res_fils", 18), ing("ventrysitem:res_cuir_tanne", 4)], out("minecraft:lead", 1)),
  R("cou_hdskin_haut", "Armure cuir (haut)", "", [ing("ventrysitem:res_cuir_tanne", 30), ing("ventrysitem:res_fils", 18)], out("hdskinmod:skin_cuirhaut1", 1)),
  R(
    "cou_hdskin_corp",
    "Armure cuir (corps)",
    "",
    [ing("ventrysitem:res_cuir_tanne", 70), ing("ventrysitem:res_clou", 48), ing("ventrysitem:res_fils", 24)],
    out("hdskinmod:skin_cuircorp1", 1)
  ),
  R("cou_hdskin_bas", "Armure cuir (bas)", "", [ing("ventrysitem:res_cuir_tanne", 40), ing("ventrysitem:res_fils", 18)], out("hdskinmod:skin_cuirbas1", 1)),
  R("cou_gambison_tete", "Gambison (tete)", "", [ing("ventrysitem:res_tissu", 30)], out("hdskinmod:skin_gambisontete", 1)),
  R("cou_gambison_corp", "Gambison (corps)", "", [ing("ventrysitem:res_tissu", 70)], out("hdskinmod:skin_gambisoncorp", 1)),
  R("cou_gambison_bas", "Gambison (bas)", "", [ing("ventrysitem:res_tissu", 40)], out("hdskinmod:skin_gambisonbas", 1)),
];

const couturier = data.jobs.find((j) => j.id === "couturier");
if (!couturier) throw new Error("couturier introuvable");
couturier.recipes = recipes;

fs.writeFileSync(jobsPath, JSON.stringify(data, null, 4) + "\n", "utf8");
console.log("couturier:", recipes.length, "recettes — tanneur supprime");
