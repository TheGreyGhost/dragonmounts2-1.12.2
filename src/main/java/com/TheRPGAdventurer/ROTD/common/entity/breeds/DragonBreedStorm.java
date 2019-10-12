//package com.TheRPGAdventurer.ROTD.common.entity.breeds;
//
//import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeP;
//import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
//import net.minecraft.entity.EntityLivingBase;
//import net.minecraft.entity.effect.EntityLightningBolt;
//import net.minecraft.entity.player.EntityPlayer;
//import net.minecraft.init.MobEffects;
//import net.minecraft.potion.PotionEffect;
//import net.minecraft.util.EnumParticleTypes;
//import net.minecraft.util.math.Vec3d;
//import net.minecraft.world.World;
//
//import java.util.Random;
//
//public class DragonBreedStorm extends DragonBreed {
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
//  @Override
//  public void onLivingUpdate(EntityTameableDragon dragon) {
//    EntityLivingBase target = dragon.getAttackTarget();
//    PotionEffect watereffect = new PotionEffect(MobEffects.WATER_BREATHING, 20 * 10);
//    if (!dragon.isPotionActive(watereffect.getPotion()) && dragon.isInWater()) { // If the Potion isn't currently active,
//      dragon.addPotionEffect(watereffect); // Apply a copy of the PotionEffect to the player
//    }
//
//    Random random = new Random();
//    boolean shouldelectrecute = target != null && target.isEntityAlive();
//
//    if (target instanceof EntityPlayer) {
//      EntityPlayer playertarget = (EntityPlayer) target;
//      if (playertarget.capabilities.isCreativeMode) {
//        shouldelectrecute = false;
//      }
//    }
//
//    if (shouldelectrecute) {
//      if (random.nextInt(70) == 1 && dragon.world.isRaining()) {
//        dragon.world.addWeatherEffect(new EntityLightningBolt(target.world, target.posX, target.posY, target.posZ, false));
//      }
//    }
//    doParticles(dragon);
//  }
//
////	@Override
////	public boolean isInfertile() {
////		return true;
////	}
////
////	public SoundEffectNames[] getBreathWeaponSoundEffects(EnumDragonLifeStage stage) {
////		final SoundEffectNames soundEffectNames[]={SoundEffectNames.ADULT_BREATHE_ICE_START, SoundEffectNames.ADULT_BREATHE_ICE_LOOP, SoundEffectNames.ADULT_BREATHE_ICE_STOP};
////		return soundEffectNames;
////	}
//
//  @Override
//  public EnumParticleTypes getSneezeParticle() {
//    return null;
//  }
//
//  private void doParticles(EntityTameableDragon dragon) {
//    if (!dragon.isEgg() && !dragon.isBaby()) {
//      float s = dragon.getAgeScale() * 1.2f;
//      for (double x1 = 0; x1 < s; ++x1) {
//        double x = dragon.posX + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;
//        double y = dragon.posY + (rand.nextDouble() - 0.5) * dragon.height * s;
//        double z = dragon.posZ + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;
//
//        dragon.world.spawnParticle(EnumParticleTypes.DRIP_WATER, x, y - 1, z, 0, 0, 0);
//      }
//    }
//  }
//
//  DragonBreedStorm() {
//    super("storm", 0xf5f1e9);
//
//  }
//
//
//}
