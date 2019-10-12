//package com.TheRPGAdventurer.ROTD.common.entity.breeds;
//
//import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeP;
//import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
//import net.minecraft.init.Biomes;
//import net.minecraft.init.Blocks;
//import net.minecraft.util.DamageSource;
//import net.minecraft.util.math.Vec3d;
//import net.minecraft.world.World;
//
//;
//
//public class DragonBreedForest extends DragonBreed {
//
//  @Override
//  public void onEnable(EntityTameableDragon dragon) {
//  }
//
//  @Override
//  public void onDisable(EntityTameableDragon dragon) {
//  }
//
//  @Override
//  public void onDeath(EntityTameableDragon dragon) {
//  }
//
//  @Override
//  public void onLivingUpdate(EntityTameableDragon dragon) {
//  }
//
//  @Override
//  public void continueAndUpdateBreathingLegacy(World world, Vec3d origin, Vec3d endOfLook, BreathNodeP.Power power, EntityTameableDragon dragon) {
//    dragon.breathweapon().getBreathAffectedAreaFire().continueBreathingLegacy(world, origin, endOfLook, power, dragon);
//    dragon.breathweapon().getBreathAffectedAreaFire().updateTickLegacy(world);
//  }
//
//  @Override
//  public void spawnBreathParticles(World world, BreathNodeP.Power power, int tickCounter, Vec3d origin, Vec3d endOfLook, EntityTameableDragon dragon) {
//    dragon.breathweapon().getEmitter().setBeamEndpoints(origin, endOfLook);
//    dragon.breathweapon().getEmitter().spawnBreathParticlesForFireDragon(world, power, tickCounter);
//  }
//
//  DragonBreedForest() {
//    super("forest", 0x298317);
//
//    setImmunity(DamageSource.MAGIC);
//    setImmunity(DamageSource.HOT_FLOOR);
//    setImmunity(DamageSource.LIGHTNING_BOLT);
//    setImmunity(DamageSource.WITHER);
//
//    setHabitatBlock(Blocks.YELLOW_FLOWER);
//    setHabitatBlock(Blocks.RED_FLOWER);
//    setHabitatBlock(Blocks.MOSSY_COBBLESTONE);
//    setHabitatBlock(Blocks.VINE);
//    setHabitatBlock(Blocks.SAPLING);
//    setHabitatBlock(Blocks.LEAVES);
//    setHabitatBlock(Blocks.LEAVES2);
//
//    setHabitatBiome(Biomes.JUNGLE);
//    setHabitatBiome(Biomes.JUNGLE_HILLS);
//
//  }
//}
//
