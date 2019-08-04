/*
 ** 2012 August 23
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.common.entity.helper.util;

import com.TheRPGAdventurer.ROTD.util.math.MathX;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 *
 *
 * Enum for dragon life stages. Used as aliases for the age value of dragons.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public enum InterpolateArray {



  //order, duration of stage in hours of minecraft time, scaleAtStartOfStage, scaleAtEndOfStage
  EGG(0, 36, 0.25f, 0.25f),
  HATCHLING(1, 48, 0.04f, 0.09f),
  INFANT(2, 12, 0.09f, 0.33f),
  PREJUVENILE(3, 18, 0.33f, 0.50f),
  JUVENILE(4, 60, 0.50f, 1.00f),
  ADULT(5, 0, 1.00f, 1.00f);        // scale of the final stage should be 1.00F to avoid breaking other code

//  desired durations (30 Jun 2019)
//  egg = 30 minutes
//          hatchling = 45 minutes
//          infant = 10 minutes (just filler)
//  prejuvenile = 15 minutes (just filler)
//  juvenile = 50 minutes
//          adult = 1 hour

  /**
   * get the current life stage based on the dragon's age
   *
   * @param ticksSinceCreation number of ticks since the egg was created
   * @return
   */
  public static InterpolateArray getLifeStageFromTickCount(int ticksSinceCreation) {
    ticksSinceCreation = clipTickCountToValid(ticksSinceCreation);

    InterpolateArray stageFound = sortedStages.get(sortedStages.firstKey());
    for (InterpolateArray dragonLifeStage : sortedStages.values()) {
      if (ticksSinceCreation < stageInfo.get(dragonLifeStage).startTicks) {
        return stageFound;
      }
      stageFound = dragonLifeStage;
    }
    return stageFound;
  }

  public static float getStageProgressFromTickCount(int ticksSinceCreation) {
    ticksSinceCreation = clipTickCountToValid(ticksSinceCreation);
    InterpolateArray lifeStage = getLifeStageFromTickCount(ticksSinceCreation);
    if (lifeStage.durationTicks == 0) return 1.0F;

    int lifeStageTicks = ticksSinceCreation - stageInfo.get(lifeStage).startTicks;
    return lifeStageTicks / (float) lifeStage.durationTicks;
  }

  public static float getAgeScaleFromTickCount(int ticksSinceCreation) {
    InterpolateArray lifeStage = getLifeStageFromTickCount(ticksSinceCreation);
    StageInfo si = stageInfo.get(lifeStage);
    int timeInThisStage = ticksSinceCreation - si.startTicks;
    if (lifeStage.durationTicks == 0) {
      return lifeStage.startAgeScale;
    }
    float fractionOfStage = timeInThisStage / (float) lifeStage.durationTicks;

    return MathX.lerp(lifeStage.startAgeScale, lifeStage.finalAgeScale, fractionOfStage);
  }

  public static int clipTickCountToValid(int ticksSinceCreation) {
    return MathHelper.clamp(ticksSinceCreation, minimumValidTickValue, maximumValidTickValue);
  }

  /**
   * true if we're in the egg, false otherwise
   *
   * @return
   */
  public boolean isEgg() {
    return this == EGG;
  }

  /**
   * does this stage act like a minecraft baby?
   *
   * @return
   */
  public boolean isBaby() {
    return this == HATCHLING || this == INFANT;
  }

  public boolean isHatchling() {
    return this == HATCHLING;
  }

  /**
   * is the dragon fully grown?
   *
   * @return
   */
  public boolean isFullyGrown() {
    return this == ADULT;
  }

  /**
   * is this dragon large enough to breath?
   *
   * @return
   */
  public boolean isOldEnoughToBreathe() {
    return this.order >= PREJUVENILE.order;
  }

  /**
   * what is the tick count corresponding to the start of this stage?
   *
   * @return
   */
  public int getStartTickCount() {
    return stageInfo.get(this).startTicks;
  }
  private static HashMap<InterpolateArray, StageInfo> stageInfo;
  private static SortedMap<Integer, InterpolateArray> sortedStages = new TreeMap<>();
  private static int minimumValidTickValue;
  private static int maximumValidTickValue;
  //
  private final int order;
  private final int durationTicks; // -1 means infinite
  private final float startAgeScale;
  private final float finalAgeScale;

  // set up various helper structures
  static { // guaranteed to run only after all enums have been created
    for (InterpolateArray dragonLifeStage : InterpolateArray.values()) {
      sortedStages.put(dragonLifeStage.order, dragonLifeStage);
    }

    int startTickCumulative = 0;
    stageInfo = new HashMap<InterpolateArray, StageInfo>(sortedStages.size());

    minimumValidTickValue = startTickCumulative;
    for (InterpolateArray dragonLifeStage : sortedStages.values()) {
      StageInfo si = new StageInfo();
      si.startTicks = startTickCumulative;
      startTickCumulative += dragonLifeStage.durationTicks;
      si.endTicks = startTickCumulative;
      stageInfo.put(dragonLifeStage, si);
    }
    maximumValidTickValue = startTickCumulative;
  }

  /**
   * Which life stage is the dragon in?
   *
   * @param order                = the sort order of this stage (0 = first, 1 = second, etc)
   * @param minecraftTimeHours   = the duration of this stage in game time hours (each hour game time is 50 seconds in real life)
   * @param ageScaleAtEndOfStage size of this stage relative to the final scale (adult)
   */
  InterpolateArray(int order, int minecraftTimeHours, float ageScaleAtStartOfStage, float ageScaleAtEndOfStage) {
    this.order = order;
    final int TICKS_PER_MINECRAFT_HOUR = 20 * 60 * 20 / 24;  // 20 (ticks/real life second) * 60 (seconds / min)
    //   * 20 (real life minutes per minecraft day) / 24 (hours/day)
    this.durationTicks = minecraftTimeHours * TICKS_PER_MINECRAFT_HOUR;
    this.startAgeScale = ageScaleAtStartOfStage;
    this.finalAgeScale = ageScaleAtEndOfStage;
  }

  static class StageInfo {
    int startTicks;
    int endTicks;
  }

}
