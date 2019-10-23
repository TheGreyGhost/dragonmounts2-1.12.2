package com.TheRPGAdventurer.ROTD.common.entity.helper;

import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Created by TGG on 20/10/2019.
 */
public class DragonSoundsHelper extends DragonHelper {
  public DragonSoundsHelper(EntityTameableDragon dragon) {
    super(dragon);
    setCompleted(FunctionTag.CONSTRUCTOR);
  }

  public static void registerConfigurationTags()
  {
    // the initialisation of the tags is all done in their static initialisers
    //    DragonVariants.addVariantTagValidator(new DragonReproductionValidator());
  }

  @Override
  public void writeToNBT(NBTTagCompound nbt) {
    checkPreConditions(FunctionTag.WRITE_TO_NBT);
    setCompleted(FunctionTag.WRITE_TO_NBT);
  }

  @Override
  public void readFromNBT(NBTTagCompound nbt) {
    checkPreConditions(FunctionTag.READ_FROM_NBT);
    setCompleted(FunctionTag.READ_FROM_NBT);
  }

  @Override
  public void registerDataParameters() {
    checkPreConditions(FunctionTag.REGISTER_DATA_PARAMETERS);
    setCompleted(FunctionTag.REGISTER_DATA_PARAMETERS);
  }

  @Override
  public void initialiseServerSide() {
    checkPreConditions(FunctionTag.INITIALISE_SERVER);
    setCompleted(FunctionTag.INITIALISE_SERVER);

  }

  @Override
  public void initialiseClientSide() {
    checkPreConditions(FunctionTag.INITIALISE_CLIENT);
    setCompleted(FunctionTag.INITIALISE_CLIENT);

  }

  @Override
  public void onConfigurationChange() {
    throw new NotImplementedException("onConfigurationChange()");
  }

  @Override
  public void onLivingUpdate() {
    checkPreConditions(FunctionTag.VANILLA);
    if (getRNG().nextInt(800) == 1) roar();
  }

  /**
   * Returns the sound this mob makes when it is hurt.
   */
  @Override
  public SoundEvent getHurtSound(DamageSource src) {
    return getBreed().getHurtSound();
  }

  public SoundEvent getWingsSound() {
    return getBreed().getWingsSound();
  }

  public SoundEvent getStepSound() {
    return getBreed().getStepSound();
  }

  public SoundEvent getEatSound() {
    return getBreed().getEatSound();
  }

  public SoundEvent getAttackSound() {
    return getBreed().getAttackSound();
  }

  public void playLivingSound() {
    SoundEvent sound = getLivingSound();
    if (sound == null || isUsingBreathWeapon()) {
      return;
    }

    playSound(sound, 0.7f, 1);

  }

  /**
   * Get number of ticks, at least during which the living entity will be silent.
   */
  public int getTalkInterval() {
    return 240;
  }

  /**
   * Client side method for wing animations. Plays wing flapping sounds.
   *
   * @param speed wing animation playback speed
   */
  public void onWingsDown(float speed) {
    if (!isInWater() && isFlying()) {
      // play wing sounds
      float pitch = (1);
      float volume = 1f + (1 - speed);
      playSound(getWingsSound(), volume, pitch, false);
    }
  }

  public void playStepSound(BlockPos entityPos, Block block) {
    // no sounds for  underwater action
    if (isInWater() || isOverWater()) return;

    if (isFlying() || isSitting()) return;

    SoundEvent stepSound;
    // baby has quiet steps, larger have stomping sound
    if (isBaby()) {
      SoundType soundType;
      // override sound type if the top block is snowy
      if (world.getBlockState(entityPos.up()).getBlock() == Blocks.SNOW_LAYER)
        soundType = Blocks.SNOW_LAYER.getSoundType();
      else
        soundType = block.getSoundType();
      stepSound = soundType.getStepSound();
    } else {
      stepSound = getStepSound();
    }
    playSound(stepSound, 1f, 1f, false);
  }

  public void playSound(SoundEvent sound, float volume, float pitch, boolean local) {
    if (sound == null || isSilent()) {
      return;
    }

    volume *= getVolume(sound);
    pitch *= getSoundPitch();

    if (local) world.playSound(posX, posY, posZ, sound, getSoundCategory(), volume, pitch, false);
    else world.playSound(null, posX, posY, posZ, sound, getSoundCategory(), volume, pitch);
  }

  /**
   * Returns the volume for a sound to play.
   */
  public float getVolume(SoundEvent sound) {
    return MathX.clamp(getAgeScale(), 0, 1.0F);
  }

  /**
   * Returns the sound this mob makes on death.
   */
  public SoundEvent getDeathSound() {
    return this.getBreed().getDeathSound();
  }

  /**
   * Returns the sound this mob makes on swimming.
   *
   * @TheRPGAdenturer: disabled due to its annoyance while swimming underwater it
   * played too many times
   */

  public SoundEvent getSwimSound() {
    return null;
  }

//  public DragonBreathHelper getBreathHelper() {
//    return getHelper(DragonBreathHelper.class);
//  }

  public void roar() {
    if (!isDead && getBreed().getRoarSoundEvent(this) != null && !isUsingBreathWeapon()) {
      this.roarTicks = 0; // MathX.clamp(getAgeScale(), 0.88f
      world.playSound(posX, posY, posZ, getBreed().getRoarSoundEvent(this), SoundCategory.NEUTRAL, MathX.clamp(getAgeScale(), 0.4F, 1.0F), getSoundPitch(), true);
      // sound volume should be between 0 - 1, and scale is also 0 - 1
    }
  }



}
