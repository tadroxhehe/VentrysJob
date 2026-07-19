import fs from "fs";
const ing = (itemId, count) => ({ itemId, count });
const out = (itemId, count) => ({ itemId, count });
function rec(id, name, description, inputs, output, outputs) {
  const d = { id, name, description, inputs };
  if (outputs != null) d.outputs = outputs;
  else d.output = output;
  return d;
}
const R = [];
const simple = [
  ["cuis_carotte_cuite", "Carotte cuite", "", "ventrysitem:item_carotte", "ventrysitem:item_carotte_cuite"],
  ["cuis_bacon", "Bacon cuit", "", "ventrysitem:item_lard_porc", "ventrysitem:item_bacon_cuit"],
  ["cuis_poulet_cuit", "Filet de poulet cuit", "", "ventrysitem:item_fillet_de_poulet_crue", "ventrysitem:item_fillet_de_poulet_cuit"],
  ["cuis_bouguignon_cuit", "Boeuf bourguignon cuit", "", "ventrysitem:item_bouguignon_boeuf", "ventrysitem:item_bouguignon_boeuf_cuit"],
  ["cuis_choux_cuit", "Choux cuit", "", "ventrysitem:item_choux", "ventrysitem:item_choux_cuit"],
  ["cuis_cotelette_cuite", "Cotelette de porc cuite", "", "ventrysitem:item_cotelette_de_porc", "ventrysitem:item_cotelette_de_porc_cuit"],
  ["cuis_crabe_cuit", "Crabe cuit", "", "ventrysitem:item_crabe", "ventrysitem:item_crabe_cuit"],
  ["cuis_dorade_cuite", "Dorade cuite", "", "ventrysitem:item_dorade", "ventrysitem:item_dorade_cuite"],
  ["cuis_epaule_sanglier", "Epaule de sanglier cuite", "", "ventrysitem:item_epaule_sanglier", "ventrysitem:item_epaule_sanglier_cuite"],
  ["cuis_faux_filet_cuit", "Faux-filet cuit", "", "ventrysitem:item_faux_fillet_boeuf", "ventrysitem:item_faux_fillet_boeuf_cuit"],
  ["cuis_cerf_cuit", "Cerf cuit", "", "ventrysitem:item_fillet_mignon_cerf", "ventrysitem:item_cerf_cuit"],
  ["cuis_gigot_lievre", "Gigot de lievre cuit", "", "ventrysitem:item_gigot_de_lievre_crue", "ventrysitem:item_gigot_de_lievre_cuit"],
  ["cuis_gigot_agneau", "Gigot d'agneau cuit", "", "ventrysitem:item_gigot_dagneau_cru", "ventrysitem:item_gigaud_dagneau_cuit"],
  ["cuis_homar_cuit", "Homar cuit", "", "ventrysitem:item_homar", "ventrysitem:item_homar_cuit"],
  ["cuis_oeuf_cuit", "Oeuf cuit", "", "ventrysitem:res_oeuf", "ventrysitem:item_oeuf_cuit"],
  ["cuis_oignon_cuit", "Oignon cuit", "", "ventrysitem:item_oignon", "ventrysitem:item_oignon_cuit"],
  ["cuis_patate_cuite", "Patate cuite", "", "ventrysitem:item_patate", "ventrysitem:item_patate_cuite"],
  ["cuis_oblade_cuite", "Oblade cuite", "", "ventrysitem:item_oblade", "ventrysitem:item_oblade_cuite"],
  ["cuis_saumon_cuit", "Saumon cuit", "", "ventrysitem:item_saumon", "ventrysitem:item_saumon_cuit"],
  ["cuis_tomate_cuite", "Tomate cuite", "", "ventrysitem:item_tomate", "ventrysitem:item_tomate_cuite"],
];
for (const [rid, name, desc, a, b] of simple) {
  R.push(rec(rid, name, desc, [ing(a, 1)], out(b, 1)));
}
/* Seau → métier artisan (art_seau) dans jobs.json */
R.push(rec("cuis_pain_patte", "Pain", "", [ing("ventrysitem:res_patte_a_pain", 1)], out("ventrysitem:item_pain", 1)));
R.push(rec("cuis_sucre_betrave", "Sucre (betterave)", "", [ing("ventrysitem:item_betrave", 1)], out("ventrysitem:item_sucre", 1)));
R.push(rec("cuis_patte_pain_complet", "Pate a pain", "", [ing("ventrysitem:res_farine", 3), ing("ventrysitem:item_gourde_deau", 1), ing("ventrysitem:res_sel", 1)], null, [out("ventrysitem:res_patte_a_pain", 1), out("ventrysitem:item_gourde_en_cuir_vide", 1)]));
R.push(rec("cuis_bol_huile", "Bol d'huile", "", [ing("ventrysitem:res_tournesol", 1), ing("ventrysitem:res_bol", 1)], out("ventrysitem:res_bol_dhuile", 1)));
R.push(rec("cuis_chope_biere", "Chope de biere", "", [ing("ventrysitem:res_malt_concasse", 3), ing("ventrysitem:res_chope_vide", 1)], out("ventrysitem:item_chope_biere", 1)));
R.push(rec("cuis_chope_whisky", "Chope de whisky", "", [ing("ventrysitem:res_malt_concasse", 9), ing("ventrysitem:res_chope_vide", 1)], out("ventrysitem:item_chope_wisky", 1)));
R.push(rec("cuis_fromage_seau", "Fromage de Scarnfell", "", [ing("minecraft:milk_bucket", 1)], null, [out("ventrysitem:item_fromage_de_scarnfell", 1), out("minecraft:bucket", 1)]));
R.push(rec("cuis_gateaux_secs", "Gateaux secs", "", [ing("ventrysitem:res_oeuf", 1), ing("ventrysitem:res_farine", 6), ing("ventrysitem:res_beurre", 1), ing("ventrysitem:item_sucre", 2)], out("ventrysitem:item_gateaux_secs", 6)));
R.push(rec("cuis_gateaux_miel", "Gateaux moelleux au miel", "", [ing("ventrysitem:res_oeuf", 1), ing("ventrysitem:res_farine", 6), ing("ventrysitem:res_beurre", 1), ing("ventrysitem:item_sucre", 1), ing("ventrysitem:item_miel", 1)], out("ventrysitem:item_gateaux_moelleux_au_miel", 6)));
R.push(rec("cuis_pain_raisins", "Pain aux raisins", "", [ing("ventrysitem:item_pain", 1), ing("ventrysitem:item_raisin", 3)], out("ventrysitem:item_pain_aux_raisins", 1)));
R.push(rec("cuis_gateaux_fraises", "Gateaux aux fraises des bois", "", [ing("ventrysitem:res_oeuf", 2), ing("ventrysitem:res_farine", 10), ing("ventrysitem:res_beurre", 1), ing("ventrysitem:item_sucre", 3), ing("ventrysitem:item_fraise_sauvage", 3)], out("ventrysitem:item_gateaux_aux_fraises_des_bois", 1)));
R.push(rec("cuis_vin_inferieur", "Vin inferieur", "", [ing("ventrysitem:item_raisin", 16), ing("ventrysitem:res_bouteille_de_vin_vide", 1)], out("ventrysitem:item_bouteille_de_vin_inferieur", 1)));
R.push(rec("cuis_vin_superieur", "Vin superieur", "", [ing("ventrysitem:item_raisin", 32), ing("ventrysitem:res_bouteille_de_vin_vide", 1)], out("ventrysitem:item_bouteille_de_vin_superieur", 1)));
R.push(rec("cuis_vin_parfaite", "Vin parfait", "", [ing("ventrysitem:item_raisin", 64), ing("ventrysitem:res_bouteille_de_vin_vide", 1)], out("ventrysitem:item_bouteille_de_vin_parfaite", 1)));
R.push(rec("cuis_caramel", "Caramel", "", [ing("ventrysitem:item_sucre", 3)], out("ventrysitem:item_caramel", 1)));
R.push(rec("cuis_sucrerie", "Sucrerie", "", [ing("ventrysitem:item_caramel", 3), ing("minecraft:stick", 3)], out("ventrysitem:item_sucrerie", 1)));
R.push(rec("cuis_salade_fruit", "Bol de salade de fruits", "", [ing("ventrysitem:item_sucre", 2), ing("ventrysitem:item_abricot", 2), ing("ventrysitem:res_bol", 1), ing("ventrysitem:item_pomme", 2)], out("ventrysitem:item_bol_de_salade_de_fruit", 1)));
R.push(rec("cuis_soupe_betrave", "Soupe de betterave", "", [ing("ventrysitem:res_bol_deau", 1), ing("ventrysitem:item_betrave", 3)], out("ventrysitem:item_bol_de_soupe_de_betrave", 1)));
R.push(rec("cuis_soupe_legume", "Soupe de legumes", "", [ing("ventrysitem:res_bol_deau", 1), ing("ventrysitem:item_choux", 1), ing("ventrysitem:item_carotte", 1), ing("ventrysitem:item_oignon", 1)], out("ventrysitem:item_bol_de_soupe_de_legume", 1)));
R.push(rec("cuis_soupe_poisson_dorade", "Soupe de poisson", "", [ing("ventrysitem:res_bol_deau", 1), ing("ventrysitem:item_dorade", 1)], out("ventrysitem:item_bol_de_soupe_de_poisson", 1)));
R.push(rec("cuis_soupe_poisson_oblade", "Soupe de poisson", "", [ing("ventrysitem:res_bol_deau", 1), ing("ventrysitem:item_oblade", 1)], out("ventrysitem:item_bol_de_soupe_de_poisson", 1)));
R.push(rec("cuis_soupe_poisson_saumon", "Soupe de poisson", "", [ing("ventrysitem:res_bol_deau", 1), ing("ventrysitem:item_saumon", 1)], out("ventrysitem:item_bol_de_soupe_de_poisson", 1)));
R.push(rec("cuis_soupe_champignon", "Soupe de champignons", "", [ing("ventrysitem:res_bol_deau", 1), ing("ventrysitem:item_mousserons", 3)], out("ventrysitem:item_bol_de_soupe_de_champignon", 1)));
R.push(rec("cuis_ecrase_pdt", "Ecrase de pommes de terre", "", [ing("ventrysitem:res_bol", 1), ing("ventrysitem:item_patate", 3)], out("ventrysitem:item_bol_decraser_de_pomme_de_terre", 1)));
R.push(rec("cuis_ecrase_carotte", "Ecrase de carottes", "", [ing("ventrysitem:res_bol", 1), ing("ventrysitem:item_carotte", 3)], out("ventrysitem:item_bol_decraser_de_carottes", 1)));
R.push(rec("cuis_salade_simple", "Salade simple", "", [ing("ventrysitem:res_bol_dhuile", 1), ing("ventrysitem:item_salade", 3), ing("ventrysitem:item_tomate", 1)], out("ventrysitem:item_bol_de_salade_simple", 1)));
R.push(rec("cuis_salade_compose", "Salade composee", "", [ing("ventrysitem:res_bol_dhuile", 1), ing("ventrysitem:item_salade", 3), ing("ventrysitem:item_tomate", 1), ing("ventrysitem:item_fillet_de_poulet_crue", 1), ing("ventrysitem:item_pain", 1)], out("ventrysitem:item_bol_de_salade_compose", 1)));
R.push(rec("cuis_tomates_farcies", "Tomates farcies", "", [ing("ventrysitem:res_bol_dhuile", 1), ing("ventrysitem:item_tomate", 3), ing("ventrysitem:item_faux_fillet_boeuf", 1), ing("ventrysitem:item_oignon", 1)], out("ventrysitem:item_bol_de_tomates_farcies", 1)));
R.push(rec("cuis_potee", "Potee", "", [ing("ventrysitem:res_bol_dhuile", 1), ing("ventrysitem:item_carotte", 3), ing("ventrysitem:item_cotelette_de_porc", 1), ing("ventrysitem:item_oignon", 1), ing("ventrysitem:item_lard_porc", 1), ing("ventrysitem:item_choux", 2), ing("ventrysitem:item_patate", 2)], out("ventrysitem:item_bol_de_pote", 1)));
R.push(rec("cuis_gratin", "Gratin de pomme de terre", "", [ing("ventrysitem:res_bol_dhuile", 1), ing("ventrysitem:item_patate", 3), ing("ventrysitem:item_oignon", 1), ing("ventrysitem:item_fromage_de_scarnfell", 1)], out("ventrysitem:item_bol_de__gratin_de_pomme_de_terre", 1)));
R.push(rec("cuis_jus_pomme", "Jus de pomme", "", [ing("ventrysitem:item_gourde_deau", 1), ing("ventrysitem:item_pomme", 2)], out("ventrysitem:item_jus_de_pomme", 1)));
R.push(rec("cuis_jus_raisin", "Jus de raisin", "", [ing("ventrysitem:item_gourde_deau", 1), ing("ventrysitem:item_raisin", 2)], out("ventrysitem:item_gourde_de_jus_de_raisin", 1)));
R.push(rec("cuis_jus_abricot", "Jus d'abricot", "", [ing("ventrysitem:item_gourde_deau", 1), ing("ventrysitem:item_abricot", 2)], out("ventrysitem:item_gourde_de_jus_dabricot", 1)));
R.push(rec("cuis_boeuf_bourguignon", "Boeuf bourguignon (3 bols)", "", [ing("ventrysitem:res_bol", 3), ing("ventrysitem:res_beurre", 3), ing("ventrysitem:item_oignon", 3), ing("ventrysitem:item_bouguignon_boeuf", 3), ing("ventrysitem:item_carotte", 6), ing("ventrysitem:item_mousserons", 6), ing("ventrysitem:item_patate", 6), ing("ventrysitem:item_bouteille_de_vin_superieur", 1)], null, [out("ventrysitem:item_bol_de_boeuf_bourgignon", 3), out("ventrysitem:res_bouteille_de_vin_vide", 1)]));
R.push(rec("cuis_choucroute", "Choucroute", "", [ing("ventrysitem:res_bol_dhuile", 1), ing("ventrysitem:item_oignon", 1), ing("ventrysitem:item_patate", 1), ing("ventrysitem:item_choux", 3), ing("ventrysitem:item_lard_porc", 1), ing("ventrysitem:item_cotelette_de_porc", 1)], out("ventrysitem:item_bol_de_choucroute", 1)));
R.push(rec("cuis_omelette", "Omelette", "", [ing("ventrysitem:res_bol", 1), ing("ventrysitem:res_oeuf", 2)], out("ventrysitem:item_bol_domelette", 1)));
R.push(rec("cuis_omelette_compose", "Omelette composee", "", [ing("ventrysitem:res_bol", 1), ing("ventrysitem:res_oeuf", 2), ing("ventrysitem:item_fromage_de_scarnfell", 1), ing("ventrysitem:item_lard_porc", 1)], out("ventrysitem:item_bol_domelette_compose", 1)));
R.push(rec("cuis_marmelade_mer", "Marmelade de la mer", "", [ing("ventrysitem:item_dorade", 1), ing("ventrysitem:item_oblade", 1), ing("ventrysitem:item_homar", 1), ing("ventrysitem:res_bol_deau", 1), ing("ventrysitem:item_patate", 3)], out("ventrysitem:item_bol_de_marmelade_de_la_mer", 1)));
R.push(rec("cuis_salade_saumon", "Salade au saumon", "", [ing("ventrysitem:res_bol_dhuile", 1), ing("ventrysitem:item_salade", 3), ing("ventrysitem:item_tomate", 1), ing("ventrysitem:item_pain", 1), ing("ventrysitem:item_saumon", 1)], out("ventrysitem:item_bol_de_salade_de_saumon_fume", 1)));
R.push(rec("cuis_salade_dorade", "Salade a la dorade", "", [ing("ventrysitem:res_bol_dhuile", 1), ing("ventrysitem:item_salade", 3), ing("ventrysitem:item_tomate", 1), ing("ventrysitem:item_pain", 1), ing("ventrysitem:item_dorade", 1)], out("ventrysitem:item_bol_de_salade_de_daurade", 1)));
R.push(rec("cuis_cotelettes_caramel", "Cotelettes caramelisees", "", [ing("ventrysitem:res_bol", 1), ing("ventrysitem:res_beurre", 1), ing("ventrysitem:item_caramel", 2), ing("ventrysitem:item_cotelette_de_porc", 3)], out("ventrysitem:item_bol_de_cotelettes_de_porc_caramelisees", 1)));
R.push(rec("cuis_agneau_pommes", "Agneau aux pommes", "", [ing("ventrysitem:res_beurre", 1), ing("ventrysitem:item_pomme", 2), ing("ventrysitem:res_bol", 1), ing("ventrysitem:item_gigot_dagneau_cru", 3)], out("ventrysitem:item_bol_dagneau_aux_pommes", 1)));
R.push(rec("cuis_boeuf_raisins", "Boeuf aux raisins secs", "", [ing("ventrysitem:res_beurre", 1), ing("ventrysitem:item_raisin", 2), ing("ventrysitem:res_bol", 1), ing("ventrysitem:item_faux_fillet_boeuf", 3)], out("ventrysitem:item_bol_de_boeuf_aux_raisins_sec", 1)));
R.push(rec("cuis_sanglier_abricots", "Epaule sanglier aux abricots", "", [ing("ventrysitem:res_beurre", 1), ing("ventrysitem:item_abricot", 2), ing("ventrysitem:res_bol", 1), ing("ventrysitem:item_epaule_sanglier", 3)], out("ventrysitem:item_bol_depaule_de_sanglier_aux_abricots_secs", 1)));
R.push(rec("cuis_lievre_miel", "Gigot de lievre au miel", "", [ing("ventrysitem:res_beurre", 1), ing("ventrysitem:item_miel", 2), ing("ventrysitem:res_bol", 1), ing("ventrysitem:item_gigot_de_lievre_crue", 3)], out("ventrysitem:item_bol_de_lievre_au_miel", 1)));
R.push(rec("cuis_cerf_champignons", "Cerf aux champignons", "", [ing("ventrysitem:res_beurre", 1), ing("ventrysitem:item_mousserons", 2), ing("ventrysitem:res_bol", 1), ing("ventrysitem:item_fillet_mignon_cerf", 3)], out("ventrysitem:item_bol_de_cerf_aux_champignons", 1)));
R.push(rec("cuis_pinces_crabe", "Pinces de crabe au fromage", "", [ing("ventrysitem:res_beurre", 1), ing("ventrysitem:item_crabe", 2), ing("ventrysitem:res_farine", 2), ing("ventrysitem:item_fromage_de_scarnfell", 1)], out("ventrysitem:item_pinces_de_crabes_au_fromage_panne", 4)));
R.push(rec("cuis_battonets_crabe", "Batonnets de crabes", "", [ing("ventrysitem:item_crabe", 2)], out("ventrysitem:item_battonets_de_crabes", 6)));
R.push(rec("cuis_poulet_morilles", "Poulet aux morilles", "", [ing("ventrysitem:res_beurre", 1), ing("ventrysitem:item_morilles", 2), ing("ventrysitem:res_bol", 1), ing("ventrysitem:item_fillet_de_poulet_crue", 3)], out("ventrysitem:item_bol_de_poulet_aux_morilles", 1)));
R.push(rec("cuis_homar_fraises", "Homar aux fraises", "", [ing("ventrysitem:res_beurre", 1), ing("ventrysitem:item_fraise_sauvage", 2), ing("ventrysitem:res_bol", 1), ing("ventrysitem:item_homar", 1)], out("ventrysitem:item_bol_de_homar_aux_fraises_des_bois", 1)));
R.push(rec("cuis_jardiniere", "Jardiniere de legumes", "", [ing("ventrysitem:item_oignon", 1), ing("ventrysitem:res_bol", 1), ing("ventrysitem:item_carotte", 1), ing("ventrysitem:item_choux", 1), ing("ventrysitem:item_patate", 1)], out("ventrysitem:item_bol_de_jardiniere_de_legume", 1)));
R.push(rec("cuis_bol_viande", "Bol de viande", "", [ing("ventrysitem:item_gigot_dagneau_cru", 1), ing("ventrysitem:res_beurre", 1), ing("ventrysitem:item_cotelette_de_porc", 1), ing("ventrysitem:item_faux_fillet_boeuf", 1), ing("ventrysitem:res_bol", 1), ing("ventrysitem:item_fillet_de_poulet_crue", 1)], out("ventrysitem:item_bol_de_viande", 1)));
R.push(rec("cuis_scarnfell", "Bol de Skarnfell", "", [ing("ventrysitem:res_bol_dhuile", 1), ing("ventrysitem:item_oignon", 1), ing("ventrysitem:item_oblade", 1), ing("ventrysitem:item_patate", 2)], out("ventrysitem:item_bol_de_scarnfell", 1)));
R.push(rec("cuis_malt_orge", "Malt concasse", "", [ing("ventrysitem:res_orge", 1)], out("ventrysitem:res_malt_concasse", 1)));

fs.writeFileSync(new URL("./cuisinier_recipes_fragment.json", import.meta.url), JSON.stringify(R, null, 8), "utf8");
console.log("wrote", R.length, "recipes");
