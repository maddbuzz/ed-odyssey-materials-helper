package nl.jixxed.eliteodysseymaterials.templates;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import nl.jixxed.eliteodysseymaterials.builder.*;
import nl.jixxed.eliteodysseymaterials.domain.*;
import nl.jixxed.eliteodysseymaterials.enums.*;
import nl.jixxed.eliteodysseymaterials.service.LocaleService;
import nl.jixxed.eliteodysseymaterials.service.event.EventService;
import nl.jixxed.eliteodysseymaterials.service.event.WishlistChangedEvent;
import nl.jixxed.eliteodysseymaterials.service.event.WishlistRecipeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class RecipeContent extends VBox {
    private final List<Ingredient> ingredients = new ArrayList<>();
    private final Recipe recipe;
    private static final ApplicationState APPLICATION_STATE = ApplicationState.getInstance();
    private Label countLabel;
    private HBox recipeHeader;


    RecipeContent(final Recipe recipe) {
        this.recipe = recipe;
        initComponents();
        initEventHandling();
    }

    private void initComponents() {
        this.getStyleClass().add("recipe-content");
        loadIngredients();
        initDescription();

        if (!(this.recipe instanceof EngineerRecipe) || this.ingredients.stream().noneMatch(ingredient -> StorageType.OTHER.equals(ingredient.getType()))) {//material based recipes
            initAsRecipe();
        } else {//mission based recipes
            initAsEngineerMission();
        }
        initIngredients();

        if (this.recipe instanceof ModuleRecipe) {
            initEngineers();
        }
        initModifiers();
    }

    private void loadIngredients() {
        this.ingredients.addAll(getRecipeIngredients(this.recipe, Good.class, StorageType.GOOD, APPLICATION_STATE.getGoods()));
        this.ingredients.addAll(getRecipeIngredients(this.recipe, Asset.class, StorageType.ASSET, APPLICATION_STATE.getAssets()));
        this.ingredients.addAll(getRecipeIngredients(this.recipe, Data.class, StorageType.DATA, APPLICATION_STATE.getData()));
        if (this.recipe instanceof EngineerRecipe) {
            this.ingredients.addAll(((EngineerRecipe) this.recipe).getOther().stream()
                    .map(MissionIngredient::new)
                    .sorted(Comparator.comparing(MissionIngredient::getName))
                    .collect(Collectors.toList()));
        }
    }

    private void initIngredients() {
        final FlowPane ingredientFlow = FlowPaneBuilder.builder().withStyleClass("recipe-ingredient-flow").withNodes(this.ingredients).build();
        this.getChildren().add(ingredientFlow);
    }

    private void initDescription() {
        final Label descriptionTitle = LabelBuilder.builder()
                .withStyleClass("recipe-title-label")
                .withText(LocaleService.getStringBinding("recipe.label.description"))
                .build();

        final Region descriptionRegion = new Region();
        HBox.setHgrow(descriptionRegion, Priority.ALWAYS);

        this.recipeHeader = BoxBuilder.builder().withNodes(descriptionTitle, descriptionRegion).buildHBox();
        final Text description = TextBuilder.builder()
                .withStyleClass("recipe-description")
                .withWrappingWidth(465D)
                .withText(LocaleService.getStringBinding(this.recipe.getRecipeName().getDescriptionLocalizationKey()))
                .build();

        this.getChildren().addAll(this.recipeHeader, description);
    }

    private void initAsRecipe() {
        final Button addToWishlist = ButtonBuilder.builder()
                .withStyleClass("recipe-wishlist-button")
                .withText(LocaleService.getStringBinding("recipe.add.to.wishlist"))
                .withOnAction(event -> APPLICATION_STATE.getPreferredCommander().ifPresent(commander -> EventService.publish(new WishlistRecipeEvent(commander.getFid(), new WishlistRecipe(this.recipe.getRecipeName(), true), Action.ADDED))))
                .build();

        final long initialCount = APPLICATION_STATE.getPreferredCommander().map(commander -> APPLICATION_STATE.getWishlist(commander.getFid()).stream().filter(wishlistRecipe -> wishlistRecipe.getRecipeName().equals(this.recipe.getRecipeName())).count()).orElse(0L);
        this.countLabel = LabelBuilder.builder().withStyleClass("recipe-wishlist-count").build();
        if (initialCount > 0L) {
            this.countLabel.textProperty().bind(LocaleService.getStringBinding("recipe.on.wishlist", initialCount));
        }
        final HBox box = BoxBuilder.builder()
                .withStyleClass("recipe-wishlist-count-box")
                .withNodes(this.countLabel, addToWishlist)
                .buildHBox();
        HBox.setHgrow(addToWishlist, Priority.ALWAYS);
        this.recipeHeader.getChildren().add(box);
        final Label materialHeader = LabelBuilder.builder()
                .withStyleClass("recipe-title-label")
                .withText(LocaleService.getStringBinding("recipe.header.material"))
                .build();
        this.getChildren().add(materialHeader);
    }

    private void initAsEngineerMission() {
        final Label materialHeader = LabelBuilder.builder()
                .withStyleClass("recipe-title-label")
                .withText(LocaleService.getStringBinding("recipe.header.objective"))
                .build();
        this.getChildren().add(materialHeader);
    }

    private void initEngineers() {
        final Label engineerLabelHeader = LabelBuilder.builder()
                .withStyleClass("recipe-title-label")
                .withText(LocaleService.getStringBinding("recipe.label.engineers"))
                .build();
        this.getChildren().add(engineerLabelHeader);
        final Label[] engineerLabels = ((ModuleRecipe) this.recipe).getEngineers().stream()
                .map(EngineerModuleLabel::new)
                .sorted(Comparator.comparing(EngineerModuleLabel::getText))
                .toArray(Label[]::new);
        final FlowPane flowPane = FlowPaneBuilder.builder().withStyleClass("recipe-engineer-flow").withNodes(engineerLabels).build();
        this.getChildren().add(flowPane);

    }

    private void initModifiers() {
        final Map<Modifier, String> modifierMap = this.recipe.getModifiers();
        if (!modifierMap.isEmpty()) {
            final Label modifierTitle = LabelBuilder.builder().withStyleClass("recipe-title-label").withText(LocaleService.getStringBinding("recipe.label.modifiers")).build();
            this.getChildren().add(modifierTitle);

            final List<HBox> modifiers = modifierMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(modifierStringEntry -> {
                final Label modifier = LabelBuilder.builder().withStyleClass("recipe-modifier-name").withText(LocaleService.getStringBinding(modifierStringEntry.getKey().getLocalizationKey())).build();
                final Label value = LabelBuilder.builder().withStyleClass("recipe-modifier-value").withNonLocalizedText(modifierStringEntry.getValue()).build();
                final HBox modifierBox = BoxBuilder.builder().withStyleClass("recipe-modifier").withNodes(modifier, value).buildHBox();
                HBox.setHgrow(value, Priority.ALWAYS);
                return modifierBox;
            }).collect(Collectors.toList());

            final FlowPane modifiersFlowPane = FlowPaneBuilder.builder().withStyleClass("recipe-modifier-flow").withNodes(modifiers).build();
            this.getChildren().addAll(modifiersFlowPane);
        }
    }

    private void initEventHandling() {
        EventService.addListener(WishlistChangedEvent.class, wishlistEvent -> {
            if (this.countLabel != null) {
                final long count = APPLICATION_STATE.getPreferredCommander().map(commander -> APPLICATION_STATE.getWishlist(commander.getFid()).stream().filter(wishlistRecipe -> wishlistRecipe.getRecipeName().equals(this.recipe.getRecipeName())).count()).orElse(0L);
                if (count > 0L) {
                    this.countLabel.textProperty().bind(LocaleService.getStringBinding("recipe.on.wishlist", count));
                } else {
                    this.countLabel.textProperty().bind(LocaleService.getStringBinding(() -> ""));
                }
            }
        });
    }

    private List<MaterialIngredient> getRecipeIngredients(final Recipe recipe, final Class<? extends Material> materialClass, final StorageType storageType, final Map<? extends Material, Storage> materialMap) {
        return recipe.getMaterialCollection(materialClass).entrySet().stream()
                .map(material -> new MaterialIngredient(storageType, material.getKey(), material.getValue(), materialMap.get(material.getKey()).getTotalValue()))
                .sorted(Comparator.comparing(MaterialIngredient::getName))
                .collect(Collectors.toList());
    }


}