package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.nodes;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.DragonBreathMode;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.IEntityParticle;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Created by TGG on 30/07/2015.
 * BreathNode Legacy
 */
public class BreathNodeLegacy extends BreathNodeP
{
  public BreathNodeLegacy(Power i_power, DragonBreathMode i_dragonBreathMode)
  {
    super(i_power, i_dragonBreathMode, LEGACY_INITIAL_SPEED, LEGACY_NODE_DIAMETER_IN_BLOCKS, LEGACY_DEFAULT_AGE_IN_TICKS);
  }

  public static class BreathNodeLegacyFactory implements BreathNodeFactory
  {
    @Override
    public BreathNodeP createBreathNode(Power i_power, DragonBreathMode dragonBreathMode)
    {
      return new BreathNodeLegacy(i_power, DragonBreathMode.DEFAULT);
    }
  }

  private static final float LEGACY_INITIAL_SPEED = 1.2F; // blocks per tick at full speed
  private static final float LEGACY_NODE_DIAMETER_IN_BLOCKS = 2.0F;
  private static final int LEGACY_DEFAULT_AGE_IN_TICKS = 40;
  private static final double SPEED_VARIATION_ABS = 0.1;  // plus or minus this amount (3 std deviations)

  /**
   * Update the age of the node based on what is happening (collisions) to the associated entity
   * Should be called once per tick
   * @param parentEntity the entity associated with this node
   * @param currentAge the current age of the entity (ticks)
   * @return the new age of the entity
   */
  @Override
  protected float calculateNewAge(IEntityParticle parentEntity, float currentAge)
  {
    if (parentEntity.isInWater()) {  // extinguish in water
      ageTicks = getMaxLifeTime() + 1;
      return ageTicks;
    }

    if (ageTicks++ > getMaxLifeTime()) {
      return ageTicks;
    }

    // collision ages breath node faster
    if (parentEntity.isCollided()) {
      ageTicks += 5;
    }

    // slow breath nodes age very fast (they look silly when sitting still)
    final double SPEED_THRESHOLD = getStartingSpeed() * 0.25;
    double speedSQ = parentEntity.getSpeedSQ();
    if (speedSQ < SPEED_THRESHOLD * SPEED_THRESHOLD) {
      ageTicks += 20;
    }
    return ageTicks;
  }

  @Override
  public float getCurrentDiameterOfEffect()
  {
    return getConstantSizeBeamDiameter();
  }

  @Override
  public Vec3d getRandomisedStartingMotion(Vec3d initialDirection, Random rand)
  {
    return getRandomisedStartingMotion(initialDirection, rand, SPEED_VARIATION_ABS);
  }

  @Override
  protected void setPower(Power newPower) {
    power = newPower;
    switch (newPower) {
      case SMALL: {
        speedPowerFactor = 1.5F;
        lifetimePowerFactor = 0.35F;
        sizePowerFactor = 0.35F;
        intensityPowerFactor = 1.2F;
        break;
      }
      case MEDIUM: {
        speedPowerFactor = 3.95F;
        lifetimePowerFactor = 0.7F;
        sizePowerFactor = 2.0F;
        intensityPowerFactor = 1.2F;
        break;
      }
      case LARGE: {
        speedPowerFactor = 4.0F;
        lifetimePowerFactor = 0.8F;
        sizePowerFactor = 3.25F;
        intensityPowerFactor = 1.4F;
        break;
      }

      default: {
        DragonMounts.loggerLimit.error_once("Invalid power in setPower:" + newPower);
      }
    }
  }

}
