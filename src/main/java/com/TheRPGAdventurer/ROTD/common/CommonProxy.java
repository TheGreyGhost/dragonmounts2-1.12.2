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
import com.TheRPGAdventurer.ROTD.client.gui.DragonMountsConfig;
import com.TheRPGAdventurer.ROTD.common.cmd.CommandDragon;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.helper.DragonLifeStageHelper;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantsReader;
import com.TheRPGAdventurer.ROTD.common.event.VanillaEggHandler;
import com.TheRPGAdventurer.ROTD.common.items.entity.ImmuneEntityItem;
import com.TheRPGAdventurer.ROTD.common.network.MessageDragonTarget;
import com.TheRPGAdventurer.ROTD.common.network.MessageDragonTargetHandlerServer;
import com.TheRPGAdventurer.ROTD.util.debugging.StartupDebugCommon;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.server.FMLServerHandler;

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

  public void PreInitialization(FMLPreInitializationEvent event) {
    DragonMountsConfig.PreInit();
    StartupDebugCommon.preInitCommon();
    DragonLifeStageHelper.registerConfigurationTags();
    DragonVariantsReader dragonVariantsReader = new DragonVariantsReader(
            Minecraft.getMinecraft().getResourceManager(), new ResourceLocation("dragonmounts:dragonvariants.json"));
    Map<String, DragonVariants> allBreedsDragonVariants = dragonVariantsReader.readVariants();
  }

  @SuppressWarnings("deprecation")
  public void Initialization(FMLInitializationEvent evt) {
    MinecraftForge.EVENT_BUS.register(new VanillaEggHandler());
    network = NetworkRegistry.INSTANCE.newSimpleChannel("DragonControls");
    network.registerMessage(MessageDragonTargetHandlerServer.class, MessageDragonTarget.class, DOT_DISCRIMINATOR_ID, Side.SERVER);

    StartupDebugCommon.initCommon();
  }

  public void PostInitialization(FMLPostInitializationEvent event) {
    registerEntities();
    if (DragonMountsConfig.isDebug()) {
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

  public void render() {
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
    EntityRegistry.registerModEntity(new ResourceLocation(DragonMounts.MODID, "dragon"), EntityTameableDragon.class, "DragonMount",
            ENTITY_ID, DragonMounts.instance, ENTITY_TRACKING_RANGE, ENTITY_UPDATE_FREQ,
            ENTITY_SEND_VELO_UPDATES);
//    EntityRegistry.registerModEntity(new ResourceLocation(DragonMounts.MODID, "indestructible"), ImmuneEntityItem.class, "Indestructible Item",
//            3, DragonMounts.instance, 32, 5, true);

    //        GameRegistry.registerTileEntity(TileEntityDragonShulker.class, new ResourceLocation(DragonMounts.MODID, "dragon_shulker"));

  }
  private final int ENTITY_TRACKING_RANGE = 80;
  private final int ENTITY_UPDATE_FREQ = 3; // 3
  private final int ENTITY_ID = 1;
  private final boolean ENTITY_SEND_VELO_UPDATES = true;
  private SimpleNetworkWrapper network;


}