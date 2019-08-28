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

//  /**
//   * Returns the texture sprite for the given breed
//   * @param dragonBreed
//   * @return null if not found
//   */
//  public TextureAtlasSprite getTextureAtlasSprite(DragonBreedNew dragonBreed) {
//    return breedTextures.get(dragonBreed);
//  }

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
//    ModelLoader.setCustomResourceLocation(item, meta, new ResourceLocation(item.getRegistryName(), id));
//
//    ModelLoader.setCustomResourceLocation(ModItems.DRAGON_HATCHABLE_EGG, BASE_MODEL_METADATA, itemResourceLocation);
//
//    for (Map.Entry<ResourceLocation, Integer> entry : allMode)
//
//      // model to be used for rendering this item
//      ResourceLocation itemResourceLocation = new ResourceLocation("dragonmounts:dragon_hatchable_egg", "inventory");
//
//    BASE_MODEL_METADATA
//    final int DUMMY_ITEM_SUBTYPE = 1;
//    ResourceLocation objResourceLocation = new ResourceLocation("dragonmounts:dragon_hatchable_egg.obj", "inventory");
//    ModelLoader.setCustomResourceLocation(ModItems.DRAGON_HATCHABLE_EGG, DUMMY_ITEM_SUBTYPE, objResourceLocation);
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

//  public void registerTextures(TextureMap textureMap) {
//    for (ResourceLocation rl : allTextures) {
//      textureMap.registerSprite(rl);
//    }
//  }

  /**
   * Stitches all the item textures into the item texture sheet (TextureAtlas) so we can use them later
   * @param event
   */
  @SubscribeEvent
  public void stitcherEventPre(TextureStitchEvent.Pre event) {
//    if (internalState == InternalState.INIT) {
//      DragonMounts.loggerLimit.error_once("Wrong call order for EggModelsValidator: texture stitch before validation");
//    }
//    Map<ResourceLocation, TextureAtlasSprite> addedTAS = new HashMap<>();
//    for (Map.Entry<DragonBreedNew, ResourceLocation> entry : breedTextureRLs.entrySet()) {
//      DragonBreedNew breed = entry.getKey();
//      ResourceLocation texRL = entry.getValue();
//      if (addedTAS.containsKey(texRL)) {
//        breedTextures.put(breed, addedTAS.get(texRL));
//      } else {
//        TextureAtlasSprite tas = event.getMap().registerSprite(texRL);
//        addedTAS.put(texRL, tas);
//        breedTextures.put(breed, tas);
//      }
//    }
//    ResourceLocation flameRL = new ResourceLocation("dragonmounts:entities/breathweapon/breath_fire");
//    event.getMap().registerSprite(flameRL);
//    ResourceLocation iceRL = new ResourceLocation("dragonmounts:entities/breathweapon/breath_ice");
//    event.getMap().registerSprite(iceRL);
//    ResourceLocation waterRL = new ResourceLocation("dragonmounts:entities/breathweapon/breath_water");
//    event.getMap().registerSprite(waterRL);
//    ResourceLocation airRL = new ResourceLocation("dragonmounts:entities/breathweapon/breath_air");
//    event.getMap().registerSprite(airRL);
//    ResourceLocation forestGasCloudRL = new ResourceLocation("dragonmounts:entities/breathweapon/breath_forest");
//    event.getMap().registerSprite(forestGasCloudRL);
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

//  private static final DragonVariantTag EGG_ITEM_MODEL_BASE = DragonVariantTag.addTag("eggmodeljson", "dragon_hatchable_egg");
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
