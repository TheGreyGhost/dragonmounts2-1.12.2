//package com.TheRPGAdventurer.ROTD.common.entity.breeds;
//
//import com.TheRPGAdventurer.ROTD.client.sound.SoundEffectNames;
//import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeP;
//import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
//import com.TheRPGAdventurer.ROTD.common.entity.helper.DragonLifeStage;
//import net.minecraft.init.Blocks;
//import net.minecraft.util.EnumParticleTypes;
//import net.minecraft.util.math.Vec3d;
//import net.minecraft.world.World;
//
//public class DragonBreedMoonlight extends DragonBreed {
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
//    if (dragon.posY > dragon.world.getHeight() && !dragon.world.isDaytime()) doParticles(dragon);
//  }
//
//  @Override
//  public void continueAndUpdateBreathingLegacy(World world, Vec3d origin, Vec3d endOfLook, BreathNodeP.Power power, EntityTameableDragon dragon) {
//    dragon.breathweapon().getBreathAffectedAreaIce().continueBreathingLegacy(world, origin, endOfLook, power, dragon);
//    dragon.breathweapon().getBreathAffectedAreaIce().updateTickLegacy(world);
//  }
//
//  @Override
//  public void spawnBreathParticles(World world, BreathNodeP.Power power, int tickCounter, Vec3d origin, Vec3d endOfLook, EntityTameableDragon dragon) {
//    dragon.breathweapon().getEmitter().setBeamEndpoints(origin, endOfLook);
//    dragon.breathweapon().getEmitter().spawnBreathParticlesforIceDragon(world, power, tickCounter);
//  }
//
//  public SoundEffectNames[] getBreathWeaponSoundEffects(DragonLifeStage stage) {
//    final SoundEffectNames soundEffectNames[] = {SoundEffectNames.ADULT_BREATHE_ICE_START, SoundEffectNames.ADULT_BREATHE_ICE_LOOP, SoundEffectNames.ADULT_BREATHE_ICE_STOP};
//
//    return soundEffectNames;
//  }
//
//  private void doParticles(EntityTameableDragon dragon) {
//    if (!dragon.isEgg() && !dragon.isBaby()) {
//      float s = dragon.getAgeScale() * 1.2f;
//      double x = dragon.posX + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;
//      double y = dragon.posY + (rand.nextDouble() - 0.5) * dragon.height * s;
//      double z = dragon.posZ + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;
//
//      dragon.world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, x, y, z, 0, 0, 0);
//    }
//  }
//
//  DragonBreedMoonlight() {
//    super("moonlight", 0x2c427c);
//
//    setHabitatBlock(Blocks.DAYLIGHT_DETECTOR_INVERTED);
//    setHabitatBlock(Blocks.BLUE_GLAZED_TERRACOTTA);
//  }
////
////	public SoundEffectNames[] getBreathWeaponSoundEffects(EnumDragonLifeStage stage) {
////		final SoundEffectNames soundEffectNames[]={SoundEffectNames.ADULT_BREATHE_ICE_START, SoundEffectNames.ADULT_BREATHE_ICE_LOOP, SoundEffectNames.ADULT_BREATHE_ICE_STOP};
////
////		return soundEffectNames;
////
////	}
//}
