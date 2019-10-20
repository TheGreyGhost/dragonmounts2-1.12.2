package com.TheRPGAdventurer.ROTD.common.entity.helper;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.EnumDragonBreed;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;

import java.util.Optional;

/**
 * Created by TGG on 20/10/2019.
 */
public class DragonCombatHelper extends DragonHelper {
  public DragonCombatHelper(EntityTameableDragon dragon) {
    super(dragon);
  }

  @Override
  public void writeToNBT(NBTTagCompound nbt) {

  }

  @Override
  public void readFromNBT(NBTTagCompound nbt) {

  }

  @Override
  public void registerDataParameters() {

  }

  @Override
  public void initialiseServerSide() {

  }

  @Override
  public void initialiseClientSide() {

  }

  @Override
  public void notifyDataManagerChange(DataParameter<?> key) {

  }

  @Override
  public void onLivingUpdate() {
    if (this.ticksExisted % (DragonMounts.instance.getConfig().hungerDecrement) == 1) {
      if (this.getHunger() > 0) {
        this.setHunger(this.getHunger() - 1);
      }
    }

    if (this.isPotionActive(MobEffects.WEAKNESS)) {
      this.removePotionEffect(MobEffects.WEAKNESS);
    }


  }

  public EnumCreatureAttribute getCreatureAttribute() {
    return getBreed().getCreatureAttribute();
  }

  public boolean attackEntityAsMob(Entity entityIn) {
    boolean attacked = entityIn.attackEntityFrom(DamageSource.causeMobDamage(this), (float) getEntityAttribute(ATTACK_DAMAGE).getAttributeValue());

    if (attacked) {
      applyEnchantments(this, entityIn);
    }

    if (!this.nothing()) {
      return false;
    }

//    if (getBreedType() == EnumDragonBreed.WITHER) {
//      ((EntityLivingBase) entityIn).addPotionEffect(new PotionEffect(MobEffects.WITHER, 200));
//    }

    return attacked;
  }

  /**
   * 1 equals iron 2 equals gold 3 equals diamond 4 equals emerald
   *
   * @return 0 no armor
   */
  public double getArmorResistance() {
    switch (getArmor()) {
      case 1:
        return 1.4;
      case 2:
        return 1.2;
      case 3:
        return 1.7;
      case 4:
        return 1.4;
      default:
        return 0;
    }
  }

  public boolean isEntityInvulnerable(DamageSource src) {
    Entity srcEnt = src.getImmediateSource();
    if (srcEnt != null) {
      // ignore own damage
      if (srcEnt == this) {
        return true;
      }

      // ignore damage from riders
      if (isPassenger(srcEnt)) {
        return true;
      }
    }

    return getBreed().isImmuneToDamage(src);
  }

  /**
   * Returns the entity's health relative to the maximum health.
   *
   * @return health normalized between 0 and 1
   */
  public double getHealthRelative() {
    return getHealth() / (double) getMaxHealth();
  }

  public void setImmuneToFire(boolean isImmuneToFire) {
    L.trace("setImmuneToFire({})", isImmuneToFire);
    this.isImmuneToFire = isImmuneToFire;
  }

  public void setAttackDamage(double damage) {
    L.trace("setAttackDamage({})", damage);
    getEntityAttribute(ATTACK_DAMAGE).setBaseValue(damage);
  }


  public int getHunger() {
    return dataManager.get(HUNGER);
  }

  public void setHunger(int hunger) {
    this.dataManager.set(HUNGER, Math.min(100, hunger));
  }

  /**
   * Called when a lightning bolt hits the entity.
   */
  @Override
  public void onStruckByLightning(EntityLightningBolt lightningBolt) {
    EnumDragonBreed currentType = getBreedType();
    super.onStruckByLightning(lightningBolt);
//    if (currentType == EnumDragonBreed.SKELETON) {
//      this.setBreedType(EnumDragonBreed.WITHER);
//
//      this.playSound(SoundEvents.BLOCK_PORTAL_TRIGGER, 2, 1);
//      this.playSound(SoundEvents.BLOCK_END_PORTAL_SPAWN, 2, 1);
//    }
//
//    if (currentType == EnumDragonBreed.SYLPHID) {
//      this.setBreedType(EnumDragonBreed.STORM);
//
//      this.playSound(SoundEvents.BLOCK_PORTAL_TRIGGER, 2, 1);
//      this.playSound(SoundEvents.BLOCK_END_PORTAL_SPAWN, 2, 1);
//    }

    addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 35 * 20));
  }

  /**
   * Checks if the dragon's health is not full and not zero.
   */
  public boolean shouldHeal() {
    return this.getHealth() > 0.0F && this.getHealth() < this.getMaxHealth();
  }

  public Optional<Boolean> shouldAttackEntity(EntityLivingBase target, EntityLivingBase owner) {
    if (!target.isChild()) {
      if (target instanceof EntityTameable) {
        EntityTameable tamedEntity = (EntityTameable) target;
        if (tamedEntity.isTamed()) {
          return false;
        }
      }

      if (target instanceof EntityPlayer) {
        EntityPlayer playertarget = (EntityPlayer) target;
        if (this.isTamedFor(playertarget)) {
          return false;
        }
      }

      if (target.hasCustomName()) {
        return false;
      }

    }

    return super.shouldAttackEntity(target, owner);
  }

  public Optional<Boolean> attackEntityFrom(DamageSource source, float damage) {
    Entity sourceEntity = source.getTrueSource();

    if (source != DamageSource.IN_WALL) {
      // don't just sit there!
      this.aiSit.setSitting(false);
    }
    //        if(!sourceEntity.onGround && sourceEntity != null) this.setFlying(true);

    if (this.isBeingRidden() && source.getTrueSource() != null && source.getTrueSource().isPassenger(source.getTrueSource()) && damage < 1) {
      return false;
    }

    if (!world.isRemote && source.getTrueSource() != null && this.getRNG().nextInt(4) == 0) {
      this.roar();
    }

    if (isBaby() && isJumping) {
      return false;
    }

    if (this.isPassenger(sourceEntity)) {
      return false;
    }

    //when killed with damage greater than 17 cause the game to crash
    if (damage >= 17 && (source != DamageSource.GENERIC || source != DamageSource.OUT_OF_WORLD)) {
      return damage == 8.0f;
    }


    float damageReduction = (float) getArmorResistance() + 3.0F;
    if (getArmorResistance() != 0) {
      damage -= damageReduction;
    }

    return super.attackEntityFrom(source, damage);
  }



  private static final DataParameter<Integer> HUNGER = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.VARINT);

  public boolean isImmuneToDamage(DamageSource dmg) {
    if (immunities.isEmpty()) {
      return false;
    }

    return immunities.contains(dmg.damageType);
  }



}
