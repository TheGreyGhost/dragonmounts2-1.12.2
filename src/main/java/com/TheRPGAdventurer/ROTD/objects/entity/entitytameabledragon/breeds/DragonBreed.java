package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breeds;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.render.dragon.breathweaponFX.BreathWeaponFXEmitter;
import com.TheRPGAdventurer.ROTD.client.render.dragon.breathweaponFX.BreathWeaponFXEmitterFire;
import com.TheRPGAdventurer.ROTD.inits.ModSounds;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.nodes.BreathNodeFactory;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.nodes.BreathNodeFire;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.nodes.BreathNodeP;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.nodes.BreathProjectileFactory;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.sound.*;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.weapons.BreathWeaponFireP;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.weapons.BreathWeaponP;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.helper.DragonLifeStage;

import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.helper.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Base class for dragon breeds.
 */
public abstract class DragonBreed {

    private final String skin;
    private final int color;
    private final Set<String> immunities=new HashSet<>();
    private final Set<Block> breedBlocks=new HashSet<>();
    private final Set<Biome> biomes=new HashSet<>();
    protected final Random rand=new Random();
    public static SoundEffectNames[] soundEffectNames;

    DragonBreed(String skin, int color) {
        this.skin=skin;
        this.color=color;

        // ignore suffocation damage
        setImmunity(DamageSource.DROWN);
        setImmunity(DamageSource.IN_WALL);
        setImmunity(DamageSource.ON_FIRE);
        setImmunity(DamageSource.IN_FIRE);
        setImmunity(DamageSource.LAVA);
        setImmunity(DamageSource.HOT_FLOOR);
        setImmunity(DamageSource.CACTUS); // assume that cactus needles don't do much damage to animals with horned scales
        setImmunity(DamageSource.DRAGON_BREATH); // ignore damage from vanilla ender dragon. I kinda disabled this because it wouldn't make any sense, feel free to re enable
    }

