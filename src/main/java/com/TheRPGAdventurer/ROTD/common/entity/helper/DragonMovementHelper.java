package com.TheRPGAdventurer.ROTD.common.entity.helper;

import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.play.server.SPacketAnimation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

/**
 * Created by TGG on 21/10/2019.
 */
public class DragonMovementHelper extends DragonHelper {
  public DragonMovementHelper(EntityTameableDragon dragon) {
    super(dragon);
    setCompleted(FunctionTag.CONSTRUCTOR);
  }

  public static void registerConfigurationTags()
  {
    // the initialisation of the tags is all done in their static initialisers
    //    DragonVariants.addVariantTagValidator(new DragonReproductionValidator());
  }

  @Override
  public void writeToNBT(NBTTagCompound nbt) {
    checkPreConditions(FunctionTag.WRITE_TO_NBT);
    nbt.setBoolean("boosting", this.boosting());
    nbt.setBoolean("down", this.isGoingDown());
    setCompleted(FunctionTag.WRITE_TO_NBT);
  }

  @Override
  public void readFromNBT(NBTTagCompound nbt) {
    checkPreConditions(FunctionTag.READ_FROM_NBT);
    setCompleted(FunctionTag.READ_FROM_NBT);
  }

  @Override
  public void registerDataParameters() {
    checkPreConditions(FunctionTag.REGISTER_DATA_PARAMETERS);
    setCompleted(FunctionTag.REGISTER_DATA_PARAMETERS);
  }

  public void registerEntityAttributes() {
    checkPreConditions(FunctionTag.REGISTER_ENTITY_ATTRIBUTES);
    dragon.getAttributeMap().registerAttribute(MOVEMENT_SPEED_AIR);
    setCompleted(FunctionTag.REGISTER_ENTITY_ATTRIBUTES);

  }

  @Override
  public void initialiseServerSide() {
    checkPreConditions(FunctionTag.INITIALISE_SERVER);
    setCompleted(FunctionTag.INITIALISE_SERVER);
  }

  @Override
  public void initialiseClientSide() {
    checkPreConditions(FunctionTag.INITIALISE_CLIENT);
    setCompleted(FunctionTag.INITIALISE_SERVER);
  }

  private void initialiseBothSides() {
    getEntityAttribute(MOVEMENT_SPEED_AIR).setBaseValue(BASE_AIR_SPEED);
    getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(BASE_GROUND_SPEED);

  }

  @Override
  public void onConfigurationChange() {
    throw new NotImplementedException("onConfigurationChange()");
  }

  /**
   * method used to fix the head rotation, call it on onlivingbase or riding ai to trigger
   */
  public void lookAtTarget(EntityLivingBase rider) {
    if ((this.isUsingBreathWeapon() && this.moveStrafing == 0) && isFlying()) {
      rotationYaw = ((EntityPlayer) rider).rotationYaw;
    }

    Vec3d dragonEyePos = this.getPositionVector().addVector(0, this.getEyeHeight(), 0);
    Vec3d lookDirection = rider.getLook(1.0F);
    Vec3d endOfLook = dragonEyePos.addVector(lookDirection.x, lookDirection.y, lookDirection.z); // todo fix the head looking down
    this.getLookHelper().setLookPosition(endOfLook.x, endOfLook.y, endOfLook.z, 120, 90);
  }


  public void equalizeYaw(EntityLivingBase rider) {
    if (isFlying() && this.moveStrafing == 0) {
      this.rotationYaw = ((EntityPlayer) rider).rotationYaw;
      this.prevRotationYaw = ((EntityPlayer) rider).prevRotationYaw;
    }
    this.rotationYawHead = ((EntityPlayer) rider).rotationYawHead;
    this.prevRotationYawHead = ((EntityPlayer) rider).prevRotationYawHead;
    this.rotationPitch = ((EntityPlayer) rider).rotationPitch;
    this.prevRotationPitch = ((EntityPlayer) rider).prevRotationPitch;
  }


  public double getFlySpeed() {
    return this.boosting() ? 4 : 1;
  }

  public double getDragonSpeed() {
    return isFlying() ? BASE_FOLLOW_RANGE_FLYING : BASE_FOLLOW_RANGE;
  }

