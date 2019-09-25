package com.TheRPGAdventurer.ROTD.client.model;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.model.wavefrontparser.WavefrontObject;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.inits.ModItems;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants.Category;
import java.util.*;

/**
 * Created by TGG on 22/08/2019.
 * Holds the various models needed for rendering DragonHatchableEggs
 * Usage:
 * 1) Create the EggModels()
 * 2) Call MinecraftForge.EVENT_BUS.register in preInitialisation
 * 3) During initial setup (preinit), call eggModels.registerConfigurationTags() to register all the tags that are
 *    used to configure egg models, and to set up the variant tag validation
 */

public class EggModels {

  public enum EggModelState {INCUBATING, HATCHED, SMASHED};

  public static EggModels getInstance() {return defaultInstance;}

  private static EggModels defaultInstance = new EggModels();

  /**
   * Initialise all the configuration tags used by this helper
   */
  public void registerConfigurationTags()
  {
    // dummy method for the tags themselves -the initialisation is all done in static initialisers
    DragonVariants.addVariantTagValidator(new EggModelsValidator());
  }

  /** retrieve the WavefrontObject for a given breed and eggModelState
   * @param dragonBreed
   * @param eggModelState
   * @return  the WavefrontObject, or a default Object if no proper model found
   */
  public WavefrontObject getModel(DragonBreedNew dragonBreed, EggModelState eggModelState) {
    ResourceLocation rl = breedModelRLs.get(new ImmutablePair<>(dragonBreed, eggModelState));
    WavefrontObject wavefrontObject = null;
    if (rl != null) {
      wavefrontObject = breedModelOBJs.get(rl);
    }
    if (wavefrontObject == null) {
      wavefrontObject = WavefrontObject.getDefaultFallback();
    }
    return wavefrontObject;
  }

  /**
   * Returns the resource location for the given breed &
   * @param dragonBreed
   * @return null if not found
   */
  public ResourceLocation getTexture(DragonBreedNew dragonBreed, EggModelState eggModelState) {
    return breedTextureRLs.get(new ImmutablePair<>(dragonBreed, eggModelState));
  }

  /**
   * returns the animated texture for the given breed & egg state
   * @param dragonBreed
   * @param eggModelState
   * @return the animated texture, or null if not found
   */
  public AnimatedTexture getAnimatedTexture(DragonBreedNew dragonBreed, EggModelState eggModelState) {
    return breedAnimatedTextures.get(new ImmutablePair<>(dragonBreed, eggModelState));
  }

  /**
   * Validates the following aspects of the tags:
   * - none-
   * Reads the various model and textures in, for later registration
   */
  public class EggModelsValidator implements DragonVariants.VariantTagValidator {
    @Override
    public void initaliseResources(DragonVariants dragonVariants, DragonVariants.ModifiedCategory modifiedCategory) throws IllegalArgumentException {
      if (!modifiedCategory.getCategory().equals(Category.EGG)) return;
      if (internalState != InternalState.INIT && internalState != InternalState.HAVE_INITIALISED_RESOURCES) {
        DragonMounts.loggerLimit.error_once("Wrong call order for EggModelsValidator: initaliseResources after Model registration");
      }
      internalState = InternalState.HAVE_INITIALISED_RESOURCES;
      DragonBreedNew whichBreed =  DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getBreed(dragonVariants);

      String str;
      ResourceLocation rl;
      if (dragonVariants.tagIsExplictlyApplied(modifiedCategory, EGG_ITEM_MODEL)) {
        str = (String) dragonVariants.getValueOrDefault(modifiedCategory, EGG_ITEM_MODEL);
        rl = new ResourceLocation("dragonmounts", str);
        addResourceLocation(whichBreed, modifiedCategory, EggModelState.INCUBATING, rl);
      }

      if (dragonVariants.tagIsExplictlyApplied(modifiedCategory, EGG_ITEM_MODEL_HATCHED)) {
        str = (String)dragonVariants.getValueOrDefault(modifiedCategory, EGG_ITEM_MODEL_HATCHED);
        rl = new ResourceLocation("dragonmounts", str);
        addResourceLocation(whichBreed, modifiedCategory, EggModelState.HATCHED, rl);
      }

      if (dragonVariants.tagIsExplictlyApplied(modifiedCategory, EGG_ITEM_MODEL_SMASHED)) {
        str = (String)dragonVariants.getValueOrDefault(modifiedCategory, EGG_ITEM_MODEL_SMASHED);
        rl = new ResourceLocation("dragonmounts", str);
        addResourceLocation(whichBreed, modifiedCategory, EggModelState.SMASHED, rl);
      }

      ResourceLocation trl;
      if (dragonVariants.tagIsExplictlyApplied(modifiedCategory, EGG_ITEM_MODEL_TEXTURE)) {
        str = (String)dragonVariants.getValueOrDefault(modifiedCategory, EGG_ITEM_MODEL_TEXTURE);
        trl = new ResourceLocation("dragonmounts", str);
        addTexture(whichBreed, modifiedCategory, EggModelState.INCUBATING, trl);
      }

      if (dragonVariants.tagIsExplictlyApplied(modifiedCategory, EGG_ITEM_MODEL_SMASHED_TEXTURE)) {
        str = (String)dragonVariants.getValueOrDefault(modifiedCategory, EGG_ITEM_MODEL_SMASHED_TEXTURE);
        trl = new ResourceLocation("dragonmounts", str);
        addTexture(whichBreed, modifiedCategory, EggModelState.SMASHED, trl);
      }

      if (dragonVariants.tagIsExplictlyApplied(modifiedCategory, EGG_ITEM_MODEL_HATCHED_TEXTURE)) {
        str = (String) dragonVariants.getValueOrDefault(modifiedCategory, EGG_ITEM_MODEL_HATCHED_TEXTURE);
        trl = new ResourceLocation("dragonmounts", str);
        addTexture(whichBreed, modifiedCategory, EggModelState.HATCHED, trl);
      }
    }
    @Override
    public void validateVariantTags(DragonVariants dragonVariants, DragonVariants.ModifiedCategory modifiedCategory) throws IllegalArgumentException {
      // do nothing - no particular validation required
    }
  }

