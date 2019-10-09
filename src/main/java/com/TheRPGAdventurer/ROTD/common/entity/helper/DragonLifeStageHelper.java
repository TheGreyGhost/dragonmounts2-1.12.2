/*
 ** 2013 October 27
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.common.entity.helper;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeP;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants.Category;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantsException;
import com.TheRPGAdventurer.ROTD.util.ClientServerSynchronisedTickCount;
import com.TheRPGAdventurer.ROTD.util.debugging.DebugSettings;
import com.TheRPGAdventurer.ROTD.util.math.Interpolation;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Doubles;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static net.minecraft.entity.SharedMonsterAttributes.*;

/**
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 *
 * DragonLifeStageHelper is responsible for keeping track of the dragon's age and maturity
 *
 * The age of the dragon affects the following aspects:
 * 1) PhysicalSize (metres) - for the base dragon, this is the height of the top of the back
 * 2) PhysicalMaturity (0->100%) - the physical abilities of the dragon such as being able to fly,
 * 3) EmotionalMaturity (0->100%) - the behaviour of the dragon eg sticking close to parent, running away from mobs
 * 4) BreathWeaponMaturity (0->100%) - the strength of the breath weapon
 * 5) AttackDamageMultiplier (0->100%) - physical attack damage
 * 6) HealthMultiplier (0->100%) - health
 * 7) ArmourMultiplier (0->100%) - armour
 * 8) ArmourToughnessMultiplier (0->100%) - armour toughness
 *
 * It uses a number of DragonVariantTags to customise the effects of age on each of these
 *
 * Usage:
 * 1) During initial setup, call DragonLifeStageHelper.registerConfigurationTags() to register all the tags that are
 *    used to configure this, and to set up the variant tag validation
 * 2) Create the DragonLifeStageHelper as described in DragonHelper
 * 3) Call its update methods to keep it synchronised as described in DragonHelper
 *
 * Is responsible for setting MAX_HEALTH, ARMOR, ARMOR_TOUGHNESS and ATTACK_DAMAGE:
 *    these are applied via the corresponding attributes using a DragonLifeStageModifier
 */
public class DragonLifeStageHelper extends DragonHelper {

  /**
   * Initialise all the configuration tags used by this helper
   */
  public static void registerConfigurationTags()
  {
    // the initialisation of the tags is all done in their static initialisers
    DragonVariants.addVariantTagValidator(new DragonLifeStageValidator());
  }


  public DragonLifeStageHelper(EntityTameableDragon dragon) {
    super(dragon);
    DragonVariants.ModifiedCategory modifiedCategory =
            new DragonVariants.ModifiedCategory(Category.LIFE_STAGE, dragon.getConfigurationFileCategoryModifiers());
    try {
      readConfiguration(dragonVariants, modifiedCategory);
    } catch (IllegalArgumentException iae) {
      DragonMounts.loggerLimit.warn_once(iae.getMessage());
    }


    if (dragon.isClient()) {
      ticksSinceCreationClient = new ClientServerSynchronisedTickCount(TICKS_SINCE_CREATION_UPDATE_INTERVAL);
      ticksSinceCreationClient.reset(ticksSinceCreationServer);
    } else {
      ticksSinceCreationClient = null;
    }
    setCompleted(FunctionTag.CONSTRUCTOR);
  }

  private DragonLifeStageHelper(DragonVariants dragonVariants) {
    super();
    testingClass = true;
    ticksSinceCreationClient = null;
    try {
      readConfiguration(dragonVariants, new DragonVariants.ModifiedCategory(Category.LIFE_STAGE));
    } catch (IllegalArgumentException iae) {
      DragonMounts.loggerLimit.warn_once(iae.getMessage());
    }
  }

  @Override
  public void registerDataParameters() {
    checkPreConditions(FunctionTag.REGISTER_DATA_PARAMETERS);
    entityDataManager.register(DATA_TICKS_SINCE_CREATION, ticksSinceCreationServer);  //default value
    setCompleted(FunctionTag.REGISTER_DATA_PARAMETERS);
  }

  @Override
  public void registerEntityAttributes() {
    checkPreConditions(FunctionTag.REGISTER_ENTITY_ATTRIBUTES);
    dragon.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
    setCompleted(FunctionTag.REGISTER_ENTITY_ATTRIBUTES);
  }

  @Override
  public void initialiseServerSide() {
    checkPreConditions(FunctionTag.INITIALISE_SERVER);
    initialiseBothSides();
    setCompleted(FunctionTag.INITIALISE_SERVER);
  }

  @Override
  public void initialiseClientSide() {
    checkPreConditions(FunctionTag.INITIALISE_CLIENT);
    initialiseBothSides();
    setCompleted(FunctionTag.INITIALISE_CLIENT);
  }

