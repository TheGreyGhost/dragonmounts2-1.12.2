/*
 ** 2011 December 10
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.client.render.dragon;

import com.TheRPGAdventurer.ROTD.client.model.AnimatedTexture;
import com.TheRPGAdventurer.ROTD.client.model.EggModels;
import com.TheRPGAdventurer.ROTD.client.model.wavefrontparser.WavefrontObject;
import com.TheRPGAdventurer.ROTD.client.other.ClientTickCounter;
import com.TheRPGAdventurer.ROTD.client.render.dragon.breeds.DefaultDragonBreedRenderer;
import com.TheRPGAdventurer.ROTD.common.entity.EntityDragonEgg;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.EnumDragonBreed;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.EnumMap;
import java.util.Map;

/**
 * renderer for EntityDragonEgg
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class DragonHatchableEggRenderer extends Render<EntityDragonEgg> {

  public DragonHatchableEggRenderer(RenderManager renderManager) {
    super(renderManager);
  }

  @Override
  public void doRender(EntityDragonEgg dragonEgg, double x, double y, double z, float yaw, float partialTicks) {

    EntityDragonEgg.UserConfiguredParameters userConfiguredParameters = dragonEgg.getUserConfiguredParameters();

    // apply egg wiggle
    float tickX = dragonEgg.getEggWiggleXtickTimer() - partialTicks;
    float tickZ = dragonEgg.getEggWiggleZtickTimer() - partialTicks;

    float rotX = 0;
    float rotZ = 0;

    final float WIGGLE_PERIOD_TICKS = EntityDragonEgg.WIGGLE_DURATION_TICKS / 2;
    final float WIGGLE_AMPLITUDE_DEGREES = 8;

    if (tickX > 0) {
      float wiggleCycleRadians = 2*(float)Math.PI * tickX / WIGGLE_PERIOD_TICKS;
      rotX = WIGGLE_AMPLITUDE_DEGREES * (float)Math.sin(wiggleCycleRadians) * tickX / EntityDragonEgg.WIGGLE_DURATION_TICKS;
      if (dragonEgg.getEggWiggleInverseDirection()) rotX = -rotX;
    }
    if (tickZ > 0) {
      float wiggleCycleRadians = 2*(float)Math.PI * tickZ / WIGGLE_PERIOD_TICKS;
      rotZ = WIGGLE_AMPLITUDE_DEGREES * (float)Math.sin(wiggleCycleRadians) * tickZ / EntityDragonEgg.WIGGLE_DURATION_TICKS;
      if (dragonEgg.getEggWiggleInverseDirection()) rotZ = -rotZ;
    }

    float rotY = -dragonEgg.rotationYaw;
    double incubationTicks = dragonEgg.getIncubationTicks() + partialTicks;
    if (dragonEgg.getEggState() == EntityDragonEgg.EggState.INCUBATING
        && userConfiguredParameters.spinFlag && incubationTicks >= userConfiguredParameters.eggSpinStartTicks) {
      double spinTimeFraction = (incubationTicks - userConfiguredParameters.eggGlowStartTicks) /
                                (userConfiguredParameters.eggIncubationCompleteTicks - userConfiguredParameters.eggGlowStartTicks);
      final double INITIAL_SPIN_SPEED = 0;
      double spinSpeedDPS = MathX.lerp(INITIAL_SPIN_SPEED, userConfiguredParameters.eggSpinMaxSpeedDegPerSecond, spinTimeFraction);
      // for a spin speed which linearly increases over time, the angular distance is the integral from t=0 to now which
      //   is half the current spin speed times the duration
      final double TICKS_PER_SECOND = 20;
      double degrees = 0.5 * spinSpeedDPS * (incubationTicks - userConfiguredParameters.eggGlowStartTicks) / TICKS_PER_SECOND;
      rotY = (float)((dragonEgg.rotationYaw + degrees) % 360);
    }

    try {
      GlStateManager.pushMatrix();

      GlStateManager.translate(x, y, z);
      GlStateManager.rotate(rotY, 0, 1, 0);
      GlStateManager.rotate(rotX, 1, 0, 0);
      GlStateManager.rotate(rotZ, 0, 0, 1);
      float renderScale = dragonEgg.getRenderScale();
      GlStateManager.scale(renderScale, renderScale, renderScale);

      AnimatedTexture animatedTexture = EggModels.getInstance().getAnimatedTexture(dragonEgg.getDragonBreed(), dragonEgg.getEggState().getEggModelState());
      if (animatedTexture != null) {
        animatedTexture.updateAnimation(ClientTickCounter.getTicksSinceStart());
        animatedTexture.bindTexture();
      } else {
        AnimatedTexture.bindMissingTexture();
      }

      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferBuilder = tessellator.getBuffer();
      bufferBuilder.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_NORMAL);
      WavefrontObject wavefrontObject = EggModels.getInstance().getModel(dragonEgg.getDragonBreed(), dragonEgg.getEggState().getEggModelState());;
      wavefrontObject.tessellateAll(bufferBuilder);
      tessellator.draw();

    } finally {
      GlStateManager.popMatrix();
    }
  }

  @Override
  protected ResourceLocation getEntityTexture(EntityDragonEgg entityDragonEgg) {
    DragonBreedNew breed = entityDragonEgg.getDragonBreed();
    EggModels.EggModelState eggModelState = entityDragonEgg.getEggState().getEggModelState();
    ResourceLocation rl = EggModels.getInstance().getTexture(breed, eggModelState);
    return rl;
  }
}

