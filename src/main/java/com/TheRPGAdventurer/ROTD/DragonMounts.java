/*
 ** 2012 August 13
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD;

import com.TheRPGAdventurer.ROTD.client.gui.DragonMountsConfig;
import com.TheRPGAdventurer.ROTD.client.gui.GuiHandler;
import com.TheRPGAdventurer.ROTD.common.CommonProxy;
import com.TheRPGAdventurer.ROTD.common.event.EntityMountEventHandler;
import com.TheRPGAdventurer.ROTD.common.event.IItemColorRegistration;
import com.TheRPGAdventurer.ROTD.common.event.RegistryEventHandler;
import com.TheRPGAdventurer.ROTD.common.inventory.tabs.CreativeTab;
import com.TheRPGAdventurer.ROTD.common.network.*;
import com.TheRPGAdventurer.ROTD.common.world.DragonMountsWorldGenerator;
import com.TheRPGAdventurer.ROTD.util.MiscPlayerProperties;
import com.TheRPGAdventurer.ROTD.util.debugging.testclasses.LoggerLimit;
import net.ilexiconn.llibrary.server.entity.EntityPropertiesHandler;
import net.ilexiconn.llibrary.server.network.NetworkWrapper;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main control class for Forge.
 */
@Mod(dependencies = "required-after:llibrary@[" + DragonMounts.LLIBRARY_VERSION + ",)",
     modid = DragonMounts.MODID,
     name = DragonMounts.NAME,
     version = DragonMounts.VERSION,
     useMetadata = true,
     guiFactory = DragonMounts.GUI_FACTORY
    )
public class DragonMounts {

  public static final String NAME = "Dragon Mounts";
  public static final String MODID = "dragonmounts";
  public static final String VERSION = "@VERSION@";
  public static final String LLIBRARY_VERSION = "1.7.14";
  public static final String GUI_FACTORY = "com.TheRPGAdventurer.ROTD.client.gui.DragonMountsConfigGuiFactory";

  public static final Logger logger = LogManager.getLogger(DragonMounts.MODID);
  public static final LoggerLimit loggerLimit = new LoggerLimit(logger);

  @NetworkWrapper({MessageDragonInventory.class, MessageDragonWhistle.class, MessageDragonGuiSit.class,
                   MessageDragonGuiLock.class, MessageDragonTeleport.class, MessageDragonExtras.class})
  public static SimpleNetworkWrapper NETWORK_WRAPPER;

  @SidedProxy(serverSide = "com.TheRPGAdventurer.ROTD.common.ServerProxy", clientSide = "com.TheRPGAdventurer.ROTD.client.ClientProxy")
  public static CommonProxy proxy;

  @Instance(value = MODID)
  public static DragonMounts instance;
  public static CreativeTabs mainTab = new CreativeTab("maintab");
//  public static DamageSource dragons_fire;

  public DragonMountsConfig getConfig() {
    return config;
  }

  // important for debug in config
  public ModMetadata getMetadata() {
    return metadata;
  }

  @EventHandler
  public void PreInitialization(FMLPreInitializationEvent event) {
    config = new DragonMountsConfig(new Configuration(event.getSuggestedConfigurationFile()));

    proxy.PreInitialization(event);
    metadata = event.getModMetadata();
  }

  @EventHandler
  public void Initialization(FMLInitializationEvent event) {
    NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
    proxy.Initialization(event);
  }

  @EventHandler
  public void PostInitialization(FMLPostInitializationEvent event) {
    proxy.PostInitialization(event);
  }

  @EventHandler
  public void ServerStarting(FMLServerStartingEvent event) {
    proxy.ServerStarting(event);
  }

  @EventHandler
  public void ServerStopped(FMLServerStoppedEvent event) {
    proxy.ServerStopped(event);
  }

//  private void initDamageSources() {  // required?
//    dragons_fire = new DamageSource("dragons_fire") {
//    };
//  }

  private ModMetadata metadata;
  private DragonMountsConfig config;
}
