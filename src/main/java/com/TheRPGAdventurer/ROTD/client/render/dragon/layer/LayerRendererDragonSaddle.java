package com.TheRPGAdventurer.ROTD.client.render.dragon.layer;

import com.TheRPGAdventurer.ROTD.client.model.dragon.DragonModel;
import com.TheRPGAdventurer.ROTD.client.render.dragon.DragonRenderer;
import com.TheRPGAdventurer.ROTD.client.render.dragon.breeds.DragonBreedWithModifiersRenderer;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;

/**
 * Created by EveryoneElse on 14/06/2015.
 */
public class LayerRendererDragonSaddle extends LayerRendererDragon {

  public LayerRendererDragonSaddle(DragonRenderer renderer,
                                   DragonBreedWithModifiersRenderer breedRenderer, DragonModel model) {
    super(renderer, breedRenderer, model);
  }

  @Override
  public void doRenderLayer(EntityTameableDragon dragon, float moveTime,
                            float moveSpeed, float partialTicks, float ticksExisted, float lookYaw,
                            float lookPitch, float scale) {
    if (!dragon.isSaddled()) {
      return;
    }
    renderer.bindTexture(breedRenderer.getSaddleTexture());
    model.render(dragon, moveTime, moveSpeed, ticksExisted, lookYaw, lookPitch, scale);
  }

  @Override
  public boolean shouldCombineTextures() {
    return false;
  }
}
