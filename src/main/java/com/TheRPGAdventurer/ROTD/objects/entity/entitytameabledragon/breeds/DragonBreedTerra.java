package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breeds;

import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.nodes.BreathNodeP;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary;

public class DragonBreedTerra extends DragonBreed {

    DragonBreedTerra() {
        super("terra", 0Xa56c21);

        setHabitatBiome(Biomes.MESA);
        setHabitatBiome(Biomes.MESA_ROCK);
        setHabitatBiome(Biomes.MESA_CLEAR_ROCK);
        setHabitatBiome(Biomes.MUTATED_MESA_CLEAR_ROCK);
        setHabitatBiome(Biomes.MUTATED_MESA_ROCK);
        setHabitatBlock(Blocks.HARDENED_CLAY);
        setHabitatBlock(Blocks.SAND);
        setHabitatBlock(Blocks.SANDSTONE);
        setHabitatBlock(Blocks.SANDSTONE_STAIRS);
        setHabitatBlock(Blocks.RED_SANDSTONE);
        setHabitatBlock(Blocks.RED_SANDSTONE_STAIRS);

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
    public void onLivingUpdate(EntityTameableDragon dragon) {
        boolean isMesa=BiomeDictionary.hasType(dragon.world.getBiome(dragon.getPosition()), BiomeDictionary.Type.MESA);
        if (isMesa && dragon.posY > dragon.world.getHeight() + 8) doParticles(dragon);
    }

    private void doParticles(EntityTameableDragon dragon) {
        if (!dragon.isEgg() && !dragon.isBaby()) {
          float s=dragon.getAgeScale() * 1.2f;
            for (double x1=0; x1 < s; ++x1) {
                double x=dragon.posX + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;
                double y=dragon.posY + (rand.nextDouble() - 0.5) * dragon.height * s;
                double z=dragon.posZ + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;

                dragon.world.spawnParticle(EnumParticleTypes.FALLING_DUST, x, y - 1, z, 0, 0, 0, (dragon.isMale() ? 3 : 5));
            }
        }
    }

  @Override
  public void continueAndUpdateBreathingLegacy(World world, Vec3d origin, Vec3d endOfLook, BreathNodeP.Power power, EntityTameableDragon dragon) {
    dragon.getBreathHelperP().getBreathAffectedAreaFire().continueBreathingLegacy(world, origin, endOfLook, power, dragon);
    dragon.getBreathHelperP().getBreathAffectedAreaFire().updateTickLegacy(world);
  }

  @Override
  public void spawnBreathParticles(World world, BreathNodeP.Power power, int tickCounter, Vec3d origin, Vec3d endOfLook, EntityTameableDragon dragon) {
    dragon.getBreathHelperP().getEmitter().setBeamEndpoints(origin, endOfLook);
    dragon.getBreathHelperP().getEmitter().spawnBreathParticlesForFireDragon(world, power, tickCounter);
  }

}