  @SubscribeEvent
  public void registerModels(ModelRegistryEvent event) {
    setCustomResourceLocations(ModItems.DRAGON_HATCHABLE_EGG);
  }

  /**
   * This event is a convenient time to load the egg models and textures
   * @param event
   */
  @SubscribeEvent
  public void onModelBakeEvent(ModelBakeEvent event) {
    for (ResourceLocation rl : ImmutableSet.copyOf(breedModelRLs.values())) {
      try {
        WavefrontObject model = new WavefrontObject(rl);
        breedModelOBJs.put(rl, model);
      } catch (Exception e) {
        DragonMounts.logger.warn(String.format("Exception loading model %s : %s", rl, e.toString()));
      }
    }

    for (Map.Entry<TripleKey, ResourceLocation> entry : breedTextureRLs.entrySet()) {
      try {
        DragonBreedNew breed = entry.getKey().dragonBreedNew;
        DragonVariants.ModifiedCategory modifiedCategory = entry.getKey().modifiedCategory;
        AnimatedTexture animatedTexture = new AnimatedTexture(entry.getValue());
        long ticksPerFrame = (long)breed.getDragonVariants().getValueOrDefault(modifiedCategory, EGG_ITEM_ANIMATION_TICKS_PER_FRAME);
        boolean interpolation = (boolean)breed.getDragonVariants().getValueOrDefault(modifiedCategory, EGG_ITEM_ANIMATION_INTERPOLATION);
        animatedTexture.setAnimation((int)ticksPerFrame, interpolation);
        animatedTexture.load(Minecraft.getMinecraft().getResourceManager());
        breedAnimatedTextures.put(entry.getKey(), animatedTexture);
      } catch (Exception e) {
        DragonMounts.logger.warn(String.format("Exception loading resource %s for breed %s: %s",
                                               entry.getValue(), entry.getKey().dragonBreedNew.getInternalName(), e.getCause()));
      }
    }

  }

  public void setCustomResourceLocations(Item itemDragonHatchableEgg) {
    if (internalState == InternalState.INIT) {
      DragonMounts.loggerLimit.error_once("Wrong call order for EggModelsValidator: missing validation");
    }
    internalState = InternalState.REGISTERED_MODELS;
    ModelResourceLocation itemResourceLocation = new ModelResourceLocation("dragonmounts:dragon_hatchable_egg", "inventory");
    ModelLoader.setCustomModelResourceLocation(itemDragonHatchableEgg, BASE_MODEL_METADATA, itemResourceLocation);
  }

