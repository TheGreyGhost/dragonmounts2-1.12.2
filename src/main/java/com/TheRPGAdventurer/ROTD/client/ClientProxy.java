/*
 ** 2012 August 27
 **
 ** The author disclaims copyright to this source code.  In place of
 ** t.AQUA legal notice, here is t.AQUA blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.client;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.gui.DragonMountsConfig;
import com.TheRPGAdventurer.ROTD.client.gui.GuiDragonDebug;
import com.TheRPGAdventurer.ROTD.client.model.TEISRDragonHatchableEgg;
import com.TheRPGAdventurer.ROTD.client.other.TargetHighlighter;
import com.TheRPGAdventurer.ROTD.client.render.TextureStitcherBreathFX;
import com.TheRPGAdventurer.ROTD.client.render.dragon.DragonRenderer;
import com.TheRPGAdventurer.ROTD.client.userinput.DragonOrbControl;
import com.TheRPGAdventurer.ROTD.common.CommonProxy;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.event.DragonViewEvent;
import com.TheRPGAdventurer.ROTD.common.inits.ModItems;
import com.TheRPGAdventurer.ROTD.common.inits.ModKeys;
import com.TheRPGAdventurer.ROTD.common.items.ItemDragonHatchableEgg;
import com.TheRPGAdventurer.ROTD.util.debugging.CentrepointCrosshairRenderer;
import com.TheRPGAdventurer.ROTD.util.debugging.StartupDebugClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;

//import scala.actors.threadpool.Arrays;

/**
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 *         2nd @author TheRPGAdventurer
 */
public class ClientProxy extends CommonProxy {

  @Override
  public void PreInitialization(FMLPreInitializationEvent event) {
    super.PreInitialization(event);
    // register dragon entity renderer
    DragonMountsConfig.clientPreInit();
    RenderingRegistry.registerEntityRenderingHandler(EntityTameableDragon.class, DragonRenderer::new);

    OBJLoader.INSTANCE.addDomain(DragonMounts.MODID);

//    ClientRegistry.registerTileEntity(TileEntityDragonHatchableEgg.class, "dragonmounts:te_dragon_hatchable_egg", new TESRDragonHatchableEgg());
    final int DEFAULT_ITEM_SUBTYPE = 0;
//    ModelResourceLocation mrlDragonHatchableEgg = new ModelResourceLocation("dragonmounts:dragon_hatchable_egg.obj", "inventory");
//    ModelLoader.setCustomModelResourceLocation(ModItems.DRAGON_HATCHABLE_EGG, DEFAULT_ITEM_SUBTYPE, mrlDragonHatchableEgg);
//    ForgeHooksClient.registerTESRItemStack(ModItems.DRAGON_HATCHABLE_EGG, DEFAULT_ITEM_SUBTYPE,  TileEntityDragonHatchableEgg.class);
//
//    RenderingRegistry.registerEntityRenderingHandler(HydroBreathFX.class, RenderHydroBreathFX::new);
//    RenderingRegistry.registerEntityRenderingHandler(FlameBreathFX.class, RenderFlameBreathFX::new);
//    RenderingRegistry.registerEntityRenderingHandler(EnderBreathFX.class, RenderEnderBreathFX::new);
//    RenderingRegistry.registerEntityRenderingHandler(NetherBreathFX.class, RenderNetherBreathFX::new);
//    RenderingRegistry.registerEntityRenderingHandler(WitherBreathFX.class, RenderWitherBreathFX::new);
//    RenderingRegistry.registerEntityRenderingHandler(IceBreathFX.class, RenderIceBreathFX::new);
//    RenderingRegistry.registerEntityRenderingHandler(PoisonBreathFX.class, RenderPoisonBreathFX::new);
//    RenderingRegistry.registerEntityRenderingHandler(AetherBreathFX.class, RenderAetherBreathFX::new);

            MinecraftForge.EVENT_BUS.register(new TextureStitcherBreathFX());

    //Override mcmod.info - This looks cooler :)
    TextFormatting t = null, r = TextFormatting.RESET;
    ModMetadata metadata = event.getModMetadata();
    metadata.name = t.DARK_AQUA + "" + t.BOLD + "Dragon Mounts";
    metadata.credits = "\n" +
            t.GREEN + "BarracudaATA4" + r + "-" + t.AQUA + "The Original Owner\n\n" +
            t.GREEN + "Merpou/Kingdomall/Masked_Ares" + r + "-" + t.AQUA + "First Dev for DM2. Has Made 500+ Textures and has put forth so much effort.\n\n" +
            t.GREEN + "Shannieanne" + r + "-" + t.AQUA + "Zombie Textures, Terra textures, Texture Fixes, Overall Second Dev\n\n" +
            t.GREEN + "GundunUkan/Lord Ukan" + r + "-" + t.AQUA + "for new fire texures, sunlight textures, and more.... I Hope he finishes his university hes hardworking working student\n\n" +
            t.GREEN + "Wolf" + r + "-" + t.AQUA + "Second Coder, started making small fixes then started doing big ones, I hope his dreams of becoming computer engineer succeeds\n\n" +
            t.GREEN + "FlaemWing" + r + "-" + t.AQUA + "for new nest block textures and dragonarmor item textures, new tool textures\n\n" +
            t.GREEN + "AlexThe666" + r + "-" + t.AQUA + "for open source code, Ice and Fire owner, Older Matured and more experience than me\n\n" +
            t.GREEN + "Majty/Guinea Owl" + r + "-" + t.AQUA + "for amulet textures\n" +
            t.GREEN + "TGG/TheGreyGhost" + r + "-" + t.AQUA + "old dm1 dev and prototype breath\n";
    metadata.authorList = Arrays.asList(StringUtils.split(t.GOLD + "" + t.BOLD + "TheRpgAdventurer, BarracudaATA, Kingdomall, Shannieanne, WolfShotz", ','));
    metadata.description =
            "\nTips:\n" +
                    "1. Don't forget to right click the egg to start the hatching process\n" +
                    "2. Also water dragon needs to be struck by lightning to become a storm dragon\n" +
                    "3. You can't hatch eggs in the End Dimension\n" +
                    "4. You can press " + t.ITALIC + "ctrl" + r + " to enable boost flight\n" +
                    "5. Dragons need to be of opposite genders to breed";
  }

