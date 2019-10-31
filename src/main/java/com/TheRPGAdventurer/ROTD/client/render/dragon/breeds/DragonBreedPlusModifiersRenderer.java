/*
** 2016 March 07
**
** The author disclaims copyright to this source code. In place of
** a legal notice, here is a blessing:
**    May you do good and not evil.
**    May you find forgiveness for yourself and forgive others.
**    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.client.render.dragon.breeds;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.model.AnimatedTexture;
import com.TheRPGAdventurer.ROTD.client.model.dragon.DragonModel;
import com.TheRPGAdventurer.ROTD.client.model.wavefrontparser.WavefrontObject;
import com.TheRPGAdventurer.ROTD.client.render.dragon.DragonRenderer;
import com.TheRPGAdventurer.ROTD.client.render.dragon.layer.*;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.EnumDragonBreed;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonPhysicalModel;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

/**
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 *
 *   The renderer doesn't contain any information about the breed; it depends fully on the configuration.
 */
public class DragonBreedPlusModifiersRenderer {

  public DragonBreedPlusModifiersRenderer(DragonRenderer parent, DragonPhysicalModel dragonPhysicalModel) {
    renderer = parent;
    model = new DragonModel(dragonPhysicalModel);

    // standard layers
    layers.add(new LayerRendererDragonGlow(parent, this, model));
//        layers.add(new LayerRendererDragonGlowAnim(parent, this, model));
    layers.add(new LayerRendererDragonSaddle(parent, this, model));
    layers.add(new LayerRendererDragonArmor(parent, this, model));
    layers.add(new LayerRendererDragonChest(parent, this, model));
    layers.add(new LayerRendererDragonBanner(parent, this, model));


//    // textures
//    String skin = breed.getBreed().getSkin();
//    maleBodyTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/bodym.png");
//    maleGlowTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/glowm.png");
//    hmaleBodyTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/hbodym.png");
//    hmaleGlowTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/hglowm.png");
//    aMaleBodyTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/abodym.png");
//    aMaleGlowTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/aglowm.png");
//    a_hMaleBodyTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/a_hbodym.png");
//    a_hMaleGlowTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/a_hglowm.png");
//
//    femaleBodyTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/bodyfm.png");
//    femaleGlowTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/glowfm.png");
//    hfemaleBodyTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/hbodyfm.png");
//    hfemaleGlowTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/hglowfm.png");
//    aFemaleBodyTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/abodyfm.png");
//    aFemaleGlowTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/aglowfm.png");
//    a_hFemaleBodyTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/a_hbodyfm.png");
//    a_hFemaleGlowTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/a_hglowfm.png");

    glowAnimTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/glow_anim.png");
    saddleTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/saddle.png");
    dissolveTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + "dissolve.png");
    chestTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/chest.png");



  }

  public List<LayerRenderer<EntityTameableDragon>> getLayers() {
    return Collections.unmodifiableList(layers);
  }

  public DragonRenderer getRenderer() {
    return renderer;
  }

  public DragonModel getModel() {
    return model;
  }

  public ResourceLocation getBodyTexture() {
    return bodyTexture;
  }

  public ResourceLocation getGlowTexture() {
    return glowTexture;
  }

  public ResourceLocation getGlowAnimTexture() {
    return glowAnimTexture;
  }

  public ResourceLocation getSaddleTexture() {
    return saddleTexture;
  }

