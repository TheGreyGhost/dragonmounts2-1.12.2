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

package com.TheRPGAdventurer.ROTD.common.entity.ai.ground;

import com.TheRPGAdventurer.ROTD.common.entity.breath.BreathWeaponTarget;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**Moves the dragon to the optimal distance from the breath weapon target- not too close, not too far
 * The location to move to taken from the dragon breath helper's getBreathTargetForMoving
 *   (The location is set by the targeting AI)
 */
public class EntityAIMoveToOptimalDistance extends EntityAIBase {
  public EntityAIMoveToOptimalDistance(EntityTameableDragon i_dragon, double i_entityMoveSpeed,
                                       float i_minAttackDistance, float i_optimalAttackDistance, float i_maxAttackDistance) {
    this.dragon = i_dragon;
    this.entityMoveSpeed = i_entityMoveSpeed;
    this.minAttackDistanceSQ = i_minAttackDistance * i_minAttackDistance;
    this.optimalAttackDistanceSQ = i_optimalAttackDistance * i_optimalAttackDistance;
    this.maxAttackDistanceSQ = i_maxAttackDistance * i_maxAttackDistance;
    this.setMutexBits(3);
    canSeeTargetTickCount = 0;
  }

  /**
   * Returns whether the EntityAIBase should begin execution.
   */
  @Override
  public boolean shouldExecute() {
    return dragon.getBreathHelperP().hasBreathTargetForMoving() && !dragon.isRiding();
  }

  /**
   * Returns whether an in-progress EntityAIBase should continue executing
   */
  @Override
  public boolean shouldContinueExecuting() {
    return this.shouldExecute();
  }

  /**
   * Resets the task
   */
  @Override
  public void resetTask() {
    dragon.getNavigator().clearPath();  // stop moving
  }

  /**
   * Updates the task:
   * moves towards (or away from) the breath target
   * looks at the target.
   */
  @Override
  public void updateTask() {
    BreathWeaponTarget currentTarget = dragon.getBreathHelperP().getBreathTargetForMoving();
    boolean targetChanged = !BreathWeaponTarget.approximatelyMatches(currentTarget, lastTickTarget);
    lastTickTarget = currentTarget;

    if (currentTarget == null) {
      dragon.getNavigator().clearPath();  // stop moving
      return;
    }

    // check if target visible: if so, look at it
    boolean canSeeTarget = true;
    if (currentTarget.getTypeOfTarget() == BreathWeaponTarget.TypeOfTarget.ENTITY) {
      Entity targetEntity = currentTarget.getTargetEntity(dragon.world);
      canSeeTarget = (targetEntity != null) && dragon.getEntitySenses().canSee(targetEntity);
    }
    if (canSeeTarget) {
      ++this.canSeeTargetTickCount;
      Vec3d dragonEyePos = dragon.getPositionVector().addVector(0, dragon.getEyeHeight(), 0);
      currentTarget.setEntityLook(dragon.world, dragon.getLookHelper(), dragonEyePos,
              dragon.getHeadYawSpeed(), dragon.getHeadPitchSpeed());
    } else {
      this.canSeeTargetTickCount = 0;
    }

    double distanceToTargetSQ = currentTarget.distanceSQtoTarget(dragon.world, dragon.getPositionVector());

    // navigate to appropriate range: only change navigation path if the target has changed or
    //   if there is no navigation path currently in progress
    final int SEE_TARGET_TICK_THRESHOLD = 40;
    if (distanceToTargetSQ < 0) {
      // don't move since distance not meaningful - this is direction only
    } else if (distanceToTargetSQ <= minAttackDistanceSQ) {
      // back up to at least minimum range.
      PathNavigate pathNavigate = dragon.getNavigator();
      if (targetChanged || pathNavigate.noPath()) {
        currentTarget.setNavigationPathAvoid(dragon.world, pathNavigate,
                dragon.getPositionVector().addVector(0, dragon.getEyeHeight(), 0),
                entityMoveSpeed,
                MathHelper.sqrt(optimalAttackDistanceSQ) + 1.0);
      }
    } else if (distanceToTargetSQ <= optimalAttackDistanceSQ) {
      dragon.getNavigator().clearPath();  // at optimal distance - stop moving
    } else if (distanceToTargetSQ <= maxAttackDistanceSQ && this.canSeeTargetTickCount >= SEE_TARGET_TICK_THRESHOLD) {
      dragon.getNavigator().clearPath();  // have been within range to attack for a while - stop moving
    } else {
      PathNavigate pathNavigate = dragon.getNavigator(); // still too far! move closer
      if (targetChanged || pathNavigate.noPath()) {
        currentTarget.setNavigationPath(dragon.world, dragon.getNavigator(), entityMoveSpeed);
      }
    }

  }
  /** The entity the AI instance has been applied to */
  private final EntityTameableDragon dragon;
  private double entityMoveSpeed;
  private int canSeeTargetTickCount;
  private float minAttackDistanceSQ;
  private float optimalAttackDistanceSQ;
  private float maxAttackDistanceSQ;
  private BreathWeaponTarget lastTickTarget = null;
}
