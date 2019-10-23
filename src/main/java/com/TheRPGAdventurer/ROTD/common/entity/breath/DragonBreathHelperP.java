package com.TheRPGAdventurer.ROTD.common.entity.breath;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.render.dragon.breathweaponFX.BreathWeaponFXEmitter;
import com.TheRPGAdventurer.ROTD.client.sound.SoundController;
import com.TheRPGAdventurer.ROTD.client.sound.SoundEffectBreathWeaponP;
import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeFactory;
import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeP;
import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathProjectileFactory;
import com.TheRPGAdventurer.ROTD.common.entity.breath.weapons.*;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreed;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.helper.DragonHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by TGG on 8/07/2015.
 * Responsible for
 * - retrieving the player's selected target (based on player's input from Dragon Orb item)
 * - synchronising the player-selected target between server AI and client copy - using datawatcher
 * - rendering the breath weapon on the client
 * - performing the effects of the weapon on the server (eg burning blocks, causing damage)
 * The selection of an actual target (typically - based on the player desired target), navigation of dragon to the appropriate range,
 * turning the dragon to face the target, is done by targeting AI.
 * DragonBreathHelper is also responsible for
 * - tracking the current breath state (IDLE, STARTING, SUSTAINED BREATHING, STOPPING)
 * - sound effects
 * - adding delays for jaw open / breathing start
 * - interrupting the beam when the dragon is facing the wrong way / the angle of the beam mismatches the head angle
 * Usage:
 * 1) Create instance, providing the parent dragon entity
 * 2) call onLivingUpdate(), onDeath(), onDeathUpdate(), readFromNBT() and writeFromNBT() from the corresponding
 * parent entity methods
 * 3a) The AI task responsible for targeting should call getPlayerSelectedTarget() to find out what the player wants
 * the dragon to target.
 * 3b) Once the target is in range and the dragon is facing the correct direction, the AI should use setBreathingTarget()
 * to commence breathing at the target
 * 4) getCurrentBreathState() and getBreathStateFractionComplete() should be called by animation routines for
 * the dragon during breath weapon (eg jaw opening)
 */
public class DragonBreathHelperP extends DragonHelper {
  public enum BreathState {
    IDLE, STARTING, SUSTAIN, STOPPING
  }

  public DragonBreathHelperP(EntityTameableDragon dragon) {
    super(dragon);
    refreshBreed(dragon);
    setCompleted(FunctionTag.CONSTRUCTOR);
  }

  public static void registerConfigurationTags()
  {
    // the initialisation of the tags is all done in their static initialisers
    //    DragonVariants.addVariantTagValidator(new DragonReproductionValidator());
  }

  // changes the breath weapon after the breed is changed
  public void refreshBreed(EntityTameableDragon dragon) {
    DragonBreed newBreed = dragon.getBreed();
    if (currentBreed == newBreed) return;
    currentBreed = newBreed;

    switch (currentBreed.getBreathWeaponSpawnType(dragon)) {
      case NODES: {
        BreathWeaponP breathWeapon = currentBreed.getBreathWeapon(dragon);
        breathAffectedAreaP = new BreathAffectedArea(breathWeapon);
        breathNodeFactory = currentBreed.getBreathNodeFactory(dragon);
        if (dragon.isClient()) {
          breathWeaponFXEmitter = currentBreed.getBreathWeaponFXEmitter(dragon);
        }
        break;
      }
      case PROJECTILE: {
        breathProjectileFactory = currentBreed.getBreathProjectileFactory(dragon);
        break;
      }
      default: {
        DragonMounts.loggerLimit.error_once(
                "Unknown BreathWeaponSpawnType:" + dragon.getBreed().getBreathWeaponSpawnType(dragon));
        return;
      }
    }
    if (dragon.isClient()) {
      refreshBreedClientOnly(dragon);
    }

  }

  public void onConfigurationChange() {
    setCompleted(FunctionTag.ON_CONFIG_CHANGE);
    throw new NotImplementedException("onConfigurationChange()");
  }

