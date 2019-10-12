//package com.TheRPGAdventurer.ROTD.common.entity.breeds;
//
//import com.TheRPGAdventurer.ROTD.client.sound.SoundEffectNames;
//import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeP;
//import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
//import com.TheRPGAdventurer.ROTD.common.entity.helper.DragonLifeStage;
//import com.TheRPGAdventurer.ROTD.common.inits.ModSounds;
//import net.minecraft.init.Blocks;
//import net.minecraft.util.DamageSource;
//import net.minecraft.util.EnumParticleTypes;
//import net.minecraft.util.SoundEvent;
//import net.minecraft.util.math.Vec3d;
//import net.minecraft.world.World;
//
//public class DragonBreedZombie extends DragonBreed {
//
//  @Override
//  public void onEnable(EntityTameableDragon dragon) {
//
//
//  }
//
//  @Override
//  public void onDisable(EntityTameableDragon dragon) {
//
//
//  }
//
//  @Override
//  public void onDeath(EntityTameableDragon dragon) {
//
//
//  }
//
//  @Override
//  public void continueAndUpdateBreathingLegacy(World world, Vec3d origin, Vec3d endOfLook, BreathNodeP.Power power, EntityTameableDragon dragon) {
//    dragon.breathweapon().getbreathAffectedAreaPoison().continueBreathingLegacy(world, origin, endOfLook, power, dragon);
//    dragon.breathweapon().getbreathAffectedAreaPoison().updateTickLegacy(world);
//  }
//
//  @Override
//  public void spawnBreathParticles(World world, BreathNodeP.Power power, int tickCounter, Vec3d origin, Vec3d endOfLook, EntityTameableDragon dragon) {
//    dragon.breathweapon().getEmitter().setBeamEndpoints(origin, endOfLook);
//    dragon.breathweapon().getEmitter().spawnBreathParticlesforPoisonDragon(world, power, tickCounter);
//  }
//
//  @Override
//  public SoundEvent getLivingSound(EntityTameableDragon dragon) {
//    return dragon.isBaby() ? ModSounds.ENTITY_DRAGON_HATCHLING_GROWL : ModSounds.ZOMBIE_DRAGON_GROWL;
//  }
//
//  @Override
//  public SoundEvent getDeathSound() {
//    return ModSounds.ENTITY_DRAGON_DEATH;
//  }
//
//  //    public SoundEffectNames[] getBreathWeaponSoundEffects(EnumDragonLifeStage stage) {
////
////        final SoundEffectNames soundEffectNames[]={SoundEffectNames.ADULT_BREATHE_ICE_START, SoundEffectNames.ADULT_BREATHE_ICE_LOOP, SoundEffectNames.ADULT_BREATHE_ICE_STOP};
////        return soundEffectNames;
////    }
//  public SoundEffectNames[] getBreathWeaponSoundEffects(DragonLifeStage stage) {
//
//    final SoundEffectNames soundEffectNames[] = {SoundEffectNames.ADULT_BREATHE_ICE_START, SoundEffectNames.ADULT_BREATHE_ICE_LOOP, SoundEffectNames.ADULT_BREATHE_ICE_STOP};
//    return soundEffectNames;
//  }
//
////	@Override
////	public boolean isInfertile() {
////		return true;
////	}
//
//  @Override
//  public EnumParticleTypes getSneezeParticle() {
//    return null;
//  }
//
//  DragonBreedZombie() {
//    super("zombie", 0X5e5602);
//
//    setImmunity(DamageSource.MAGIC);
//    setImmunity(DamageSource.HOT_FLOOR);
//    setImmunity(DamageSource.LIGHTNING_BOLT);
//    setImmunity(DamageSource.WITHER);
//
//    setHabitatBlock(Blocks.SOUL_SAND);
//    setHabitatBlock(Blocks.NETHER_WART_BLOCK);
//  }
//
////	@Override
////	public boolean canChangeBreed() {
////		return false;
////	}
//
//}
