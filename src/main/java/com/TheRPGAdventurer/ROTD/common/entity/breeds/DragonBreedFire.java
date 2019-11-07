///*
// ** 2013 October 24
// **
// ** The author disclaims copyright to this source code.  In place of
// ** a legal notice, here is a blessing:
// **    May you do good and not evil.
// **    May you find forgiveness for yourself and forgive others.
// **    May you share freely, never taking more than you give.
// */
//package com.TheRPGAdventurer.ROTD.common.entity.breeds;
//
//import com.TheRPGAdventurer.ROTD.client.render.dragon.breathweaponFX.BreathWeaponFXEmitter;
//import com.TheRPGAdventurer.ROTD.client.render.dragon.breathweaponFX.BreathWeaponFXEmitterFire;
//import com.TheRPGAdventurer.ROTD.client.sound.SoundController;
//import com.TheRPGAdventurer.ROTD.client.sound.SoundEffectBreathWeaponFireP;
//import com.TheRPGAdventurer.ROTD.client.sound.SoundEffectBreathWeaponP;
//import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeFactory;
//import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeFire;
//import com.TheRPGAdventurer.ROTD.common.entity.breath.weapons.BreathWeaponFireP;
//import com.TheRPGAdventurer.ROTD.common.entity.breath.weapons.BreathWeaponP;
//import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
//import net.minecraft.block.material.Material;
//import net.minecraft.init.Blocks;
//import net.minecraft.util.DamageSource;
//import net.minecraft.util.EnumParticleTypes;
//
///**
// * @author Nico Bergemann <barracuda415 at yahoo.de>
// */
//public class DragonBreedFire extends DragonBreed {
//
//  public DragonBreedFire() {
//    super("fire", 0x960b0f);
//
//    setImmunity(DamageSource.MAGIC);
//    setImmunity(DamageSource.HOT_FLOOR);
//    setImmunity(DamageSource.LIGHTNING_BOLT);
//    setImmunity(DamageSource.WITHER);
//
//    setHabitatBlock(Blocks.FIRE);
//    setHabitatBlock(Blocks.LIT_FURNACE);
//    setHabitatBlock(Blocks.LAVA);
//    setHabitatBlock(Blocks.FLOWING_LAVA);
//
//  }
//
//  @Override
//  public void onEnable(EntityTameableDragon dragon) {
//    dragon.brain().setAvoidsWater(true);
//  }
//
//
//  /*    @Override
//      public SoundEvent getLivingSound() {
//         return SoundEvents.BLOCK_FIRE_AMBIENT;
//      }
//  */
//  @Override
//  public void onDisable(EntityTameableDragon dragon) {
//    dragon.brain().setAvoidsWater(false);
//  }
//
//  @Override
//  public void onDeath(EntityTameableDragon dragon) {
//  }
//
//  @Override
//  public void onLivingUpdate(EntityTameableDragon dragon) {
//    if (dragon.isInLava() || dragon.world.isMaterialInBB(dragon.getEntityBoundingBox().grow(-0.1, -0.4, -0.1), Material.FIRE))
//      doParticles(dragon);
//  }
//
////  @Override
////  public void continueAndUpdateBreathingLegacy(World world, Vec3d origin, Vec3d endOfLook, BreathNodeP.Power power, EntityTameableDragon dragon) {
////    dragon.breathweapon().getBreathAffectedAreaFire().continueBreathingLegacy(world, origin, endOfLook, power, dragon);
////    dragon.breathweapon().getBreathAffectedAreaFire().updateTickLegacy(world);
////  }
////
////  @Override
////  public void spawnBreathParticles(World world, BreathNodeP.Power power, int tickCounter, Vec3d origin, Vec3d endOfLook, EntityTameableDragon dragon) {
////    dragon.breathweapon().getEmitter().setBeamEndpoints(origin, endOfLook);
////    dragon.breathweapon().getEmitter().spawnBreathParticlesForFireDragon(world, power, tickCounter);
////  }
//
//  /**
//   * return a new fire breathweapon FX emitter
//   *
//   * @return
//   */
//  @Override
//  public BreathWeaponFXEmitter getBreathWeaponFXEmitter(EntityTameableDragon dragon) {
//    return new BreathWeaponFXEmitterFire();
//  }
//
//  @Override
//  public BreathWeaponSpawnType getBreathWeaponSpawnType(EntityTameableDragon dragon) {
//    return BreathWeaponSpawnType.NODES;
//  }
//
//  /**
//   * return a new BreathWeapon based on breed
//   *
//   * @return
//   */
//  @Override
//  public BreathWeaponP getBreathWeapon(EntityTameableDragon dragon) {
//    return new BreathWeaponFireP(dragon);
//  }
//
//  @Override
//  public BreathNodeFactory getBreathNodeFactory(EntityTameableDragon dragon) {
//    return new BreathNodeFire.BreathNodeFireFactory();
//  }
//
//  @Override
//  public SoundEffectBreathWeaponP getSoundEffectBreathWeapon(SoundController i_soundController,
//                                                             SoundEffectBreathWeaponP.WeaponSoundUpdateLink i_weaponSoundUpdateLink) {
//    return new SoundEffectBreathWeaponFireP(i_soundController, i_weaponSoundUpdateLink);
//  }
//
//  private void doParticles(EntityTameableDragon dragon) {
////    if (!dragon.isBaby()) {
//      float s = dragon.getAgeScale() * 1.2f;
//      for (double x1 = 0; x1 < s + 1; ++x1) {
//        double x = dragon.posX + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;
//        double y = dragon.posY + (rand.nextDouble() - 0.5) * dragon.height * s;
//        double z = dragon.posZ + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;
//
//        dragon.world.spawnParticle(EnumParticleTypes.FLAME, x, y, z, 0, 0, 0);
//      }
////    }
//  }
//
//}