  /**
   * Used to get the hand in which to swing and play the hand swinging animation
   * when attacking In this case the dragon's jaw
   */
  @Override
  public void swingArm(EnumHand hand) {
    // play eating sound
    playSound(getAttackSound(), 1, 0.7f);

    // play attack animation
    if (world instanceof WorldServer) {
      ((WorldServer) world).getEntityTracker().sendToTracking(this, new SPacketAnimation(this, 0));
    }

    ticksSinceLastAttack = 0;
  }


  /**
   * Causes this entity to lift off if it can fly.
   */
  public void liftOff() {
    L.trace("liftOff");
    if (canFly()) {
      boolean ridden = isBeingRidden();
      // stronger jump for an easier lift-off
      motionY += ridden || (isInWater() && isInLava()) ? 0.7 : 6;
      inAirTicks += ridden || (isInWater() && isInLava()) ? 3.0 : 4;
      jump();
    }
  }

  /**
   * Returns true if the entity is breathing.
   */
  public boolean isGoingDown() {
    if (world.isRemote) {
      boolean goingdown = this.dataManager.get(GOING_DOWN);
      this.isGoingDown = goingdown;
      return isGoingDown;
    }
    return isGoingDown;
  }

  /**
   * Set the breathing flag of the entity.
   */
  public void setGoingDown(boolean goingdown) {
    this.dataManager.set(GOING_DOWN, goingdown);
    if (!world.isRemote) {
      this.isGoingDown = goingdown;
    }
  }

  public boolean isYLocked() {
    if (world.isRemote) {
      boolean yLocked = dataManager.get(Y_LOCKED);
      this.yLocked = yLocked;
      return yLocked;
    }
    return yLocked;
  }

  public void setYLocked(boolean yLocked) {
    dataManager.set(Y_LOCKED, yLocked);
    if (!world.isRemote) {
      this.yLocked = yLocked;
    }
  }

  public boolean isUnHovered() {
    if (world.isRemote) {
      boolean isUnhovered = dataManager.get(HOVER_CANCELLED);
      this.isUnhovered = isUnhovered;
      return isUnhovered;
    }
    return isUnhovered;
  }

  public void setUnHovered(boolean isUnhovered) {
    dataManager.set(HOVER_CANCELLED, isUnhovered);
    if (!world.isRemote) {
      this.isUnhovered = isUnhovered;
    }
  }

  public boolean followYaw() {
    if (world.isRemote) {
      boolean folowYaw = dataManager.get(FOLLOW_YAW);
      this.followYaw = folowYaw;
      return folowYaw;
    }
    return followYaw;
  }

  public void setFollowYaw(boolean folowYaw) {
    dataManager.set(FOLLOW_YAW, folowYaw);
    if (!world.isRemote) {
      this.followYaw = folowYaw;
    }
  }

  /**
   * Returns relative speed multiplier for the vertical flying speed.
   *
   * @return relative vertical speed multiplier
   */
  public double getMoveSpeedAirVert() {
    return this.airSpeedVertical;
  }

  public boolean boosting() {
    return dataManager.get(BOOSTING);
  }

  public void setBoosting(boolean allow) {
    dataManager.set(BOOSTING, allow);
  }

  public boolean canFly() {
    // eggs can't fly
    return !isBaby();
  }

  /**
   * Returns true if the entity is flying.
   */
  public boolean isFlying() {
    return dataManager.get(DATA_FLYING);
  }

  /**
   * f Set the flying flag of the entity.
   */
  public void setFlying(boolean flying) {
    L.trace("setFlying({})", flying);
    dataManager.set(DATA_FLYING, flying);
  }

  public int getTicksSinceLastAttack() {
    return ticksSinceLastAttack;
  }

  /**
   * returns the pitch of the dragon's body
   */
  public float getBodyPitch() {
    return getAnimator().getBodyPitch();
  }

  /** returns true if the dragon is susceptible to falling damage
   *
   * @param distance
   * @param damageMultiplier
   * @return
   */
  public boolean  shouldSufferFallDamager(float distance, float damageMultiplier) {
    return (!canFly());
  }


  /**
   * Returns the distance to the ground while the entity is flying.
   */
  public double getAltitude() {
    BlockPos groundPos = world.getHeight(getPosition());
    double altitude = posY - groundPos.getY();
    return altitude;
  }


