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
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.Modifiers;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Helper class for breed properties.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 * @author TGG
 *
 * Usage:
 *   1) See DragonHelper for correct initialisation
 *   2) changeConfiguration(), addModifier(), removeModifier() to change the breed and/or modifiers.  Server side only.
 *   3) hasModifier() to check if a given modifier is applied
 *   4) getVariantTagValue() to retrieve the value for a given DragonVariantTag
 *
 */
public class DragonConfigurationHelper extends DragonHelper {

  public DragonConfigurationHelper(EntityTameableDragon dragon) {
    super(dragon);

//    if (dragon.isServer()) {
//      // initialize map to avoid future checkings
//      for (EnumDragonBreed type : EnumDragonBreed.values()) {
//        breedPoints.put(type, new AtomicInteger());
//      }
//
//      // default breed has initial points
//      breedPoints.get(EnumDragonBreed.FIRE).set(POINTS_INITIAL);
//    }
    setCompleted(FunctionTag.CONSTRUCTOR);
  }

  @Override
  public void registerDataParameters() {
    checkPreConditions(FunctionTag.REGISTER_DATA_PARAMETERS);
    DragonBreedNew.registerDataParameter(entityDataManager, DATAPARAM_BREED);
    Modifiers.registerDataParameter(entityDataManager, DATAPARAM_MODIFIERS);
    intialisedDataParameters.put(DATAPARAM_MODIFIERS, Boolean.FALSE);
    intialisedDataParameters.put(DATAPARAM_BREED, Boolean.FALSE);
    setCompleted(FunctionTag.REGISTER_DATA_PARAMETERS);
  }

  @Override
  public void initialiseServerSide() {
    checkPreConditions(FunctionTag.INITIALISE_SERVER);
    setCompleted(FunctionTag.INITIALISE_SERVER);
  }

  @Override
  public void initialiseClientSide() {
    checkPreConditions(FunctionTag.INITIALISE_CLIENT);
    DragonBreedNew newBreed = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getDefaultBreed();
    Modifiers newModifiers = new Modifiers();
    try {
      newBreed = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getBreed(entityDataManager, DATAPARAM_BREED);
    } catch (IllegalArgumentException iae) {
      DragonMounts.loggerLimit.warn_once(iae.getMessage());
    }
    try {
      newModifiers = Modifiers.getStateFromDataParam(entityDataManager, DATAPARAM_MODIFIERS);
    } catch (IllegalArgumentException iae) {
      DragonMounts.loggerLimit.warn_once(iae.getMessage());
    }
    dragonBreedNew = newBreed;
    modifiers = newModifiers;

    setCompleted(FunctionTag.INITIALISE_CLIENT);
  }

  @Override
  public void notifyDataManagerChange(DataParameter<?> key) {
    if (!intialisedDataParameters.containsKey(key)) return;
    // if initialised, change configuration.  Ignore errors
    // if not initialised, mark as received (once all are received, initialisation will be performed via initialiseClientSide)
    if (helperState == HelperState.INITIALISED) {
      DragonBreedNew newBreed;
      Modifiers newModifiers;
      try {
        newBreed = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getBreed(entityDataManager, DATAPARAM_BREED);
        newModifiers = Modifiers.getStateFromDataParam(entityDataManager, DATAPARAM_MODIFIERS);
      } catch (IllegalArgumentException iae) {
        DragonMounts.loggerLimit.warn_once(iae.getMessage());
        return;
      }
      changeConfiguration(newBreed, newModifiers);
    } else {
      checkPreConditions(FunctionTag.DATAPARAMETER_RECEIVED);
      receivedDataParameter(key);
      setCompleted(FunctionTag.DATAPARAMETER_RECEIVED);
    }
  }

  public static void registerConfigurationTags() { //todo initialise tags here
  }

  public void setInitialConfiguration(DragonBreedNew newBreed, Modifiers newModifiers) {
    checkPreConditions(FunctionTag.SET_INITIAL_CONFIGURATION);
    dragonBreedNew = newBreed;
    modifiers = newModifiers;
    setCompleted(FunctionTag.SET_INITIAL_CONFIGURATION);
  }

  public void changeConfiguration(DragonBreedNew newBreed, Modifiers newModifiers) {
    checkPreConditions(FunctionTag.CHANGE_CONFIGURATION);
    if (dragonBreedNew == newBreed && newModifiers.equals(modifiers)) return;
    dragonBreedNew = newBreed;
    modifiers = newModifiers;
    notifyOfConfigurationChange();
  }

  public void changeConfiguration(DragonBreedNew newBreed) {
    checkPreConditions(FunctionTag.CHANGE_CONFIGURATION);
    if (dragonBreedNew == newBreed) return;
    dragonBreedNew = newBreed;
    notifyOfConfigurationChange();
  }

