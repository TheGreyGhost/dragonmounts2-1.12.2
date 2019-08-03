package com.TheRPGAdventurer.ROTD.common.entity.breath.weapons;

import com.TheRPGAdventurer.ROTD.client.gui.DragonMountsConfig;
import com.TheRPGAdventurer.ROTD.common.entity.breath.BreathAffectedBlock;
import com.TheRPGAdventurer.ROTD.common.entity.breath.BreathAffectedEntity;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.Random;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by TGG on 5/08/2015.
 * <p>
 * Models the effects of a breathweapon on blocks and entities
 * Usage:
 * 1) Construct with a parent dragon
 * 2) affectBlock() to apply an area of effect to the given block (eg set fire to it)
 * 3) affectEntity() to apply an area of effect to the given entity (eg damage it)
 * <p>
 * Currently does fire only.  Intended to be subclassed later on for different weapon types.
 */
public class BreathWeaponFire extends BreathWeapon {

  public BreathWeaponFire(EntityTameableDragon dragon) {
    super(dragon);
  }

  /**
   * if the hitDensity is high enough, manipulate the block (eg set fire to it)
   *
   * @param world
   * @param blockPosition     the world [x,y,z] of the block
   * @param currentHitDensity
   * @return the updated block hit density
   */
  public BreathAffectedBlock affectBlock(World world, Vec3i blockPosition, BreathAffectedBlock currentHitDensity) {
    checkNotNull(world);
    checkNotNull(blockPosition);
    checkNotNull(currentHitDensity);

    BlockPos blockPos = new BlockPos(blockPosition);
    IBlockState iBlockState = world.getBlockState(blockPos);
    Block block = iBlockState.getBlock();

    Random rand = new Random();

    // Flammable blocks: set fire to them once they have been exposed enough.

    for (EnumFacing facing : EnumFacing.values()) {
      BlockPos sideToIgnite = blockPos.offset(facing);
      if (FireEffectsOnBlocks.processFlammability(block, world, sideToIgnite, facing) > 0) {
        int flammability = FireEffectsOnBlocks.processFlammability(block, world, sideToIgnite, facing);
        float thresholdForIgnition = FireEffectsOnBlocks.convertFlammabilityToHitDensityThreshold(flammability);
        float densityOfThisFace = currentHitDensity.getHitDensity(facing);
        if (densityOfThisFace >= thresholdForIgnition && world.isAirBlock(sideToIgnite) && thresholdForIgnition != 0 && DragonMountsConfig.canFireBreathAffectBlocks) {
          final float MIN_PITCH = 0.8F;
          final float MAX_PITCH = 1.2F;
          final float VOLUME = 1.0F;
          world.playSound(sideToIgnite.getX() + 0.5, sideToIgnite.getY() + 0.5, sideToIgnite.getZ() + 0.5,
                  SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, VOLUME, MIN_PITCH + rand.nextFloat() * (MAX_PITCH - MIN_PITCH), false);
          final double PERCENT_CHANCE_OF_IGNITION = 22.0 / 2500.0;
          FireEffectsOnBlocks.burnBlocks(sideToIgnite, rand, PERCENT_CHANCE_OF_IGNITION, world);
        }
      }
    }

//    FireEffectsOnBlocks.BlockBurnProperties burnProperties = FireEffectsOnBlocks.getBurnProperties(iBlockState);
//    if (burnProperties.burnResult == null
//            || currentHitDensity.getMaxHitDensity() < burnProperties.threshold) {
//      return currentHitDensity;
//    }
//    world.setBlockState(blockPos, burnProperties.burnResult);

    return new BreathAffectedBlock();  // reset to zero - no cumulative effect
  }

  /**
   * if the hitDensity is high enough, manipulate the entity (eg set fire to it, damage it)
   * A dragon can't be damaged by its own breathweapon;
   *
   * @param world
   * @param entityID          the ID of the affected entity
   * @param currentHitDensity the hit density
   * @return the updated hit density; null if entity dead, doesn't exist, or otherwise not affected
   */
  @Override
  public BreathAffectedEntity affectEntity(World world, Integer entityID, BreathAffectedEntity currentHitDensity) {
    checkNotNull(world);
    checkNotNull(entityID);
    checkNotNull(currentHitDensity);

    Entity entityAffected = world.getEntityByID(entityID);
    if (isImmuneToBreath(entityAffected)) return null;

    final float CATCH_FIRE_THRESHOLD = 1.4F;
    final float BURN_SECONDS_PER_HIT_DENSITY = 1.0F;
    final float FIRE_DAMAGE_PER_HIT_DENSITY = 1F;

    float hitDensity = currentHitDensity.getHitDensity();
    final float damage = FIRE_DAMAGE_PER_HIT_DENSITY * hitDensity;
    MathX.clamp(hitDensity, 0, 2);

    this.xp(entityAffected);

    final int BURN_DURATION_SECONDS = 4;
    entityAffected.setFire(BURN_DURATION_SECONDS);
    entityAffected.attackEntityFrom(DamageSource.causeMobDamage(dragon), damage);

    return currentHitDensity;
  }

  /**
   * check if the target entity is immune to the breath effects
   * base checks plus additional check for fire resistance
   *
   * @param entityAffected
   * @return
   */
  protected boolean isImmuneToBreath(Entity entityAffected) {
    if (super.isImmuneToBreath(entityAffected)) return true;
    if (entityAffected instanceof EntityLivingBase) {
      EntityLivingBase entity1 = (EntityLivingBase) entityAffected;
      if (entity1.isPotionActive(MobEffects.FIRE_RESISTANCE)) {
        return true;
      }
    }
    if (entityAffected.isImmuneToFire()) return true;
    return false;
  }

}
