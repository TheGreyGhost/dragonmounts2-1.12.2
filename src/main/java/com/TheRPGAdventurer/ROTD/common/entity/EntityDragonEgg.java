package com.TheRPGAdventurer.ROTD.common.entity;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.helper.DragonLifeStage;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.inits.ModSounds;
import com.TheRPGAdventurer.ROTD.util.ClientServerSynchronisedTickCount;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.io.IOException;

/**
 * Created by TGG on 10/08/2019.
 */
public class EntityDragonEgg extends Entity {


  public EntityDragonEgg(World worldIn, DragonBreedNew dragonBreed, DragonVariants dragonVariants, double x, double y, double z) {
    super(worldIn);
    this.dragonBreed = dragonBreed;
    this.setSize(0.25F, 0.25F);
    this.setPosition(x, y, z);
    this.rotationYaw = (float) (Math.random() * 360.0D);
    this.motionX = 0;
    this.motionY = 0;
    this.motionZ = 0;
    eggState = EggState.INCUBATING;
    if (world.isRemote) {
      incubationTicksClient = new ClientServerSynchronisedTickCount(TICKS_SINCE_CREATION_UPDATE_INTERVAL);
      incubationTicksClient.reset(incubationTicksServer);
    } else {
      incubationTicksClient = null;
      incubationTicksServer = 0;
    }
  }

  @Override
  protected void entityInit() {
    dragonBreed.registerDataParameter(this.getDataManager(), DATAPARAM_BREED);
    eggState.registerDataParameter(this.getDataManager(), DATAPARAM_EGGSTATE);
    getDataManager().register(DATAPARAM_INCUBATIONTICKS, incubationTicksServer);
  }

