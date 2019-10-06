/*
 ** 2012 August 27
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.common;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.model.EggModels;
import com.TheRPGAdventurer.ROTD.common.cmd.CommandDragon;
import com.TheRPGAdventurer.ROTD.common.entity.EntityDragonEgg;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.helper.DragonLifeStageHelper;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantsException;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantsReader;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.Modifiers;
import com.TheRPGAdventurer.ROTD.common.event.EntityMountEventHandler;
import com.TheRPGAdventurer.ROTD.common.event.RegistryEventHandler;
import com.TheRPGAdventurer.ROTD.common.event.VanillaEggHandler;
import com.TheRPGAdventurer.ROTD.common.network.MessageDragonTarget;
import com.TheRPGAdventurer.ROTD.common.network.MessageDragonTargetHandlerServer;
import com.TheRPGAdventurer.ROTD.common.world.DragonMountsWorldGenerator;
import com.TheRPGAdventurer.ROTD.util.MiscPlayerProperties;
import com.TheRPGAdventurer.ROTD.util.debugging.StartupDebugCommon;
import net.ilexiconn.llibrary.server.entity.EntityPropertiesHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.util.Map;

/**
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 *         2nd @author TheRPGAdventurer
 */
abstract public class CommonProxy {

  public final byte DCM_DISCRIMINATOR_ID = 35;  // arbitrary non-zero ID (non-zero makes troubleshooting easier)
  public final byte DOT_DISCRIMINATOR_ID = 73;  // arbitrary non-zero ID (non-zero makes troubleshooting easier)

  public SimpleNetworkWrapper getNetwork() {
    return this.network;
  }

  /**
   * Split into phase 1 and phase 2 to allow the client to do tasks before the common (eg DragonVariantsTags registrations)
   * @param event
   */
  public void PreInitialization(FMLPreInitializationEvent event) {
    preInitialisePhase1(event);
    preInitialisePhase2(event);
  }

  @SuppressWarnings("deprecation")
  public void Initialization(FMLInitializationEvent evt) {
    MinecraftForge.EVENT_BUS.register(new VanillaEggHandler());
    MinecraftForge.EVENT_BUS.register(new EntityMountEventHandler());
    network = NetworkRegistry.INSTANCE.newSimpleChannel("DragonControls");
    network.registerMessage(MessageDragonTargetHandlerServer.class, MessageDragonTarget.class, DOT_DISCRIMINATOR_ID, Side.SERVER);
    EntityPropertiesHandler.INSTANCE.registerProperties(MiscPlayerProperties.class);
    GameRegistry.registerWorldGenerator(new DragonMountsWorldGenerator(), 0);
//    initDamageSources();
    RegistryEventHandler.initRegistries();

    StartupDebugCommon.initCommon();
  }

  public void PostInitialization(FMLPostInitializationEvent event) {
    registerEntities();
    if (DragonMounts.instance.getConfig().isDebug()) {
      StartupDebugCommon.postInitCommon();
    }
  }

  public void ServerStarting(FMLServerStartingEvent evt) {
    MinecraftServer server = evt.getServer();
    ServerCommandManager cmdman = (ServerCommandManager) server.getCommandManager();
    cmdman.registerCommand(new CommandDragon());
  }

  public void ServerStopped(FMLServerStoppedEvent evt) {
  }

  public int getDragon3rdPersonView() {
    return 0;
  }

  public void setDragon3rdPersonView(int view) {
  }

  public void registerModel(Item item, int metadata) {
  }

  public void registerItemRenderer(Item item, int meta, String id) {
  }

  // get the directory on disk used for storing the game files
  // is different for dedicated server vs client
  abstract public File getDataDirectory();

  private void registerEntities() {
    EntityRegistry.registerModEntity(new ResourceLocation(DragonMounts.MODID, "dragon_hatchable_egg"), EntityDragonEgg.class, "DragonMount",
            DRAGON_HATCHABLE_EGG_ENTITY_ID, DragonMounts.instance, DRAGON_HATCHABLE_EGG_ENTITY_TRACKING_RANGE, ENTITY_UPDATE_FREQ,
            ENTITY_SEND_VELO_UPDATES);
    EntityRegistry.registerModEntity(new ResourceLocation(DragonMounts.MODID, "dragon"), EntityTameableDragon.class, "DragonMount",
            DRAGON_ENTITY_ID, DragonMounts.instance, DRAGON_ENTITY_TRACKING_RANGE, ENTITY_UPDATE_FREQ,
            ENTITY_SEND_VELO_UPDATES);
//    EntityRegistry.registerModEntity(new ResourceLocation(DragonMounts.MODID, "indestructible"), ImmuneEntityItem.class, "Indestructible Item",
//            3, DragonMounts.instance, 32, 5, true);

    //        GameRegistry.registerTileEntity(TileEntityDragonShulker.class, new ResourceLocation(DragonMounts.MODID, "dragon_shulker"));
  }

  protected void preInitialisePhase1(FMLPreInitializationEvent event) {
//    DragonMountsConfig.PreInit();
    Modifiers.preInitialise();
    DragonLifeStageHelper.registerConfigurationTags();
    EntityDragonEgg.registerConfigurationTags();

  }

  // read the dragon variants information for all breed config files, create the respective breeds, and initialise any corresponding resources
  protected void preInitialisePhase2(FMLPreInitializationEvent event) {
    DragonVariantsReader dragonVariantsReader = new DragonVariantsReader(Minecraft.getMinecraft().getResourceManager(), "variants");
    Map<String, DragonVariants> allBreedsDragonVariants = dragonVariantsReader.readAllVariants();
    try {
      DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getDefaultBreed().getDragonVariants().initialiseResourcesForCollection();
    } catch (DragonVariantsException dve) {
      DragonMounts.logger.error("One or more errors occurred while initialising the default breed resources- indicates an internal program error.  Msg:\n" + dve.getMessage());
    }

    for (Map.Entry<String, DragonVariants> entry : allBreedsDragonVariants.entrySet()) {
      try {
        DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().createDragonBreedNew(entry.getKey(), entry.getValue());
        entry.getValue().initialiseResourcesForCollection();
      } catch (DragonVariantsException dve) {
        DragonMounts.logger.error("One or more errors occurred while initialising the resources for breed " + entry.getKey()
                                  + ":\n" + dve.getMessage());
      }
    }

    StartupDebugCommon.preInitCommon();
  }

  abstract public boolean isDedicatedServer();

  private final int DRAGON_ENTITY_TRACKING_RANGE = 80;
  private final int ENTITY_UPDATE_FREQ = 3; // 3
  private final int DRAGON_ENTITY_ID = 1;
  private final int DRAGON_HATCHABLE_EGG_ENTITY_ID = 2;
  private final int DRAGON_HATCHABLE_EGG_ENTITY_TRACKING_RANGE = 20;

  private final boolean ENTITY_SEND_VELO_UPDATES = true;
  private SimpleNetworkWrapper network;
}