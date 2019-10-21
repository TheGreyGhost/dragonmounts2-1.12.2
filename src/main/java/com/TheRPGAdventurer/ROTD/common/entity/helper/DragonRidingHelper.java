package com.TheRPGAdventurer.ROTD.common.entity.helper;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.inits.ModKeys;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by TGG on 19/10/2019.
 * Used to assist two types of riding:
 * 1) when the dragon rides on the player's shoulder
 * 2) when the player is riding on the dragon
 */
public class DragonRidingHelper extends DragonHelper {
  public DragonRidingHelper(EntityTameableDragon dragon) {
    super(dragon);
  }

  /**
   * Initialise all the configuration tags used by this helper
   */
  public static void registerConfigurationTags()
  {
    // the initialisation of the tags is all done in their static initialisers
//    DragonVariants.addVariantTagValidator(new DragonReproductionValidator());
  }


  @Override
  public void writeToNBT(NBTTagCompound nbt) {

  }

  @Override
  public void readFromNBT(NBTTagCompound nbt) {

  }

  @Override
  public void registerDataParameters() {

  }

  @Override
  public void initialiseServerSide() {

  }

  @Override
  public void initialiseClientSide() {

  }

  @Override
  public void notifyDataManagerChange(DataParameter<?> key) {

  }

  @Override
  public void onLivingUpdate() {
    if (this.getRidingEntity() instanceof EntityLivingBase) {
      EntityLivingBase ridingEntity = (EntityLivingBase) this.getRidingEntity();
      if (ridingEntity.isElytraFlying() && ridingEntity != null) {
        this.setUnHovered(true);
      }
    }

    if (getControllingPlayer() == null && !isFlying() && isSitting()) {
      removePassengers();
    }


  }

  @Override
  public void onDeathUpdate() {
    // unmount any riding entities
    removePassengers();
  }


  /**
   * Attempt to place the dragon onto one of the player's shoulders
   * @param entityPlayer
   * @return
   */
  public boolean attemptToPlaceOnShoulder(EntityPlayer entityPlayer) {
    if (!dragon.isTamedFor(entityPlayer)) return false;
    if (dragon.getRidingEntity() == entityPlayer) return false;
    if (true != (boolean)dragon.configuration().getVariantTagValue(DragonVariants.Category.BEHAVIOUR, WILL_RIDE_SHOULDER)) return false;
    entityPlayer.set

    if (!this.isSittingOnShoulder && !this.entity.isSitting() && !this.entity.getLeashed())
    {
      if (this.entity.getEntityBoundingBox().intersects(this.owner.getEntityBoundingBox()))
      {
        this.isSittingOnShoulder = this.entity.setEntityOnShoulder(this.owner);
      }
    }
  }

//------------------------ player riding on dragon ------------------


  /**
   * Returns true if the dragon is saddled.
   */
  public boolean isSaddled() {
    return dataManager.get(DATA_SADDLED);
  }

  /**
   * Set or remove the saddle of the
   */
  public void setSaddled(boolean saddled) {
    L.trace("setSaddled({})", saddled);
    dataManager.set(DATA_SADDLED, saddled);
  }


  public boolean canBeSteered() {
    //         must always return false or the vanilla movement code interferes
    //         with DragonMoveHelper
    return false;
  }

  public boolean hasControllingPlayer(EntityPlayer player) {
    return this.getControllingPassenger() != null && this.getControllingPassenger() instanceof EntityPlayer
            && this.getControllingPassenger().getUniqueID().equals(player.getUniqueID());
  }

  public void setRidingPlayer(EntityPlayer player) {
    L.trace("setRidingPlayer({})", player.getName());
    player.rotationYaw = rotationYaw;
    player.rotationPitch = rotationPitch;
    player.startRiding(this);
  }

  public void updateRidden() {
    Entity entity = this.getRidingEntity();
    this.motionX = 0.0D;
    this.motionY = 0.0D;
    this.motionZ = 0.0D;
    this.onUpdate();
    if (this.isRiding()) this.updateRiding((EntityLivingBase) entity);
  }

  public void updateIntendedRideRotation(EntityPlayer rider) {
    boolean hasRider = this.hasControllingPlayer(rider);
    if (this.isUsingBreathWeapon() && hasRider && rider.moveStrafing == 0) {
      this.rotationYaw = rider.rotationYaw;
      this.prevRotationYaw = this.rotationYaw;
      this.rotationPitch = rider.rotationPitch;
      this.setRotation(this.rotationYaw, this.rotationPitch);
      this.renderYawOffset = this.rotationYaw;
      this.rotationYawHead = this.renderYawOffset;
    }
  }

  @Nullable
  public Entity getControllingPassenger() {
    return this.getPassengers().isEmpty() ? null : getPassengers().get(0);
  }

  public boolean isRidingAboveGround(Entity entityBeingRidden) {
    int groundPos = world.getHeight(getPosition()).getY();
    double altitude = entityBeingRidden.posY - groundPos;
    return altitude > 2.0;
  }

