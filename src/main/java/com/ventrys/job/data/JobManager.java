package com.ventrys.job.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.ventrys.job.VentrysJob;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JobManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Job> JOBS = new ConcurrentHashMap<>();
    private static Path configPath;

    public static void loadJobs() throws Exception {
        // Nettoyer les données précédentes
        JOBS.clear();
        
        Exception lastException = null;
        
        // Charger depuis les ressources embarquées du mod (classe @Mod = bon JAR / classpath)
        try (InputStream resourceStream = openEmbeddedJobsStream()) {
            if (resourceStream != null) {
                try (Reader reader = new InputStreamReader(resourceStream, StandardCharsets.UTF_8)) {
                    List<Job> jobs = parseJobsJson(reader);
                    if (jobs != null && !jobs.isEmpty()) {
                        loadJobsFromList(jobs, "ressources (data/ventrysjob/jobs.json dans le jar)");
                        int recipeTotal = 0;
                        for (Job j : JOBS.values()) {
                            recipeTotal += j.getRecipes().size();
                        }
                        VentrysJob.LOGGER.debug(
                            "Jobs depuis le JAR — {} recettes valides au total (prioritaire)",
                            recipeTotal);
                        syncEmbeddedJobsJsonFilesToConfigIfDedicatedOrServerJvm();
                        return;
                    }
                    if (jobs == null) {
                        VentrysJob.LOGGER.error(
                            "jobs.json dans le jar: JSON non reconnu (attendu clef \"jobs\" ou tableau racine). Fallback config.");
                    } else {
                        VentrysJob.LOGGER.error(
                            "jobs.json dans le jar: liste de metiers vide. Fallback config.");
                    }
                }
            } else {
                VentrysJob.LOGGER.warn("data/ventrysjob/jobs.json introuvable dans le jar du mod (flux null)");
            }
        } catch (Exception e) {
            lastException = e;
            VentrysJob.LOGGER.warn("Impossible de charger depuis les ressources du mod: {}", e.getMessage());
        }

        // Côté client (joueur) : aucun fallback config — évite tromperie / divergence avec le serveur.
        if (isPhysicalGameClient()) {
            String hint = lastException != null ? lastException.getMessage() : "jar vide ou flux null";
            throw new Exception(
                "Client: impossible de charger jobs depuis le JAR du mod (données embarquees uniquement). " + hint);
        }

        // Fallback serveur uniquement : fichier config (format tableau [...] ou objet {"jobs": [...]})
        configPath = FMLPaths.CONFIGDIR.get().resolve("ventrysjob");
        Path jobsFile = configPath.resolve("jobs.json");

        try {
            Files.createDirectories(configPath);

            if (!Files.exists(jobsFile)) {
                VentrysJob.LOGGER.debug("config/jobs.json absent — copie depuis le jar si possible...");
                copyEmbeddedJobsToConfigOrDefault(jobsFile);
            }

            try (Reader reader = Files.newBufferedReader(jobsFile, StandardCharsets.UTF_8)) {
                List<Job> jobs = parseJobsJson(reader);
                if (jobs != null && !jobs.isEmpty()) {
                    loadJobsFromList(jobs, "config (" + jobsFile + ")");
                    VentrysJob.LOGGER.warn(
                        "Jobs charges depuis CONFIG (pas depuis le jar). Le chargement embarque a echoue ou etait vide — voir messages plus haut. Mettez a jour ou supprimez ce fichier si les recettes ne correspondent pas au mod.");
                } else {
                    throw new IOException("Le fichier jobs.json est vide ou invalide");
                }
            }
        } catch (IOException e) {
            VentrysJob.LOGGER.error("Erreur lors du chargement des metiers depuis le fichier config", e);
            if (lastException != null) {
                throw new Exception("Impossible de charger les jobs depuis les ressources et le fichier config", e);
            }
            throw e;
        }
        
        if (JOBS.isEmpty()) {
            throw new Exception("Aucun job n'a pu etre charge. Verifiez le fichier jobs.json.");
        }
    }

    /**
     * Flux vers jobs.json dans le même mod jar que {@link VentrysJob} (plus fiable que {@code JobManager.class}).
     */
    private static InputStream openEmbeddedJobsStream() {
        InputStream in = VentrysJob.class.getResourceAsStream("/data/ventrysjob/jobs.json");
        if (in == null) {
            in = VentrysJob.class.getResourceAsStream("data/ventrysjob/jobs.json");
        }
        return in;
    }

    /**
     * Accepte {@code {"jobs": [...]}} (comme le jar) ou {@code [...]} (ancien format config).
     */
    private static List<Job> parseJobsJson(Reader reader) throws IOException {
        JsonElement root = JsonParser.parseReader(reader);
        if (root == null || root.isJsonNull()) {
            return null;
        }
        Type listType = new TypeToken<List<Job>>(){}.getType();
        if (root.isJsonArray()) {
            return GSON.fromJson(root, listType);
        }
        if (root.isJsonObject() && root.getAsJsonObject().has("jobs")) {
            return GSON.fromJson(root.getAsJsonObject().get("jobs"), listType);
        }
        return null;
    }

    /** Le client Minecraft (UI) ne doit ni lire ni écrire les jobs depuis un fichier modifiable localement. */
    private static boolean isPhysicalGameClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    /**
     * Recopie le jobs.json du jar vers config/ventrysjob/ (opérateur de serveur).
     * Jamais sur le client pour ne pas exposer / écraser une « config » que le joueur pourrait croire éditable.
     */
    private static void syncEmbeddedJobsJsonFilesToConfigIfDedicatedOrServerJvm() {
        if (isPhysicalGameClient()) {
            return;
        }
        try {
            Path dir = FMLPaths.CONFIGDIR.get().resolve("ventrysjob");
            Files.createDirectories(dir);
            try (InputStream in = openEmbeddedJobsStream()) {
                if (in == null) {
                    return;
                }
                byte[] data = in.readAllBytes();
                Path referenceFile = dir.resolve("jobs_embedded_reference.json");
                Files.write(referenceFile, data);
                Path jobsFile = dir.resolve("jobs.json");
                if (!Files.exists(jobsFile)) {
                    Files.write(jobsFile, data);
                    VentrysJob.LOGGER.info(
                        "config/ventrysjob/jobs.json créé depuis le jar — {}",
                        jobsFile.toAbsolutePath());
                } else {
                    VentrysJob.LOGGER.debug(
                        "config/ventrysjob/jobs.json conservé (référence jar : {})",
                        referenceFile.toAbsolutePath());
                }
            }
        } catch (IOException e) {
            VentrysJob.LOGGER.warn("Impossible de synchroniser jobs.json depuis le jar vers config: {}", e.getMessage());
        }
    }

    private static void copyEmbeddedJobsToConfigOrDefault(Path jobsFile) throws IOException {
        try (InputStream in = openEmbeddedJobsStream()) {
            if (in != null) {
                Files.copy(in, jobsFile, StandardCopyOption.REPLACE_EXISTING);
                VentrysJob.LOGGER.debug("config/ventrysjob/jobs.json créé à partir du jar");
                return;
            }
        }
        createDefaultJobs(jobsFile);
    }
    
    /**
     * Charge une liste de métiers avec validation optimisée
     */
    private static void loadJobsFromList(List<Job> jobs, String source) {
        int validJobs = 0;
        int totalRecipes = 0;
        int validRecipes = 0;
        
        for (Job job : jobs) {
            // Valider les recettes
            List<JobRecipe> validRecipesList = new ArrayList<>();
            for (JobRecipe recipe : job.getRecipes()) {
                totalRecipes++;
                if (isRecipeValid(recipe)) {
                    validRecipesList.add(recipe);
                    validRecipes++;
                } else {
                    VentrysJob.LOGGER.warn("Recette invalide ignoree: {} pour le metier {}", 
                        recipe.getId(), job.getId());
                }
            }
            
            // Toujours ajouter le métier, même s'il n'a pas de recettes (comme le bâtisseur)
            job.setRecipes(validRecipesList);
            JOBS.put(job.getId(), job);
            validJobs++;
            
            VentrysJob.LOGGER.debug("Métier chargé: {} ({} recettes valides)", job.getId(), validRecipesList.size());
        }
        
        VentrysJob.LOGGER.info("Métiers: {} chargés depuis « {} » — {}/{} recettes valides", 
            validJobs, source, validRecipes, totalRecipes);
    }

    private static boolean isRecipeValid(JobRecipe recipe) {
        if (recipe == null || recipe.getId() == null || recipe.getId().isEmpty()) {
            return false;
        }
        
        // Validation des ingrédients
        if (recipe.getInputs() == null || recipe.getInputs().isEmpty()) {
            VentrysJob.LOGGER.warn("Recette {} sans ingrédients", recipe.getId());
            return false;
        }
        
        // Limite le nombre d'ingrédients pour éviter les abus
        if (recipe.getInputs().size() > 9) {
            VentrysJob.LOGGER.warn("Recette {} avec trop d'ingrédients ({}), maximum 9", 
                recipe.getId(), recipe.getInputs().size());
            return false;
        }
        
        // Identifiants modid:chemin (pas de lookup registre ici : ordre de chargement des mods / client-serveur)
        for (RecipeIngredient input : recipe.getInputs()) {
            if (input == null || input.getItemId() == null || input.getItemId().isEmpty()) {
                VentrysJob.LOGGER.warn("Recette {} avec ingrédient invalide", recipe.getId());
                return false;
            }
            
            // Quantité > 0 (pas de plafond 64 : une ligne peut exiger 180+ lingots sur plusieurs piles)
            if (input.getCount() <= 0 || input.getCount() > 1_000_000) {
                VentrysJob.LOGGER.warn("Recette {} avec quantité d'ingrédient invalide: {}", 
                    recipe.getId(), input.getCount());
                return false;
            }
            
            if (!isValidItemResourceId(input.getItemId())) {
                VentrysJob.LOGGER.warn("Recette {} : itemId d'entree invalide (attendu modid:chemin): {}", 
                    recipe.getId(), input.getItemId());
                return false;
            }
        }

        // Sortie(s)
        List<RecipeIngredient> outs = recipe.getOutputsForCraft();
        if (outs.isEmpty()) {
            VentrysJob.LOGGER.warn("Recette {} sans sortie", recipe.getId());
            return false;
        }
        for (RecipeIngredient o : outs) {
            if (o == null || o.getItemId() == null || o.getItemId().isEmpty()) {
                VentrysJob.LOGGER.warn("Recette {} avec une sortie invalide", recipe.getId());
                return false;
            }
            if (o.getCount() <= 0 || o.getCount() > 1_000_000) {
                VentrysJob.LOGGER.warn("Recette {} avec quantité de sortie invalide: {}", recipe.getId(), o.getCount());
                return false;
            }
            if (!isValidItemResourceId(o.getItemId())) {
                VentrysJob.LOGGER.warn("Recette {} : itemId de sortie invalide (attendu modid:chemin): {}",
                    recipe.getId(), o.getItemId());
                return false;
            }
        }

        return true;
    }

    /** Valide uniquement la syntaxe ResourceLocation, sans interroger le registre (évite recettes masquées à tort). */
    private static boolean isValidItemResourceId(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return false;
        }
        try {
            ResourceLocation rl = new ResourceLocation(itemId);
            return !rl.getNamespace().isEmpty() && !rl.getPath().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private static void createDefaultJobs(Path jobsFile) throws IOException {
        List<Job> defaultJobs = new ArrayList<>();

        // Forgeron - craft test vanilla
        Job blacksmith = new Job("forgeron", "Forgeron", "Maitre de la forge et du metal");
        JobRecipe blacksmithRecipe = new JobRecipe("iron_sword_craft", "Epee en fer");
        blacksmithRecipe.setDescription("Forge une epee en fer");
        blacksmithRecipe.addInput(new RecipeIngredient("minecraft:iron_ingot", 2));
        blacksmithRecipe.addInput(new RecipeIngredient("minecraft:stick", 1));
        blacksmithRecipe.setOutput(new RecipeIngredient("minecraft:iron_sword", 1));
        // blacksmithRecipe.setEnergyCost(0); // Desactive temporairement
        blacksmithRecipe.setCraftTime(40);
        blacksmith.addRecipe(blacksmithRecipe);
        defaultJobs.add(blacksmith);

        // Artisan - craft test vanilla
        Job artisan = new Job("artisan", "Artisan", "Expert en travail du bois et fabrication");
        JobRecipe artisanRecipe = new JobRecipe("crafting_table_craft", "Table de craft");
        artisanRecipe.setDescription("Fabrique une table de craft");
        artisanRecipe.addInput(new RecipeIngredient("minecraft:oak_planks", 4));
        artisanRecipe.setOutput(new RecipeIngredient("minecraft:crafting_table", 1));
        // artisanRecipe.setEnergyCost(0); // Desactive temporairement
        artisanRecipe.setCraftTime(30);
        artisan.addRecipe(artisanRecipe);
        defaultJobs.add(artisan);

        // Apothicaire - craft test vanilla
        Job apothecary = new Job("apothicaire", "Apothicaire", "Maitre des potions et remedes");
        JobRecipe apothecaryRecipe = new JobRecipe("brewing_stand_craft", "Alambic");
        apothecaryRecipe.setDescription("Fabrique un alambic");
        apothecaryRecipe.addInput(new RecipeIngredient("minecraft:blaze_rod", 1));
        apothecaryRecipe.addInput(new RecipeIngredient("minecraft:cobblestone", 3));
        apothecaryRecipe.setOutput(new RecipeIngredient("minecraft:brewing_stand", 1));
        // apothecaryRecipe.setEnergyCost(0); // Desactive temporairement
        apothecaryRecipe.setCraftTime(40);
        apothecary.addRecipe(apothecaryRecipe);
        defaultJobs.add(apothecary);

        // Cuisinier - craft test vanilla
        Job cook = new Job("cuisinier", "Cuisinier", "Expert en cuisine et preparations culinaires");
        JobRecipe cookRecipe = new JobRecipe("bread_craft", "Pain");
        cookRecipe.setDescription("Prepare du pain");
        cookRecipe.addInput(new RecipeIngredient("minecraft:wheat", 3));
        cookRecipe.setOutput(new RecipeIngredient("minecraft:bread", 1));
        // cookRecipe.setEnergyCost(0); // Desactive temporairement
        cookRecipe.setCraftTime(20);
        cook.addRecipe(cookRecipe);
        defaultJobs.add(cook);

        // Ouvrier - extraction et transformation
        Job worker = new Job("ouvrier", "Ouvrier", "Specialiste de l'extraction et de la transformation");
        JobRecipe workerRecipe = new JobRecipe("charcoal_craft", "Charbon");
        workerRecipe.setDescription("Transforme les planches en charbon");
        workerRecipe.addInput(new RecipeIngredient("minecraft:oak_planks", 1));
        workerRecipe.setOutput(new RecipeIngredient("minecraft:charcoal", 1));
        workerRecipe.setCraftTime(100); // 5 secondes
        worker.addRecipe(workerRecipe);
        defaultJobs.add(worker);

        // Couturier (sans recette par défaut — le jar fournit jobs.json complet)
        Job tailor = new Job("couturier", "Couturier", "Maitre du tissage, de la couture, du cuir et du textile");
        defaultJobs.add(tailor);

        // Sauvegarder les metiers par defaut
        try (Writer writer = new FileWriter(jobsFile.toFile())) {
            GSON.toJson(defaultJobs, writer);
            VentrysJob.LOGGER.warn("Fichier jobs.json créé avec métiers par défaut (fallback — vérifiez la config)");
        }
    }

    public static Map<String, Job> getJobs() {
        return Collections.unmodifiableMap(JOBS);
    }

    public static Job getJob(String id) {
        return JOBS.get(id);
    }

    public static List<Job> getAllJobs() {
        return new ArrayList<>(JOBS.values());
    }
    
    /**
     * Nettoie le cache des items pour forcer la revalidation lors du reload
     */
    public static void clearItemCache() {
        // Ancien cache registre supprimé : rechargement jobs suffit
    }
}

