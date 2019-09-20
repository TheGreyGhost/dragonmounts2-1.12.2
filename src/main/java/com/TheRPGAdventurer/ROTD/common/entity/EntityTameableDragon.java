/*
 ** 2012 August 13
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.common.entity;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.model.dragon.anim.DragonAnimator;
import com.TheRPGAdventurer.ROTD.client.userinput.DragonOrbControl;
import com.TheRPGAdventurer.ROTD.common.entity.ai.ground.EntityAIDragonSit;
import com.TheRPGAdventurer.ROTD.common.entity.ai.path.PathNavigateFlying;
import com.TheRPGAdventurer.ROTD.common.entity.breath.BreathWeaponTarget;
import com.TheRPGAdventurer.ROTD.common.entity.breath.DragonBreathHelperP;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreed;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.EnumDragonBreed;
import com.TheRPGAdventurer.ROTD.common.entity.helper.*;
import com.TheRPGAdventurer.ROTD.common.entity.interact.DragonInteractBase;
import com.TheRPGAdventurer.ROTD.common.entity.interact.DragonInteractHelper;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonPhysicalModel;
import com.TheRPGAdventurer.ROTD.common.inits.*;
import com.TheRPGAdventurer.ROTD.common.inventory.ContainerDragon;
import com.TheRPGAdventurer.ROTD.common.network.MessageDragonExtras;
import com.TheRPGAdventurer.ROTD.common.network.MessageDragonInventory;
import com.TheRPGAdventurer.ROTD.util.debugging.DebugSettings;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import com.google.common.base.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.ContainerHorseChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.IInventoryChangedListener;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketAnimation;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

import static net.minecraft.entity.SharedMonsterAttributes.ATTACK_DAMAGE;
import static net.minecraft.entity.SharedMonsterAttributes.FOLLOW_RANGE;

/**
 * Here be dragons.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 * @Modifier James Miller <TheRPGAdventurer.>
 *
 *   Usage:
 *   1) If spawning manually:
 *     a) EntityTameableDragon(world)
 *     b) either initialise(breed) or readEntityFromNBT(nbt)
 *
 */
public class EntityTameableDragon extends EntityTameable {
  // base attributes
  public static final double BASE_GROUND_SPEED = 0.4;
  public static final double BASE_AIR_SPEED = 0.9;
  public static final IAttribute MOVEMENT_SPEED_AIR = new RangedAttribute(null, "generic.movementSpeedAir", 0.9, 0.0, Double.MAX_VALUE).setDescription("Movement Speed Air").setShouldWatch(true);
//  public static final double BASE_DAMAGE = DragonMounts.instance.getConfig().BASE_DAMAGE;
//  public static final double BASE_ARMOR = DragonMounts.instance.getConfig().ARMOR;
//  public static final double BASE_TOUGHNESS = 30.0D;
  public static final float RESISTANCE = 10.0f;
  public static final double BASE_FOLLOW_RANGE = 70;
  public static final double BASE_FOLLOW_RANGE_FLYING = BASE_FOLLOW_RANGE * 2;
  public static final int HOME_RADIUS = 64;
  public static final double IN_AIR_THRESH = 10;
  public DragonInventory dragonInv;
  public int inAirTicks;
  public int boostTicks;
  public boolean hasHomePosition = false;
  public int roarTicks;
  public BlockPos homePos;
  public EntityTameableDragonStats dragonStats = new EntityTameableDragonStats();

  public EntityTameableDragon(World world) {
    super(world);
  }

  /** initialise the dragon to the desired breed:
   *  * if the caller has manually constructed the entity, need to call this method
   *  * otherwise, the vanilla constructor will create it and call readFromNBT
   * @param dragonBreed
   */
  public void initialise(DragonBreedNew dragonBreed) {
    // enables walking over blocks
    stepHeight = 1;

    // create entity delegates
    addHelper(new DragonBreedHelper(this, dragonBreed, DATA_BREED, DATA_BREED_NEW));

    dragonPhysicalModel = getBreed().getDragonPhysicalModel();

    addHelper(new DragonLifeStageHelper(this, DATA_TICKS_SINCE_CREATION, dragonBreed.getDragonVariants()));
    addHelper(new DragonReproductionHelper(this, DATA_BREEDER, DATA_REPRO_COUNT));
    addHelper(new DragonBreathHelperP(this, DATA_BREATH_WEAPON_TARGET, DATA_BREATH_WEAPON_MODE));
    addHelper(new DragonInteractHelper(this));
    if (isServer()) addHelper(new DragonBrain(this));

    // set dimensions of full-grown dragon.  The actual width & height is multiplied by the dragon scale (setScale) in EntityAgeable
    final float FULL_SIZE_DRAGON_SCALE = 1.0F;
    float adultWidth = dragonPhysicalModel.getHitboxWidthWC(FULL_SIZE_DRAGON_SCALE);
    float adultHeight = dragonPhysicalModel.getHitboxHeightWC(FULL_SIZE_DRAGON_SCALE);
    setSize(adultWidth, adultHeight);           //todo: later - update it when breed changes

    // init helpers
    moveHelper = new DragonMoveHelper(this);
    aiSit = new EntityAIDragonSit(this);
    helpers.values().forEach(DragonHelper::applyEntityAttributes);
    animator = new DragonAnimator(this);

    InitializeDragonInventory();
  }

  /**
   * (abstract) Protected helper method to write subclass entity data to NBT.
   */

  @Override
  public void writeEntityToNBT(NBTTagCompound nbt) {
    super.writeEntityToNBT(nbt);
    //        nbt.setUniqueId("IdAmulet", this.getUniqueID()); // doesnt save uuid i double checked i/f has this bug makes dragon duplication posible, also why whitle wont work after amulet
    nbt.setBoolean(NBT_SADDLED, isSaddled());
    nbt.setInteger(NBT_ARMOR, this.getArmor());
    nbt.setBoolean(NBT_CHESTED, this.isChested());
//    nbt.setBoolean(NBT_SHEARED, this.isSheared());
//    nbt.setBoolean("Breathing", this.isUsingBreathWeapon());
//    nbt.setBoolean("alt_breathing", this.isUsingAltBreathWeapon());
    nbt.setBoolean("down", this.isGoingDown());
    nbt.setBoolean(NBT_ISMALE, this.isMale());
//    nbt.setBoolean(NBT_ISALBINO, this.isAlbino());
    nbt.setBoolean("unhovered", this.isUnHovered());
    nbt.setBoolean("followyaw", this.followYaw());
    nbt.setBoolean("firesupport", this.firesupport());
    //        nbt.setBoolean("unFluttered", this.isUnFluttered());
    nbt.setInteger("AgeTicks", this.getLifeStageHelper().getTicksSinceCreation());
    nbt.setInteger("hunger", this.getHunger());
    nbt.setBoolean("boosting", this.boosting());
    nbt.setBoolean("ylocked", this.isYLocked());
//    nbt.setBoolean(NBT_ELDER, this.canBeElder());
//    nbt.setBoolean(NBT_ADJUCATOR, this.canBeAdjucator());
    nbt.setBoolean("growthpause", this.isGrowthPaused());
    nbt.setBoolean(NBT_ALLOWOTHERPLAYERS, this.allowedOtherPlayers());
    //        nbt.setBoolean("sleeping", this.isSleeping()); //unused as of now
    nbt.setBoolean("HasHomePosition", this.hasHomePosition);
    if (homePos != null && this.hasHomePosition) {
      nbt.setInteger("HomeAreaX", homePos.getX());
      nbt.setInteger("HomeAreaY", homePos.getY());
      nbt.setInteger("HomeAreaZ", homePos.getZ());
    }
    writeDragonInventory(nbt);
    dragonStats.writeNBT(nbt);
    helpers.values().forEach(helper -> helper.writeToNBT(nbt));
  }

