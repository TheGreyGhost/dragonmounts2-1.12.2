package com.TheRPGAdventurer.ROTD.common.entity;

import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.walkers.ItemStackData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * Created by TGG on 10/08/2019.
 */
public class EntityDragonEgg extends Entity {


  public EntityItem(World worldIn, double x, double y, double z)
  {
    super(worldIn);
    this.health = 5;
    this.hoverStart = (float)(Math.random() * Math.PI * 2.0D);
    this.setSize(0.25F, 0.25F);
    this.setPosition(x, y, z);
    this.rotationYaw = (float)(Math.random() * 360.0D);
    this.motionX = (double)((float)(Math.random() * 0.20000000298023224D - 0.10000000149011612D));
    this.motionY = 0.20000000298023224D;
    this.motionZ = (double)((float)(Math.random() * 0.20000000298023224D - 0.10000000149011612D));
  }

  protected void entityInit()
  {
    this.getDataManager().register(ITEM, ItemStack.EMPTY);
    this.getDataManager().register(ROTATION, Integer.valueOf(0));
  }

  public float getCollisionBorderSize()
  {
    return 0.0F;
  }

  /**
   * Called when the entity is attacked.
   */
  public boolean attackEntityFrom(DamageSource source, float amount)
  {
    if (this.isEntityInvulnerable(source))
    {
      return false;
    }
    else if (!source.isExplosion() && !this.getDisplayedItem().isEmpty())
    {
      if (!this.world.isRemote)
      {
        this.dropItemOrSelf(source.getTrueSource(), false);
        this.playSound(SoundEvents.ENTITY_ITEMFRAME_REMOVE_ITEM, 1.0F, 1.0F);
        this.setDisplayedItem(ItemStack.EMPTY);
      }

      return true;
    }
    else
    {
      return super.attackEntityFrom(source, amount);
    }
  }

//  public int getWidthPixels()
//  {
//    return 12;
//  }
//
//  public int getHeightPixels()
//  {
//    return 12;
//  }
//
  /**
   * Checks if the entity is in range to render.
   */
  @SideOnly(Side.CLIENT)
  public boolean isInRangeToRenderDist(double distance)
  {
    double d0 = 16.0D;
    d0 = d0 * 64.0D * getRenderDistanceWeight();
    return distance < d0 * d0;
  }

  /**
   * Called when this entity is broken. Entity parameter may be null.
   */
  public void onBroken(@Nullable Entity brokenEntity)
  {
    this.playSound(SoundEvents.ENTITY_ITEMFRAME_BREAK, 1.0F, 1.0F);
    this.dropItemOrSelf(brokenEntity, true);
  }

  public void playPlaceSound()
  {
    this.playSound(SoundEvents.ENTITY_ITEMFRAME_PLACE, 1.0F, 1.0F);
  }

  public void dropItemOrSelf(@Nullable Entity entityIn, boolean p_146065_2_)
  {
    if (this.world.getGameRules().getBoolean("doEntityDrops"))
    {
      ItemStack itemstack = this.getDisplayedItem();

      if (entityIn instanceof EntityPlayer)
      {
        EntityPlayer entityplayer = (EntityPlayer)entityIn;

        if (entityplayer.capabilities.isCreativeMode)
        {
          this.removeFrameFromMap(itemstack);
          return;
        }
      }

      if (p_146065_2_)
      {
        this.entityDropItem(new ItemStack(Items.ITEM_FRAME), 0.0F);
      }

      if (!itemstack.isEmpty() && this.rand.nextFloat() < this.itemDropChance)
      {
        itemstack = itemstack.copy();
        this.removeFrameFromMap(itemstack);
        this.entityDropItem(itemstack, 0.0F);
      }
    }
  }

  /**
   * Removes the dot representing this frame's position from the map when the item frame is broken.
   */
  private void removeFrameFromMap(ItemStack stack)
  {
    if (!stack.isEmpty())
    {
      if (stack.getItem() instanceof net.minecraft.item.ItemMap)
      {
        MapData mapdata = ((ItemMap)stack.getItem()).getMapData(stack, this.world);
        mapdata.mapDecorations.remove("frame-" + this.getEntityId());
      }

      stack.setItemFrame((EntityItemFrame)null);
      this.setDisplayedItem(ItemStack.EMPTY); //Forge: Fix MC-124833 Pistons duplicating Items.
    }
  }

  public ItemStack getDisplayedItem()
  {
    return (ItemStack)this.getDataManager().get(ITEM);
  }

  public void setDisplayedItem(ItemStack stack)
  {
    this.setDisplayedItemWithUpdate(stack, true);
  }

