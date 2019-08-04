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
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import com.google.common.collect.ImmutableMap;
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

import java.util.Map;

import static net.minecraft.entity.SharedMonsterAttributes.*;

/**
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 *
 * DragonLifeStageHelper is responsible for keeping track of the dragon's age and maturity
 *
 * The age of the dragon affects the following aspects:
 * 1) PhysicalSize (metres) - for the base dragon, this is the height of the top of the back
 * 2) PhysicalMaturity (0->100%) - the physical abilities of the dragon such as being able to fly
 * 3) EmotionalMaturity (0->100%) - the behaviour of the dragon eg sticking close to parent, running away from mobs
 * 4) BreathWeaponMaturity (0->100%) - the strength of the breath weapon
 *
 * It uses a number of DragonVariantTags to customise the effects of age on each of these
 *
 * Usage:
 * 1) During initial setup, call DragonLifeStageHelper.registerConfigurationTags() to register all the tags that are
 *    used to configure this
 * 2) Create the DragonLifeStageHelper
 * 3) Call its update methods to keep it synchronised as described in DragonHelper
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

    this.dataParam = dataParam;
    dataWatcher.register(dataParam, ticksSinceCreationServer);

    if (dragon.isClient()) {
      ticksSinceCreationClient = new ClientServerSynchronisedTickCount(TICKS_SINCE_CREATION_UPDATE_INTERVAL);
      ticksSinceCreationClient.reset(ticksSinceCreationServer);
    } else {
      ticksSinceCreationClient = null;
    }
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

  /**
   * Returns the size multiplier for the current age.
   *
   * @return size
   */
  public float getAgeScale() {
    return DragonLifeStage.getAgeScaleFromTickCount(getTicksSinceCreation());
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
  private final DragonVariantTag GROWTHRATE_HATCHLING = DragonVariantTag.addTag("growthratehatchling", 10.0);   // relative growth rate
  private final DragonVariantTag GROWTHRATE_CHILD = DragonVariantTag.addTag("growthratechild",  100.0);
  private final DragonVariantTag GROWTHRATE_LATE_TEEN = DragonVariantTag.addTag("growthratelateteen", 400.0);
  private final DragonVariantTag SIZE_HATCHLING = DragonVariantTag.addTag("sizehatchling", 0.1);  // height of back in m
  private final DragonVariantTag SIZE_ADULT = DragonVariantTag.addTag("sizeadult", 2.0);          // height of back in m

  // physical maturity = for physical abilities such as flying; 0% (hatchling) - 100% (adult)
  private final DragonVariantTag PHYSICALMATURITY_INFANT = DragonVariantTag.addTag("physicalmaturityinfant", 10.0);
  private final DragonVariantTag PHYSICALMATURITY_CHILD = DragonVariantTag.addTag("physicalmaturitychild", 30.0);
  private final DragonVariantTag PHYSICALMATURITY_EARLY_TEEN = DragonVariantTag.addTag("physicalmaturityearlyteen", 50.0);
  private final DragonVariantTag PHYSICALMATURITY_LATE_TEEN = DragonVariantTag.addTag("physicalmaturitylateteen", 80.0);

  // emotional maturity = for behaviour such as seeking out parents, danger aversion, etc
  private final DragonVariantTag EMOTIONALMATURITY_INFANT = DragonVariantTag.addTag("emotionalmaturityinfant", 0.0);
  private final DragonVariantTag EMOTIONALMATURITY_CHILD = DragonVariantTag.addTag("emotionalmaturitychild", 30.0);
  private final DragonVariantTag EMOTIONALMATURITY_EARLY_TEEN = DragonVariantTag.addTag("emotionalmaturityearlyteen", 30.0);
  private final DragonVariantTag EMOTIONALMATURITY_LATE_TEEN = DragonVariantTag.addTag("emotionalmaturitylateteen", 75.0);

  // breathweapon maturity 0% (hatchling) - 100% (adult)
  private final DragonVariantTag BREATHMATURITY_INFANT = DragonVariantTag.addTag("breathmaturityinfant", 0.0);
  private final DragonVariantTag BREATHMATURITY_CHILD = DragonVariantTag.addTag("breathmaturitychild", 0.0);
  private final DragonVariantTag BREATHMATURITY_EARLY_TEEN = DragonVariantTag.addTag("breathmaturityearlyteen", 25.0);
  private final DragonVariantTag BREATHMATURITY_LATE_TEEN = DragonVariantTag.addTag("breathmaturitylateteen", 75.0);


  /**
   * Read the configuration parameters from the DragonVariants config file, validate them, and convert them into
   *   internal structures
   * @param dragonVariants
   */
  private void readConfiguration(DragonVariants dragonVariants) {

  }

  ordinal


  Integral (%.day)
  Hatchling	20
  Infant	165
  Child	700
  EarlyTeen	600
  LateTeen	450
  TOTAL	1935	%.day

  Net size increase:	1.9	m
  ScalingFactor:	0.000981912	m/%.day


}