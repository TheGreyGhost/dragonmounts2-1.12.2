package com.TheRPGAdventurer.ROTD.common.entity.breath.weapons;

import com.TheRPGAdventurer.ROTD.common.entity.breath.BreathAffectedBlock;
import com.TheRPGAdventurer.ROTD.common.entity.breath.BreathAffectedEntity;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAreaEffectCloud;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
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
 */
public class BreathWeaponPoison extends BreathWeapon {

  public BreathWeaponPoison(EntityTameableDragon i_dragon) {
    super(i_dragon);
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

    if (!world.isRemote) {  //todo: this might generate a large amount of EntityAreaEffectClouds; might need to reduce it/ check for existing clouds nearby
      EntityAreaEffectCloud entityareaeffectcloud = new EntityAreaEffectCloud(world, blockPos.getX(), blockPos.getY(), blockPos.getZ());
      entityareaeffectcloud.setOwner(this.dragon);
      entityareaeffectcloud.setRadius(1.3F);
      entityareaeffectcloud.setDuration(600);
      entityareaeffectcloud.setRadiusPerTick((1.0F - entityareaeffectcloud.getRadius()) / (float) entityareaeffectcloud.getDuration());
      entityareaeffectcloud.addEffect(new PotionEffect(MobEffects.POISON, poisonDuration));

      entityareaeffectcloud.setPosition(blockPos.getX(), blockPos.getY(), blockPos.getZ());
      if (!block.isAir(iBlockState, world, blockPos) && rand.nextInt(500) == 1) {
        world.spawnEntity(entityareaeffectcloud);
      }
    }
    return new BreathAffectedBlock();  // reset to zero
  }

  /**
   * if the hitDensity is high enough, manipulate the entity (eg set fire to it, damage it)
   * A dragon can't be damaged by its own breathweapon;
   *
   * @param world
   * @param entityID          the ID of the affected entity
   * @param currentHitDensity the hit density
   * @return the updated hit density; null if the entity is dead, doesn't exist, or otherwise not affected
   */
  public BreathAffectedEntity affectEntity(World world, Integer entityID, BreathAffectedEntity currentHitDensity) {
    checkNotNull(world);
    checkNotNull(entityID);
    checkNotNull(currentHitDensity);

    Entity entityAffected = world.getEntityByID(entityID);
    if (isImmuneToBreath(entityAffected)) return null;

    final float POISON_DAMAGE_PER_HIT_DENSITY = 1F;
    float hitDensity = currentHitDensity.getHitDensity();
    final float DAMAGE_PER_HIT_DENSITY = POISON_DAMAGE_PER_HIT_DENSITY * hitDensity;

    entityAffected.attackEntityFrom(DamageSource.causeMobDamage(dragon), DAMAGE_PER_HIT_DENSITY);

    this.xp(entityAffected);

    return currentHitDensity;
  }
  private int poisonDuration = 10 * 10;

}
