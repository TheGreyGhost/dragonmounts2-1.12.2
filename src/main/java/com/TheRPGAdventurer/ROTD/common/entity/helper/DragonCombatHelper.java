package com.TheRPGAdventurer.ROTD.common.entity.helper;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantsException;
import com.TheRPGAdventurer.ROTD.util.EntityState;
import com.TheRPGAdventurer.ROTD.util.TickTimer;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import com.google.common.collect.ImmutableSet;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;

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
    DragonVariants.addVariantTagValidator(new DragonCombatValidator());
  }

  private final String NBT_HUNGER = "hunger";
  private final String NBT_FULLNESS_TICKNESS_REMAINING = "fullnessticksremaining";

  @Override
  public void writeToNBT(NBTTagCompound nbt) {
    checkPreConditions(FunctionTag.WRITE_TO_NBT);
    nbt.setFloat(NBT_HUNGER, getHunger());
    nbt.setInteger(NBT_FULLNESS_TICKNESS_REMAINING, serverHungerFullnessTicksLeft);
    setCompleted(FunctionTag.WRITE_TO_NBT);
  }

  @Override
  public void readFromNBT(NBTTagCompound nbt) {
    checkPreConditions(FunctionTag.READ_FROM_NBT);
    this.setHunger(nbt.getFloat(NBT_HUNGER));
    serverHungerFullnessTicksLeft = nbt.getInteger(NBT_FULLNESS_TICKNESS_REMAINING);
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

//  private void initialiseBothSides() {
//    // all takes place in onConfigurationChange
//  }

  /**
   * update immunities
   */
  public void onConfigurationChange() {
    checkPreConditions(FunctionTag.ON_CONFIG_CHANGE);
    damageSourceImmunities.clear();

    String [] damageSourceNames = (String [])dragon.configuration().getVariantTagValue(DragonVariants.Category.METABOLISM, DAMAGE_SOURCE_IMMUNITIES);
    Set<String> damageSourceNamesSet = ImmutableSet.copyOf(damageSourceNames);
    damageSourceImmunities.addAll(damageSourceNamesSet);

//    int debugCount = 0;
//    for (DamageSource damageSource : validDamageSources) {
//      if (damageSourceNamesSet.contains(damageSource.getDamageType())) {
//        ++debugCount;
//      }
//    }
//    if (debugCount != damageSourceNames.length) {
//      DragonMounts.loggerLimit.error_once("Internal Error: mismatch in DragonCombatHelper.onConfigurationChange()");
//    }

    String [] potionNames = (String [])dragon.configuration().getVariantTagValue(DragonVariants.Category.METABOLISM, POTION_IMMUNITIES);
    for (String name : potionNames) {
      Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(name));
      if (potion == null) {
        DragonMounts.loggerLimit.error_once("Internal Error: unknown potion name:" + name);
      }
      potionImmunities.add(potion);
    }

    String [] foodItemsWhiteListNames = (String [])dragon.configuration().getVariantTagValue(DragonVariants.Category.METABOLISM, FOOD_ITEMS_WHITELIST);
    for (String name : foodItemsWhiteListNames) {
      Item item = Item.REGISTRY.getObject(new ResourceLocation(name));
      if (item == null) {
        DragonMounts.loggerLimit.error_once("Internal Error: unknown food whitelist item name:" + name);
      }
      foodItemsWhiteList.add(item);
    }
    String [] foodItemsBlackListNames = (String [])dragon.configuration().getVariantTagValue(DragonVariants.Category.METABOLISM, FOOD_ITEMS_BLACKLIST);
    for (String name : foodItemsBlackListNames) {
      Item item = Item.REGISTRY.getObject(new ResourceLocation(name));
      if (item == null) {
        DragonMounts.loggerLimit.error_once("Internal Error: unknown food blacklist item name:" + name);
      }
      foodItemsBlackList.add(item);
    }

    enumCreatureAttribute = EnumCreatureAttribute.UNDEFINED;
    String enumCreatureAttributeText = (String)dragon.configuration().getVariantTagValue(DragonVariants.Category.METABOLISM, CREATURE_ATTRIBUTE);
    boolean debugFound = false;
    for (EnumCreatureAttribute checkValue : EnumCreatureAttribute.values()) {
      if (enumCreatureAttribute.toString().equals(enumCreatureAttributeText)) {
        enumCreatureAttribute = checkValue;
        debugFound = true;
        break;
      }
    }
    if (!debugFound) {
      DragonMounts.loggerLimit.error_once("Internal Error: unknown creature attribute:" + enumCreatureAttributeText);
    }

    double knockbackResistance = (double)dragon.configuration().getVariantTagValue(DragonVariants.Category.METABOLISM, KNOCKBACK_RESISTANCE);
    dragon.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(knockbackResistance);

    double followRange = (double)dragon.configuration().getVariantTagValue(DragonVariants.Category.BEHAVIOUR, FOLLOW_RANGE);
    dragon.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(followRange);
  }

  @Override
  public void onLivingUpdate() {
    checkPreConditions(FunctionTag.VANILLA);

    if (dragon.isServer()) {
      tickHungerAndHealth();
    }

    ticksSinceLastDamageTaken.tick();
    ticksSinceLastBite.tick();
  }

  public boolean attackEntityAsMob(Entity victim) {
    // based on EntityMob.attackEntityAsMob
    float damage = (float)dragon.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
    int knockback = 0;

    if (victim instanceof EntityLivingBase) {
      damage += EnchantmentHelper.getModifierForCreature(dragon.getHeldItemMainhand(), ((EntityLivingBase)victim).getCreatureAttribute());
      knockback += EnchantmentHelper.getKnockbackModifier(dragon);
    }

    boolean attackSucceeded = victim.attackEntityFrom(DamageSource.causeMobDamage(dragon), damage);
    if (attackSucceeded) {
      if (knockback > 0 && victim instanceof EntityLivingBase) {
        EntityLivingBase victimELB = (EntityLivingBase) victim;
        float yawRadians = (float)Math.toRadians(dragon.rotationYaw);
        victimELB.knockBack(dragon, (float) knockback * 0.5F, MathHelper.sin(yawRadians), (-MathHelper.cos(yawRadians)));
        dragon.motionX *= 0.6D;
        dragon.motionZ *= 0.6D;
      }

      int fireAspectModifier = EnchantmentHelper.getFireAspectModifier(dragon);
      if (fireAspectModifier > 0) {
        victim.setFire(fireAspectModifier * 4);
      }

      if (victim instanceof EntityPlayer) {
        EntityPlayer victimEP = (EntityPlayer)victim;
        ItemStack dragonItemStack = dragon.getHeldItemMainhand();
        ItemStack victimItemStack = victimEP.isHandActive() ? victimEP.getActiveItemStack() : ItemStack.EMPTY;

        if (!dragonItemStack.isEmpty() && !victimItemStack.isEmpty()
            && dragonItemStack.getItem().canDisableShield(dragonItemStack, victimItemStack, victimEP, dragon)
            && victimItemStack.getItem().isShield(victimItemStack, victimEP)) {
          float efficiencyModifier = 0.25F + (float)EnchantmentHelper.getEfficiencyModifier(dragon) * 0.05F;

          if (this.rand.nextFloat() < efficiencyModifier) {
            victimEP.getCooldownTracker().setCooldown(victimItemStack.getItem(), 100);
            dragon.world.setEntityState(victimEP, EntityState.KNOCKBACK.getMagicNumber());
          }
        }
      }

      dragon.applyVanillaEnchantments(dragon, victim);
    }

    return attackSucceeded;
//    if (!this.nothing()) {    // whistle?
//      return false;
//    }
//    if (getBreedType() == EnumDragonBreed.WITHER) {
//      ((EntityLivingBase) entityIn).addPotionEffect(new PotionEffect(MobEffects.WITHER, 200));
//    }

  }

