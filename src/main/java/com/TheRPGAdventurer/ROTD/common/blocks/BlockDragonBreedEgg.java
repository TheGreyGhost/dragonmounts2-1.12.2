/*
** 2016 March 09
**
** The author disclaims copyright to this source code. In place of
** a legal notice, here is a blessing:
**    May you do good and not evil.
**    May you find forgiveness for yourself and forgive others.
**    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.common.blocks;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.gui.DragonMountsConfig;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.EnumDragonBreed;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.helper.DragonLifeStage;
import com.TheRPGAdventurer.ROTD.util.DMUtils;
import net.minecraft.block.BlockDragonEgg;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;

import java.util.Random;

/**
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 *         Modified by WolfShotz <p>
 */
public class BlockDragonBreedEgg extends BlockDragonEgg {

  public static final PropertyEnum<EnumDragonBreed> BREED = EnumDragonBreed.BREED;  // PropertyEnum.create("breed", EnumDragonBreed.class);
  public static BlockDragonBreedEgg DRAGON_BREED_EGG;
  public static final BlockDragonBreedEgg[] BLOCK_EGG = {DRAGON_BREED_EGG = new BlockDragonBreedEgg()};
  public static int meta;

  public BlockDragonBreedEgg() {
    setUnlocalizedName("dragonEgg");
    setHardness(0);
    setResistance(30);
    setSoundType(SoundType.WOOD);
    setLightLevel(0.125f);
    setCreativeTab(DragonMounts.mainTab);

  }

  @Override
  public IBlockState getStateFromMeta(int meta) {
    this.meta = meta;
    return EnumDragonBreed.getBlockstateFromMeta(this, meta);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return EnumDragonBreed.getMetaFromBlockState(state);
  }

  @Override
  public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
    for (EnumDragonBreed breed : EnumDragonBreed.values()) {
      items.add(breed.createEggItemStack());
    }
  }

  @Override
  public int damageDropped(IBlockState state) {
    return getMetaFromState(state);
  }

  @Override
  public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
    this.checkFall(worldIn, pos);
  }

  @Override
  public void onBlockClicked(World worldIn, BlockPos pos, EntityPlayer player) {
    return;
  }

  /**
   * Called when the block is right clicked by a player.
   */
  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (worldIn.isRemote || DragonMountsConfig.isDisableBlockOverride()) return false;
    if (worldIn.provider.getDimensionType() == DimensionType.THE_END) {
      player.sendStatusMessage(new TextComponentTranslation(DMUtils.translateToLocal("egg.cantHatchEnd.DragonMounts")), true);
      return false;
    }

    EntityTameableDragon dragon = new EntityTameableDragon(worldIn);
    dragon.setBreedType(worldIn.getBlockState(pos).getValue(BlockDragonBreedEgg.BREED));
    worldIn.setBlockToAir(pos); // Set to air AFTER setting breed type
    dragon.getLifeStageHelper().setLifeStage(DragonLifeStage.EGG);
    dragon.getReproductionHelper().setBreeder(player);
    dragon.setPosition(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5);
    if (worldIn.isRemote)
      dragon.world.playSound(player, dragon.getPosition(), SoundEvents.BLOCK_WOOD_HIT, SoundCategory.PLAYERS, 1, 1);


    worldIn.spawnEntity(dragon);

    return true;

  }

  @Override
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, BREED);
  }

  private void checkFall(World worldIn, BlockPos pos) {
    if (worldIn.isAirBlock(pos.down()) && BlockFalling.canFallThrough(worldIn.getBlockState(pos.down())) && pos.getY() >= 0) {
      int i = 32;

      if (!BlockFalling.fallInstantly && worldIn.isAreaLoaded(pos.add(-32, -32, -32), pos.add(32, 32, 32))) {
        worldIn.spawnEntity(new EntityFallingBlock(worldIn, (double) ((float) pos.getX() + 0.5F), (double) pos.getY(), (double) ((float) pos.getZ() + 0.5F), this.getStateFromMeta(meta)));
      } else {
        worldIn.setBlockToAir(pos);
        BlockPos blockpos;

        for (blockpos = pos; worldIn.isAirBlock(blockpos) && BlockFalling.canFallThrough(worldIn.getBlockState(blockpos)) && blockpos.getY() > 0; blockpos = blockpos.down()) {
        }

        if (blockpos.getY() > 0) {
          worldIn.setBlockState(blockpos, this.getStateFromMeta(meta), 2);
        }
      }
    }
  }

}