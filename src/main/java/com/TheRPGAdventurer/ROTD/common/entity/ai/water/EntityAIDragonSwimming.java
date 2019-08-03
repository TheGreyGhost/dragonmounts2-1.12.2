package com.TheRPGAdventurer.ROTD.common.entity.ai.water;

import com.TheRPGAdventurer.ROTD.common.entity.ai.EntityAIDragonBase;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import net.minecraft.pathfinding.PathNavigateFlying;
import net.minecraft.pathfinding.PathNavigateGround;

public class EntityAIDragonSwimming extends EntityAIDragonBase {

  public EntityAIDragonSwimming(EntityTameableDragon dragon) {
    super(dragon);
    this.setMutexBits(4);

    if (dragon.getNavigator() instanceof PathNavigateGround) {
      ((PathNavigateGround) dragon.getNavigator()).setCanSwim(true);
    } else if (dragon.getNavigator() instanceof PathNavigateFlying) {
      ((PathNavigateFlying) dragon.getNavigator()).setCanFloat(true);
    }
  }

  /**
   * Returns whether the EntityAIBase should begin execution.
   */
  @Override
  public boolean shouldExecute() {
    return this.dragon.isInWater() || this.dragon.isInLava() && !dragon.onGround;
  }

  /**
   * Keep ticking a continuous task that has already been started
   */
  @Override
  public void updateTask() {
    if (this.dragon.getRNG().nextFloat() < 1.3F && !dragon.onGround && !dragon.isInWater()) {
      this.dragon.getJumpHelper().setJumping();
    }
  }
}