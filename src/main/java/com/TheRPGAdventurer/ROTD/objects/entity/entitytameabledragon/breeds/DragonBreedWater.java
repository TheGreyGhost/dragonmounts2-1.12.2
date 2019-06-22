/*
 ** 2013 October 24
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breeds;

import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.EntityTameableDragon;

import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.BreathNode;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class DragonBreedWater extends DragonBreed {

    public DragonBreedWater() {
        super("sylphid", 0x4f69a8);
        
        setImmunity(DamageSource.DROWN);
        setImmunity(DamageSource.MAGIC);
        setImmunity(DamageSource.HOT_FLOOR);
        setImmunity(DamageSource.LIGHTNING_BOLT);
        setImmunity(DamageSource.WITHER);
        
        setHabitatBlock(Blocks.WATER);
        setHabitatBlock(Blocks.FLOWING_WATER);
        
        setHabitatBiome(Biomes.OCEAN);
        setHabitatBiome(Biomes.RIVER);
    }

	@Override
	public void onEnable(EntityTameableDragon dragon) {}

	@Override
	public void onDisable(EntityTameableDragon dragon) {}

	@Override
	public void onDeath(EntityTameableDragon dragon) {}

	@Override
    public void continueAndUpdateBreathingLegacy(World world, Vec3d origin, Vec3d endOfLook, BreathNode.Power power, EntityTameableDragon dragon) {
		dragon.getBreathHelperP().getbreathAffectedAreaHydro().continueBreathingLegacy(world, origin, endOfLook, power, dragon);
		dragon.getBreathHelperP().getbreathAffectedAreaHydro().updateTick(world);
    }

	@Override
    public void spawnBreathParticles(World world, BreathNode.Power power, int tickCounter, Vec3d origin, Vec3d endOfLook, EntityTameableDragon dragon) {
        dragon.getBreathHelperP().getEmitter().setBeamEndpoints(origin, endOfLook);
        dragon.getBreathHelperP().getEmitter().spawnBreathParticlesforWaterDragon(world, power, tickCounter);
    }

	public void onLivingUpdate(EntityTameableDragon dragon) {
		PotionEffect watereffect = new PotionEffect(MobEffects.WATER_BREATHING, 20*10, 0, false,false);
    	if (!dragon.isPotionActive(watereffect.getPotion()) && dragon.isInWater()) { // If the Potion isn't currently active,
    		dragon.addPotionEffect(watereffect); // Apply a copy of the PotionEffect to the player
		}
    	doParticles(dragon);
	}
	
    @SideOnly(Side.CLIENT)
    private void doParticles(EntityTameableDragon dragon) {
        if (!dragon.isEgg() && !dragon.isHatchling()) {
	        float s = dragon.getScale() * 1.2f;
	        for (double x1 = 0; x1 < s + 2; ++x1) {
		        double x = dragon.posX + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;
		        double y = dragon.posY + (rand.nextDouble() - 0.5) * dragon.height * s;
		        double z = dragon.posZ + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;
		        
		        dragon.world.spawnParticle(EnumParticleTypes.DRIP_WATER, x, y - 1, z, 0, 0, 0);
	        }
        }
    }
	
	@Override
	public EnumParticleTypes getSneezeParticle() {
		return null;
	}

//	public SoundEffectNames[] getBreathWeaponSoundEffects(EnumDragonLifeStage stage) {
//		final SoundEffectNames soundEffectNames[]={SoundEffectNames.ADULT_BREATHE_WATER_START, SoundEffectNames.ADULT_BREATHE_WATER_LOOP, SoundEffectNames.ADULT_BREATHE_WATER_STOP};
//
//		return soundEffectNames;
//
//	}


}