//package com.TheRPGAdventurer.ROTD.common.entity.breeds;
//
//import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeP;
//import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
//import net.minecraft.init.Blocks;
//import net.minecraft.util.DamageSource;
//import net.minecraft.util.EnumParticleTypes;
//import net.minecraft.util.math.Vec3d;
//import net.minecraft.world.World;
//
//public class DragonBreedEnchant extends DragonBreed {
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
//    doParticles(dragon);
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
//  private void doParticles(EntityTameableDragon dragon) {
//    if (!dragon.isEgg() && !dragon.isBaby()) {
//      float s = dragon.getAgeScale() * 1.2f;
//      for (double x1 = 0; x1 < s + 25; ++x1) {
//        double x = dragon.posX + (rand.nextDouble() - 0.5) * (dragon.width - 1.2) * s;
//        double y = dragon.posY + (rand.nextDouble() - 0.5) * dragon.height * s;
//        double z = dragon.posZ + (rand.nextDouble() - 0.5) * (dragon.width - 1.2) * s;
//
//        dragon.world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE, x, y, z, 0, 0, 0);
//      }
//    }
//  }
//
//  DragonBreedEnchant() {
//    super("enchant", 0x8359ae);
//
//    setImmunity(DamageSource.MAGIC);
//    setImmunity(DamageSource.HOT_FLOOR);
//    setImmunity(DamageSource.LIGHTNING_BOLT);
//    setImmunity(DamageSource.WITHER);
//
//    setHabitatBlock(Blocks.BOOKSHELF);
//    setHabitatBlock(Blocks.ENCHANTING_TABLE);
//  }
//
//}
