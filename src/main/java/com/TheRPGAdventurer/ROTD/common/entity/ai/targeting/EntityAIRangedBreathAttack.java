/*
 ** 2012 April 22
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
/**
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */

package com.TheRPGAdventurer.ROTD.common.entity.ai.targeting;


import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.breath.BreathWeaponTarget;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Selects the appropriate target based on the DragonOrb targeting information.  Applies Autolock if requested.
 * Once the target is at the correct range, breathe at it.  Bite if the dragon can't move far enough away.
 *   (Moving towards the target is handled by a different AI task)
 */

public class EntityAIRangedBreathAttack extends EntityAIBase {
  public EntityAIRangedBreathAttack(EntityTameableDragon i_dragon,
                                    float i_minAttackDistance, float i_optimalAttackDistance, float i_maxAttackDistance) {
    this.dragon = i_dragon;
    this.minAttackDistanceSQ = i_minAttackDistance * i_minAttackDistance;
    this.maxAttackDistanceSQ = i_maxAttackDistance * i_maxAttackDistance;
    this.setMutexBits(1);
  }

  /**
   * Returns whether the EntityAIBase should begin execution.
   */
  @Override
  public boolean shouldExecute() {
    BreathWeaponTarget playerSelectedTarget = this.dragon.getBreathHelperP().getPlayerSelectedTarget();
    return playerSelectedTarget != null || currentTarget != null;
  }

  /**
   * Returns whether an in-progress EntityAIBase should continue executing
   */
  @Override
  public boolean shouldContinueExecuting() {
    return this.shouldExecute() || !this.dragon.getNavigator().noPath();
  }

  /**
   * Resets the task
   */
  @Override
  public void resetTask() {
    currentTarget = null;
    dragon.getBreathHelperP().setBreathingTarget(null);
    dragon.getBreathHelperP().setBreathTargetForMoving(null);
    dragon.setAttackTarget(null);
  }

  /**
   * Updates the task
   */
  @Override
  public void updateTask() {
    // check which target the player has selected; if deselected, wait a short while before losing interest
    // if autolock is on, only change target when the player releases the button
    final int TARGET_DESELECTION_TIME = 60; // 60 ticks until dragon loses interest in target
    BreathWeaponTarget playerSelectedTarget = this.dragon.getBreathHelperP().getPlayerSelectedTarget();
    boolean breathingNow = (playerSelectedTarget != null);
    boolean orbTargetAutoLock = DragonMounts.instance.getConfig().isOrbTargetAutoLock();
    if (playerSelectedTarget != null) {
      if (!orbTargetAutoLock || currentTarget == null) {
        currentTarget = playerSelectedTarget;
        targetDeselectedCountDown = TARGET_DESELECTION_TIME;
      }
    } else {
      if (targetDeselectedCountDown <= 0 || orbTargetAutoLock) {
        currentTarget = null;
      } else {
        --targetDeselectedCountDown;
      }
    }

    if (currentTarget == null) {
      dragon.getBreathHelperP().setBreathingTarget(null);
      dragon.getBreathHelperP().setBreathTargetForMoving(null);
      dragon.setAttackTarget(null);
      return;
    }

    double distanceToTargetSQ = currentTarget.distanceSQtoTarget(dragon.world, dragon.getPositionVector());

    // If the target is too close but the dragon can't back up (stays too close for more than a few seconds), bite.
    // Uses the EntityAIAttackOnCollide AI to do this.
    final int BITE_MODE_TICKS = 80;
    if (distanceToTargetSQ < minAttackDistanceSQ && distanceToTargetSQ >= 0) {
      ++ticksBelowMinimumRange;
    } else {
      ticksBelowMinimumRange = 0;
    }

    boolean biteMode = (ticksBelowMinimumRange >= BITE_MODE_TICKS);
    if (biteMode && currentTarget.getTypeOfTarget() == BreathWeaponTarget.TypeOfTarget.ENTITY) {
      EntityLivingBase owner = dragon.getOwner();
      if (owner == null) {
        biteMode = false;
      } else {
        Entity targetEntity = currentTarget.getTargetEntity(dragon.getEntityWorld());
        if (!(targetEntity instanceof EntityLivingBase)) {
          biteMode = false;
        } else {
          dragon.setAttackTarget((EntityLivingBase) targetEntity);
          dragon.getBreathHelperP().setBreathTargetForMoving(null);
        }
      }
    }

    if (!biteMode) {
      dragon.getBreathHelperP().setBreathTargetForMoving(currentTarget);
    }

    // breathe at the target if the conditions are right

    boolean targetRangeOK = distanceToTargetSQ < 0
            || (distanceToTargetSQ >= minAttackDistanceSQ && distanceToTargetSQ <= maxAttackDistanceSQ);

    boolean headAngleOK = areHeadAnglesWithinTolerance();
    boolean canSeeTarget = true;
    if (currentTarget.getTypeOfTarget() == BreathWeaponTarget.TypeOfTarget.ENTITY) {
      Entity targetEntity = currentTarget.getTargetEntity(dragon.world);
      canSeeTarget = (targetEntity != null) && dragon.getEntitySenses().canSee(targetEntity);
    }
    if (breathingNow && canSeeTarget && targetRangeOK && headAngleOK) {
      dragon.getBreathHelperP().setBreathingTarget(currentTarget);
    } else {
      dragon.getBreathHelperP().setBreathingTarget(null);
    }
  }

  /**
   * Check the head yaw and pitch to verify it matches the beam weapon within acceptable limits
   * @return
   */
  private boolean areHeadAnglesWithinTolerance() {
    Vec3d origin = dragon.getAnimator().getThroatPosition();
    Vec3d targetedPoint = currentTarget.getTargetedPoint(dragon.world, origin);
    if (targetedPoint == null) return false;
    double deltaX = targetedPoint.x - origin.x;
    double deltaY = targetedPoint.y - origin.y;
    double deltaZ = targetedPoint.z - origin.z;
    double xzProjectionLength = MathHelper.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    double yaw = (Math.atan2(deltaZ, deltaX) * 180.0D / Math.PI) - 90.0F;
    double pitch = (-(Math.atan2(deltaY, xzProjectionLength) * 180.0D / Math.PI));
    double yawDeviation = MathX.normDeg(yaw - dragon.getRotationYawHead());
    double pitchDeviation = MathX.normDeg(pitch - dragon.rotationPitch);
    final double YAW_ANGLE_TOLERANCE = 20;
    final double PITCH_ANGLE_TOLERANCE = 20;

    return (Math.abs(yawDeviation) <= YAW_ANGLE_TOLERANCE
            && Math.abs(pitchDeviation) <= PITCH_ANGLE_TOLERANCE);
  }
  /** The entity the AI instance has been applied to */
  private final EntityTameableDragon dragon;
  private float minAttackDistanceSQ;
  private float maxAttackDistanceSQ;
  private int targetDeselectedCountDown = 0;  // Countdown after player deselects target
  private BreathWeaponTarget currentTarget = null;
  private int ticksBelowMinimumRange = 0;
}