  public void refreshBreedClientOnly(EntityTameableDragon dragon) {
    soundEffectBreathWeapon = dragon.getBreed().getSoundEffectBreathWeapon(getSoundController(dragon.getEntityWorld()), weaponInfoLink);
  }

  public BreathState getCurrentBreathState() {
    return currentBreathState;
  }


  /**
   * Returns true if the entity is breathing.
   */
  public boolean isUsingBreathWeapon() {
    BreathWeaponTarget breathWeaponTarget = this.breathweapon().getPlayerSelectedTarget();
    return (null != breathWeaponTarget);
  }

  public float getBreathStateFractionComplete() {
    switch (currentBreathState) {
      case IDLE: {
        return 0.0F;
      }
      case STARTING: {
        int ticksSpentStarting = tickCounter - transitionStartTick;
        return MathHelper.clamp(ticksSpentStarting / (float) BREATH_START_DURATION, 0.0F, 1.0F);
      }
      case SUSTAIN: {
        return 0.0F;
      }
      case STOPPING: {
        int ticksSpentStopping = tickCounter - transitionStartTick;
        return MathHelper.clamp(ticksSpentStopping / (float) BREATH_STOP_DURATION, 0.0F, 1.0F);
      }
      default: {
        DragonMounts.loggerLimit.error_once("Unknown currentBreathState:" + currentBreathState);
        return 0.0F;
      }
    }
  }

  /**
   * set the target currently being breathed at.
   * server only.
   *
   * @param target the new target the dragon is breathing at, null = no target
   */
  public void setBreathingTarget(BreathWeaponTarget target) {
    if (dragon.isServer()) {
      targetBeingBreathedAt = target;
      boolean updateDataWatcher = false;
      if (lastBreathTargetSent == null) {
        updateDataWatcher = true;
      } else {
        updateDataWatcher = !lastBreathTargetSent.approximatelyMatches(target);
      }
      if (updateDataWatcher) {
        lastBreathTargetSent = target;
        if (target == null) {
          entityDataManager.set(dataParamBreathWeaponTarget, "");
        } else {
          entityDataManager.set(dataParamBreathWeaponTarget, target.toEncodedString());
        }
      }
    } else {
      L.warn("setBreathingTarget is only valid on server");
    }

    updateBreathState(target);
  }

  /**
   * gets the target that the movement AI should move towards (or away from) to move to the optimal breathing distance
   */
  public BreathWeaponTarget getBreathTargetForMoving() {
    return breathWeaponTarget;
  }

  /**
   * sets the target that the movement AI should move towards (or away from) to move to the optimal breathing distance
   *
   * @param targetForMoving the new target - NULL for no target
   */
  public void setBreathTargetForMoving(BreathWeaponTarget targetForMoving) {
    breathWeaponTarget = targetForMoving;
  }

  /**
   * check if the dragon has a breath target that it should move towards (or away from)
   *
   * @return true if the dragon has a movement target
   */
  public boolean hasBreathTargetForMoving() {
    return breathWeaponTarget != null;
  }

  /**
   * Get the current mode of the breath weapon (only relevant for some breath weapon types)
   * 1) On the client, from the datawatcher
   * 2) On the server- previously set by others
   *
   * @return the current breath weapon mode
   */
  public DragonBreathMode getBreathMode() {
    if (dragon.isClient()) {
      return DragonBreathMode.createFromDataParameter(entityDataManager, dataParamBreathWeaponMode);
    } else {
      return breathWeaponMode;
    }
  }

  /**
   * set the breath weapon mode (only relevant for some breath weapon types)
   * server only.
   *
   * @param newMode - new breath weapon mode (meaning depends on breath weapon type)
   */

  public void setBreathMode(DragonBreathMode newMode) {
    if (dragon.isServer()) {
      breathWeaponMode = newMode;
      breathWeaponMode.writeToDataWatcher(entityDataManager, dataParamBreathWeaponMode);
    } else {
      L.warn("setBreathMode is only valid on server");
    }
  }

