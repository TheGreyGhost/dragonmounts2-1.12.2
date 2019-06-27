package com.TheRPGAdventurer.ROTD.util.debugging;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import org.lwjgl.input.Keyboard;

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

  public static boolean isAnimationFrozen()
  {
    return DragonMounts.instance.getConfig().isDebug() && frozen;
  }
  public static void setAnimationFrozen(boolean newstate) {frozen = newstate;}
  private static boolean frozen;
}
