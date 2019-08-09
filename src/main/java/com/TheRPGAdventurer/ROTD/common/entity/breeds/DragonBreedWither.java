//package com.TheRPGAdventurer.ROTD.common.entity.breeds;
//
//import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeP;
//import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
//import com.TheRPGAdventurer.ROTD.common.inits.ModSounds;
//import net.minecraft.util.DamageSource;
//import net.minecraft.util.EnumParticleTypes;
//import net.minecraft.util.SoundEvent;
//import net.minecraft.util.math.Vec3d;
//import net.minecraft.world.World;
//import net.minecraft.world.WorldServer;
//
//
//public class DragonBreedWither extends DragonBreed {
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
//  public SoundEvent getLivingSound(EntityTameableDragon dragon) {
//    if (dragon.isBaby()) {
//      return ModSounds.ENTITY_DRAGON_HATCHLING_GROWL;
//    } else {
//      return ModSounds.ENTITY_NETHER_DRAGON_GROWL;
//    }
//  }
//
//  @Override
//  public boolean canChangeBreed() {
//    return false;
//  }
//
//  @Override
//  public void continueAndUpdateBreathingLegacy(World world, Vec3d origin, Vec3d endOfLook, BreathNodeP.Power power, EntityTameableDragon dragon) {
//    dragon.getBreathHelperP().getbreathAffectedAreaWither().continueBreathingLegacy(world, origin, endOfLook, power, dragon);
//    dragon.getBreathHelperP().getbreathAffectedAreaWither().updateTickLegacy(world);
//  }
//
//  @Override
//  public void spawnBreathParticles(World world, BreathNodeP.Power power, int tickCounter, Vec3d origin, Vec3d endOfLook, EntityTameableDragon dragon) {
//    dragon.getBreathHelperP().getEmitter().setBeamEndpoints(origin, endOfLook);
//    dragon.getBreathHelperP().getEmitter().spawnBreathParticlesforWitherDragon(world, power, tickCounter);
//  }
//
//  @Override
//  public void onLivingUpdate(EntityTameableDragon dragon) {
//    World world = dragon.world;
//    if (dragon.isUsingBreathWeapon()) {
//      if (world instanceof WorldServer && !dragon.isDead && !dragon.isEgg()) {
//        ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_NORMAL, dragon.posX,
//                dragon.posY + dragon.getEyeHeight(), dragon.posZ, 1, 0.5D, 0.25D, 0.5D, 0.0D);
//      }
//    }
//  }
//
//  @Override
//  public EnumParticleTypes getSneezeParticle() {
//    return null;
//  }
//
////	@Override
////	public boolean isInfertile() {
////		return true;
////	}
//
//  DragonBreedWither() {
//    super("wither", 0x50260a);
//
//    setImmunity(DamageSource.MAGIC);
//    setImmunity(DamageSource.HOT_FLOOR);
//    setImmunity(DamageSource.LIGHTNING_BOLT);
//    setImmunity(DamageSource.WITHER);
//
//  }
//
//}
//
