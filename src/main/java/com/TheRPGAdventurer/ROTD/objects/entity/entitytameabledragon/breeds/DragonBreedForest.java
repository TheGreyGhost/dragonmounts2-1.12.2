package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breeds;

import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.nodes.BreathNodeP;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockSapling;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;;


public class DragonBreedForest extends DragonBreed {

    DragonBreedForest() {
        super("forest", 0x298317);
        
        setImmunity(DamageSource.MAGIC);
        setImmunity(DamageSource.HOT_FLOOR);
        setImmunity(DamageSource.LIGHTNING_BOLT);
        setImmunity(DamageSource.WITHER);
        
        setHabitatBlock(Blocks.YELLOW_FLOWER);
        setHabitatBlock(Blocks.RED_FLOWER);
        setHabitatBlock(Blocks.MOSSY_COBBLESTONE);
        setHabitatBlock(Blocks.VINE);
        setHabitatBlock(Blocks.SAPLING);
        setHabitatBlock(Blocks.LEAVES);
        setHabitatBlock(Blocks.LEAVES2);
        
        setHabitatBiome(Biomes.JUNGLE);
        setHabitatBiome(Biomes.JUNGLE_HILLS);
        
    }

    @Override
    public void onEnable(EntityTameableDragon dragon) {}

    @Override
    public void onDisable(EntityTameableDragon dragon) {}

    @Override
    public void onDeath(EntityTameableDragon dragon) {}

    @Override
    public void onLivingUpdate(EntityTameableDragon dragon) {
        if(dragon.isSheared()) {
            net.minecraft.entity.item.EntityItem ent = dragon.entityDropItem(new ItemStack(
                    Item.getItemFromBlock(Blocks.SAPLING.getBlockState().getBaseState().withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.OAK).getBlock())), 1.0F);
            ent.motionY += rand.nextFloat() * 0.05F;
            ent.motionX += (rand.nextFloat() - rand.nextFloat()) * 0.1F;
            ent.motionZ += (rand.nextFloat() - rand.nextFloat()) * 0.1F;
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
	
