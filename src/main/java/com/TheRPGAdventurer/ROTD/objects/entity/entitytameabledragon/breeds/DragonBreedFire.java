/*
 ** 2013 October 24
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breeds;

import com.TheRPGAdventurer.ROTD.client.render.dragon.breathweaponFX.BreathWeaponFXEmitter;
import com.TheRPGAdventurer.ROTD.client.render.dragon.breathweaponFX.BreathWeaponFXEmitterFire;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.nodes.BreathNodeFactory;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.nodes.BreathNodeFire;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.sound.SoundController;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.sound.SoundEffectBreathWeapon;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.sound.SoundEffectBreathWeaponFire;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.sound.SoundEffectBreathWeaponP;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.weapons.BreathWeaponFireP;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.weapons.BreathWeaponP;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class DragonBreedFire extends DragonBreed {

    public DragonBreedFire() {
        super("fire",0x960b0f);
        
        setImmunity(DamageSource.MAGIC);
        setImmunity(DamageSource.HOT_FLOOR);
        setImmunity(DamageSource.LIGHTNING_BOLT);
        setImmunity(DamageSource.WITHER);
        
        setHabitatBlock(Blocks.FIRE);
        setHabitatBlock(Blocks.LIT_FURNACE);
        setHabitatBlock(Blocks.LAVA);
        setHabitatBlock(Blocks.FLOWING_LAVA);

    }

    @Override
    public void onEnable(EntityTameableDragon dragon) {
        dragon.getBrain().setAvoidsWater(true);
    }
    

/*    @Override
    public SoundEvent getLivingSound() {
       return SoundEvents.BLOCK_FIRE_AMBIENT;
    }
*/
    @Override
    public void onDisable(EntityTameableDragon dragon) {
        dragon.getBrain().setAvoidsWater(false);
   }

	@Override
	public void onDeath(EntityTameableDragon dragon) {}
	
	@Override
	public void onLivingUpdate(EntityTameableDragon dragon) {
		if(dragon.isInLava() || dragon.world.isMaterialInBB(dragon.getEntityBoundingBox().grow(-0.1, -0.4, -0.1), Material.FIRE)) doParticles(dragon);
	}
	
    @SideOnly(Side.CLIENT)
    private void doParticles(EntityTameableDragon dragon) {
        if (!dragon.isEgg() && !dragon.isHatchling()) {
	        float s = dragon.getScale() * 1.2f;
	        for (double x1 = 0; x1 < s + 1; ++x1) {
		        double x = dragon.posX + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;
		        double y = dragon.posY + (rand.nextDouble() - 0.5) * dragon.height * s;
		        double z = dragon.posZ + (rand.nextDouble() - 0.5) * (dragon.width - 0.65) * s;
		        
		        dragon.world.spawnParticle(EnumParticleTypes.FLAME, x, y, z, 0, 0, 0);
	        }
        }
    }

  /** return a new fire breathweapon FX emitter
   * @return
   */
  @Override
  public BreathWeaponFXEmitter getBreathWeaponFXEmitter(EntityTameableDragon dragon)
  {
    return new BreathWeaponFXEmitterFire();
  }

  @Override
  public BreathWeaponSpawnType getBreathWeaponSpawnType(EntityTameableDragon dragon)
  {
    return BreathWeaponSpawnType.NODES;
  }

  /** return a new BreathWeapon based on breed
   * @return
   */
  @Override
  public BreathWeaponP getBreathWeapon(EntityTameableDragon dragon)
  {
    return new BreathWeaponFireP(dragon);
  }

  @Override
  public BreathNodeFactory getBreathNodeFactory(EntityTameableDragon dragon)
  {
    return new BreathNodeFire.BreathNodeFireFactory();
  }

  @Override
  public SoundEffectBreathWeaponP getSoundEffectBreathWeapon(SoundController i_soundController,
                                                            SoundEffectBreathWeaponP.WeaponSoundUpdateLink i_weaponSoundUpdateLink) {
    return new SoundEffectBreathWeaponFire(i_soundController, i_weaponSoundUpdateLink);
  }

}