    public String getSkin() {
        return skin;
    }

    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.UNDEFINED;
    }

    public int getColor() {
        return color;
    }

    public float getColorR() {
        return ((color >> 16) & 0xFF) / 255f;
    }

    public float getColorG() {
        return ((color >> 8) & 0xFF) / 255f;
    }

    public float getColorB() {
        return (color & 0xFF) / 255f;
    }

    protected final void setImmunity(DamageSource dmg) {
        immunities.add(dmg.damageType);
    }

    public boolean isImmuneToDamage(DamageSource dmg) {
        if (immunities.isEmpty()) {
            return false;
        }

        return immunities.contains(dmg.damageType);
    }

    protected final void setHabitatBlock(Block block) {
        breedBlocks.add(block);
    }

    public boolean isHabitatBlock(Block block) {
        return breedBlocks.contains(block);
    }

    protected final void setHabitatBiome(Biome biome) {
        biomes.add(biome);
    }

    public boolean isHabitatBiome(Biome biome) {
        return biomes.contains(biome);
    }

    public boolean isHabitatEnvironment(EntityTameableDragon dragon) {
        return false;
    }

    public static Item[] getFoodItems() {
        return new Item[]{Items.FISH, Items.PORKCHOP, Items.BEEF, Items.CHICKEN, Items.ROTTEN_FLESH, Items.RABBIT, Items.COOKED_FISH, Items.COOKED_BEEF, Items.COOKED_CHICKEN, Items.COOKED_MUTTON, Items.COOKED_RABBIT, Items.COOKED_FISH, Items.MUTTON, Items.COOKED_PORKCHOP, Items.RABBIT_STEW};
    }

    public Item[] getShrinkingFood() {
        return new Item[]{Items.POISONOUS_POTATO};
    }

    public Item[] getGrowingFood() {
        return new Item[]{Items.CARROT};
    }

    public Item getBreedingItem() {
        return Items.RABBIT_STEW;
    }

    public void onUpdate(EntityTameableDragon dragon) {
        placeFootprintBlocks(dragon);
    }

    protected void placeFootprintBlocks(EntityTameableDragon dragon) {
        // only apply on server
        if (!dragon.isServer()) {
            return;
        }

        // only apply on adult dragons that don't fly
        if (!dragon.isAdult() || dragon.isFlying()) {
            return;
        }

        // only apply if footprints are enabled
        float footprintChance=getFootprintChance();
        if (footprintChance==1) {
            return;
        }

        // footprint loop, from EntitySnowman.onLivingUpdate with slight tweaks
        World world=dragon.world;
        for (int i=0; i < 4; i++) {
            // place only if randomly selected
            if (world.rand.nextFloat() > footprintChance) {
                continue;
            }

            // get footprint position
            double bx=dragon.posX + (i % 2 * 2 - 1) * 0.25;
            double by=dragon.posY + 0.5;
            double bz=dragon.posZ + (i / 2 % 2 * 2 - 1) * 0.25;
            BlockPos pos=new BlockPos(bx, by, bz);

            // footprints can only be placed on empty space
            if (world.isAirBlock(pos)) {
                continue;
            }

            placeFootprintBlock(dragon, pos);
        }
    }

    protected void placeFootprintBlock(EntityTameableDragon dragon, BlockPos blockPos) {

    }

    protected float getFootprintChance() {
        return 1;
    }

    public abstract void onEnable(EntityTameableDragon dragon);

    public abstract void onDisable(EntityTameableDragon dragon);

    public abstract void onDeath(EntityTameableDragon dragon);

    public SoundEvent getLivingSound(EntityTameableDragon dragon) {
        if (dragon.isBaby()) {
            return ModSounds.ENTITY_DRAGON_HATCHLING_GROWL;
        } else {
            if (rand.nextInt(3)==0) {
                return ModSounds.ENTITY_DRAGON_GROWL;
            } else {
                return ModSounds.ENTITY_DRAGON_BREATHE;
            }
        }
    }

    public SoundEvent getRoarSoundEvent(EntityTameableDragon dragon) {
        return dragon.isBaby() ? ModSounds.HATCHLING_DRAGON_ROAR : ModSounds.DRAGON_ROAR;
    }

    public SoundEvent getHurtSound() {
        return SoundEvents.ENTITY_ENDERDRAGON_HURT;
    }

    public SoundEvent getDeathSound() {
        return ModSounds.ENTITY_DRAGON_DEATH;
    }

    public SoundEvent getWingsSound() {
        return SoundEvents.ENTITY_ENDERDRAGON_FLAP;
    }

    public SoundEvent getStepSound() {
        return ModSounds.ENTITY_DRAGON_STEP;
    }

    public SoundEvent getEatSound() {
        return SoundEvents.ENTITY_GENERIC_EAT;
    }

    public SoundEvent getAttackSound() {
        return SoundEvents.ENTITY_GENERIC_EAT;
    }

    public float getSoundPitch(SoundEvent sound) {
        return 1;
    }

    public float getSoundVolume(SoundEvent sound) {
        return 1.5f;
    }

    public boolean canChangeBreed() {
        return true;
    }

    public boolean canUseBreathWeapon() {
        return true;
    }

    @Deprecated
    abstract public void continueAndUpdateBreathingLegacy(World world, Vec3d origin, Vec3d endOfLook, BreathNodeP.Power power, EntityTameableDragon dragon);

    @Deprecated
    abstract public void spawnBreathParticles(World world, BreathNodeP.Power power, int tickCounter, Vec3d origin, Vec3d endOfLook, EntityTameableDragon dragon);

    @Deprecated
    public SoundEffectNames[] getBreathWeaponSoundEffects(DragonLifeStage stage) {
      final SoundEffectNames prejuvenile[]={SoundEffectNames.HATCHLING_BREATHE_FIRE_START, SoundEffectNames.HATCHLING_BREATHE_FIRE_LOOP, SoundEffectNames.HATCHLING_BREATHE_FIRE_STOP};

      final SoundEffectNames juvenile[]={SoundEffectNames.JUVENILE_BREATHE_FIRE_START, SoundEffectNames.JUVENILE_BREATHE_FIRE_LOOP, SoundEffectNames.JUVENILE_BREATHE_FIRE_STOP};

      final SoundEffectNames adult[]={SoundEffectNames.ADULT_BREATHE_FIRE_START, SoundEffectNames.ADULT_BREATHE_FIRE_LOOP, SoundEffectNames.ADULT_BREATHE_FIRE_STOP};

      switch (stage) {
        case EGG:
        case INFANT:
        case HATCHLING:
          break;

        case PREJUVENILE:
          soundEffectNames=prejuvenile;
          break;

        case JUVENILE:
          soundEffectNames=juvenile;
          break;

        case ADULT:
          soundEffectNames=adult;
          break;

        default:
          DragonMounts.loggerLimit.error_once("Invalid life stage:" + stage);
          break;
      }

      return soundEffectNames;
    }

    public void onLivingUpdate(EntityTameableDragon dragon) {

    }