  /**
   * Called when the entity is attacked.
   */
  @Override
  public boolean attackEntityFrom(DamageSource source, float amount) {
    if (this.isEntityInvulnerable(source) || amount <= 0) {
      return false;
    } else {
      changeEggState(EggState.SMASHED);
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
    EggState newEggState = EggState.getStateFromNBT(compound);
    int incubationTicks = compound.getInteger(NBT_INCUBATION_TICKS);
    changeBreed(newBreed);
    changeEggState(newEggState);
    setIncubationTicks(incubationTicks);
  }

  /**
   * Called to update the entity's position/logic.
   */
  @Override
  public void onUpdate() {
    super.onUpdate();

    // code adapted from EntityItem

    this.prevPosX = this.posX;
    this.prevPosY = this.posY;
    this.prevPosZ = this.posZ;
    double initialMotionX = this.motionX;
    double initialMotionY = this.motionY;
    double initialMotionZ = this.motionZ;

    if (!this.hasNoGravity()) {
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

    if (eggState == EggState.HATCHED || eggState == EggState.SMASHED) {
      ++this.idleTicks;
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

    if (!this.world.isRemote && this.idleTicks >= idleTicksBeforeDisappear) {
      this.setDead();
    }

    updateIncubationTime();
    updateEggAnimation();
  }

  private void updateEggAnimation() {
    // animate egg wiggle based on the time the eggs take to hatch
    float progress = DragonLifeStage.getStageProgressFromTickCount(getIncubationTicks());

    // wait until the egg is nearly hatched
    if (progress > EGG_WIGGLE_THRESHOLD) {
      float wiggleChance = (progress - EGG_WIGGLE_THRESHOLD) / EGG_WIGGLE_BASE_CHANCE * (1 - EGG_WIGGLE_THRESHOLD);

      if (eggWiggleX > 0) {
        eggWiggleX--;
      } else if (rand.nextFloat() < wiggleChance) {
        eggWiggleX = rand.nextBoolean() ? 10 : 20;
        if (progress > EGG_CRACK_THRESHOLD) {
          playEggCrackEffect();
        }
      }

      if (eggWiggleZ > 0) {
        eggWiggleZ--;
      } else if (rand.nextFloat() < wiggleChance) {
        eggWiggleZ = rand.nextBoolean() ? 10 : 20;
        if (progress > EGG_CRACK_THRESHOLD) {
          playEggCrackEffect();
        }
      }
    }

    // spawn generic particles
    double px = posX + (rand.nextDouble() - 0.3);
    double py = posY + (rand.nextDouble() - 0.3);
    double pz = posZ + (rand.nextDouble() - 0.3);
    double ox = (rand.nextDouble() - 0.3) * 2;
    double oy = (rand.nextDouble() - 0.3) * 2;
    double oz = (rand.nextDouble() - 0.3) * 2;
    world.spawnParticle(this.getEggParticle(), px, py, pz, ox, oy, oz);
  }


  private EnumParticleTypes getEggParticle() {
    return EnumParticleTypes.TOWN_AURA;
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

  public int getIncubationTicks() {
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

  public int getEggWiggleX() {
    return eggWiggleX;
  }

  public int getEggWiggleZ() {
    return eggWiggleZ;
  }

  /**
   * Returns true if other Entities should be prevented from moving through this Entity.
   */
  @Override
  public boolean canBeCollidedWith() {
    return eggState == EggState.INCUBATING;
  }

  private void changeEggState(EggState newEggState) {
    final int LIFESPAN_SMASHED = 20 * 60;
    final int LIFESPAN_HATCHED = 20 * 60;

    if (eggState == EggState.INCUBATING && newEggState != EggState.INCUBATING) {
      idleTicksBeforeDisappear = (newEggState == EggState.SMASHED) ? LIFESPAN_SMASHED : LIFESPAN_HATCHED;
    }
    eggState = newEggState;
  }

  /**
   * Generates some egg shell particles and a breaking sound.
   */
  public void playEggCrackEffect() {
    world.playSound(null, posX, posY, posZ, ModSounds.DRAGON_HATCHING, SoundCategory.BLOCKS, +1.0F, 1.0F);
  }

  private void changeBreed(DragonBreedNew newDragonBreed) {
    dragonBreed = newDragonBreed;
  }

  private DragonBreedNew dragonBreed;
  private EggState eggState = EggState.INCUBATING;
  private int idleTicks = 0;
  private int idleTicksBeforeDisappear;

  public enum EggState {
    INCUBATING, HATCHED, SMASHED;

    public void registerDataParameter(EntityDataManager entityDataManager, DataParameter<EggState> dataParameter) {
      entityDataManager.register(dataParameter, this);
    }

    public static EggState getStateFromDataParam(EntityDataManager entityDataManager, DataParameter<EggState> dataParameter) {
      try {
        EggState newEggState = entityDataManager.get(DATAPARAM_EGGSTATE);
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

    public void writeToNBT(NBTTagCompound nbt) {
      nbt.setInteger(NBT_EGGSTATE, this.ordinal());
    }

    private static final String NBT_EGGSTATE = "EggState";
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

  private static final DataParameter<String> DATAPARAM_BREED = EntityDataManager.createKey(EntityDragonEgg.class, DataSerializers.STRING);
  private static final DataParameter<EggState> DATAPARAM_EGGSTATE = EntityDataManager.createKey(EntityDragonEgg.class, EGG_STATE_SERIALIZER);
  private static final DataParameter<Integer> DATAPARAM_INCUBATIONTICKS = EntityDataManager.createKey(EntityDragonEgg.class, DataSerializers.VARINT);
  private int eggWiggleX;
  private int eggWiggleZ;

  private static final String NBT_INCUBATION_TICKS = "IncubationTicks";
  private static final int TICKS_SINCE_CREATION_UPDATE_INTERVAL = 100;
  private static final float EGG_CRACK_THRESHOLD = 0.9f;
  private static final float EGG_WIGGLE_THRESHOLD = 0.75f;
  private static final float EGG_WIGGLE_BASE_CHANCE = 20;

  // the ticks since creation is used to control the egg's hatching time.  It is only updated by the server occasionally.
  // the client keeps a cached copy of it and uses client ticks to interpolate in the gaps.
  // when the watcher is updated from the server, the client will tick it faster or slower to resynchronise
  private final ClientServerSynchronisedTickCount incubationTicksClient;
  private int incubationTicksServer;
}
