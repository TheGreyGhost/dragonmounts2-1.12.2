package com.TheRPGAdventurer.ROTD.common.entity.breeds;

import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeP;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class DragonBreedSunlight extends DragonBreed {

  @Override
  public void onEnable(EntityTameableDragon dragon) {
  }

  @Override
  public void onDisable(EntityTameableDragon dragon) {
  }

  @Override
  public void onDeath(EntityTameableDragon dragon) {
  }

  @Override
  public void onLivingUpdate(EntityTameableDragon dragon) {
    if (dragon.posY > dragon.world.getHeight() + 8 && dragon.world.isDaytime()) doParticles(dragon);
  }

  @Override
  public void continueAndUpdateBreathingLegacy(World world, Vec3d origin, Vec3d endOfLook, BreathNodeP.Power power, EntityTameableDragon dragon) {
    dragon.getBreathHelperP().getBreathAffectedAreaFire().continueBreathingLegacy(world, origin, endOfLook, power, dragon);
    dragon.getBreathHelperP().getBreathAffectedAreaFire().updateTickLegacy(world);
  }

  @Override
  public void spawnBreathParticles(World world, BreathNodeP.Power power, int tickCounter, Vec3d origin, Vec3d endOfLook, EntityTameableDragon dragon) {
    dragon.getBreathHelperP().getEmitter().setBeamEndpoints(origin, endOfLook);
    dragon.getBreathHelperP().getEmitter().spawnBreathParticlesForFireDragon(world, power, tickCounter);
  }

  private void doParticles(EntityTameableDragon dragon) {
    if (!dragon.isEgg() && !dragon.isBaby()) {
      float s = dragon.getAgeScale() * 1.2f;
      for (double x1 = 0; x1 < s + 2; ++x1) {
        double x = dragon.posX + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;
        double y = dragon.posY + (rand.nextDouble() - 0.5) * dragon.height * s;
        double z = dragon.posZ + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;

        dragon.world.spawnParticle(EnumParticleTypes.CRIT, x, y, z, 0, 0, 0);
      }
    }
  }

  DragonBreedSunlight() {
    super("sunlight", 0Xffde00);

    setHabitatBlock(Blocks.DAYLIGHT_DETECTOR);
    setHabitatBlock(Blocks.GLOWSTONE);
    setHabitatBlock(Blocks.YELLOW_GLAZED_TERRACOTTA);
  }


}
