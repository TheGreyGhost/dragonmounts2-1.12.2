package com.TheRPGAdventurer.ROTD.common.entity.helper;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.EnumDragonBreed;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Optional;

/**
 * Created by TGG on 20/10/2019.
 *
 * Contains functionality related to combat- damage and health.  AI is out of scope.
 */
public class DragonCombatHelper extends DragonHelper {
  public DragonCombatHelper(EntityTameableDragon dragon) {
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
    nbt.setInteger("hunger", this.getHunger());
    setCompleted(FunctionTag.WRITE_TO_NBT);
  }

  @Override
  public void readFromNBT(NBTTagCompound nbt) {
    checkPreConditions(FunctionTag.READ_FROM_NBT);
    this.setHunger(nbt.getInteger("hunger"));
    setCompleted(FunctionTag.READ_FROM_NBT);
  }

  @Override
  public void registerDataParameters() {
    checkPreConditions(FunctionTag.REGISTER_DATA_PARAMETERS);
    entityDataManager.register(HUNGER, 0);
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

  private void initialiseBothSides() {
    getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(BASE_FOLLOW_RANGE);
    getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(RESISTANCE);

  }

  public void onConfigurationChange() {
    checkPreConditions(FunctionTag.ON_CONFIG_CHANGE);
    throw new NotImplementedException("onConfigurationChange()");
  }

  @Override
  public void onLivingUpdate() {
    checkPreConditions(FunctionTag.VANILLA);
    if (this.ticksExisted % (DragonMounts.instance.getConfig().hungerDecrement) == 1) {
      if (this.getHunger() > 0) {
        this.setHunger(this.getHunger() - 1);
      }
    }

    if (this.isPotionActive(MobEffects.WEAKNESS)) {
      this.removePotionEffect(MobEffects.WEAKNESS);
    }

    if (ticksSinceLastAttack >= 0) { // used for jaw animation
      ++ticksSinceLastAttack;
      if (ticksSinceLastAttack > 1000) {
        ticksSinceLastAttack = -1; // reset at arbitrary large value
      }
    }



  }

  public EnumCreatureAttribute getCreatureAttribute() {
    return getBreed().getCreatureAttribute();
  }

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

  public void setImmuneToFire(boolean isImmuneToFire) {
    L.trace("setImmuneToFire({})", isImmuneToFire);
    this.isImmuneToFire = isImmuneToFire;
  }

  public void setAttackDamage(double damage) {
    L.trace("setAttackDamage({})", damage);
    getEntityAttribute(ATTACK_DAMAGE).setBaseValue(damage);
  }

  /**
   * retrieves the dragon's current hunger level
   * @return the hunger level (0% -> 100%)
   */
  public float getHunger() {
    if (dragon.isClient()) {
      return entityDataManager.get(HUNGER);
    }
    return serverHungerLevel;
  }

  /**
   * Sets the new hunger level - to be called on the server only
   * @param hunger the new hunger level as a percentage; will be clamped to valid range
   */
  public void setHunger(float hunger) {
    if (!assertServerSide("setHunger")) return;
    serverHungerLevel = MathX.clamp(hunger, 0.0F, 100.0F);
    entityDataManager.set(HUNGER, (int)serverHungerLevel);
  }

  /**
   * Feed the dragon: restores hunger level and/or heals damage
   * Server side only
   * @param foodpoints the number of hunger level points to restore
   */
  public void feed(int foodpoints) {
    if (!assertServerSide("setHunger")) return;
    float percentFed = foodpoints / (float)dragon.configuration().getVariantTagValue(DragonVariants.Category.METABOLISM, MAX_HUNGER);
    float newHungerLevel = serverHungerLevel + percentFed;
    setHunger(newHungerLevel);
    float hungerSurplus = newHungerLevel - 100.0F;
    if (hungerSurplus <= 0.0F) return;
    dragon.heal(hungerSurplus / 100.0F * (float)dragon.lifeStage().getMaxHealth());
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

  public Optional<Boolean> shouldAttackEntity(EntityLivingBase target, EntityLivingBase owner) {
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

  public Optional<Boolean> attackEntityFrom(DamageSource source, float damage) {
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


  // hunger is tracked internally as a float (also NBT load/save) but is transmitted to client as integer to avoid frequent updates
  //  the units are percent
  private static final DataParameter<Integer> HUNGER = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.VARINT);
  private float serverHungerLevel;  // server side only: hunger level (0 --> 100%)


  public boolean isImmuneToDamage(DamageSource dmg) {
    if (immunities.isEmpty()) {
      return false;
    }

    return immunities.contains(dmg.damageType);
  }

//  public static final double BASE_DAMAGE = DragonMounts.instance.getConfig().BASE_DAMAGE;
//  public static final double BASE_ARMOR = DragonMounts.instance.getConfig().ARMOR;
//  public static final double BASE_TOUGHNESS = 30.0D;
public static final float RESISTANCE = 10.0f;

  private static final DragonVariantTag MAX_HUNGER = DragonVariantTag.addTag("maxhungerunits", 100, 0.0, 500,
          "the maximum hunger level of the full-size dragon? (vanilla player maximum = 20 units)").categories(DragonVariants.Category.METABOLISM);
  private static final DragonVariantTag HUNGER_DECAY_RATE = DragonVariantTag.addTag("hungerdecayrate", 10, 0.0, 1000,
          "the speed at which hunger level decreases (% of maximum per minute)").categories(DragonVariants.Category.METABOLISM);
  private static final DragonVariantTag HUNGER_HEAL_THRESHOLD = DragonVariantTag.addTag("hungerhealthreshold", 85, 0.0, 101,
          "when hunger level is above this threshold (%), the dragon regenerates health ").categories(DragonVariants.Category.METABOLISM);
  private static final DragonVariantTag MAX_HEALTH_REGEN_RATE = DragonVariantTag.addTag("maxhealthregenrate", 75, 0.0, 1000,
          "when hunger level is maximum, the dragon regenerates health at this rate (% per minute)").categories(DragonVariants.Category.METABOLISM);

  private boolean assertServerSide(String functionName) {
    if (dragon.isClient()) {
      DragonMounts.loggerLimit.error_once("Unexpectedly called DragonCombatHelper::" + functionName + "() on client side");
      return false;
    }
    return true;
  }
}
