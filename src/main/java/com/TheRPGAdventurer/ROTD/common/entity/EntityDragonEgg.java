package com.TheRPGAdventurer.ROTD.common.entity;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.model.EggModels;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantsException;
import com.TheRPGAdventurer.ROTD.common.inits.ModSounds;
import com.TheRPGAdventurer.ROTD.util.ClientServerSynchronisedTickCount;
import com.TheRPGAdventurer.ROTD.util.debugging.DebugSettings;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializer;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.List;

/**
 * Created by TGG on 10/08/2019.
 *
 *  *   Usage:
 *   1) If spawning manually:
 *     a) EntityTameableDragon(world)
 *     b) either initialise(breed) or readEntityFromNBT(nbt)
 */
public class EntityDragonEgg extends Entity {

  public EntityDragonEgg(World worldIn) {
    super(worldIn);
    if (world.isRemote) {
      incubationTicksClient = new ClientServerSynchronisedTickCount(TICKS_SINCE_CREATION_UPDATE_INTERVAL);
      incubationTicksClient.reset(incubationTicksServer);
    } else {
      incubationTicksClient = null;
      incubationTicksServer = 0;
    }
  }

  public static void registerConfigurationTags() {
    // tags are initialised in static member variables
    DragonVariants.addVariantTagValidator(new EntityEggValidator());
    DataSerializers.registerSerializer(EGG_STATE_SERIALIZER);
  }

  public void initialise(DragonBreedNew dragonBreed) {
    this.dragonBreed = dragonBreed;
    eggState = EggState.INCUBATING;

    final int TICKS_PER_MINECRAFT_DAY = 20 * 60 * 20;  // 20 ticks/sec * 60 sec/min * 20 min per minecraft day
    DragonVariants dragonVariants = dragonBreed.getDragonVariants();
    userConfiguredParameters.eggSizeMeters = (double)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_SIZE_METRES);

