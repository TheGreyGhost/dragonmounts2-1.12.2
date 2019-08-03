package com.TheRPGAdventurer.ROTD.client.sound;

import com.TheRPGAdventurer.ROTD.common.entity.helper.DragonLifeStage;

/**
 * Created by TGG on 24/06/2016.
 */
public class SoundEffectProjectileNether extends SoundEffectProjectile {
  public SoundEffectProjectileNether(SoundController i_soundController, ProjectileSoundUpdateLink i_projectileSoundUpdateLink) {
    super(i_soundController, i_projectileSoundUpdateLink);
  }

  /**
   * Returns the sound of the projectile for the given breed, lifestage, and sound part
   *
   * @param soundPart which part of the breathing sound?
   * @param lifeStage how old is the dragon?
   * @return the resourcelocation corresponding to the desired sound.  null for none.
   */
  protected SoundEffectName projectileSound(SoundPart soundPart, DragonLifeStage lifeStage) {
    if (soundPart == SoundPart.SPAWN) return SoundEffectName.BREATHE_NETHER_SPAWN;
    if (soundPart == SoundPart.LOOP) return SoundEffectName.NETHER_PROJECTILE_LOOP;
    return SoundEffectName.SILENCE;
  }
}
