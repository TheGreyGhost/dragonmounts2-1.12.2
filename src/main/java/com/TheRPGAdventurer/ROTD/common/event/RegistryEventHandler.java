package com.TheRPGAdventurer.ROTD.common.event;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.gui.GuiHandler;
import com.TheRPGAdventurer.ROTD.common.blocks.BlockDragonBreedEgg;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.EnumDragonBreed;
import com.TheRPGAdventurer.ROTD.common.inits.ModBlocks;
import com.TheRPGAdventurer.ROTD.common.inits.ModItems;
import com.TheRPGAdventurer.ROTD.common.items.ItemDragonBreedEgg;
import com.TheRPGAdventurer.ROTD.util.DMUtils;
import com.TheRPGAdventurer.ROTD.util.IHasModel;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@Mod.EventBusSubscriber
public class RegistryEventHandler {

  @SubscribeEvent
  public static void registerBlocks(RegistryEvent.Register<Block> event) {
    event.getRegistry().registerAll(ModBlocks.BLOCKS.toArray(new Block[0]));

    DMUtils.getLogger().info("Block Registries Successfully Registered");
  }

  @SubscribeEvent
  public static void registerItems(RegistryEvent.Register<Item> event) {
    event.getRegistry().registerAll(ModItems.ITEMS.toArray(new Item[0]));

    DMUtils.getLogger().info("Item Registries Successfully Registered!");
  }

  @SubscribeEvent
  public static void registerDragonEggItem(RegistryEvent.Register<Item> event) {
    event.getRegistry().register(ItemDragonBreedEgg.DRAGON_BREED_EGG.setRegistryName("dragon_egg"));
  }

  @SubscribeEvent
  public static void registerDragonnEggBlock(RegistryEvent.Register<Block> event) {
    event.getRegistry().register(BlockDragonBreedEgg.DRAGON_BREED_EGG.setRegistryName("dragon_egg"));
  }


  @SubscribeEvent
  public static void registerModels(ModelRegistryEvent event) {

    for (Block block : ModBlocks.BLOCKS) {
      if (block instanceof IHasModel) {
        ((IHasModel) block).RegisterModels();
      }
    }

    for (Item item : ModItems.ITEMS) {
      if (item instanceof IHasModel) {
        ((IHasModel) item).RegisterModels();
      }
    }

    for (Item itemegg : ItemDragonBreedEgg.ITEM_EGG) {
      // register item renderer for dragon egg block variants
      ResourceLocation eggModelItemLoc = new ResourceLocation(DragonMounts.MODID, "dragon_egg");
      Item itemBlockDragonEgg = Item.REGISTRY.getObject(eggModelItemLoc);
      EnumDragonBreed.getAllBreedMetas().forEach((breed, meta) -> {
        ModelResourceLocation eggModelLoc = new ModelResourceLocation(DragonMounts.MODID + ":dragon_egg", "breed=" + breed.getName());
        ModelLoader.setCustomModelResourceLocation(itemBlockDragonEgg, meta, eggModelLoc);
      });
    }

    for (Block blockegg : BlockDragonBreedEgg.BLOCK_EGG) {
      // register item renderer for dragon egg block variants
      ResourceLocation eggModelItemLoc = new ResourceLocation(DragonMounts.MODID, "dragon_egg");
      Item itemBlockDragonEgg = Item.REGISTRY.getObject(eggModelItemLoc);
      EnumDragonBreed.getAllBreedMetas().forEach((breed, meta) -> {
        ModelResourceLocation eggModelLoc = new ModelResourceLocation(DragonMounts.MODID + ":dragon_egg", "breed=" + breed.getName());
        ModelLoader.setCustomModelResourceLocation(itemBlockDragonEgg, meta, eggModelLoc);
      });
    }

    DMUtils.getLogger().info("Models Sucessfully Registered");
  }


  @SubscribeEvent
  public static void registerTESRblockstates(ModelBakeEvent event) {
    // prevent model errors for the shulker block by registering it like a vanilla chest
//        event.getModelManager().getBlockModelShapes().registerBuiltInBlocks(ModBlocks.DRAGONSHULKER);  //todo I think this will work...  it didn't.  Find out why later on
  }

  public static void preInitRegistries() {
  }

  public static void initRegistries() {
    NetworkRegistry.INSTANCE.registerGuiHandler(DragonMounts.instance, new GuiHandler());
    DMUtils.getLogger().info("Gui's Successfully Registered");
  }
}