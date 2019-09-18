package com.TheRPGAdventurer.ROTD.common.items;


import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.blocks.BlockDragonBreedEgg;
import com.TheRPGAdventurer.ROTD.common.entity.EntityDragonEgg;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew.DragonBreedsRegistry;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonFactory;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.EnumDragonBreed;
import com.TheRPGAdventurer.ROTD.common.inits.ModItems;
import com.TheRPGAdventurer.ROTD.util.DMUtils;
import com.TheRPGAdventurer.ROTD.util.IHasModel;
import net.minecraft.block.BlockLiquid;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemDragonHatchableEgg extends Item {

  public ItemDragonHatchableEgg() {
    this.setUnlocalizedName("dragon_hatchable_egg");
    this.setRegistryName(new ResourceLocation(DragonMounts.MODID, "dragon_hatchable_egg"));
    this.setMaxDamage(0);
    this.setMaxStackSize(1);
    this.setHasSubtypes(true);
    this.setCreativeTab(DragonMounts.mainTab);

    ModItems.ITEMS.add(this);
  }

  @Override
  public int getMetadata(int metadata) {
    return metadata;
  }

  @Override
  public String getItemStackDisplayName(ItemStack stack) {
    DragonBreedNew breed = DragonBreedsRegistry.getDefaultRegistry().getDefaultBreed();
    try {
      breed = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getBreed(stack.getTagCompound());
    } catch (IllegalArgumentException iae) {
      DragonMounts.loggerLimit.warn_once("Dragon egg had unknown breed.");
    }
    String breedName = breed.getLocalisedName();
    return net.minecraft.util.text.translation.I18n.translateToLocalFormatted("item.dragon_egg.name", breedName);
  }

  // Code mostly copied from ItemMonsterPlacer

  /**
   * Called when a Block is right-clicked with this Item
   */
  public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    ItemStack itemstack = player.getHeldItem(hand);

    if (worldIn.isRemote) {
      return EnumActionResult.SUCCESS;
    } else if (!player.canPlayerEdit(pos.offset(facing), facing, itemstack)) {
      return EnumActionResult.FAIL;
    }
    BlockPos blockpos = pos.offset(facing);
    double yOffset = this.getYOffset(worldIn, blockpos);

    if (worldIn.provider.getDimensionType() == DimensionType.THE_END) {
      player.sendStatusMessage(new TextComponentTranslation(DMUtils.translateToLocal("egg.cantHatchEnd.DragonMounts")), true);
      return EnumActionResult.FAIL;
    }
    DragonBreedNew breed;
    try {
      breed = getBreedFrom(itemstack);
    } catch (IllegalArgumentException iae) {
      DragonMounts.loggerLimit.warn_once(iae.getMessage());
      return EnumActionResult.FAIL;
    }

    EntityDragonEgg entityDragonEgg = spawnEgg(worldIn, breed, blockpos.getX() + 0.5D, blockpos.getY() + yOffset, blockpos.getZ() + 0.5D);
    if (entityDragonEgg == null) {
      return EnumActionResult.FAIL;
    }
    if (!player.capabilities.isCreativeMode) {
      itemstack.shrink(1);
    }
    return EnumActionResult.SUCCESS;
  }

  protected double getYOffset(World world, BlockPos blockPos) {
    AxisAlignedBB axisalignedbb = (new AxisAlignedBB(blockPos)).expand(0.0D, -1.0D, 0.0D);
    List<AxisAlignedBB> collidingAABB = world.getCollisionBoxes(null, axisalignedbb);

    if (collidingAABB.isEmpty()) {
      return 0.0D;
    } else {
      double highestY = axisalignedbb.minY;

      for (AxisAlignedBB axisalignedbb1 : collidingAABB) {
        highestY = Math.max(axisalignedbb1.maxY, highestY);
      }

      return highestY -  blockPos.getY();
    }
  }

  /**
   * Called when the equipped item is right clicked.
   */
  public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
    ItemStack itemstack = playerIn.getHeldItem(handIn);
    if (worldIn.isRemote) {
      return new ActionResult<ItemStack>(EnumActionResult.PASS, itemstack);
    } else {
      RayTraceResult raytraceresult = this.rayTrace(worldIn, playerIn, true);

      if (raytraceresult != null && raytraceresult.typeOfHit == RayTraceResult.Type.BLOCK) {
        BlockPos blockpos = raytraceresult.getBlockPos();

        if (!(worldIn.getBlockState(blockpos).getBlock() instanceof BlockLiquid)) {
          return new ActionResult<ItemStack>(EnumActionResult.PASS, itemstack);
        } else if (worldIn.isBlockModifiable(playerIn, blockpos) && playerIn.canPlayerEdit(blockpos, raytraceresult.sideHit, itemstack)) {
          if (worldIn.provider.getDimensionType() == DimensionType.THE_END) {
            playerIn.sendStatusMessage(new TextComponentTranslation(DMUtils.translateToLocal("egg.cantHatchEnd.DragonMounts")), true);
            return new ActionResult<ItemStack>(EnumActionResult.FAIL, itemstack);
          }
          DragonBreedNew breed;
          try {
            breed = getBreedFrom(itemstack);
          } catch (IllegalArgumentException iae) {
            DragonMounts.loggerLimit.warn_once(iae.getMessage());
            return ActionResult.newResult(EnumActionResult.FAIL, itemstack);
          }

          Entity entity = spawnEgg(worldIn, breed, blockpos.getX() + 0.5D, blockpos.getY() + 0.5D, blockpos.getZ() + 0.5D);

          if (entity == null) {
            return new ActionResult<ItemStack>(EnumActionResult.PASS, itemstack);
          } else {
            if (!playerIn.capabilities.isCreativeMode) {
              itemstack.shrink(1);
            }

            playerIn.addStat(StatList.getObjectUseStats(this));
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, itemstack);
          }
        } else {
          return new ActionResult<ItemStack>(EnumActionResult.FAIL, itemstack);
        }
      } else {
        return new ActionResult<ItemStack>(EnumActionResult.PASS, itemstack);
      }
    }
  }

  /**
   * Spawns the creature specified by the egg's type in the location specified by the last three parameters.
   * Parameters: world, entityID, x, y, z.
   */
  @Nullable
  public static EntityDragonEgg spawnEgg(World worldIn, DragonBreedNew breed, double x, double y, double z) {
    EntityDragonEgg entityDragonEgg = DragonFactory.getDefaultDragonFactory().createEgg(worldIn, breed);
    entityDragonEgg.setLocationAndAngles(x, y, z, MathHelper.wrapDegrees(worldIn.rand.nextFloat() * 360.0F), 0.0F);
//    dragon.getReproductionHelper().setBreeder(player);
    boolean success = worldIn.spawnEntity(entityDragonEgg);
    if (!success) return null;

    if (worldIn.isRemote) {
      worldIn.playSound(null, entityDragonEgg.getPosition(), SoundEvents.BLOCK_WOOD_HIT, SoundCategory.PLAYERS, 1, 1);
    }
    return entityDragonEgg;
  }

  /**
   * returns a list of items with the same ID, but different meta (eg: dye returns 16 items)
   */
  public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
    if (this.isInCreativeTab(tab)) {
      for (DragonBreedNew breed : DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getAllNonDefaultBreeds()) {
        ItemStack itemstack = new ItemStack(this, 1);
        applyBreedToItemStack(itemstack, breed);
        items.add(itemstack);
      }
    }
  }

  /**
   * APplies the given entity ID to the given ItemStack's NBT data.
   */
  public static void applyBreedToItemStack(ItemStack stack, DragonBreedNew breed) {
    NBTTagCompound nbttagcompound = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
    breed.writeToNBT(nbttagcompound);
    stack.setTagCompound(nbttagcompound);
  }

  /**
   * Gets the entity type ID from the given itemstack.
   *
   * @return The type ID, or {@code null} if there is no valid tag on the item.
   */

  public static DragonBreedNew getBreedFrom(ItemStack stack) throws IllegalArgumentException {
    NBTTagCompound nbttagcompound = stack.getTagCompound();
    return DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getBreed(nbttagcompound);
  }

}
