package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.sound;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * User: The Grey Ghost
 * Date: 17/04/2014
 * Contains (some of the) sound effect names used for the dragon
 */
public enum SoundEffectName
{
  SILENCE("mob.enderdragon.silence", 0),

  ADULT_BREATHE_FIRE_START("mob.enderdragon.breathweapon_p.fire.adultbreathefirestart", 2.0),
  ADULT_BREATHE_FIRE_LOOP("mob.enderdragon.breathweapon_p.fire.adultbreathefireloop", 5.336),
  ADULT_BREATHE_FIRE_STOP("mob.enderdragon.breathweapon_p.fire.adultbreathefirestop", 1.0),
  JUVENILE_BREATHE_FIRE_START("mob.enderdragon.breathweapon_p.fire.juvenilebreathefirestart", 2.0),
  JUVENILE_BREATHE_FIRE_LOOP("mob.enderdragon.breathweapon_p.fire.juvenilebreathefireloop", 5.336),
  JUVENILE_BREATHE_FIRE_STOP("mob.enderdragon.breathweapon_p.fire.juvenilebreathefirestop", 1.0),
  HATCHLING_BREATHE_FIRE_START("mob.enderdragon.breathweapon_p.fire.hatchlingbreathefirestart", 2.0),
  HATCHLING_BREATHE_FIRE_LOOP("mob.enderdragon.breathweapon_p.fire.hatchlingbreathefireloop", 5.336),
  HATCHLING_BREATHE_FIRE_STOP("mob.enderdragon.breathweapon_p.fire.hatchlingbreathefirestop", 1.0),

  BREATHE_ICE_START("mob.enderdragon.breathweapon_p.ice.breatheicestart", 1.7),
  BREATHE_ICE_LOOP("mob.enderdragon.breathweapon_p.ice.breatheiceloop", 7.663),
  BREATHE_ICE_STOP("mob.enderdragon.breathweapon_p.ice.breatheicestop", 1.2),

  BREATHE_FOREST_START("mob.enderdragon.breathweapon_p.forest.breatheforeststart", 0.929),
  BREATHE_FOREST_LOOP("mob.enderdragon.breathweapon_p.forest.breatheforestloop", 1.936),
  BREATHE_FOREST_STOP("mob.enderdragon.breathweapon_p.forest.breatheforeststop", 0.708),

  BREATHE_AIR_START("mob.enderdragon.breathweapon_p.air.breatheairstart", 1.5),
  BREATHE_AIR_LOOP("mob.enderdragon.breathweapon_p.air.breatheairloop", 7.407),
  BREATHE_AIR_STOP("mob.enderdragon.breathweapon_p.air.breatheairstop", 1.317),

  BREATHE_WATER_START("mob.enderdragon.breathweapon_p.water.breathewaterstart", 0.733),
  BREATHE_WATER_LOOP("mob.enderdragon.breathweapon_p.water.breathewaterloop", 4.949),
  BREATHE_WATER_STOP("mob.enderdragon.breathweapon_p.water.breathewaterstop", 3.536),

  BREATHE_NETHER_SPAWN("mob.enderdragon.breathweapon_p.nether.breathenetherspawn", 2.0),
  NETHER_PROJECTILE_LOOP("mob.enderdragon.breathweapon_p.nether.projectilenetherloop", 3.4),

  BREATHE_ENDER_SPAWN("mob.enderdragon.breathweapon_p.ender.breatheenderspawn", 2.0),
  ENDER_PROJECTILE_LOOP("mob.enderdragon.breathweapon_p.ender.projectileenderloop", 9.056);


  public final String getJsonName() {return DragonMounts.MODID + ":" + jsonName;}
  public final SoundEvent getSoundEvent() {return soundEvent;}
  public final double getDurationInSeconds() {return durationInSeconds;}
  public final int getDurationInTicks() {return (int)(durationInSeconds * 20.0);}

  /**
   * Call this during client startup to register all the sounds, otherwise class initialises late.
   * doesn't actually do anything other than initialise the class
   */
  public static void registerAllSounds()
  {
  }

  /**
   * Information about the sound effect
   * @param i_jsonName
   * @param i_durationInSeconds the duration of the sound effect (0 = unused) - in practice, this is the duration
   *                            before the cross-fade to the next sound starts.  For looping sounds no effect
   */
  private SoundEffectName(String i_jsonName, double i_durationInSeconds) {
    jsonName = i_jsonName;
    durationInSeconds = i_durationInSeconds;
    soundEvent = registerSound(jsonName);
  }
  private final String jsonName;
  private final double durationInSeconds;
  private final SoundEvent soundEvent;

  private static SoundEvent registerSound(String soundName) {
    ResourceLocation soundID = new ResourceLocation(DragonMounts.MODID, soundName);
    SoundEvent retval = new SoundEvent(soundID).setRegistryName(soundID);
    ForgeRegistries.SOUND_EVENTS.register(retval);
    return retval;
  }

}