  /**
   * (abstract) Protected helper method to read subclass entity data from NBT.
   */
  @Override
  public void readEntityFromNBT(NBTTagCompound nbt) {
    super.readEntityFromNBT(nbt);

    DragonBreedNew dragonBreed = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getDefaultBreed();
    try {
      dragonBreed = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getBreed(nbt);
    } catch (IllegalArgumentException iae) {
      DragonMounts.loggerLimit.warn_once(iae.getMessage());
    }
    initialise(dragonBreed);

    //        this.setUniqueId(nbt.getUniqueId("IdAmulet")); // doesnt save uuid i double checked i/f has this bug makes dragon duplication posible, also why whitle wont work after amulet
    this.setSaddled(nbt.getBoolean(NBT_SADDLED));
    this.setChested(nbt.getBoolean(NBT_CHESTED));
//    this.setSheared(nbt.getBoolean(NBT_SHEARED));
    this.setHunger(nbt.getInteger("hunger"));
    this.setfiresupport(nbt.getBoolean("firesupport"));
    this.setGrowthPaused(nbt.getBoolean("growthpause"));
//    this.setUsingBreathWeapon(nbt.getBoolean("Breathing"));
//    this.setUsingAltBreathWeapon(nbt.getBoolean("alt_breathing"));
    this.getLifeStageHelper().setTicksSinceCreation(nbt.getInteger("AgeTicks"));
    this.setArmor(nbt.getInteger(NBT_ARMOR));
    this.setMale(nbt.getBoolean(NBT_ISMALE));
//    this.setAlbino(nbt.getBoolean(NBT_ISALBINO));
    this.setGoingDown(nbt.getBoolean("down"));
    this.setUnHovered(nbt.getBoolean("unhovered"));
    this.setYLocked(nbt.getBoolean("ylocked"));
    this.setFollowYaw(nbt.getBoolean("followyaw"));
    //        this.setUnFluttered(nbt.getBoolean("unFluttered"));
    this.setBoosting(nbt.getBoolean("boosting"));
    //        this.setSleeping(nbt.getBoolean("sleeping")); //unused as of now
//    this.setCanBeElder(nbt.getBoolean(NBT_ELDER));
//    this.setCanBeAdjucator(nbt.getBoolean(NBT_ADJUCATOR));
    this.setToAllowedOtherPlayers(nbt.getBoolean(NBT_ALLOWOTHERPLAYERS));
    this.hasHomePosition = nbt.getBoolean("HasHomePosition");
    if (hasHomePosition && nbt.getInteger("HomeAreaX") != 0 && nbt.getInteger("HomeAreaY") != 0 && nbt.getInteger("HomeAreaZ") != 0) {
      homePos = new BlockPos(nbt.getInteger("HomeAreaX"), nbt.getInteger("HomeAreaY"), nbt.getInteger("HomeAreaZ"));
    }
    dragonStats.readNBT(nbt);
    readDragonInventory(nbt);
    helpers.values().forEach(helper -> helper.readFromNBT(nbt));
  }

  /**
   * Returns relative speed multiplier for the vertical flying speed.
   *
   * @return relative vertical speed multiplier
   */
  public double getMoveSpeedAirVert() {
    return this.airSpeedVertical;
  }

  public ItemStack getControllingWhistle() {
    return dataManager.get(WHISTLE);
  }

  public void setControllingWhistle(ItemStack whistle) {
    dataManager.set(WHISTLE, whistle);
  }

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

  public boolean boosting() {
    return dataManager.get(BOOSTING);
  }

  public void setBoosting(boolean allow) {
    dataManager.set(BOOSTING, allow);
  }

  // used to be called isChestedLeft
  public boolean isChested() {
    return dataManager.get(CHESTED);
  }

  public void setChested(boolean chested) {
    dataManager.set(CHESTED, chested);
    hasChestVarChanged = true;
  }

//  public boolean canBeAdjucator() {
//    return dataManager.get(HAS_ADJUCATOR_STONE);
//  }
//
//  public void setCanBeAdjucator(boolean male) {
//    dataManager.set(HAS_ADJUCATOR_STONE, male);
//  }
//
//  public boolean canBeElder() {
//    return dataManager.get(HAS_ELDER_STONE);
//  }
//
//  public void setCanBeElder(boolean male) {
//    dataManager.set(HAS_ELDER_STONE, male);
//  }

  public ItemStack getBanner1() {
    return dataManager.get(BANNER1);
  }

  public void setBanner1(ItemStack bannered) {
    dataManager.set(BANNER1, bannered);
  }

  public ItemStack getBanner2() {
    return dataManager.get(BANNER2);
  }

  public void setBanner2(ItemStack male) {
    dataManager.set(BANNER2, male);
  }

  public ItemStack getBanner3() {
    return dataManager.get(BANNER3);
  }

  public void setBanner3(ItemStack male) {
    dataManager.set(BANNER3, male);
  }

  public ItemStack getBanner4() {
    return dataManager.get(BANNER4);
  }

