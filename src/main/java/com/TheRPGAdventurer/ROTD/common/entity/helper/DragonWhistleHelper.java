package com.TheRPGAdventurer.ROTD.common.entity.helper;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Created by TGG on 20/10/2019.
 */
public class DragonWhistleHelper extends DragonHelper {
  public DragonWhistleHelper(EntityTameableDragon dragon) {
    super(dragon);
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
    ItemStack whistle = this.getControllingWhistle();
    if (whistle != null && whistle.getTagCompound() != null && !whistle.getTagCompound().getUniqueId(DragonMounts.MODID + "dragon").equals(this.getUniqueID()) && whistle.hasTagCompound()) {
      this.setnothing(true);
    }

    if (getOwner() != null && firesupport()) {
      Vec3d dragonEyePos = this.getPositionVector().addVector(0, this.getEyeHeight(), 0);
      Vec3d lookDirection = getOwner().getLook(1.0F);
      Vec3d endOfLook = dragonEyePos.addVector(lookDirection.x, lookDirection.y, lookDirection.z); // todo fix the head looking down
      this.getLookHelper().setLookPosition(lookDirection.x, lookDirection.y, lookDirection.z,
              120, 90);
    }



  }

  public ItemStack getControllingWhistle() {
    return dataManager.get(WHISTLE);
  }

  public void setControllingWhistle(ItemStack whistle) {
    dataManager.set(WHISTLE, whistle);
  }




  public boolean nothing() {
    return (dataManager.get(WHISTLE_STATE)) == 0;
  }

  public boolean follow() {
    return (dataManager.get(WHISTLE_STATE)) == 1;
  }

  public boolean circle() {
    return (dataManager.get(WHISTLE_STATE)) == 2;
  }

  public boolean come() {
    return (dataManager.get(WHISTLE_STATE)) == 3;
  }

  public boolean homepos() {
    return (dataManager.get(WHISTLE_STATE)) == 4;
  }

  public boolean sit() {
    return (dataManager.get(WHISTLE_STATE)) == 5;
  }

  public boolean firesupport() {
    return dataManager.get(FIRE_SUPPORT);
  }

  public void setfiresupport(boolean firesupport) {
    dataManager.set(FIRE_SUPPORT, firesupport);
  }

  public void setnothing(boolean nothing) {
    setStateField(0, nothing);
  }

  /**
   * @TheRPGAdventurer thanks AlexThe666
   */
  public void setStateField(int i, boolean newState) {
    byte prevState = dataManager.get(WHISTLE_STATE).byteValue();
    if (newState) {
      setWhistleState((byte) i);
    } else {
      setWhistleState(prevState);
    }
  }

  public byte getWhistleState() {
    return dataManager.get(WHISTLE_STATE).byteValue();
  }

  public void setWhistleState(byte state) {
    dataManager.set(WHISTLE_STATE, state);
  }


  public boolean followPlayerFlying(EntityLivingBase entityLivingBase) {
    BlockPos midPoint = entityLivingBase.getPosition();
    double x = midPoint.getX() + 0.5 - 12;
    double y = midPoint.getY() + 0.5 + 24;
    double z = midPoint.getZ() + 0.5 - 12;
    this.setBoosting(this.getDistance(getOwner()) > 180);
    return this.getNavigator().tryMoveToXYZ(x, y, z, 1);
  }

  public boolean fireSupport(EntityTameableDragon dragon, EntityLivingBase owner) {
    if (dragon.isUsingBreathWeapon() && owner != null) {
      equalizeYaw(owner);
    }

    BlockPos midPoint = owner.getPosition();
    double offset = 16D;
    double x = midPoint.getX() + 0.5 - 12;
    double y = midPoint.getY() + 0.5 + 24;
    double z = midPoint.getZ() + 0.5 - offset;
    this.setBoosting(this.getDistance(getOwner()) > 50);
    return this.getNavigator().tryMoveToXYZ(x, y, z, 1);
  }

  public boolean comeToPlayerFlying(BlockPos point, EntityLivingBase owner) {
    float dist = this.getDistance(owner);
    if (dist <= 12) {
      this.inAirTicks = 0;
      this.setFlying(false);
      if (!isFlying()) {
        this.setnothing(true);
      }
    }

    this.setBoosting(this.getDistance(getOwner()) > 80);

    if (this.getControllingPlayer() != null) return false;

    if (!isFlying() && dist >= 5) this.liftOff();

    if (isFlying()) return this.getNavigator().tryMoveToXYZ(point.getX(), point.getY(), point.getZ(), 1);
    else return false;
  }

  public boolean circleTarget2(BlockPos target, float height, float radius, float speed, boolean direction, float offset, float moveSpeedMultiplier) {
    int directionInt = direction ? 1 : -1;
    this.setBoosting(this.getDistance(getOwner()) > 80);
    return this.getNavigator().tryMoveToXYZ(
            target.getX() + radius * Math.cos(directionInt * this.ticksExisted * 0.5 * speed / radius + offset),
            DragonMounts.instance.getConfig().maxFlightHeight + target.getY(),
            target.getZ() + radius * Math.sin(directionInt * this.ticksExisted * 0.5 * speed / radius + offset),
            speed * moveSpeedMultiplier);

  }

  public boolean circleTarget1(BlockPos midPoint) {
    if (this.getControllingPlayer() != null) return false;

//        Vec3d vec1 = this.getPositionVector().subtract(midPoint.getX(), midPoint.getY(), midPoint.getZ());
//        Vec3d vec2 = new Vec3d(0, 0, 1);
//
//        double a = Math.acos((vec1.dotProduct(vec2)) / (vec1.lengthVector() * vec2.lengthVector()));
//        double r = 0.9 * 30;  // DragonMountsConfig.dragonFlightHeight
//        double x = midPoint.getX() + 1;
//        double y = midPoint.getY() + 20; // DragonMountsConfig.dragonFlightHeight
//        double z = midPoint.getZ() + 1;
//        this.getMoveHelper().setMoveTo(x + 0.5, y + 0.5, z + 0.5, 1);
//
//        return true;
    this.setBoosting(this.getDistance(getOwner()) > 180); // todo fix the rotation
    return this.getNavigator().tryMoveToXYZ(midPoint.getX() + 10 * Math.cos(1 * this.ticksExisted * 0.5 * 1 / 10 + 4), DragonMounts.instance.getConfig().maxFlightHeight + midPoint.getY(), midPoint.getZ() + 10 * Math.sin(1 * this.ticksExisted * 0.5 * 1 / 10 + 4), 1);

  }

  private static final DataParameter<Byte> WHISTLE_STATE = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BYTE);
  private static final DataParameter<ItemStack> WHISTLE = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.ITEM_STACK);
  private static final DataParameter<Boolean> SLEEP = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Boolean> FIRE_SUPPORT = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);

}