  private void initialiseBothSides() {
    DragonConfigurationHelper dch = dragon.getConfigurationHelper();
    dragon.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue((double) dch.getVariantTagValue(Category.LIFE_STAGE, ATTACKDAMAGEBASE));
    dragon.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue((double)dch.getVariantTagValue(Category.LIFE_STAGE, ARMOURBASE));
    dragon.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).setBaseValue((double)dch.getVariantTagValue(Category.LIFE_STAGE, ARMOURTOUGHNESSBASE));
    dragon.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue((double)dch.getVariantTagValue(Category.LIFE_STAGE, HEALTHBASE));
  }

  @Override
  public void notifyDataManagerChange(DataParameter<?> key) {

  }

  @Override
  public void onConfigurationChange() {
    initialiseBothSides();
  }

  /** get the physical maturity of the dragon at its current age
   * @return  physical maturity, from 0 % to 100.0 %
   */
  public double getPhysicalMaturity() { return Interpolation.linearArray(getTicksSinceCreation() / TICKS_PER_MINECRAFT_DAY,
          lifeStageAges, physicalMaturityPoints);  }

  /** get the emotional maturity of the dragon at its current age
   * @return  emotional maturity, from 0 % to 100.0 %
   */
  public double getEmotionalMaturity() { return Interpolation.linearArray(getTicksSinceCreation() / TICKS_PER_MINECRAFT_DAY,
          lifeStageAges, emotionalMaturityPoints);  }

  /** get the breath maturity of the dragon at its current age
   * @return  breath maturity, from 0 % to 100.0 %
   */
  public double getBreathMaturity() { return Interpolation.linearArray(getTicksSinceCreation() / TICKS_PER_MINECRAFT_DAY,
          lifeStageAges, breathMaturityPoints);  }

  /** get the physical size of the dragon at its current age
   * @return  height to the top of the dragon's back, in metres.
   */
  public double getPhysicalSize() {
    double index = Interpolation.findIndex(getTicksSinceCreation() / TICKS_PER_MINECRAFT_DAY, lifeStageAges);
    int idx = (int)index;
    double frac = index - idx;
    double partialStage = 0;

    if (idx < growthratePoints.length-1) {
      double growthRateNow = Interpolation.linear(growthratePoints[idx], growthratePoints[idx+1], frac);
      partialStage = frac * (lifeStageAges[idx+1] - lifeStageAges[idx]) * (growthratePoints[idx] + growthRateNow) / 2.0;
    }
    double size = physicalSizePoints[idx] + partialStage;
    size = MathX.clamp(size, SIZE_MIN, SIZE_MAX);
    return size;
  }

  /** get the unmodified health points of the dragon at its current age
   * @return  health points
   */
  public double getHealth() { return Interpolation.linearArray(getTicksSinceCreation() / TICKS_PER_MINECRAFT_DAY,
          lifeStageAges, health);  }

  /** get the unmodified attack damage of the dragon at its current age
   * @return  attack damage
   */
  public double getAttackDamage() { return Interpolation.linearArray(getTicksSinceCreation() / TICKS_PER_MINECRAFT_DAY,
          lifeStageAges, attackdamage);  }

  /** get the unmodified armour of the dragon at its current age
   * @return  armour
   */
  public double getArmour() { return Interpolation.linearArray(getTicksSinceCreation() / TICKS_PER_MINECRAFT_DAY,
          lifeStageAges, armour);  }

  /** get the unmodified armour toughness of the dragon at its current age
   * @return  armour toughness
   */
  public double getArmourToughness() { return Interpolation.linearArray(getTicksSinceCreation() / TICKS_PER_MINECRAFT_DAY,
          lifeStageAges, armourtoughness);  }


  /**
   * get the text label for this age (infant, child, etc)
   * @return
   */
  public String getAgeLabel() {
    double index = Interpolation.findIndex(getTicksSinceCreation() / TICKS_PER_MINECRAFT_DAY, lifeStageAges);
    return ageLabels[(int)index].getTextLabel();
  }

  /**
   * Returns the current life stage of the dragon.
   *
   * @return current life stage
   */
  public DragonLifeStage getLifeStage() {
    int age = getTicksSinceCreation();
    return DragonLifeStage.getLifeStageFromTickCount(age);
  }

  /**
   * Sets a new life stage for the dragon.
   *
   * @param lifeStage
   */
  public final void setLifeStage(DragonLifeStage lifeStage) {
    L.trace("setLifeStage({})", lifeStage);
    if (dragon.isServer()) {
      ticksSinceCreationServer = lifeStage.getStartTickCount();
      entityDataManager.set(dataParam, ticksSinceCreationServer);
    } else {
      L.error("setLifeStage called on Client");
    }
    updateLifeStage();
  }

  public int getTicksSinceCreation() {
    if (DebugSettings.existsDebugParameter("forcedageticks")) {
      return (int) DebugSettings.getDebugParameter("forcedageticks");
    }
    if (testingClass) {
      return testingTicksSinceCreation;
    }

    if (dragon.isServer()) {
      return ticksSinceCreationServer;
    } else {
      return ticksSinceCreationClient.getCurrentTickCount();
    }
  }

  public void setTicksSinceCreation(int ticksSinceCreation) {
    if (dragon.isServer()) {
      ticksSinceCreationServer = ticksSinceCreation;
    } else {
      ticksSinceCreationClient.updateFromServer(ticksSinceCreationServer);
    }
  }

  /**
   * What is the maximum size of this dragon at any age?
   * Useful for rendering scale calculations
   * @return the maximum size (in metres to the top of the back)
   */
  public double getMaximumSizeAtAnyAge() {
    return maximumSizeAtAnyAge;
  }

  @Override
  public void onLivingUpdate() {
    // if the dragon is not an adult or paused, update its growth ticks
    if (dragon.isServer()) {
      if (!isFullyGrown() && !dragon.isGrowthPaused()) {
        ticksSinceCreationServer++;
        if (ticksSinceCreationServer % TICKS_SINCE_CREATION_UPDATE_INTERVAL == 0)
          entityDataManager.set(dataParam, ticksSinceCreationServer);
      }
    } else {
      ticksSinceCreationClient.updateFromServer(entityDataManager.get(dataParam));
      if (!isFullyGrown()) ticksSinceCreationClient.tick();
    }

    updateLifeStage();
    updateAgeScale();
  }

  @Override
  public void onDeath() {
  }

  public boolean isFullyGrown() {
    return getLifeStage().isFullyGrown();
  }

  /**
   * Does this life stage act like a minecraft baby?
   *
   * @return
   */
  public boolean isBaby() {
    return getLifeStage().isBaby();
  }

  public BreathNodeP.Power getBreathPowerP() {
    BreathNodeP.Power power = BREATHNODEP_POWER_BY_STAGE.get(getLifeStage());
    if (power == null) {
      DragonMounts.loggerLimit.error_once("Illegal lifestage in getBreathPowerP():" + getLifeStage());
      power = BreathNodeP.Power.SMALL;
    }
    return power;
  }


  public static final Map<DragonLifeStage, BreathNodeP.Power> BREATHNODEP_POWER_BY_STAGE =
          ImmutableMap.<DragonLifeStage, BreathNodeP.Power>builder()
                  .put(DragonLifeStage.EGG, BreathNodeP.Power.SMALL)           // dummy
                  .put(DragonLifeStage.HATCHLING, BreathNodeP.Power.SMALL)     // dummy
                  .put(DragonLifeStage.INFANT, BreathNodeP.Power.SMALL)        // dummy
                  .put(DragonLifeStage.PREJUVENILE, BreathNodeP.Power.SMALL)
                  .put(DragonLifeStage.JUVENILE, BreathNodeP.Power.MEDIUM)
                  .put(DragonLifeStage.ADULT, BreathNodeP.Power.LARGE)
                  .build();


  private void applyLifeStageModifiers() {
    applyLifeStageModifier(MAX_HEALTH);
    applyLifeStageModifier(ATTACK_DAMAGE);
    applyScaleModifierArmor(ARMOR);
  }

  private void applyLifeStageModifier(IAttribute attribute, double multiplier) {
    IAttributeInstance instance = dragon.getEntityAttribute(attribute);
    AttributeModifier oldModifier = instance.getModifier(DragonLifeStageModifier.ID);
    if (oldModifier != null) {
      instance.removeModifier(oldModifier);
    }
    instance.applyModifier(new DragonLifeStageModifier(multiplier));
  }

  private void applyScaleModifierArmor(IAttribute attribute) {
    IAttributeInstance instance = dragon.getEntityAttribute(attribute);
    AttributeModifier oldModifier = instance.getModifier(DragonLifeStageModifier.ID);
    if (oldModifier != null) {
      instance.removeModifier(oldModifier);
    }
    instance.applyModifier(new DragonLifeStageModifier(MathX.clamp(getAgeScale(), 0.1, 1.2)));
  }

  /**
   * Called when the dragon enters a new life stage.
   */
  private void onNewLifeStage(DragonLifeStage lifeStage, DragonLifeStage prevLifeStage) {
    L.trace("onNewLifeStage({},{})", prevLifeStage, lifeStage);

    if (dragon.isClient()) {
    } else {
      // update AI
      dragon.getBrain().updateAITasks();

      // update attribute modifier
      registerEntityAttributes();

      // heal dragon to updated full health
      dragon.setHealth(dragon.getMaxHealth());
    }
  }

  private void updateLifeStage() {
    // trigger event when a new life stage was reached
    DragonLifeStage lifeStage = getLifeStage();
    if (lifeStagePrev != lifeStage) {
      onNewLifeStage(lifeStage, lifeStagePrev);
      lifeStagePrev = lifeStage;
    }
  }

  private enum AgeLabel {

    HATCHLING("hatchling"),
    INFANT("infant"),
    CHILD("child"),
    EARLYTEEN("earlyteen"),
    LATETEEN("lateteen"),
    ADULT("adult");

    public String getTextLabel() {return textLabel;}

    AgeLabel(String textlabel) {
      this.textLabel = textlabel;
    }
    private String textLabel;
  }

  private void updateAgeScale() {
    dragon.setAgeScalePublic(getAgeScale());
  }

  /**
   * Returns the size multiplier for the current age.
   *
   * @return size
   */
  public float getAgeScale() {
    return DragonLifeStage.getAgeScaleFromTickCount(getTicksSinceCreation());
  }

  private static final Logger L = LogManager.getLogger();
  private static final String NBT_TICKS_SINCE_CREATION = "TicksSinceCreation";
  private static final int TICKS_SINCE_CREATION_UPDATE_INTERVAL = 100;
  // the ticks since creation is used to control the dragon's life stage.  It is only updated by the server occasionally.
  // the client keeps a cached copy of it and uses client ticks to interpolate in the gaps.
  // when the watcher is updated from the server, the client will tick it faster or slower to resynchronise
  private final ClientServerSynchronisedTickCount ticksSinceCreationClient;
  private DragonLifeStage lifeStagePrev;
  private int ticksSinceCreationServer;

  private boolean testingClass = false;
  private int testingTicksSinceCreation = 0;

  @Override
  public void writeToNBT(NBTTagCompound nbt) {
    nbt.setInteger(NBT_TICKS_SINCE_CREATION, getTicksSinceCreation());
  }

  @Override
  public void readFromNBT(NBTTagCompound nbt) {
    int ticksRead = nbt.getInteger(NBT_TICKS_SINCE_CREATION);
    ticksRead = DragonLifeStage.clipTickCountToValid(ticksRead);
    ticksSinceCreationServer = ticksRead;
    entityDataManager.set(dataParam, ticksSinceCreationServer);
  }

  private static final DataParameter<Integer> DATA_TICKS_SINCE_CREATION = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.VARINT);

  //----------------------------------

  // see codenotes - 190804-GrowthProfile and AgeProfile for explanation
  // assume dragons follow roughly human growth / maturity (why not?)
  private static final float AGE_HUMAN_INFANT = 2;
  private static final float AGE_HUMAN_CHILD = 5;
  private static final float AGE_HUMAN_EARLY_TEEN = 12;
  private static final float AGE_HUMAN_LATE_TEEN = 15;
  private static final float AGE_HUMAN_ADULT = 18;
  private static final float AGE_ADULT_MINECRAFT_DAYS = 3;  // we want our standard dragon to grow to full size in 1 hour = 3 minecraft days
  private static final float H2D = AGE_ADULT_MINECRAFT_DAYS / AGE_HUMAN_ADULT;  // human to dragon conversion

  private static final double TICKS_PER_SECOND = 20.0;
  private static final double REAL_LIFE_MINUTES_PER_MINECRAFT_DAY = 20.0;
  private static final double TICKS_PER_MINECRAFT_DAY = REAL_LIFE_MINUTES_PER_MINECRAFT_DAY * 60.0 * TICKS_PER_SECOND;
  private static final double MAX_AGE = Integer.MAX_VALUE / TICKS_PER_MINECRAFT_DAY;


  private static final DragonVariantTag AGE_INFANT = DragonVariantTag.addTag("age1infant", Math.rint(100*AGE_HUMAN_INFANT * H2D)/100.0, 0, MAX_AGE,
          "in minecraft days").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag AGE_CHILD = DragonVariantTag.addTag("age2child", Math.rint(100*AGE_HUMAN_CHILD * H2D)/100.0, 0, MAX_AGE,
          "in minecraft days").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag AGE_EARLY_TEEN = DragonVariantTag.addTag("age3earlyteen", Math.rint(100*AGE_HUMAN_EARLY_TEEN * H2D)/100.0, 0, MAX_AGE,
          "in minecraft days").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag AGE_LATE_TEEN = DragonVariantTag.addTag("age4lateteen", Math.rint(100*AGE_HUMAN_LATE_TEEN * H2D)/100.0, 0, MAX_AGE,
          "in minecraft days").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag AGE_ADULT = DragonVariantTag.addTag("age5adult", Math.rint(100*AGE_HUMAN_ADULT * H2D)/100.0, 0, MAX_AGE,
          "in minecraft days").categories(Category.LIFE_STAGE);

  // see 190804-GrowthProfile and AgeProfile for explanation
  private static final double SIZE_MIN = 0.01;  // size is in metres to the top of the dragon's back
  private static final double SIZE_MAX = 10.0;

  private static final DragonVariantTag GROWTHRATE_HATCHLING = DragonVariantTag.addTag("growthrate0hatchling", 10.0, -1000, 1000,
          "relative growth rate while a hatchling").categories(Category.LIFE_STAGE);   // relative growth rate
  private static final DragonVariantTag GROWTHRATE_INFANT = DragonVariantTag.addTag("growthrate1infant",  10.0, -1000, 1000,
          "relative growth rate while an infant").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag GROWTHRATE_CHILD = DragonVariantTag.addTag("growthrate2child",  100.0, -1000, 1000,
          "relative growth rate while a child").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag GROWTHRATE_EARLY_TEEN = DragonVariantTag.addTag("growthrate3earlyteen", 100.0, -1000, 1000,
          "relative growth rate while an early teen").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag GROWTHRATE_LATE_TEEN = DragonVariantTag.addTag("growthrate4lateteen", 400.0, -1000, 1000,
          "relative growth rate while a late teen").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag SIZE_HATCHLING = DragonVariantTag.addTag("size0hatchling", 0.1, SIZE_MIN, SIZE_MAX,
          "height of the hatchling's back, in metres").categories(Category.LIFE_STAGE);  // height of back in m
  private static final DragonVariantTag SIZE_ADULT = DragonVariantTag.addTag("size5adult", 2.0, SIZE_MIN, SIZE_MAX,
          "height of the adult's back, in metres").categories(Category.LIFE_STAGE);          // height of back in m

  // physical maturity = for physical abilities such as flying; 0% (hatchling) - 100% (adult)
  private static final DragonVariantTag PHYSICALMATURITY_INFANT = DragonVariantTag.addTag("physicalmaturity1infant", 10.0, 0, 100,
          "relative physical maturity of the infant (0 -> 100)").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag PHYSICALMATURITY_CHILD = DragonVariantTag.addTag("physicalmaturity2child", 30.0, 0, 100,
          "relative physical maturity of the child (0 -> 100)").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag PHYSICALMATURITY_EARLY_TEEN = DragonVariantTag.addTag("physicalmaturity3earlyteen", 50.0, 0, 100,
          "relative physical maturity of the early teen (0 -> 100)").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag PHYSICALMATURITY_LATE_TEEN = DragonVariantTag.addTag("physicalmaturity4lateteen", 80.0, 0, 100,
          "relative physical maturity of the late teen (0 -> 100)").categories(Category.LIFE_STAGE);

  // emotional maturity = for behaviour such as seeking out parents, danger aversion, etc
  private static final DragonVariantTag EMOTIONALMATURITY_INFANT = DragonVariantTag.addTag("emotionalmaturity1infant", 0.0, 0, 100,
          "relative emotional maturity (0 -> 100)").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag EMOTIONALMATURITY_CHILD = DragonVariantTag.addTag("emotionalmaturity2child", 30.0, 0, 100,
          "relative emotional maturity (0 -> 100)").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag EMOTIONALMATURITY_EARLY_TEEN = DragonVariantTag.addTag("emotionalmaturity3earlyteen", 30.0, 0, 100,
          "relative emotional maturity (0 -> 100)").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag EMOTIONALMATURITY_LATE_TEEN = DragonVariantTag.addTag("emotionalmaturity4lateteen", 75.0, 0, 100,
          "relative emotional maturity of the late teen (0 -> 100)").categories(Category.LIFE_STAGE);

  // breathweapon maturity 0% - 100%
  private static final DragonVariantTag BREATHMATURITY_HATCHLING = DragonVariantTag.addTag("breathmaturity0hatchling", 0.0, 0, 100,
          "relative breathweapon maturity (0 -> 100)").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag BREATHMATURITY_INFANT = DragonVariantTag.addTag("breathmaturity1infant", 0.0, 0, 100,
          "relative breathweapon maturity (0 -> 100)").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag BREATHMATURITY_CHILD = DragonVariantTag.addTag("breathmaturity2child", 0.0, 0, 100,
          "relative breathweapon maturity (0 -> 100)").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag BREATHMATURITY_EARLY_TEEN = DragonVariantTag.addTag("breathmaturity3earlyteen", 25.0, 0, 100,
          "relative breathweapon maturity (0 -> 100)").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag BREATHMATURITY_LATE_TEEN = DragonVariantTag.addTag("breathmaturity4lateteen", 75.0, 0, 100,
          "relative breathweapon maturity of the late teen (0 -> 100)").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag BREATHMATURITY_ADULT = DragonVariantTag.addTag("breathmaturity5adult", 100.0, 0, 100,
          "relative breathweapon maturity of the adult (0 -> 100)").categories(Category.LIFE_STAGE);

  // attack damage multiplier 0% - 100% * ATTACKDAMAGEBASE
  private static final DragonVariantTag ATTACKDAMAGEPERCENT_HATCHLING = DragonVariantTag.addTag("attackdamagepercent0hatchling", 0.0, 0, 100,
          "hatchling's relative attack damage (0 -> 100). Is multipied by attackdamagebase / 100 to give actual damage").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag ATTACKDAMAGEPERCENT_INFANT = DragonVariantTag.addTag("attackdamagepercent1infant", 0.0, 0, 100,
          "infant's relative attack damage (0 -> 100). Is multipied by attackdamagebase / 100 to give actual damage").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag ATTACKDAMAGEPERCENT_CHILD = DragonVariantTag.addTag("attackdamagepercent2child", 10.0, 0, 100,
          "child's relative attack damage (0 -> 100). Is multipied by attackdamagebase / 100 to give actual damage").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag ATTACKDAMAGEPERCENT_EARLY_TEEN = DragonVariantTag.addTag("attackdamagepercent3earlyteen", 40.0, 0, 100,
          "early teen's relative attack damage (0 -> 100). Is multipied by attackdamagebase / 100 to give actual damage").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag ATTACKDAMAGEPERCENT_LATE_TEEN = DragonVariantTag.addTag("attackdamagepercent4lateteen", 90.0, 0, 100,
          "late teen's relative attack damage (0 -> 100). Is multipied by attackdamagebase / 100 to give actual damage").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag ATTACKDAMAGEPERCENT_ADULT = DragonVariantTag.addTag("attackdamagepercent5adult", 100.0, 0, 100,
          "adult's relative attack damage (0 -> 100). Is multipied by attackdamagebase / 100 to give actual damage").categories(Category.LIFE_STAGE);

  // health multiplier 0% - 100% * HEALTHBASE
  private static final DragonVariantTag HEALTHPERCENT_HATCHLING = DragonVariantTag.addTag("healthpercent0hatchling", 1.0, 0, 100,
          "hatchling's relative health (0 -> 100). Is multipied by healthbase / 100 to give actual health").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag HEALTHPERCENT_INFANT = DragonVariantTag.addTag("healthpercent1infant", 1.0, 0, 100,
          "infant's relative health (0 -> 100). Is multipied by healthbase / 100 to give actual health").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag HEALTHPERCENT_CHILD = DragonVariantTag.addTag("healthpercent2child", 20.0, 0, 100,
          "child's relative health (0 -> 100). Is multipied by healthbase / 100 to give actual health").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag HEALTHPERCENT_EARLY_TEEN = DragonVariantTag.addTag("healthpercent3earlyteen", 50.0, 0, 100,
          "early teen's relative health (0 -> 100). Is multipied by healthbase / 100 to give actual health").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag HEALTHPERCENT_LATE_TEEN = DragonVariantTag.addTag("healthpercent4lateteen", 75.0, 0, 100,
          "late teen's relative health (0 -> 100). Is multipied by healthbase / 100 to give actual health").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag HEALTHPERCENT_ADULT = DragonVariantTag.addTag("healthpercent5adult", 100.0, 0, 100,
          "adult's relative health (0 -> 100). Is multipied by healthbase / 100 to give actual health").categories(Category.LIFE_STAGE);

  // armour multiplier 0% - 100% * ARMOURBASE
  private static final DragonVariantTag ARMOURPERCENT_HATCHLING = DragonVariantTag.addTag("armorpercent0hatchling", 0.0, 0, 100,
          "hatchling's relative armour (0 -> 100). Is multipied by armorbase / 100 to give actual armor").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag ARMOURPERCENT_INFANT = DragonVariantTag.addTag("armorpercent1infant", 0.0, 0, 100,
          "infant's relative armour (0 -> 100). Is multipied by armorbase / 100 to give actual armor").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag ARMOURPERCENT_CHILD = DragonVariantTag.addTag("armorpercent2child", 10.0, 0, 100,
          "child's relative armour (0 -> 100). Is multipied by armorbase / 100 to give actual armor").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag ARMOURPERCENT_EARLY_TEEN = DragonVariantTag.addTag("armorpercent3earlyteen", 20.0, 0, 100,
          "early teen's relative armour (0 -> 100). Is multipied by armorbase / 100 to give actual armor").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag ARMOURPERCENT_LATE_TEEN = DragonVariantTag.addTag("armorpercent4lateteen", 40.0, 0, 100,
          "late teen's relative armour (0 -> 100). Is multipied by armorbase / 100 to give actual armor").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag ARMOURPERCENT_ADULT = DragonVariantTag.addTag("armorpercent5adult", 100.0, 0, 100,
          "adult's relative armour (0 -> 100). Is multipied by armorbase / 100 to give actual armor").categories(Category.LIFE_STAGE);

  // armour toughness multiplier 0% - 100% * ARMOURTOUGHNESSBASE
  private static final DragonVariantTag ARMOURTOUGHNESSPERCENT_HATCHLING = DragonVariantTag.addTag("armortoughnesspercent0hatchling", 0.0, 0, 100,
          "hatchling's relative armour toughness (0 -> 100). Is multipied by armortoughnessbase / 100 to give actual toughness")
          .categories(Category.LIFE_STAGE);
  private static final DragonVariantTag ARMOURTOUGHNESSPERCENT_INFANT = DragonVariantTag.addTag("armortoughnesspercent1infant", 0.0, 0, 100,
          "infant's relative armour toughness (0 -> 100). Is multipied by armortoughnessbase / 100 to give actual toughness").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag ARMOURTOUGHNESSPERCENT_CHILD = DragonVariantTag.addTag("armortoughnesspercent2child", 0.0, 0, 100,
          "child's relative armour toughness (0 -> 100). Is multipied by armortoughnessbase / 100 to give actual toughness").categories(Category.LIFE_STAGE);
  private static final DragonVariantTag ARMOURTOUGHNESSPERCENT_EARLY_TEEN = DragonVariantTag.addTag("armortoughnesspercent3earlyteen", 0.0, 0, 100,
          "early teen's relative armour toughness (0 -> 100). Is multipied by armortoughnessbase / 100 to give actual toughness")
          .categories(Category.LIFE_STAGE);
  private static final DragonVariantTag ARMOURTOUGHNESSPERCENT_LATE_TEEN = DragonVariantTag.addTag("armortoughnesspercent4lateteen", 20.0, 0, 100,
          "late teen's relative armour toughness (0 -> 100). Is multipied by armortoughnessbase / 100 to give actual toughness")
          .categories(Category.LIFE_STAGE);
  private static final DragonVariantTag ARMOURTOUGHNESSPERCENT_ADULT = DragonVariantTag.addTag("armortoughnesspercent5adult", 100.0, 0, 100,
          "adult's relative armour toughness (0 -> 100). Is multipied by armortoughnessbase / 100 to give actual toughness").categories(Category.LIFE_STAGE);

  private static final double ATTACKDAMAGE_MIN = 0.0;
  private static final double ATTACKDAMAGE_MAX = 40.0;
  private static final double ATTACKDAMAGE_DEFAULT = 5.0;
  private static final double HEALTH_MIN = 1.0;
  private static final double HEALTH_MAX = 100.0;
  private static final double HEALTH_DEFAULT = 20.0;
  private static final double ARMOUR_MIN = 0.0;
  private static final double ARMOUR_MAX = 40.0;
  private static final double ARMOUR_DEFAULT = 8.0;
  private static final double ARMOURTOUGHNESS_MIN = 0.0;
  private static final double ARMOURTOUGHNESS_MAX = 100.0;
  private static final double ARMOURTOUGHNESS_DEFAULT = 30.0;

  // base values.  Multiplied by the XXXMULTIPLIER_HATCHLING at a given age.
  private static final DragonVariantTag ATTACKDAMAGEBASE = DragonVariantTag.addTag("attackdamagebase",
          ATTACKDAMAGE_DEFAULT, ATTACKDAMAGE_MIN, ATTACKDAMAGE_MAX,
          "the base attack damage (as per vanilla): modified by the attackdamagepercent tags")
          .categories(Category.LIFE_STAGE);
  private static final DragonVariantTag HEALTHBASE = DragonVariantTag.addTag("healthbase", HEALTH_DEFAULT, HEALTH_MIN, HEALTH_MAX,
          "the base health of the dragon in hearts (as per vanilla): modified by the healthpercent tags")
          .categories(Category.LIFE_STAGE);
  private static final DragonVariantTag ARMOURBASE = DragonVariantTag.addTag("armorbase", ARMOUR_DEFAULT, ARMOUR_MIN, ARMOUR_MAX,
          "the base armor (as per vanilla): modified by the armorpercent tags")
          .categories(Category.LIFE_STAGE);
  private static final DragonVariantTag ARMOURTOUGHNESSBASE = DragonVariantTag.addTag("armortoughnessbase",
          ARMOURTOUGHNESS_DEFAULT, ARMOURTOUGHNESS_MIN, ARMOURTOUGHNESS_MAX,
          "the base armor toughness (as per vanilla): modified by the armortoughnesspercent tags")
          .categories(Category.LIFE_STAGE);

  /** interpolation arrays:
   * lifeStageAges is the age corresponding to each ageLabel, in minecraft days
   * breathMaturityPoints, physicalMaturityPoints, emotionalMaturityPoints are the corresponding curve points, to be
   *    linearly interpolated
   * growthratePoints is the growth rate curve points which are then integrated to give the physical size
   * physicalSizePoints is the integral of the growth rate
   */
  private static final AgeLabel [] ageLabels = {AgeLabel.HATCHLING, AgeLabel.INFANT, AgeLabel.CHILD, AgeLabel.EARLYTEEN, AgeLabel.LATETEEN, AgeLabel.ADULT};
  private double [] lifeStageAges = new double[AgeLabel.values().length];             // in ticks
  private double [] breathMaturityPoints = new double[AgeLabel.values().length];      // 0 - 100%
  private double [] physicalMaturityPoints = new double[AgeLabel.values().length];    // 0 - 100%
  private double [] emotionalMaturityPoints = new double[AgeLabel.values().length];   // 0 - 100%
  private double [] physicalSizePoints = new double[AgeLabel.values().length];        // metres to top of back
  private double [] growthratePoints = new double[AgeLabel.values().length];          // metres per day

  private double [] attackdamagePoints = new double[AgeLabel.values().length];
  private double [] healthPoints = new double[AgeLabel.values().length];
  private double [] armourPoints = new double[AgeLabel.values().length];
  private double [] armourtoughnessPoints = new double[AgeLabel.values().length];

  private double [] attackdamage = new double[AgeLabel.values().length];
  private double [] health = new double[AgeLabel.values().length];
  private double [] armour = new double[AgeLabel.values().length];
  private double [] armourtoughness = new double[AgeLabel.values().length];

  private double adultAgeTicks = 0.0;
  private double maximumSizeAtAnyAge = 0.0;

  private static double [] getLifeStageAges(DragonVariants dragonVariants, DragonVariants.ModifiedCategory modifiedCategory) {
    double [] lifeStageAgesConfig = {0.0,
            (double)dragonVariants.getValueOrDefault(modifiedCategory, AGE_INFANT),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, AGE_CHILD),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, AGE_EARLY_TEEN),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, AGE_LATE_TEEN),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, AGE_ADULT)
    };
    return lifeStageAgesConfig;
  }

  private static double [] getGrowthRatePoints(DragonVariants dragonVariants, DragonVariants.ModifiedCategory modifiedCategory) {
    double [] growthRatePointsConfig = {
            (double)dragonVariants.getValueOrDefault(modifiedCategory, GROWTHRATE_HATCHLING),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, GROWTHRATE_INFANT),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, GROWTHRATE_CHILD),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, GROWTHRATE_EARLY_TEEN),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, GROWTHRATE_LATE_TEEN),
            0.0
    };
    return growthRatePointsConfig;
  }

  /**
   * Read the configuration parameters from the DragonVariants config file, validate them, and convert them into
   *   internal structures
   * @param dragonVariants
   * @throws IllegalArgumentException if the configuration is bad (not expected, because validation should have fixed this already)
   */
  private void readConfiguration(DragonVariants dragonVariants, DragonVariants.ModifiedCategory modifiedCategory) throws IllegalArgumentException {
    lifeStageAges = getLifeStageAges(dragonVariants, modifiedCategory);
    adultAgeTicks = lifeStageAges[lifeStageAges.length - 1] * TICKS_PER_MINECRAFT_DAY;

    double [] init2 = {
            (double)dragonVariants.getValueOrDefault(modifiedCategory, BREATHMATURITY_HATCHLING),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, BREATHMATURITY_INFANT),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, BREATHMATURITY_CHILD),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, BREATHMATURITY_EARLY_TEEN),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, BREATHMATURITY_LATE_TEEN),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, BREATHMATURITY_ADULT)
            };
    breathMaturityPoints = init2;

    double [] init3 = {
            0.0,
            (double)dragonVariants.getValueOrDefault(modifiedCategory, PHYSICALMATURITY_INFANT),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, PHYSICALMATURITY_CHILD),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, PHYSICALMATURITY_EARLY_TEEN),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, PHYSICALMATURITY_LATE_TEEN),
            100.0
    };
    physicalMaturityPoints = init3;

    double [] init4 = {
            0.0,
            (double)dragonVariants.getValueOrDefault(modifiedCategory, EMOTIONALMATURITY_INFANT),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, EMOTIONALMATURITY_CHILD),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, EMOTIONALMATURITY_EARLY_TEEN),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, EMOTIONALMATURITY_LATE_TEEN),
            100.0
    };
    emotionalMaturityPoints = init4;

    double [] init5 = {
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ATTACKDAMAGEPERCENT_HATCHLING),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ATTACKDAMAGEPERCENT_INFANT),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ATTACKDAMAGEPERCENT_CHILD),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ATTACKDAMAGEPERCENT_EARLY_TEEN),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ATTACKDAMAGEPERCENT_LATE_TEEN),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ATTACKDAMAGEPERCENT_ADULT)
    };
    attackdamagePoints = multiplyArrayWithClipping(init5, 0.01, 0, 1);
    attackdamage = multiplyArrayWithClipping(init5, 0.01 *
                    (double)dragonVariants.getValueOrDefault(modifiedCategory, ATTACKDAMAGEBASE),
                    ATTACKDAMAGE_MIN, ATTACKDAMAGE_MAX);

    double [] init6 = {
            (double)dragonVariants.getValueOrDefault(modifiedCategory, HEALTHPERCENT_HATCHLING),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, HEALTHPERCENT_INFANT),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, HEALTHPERCENT_CHILD),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, HEALTHPERCENT_EARLY_TEEN),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, HEALTHPERCENT_LATE_TEEN),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, HEALTHPERCENT_ADULT)
    };
    healthPoints = multiplyArrayWithClipping(init6, 0.01, 0, 1);
    health = multiplyArrayWithClipping(init6, 0.01 *
                    (double)dragonVariants.getValueOrDefault(modifiedCategory, HEALTHBASE),
                    HEALTH_MIN, HEALTH_MAX);

    double [] init7 = {
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ARMOURPERCENT_HATCHLING),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ARMOURPERCENT_INFANT),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ARMOURPERCENT_CHILD),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ARMOURPERCENT_EARLY_TEEN),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ARMOURPERCENT_LATE_TEEN),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ARMOURPERCENT_ADULT)
    };
    armourPoints = multiplyArrayWithClipping(init7, 0.01, 0, 1);
    armour = multiplyArrayWithClipping(init7, 0.01 *
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ARMOURBASE),
            ARMOUR_MIN, ARMOUR_MAX);

    double [] init8 = {
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ARMOURTOUGHNESSPERCENT_HATCHLING),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ARMOURTOUGHNESSPERCENT_INFANT),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ARMOURTOUGHNESSPERCENT_CHILD),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ARMOURTOUGHNESSPERCENT_EARLY_TEEN),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ARMOURTOUGHNESSPERCENT_LATE_TEEN),
            (double)dragonVariants.getValueOrDefault(modifiedCategory, ARMOURTOUGHNESSPERCENT_ADULT)
    };
    armourtoughnessPoints = multiplyArrayWithClipping(init8, 0.01, 0, 1);
    armourtoughness = multiplyArrayWithClipping(init8, 0.01 *
                    (double)dragonVariants.getValueOrDefault(modifiedCategory, ARMOURTOUGHNESSBASE),
                    ARMOURTOUGHNESS_MIN, ARMOURTOUGHNESS_MAX);

    growthratePoints = getGrowthRatePoints(dragonVariants, modifiedCategory);
    Pair<double[], double []> curves = calculatePhysicalSizeCurve(dragonVariants, modifiedCategory, lifeStageAges, growthratePoints);
    physicalSizePoints = curves.getLeft();
    growthratePoints = curves.getRight();
    maximumSizeAtAnyAge = Doubles.max(physicalSizePoints);
  }

  /** multiply all entries in the array by the multiplier and clip all entries to [minval, maxval] inclusive
   *
   * @param input
   * @param multiplier
   * @param minval
   * @param maxval
   * @return
   */
  private double [] multiplyArrayWithClipping(double [] input, double multiplier, double minval, double maxval) {
    double [] retval = input.clone();
    for (int i = 0; i < retval.length; ++i) {
      double temp = retval[i];
      temp *= multiplier;
      if (temp < minval) {
        temp = minval;
      } else if (temp > maxval) {
        temp = maxval;
      }
      retval[i] = temp;
    }
    return retval;
  }

  /**
   * Integrates the growth curve to produce the physical size curve
   * Validates it at the same time
   * @param dragonVariants
   * @param lifeStageAges
   * @param growthratePoints
   * @return the physical size curve and the growth curve, scaled to units of metres and metres/day respectively
   * @throws IllegalArgumentException
   */
  private static Pair<double[], double[]> calculatePhysicalSizeCurve(DragonVariants dragonVariants, DragonVariants.ModifiedCategory modifiedCategory,
                                                                     double[] lifeStageAges, double[] growthratePoints)
          throws DragonVariantsException
  {
    DragonVariantsException.DragonVariantsErrors dragonVariantsErrors = new DragonVariantsException.DragonVariantsErrors();

    // The curve for physical size is produced by integrating the growth curve:
    // 1) Find the integral of growth rate.  This is the profile to grow from the initial size to the final size
    // 2) Renormalise the growth rate integral to match the units of size.
    //  eg if our growth rate is 100% for 1 day then 50% for 2 days, initial size is 1m and final size 2m:
    //  integral = 100%*1 + 50%*2 = 200%.days.  Growth = 2m - 1m = 1m
    //  So 200%.days is equivalent to 1m of growth, the normalisefactor is 1m / 200%.days = 0.005 m/%.days
    //  After the first day at 100% growth = 100%.days * 0.005 m/%.days = 0.5m + 1m initial size = 1.5m size.
    // The size is also clipped to the valid size range (can't go too small or too large)
    //    If growth rate goes negative, the size might actually briefly integrate outside the valid size range but
    //    it's not worth worrying about that I think
    double[] physicalSizePoints = new double[AgeLabel.values().length];
    physicalSizePoints[0] = 0.0;
    double minIntegral = 0;
    double maxIntegral = 0;
    for (int i = 1; i < growthratePoints.length; ++i) {
      physicalSizePoints[i] = physicalSizePoints[i-1]
              + (lifeStageAges[i] - lifeStageAges[i-1]) * (growthratePoints[i] + growthratePoints[i-1]) / 2.0;
      minIntegral = Math.min(minIntegral, physicalSizePoints[i]);
      maxIntegral = Math.max(maxIntegral, physicalSizePoints[i]);
    }
    double sizeHatchling = (double)dragonVariants.getValueOrDefault(modifiedCategory, SIZE_HATCHLING);
    double sizeAdult = (double)dragonVariants.getValueOrDefault(modifiedCategory, SIZE_ADULT);
    double integral = physicalSizePoints[growthratePoints.length-1];

    final double SMALL_NON_ZERO = 1;
    double normaliseFactor = 0.0;
    if (   (sizeAdult >= sizeHatchling && integral < SMALL_NON_ZERO)
        || (sizeAdult < sizeHatchling && integral > -SMALL_NON_ZERO))  {
      dragonVariantsErrors.addError(modifiedCategory.toString()
                                      + " growthrate curve can't be fitted to sizehatchling->sizeadult "
                                      + " (eg growthrates are positive but adult is smaller than hatchling)");
    } else {
      normaliseFactor = (sizeAdult - sizeHatchling) / integral;
      if (sizeHatchling + minIntegral * normaliseFactor < SIZE_MIN) {
        normaliseFactor = 0.0;
        dragonVariantsErrors.addError(modifiedCategory.toString()
                + " growthrate curve produces a dragon less than the minimum size of " + SIZE_MIN);
      } else if (sizeHatchling + maxIntegral * normaliseFactor > SIZE_MAX) {
        normaliseFactor = 0.0;
        dragonVariantsErrors.addError(modifiedCategory.toString()
                + " growthrate curve produces a dragon bigger than the maximum size of " + SIZE_MAX);
      }
    }
    for (int i = 0; i < physicalSizePoints.length; ++i) {
      physicalSizePoints[i] = sizeHatchling + physicalSizePoints[i] * normaliseFactor;
    }

    if (dragonVariantsErrors.hasErrors()) throw new DragonVariantsException(dragonVariantsErrors);

    double [] growthcurveNormalised = growthratePoints.clone();
    for (int i = 0; i < growthcurveNormalised.length; ++i) {
      growthcurveNormalised[i] *= normaliseFactor;
    }

    return new ImmutablePair<>(physicalSizePoints, growthcurveNormalised);
  }

  /**
   * Validates the following aspects of the tags:
   * 1) life stage ages are in the correct order
   * 2) growth rate curve matches the relative size of sizehatchling and sizeadult and doesn't produce a dragon smaller
   *    than minimum_size or bigger than maximum_size
   * If any errors are found, revert to the defaults and throw an error
   */
  public static class DragonLifeStageValidator implements DragonVariants.VariantTagValidator {
    @Override
    public void validateVariantTags(DragonVariants dragonVariants, DragonVariants.ModifiedCategory modifiedCategory) throws IllegalArgumentException {
      DragonVariantsException.DragonVariantsErrors dragonVariantsErrors = new DragonVariantsException.DragonVariantsErrors();
      if (!modifiedCategory.getCategory().equals(Category.LIFE_STAGE)) return;

      double [] lifeStageAgesConfig = getLifeStageAges(dragonVariants, modifiedCategory);
      if (!Interpolation.isValidInterpolationArray(lifeStageAgesConfig)) {
        DragonVariantTag [] tagsToRemove = {AGE_INFANT, AGE_CHILD, AGE_EARLY_TEEN, AGE_LATE_TEEN, AGE_ADULT};
        dragonVariants.removeTags(DragonVariants.Category.LIFE_STAGE, tagsToRemove);
        lifeStageAgesConfig = getLifeStageAges(dragonVariants, modifiedCategory);  // read defaults
        dragonVariantsErrors.addError(DragonVariants.Category.LIFE_STAGE.getTextName()
                + " age values invalid (each age must be bigger than the previous age)");
      }

      double [] growthRatePointsConfig = getGrowthRatePoints(dragonVariants, modifiedCategory);
      try {
        Pair<double[], double []> curves = calculatePhysicalSizeCurve(dragonVariants, modifiedCategory, lifeStageAgesConfig, growthRatePointsConfig);
      } catch (DragonVariantsException dve) {
        DragonVariantTag [] tagsToRemove = {GROWTHRATE_HATCHLING, GROWTHRATE_INFANT, GROWTHRATE_CHILD,
                                            GROWTHRATE_EARLY_TEEN, GROWTHRATE_LATE_TEEN, SIZE_HATCHLING, SIZE_ADULT};
        dragonVariants.removeTags(DragonVariants.Category.LIFE_STAGE, tagsToRemove);
        dragonVariantsErrors.addError(dve);
      }

      if (dragonVariantsErrors.hasErrors()) {
        throw new DragonVariantsException(dragonVariantsErrors);
      }
    }
    @Override
    public void initaliseResources(DragonVariants dragonVariants, DragonVariants.ModifiedCategory modifiedCategory) throws IllegalArgumentException {
      // do nothing - no resources to initialise
    }
  }

  /**
   * Run some tests on the class.  Inspect the results printed to console...
   * @param dragonVariants
   * @param testnumber
   */
  public static void testClass(DragonVariants dragonVariants, int testnumber) {
    DragonLifeStageHelper test = new DragonLifeStageHelper(dragonVariants);
    switch (testnumber) {
      case 0: {
        DragonMounts.logger.info("Age AgeLabel physicalmaturity breathmaturity emotionalmaturity physicalsize");

        for (double age = 0; age < 25; age += 1) {
          test.testingTicksSinceCreation = (int)(age * TICKS_PER_MINECRAFT_DAY);
          String output = String.format("%3f:%15s%20.1f%20.1f%20.1f%20.2f", age, test.getAgeLabel(), test.getPhysicalMaturity(),
                  test.getBreathMaturity(), test.getEmotionalMaturity(), test.getPhysicalSize());
          DragonMounts.logger.info(output);
        }
        DragonMounts.logger.info("Max size at any age:"+test.getMaximumSizeAtAnyAge());
        break;
      }
      case 1: {
        DragonMounts.logger.info("Age AgeLabel health attackdamage armour armourtoughness");

        for (double age = 0; age < 25; age += 1) {
          test.testingTicksSinceCreation = (int)(age * TICKS_PER_MINECRAFT_DAY);
          String output = String.format("%3f:%15s%20.1f%20.1f%20.1f%20.2f", age, test.getAgeLabel(), test.getHealth(),
                  test.getAttackDamage(), test.getArmour(), test.getArmourToughness());
          DragonMounts.logger.info(output);
        }
        break;
      }
      case 2: {  // output the default curves
        DragonVariants dvDefault = new DragonVariants("test");
        DragonLifeStageHelper testDefault = new DragonLifeStageHelper(dvDefault);
        DragonMounts.logger.info("Age AgeLabel physicalmaturity breathmaturity emotionalmaturity physicalsize");

        for (double age = 0; age <= 3; age += 0.01) {
          testDefault.testingTicksSinceCreation = (int)(age * TICKS_PER_MINECRAFT_DAY);
          String output = String.format("%3f:%15s%20.1f%20.1f%20.1f%20.2f", age, testDefault.getAgeLabel(), testDefault.getPhysicalMaturity(),
                  testDefault.getBreathMaturity(), testDefault.getEmotionalMaturity(), testDefault.getPhysicalSize());
          DragonMounts.logger.info(output);
        }
        DragonMounts.logger.info("Age AgeLabel health attackdamage armour armourtoughness");

        for (double age = 0; age <= 3; age += 0.01) {
          testDefault.testingTicksSinceCreation = (int)(age * TICKS_PER_MINECRAFT_DAY);
          String output = String.format("%3f:%15s%20.1f%20.1f%20.1f%20.2f", age, testDefault.getAgeLabel(), testDefault.getHealth(),
                  testDefault.getAttackDamage(), testDefault.getArmour(), testDefault.getArmourToughness());
          DragonMounts.logger.info(output);
        }
      }
    }
  }
}