  /**
   * Checks if the blocks below the dragons hitbox is present and solid
   */
  public boolean onSolidGround() {
    for (double y = -3.0; y <= -1.0; ++y) {
      for (double xz = -2.0; xz < 3.0; ++xz) {
        if (isBlockSolid(posX + xz, posY + y, posZ + xz)) return true;
      }
    }
    return false;
  }


  /** vanilla travel method
   *
   * @param strafe
   * @param forward
   * @param vertical
   * @return true if this method handled the movement, false if vanilla travel() should be called.
   */
  public boolean travel(float strafe, float forward, float vertical) {
    // disable method while flying, the movement is done entirely by
    // moveEntity() and this one just makes the dragon to fall slowly when
    // hovering
    return !isFlying();
  }

  /**
   * when the dragon rotates its head left-right (yaw), how fast does it move?
   *
   * @return max yaw speed in degrees per tick
   */
  public float getHeadYawSpeed() {
    return 120; //this.getControllingPlayer()!=null ? 400 : 1;
  }

  /**
   * when the dragon rotates its head up-down (pitch), how fast does it move?
   *
   * @return max pitch speed in degrees per tick
   */
  public float getHeadPitchSpeed() {
    return 90; //this.getControllingPlayer()!=null ? 400 : 1;
  }

  @Override
  public float getJumpUpwardsMotion() {
    // stronger jumps for easier lift-offs
    return canFly() ? 1 : super.getJumpUpwardsMotion();
  }

  public float getWaterSlowDown() {
    return 0.9F;
  }

  /**
   * Applies this boat's yaw to the given entity. Used to update the orientation of its passenger.
   */
  protected void applyYawToEntity(Entity entityToUpdate) {
    entityToUpdate.setRenderYawOffset(this.rotationYaw);
    float f = MathHelper.wrapDegrees(entityToUpdate.rotationYaw - this.rotationYaw);
    float f1 = MathHelper.clamp(f, -105.0F, 105.0F);
    entityToUpdate.prevRotationYaw += f1 - f;
    entityToUpdate.rotationYaw += f1 - f;
    entityToUpdate.setRotationYawHead(entityToUpdate.rotationYaw);
  }

  protected double getFollowRange() {
    return this.getAttributeMap().getAttributeInstance(FOLLOW_RANGE).getAttributeValue();
  }
  protected int ticksSinceLastAttack;

  /*
 * Called in onSolidGround()
 */
  private boolean isBlockSolid(double xcoord, double ycoord, double zcoord) {
    BlockPos pos = new BlockPos(xcoord, ycoord, zcoord);
    IBlockState state = world.getBlockState(pos);
    return state.getMaterial().isSolid() || (this.getControllingPlayer() == null && (this.isInWater() || this.isInLava()));
  }

  /**
   * Pushes all entities inside the list away from the ender
   */
  private void collideWithEntities(List<Entity> p_70970_1_, double strength) {
    double x = (this.getEntityBoundingBox().minX + this.getEntityBoundingBox().maxX) / 2.0D;
    double z = (this.getEntityBoundingBox().minZ + this.getEntityBoundingBox().maxZ) / 2.0D;

    for (Entity entity : p_70970_1_) {
      if (entity instanceof EntityLivingBase && !this.isPassenger(entity)) {
        double x1 = entity.posX - x;
        double z1 = entity.posZ - z;
        double xzSquared = x1 * x1 + z1 * z1;
        entity.addVelocity(x1 / xzSquared * 4.0D, 0.20000000298023224D, z1 / xzSquared * strength);

        if (this.isFlying()) {
          entity.attackEntityFrom(DamageSource.causeMobDamage(this), 5.0F);
          this.applyEnchantments(this, entity);
        }
      }
    }
  }

  public static final IAttribute MOVEMENT_SPEED_AIR = new RangedAttribute(null, "generic.movementSpeedAir", 0.9, 0.0, Double.MAX_VALUE).setDescription("Movement Speed Air").setShouldWatch(true);


  public static final double BASE_GROUND_SPEED = 0.4;
  public static final double BASE_AIR_SPEED = 0.9;


}