  private void setDisplayedItemWithUpdate(ItemStack stack, boolean p_174864_2_)
  {
    if (!stack.isEmpty())
    {
      stack = stack.copy();
      stack.setCount(1);
      stack.setItemFrame(this);
    }

    this.getDataManager().set(ITEM, stack);
    this.getDataManager().setDirty(ITEM);

    if (!stack.isEmpty())
    {
      this.playSound(SoundEvents.ENTITY_ITEMFRAME_ADD_ITEM, 1.0F, 1.0F);
    }

    if (p_174864_2_ && this.hangingPosition != null)
    {
      this.world.updateComparatorOutputLevel(this.hangingPosition, Blocks.AIR);
    }
  }

  public void notifyDataManagerChange(DataParameter<?> key)
  {
    if (key.equals(ITEM))
    {
      ItemStack itemstack = this.getDisplayedItem();

      if (!itemstack.isEmpty() && itemstack.getItemFrame() != this)
      {
        itemstack.setItemFrame(this);
      }
    }
  }

  /**
   * Return the rotation of the item currently on this frame.
   */
  public int getRotation()
  {
    return ((Integer)this.getDataManager().get(ROTATION)).intValue();
  }

  public void setItemRotation(int rotationIn)
  {
    this.setRotation(rotationIn, true);
  }

  private void setRotation(int rotationIn, boolean p_174865_2_)
  {
    this.getDataManager().set(ROTATION, Integer.valueOf(rotationIn % 8));

    if (p_174865_2_ && this.hangingPosition != null)
    {
      this.world.updateComparatorOutputLevel(this.hangingPosition, Blocks.AIR);
    }
  }

  public static void registerFixesItemFrame(DataFixer fixer)
  {
    fixer.registerWalker(FixTypes.ENTITY, new ItemStackData(EntityItemFrame.class, new String[] {"Item"}));
  }

  /**
   * (abstract) Protected helper method to write subclass entity data to NBT.
   */
  public void writeEntityToNBT(NBTTagCompound compound)
  {
    if (!this.getDisplayedItem().isEmpty())
    {
      compound.setTag("Item", this.getDisplayedItem().writeToNBT(new NBTTagCompound()));
      compound.setByte("ItemRotation", (byte)this.getRotation());
      compound.setFloat("ItemDropChance", this.itemDropChance);
    }

    super.writeEntityToNBT(compound);
  }

  /**
   * (abstract) Protected helper method to read subclass entity data from NBT.
   */
  public void readEntityFromNBT(NBTTagCompound compound)
  {
    NBTTagCompound nbttagcompound = compound.getCompoundTag("Item");

    if (nbttagcompound != null && !nbttagcompound.hasNoTags())
    {
      this.setDisplayedItemWithUpdate(new ItemStack(nbttagcompound), false);
      this.setRotation(compound.getByte("ItemRotation"), false);

      if (compound.hasKey("ItemDropChance", 99))
      {
        this.itemDropChance = compound.getFloat("ItemDropChance");
      }
    }

    super.readEntityFromNBT(compound);
  }

  public boolean processInitialInteract(EntityPlayer player, EnumHand hand)
  {
    ItemStack itemstack = player.getHeldItem(hand);

    if (!this.world.isRemote)
    {
      if (this.getDisplayedItem().isEmpty())
      {
        if (!itemstack.isEmpty())
        {
          this.setDisplayedItem(itemstack);

          if (!player.capabilities.isCreativeMode)
          {
            itemstack.shrink(1);
          }
        }
      }
      else
      {
        this.playSound(SoundEvents.ENTITY_ITEMFRAME_ROTATE_ITEM, 1.0F, 1.0F);
        this.setItemRotation(this.getRotation() + 1);
      }
    }

    return true;
  }

  public int getAnalogOutput()
  {
    return this.getDisplayedItem().isEmpty() ? 0 : this.getRotation() % 8 + 1;
  }

  /**
   * Called to update the entity's position/logic.
   */
  public void onUpdate()
  {
    this.prevPosX = this.posX;
    this.prevPosY = this.posY;
    this.prevPosZ = this.posZ;

    if (this.tickCounter1++ == 100 && !this.world.isRemote)
    {
      this.tickCounter1 = 0;

      if (!this.isDead && !this.onValidSurface())
      {
        this.setDead();
        this.onBroken((Entity)null);
      }
    }
  }

