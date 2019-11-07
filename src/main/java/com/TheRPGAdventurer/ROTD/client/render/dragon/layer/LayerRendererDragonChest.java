package com.TheRPGAdventurer.ROTD.client.render.dragon.layer;

import com.TheRPGAdventurer.ROTD.client.model.dragon.DragonModel;
import com.TheRPGAdventurer.ROTD.client.render.dragon.DragonRenderer;
import com.TheRPGAdventurer.ROTD.client.render.dragon.breeds.DragonBreedWithModifiersRenderer;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;

public class LayerRendererDragonChest extends LayerRendererDragon {

  public LayerRendererDragonChest(DragonRenderer renderer, DragonBreedWithModifiersRenderer breedRenderer, DragonModel model) {
    super(renderer, breedRenderer, model);
  }

  @Override
  public void doRenderLayer(EntityTameableDragon dragon, float moveTime,
                            float moveSpeed, float partialTicks, float ticksExisted, float lookYaw,
                            float lookPitch, float scale) {
    if (dragon.isChested()) {
      renderer.bindTexture(breedRenderer.getChestTexture());
      model.render(dragon, moveTime, moveSpeed, ticksExisted, lookYaw, lookPitch, scale);
    }
  }

  @Override
  public boolean shouldCombineTextures() {
    return false;
  }
}