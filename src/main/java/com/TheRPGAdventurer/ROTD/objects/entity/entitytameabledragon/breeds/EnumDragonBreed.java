package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breeds;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.objects.blocks.BlockDragonBreedEgg;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public enum EnumDragonBreed implements IStringSerializable {

    AETHER(0, DragonBreedAir::new),
    FIRE(1, DragonBreedFire::new),
    FOREST(2, DragonBreedForest::new),
    SYLPHID(3, DragonBreedWater::new),
    ICE(4, DragonBreedIce::new),
    END(5, DragonBreedEnd::new),
    NETHER(6, DragonBreedNether::new),
    SKELETON(7, DragonBreedSkeleton::new),
    WITHER(8, DragonBreedWither::new),
    ENCHANT(9, DragonBreedEnchant::new),
    SUNLIGHT(10, DragonBreedSunlight::new),
    STORM(11, DragonBreedStorm::new),
    ZOMBIE(12, DragonBreedZombie::new),
    TERRA(13, DragonBreedTerra::new),
    MOONLIGHT(14, DragonBreedMoonlight::new);
//	LIGHT(15, DragonBreedLight::new);
//	DARK(16, DragonBreedDark::new);
//	SPECTER(17, DragonBreedSpecter::new);

    // from prototypes
//    AIR(0, DragonBreedAir::new),
//    END(1, DragonBreedEnd::new),
//    FIRE(2, DragonBreedFire::new),
//    FOREST(3, DragonBreedForest::new),
//    GHOST(4, DragonBreedGhost::new),
//    ICE(5, DragonBreedIce::new),
//    NETHER(6, DragonBreedNether::new),
//    WATER(7, DragonBreedWater::new);

    // create static bimap between enums and meta data for faster and easier
    // lookups
    private static final BiMap<EnumDragonBreed, Integer> META_MAPPING = ImmutableBiMap.copyOf(Arrays.asList(values()).stream().collect(Collectors.toMap(Function.identity(), EnumDragonBreed::getMeta)));
    public static final PropertyEnum<EnumDragonBreed> BREED = PropertyEnum.create("breed", EnumDragonBreed.class);
    private final DragonBreed breed;

    // this field is used for block metadata and is technically the same as
    // ordinal(), but it is saved separately to make sure the values stay
    // constant after adding more breeds in unexpected orders
    private final int meta;

    EnumDragonBreed(int meta, Supplier<DragonBreed> factory) {
        this.breed = factory.get();
        this.meta = meta;
    }

    public DragonBreed getBreed() {
        return breed;
    }

    private int getMeta() {
        return meta;
    }

  public static ImmutableBiMap<EnumDragonBreed, Integer> getAllBreedMetas()
  {
    return ImmutableBiMap.copyOf(META_MAPPING);
  }

    @Deprecated
    // for debugging purposes...
    public static Set<Integer> getAllMetas()
    {
      return META_MAPPING.values();
    }

  @Deprecated
  // for debugging purposes...
  public static EnumDragonBreed getBreedForMeta(int meta)
  {
    return META_MAPPING.inverse().get(meta);
  }

  // which Breed does this egg produce?
  // default = FIRE
  public static EnumDragonBreed getBreedFromItemStack(ItemStack dragonEgg)
  {
    int metaValue = dragonEgg.getMetadata();
    if (META_MAPPING.containsValue(metaValue)) {
      EnumDragonBreed breed = EnumDragonBreed.META_MAPPING.inverse().get(metaValue);
      return breed;
    } else {
      return FIRE;
    }
  }

  // generate an ItemStack egg for this breed
  public ItemStack createEggItemStack()
  {
    final int AMOUNT = 1;
    return new ItemStack(BlockDragonBreedEgg.DRAGON_BREED_EGG, AMOUNT, meta);
  }

  public static int getMetaFromBlockState(IBlockState state) {
    EnumDragonBreed type = state.getValue(BREED);
    return EnumDragonBreed.META_MAPPING.get(type);
  }

  public static IBlockState getBlockstateFromMeta(BlockDragonBreedEgg blockDragonBreedEgg, int metaValue)
  {
    EnumDragonBreed breed = FIRE;  // default if unknown breed
    if (META_MAPPING.containsValue(metaValue)) {
      breed = EnumDragonBreed.META_MAPPING.inverse().get(metaValue);
    } else {
      DragonMounts.loggerLimit.error_once("Invalid meta given to EnumDragonBreed::getBlockstateFromMeta=" + metaValue);
    }

    return blockDragonBreedEgg.getDefaultState().withProperty(BREED, breed);
  }

  @Override
    public String getName() {
        return name().toLowerCase();
    }

    public int getNumberOfNeckSegments() {
        return 7;
    }

    public int getNumberOfTailSegments() {
        return 12;
    }

    public int getNumberOfWingFingers() {
        return 4;
    }

}

