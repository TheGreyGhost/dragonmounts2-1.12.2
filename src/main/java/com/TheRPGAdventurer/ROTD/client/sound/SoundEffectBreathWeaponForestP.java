package com.TheRPGAdventurer.ROTD.client.sound;


import com.TheRPGAdventurer.ROTD.common.entity.helper.DragonLifeStage;

/**
 * Created by TheGreyGhost on 24/6/16.
 * Sounds effects for the dragon's mouth for ice
 */
public class SoundEffectBreathWeaponForestP extends SoundEffectBreathWeaponP {
  public SoundEffectBreathWeaponForestP(SoundController i_soundController,
                                        WeaponSoundUpdateLink i_weaponSoundUpdateLink) {
    super(i_soundController, i_weaponSoundUpdateLink);
  }

  /**
   * Returns the sound for the given breed, lifestage, and sound part
   *
   * @param soundPart which part of the breathing sound?
   * @param lifeStage how old is the dragon?
   * @return the resourcelocation corresponding to the desired sound
   */
  @Override
  protected SoundEffectName weaponHeadSound(SoundPart soundPart, DragonLifeStage lifeStage) {
//    final SoundEffectName hatchling[] = {SoundEffectName.HATCHLING_BREATHE_FIRE_START,
//                                          SoundEffectName.HATCHLING_BREATHE_FIRE_LOOP,
//                                          SoundEffectName.HATCHLING_BREATHE_FIRE_STOP};
//
//    final SoundEffectName juvenile[] = {SoundEffectName.JUVENILE_BREATHE_FIRE_START,
//                                          SoundEffectName.JUVENILE_BREATHE_FIRE_LOOP,
//                                          SoundEffectName.JUVENILE_BREATHE_FIRE_STOP};
//
//    final SoundEffectName adult[] = {SoundEffectName.ADULT_BREATHE_FIRE_START,
//                                      SoundEffectName.ADULT_BREATHE_FIRE_LOOP,
//                                      SoundEffectName.ADULT_BREATHE_FIRE_STOP};

    SoundEffectName soundEffectNames;
    switch (soundPart) {
      case START: {
        soundEffectNames = SoundEffectName.BREATHE_FOREST_START;
        break;
      }
      case LOOP: {
        soundEffectNames = SoundEffectName.BREATHE_FOREST_LOOP;
        break;
      }
      case STOP: {
        soundEffectNames = SoundEffectName.BREATHE_FOREST_STOP;
        break;
      }
      default: {
        throw new IllegalArgumentException("Unknown SoundPart:" + soundPart);
      }
    }
    return soundEffectNames;
  }


}
