//package com.TheRPGAdventurer.ROTD.common.event;
//
//import com.TheRPGAdventurer.ROTD.DragonMounts;
//import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
//import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonFactory;
//import com.TheRPGAdventurer.ROTD.common.entity.breeds.EnumDragonBreed;
//import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
//import com.TheRPGAdventurer.ROTD.common.entity.helper.DragonLifeStage;
//import com.TheRPGAdventurer.ROTD.util.DMUtils;
//import net.minecraft.init.Blocks;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.text.TextComponentTranslation;
//import net.minecraft.world.DimensionType;
//import net.minecraft.world.World;
//import net.minecraftforge.event.entity.player.PlayerInteractEvent;
//import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//
///**
// * Handler for the vanilla dragon egg block
// *
// * @author WolfShotz
// * @deprecated - Should be handled in a different way. (Replacing the vanilla egg with our custom egg etc) This is a temporary solution
// */
//public class VanillaEggHandler {
//
//  @SubscribeEvent
//  public void onPlayerInteract(PlayerInteractEvent.RightClickBlock evt) {
//    World world = evt.getWorld();
//    BlockPos pos = evt.getPos();
//    if (world.getBlockState(pos).getBlock() != Blocks.DRAGON_EGG) return; //ignore all other blocks
//    if (world.isRemote) return; //do nothing on client world
//    if (DragonMounts.instance.getConfig().isDisableBlockOverride()) return; //do nothing if config is set
//    if (world.provider.getDimensionType() == DimensionType.THE_END) {
//      evt.getEntityPlayer().sendStatusMessage(new TextComponentTranslation(DMUtils.translateToLocal("egg.cantHatchEnd.DragonMounts")), true);
//      return;  //cant hatch in the end
//    }
//
//    world.setBlockToAir(evt.getPos());
//
//    EntityTameableDragon entityDragon = DragonFactory.getDefaultDragonFactory().createDragon(world, DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getDefaultBreed());
//    entityDragon.setPosition(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5);
//    entityDragon.setBreedType(EnumDragonBreed.FIRE);
//    entityDragon.lifeStage().setLifeStage(DragonLifeStage.EGG);
//    entityDragon.reproduction().setBreeder(evt.getEntityPlayer());
//
//    world.spawnEntity(entityDragon);
//  }
//}