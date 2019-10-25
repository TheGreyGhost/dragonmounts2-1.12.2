package com.TheRPGAdventurer.ROTD.common.entity.helper;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.Modifiers;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Created by TGG on 20/10/2019.
 */
public class DragonWhistleHelper extends DragonHelper {
  public DragonWhistleHelper(EntityTameableDragon dragon) {
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
    nbt.setBoolean("unhovered", this.isUnHovered());
    nbt.setBoolean("followyaw", this.followYaw());
    nbt.setBoolean("firesupport", this.firesupport());
    //        nbt.setBoolean("unFluttered", this.isUnFluttered());
    nbt.setBoolean("ylocked", this.isYLocked());
    setCompleted(FunctionTag.WRITE_TO_NBT);
  }

  @Override
  public void readFromNBT(NBTTagCompound nbt) {
    checkPreConditions(FunctionTag.READ_FROM_NBT);
    this.setGoingDown(nbt.getBoolean("down"));
    this.setUnHovered(nbt.getBoolean("unhovered"));
    this.setYLocked(nbt.getBoolean("ylocked"));
    this.setFollowYaw(nbt.getBoolean("followyaw"));
    //        this.setUnFluttered(nbt.getBoolean("unFluttered"));
    this.setBoosting(nbt.getBoolean("boosting"));
    this.setfiresupport(nbt.getBoolean("firesupport"));
    setCompleted(FunctionTag.READ_FROM_NBT);
  }

  @Override
  public void registerDataParameters() {
    checkPreConditions(FunctionTag.REGISTER_DATA_PARAMETERS);

    dataManager.register(DATA_FLYING, false);
    dataManager.register(GOING_DOWN, false);

    dataManager.register(WHISTLE_STATE, (byte) 0);
    dataManager.register(WHISTLE, ItemStack.EMPTY);
    dataManager.register(BOOSTING, false);
    dataManager.register(HOVER_CANCELLED, false);
    dataManager.register(Y_LOCKED, false);
    dataManager.register(FOLLOW_YAW, true);
    dataManager.register(FIRE_SUPPORT, false);
    setCompleted(FunctionTag.REGISTER_DATA_PARAMETERS);


  }

  @Override
  public void initialiseServerSide() {
    checkPreConditions(FunctionTag.INITIALISE_SERVER);
    setCompleted(FunctionTag.INITIALISE_SERVER);
  }

  @Override
  public void initialiseClientSide() {
    checkPreConditions(FunctionTag.INITIALISE_CLIENT);
    setCompleted(FunctionTag.INITIALISE_CLIENT);
  }

  @Override
  public void onConfigurationChange() {
    checkPreConditions(FunctionTag.ON_CONFIG_CHANGE);
    throw new NotImplementedException("onConfigurationChange()");
  }

  @Override
  public void onLivingUpdate() {
    checkPreConditions(FunctionTag.VANILLA);
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

  private static final DataParameter<Boolean> DATA_FLYING = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Boolean> GOING_DOWN = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Boolean> BOOSTING = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Boolean> HOVER_CANCELLED = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Boolean> Y_LOCKED = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Boolean> FOLLOW_YAW = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);

  private boolean isGoingDown;
  private boolean isUnhovered;
  private boolean yLocked;
  private boolean followYaw;

}
