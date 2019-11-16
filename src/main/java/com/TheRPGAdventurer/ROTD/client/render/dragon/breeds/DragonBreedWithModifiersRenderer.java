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
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedWithModifiers;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonPhysicalModel;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.Modifiers;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.NotImplementedException;

import java.awt.image.BufferedImage;
import java.util.*;

/**
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 *
 *   The renderer doesn't contain any information about the breed; it depends fully on the configuration.
 */
public class DragonBreedWithModifiersRenderer {

  public DragonBreedWithModifiersRenderer(DragonRenderer parent, DragonPhysicalModel dragonPhysicalModel,
                                          DragonBreedWithModifiers dragonBreedWithModifiers) {
    renderer = parent;
    model = new DragonModel(dragonPhysicalModel);

    // standard layers
    layers.add(new LayerRendererDragonGlow(parent, this, model));
//        layers.add(new LayerRendererDragonGlowAnim(parent, this, model));
    layers.add(new LayerRendererDragonSaddle(parent, this, model));
    layers.add(new LayerRendererDragonArmor(parent, this, model));
    layers.add(new LayerRendererDragonChest(parent, this, model));
    layers.add(new LayerRendererDragonBanner(parent, this, model));

    DragonVariants dragonVariants = dragonBreedWithModifiers.dragonBreedNew.getDragonVariants();
    DragonVariants.ModifiedCategory mc = new DragonVariants.ModifiedCategory(DragonVariants.Category.PHYSICAL_MODEL, dragonBreedWithModifiers.modifiers);

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

//    glowAnimTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/glow_anim.png");
//    saddleTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/saddle.png");
//    dissolveTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + "dissolve.png");
//    chestTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/chest.png");

    glowTexture = new ResourceLocation(DragonMounts.MODID, (String)dragonVariants.getValueOrDefault(mc, GLOW_TEXTURE));
    saddleTexture = new ResourceLocation(DragonMounts.MODID, (String)dragonVariants.getValueOrDefault(mc, SADDLE_TEXTURE));
    dissolveTexture = new ResourceLocation(DragonMounts.MODID, (String)dragonVariants.getValueOrDefault(mc, DISSOLVE_TEXTURE));
    chestTexture = new ResourceLocation(DragonMounts.MODID, (String)dragonVariants.getValueOrDefault(mc, CHEST_TEXTURE));
    bodyTexture = new ResourceLocation(DragonMounts.MODID, (String)dragonVariants.getValueOrDefault(mc, BODY_TEXTURE));
    glowAnimTexture = null; // not implemented yet
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
    throw new NotImplementedException("GlowAnimTexture not implemented");
//    return glowAnimTexture;
  }

  public ResourceLocation getSaddleTexture() {
    return saddleTexture;
  }

  public ResourceLocation getDissolveTexture() {
    return dissolveTexture;
  }

  public ResourceLocation getChestTexture() {
    return chestTexture;
  }

