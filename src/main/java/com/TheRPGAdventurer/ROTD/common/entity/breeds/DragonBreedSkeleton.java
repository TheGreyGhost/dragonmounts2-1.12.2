//package com.TheRPGAdventurer.ROTD.common.entity.breeds;
//
//import com.TheRPGAdventurer.ROTD.common.entity.breath.nodes.BreathNodeP;
//import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
//import com.TheRPGAdventurer.ROTD.common.inits.ModSounds;
//import net.minecraft.init.Blocks;
//import net.minecraft.util.DamageSource;
//import net.minecraft.util.EnumParticleTypes;
//import net.minecraft.util.SoundEvent;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.MathHelper;
//import net.minecraft.util.math.Vec3d;
//import net.minecraft.world.World;
//
//
//public class DragonBreedSkeleton extends DragonBreed {
//
//  @Override
//  public boolean isHabitatEnvironment(EntityTameableDragon dragon) {
//    if (dragon.posY > dragon.world.getHeight() * 0.25) {
//      // woah dude, too high!
//      return false;
//    }
//
//    int bx = MathHelper.floor(dragon.posX);
//    int by = MathHelper.floor(dragon.posY);
//    int bz = MathHelper.floor(dragon.posZ);
//    BlockPos blockPos = new BlockPos(bx, by, bz);
//
//    if (dragon.world.getLightBrightness(blockPos) > 7) {
//      // too bright!
//      return false;
//    }
//
//    return true;
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
//  public SoundEvent getLivingSound(EntityTameableDragon dragon) {
//    if (dragon.isBaby()) {
//      return ModSounds.ENTITY_DRAGON_HATCHLING_GROWL;
//    } else {
//      return ModSounds.ENTITY_SKELETON_DRAGON_GROWL;
//    }
//  }
//
//  @Override
//  public boolean canUseBreathWeapon() {
//    return false;
//  }
//
////	@Override
////	public boolean canChangeBreed() {return false;}
//
//  @Override
//  public void continueAndUpdateBreathingLegacy(World world, Vec3d origin, Vec3d endOfLook, BreathNodeP.Power power, EntityTameableDragon dragon) {
//    // no legacy breath
//  }
//
//  @Override
//  public void spawnBreathParticles(World world, BreathNodeP.Power power, int tickCounter, Vec3d origin, Vec3d endOfLook, EntityTameableDragon dragon) {
//    // no legacy breath
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
//  DragonBreedSkeleton() {
//    super("skeleton", 0xffffff);
//
//    setHabitatBlock(Blocks.BONE_BLOCK);
//    setImmunity(DamageSource.LIGHTNING_BOLT);
//    setImmunity(DamageSource.WITHER);
//  }
//
//
//}
//