  /**
   * For tamed dragons, returns the target that their controlling player has selected using the DragonOrb or riding
   * while holding the breath key.
   *
   * @return the player's selected target, or null if no player target or dragon isn't tamed.
   */
  public BreathWeaponTarget getPlayerSelectedTarget() {
    Entity owner = dragon.getOwner();
    if (owner == null) {
      return null;
    }

    if (dragon.isClient()) {
      return getTarget();
    }

    EntityPlayerMP entityPlayerMP = (EntityPlayerMP) owner;
    BreathWeaponTarget breathWeaponTarget = DragonOrbTargets.getInstance().getPlayerTarget(entityPlayerMP);
    return breathWeaponTarget;
  }

  @Override
  public void onLivingUpdate() {
    checkPreConditions(FunctionTag.VANILLA);
    ++tickCounter;
    if (dragon.isClient()) {
      onLivingUpdateClient();
    } else {
      onLivingUpdateServer();
    }
  }

  @Override
  public void writeToNBT(NBTTagCompound nbt) {
    checkPreConditions(FunctionTag.WRITE_TO_NBT);
    setCompleted(FunctionTag.WRITE_TO_NBT);
  }

  @Override
  public void readFromNBT(NBTTagCompound nbt) {
    checkPreConditions(FunctionTag.READ_FROM_NBT);
    setCompleted(FunctionTag.READ_FROM_NBT);
  }

  @Override
  public void registerDataParameters() {
    checkPreConditions(FunctionTag.REGISTER_DATA_PARAMETERS);
    registerForInitialisation(DATA_BREATH_WEAPON_TARGET, "");  //default value
    registerForInitialisation(DATA_BREATH_WEAPON_MODE, 0);
    setCompleted(FunctionTag.REGISTER_DATA_PARAMETERS);
  }


  @Override
  public void registerEntityAttributes() {
    checkPreConditions(FunctionTag.REGISTER_ENTITY_ATTRIBUTES);
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

  }

  @Override
  public void onDeathUpdate() {
    checkPreConditions(FunctionTag.VANILLA);
  }

  @Override
  public void onDeath() {
    checkPreConditions(FunctionTag.VANILLA);
  }

  public SoundController getSoundController(World world) {
    if (!world.isRemote) {
      throw new IllegalArgumentException("getSoundController() only valid for WorldClient");
    }
    if (soundController == null) {
      soundController = new SoundController((WorldClient) world);
    }

    return soundController;
  }

  // Callback link to provide the Sound generator with state information
  public class WeaponInfoLink implements SoundEffectBreathWeaponP.WeaponSoundUpdateLink {

    @Override
    public boolean refreshWeaponSoundInfo(SoundEffectBreathWeaponP.WeaponSoundInfo infoToUpdate) {
      BreathWeaponTarget target = getTarget();
      Vec3d origin;
      origin = dragon.getAnimator().getThroatPosition();
      infoToUpdate.dragonHeadLocation = origin;
      infoToUpdate.relativeVolume = dragon.getAgeScale();
      infoToUpdate.lifeStage = dragon.lifeStage().getLifeStage();

      boolean isBreathing = false;
      if (target != null) {
        Vec3d destination = target.getTargetedPoint(dragon.world, origin);
        if (destination != null && currentBreathState == BreathState.SUSTAIN) {
          isBreathing = true;
        }
      }
      infoToUpdate.breathingState = isBreathing ? SoundEffectBreathWeaponP.WeaponSoundInfo.State.BREATHING
              : SoundEffectBreathWeaponP.WeaponSoundInfo.State.IDLE;

      return true;
    }
  }

//  // Callback link to provide the Sound generator with state information
//  @Deprecated
//  public class WeaponInfoLinkLegacy implements SoundEffectBreathWeapon.WeaponSoundUpdateLinkLegacy {
//
//    @Override
//    public boolean refreshWeaponSoundInfo(SoundEffectBreathWeapon.WeaponSoundInfo infoToUpdate) {
//      Vec3d origin = dragon.getAnimator().getThroatPosition();
//      infoToUpdate.dragonHeadLocation = origin;
//      infoToUpdate.relativeVolume = dragon.getAgeScale();
//      infoToUpdate.lifeStage = dragon.lifeStage().getLifeStage();
//
//      boolean isUsingBreathweapon = false;
//      if (dragon.isUsingBreathWeapon()) {
//        Vec3d lookDirection = dragon.getLook(1.0f);
//        Vec3d endOfLook = origin.addVector(lookDirection.x, lookDirection.y, lookDirection.z);
//        if (endOfLook != null && currentBreathState == BreathState.SUSTAIN && dragon.getBreed().canUseBreathWeapon()) {
//          isUsingBreathweapon = true;
//        }
//      }
//
//      infoToUpdate.breathingState = isUsingBreathweapon ? SoundEffectBreathWeapon.WeaponSoundInfo.State.BREATHING : SoundEffectBreathWeapon.WeaponSoundInfo.State.IDLE;
//      return true;
//    }
//  }
//  @Deprecated
//  protected BreathWeaponEmitter breathWeaponEmitter = null;

