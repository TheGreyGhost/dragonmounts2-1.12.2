package com.TheRPGAdventurer.ROTD.client.render.dragon.layer;

import com.TheRPGAdventurer.ROTD.client.model.dragon.DragonModel;
import com.TheRPGAdventurer.ROTD.client.model.dragon.DragonModelMode;
import com.TheRPGAdventurer.ROTD.client.render.dragon.DragonRenderer;
import com.TheRPGAdventurer.ROTD.client.render.dragon.breeds.DefaultDragonBreedRenderer;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.EnumDragonBreed;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import net.minecraft.client.renderer.GlStateManager;

import static org.lwjgl.opengl.GL11.GL_ONE;

/**
 * Created by EveryoneElse on 14/06/2015.
 */
public class LayerRendererDragonGlow extends LayerRendererDragon {

  public LayerRendererDragonGlow(DragonRenderer renderer, DefaultDragonBreedRenderer breedRenderer, DragonModel model) {
    super(renderer, breedRenderer, model);
  }

  @Override
  public void doRenderLayer(EntityTameableDragon dragon, float moveTime, float moveSpeed, float partialTicks, float ticksExisted, float lookYaw, float lookPitch, float scale) {
//    renderer.bindTexture(dragon.isMale() ? breedRenderer.getMaleGlowTexture(dragon.isBaby(), false) : breedRenderer.getFemaleGlowTexture(dragon.isBaby(), false));
    renderer.bindTexture(dragon.isMale() ? breedRenderer.getMaleGlowTexture(false, false) : breedRenderer.getFemaleGlowTexture(false, false));

    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL_ONE, GL_ONE);
    GlStateManager.color(1, 1, 1, 1);

        /*if (!dragon.isAsleep)*/
    disableLighting();
    model.setMode(DragonModelMode.FULL);
    model.render(dragon, moveTime, moveSpeed, ticksExisted, lookYaw, lookPitch, scale);
    enableLighting(dragon.getBrightnessForRender());

    GlStateManager.disableBlend();

//    if (dragon.getBreedType() == EnumDragonBreed.ENCHANT) { //todo
//      renderEnchantedGlint(this.renderer, dragon, model, moveTime, moveSpeed, partialTicks, ticksExisted, lookYaw, lookPitch, scale);
//    }
  }

  @Override
  public boolean shouldCombineTextures() {
    return false;
  }
}