//  /**
//   * 1 equals iron 2 equals gold 3 equals diamond 4 equals emerald
//   *
//   * @return 0 no armor
//   */
//  public double getArmorResistance() {
//    switch (getArmor()) {
//      case 1:
//        return 1.4;
//      case 2:
//        return 1.2;
//      case 3:
//        return 1.7;
//      case 4:
//        return 1.4;
//      default:
//        return 0;
//    }
//  }

//  public void setImmuneToFire(boolean isImmuneToFire) {
//    L.trace("setImmuneToFire({})", isImmuneToFire);
//    this.isImmuneToFire = isImmuneToFire;
//  }
//
//  public void setAttackDamage(double damage) {
//    L.trace("setAttackDamage({})", damage);
//    getEntityAttribute(ATTACK_DAMAGE).setBaseValue(damage);
//  }

  /**
   * Called when a lightning bolt hits the entity.
   * Return true if the vanilla code should be called, false otherwise
   */
  public boolean onStruckByLightning(EntityLightningBolt lightningBolt) {
    return true;

    // maybe later: add options for special effects with some dragon types
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

//    addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 35 * 20));
  }

  public Optional<Boolean> shouldAttackEntityOnBehalfOfOwner(EntityLivingBase target, EntityLivingBase owner) {
    if (!target.isChild()) {
      if (target instanceof EntityTameable) {
        EntityTameable tamedEntity = (EntityTameable) target;
        if (tamedEntity.isTamed()) {
          return Optional.of(false);
        }
      }
      if (target instanceof EntityPlayer) {
        EntityPlayer playertarget = (EntityPlayer) target;
        if (dragon.interactions().isTamedFor(playertarget)) {
          return Optional.of(false);
        }
      }
      if (target.hasCustomName()) {
        return Optional.of(false);
      }
    }
    return Optional.empty();
  }

  /**
   * Called when the dragon is attacked.  Useful to trigger behaviour in response.
   *
   * @param source
   * @param damage
   * @return true if the attack succeed, false if it failed.  EMPTY if vanilla should be called.
   */
  public Optional<Boolean> attackEntityFrom(DamageSource source, float damage) {
    if (isInvulnerableToThisDamageSource(source)) return Optional.of(false);

    Entity sourceEntity = source.getTrueSource();

    if (dragon.isServer()) {
      if (sourceEntity != null && dragon.getRNG().nextInt(4) == 0) {
        dragon.sounds().roar();
      }
    }

    ticksSinceLastDamageTaken.restart();

    //        if(!sourceEntity.onGround && sourceEntity != null) this.setFlying(true);

//    if (this.isBeingRidden() && source.getTrueSource() != null && source.getTrueSource().isPassenger(source.getTrueSource()) && damage < 1) {
//      return false;
//    }
//
//    if (isBaby() && isJumping) {
//      return false;
//    }
//
//    if (this.isPassenger(sourceEntity)) {
//      return false;
//    }
//
//    //when killed with damage greater than 17 cause the game to crash
//    if (damage >= 17 && (source != DamageSource.GENERIC || source != DamageSource.OUT_OF_WORLD)) {
//      return damage == 8.0f;
//    }
//
//
//    float damageReduction = (float) getArmorResistance() + 3.0F;
//    if (getArmorResistance() != 0) {
//      damage -= damageReduction;
//    }

    return Optional.empty();
  }

  public Optional<Integer> getTicksSinceLastDamageTaken() {
    return ticksSinceLastDamageTaken.getTickCount();
  }

  public Optional<Integer> getTicksSinceLastBite() {
    return ticksSinceLastBite.getTickCount();
  }

  private TickTimer ticksSinceLastDamageTaken = new TickTimer();
  private TickTimer ticksSinceLastBite = new TickTimer();

  // ----------- food, hunger, and health -----------------

  private final static float MIN_HUNGER_PERCENT = 0.0F;
  private final static float MAX_HUNGER_PERCENT = 100.0F;

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
   * is the dragon hungry?
   * @return true if the dragon is hungry
   */
  public boolean isHungry() {
    return getHunger() < MAX_HUNGER_PERCENT;
  }

  /**
   * Sets the new hunger level - to be called on the server only.
   * Values sent to client are rounded to nearest percentage point
   * @param hunger the new hunger level as a percentage; will be clamped to valid range
   */
  public void setHunger(float hunger) {
    if (!assertServerSide("setHunger")) return;
//    final float MINIMUM_SIGNIFICANT_DIFFERENCE = 1.0F;
    serverHungerLevel = MathX.clamp(hunger, MIN_HUNGER_PERCENT, MAX_HUNGER_PERCENT);
//    float dmHungerLevel = entityDataManager.get(HUNGER);
//    if (serverHungerLevel == MIN_HUNGER || serverHungerLevel == MAX_HUNGER ||
//        Math.abs(dmHungerLevel - serverHungerLevel) >= MINIMUM_SIGNIFICANT_DIFFERENCE) {
    entityDataManager.set(HUNGER, (int) serverHungerLevel);
//    }
  }

  /**
   * Returns the entity's health relative to the maximum health.
   *
   * @return health normalized between 0 and 1
   */
  public double getHealthRelative() {
    return dragon.getHealth() / (double) dragon.getMaxHealth();
  }

  /**
   * Feed the dragon: restores hunger level and/or heals damage
   * Server side only
   * @param foodpoints the number of hunger level points to restore
   */
  public void feed(int foodpoints) {
    checkArgument(foodpoints >= 0);
    if (!assertServerSide("setHunger")) return;
    float percentFed = foodpoints / (float)dragon.configuration().getVariantTagValue(DragonVariants.Category.METABOLISM, MAX_HUNGER);
    float newHungerLevel = serverHungerLevel + percentFed;
    setHunger(newHungerLevel);
    if (newHungerLevel >= MAX_HUNGER_PERCENT) {

    }

    float hungerSurplus = newHungerLevel - 100.0F;
    if (hungerSurplus <= 0.0F) return;
    healFraction(hungerSurplus / 100.0F * dragon.getMaxHealth());
  }

  /**
   * Feed the dragon using the item:
   *   restores hunger level and/or heals damage
   * NOTE- does not affect the itemStack passed in.
   * Only updates hunger and food on the server side.
   * Client side: performs success checks only
   * @param itemStack the item to feed to the dragon
   * @return true if the dragon ate it; false otherwise.
   */
  public boolean feed(ItemStack itemStack) {
    if (!considersThisItemEdible(itemStack.getItem())) return false;
    if (!isHungry() && !needsHealing()) return false;
    if (!dragon.isServer()) return true;

    int foodpoints = 0;
    if (itemStack.getItem() instanceof ItemFood) {
      ItemFood itemFood = (ItemFood)itemStack.getItem();
      foodpoints = itemFood.getHealAmount(itemStack);
    }
    if (foodpoints == 0) return true;

    float percentFed = 100.0F * foodpoints / (float)dragon.configuration().getVariantTagValue(DragonVariants.Category.METABOLISM, MAX_HUNGER);
    float newHungerLevel = serverHungerLevel + percentFed;
    setHunger(newHungerLevel);
    if (newHungerLevel >= MAX_HUNGER_PERCENT) {
      final int TICKS_PER_SECOND = 20;
      serverHungerFullnessTicksLeft = (int)dragon.configuration().getVariantTagValue(DragonVariants.Category.METABOLISM, FULLNESS_TIME) * TICKS_PER_SECOND;
    }

    float hungerSurplus = newHungerLevel - 100.0F;
    if (hungerSurplus <= 0.0F) return true;
    healFraction(hungerSurplus / 100.0F * dragon.getMaxHealth());
    return true;
  }

  /**
   * Checks if the dragon's health is not full and not zero.
   */
  public boolean needsHealing() {
    return dragon.getHealth() > 0.0F && dragon.getHealth() < dragon.getMaxHealth();
  }

  /**
   * Will the dragon eat this item?
   * Does not pay attention to whether the dragon is currently hungry or not
   * @param item
   * @return true if the dragon will eat it.
   */
  public boolean considersThisItemEdible(Item item) {
    if (foodItemsWhiteList.contains(item)) return true;
    if (foodItemsBlackList.contains(item)) return false;
    if (item instanceof ItemFood
        && (boolean)dragon.configuration().getVariantTagValue(DragonVariants.Category.METABOLISM, EATS_ITEMFOOD)) {
      return true;
    }
    return false;
  }

  /**
   * Change hunger level and heal damage as appropriate
   * a) when hunger is > threshold, then regenerate health proportional to (hunger - threshold) / (max hunger - threshold) * max_regen_rate
   * b) decrease hunger level (when hunger drops to zero, it doesn't cause health damage)
   */
  private void tickHungerAndHealth() {
    final float TICKS_PER_MINUTE = 60.0F * 20.0F;

    float hungerThreshold = (float)dragon.configuration().getVariantTagValue(DragonVariants.Category.METABOLISM, HUNGER_HEAL_THRESHOLD);
    if (serverHungerLevel >= hungerThreshold) {
      float maxHealthRegenRatePCperMin = (float)dragon.configuration().getVariantTagValue(DragonVariants.Category.METABOLISM, MAX_HEALTH_REGEN_RATE) *
              0.01F * (float)DragonMounts.instance.getConfig().HEALTH_REGEN_RATE_MULTIPLIER_PERCENT;
      float hungerSurplus = serverHungerLevel - hungerThreshold;
      float relativeHungerSurplus = (hungerThreshold >= 99.0F) ? 1.0F : (hungerSurplus / (100.0F - hungerThreshold));  // prevent divide by zero
      float regenRatePCperMin = relativeHungerSurplus * maxHealthRegenRatePCperMin;
      float healAmount = regenRatePCperMin * dragon.getMaxHealth() / TICKS_PER_MINUTE;
      healFraction(healAmount);
    }

    if (serverHungerFullnessTicksLeft > 0) {
      --serverHungerFullnessTicksLeft;
    } else {
      float hungerRatePCperMin = (float) dragon.configuration().getVariantTagValue(DragonVariants.Category.METABOLISM, HUNGER_DECAY_RATE);
      hungerRatePCperMin *= 0.01 * DragonMounts.instance.getConfig().HUNGER_SPEED_MULTIPLIER_PERCENT;
      float hungerDecrease = hungerRatePCperMin * 0.01F / TICKS_PER_MINUTE;
      setHunger(serverHungerLevel - hungerDecrease);
    }
  }

  /**
   * Heal a fractional amount of health points
   * @param healAmount
   */
  private void healFraction(float healAmount) {
    checkArgument(healAmount >= 0.0F);
    fractionalHeal += healAmount;
    if (fractionalHeal < 1.0F) return;
    dragon.heal((int)fractionalHeal);
    fractionalHeal %= 1;
  }

  private float fractionalHeal = 0;  // keeps track of fractional healing, to avoid frequent health updates i.e. only when integer number of health points have been healed

  // hunger is tracked internally as a float (also NBT load/save) but is transmitted to client as integer to avoid frequent updates
  //  the units are percent
  private static final DataParameter<Integer> HUNGER = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.VARINT);
  private float serverHungerLevel;  // server side only: hunger level (0 --> 100%)
  private int serverHungerFullnessTicksLeft = 0;  // number of ticks remaining until hunger no longer stays at full

  private static final DragonVariantTag MAX_HUNGER = DragonVariantTag.addTag("maxhungerunits", 100, 0.0, 500,
          "the maximum hunger level of the full-size dragon? (vanilla player maximum = 20 units)").categories(DragonVariants.Category.METABOLISM);
  private static final DragonVariantTag HUNGER_DECAY_RATE = DragonVariantTag.addTag("hungerdecayrate", 10, 0.0, 1000,
          "the speed at which hunger level decreases (% of maximum per minute)").categories(DragonVariants.Category.METABOLISM);
  private static final DragonVariantTag HUNGER_HEAL_THRESHOLD = DragonVariantTag.addTag("hungerhealthreshold", 85, 0.0, 101,
          "when hunger level is at or above this threshold (%), the dragon regenerates health ").categories(DragonVariants.Category.METABOLISM);
  private static final DragonVariantTag MAX_HEALTH_REGEN_RATE = DragonVariantTag.addTag("maxhealthregenrate", 75, 0.0, 1000,
          "when hunger level is maximum, the dragon regenerates health at this rate (% per minute)").categories(DragonVariants.Category.METABOLISM);
  private static final DragonVariantTag FULLNESS_TIME = DragonVariantTag.addTag("fullnesstime", 60, 0, 1000,
          "when hunger level is raised to maximum, the dragon stays full for this length of time (seconds)").categories(DragonVariants.Category.METABOLISM);

  private static final String [] defaultFoodItemsWhitelist = {"item.wheat"};
  private static final String [] defaultFoodItemsBlacklist = {"item.spider_eye", "item.poisonous_potato", "item.rotten_flesh"};
  private static final DragonVariantTag FOOD_ITEMS_WHITELIST = DragonVariantTag.addTag("fooditemswhitelist", defaultFoodItemsWhitelist,
          "list of items which the dragon will eat").categories(DragonVariants.Category.METABOLISM);
  private static final DragonVariantTag FOOD_ITEMS_BLACKLIST = DragonVariantTag.addTag("fooditemsblacklist", defaultFoodItemsBlacklist,
          "list of items which the dragon won't eat").categories(DragonVariants.Category.METABOLISM);
  private static final DragonVariantTag EATS_ITEMFOOD = DragonVariantTag.addTag("fooditemsstandard", true,
          "does the dragon eat standard food items?").categories(DragonVariants.Category.METABOLISM);

  private Set<Item> foodItemsWhiteList = new HashSet<>();
  private Set<Item> foodItemsBlackList = new HashSet<>();

  // ------------------ Immunities and attributes -----------------


  /**
   * Does this type of potion affect the dragon?
   * @param potioneffectIn
   * @return true if it is affected by this type of potion
   */
  public boolean isPotionApplicable(PotionEffect potioneffectIn) {
    // see EntitySpider
    Potion potion = potioneffectIn.getPotion();
    if (potionImmunities.contains(potion)) return false;
    return true;
  }

  /**
   * Is the dragon invulnerable to damage from this source?
   * @param src
   * @return true if invulnerable
   */
  public boolean isInvulnerableToThisDamageSource(DamageSource src) {
    // invulnerable to attacks from self, from riders, and from configurable immunities

    Entity srcEnt = src.getImmediateSource();
    if (srcEnt != null) {
      if (srcEnt == dragon) return true;
      if (dragon.riding().hasThisPassenger(srcEnt)) return true;
    }
    return isImmuneToDamageType(src);
  }

  public boolean isImmuneToDamageType(DamageSource dmg) {
    return damageSourceImmunities.contains(dmg.getDamageType());
  }

  public EnumCreatureAttribute getCreatureAttribute() {
    return enumCreatureAttribute;
  }

  private HashSet<String> damageSourceImmunities = new HashSet<>();
  private static final String [] defaultDamageSourceImmunities = {"drown", "inWall", "onFire", "inFire", "cactus", "fall"};
  private static final DragonVariantTag DAMAGE_SOURCE_IMMUNITIES = DragonVariantTag.addTag("damagesourceimmunity", defaultDamageSourceImmunities,
          "list of damage sources that the dragon is immune to (vanilla names)").categories(DragonVariants.Category.METABOLISM);

  private HashSet<Potion> potionImmunities = new HashSet<>();
  private static final String [] defaultPotionImmunities = {"weakness"};
  private static final DragonVariantTag POTION_IMMUNITIES = DragonVariantTag.addTag("potionimmunity", defaultPotionImmunities,
          "list of potion effects that the dragon is immune to (vanilla names)").categories(DragonVariants.Category.METABOLISM);

  private EnumCreatureAttribute enumCreatureAttribute;
  private static final DragonVariantTag CREATURE_ATTRIBUTE = DragonVariantTag.addTag("creatureattribute", "UNDEFINED",
          "Is this dragon a special class of creature?").categories(DragonVariants.Category.METABOLISM)
          .values("UNDEFINED", "UNDEAD", "ARTHROPOD"," ILLAGER");


  public static final float DEFAULT_KNOCKBACK_RESISTANCE = 0.8F;
  private static final DragonVariantTag KNOCKBACK_RESISTANCE = DragonVariantTag.addTag("knockbackresistance", DEFAULT_KNOCKBACK_RESISTANCE, 0.0F, 1.0F,
          "Knockback resistance of the dragon (0 = no resistance, 1 = full resistance).").categories(DragonVariants.Category.METABOLISM);

  public static final double DEFAULT_FOLLOW_RANGE = 32;
  public static final double MIN_FOLLOW_RANGE = 16;
  public static final double MAX_FOLLOW_RANGE = 16;
  private static final DragonVariantTag FOLLOW_RANGE = DragonVariantTag.addTag("followrange", DEFAULT_FOLLOW_RANGE, MIN_FOLLOW_RANGE, MAX_FOLLOW_RANGE,
          "How far away does the dragon lose interest in a target (in blocks)?").categories(DragonVariants.Category.BEHAVIOUR);

  // --------- misc --------

  private boolean assertServerSide(String functionName) {
    if (dragon.isClient()) {
      DragonMounts.loggerLimit.error_once("Unexpectedly called DragonCombatHelper::" + functionName + "() on client side");
      return false;
    }
    return true;
  }

  // deliberately omitted OUT_OF_WORLD and CRAMMING to avoid destabilising the game
  private final static DamageSource [] validDamageSources = {
          DamageSource.IN_FIRE, DamageSource.LIGHTNING_BOLT, DamageSource.ON_FIRE, DamageSource.LAVA, DamageSource.HOT_FLOOR,
          DamageSource.IN_WALL, DamageSource.DROWN, DamageSource.STARVE, DamageSource.CACTUS, DamageSource.FALL, DamageSource.FLY_INTO_WALL,
          DamageSource.GENERIC, DamageSource.MAGIC, DamageSource.WITHER, DamageSource.ANVIL, DamageSource.FALLING_BLOCK, DamageSource.DRAGON_BREATH,
          DamageSource.FIREWORKS
        };
  private final static Set<String> validDamageSourceNames = new HashSet<>(validDamageSources.length);

  /**
   * Validates the following aspects of the tags:
   * 1) damage source names (immunity)
   * 2) potion immunities
   * 3) food items
   * If any errors are found, revert to the defaults and throw an error
   */
  public static class DragonCombatValidator implements DragonVariants.VariantTagValidator {
    @Override
    public void validateVariantTags(DragonVariants dragonVariants, DragonVariants.ModifiedCategory modifiedCategory) throws IllegalArgumentException {
      DragonVariantsException.DragonVariantsErrors dragonVariantsErrors = new DragonVariantsException.DragonVariantsErrors();
      if (!modifiedCategory.getCategory().equals(DragonVariants.Category.METABOLISM)) return;

      if (validDamageSourceNames.isEmpty()) {
        for (DamageSource damageSource : validDamageSources) {
          validDamageSourceNames.add(damageSource.getDamageType());
        }
      }

      String [] damageSourceNames = (String [])dragonVariants.getValueOrDefault(modifiedCategory, DAMAGE_SOURCE_IMMUNITIES);
      ArrayList<String> badNames = new ArrayList<>();
      for (String name : damageSourceNames) {
        if (!validDamageSourceNames.contains(name)) {
          badNames.add(name);
        }
      }
      if (badNames.size() > 0) {
        StringBuilder sb = new StringBuilder();
        sb.append("one or more damage source names were not found:");
        boolean first = true;
        for (String badItemName : badNames) {
          if (!first) sb.append(",");
          sb.append("\"");
          sb.append(badItemName);
          sb.append("\"");
          first = false;
        }
        dragonVariants.removeTag(DragonVariants.Category.METABOLISM, DAMAGE_SOURCE_IMMUNITIES);
        dragonVariantsErrors.addError(sb.toString());
      }

      String [] potionNames = (String [])dragonVariants.getValueOrDefault(modifiedCategory, POTION_IMMUNITIES);
      badNames.clear();
      for (String name : potionNames) {
        if (null == Potion.getPotionFromResourceLocation(name)) {
          badNames.add(name);
        }
      }
      if (badNames.size() > 0) {
        StringBuilder sb = new StringBuilder();
        sb.append("one or more Potion names were not found:");
        boolean first = true;
        for (String badPotionName : badNames) {
          if (!first) sb.append(",");
          sb.append("\"");
          sb.append(badPotionName);
          sb.append("\"");
          first = false;
        }
        dragonVariants.removeTag(DragonVariants.Category.METABOLISM, POTION_IMMUNITIES);
        dragonVariantsErrors.addError(sb.toString());
      }

      String [] foodItemNames = (String [])dragonVariants.getValueOrDefault(modifiedCategory, FOOD_ITEMS_WHITELIST);
      if (!areItemNamesValid(foodItemNames,dragonVariantsErrors,"Invalid item names:")) {
        dragonVariants.removeTag(DragonVariants.Category.REPRODUCTION, FOOD_ITEMS_WHITELIST);
      }
      foodItemNames = (String [])dragonVariants.getValueOrDefault(modifiedCategory, FOOD_ITEMS_BLACKLIST);
      if (!areItemNamesValid(foodItemNames,dragonVariantsErrors,"Invalid item names:")) {
        dragonVariants.removeTag(DragonVariants.Category.REPRODUCTION, FOOD_ITEMS_BLACKLIST);
      }

      if (dragonVariantsErrors.hasErrors()) {
        throw new DragonVariantsException(dragonVariantsErrors);
      }
    }
    @Override
    public void initaliseResources(DragonVariants dragonVariants, DragonVariants.ModifiedCategory modifiedCategory) throws IllegalArgumentException {
      // do nothing - no resources to initialise
    }

    /**
     * Check the list of itemnames to make sure all exist.
     * If any do not exist, add them to the error message
     * @param itemNames
     * @param dragonVariantsErrors
     * @param notFoundErrorMsg Error message prefix to be used
     * @return true if all existed, false if any did not exist
     */
    private static boolean areItemNamesValid(String [] itemNames, DragonVariantsException.DragonVariantsErrors dragonVariantsErrors,
                                                    String notFoundErrorMsg) {
      ArrayList<String> badItemNames = new ArrayList<>();
      for (String name : itemNames) {
        Item item = Item.REGISTRY.getObject(new ResourceLocation(name));
        if (item == null) {
          badItemNames.add(name);
        }
      }
      if (badItemNames.isEmpty()) return true;
      StringBuilder sb = new StringBuilder();
      sb.append(notFoundErrorMsg);
      boolean first = true;
      for (String badItemName : badItemNames) {
        if (!first) sb.append(",");
        sb.append("\"");
        sb.append(badItemName);
        sb.append("\"");
        first = false;
      }
      dragonVariantsErrors.addError(sb.toString());
      return false;
    }
  }



}