  private void updateBreathState(BreathWeaponTarget targetBeingBreathedAt) {
    if (targetBeingBreathedAt == null) {
      playerHasReleasedTargetSinceLastBreath = true;
    }
    switch (currentBreathState) {
      case IDLE: {
        if (targetBeingBreathedAt != null && playerHasReleasedTargetSinceLastBreath) {
          transitionStartTick = tickCounter;
          currentBreathState = BreathState.STARTING;
          playerHasReleasedTargetSinceLastBreath = false;
        }
        break;
      }
      case STARTING: {
        int ticksSpentStarting = tickCounter - transitionStartTick;
        if (ticksSpentStarting >= BREATH_START_DURATION) {
          transitionStartTick = tickCounter;
          currentBreathState = (targetBeingBreathedAt != null) ? BreathState.SUSTAIN : BreathState.STOPPING;
        }
        break;
      }
      case SUSTAIN: {
        if (targetBeingBreathedAt == null) {
          forceStop();
        }
        break;
      }
      case STOPPING: {
        int ticksSpentStopping = tickCounter - transitionStartTick;
        if (ticksSpentStopping >= BREATH_STOP_DURATION) {
          currentBreathState = BreathState.IDLE;
        }
        break;
      }
      default: {
        DragonMounts.loggerLimit.error_once("Unknown currentBreathState:" + currentBreathState);
        return;
      }
    }
  }

  private void forceStop() {
    transitionStartTick = tickCounter;
    currentBreathState = BreathState.STOPPING;
  }

