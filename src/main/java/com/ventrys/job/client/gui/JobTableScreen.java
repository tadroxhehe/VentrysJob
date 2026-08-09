package com.ventrys.job.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.ventrys.job.VentrysJob;
import com.ventrys.job.data.Job;
import com.ventrys.job.data.JobActions;
import com.ventrys.job.data.JobManager;
import com.ventrys.job.data.JobRecipe;
import com.ventrys.job.data.RecipeIngredient;
import com.ventrys.job.menu.JobTableMenu;
import com.ventrys.job.network.NetworkHandler;
import com.ventrys.job.network.packet.CraftJobRecipePacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ecran GUI amélioré pour les tables de métier avec design moderne
 */
public class JobTableScreen extends AbstractContainerScreen<JobTableMenu> {
    private Job job;
    private JobRecipe selectedRecipe = null;
    private int scrollOffset = 0;
    // === Textures du GUI (style parchemin) ===
    private static final ResourceLocation TEX_LIST = new ResourceLocation("ventrysjob", "textures/gui/guilist.png");
    private static final ResourceLocation TEX_CRAFT = new ResourceLocation("ventrysjob", "textures/gui/guicraft.png");
    private static final int TEX_LIST_W = 512, TEX_LIST_H = 720;
    private static final int TEX_CRAFT_W = 512, TEX_CRAFT_H = 402;

    // === Disposition des deux panneaux (coords relatives au coin haut-gauche du GUI) ===
    // Panneau gauche : liste des crafts (texture portrait)
    private static final int LIST_X = 0, LIST_Y = 0, LIST_W = 149, LIST_H = 210;
    // Panneau droit : détails du craft (texture paysage), placé à droite
    private static final int CRAFT_X = 154, CRAFT_Y = 0, CRAFT_W = 206, CRAFT_H = 162;

    // Zone "parchemin" utilisable (à l'intérieur du cadre) — marges issues de l'analyse des PNG
    private static final int LIST_CONTENT_X = LIST_X + 10;
    private static final int LIST_CONTENT_TOP = LIST_Y + 14;
    private static final int LIST_CONTENT_RIGHT = LIST_X + LIST_W - 11;   // 138
    private static final int LIST_CONTENT_BOTTOM = LIST_Y + LIST_H - 16;  // 194

    private static final int CRAFT_CONTENT_X = CRAFT_X + 13;             // 167
    private static final int CRAFT_CONTENT_TOP = CRAFT_Y + 22;           // 22
    private static final int CRAFT_CONTENT_RIGHT = CRAFT_X + CRAFT_W - 11; // 349
    private static final int CRAFT_CONTENT_BOTTOM = CRAFT_Y + CRAFT_H - 23; // 139
    private static final int CRAFT_CONTENT_W = CRAFT_CONTENT_RIGHT - CRAFT_CONTENT_X; // 182

    // Liste des recettes
    private static final int RECIPE_LIST_START_Y = LIST_CONTENT_TOP + 24; // 38
    private static final int RECIPE_ROW_H = 9;       // hauteur d'une ligne de craft
    private static final int SCROLLBAR_W = 5;
    private int maxVisibleRecipes = 0; // Sera calculé dynamiquement

    // Cache pour le rendu de l'item de sortie
    private ItemStack previewItemStack = ItemStack.EMPTY;
    private int previewItemX = 0;
    private int previewItemY = 0;

    // Zones de survol des icônes d'ingrédients (coords écran) pour afficher leur tooltip
    private final java.util.List<ItemStack> ingredientIconStacks = new java.util.ArrayList<>();
    private final java.util.List<int[]> ingredientIconRects = new java.util.ArrayList<>(); // [x, y, taille]

    /** Évite createDropItemPublic + getHoverName à chaque frame (coûteux). */
    private final Map<String, String> itemDisplayNameCache = new HashMap<>();
    private String cachedDescSplitKey = "";
    private List<FormattedText> cachedDescLines = List.of();
    /** Évite de recréer les ItemStack d'ingrédients/résultat à chaque frame pendant l'affichage des détails. */
    private String cachedItemStacksKey = "";
    private List<ItemStack> cachedIngredientStacks = List.of();
    private List<ItemStack> cachedOutputStacks = List.of();

