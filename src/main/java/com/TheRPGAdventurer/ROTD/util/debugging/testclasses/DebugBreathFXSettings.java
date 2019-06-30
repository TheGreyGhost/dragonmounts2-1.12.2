package com.TheRPGAdventurer.ROTD.util.debugging.testclasses;

/**
 * Created by TGG on 2/03/2017.
 * used for debugging the breath effects
 */
public class DebugBreathFXSettings {


  static public boolean isMotionFrozen()
  {
    return freezeMotion;
  }

  /**
   * used to limit breath spawning to a single instance
   * Usage:
   * (1) set the spawnSingleOnly to true
   * (2) When the breath weapon is not running, call resetSpawnSuppressor()
   * (3) Each time a breathFX is spawned, check okToSpawnSingle().  If true, spawn it, Otherwise don't
   *     okToSpawnSingle will return true on the first call after resetSpawnSuppressor, false for all the rest until reset
   * @return
   */
  static public boolean okToSpawnSingle()
  {
    if (!spawnSingleOnly) return true;
    if (haveSpawnedSingleAlready) return false;
    haveSpawnedSingleAlready = true;
    return true;
  }

  /**
   * see okToSpawnSingle
   */
  static public boolean isSpawnSingleOnly()
  {
    return spawnSingleOnly;
  }

  /**
   * see okToSpawnSingle
   */
  static public void resetSpawnSuppressor()
  {
    haveSpawnedSingleAlready = false;
  }

  public static boolean isLogBreathFXinfo() {
    return logBreathFXinfo;
  }

  static private boolean freezeAging = false;
  static private boolean freezeMotion = false;
  static private boolean freezeAnimation = false;
  static private boolean spawnSingleOnly = false;
  static private boolean haveSpawnedSingleAlready = false;
  static private boolean stillAlive = true;
  static private boolean logBreathFXinfo = false;


}
