/*
 ** 2013 May 30
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.client.gui;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.google.common.collect.Lists;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

public class DragonMountsConfig {

  public static final String CATEGORY_MAIN = "main";
  public static final String CATEGORY_WORLDGEN = "worldGen";
  public static final String CATEGORY_CLIENTDM2 = "clientDM2";
  public boolean shouldChangeBreedViaHabitatOrBlock = true;
  public boolean canDragonDespawn = true;
  public boolean canMilk = true;
  public boolean canIceBreathBePermanent = false;
  public boolean canFireBreathAffectBlocks = true;
  public boolean useCommandingPlayer = false;
  public boolean allowOtherPlayerControl = true;
  public boolean allowBreeding = true;
  public boolean canSpawnSurfaceDragonNest = true;
  public boolean canSpawnUnderGroundNest = true;
  public boolean canSpawnNetherNest = true;
  public boolean canSpawnEndNest = true;
  public double ARMOR = 8F;
  public double BASE_DAMAGE = 5.0F;
  public double BASE_HEALTH = 90.0d;
  public int REG_FACTOR = 75;
  public int hungerDecrement = 3000;
  // chances
  public int FireNestRarity = 50;
  //	public static int ZombieNestRarity1  = 180;
  public int TerraNestRarity = 180;
  public int ForestNestRarity = 180;
  public int SunlightNestRarity = 60;
  public int OceanNestRarity = 4000;
  public int EnchantNestRarity = 300;
  public int JungleNestRarity = 700;
  public int WaterNestRarity = 150;
  public int IceNestRarity = 200;
  public int netherNestRarity = 200;
  public int netherNestRarerityInX = 16;
  public int netherNestRarerityInZ = 16;
  public int zombieNestRarity = 500;
  public int zombieNestRarerityInX = 28;
  public int zombieNestRarerityInZ = 28;
  public double thirdPersonZoom = 8;
  public int dragonFollowOwnerFlyingHeight = 50;
  public int dragonWanderFromHomeDist = 50;
  public double maxFlightHeight = 20;
  public int[] dragonBlacklistedDimensions = new int[]{1, -1};
  public int[] dragonWhitelistedDimensions = new int[]{0};
  public int minimumDistance = 16;
  public boolean useDimensionBlackList = true;

  public DragonMountsConfig(Configuration i_config) {
    config = i_config;
    syncFromFiles();
  }

  public void clientPreInit() {
    MinecraftForge.EVENT_BUS.register(new ConfigEventHandler());
  }

  public void syncFromFiles() {
    syncconfigs(true, true);
  }

  public void syncFromGui() {
    syncconfigs(false, true);
  }

  public void syncFromFields() {
    syncconfigs(false, false);
  }
  // can be caused by static instantiation of classes especially Items Blocks and similar

  public Configuration getConfig() {
    return config;
  }

  public boolean isDebug() {
    verifyLoaded();
    return debug;
  }

  public boolean isDisableBlockOverride() {
    verifyLoaded();
    return disableBlockOverride;
  }

  public boolean isOrbTargetAutoLock() {
    verifyLoaded();
    return true;
  } //todo update later if dragon orb gets reintroduced

  public boolean isOrbHighlightTarget() {
    verifyLoaded();
    return true;
  }

  public boolean isPrototypeBreathweapons() {
    verifyLoaded();
    return isDebug() && prototypeBreathWeapons;
  } // turn off prototype breathweapons if not debugging

  public boolean doBreathweaponsAffectBlocks() {
    verifyLoaded();
    return true;
  } // todo implement later

  public boolean isOrbHolderImmune() {
    verifyLoaded();
    return true;
  } //todo implement later

  public class ConfigEventHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEvent(ConfigChangedEvent.OnConfigChangedEvent event) {
      if (event.getModID().equals(DragonMounts.MODID)) {
        syncFromGui();
      }
    }
  }

  private void verifyLoaded() {
    if (configHasLoaded) return;

    DragonMounts.loggerLimit.error_once(
            "One or more DragonMountsConfig properties were accessed before loading the configuration");
  }

  private void syncconfigs(boolean loadFromConfigFile, boolean readFromConfig) {
    if (loadFromConfigFile)
      config.load();

    List<String> propOrder = Lists.newArrayList();
    Property prop;

		/*
     *  MAIN
		 */
    prop = config.get(CATEGORY_MAIN, "debug", debug);
    prop.setComment("Debug mode. You need to restart Minecraft for the change to take effect.  Unless you're a developer or are told to activate it, you don't want to set this to true.");
    debug = prop.getBoolean();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_MAIN, "prototype breath weapons", prototypeBreathWeapons);
    prop.setComment("Use prototype breath weapons (Debug mode only).  Unless you're a developer or are told to activate it, you don't want to set this to true.");
    prototypeBreathWeapons = prop.getBoolean();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_MAIN, "can eggs change breeds", shouldChangeBreedViaHabitatOrBlock);
    prop.setComment("Enables changing of egg breeds via block or environment");
    shouldChangeBreedViaHabitatOrBlock = prop.getBoolean();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_MAIN, "disable block override", disableBlockOverride);
    prop.setComment("Disables right-click override on the vanilla dragon egg block. May help to fix issues with other mods.");
    disableBlockOverride = prop.getBoolean();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_MAIN, "Armor", ARMOR);
    prop.setComment("Makes Dragons Tougher or Not");
    ARMOR = prop.getDouble();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_MAIN, "Damage", BASE_DAMAGE);
    prop.setComment("Damage for dragon attack");
    BASE_DAMAGE = prop.getDouble();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_MAIN, "Dragon Base Health", BASE_HEALTH);
    prop.setComment("Dragon Base Health" + TextFormatting.ITALIC + " Note: Some Dragons have unique health values and are still affected by this");
    BASE_HEALTH = prop.getDouble();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_MAIN, "Health Regen Speed", REG_FACTOR);
    prop.setComment("Higher numbers slower regen for dragons");
    REG_FACTOR = prop.getInt();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_MAIN, "Hunger Speed", hungerDecrement);
    prop.setComment("More numbers slower, i.e. gets a number from the factor of (3000) to 1 per tick (millisecond) if it equals to 1 reduce hunger, set to zero for no hunger, dont make it too low or might crash the game");
    hungerDecrement = prop.getInt();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_MAIN, "can dragons despawn", canDragonDespawn);
    prop.setComment("Enables or Disables dragons ability to despawn, works only for adult non tamed dragons");
    canDragonDespawn = prop.getBoolean();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_MAIN, "Milk Dregons", canMilk);
    prop.setComment("Joke Feature: makes dragons milkable like cows");
    canMilk = prop.getBoolean();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_MAIN, "can ice breath be permanent", canIceBreathBePermanent);
    prop.setComment("refers to the ice breath for the dragon in water, set true if you want the ice block to be permanent. false otherwise.");
    canIceBreathBePermanent = prop.getBoolean();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_MAIN, "fire breath affect blocks", canFireBreathAffectBlocks);
    prop.setComment("refers to the fire breath to affect blocks");
    canFireBreathAffectBlocks = prop.getBoolean();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_MAIN, "use CommandingPlayer", useCommandingPlayer);
    prop.setComment("Use a commanding player method(Experimental) to make dragons land on multiple players");
    useCommandingPlayer = prop.getBoolean();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_MAIN, "Allow Other Player's Control", allowOtherPlayerControl);
    prop.setComment("Disable or enable the dragon's ability to obey other players");
    allowOtherPlayerControl = prop.getBoolean();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_MAIN, "Allow Other Breeding", allowBreeding);
    prop.setComment("Allow or disallow breeding");
    allowBreeding = prop.getBoolean();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_CLIENTDM2, "Max Flight Height", maxFlightHeight);
    prop.setComment("Max flight for dragons circling players on a whistle");
    maxFlightHeight = prop.getDouble();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_CLIENTDM2, "Third Person Zoom BACK", thirdPersonZoom);
    prop.setComment("Zoom out for third person 2 while riding the the dragon and dragon carriages DO NOT EXXAGERATE IF YOU DONT WANT CORRUPTED WORLDS");
    thirdPersonZoom = prop.getDouble();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_CLIENTDM2, "Wander From HomeDist", dragonWanderFromHomeDist);
    prop.setComment("Wander From HomeDist");
    dragonWanderFromHomeDist = prop.getInt();
    propOrder.add(prop.getName());

		/*
		 *  WORLDGEN
		 */

    // thanks i/f
    dragonBlacklistedDimensions = config.get("all", "Blacklisted Dragon Dimensions", new int[]{-1, 1}, "Dragons cannot spawn in these dimensions' IDs").getIntList();
    dragonWhitelistedDimensions = config.get("all", "Whitelisted Dragon Dimensions", new int[]{0}, "Dragons can only spawn in these dimensions' IDs").getIntList();
    useDimensionBlackList = config.getBoolean("use Dimension Blacklist", "all", true, "true to use dimensional blacklist, false to use the whitelist.");

    prop = config.get(CATEGORY_WORLDGEN, "canSpawnSurfaceDragonNest", canSpawnSurfaceDragonNest);
    prop.setComment("Enables spawning of nests in extreme hills");
    canSpawnSurfaceDragonNest = prop.getBoolean();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_WORLDGEN, "canSpawnNetherNest", canSpawnNetherNest);
    prop.setComment("Enables spawning of nether, zombie, and skeleton dragon nests in the nether");
    canSpawnNetherNest = prop.getBoolean();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_WORLDGEN, "canSpawnEnchantNest", canSpawnEndNest);
    prop.setComment("Enables spawning of end dragon nests in end cities");
    canSpawnEndNest = prop.getBoolean();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_WORLDGEN, "Forest Nest Rarity", ForestNestRarity);
    prop.setComment("Determines how rare Forest Plains dragon nests will mainly spawn. I did this because the forest biome is too common thus making the forest breed to common. Higher numbers = higher rarity (in other words  how many blocks for another nest to spawn), "
            + "(Note: Expermiment on a new world when editing these numbers because it may cause damages to your own worlds)");
    ForestNestRarity = prop.getInt();
    propOrder.add(prop.getName());

    // sunlight world nest
    prop = config.get(CATEGORY_WORLDGEN, "Sunlight Nest Rarity", SunlightNestRarity);
    prop.setComment("Determines how rare sunlight dragon temples will mainly spawn. Higher numbers = higher rarity (in other words  how many blocks for another nest to spawn), "
            + "(Note: Expermiment on a new world when editing these numbers because it may cause damages to your own worlds)");
    SunlightNestRarity = prop.getInt();
    propOrder.add(prop.getName());

    // sunlight world nest
    prop = config.get(CATEGORY_WORLDGEN, "Terra Nest Rarity", TerraNestRarity);
    prop.setComment("Determines how rare terra dragon nests will mainly spawn. Higher numbers = higher rarity (in other words  how many blocks for another nest to spawn), "
            + "(Note: Expermiment on a new world when editing these numbers because it may cause damages to your own worlds)");
    TerraNestRarity = prop.getInt();
    propOrder.add(prop.getName());

    // sunlight world nest
    prop = config.get(CATEGORY_WORLDGEN, "Ocean Nest Rarity", OceanNestRarity);
    prop.setComment("Determines how rare moonlight or aether dragon temples will spawn above the ocean. Higher numbers = higher rarity (in other words  how many blocks for another nest to spawn), "
            + "(Note: Expermiment on a new world when editing these numbers because it may cause damages to your own worlds)");
    OceanNestRarity = prop.getInt();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_WORLDGEN, "Jungle Nest Rarity", JungleNestRarity);
    prop.setComment("Determines how rare forest jungnle dragon nests will mainly spawn. Higher numbers = higher rarity (in other words  how many blocks for another nest to spawn), "
            + "(Note: Expermiment on a new world when editing these numbers because it may cause damages to your own worlds)");
    JungleNestRarity = prop.getInt();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_WORLDGEN, "Water Nest Rarity", WaterNestRarity);
    prop.setComment("Determines how rare water dragon nests will mainly spawn. Higher numbers = higher rarity (in other words  how many blocks for another nest to spawn), "
            + "(Note: Expermiment on a new world when editing these numbers because it may cause damages to your own worlds)");
    WaterNestRarity = prop.getInt();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_WORLDGEN, "Ice Nest Rarity", IceNestRarity);
    prop.setComment("Determines how rare ice dragon nests will mainly spawn. Higher numbers = higher rarity (in other words  how many blocks for another nest to spawn), "
            + "(Note: Expermiment on a new world when editing these numbers because it may cause damages to your own worlds)");
    IceNestRarity = prop.getInt();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_WORLDGEN, "Fire Nest Rarity", FireNestRarity);
    prop.setComment("Determines how rare fire dragon nests will mainly spawn. Higher numbers = higher rarity (in other words  how many blocks for another nest to spawn), "
            + "(Note: Expermiment on a new world when editing these numbers because it may cause damages to your own worlds)");
    FireNestRarity = prop.getInt();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_WORLDGEN, "Enchant Nest Rarity", EnchantNestRarity);
    prop.setComment("Determines how rare forest enchant dragon nests will mainly spawn. Higher numbers = higher rarity (in other words  how many blocks for another nest to spawn), "
            + "(Note: Expermiment on a new world when editing these numbers because it may cause damages to your own worlds)");
    EnchantNestRarity = prop.getInt();
    propOrder.add(prop.getName());

    // nether nest
    prop = config.get(CATEGORY_WORLDGEN, "Nether Nest Chance", netherNestRarity);
    prop.setComment("Determines how rare nether nests will mainly spawn. Higher numbers = higher rarity (in other words  how many blocks for another nest to spawn)"
            + "(Note: Expermiment on a new world when editing these numbers because it may cause damages to your own worlds)");
    netherNestRarity = prop.getInt();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_WORLDGEN, "2 Nether Nest Rarity X", netherNestRarerityInX);
    prop.setComment("Determines how rare nether nests will spawn in the X Axis. Higher numbers = higher rarity (in other words  how many blocks for another nest to spawn)"
            + "(Note: Expermiment on a new world when editing these numbers because it may cause damages to your own worlds)");
    netherNestRarerityInX = prop.getInt();
    propOrder.add(prop.getName());

    prop = config.get(CATEGORY_WORLDGEN, "2 Nest Nether Rarity Z", netherNestRarerityInZ);
    prop.setComment("Determines how rare nether nests will spawn in the Z Axis. Higher numbers = higher rarity (in other words  how many blocks for another nest to spawn)"
            + "(Note: Expermiment on a new world when editing these numbers because it may cause damages to your own worlds)");
    netherNestRarerityInZ = prop.getInt();
    propOrder.add(prop.getName());

    configHasLoaded = true;

    config.setCategoryPropertyOrder(CATEGORY_WORLDGEN, propOrder);
    config.save();

    if (config.hasChanged()) {
      config.save();
    }
  }
  private static Configuration config;
  // config properties
  private boolean disableBlockOverride = false;
  private boolean debug = false;
  private boolean prototypeBreathWeapons = false;
  private boolean configHasLoaded = false; // used to detect code which tries to access a property before the config has been loaded

}