    // === Palette "parchemin" : encre brune lisible sur fond clair ===
    private static final int INK = 0xFF2E2012;          // texte principal
    private static final int INK_DIM = 0xFF5A4326;       // texte secondaire
    private static final int INK_TITLE = 0xFF4A2A0C;     // titres
    private static final int ACCENT_RUST = 0xFF7A3B0F;   // sous-titres / accents
    private static final int SEL_TEXT = 0xFF8A3A06;      // recette sélectionnée
    private static final int RESULT_GREEN = 0xFF2E6B16;  // résultat (vert foncé lisible)
    private static final int SEL_BG = 0x55502A0E;        // surbrillance sélection (translucide)
    private static final int HOV_BG = 0x33502A0E;        // surbrillance survol (translucide)
    private static final int DIVIDER = 0x66432A12;       // ligne de séparation
    private static final int SCROLL_TRACK = 0x40241A0E;
    private static final int SCROLL_THUMB = 0xFF6E4A1E;
    private static final int SCROLL_THUMB_HI = 0xFF9A6E2C;

    // Boutons Craft / Fermer — thème sombre jaune/noir
    private static final int BTN_BG = 0xFF141008;
    private static final int BTN_BG_HOV = 0xFF241A0C;
    private static final int BTN_BORDER = 0xFF6E5418;
    private static final int BTN_BORDER_HOV = 0xFF9A7820;
    private static final int BTN_TEXT = 0xFFE8C84A;
    private static final int BTN_TEXT_DIM = 0xFF5A4A28;
    private static final int BTN_CRAFT_BG = 0xFF2A1E08;
    private static final int BTN_CRAFT_BG_HOV = 0xFF3A2A0C;
    private static final int BTN_CRAFT_BORDER = 0xFF8A6E2C;
    private static final int BTN_CRAFT_BORDER_HOV = 0xFFC8A030;
    
    public JobTableScreen(JobTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = CRAFT_X + CRAFT_W; // 360
        this.imageHeight = LIST_H;           // 210
        
        // Le job sera chargé dans renderLabels() pour s'assurer qu'on a le bon jobId
        this.job = null;
    }

    // Références aux boutons pour le rendu personnalisé
    private Button craftButton;
    private Button closeButton;

    /** Anti double-envoi réseau sans désactiver le bouton (répétition du même craft sans fermer le menu). */
    private int craftSendCooldownTicks = 0;
    
    @Override
    protected void init() {
        super.init();
        
        // Position des boutons - sous le panneau de craft (à droite), dans l'espace libre
        int buttonY = topPos + CRAFT_Y + CRAFT_H + 6;
        int buttonHeight = 20;
        int buttonWidth = 70;
        int buttonSpacing = 8;
        int rightEdge = leftPos + CRAFT_X + CRAFT_W - 4;
        
        // Bouton Fermer
        closeButton = new Button(
            rightEdge - buttonWidth,
            buttonY,
            buttonWidth,
            buttonHeight,
                new TranslatableComponent("ventrysjob.gui.button.close"),
            button -> this.onClose()
        ) {
            @Override
            public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
                renderJobButton(poseStack, this.x, this.y, this.width, this.height,
                        this.isHoveredOrFocused(), this.active, false);
                drawCenteredString(poseStack, font, this.getMessage(),
                        this.x + this.width / 2, this.y + (this.height - 8) / 2,
                        this.active ? BTN_TEXT : BTN_TEXT_DIM);
            }
        };
        this.addRenderableWidget(closeButton);

