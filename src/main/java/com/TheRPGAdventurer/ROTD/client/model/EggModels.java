package com.TheRPGAdventurer.ROTD.client.model;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.helper.util.Pair;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantsException;
import com.TheRPGAdventurer.ROTD.common.inits.ModItems;
import com.TheRPGAdventurer.ROTD.util.math.Interpolation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

  public IBakedModel getModel(DragonBreedNew, EggModelState eggModelState) {
    ModelResourceLocation mrl = 
    Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getModelManager().getModel()

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
      DragonBreedNew whichBreed =  DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getBreed(dragonVariants);
      DragonVariantsException.DragonVariantsErrors dragonVariantsErrors = new DragonVariantsException.DragonVariantsErrors();

      String str = (String)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_ITEM_MODEL);
      ModelResourceLocation mrl = new ModelResourceLocation("dragonmounts:" + str, "inventory");
      addModelResourceLocation(whichBreed, EggModelState.INCUBATING, mrl);

      str = (String)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_ITEM_MODEL_HATCHED);
      mrl = new ModelResourceLocation("dragonmounts:" + str, "inventory");
      addModelResourceLocation(whichBreed, EggModelState.HATCHED, mrl);

      str = (String)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_ITEM_MODEL_SMASHED);
      mrl = new ModelResourceLocation("dragonmounts:" + str, "inventory");
      addModelResourceLocation(whichBreed, EggModelState.SMASHED, mrl);

      str = (String)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_ITEM_MODEL_TEXTURE);
      ResourceLocation rl = new ResourceLocation("dragonmounts", str);
      addTexture(rl);
      internalState = InternalState.HAVE_VALIDATED;
    }
  }


  @SubscribeEvent
  public void registerModels(ModelRegistryEvent event) {
    setCustomModelResourceLocations(ModItems.DRAGON_HATCHABLE_EGG);
//    ModelLoader.setCustomModelResourceLocation(item, meta, new ModelResourceLocation(item.getRegistryName(), id));
//
//    ModelLoader.setCustomModelResourceLocation(ModItems.DRAGON_HATCHABLE_EGG, BASE_MODEL_METADATA, itemModelResourceLocation);
//
//    for (Map.Entry<ModelResourceLocation, Integer> entry : allMode)
//
//      // model to be used for rendering this item
//      ModelResourceLocation itemModelResourceLocation = new ModelResourceLocation("dragonmounts:dragon_hatchable_egg", "inventory");
//
//    BASE_MODEL_METADATA
//    final int DUMMY_ITEM_SUBTYPE = 1;
//    ModelResourceLocation objModelResourceLocation = new ModelResourceLocation("dragonmounts:dragon_hatchable_egg.obj", "inventory");
//    ModelLoader.setCustomModelResourceLocation(ModItems.DRAGON_HATCHABLE_EGG, DUMMY_ITEM_SUBTYPE, objModelResourceLocation);
  }

  public void setCustomModelResourceLocations(Item itemDragonHatchableEgg) {
    if (internalState == InternalState.INIT) {
      DragonMounts.loggerLimit.error_once("Wrong call order for EggModelsValidator: missing validation");
    }
    internalState = InternalState.REGISTERED_MODELS;
    ModelResourceLocation itemModelResourceLocation = new ModelResourceLocation("dragonmounts:dragon_hatchable_egg", "inventory");
    ModelLoader.setCustomModelResourceLocation(itemDragonHatchableEgg, BASE_MODEL_METADATA, itemModelResourceLocation);

    for (Map.Entry<ModelResourceLocation, Integer> entry : allModelsAndMetadata.entrySet()) {
      ModelLoader.setCustomModelResourceLocation(itemDragonHatchableEgg, entry.getValue(), entry.getKey());
    }
  }

//  public void registerTextures(TextureMap textureMap) {
//    for (ResourceLocation rl : allTextures) {
//      textureMap.registerSprite(rl);
//    }
//  }

  @SubscribeEvent
  public void stitcherEventPre(TextureStitchEvent.Pre event) {
    if (internalState == InternalState.INIT) {
      DragonMounts.loggerLimit.error_once("Wrong call order for EggModelsValidator: texture stitch before validation");
    }
    for (ResourceLocation rl : allTextures) {
      event.getMap().registerSprite(rl);
    }
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

  private void addModelResourceLocation(DragonBreedNew dragonBreedNew, EggModelState eggModelState, ModelResourceLocation mrl) {
    Pair<DragonBreedNew, EggModelState> key = new Pair<>(dragonBreedNew,eggModelState);
    if (modelMetadata.containsKey(key)) {
      DragonMounts.loggerLimit.warn_once("Called addModelResourceLocation twice for same breed + state");
      return;
    }
    int oldMetadata = nextMetadata;
    if (!allModelsAndMetadata.containsKey(mrl)) {
      allModelsAndMetadata.put(mrl, nextMetadata++);
    }
    modelMetadata.put(key, oldMetadata);
  }

  private void addTexture(ResourceLocation rl) {
    allTextures.add(rl);
  }

//  private static final DragonVariantTag EGG_ITEM_MODEL_BASE = DragonVariantTag.addTag("eggmodeljson", "dragon_hatchable_egg");
  private static final DragonVariantTag EGG_ITEM_MODEL = DragonVariantTag.addTag("eggmodelobj", "dragon_hatchable_egg.obj");
  private static final DragonVariantTag EGG_ITEM_MODEL_TEXTURE = DragonVariantTag.addTag("eggmodeltexture", "eggs/egg_default");
  private static final DragonVariantTag EGG_ITEM_MODEL_SMASHED = DragonVariantTag.addTag("eggmodelobj", "dragon_hatchable_egg_smashed.obj");
  private static final DragonVariantTag EGG_ITEM_MODEL_HATCHED = DragonVariantTag.addTag("eggmodelobj", "dragon_hatchable_egg_hatched.obj");

  // The EggModels uses the metadata for ItemHatchableEgg to register and load the various obj models, but uses NBT tag to
  //    choose which one to display.  The metadata itself is arbitrary / ignored.

  private Map<ModelResourceLocation, Integer> allModelsAndMetadata = new HashMap<>();
  private int nextMetadata = 1;
  private final int BASE_MODEL_METADATA = 0;
  private Set<ResourceLocation> allTextures = new HashSet<>();

  private enum InternalState {INIT, HAVE_VALIDATED, REGISTERED_MODELS};
  private InternalState internalState = InternalState.INIT;  // just for debugging / assertion

  private Map<Pair<DragonBreedNew, EggModelState>, Integer> modelMetadata = new HashMap<>();
}
