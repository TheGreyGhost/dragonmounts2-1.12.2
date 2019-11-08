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
import com.TheRPGAdventurer.ROTD.common.entity.breath.DragonBreathHelperP;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.helper.*;
import com.TheRPGAdventurer.ROTD.common.entity.interact.DragonInteractHelper;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonPhysicalModel;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.Modifiers;
import com.TheRPGAdventurer.ROTD.util.debugging.DebugSettings;
import net.minecraft.block.Block;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static com.google.common.base.Preconditions.checkArgument;
import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.base.Preconditions.checkState;

/**
 * Here be dragons.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 * @Modifier James Miller <TheRPGAdventurer.>
 *
 *
 *  Usage:
 *   1) call EntityTameableDragon.registerConfigurationTags() during commonproxy.preInitialisePhase to ensure that the configuration tags are properly registered
 *   2) To spawn a new entity:
 *     a) EntityTameableDragon(world, dragonBreed, modifiers)
 *     b) worldIn.spawnEntity(myEntity);
 *
 *  The usual mechanics for vanilla entity spawing are followed:
 *  After the user has spawned a new Entity on the server, the packet to the client will call:
 *  1) myEntityClient = new MyEntity(world) will be called
 *  2) myEntityClient notifyDataManagerChange will be called for all DataParameters

 *  For reloading of an existing entity from disk:
 *  1) myEntityServer = new MyEntity(world) will be called
 *  2) myEntityServer.readEntityFromNBT will be called
 *     The client creation then occurs as above.
 */

public class EntityTameableDragon extends EntityTameable {

  public EntityTameableDragon(World world) {
    super(world);
  }

  public EntityTameableDragon(World world, DragonBreedNew breed, Modifiers modifiers) {
    this(world);
    checkState(isServer(), "Attempted to explicitly construct EntityTameableDragon on client side.");
    configuration().setInitialConfiguration(breed, modifiers);
    helpers.values().forEach(DragonHelper::registerEntityAttributes);
    helpers.values().forEach(DragonHelper::onExplicitConstruction);
    initialiseServerSide();
  }

  public static void registerConfigurationTags() {
    DragonBreathHelperP.registerConfigurationTags();
    DragonCombatHelper.registerConfigurationTags();
    DragonConfigurationHelper.registerConfigurationTags();
    DragonInteractHelper.registerConfigurationTags();
    DragonInventoryHelper.registerConfigurationTags();
    DragonLifeStageHelper.registerConfigurationTags();
    DragonMovementHelper.registerConfigurationTags();
    DragonReproductionHelper.registerConfigurationTags();
    DragonRidingHelper.registerConfigurationTags();
    DragonSoundsHelper.registerConfigurationTags();
    DragonWhistleHelper.registerConfigurationTags();
  }

  @Override
  protected void entityInit() {
    super.entityInit();
    addHelpers();
    helpers.values().forEach(DragonHelper::registerDataParameters);
  }

  @Override
  protected void applyEntityAttributes() {
    super.applyEntityAttributes();
    helpers.values().forEach(DragonHelper::registerEntityAttributes);
  }

  private void initialiseServerSide() {
    checkState(isServer(), "initialiseServerSide unexpectedly called on non-server side");
    initialiseBothSides();
    helpers.values().forEach(DragonHelper::initialiseServerSide);
  }

  private void initialiseClientSide() {
    checkState(isClient(), "initialiseClientSide unexpectedly called on non-client side");
    initialiseBothSides();
    helpers.values().forEach(DragonHelper::initialiseClientSide);
  }

  private void initialiseBothSides() {
    dragonPhysicalModel = new DragonPhysicalModel(configuration().getDragonBreedNew(), configuration().getModifiers());
    animator = new DragonAnimator(this);
  }

  private void onConfigurationChangeThis() {
    dragonPhysicalModel = new DragonPhysicalModel(configuration().getDragonBreedNew(), configuration().getModifiers());
    animator = new DragonAnimator(this);
  }

  /** called to notify that the dragon configuration has changed.
   * intended to be called by DragonConfigurationHelper only.
   * Detects cascading changes (recursion beyond a given depth) and aborts if detected
  **/
  public void onConfigurationChange() {
    if (recursionLimitReached) return;
    if (configurationRecursionCount > MAX_CONFIGURATION_RECURSION) {
      DragonMounts.loggerLimit.warn_once("Excessive recursion detected in onConfigurationChange: i.e. the changed configuration has further configuration changes.");
      recursionLimitReached = true;
      return;
    }
    try {
      ++configurationRecursionCount;
      helpers.values().forEach(DragonHelper::onConfigurationChange);
    } finally {
      --configurationRecursionCount;
      if (configurationRecursionCount == 0) recursionLimitReached = false;
    }

    if (configurationRecursionCount == 0) {
      onConfigurationChangeThis();
    }
  }

