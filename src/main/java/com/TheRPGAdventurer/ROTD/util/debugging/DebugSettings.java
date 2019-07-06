package com.TheRPGAdventurer.ROTD.util.debugging;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.sun.javafx.geom.Vec3f;
import org.lwjgl.input.Keyboard;
import java.util.HashMap;

/**
 * Created by TGG on 29/06/2015.
 *   Freeze dragon animation and updates for debugging purposes
 *   frozen when the scroll lock key is down (and debug mode is set)
 */
public class DebugSettings
{

  public static boolean isDebugGuiEnabled()
  {
    return DragonMounts.instance.getConfig().isDebug() && debugGuiEnabled;
  }
  public static void setDebugGuiEnabled(boolean newstate) {debugGuiEnabled = newstate;}
  private static boolean debugGuiEnabled;

  public static boolean isSpawningInhibited()
  {
    return DragonMounts.instance.getConfig().isDebug() && spawningInhibited;
  }
  public static void setSpawningInhibited(boolean newstate) {spawningInhibited = newstate;}
  private static boolean spawningInhibited;

  /** are the draqon and breath animations be frozen in place?
   * Usage:
   * 1) setAnimationFreezeEnabled to enable freezing
   * 2) whenever you want the animation to be frozen, call setAnimationFreezeActive
   *    (this does nothing unless freezing has been enabled)
   * Disabling the freezing will also deactivate freezing
   * @return
   */
  public static boolean isAnimationFrozen()
  {
    return DragonMounts.instance.getConfig().isDebug() && frozenEnabled && frozenActive;
  }
  public static void setAnimationFreezeEnabled(boolean newstate) {
    frozenEnabled = newstate;
    if (!frozenEnabled) frozenActive = false;
  }
  public static float animationFrozenPartialTicks() {return partialTicks;}
  public static boolean isAnimationFreezeEnabled() {return frozenEnabled;}
  public static void setAnimationFreezeActive(boolean newstate) {frozenActive = newstate;}
  private static boolean frozenEnabled;
  private static boolean frozenActive;
  private static final float partialTicks = 0.25F;

  public static boolean isRenderCentrePoints() {
    return renderCentrePoints;
  }
  public static void setRenderCentrePoints(boolean renderCentrePoints) {
    DebugSettings.renderCentrePoints = renderCentrePoints;
  }
  private static boolean renderCentrePoints;

  public static boolean isRenderXYZmarkers() {
    return renderXYZmarkers;
  }
  public static void setRenderXYZmarkers(boolean renderXYZmarkers) {
    DebugSettings.renderXYZmarkers = renderXYZmarkers;
  }
  private static boolean renderXYZmarkers;

  public static boolean isForceDragonModel() {
    return forceDragonModel;
  }
  public static void setForceDragonModel(boolean forceDragonModel) {
    DebugSettings.forceDragonModel = forceDragonModel;
  }
  private static boolean forceDragonModel = true; //todo restore to false

  public static boolean isBoxDragon() {
    return boxDragon;
  }
  public static void setBoxDragon(boolean boxDragon) {
    DebugSettings.boxDragon = boxDragon;
  }
  private static boolean boxDragon = false;    //todo restore to false

  public static boolean isRiderPositionTweak() {
    return riderPositionTweak;
  }
  public static void setRiderPositionTweak(boolean riderPositionTweak) {
    DebugSettings.riderPositionTweak = riderPositionTweak;
  }
  public static Vec3f getRiderPositionOffset(int rider, Vec3f offset) {
    Vec3f retval = new Vec3f(offset);
    if (existsDebugParameter("rx"+ rider)) {
      retval.x = (float)DebugSettings.getDebugParameter("rx"+ rider);
    }
    if (existsDebugParameter("ry"+ rider)) {
      retval.y = (float)DebugSettings.getDebugParameter("ry"+ rider);
    }
    if (existsDebugParameter("rz"+ rider)) {
      retval.z = (float)DebugSettings.getDebugParameter("rz"+ rider);
    }
    return retval;
  }
  private static boolean riderPositionTweak = false;


  /**
   * Debug parameters can be set using the command console
   * /dragon debug parameter {name} {value}
   * eg
   * /dragon debug parameter x 0.3
   * Useful for interactively adjusting rendering offsets in-game
   * @param parameterName
   * @param value
   */
  public static void setDebugParameter(String parameterName, double value)
  {
    debugParameters.put(parameterName, value);
  }

  public static double getDebugParameter(String parameterName)
  {
    Double value = debugParameters.get(parameterName);
    return (value == null) ? 0.0 : value;
  }

  public static boolean existsDebugParameter(String parameterName)
  {
    return debugParameters.containsKey(parameterName);
  }

  private static HashMap<String, Double> debugParameters = new HashMap<>();


}
