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

import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.EntityDataManager;

import java.util.Comparator;
import java.util.Random;

/**
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 *
 *   Usage:
 *   1) Implement the vanilla methods as appropriate
 *   2a) Add a static method public static void registerConfigurationTags() to register any configuration tags used by this helper
 *   2b) Add DragonHelperMyNewClass.registerConfigurationTags() to EntityTameableDragon.registerConfigurationTags()
 *   3) Implement the remaining methods to initialise the helper correctly:
 *    The initialisation sequence for the helper is:
 *    a) new DragonHelper(EntityTameableDragon)
 *    b) entityInit() = register DataParameters
 *    c1) server side only: readEntityFromNBT
 *    c2) server side only: initialiseServerSide
 *    d1) client side only: notifyDataManager is received for all DataParameters
 *    d2) client side only: when all dataParameters have been received, allDataParametersReceived should return true.
 *    d3) client side only: initialiseClientSide
 *    e) both sides: onConfigurationChange()
 *   4) If the helper depends on the dragon configuration (breed, Modifiers) then implement onConfigurationChange()
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
  public abstract void entityInit();

  /**
   * Initialise the helper - server side - called once all other helpers have loaded their configuration data (NBT),
   *   but not guaranteed that all other helpers will be fully initialised.
   */
  public abstract void initialiseServerSide();

  /**
   * Initialise the helper - client side - called once all other helpers have received all their DataParameter
   *   but not guaranteed that all other helpers will be fully initialised.
   */
  public abstract void initialiseClientSide();

  /** Should return true once the client side has received all the DataParameter it expects.
   * @return true once all expected DataParameter have been received
   */
  public boolean allDataParametersReceived() {
    boolean retval = helperState == HelperState.NBT_HAS_BEEN_READ
                  || helperState == HelperState.ALL_DATAPARAMETERS_RECEIVED
                  || helperState == HelperState.INITIALISED;
    return retval;
  }

  /**
   * Notify that the dragon configuration (breed and/or modifiers) has changed, i.e.
   * Called at the end of initialisation and then again whenever the breed/and or modifiers have been changed to new values
   *   (different from previous value)
   */
  public void onConfigurationChange() {
  }

  public void applyEntityAttributes() {
  }

  public void onLivingUpdate() {
  }

  public void onDeathUpdate() {
  }

  public void onDeath() {
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

  protected enum HelperState {INITIAL, ENTITY_INIT_DONE, NBT_HAS_BEEN_READ, ALL_DATAPARAMETERS_RECEIVED, INITIALISED}
  protected HelperState helperState = HelperState.INITIAL;
}
