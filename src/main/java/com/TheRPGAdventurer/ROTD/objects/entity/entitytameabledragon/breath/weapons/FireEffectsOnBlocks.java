package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.weapons;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Created by TGG on 13/06/2019.
 * Contains a number of helper methods to determine how blocks are affected by fire
 */
public class FireEffectsOnBlocks {

  /**
   * Used this to be compatible for Biomes O Plenty, BOP Author made a switch statement on his/her blocks
   * Instead of programming the blocks one by one. I dunno if that was allowed
   */
  public static int processFlammability(Block block, World world, BlockPos sideToIgnite, EnumFacing facing) {
    int flammability = 0;
    try {
      return flammability = block.getFlammability(world, sideToIgnite, facing);
    } catch (IllegalArgumentException e) {
      return flammability = 3;
    }
  }

  /**
   * returns the hitDensity threshold for the given block flammability (0 - 300 as per Block.getFlammability)
   *
   * @param flammability
   * @return the hit density threshold above which the block catches fire
   */
  public static float convertFlammabilityToHitDensityThreshold(int flammability) {
    checkArgument(flammability >= 0 && flammability <= 300);
    if (flammability == 0) return Float.MAX_VALUE;
    // typical values for items are 5 (coal, logs), 20 (gates etc), 60 - 100 for leaves & flowers & grass
    // want: leaves & flowers to burn instantly; gates to take ~1 second at full power, coal / logs to take ~3 seconds
    // hitDensity of 1 is approximately 1-2 ticks of full exposure from a single beam, so 3 seconds is ~30

    float threshold = 15.0F / flammability;
    return threshold;
  }


  /**
   * For the given block, return information about what happens when it's exposed to fire:
   * @param iBlockState the block exposed to fire
   * @return What the block is transformed into, and the exposure threshold required to transform it
   */
  public static BlockBurnProperties getBurnProperties(IBlockState iBlockState) {
    Block block = iBlockState.getBlock();
    if (blockBurnPropertiesCache.containsKey(block)) {
      return blockBurnPropertiesCache.get(block);
    }

    BlockBurnProperties blockBurnProperties = new BlockBurnProperties();
    IBlockState result = getSmeltingResult(iBlockState);
    blockBurnProperties.threshold = 20;
//    if (result == null) {
//      blockBurnProperties.threshold = 3;
//      result = getScorchedResult(iBlockState);
//    }
    if (result == null) {
      blockBurnProperties.threshold = 5;
      result = getVaporisedLiquidResult(iBlockState);
    }
    if (result == null) {
      blockBurnProperties.threshold = 100;
      result = getMoltenLavaResult(iBlockState);
    }
    blockBurnProperties.burnResult = result;
    blockBurnPropertiesCache.put(block, blockBurnProperties);
    return blockBurnProperties;
  }

  public static class BlockBurnProperties {
    public IBlockState burnResult = null;  // null if no effect
    public float threshold;
  }

  /**
   * if sourceBlock can be smelted, return the smelting result as a block
   *
   * @param sourceBlock
   * @return the smelting result, or null if none
   */
  public static IBlockState getSmeltingResult(IBlockState sourceBlock) {
    Block block = sourceBlock.getBlock();
    Item itemFromBlock = Item.getItemFromBlock(block);
    ItemStack itemStack;
    if (itemFromBlock != null && itemFromBlock.getHasSubtypes())     {
      int metadata = block.getMetaFromState(sourceBlock);
      itemStack = new ItemStack(itemFromBlock, 1, metadata);
    } else {
      itemStack = new ItemStack(itemFromBlock);
    }

    ItemStack smeltingResult = FurnaceRecipes.instance().getSmeltingResult(itemStack);
    if (smeltingResult != null) {
      Block smeltedResultBlock = Block.getBlockFromItem(smeltingResult.getItem());
      if (smeltedResultBlock != null) {
        IBlockState iBlockStateSmelted = smeltedResultBlock.getStateFromMeta(smeltingResult.getMetadata());
        return iBlockStateSmelted;
      }
    }
    if (block == Blocks.IRON_ORE) return Blocks.IRON_BLOCK.getDefaultState();
    if (block == Blocks.GOLD_ORE) return Blocks.GOLD_BLOCK.getDefaultState();
    if (block == Blocks.EMERALD_ORE) return Blocks.EMERALD_BLOCK.getDefaultState();
    if (block == Blocks.DIAMOND_ORE) return Blocks.DIAMOND_BLOCK.getDefaultState();
    if (block == Blocks.COAL_ORE) return Blocks.COAL_BLOCK.getDefaultState();
    if (block == Blocks.REDSTONE_ORE) return Blocks.REDSTONE_BLOCK.getDefaultState();
    if (block == Blocks.LAPIS_ORE) return Blocks.LAPIS_BLOCK.getDefaultState();
    if (block == Blocks.QUARTZ_ORE) return Blocks.QUARTZ_BLOCK.getDefaultState();
    return null;
  }

  /**
   * if sourceBlock is a liquid or snow that can be molten or vaporised, return the result as a block
   *
   * @param sourceBlock
   * @return the vaporised result, or null if none
   */
  public static IBlockState getVaporisedLiquidResult(IBlockState sourceBlock) {
    Block block = sourceBlock.getBlock();
    Material material = block.getDefaultState().getMaterial();

    if (material == Material.SNOW || material == Material.ICE) {
      final int SMALL_LIQUID_AMOUNT = 4;
      return Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, SMALL_LIQUID_AMOUNT);
    } else if (material == Material.PACKED_ICE || material == Material.CRAFTED_SNOW) {
      final int LARGE_LIQUID_AMOUNT = 1;
      return Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, LARGE_LIQUID_AMOUNT);
    }
    return null;
  }

  /**
   * if sourceBlock is a block that can be melted to lava, return the result as a block
   *
   * @param sourceBlock
   * @return the molten lava result, or null if none
   */
  public static IBlockState getMoltenLavaResult(IBlockState sourceBlock) {
    Block block = sourceBlock.getBlock();
    Material material = block.getDefaultState().getMaterial();

    if (material == Material.SAND || material == Material.CLAY
            || material == Material.GLASS || material == Material.IRON
            || material == Material.GROUND || material == Material.ROCK
            || block != Blocks.BEDROCK || block != Blocks.OBSIDIAN) {
      final int LARGE_LIQUID_AMOUNT = 1;
      return Blocks.LAVA.getDefaultState().withProperty(BlockLiquid.LEVEL, LARGE_LIQUID_AMOUNT);
    }
    return null;
  }

  /**
   * if sourceBlock is a block that isn't flammable but can be scorched / changed, return the result as a block
   *
   * @param sourceBlock
   * @return the scorched result, or null if none
   */
  public static IBlockState getScorchedResult(IBlockState sourceBlock) {
    Block block = sourceBlock.getBlock();
    Material material = block.getDefaultState().getMaterial();

    if (material == Material.GRASS) {
      return Blocks.DIRT.getDefaultState();
    }
    return null;
  }

  /**
   * Attempt to set fire to this block - random chance.
   * @param sideToIgnite
   * @param rand
   * @param percentChanceOfIgnition nominally 0% - 100%
   * @param world
   */
  public static void burnBlocks(BlockPos sideToIgnite, Random rand, double percentChanceOfIgnition, World world) {

    if (rand.nextInt(10000) < percentChanceOfIgnition * 10000)
      world.setBlockState(sideToIgnite, Blocks.FIRE.getDefaultState());
  }

  static private HashMap<Block, BlockBurnProperties> blockBurnPropertiesCache = new HashMap<Block, BlockBurnProperties>();
}
