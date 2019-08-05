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
import com.TheRPGAdventurer.ROTD.common.inits.ModSounds;
import com.TheRPGAdventurer.ROTD.util.ClientServerSynchronisedTickCount;
import com.TheRPGAdventurer.ROTD.util.debugging.DebugSettings;
import com.TheRPGAdventurer.ROTD.util.math.Interpolation;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
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
 *    used to configure this
 * 2) Create the DragonLifeStageHelper
 * 3) Call its update methods to keep it synchronised as described in DragonHelper
 *
 * The ageScale, used by vanilla, is set to 0->1.0 and is
 *
 */
public class DragonLifeStageHelper extends DragonHelper {

  public static final Map<DragonLifeStage, BreathNodeP.Power> BREATHNODEP_POWER_BY_STAGE =
          ImmutableMap.<DragonLifeStage, BreathNodeP.Power>builder()
                  .put(DragonLifeStage.EGG, BreathNodeP.Power.SMALL)           // dummy
                  .put(DragonLifeStage.HATCHLING, BreathNodeP.Power.SMALL)     // dummy
                  .put(DragonLifeStage.INFANT, BreathNodeP.Power.SMALL)        // dummy
                  .put(DragonLifeStage.PREJUVENILE, BreathNodeP.Power.SMALL)
                  .put(DragonLifeStage.JUVENILE, BreathNodeP.Power.MEDIUM)
                  .put(DragonLifeStage.ADULT, BreathNodeP.Power.LARGE)
                  .build();


  /**
   * Initialise all the configuration tags used by this helper
   */
  public static void registerConfigurationTags()
  {
    // dummy method -the initialisation is all done in static initialisers
  }

  public DragonLifeStageHelper(EntityTameableDragon dragon, DataParameter<Integer> dataParam, DragonVariants dragonVariants) {
    super(dragon);
    try {
      readConfiguration(dragonVariants);
    } catch (IllegalArgumentException iae) {
      DragonMounts.loggerLimit.warn_once(iae.getMessage());
    }

    this.dataParam = dataParam;
    dataWatcher.register(dataParam, ticksSinceCreationServer);

    if (dragon.isClient()) {
      ticksSinceCreationClient = new ClientServerSynchronisedTickCount(TICKS_SINCE_CREATION_UPDATE_INTERVAL);
      ticksSinceCreationClient.reset(ticksSinceCreationServer);
    } else {
      ticksSinceCreationClient = null;
    }
  }

  /** get the physical maturity of the dragon at its current age
   * @return  physical maturity, from 0 % to 100.0 %
   */
  public double getPhysicalMaturity() { return Interpolation.linearArray(getTicksSinceCreation() / adultAgeTicks,
          lifeStageAges, physicalMaturityPoints);  }

  /** get the emotional maturity of the dragon at its current age
   * @return  emotional maturity, from 0 % to 100.0 %
   */
  public double getEmotionalMaturity() { return Interpolation.linearArray(getTicksSinceCreation() / adultAgeTicks,
          lifeStageAges, emotionalMaturityPoints);  }

  /** get the breath maturity of the dragon at its current age
   * @return  breath maturity, from 0 % to 100.0 %
   */
  public double getBreathMaturity() { return Interpolation.linearArray(getTicksSinceCreation() / adultAgeTicks,
          lifeStageAges, breathMaturityPoints);  }

  /** get the physical size of the dragon at its current age
   * @return  height to the top of the dragon's back, in metres.
   */
  public double getPhysicalSize() {
    double index = Interpolation.findIndex(getTicksSinceCreation() / adultAgeTicks, lifeStageAges);
    int idx = (int)index;
    double frac = index - idx;
    double partialStage = 0;

    if (idx < growthratePoints.length-1) {
      partialStage = frac * (lifeStageAges[idx+1] - lifeStageAges[idx]) * (growthratePoints[idx] + growthratePoints[idx+1]) / 2.0;
    }
    return physicalSizePoints[idx] + partialStage;
  }

  /**
   * get the text label for this age (infant, child, etc)
   * @return
   */
  public String getAgeLabel() {
    double index = Interpolation.findIndex(getTicksSinceCreation() / adultAgeTicks, lifeStageAges);
    return ageLabels[(int)index].getTextLabel();
  }

  @Override
  public void applyEntityAttributes() {
    applyScaleModifier(MAX_HEALTH);
    applyScaleModifier(ATTACK_DAMAGE);
    applyScaleModifierArmor(ARMOR);
  }