  public ResourceLocation getArmorTexture() {
    throw new NotImplementedException("ArmorTexture not implemented");
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
//      if (internalState != InternalState.INIT && internalState != InternalState.HAVE_INITIALISED_RESOURCES) {
//        DragonMounts.loggerLimit.error_once("Wrong call order for DragonRendererValidator: initaliseResources after Model registration");
//      }
//      internalState = InternalState.HAVE_INITIALISED_RESOURCES;
      DragonBreedNew whichBreed =  DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getBreed(dragonVariants);

      String str;
      ResourceLocation trl;
      if (dragonVariants.tagIsExplictlyApplied(modifiedCategory, BODY_TEXTURE)) {
        str = (String)dragonVariants.getValueOrDefault(modifiedCategory, BODY_TEXTURE);
        trl = new ResourceLocation("dragonmounts", str);
        addTexture(whichBreed, modifiedCategory, trl);
      }
      if (dragonVariants.tagIsExplictlyApplied(modifiedCategory, GLOW_TEXTURE)) {
        str = (String)dragonVariants.getValueOrDefault(modifiedCategory, GLOW_TEXTURE);
        trl = new ResourceLocation("dragonmounts", str);
        addTexture(whichBreed, modifiedCategory, trl);
      }
      if (dragonVariants.tagIsExplictlyApplied(modifiedCategory, SADDLE_TEXTURE)) {
        str = (String)dragonVariants.getValueOrDefault(modifiedCategory, SADDLE_TEXTURE);
        trl = new ResourceLocation("dragonmounts", str);
        addTexture(whichBreed, modifiedCategory, trl);
      }
      if (dragonVariants.tagIsExplictlyApplied(modifiedCategory, CHEST_TEXTURE)) {
        str = (String)dragonVariants.getValueOrDefault(modifiedCategory, CHEST_TEXTURE);
        trl = new ResourceLocation("dragonmounts", str);
        addTexture(whichBreed, modifiedCategory, trl);
      }
      if (dragonVariants.tagIsExplictlyApplied(modifiedCategory, DISSOLVE_TEXTURE)) {
        str = (String)dragonVariants.getValueOrDefault(modifiedCategory, DISSOLVE_TEXTURE);
        trl = new ResourceLocation("dragonmounts", str);
        addTexture(whichBreed, modifiedCategory, trl);
      }

    }
    @Override
    public void validateVariantTags(DragonVariants dragonVariants, DragonVariants.ModifiedCategory modifiedCategory) throws IllegalArgumentException {
      // do nothing - no particular validation required
    }
  }

  /**
   * This event is a convenient time to check the model textures
   * @param event
   */
  @SubscribeEvent
  public void onModelBakeEvent(ModelBakeEvent event) {

    // attempt to load each texture; we don't actually convert to a texture now - we just verify that it can be loaded.
    for (Map.Entry<DualKey, ResourceLocation> entry : textureRLs.entrySet()) {
      try {
        IResourceManager irm = Minecraft.getMinecraft().getResourceManager();
        IResource iresource = irm.getResource(entry.getValue());
      } catch (Exception e) {
        DragonMounts.logger.warn(String.format("Exception loading texture %s for breed %s: %s",
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

  private static final DragonVariantTag BODY_TEXTURE = DragonVariantTag.addTag("basetexture", "defaultbreed/body.png",
          "the texture used for the base body texture; base path is " + DragonRenderer.TEX_BASE).categories(DragonVariants.Category.PHYSICAL_MODEL);
  private static final DragonVariantTag GLOW_TEXTURE = DragonVariantTag.addTag("glowtexture", "defaultbreed/glow.png",
          "the texture used for the glowing parts of the dragon (eg eyes); base path is" + DragonRenderer.TEX_BASE).categories(DragonVariants.Category.PHYSICAL_MODEL);
  private static final DragonVariantTag SADDLE_TEXTURE = DragonVariantTag.addTag("saddletexture", "defaultbreed/saddle.png",
          "the texture used for the saddle worn by the dragon; base path is " + DragonRenderer.TEX_BASE).categories(DragonVariants.Category.PHYSICAL_MODEL);
  private static final DragonVariantTag CHEST_TEXTURE = DragonVariantTag.addTag("chesttexture", "defaultbreed/chest.png",
          "the texture used for saddlebags/chest carried by the dragon; base path is " + DragonRenderer.TEX_BASE).categories(DragonVariants.Category.PHYSICAL_MODEL);
  private static final DragonVariantTag DISSOLVE_TEXTURE = DragonVariantTag.addTag("dissolvetexture", "defaultbreed/dissolve.png",
          "the texture used to make the dragon 'dissolve away'' when it dies; base path is " + DragonRenderer.TEX_BASE).categories(DragonVariants.Category.PHYSICAL_MODEL);

//  private enum InternalState {INIT, HAVE_INITIALISED_RESOURCES, REGISTERED_MODELS};
//  private InternalState internalState = InternalState.INIT;  // just for debugging / assertion

  private static void addTexture(DragonBreedNew breed, DragonVariants.ModifiedCategory modifiedCategory, ResourceLocation trl) {
    DualKey dualKey = new DualKey(breed, modifiedCategory);
    if (textureRLs.containsKey(dualKey)) {
      DragonMounts.loggerLimit.warn_once("Called addResourceLocation twice for same breed + modified category");
      return;
    }
    textureRLs.put(dualKey, trl);
  }

  private static class DualKey {
    public DualKey(DragonBreedNew dragonBreedNew, DragonVariants.ModifiedCategory modifiedCategory) {
      this.dragonBreedNew = dragonBreedNew;
      this.modifiedCategory = modifiedCategory;
    }

    public DragonBreedNew dragonBreedNew;
    public DragonVariants.ModifiedCategory modifiedCategory;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || !(o instanceof DualKey)) return false;
      DualKey dualKey2 = (DualKey)o;
      return this.dragonBreedNew.equals(dualKey2.dragonBreedNew) && this.modifiedCategory.equals(dualKey2.modifiedCategory);
    }
    @Override
    public int hashCode() {
      return dragonBreedNew.hashCode() ^ modifiedCategory.hashCode();
    }

  }

  private static Map<DualKey, ResourceLocation> textureRLs = new HashMap<>();

}