  public void changeConfiguration(Modifiers newModifiers) {
    checkPreConditions(FunctionTag.CHANGE_CONFIGURATION);
    if (newModifiers.equals(modifiers)) return;
    modifiers = newModifiers;
    notifyOfConfigurationChange();
  }

  public void addModifier(DragonVariants.Modifier modifier) {
    checkPreConditions(FunctionTag.CHANGE_CONFIGURATION);
    Modifiers prevModifiers = modifiers.createCopy();
    modifiers.add(modifier);
    if (!prevModifiers.equals(modifiers)) {
      notifyOfConfigurationChange();
    }
  }

  public void removeModifier(DragonVariants.Modifier modifier) {
    checkPreConditions(FunctionTag.CHANGE_CONFIGURATION);
    Modifiers prevModifiers = modifiers.createCopy();
    modifiers.remove(modifier);
    if (!prevModifiers.equals(modifiers)) {
      notifyOfConfigurationChange();
    }
  }

  public boolean hasModifier(DragonVariants.Modifier modifier) {
    checkPreConditions(FunctionTag.VANILLA);
    return modifiers.hasModifier(modifier);
  }

  public Modifiers getModifiers() {
    checkPreConditions(FunctionTag.VANILLA);
    return modifiers.createCopy();
  }

  public Object getVariantTagValue(DragonVariants.Category category, DragonVariantTag tag) {
    checkPreConditions(FunctionTag.VANILLA);
    DragonVariants.ModifiedCategory modifiedCategory = new DragonVariants.ModifiedCategory(category, modifiers);
    return dragonBreedNew.getDragonVariants().getValueOrDefault(modifiedCategory, tag);
  }

  private void notifyOfConfigurationChange() {
    dragon.onConfigurationChange();
  }

  @Override
  public void writeToNBT(NBTTagCompound nbt) {
    checkPreConditions(FunctionTag.WRITE_TO_NBT);
    dragonBreedNew.writeToNBT(nbt);
    modifiers.writeToNBT(nbt);

//    NBTTagCompound breedPointTag = new NBTTagCompound();
//    breedPoints.forEach((type, points) -> {
//      breedPointTag.setInteger(type.getName(), points.get());
//    });
//    nbt.setTag(NBT_BREED_POINTS, breedPointTag);
    setCompleted(FunctionTag.WRITE_TO_NBT);
  }

  @Override
  public void readFromNBT(NBTTagCompound nbt) {
    checkPreConditions(FunctionTag.READ_FROM_NBT);
    DragonBreedNew newBreed = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getDefaultBreed();
    Modifiers newModifiers = new Modifiers();
    try {
      newBreed = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getBreed(nbt);
    } catch (IllegalArgumentException iae) {
      DragonMounts.loggerLimit.warn_once(iae.getMessage());
    }
    try {
      newModifiers = Modifiers.getStateFromNBT(nbt);
    } catch (IllegalArgumentException iae) {
      DragonMounts.loggerLimit.warn_once(iae.getMessage());
    }
    dragonBreedNew = newBreed;
    modifiers = newModifiers;

//    // read breed name and convert it to the corresponding breed object
//    String breedName = nbt.getString(NBT_BREED);
//    EnumDragonBreed breed = EnumUtils.getEnum(EnumDragonBreed.class, breedName.toUpperCase());
//    if (breed == null) {
//      breed = EnumDragonBreed.FIRE;
//      L.warn("Dragon {} loaded with invalid breed type {}, using {} instead",
//              dragon.getEntityId(), breedName, breed);
//    }
//    setBreedType(breed);
//
//    try {
//      dragonBreedNew = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getBreed(nbt);
//    } catch (IllegalArgumentException iae) {
//      DragonMounts.loggerLimit.warn_once(iae.getMessage());
//      dragonBreedNew = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getDefaultBreed();
//    }
//
//    // read breed points
//    NBTTagCompound breedPointTag = nbt.getCompoundTag(NBT_BREED_POINTS);
//    breedPoints.forEach((type, points) -> {
//      points.set(breedPointTag.getInteger(type.getName()));
//    });
    setCompleted(FunctionTag.READ_FROM_NBT);
  }

//  public Map<EnumDragonBreed, AtomicInteger> getBreedPoints() {
//    return Collections.unmodifiableMap(breedPoints);
//  }

//  public EnumDragonBreed getBreedType() {
//    String breedName = entityDataManager.get(dataParam);
//    return EnumUtils.getEnum(EnumDragonBreed.class, breedName.toUpperCase());
//  }

