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
//import com.TheRPGAdventurer.ROTD.client.sound.SoundEffectNames;
//import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeP;
//import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
//import com.TheRPGAdventurer.ROTD.common.entity.helper.DragonLifeStage;
//import net.minecraft.init.Biomes;
//import net.minecraft.init.Blocks;
//import net.minecraft.init.MobEffects;
//import net.minecraft.potion.PotionEffect;
//import net.minecraft.util.DamageSource;
//import net.minecraft.util.EnumParticleTypes;
//import net.minecraft.util.math.Vec3d;
//import net.minecraft.world.World;
//
///**
// * @author Nico Bergemann <barracuda415 at yahoo.de>
// */
//public class DragonBreedWater extends DragonBreed {
//
//  public DragonBreedWater() {
//    super("sylphid", 0x4f69a8);
//
//    setImmunity(DamageSource.DROWN);
//    setImmunity(DamageSource.MAGIC);
//    setImmunity(DamageSource.HOT_FLOOR);
//    setImmunity(DamageSource.LIGHTNING_BOLT);
//    setImmunity(DamageSource.WITHER);
//
//    setHabitatBlock(Blocks.WATER);
//    setHabitatBlock(Blocks.FLOWING_WATER);
//
//    setHabitatBiome(Biomes.OCEAN);
//    setHabitatBiome(Biomes.RIVER);
//  }
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
//  public void continueAndUpdateBreathingLegacy(World world, Vec3d origin, Vec3d endOfLook, BreathNodeP.Power power, EntityTameableDragon dragon) {
//    dragon.breathweapon().getbreathAffectedAreaHydro().continueBreathingLegacy(world, origin, endOfLook, power, dragon);
//    dragon.breathweapon().getbreathAffectedAreaHydro().updateTickLegacy(world);
//  }
//
//  @Override
//  public void spawnBreathParticles(World world, BreathNodeP.Power power, int tickCounter, Vec3d origin, Vec3d endOfLook, EntityTameableDragon dragon) {
//    dragon.breathweapon().getEmitter().setBeamEndpoints(origin, endOfLook);
//    dragon.breathweapon().getEmitter().spawnBreathParticlesforWaterDragon(world, power, tickCounter);
//  }
//
//  public void onLivingUpdate(EntityTameableDragon dragon) {
//    PotionEffect watereffect = new PotionEffect(MobEffects.WATER_BREATHING, 20 * 10, 0, false, false);
//    if (!dragon.isPotionActive(watereffect.getPotion()) && dragon.isInWater()) { // If the Potion isn't currently active,
//      dragon.addPotionEffect(watereffect); // Apply a copy of the PotionEffect to the player
//    }
//    doParticles(dragon);
//  }
//
//  @Override
//  public EnumParticleTypes getSneezeParticle() {
//    return null;
//  }
//
////	public SoundEffectNames[] getBreathWeaponSoundEffects(EnumDragonLifeStage stage) {
////		final SoundEffectNames soundEffectNames[]={SoundEffectNames.ADULT_BREATHE_WATER_START, SoundEffectNames.ADULT_BREATHE_WATER_LOOP, SoundEffectNames.ADULT_BREATHE_WATER_STOP};
////
////		return soundEffectNames;
////
////	}
//
//  public SoundEffectNames[] getBreathWeaponSoundEffects(DragonLifeStage stage) {
//    final SoundEffectNames soundEffectNames[] = {SoundEffectNames.ADULT_BREATHE_ICE_START, SoundEffectNames.ADULT_BREATHE_ICE_LOOP, SoundEffectNames.ADULT_BREATHE_ICE_STOP};
//
//    return soundEffectNames;
//
//  }
//
//  private void doParticles(EntityTameableDragon dragon) {
//    if (!dragon.isEgg() && !dragon.isBaby()) {
//      float s = dragon.getAgeScale() * 1.2f;
//      for (double x1 = 0; x1 < s + 2; ++x1) {
//        double x = dragon.posX + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;
//        double y = dragon.posY + (rand.nextDouble() - 0.5) * dragon.height * s;
//        double z = dragon.posZ + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;
//
//        dragon.world.spawnParticle(EnumParticleTypes.DRIP_WATER, x, y - 1, z, 0, 0, 0);
//      }
//    }
//  }
//}