//    /**
//     * creates a SoundEffectBreathWeapon that creates the sound from the dragon's mouth when breathing
//     *
//     * @return
//     */
//    @Deprecated
//    public SoundEffectBreathWeapon getSoundEffectBreathWeapon(SoundController i_soundController, SoundEffectBreathWeapon.WeaponSoundUpdateLinkLegacy i_weaponSoundUpdateLink) {
//
//        return soun;
//    }

    public boolean isInfertile() {
        return false;
    }

    public SoundEvent getSneezeSound() {
        return ModSounds.DRAGON_SNEEZE;
    }

    public EnumParticleTypes getSneezeParticle() {
        return EnumParticleTypes.SMOKE_LARGE;
    }

  public enum BreathWeaponSpawnType {PROJECTILE, NODES}
  // PROJECTILE = spawn a single Entity, similar to EntityFIreball for ghast
  // NODES = continuous stream of small nodes

   public BreathWeaponSpawnType getBreathWeaponSpawnType(EntityTameableDragon dragon)
    {
      return BreathWeaponSpawnType.NODES;
//      throw new UnsupportedOperationException();
    }

  /** return a new Breath Weapon FX Emitter based on breed
   * @return
   */
  public BreathWeaponFXEmitter getBreathWeaponFXEmitter(EntityTameableDragon dragon)
  {
    return new BreathWeaponFXEmitterFire();   // todo just for now to support legacy
//    throw new UnsupportedOperationException();
  }

  /** return a new BreathWeapon based on breed
   * @return
   */
  public BreathWeaponP getBreathWeapon(EntityTameableDragon dragon)  //todo make abstract
    {
      return new BreathWeaponFireP(dragon); // todo just dummy for now to support legacy
//        throw new UnsupportedOperationException();
    }

  public BreathNodeFactory getBreathNodeFactory(EntityTameableDragon dragon)
  {
    return new BreathNodeFire.BreathNodeFireFactory(); // todo just dummy for now to support legacy
  }

  public BreathProjectileFactory getBreathProjectileFactory(EntityTameableDragon dragon)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * returns the range of the breath weapon
   * @param dragonLifeStage
   * @return  minimum range, maximum range, in blocks
   */
  public Pair<Float, Float> getBreathWeaponRange(DragonLifeStage dragonLifeStage)
  {
    return getBreathWeaponRangeDefault(dragonLifeStage);
  }

  /**
   * creates a SoundEffectBreathWeapon that creates the sound from the dragon's mouth when breathing
   * @return
   */
  public SoundEffectBreathWeaponP getSoundEffectBreathWeapon(SoundController i_soundController,
                                                            SoundEffectBreathWeaponP.WeaponSoundUpdateLink i_weaponSoundUpdateLink) {
    return new SoundEffectBreathWeaponNull(i_soundController, i_weaponSoundUpdateLink);
  }

  private Pair<Float, Float> getBreathWeaponRangeDefault(DragonLifeStage dragonLifeStage) {
    float minAttackRange = 1.0F;
    float maxAttackRange = 1.0F;
    switch (dragonLifeStage) {
      case EGG:
        break;
      case HATCHLING: {
        minAttackRange = 2.0F;
        maxAttackRange = 4.0F;
        break;
      }
      case JUVENILE: {
        minAttackRange = 3.0F;
        maxAttackRange = 8.0F;
        break;
      }
      case ADULT: {
        minAttackRange = 5.0F;
        maxAttackRange = 25.0F;
        break;
      }
      default: {
        DragonMounts.loggerLimit.error_once("Unknown lifestage:" + dragonLifeStage);
        break;
      }
    }
    return new Pair<Float, Float>(minAttackRange, maxAttackRange);
  }

  /**
   * returns the width and height of the entity when it's an adult
   * later: may vary for different breeds
   * @return a pair of [width, height] for the entity - affects the Axis Aligned Bounding Box
   */
  public Pair<Float, Float> getAdultEntitySize()
  {
    return new Pair<>(4.8F, 4.2F);
//    public static final float BASE_WIDTH = 4.8f; //2.4f;      make the adult twice the size it used to be
//    public static final float BASE_HEIGHT = 4.2F; //2.1f;      make the adult twice the size it used to be
  }

  /**
   * used when rendering; scale up the model by this factor for a fully-grown adult
   * @return the relative scale factor (1.0 = no scaling)
   */
  public float getAdultModelRenderScaleFactor()
  {
    return 1.6F;  // I don't know why this is the magic number 1.6, it just gives the right size
  }

  /**
   * used for converting the model dimensions into world dimensions (see DragonHeadPositionHelper)
   * I don't know why this is the magic # of 0.1F
   * It's probably linked to getAdultModelRenderScaleFactor
   * @return
   */
  public float getAdultModelScaleFactor() {
    return 0.1F;
  }

  /**
   * gets the position offset to use for a passenger on a fully-grown adult dragon
   * @param isSitting is the dragon sitting down?
   * @param passengerNumber the number (0.. max) of the passenger
   * @return the [x, y, z] of the mounting position relative to the dragon [posX, posY, posZ]
   * for smaller-than-adult sizes, multiply by
   */
  public Vec3d getAdultMountedPositionOffset(boolean isSitting, int passengerNumber)
  {
    double yoffset = (isSitting ? 3.4 : 4.4);

    // dragon position is the middle of the model and the saddle is on
    // the shoulders, so move player forwards on Z axis relative to the
    // dragon's rotation to fix that
    //

    switch (passengerNumber) {
      case 0: return new Vec3d(   0, yoffset, 2.2);
      case 1: return new Vec3d(+0.6, yoffset, 1.5);
      case 2: return new Vec3d(-0.6, yoffset, 1.5);
    }
    DragonMounts.loggerLimit.error_once("Illegal passengerNumber:" + passengerNumber);
    return new Vec3d(0, yoffset, 2.2);
  }

  /** how many passengers can ride on this breed?
   * @return
   */
  public int getMaxNumberOfPassengers(DragonLifeStage dragonLifeStage)
  {
    return 3;
  }

  // what is the relative eye height of this dragon?
  // todo: move to headpositionhelper
  public float getRelativeEyeHeight(boolean isSitting)
  {
    return isSitting ? 0.8F : 0.85F;
  }
}

