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
import com.TheRPGAdventurer.ROTD.client.model.dragon.DragonModel;
import com.TheRPGAdventurer.ROTD.client.render.dragon.DragonRenderer;
import com.TheRPGAdventurer.ROTD.client.render.dragon.layer.*;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.EnumDragonBreed;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class DefaultDragonBreedRenderer implements DragonBreedRenderer {

  public DefaultDragonBreedRenderer(DragonRenderer parent, EnumDragonBreed breed) {
    renderer = parent;
    model = new DragonModel(breed);

    // standard layers
    layers.add(new LayerRendererDragonGlow(parent, this, model));
//        layers.add(new LayerRendererDragonGlowAnim(parent, this, model));
    layers.add(new LayerRendererDragonSaddle(parent, this, model));
    layers.add(new LayerRendererDragonArmor(parent, this, model));
    layers.add(new LayerRendererDragonChest(parent, this, model));
    layers.add(new LayerRendererDragonBanner(parent, this, model));


    // textures
    String skin = breed.getBreed().getSkin();
    maleBodyTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/bodym.png");
    maleGlowTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/glowm.png");
    hmaleBodyTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/hbodym.png");
    hmaleGlowTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/hglowm.png");
    aMaleBodyTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/abodym.png");
    aMaleGlowTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/aglowm.png");
    a_hMaleBodyTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/a_hbodym.png");
    a_hMaleGlowTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/a_hglowm.png");

    femaleBodyTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/bodyfm.png");
    femaleGlowTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/glowfm.png");
    hfemaleBodyTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/hbodyfm.png");
    hfemaleGlowTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/hglowfm.png");
    aFemaleBodyTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/abodyfm.png");
    aFemaleGlowTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/aglowfm.png");
    a_hFemaleBodyTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/a_hbodyfm.png");
    a_hFemaleGlowTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/a_hglowfm.png");

    glowAnimTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/glow_anim.png");
    saddleTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/saddle.png");
    eggTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/egg.png");
    dissolveTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + "dissolve.png");
    chestTexture = new ResourceLocation(DragonMounts.MODID, DragonRenderer.TEX_BASE + skin + "/chest.png");
  }

  @Override
  public List<LayerRenderer<EntityTameableDragon>> getLayers() {
    return Collections.unmodifiableList(layers);
  }

  @Override
  public DragonRenderer getRenderer() {
    return renderer;
  }

  @Override
  public DragonModel getModel() {
    return model;
  }

  @Override
  public ResourceLocation getMaleBodyTexture(boolean hatchling, boolean albino) {
    return hatchling ? (albino ? a_hMaleBodyTexture : hmaleBodyTexture) : albino ? aMaleBodyTexture : maleBodyTexture;
  }

  @Override
  public ResourceLocation getFemaleBodyTexture(boolean hatchling, boolean albino) {
    return hatchling ? (albino ? a_hFemaleBodyTexture : hfemaleBodyTexture) : albino ? aFemaleBodyTexture : femaleBodyTexture;
  }

  @Override
  public ResourceLocation getMaleGlowTexture(boolean hatchling, boolean albino) {
    return hatchling ? (albino ? a_hMaleGlowTexture : hmaleGlowTexture) : albino ? aMaleGlowTexture : maleGlowTexture;
  }

  @Override
  public ResourceLocation getFemaleGlowTexture(boolean hatchling, boolean albino) {
    return hatchling ? (albino ? a_hFemaleGlowTexture : hfemaleGlowTexture) : albino ? aFemaleGlowTexture : femaleGlowTexture;
  }

  @Override
  public ResourceLocation getGlowAnimTexture() {
    return glowAnimTexture;
  }

  @Override
  public ResourceLocation getSaddleTexture() {
    return saddleTexture;
  }

//  @Override
//  public ResourceLocation getEggTexture() {
//    return eggTexture;
//  }

  @Override
  public ResourceLocation getDissolveTexture() {
    return dissolveTexture;
  }

  @Override
  public ResourceLocation getChestTexture() {
    return chestTexture;
  }

  @Override
  public ResourceLocation getArmorTexture() {
    return null;
  }
  protected final List<LayerRenderer<EntityTameableDragon>> layers = new ArrayList<>();
  private final DragonRenderer renderer;
  private final DragonModel model;
  private final ResourceLocation maleBodyTexture;
  private final ResourceLocation maleGlowTexture;
  private final ResourceLocation hmaleBodyTexture;
  private final ResourceLocation hmaleGlowTexture;
  private final ResourceLocation aMaleBodyTexture;
  private final ResourceLocation aMaleGlowTexture;
  private final ResourceLocation a_hMaleBodyTexture;
//    private final ResourceLocation armorTexture;
  private final ResourceLocation a_hMaleGlowTexture;
  private final ResourceLocation femaleBodyTexture;
  private final ResourceLocation femaleGlowTexture;
  private final ResourceLocation hfemaleBodyTexture;
  private final ResourceLocation hfemaleGlowTexture;
  private final ResourceLocation aFemaleBodyTexture;
  private final ResourceLocation aFemaleGlowTexture;
  private final ResourceLocation a_hFemaleBodyTexture;
  private final ResourceLocation a_hFemaleGlowTexture;
  private final ResourceLocation glowAnimTexture;
  private final ResourceLocation saddleTexture;
  private final ResourceLocation eggTexture;
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
