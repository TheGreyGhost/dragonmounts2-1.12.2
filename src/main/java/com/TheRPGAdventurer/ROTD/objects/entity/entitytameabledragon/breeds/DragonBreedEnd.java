package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breeds;

import com.TheRPGAdventurer.ROTD.inits.ModSounds;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.nodes.BreathNodeP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class DragonBreedEnd extends DragonBreed {

    DragonBreedEnd() {
        super("ender", 0xab39be);
        
        setImmunity(DamageSource.MAGIC);
        setImmunity(DamageSource.HOT_FLOOR);
        setImmunity(DamageSource.LIGHTNING_BOLT);
        setImmunity(DamageSource.WITHER);
        
    }

    @Override
    public void onEnable(EntityTameableDragon dragon) {}

    @Override
    public void onDisable(EntityTameableDragon dragon) {}

    @Override
    public void onDeath(EntityTameableDragon dragon) {}
    
    @Override
    public SoundEvent getLivingSound(EntityTameableDragon dragon) {
       return ModSounds.ENTITY_DRAGON_BREATHE;
    }
    
    @Override
    public SoundEvent getRoarSoundEvent(EntityTameableDragon dragon) {
    return SoundEvents.ENTITY_ENDERDRAGON_GROWL;
    }
    
	@Override
	public boolean canChangeBreed() {
		return false;
	}
	
	@Override
	public void onLivingUpdate(EntityTameableDragon dragon) {
		doParticles(dragon);
	}
	
    private void doParticles(EntityTameableDragon dragon) {
        if (!dragon.isEgg() && !dragon.isBaby()) {
	        float s = dragon.getScale() * 1.2f;
	        for (double x1 = 0; x1 < s + 2; ++x1) {
		        double x = dragon.posX + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;
		        double y = dragon.posY + (rand.nextDouble() - 0.5) * dragon.height * s;
		        double z = dragon.posZ + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;
		        
		        dragon.world.spawnParticle(EnumParticleTypes.PORTAL, x, y, z, 0, 0, 0);
	        }
        }
    }

	@Override
    public void continueAndUpdateBreathingLegacy(World world, Vec3d origin, Vec3d endOfLook, BreathNodeP.Power power, EntityTameableDragon dragon) {
		dragon.getBreathHelperP().getBreathAffectedAreaEnd().continueBreathingLegacy(world, origin, endOfLook, power, dragon);
		dragon.getBreathHelperP().getBreathAffectedAreaEnd().updateTickLegacy(world);
    }

	@Override
    public void spawnBreathParticles(World world, BreathNodeP.Power power, int tickCounter, Vec3d origin, Vec3d endOfLook, EntityTameableDragon dragon) {
		dragon.getBreathHelperP().getEmitter().setBeamEndpoints(origin, endOfLook);
		dragon.getBreathHelperP().getEmitter().spawnBreathParticlesforEnderDragon(world, power, tickCounter);
    }
    
}
	