  /**
   * This code is called when the dragon is riding on the shoulder of the player
   *
   * @param entityBeingRidden
   */
  public void updateRiding(EntityLivingBase entityBeingRidden) {
    if (entityBeingRidden == null || !(entityBeingRidden instanceof EntityPlayer)) return;
    EntityPlayer playerBeingRidden = (EntityPlayer) entityBeingRidden;

    if (playerBeingRidden.isPassenger(this)) { // this dragon is a passenger of the player being ridden
      int i = playerBeingRidden.getPassengers().indexOf(this);
      float radius = (i == 2 ? 0F : 0.4F) + (playerBeingRidden.isElytraFlying() ? 2 : 0);
      float angle = 0.01745329251F * playerBeingRidden.renderYawOffset + (i == 1 ? -90 : i == 0 ? 90 : 0);
      double extraX = (double) (radius * MathHelper.sin((float) (Math.PI + angle)));
      double extraZ = (double) (radius * MathHelper.cos(angle));
      double extraY = (playerBeingRidden.isSneaking() ? 1.3D : 1.4D) + (i == 2 ? 0.4D : 0D);
      this.rotationYaw = playerBeingRidden.rotationYaw;
      this.prevRotationYaw = playerBeingRidden.prevRotationYaw;
      this.rotationYawHead = playerBeingRidden.rotationYawHead;
      this.prevRotationYawHead = playerBeingRidden.prevRotationYawHead;
      this.rotationPitch = playerBeingRidden.rotationPitch;
      this.prevRotationPitch = playerBeingRidden.prevRotationPitch;
      this.setPosition(playerBeingRidden.posX + extraX, playerBeingRidden.posY + extraY, playerBeingRidden.posZ + extraZ);
      if (ModKeys.DISMOUNT.isKeyDown() || this.isDead || !this.isBaby()) this.dismountRidingEntity();
      this.setFlying(isRidingAboveGround(playerBeingRidden) && !playerBeingRidden.capabilities.isFlying && !playerBeingRidden.onGround);
    }
  }

  /**
   * This code is called when the passenger is riding on the dragon
   *
   * @param passenger
   */
  @Override
  public void updatePassenger(Entity passenger) {
    if (this.isPassenger(passenger)) {
      List<Entity> passengers = getPassengers();
      int passengerNumber = passengers.indexOf(passenger);
      if (passengerNumber < 0) {  // should never happen!
        DragonMounts.loggerLimit.error_once("Logic error- passenger not found");
        return;
      }

      Vec3d mountedPositionOffset = dragonPhysicalModel.getRiderPositionOffsetWC(getAgeScale(), getBodyPitch(), isSitting(), passengerNumber);

//      // todo remove (debugging only)
//      mountedPositionOffset = new Vec3d(DebugSettings.getDebugParameter("x"),
//                                        DebugSettings.getDebugParameter("y"),
//                                        DebugSettings.getDebugParameter("z"));
//      System.out.println("MountedOffset:" + mountedPositionOffset);

//      double dragonScaling = getScale(); //getBreed().getAdultModelRenderScaleFactor() * getScale();
//
//      mountedPositionOffset = mountedPositionOffset.scale(dragonScaling);
      mountedPositionOffset = mountedPositionOffset.rotateYaw((float) Math.toRadians(-renderYawOffset));
      final double EXTRA_HEIGHT_TO_PLAYER_BUTT = 0.28F;  // the passenger.getYOffset doesn't actually give the correct butt position for the player
      //  --> need to allow for extra
      double passengerOriginToButtHeight = -passenger.getYOffset() + EXTRA_HEIGHT_TO_PLAYER_BUTT;
      mountedPositionOffset = mountedPositionOffset.subtract(0, passengerOriginToButtHeight, 0);  // adjust for passenger's seated change in height

      if (!(passenger instanceof EntityPlayer)) {
        passenger.rotationYaw = this.rotationYaw;
        passenger.setRotationYawHead(passenger.getRotationYawHead() + this.rotationYaw);
        this.applyYawToEntity(passenger);
      }
      Vec3d passengerPosition = mountedPositionOffset.addVector(this.posX, this.posY, this.posZ);
      passenger.setPosition(passengerPosition.x, passengerPosition.y, passengerPosition.z);

      // fix rider rotation
      if (passenger == getControllingPlayer()) {
        EntityPlayer rider = getControllingPlayer();
        rider.prevRotationPitch = rider.rotationPitch;
        rider.prevRotationYaw = rider.rotationYaw;
        rider.renderYawOffset = renderYawOffset;
      }
    }
  }

  public boolean shouldDismountInWater(Entity rider) {
    return false;
  }

  public boolean canFitPassenger(Entity passenger) {
    return this.getPassengers().size() < dragonPhysicalModel.getMaxNumberOfPassengers(lifeStage().getLifeStage());
  }


  private static final DragonVariantTag WILL_RIDE_SHOULDER = DragonVariantTag.addTag("willrideshoulder", true,
          "will the dragon ride on the player's shoulder?").categories(DragonVariants.Category.BEHAVIOUR);


}