  private void addResourceLocation(DragonBreedNew dragonBreedNew, DragonVariants.ModifiedCategory modifiedCategory, EggModelState eggModelState, ResourceLocation rl) {
    TripleKey tripleKey = new TripleKey(dragonBreedNew, modifiedCategory, eggModelState);
    if (breedModelRLs.containsKey(tripleKey)) {
      DragonMounts.loggerLimit.warn_once("Called addResourceLocation twice for same breed + modified category + state");
      return;
    }
    breedModelRLs.put(tripleKey, rl);
  }

  private void addTexture(DragonBreedNew breed, DragonVariants.ModifiedCategory modifiedCategory, EggModelState eggModelState, ResourceLocation rl) {
    TripleKey tripleKey = new TripleKey(breed, modifiedCategory, eggModelState);
    if (breedTextureRLs.containsKey(tripleKey)) {
      DragonMounts.loggerLimit.warn_once("Called addResourceLocation twice for same breed + modified category + state");
      return;
    }
    breedTextureRLs.put(tripleKey, rl);
  }

  private static final DragonVariantTag EGG_ITEM_MODEL = DragonVariantTag.addTag("model", "models/item/egg_incubating.obj",
          "path to a wavefront model for the incubating egg (also when rendered as an item).  Expected to occupy the space from [-0.5, 0, -0.5] to [0.5, 0, 0.5]").categories(Category.EGG);
  private static final DragonVariantTag EGG_ITEM_MODEL_SMASHED = DragonVariantTag.addTag("modelsmashed", "models/item/egg_smashed.obj",
          "path to a wavefront model for the smashed egg.  Expected to occupy the space from [-0.5, 0, -0.5] to [0.5, 0, 0.5]").categories(Category.EGG);
  private static final DragonVariantTag EGG_ITEM_MODEL_HATCHED = DragonVariantTag.addTag("modelhatched", "models/item/egg_hatched.obj",
          "path to a wavefront model for the hatched egg.  Expected to occupy the space from [-0.5, 0, -0.5] to [0.5, 0, 0.5]").categories(Category.EGG);
  private static final DragonVariantTag EGG_ITEM_MODEL_TEXTURE = DragonVariantTag.addTag("modeltexture", "textures/items/eggs/egg_default.png",
          "path to the animated texture for the incubating egg.  Expected to contain one or more vertical square frames (max 20)").categories(Category.EGG);
  private static final DragonVariantTag EGG_ITEM_MODEL_SMASHED_TEXTURE = DragonVariantTag.addTag("modelsmashedtexture", "textures/items/eggs/egg_smashed_default.png",
          "path to the animated texture for the smashed egg.  Expected to contain one or more vertical square frames (max 20)").categories(Category.EGG);
  private static final DragonVariantTag EGG_ITEM_MODEL_HATCHED_TEXTURE = DragonVariantTag.addTag("modelhatchedtexture", "textures/items/eggs/egg_hatched_default.png",
          "path to the animated texture for the hatched egg.  Expected to contain one or more vertical square frames (max 20)").categories(Category.EGG);
  private static final DragonVariantTag EGG_ITEM_ANIMATION_TICKS_PER_FRAME = DragonVariantTag.addTag("animationticksperframe", 8, 1, 1000,
          "the number of ticks (1/20 of a second) to display each frame of the animated egg texture").categories(Category.EGG);
  private static final DragonVariantTag EGG_ITEM_ANIMATION_INTERPOLATION = DragonVariantTag.addTag("animationinterpolation", true,
          "if this flag is true, interpolate between the frames of the animated egg texture").categories(Category.EGG);

  private final int BASE_MODEL_METADATA = 0;
  private Map<TripleKey, ResourceLocation> breedTextureRLs = new HashMap<>();
  private Map<TripleKey, AnimatedTexture> breedAnimatedTextures = new HashMap<>();

  private enum InternalState {INIT, HAVE_INITIALISED_RESOURCES, REGISTERED_MODELS};
  private InternalState internalState = InternalState.INIT;  // just for debugging / assertion

  private Map<TripleKey, ResourceLocation> breedModelRLs = new HashMap<>();
  private Map<ResourceLocation, WavefrontObject> breedModelOBJs = new HashMap<>();

  private static class TripleKey {
    public TripleKey(DragonBreedNew dragonBreedNew, DragonVariants.ModifiedCategory modifiedCategory, EggModelState eggModelState) {
      this.dragonBreedNew = dragonBreedNew;
      this.modifiedCategory = modifiedCategory;
      this.eggModelState = eggModelState;
    }

    public DragonBreedNew dragonBreedNew;
    public DragonVariants.ModifiedCategory modifiedCategory;
    public EggModelState eggModelState;
  }

}
