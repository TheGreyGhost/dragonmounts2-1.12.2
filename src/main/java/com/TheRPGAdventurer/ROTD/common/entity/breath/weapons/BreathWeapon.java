package com.TheRPGAdventurer.ROTD.common.entity.breath.weapons;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.breath.BreathAffectedBlock;
import com.TheRPGAdventurer.ROTD.common.entity.breath.BreathAffectedEntity;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.inits.ModItems;
import com.TheRPGAdventurer.ROTD.util.DMUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

/**
 * Created by TGG on 5/08/2015.
 * <p>
 * Models the effects of a breathweapon on blocks and entities
 * Usage:
 * 1) Construct with a parent dragon
 * 2) affectBlock() to apply an area of effect to the given block (eg set fire to it)
 * 3) affectEntity() to apply an area of effect to the given entity (eg damage it)
 * <p>
 * Subclassed for each weapon type
 */
abstract public class BreathWeapon {

  public BreathWeapon(EntityTameableDragon i_dragon) {
    dragon = i_dragon;
  }

//    protected float ENDER_DAMAGE = 1.2F;
//    protected float HYDRO_DAMAGE = 1F;
//    protected float ICE_DAMAGE = 1F;
//    protected float POISON_DAMAGE = 1F;
//    protected float WITHER_DAMAGE = 1F;

  /**
   * if the hitDensity is high enough, manipulate the block (eg set fire to it)
   *
   * @param world
   * @param blockPosition     the world [x,y,z] of the block
   * @param currentHitDensity
   * @return the updated block hit density
   */
  abstract public BreathAffectedBlock affectBlock(World world, Vec3i blockPosition,
                                                  BreathAffectedBlock currentHitDensity);

  /**
   * if the hitDensity is high enough, manipulate the entity (eg set fire to it, damage it)
   * A dragon can't be damaged by its own breathweapon;
   * If the "orbholder immune" option is on, and the entity is a player holding a dragon orb, ignore damage.
   *
   * @param world
   * @param entityID          the ID of the affected entity
   * @param currentHitDensity the hit density
   * @return the updated hit density; null if entity dead, doesn't exist, or otherwise not affected
   */
  abstract public BreathAffectedEntity affectEntity(World world, Integer entityID, BreathAffectedEntity currentHitDensity);

  /**
   * check if the target entity is immune to the breath effects
   *
   * @param entityAffected
   * @return true for immune, false if susceptible
   */
  protected boolean isImmuneToBreath(Entity entityAffected) {
    if (entityAffected == null) return true;    // should never happen, but just in case...
    if (entityAffected == dragon) return true;  // doesn't affect itself
    if (entityAffected.isDead) return true;

    //if (!(entityAffected instanceof EntityLivingBase)) return true; //todo: consider making all non-living entities immune

    if (dragon.isBeingRidden() && dragon.isPassenger(entityAffected)) return true;  // don't affect rider
    if (dragon.getControllingPlayer() != null && entityAffected == dragon.getControllingPlayer())
      return true;  // don't affect controlling player

    if (entityAffected instanceof EntityPlayer) {
      EntityPlayer entityPlayer = (EntityPlayer) entityAffected;
      if (DragonMounts.instance.getConfig().isOrbHolderImmune()
              && DMUtils.hasEquipped(entityPlayer, ModItems.dragon_orb)) {
        return false;
      }
    }

    if (entityAffected instanceof EntityTameable) { // don't affect pets
      EntityTameable entityTameable = (EntityTameable) entityAffected;
      if (entityTameable.isTamed()) return true;
    }
    return false;
  }

  protected void xp(Entity entity) {
//        try {
//            ReflectionHelper.setPrivateValue(EntityLivingBase.class, (EntityLivingBase) entity, 100,
//                    "recentlyHit", "field_70718_bc");
//        } catch (Exception ex) {
//            DMUtils.getLogger().warn("Can't override XP", ex);
//        }
  }
  protected EntityTameableDragon dragon;
}