//  @Override
//  public ResourceLocation getEggTexture() {
//    return eggTexture;
//  }

  public ResourceLocation getDissolveTexture() {
    return dissolveTexture;
  }

  public ResourceLocation getChestTexture() {
    return chestTexture;
  }

  public ResourceLocation getArmorTexture() {
    return null;
  }


  /**
   * Initialise all the configuration tags used by this helper
   */
  public static void registerConfigurationTags()
  {
    // dummy method for the tags themselves -the initialisation is all done in static initialisers
    DragonVariants.addVariantTagValidator(new DragonRendererValidator());
  }

  /**
   * Validates the following aspects of the tags:
   * - none-
   * Reads the various model and textures in, for later registration
   */
  public static class DragonRendererValidator implements DragonVariants.VariantTagValidator {
    @Override
    public void initaliseResources(DragonVariants dragonVariants, DragonVariants.ModifiedCategory modifiedCategory) throws IllegalArgumentException {
      if (!modifiedCategory.getCategory().equals(DragonVariants.Category.PHYSICAL_MODEL)) return;
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

  private void addTexture(DragonBreedNew breed, DragonVariants.ModifiedCategory modifiedCategory, EggModelState eggModelState, ResourceLocation rl) {
    TripleKey tripleKey = new TripleKey(breed, modifiedCategory, eggModelState);
    if (breedTextureRLs.containsKey(tripleKey)) {
      DragonMounts.loggerLimit.warn_once("Called addResourceLocation twice for same breed + modified category + state");
      return;
    }
    breedTextureRLs.put(tripleKey, rl);
  }
  private Map<TripleKey, ResourceLocation> breedTextureRLs = new HashMap<>();

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


  protected final List<LayerRenderer<EntityTameableDragon>> layers = new ArrayList<>();
  private final DragonRenderer renderer;
  private final DragonModel model;
//  private final ResourceLocation maleBodyTexture;
//  private final ResourceLocation maleGlowTexture;
//  private final ResourceLocation hmaleBodyTexture;
//  private final ResourceLocation hmaleGlowTexture;
//  private final ResourceLocation aMaleBodyTexture;
//  private final ResourceLocation aMaleGlowTexture;
//  private final ResourceLocation a_hMaleBodyTexture;
////    private final ResourceLocation armorTexture;
//  private final ResourceLocation a_hMaleGlowTexture;
//  private final ResourceLocation femaleBodyTexture;
//  private final ResourceLocation femaleGlowTexture;
//  private final ResourceLocation hfemaleBodyTexture;
//  private final ResourceLocation hfemaleGlowTexture;
//  private final ResourceLocation aFemaleBodyTexture;
//  private final ResourceLocation aFemaleGlowTexture;
//  private final ResourceLocation a_hFemaleBodyTexture;
//  private final ResourceLocation a_hFemaleGlowTexture;

  private final ResourceLocation bodyTexture;
  private final ResourceLocation glowTexture;
  private final ResourceLocation glowAnimTexture;
  private final ResourceLocation saddleTexture;
//  private final ResourceLocation eggTexture;
  private final ResourceLocation dissolveTexture;
  private final ResourceLocation chestTexture;

  private static final DragonVariantTag BODY_TEXTURE = DragonVariantTag.addTag("basetexture", "textures/entities/dragon/defaultbreed/body.png",
          "the texture used for the base body texture").categories(DragonVariants.Category.PHYSICAL_MODEL);
  private static final DragonVariantTag GLOW_TEXTURE = DragonVariantTag.addTag("glowtexture", "textures/entities/dragon/defaultbreed/glow.png",
          "the texture used for the glowing parts of the dragon (eg eyes)").categories(DragonVariants.Category.PHYSICAL_MODEL);
  private static final DragonVariantTag SADDLE_TEXTURE = DragonVariantTag.addTag("saddletexture", "textures/entities/dragon/defaultbreed/saddle.png",
          "the texture used for the saddle worn by the dragon").categories(DragonVariants.Category.PHYSICAL_MODEL);
  private static final DragonVariantTag CHEST_TEXTURE = DragonVariantTag.addTag("chesttexture", "textures/entities/dragon/defaultbreed/chest.png",
          "the texture used for saddlebags/chest carried by the dragon").categories(DragonVariants.Category.PHYSICAL_MODEL);
  private static final DragonVariantTag DISSOLVE_TEXTURE = DragonVariantTag.addTag("dissolvetexture", "textures/entities/dragon/defaultbreed/dissolve.png",
          "the texture used to make the dragon 'dissolve away'' when it dies").categories(DragonVariants.Category.PHYSICAL_MODEL);

// todo later: implement texture as optional age-based array to allow for texture changing as dragon matures

}
