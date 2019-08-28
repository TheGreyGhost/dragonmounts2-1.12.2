package com.TheRPGAdventurer.ROTD.client.model;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.model.wavefrontparser.ModelFormatException;
import com.TheRPGAdventurer.ROTD.client.model.wavefrontparser.WavefrontObject;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantsException;
import com.TheRPGAdventurer.ROTD.common.inits.ModItems;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
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

  /**
   * Returns the texture sprite for the given breed
   * @param dragonBreed
   * @return null if not found
   */
  public ResourceLocation getTexture(DragonBreedNew dragonBreed) {
    return breedTextureRLs.get(dragonBreed);
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
      addTexture(whichBreed, trl);
    }
  }


  @SubscribeEvent
  public void registerModels(ModelRegistryEvent event) {
    setCustomResourceLocations(ModItems.DRAGON_HATCHABLE_EGG);
  }

  /**
   * This event is a convenient time to load the egg models
   * @param event
   */
  @SubscribeEvent
  public void onModelBakeEvent(ModelBakeEvent event) {
    for (ResourceLocation rl : ImmutableSet.copyOf(breedModelRLs.values())) {
      try {
        WavefrontObject model = new WavefrontObject(rl);
        breedModelOBJs.put(rl, model);
      } catch (Exception e) {
        DragonMounts.logger.warn(String.format("Exception loading model %s : ", rl, e.getMessage()));
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

  private void addTexture(DragonBreedNew breed, ResourceLocation rl) {
    if (breedTextureRLs.containsKey(breed)) {
      DragonMounts.loggerLimit.warn_once("Called addResourceLocation twice for same breed + state");
      return;
    }
    breedTextureRLs.put(breed, rl);
  }

  private static final DragonVariantTag EGG_ITEM_MODEL = DragonVariantTag.addTag("eggmodelobj", "models/item/dragon_hatchable_egg.obj");
  private static final DragonVariantTag EGG_ITEM_MODEL_TEXTURE = DragonVariantTag.addTag("eggmodeltexture", "textures/items/eggs/egg_default.png");
  private static final DragonVariantTag EGG_ITEM_MODEL_SMASHED = DragonVariantTag.addTag("eggmodelsmashedobj", "models/item/dragon_hatchable_egg_smashed.obj");
  private static final DragonVariantTag EGG_ITEM_MODEL_HATCHED = DragonVariantTag.addTag("eggmodelhatchedobj", "models/item/dragon_hatchable_egg_hatched.obj");

  private final int BASE_MODEL_METADATA = 0;
  private Map<DragonBreedNew, ResourceLocation> breedTextureRLs = new HashMap<>();

  private enum InternalState {INIT, HAVE_VALIDATED, REGISTERED_MODELS};
  private InternalState internalState = InternalState.INIT;  // just for debugging / assertion

  private Map<Pair<DragonBreedNew, EggModelState>, ResourceLocation> breedModelRLs = new HashMap<>();
  private Map<ResourceLocation, WavefrontObject> breedModelOBJs = new HashMap<>();
}