  /**
   * checks to make sure painting can be placed there
   */
  public boolean onValidSurface()
  {
    if (!this.world.getCollisionBoxes(this, this.getEntityBoundingBox()).isEmpty())
    {
      return false;
    }
    else
    {
      int i = Math.max(1, this.getWidthPixels() / 16);
      int j = Math.max(1, this.getHeightPixels() / 16);
      BlockPos blockpos = this.hangingPosition.offset(this.facingDirection.getOpposite());
      EnumFacing enumfacing = this.facingDirection.rotateYCCW();
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (int k = 0; k < i; ++k)
      {
        for (int l = 0; l < j; ++l)
        {
          int i1 = (i - 1) / -2;
          int j1 = (j - 1) / -2;
          blockpos$mutableblockpos.setPos(blockpos).move(enumfacing, k + i1).move(EnumFacing.UP, l + j1);
          IBlockState iblockstate = this.world.getBlockState(blockpos$mutableblockpos);

          if (iblockstate.isSideSolid(this.world, blockpos$mutableblockpos, this.facingDirection))
            continue;

          if (!iblockstate.getMaterial().isSolid() && !BlockRedstoneDiode.isDiode(iblockstate))
          {
            return false;
          }
        }
      }

      return this.world.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox(), IS_HANGING_ENTITY).isEmpty();
    }
  }

  /**
   * Returns true if other Entities should be prevented from moving through this Entity.
   */
  public boolean canBeCollidedWith()
  {
    return true;
  }

  /**
   * Called when a player attacks an entity. If this returns true the attack will not happen.
   */
  public boolean hitByEntity(Entity entityIn)
  {
    return entityIn instanceof EntityPlayer ? this.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer)entityIn), 0.0F) : false;
  }

  /**
   * Gets the horizontal facing direction of this Entity.
   */
  public EnumFacing getHorizontalFacing()
  {
    return this.facingDirection;
  }

  /**
   * Called when the entity is attacked.
   */
  public boolean attackEntityFrom(DamageSource source, float amount)
  {
    if (this.isEntityInvulnerable(source))
    {
      return false;
    }
    else
    {
      if (!this.isDead && !this.world.isRemote)
      {
        this.setDead();
        this.markVelocityChanged();
        this.onBroken(source.getTrueSource());
      }

      return true;
    }
  }

  /**
   * Tries to move the entity towards the specified location.
   */
  public void move(MoverType type, double x, double y, double z)
  {
    if (!this.world.isRemote && !this.isDead && x * x + y * y + z * z > 0.0D)
    {
      this.setDead();
      this.onBroken((Entity)null);
    }
  }

  /**
   * Adds to the current velocity of the entity, and sets {@link #isAirBorne} to true.
   */
  public void addVelocity(double x, double y, double z)
  {
    if (!this.world.isRemote && !this.isDead && x * x + y * y + z * z > 0.0D)
    {
      this.setDead();
      this.onBroken((Entity)null);
    }
  }

  /**
   * (abstract) Protected helper method to write subclass entity data to NBT.
   */
  public void writeEntityToNBT(NBTTagCompound compound)
  {
    compound.setByte("Facing", (byte)this.facingDirection.getHorizontalIndex());
    BlockPos blockpos = this.getHangingPosition();
    compound.setInteger("TileX", blockpos.getX());
    compound.setInteger("TileY", blockpos.getY());
    compound.setInteger("TileZ", blockpos.getZ());
  }

  /**
   * (abstract) Protected helper method to read subclass entity data from NBT.
   */
  public void readEntityFromNBT(NBTTagCompound compound)
  {
    this.hangingPosition = new BlockPos(compound.getInteger("TileX"), compound.getInteger("TileY"), compound.getInteger("TileZ"));
    this.updateFacingWithBoundingBox(EnumFacing.getHorizontal(compound.getByte("Facing")));
  }

  public abstract int getWidthPixels();

  public abstract int getHeightPixels();

  /**
   * Called when this entity is broken. Entity parameter may be null.
   */
  public abstract void onBroken(@Nullable Entity brokenEntity);

  public abstract void playPlaceSound();

  /**
   * Drops an item at the position of the entity.
   */
  public EntityItem entityDropItem(ItemStack stack, float offsetY)
  {
    EntityItem entityitem = new EntityItem(this.world, this.posX + (double)((float)this.facingDirection.getFrontOffsetX() * 0.15F), this.posY + (double)offsetY, this.posZ + (double)((float)this.facingDirection.getFrontOffsetZ() * 0.15F), stack);
    entityitem.setDefaultPickupDelay();
    this.world.spawnEntity(entityitem);
    return entityitem;
  }

  protected boolean shouldSetPosAfterLoading()
  {
    return false;
  }

  /**
   * Sets the x,y,z of the entity from the given parameters. Also seems to set up a bounding box.
   */
  public void setPosition(double x, double y, double z)
  {
    this.hangingPosition = new BlockPos(x, y, z);
    this.updateBoundingBox();
    this.isAirBorne = true;
  }



}



