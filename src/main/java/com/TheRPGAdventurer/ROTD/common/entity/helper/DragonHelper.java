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
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.Modifiers;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.DifficultyInstance;

import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 * @author TGG
 *
 *   Usage:
 *   1) Implement the vanilla methods as appropriate
 *   2a) Add a static method public static void registerConfigurationTags() to register any configuration tags used by this helper
 *   2b) In EntityTameableDragon.registerConfigurationTags(), add a call to
 *          DragonHelperMyNewClass.registerConfigurationTags()
 *   3) Implement the remaining methods to initialise the helper correctly:
 *    The initialisation sequence for the helper is:
 *    a) new DragonHelper(EntityTameableDragon)
 *    b) registerDataParameters() = register DataParameters; you should also add them to the initialisedDataParameters HashMap
 *    c) registerEntityAttributes - register IAttributes here.  Don't set the base values yet, this is deferred to initialiseServerSide below, once all helpers are initialised.
 *
 *    d1) server side only: vanilla will set the initial configuration either using a non-default constructor or
 *         readEntityFromNBT (when loading from disk)
 *        if necessary for newly spawned entity, define a setInitialConfiguration() add a call to it into the
 *           EntityTameableDragon explicit constructor.
 *    d2) server side only: initialiseServerSide to initialise all local data structures and Attributes from the DataParameters
 *    d3) server side only: onInitialSpawn() - for freshly spawned creatures only (vanilla does this in ItemMonsterPlacer).  Used to randomly vary attributes.
 *
 *    e1) client side only: notifyDataManager is received for all DataParameters
 *    e2) client side only: when all dataParameters have been received, allDataParametersReceived should return true.
 *    e3) client side only: initialiseClientSide to initialise all local data structures and Attributes from the DataParameters
 *
 *    f) both sides: onConfigurationChange()
 *   4) If the helper depends on the dragon configuration (breed, Modifiers) then implement onConfigurationChange()
 *
 *   To help detect program bugs early, several helper functions are provided:
 *   checkPreConditions
 *   setCompleted
 *   allDataParametersReceived
 *   markDataParameterAsInitialised
 *
 *   They are used as follows:
 *
 *   DragonHelperChild.registerDataParameters() {
 *     checkPreConditions(FunctionTag.REGISTER_DATA_PARAMETERS);
 *        // do stuff here
 *     setCompleted(FunctionTag.REGISTER_DATA_PARAMETERS);
 *   }
 *
 *   likewise for the other functions implemented by the DragonHelper.
 *
 *   markDataParameterAsInitialised(DataParameter) is used to notify when each data parameter has been received, so that
 *     the helper state can be updated when ready.
 *
 * The sequence of states is:
 * Server side:
 *   INITIAL
 *   CONSTRUCTED
 *   ENTITY_INIT_DONE (after registerDataParameters() has been called; while in this state we are still assembling config info (registerEntityAttributes, initialSpawn)
 *   SERVER_DATA_COMPLETE (after readFromNBT() has been called, or the relevant parameters have been explicitly provided)
 *   INITIALISED (after initialiseServerSide() has been called)
 *
 * Client side:
 *   INITIAL
 *   CONSTRUCTED
 *   ENTITY_INIT_DONE (after registerDataParameters() has been called).  Assembling config info from DataParameters.
 *   CLIENT_DATA_COMPLETE (after markDataParameterAsInitialised has been called for all DataParameters)
 *   INITIALISED (after initialiseClientSide() has been called)
 *
 */
public abstract class DragonHelper {

  public DragonHelper(EntityTameableDragon dragon) {
    this.dragon = dragon;
    this.entityDataManager = dragon.getDataManager();
    this.rand = dragon.getRNG();
  }

  public abstract void writeToNBT(NBTTagCompound nbt);
  public abstract void readFromNBT(NBTTagCompound nbt);

  /**
   * Register all DataParameter used by the helper in here
   */
  public abstract void registerDataParameters();

  /**
   * Register new IAttributes in here.  Don't set base values yet, these go in initialiseServerSide, initialiseClientSide.
    */
  public void registerEntityAttributes() {
  }

  /** called to inform the helper that the entity is being explicitly constructed (i.e. not loaded from NBT).
   * Later, once all helpers are initialised, onInitialSpawn() will also be called.
   * This function is a default in case the helper doesn't need an explicit setInitialConfiguration
   */
  public void onExplicitConstruction() {
    checkPreConditions(FunctionTag.SET_INITIAL_CONFIGURATION);
    setCompleted(FunctionTag.SET_INITIAL_CONFIGURATION);
  }

    /**
     * Initialise the helper - server side - called once all other helpers have loaded their configuration data (NBT etc),
     *   but not guaranteed that all other helpers will be fully initialised.
     */
  public abstract void initialiseServerSide();

  /**
   * Called once when the entity is first spawned (and not when it's later loaded from disk)
   * Is called after initialiseServerSide
   * @param difficulty
   * @param livingdata
   * @return
   */
  @Nullable
  public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata) {
    checkPreConditions(FunctionTag.ON_INITIAL_SPAWN);
    setCompleted(FunctionTag.ON_INITIAL_SPAWN);
    return livingdata;
  }

  /**
   * Initialise the helper - client side - called once all other helpers have received all their DataParameter information
   *   but not guaranteed that all other helpers will be fully initialised.
   */
  public abstract void initialiseClientSide();

  /**
   * Notify that the dragon configuration (breed and/or modifiers) has changed, i.e.
   * Called at the end of initialisation and then again whenever the breed/and or modifiers have been changed to new values
   *   (different from previous value)
   * Be careful not to trigger a recursive update (eg changing the modifier causes variants to change, which causes the modifier to change back, etc)
   */
  public void onConfigurationChange() {
    checkPreConditions(FunctionTag.ON_CONFIG_CHANGE);
  }

  /** Called when a DataParameter has been received for this helper after initialisation has been completed
   *
   * @param key
   */
  protected void notifyDataManagerChange(DataParameter<?> key) {
  }

  public void onLivingUpdate() {
    checkPreConditions(FunctionTag.VANILLA);
  }

  public void onDeathUpdate() {
    checkPreConditions(FunctionTag.VANILLA);
  }

  public void onDeath() {
    checkPreConditions(FunctionTag.VANILLA);
  }


  /** Call this when a DataParameter has been received.
   * 1) checks whether it is relevant to this helper.  If so;
   * 2) If the helper isn't initialised yet, mark this dataparameter as received.
   *    Otherwise, call notifyDataManagerChange to inform the helper of the change-after-initialisation
   *
   * @param key
   */
  public final void notifyDataManagerReceived(DataParameter<?> key) {
    boolean shouldProcessFurther = notifyDataManagerChangeCommon(key);
    if (!shouldProcessFurther) return;
    notifyDataManagerChange(key);
  }


  /** Should return true once the client side has received all the DataParameter it expects.
   * @return true once all expected DataParameter have been received
   */
  public boolean allDataParametersReceived() {
    checkState(dragon.isClient(), "DragonHelper.allDataParametersReceived() was called from non-Client code");

    boolean retval = helperState == HelperState.CLIENT_DATA_COMPLETE || helperState == HelperState.INITIALISED;
    return retval;
  }

  // for testing only
  protected DragonHelper() {
    dragon = null;
    entityDataManager = null;
    rand = null;
  }

  protected final EntityTameableDragon dragon;
  protected final EntityDataManager entityDataManager;
  protected final Random rand;

  /**
   * Register a DataParameter and add it to the helper's list of DataParameters which require initialisation
   *   (i.e. the helper won't proceed to INITIALISE_CLIENT until all the expected DataParameters have been received
   * @param dataParameter
   * @param value
   * @param <T>
   */
  protected <T> void registerForInitialisation(DataParameter<T> dataParameter, T value) {
    entityDataManager.register(dataParameter, value);
    intialisedDataParameters.put(dataParameter, Boolean.FALSE);
  }

  /**
   * Perform initial checks on the received key, and mark as received (for initialisation purposes).
   * @param key
   * @return true if the caller should process the key further (it's valid for this helper, and the helper is
   *         fully initialised.)
   */
  protected boolean notifyDataManagerChangeCommon(DataParameter<?> key) {
    if (!intialisedDataParameters.containsKey(key)) return false;
    // if not initialised, mark as received (once all are received, initialisation will be performed via initialiseClientSide)
    if (helperState == HelperState.INITIALISED) return true;

    checkPreConditions(FunctionTag.DATAPARAMETER_RECEIVED);
    markDataParameterAsInitialised(key);
    setCompleted(FunctionTag.DATAPARAMETER_RECEIVED);
    return false;
  }

  /**
   *
   * @param dataParameter
   */
  protected void markDataParameterAsInitialised(DataParameter dataParameter) {
    checkArgument(intialisedDataParameters.containsKey(dataParameter),
            "DragonHelper.markDataParameterAsInitialised was called for DataParameter %s which was not expected", dataParameter.getId());
    if (helperState == HelperState.CLIENT_DATA_COMPLETE || helperState == HelperState.INITIALISED) return;
    intialisedDataParameters.put(dataParameter, Boolean.TRUE);
    if (intialisedDataParameters.values().contains(Boolean.FALSE)) return;

    helperState = HelperState.CLIENT_DATA_COMPLETE;
  }

  protected void checkPreConditions(FunctionTag whichFunction) {
    switch (whichFunction) {
      case CONSTRUCTOR: {
        checkState(helperState == HelperState.INITIAL, "Unexpected helperState when calling DragonHelper constructor:%s", helperState);
        break;
      }
      case REGISTER_DATA_PARAMETERS: {
        checkState(helperState == HelperState.CONSTRUCTED, "Unexpected helperState when calling DragonHelper.registerDataParameters():%s", helperState);
        break;
      }
      case ON_INITIAL_SPAWN: {
        checkState(dragon.isServer(), "DragonHelper.onInitialSpawn() was called from non-Server code");
        checkState(helperState == HelperState.ENTITY_INIT_DONE, "Unexpected helperState when calling DragonHelper.onInitialSpawn():%s", helperState);
        break;
      }
      case REGISTER_ENTITY_ATTRIBUTES: {
        checkState(helperState == HelperState.ENTITY_INIT_DONE, "Unexpected helperState when calling DragonHelper.registerEntityAttributes():%s", helperState);
        break;
      }
      case DATAPARAMETER_RECEIVED: {
        checkState(dragon.isClient(), "DragonHelper.markDataParameterAsInitialised() was called from non-Client code");
        checkState(helperState == HelperState.ENTITY_INIT_DONE, "Unexpected helperState when calling DragonHelper.markDataParameterAsInitialised():%s", helperState);
        break;
      }
      case READ_FROM_NBT: {
        checkState(dragon.isServer(), "DragonHelper.readFromNBT() was called from non-Server code");
        checkState(helperState == HelperState.ENTITY_INIT_DONE  || helperState == HelperState.INITIALISED,
                // sometimes vanilla does WRITE_NBT, add a name tag, then READ_NBT again, before spawning
                "Unexpected helperState when calling DragonHelper.onConfigurationChange():%s", helperState);
        break;
      }
      case SET_INITIAL_CONFIGURATION: {
        checkState(dragon.isServer(), "DragonHelper.setInitialConfiguration() was called from non-Server code");
        checkState(helperState == HelperState.ENTITY_INIT_DONE, "Unexpected helperState when calling DragonHelper.setInitialConfiguration():%s", helperState);
        break;
      }
      case INITIALISE_CLIENT: {
        checkState(dragon.isClient(), "DragonHelper.initialiseClientSide() was called from non-Client code");
        checkState(helperState == HelperState.CLIENT_DATA_COMPLETE, "Unexpected helperState when calling DragonHelper.initialiseClientSide():%s", helperState);
        break;
      }
      case INITIALISE_SERVER: {
        checkState(dragon.isServer(), "DragonHelper.initialiseServerSide() was called from non-Server code");
        checkState(helperState == HelperState.SERVER_DATA_COMPLETE, "Unexpected helperState when calling DragonHelper.initialiseServerSide():%s", helperState);
        break;
      }
      case ON_CONFIG_CHANGE: {
        checkState(helperState == HelperState.INITIALISED, "Unexpected helperState when calling DragonHelper.onConfigurationChange():%s", helperState);
        break;
      }
      case VANILLA: {
        checkState(helperState == HelperState.INITIALISED, "Unexpected helperState when calling DragonHelper.{vanilla method}:%s", helperState);
        break;
      }
      case WRITE_TO_NBT: {
        checkState(dragon.isServer(), "DragonHelper.writeToNBT() was called from non-Server code");
        checkState(helperState == HelperState.INITIALISED, "Unexpected helperState when calling DragonHelper.writeToNBT():%s", helperState);
        break;
      }
      case CHANGE_CONFIGURATION: {
        checkState(dragon.isServer(), "DragonHelper.changeConfiguration() was called from non-Server code");
        checkState(helperState == HelperState.INITIALISED, "Unexpected helperState when calling DragonHelper.changeConfiguration():%s", helperState);
        break;
      }
      default: {
        throw new IllegalArgumentException(String.format("Unexpected FunctionTag in DragonHelper.checkPreConditions:%s", whichFunction));
      }
    }
  }

  protected void setCompleted(FunctionTag whichFunction) {
    switch (whichFunction) {
      case CONSTRUCTOR: {
        helperState = HelperState.CONSTRUCTED;
        break;
      }
      case REGISTER_DATA_PARAMETERS: {
        helperState = HelperState.ENTITY_INIT_DONE;
        break;
      }
      case DATAPARAMETER_RECEIVED: {
        // do nothing: the state is updated in markDataParameterAsInitialised()
        break;
      }
      case INITIALISE_CLIENT: {
        helperState = HelperState.INITIALISED;
        break;
      }
      case INITIALISE_SERVER: {
        helperState = HelperState.INITIALISED;
        break;
      }
      case ON_INITIAL_SPAWN:
      case REGISTER_ENTITY_ATTRIBUTES:
      case ON_CONFIG_CHANGE:  // do nothing
      case VANILLA:
      case WRITE_TO_NBT:
      case CHANGE_CONFIGURATION: {
        break;
      }
      case READ_FROM_NBT: {
        helperState = HelperState.SERVER_DATA_COMPLETE;
        break;
      }
      case SET_INITIAL_CONFIGURATION: {
        helperState = HelperState.SERVER_DATA_COMPLETE;
        break;
      }
      default: {
        throw new IllegalArgumentException(String.format("Unexpected FunctionTag in DragonHelper.checkPreConditions:%s", whichFunction));
      }
    }
  }

  protected enum HelperState {INITIAL, CONSTRUCTED, ENTITY_INIT_DONE, SERVER_DATA_COMPLETE, CLIENT_DATA_COMPLETE, INITIALISED}
  protected HelperState helperState = HelperState.INITIAL;
  protected enum FunctionTag {CONSTRUCTOR, REGISTER_DATA_PARAMETERS, REGISTER_ENTITY_ATTRIBUTES, ON_INITIAL_SPAWN,
                              SET_INITIAL_CONFIGURATION, READ_FROM_NBT, DATAPARAMETER_RECEIVED,
                              INITIALISE_CLIENT, INITIALISE_SERVER, ON_CONFIG_CHANGE,
                              VANILLA, WRITE_TO_NBT, CHANGE_CONFIGURATION}
  protected Map<DataParameter, Boolean> intialisedDataParameters = new HashMap<>();

  /**
   * Needed to ensure that DragonHelpers are iterated in well-defined order (important for DataParameter registration)
   */
  public static class DragonHelperSorter implements Comparator<Class> {

    public DragonHelperSorter() { //todo need to test that this works correctly
    }

    @Override
    public int compare(Class o1, Class o2) {
      if (o1 == o2) return 0;
      int cmp = o1.getCanonicalName().compareTo(o2.getCanonicalName());
      return cmp;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null) return false;
      return 0 == compare(this.getClass(), o.getClass());
    }
  }

}