  private void onLivingUpdateServer() {
    refreshBreed(dragon);
    BreathWeaponTarget target = getTarget();
    updateBreathState(target);
//    if (!DragonMounts.instance.getConfig().isPrototypeBreathweapons()
//            || isLegacyBreath(target)) {
//      onLivingUpdateServerLegacy();
//      return;
//    }

    dragon.getBreed().getBreathWeapon(dragon).updateBreathWeaponMode();
    DragonBreathMode dragonBreathMode = dragon.breathweapon().getBreathMode();

    switch (dragon.getBreed().getBreathWeaponSpawnType(dragon)) {
      case NODES: {
        if (target != null) {
          Vec3d origin = dragon.getAnimator().getThroatPosition();
          Vec3d destination = target.getTargetedPoint(dragon.world, origin);
          if (destination != null && currentBreathState == BreathState.SUSTAIN) {
            BreathNodeP.Power power = dragon.lifeStage().getBreathPowerP();
            breathAffectedAreaP.continueBreathing(dragon.getEntityWorld(), origin, destination, breathNodeFactory, power, dragonBreathMode);
          }
        }
        breathAffectedAreaP.updateTick(dragon.world, dragonBreathMode);
        break;
      }
      case PROJECTILE: {
        if (target != null) {
          Vec3d origin = dragon.getAnimator().getThroatPosition();
          Vec3d destination = target.getTargetedPoint(dragon.world, origin);
          if (destination != null && currentBreathState == BreathState.SUSTAIN) {
            BreathNodeP.Power power = dragon.lifeStage().getBreathPowerP();
            boolean spawned = breathProjectileFactory.spawnProjectile(dragon.getEntityWorld(), dragon,  // may not spawn anything if a projectile was spawned recently...
                    origin, destination, power);
            if (spawned) {
              forceStop();
            }
          }
        }
        breathProjectileFactory.updateTick(currentBreathState);
        break;
      }
      default: {
        DragonMounts.loggerLimit.error_once(
                "Unknown BreathWeaponSpawnType:" + dragon.getBreed().getBreathWeaponSpawnType(dragon));
        return;
      }
    }
  }

//  @Deprecated
//  private void onLivingUpdateServerLegacy() {
////        updateBreathState(dragon.isUsingBreathWeapon());
//    if (dragon.isUsingBreathWeapon()) {
//      Vec3d origin = dragon.getAnimator().getThroatPosition();
//      Vec3d lookDirection = dragon.getLook(1.0f);
//      Vec3d endOfLook = origin.addVector(lookDirection.x, lookDirection.y, lookDirection.z);
//      BreathNodeP.Power power = dragon.lifeStage().getBreathPowerP();
//      if (endOfLook != null && currentBreathState == BreathState.SUSTAIN) {
//        dragon.getBreed().continueAndUpdateBreathingLegacy(dragon.getEntityWorld(), origin, endOfLook, power, dragon);
//      }
//    }
//  }

  private void onLivingUpdateClient() {
    refreshBreed(dragon);
    BreathWeaponTarget target = getTarget();
    updateBreathState(target);

//    if (!DragonMounts.instance.getConfig().isPrototypeBreathweapons()
//            || isLegacyBreath(target)) {
//      onLivingUpdateClientLegacy();
//      return;
//    }

    switch (dragon.getBreed().getBreathWeaponSpawnType(dragon)) {
      case NODES: {
        DragonBreathMode dragonBreathMode = getBreathMode();
        breathWeaponFXEmitter.changeBreathMode(dragonBreathMode);

        if (target != null) {
          Vec3d origin = dragon.getAnimator().getThroatPosition();
          Vec3d destination = target.getTargetedPoint(dragon.world, origin);
          if (destination != null && currentBreathState == BreathState.SUSTAIN) {
            breathWeaponFXEmitter.setBeamEndpoints(origin, destination);
            BreathNodeP.Power power = dragon.lifeStage().getBreathPowerP();
            breathWeaponFXEmitter.spawnBreathParticles(dragon.getEntityWorld(), power, tickCounter);
          }
        }
        break;
      }
      case PROJECTILE: {
        //nothing to do client side for projectiles; they are normal entities
        // just animate the mouth closed once sustain is reached
        final int SUSTAIN_VISUAL_DELAY = 8;
        if (currentBreathState == BreathState.SUSTAIN
                && tickCounter > transitionStartTick + SUSTAIN_VISUAL_DELAY) {
          forceStop();
        }
        break;
      }
      default: {
        DragonMounts.loggerLimit.error_once(
                "Unknown BreathWeaponSpawnType:" + dragon.getBreed().getBreathWeaponSpawnType(dragon));
        return;
      }
    }

    soundEffectBreathWeapon.performTick(Minecraft.getMinecraft().player);
  }

//  @Deprecated
//  private void onLivingUpdateClientLegacy() {
//    if (dragon.isUsingBreathWeapon()) {
//      Vec3d origin = dragon.getAnimator().getThroatPosition();
//      final double LEGACY_Y_OFFSET = -0.4;  // adjusts to account for the different x,y,z of the legacy breath
//
//      origin = origin.addVector(0.0, LEGACY_Y_OFFSET, 0.0);
//
//      Vec3d lookDirection = dragon.getLook(1.0f);
//      Vec3d endOfLook = origin.addVector(lookDirection.x, lookDirection.y, lookDirection.z);
//      if (endOfLook != null && currentBreathState == BreathState.SUSTAIN && dragon.getBreed().canUseBreathWeapon()) {
//
//        BreathNodeP.Power power = dragon.lifeStage().getBreathPowerP();
//        dragon.getBreed().spawnBreathParticles(dragon.getEntityWorld(), power, tickCounter, origin, endOfLook, dragon);
//      }
//    }
//
//    if (soundEffectBreathWeaponLegacy == null) {
//      soundEffectBreathWeaponLegacy = new SoundEffectBreathWeapon(getSoundController(dragon.getEntityWorld()), weaponInfoLinkLegacy);
//    }
//    soundEffectBreathWeaponLegacy.performTick(Minecraft.getMinecraft().player, dragon);
//  }

