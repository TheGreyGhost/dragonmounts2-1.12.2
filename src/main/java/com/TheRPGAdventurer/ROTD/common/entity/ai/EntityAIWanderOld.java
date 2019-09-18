package com.TheRPGAdventurer.ROTD.common.entity.ai;

import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.Vec3d;

public class EntityAIWanderOld extends EntityAIDragonBase {
  public EntityAIWanderOld(EntityTameableDragon dragon, double p_i1648_2_) {
    super(dragon);
    this.speed = p_i1648_2_;
    this.setMutexBits(1);
  }

  /**
   * Returns whether the EntityAIBase should begin execution.
   */
  @Override
  public boolean shouldExecute() {
    if (this.dragon.getRNG().nextInt(120) != 0) return false;
    Vec3d vec3d = RandomPositionGenerator.findRandomTarget(this.dragon, 10, 7);

    if (vec3d == null) return false;
    else {
      this.xPosition = vec3d.x;
      this.yPosition = vec3d.y;
      this.zPosition = vec3d.z;
      return true;
    }
  }

  /**
   * Returns whether an in-progress EntityAIBase should continue executing
   */
  @Override
  public boolean shouldContinueExecuting() {
    return !this.dragon.getNavigator().noPath();
  }

  /**
   * Execute a one shot task or start executing a continuous task
   */
  @Override
  public void startExecuting() {
    this.dragon.getNavigator().tryMoveToXYZ(this.xPosition, this.yPosition, this.zPosition, this.speed);
  }

  public void makeUpdate() {
    this.mustUpdate = true;
  }
  private static final String __OBFID = "CL_00001608";
  private double xPosition;
  private double yPosition;
  private double zPosition;
  private double speed;
  private boolean mustUpdate;
}