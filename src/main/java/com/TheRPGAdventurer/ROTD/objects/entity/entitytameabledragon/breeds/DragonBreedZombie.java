package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breeds;

import com.TheRPGAdventurer.ROTD.inits.ModSounds;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.BreathNode;
import net.minecraft.init.Blocks;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class DragonBreedZombie extends DragonBreed {

    DragonBreedZombie() {
        super("zombie", 0X5e5602);

        setImmunity(DamageSource.MAGIC);
        setImmunity(DamageSource.HOT_FLOOR);
        setImmunity(DamageSource.LIGHTNING_BOLT);
        setImmunity(DamageSource.WITHER);

        setHabitatBlock(Blocks.SOUL_SAND);
        setHabitatBlock(Blocks.NETHER_WART_BLOCK);
    }

    @Override
    public void onEnable(EntityTameableDragon dragon) {


    }

    @Override
    public void onDisable(EntityTameableDragon dragon) {


    }

    @Override
    public void onDeath(EntityTameableDragon dragon) {


    }

    @Override
    public void continueAndUpdateBreathingLegacy(World world, Vec3d origin, Vec3d endOfLook, BreathNode.Power power, EntityTameableDragon dragon) {
        dragon.getBreathHelperP().getbreathAffectedAreaPoison().continueBreathingLegacy(world, origin, endOfLook, power, dragon);
        dragon.getBreathHelperP().getbreathAffectedAreaPoison().updateTick(world);
    }

    @Override
    public void spawnBreathParticles(World world, BreathNode.Power power, int tickCounter, Vec3d origin, Vec3d endOfLook, EntityTameableDragon dragon) {
        dragon.getBreathHelperP().getEmitter().setBeamEndpoints(origin, endOfLook);
        dragon.getBreathHelperP().getEmitter().spawnBreathParticlesforPoisonDragon(world, power, tickCounter);
    }

    @Override
    public SoundEvent getLivingSound(EntityTameableDragon dragon) {
        return dragon.isHatchling() ?  ModSounds.ENTITY_DRAGON_HATCHLING_GROWL : ModSounds.ZOMBIE_DRAGON_GROWL;
    }

    @Override
    public SoundEvent getDeathSound() {
        return ModSounds.ENTITY_DRAGON_DEATH;
    }

//	@Override
//	public boolean isInfertile() {
//		return true;
//	}

//    public SoundEffectNames[] getBreathWeaponSoundEffects(EnumDragonLifeStage stage) {
//
//        final SoundEffectNames soundEffectNames[]={SoundEffectNames.ADULT_BREATHE_ICE_START, SoundEffectNames.ADULT_BREATHE_ICE_LOOP, SoundEffectNames.ADULT_BREATHE_ICE_STOP};
//        return soundEffectNames;
//    }

    @Override
    public EnumParticleTypes getSneezeParticle() {
        return null;
    }

//	@Override
//	public boolean canChangeBreed() {
//		return false;
//	}

}