  public DragonBreedNew getDragonBreedNew() {return dragonBreedNew;}
//
//  public void setBreedType(EnumDragonBreed newType) {
//    L.trace("setBreed({})", newType);
//
//    // ignore breed changes on client side, it's controlled by the server
//    if (dragon.isClient()) {
//      return;
//    }
//
//    Objects.requireNonNull(newType);
//
//    // check if the breed actually changed
//    EnumDragonBreed oldType = getBreedType();
//    if (oldType == newType) {
//      return;
//    }
//
//    DragonBreed oldBreed = oldType.getBreed();
//    DragonBreed newBreed = newType.getBreed();
//
//    // switch breed stats
//    oldBreed.onDisable(dragon);
//    newBreed.onEnable(dragon);
//
//    // check for fire immunity and disable fire particles
//    dragon.setImmuneToFire(newBreed.isImmuneToDamage(DamageSource.IN_FIRE) || newBreed.isImmuneToDamage(DamageSource.ON_FIRE) || newBreed.isImmuneToDamage(DamageSource.LAVA));
//
//    // update breed name
//    entityDataManager.set(dataParam, newType.getName());
//
//    // reset breed points
//    if (dragon.isEgg()) {
//      breedPoints.values().forEach(points -> points.set(0));
//      breedPoints.get(newType).set(POINTS_INITIAL);
//    }
//  }

  @Override
  public void onLivingUpdate() {
    checkPreConditions(FunctionTag.VANILLA);
//    EnumDragonBreed currentType = getBreedType();
//
//    // update egg breed every second on the server
//    if (dragon.getBreed().canChangeBreed() && dragon.isServer() && dragon.ticksExisted % TICK_RATE_BLOCK == 0) {
//      BlockPos eggPos = dragon.getPosition();
//
//      // scan surrounding for breed-loving blocks
//      BlockPos eggPosFrom = eggPos.add(BLOCK_RANGE, BLOCK_RANGE, BLOCK_RANGE);
//      BlockPos eggPosTo = eggPos.add(-BLOCK_RANGE, -BLOCK_RANGE, -BLOCK_RANGE);
//
//      BlockPos.getAllInBoxMutable(eggPosFrom, eggPosTo).forEach(blockPos -> {
//        Block block = dragon.world.getBlockState(blockPos).getBlock();
//        breedPoints.entrySet().stream()
//                .filter(breed -> (breed.getKey().getBreed().isHabitatBlock(block)))
//                .forEach(breed -> breed.getValue().addAndGet(POINTS_BLOCK));
//      });
//
//      // check biome
//      Biome biome = dragon.world.getBiome(eggPos);
//
//      breedPoints.keySet().forEach(breed -> {
//        // check for biomes
//        if (breed.getBreed().isHabitatBiome(biome)) {
//          breedPoints.get(breed).addAndGet(POINTS_BIOME);
//        }
//
//        // extra points for good environments
//        if (breed.getBreed().isHabitatEnvironment(dragon)) {
//          breedPoints.get(breed).addAndGet(POINTS_ENV);
//        }
//      });
//
//      // update most dominant breed
//      EnumDragonBreed newType = breedPoints.entrySet().stream()
//              .max((breed1, breed2) -> Integer.compare(
//                      breed1.getValue().get(),
//                      breed2.getValue().get()))
//              .get().getKey();
//
//      if (newType != currentType) {
//        setBreedType(newType);
//
//      }
//    }
  }

  @Override
  public void onDeath() {
//    getBreedType().getBreed().onDeath(dragon);
    checkPreConditions(FunctionTag.VANILLA);
  }

//  public void inheritBreed(EntityTameableDragon parent1, EntityTameableDragon parent2) {
//    breedPoints.get(parent1.getBreedType()).addAndGet(POINTS_INHERIT + rand.nextInt(POINTS_INHERIT));
//    breedPoints.get(parent2.getBreedType()).addAndGet(POINTS_INHERIT + rand.nextInt(POINTS_INHERIT));
//  }

//  /**
//   * Get's the health of the dragon per breed, doubles
//   * when it turns into an adult
//   */
//  public void getBreedHealth() {
//
//    IAttributeInstance health = dragon.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
//    double base = DragonMounts.instance.getConfig().BASE_HEALTH; //90d
//
////    switch (getBreedType()) {
////      case NETHER:
////        health.setBaseValue(base + 5d);
////        break;
////      case END:
////        health.setBaseValue(base + 10d);
////        break;
////      case SKELETON:
////        health.setBaseValue(base - (base < 16d ? 0d : 15d)); // Cant have 0 health!
////        break;
////      case WITHER:
////        health.setBaseValue(base - (base < 6d ? 0d : 10d)); // Cant have 0 health!
////        break;
////      default: //All Dragons without special health parameters
//        health.setBaseValue(base);
////        break;
////    }
//  }
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
  private DragonBreedNew dragonBreedNew;
  private Modifiers modifiers;

//  private final Map<EnumDragonBreed, AtomicInteger> breedPoints = new EnumMap<>(EnumDragonBreed.class);

  private static final DataParameter<String> DATAPARAM_BREED = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.STRING);
  private static final DataParameter<Modifiers> DATAPARAM_MODIFIERS = EntityDataManager.createKey(EntityTameableDragon.class, Modifiers.MODIFIERS_DATA_SERIALIZER);
}