  @Override
  public void Initialization(FMLInitializationEvent evt) {
    super.Initialization(evt);
    if (DragonMountsConfig.isDebug()) {
      MinecraftForge.EVENT_BUS.register(new GuiDragonDebug());
    }
    StartupDebugClientOnly.initClientOnly();

    // Dragon Whistle String Color
    Minecraft.getMinecraft().getItemColors().registerItemColorHandler((stack, tintIndex) -> {
      if (stack.hasTagCompound() && stack.getTagCompound().hasKey("Color") && tintIndex == 1)
        return stack.getTagCompound().getInteger("Color");
      return 0xFFFFFF;
    }, ModItems.dragon_whistle);

    ModItems.DRAGON_HATCHABLE_EGG.setTileEntityItemStackRenderer(new TEISRDragonHatchableEgg());
  }

  @Override
  public void PostInitialization(FMLPostInitializationEvent event) {
    super.PostInitialization(event);

    if (DragonMountsConfig.isDebug()) {
      MinecraftForge.EVENT_BUS.register(new GuiDragonDebug());
    }
    StartupDebugClientOnly.postInitClientOnly();

    DragonOrbControl.createSingleton(getNetwork());
    DragonOrbControl.initialiseInterceptors();
    MinecraftForge.EVENT_BUS.register(DragonOrbControl.getInstance());
    MinecraftForge.EVENT_BUS.register(new TargetHighlighter());
    MinecraftForge.EVENT_BUS.register(new CentrepointCrosshairRenderer());
    //       FMLCommonHandler.instance().bus().register(new DragonEntityWatcher());  todo not required? if i remember correctly this is used to make a zoom in, thridpersonview i unused now now uing DragonViewEvent rpg

    MinecraftForge.EVENT_BUS.register(new ModKeys());
    MinecraftForge.EVENT_BUS.register(new DragonViewEvent());
//    MinecraftForge.EVENT_BUS.register(ImmuneEntityItem.EventHandler.instance);

  }

  @SideOnly(Side.CLIENT)
  @Override
  public void render() {
    ModKeys.init();
  }

  public int getDragon3rdPersonView() {
    return thirdPersonViewDragon;
  }

  public void setDragon3rdPersonView(int view) {
    thirdPersonViewDragon = view;
  }


  public void registerItemRenderer(Item item, int meta, String id) {
    ModelLoader.setCustomModelResourceLocation(item, meta, new ModelResourceLocation(item.getRegistryName(), id));
  }

  @Override
  public File getDataDirectory() {
    return Minecraft.getMinecraft().mcDataDir;
  }
  private int thirdPersonViewDragon = 0;
}
