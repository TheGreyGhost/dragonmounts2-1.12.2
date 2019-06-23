package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breeds;


import com.TheRPGAdventurer.ROTD.inits.ModSounds;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.nodes.BreathNodeP;
import net.minecraft.init.Biomes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


public class DragonBreedNether extends DragonBreed {

    DragonBreedNether() {
        super("nether", 0xe5b81b);
        setHabitatBiome(Biomes.HELL);

        setImmunity(DamageSource.MAGIC);
        setImmunity(DamageSource.HOT_FLOOR);
        setImmunity(DamageSource.LIGHTNING_BOLT);
        setImmunity(DamageSource.WITHER);

    }

    @Override
    public void onEnable(EntityTameableDragon dragon) {
        dragon.getBrain().setAvoidsWater(true);
    }

    @Override
    public void onDisable(EntityTameableDragon dragon) {
        dragon.getBrain().setAvoidsWater(false);
    }

    @Override
    public void onDeath(EntityTameableDragon dragon) {

    }

    public SoundEvent getLivingSound(EntityTameableDragon dragon) {
        if (dragon.isHatchling()) {
            return ModSounds.ENTITY_DRAGON_HATCHLING_GROWL;
        } else {
            return ModSounds.ENTITY_NETHER_DRAGON_GROWL;
        }
    }

    //	@Override
    //	public boolean canChangeBreed() {
    //		return false;
    //	}

    @Override
    public void continueAndUpdateBreathingLegacy(World world, Vec3d origin, Vec3d endOfLook, BreathNodeP.Power power, EntityTameableDragon dragon) {
        dragon.getBreathHelperP().getBreathAffectedAreaNether().continueBreathingLegacy(world, origin, endOfLook, power, dragon);
        dragon.getBreathHelperP().getBreathAffectedAreaNether().updateTickLegacy(world);
    }

    @Override
    public void spawnBreathParticles(World world, BreathNodeP.Power power, int tickCounter, Vec3d origin, Vec3d endOfLook, EntityTameableDragon dragon) {
        dragon.getBreathHelperP().getEmitter().setBeamEndpoints(origin, endOfLook);
        dragon.getBreathHelperP().getEmitter().spawnBreathParticlesforNetherDragon(world, power, tickCounter);
    }

    @Override
    public void onLivingUpdate(EntityTameableDragon dragon) {
        World world=dragon.world;
        if (world instanceof WorldServer && dragon.isWet() && !dragon.isEgg()) {
            doParticles(dragon, EnumParticleTypes.SMOKE_NORMAL);
        }

        if (world instanceof WorldServer && !dragon.isDead && !dragon.isEgg()) {
            doParticles(dragon, EnumParticleTypes.DRIP_LAVA);
        }
    }

    @SideOnly(Side.CLIENT)
    private void doParticles(EntityTameableDragon dragon, EnumParticleTypes types) {
        if (!dragon.isEgg() && !dragon.isHatchling()) {
            float s = dragon.getScale(); //  * 1.2f
            for (double x1 = 0; x1 < s + 1; ++x1) {
                double x = dragon.posX + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;
                double y = dragon.posY + (rand.nextDouble() - 0.5) * dragon.height * s;
                double z = dragon.posZ + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;

                dragon.world.spawnParticle(types, x, y, z, 0, 0, 0);
            }
        }
    }


    //	@Override
    //	public boolean isInfertile() {
    //		return true;
    //	}

}
