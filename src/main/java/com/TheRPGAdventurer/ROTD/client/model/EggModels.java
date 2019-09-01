package com.TheRPGAdventurer.ROTD.client.model;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.model.wavefrontparser.WavefrontObject;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantsException;
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

import java.util.*;

/**
 * Created by TGG on 22/08/2019.
 * Holds the various models needed for rendering DragonHatchableEggs
 * Usage:
 * 1) Create the EggModels()
 * 2) Call MinecraftForge.EVENT_BUS.register in preInitialisation
 * 3) During initial setup, call eggModels.registerConfigurationTags() to register all the tags that are
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

//  /**
//   * Returns the resource location for the given breed &
//   * @param dragonBreed
//   * @return null if not found
//   */
//  public ResourceLocation getTexture(DragonBreedNew dragonBreed) {
//    return breedTextureRLs.get(dragonBreed);
//  }

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
    public void validateVariantTags(DragonVariants dragonVariants) throws IllegalArgumentException {
      if (internalState != InternalState.INIT && internalState != InternalState.HAVE_VALIDATED) {
        DragonMounts.loggerLimit.error_once("Wrong call order for EggModelsValidator: validation after Model registration");
      }
      internalState = InternalState.HAVE_VALIDATED;
      DragonBreedNew whichBreed =  DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getBreed(dragonVariants);
      DragonVariantsException.DragonVariantsErrors dragonVariantsErrors = new DragonVariantsException.DragonVariantsErrors();

      String str = (String)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_ITEM_MODEL);
      ResourceLocation rl = new ResourceLocation("dragonmounts", str);
      addResourceLocation(whichBreed, EggModelState.INCUBATING, rl);

      str = (String)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_ITEM_MODEL_HATCHED);
      rl = new ResourceLocation("dragonmounts", str);
      addResourceLocation(whichBreed, EggModelState.HATCHED, rl);

      str = (String)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_ITEM_MODEL_SMASHED);
      rl = new ResourceLocation("dragonmounts", str);
      addResourceLocation(whichBreed, EggModelState.SMASHED, rl);

      str = (String)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_ITEM_MODEL_TEXTURE);
      ResourceLocation trl = new ResourceLocation("dragonmounts", str);
      addTexture(whichBreed, EggModelState.INCUBATING, trl);

      str = (String)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_ITEM_MODEL_SMASHED_TEXTURE);
      trl = new ResourceLocation("dragonmounts", str);
      addTexture(whichBreed, EggModelState.SMASHED, trl);

      str = (String)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_ITEM_MODEL_HATCHED_TEXTURE);
      trl = new ResourceLocation("dragonmounts", str);
      addTexture(whichBreed, EggModelState.HATCHED, trl);
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

    for (Map.Entry<Pair<DragonBreedNew, EggModelState>, ResourceLocation> entry : breedTextureRLs.entrySet()) {
      try {
        DragonBreedNew breed = entry.getKey().getLeft();
        AnimatedTexture animatedTexture = new AnimatedTexture(entry.getValue());
        long ticksPerFrame = (long)breed.getDragonVariants().getValueOrDefault(DragonVariants.Category.EGG, EGG_ITEM_ANIMATION_TICKS_PER_FRAME);
        boolean noInterpolation = (boolean)breed.getDragonVariants().getValueOrDefault(DragonVariants.Category.EGG, EGG_ITEM_ANIMATION_NO_INTERPOLATION);
        animatedTexture.setAnimation((int)ticksPerFrame, !noInterpolation);
        animatedTexture.load(Minecraft.getMinecraft().getResourceManager());
        breedAnimatedTextures.put(entry.getKey(), animatedTexture);
      } catch (Exception e) {
        DragonMounts.logger.warn(String.format("Exception loading resource %s for breed %s: %s",
                                               entry.getValue(), entry.getKey().getLeft().getInternalName(), e.getCause()));
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

  private void addResourceLocation(DragonBreedNew dragonBreedNew, EggModelState eggModelState, ResourceLocation rl) {
    Pair<DragonBreedNew, EggModelState> key = new ImmutablePair<>(dragonBreedNew,eggModelState);
    if (breedModelRLs.containsKey(key)) {
      DragonMounts.loggerLimit.warn_once("Called addResourceLocation twice for same breed + state");
      return;
    }
    breedModelRLs.put(key, rl);
  }

  private void addTexture(DragonBreedNew breed, EggModelState eggModelState, ResourceLocation rl) {
    if (breedTextureRLs.containsKey(breed)) {
      DragonMounts.loggerLimit.warn_once("Called addResourceLocation twice for same breed + state");
      return;
    }
    breedTextureRLs.put(new ImmutablePair<>(breed, eggModelState), rl);
  }

  private static final DragonVariantTag EGG_ITEM_MODEL = DragonVariantTag.addTag("eggmodelobj", "models/item/dragon_hatchable_egg.obj");
  private static final DragonVariantTag EGG_ITEM_MODEL_SMASHED = DragonVariantTag.addTag("eggmodelsmashedobj", "models/item/dragon_hatchable_egg_smashed.obj");
  private static final DragonVariantTag EGG_ITEM_MODEL_HATCHED = DragonVariantTag.addTag("eggmodelhatchedobj", "models/item/dragon_hatchable_egg_hatched.obj");

  private static final DragonVariantTag EGG_ITEM_MODEL_TEXTURE = DragonVariantTag.addTag("eggmodeltexture", "textures/items/eggs/egg_default.png");
  private static final DragonVariantTag EGG_ITEM_MODEL_SMASHED_TEXTURE = DragonVariantTag.addTag("eggmodelsmashedtexture", "egg_smashed_default.png");
  private static final DragonVariantTag EGG_ITEM_MODEL_HATCHED_TEXTURE = DragonVariantTag.addTag("eggmodelhatchedtexture", "egg_hatched_default.png");
  private static final DragonVariantTag EGG_ITEM_ANIMATION_TICKS_PER_FRAME = DragonVariantTag.addTag("egganimationticksperframe", 8, 1, 1000);
  private static final DragonVariantTag EGG_ITEM_ANIMATION_NO_INTERPOLATION = DragonVariantTag.addTag("egganimationnointerpolation");

  private final int BASE_MODEL_METADATA = 0;
  private Map<Pair<DragonBreedNew, EggModelState>, ResourceLocation> breedTextureRLs = new HashMap<>();
  private Map<Pair<DragonBreedNew, EggModelState>, AnimatedTexture> breedAnimatedTextures = new HashMap<>();

  private enum InternalState {INIT, HAVE_VALIDATED, REGISTERED_MODELS};
  private InternalState internalState = InternalState.INIT;  // just for debugging / assertion

  private Map<Pair<DragonBreedNew, EggModelState>, ResourceLocation> breedModelRLs = new HashMap<>();
  private Map<ResourceLocation, WavefrontObject> breedModelOBJs = new HashMap<>();
}