  /**
   * Generates some egg shell particles and a breaking sound.
   */
  public void playEggCrackEffect() {
    // dragon.world.playEvent(2001, dragon.getPosition(), Block.getIdFromBlock(BlockDragonBreedEgg.DRAGON_BREED_EGG));
    this.playEvent(dragon.getPosition());
  }

  public void playEvent(BlockPos blockPosIn) {
    dragon.world.playSound(null, blockPosIn, ModSounds.DRAGON_HATCHING, SoundCategory.BLOCKS, +1.0F, 1.0F);
  }

  public int getEggWiggleX() {
    return eggWiggleX;
  }

  public int getEggWiggleZ() {
    return eggWiggleZ;
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
      dataWatcher.set(dataParam, ticksSinceCreationServer);
    } else {
      L.error("setLifeStage called on Client");
    }
    updateLifeStage();
  }

  public int getTicksSinceCreation() {
    if (DebugSettings.existsDebugParameter("forcedageticks")) {
      return (int) DebugSettings.getDebugParameter("forcedageticks");
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

  /**
   * Transforms the dragon to an egg (item form)
   */
  public void transformToEgg() {
    if (dragon.getHealth() <= 0) {
      // no can do
      return;
    }

    L.debug("transforming to egg");

    float volume = 3;
    float pitch = 1;
    dragon.playSound(SoundEvents.ENTITY_ENDERMEN_TELEPORT, volume, pitch);

    if (dragon.isSaddled()) {
      dragon.dropItem(Items.SADDLE, 1);
    }

    final float OFFSET_POSITION = 0.0F;
    dragon.entityDropItem(dragon.getBreedType().createEggItemStack(), OFFSET_POSITION);

    dragon.setDead();
  }

  @Override
  public void onLivingUpdate() {
    // if the dragon is not an adult pr paused, update its growth ticks
    if (dragon.isServer()) {
      if (!isFullyGrown() && !dragon.isGrowthPaused()) {
        ticksSinceCreationServer++;
        if (ticksSinceCreationServer % TICKS_SINCE_CREATION_UPDATE_INTERVAL == 0)
          dataWatcher.set(dataParam, ticksSinceCreationServer);
      }
    } else {
      ticksSinceCreationClient.updateFromServer(dataWatcher.get(dataParam));
      if (!isFullyGrown()) ticksSinceCreationClient.tick();
    }

    updateLifeStage();
    updateEgg();
    updateAgeScale();
  }

  public EntityDataManager getDataWatcher() {
    return dataWatcher;
  }

  @Override
  public void onDeath() {
    if (dragon.isClient() && isEgg()) {
      playEggCrackEffect();
    }
  }

  public boolean isEgg() {
    return getLifeStage().isEgg();
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

//    public DragonLifeStage getLifeStageP() {
//        int age = getTicksSinceCreation();
//        return DragonLifeStage.getLifeStageFromTickCount(age);
//    }

  public boolean isOldEnoughToBreathe() {
    return getLifeStage().isOldEnoughToBreathe();
  }

  public BreathNodeP.Power getBreathPowerP() {
    BreathNodeP.Power power = BREATHNODEP_POWER_BY_STAGE.get(getLifeStage());
    if (power == null) {
      DragonMounts.loggerLimit.error_once("Illegal lifestage in getBreathPowerP():" + getLifeStage());
      power = BreathNodeP.Power.SMALL;
    }
    return power;
  }

  protected EnumParticleTypes getEggParticle() {
    switch (dragon.getBreedType()) {
      case END:
        return EnumParticleTypes.PORTAL;
      case NETHER:
        return EnumParticleTypes.DRIP_LAVA;
      //All Eggs without special particles:
      default:
        return EnumParticleTypes.TOWN_AURA;
    }
  }

  private void applyScaleModifier(IAttribute attribute) {
    IAttributeInstance instance = dragon.getEntityAttribute(attribute);
    AttributeModifier oldModifier = instance.getModifier(DragonScaleModifier.ID);
    if (oldModifier != null) {
      instance.removeModifier(oldModifier);
    }
    instance.applyModifier(new DragonScaleModifier(MathX.clamp(getAgeScale(), 0.1, 1)));
  }

  private void applyScaleModifierArmor(IAttribute attribute) {
    IAttributeInstance instance = dragon.getEntityAttribute(attribute);
    AttributeModifier oldModifier = instance.getModifier(DragonScaleModifier.ID);
    if (oldModifier != null) {
      instance.removeModifier(oldModifier);
    }
    instance.applyModifier(new DragonScaleModifier(MathX.clamp(getAgeScale(), 0.1, 1.2)));
  }

  /**
   * Called when the dragon enters a new life stage.
   */
  private void onNewLifeStage(DragonLifeStage lifeStage, DragonLifeStage prevLifeStage) {
    L.trace("onNewLifeStage({},{})", prevLifeStage, lifeStage);

    if (dragon.isClient()) {
      // play particle and sound effects when the dragon hatches
      if (prevLifeStage != null && prevLifeStage.isEgg() && !lifeStage.isBaby()) {
        playEggCrackEffect();
        dragon.world.playSound(dragon.posX, dragon.posY, dragon.posZ, ModSounds.DRAGON_HATCHED, SoundCategory.BLOCKS, 4, 1, false);
      }
    } else {
      // update AI
      dragon.getBrain().updateAITasks();

      // update attribute modifier
      applyEntityAttributes();

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

  private void updateEgg() {
    if (!isEgg()) {
      return;
    }

    // animate egg wiggle based on the time the eggs take to hatch
    float progress = DragonLifeStage.getStageProgressFromTickCount(getTicksSinceCreation());

    // wait until the egg is nearly hatched
    if (progress > EGG_WIGGLE_THRESHOLD) {
      float wiggleChance = (progress - EGG_WIGGLE_THRESHOLD) / EGG_WIGGLE_BASE_CHANCE * (1 - EGG_WIGGLE_THRESHOLD);

      if (eggWiggleX > 0) {
        eggWiggleX--;
      } else if (rand.nextFloat() < wiggleChance) {
        eggWiggleX = rand.nextBoolean() ? 10 : 20;
        if (progress > EGG_CRACK_THRESHOLD) {
          playEggCrackEffect();
        }
      }

      if (eggWiggleZ > 0) {
        eggWiggleZ--;
      } else if (rand.nextFloat() < wiggleChance) {
        eggWiggleZ = rand.nextBoolean() ? 10 : 20;
        if (progress > EGG_CRACK_THRESHOLD) {
          playEggCrackEffect();
        }
      }
    }

    // spawn generic particles
    double px = dragon.posX + (rand.nextDouble() - 0.3);
    double py = dragon.posY + (rand.nextDouble() - 0.3);
    double pz = dragon.posZ + (rand.nextDouble() - 0.3);
    double ox = (rand.nextDouble() - 0.3) * 2;
    double oy = (rand.nextDouble() - 0.3) * 2;
    double oz = (rand.nextDouble() - 0.3) * 2;
    dragon.world.spawnParticle(this.getEggParticle(), px, py, pz, ox, oy, oz);

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
  private static final float EGG_CRACK_THRESHOLD = 0.9f;
  private static final float EGG_WIGGLE_THRESHOLD = 0.75f;
  private static final float EGG_WIGGLE_BASE_CHANCE = 20;
  // the ticks since creation is used to control the dragon's life stage.  It is only updated by the server occasionally.
  // the client keeps a cached copy of it and uses client ticks to interpolate in the gaps.
  // when the watcher is updated from the server, the client will tick it faster or slower to resynchronise
  private final DataParameter<Integer> dataParam;
  private final ClientServerSynchronisedTickCount ticksSinceCreationClient;
  private DragonLifeStage lifeStagePrev;
  private int eggWiggleX;

  private int eggWiggleZ;
  private int ticksSinceCreationServer;

  @Override
  public void writeToNBT(NBTTagCompound nbt) {
    nbt.setInteger(NBT_TICKS_SINCE_CREATION, getTicksSinceCreation());
  }

  @Override
  public void readFromNBT(NBTTagCompound nbt) {
    int ticksRead = nbt.getInteger(NBT_TICKS_SINCE_CREATION);
    ticksRead = DragonLifeStage.clipTickCountToValid(ticksRead);
    ticksSinceCreationServer = ticksRead;
    dataWatcher.set(dataParam, ticksSinceCreationServer);
  }

  // see codenotes - 190804-GrowthProfile and AgeProfile for explanation
  // assume dragons follow roughly human growth / maturity (why not?)
  private final float AGE_HUMAN_INFANT = 2;
  private final float AGE_HUMAN_CHILD = 5;
  private final float AGE_HUMAN_EARLY_TEEN = 12;
  private final float AGE_HUMAN_LATE_TEEN = 15;
  private final float AGE_HUMAN_ADULT = 18;
  private final float AGE_ADULT_MINECRAFT_DAYS = 3;  // we want our standard dragon to grow to full size in 1 hour = 3 minecraft days
  private final float H2D = AGE_ADULT_MINECRAFT_DAYS / AGE_HUMAN_ADULT;  // human to dragon conversion

  private final DragonVariantTag AGE_INFANT = DragonVariantTag.addTag("ageinfant", AGE_HUMAN_INFANT * H2D);
  private final DragonVariantTag AGE_CHILD = DragonVariantTag.addTag("agechild", AGE_HUMAN_CHILD * H2D);
  private final DragonVariantTag AGE_EARLY_TEEN = DragonVariantTag.addTag("ageearlyteen", AGE_HUMAN_EARLY_TEEN * H2D);
  private final DragonVariantTag AGE_LATE_TEEN = DragonVariantTag.addTag("agelateteen", AGE_HUMAN_LATE_TEEN * H2D);
  private final DragonVariantTag AGE_ADULT = DragonVariantTag.addTag("ageadult", AGE_HUMAN_ADULT * H2D);

  // see 190804-GrowthProfile and AgeProfile for explanation
  private final double MINIMUM_SIZE = 0.01;
  private final double MAXIMUM_SIZE = 10.0;

  private final DragonVariantTag GROWTHRATE_HATCHLING = DragonVariantTag.addTag("growthratehatchling", 10.0, -1000, 1000);   // relative growth rate
  private final DragonVariantTag GROWTHRATE_INFANT = DragonVariantTag.addTag("growthrateinfant",  10.0, -1000, 1000);
  private final DragonVariantTag GROWTHRATE_CHILD = DragonVariantTag.addTag("growthratechild",  100.0, -1000, 1000);
  private final DragonVariantTag GROWTHRATE_EARLY_TEEN = DragonVariantTag.addTag("growthratearlyteen", 100.0, -1000, 1000);
  private final DragonVariantTag GROWTHRATE_LATE_TEEN = DragonVariantTag.addTag("growthratelateteen", 400.0, -1000, 1000);
  private final DragonVariantTag SIZE_HATCHLING = DragonVariantTag.addTag("sizehatchling", 0.1, MINIMUM_SIZE, MAXIMUM_SIZE);  // height of back in m
  private final DragonVariantTag SIZE_ADULT = DragonVariantTag.addTag("sizeadult", 2.0, MINIMUM_SIZE, MAXIMUM_SIZE);          // height of back in m

  // physical maturity = for physical abilities such as flying; 0% (hatchling) - 100% (adult)
  private final DragonVariantTag PHYSICALMATURITY_INFANT = DragonVariantTag.addTag("physicalmaturityinfant", 10.0, 0, 100);
  private final DragonVariantTag PHYSICALMATURITY_CHILD = DragonVariantTag.addTag("physicalmaturitychild", 30.0, 0, 100);
  private final DragonVariantTag PHYSICALMATURITY_EARLY_TEEN = DragonVariantTag.addTag("physicalmaturityearlyteen", 50.0, 0, 100);
  private final DragonVariantTag PHYSICALMATURITY_LATE_TEEN = DragonVariantTag.addTag("physicalmaturitylateteen", 80.0, 0, 100);

  // emotional maturity = for behaviour such as seeking out parents, danger aversion, etc
  private final DragonVariantTag EMOTIONALMATURITY_INFANT = DragonVariantTag.addTag("emotionalmaturityinfant", 0.0, 0, 100);
  private final DragonVariantTag EMOTIONALMATURITY_CHILD = DragonVariantTag.addTag("emotionalmaturitychild", 30.0, 0, 100);
  private final DragonVariantTag EMOTIONALMATURITY_EARLY_TEEN = DragonVariantTag.addTag("emotionalmaturityearlyteen", 30.0, 0, 100);
  private final DragonVariantTag EMOTIONALMATURITY_LATE_TEEN = DragonVariantTag.addTag("emotionalmaturitylateteen", 75.0, 0, 100);

  // breathweapon maturity 0% - 100%
  private final DragonVariantTag BREATHMATURITY_HATCHLING = DragonVariantTag.addTag("breathmaturityhatchling", 0.0, 0, 100);
  private final DragonVariantTag BREATHMATURITY_INFANT = DragonVariantTag.addTag("breathmaturityinfant", 0.0, 0, 100);
  private final DragonVariantTag BREATHMATURITY_CHILD = DragonVariantTag.addTag("breathmaturitychild", 0.0, 0, 100);
  private final DragonVariantTag BREATHMATURITY_EARLY_TEEN = DragonVariantTag.addTag("breathmaturityearlyteen", 25.0, 0, 100);
  private final DragonVariantTag BREATHMATURITY_LATE_TEEN = DragonVariantTag.addTag("breathmaturitylateteen", 75.0, 0, 100);
  private final DragonVariantTag BREATHMATURITY_ADULT = DragonVariantTag.addTag("breathmaturityadult", 100.0, 0, 100);


  /** interpolation arrays:
   * lifeStageAges is the age corresponding to each ageLabel, in minecraft days
   * breathMaturityPoints, physicalMaturityPoints, emotionalMaturityPoints are the corresponding curve points, to be
   *    linearly interpolated
   * growthratePoints is the growth rate curve points which are then integrated to give the physical size
   * physicalSizePoints is the integral of the growth rate
   */
  private AgeLabel [] ageLabels = new AgeLabel[AgeLabel.values().length];
  private double [] lifeStageAges = new double[AgeLabel.values().length];             // in ticks
  private double [] breathMaturityPoints = new double[AgeLabel.values().length];      // 0 - 100%
  private double [] physicalMaturityPoints = new double[AgeLabel.values().length];    // 0 - 100%
  private double [] emotionalMaturityPoints = new double[AgeLabel.values().length];   // 0 - 100%
  private double [] physicalSizePoints = new double[AgeLabel.values().length];        // metres to top of back
  private double [] growthratePoints = new double[AgeLabel.values().length];          // metres per tick (yes I know this is very small)
  private double adultAgeTicks = 0.0;
  private double maximumSizeAtAnyAge = 0.0;

  /**
   * Read the configuration parameters from the DragonVariants config file, validate them, and convert them into
   *   internal structures
   * @param dragonVariants
   * @throws IllegalArgumentException if the configuration is bad (defaults to safe values before throwing)
   */
  private void readConfiguration(DragonVariants dragonVariants) throws IllegalArgumentException {
    boolean configOK = true;
    String configErrors = "";

    double [] lifeStageAgesConfig = {0.0,
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, AGE_INFANT),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, AGE_CHILD),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, AGE_EARLY_TEEN),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, AGE_LATE_TEEN),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, AGE_ADULT)
            };

    if (Interpolation.isValidInterpolationArray(lifeStageAgesConfig)) {
      lifeStageAges = lifeStageAgesConfig;
    } else {
      double [] lifeStageAgesDefault = {0.0,
              (double)AGE_INFANT.getDefaultValue(),
              (double)AGE_CHILD.getDefaultValue(),
              (double)AGE_EARLY_TEEN.getDefaultValue(),
              (double)AGE_LATE_TEEN.getDefaultValue(),
              (double)AGE_ADULT.getDefaultValue()
      };
      lifeStageAges = lifeStageAgesDefault;
      configOK = false;
      configErrors += DragonVariants.Category.LIFE_STAGE.getTextName() + " age values invalid (each age must be bigger than the previous age)";
    }

    final double TICKS_PER_SECOND = 20.0;
    final double REAL_LIFE_MINUTES_PER_MINECRAFT_DAY = 20.0;
    final double TICKS_PER_MINECRAFT_DAY = REAL_LIFE_MINUTES_PER_MINECRAFT_DAY * 60.0 * TICKS_PER_SECOND;
    adultAgeTicks = lifeStageAges[lifeStageAges.length - 1] * TICKS_PER_MINECRAFT_DAY;



    AgeLabel [] init1 = {AgeLabel.HATCHLING, AgeLabel.INFANT, AgeLabel.CHILD, AgeLabel.EARLYTEEN, AgeLabel.LATETEEN, AgeLabel.ADULT};
    ageLabels = init1;

    double [] init2 = {
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, BREATHMATURITY_HATCHLING),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, BREATHMATURITY_INFANT),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, BREATHMATURITY_CHILD),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, BREATHMATURITY_EARLY_TEEN),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, BREATHMATURITY_LATE_TEEN),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, BREATHMATURITY_ADULT)
            };
    breathMaturityPoints = init2;

    double [] init3 = {
            0.0,
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, PHYSICALMATURITY_INFANT),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, PHYSICALMATURITY_CHILD),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, PHYSICALMATURITY_EARLY_TEEN),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, PHYSICALMATURITY_LATE_TEEN),
            100.0
    };
    physicalMaturityPoints = init3;

    double [] init4 = {
            0.0,
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, EMOTIONALMATURITY_INFANT),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, EMOTIONALMATURITY_CHILD),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, EMOTIONALMATURITY_EARLY_TEEN),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, EMOTIONALMATURITY_LATE_TEEN),
            100.0
    };
    emotionalMaturityPoints = init4;

    double [] init5 = {
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, GROWTHRATE_HATCHLING),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, GROWTHRATE_INFANT),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, GROWTHRATE_CHILD),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, GROWTHRATE_EARLY_TEEN),
            (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, GROWTHRATE_LATE_TEEN),
            100.0
    };
    growthratePoints = init5;

    // The curve for physical size is produced by integrating the growth curve:
    // 1) Find the integral of growth rate.  This is the profile to grow from the initial size to the final size
    // 2) Renormalise the growth rate integral to match the units of size.
    //  eg if our growth rate is 100% for 1 day then 50% for 2 days, initial size is 1m and final size 2m:
    //  integral = 100%*1 + 50%*2 = 200%.days.  Growth = 2m - 1m = 1m
    //  So 200%.days is equivalent to 1m of growth, the normalisefactor is 1m / 200%.days = 0.005 m/%.days
    //  After the first day at 100% growth = 100%.days * 0.005 m/%.days = 0.5m + 1m initial size = 1.5m size.
    // The size is also clipped to the valid size range (can't go too small or too large)
    physicalSizePoints[0] = 0.0;
    double minIntegral = 0;
    double maxIntegral = 0;
    for (int i = 1; i < growthratePoints.length; ++i) {
      physicalSizePoints[i] = physicalSizePoints[0]
              + (lifeStageAges[i] - lifeStageAges[i-1]) * (growthratePoints[i] + growthratePoints[i-1]) / 2.0;
      minIntegral = Math.min(minIntegral, physicalSizePoints[i]);
      maxIntegral = Math.max(maxIntegral, physicalSizePoints[i]);
    }
    double sizeHatchling = (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, SIZE_HATCHLING);
    double sizeAdult = (double)dragonVariants.getValueOrDefault(DragonVariants.Category.LIFE_STAGE, SIZE_ADULT);
    double integral = physicalSizePoints[growthratePoints.length-1];

    final double SMALL_NON_ZERO = 1;
    double normaliseFactor = 0.0;
    if (   (sizeAdult >= sizeHatchling && integral < SMALL_NON_ZERO)
        || (sizeAdult < sizeHatchling && integral > -SMALL_NON_ZERO))  {
      configOK = false;
      configErrors += DragonVariants.Category.LIFE_STAGE.getTextName() + " growthrate curve can't be fitted to sizehatchling->sizeadult "
                                                                       + " (eg growthrates are positive but adult is smaller than hatchling)"  ;
    } else {
      normaliseFactor = (sizeAdult - sizeHatchling) / normaliseFactor;
      if (sizeHatchling + minIntegral * normaliseFactor < MINIMUM_SIZE) {
        normaliseFactor = 0.0;
        configOK = false;
        configErrors += DragonVariants.Category.LIFE_STAGE.getTextName() + " growthrate curve produces a dragon less than the minimum size of " + MINIMUM_SIZE;
      } else if (sizeHatchling + minIntegral * normaliseFactor > MAXIMUM_SIZE) {
        normaliseFactor = 0.0;
        configErrors += DragonVariants.Category.LIFE_STAGE.getTextName() + " growthrate curve produces a dragon bigger than the maximum size of " + MAXIMUM_SIZE;
      }
    }
    for (int i = 1; i < physicalSizePoints.length; ++i) {
      physicalSizePoints[i] = sizeHatchling + physicalSizePoints[i] * normaliseFactor;
    }
    maximumSizeAtAnyAge = sizeHatchling + maxIntegral * normaliseFactor;

    if (!configOK) throw new IllegalArgumentException(configErrors);
  }

}