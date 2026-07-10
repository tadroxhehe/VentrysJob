import fs from "fs";
const path = new URL("./src/main/resources/data/ventrysjob/jobs.json", import.meta.url);
const data = JSON.parse(fs.readFileSync(path, "utf8"));
const job = data.jobs.find((j) => j.id === "apothicaire");
if (!job) throw new Error("apothicaire not found");

const ing = (itemId, count) => ({ itemId, count });
const out = (itemId, count) => ({ itemId, count });
const R = (id, name, desc, inputs, output) => ({ id, name, description: desc || "", inputs, output });

job.recipes = [
  R("apo_chandelier", "Chandelier", "", [ing("westerosblocks:candle", 8), ing("ventrysitem:res_planche_sapin", 24), ing("minecraft:iron_nugget", 12)], out("ventrys_blocs:chandelier", 1)),
  R("apo_candles_honey", "Bougies (miel)", "", [ing("minecraft:honeycomb", 1), ing("minecraft:stick", 2), ing("minecraft:coal", 1)], out("westerosblocks:candle", 2)),
  R("apo_candle_altar", "Bougie d'autel", "", [ing("minecraft:honeycomb", 3), ing("minecraft:stick", 6), ing("minecraft:coal", 2)], out("westerosblocks:candle_altar", 1)),
  R("apo_remede_maux", "Remede maux de tete", "", [ing("ventrysitem:res_feuille_de_lune", 2), ing("ventrysitem:res_menthe_des_collines", 2), ing("minecraft:potion", 1)], out("ventrysitem:item_remede_contre_les_maux_de_tete", 1)),
  R("apo_remede_anti_douleur", "Remede anti-douleur", "", [ing("ventrysitem:res_racine_dorage", 1), ing("ventrysitem:res_feuille_de_lune", 2), ing("minecraft:potion", 1)], out("ventrysitem:item_remede_anti_douleur", 1)),
  R("apo_baume_cicatrisant", "Baume cicatrisant", "", [ing("ventrysitem:res_soucis_des_mers", 1), ing("minecraft:honeycomb", 1), ing("ventrysitem:res_plantin", 1)], out("ventrysitem:item_baume_cicatrisant", 1)),
  R("apo_cataplasme_desinfectant", "Cataplasme desinfectant", "", [ing("ventrysitem:res_sauge_du_nord", 2), ing("ventrysitem:res_soucis_des_mers", 1), ing("minecraft:honeycomb", 1)], out("ventrysitem:item_cataplasme_desinfectant", 1)),
  R("apo_cataplasme_anti_brulure", "Cataplasme anti-brulure", "", [ing("ventrysitem:res_ronce_pourpre", 1), ing("ventrysitem:res_soucis_des_mers", 1), ing("minecraft:honeycomb", 1), ing("ventrysitem:res_fleur_de_lys", 1)], out("ventrysitem:item_cataplasme_anti_brulure", 1)),
  R("apo_bandage", "Bandage", "", [ing("ventrysitem:res_tissu", 2), ing("ventrysitem:res_sauge_du_nord", 1), ing("ventrysitem:res_plantin", 1)], out("ventrysitem:item_bandage", 1)),
  R("apo_tisane_calmante", "Tisane calmante", "", [ing("ventrysitem:res_menthe_des_collines", 2), ing("ventrysitem:res_fleur_de_lys", 1), ing("minecraft:potion", 1)], out("ventrysitem:item_tisane_calmante", 1)),
  R("apo_baume_antiseptique", "Baume antiseptique", "", [ing("ventrysitem:res_sauge_du_nord", 1), ing("ventrysitem:res_soucis_des_mers", 1), ing("ventrysitem:res_menthe_des_collines", 1), ing("minecraft:honeycomb", 1)], out("ventrysitem:item_baume_antiseptique", 1)),
  R("apo_remede_dhiver", "Remede d'hiver", "", [ing("ventrysitem:res_menthe_des_collines", 1), ing("ventrysitem:res_ronce_pourpre", 1), ing("ventrysitem:res_feuille_de_lune", 1), ing("minecraft:potion", 1)], out("ventrysitem:item_remede_dhiver", 1)),
  R("apo_infusion_fortifiante", "Infusion fortifiante", "", [ing("ventrysitem:res_racine_dorage", 1), ing("ventrysitem:res_plantin", 1), ing("ventrysitem:res_feuille_de_lune", 1), ing("minecraft:potion", 1)], out("ventrysitem:item_infusion_fortifiante", 1)),
  R("apo_infusion_adrenaline", "Infusion d'adrenaline", "", [ing("ventrysitem:res_racine_dorage", 2), ing("ventrysitem:res_verveine_noir", 1), ing("minecraft:potion", 1)], out("ventrysitem:item_infusion_dadrenaline", 1)),
  R("apo_infusion_reconstituante", "Infusion reconstituante", "", [ing("ventrysitem:res_plantin", 2), ing("ventrysitem:res_racine_dorage", 1), ing("minecraft:potion", 1)], out("ventrysitem:item_infusion_reconstituante", 1)),
  R("apo_the_de_lune", "The de lune", "", [ing("ventrysitem:res_ronce_pourpre", 1), ing("ventrysitem:res_feuille_de_lune", 2), ing("minecraft:potion", 1)], out("ventrysitem:item_the_de_lune", 1)),
  R("apo_infusion_somnifere", "Infusion somnifere", "", [ing("ventrysitem:res_verveine_noir", 1), ing("ventrysitem:res_menthe_des_collines", 1), ing("minecraft:potion", 1)], out("ventrysitem:item_infusion_somnifere", 1)),
  R("apo_infusion_lierre_mortel", "Infusion lierre mortel", "", [ing("ventrysitem:res_lierre_mortel", 2), ing("ventrysitem:res_verveine_noir", 1), ing("minecraft:potion", 1)], out("ventrysitem:item_infusion_lierre_mortel", 1)),
  R("apo_remede_anti_lierre", "Remede anti-lierre mortel", "", [ing("ventrysitem:res_lierre_mortel", 1), ing("ventrysitem:res_iris_cendre", 1), ing("ventrysitem:res_feuille_de_lune", 1), ing("minecraft:potion", 1)], out("ventrysitem:item_remede_anti_lierre_mortel", 1)),
  R("apo_savon_lys", "Savon de lys", "", [ing("ventrysitem:res_fleur_de_lys", 2), ing("minecraft:honeycomb", 1)], out("ventrysitem:item_savon_de_lys", 1)),
  R("apo_savon_milles_petales", "Savon aux milles petales", "", [ing("ventrysitem:res_fleur_de_lys", 1), ing("ventrysitem:res_ronce_pourpre", 2), ing("ventrysitem:res_iris_cendre", 2), ing("minecraft:honeycomb", 1)], out("ventrysitem:item_savon_aux_milles_petales", 1)),
  R("apo_savon_baiser_noir", "Savon du baiser noir", "", [ing("ventrysitem:res_lierre_mortel", 1), ing("ventrysitem:res_verveine_noir", 1), ing("ventrysitem:res_fleur_de_lys", 1), ing("minecraft:honeycomb", 1)], out("ventrysitem:item_savon_du_baiser_noir", 1)),
  R("apo_dye_brown", "Teinture marron (racine)", "", [ing("ventrysitem:res_racine_dorage", 1)], out("minecraft:brown_dye", 8)),
  R("apo_dye_white", "Teinture blanche (lys)", "", [ing("ventrysitem:res_fleur_de_lys", 1)], out("minecraft:white_dye", 8)),
  R("apo_dye_black", "Teinture noire (verveine)", "", [ing("ventrysitem:res_verveine_noir", 1)], out("minecraft:black_dye", 8)),
  R("apo_dye_yellow", "Teinture jaune (soucis)", "", [ing("ventrysitem:res_soucis_des_mers", 1)], out("minecraft:yellow_dye", 8)),
  R("apo_dye_red", "Teinture rouge (ronce)", "", [ing("ventrysitem:res_ronce_pourpre", 1)], out("minecraft:red_dye", 8)),
  R("apo_dye_blue", "Teinture bleue (iris)", "", [ing("ventrysitem:res_iris_cendre", 1)], out("minecraft:blue_dye", 8)),
];

fs.writeFileSync(path, JSON.stringify(data, null, 4) + "\n", "utf8");
console.log("apothicaire:", job.recipes.length, "recettes");