  private int configurationRecursionCount = 0;
  private boolean recursionLimitReached = false;
  private static final int MAX_CONFIGURATION_RECURSION = 4;

  @Override
  public void notifyDataManagerChange(DataParameter<?> key) {
    helpers.values().forEach(helper -> helper.notifyDataManagerReceived(key));
    if (allDataParametersReceived) return;
    for (DragonHelper dragonHelper : helpers.values()) {
      if (!dragonHelper.allDataParametersReceived()) return;
    }
    allDataParametersReceived = true;
    initialiseClientSide();
  }

  /**
   * Called once when the entity is first spawned (and not when it's later loaded from disk)
   * Used to randomly vary attributes for this instance
   * @param difficulty
   * @param livingdata
   * @return
   */
  @Override
  @Nullable
  public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata) {
    for (DragonHelper dragonHelper : helpers.values()) {
      livingdata = dragonHelper.onInitialSpawn(difficulty, livingdata);
    }
    return livingdata;
  }

  private void addHelper(DragonHelper helper) {
    helpers.put(helper.getClass(), helper);
  }

  @SuppressWarnings("unchecked")
  private <T extends DragonHelper> T getHelper(Class<T> clazz) {
    return (T) helpers.get(clazz);
  }

  private void addHelpers() {
    // create entity delegates
    // don't forget to add corresponding entries in registerConfigurationTags too.
    addHelper(new DragonBreathHelperP(this));
    addHelper(new DragonCombatHelper(this));
    addHelper(new DragonConfigurationHelper(this));
    addHelper(new DragonInteractHelper(this));
    addHelper(new DragonInventoryHelper(this));
    addHelper(new DragonLifeStageHelper(this));
    addHelper(new DragonMovementHelper(this));
    addHelper(new DragonReproductionHelper(this));
    addHelper(new DragonRidingHelper(this));
    addHelper(new DragonSoundsHelper(this));
    addHelper(new DragonWhistleHelper(this));

    if (isServer()) {
      addHelper(new DragonBrain(this));
    }
    if (isClient()) {
      // add client helpers
    }
    // just a default for now
    dragonPhysicalModel = new DragonPhysicalModel(DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getDefaultBreed(), new Modifiers());
    animator = new DragonAnimator(this);

    moveHelper = new DragonMoveHelper(this);   // not a DragonHelper despite the name; is vanilla
     // consider the other vanilla helpers from EntityLivingBase - (Look, Move, Jump, Body)
  }

  // server/client delegates
  private final Map<Class, DragonHelper> helpers = new TreeMap<>(new DragonHelper.DragonHelperSorter());
//  private final Map<Class, DragonHelperClient> clientHelpers = new TreeMap<>(new DragonHelper.DragonHelperSorter());

  @Override
  protected EntityBodyHelper createBodyHelper()
  {
    return new DragonBodyHelper(this);
  }

  //---------------------

  public int roarTicks;
  public EntityTameableDragonStats dragonStats = new EntityTameableDragonStats();