  public void setBanner4(ItemStack male) {
    dataManager.set(BANNER4, male);
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

  /**
   * Gets the gender since booleans return only 2 values (true or false) true == MALE, false == FEMALE
   * 2 genders only dont call me sexist and dont talk to me about political correctness
   */
  public boolean isMale() {
    return dataManager.get(IS_MALE);
  }

  public void setMale(boolean male) {
    dataManager.set(IS_MALE, male);
  }

  /**
   * set in commands
   */
  public void setOppositeGender() {
    this.setMale(!this.isMale());
  }

//  public boolean isAlbino() {
//    return dataManager.get(IS_ALBINO);
//  }
//
//  public void setAlbino(boolean albino) {
//    dataManager.set(IS_ALBINO, albino);
//  }

  /**
   * 1 equals iron 2 equals gold 3 equals diamond 4 equals emerald
   *
   * @return 0 no armor
   */
  public int getArmor() {
    return this.dataManager.get(ARMOR);
  }

  public void setArmor(int armorType) {
    this.dataManager.set(ARMOR, armorType);
  }

  public boolean canFly() {
    // eggs can't fly
    return !isBaby();
  }

  public boolean isGrowthPaused() {
    return dataManager.get(GROWTH_PAUSED);
  }

  public void setGrowthPaused(boolean paused) {
    dataManager.set(GROWTH_PAUSED, paused);
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

  /**
   * Returns true if the entity is breathing.
   */
  public boolean isUsingBreathWeapon() {
    BreathWeaponTarget breathWeaponTarget = this.getBreathHelperP().getPlayerSelectedTarget();
    return (null != breathWeaponTarget);
  }

//  /**
//   * Returns true if the entity is breathing.
//   */
//  public boolean isUsingAltBreathWeapon() {
//    if (world.isRemote) {
//      boolean usingBreathWeapon = this.dataManager.get(DATA_ALT_BREATHING);
//      this.altBreathing = altBreathing;
//      return altBreathing;
//    }
//    return altBreathing;
//  }

//  /**
//   * Set the breathing flag of the entity.
//   */
//  public void setUsingAltBreathWeapon(boolean altBreathing) {
//    this.dataManager.set(DATA_ALT_BREATHING, altBreathing);
//    if (!world.isRemote) {
//      this.altBreathing = altBreathing;
//    }
//  }

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

  public boolean allowedOtherPlayers() {
    return this.dataManager.get(ALLOW_OTHERPLAYERS);
  }

  public void setToAllowedOtherPlayers(boolean allow) {
    dataManager.set(ALLOW_OTHERPLAYERS, allow);
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
   * Called when the mob is falling. Calculates and applies fall damage.
   */
  @Override
  public void fall(float distance, float damageMultiplier) {
    // ignore fall damage if the entity can fly
    if (!canFly()) {
      super.fall(distance, damageMultiplier);
    }
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

  /**
   * Returns the distance to the ground while the entity is flying.
   */
  public double getAltitude() {
    BlockPos groundPos = world.getHeight(getPosition());
    double altitude = posY - groundPos.getY();
    return altitude;
  }

//  public float getDistanceSquared(Vec3d vec3d) {
//    float f = (float) (this.posX - vec3d.x);
//    float f1 = (float) (this.posY - vec3d.y);
//    float f2 = (float) (this.posZ - vec3d.z);
//    return f * f + f1 * f1 + f2 * f2;
//
//  }

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

  @SideOnly(Side.CLIENT)
  public void updateKeys() {
    Minecraft mc = Minecraft.getMinecraft();
    if ((hasControllingPlayer(mc.player) && getControllingPlayer() != null) || (this.getRidingEntity() instanceof EntityPlayer && this.getRidingEntity() != null && this.getRidingEntity().equals(mc.player)) || (getOwner() != null && firesupport())) {
      boolean breathKeyHeldDownPrimary = ModKeys.KEY_BREATH_PRIMARY.isKeyDown();
      boolean breathKeyHeldDownSecondary = ModKeys.KEY_BREATH_SECONDARY.isKeyDown();
      BreathWeaponTarget.WeaponUsed breathWeaponUsed = BreathWeaponTarget.WeaponUsed.NONE;
      boolean breathKeyHeldDownEither = breathKeyHeldDownPrimary || breathKeyHeldDownSecondary;
      if (breathKeyHeldDownPrimary) {
        breathWeaponUsed = BreathWeaponTarget.WeaponUsed.PRIMARY;
      } else if (breathKeyHeldDownSecondary) {
        breathWeaponUsed = BreathWeaponTarget.WeaponUsed.SECONDARY;
      }
      DragonOrbControl.getInstance().setKeyBreathState(this, breathKeyHeldDownEither, breathWeaponUsed);

      boolean isBoosting = ModKeys.BOOST.isKeyDown();
      boolean isDown = ModKeys.DOWN.isKeyDown();
      boolean unhover = ModKeys.KEY_HOVERCANCEL.isPressed();
      boolean followyaw = ModKeys.FOLLOW_YAW.isPressed();
      boolean locky = ModKeys.KEY_LOCKEDY.isPressed();

//      DragonMounts.NETWORK_WRAPPER.sendToServer(new MessageDragonBreath(getEntityId(), isBreathing, projectile));
      DragonMounts.NETWORK_WRAPPER.sendToServer(new MessageDragonExtras(getEntityId(), unhover, followyaw, locky, isBoosting, isDown));
    }
  }

  @Override
  public void onUpdate() {
    if (DebugSettings.isDragonWalkStraightLine()) {
      this.rotationYaw = -90.0F;
      this.motionX = DebugSettings.getDragonWalkSpeed();
      this.motionY = 0;
      this.motionZ = 0;
    }
    super.onUpdate();
    if (world.isRemote) {
      this.updateKeys();
      dragonStats.onUpdate(this);
    }
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

  @Override
  public void onEntityUpdate() {
    if (DebugSettings.isAnimationFrozen()) {
      return;
    }
    if (getRNG().nextInt(800) == 1) roar();
    super.onEntityUpdate();
  }

  @Override
  public void onLivingUpdate() {
    if (DebugSettings.existsDebugParameter("dragonyaw")) {
      this.renderYawOffset = (float) DebugSettings.getDebugParameter("dragonyaw");
      this.prevRenderYawOffset = renderYawOffset;
      this.rotationYaw = renderYawOffset;
    }

    if (DebugSettings.isAnimationFrozen()) return;

    helpers.values().forEach(DragonHelper::onLivingUpdate);
    getBreed().onLivingUpdate(this);

    if (isServer()) {
      final float DUMMY_MOVETIME = 0;
      final float DUMMY_MOVESPEED = 0;
      animator.setMovement(DUMMY_MOVETIME, DUMMY_MOVESPEED);
      float netYawHead = getRotationYawHead() - renderYawOffset;
      animator.setLook(netYawHead, rotationPitch);
      animator.tickingUpdate();
      animator.animate();

      // set home position near owner when tamed
      if (isTamed()) {
        Entity owner = getOwner();
        if (owner != null) {
          setHomePosAndDistance(owner.getPosition(), HOME_RADIUS);
        }
      }

      // delay flying state for 10 ticks (0.5s)
      if (onSolidGround()) {
        inAirTicks = 0;
      } else {
        inAirTicks++;
      }

      if (boosting()) {
        boostTicks++;
      } else {
        boostTicks--;
      }

      boolean flying = canFly() && inAirTicks > IN_AIR_THRESH && (!isInWater() || !isInLava() && getControllingPlayer() != null);
      if (flying != isFlying()) {

        // notify client
        setFlying(flying);

        // clear tasks (needs to be done before switching the navigator!)
        //			getBrain().clearTasks();

        // update AI follow range (needs to be updated before creating
        // new PathNavigate!)
        getEntityAttribute(FOLLOW_RANGE).setBaseValue(getDragonSpeed());

        // update pathfinding method
        if (isFlying()) {
          navigator = new PathNavigateFlying(this, world);
        } else {
          navigator = new PathNavigateGround(this, world);
        }

        // tasks need to be updated after switching modes
        getBrain().updateAITasks();

      }

      ItemStack whistle = this.getControllingWhistle();
      if (whistle != null && whistle.getTagCompound() != null && !whistle.getTagCompound().getUniqueId(DragonMounts.MODID + "dragon").equals(this.getUniqueID()) && whistle.hasTagCompound()) {
        this.setnothing(true);
      }

    } else {
      animator.tickingUpdate();
    }

    if (ticksSinceLastAttack >= 0) { // used for jaw animation
      ++ticksSinceLastAttack;
      if (ticksSinceLastAttack > 1000) {
        ticksSinceLastAttack = -1; // reset at arbitrary large value
      }
    }

    if (roarTicks >= 0) { // used for jaw animation
      ++roarTicks;
      if (roarTicks > 1000) {
        roarTicks = -1; // reset at arbitrary large value
      }
    }


    if (this.getRidingEntity() instanceof EntityLivingBase) {
      EntityLivingBase ridingEntity = (EntityLivingBase) this.getRidingEntity();
      if (ridingEntity.isElytraFlying() && ridingEntity != null) {
        this.setUnHovered(true);
      }
    }
    if (this.ticksExisted % (DragonMounts.instance.getConfig().hungerDecrement) == 1) {
      if (this.getHunger() > 0) {
        this.setHunger(this.getHunger() - 1);
      }
    }

//        if (this.isUsingBreathWeapon() && this.getBreed().canUseBreathWeapon() && this.getControllingPlayer()!=null && (this.isUsingBreathWeapon())) {
//            this.equalizeYaw(this.getControllingPlayer());
//        }

    // if we're breathing at a target, look at it
    if (isUsingBreathWeapon()) {
      Vec3d dragonEyePos = this.getPositionVector().addVector(0, this.getEyeHeight(), 0);
      BreathWeaponTarget breathWeaponTarget = this.getBreathHelperP().getPlayerSelectedTarget();
      if (breathWeaponTarget != null) {
        breathWeaponTarget.setEntityLook(this.world, this.getLookHelper(), dragonEyePos,
                this.getHeadYawSpeed(), this.getHeadPitchSpeed());
      }
    }

    if (getOwner() != null && firesupport()) {
      Vec3d dragonEyePos = this.getPositionVector().addVector(0, this.getEyeHeight(), 0);
      Vec3d lookDirection = getOwner().getLook(1.0F);
      Vec3d endOfLook = dragonEyePos.addVector(lookDirection.x, lookDirection.y, lookDirection.z); // todo fix the head looking down
      this.getLookHelper().setLookPosition(lookDirection.x, lookDirection.y, lookDirection.z,
              120, 90);
    }

    if (hasChestVarChanged && dragonInv != null && !this.isChested()) {
      for (int i = ContainerDragon.chestStartIndex; i < 30; i++) {
        if (!dragonInv.getStackInSlot(i).isEmpty()) {
          if (!world.isRemote) {
            this.entityDropItem(dragonInv.getStackInSlot(i), 1);
          }
          dragonInv.removeStackFromSlot(i);
        }
      }
      hasChestVarChanged = false;
    }

    if (this.isPotionActive(MobEffects.WEAKNESS)) {
      this.removePotionEffect(MobEffects.WEAKNESS);
    }

    doBlockCollisions();
    List<Entity> list = this.world.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox().grow(0.2, -0.01, 0.2),
            EntitySelectors.getTeamCollisionPredicate(this));

    if (getControllingPlayer() == null && !isFlying() && isSitting()) {
      removePassengers();
    }

    Random rand = new Random();
    if (this.getBreed().getSneezeParticle() != null && rand.nextInt(750) == 1 && !this.isUsingBreathWeapon() && !isBaby()) {
      double throatPosX = (this.getAnimator().getThroatPosition().x);
      double throatPosY = (this.getAnimator().getThroatPosition().z);
      double throatPosZ = (this.getAnimator().getThroatPosition().y + 1.7);
      world.spawnParticle(this.getBreed().getSneezeParticle(), throatPosX, throatPosY, throatPosZ, 0, 0.3, 0);
      world.spawnParticle(this.getBreed().getSneezeParticle(), throatPosX, throatPosY, throatPosZ, 0, 0.3, 0);
      world.spawnParticle(this.getBreed().getSneezeParticle(), throatPosX, throatPosY, throatPosZ, 0, 0.3, 0);
      world.playSound(null, new BlockPos(throatPosX, throatPosY, throatPosZ), ModSounds.DRAGON_SNEEZE, SoundCategory.NEUTRAL, 1, 1);
    }

    super.onLivingUpdate();
  }

  public void spawnBodyParticle(EnumParticleTypes type) {
    double ox, oy, oz;
    float s = this.getAgeScale() * 1.2f;

    switch (type) {
      case EXPLOSION_NORMAL:
        ox = rand.nextGaussian() * s;
        oy = rand.nextGaussian() * s;
        oz = rand.nextGaussian() * s;
        break;

      case CLOUD:
        ox = (rand.nextDouble() - 0.5) * 0.1;
        oy = rand.nextDouble() * 0.2;
        oz = (rand.nextDouble() - 0.5) * 0.1;
        break;

      case REDSTONE:
        ox = 0.8;
        oy = 0;
        oz = 0.8;
        break;

      default:
        ox = 0;
        oy = 0;
        oz = 0;
    }

    // use generic random box spawning
    double x = this.posX + (rand.nextDouble() - 0.5) * this.width * s;
    double y = this.posY + (rand.nextDouble() - 0.5) * this.height * s;
    double z = this.posZ + (rand.nextDouble() - 0.5) * this.width * s;

    this.world.spawnParticle(type, x, y, z, ox, oy, oz);
  }

  public void spawnBodyParticles(EnumParticleTypes type, int baseAmount) {
    int amount = (int) (baseAmount * this.getAgeScale());
    for (int i = 0; i < amount; i++) {
      spawnBodyParticle(type);
    }
  }

  /**
   * Called when the mob's health reaches 0.
   */
  public void onDeath(DamageSource src) {
    super.onDeath(src);
    if (dragonInv != null && !this.world.isRemote && !isTamed()) {
      for (int i = 0; i < dragonInv.getSizeInventory(); ++i) {
        ItemStack itemstack = dragonInv.getStackInSlot(i);
        if (!itemstack.isEmpty()) {
          this.entityDropItem(itemstack, 0.0F);
        }
      }
    }
  }

  @Override
  public void setDead() {
    helpers.values().forEach(DragonHelper::onDeath);
    super.setDead();
  }

  @Override
  public ITextComponent getDisplayName() {
    // return custom name if set
    String s = this.getCustomNameTag();
    if (s != null && !s.isEmpty()) {
      TextComponentString textcomponentstring = new TextComponentString(s);
      return textcomponentstring;
    }

    // return default breed name otherwise
    String entName = EntityList.getEntityString(this);
    String breedName = getBreed().getSkin().toLowerCase();
    ITextComponent name = new TextComponentTranslation("entity." + entName + "." + breedName + ".name");
    return name;
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

  public void roar() {
    if (!isDead && getBreed().getRoarSoundEvent(this) != null && !isUsingBreathWeapon()) {
      this.roarTicks = 0; // MathX.clamp(getAgeScale(), 0.88f
      world.playSound(posX, posY, posZ, getBreed().getRoarSoundEvent(this), SoundCategory.NEUTRAL, MathX.clamp(getAgeScale(), 0.4F, 1.0F), getSoundPitch(), true);
      // sound volume should be between 0 - 1, and scale is also 0 - 1
    }
  }

  /**
   * Returns the sound this mob makes while it's alive.
   */
  public SoundEvent getLivingSound() {
    if (isUsingBreathWeapon()) return null;
    else return getBreed().getLivingSound(this);
  }

  /**
   * Returns the sound this mob makes when it is hurt.
   */
  @Override
  public SoundEvent getHurtSound(DamageSource src) {
    return getBreed().getHurtSound();
  }

  public SoundEvent getWingsSound() {
    return getBreed().getWingsSound();
  }

  public SoundEvent getStepSound() {
    return getBreed().getStepSound();
  }

  public SoundEvent getEatSound() {
    return getBreed().getEatSound();
  }

  public SoundEvent getAttackSound() {
    return getBreed().getAttackSound();
  }

  /**
   * Plays living's sound at its position
   */
  public void playLivingSound() {
    SoundEvent sound = getLivingSound();
    if (sound == null || isUsingBreathWeapon()) {
      return;
    }

    playSound(sound, 0.7f, 1);
  }

  /**
   * Get number of ticks, at least during which the living entity will be silent.
   */
  public int getTalkInterval() {
    return 240;
  }

  /**
   * Client side method for wing animations. Plays wing flapping sounds.
   *
   * @param speed wing animation playback speed
   */
  public void onWingsDown(float speed) {
    if (!isInWater() && isFlying()) {
      // play wing sounds
      float pitch = (1);
      float volume = 1f + (1 - speed);
      playSound(getWingsSound(), volume, pitch, false);
    }
  }

  /**
   * Plays step sound at given x, y, z for the entity
   */
  public void playStepSound(BlockPos entityPos, Block block) {
    // no sounds for  underwater action
    if (isInWater() || isOverWater()) return;

    if (isFlying() || isSitting()) return;

    SoundEvent stepSound;
    // baby has quiet steps, larger have stomping sound
    if (isBaby()) {
      SoundType soundType;
      // override sound type if the top block is snowy
      if (world.getBlockState(entityPos.up()).getBlock() == Blocks.SNOW_LAYER)
        soundType = Blocks.SNOW_LAYER.getSoundType();
      else
        soundType = block.getSoundType();
      stepSound = soundType.getStepSound();
    } else {
      stepSound = getStepSound();
    }
    playSound(stepSound, 1f, 1f, false);
  }

  public void playSound(SoundEvent sound, float volume, float pitch, boolean local) {
    if (sound == null || isSilent()) {
      return;
    }

    volume *= getVolume(sound);
    pitch *= getSoundPitch();

    if (local) world.playSound(posX, posY, posZ, sound, getSoundCategory(), volume, pitch, false);
    else world.playSound(null, posX, posY, posZ, sound, getSoundCategory(), volume, pitch);
  }

  public void playSound(SoundEvent sound, float volume, float pitch) {
    playSound(sound, volume, pitch, false);
  }

  /**
   * Returns the volume for a sound to play.
   */
  public float getVolume(SoundEvent sound) {
    return MathX.clamp(getAgeScale(), 0, 1.0F);
  }

  /**
   * Get this Entity's EnumCreatureAttribute
   */
  @Override
  public EnumCreatureAttribute getCreatureAttribute() {
    return getBreed().getCreatureAttribute();
  }

  /**
   * Called when a player interacts with a mob. e.g. gets milk from a cow, gets into the saddle on a pig.
   */
  @Override
  public boolean processInteract(EntityPlayer player, EnumHand hand) {
    ItemStack item = player.getHeldItem(hand);

    ItemStack itemstack = player.getHeldItem(hand);

    if (itemstack.getItem() == Items.BUCKET && !player.capabilities.isCreativeMode && !this.isChild() && DragonMounts.instance.getConfig().canMilk) {
      player.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
      itemstack.shrink(1);

      if (itemstack.isEmpty()) {
        player.setHeldItem(hand, new ItemStack(Items.MILK_BUCKET));
      } else if (!player.inventory.addItemStackToInventory(new ItemStack(Items.MILK_BUCKET))) {
        player.dropItem(new ItemStack(Items.MILK_BUCKET), false);
      }

      return true;
    }

    if (getHealth() <= 0) return false;

    // if the dragon is small enough, put it on the player's shoulder
    if (this.isTamedFor(player) && this.isBaby() && !player.isSneaking() && !DragonInteractBase.hasInteractItemsEquipped(player)) {
      this.setSitting(false);
      this.startRiding(player, true);
      return true;
    }

    if (player.isPassenger(this)) {
      return false;
    }

    return getInteractHelper().interact(player, item);
  }

  public void tamedFor(EntityPlayer player, boolean successful) {
    if (successful) {
      setTamed(true);
      navigator.clearPath(); // replacement for setPathToEntity(null);
      setAttackTarget(null);
      setOwnerId(player.getUniqueID());
      playTameEffect(true);
      world.setEntityState(this, (byte) 7);
    } else {
      playTameEffect(false);
      world.setEntityState(this, (byte) 6);
    }
  }

  public boolean isTamedFor(EntityPlayer player) {
    return isTamed() && isOwner(player);
  }

  /**
   * Checks if the parameter is an item which this animal can be fed to breed it
   * (wheat, carrots or seeds depending on the animal type)
   */
  @Override
  public boolean isBreedingItem(ItemStack item) {
    return getBreed().getBreedingItem() == item.getItem();
  }

  /**
   * Returns the height of the eyes. Used for looking at other entities.
   */
  @Override
  public float getEyeHeight() {
    float eyeHeight = dragonPhysicalModel.getEyeHeightWC(getAgeScale(), isSitting());
    return eyeHeight;
  }

  /**
   * Returns the Y offset from the entity's position for any entity riding this
   * one.
   * May not be necessary since we also override updatePassenger()
   */
  @Override
  public double getMountedYOffset() {
    final int DEFAULT_PASSENGER_NUMBER = 0;
    return dragonPhysicalModel.getRiderPositionOffsetWC(getAgeScale(), getBodyPitch(), isSitting(), DEFAULT_PASSENGER_NUMBER).y;
  }

  /**
   * Returns render size modifier for the shadow
   */
  @Override
  public float getRenderSizeModifier() {
    return getAgeScale() / (isChild() ? 0.5F : 1.0F);
//  0.5 isChild() correction is required due to the code in Render::renderShadow which shrinks the shadow for a child
//    if (entityIn instanceof EntityLiving)
//    {
//      EntityLiving entityliving = (EntityLiving)entityIn;
//      f *= entityliving.getRenderSizeModifier();
//
//      if (entityliving.isChild())
//      {
//        f *= 0.5F;
//      }
//    }

  }

  @SideOnly(Side.CLIENT)
  @Override
  // makes the visual rendering limit of the dragon bigger (otherwise the head or tail sometimes doesn't render when you
  //   can't see the body AABB)
  // The dragon is so big that parts of the dragon will still sometimes disappear if the dragon posX, posZ is not in the
  //   same chunk as the player - this is due to the way vanilla eliminates chunks it won't render, there's
  //   not much we can do about that.  It only happens when the player is right up close to the dragon
  public AxisAlignedBB getRenderBoundingBox() {
    // the dragon visual limits are up to four times the body radius, including the tail
    AxisAlignedBB bodyAABB = this.getEntityBoundingBox();
    double halfwidth = (bodyAABB.maxX - bodyAABB.minX) / 2.0;  // width is equal in x and z directions
    double extraRadius = 3 * halfwidth;
    return new AxisAlignedBB(bodyAABB.minX - extraRadius, bodyAABB.minY, bodyAABB.minZ - extraRadius,
            bodyAABB.maxX + extraRadius, bodyAABB.maxY, bodyAABB.maxZ + extraRadius);
  }

  /**
   * Returns true if this entity should push and be pushed by other entities when
   * colliding.
   */
  @Override
  public boolean canBePushed() {
    return super.canBePushed();
  }

  /**
   * returns true if this entity is by a ladder, false otherwise
   */
  @Override
  public boolean isOnLadder() {
    // this better doesn't happen...
    return false;
  }

  /**
   * Called when an entity attacks
   */
  public boolean attackEntityAsMob(Entity entityIn) {
    boolean attacked = entityIn.attackEntityFrom(DamageSource.causeMobDamage(this), (float) getEntityAttribute(ATTACK_DAMAGE).getAttributeValue());

    if (attacked) {
      applyEnchantments(this, entityIn);
    }

    if (!this.nothing()) {
      return false;
    }

//    if (getBreedType() == EnumDragonBreed.WITHER) {
//      ((EntityLivingBase) entityIn).addPotionEffect(new PotionEffect(MobEffects.WITHER, 200));
//    }

    return attacked;
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
   * 1 equals iron 2 equals gold 3 equals diamond 4 equals emerald
   *
   * @return 0 no armor
   */
  public double getArmorResistance() {
    switch (getArmor()) {
      case 1:
        return 1.4;
      case 2:
        return 1.2;
      case 3:
        return 1.7;
      case 4:
        return 1.4;
      default:
        return 0;
    }
  }

  /**
   * Return whether this entity should be rendered as on fire.
   */
  @Override
  public boolean canRenderOnFire() {
    return super.canRenderOnFire() && !getBreed().isImmuneToDamage(DamageSource.IN_FIRE);
  }

  /**
   * Returns true if the mob is currently able to mate with the specified mob.
   */
  @Override
  public boolean canMateWith(EntityAnimal mate) {
    return getReproductionHelper().canMateWith(mate);
  }

  /**
   * This function is used when two same-species animals in 'love mode' breed to
   * generate the new baby animal.
   */
  @Override
  public EntityAgeable createChild(EntityAgeable mate) {
    EntityTameableDragon parent1 = this;
    EntityTameableDragon parent2 = (EntityTameableDragon) mate;

    if (parent1.isMale() && !parent2.isMale() || !parent1.isMale() && parent2.isMale()) {
      return getReproductionHelper().createChild(parent1.isMale() ? mate : parent1);
    } else {
      return null;
    }
  }

  public DragonBreedHelper getBreedHelper() {
    return getHelper(DragonBreedHelper.class);
  }

  public DragonLifeStageHelper getLifeStageHelper() {
    return getHelper(DragonLifeStageHelper.class);
  }

  public DragonReproductionHelper getReproductionHelper() {
    return getHelper(DragonReproductionHelper.class);
  }

  public DragonBreathHelperP getBreathHelperP() {
    return getHelper(DragonBreathHelperP.class);
  }

  public DragonAnimator getAnimator() {
    return animator;
  }

  public DragonBrain getBrain() {
    return getHelper(DragonBrain.class);
  }

  public DragonInteractHelper getInteractHelper() {
    return getHelper(DragonInteractHelper.class);
  }

  /**
   * Returns the breed for this
   *
   * @return breed
   */
  public EnumDragonBreed getBreedType() {
    return getBreedHelper().getBreedType();
  }

  /**
   * Sets the new breed for this
   *
   * @param type new breed
   */
  public void setBreedType(EnumDragonBreed type) {
    getBreedHelper().setBreedType(type);
  }

  public DragonPhysicalModel getPhysicalModel() {
    return dragonPhysicalModel;
  }

  public DragonBreed getBreed() {
    return getBreedType().getBreed();
  }

  public double getDragonSpeed() {
    return isFlying() ? BASE_FOLLOW_RANGE_FLYING : BASE_FOLLOW_RANGE;
  }

  @Override
  public boolean canBeSteered() {
    //         must always return false or the vanilla movement code interferes
    //         with DragonMoveHelper
    return false;
  }

  public double getFlySpeed() {
    return this.boosting() ? 4 : 1;
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

  @Override
  public void travel(float strafe, float forward, float vertical) {
    // disable method while flying, the movement is done entirely by
    // moveEntity() and this one just makes the dragon to fall slowly when
    // hovering
    if (!isFlying()) {
      super.travel(strafe, forward, vertical);
    }
  }

  @Nullable
  public Entity getControllingPassenger() {
    return this.getPassengers().isEmpty() ? null : getPassengers().get(0);
  }

  @Nullable
  public EntityPlayer getControllingPlayer() {
    Entity entity = this.getPassengers().isEmpty() ? null : getPassengers().get(0);
    if (entity instanceof EntityPlayer) {
      return (EntityPlayer) entity;
    } else {
      return null;
    }
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

  @Override
  public void updateRidden() {
    Entity entity = this.getRidingEntity();
    this.motionX = 0.0D;
    this.motionY = 0.0D;
    this.motionZ = 0.0D;
    this.onUpdate();
    if (this.isRiding()) this.updateRiding((EntityLivingBase) entity);
  }

  public boolean isRidingAboveGround(Entity entityBeingRidden) {
    int groundPos = world.getHeight(getPosition()).getY();
    double altitude = entityBeingRidden.posY - groundPos;
    return altitude > 2.0;
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

  @Override
  public boolean isEntityInvulnerable(DamageSource src) {
    Entity srcEnt = src.getImmediateSource();
    if (srcEnt != null) {
      // ignore own damage
      if (srcEnt == this) {
        return true;
      }

      // ignore damage from riders
      if (isPassenger(srcEnt)) {
        return true;
      }
    }

    return getBreed().isImmuneToDamage(src);
  }

  /**
   * Returns the entity's health relative to the maximum health.
   *
   * @return health normalized between 0 and 1
   */
  public double getHealthRelative() {
    return getHealth() / (double) getMaxHealth();
  }

  public int getDeathTime() {
    return deathTime;
  }

  public int getMaxDeathTime() {
    return 120;
  }

  public boolean canBeLeashedTo(EntityPlayer player) {
    return true;
  }

  public void setImmuneToFire(boolean isImmuneToFire) {
    L.trace("setImmuneToFire({})", isImmuneToFire);
    this.isImmuneToFire = isImmuneToFire;
  }

  public void setAttackDamage(double damage) {
    L.trace("setAttackDamage({})", damage);
    getEntityAttribute(ATTACK_DAMAGE).setBaseValue(damage);
  }

  /**
   * Public wrapper for protected final setScale(), used by DragonLifeStageHelper.
   *
   * @param ageScale
   */
  public void setAgeScalePublic(float ageScale) {
    double posXTmp = posX;
    double posYTmp = posY;
    double posZTmp = posZ;
    boolean onGroundTmp = onGround;

    setScale(ageScale);

    // workaround for a vanilla bug; the position is apparently not set correctly
    // after changing the entity size, causing asynchronous server/client
    // positioning
    setPosition(posXTmp, posYTmp, posZTmp);

    // otherwise, setScale stops the dragon from landing while it is growing
    onGround = onGroundTmp;
  }

  /**
   * The age value may be negative or positive or zero. If it's negative, it get's
   * incremented on each tick, if it's positive, it get's decremented each tick.
   * Don't confuse this with EntityLiving.getAge. With a negative value the Entity
   * is considered a child.
   */
  @Override
  public int getGrowingAge() {
    // adapter for vanilla code to enable breeding interaction
    return isAdult() ? 0 : -1;
  }

  /**
   * The age value may be negative or positive or zero. If it's negative, it get's
   * incremented on each tick, if it's positive, it get's decremented each tick.
   * With a negative value the Entity is considered a child.
   */
  @Override
  public void setGrowingAge(int age) {
    // managed by DragonLifeStageHelper, so this is a no-op
  }

  /**
   * Sets the scale for an ageable entity according to the boolean parameter,
   * which says if it's a child.
   */
  @Override
  public void setScaleForAge(boolean child) {
    // managed by DragonLifeStageHelper, so this is a no-op
  }

  @Override
  public boolean shouldDismountInWater(Entity rider) {
    return false;
  }

  /**
   * Returns the size multiplier for the current age.
   *
   * @return scale
   */
  public float getAgeScale() {
    return getLifeStageHelper().getAgeScale();
  }

  public boolean isBaby() {
    return getLifeStageHelper().isBaby();
  }

  /**
   * Calls both hatchling and infant since infant is just another stage to reduce growth speed
   *
   * @return
   */
//    public boolean isHatchling() {
//        return getLifeStageHelper().isHatchling() || getLifeStageHelper().isInfant();
//    }
//
//    public boolean isInfant() {
//        return getLifeStageHelper().isInfant();
//    }
//
//    public boolean isJuvenile() {
//        return getLifeStageHelper().isJuvenile() || getLifeStageHelper().isPreJuvenile();
//    }
//
  public boolean isAdult() {
    return getLifeStageHelper().isFullyGrown();
  }

  @Override
  public boolean isChild() {
    return getLifeStageHelper().isBaby();
  }

  /**
   * Checks if this entity is running on a client.
   * <p>
   * Required since MCP's isClientWorld returns the exact opposite...
   *
   * @return true if the entity runs on a client or false if it runs on a server
   */
  public final boolean isClient() {
    return world.isRemote;
  }

  /**
   * Checks if this entity is running on a server.
   *
   * @return true if the entity runs on a server or false if it runs on a client
   */
  public final boolean isServer() {
    return !world.isRemote;
  }

//  public boolean isSheared() {
//    return (this.dataManager.get(DRAGON_SCALES).byteValue() & 16) != 0;
//  }

//  /**
//   * make a dragon sheared if set to true
//   */
//  public void setSheared(boolean sheared) {
//    byte b0 = this.dataManager.get(DRAGON_SCALES).byteValue();
//
//    if (sheared) {
//      dataManager.set(DRAGON_SCALES, Byte.valueOf((byte) (b0 | 16)));
//    } else {
//      dataManager.set(DRAGON_SCALES, Byte.valueOf((byte) (b0 & -17)));
//    }
//  }

  public int getHunger() {
    return dataManager.get(HUNGER);
  }

  public void setHunger(int hunger) {
    this.dataManager.set(HUNGER, Math.min(100, hunger));
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

  /**
   * Called when a lightning bolt hits the entity.
   */
  @Override
  public void onStruckByLightning(EntityLightningBolt lightningBolt) {
    EnumDragonBreed currentType = getBreedType();
    super.onStruckByLightning(lightningBolt);
//    if (currentType == EnumDragonBreed.SKELETON) {
//      this.setBreedType(EnumDragonBreed.WITHER);
//
//      this.playSound(SoundEvents.BLOCK_PORTAL_TRIGGER, 2, 1);
//      this.playSound(SoundEvents.BLOCK_END_PORTAL_SPAWN, 2, 1);
//    }
//
//    if (currentType == EnumDragonBreed.SYLPHID) {
//      this.setBreedType(EnumDragonBreed.STORM);
//
//      this.playSound(SoundEvents.BLOCK_PORTAL_TRIGGER, 2, 1);
//      this.playSound(SoundEvents.BLOCK_END_PORTAL_SPAWN, 2, 1);
//    }

    addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 35 * 20));
  }

  /**
   * Checks if the dragon's health is not full and not zero.
   */
  public boolean shouldHeal() {
    return this.getHealth() > 0.0F && this.getHealth() < this.getMaxHealth();
  }

  @Override
  public boolean shouldAttackEntity(EntityLivingBase target, EntityLivingBase owner) {
    if (!target.isChild()) {
      if (target instanceof EntityTameable) {
        EntityTameable tamedEntity = (EntityTameable) target;
        if (tamedEntity.isTamed()) {
          return false;
        }
      }

      if (target instanceof EntityPlayer) {
        EntityPlayer playertarget = (EntityPlayer) target;
        if (this.isTamedFor(playertarget)) {
          return false;
        }
      }

      if (target.hasCustomName()) {
        return false;
      }

    }

    return super.shouldAttackEntity(target, owner);
  }

  public boolean canFitPassenger(Entity passenger) {
    return this.getPassengers().size() < dragonPhysicalModel.getMaxNumberOfPassengers(getLifeStageHelper().getLifeStage());
  }

  /**
   * Credits: AlexThe 666 Ice and Fire
   */
  public void openGUI(EntityPlayer playerEntity, int guiId) {
    if (!this.world.isRemote && (!this.isPassenger(playerEntity))) {
      playerEntity.openGui(DragonMounts.instance, guiId, this.world, this.getEntityId(), 0, 0);
    }
  }

  /**
   * Credits: AlexThe 666 Ice and Fire
   */
  public boolean replaceItemInInventory(int inventorySlot, @Nullable ItemStack itemStackIn) {
    int j = inventorySlot - 500 + 2;
    if (j >= 0 && j < this.dragonInv.getSizeInventory()) {
      this.dragonInv.setInventorySlotContents(j, itemStackIn);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Credits: AlexThe 666 Ice and Fire
   */
  public void readDragonInventory(NBTTagCompound nbt) {
    if (dragonInv != null) {
      NBTTagList nbttaglist = nbt.getTagList("Items", 10);
      InitializeDragonInventory();
      for (int i = 0; i < nbttaglist.tagCount(); ++i) {
        NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
        int j = nbttagcompound.getByte("Slot") & 255;
        this.dragonInv.setInventorySlotContents(j, new ItemStack(nbttagcompound));
      }
    } else {
      NBTTagList nbttaglist = nbt.getTagList("Items", 10);
      InitializeDragonInventory();
      for (int i = 0; i < nbttaglist.tagCount(); ++i) {
        NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
        int j = nbttagcompound.getByte("Slot") & 255;
        this.InitializeDragonInventory();
        this.dragonInv.setInventorySlotContents(j, new ItemStack(nbttagcompound));

        ItemStack saddle = dragonInv.getStackInSlot(0);
        ItemStack chest = dragonInv.getStackInSlot(1);
        ItemStack banner1 = dragonInv.getStackInSlot(31);
        ItemStack banner2 = dragonInv.getStackInSlot(32);
        ItemStack banner3 = dragonInv.getStackInSlot(33);
        ItemStack banner4 = dragonInv.getStackInSlot(34);

        if (world.isRemote) {
          n.sendToServer(new MessageDragonInventory(this.getEntityId(), 0, saddle != null && saddle.getItem() == Items.SADDLE && !saddle.isEmpty() ? 1 : 0));

          n.sendToServer(new MessageDragonInventory(this.getEntityId(), 1, chest != null && chest.getItem() == Item.getItemFromBlock(Blocks.CHEST) && !chest.isEmpty() ? 1 : 0));

          // maybe later we reintroduce armour?
//          n.sendToServer(new MessageDragonInventory(this.getEntityId(), 2, this.getIntFromArmor(dragonInv.getStackInSlot(2))));

          n.sendToServer(new MessageDragonInventory(this.getEntityId(), 31, banner1 != null && banner1.getItem() == Items.BANNER && !banner1.isEmpty() ? 1 : 0));

          n.sendToServer(new MessageDragonInventory(this.getEntityId(), 32, banner2 != null && banner2.getItem() == Items.BANNER && !banner2.isEmpty() ? 1 : 0));

          n.sendToServer(new MessageDragonInventory(this.getEntityId(), 33, banner3 != null && banner3.getItem() == Items.BANNER && !banner3.isEmpty() ? 1 : 0));

          n.sendToServer(new MessageDragonInventory(this.getEntityId(), 34, banner4 != null && banner4.getItem() == Items.BANNER && !banner4.isEmpty() ? 1 : 0));
        }
      }
    }
  }

  /**
   * Credits: AlexThe 666 Ice and Fire
   */
  public void refreshInventory() {
    ItemStack saddle = this.dragonInv.getStackInSlot(0);
    ItemStack leftChestforInv = this.dragonInv.getStackInSlot(1);
    ItemStack banner1 = this.dragonInv.getStackInSlot(31);
    ItemStack banner2 = this.dragonInv.getStackInSlot(32);
    ItemStack banner3 = this.dragonInv.getStackInSlot(33);
    ItemStack banner4 = this.dragonInv.getStackInSlot(34);

    this.setSaddled(saddle != null && saddle.getItem() == Items.SADDLE && !saddle.isEmpty());
    this.setChested(leftChestforInv != null && leftChestforInv.getItem() == Item.getItemFromBlock(Blocks.CHEST) && !leftChestforInv.isEmpty());

    this.setBanner1(banner1);
    this.setBanner2(banner2);
    this.setBanner3(banner3);
    this.setBanner4(banner4);
//    this.setArmor(getIntFromArmor(this.dragonInv.getStackInSlot(2)));

    if (this.world.isRemote) {
      n.sendToServer(new MessageDragonInventory(this.getEntityId(), 0, saddle != null && saddle.getItem() == Items.SADDLE && !saddle.isEmpty() ? 1 : 0));
      n.sendToServer(new MessageDragonInventory(this.getEntityId(), 1, leftChestforInv != null && leftChestforInv.getItem() == Item.getItemFromBlock(Blocks.CHEST) && !leftChestforInv.isEmpty() ? 1 : 0));
//      n.sendToServer(new MessageDragonInventory(this.getEntityId(), 2, this.getIntFromArmor(this.dragonInv.getStackInSlot(2))));
      n.sendToServer(new MessageDragonInventory(this.getEntityId(), 31, banner1 != null && banner1.getItem() == Items.BANNER && !banner1.isEmpty() ? 1 : 0));
      n.sendToServer(new MessageDragonInventory(this.getEntityId(), 32, banner2 != null && banner2.getItem() == Items.BANNER && !banner2.isEmpty() ? 1 : 0));
      n.sendToServer(new MessageDragonInventory(this.getEntityId(), 31, banner3 != null && banner3.getItem() == Items.BANNER && !banner3.isEmpty() ? 1 : 0));
      n.sendToServer(new MessageDragonInventory(this.getEntityId(), 32, banner4 != null && banner4.getItem() == Items.BANNER && !banner4.isEmpty() ? 1 : 0));


    }
  }

  /**
   * Credits: AlexThe 666 Ice and Fire
   */
  public void writeDragonInventory(NBTTagCompound nbt) {
    if (dragonInv != null) {
      NBTTagList nbttaglist = new NBTTagList();
      for (int i = 0; i < this.dragonInv.getSizeInventory(); ++i) {
        ItemStack itemstack = this.dragonInv.getStackInSlot(i);
        if (!itemstack.isEmpty()) {
          NBTTagCompound nbttagcompound = new NBTTagCompound();
          nbttagcompound.setByte("Slot", (byte) i);
          itemstack.writeToNBT(nbttagcompound);
          nbttaglist.appendTag(nbttagcompound);
        }
      }
      nbt.setTag("Items", nbttaglist);
    }
    if (this.getCustomNameTag() != null && !this.getCustomNameTag().isEmpty()) {
      nbt.setString("CustomName", this.getCustomNameTag());
    }
  }

  @Override
  public boolean attackEntityFrom(DamageSource source, float damage) {
    Entity sourceEntity = source.getTrueSource();

    if (source != DamageSource.IN_WALL) {
      // don't just sit there!
      this.aiSit.setSitting(false);
    }
    //        if(!sourceEntity.onGround && sourceEntity != null) this.setFlying(true);

    if (this.isBeingRidden() && source.getTrueSource() != null && source.getTrueSource().isPassenger(source.getTrueSource()) && damage < 1) {
      return false;
    }

    if (!world.isRemote && source.getTrueSource() != null && this.getRNG().nextInt(4) == 0) {
      this.roar();
    }

    if (isBaby() && isJumping) {
      return false;
    }

    if (this.isPassenger(sourceEntity)) {
      return false;
    }

    //when killed with damage greater than 17 cause the game to crash
    if (damage >= 17 && (source != DamageSource.GENERIC || source != DamageSource.OUT_OF_WORLD)) {
      return damage == 8.0f;
    }


    float damageReduction = (float) getArmorResistance() + 3.0F;
    if (getArmorResistance() != 0) {
      damage -= damageReduction;
    }

    return super.attackEntityFrom(source, damage);
  }

  /**
   * Credits: AlexThe 666 Ice and Fire
   */
  public class DragonInventory extends ContainerHorseChest {

    public DragonInventory(String inventoryTitle, int slotCount, EntityTameableDragon dragon) {
      super(inventoryTitle, slotCount);
      this.addInventoryChangeListener(new DragonInventoryListener(dragon));
    }
  }

  public class DragonInventoryListener implements IInventoryChangedListener {

    public DragonInventoryListener(EntityTameableDragon dragon) {
      this.dragon = dragon;
    }

    @Override
    public void onInventoryChanged(IInventory invBasic) {
      refreshInventory();
    }
    EntityTameableDragon dragon;

  }

  @Override
  protected float updateDistance(float f1, float f2) {
    dragonBodyHelper.updateRenderAngles();
    return f2;
  }

  @Override
  protected void entityInit() {
    super.entityInit();

    dataManager.register(DATA_FLYING, false);
    dataManager.register(GROWTH_PAUSED, false);
//    dataManager.register(DATA_BREATHING, false);
//    dataManager.register(DATA_ALT_BREATHING, false);
    dataManager.register(GOING_DOWN, false);
    dataManager.register(DATA_SADDLED, false);
    dataManager.register(CHESTED, false);
    dataManager.register(IS_MALE, getRNG().nextBoolean());
//    dataManager.register(IS_ALBINO, getRNG().nextInt(40) == 0);
//    dataManager.register(DRAGON_SCALES, (byte) 0);
    dataManager.register(ARMOR, 0);
    dataManager.register(BANNER1, ItemStack.EMPTY);
    dataManager.register(BANNER2, ItemStack.EMPTY);
    dataManager.register(BANNER3, ItemStack.EMPTY);
    dataManager.register(BANNER4, ItemStack.EMPTY);
//    dataManager.register(HAS_ELDER_STONE, false);
//    dataManager.register(HAS_ADJUCATOR_STONE, false);
    dataManager.register(FIRE_SUPPORT, false);
    dataManager.register(ALLOW_OTHERPLAYERS, false);
    dataManager.register(BOOSTING, false);
    dataManager.register(WHISTLE_STATE, (byte) 0);
    dataManager.register(WHISTLE, ItemStack.EMPTY);
    //        dataManager.register(SLEEP, false); //unused as of now
    dataManager.register(HOVER_CANCELLED, false);
    dataManager.register(Y_LOCKED, false);
    dataManager.register(FOLLOW_YAW, true);
    dataManager.register(DATA_BREATH_WEAPON_TARGET, "");
    dataManager.register(DATA_BREATH_WEAPON_MODE, 0);

    dataManager.register(HUNGER, 0);
  }

  @Override
  protected void applyEntityAttributes() {
    super.applyEntityAttributes();

    getAttributeMap().registerAttribute(MOVEMENT_SPEED_AIR);
    getAttributeMap().registerAttribute(ATTACK_DAMAGE);
    getEntityAttribute(MOVEMENT_SPEED_AIR).setBaseValue(BASE_AIR_SPEED);
    getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(BASE_GROUND_SPEED);
    getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(BASE_DAMAGE);
    getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(BASE_FOLLOW_RANGE);
    getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(RESISTANCE);
    getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(BASE_ARMOR);
    getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).setBaseValue(BASE_TOUGHNESS);
    this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
  }

  @Override
  protected float getJumpUpwardsMotion() {
    // stronger jumps for easier lift-offs
    return canFly() ? 1 : super.getJumpUpwardsMotion();
  }

  /**
   * Handles entity death timer, experience orb and particle creation
   */
  @Override
  protected void onDeathUpdate() {
    helpers.values().forEach(DragonHelper::onDeathUpdate);

    // unmount any riding entities
    removePassengers();

    // freeze at place
    motionX = motionY = motionZ = 0;
    rotationYaw = prevRotationYaw;
    rotationYawHead = prevRotationYawHead;

    if (deathTime >= getMaxDeathTime()) setDead(); // actually delete entity after the time is up

    if (isClient() && deathTime < getMaxDeathTime() - 20)
      spawnBodyParticles(EnumParticleTypes.CLOUD, 4);

    deathTime++;
  }

  /**
   * Returns the sound this mob makes on death.
   */
  @Override
  protected SoundEvent getDeathSound() {
    return this.getBreed().getDeathSound();
  }

  /**
   * Returns the sound this mob makes on swimming.
   *
   * @TheRPGAdenturer: disabled due to its annoyance while swimming underwater it
   * played too many times
   */
  @Override
  protected SoundEvent getSwimSound() {
    return null;
  }

//  public DragonBreathHelper getBreathHelper() {
//    return getHelper(DragonBreathHelper.class);
//  }

  /**
   * Returns the volume for the sounds this mob makes.
   */
  @Override
  protected float getSoundVolume() {
    // note: unused, managed in playSound()
    return 1;
  }

  /**
   * Gets the pitch of living sounds in living entities.
   */
  @Override
  protected float getSoundPitch() {
    // note: unused, managed in playSound()
    return 1;
  }

  @Override
  protected float getWaterSlowDown() {
    return 0.9F;
  }

  /**
   * Determines if an entity can be despawned, used on idle far away entities
   */
  @Override
  protected boolean canDespawn() {
    return false;
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

  private void addHelper(DragonHelper helper) {
    L.trace("addHelper({})", helper.getClass().getName());
    helpers.put(helper.getClass(), helper);
  }

  @SuppressWarnings("unchecked")
  private <T extends DragonHelper> T getHelper(Class<T> clazz) {
    return (T) helpers.get(clazz);
  }

  /**
   * Credits: AlexThe 666 Ice and Fire
   */
  private void InitializeDragonInventory() {
    int numberOfInventoryforChest = 27;
    int numberOfPlayerArmor = 5;
    DragonInventory dragonInv = this.dragonInv;
    this.dragonInv = new DragonInventory("dragonInv", 6 + numberOfInventoryforChest + 6 + numberOfPlayerArmor, this);
    this.dragonInv.setCustomName(this.getName());
    if (dragonInv != null) {
      int i = Math.min(dragonInv.getSizeInventory(), this.dragonInv.getSizeInventory());
      for (int j = 0; j < i; ++j) {
        ItemStack itemstack = dragonInv.getStackInSlot(j);
        if (!itemstack.isEmpty()) {
          this.dragonInv.setInventorySlotContents(j, itemstack.copy());
        }
      }

      if (world.isRemote) {
        ItemStack saddle = dragonInv.getStackInSlot(0);
        ItemStack chest_left = dragonInv.getStackInSlot(1);
        ItemStack banner1 = this.dragonInv.getStackInSlot(31);
        ItemStack banner2 = this.dragonInv.getStackInSlot(32);
        ItemStack banner3 = this.dragonInv.getStackInSlot(33);
        ItemStack banner4 = this.dragonInv.getStackInSlot(34);

        n.sendToServer(new MessageDragonInventory(this.getEntityId(), 0, saddle != null && saddle.getItem() == Items.SADDLE && !saddle.isEmpty() ? 1 : 0));

        n.sendToServer(new MessageDragonInventory(this.getEntityId(), 1, chest_left != null && chest_left.getItem() == Item.getItemFromBlock(Blocks.CHEST) && !chest_left.isEmpty() ? 1 : 0));

//        n.sendToServer(new MessageDragonInventory(this.getEntityId(), 2, this.getIntFromArmor(dragonInv.getStackInSlot(2))));

        n.sendToServer(new MessageDragonInventory(this.getEntityId(), 31, banner1 != null && banner1.getItem() == Items.BANNER && !banner1.isEmpty() ? 1 : 0));

        n.sendToServer(new MessageDragonInventory(this.getEntityId(), 32, banner2 != null && banner2.getItem() == Items.BANNER && !banner2.isEmpty() ? 1 : 0));

        n.sendToServer(new MessageDragonInventory(this.getEntityId(), 33, banner3 != null && banner3.getItem() == Items.BANNER && !banner3.isEmpty() ? 1 : 0));

        n.sendToServer(new MessageDragonInventory(this.getEntityId(), 34, banner4 != null && banner4.getItem() == Items.BANNER && !banner4.isEmpty() ? 1 : 0));

      }
    }
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
  private static final Logger L = LogManager.getLogger();
  private static final SimpleNetworkWrapper n = DragonMounts.NETWORK_WRAPPER;
  // data value IDs
  private static final DataParameter<Boolean> DATA_FLYING = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Boolean> GROWTH_PAUSED = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Boolean> DATA_SADDLED = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
//  private static final DataParameter<Boolean> DATA_BREATHING = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
//  private static final DataParameter<Boolean> DATA_ALT_BREATHING = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Boolean> GOING_DOWN = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Boolean> CHESTED = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Boolean> ALLOW_OTHERPLAYERS = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Boolean> BOOSTING = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Boolean> IS_MALE = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
//  private static final DataParameter<Boolean> IS_ALBINO = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Integer> ARMOR = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.VARINT);
  private static final DataParameter<Boolean> HOVER_CANCELLED = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Boolean> Y_LOCKED = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Boolean> FOLLOW_YAW = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Optional<UUID>> DATA_BREEDER = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.OPTIONAL_UNIQUE_ID);
  private static final DataParameter<String> DATA_BREED = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.STRING);
  private static final DataParameter<Integer> DATA_REPRO_COUNT = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.VARINT);
  private static final DataParameter<Integer> HUNGER = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.VARINT);
  private static final DataParameter<Integer> DATA_TICKS_SINCE_CREATION = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.VARINT);
//  private static final DataParameter<Byte> DRAGON_SCALES = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BYTE);
  private static final DataParameter<ItemStack> BANNER1 = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.ITEM_STACK);
  private static final DataParameter<ItemStack> BANNER2 = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.ITEM_STACK);
  private static final DataParameter<ItemStack> BANNER3 = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.ITEM_STACK);
  private static final DataParameter<ItemStack> BANNER4 = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.ITEM_STACK);
//  private static final DataParameter<Boolean> HAS_ADJUCATOR_STONE = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);

  private static final DataParameter<String> DATA_BREED_NEW = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.STRING);


  /*    public boolean isGiga() {
        return getLifeStageHelper().isAdult();
    }
    public boolean isAdjudicator() {
        return getLifeStageHelper().isAdult();
    }
*/
//  private static final DataParameter<Boolean> HAS_ELDER_STONE = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Byte> WHISTLE_STATE = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BYTE);
  private static final DataParameter<ItemStack> WHISTLE = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.ITEM_STACK);
  private static final DataParameter<Boolean> SLEEP = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<Boolean> FIRE_SUPPORT = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.BOOLEAN);
  private static final DataParameter<String> DATA_BREATH_WEAPON_TARGET = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.STRING);
  private static final DataParameter<Integer> DATA_BREATH_WEAPON_MODE = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.VARINT);
  // data NBT IDs
  private static final String NBT_ARMOR = "Armor";
  private static final String NBT_ALLOWOTHERPLAYERS = "AllowOtherPlayers";
  private static final String NBT_SADDLED = "Saddle";
//  private static final String NBT_SHEARED = "Sheared";
  private static final String NBT_CHESTED = "Chested";
  private static final String NBT_BREATHING = "Breathing";
  private static final String NBT_ISMALE = "IsMale";
//  private static final String NBT_ISALBINO = "IsAlbino";
//  private static final String NBT_ELDER = "Elder";
//  private static final String NBT_ADJUCATOR = "Adjucator";
  // server/client delegates
  private final Map<Class, DragonHelper> helpers = new HashMap<>();
  // client-only delegates
  private final DragonBodyHelper dragonBodyHelper = new DragonBodyHelper(this);
  private boolean hasChestVarChanged = false;
  //  private boolean isUsingBreathWeapon;
//  private boolean altBreathing;
  private boolean isGoingDown;
  private boolean isUnhovered;
  private boolean yLocked;
  private boolean followYaw;
  private DragonAnimator animator;
  private double airSpeedVertical = 0;
  private DragonPhysicalModel dragonPhysicalModel;
}