  /**
   * Get the target currently being breathed at, for this dragon:
   * 1) On the client, from the datawatcher
   * 2) On the server- previously set by AI
   *
   * @return the target, or null for none
   */
  private BreathWeaponTarget getTarget() {
    if (dragon.isClient()) {
      String targetString = entityDataManager.get(DATA_BREATH_WEAPON_TARGET);
      BreathWeaponTarget target = BreathWeaponTarget.fromEncodedString(targetString);
      return target;
    } else {
      return targetBeingBreathedAt;
    }
  }

//  /**
//   * Should we use the legacy breath for this dragon?
//   *
//   * @param newBreathWeaponTarget
//   * @return true for legacy
//   */
//  private boolean isLegacyBreath(BreathWeaponTarget newBreathWeaponTarget) {
//    if (newBreathWeaponTarget != null) {
//      useLegacy = newBreathWeaponTarget.getWeaponUsed() == BreathWeaponTarget.WeaponUsed.PRIMARY;
//    }
//    return useLegacy;
//  }
  private static final Logger L = LogManager.getLogger();
  //  private final int DATA_WATCHER_BREATH_TARGET;
//  private final int DATA_WATCHER_BREATH_MODE;
  private final int BREATH_START_DURATION = 5; // ticks
  private final int BREATH_STOP_DURATION = 5; // ticks
//  private DataParameter<String> dataParamBreathWeaponTarget;
//  private DataParameter<Integer> dataParamBreathWeaponMode;
  private SoundController soundController;
  private SoundEffectBreathWeaponP soundEffectBreathWeapon;
//  @Deprecated
//  private SoundEffectBreathWeapon soundEffectBreathWeaponLegacy;
  private WeaponInfoLink weaponInfoLink = new WeaponInfoLink();
//  private WeaponInfoLinkLegacy weaponInfoLinkLegacy = new WeaponInfoLinkLegacy();
  private BreathWeaponTarget targetBeingBreathedAt = null;  // server: the target currently being breathed at
  private BreathWeaponTarget lastBreathTargetSent = null;   // server: the last target sent to the client thru DataWatcher
  private BreathState currentBreathState = BreathState.IDLE;
  private int transitionStartTick;
  private BreathWeaponFXEmitter breathWeaponFXEmitter = null;
  private int tickCounter = 0;
  private BreathWeaponTarget breathWeaponTarget;
  private boolean playerHasReleasedTargetSinceLastBreath = false;
  private BreathAffectedArea breathAffectedAreaP;
  private DragonBreed currentBreed = null;
  private BreathProjectileFactory breathProjectileFactory = null;
  private BreathNodeFactory breathNodeFactory = null;
  private DragonBreathMode breathWeaponMode = DragonBreathMode.DEFAULT;
//  @Deprecated
//  private BreathAffectedArea breathAffectedAreaFire = null;
//  private boolean useLegacy = true; //todo for debugging: was the last weapon used the primary (== legacy)

  private static final DataParameter<String> DATA_BREATH_WEAPON_TARGET = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.STRING);
  private static final DataParameter<Integer> DATA_BREATH_WEAPON_MODE = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.VARINT);


}