    int eggIncubationTicks = (int)(TICKS_PER_MINECRAFT_DAY * (double)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_INCUBATION_DAYS));
    userConfiguredParameters.eggIncubationCompleteTicks = eggIncubationTicks;
    userConfiguredParameters.eggWiggleStartTicks = (int)(eggIncubationTicks * (double)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_WIGGLE_START_FRACTION));
    userConfiguredParameters.eggCrackStartTicks = (int)(eggIncubationTicks * (double)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_CRACK_START_FRACTION));
    userConfiguredParameters.eggGlowStartTicks = (int)(eggIncubationTicks * (double)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_GLOW_START_FRACTION));
    userConfiguredParameters.eggLevitateStartTicks = (int)(eggIncubationTicks * (double)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_LEVITATE_START_FRACTION));
    userConfiguredParameters.eggSpinStartTicks = (int)(eggIncubationTicks * (double)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_SPIN_START_FRACTION));

    userConfiguredParameters.eggLevitateHeightMetres = (double)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_LEVITATE_HEIGHT_METRES);
    userConfiguredParameters.eggSpinMaxSpeedDegPerSecond = 360.0F * (double)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_SPIN_REVS_PER_SECOND);

    userConfiguredParameters.wiggleFlag = (boolean)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_WIGGLE);
    userConfiguredParameters.crackFlag = (boolean)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_CRACKING);
    userConfiguredParameters.glowFlag = (boolean)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_GLOW);
    userConfiguredParameters.levitateFlag = (boolean)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_LEVITATE);
    userConfiguredParameters.spinFlag = (boolean)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_SPIN);

    String particleName = (String)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_PARTICLES_NAME);
    userConfiguredParameters.enumParticleType = EnumParticleTypes.getByName(particleName);
    if ((boolean)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_NO_PARTICLES)) {
      userConfiguredParameters.enumParticleType = null;
    }

    dragonBreed.registerDataParameter(this.getDataManager(), DATAPARAM_BREED);
    eggState.registerDataParameter(this.getDataManager(), DATAPARAM_EGGSTATE);
    getDataManager().register(DATAPARAM_INCUBATIONTICKS, incubationTicksServer);

    this.setSize((float)userConfiguredParameters.eggSizeMeters, (float)userConfiguredParameters.eggSizeMeters);
    this.rotationYaw = (float) (Math.random() * 360.0D);
    this.motionX = 0;
    this.motionY = 0;
    this.motionZ = 0;
  }

  @Override
  protected void entityInit() {
  }

  /**
   * Called when the entity is attacked.
   */
  @Override
  public boolean attackEntityFrom(DamageSource source, float amount) {
    if (this.isEntityInvulnerable(source) || amount <= 0) {
      return false;
    } else {
      if (eggState == EggState.SMASHED) {
        this.setDead();
      } else {
        changeEggState(EggState.SMASHED);
      }
      return true;
    }
  }

  public void notifyDataManagerChange(DataParameter<?> key) {
    if (key.equals(DATAPARAM_BREED)) {
      DragonBreedNew newBreed = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getBreed(this.getDataManager(), DATAPARAM_BREED);
      if (newBreed != dragonBreed) {
        changeBreed(newBreed);
      }
    }
    if (key.equals(DATAPARAM_EGGSTATE)) {
      EggState newEggState = EggState.getStateFromDataParam(this.getDataManager(), DATAPARAM_EGGSTATE);
      if (newEggState != eggState) {
        changeEggState(newEggState);
      }
    }
    if (key.equals(DATAPARAM_INCUBATIONTICKS) && world.isRemote) {
      incubationTicksClient.updateFromServer(this.getDataManager().get(DATAPARAM_INCUBATIONTICKS));
    }
  }

  /**
   * (abstract) Protected helper method to write subclass entity data to NBT.
   */
  @Override
  public void writeEntityToNBT(NBTTagCompound compound) {
    dragonBreed.writeToNBT(compound);
    eggState.writeToNBT(compound);
    compound.setInteger(NBT_INCUBATION_TICKS, getIncubationTicks());
  }

  /**
   * (abstract) Protected helper method to read subclass entity data from NBT.
   */
  @Override
  public void readEntityFromNBT(NBTTagCompound compound) {
    DragonBreedNew newBreed = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getDefaultBreed();
    try {
      newBreed = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getBreed(compound);
    } catch (IllegalArgumentException iae) {
      DragonMounts.loggerLimit.warn_once(iae.getMessage());
    }
    initialise(newBreed);
    EggState newEggState = EggState.getStateFromNBT(compound);
    int incubationTicks = compound.getInteger(NBT_INCUBATION_TICKS);
    changeEggState(newEggState);
    setIncubationTicks(incubationTicks);
  }

  /**
   * Called to update the entity's position/logic.
   */
  @Override
  public void onUpdate() {
    super.onUpdate();

    if (getIncubationTicks() >= userConfiguredParameters.eggIncubationCompleteTicks) {
      hatchEgg();
    }

    // code adapted from EntityItem

    this.prevPosX = this.posX;
    this.prevPosY = this.posY;
    this.prevPosZ = this.posZ;
    double initialMotionX = this.motionX;
    double initialMotionY = this.motionY;
    double initialMotionZ = this.motionZ;

    if (userConfiguredParameters.levitateFlag) {
      applyLevitation();
    } else if (!this.hasNoGravity()) {
      this.motionY -= 0.04;
    }

    if (this.world.isRemote) {
      this.noClip = false;
    } else {
      this.noClip = this.pushOutOfBlocks(this.posX, (this.getEntityBoundingBox().minY + this.getEntityBoundingBox().maxY) / 2.0D, this.posZ);
    }

    this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
    boolean collidedWithSomething = (int) this.prevPosX != (int) this.posX || (int) this.prevPosY != (int) this.posY || (int) this.prevPosZ != (int) this.posZ;

    final int CHECK_FOR_LAVA_TIME = 25;
    if (collidedWithSomething || this.ticksExisted % CHECK_FOR_LAVA_TIME == 0) {
      if (this.world.getBlockState(new BlockPos(this)).getMaterial() == Material.LAVA) {
        this.motionY = 0.2;
        this.motionX = (double) ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F);
        this.motionZ = (double) ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F);
        this.playSound(SoundEvents.ENTITY_GENERIC_BURN, 0.4F, 2.0F + this.rand.nextFloat() * 0.4F);
      }
    }

    final double DEFAULT_FRICTION_FACTOR = 0.98;
    double frictionFactor = DEFAULT_FRICTION_FACTOR;

    if (this.onGround) {
      BlockPos underPos = new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(this.getEntityBoundingBox().minY) - 1, MathHelper.floor(this.posZ));
      net.minecraft.block.state.IBlockState underState = this.world.getBlockState(underPos);
      frictionFactor = underState.getBlock().getSlipperiness(underState, this.world, underPos, this) * 0.98F;
    }

    this.motionX *= frictionFactor;
    this.motionY *= DEFAULT_FRICTION_FACTOR;
    this.motionZ *= frictionFactor;

    final double BOUNCE_RECOIL_FACTOR = 0.5;
    if (this.onGround) {
      this.motionY *= -BOUNCE_RECOIL_FACTOR;
    }

    this.handleWaterMovement();

    if (!this.world.isRemote) {
      double dx = this.motionX - initialMotionX;
      double dy = this.motionY - initialMotionY;
      double dz = this.motionZ - initialMotionZ;
      double speedSquared = dx * dx + dy * dy + dz * dz;

      final double SPEED_SQUARED_FOR_AIRBORNE = 0.01;
      if (speedSquared > SPEED_SQUARED_FOR_AIRBORNE) {
        this.isAirBorne = true;
      }
    }

    if (eggState == EggState.HATCHED || eggState == EggState.SMASHED) {
      ++this.idleTicks;
      if (!this.world.isRemote && this.idleTicks >= idleTicksBeforeDisappear) {
        this.setDead();
      }
    }

    if (eggState == EggState.INCUBATING) {
      updateIncubationTime();
    }
    updateEggAnimation();
  }

  public int getIncubationTicks() {
    if (DebugSettings.existsDebugParameter("forcedageticks")) {
      return (int) DebugSettings.getDebugParameter("forcedageticks");
    }

    if (!world.isRemote) {
      return incubationTicksServer;
    } else {
      return incubationTicksClient.getCurrentTickCount();
    }
  }

  public void setIncubationTicks(int incubationTicks) {
    if (!world.isRemote) {
      incubationTicksServer = incubationTicks;
    } else {
      incubationTicksClient.updateFromServer(incubationTicksServer);
    }
  }

  public int getEggWiggleXtickTimer() {
    return eggWiggleXtickTimer;
  }

  public int getEggWiggleZtickTimer() {
    return eggWiggleZtickTimer;
  }

  /**
   * Returns true if other Entities should be prevented from moving through this Entity.
   */  @Override
  public boolean canBeCollidedWith() {
    return eggState == EggState.INCUBATING;
  }

  /**
   * Retrieves the user-configured parameters for the Entity.  Just to make it easier for the renderer.
   * Do not modify directly...
   * @return
   */
  public UserConfiguredParameters getUserConfiguredParameters() {return userConfiguredParameters;}

  public DragonBreedNew getDragonBreed() {
    return dragonBreed;
  }

  public EggState getEggState() {
    return eggState;
  }

  /**
   * If the egg is glowing, add blocklight
   * @return
   */
  @Override
  @SideOnly(Side.CLIENT)
  public int getBrightnessForRender()
  {
    int baseLight = super.getBrightnessForRender();

    int incubationTicks = getIncubationTicks();
    if (!userConfiguredParameters.glowFlag || incubationTicks < userConfiguredParameters.eggGlowStartTicks) {
      return baseLight;
    }

    int baseSkyLight = baseLight & (0xf << 20);
    int baseBlockLight = baseLight & (0xf << 4);

    float glowFraction =  (incubationTicks - userConfiguredParameters.eggGlowStartTicks) /
                            (userConfiguredParameters.eggIncubationCompleteTicks - userConfiguredParameters.eggGlowStartTicks);
    final float MIN_GLOW = 0;
    final float MAX_GLOW = 0x0f;
    int glowBlockLight = Math.round(MathX.lerp(MIN_GLOW, MAX_GLOW, glowFraction));
    if (glowBlockLight < baseBlockLight) return baseLight;

    return baseSkyLight | (glowBlockLight << 4);
  }

  public enum EggState {
    INCUBATING(EggModels.EggModelState.INCUBATING),
    HATCHED(EggModels.EggModelState.HATCHED),
    SMASHED(EggModels.EggModelState.SMASHED);

    EggState(EggModels.EggModelState eggModelState) {this.eggModelState = eggModelState;}

    public static EggState getStateFromDataParam(EntityDataManager entityDataManager, DataParameter<EggState> dataParameter) {
      try {
        EggState newEggState = entityDataManager.get(dataParameter);
        return newEggState;
      } catch (Exception e) {
        return SMASHED;
      }
    }

    public static EggState getStateFromNBT(NBTTagCompound nbtTagCompound) {
      try {
        int ordinalvalue = nbtTagCompound.getInteger(NBT_EGGSTATE);
        EggState newEggState = EggState.values()[ordinalvalue];
        return newEggState;
      } catch (Exception e) {
        return SMASHED;
      }
    }

    public void registerDataParameter(EntityDataManager entityDataManager, DataParameter<EggState> dataParameter) {
      entityDataManager.register(dataParameter, this);
    }

    public void writeToNBT(NBTTagCompound nbt) {
      nbt.setInteger(NBT_EGGSTATE, this.ordinal());
    }

    public EggModels.EggModelState getEggModelState() {return  eggModelState;}

    private static final String NBT_EGGSTATE = "EggState";
    private EggModels.EggModelState eggModelState;
  }

  public static final DataSerializer<EggState> EGG_STATE_SERIALIZER = new DataSerializer<EggState>() {
    public void write(PacketBuffer buf, EggState value) {
      buf.writeEnumValue(value);
    }

    public EggState read(PacketBuffer buf) throws IOException {
      return buf.readEnumValue(EggState.class);
    }

    public DataParameter<EggState> createKey(int id) {
      return new DataParameter<EggState>(id, this);
    }

    public EggState copyValue(EggState value) {
      return value;
    }
  };

  private void hatchEgg() {
    changeEggState(EggState.HATCHED);
    //todo spawn the dragon here!
  }

  // levitate to a given target height by manipulating motionY:
  // if we're below the target height, levitate upwards; if above the target height, levitate downwards.
  // once we get close to the target height, the levitation speed smoothly tapers off to zero
  // if the egg has other motion already (eg due to falling or being hit), decay it exponentially.
  private void applyLevitation() {

    final float MINECRAFT_BASE_GRAVITY = 0.04F;
    final float RELATIVE_GRAVITY_STRENGTH = 0.25F;
    int incubationTicks = getIncubationTicks();
    if (incubationTicks < userConfiguredParameters.eggLevitateStartTicks) {
      motionY -= MINECRAFT_BASE_GRAVITY * RELATIVE_GRAVITY_STRENGTH;
      return;
    }
    float levitateDuration = userConfiguredParameters.eggIncubationCompleteTicks - userConfiguredParameters.eggLevitateStartTicks;
    float levitateTimeFraction =  (incubationTicks - userConfiguredParameters.eggLevitateStartTicks) / levitateDuration;

    double targetHeight = userConfiguredParameters.eggLevitateHeightMetres * levitateTimeFraction;
    double currentHeight = currentHeightAboveBlock(targetHeight);

    final double MAX_SPEED = 0.01;
    final double DISTANCE_FOR_MAX_SPEED = 0.10;
    final double SPEED_SMOOTHING_FACTOR = 0.90;
    double levitationSpeed =  MAX_SPEED * (targetHeight - currentHeight)/ DISTANCE_FOR_MAX_SPEED;
    levitationSpeed = MathX.clamp(levitationSpeed, -MAX_SPEED, MAX_SPEED);

    motionY = SPEED_SMOOTHING_FACTOR * motionY + (1 - SPEED_SMOOTHING_FACTOR) * levitationSpeed;
  }

  private double currentHeightAboveBlock(double targetHeight) {
    double searchDY = -(targetHeight + 0.5);
    AxisAlignedBB eggAABB = this.getCollisionBoundingBox().expand(0, searchDY, 0);
    List<AxisAlignedBB> collidingBoxes = this.world.getCollisionBoxes(null, eggAABB);

    for (AxisAlignedBB aabb : collidingBoxes) {
      searchDY = aabb.calculateYOffset(eggAABB, searchDY);
    }
    return -searchDY;
  }

  public static final int WIGGLE_DURATION_TICKS = 20;

  private void updateEggAnimation() {
    // wiggle, crack and particle are handled here
    // if both wiggle and crack are applied, synchronise the cracking to the wiggling

    // animate egg wiggle based on the time the eggs take to hatch

    if (eggState != EggState.INCUBATING) {
      eggWiggleXtickTimer = 0;
      eggWiggleZtickTimer = 0;
      return;
    }

    int incubationTicks = getIncubationTicks();
    boolean doIndependentCrackCheck = true;
    if (userConfiguredParameters.wiggleFlag) {
      // wait until the egg is nearly hatched
      if (incubationTicks > userConfiguredParameters.eggWiggleStartTicks) {
        doIndependentCrackCheck = false;
        final float WIGGLE_BASE_CHANCE = 1 / 20.0F;  // one wiggle per second on average

        float wiggleDuration = (userConfiguredParameters.eggIncubationCompleteTicks - userConfiguredParameters.eggWiggleStartTicks);
        float wiggleChance = WIGGLE_BASE_CHANCE * (incubationTicks - userConfiguredParameters.eggWiggleStartTicks) / wiggleDuration;

        if (eggWiggleXtickTimer > 0) {
          eggWiggleXtickTimer -= 1/WIGGLE_DURATION_TICKS;
        } else if (rand.nextFloat() < wiggleChance) {
          eggWiggleXtickTimer = rand.nextBoolean() ? WIGGLE_DURATION_TICKS / 2 : WIGGLE_DURATION_TICKS;
          playEggCrackEffect(incubationTicks, false);
        }

        if (eggWiggleZtickTimer > 0) {
          eggWiggleZtickTimer -= 1/WIGGLE_DURATION_TICKS;
        } else if (rand.nextFloat() < wiggleChance) {
          eggWiggleZtickTimer = rand.nextBoolean() ? WIGGLE_DURATION_TICKS / 2 : WIGGLE_DURATION_TICKS;
          playEggCrackEffect(incubationTicks, false);
        }
      }
    }
    if (doIndependentCrackCheck) {
      playEggCrackEffect(incubationTicks, true);
    }

    // spawn generic particles
    double px = posX + (rand.nextDouble() - 0.3);
    double py = posY + (rand.nextDouble() - 0.3);
    double pz = posZ + (rand.nextDouble() - 0.3);
    double ox = (rand.nextDouble() - 0.3) * 2;
    double oy = (rand.nextDouble() - 0.3) * 2;
    double oz = (rand.nextDouble() - 0.3) * 2;
    if (userConfiguredParameters.enumParticleType != null) {
      world.spawnParticle(userConfiguredParameters.enumParticleType, px, py, pz, ox, oy, oz);
    }
  }

  /** PLays the egg crack sound effect if conditions are right (crack flag enabled
   *
   * @param progressTicks  number of ticks that the egg has been incubating
   * @param randomCheck if true: random check is performed and only make sound if it succeeds
   */
  private void playEggCrackEffect(int progressTicks, boolean randomCheck)
  {
    final float BASE_CHANCE = 1/20.0F; // average one crack per 20 ticks
    if (!userConfiguredParameters.crackFlag) return;
    if (progressTicks < userConfiguredParameters.eggCrackStartTicks) return;
    if (randomCheck && userConfiguredParameters.eggCrackStartTicks != userConfiguredParameters.eggIncubationCompleteTicks) {
      float crackChance = BASE_CHANCE *  (progressTicks - userConfiguredParameters.eggCrackStartTicks)
              / (float)(userConfiguredParameters.eggIncubationCompleteTicks - userConfiguredParameters.eggCrackStartTicks);

      if (rand.nextFloat() > crackChance) return;
    }
    world.playSound(null, posX, posY, posZ, ModSounds.DRAGON_HATCHING, SoundCategory.BLOCKS, +1.0F, 1.0F);
  }

  /**
   * Validates the following aspects of the tags:
   *   the egg particle
   */
  public static class EntityEggValidator implements DragonVariants.VariantTagValidator {
    @Override
    public void validateVariantTags(DragonVariants dragonVariants) throws IllegalArgumentException {
//      DragonBreedNew whichBreed =  DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getBreed(dragonVariants);
      DragonVariantsException.DragonVariantsErrors dragonVariantsErrors = new DragonVariantsException.DragonVariantsErrors();
      String particleName = (String)dragonVariants.getValueOrDefault(DragonVariants.Category.EGG, EGG_PARTICLES_NAME);
      EnumParticleTypes particleType = EnumParticleTypes.getByName(particleName);
      if (particleType == null) {
        dragonVariants.removeTag(DragonVariants.Category.EGG, EGG_PARTICLES_NAME);
        dragonVariantsErrors.addError("Unknown egg particle name:" + particleName);
      }

      if (dragonVariants.tagIsExplictlyApplied(DragonVariants.Category.EGG, EGG_CRACK_START_FRACTION) &&
          !dragonVariants.tagIsExplictlyApplied(DragonVariants.Category.EGG, EGG_CRACKING)) {
        dragonVariantsErrors.addError("eggcrackstart is defined but cracking flag is not defined");
      }

      if (dragonVariants.tagIsExplictlyApplied(DragonVariants.Category.EGG, EGG_WIGGLE_START_FRACTION) &&
              !dragonVariants.tagIsExplictlyApplied(DragonVariants.Category.EGG, EGG_WIGGLE)) {
        dragonVariantsErrors.addError("eggwigglestart is defined but wiggle flag is not defined");
      }

      if (dragonVariants.tagIsExplictlyApplied(DragonVariants.Category.EGG, EGG_GLOW_START_FRACTION) &&
              !dragonVariants.tagIsExplictlyApplied(DragonVariants.Category.EGG, EGG_GLOW)) {
        dragonVariantsErrors.addError("eggglowstart is defined but glow flag is not defined");
      }

      if ((dragonVariants.tagIsExplictlyApplied(DragonVariants.Category.EGG, EGG_LEVITATE_HEIGHT_METRES) ||
              dragonVariants.tagIsExplictlyApplied(DragonVariants.Category.EGG, EGG_LEVITATE_START_FRACTION) ) &&
              !dragonVariants.tagIsExplictlyApplied(DragonVariants.Category.EGG, EGG_LEVITATE)) {
        dragonVariantsErrors.addError("egglevitatestart and/or levitateheightmetres are defined but levitate flag is not defined");
      }

      if ((dragonVariants.tagIsExplictlyApplied(DragonVariants.Category.EGG, EGG_SPIN_START_FRACTION) ||
              dragonVariants.tagIsExplictlyApplied(DragonVariants.Category.EGG, EGG_SPIN_REVS_PER_SECOND) ) &&
              !dragonVariants.tagIsExplictlyApplied(DragonVariants.Category.EGG, EGG_SPIN)) {
        dragonVariantsErrors.addError("eggspinstart and/or spinspeedrevspersec are defined but spin flag is not defined");
      }

      if (dragonVariants.tagIsExplictlyApplied(DragonVariants.Category.EGG, EGG_PARTICLES_NAME) &&
              dragonVariants.tagIsExplictlyApplied(DragonVariants.Category.EGG, EGG_NO_PARTICLES)) {
        dragonVariantsErrors.addError("eggparticlesname is defined but noparticles flag is also defined");
      }

      if (dragonVariantsErrors.hasErrors()) {
        throw new DragonVariantsException(dragonVariantsErrors);
      }
    }
  }

  private void updateIncubationTime() {
    // If the egg is incubating, increase the age
    if (!world.isRemote) {
      if (eggState == EggState.INCUBATING) {
        incubationTicksServer++;
        if (incubationTicksServer % TICKS_SINCE_CREATION_UPDATE_INTERVAL == 0)
          this.getDataManager().set(DATAPARAM_INCUBATIONTICKS, incubationTicksServer);
      }
    } else {
      if (eggState == EggState.INCUBATING) incubationTicksClient.tick();
    }
  }

  private void changeEggState(EggState newEggState) {
    final int LIFESPAN_SMASHED = 20 * 60;
    final int LIFESPAN_HATCHED = 20 * 60;

    if (eggState == EggState.INCUBATING && newEggState != EggState.INCUBATING) {
      idleTicksBeforeDisappear = (newEggState == EggState.SMASHED) ? LIFESPAN_SMASHED : LIFESPAN_HATCHED;
    }
    eggState = newEggState;
  }

  private void changeBreed(DragonBreedNew newDragonBreed) {
    dragonBreed = newDragonBreed;
  }

  private static final DragonVariantTag EGG_SIZE_METRES = DragonVariantTag.addTag("eggsizemetres", 0.25, 0.05, 2.0);
  private static final DragonVariantTag EGG_INCUBATION_DAYS = DragonVariantTag.addTag("incubationdurationdays", 1.0, 0.1, 10.0);
  private static final DragonVariantTag EGG_WIGGLE = DragonVariantTag.addTag("wiggle");
  private static final DragonVariantTag EGG_GLOW = DragonVariantTag.addTag("glow");
  private static final DragonVariantTag EGG_LEVITATE = DragonVariantTag.addTag("levitate");
  private static final DragonVariantTag EGG_SPIN = DragonVariantTag.addTag("spin");
  private static final DragonVariantTag EGG_CRACKING = DragonVariantTag.addTag("cracking");
  private static final DragonVariantTag EGG_PARTICLES_NAME = DragonVariantTag.addTag("eggparticlesname", "townaura");
  private static final DragonVariantTag EGG_NO_PARTICLES = DragonVariantTag.addTag("noparticles");

  private static final DragonVariantTag EGG_WIGGLE_START_FRACTION = DragonVariantTag.addTag("eggwigglestart", 0.75, 0.0, 1.0);
  private static final DragonVariantTag EGG_CRACK_START_FRACTION = DragonVariantTag.addTag("eggcrackstart", 0.9, 0.0, 1.0);
  private static final DragonVariantTag EGG_GLOW_START_FRACTION = DragonVariantTag.addTag("eggglowstart", 0.5, 0.0, 1.0);
  private static final DragonVariantTag EGG_LEVITATE_START_FRACTION = DragonVariantTag.addTag("egglevitatestart", 0.5, 0.0, 1.0);
  private static final DragonVariantTag EGG_SPIN_START_FRACTION = DragonVariantTag.addTag("eggspinstart", 0.5, 0.0, 1.0);
  private static final DragonVariantTag EGG_LEVITATE_HEIGHT_METRES = DragonVariantTag.addTag("levitateheightmetres", 0.5, 0.0, 3.0);
  private static final DragonVariantTag EGG_SPIN_REVS_PER_SECOND = DragonVariantTag.addTag("spinspeedrevspersec", 1.0, -3.0, 3.0);

  // cluster the userConfiguredParameters together to make it easier for the renderer to access
  public class UserConfiguredParameters {
    public double eggSizeMeters;
    public int eggIncubationCompleteTicks;  // duration of incubation in ticks
    public int eggWiggleStartTicks;
    public int eggCrackStartTicks;
    public int eggGlowStartTicks;
    public int eggLevitateStartTicks;
    public double eggLevitateHeightMetres;
    public int eggSpinStartTicks;
    public double eggSpinMaxSpeedDegPerSecond;

    public boolean wiggleFlag;
    public boolean crackFlag;
    public boolean glowFlag;
    public boolean levitateFlag;
    public boolean spinFlag;

    public EnumParticleTypes enumParticleType;
  }

  private UserConfiguredParameters userConfiguredParameters = new UserConfiguredParameters();

  private static final DataParameter<String> DATAPARAM_BREED = EntityDataManager.createKey(EntityDragonEgg.class, DataSerializers.STRING);
  private static final DataParameter<EggState> DATAPARAM_EGGSTATE = EntityDataManager.createKey(EntityDragonEgg.class, EGG_STATE_SERIALIZER);
  private static final DataParameter<Integer> DATAPARAM_INCUBATIONTICKS = EntityDataManager.createKey(EntityDragonEgg.class, DataSerializers.VARINT);
  private static final String NBT_INCUBATION_TICKS = "IncubationTicks";
  private static final int TICKS_SINCE_CREATION_UPDATE_INTERVAL = 100;

  // the ticks since creation is used to control the egg's hatching time.  It is only updated by the server occasionally.
  // the client keeps a cached copy of it and uses client ticks to interpolate in the gaps.
  // when the watcher is updated from the server, the client will tick it faster or slower to resynchronise
  private final ClientServerSynchronisedTickCount incubationTicksClient;
  private int incubationTicksServer;

  private DragonBreedNew dragonBreed;
  private EggState eggState = EggState.INCUBATING;
  private int idleTicks = 0;
  private int idleTicksBeforeDisappear;
  private int eggWiggleXtickTimer;
  private int eggWiggleZtickTimer;

}
