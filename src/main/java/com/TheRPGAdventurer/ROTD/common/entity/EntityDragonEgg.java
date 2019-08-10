package com.TheRPGAdventurer.ROTD.common.entity;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializer;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.io.IOException;

/**
 * Created by TGG on 10/08/2019.
 */
public class EntityDragonEgg extends Entity {


  public EntityDragonEgg(World worldIn, DragonBreedNew dragonBreed, double x, double y, double z) {
    super(worldIn);
    this.dragonBreed = dragonBreed;
    this.setSize(0.25F, 0.25F);
    this.setPosition(x, y, z);
    this.rotationYaw = (float) (Math.random() * 360.0D);
    this.motionX = 0;
    this.motionY = 0;
    this.motionZ = 0;
    eggState = EggState.INCUBATING;
  }

  @Override
  protected void entityInit() {
    dragonBreed.registerDataParameter(this.getDataManager(), DATAPARAM_BREED);
    eggState.registerDataParameter(this.getDataManager(), DATAPARAM_EGGSTATE);
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
  }

  /**
   * (abstract) Protected helper method to write subclass entity data to NBT.
   */
  @Override
  public void writeEntityToNBT(NBTTagCompound compound) {
    dragonBreed.writeToNBT(compound);
    eggState.writeToNBT(compound);
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
    changeBreed(newBreed);
    changeEggState(newEggState);
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
      ++this.age;
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

    if (!this.world.isRemote && this.age >= lifespan) {
      this.setDead();
    }
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
      lifespan = (newEggState == EggState.SMASHED) ? LIFESPAN_SMASHED : LIFESPAN_HATCHED;
    }
    eggState = newEggState;
  }

  private void changeBreed(DragonBreedNew newDragonBreed) {
    dragonBreed = newDragonBreed;
  }

  private DragonBreedNew dragonBreed;
  private EggState eggState = EggState.INCUBATING;
  private int age = 0;
  private int lifespan;

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

}



