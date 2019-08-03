package com.TheRPGAdventurer.ROTD.client.render.dragon.breathweaponFX;

import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeP;
import com.TheRPGAdventurer.ROTD.util.debugging.testclasses.DebugBreathFXSettings;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Created by TGG on 7/12/2015.
 */
public class BreathWeaponFXEmitterFire extends BreathWeaponFXEmitter {
  // for prototype breath weapons
  @Override
  public void spawnBreathParticles(World world, BreathNodeP.Power power, int tickCount) {
    int FIRE_PARTICLES_PER_TICK = 4;
    if (DebugBreathFXSettings.isSpawnSingleOnly()) {
      if (!DebugBreathFXSettings.okToSpawnSingle()) return;
      FIRE_PARTICLES_PER_TICK = 1;
    }
    spawnMultipleWithSmoothedDirection(world, power, FIRE_PARTICLES_PER_TICK, tickCount);
  }

  @Override
  protected BreathFX createSingleParticle(World world, Vec3d spawnOrigin, Vec3d spawnDirection, BreathNodeP.Power power,
                                          int tickCount, float partialTickHeadStart) {
    BreathFXFire breathFXFire = BreathFXFire.createBreathFXFire(world,
            spawnOrigin.x, spawnOrigin.y, spawnOrigin.z,
            spawnDirection.x, spawnDirection.y, spawnDirection.z,
            power,
            partialTickHeadStart);
    return breathFXFire;
  }

}
