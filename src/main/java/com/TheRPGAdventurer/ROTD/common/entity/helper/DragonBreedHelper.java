/*
 ** 2013 March 23
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.common.entity.helper;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.gui.DragonMountsConfig;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreed;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.EnumDragonBreed;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import net.minecraft.block.Block;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.apache.commons.lang3.EnumUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class for breed properties.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class DragonBreedHelper extends DragonHelper {

  public DragonBreedHelper(EntityTameableDragon dragon, DragonBreedNew dragonBreedNew,
                           DataParameter<String> dataParamBreed, DataParameter<String> dataParamBreedNew) {
    super(dragon);

    this.dataParam = dataParamBreed;
    this.dataParamNew = dataParamBreedNew;
    this.dragonBreedNew = dragonBreedNew;

    entityDataManager.register(dataParamBreed, EnumDragonBreed.FIRE.getName());
    dragonBreedNew.registerDataParameter(entityDataManager, dataParamBreedNew);
//    entityDataManager.register(dataParamBreed, EnumDragonBreed.END.getName());

    if (dragon.isServer()) {
      // initialize map to avoid future checkings
      for (EnumDragonBreed type : EnumDragonBreed.values()) {
        breedPoints.put(type, new AtomicInteger());
      }

      // default breed has initial points
      breedPoints.get(EnumDragonBreed.FIRE).set(POINTS_INITIAL);
    }

  }

  @Override
  public void writeToNBT(NBTTagCompound nbt) {
    nbt.setString(NBT_BREED, getBreedType().getName());
    dragonBreedNew.writeToNBT(nbt);

    NBTTagCompound breedPointTag = new NBTTagCompound();
    breedPoints.forEach((type, points) -> {
      breedPointTag.setInteger(type.getName(), points.get());
    });
    nbt.setTag(NBT_BREED_POINTS, breedPointTag);
  }

  @Override
  public void readFromNBT(NBTTagCompound nbt) {
    // read breed name and convert it to the corresponding breed object
    String breedName = nbt.getString(NBT_BREED);
    EnumDragonBreed breed = EnumUtils.getEnum(EnumDragonBreed.class, breedName.toUpperCase());
    if (breed == null) {
      breed = EnumDragonBreed.FIRE;
      L.warn("Dragon {} loaded with invalid breed type {}, using {} instead",
              dragon.getEntityId(), breedName, breed);
    }
    setBreedType(breed);

    try {
      dragonBreedNew = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getBreed(nbt);
    } catch (IllegalArgumentException iae) {
      DragonMounts.loggerLimit.warn_once(iae.getMessage());
      dragonBreedNew = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getDefaultBreed();
    }

    // read breed points
    NBTTagCompound breedPointTag = nbt.getCompoundTag(NBT_BREED_POINTS);
    breedPoints.forEach((type, points) -> {
      points.set(breedPointTag.getInteger(type.getName()));
    });
  }

  public Map<EnumDragonBreed, AtomicInteger> getBreedPoints() {
    return Collections.unmodifiableMap(breedPoints);
  }

  public EnumDragonBreed getBreedType() {
    String breedName = entityDataManager.get(dataParam);
    return EnumUtils.getEnum(EnumDragonBreed.class, breedName.toUpperCase());
  }

  public DragonBreedNew getDragonBreedNew() {return dragonBreedNew;}

  public void setBreedType(EnumDragonBreed newType) {
    L.trace("setBreed({})", newType);

    // ignore breed changes on client side, it's controlled by the server
    if (dragon.isClient()) {
      return;
    }

    Objects.requireNonNull(newType);

    // check if the breed actually changed
    EnumDragonBreed oldType = getBreedType();
    if (oldType == newType) {
      return;
    }

    DragonBreed oldBreed = oldType.getBreed();
    DragonBreed newBreed = newType.getBreed();

    // switch breed stats
    oldBreed.onDisable(dragon);
    newBreed.onEnable(dragon);

    // check for fire immunity and disable fire particles
    dragon.setImmuneToFire(newBreed.isImmuneToDamage(DamageSource.IN_FIRE) || newBreed.isImmuneToDamage(DamageSource.ON_FIRE) || newBreed.isImmuneToDamage(DamageSource.LAVA));

    // update breed name
    entityDataManager.set(dataParam, newType.getName());

    // reset breed points
    if (dragon.isEgg()) {
      breedPoints.values().forEach(points -> points.set(0));
      breedPoints.get(newType).set(POINTS_INITIAL);
    }
  }

  @Override
  public void onLivingUpdate() {
    EnumDragonBreed currentType = getBreedType();

    if (dragon.isEgg()) {
      // spawn breed-specific particles every other tick
      if (dragon.isClient() && dragon.ticksExisted % TICK_RATE_PARTICLES == 0) {
        if (true) { //todo currentType != EnumDragonBreed.END) {
          double px = dragon.posX + (rand.nextDouble() - 0.5);
          double py = dragon.posY + (rand.nextDouble() - 0.5);
          double pz = dragon.posZ + (rand.nextDouble() - 0.5);
          DragonBreed current = currentType.getBreed();
          dragon.world.spawnParticle(EnumParticleTypes.REDSTONE, px, py + 1, pz,
                  current.getColorR(), current.getColorG(), current.getColorB());
        }
      }

      // update egg breed every second on the server
      if (dragon.getBreed().canChangeBreed() && dragon.isServer() && dragon.ticksExisted % TICK_RATE_BLOCK == 0) {
        BlockPos eggPos = dragon.getPosition();

        // scan surrounding for breed-loving blocks
        BlockPos eggPosFrom = eggPos.add(BLOCK_RANGE, BLOCK_RANGE, BLOCK_RANGE);
        BlockPos eggPosTo = eggPos.add(-BLOCK_RANGE, -BLOCK_RANGE, -BLOCK_RANGE);

        BlockPos.getAllInBoxMutable(eggPosFrom, eggPosTo).forEach(blockPos -> {
          Block block = dragon.world.getBlockState(blockPos).getBlock();
          breedPoints.entrySet().stream()
                  .filter(breed -> (breed.getKey().getBreed().isHabitatBlock(block)))
                  .forEach(breed -> breed.getValue().addAndGet(POINTS_BLOCK));
        });

        // check biome
        Biome biome = dragon.world.getBiome(eggPos);

        breedPoints.keySet().forEach(breed -> {
          // check for biomes
          if (breed.getBreed().isHabitatBiome(biome)) {
            breedPoints.get(breed).addAndGet(POINTS_BIOME);
          }

          // extra points for good environments
          if (breed.getBreed().isHabitatEnvironment(dragon)) {
            breedPoints.get(breed).addAndGet(POINTS_ENV);
          }
        });

        // update most dominant breed
        EnumDragonBreed newType = breedPoints.entrySet().stream()
                .max((breed1, breed2) -> Integer.compare(
                        breed1.getValue().get(),
                        breed2.getValue().get()))
                .get().getKey();

        if (newType != currentType) {
          setBreedType(newType);

        }
      }
    }

    currentType.getBreed().onUpdate(dragon);
    getBreedHealth();
  }

  @Override
  public void onDeath() {
    getBreedType().getBreed().onDeath(dragon);
  }

  public void inheritBreed(EntityTameableDragon parent1, EntityTameableDragon parent2) {
    breedPoints.get(parent1.getBreedType()).addAndGet(POINTS_INHERIT + rand.nextInt(POINTS_INHERIT));
    breedPoints.get(parent2.getBreedType()).addAndGet(POINTS_INHERIT + rand.nextInt(POINTS_INHERIT));
  }

  /**
   * Get's the health of the dragon per breed, doubles
   * when it turns into an adult
   */
  public void getBreedHealth() {

    IAttributeInstance health = dragon.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
    double base = DragonMountsConfig.BASE_HEALTH; //90d

//    switch (getBreedType()) {
//      case NETHER:
//        health.setBaseValue(base + 5d);
//        break;
//      case END:
//        health.setBaseValue(base + 10d);
//        break;
//      case SKELETON:
//        health.setBaseValue(base - (base < 16d ? 0d : 15d)); // Cant have 0 health!
//        break;
//      case WITHER:
//        health.setBaseValue(base - (base < 6d ? 0d : 10d)); // Cant have 0 health!
//        break;
//      default: //All Dragons without special health parameters
        health.setBaseValue(base);
//        break;
//    }
  }
  private static final Logger L = LogManager.getLogger();
  private static final int BLOCK_RANGE = 2;
  private static final int POINTS_BLOCK = 1;
  private static final int POINTS_BIOME = 1;
  private static final int POINTS_INITIAL = 1000;
  private static final int POINTS_INHERIT = 1800;
  private static final int POINTS_ENV = 3;
  private static final int TICK_RATE_PARTICLES = 2;
  private static final int TICK_RATE_BLOCK = 20;
  private static final String NBT_BREED = "Breed";
  private static final String NBT_BREED_POINTS = "breedPoints";
  private final DataParameter<String> dataParam;
  private final DataParameter<String> dataParamNew;
  private DragonBreedNew dragonBreedNew;

  private final Map<EnumDragonBreed, AtomicInteger> breedPoints = new EnumMap<>(EnumDragonBreed.class);

}
