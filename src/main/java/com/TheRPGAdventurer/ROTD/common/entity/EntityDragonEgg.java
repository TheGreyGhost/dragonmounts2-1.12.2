package com.TheRPGAdventurer.ROTD.common.entity;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.model.EggModels;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants.Category;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.List;

/**
 * Created by TGG on 10/08/2019.
 *
 *  Usage:
 *   1) call EntityDragonEgg.registerConfigurationTags() during commonproxy.preInitialisePhase to ensure that the configuration tags are properly registered
 *   2) To spawn a new entity:
 *     a) EntityDragonEgg(world, dragonBreed)
 *     b) worldIn.spawnEntity(myEntity);
 *
 *  The usual mechanics for vanilla entity spawing are followed:
 *  After the user has spawned a new Entity on the server, the packet to the client will call:
 *  1) myEntityClient = new MyEntity(world) will be called
 *  2) myEntityClient notifyDataManagerChange will be called for all DataParameters

 *  For reloading of an existing entity from disk:
 *  1) myEntityServer = new MyEntity(world) will be called
 *  2) myEntityServer.readEntityFromNBT will be called
 *     The client creation then occurs as above.
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
    this.rotationYaw = (float) (Math.random() * 360.0D);
    this.motionX = 0;
    this.motionY = 0;
    this.motionZ = 0;
  }

  public EntityDragonEgg(World worldIn, DragonBreedNew dragonBreed) {
    this(worldIn);
    initialiseConfigurableParameters(dragonBreed);
    serverInitialiseState(dragonBreed, EggState.INCUBATING, 0);
  }

  public static void registerConfigurationTags() {
    // the tags themselves are initialised in static member variables
    DragonVariants.addVariantTagValidator(new EntityEggValidator());
    DataSerializers.registerSerializer(EGG_STATE_SERIALIZER);
  }

  // get the modifiers which are applied to this dragon
  //  (female, male, etc)
  public DragonVariants.Modifier []  getConfigurationFileCategoryModifiers() {
    return new DragonVariants.Modifier[0];  //todo later on: set appropriately.
  }

  private void initialiseConfigurableParameters(DragonBreedNew dragonBreed) {
    DragonVariants.ModifiedCategory modifiedCategory = new DragonVariants.ModifiedCategory(Category.EGG, getConfigurationFileCategoryModifiers());

    final int TICKS_PER_MINECRAFT_DAY = 20 * 60 * 20;  // 20 ticks/sec * 60 sec/min * 20 min per minecraft day
    DragonVariants dragonVariants = dragonBreed.getDragonVariants();
    userConfiguredParameters.eggSizeMeters = (double)dragonVariants.getValueOrDefault(modifiedCategory, EGG_SIZE_METRES);

    int eggIncubationTicks = (int)(TICKS_PER_MINECRAFT_DAY * (double)dragonVariants.getValueOrDefault(modifiedCategory, EGG_INCUBATION_DAYS));
    userConfiguredParameters.eggIncubationCompleteTicks = eggIncubationTicks;
    userConfiguredParameters.eggWiggleStartTicks = (int)(eggIncubationTicks * (double)dragonVariants.getValueOrDefault(modifiedCategory, EGG_WIGGLE_START_FRACTION));
    userConfiguredParameters.eggCrackStartTicks = (int)(eggIncubationTicks * (double)dragonVariants.getValueOrDefault(modifiedCategory, EGG_CRACK_START_FRACTION));
    userConfiguredParameters.eggGlowStartTicks = (int)(eggIncubationTicks * (double)dragonVariants.getValueOrDefault(modifiedCategory, EGG_GLOW_START_FRACTION));
    userConfiguredParameters.eggLevitateStartTicks = (int)(eggIncubationTicks * (double)dragonVariants.getValueOrDefault(modifiedCategory, EGG_LEVITATE_START_FRACTION));
    userConfiguredParameters.eggSpinStartTicks = (int)(eggIncubationTicks * (double)dragonVariants.getValueOrDefault(modifiedCategory, EGG_SPIN_START_FRACTION));

    userConfiguredParameters.eggLevitateHeightMetres = (double)dragonVariants.getValueOrDefault(modifiedCategory, EGG_LEVITATE_HEIGHT_METRES);
    userConfiguredParameters.eggSpinMaxSpeedDegPerSecond = 360.0F * (double)dragonVariants.getValueOrDefault(modifiedCategory, EGG_SPIN_REVS_PER_SECOND);

    userConfiguredParameters.bounceRecoilFactor = (double)dragonVariants.getValueOrDefault(modifiedCategory, EGG_BOUNCE_RECOIL_FACTOR);

    userConfiguredParameters.wiggleFlag = (boolean)dragonVariants.getValueOrDefault(modifiedCategory, EGG_WIGGLE);
    userConfiguredParameters.crackFlag = (boolean)dragonVariants.getValueOrDefault(modifiedCategory, EGG_CRACKING);
    userConfiguredParameters.glowFlag = (boolean)dragonVariants.getValueOrDefault(modifiedCategory, EGG_GLOW);
    userConfiguredParameters.levitateFlag = (boolean)dragonVariants.getValueOrDefault(modifiedCategory, EGG_LEVITATE);
    userConfiguredParameters.spinFlag = (boolean)dragonVariants.getValueOrDefault(modifiedCategory, EGG_SPIN);

    String particleName = (String)dragonVariants.getValueOrDefault(modifiedCategory, EGG_PARTICLES_NAME);
    userConfiguredParameters.enumParticleType = EnumParticleTypes.getByName(particleName);
    if ((boolean)dragonVariants.getValueOrDefault(modifiedCategory, EGG_PARTICLES)) {
      userConfiguredParameters.enumParticleType = null;
    }
    userConfiguredParameters.particleSpawnParameterIsColour = "color".matches((String)dragonVariants.getValueOrDefault(modifiedCategory, EGG_PARTICLES_SPEED_OR_COLOUR));
    userConfiguredParameters.particleSpawnRGB = parseRGB((String)dragonVariants.getValueOrDefault(modifiedCategory, EGG_PARTICLES_RGB));
    userConfiguredParameters.particleSpawnRatePerTick = (double)dragonVariants.getValueOrDefault(modifiedCategory, EGG_PARTICLES_SPAWN_RATE_PER_SECOND)
                                                        / 20.0;
    userConfiguredParameters.particleSpawnSpeedMinMperTick = (double)dragonVariants.getValueOrDefault(modifiedCategory, EGG_PARTICLES_SPEED_MIN_MPS)
                                                              / 20.0;
    userConfiguredParameters.particleSpawnSpeedMaxMperTick = (double)dragonVariants.getValueOrDefault(modifiedCategory, EGG_PARTICLES_SPEED_MAX_MPS)
                                                            / 20.0;
    userConfiguredParameters.particlesSpawnRegularIntervals = (boolean)dragonVariants.getValueOrDefault(modifiedCategory, EGG_PARTICLES_SPAWN_REGULAR_TIMES);
  }

  // parses the given text fragment into an int RGB
  // expects "r,g,b" eg "255,13,26"
  private static int parseRGB(String textRGB) throws IllegalArgumentException {
    String[] parts = textRGB.split(",");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Expected R,G,B where eg R= 0 to 255; instead found " + textRGB);
    }
    int red = Integer.parseInt(parts[0]);
    int green = Integer.parseInt(parts[1]);
    int blue = Integer.parseInt(parts[2]);
    if (red < 0 || red > 255 || blue < 0 || blue > 255 || green < 0 || green > 255) {
      throw new IllegalArgumentException("R, G, B must each be 0 to 255");
    }
    return (red << 16) | (green << 8) | blue;
  }

  private void serverInitialiseState(DragonBreedNew dragonBreed, EggState eggState, int incubationTicksServer) {
    commonInitialiseState(dragonBreed, eggState, incubationTicksServer);
    dragonBreed.setDataParameter(this.getDataManager(), DATAPARAM_BREED);
    eggState.setDataParameter(this.getDataManager(), DATAPARAM_EGGSTATE);
    getDataManager().set(DATAPARAM_INCUBATIONTICKS, incubationTicksServer);
  }

  private void commonInitialiseState(DragonBreedNew dragonBreed, EggState eggState, int incubationTicksServer) {
    this.dragonBreed = dragonBreed;
    this.eggState = eggState;
    this.incubationTicksServer = incubationTicksServer;
    this.setSize((float) userConfiguredParameters.eggSizeMeters, (float) userConfiguredParameters.eggSizeMeters);
    fullyInitialised = true;
  }

  @Override
  protected void entityInit() {
    // super.registerDataParameters();
    DragonBreedNew.registerDataParameter(this.getDataManager(), DATAPARAM_BREED);
    EggState.registerDataParameter(this.getDataManager(), DATAPARAM_EGGSTATE);
    getDataManager().register(DATAPARAM_INCUBATIONTICKS, 0); // just a default
  }

  /**
   * Called when the entity is attacked.
   */
  @Override
  public boolean attackEntityFrom(DamageSource source, float amount) {
    if (this.isEntityInvulnerable(source) || amount <= 0) {
      return false;
    } else {
      if (eggState == EggState.INCUBATING) {
        changeEggState(EggState.SMASHED);
      } else {
        this.setDead();
      }
      return true;
    }
  }

  public void notifyDataManagerChange(DataParameter<?> key) {
    if (key.equals(DATAPARAM_BREED)) {
      DragonBreedNew newBreed = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getBreed(this.getDataManager(), DATAPARAM_BREED);
      if (!fullyInitialised) {
        clientInitDragonBreed = newBreed;
      } else if (newBreed != dragonBreed) {
        changeBreed(newBreed);
      }
    }
    if (key.equals(DATAPARAM_EGGSTATE)) {
      EggState newEggState = EggState.getStateFromDataParam(this.getDataManager(), DATAPARAM_EGGSTATE);
      if (!fullyInitialised) {
        clientInitEggState = newEggState;
      } else if (newEggState != eggState) {
        changeEggState(newEggState);
      }
    }
    if (key.equals(DATAPARAM_INCUBATIONTICKS) && world.isRemote) {
      int tickCount = this.getDataManager().get(DATAPARAM_INCUBATIONTICKS);
      if (!fullyInitialised) {
        clientInitIncubationTicks = (tickCount < 0 ? 0 : tickCount); // just in case of unexpected value...
      } else {
        incubationTicksClient.updateFromServer(tickCount);
      }
    }
    if (!fullyInitialised) clientCheckForAllDataParametersReceived();
  }

  private void clientCheckForAllDataParametersReceived() {
    if (clientInitDragonBreed != null && clientInitEggState != null && clientInitIncubationTicks != UNINITIALISED_TICKS_VALUE) {
      initialiseConfigurableParameters(clientInitDragonBreed);
      commonInitialiseState(clientInitDragonBreed, clientInitEggState, clientInitIncubationTicks);
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
    EggState newEggState = EggState.getStateFromNBT(compound);
    int incubationTicks = compound.getInteger(NBT_INCUBATION_TICKS);

    initialiseConfigurableParameters(newBreed);
    serverInitialiseState(newBreed, newEggState, incubationTicks);
  }

  /**
   * Called to update the entity's position/logic.
   */
  @Override
  public void onUpdate() {
    super.onUpdate();

    if (!fullyInitialised) {
      DragonMounts.loggerLimit.error_once("EntityDragonEgg.onUpdate() called before entity has been fully initialised");
      return;
    }

    if (eggState == EggState.INCUBATING && getIncubationTicks() >= userConfiguredParameters.eggIncubationCompleteTicks) {
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
    boolean hasMoved = (int) this.prevPosX != (int) this.posX || (int) this.prevPosY != (int) this.posY || (int) this.prevPosZ != (int) this.posZ;

    final double MIN_SPEED_FOR_BOUNCE = 0.05;
    if (-initialMotionY >= MIN_SPEED_FOR_BOUNCE && this.motionY == 0) {
      this.motionY = -userConfiguredParameters.bounceRecoilFactor * initialMotionY;
    }

    final int CHECK_FOR_LAVA_TIME = 25;
    if (hasMoved || this.ticksExisted % CHECK_FOR_LAVA_TIME == 0) {
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
      frictionFactor = underState.getBlock().getSlipperiness(underState, this.world, underPos, this) * DEFAULT_FRICTION_FACTOR;
    }

    this.motionX *= frictionFactor;
    this.motionY *= DEFAULT_FRICTION_FACTOR;
    this.motionZ *= frictionFactor;

    this.handleWaterMovement();

    if (!this.world.isRemote) {
      double dx = this.motionX - initialMotionX;
      double dy = this.motionY - initialMotionY;
      double dz = this.motionZ - initialMotionZ;
      double accelerationSquared = dx * dx + dy * dy + dz * dz;

      final double ACCELERATION_SQUARED_FOR_AIRBORNE = 0.01;
      if (accelerationSquared > ACCELERATION_SQUARED_FOR_AIRBORNE) {
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

  public boolean getEggWiggleInverseDirection() {
    return eggWiggleInverseDirection;
  }

  public float getRenderScale() {
    return width;
  }

  /**
   * Returns true if other Entities should be prevented from moving through this Entity.
   */
  @Override
  public boolean canBeCollidedWith() {
    return !this.isDead;
  }

  @Override
  public boolean canBePushed() {
    return !this.isDead;
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
    if (eggState != EggState.INCUBATING) return baseLight;

    int incubationTicks = getIncubationTicks();
    if (!userConfiguredParameters.glowFlag || incubationTicks < userConfiguredParameters.eggGlowStartTicks) {
      return baseLight;
    }

    int baseSkyLight = baseLight & (0xf << 20);
    int baseBlockLight = baseLight & (0xf << 4);

    float glowFraction =  (incubationTicks - userConfiguredParameters.eggGlowStartTicks) /
            (float)(userConfiguredParameters.eggIncubationCompleteTicks - userConfiguredParameters.eggGlowStartTicks);
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

    public static void registerDataParameter(EntityDataManager entityDataManager, DataParameter<EggState> dataParameter) {
      entityDataManager.register(dataParameter, INCUBATING); // just a default
    }
    public void setDataParameter(EntityDataManager entityDataManager, DataParameter<EggState> dataParameter) {
      entityDataManager.set(dataParameter, this);
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
    //todo copy the egg name into the dragon

//    // play particle and sound effects when the dragon hatches
//    if (prevLifeStage != null && prevLifeStage.isEgg() && !lifeStage.isBaby()) {
//      playEggCrackEffect();
//      dragon.world.playSound(dragon.posX, dragon.posY, dragon.posZ, ModSounds.DRAGON_HATCHED, SoundCategory.BLOCKS, 4, 1, false);
//    }
//    /**
//     * Generates some egg shell particles and a breaking sound.
//     */
//    public void playEggCrackEffect() {
//      // dragon.world.playEvent(2001, dragon.getPosition(), Block.getIdFromBlock(BlockDragonBreedEgg.DRAGON_HATCHABLE_EGG));
//      this.playEvent(dragon.getPosition());
//    }
//
//    public void playEvent(BlockPos blockPosIn) {
//      dragon.world.playSound(null, blockPosIn, ModSounds.DRAGON_HATCHING, SoundCategory.BLOCKS, +1.0F, 1.0F);
//    }

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
    AxisAlignedBB eggAABB = this.getEntityBoundingBox();
    AxisAlignedBB searchAABB = eggAABB.expand(0, searchDY, 0);
    List<AxisAlignedBB> collidingBoxes = this.world.getCollisionBoxes(null, searchAABB);

    for (AxisAlignedBB aabb : collidingBoxes) {
      searchDY = aabb.calculateYOffset(eggAABB, searchDY);
    }
    return -searchDY;
  }

  public static final int WIGGLE_DURATION_TICKS = 30;

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
        final float WIGGLE_BASE_CHANCE = 1 / (1.0F * 20);  // a one second gap between wiggles on average, when ready to hatch

        float wiggleDuration = (userConfiguredParameters.eggIncubationCompleteTicks - userConfiguredParameters.eggWiggleStartTicks);
        float wiggleChance = WIGGLE_BASE_CHANCE * (incubationTicks - userConfiguredParameters.eggWiggleStartTicks) / wiggleDuration;

        if (eggWiggleXtickTimer > 0) {
          --eggWiggleXtickTimer;
        } else if (eggWiggleZtickTimer > 0) {
          --eggWiggleZtickTimer;
        } else if (rand.nextFloat() < wiggleChance) {
          if (rand.nextBoolean()) {
            eggWiggleXtickTimer = rand.nextBoolean() ? WIGGLE_DURATION_TICKS / 2 : WIGGLE_DURATION_TICKS;
          } else {
            eggWiggleZtickTimer = rand.nextBoolean() ? WIGGLE_DURATION_TICKS / 2 : WIGGLE_DURATION_TICKS;
          }
          eggWiggleInverseDirection = rand.nextBoolean();
          playEggCrackEffect(incubationTicks, false);
        }
      }
    }
    if (doIndependentCrackCheck) {
      playEggCrackEffect(incubationTicks, true);
    }

    // spawn  particles: random position around the egg, with an initial velocity whose origin is the entity 0,0,0
    if (userConfiguredParameters.enumParticleType != null) {
      ticksUntilNextParticleSpawn -= 1.0;
      int maxParticlesPerTick = 10;  // sanity check in case of something dumb
      while (ticksUntilNextParticleSpawn < 0 && --maxParticlesPerTick >= 0) {
        final double MIN_PARTICLE_SPAWN_RADIUS = userConfiguredParameters.eggSizeMeters * 0.4;
        final double MAX_PARTICLE_SPAWN_RADIUS = MIN_PARTICLE_SPAWN_RADIUS + 0.3;
        final double MIN_PARTICLE_SPAWN_SPEED = userConfiguredParameters.particleSpawnSpeedMinMperTick;
        final double MAX_PARTICLE_SPAWN_SPEED = userConfiguredParameters.particleSpawnSpeedMaxMperTick;
        double spawnRadius = MathX.lerp(MIN_PARTICLE_SPAWN_RADIUS, MAX_PARTICLE_SPAWN_RADIUS, rand.nextDouble());
        double spawnSpeed = MathX.lerp(MIN_PARTICLE_SPAWN_SPEED, MAX_PARTICLE_SPAWN_SPEED, rand.nextDouble());

        Vec3d spawnPosition = MathX.randomSphericalCoordinate(spawnRadius);
        Vec3d spawnVelocity = spawnPosition.normalize().scale(spawnSpeed);

        Vec3d eggCentre = new Vec3d(posX, posY + height / 2.0, posZ);
        spawnPosition = spawnPosition.add(eggCentre);

        if (userConfiguredParameters.particleSpawnParameterIsColour) {
          int rgb = userConfiguredParameters.particleSpawnRGB;
          float red = ((rgb >> 16) & 0xff) / 255.0F;
          float green = ((rgb >> 8) & 0xff) / 255.0F;
          float blue = (rgb & 0xff) / 255.0F;
          world.spawnParticle(userConfiguredParameters.enumParticleType,
                  spawnPosition.x, spawnPosition.y, spawnPosition.z,
                  red, green, blue);
        } else {  // spawn parameters are speed
          world.spawnParticle(userConfiguredParameters.enumParticleType,
                  spawnPosition.x, spawnPosition.y, spawnPosition.z,
                  spawnVelocity.x, spawnVelocity.y, spawnVelocity.z);
        }

        if (userConfiguredParameters.particlesSpawnRegularIntervals) {
          ticksUntilNextParticleSpawn += 1.0 / userConfiguredParameters.particleSpawnRatePerTick;
        } else {
          double p =MathX.clamp(rand.nextDouble(), 0.01, 0.99);
          double ticksTillNextOccurrence = -Math.log(1.0 - p) / userConfiguredParameters.particleSpawnRatePerTick;  // exponential distribution for randomly occurring events
          ticksUntilNextParticleSpawn += ticksTillNextOccurrence;
        }
      }
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
    public void validateVariantTags(DragonVariants dragonVariants, DragonVariants.ModifiedCategory modifiedCategory) throws IllegalArgumentException {
      DragonVariantsException.DragonVariantsErrors dragonVariantsErrors = new DragonVariantsException.DragonVariantsErrors();
      if (!modifiedCategory.getCategory().equals(Category.EGG)) return;

      DragonVariants.DragonVariantsCategoryShortcut dvc = dragonVariants.new DragonVariantsCategoryShortcut(modifiedCategory);

      String particleName = (String)dvc.getValueOrDefault(EGG_PARTICLES_NAME);
      EnumParticleTypes particleType = EnumParticleTypes.getByName(particleName);
      if (particleType == null) {
        dragonVariants.removeTag(modifiedCategory, EGG_PARTICLES_NAME);
        dragonVariantsErrors.addError("Unknown egg particle name:" + particleName);
      }

      dvc.checkForConflict(dragonVariantsErrors, EGG_CRACKING, false, true, EGG_CRACK_START_FRACTION);
//      if (dvc.tagIsExplictlyApplied(EGG_CRACK_START_FRACTION) &&
//          !dvc.tagIsExplictlyApplied(EGG_CRACKING)) {
//        dragonVariantsErrors.addError("crackstarttime is defined but crackingsounds flag is not defined");
//      }

      dvc.checkForConflict(dragonVariantsErrors, EGG_WIGGLE, false, true, EGG_WIGGLE_START_FRACTION);
//      if (dvc.tagIsExplictlyApplied( EGG_WIGGLE_START_FRACTION) &&
//              !dvc.tagIsExplictlyApplied(EGG_WIGGLE)) {
//        dragonVariantsErrors.addError("wigglestarttime is defined but wiggle flag is not defined");
//      }

      dvc.checkForConflict(dragonVariantsErrors, EGG_GLOW, false, true, EGG_GLOW_START_FRACTION);
//      if (dvc.tagIsExplictlyApplied(EGG_GLOW_START_FRACTION) &&
//              !dvc.tagIsExplictlyApplied(EGG_GLOW)) {
//        dragonVariantsErrors.addError("glowstarttime is defined but glow flag is not defined");
//      }

      dvc.checkForConflict(dragonVariantsErrors, EGG_LEVITATE, false, true, EGG_LEVITATE_HEIGHT_METRES, EGG_LEVITATE_START_FRACTION);
//      if ((dvc.tagIsExplictlyApplied(EGG_LEVITATE_HEIGHT_METRES) ||
//              dvc.tagIsExplictlyApplied(EGG_LEVITATE_START_FRACTION) ) &&
//              !dvc.tagIsExplictlyApplied(EGG_LEVITATE)) {
//        dragonVariantsErrors.addError("egglevitatestart and/or levitateheightmetres are defined but levitate flag is not defined");
//      }

      dvc.checkForConflict(dragonVariantsErrors, EGG_SPIN, false, true, EGG_SPIN_START_FRACTION, EGG_SPIN_REVS_PER_SECOND);
//      if ((dvc.tagIsExplictlyApplied(EGG_SPIN_START_FRACTION) ||
//              dvc.tagIsExplictlyApplied(EGG_SPIN_REVS_PER_SECOND) ) &&
//              !dvc.tagIsExplictlyApplied(EGG_SPIN)) {
//        dragonVariantsErrors.addError("spinstarttime and/or spinspeed are defined but spin flag is not defined");
//      }

      dvc.checkForConflict(dragonVariantsErrors, EGG_PARTICLES, false, true,
              EGG_PARTICLES_NAME, EGG_PARTICLES_SPEED_OR_COLOUR, EGG_PARTICLES_RGB, EGG_PARTICLES_SPAWN_RATE_PER_SECOND,
              EGG_PARTICLES_SPEED_MIN_MPS, EGG_PARTICLES_SPEED_MAX_MPS, EGG_PARTICLES_SPAWN_REGULAR_TIMES);
//      if (dvc.tagIsExplictlyApplied(EGG_PARTICLES) &&
//          (dvc.tagIsExplictlyApplied(EGG_PARTICLES_NAME) ||
//                  dvc.tagIsExplictlyApplied(EGG_PARTICLES_SPEED_OR_COLOUR) ||
//                  dvc.tagIsExplictlyApplied(EGG_PARTICLES_RGB) ||
//
//          )) {
//        dragonVariantsErrors.addError("noparticles flag is defined, but at least one of particlesname, particlesspawntype, or particlesspawncolor are also defined");
//      }

      dvc.checkForConflict(dragonVariantsErrors, EGG_PARTICLES_SPEED_OR_COLOUR, "color", true, EGG_PARTICLES_SPEED_MIN_MPS, EGG_PARTICLES_SPEED_MAX_MPS);

      dvc.checkForConflict(dragonVariantsErrors, EGG_PARTICLES_SPEED_OR_COLOUR, "speed", true, EGG_PARTICLES_RGB);

      if ((double)dvc.getValueOrDefault(EGG_PARTICLES_SPEED_MIN_MPS) > (double)dvc.getValueOrDefault(EGG_PARTICLES_SPEED_MAX_MPS)) {
        dragonVariantsErrors.addError("the value of " + EGG_PARTICLES_SPEED_MIN_MPS.getTextname() + " must be <= the value of  " + EGG_PARTICLES_SPEED_MAX_MPS.getTextname());
        dragonVariants.removeTags(Category.EGG, EGG_PARTICLES_SPEED_MIN_MPS, EGG_PARTICLES_SPEED_MAX_MPS);
      }

//      if (!"color".matches((String)dvc.getValueOrDefault(EGG_PARTICLES_SPEED_OR_COLOUR)) &&
//              dvc.tagIsExplictlyApplied(EGG_PARTICLES_RGB)
//         ) {
//        dragonVariantsErrors.addError("particlesspawncolor is defined but particlesspawntype is not set to \"color\".");
//      }

//      String spawnType = (String)dvc.getValueOrDefault(EGG_PARTICLES_SPEED_OR_COLOUR);
//      if (!"speed".matches(spawnType) && !"color".matches(spawnType)) {
//        dragonVariantsErrors.addError("particlesspawntype must be \"color\" or \"speed\"");
//      }

      try {
        int rgb = parseRGB((String)dvc.getValueOrDefault(EGG_PARTICLES_RGB));
      } catch (IllegalArgumentException iae) {
        dragonVariantsErrors.addError(iae);
      }

      if (dragonVariantsErrors.hasErrors()) {
        throw new DragonVariantsException(dragonVariantsErrors);
      }
    }
    @Override
    public void initaliseResources(DragonVariants dragonVariants, DragonVariants.ModifiedCategory modifiedCategory) throws IllegalArgumentException {
      // do nothing - no resources to initialise
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

  private static final DragonVariantTag EGG_SIZE_METRES = DragonVariantTag.addTag("size", 0.5, 0.05, 2.0,
          "the size (width and height) of the egg in metres").categories(Category.EGG);
  private static final DragonVariantTag EGG_INCUBATION_DAYS = DragonVariantTag.addTag("incubationduration", 1.0, 0.1, 10.0,
          "the number of days that the egg will incubate until it hatches").categories(Category.EGG);
  private static final DragonVariantTag EGG_WIGGLE = DragonVariantTag.addTag("wiggle", true,
          "should the egg wiggle when it gets near to hatching?").categories(Category.EGG);
  private static final DragonVariantTag EGG_GLOW = DragonVariantTag.addTag("glow", false,
          "should the egg glow when it gets near to hatching?").categories(Category.EGG);
  private static final DragonVariantTag EGG_LEVITATE = DragonVariantTag.addTag("levitate", false,
          "should the egg levitate when it gets near to hatching?").categories(Category.EGG);
  private static final DragonVariantTag EGG_SPIN = DragonVariantTag.addTag("spin", false,
          "should the egg spin when it gets near to hatching?").categories(Category.EGG);
  private static final DragonVariantTag EGG_CRACKING = DragonVariantTag.addTag("crackingsounds", true,
          "should the egg make cracking sounds when it gets near to hatching?").categories(Category.EGG);

  private static final DragonVariantTag EGG_PARTICLES_NAME = DragonVariantTag.addTag("particlesname", "reddust",
          "what particle effect does the egg produce while incubating? as per the /particle command").categories(Category.EGG);
  private static final DragonVariantTag EGG_PARTICLES = DragonVariantTag.addTag("particles", true,
          "if this flag is present, produce particles while incubating").categories(Category.EGG);
  private static final DragonVariantTag EGG_PARTICLES_SPEED_OR_COLOUR =  DragonVariantTag.addTag("particlesspawntype", "color",
          "when spawning particles, provide \"color\" or \"speed\" (depends on particle type)")
          .categories(Category.EGG).values("color", "speed");
  private static final DragonVariantTag EGG_PARTICLES_RGB =  DragonVariantTag.addTag("particlesspawncolor", "150,11,15",
          "when spawning colored particle types, what colour? format: \"Red, Green, Blue\" eg \"255,0,128\"").categories(Category.EGG);
  private static final DragonVariantTag EGG_PARTICLES_SPAWN_RATE_PER_SECOND = DragonVariantTag.addTag("particlesspawnrate", 20.0, 0.1, 50.0,
          "how frequently do particles spawn on average (number of times per second)").categories(Category.EGG);
  private static final DragonVariantTag EGG_PARTICLES_SPEED_MIN_MPS = DragonVariantTag.addTag("particlesspawnspeedmin", 1.0, 0.1, 10.0,
          "for particles which spawn with an initial speed, what is the minimum speed? (metres per second))").categories(Category.EGG);
  private static final DragonVariantTag EGG_PARTICLES_SPEED_MAX_MPS = DragonVariantTag.addTag("particlesspawnspeedmax", 1.0, 0.1, 10.0,
          "for particles which spawn with an initial speed, what is the maximum speed? (metres per second))").categories(Category.EGG);
  private static final DragonVariantTag EGG_PARTICLES_SPAWN_REGULAR_TIMES = DragonVariantTag.addTag("particlesspawnregularintervals", false,
          "if this flag is present, the particles spawn at regular intervals.  Otherwise, they spawn at random times.").categories(Category.EGG);

  private static final DragonVariantTag EGG_WIGGLE_START_FRACTION = DragonVariantTag.addTag("wigglestarttime", 0.75, 0.0, 1.0,
          "when does the incubating egg start wiggling, as a fraction of the incubation time (0 -> 1)").categories(Category.EGG);
  private static final DragonVariantTag EGG_CRACK_START_FRACTION = DragonVariantTag.addTag("crackstarttime", 0.9, 0.0, 1.0,
          "when does the incubating egg start making cracking sounds, as a fraction of the incubation time (0 -> 1)").categories(Category.EGG);
  private static final DragonVariantTag EGG_GLOW_START_FRACTION = DragonVariantTag.addTag("glowstarttime", 0.5, 0.0, 1.0,
          "when does the incubating egg start glowing, as a fraction of the incubation time (0 -> 1)").categories(Category.EGG);
  private static final DragonVariantTag EGG_LEVITATE_START_FRACTION = DragonVariantTag.addTag("levitatestarttime", 0.5, 0.0, 1.0,
          "when does the incubating egg start levitating, as a fraction of the incubation time (0 -> 1)").categories(Category.EGG);
  private static final DragonVariantTag EGG_SPIN_START_FRACTION = DragonVariantTag.addTag("spinstarttime", 0.5, 0.0, 1.0,
          "when does the incubating egg start spinning, as a fraction of the incubation time (0 -> 1)").categories(Category.EGG);
  private static final DragonVariantTag EGG_LEVITATE_HEIGHT_METRES = DragonVariantTag.addTag("levitateheight", 0.5, 0.0, 3.0,
          "what's the maximum levitation height of the incubating egg (in metres)").categories(Category.EGG);
  private static final DragonVariantTag EGG_SPIN_REVS_PER_SECOND = DragonVariantTag.addTag("spinspeed", 1.0, -3.0, 3.0,
          "how fast does the incubating egg spin, in revolutions per second?  (negative = anticlockwise)").categories(Category.EGG);

  private static final DragonVariantTag EGG_BOUNCE_RECOIL_FACTOR = DragonVariantTag.addTag("bouncerecoilfactor", 0.75, 0, 1.0,
          "when the egg falls and bounces, how much of its speed does it retain (0.0 = none, 1.0 = all)").categories(Category.EGG);

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
    public double bounceRecoilFactor;

    public boolean wiggleFlag;
    public boolean crackFlag;
    public boolean glowFlag;
    public boolean levitateFlag;
    public boolean spinFlag;

    public EnumParticleTypes enumParticleType;
    public boolean particleSpawnParameterIsColour;
    public int particleSpawnRGB;
    public double particleSpawnSpeedMinMperTick;
    public double particleSpawnSpeedMaxMperTick;
    public double particleSpawnRatePerTick;
    public boolean particlesSpawnRegularIntervals;
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
  private int incubationTicksServer = 0;
  private DragonBreedNew dragonBreed = null;
  private EggState eggState = null;

  private int UNINITIALISED_TICKS_VALUE = -1;
  private DragonBreedNew clientInitDragonBreed = null;
  private EggState clientInitEggState = null;
  private int clientInitIncubationTicks = UNINITIALISED_TICKS_VALUE;

  private int idleTicks = 0;
  private int idleTicksBeforeDisappear;
  private int eggWiggleXtickTimer;
  private int eggWiggleZtickTimer;
  private boolean eggWiggleInverseDirection;
  private double ticksUntilNextParticleSpawn = 0;

  private boolean fullyInitialised = false;
}
