package com.TheRPGAdventurer.ROTD.client.render.dragon.breathweaponFX;

import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.nodes.BreathNodeP;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Created by TGG on 7/12/2015.
 */
public class BreathWeaponFXEmitterFire extends BreathWeaponFXEmitter
{
  public void spawnBreathParticles(World world, BreathNodeP.Power power, int tickCount)
  {
    final int FIRE_PARTICLES_PER_TICK = 4;
    spawnMultipleWithSmoothedDirection(world, power, FIRE_PARTICLES_PER_TICK, tickCount);
  }

  @Override
  protected BreathFX createSingleParticle(World world, Vec3d spawnOrigin, Vec3d spawnDirection, BreathNodeP.Power power,
                                           int tickCount, float partialTickHeadStart)
  {
    BreathFXFire breathFXFire = BreathFXFire.createBreathFXFire(world,
            spawnOrigin.xCoord, spawnOrigin.yCoord, spawnOrigin.zCoord,
            spawnDirection.xCoord, spawnDirection.yCoord, spawnDirection.zCoord,
            power,
            partialTickHeadStart, debugBreathFXSettings);
    return breathFXFire;
  }

}