//  /** initialise the dragon to the desired breed:
//   *  * if the caller has manually constructed the entity, need to call this method
//   *  * otherwise, the vanilla constructor will create it and call readFromNBT
//   * @param dragonBreed
//   */
//  public void initialise(DragonBreedWithModifiers dragonBreed) {
//    // enables walking over blocks
//    stepHeight = 1;
//
//    // set dimensions of full-grown dragon.  The actual width & height is multiplied by the dragon scale (setScale) in EntityAgeable
//    final float FULL_SIZE_DRAGON_SCALE = 1.0F;
//    float adultWidth = dragonPhysicalModel.getHitboxWidthWC(FULL_SIZE_DRAGON_SCALE);
//    float adultHeight = dragonPhysicalModel.getHitboxHeightWC(FULL_SIZE_DRAGON_SCALE);
//    setSize(adultWidth, adultHeight);           //todo: later - update it when breed changes
//
//    // init helpers
//    aiSit = new EntityAIDragonSit(this);
//    helpers.values().forEach(DragonHelper::registerEntityAttributes);
//
////    InitializeDragonInventory();
//  }

  /**
   * (abstract) Protected helper method to write subclass entity data to NBT.
   */

  @Override
  public void writeEntityToNBT(NBTTagCompound nbt) {
    super.writeEntityToNBT(nbt);

    dragonStats.writeNBT(nbt);
    helpers.values().forEach(helper -> helper.writeToNBT(nbt));
  }

  /**
   * (abstract) Protected helper method to read subclass entity data from NBT.
   */
  @Override
  public void readEntityFromNBT(NBTTagCompound nbt) {
    super.readEntityFromNBT(nbt);

//    DragonBreedNew dragonBreed = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getDefaultBreed();
//    try {
//      dragonBreed = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getBreed(nbt);
//    } catch (IllegalArgumentException iae) {
//      DragonMounts.loggerLimit.warn_once(iae.getMessage());
//    }
//    modifiers = Modifiers.getStateFromNBT(nbt);
//    initialise(dragonBreed);

    dragonStats.readNBT(nbt);
    helpers.values().forEach(helper -> helper.readFromNBT(nbt));
    initialiseServerSide();
  }

  /**
   * Called when the mob is falling. Calculates and applies fall damage.
   */
  @Override
  public void fall(float distance, float damageMultiplier) {
    if (movement().shouldSufferFallDamage(distance, damageMultiplier)) {
      super.fall(distance, damageMultiplier);
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
//      this.updateKeys();  // moved to DragonRiderControls witih clienttick
      dragonStats.onUpdate(this);
    }
  }

  @Override
  public void onEntityUpdate() {
    if (DebugSettings.isAnimationFrozen()) {
      return;
    }
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

    if (isServer()) {
      final float DUMMY_MOVETIME = 0;
      final float DUMMY_MOVESPEED = 0;
      animator.setMovement(DUMMY_MOVETIME, DUMMY_MOVESPEED);
      float netYawHead = getRotationYawHead() - renderYawOffset;
      animator.setLook(netYawHead, rotationPitch);
      animator.tickingUpdate();
      animator.animate();
    } else {
      animator.tickingUpdate();
    }

    if (roarTicks >= 0) { // used for jaw animation
      ++roarTicks;
      if (roarTicks > 1000) {
        roarTicks = -1; // reset at arbitrary large value
      }
    }


//        if (this.isUsingBreathWeapon() && this.getBreed().canUseBreathWeapon() && this.getControllingPlayer()!=null && (this.isUsingBreathWeapon())) {
//            this.equalizeYaw(this.getControllingPlayer());
//        }

    doBlockCollisions();  //todo is this necessary?!
//    List<Entity> list = this.world.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox().grow(0.2, -0.01, 0.2),  //todo what is this for?
//            EntitySelectors.getTeamCollisionPredicate(this));

    super.onLivingUpdate(); //todo why is this at the end, not the start?
  }

  public void spawnBodyParticle(EnumParticleTypes type) {
    double ox, oy, oz;
    float s = this.lifeStage().getAgeScale() * 1.2f;

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
    int amount = (int) (baseAmount * this.lifeStage().getAgeScale());
    for (int i = 0; i < amount; i++) {
      spawnBodyParticle(type);
    }
  }

  /**
   * Called when the mob's health reaches 0.
   */
  @Override
  public void onDeath(DamageSource src) {
    helpers.values().forEach(DragonHelper::onDeath);
    super.onDeath(src);
  }

  @Override
  public void setDead() {
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
    String breedName = configuration().getDragonBreedNew().getInternalName();
    ITextComponent name = new TextComponentTranslation("entity." + entName + "." + breedName + ".name");
    return name;
  }

  /**
   * Plays living's sound at its position
   */
  @Override
  public void playLivingSound() {
    sounds().playLivingSound();
  }

  /**
   * Get number of ticks, at least during which the living entity will be silent.
   */
  @Override
  public int getTalkInterval() {
    return sounds().getTalkInterval();
  }

  /**
   * Plays step sound at given x, y, z for the entity
   */
  @Override
  public void playStepSound(BlockPos entityPos, Block block) {
    sounds().playStepSound(entityPos, block);
  }

  @Override
  public void playSound(SoundEvent sound, float volume, float pitch) {
    sounds().playSound(sound, volume, pitch, false);
  }

  /**
   * Get this Entity's EnumCreatureAttribute
   */
  @Override
  public EnumCreatureAttribute getCreatureAttribute() {
    return combat().getCreatureAttribute();
  }

  /**
   * Called when a player interacts with a mob. e.g. gets milk from a cow, gets into the saddle on a pig.
   */
  @Override
  public boolean processInteract(EntityPlayer player, EnumHand hand) {
    return interactions().processInteract(player, hand);
  }

  /**
   * Checks if the parameter is an item which this animal can be fed to breed it
   * (wheat, carrots or seeds depending on the animal type)
   */
  @Override
  public boolean isBreedingItem(ItemStack item) {
    return reproduction().isBreedingItem(item);
  }

  /**
   * Returns the height of the eyes. Used for looking at other entities.
   */
  @Override
  public float getEyeHeight() {
    float eyeHeight = dragonPhysicalModel.getEyeHeightWC(lifeStage().getAgeScale(), isSitting());
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
    return dragonPhysicalModel.getRiderPositionOffsetWC(lifeStage().getAgeScale(), movement().getBodyPitch(), isSitting(), DEFAULT_PASSENGER_NUMBER).y;
  }

  /**
   * Returns render size modifier for the shadow
   */
  @Override
  public float getRenderSizeModifier() {
    return lifeStage().getAgeScale();
//    return getAgeScale() / (isChild() ? 0.5F : 1.0F);
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
  // perhaps later we can add sub-entity AABB (similar to the vanilla dragon) to trigger render.
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
   * Called when the dragon attacks another entity
   */
  @Override
  public boolean attackEntityAsMob(Entity entityIn) {
    super.attackEntityAsMob(entityIn);                // just to set vanilla attack timer
    return combat().attackEntityAsMob(entityIn);
  }

  public void applyVanillaEnchantments(EntityLivingBase attacker, Entity victim) {
    super.applyEnchantments(attacker, victim);
  }

  @Override
  public boolean isPotionApplicable(PotionEffect potioneffectIn) {
    return combat().isPotionApplicable(potioneffectIn);
  }

  /**
   * Used to get the hand in which to swing and play the hand swinging animation
   * when attacking In this case the dragon's jaw
   */
  @Override
  public void swingArm(EnumHand hand) {
    movement().swingArm(hand);
  }

  /**
   * Return whether this entity should be rendered as on fire.
   */
  @Override
  public boolean canRenderOnFire() {
    return super.canRenderOnFire() && !combat().isImmuneToDamageType(DamageSource.IN_FIRE);
  }

  /**
   * Returns true if the mob is currently able to mate with the specified mob.
   */
  @Override
  public boolean canMateWith(EntityAnimal mate) {
    return reproduction().canMateWith(mate);
  }

  /**
   * This function is used when two same-species animals in 'love mode' breed to
   * generate the new baby animal.
   */
  @Override
  public EntityAgeable createChild(EntityAgeable mate) {
    return this.reproduction().createChild(mate);
  }

  public DragonConfigurationHelper configuration() {
    return getHelper(DragonConfigurationHelper.class);
  }
  public DragonLifeStageHelper lifeStage() {
    return getHelper(DragonLifeStageHelper.class);
  }
  public DragonReproductionHelper reproduction() {
    return getHelper(DragonReproductionHelper.class);
  }
  public DragonBreathHelperP breathweapon() {
    return getHelper(DragonBreathHelperP.class);
  }
  public DragonAnimator getAnimator() {
    return animator;
  }
  public DragonBrain brain() {
    return getHelper(DragonBrain.class);
  }
  public DragonInteractHelper interactions() {
    return getHelper(DragonInteractHelper.class);
  }
  public DragonCombatHelper combat() {
    return getHelper(DragonCombatHelper.class);
  }
  public DragonSoundsHelper sounds() {
    return getHelper(DragonSoundsHelper.class);
  }
  public DragonInventoryHelper inventory() {
    return getHelper(DragonInventoryHelper.class);
  }
  public DragonWhistleHelper whistle() {
    return getHelper(DragonWhistleHelper.class);
  }
  public DragonRidingHelper riding() {return getHelper(DragonRidingHelper.class);}
  public DragonPhysicalModel getPhysicalModel() {
    return dragonPhysicalModel;
  }
  public DragonMovementHelper movement() {return getHelper(DragonMovementHelper.class);}

  @Override
  public boolean canBeSteered() {
    return riding().canBeSteered();
  }

  @Override
  public void travel(float strafe, float forward, float vertical) {
    boolean shouldCallVanilla = movement().travel(strafe, forward, vertical);
    if (shouldCallVanilla) {
      super.travel(strafe, forward, vertical);
    }
  }

  @Nullable
  public Entity getControllingPassenger() {
    return this.getPassengers().isEmpty() ? null : getPassengers().get(0);
  }

  @Override
  public void updateRidden() {
    riding().updateRidden();
  }

  /**
   * This code is called when the passenger is riding on the dragon
   *
   * @param passenger
   */
  @Override
  public void updatePassenger(Entity passenger) {
    riding().updatePassenger(passenger);
  }

  @Override
  public boolean isEntityInvulnerable(DamageSource src) {
    return combat().isInvulnerableToThisDamageSource(src);
  }

  public int getDeathTime() {
    return deathTime;
  }

  public int getMaxDeathTime() {
    return 120;
  }

  @Override
  public boolean canBeLeashedTo(EntityPlayer player) {
    return interactions().canBeLeashedTo(player);
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

  // aging is handled by DragonLifeStageHelper, not the vanilla aging mechanics
  @Override
  public int getGrowingAge() {
    return 0;
  }
  @Override
  public boolean isChild() {
    return false;
  }
  @Override
  public void setGrowingAge(int age) {
  }

  // aging is handled by DragonLifeStageHelper, not the vanilla aging mechanics
  @Override
  public void setScaleForAge(boolean child) {
  }

  @Override
  public boolean shouldDismountInWater(Entity rider) {
    return riding().shouldDismountInWater(rider);
  }

//  /**
//   * Returns the size multiplier for the current age.
//   *
//   * @return scale
//   */
//  public float getAgeScale() {
//    return lifeStage().getAgeScale();
//  }

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

  /**
   * Called when a lightning bolt hits the entity.
   */
  @Override
  public void onStruckByLightning(EntityLightningBolt lightningBolt) {
    boolean shouldCallVanilla = combat().onStruckByLightning(lightningBolt);
    if (!shouldCallVanilla) return;
    super.onStruckByLightning(lightningBolt);
  }

  @Override
  public boolean shouldAttackEntity(EntityLivingBase target, EntityLivingBase owner) {
    Optional<Boolean> shouldAttack = combat().shouldAttackEntityOnBehalfOfOwner(target, owner);
    if (shouldAttack.isPresent()) return shouldAttack.get();
    return super.shouldAttackEntity(target, owner);
  }

  @Override
  public boolean canFitPassenger(Entity passenger) {
    return riding().canFitPassenger(passenger);
  }

  public boolean replaceItemInInventory(int inventorySlot, @Nullable ItemStack itemStackIn) {
    return inventory().replaceItemInInventory(inventorySlot, itemStackIn);
  }

  @Override
  public boolean attackEntityFrom(DamageSource source, float damage) {
    Optional<Boolean> attackSucceeded = combat().attackEntityFrom(source, damage);
    if (attackSucceeded.isPresent()) return attackSucceeded.get();
    return super.attackEntityFrom(source, damage);
  }

// not needed? is an exact copy of the overriden function?
//  @Override
//  protected float updateDistance(float f1, float f2) {
//    checkState(bodyHelper instanceof DragonBodyHelper, );
//    dragonBodyHelper.updateRenderAngles();
//    return f2;
//  }

  @Override
  protected float getJumpUpwardsMotion() {
    return movement().getJumpUpwardsMotion();
  }

  /**
   * Handles entity death timer, experience orb and particle creation
   */
  @Override
  protected void onDeathUpdate() {
    helpers.values().forEach(DragonHelper::onDeathUpdate);

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
    return sounds().getDeathSound();
  }

  /**
   * Returns the sound this mob makes on swimming.
   */
  @Override
  protected SoundEvent getSwimSound() {
    return sounds().getSwimSound();
  }

  // note: unused, managed in playSound()
  @Override
  protected float getSoundVolume() {
    return 1;
  }

  // note: unused, managed in playSound()
  @Override
  protected float getSoundPitch() {
    return 1;
  }

  @Override
  protected float getWaterSlowDown() {
    return movement().getWaterSlowDown();
  }

  /**
   * Determines if an entity can be despawned, used on idle far away entities
   */
  @Override
  protected boolean canDespawn() {
    return false;
  }

  private static final Logger L = LogManager.getLogger();
  private static final SimpleNetworkWrapper n = DragonMounts.NETWORK_WRAPPER;

  private DragonAnimator animator;
  private double airSpeedVertical = 0;
  private DragonPhysicalModel dragonPhysicalModel;
  boolean allDataParametersReceived = false;
}