        // Bouton Craft
        craftButton = new Button(
            rightEdge - (buttonWidth * 2) - buttonSpacing,
            buttonY,
            buttonWidth,
            buttonHeight,
                new TranslatableComponent("ventrysjob.gui.button.craft"),
            button -> {
                if (selectedRecipe != null && job != null && craftSendCooldownTicks <= 0 && button.active) {
                    craftSendCooldownTicks = 5;
                    NetworkHandler.INSTANCE.sendToServer(
                        new CraftJobRecipePacket(job.getId(), selectedRecipe.getId(), menu.containerId, menu.getTablePos())
                    );
                }
            }
        ) {
            @Override
            public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
                boolean hovered = this.isHoveredOrFocused() && this.active;
                renderJobButton(poseStack, this.x, this.y, this.width, this.height,
                        hovered, this.active, true);
                drawCenteredString(poseStack, font, this.getMessage(),
                        this.x + this.width / 2, this.y + (this.height - 8) / 2,
                        this.active ? BTN_TEXT : BTN_TEXT_DIM);
            }
        };
        craftButton.active = selectedRecipe != null;
        this.addRenderableWidget(craftButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (craftSendCooldownTicks > 0) {
            craftSendCooldownTicks--;
        }
        if (craftButton != null) {
            craftButton.active = selectedRecipe != null && job != null && craftSendCooldownTicks <= 0;
        }
    }

    private void renderJobButton(PoseStack poseStack, int x, int y, int width, int height,
                                   boolean hovered, boolean active, boolean craftStyle) {
        int bgColor;
        int borderColor;
        if (!active) {
            bgColor = BTN_BG;
            borderColor = BTN_BORDER;
        } else if (craftStyle) {
            bgColor = hovered ? BTN_CRAFT_BG_HOV : BTN_CRAFT_BG;
            borderColor = hovered ? BTN_CRAFT_BORDER_HOV : BTN_CRAFT_BORDER;
        } else {
            bgColor = hovered ? BTN_BG_HOV : BTN_BG;
            borderColor = hovered ? BTN_BORDER_HOV : BTN_BORDER;
        }

        fill(poseStack, x, y, x + width, y + height, bgColor);
        fill(poseStack, x, y, x + width, y + 1, borderColor);
        fill(poseStack, x, y + height - 1, x + width, y + height, borderColor);
        fill(poseStack, x, y, x + 1, y + height, borderColor);
        fill(poseStack, x + width - 1, y, x + width, y + height, borderColor);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        this.renderBg(poseStack, partialTick, mouseX, mouseY);
        // Ne pas appeler super.render() pour éviter le rendu des slots d'inventaire
        // Rendre les widgets (boutons) manuellement
        for (net.minecraft.client.gui.components.Widget widget : this.renderables) {
            widget.render(poseStack, mouseX, mouseY, partialTick);
        }
        // renderLabels avec PoseStack translaté
        poseStack.pushPose();
        poseStack.translate((float)this.leftPos, (float)this.topPos, 0.0F);
        this.renderLabels(poseStack, mouseX, mouseY);
        poseStack.popPose();
        
        // Tooltip : priorité au survol d'une icône d'ingrédient (affiche le nom de l'item)
        ItemStack hoveredIngredient = ItemStack.EMPTY;
        for (int k = 0; k < ingredientIconRects.size(); k++) {
            int[] r = ingredientIconRects.get(k);
            if (mouseX >= r[0] && mouseX <= r[0] + r[2] && mouseY >= r[1] && mouseY <= r[1] + r[2]) {
                hoveredIngredient = ingredientIconStacks.get(k);
                break;
            }
        }
        if (!hoveredIngredient.isEmpty()) {
            this.renderTooltip(poseStack, hoveredIngredient, mouseX, mouseY);
        } else if (!previewItemStack.isEmpty() &&
            // L'item de sortie est rendu à sa taille native (16 pixels)
            mouseX >= previewItemX && mouseX <= previewItemX + 16 &&
            mouseY >= previewItemY && mouseY <= previewItemY + 16) {
            this.renderTooltip(poseStack, previewItemStack, mouseX, mouseY);
        } else {
            this.renderTooltip(poseStack, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        // Dessiner les deux textures de parchemin
        drawTexture(poseStack, TEX_LIST, leftPos + LIST_X, topPos + LIST_Y, LIST_W, LIST_H, TEX_LIST_W, TEX_LIST_H);
        drawTexture(poseStack, TEX_CRAFT, leftPos + CRAFT_X, topPos + CRAFT_Y, CRAFT_W, CRAFT_H, TEX_CRAFT_W, TEX_CRAFT_H);

        // Nombre de recettes visibles selon l'espace de la liste
        int listHeight = LIST_CONTENT_BOTTOM - RECIPE_LIST_START_Y;
        maxVisibleRecipes = Math.max(1, listHeight / RECIPE_ROW_H);

        // Scrollbar (sur le parchemin gauche)
        if (job != null && job.getRecipes().size() > maxVisibleRecipes) {
            int scrollbarX = leftPos + LIST_CONTENT_RIGHT - SCROLLBAR_W;
            int scrollbarY = topPos + RECIPE_LIST_START_Y;
            int scrollbarHeight = LIST_CONTENT_BOTTOM - RECIPE_LIST_START_Y;

            fill(poseStack, scrollbarX, scrollbarY, scrollbarX + SCROLLBAR_W, scrollbarY + scrollbarHeight, SCROLL_TRACK);

            int totalRecipes = job.getRecipes().size();
            int maxScroll = Math.max(0, totalRecipes - maxVisibleRecipes);
            double scrollRatio = maxScroll > 0 ? (double) scrollOffset / maxScroll : 0.0;
            double visibleRatio = (double) maxVisibleRecipes / totalRecipes;

            int scrollBarSize = Math.max(14, (int) (scrollbarHeight * visibleRatio));
            int maxScrollBarPos = scrollbarHeight - scrollBarSize;
            int scrollBarPos = (int) (scrollRatio * maxScrollBarPos);
            scrollBarPos = Math.max(0, Math.min(scrollBarPos, maxScrollBarPos));

            fill(poseStack, scrollbarX, scrollbarY + scrollBarPos, scrollbarX + SCROLLBAR_W,
                scrollbarY + scrollBarPos + scrollBarSize, SCROLL_THUMB);
            fill(poseStack, scrollbarX + 1, scrollbarY + scrollBarPos + 1, scrollbarX + SCROLLBAR_W - 1,
                scrollbarY + scrollBarPos + scrollBarSize - 1, SCROLL_THUMB_HI);
        }
    }

    /** Dessine une texture complète mise à l'échelle dans un rectangle (x,y,w,h). */
    private void drawTexture(PoseStack poseStack, ResourceLocation tex, int x, int y, int w, int h, int texW, int texH) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        blit(poseStack, x, y, w, h, 0.0F, 0.0F, texW, texH, texW, texH);
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        // Réinitialiser l'item de prévisualisation et les icônes d'ingrédients à chaque frame
        previewItemStack = ItemStack.EMPTY;
        ingredientIconStacks.clear();
        ingredientIconRects.clear();
        
        // Charger le job au moment du rendu
        if (job == null) {
            job = JobManager.getJob(menu.getJobId());
        }
        
        if (job == null) {
            this.font.draw(poseStack, new TranslatableComponent("ventrysjob.gui.job.not_found").getString(), 20, 20, 0xFF0000);
            return;
        }

        // Titre du métier (texte centré, sans ombre)
        String jobName = stripProblematicAccents(job.getName() != null ? job.getName() : "");
        int jobNameX = LIST_X + LIST_W / 2 - this.font.width(jobName) / 2;
        this.font.draw(poseStack, jobName, jobNameX, LIST_CONTENT_TOP + 2, INK_TITLE);

        // Ligne de séparation sous le titre
        fill(poseStack, LIST_CONTENT_X, LIST_CONTENT_TOP + 14, LIST_CONTENT_RIGHT, LIST_CONTENT_TOP + 15, DIVIDER);

        if (job.getRecipes().isEmpty()) {
            this.font.draw(poseStack, new TranslatableComponent("ventrysjob.gui.recipe.none").getString(),
                LIST_CONTENT_X + 2, RECIPE_LIST_START_Y, ACCENT_RUST);
            return;
        }

        // S'assurer que maxVisibleRecipes est calculé
        if (maxVisibleRecipes == 0) {
            int listHeight = LIST_CONTENT_BOTTOM - RECIPE_LIST_START_Y;
            maxVisibleRecipes = Math.max(1, listHeight / RECIPE_ROW_H);
        }

        int startIndex = scrollOffset;
        int endIndex = Math.min(startIndex + maxVisibleRecipes, job.getRecipes().size());

        int maxScroll = Math.max(0, job.getRecipes().size() - maxVisibleRecipes);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
            startIndex = scrollOffset;
            endIndex = Math.min(startIndex + maxVisibleRecipes, job.getRecipes().size());
        }

        // Largeur cliquable d'une ligne (jusqu'à la scrollbar)
        boolean hasScroll = job.getRecipes().size() > maxVisibleRecipes;
        int rowRight = hasScroll ? (LIST_CONTENT_RIGHT - SCROLLBAR_W - 2) : LIST_CONTENT_RIGHT;
        int recipeX = LIST_CONTENT_X + 2;
        int recipeY = RECIPE_LIST_START_Y;

        for (int i = startIndex; i < endIndex; i++) {
            JobRecipe recipe = job.getRecipes().get(i);
            boolean isSelected = selectedRecipe != null && selectedRecipe.getId().equals(recipe.getId());
            boolean isHovered = mouseX >= leftPos + LIST_CONTENT_X && mouseX <= leftPos + rowRight &&
                                mouseY >= topPos + recipeY - 1 && mouseY <= topPos + recipeY + RECIPE_ROW_H - 1;

            if (isSelected) {
                fill(poseStack, LIST_CONTENT_X, recipeY - 1, rowRight, recipeY + RECIPE_ROW_H - 1, SEL_BG);
            } else if (isHovered) {
                fill(poseStack, LIST_CONTENT_X, recipeY - 1, rowRight, recipeY + RECIPE_ROW_H - 1, HOV_BG);
            }

            String prefix = isSelected ? "> " : "";
            int color = isSelected ? SEL_TEXT : INK;

            String recipeName = recipe.getName();
            if (recipeName == null) {
                recipeName = "Recette sans nom";
            }
            recipeName = stripProblematicAccents(recipeName);

            // Texte condensé (scale 0.75) pour tenir dans le panneau
            float sc = 0.75f;
            poseStack.pushPose();
            poseStack.scale(sc, sc, 1.0f);
            int maxTextW = (int) ((rowRight - recipeX - 2) / sc);
            String label = prefix + recipeName;
            if (this.font.width(label) > maxTextW) {
                label = this.font.plainSubstrByWidth(label, maxTextW - 6) + "...";
            }
            this.font.draw(poseStack, label, recipeX / sc, (recipeY + 1) / sc, color);
            poseStack.popPose();

            recipeY += RECIPE_ROW_H;
        }

        // Détails de la recette sélectionnée (panneau droit)
        if (selectedRecipe != null) {
            renderRecipeDetails(poseStack, selectedRecipe, mouseX, mouseY);
        }
    }

    private void renderRecipeDetails(PoseStack poseStack, JobRecipe recipe, int mouseX, int mouseY) {
        // Limites du parchemin du panneau craft (coordonnées relatives)
        final int PANEL_X = CRAFT_CONTENT_X;
        final int PANEL_START_Y = CRAFT_CONTENT_TOP;
        final int PANEL_END_Y = CRAFT_CONTENT_BOTTOM;
        final int PANEL_WIDTH = CRAFT_CONTENT_W;

        int currentY = PANEL_START_Y + 2;

        // Titre de la recette (centré)
        String recipeName = recipe.getName();
        if (recipeName == null) {
            recipeName = "Recette sans nom";
        }
        recipeName = stripProblematicAccents(recipeName);
        String titleToDraw = recipeName;
        if (this.font.width(titleToDraw) > PANEL_WIDTH) {
            titleToDraw = this.font.plainSubstrByWidth(titleToDraw, PANEL_WIDTH - 6) + "...";
        }
        int titleX = PANEL_X + PANEL_WIDTH / 2 - this.font.width(titleToDraw) / 2;
        this.font.draw(poseStack, titleToDraw, titleX, currentY, INK_TITLE);
        currentY += 12;

        // Ligne de séparation
        fill(poseStack, PANEL_X, currentY - 2, PANEL_X + PANEL_WIDTH, currentY - 1, DIVIDER);

        // Description
        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty() && currentY < PANEL_END_Y - 50) {
            String desc = recipe.getDescription();
            int lineHeight = 7;

            poseStack.pushPose();
            poseStack.scale(0.85f, 0.85f, 1.0f);
            String descKey = recipe.getId() + "|" + desc;
            if (!descKey.equals(cachedDescSplitKey)) {
                cachedDescSplitKey = descKey;
                cachedDescLines = this.font.getSplitter().splitLines(desc, (int) (PANEL_WIDTH / 0.85f), Style.EMPTY);
            }
            for (FormattedText line : cachedDescLines) {
                if (currentY > PANEL_END_Y - 50) break;
                this.font.draw(poseStack, line.getString(), PANEL_X / 0.85f, currentY / 0.85f, INK_DIM);
                currentY += lineHeight;
            }
            poseStack.popPose();
            currentY += 3;
        }

        // Construit les ItemStack d'ingrédients/résultat une seule fois par recette selectionnee
        // (createDropItemPublic fait un lookup registre + NBT, coûteux à refaire chaque frame).
        if (!recipe.getId().equals(cachedItemStacksKey)) {
            cachedItemStacksKey = recipe.getId();
            List<ItemStack> ingredients = new ArrayList<>();
            for (RecipeIngredient input : recipe.getInputs()) {
                ingredients.add(createIngredientStack(input));
            }
            cachedIngredientStacks = ingredients;
            List<ItemStack> outputs = new ArrayList<>();
            for (RecipeIngredient outIng : recipe.getOutputsForCraft()) {
                outputs.add(createOutputStack(outIng));
            }
            cachedOutputStacks = outputs;
        }

        // Section ingrédients - icône + quantité "xN" (le nom apparaît au survol)
        if (currentY < PANEL_END_Y - 22) {
            poseStack.pushPose();
            poseStack.scale(0.9f, 0.9f, 1.0f);
            this.font.draw(poseStack, new TranslatableComponent("ventrysjob.gui.recipe.ingredients").getString(), PANEL_X / 0.9f, currentY / 0.9f, ACCENT_RUST);
            poseStack.popPose();
            currentY += 11;

            final int ICON = 16;
            final int CELL_W = 40;   // icône (16) + "xN"
            final int ROW_H = 18;
            final int perRow = Math.max(1, PANEL_WIDTH / CELL_W);
            int col = 0;

            ItemRenderer itemRenderer = this.minecraft != null ? this.minecraft.getItemRenderer() : null;
            int ingredientIndex = 0;

            for (RecipeIngredient input : recipe.getInputs()) {
                if (currentY > PANEL_END_Y - ICON) break;
                int relX = PANEL_X + col * CELL_W;
                int relY = currentY;
                int absX = leftPos + relX;
                int absY = topPos + relY;

                ItemStack stack = ingredientIndex < cachedIngredientStacks.size()
                    ? cachedIngredientStacks.get(ingredientIndex) : ItemStack.EMPTY;
                ingredientIndex++;
                if (!stack.isEmpty() && itemRenderer != null) {
                    // IMPORTANT : renderGuiItem utilise le ModelViewStack (origine écran),
                    // il faut donc lui passer des coordonnées ABSOLUES, pas le PoseStack translaté.
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    itemRenderer.renderGuiItem(stack, absX, absY);
                    RenderSystem.disableBlend();

                    // Quantité "xN" à droite de l'icône (toujours affichée, même x1)
                    poseStack.pushPose();
                    poseStack.scale(0.9f, 0.9f, 1.0f);
                    this.font.draw(poseStack, "x" + input.getCount(),
                        (relX + ICON + 2) / 0.9f, (relY + 5) / 0.9f, INK);
                    poseStack.popPose();

                    // Zone de survol (coords écran) pour le tooltip = nom de l'item
                    ingredientIconStacks.add(stack);
                    ingredientIconRects.add(new int[]{absX, absY, ICON});
                } else {
                    poseStack.pushPose();
                    poseStack.scale(0.7f, 0.7f, 1.0f);
                    this.font.draw(poseStack, input.getCount() + "x " + getItemName(input.getItemId()),
                        relX / 0.7f, relY / 0.7f, INK);
                    poseStack.popPose();
                }

                col++;
                if (col >= perRow) {
                    col = 0;
                    currentY += ROW_H;
                }
            }
            if (col > 0) {
                currentY += ROW_H;
            }
            currentY += 2;
        }

        // Résultat(s) - icône + quantité "xN" (le nom apparaît au survol, comme les ingrédients)
        List<RecipeIngredient> resultList = recipe.getOutputsForCraft();
        if (!resultList.isEmpty() && currentY < PANEL_END_Y - 16) {
            poseStack.pushPose();
            poseStack.scale(0.9f, 0.9f, 1.0f);
            this.font.draw(poseStack, new TranslatableComponent("ventrysjob.gui.recipe.result").getString(), PANEL_X / 0.9f, currentY / 0.9f, ACCENT_RUST);
            poseStack.popPose();
            currentY += 11;

            final int ICON = 16;
            final int CELL_W = 40;   // icône (16) + "xN"
            final int ROW_H = 18;
            final int perRow = Math.max(1, PANEL_WIDTH / CELL_W);
            int col = 0;

            ItemRenderer itemRenderer = this.minecraft != null ? this.minecraft.getItemRenderer() : null;
            int outputIndex = 0;

            for (RecipeIngredient outIng : resultList) {
                if (currentY > PANEL_END_Y - ICON) break;
                int relX = PANEL_X + col * CELL_W;
                int relY = currentY;
                int absX = leftPos + relX;
                int absY = topPos + relY;

                ItemStack outputStack = outputIndex < cachedOutputStacks.size()
                    ? cachedOutputStacks.get(outputIndex) : ItemStack.EMPTY;
                outputIndex++;
                if (!outputStack.isEmpty() && itemRenderer != null) {
                    // Coordonnées ABSOLUES (renderGuiItem utilise le ModelViewStack)
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    itemRenderer.renderGuiItem(outputStack, absX, absY);
                    RenderSystem.disableBlend();

                    poseStack.pushPose();
                    poseStack.scale(0.9f, 0.9f, 1.0f);
                    this.font.draw(poseStack, "x" + outIng.getCount(),
                        (relX + ICON + 2) / 0.9f, (relY + 5) / 0.9f, RESULT_GREEN);
                    poseStack.popPose();

                    // Survol -> tooltip avec le displayName (réutilise la liste d'icônes survolables)
                    ingredientIconStacks.add(outputStack);
                    ingredientIconRects.add(new int[]{absX, absY, ICON});
                } else {
                    poseStack.pushPose();
                    poseStack.scale(0.7f, 0.7f, 1.0f);
                    this.font.draw(poseStack, outIng.getCount() + "x " + outputDisplayLabel(outIng),
                        relX / 0.7f, relY / 0.7f, RESULT_GREEN);
                    poseStack.popPose();
                }

                col++;
                if (col >= perRow) {
                    col = 0;
                    currentY += ROW_H;
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Gérer la scrollbar
        if (job != null && job.getRecipes().size() > maxVisibleRecipes) {
            int scrollbarX = leftPos + LIST_CONTENT_RIGHT - SCROLLBAR_W;
            int scrollbarY = topPos + RECIPE_LIST_START_Y;
            int scrollbarHeight = LIST_CONTENT_BOTTOM - RECIPE_LIST_START_Y;

            if (mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_W &&
                mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {

                int totalRecipes = job.getRecipes().size();
                int maxScroll = Math.max(0, totalRecipes - maxVisibleRecipes);

                int clickPos = (int) (mouseY - scrollbarY);
                double clickRatio = Math.max(0.0, Math.min(1.0, (double) clickPos / scrollbarHeight));
                int newOffset = (int) (clickRatio * maxScroll);

                scrollOffset = Math.max(0, Math.min(newOffset, maxScroll));
                return true;
            }
        }

        // Gérer la sélection des recettes
        if (job != null) {
            int startIndex = scrollOffset;
            int endIndex = Math.min(startIndex + maxVisibleRecipes, job.getRecipes().size());

            boolean hasScroll = job.getRecipes().size() > maxVisibleRecipes;
            int rowRight = leftPos + (hasScroll ? (LIST_CONTENT_RIGHT - SCROLLBAR_W - 2) : LIST_CONTENT_RIGHT);
            int rowLeft = leftPos + LIST_CONTENT_X;
            int recipeY = topPos + RECIPE_LIST_START_Y;
            for (int i = startIndex; i < endIndex; i++) {
                if (mouseX >= rowLeft && mouseX <= rowRight &&
                    mouseY >= recipeY - 1 && mouseY <= recipeY + RECIPE_ROW_H - 1) {

                    selectedRecipe = job.getRecipes().get(i);
                    if (craftButton != null) {
                        craftButton.active = true;
                    }
                    return true;
                }
                recipeY += RECIPE_ROW_H;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (job != null && job.getRecipes().size() > maxVisibleRecipes) {
            int maxScroll = Math.max(0, job.getRecipes().size() - maxVisibleRecipes);
            // Amélioration de la fluidité : scroll par pas de 1 au lieu de delta direct
            int scrollStep = delta > 0 ? 1 : -1;
            scrollOffset = Math.max(0, Math.min(scrollOffset - scrollStep, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private String getItemName(String itemId) {
        String cached = itemDisplayNameCache.get(itemId);
        if (cached != null) {
            return cached;
        }
        try {
            ItemStack stack = JobActions.createDropItemPublic(itemId, 1);
            if (!stack.isEmpty()) {
                String name = stack.getHoverName().getString();
                itemDisplayNameCache.put(itemId, name);
                return name;
            }
        } catch (Exception e) {
            VentrysJob.LOGGER.debug("Erreur lors de la recuperation du nom de l'item: {}", itemId);
        }
        itemDisplayNameCache.put(itemId, itemId);
        return itemId;
    }

    private static String stripProblematicAccents(String s) {
        return s.replace("â", "a").replace("é", "e").replace("è", "e")
            .replace("ê", "e").replace("ë", "e").replace("î", "i").replace("ï", "i")
            .replace("ô", "o").replace("ù", "u").replace("û", "u").replace("ü", "u")
            .replace("ç", "c").replace("à", "a");
    }
    
    /**
     * Même logique que les drops / crafts serveur : défaut créatif + NBT pour les mods.
     */
    private ItemStack createIngredientStack(RecipeIngredient in) {
        try {
            return JobActions.createDropItemPublic(in.getItemId(), in.getCount());
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private ItemStack createOutputStack(RecipeIngredient out) {
        String dn = out.getDisplayName();
        if (dn != null && !dn.isEmpty()) {
            return JobActions.createDropItemPublic(out.getItemId(), out.getCount(), dn);
        }
        return JobActions.createDropItemPublic(out.getItemId(), out.getCount());
    }

    private String outputDisplayLabel(RecipeIngredient out) {
        String dn = out.getDisplayName();
        if (dn != null && !dn.isEmpty()) {
            return dn;
        }
        return getItemName(out.getItemId());
    }
}
