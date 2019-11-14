/*
 ** 2013 October 28
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.common.entity.helper;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.EntityDragonEgg;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonFactory;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantsException;
import com.google.common.base.Optional;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

/**
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 *
 * Controls reproduction and gender
 * * male (default) or female - via modifiers
 * * fertile or infertile - based on reproduction limit - via modifiers
 *
 */
public class DragonReproductionHelper extends DragonHelper {

  public DragonReproductionHelper(EntityTameableDragon dragon) {
    super(dragon);
    setCompleted(FunctionTag.CONSTRUCTOR);
  }

  /**
   * Initialise all the configuration tags used by this helper
   */
  public static void registerConfigurationTags()
  {
    // the initialisation of the tags is all done in their static initialisers
    DragonVariants.addVariantTagValidator(new DragonReproductionValidator());
  }

  @Override
  public void registerDataParameters() {
    checkPreConditions(FunctionTag.REGISTER_DATA_PARAMETERS);
    registerForInitialisation(DATAPARAM_BREEDER, Optional.absent());
    registerForInitialisation(DATAPARAM_REPRO_COUNT, 0);
    setCompleted(FunctionTag.REGISTER_DATA_PARAMETERS);
  }

  @Override
  public void writeToNBT(NBTTagCompound nbt) {
    checkPreConditions(FunctionTag.WRITE_TO_NBT);
    Optional<UUID> breederID = getBreederID();
    if (breederID.isPresent()) {
      nbt.setUniqueId(NBT_BREEDER, breederID.get());
    }
    nbt.setInteger(NBT_REPRO_COUNT, getReproCount());
    nbt.setInteger(NBT_REPRO_LIMIT, reproductionLimit);
    setCompleted(FunctionTag.WRITE_TO_NBT);
  }

  @Override
  public void readFromNBT(NBTTagCompound nbt) {
    checkPreConditions(FunctionTag.READ_FROM_NBT);
    int reproCount = nbt.getInteger(NBT_REPRO_COUNT);
    UUID uuid = nbt.getUniqueId(NBT_BREEDER);
    reproductionLimit = nbt.getInteger(NBT_REPRO_LIMIT);
    setReproCount(reproCount);
    setBreederID(uuid);
    setCompleted(FunctionTag.READ_FROM_NBT);
  }

  @Override
  public void initialiseServerSide() {
    checkPreConditions(FunctionTag.INITIALISE_SERVER);
    verifyModifiers(false);
    setCompleted(FunctionTag.INITIALISE_SERVER);
  }

  /**
   * Called once when the entity is first spawned (and not when it's later loaded from disk)
   * Is called after initialiseServerSide
   * @param difficulty
   * @param livingdata
   * @return
   */
  @Nullable
  public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata) {
    // set the reproduction limit for this dragon
    int lowerlimit = (int)dragon.configuration().getVariantTagValue(DragonVariants.Category.REPRODUCTION, REPRODUCTION_LOWER_LIMIT);
    int upperlimit = (int)dragon.configuration().getVariantTagValue(DragonVariants.Category.REPRODUCTION, REPRODUCTION_UPPER_LIMIT);
    reproductionLimit = rand.nextInt(upperlimit - lowerlimit + 1) + lowerlimit;
    updateFertilityModifiers();
    verifyModifiers(true);

    return livingdata;
  }

  @Override
  public void initialiseClientSide() {
    checkPreConditions(FunctionTag.INITIALISE_CLIENT);
    setCompleted(FunctionTag.INITIALISE_CLIENT);
  }

  @Override
  public void onConfigurationChange() {
    verifyModifiers(true);
  }

  private void verifyModifiers(boolean raiseError) {
    boolean hasFertileModifier = dragon.configuration().hasModifier(DragonVariants.Modifier.FERTILE);
    boolean hasInfertileModifier = dragon.configuration().hasModifier(DragonVariants.Modifier.INFERTILE);
    boolean hasMaleModifier = dragon.configuration().hasModifier(DragonVariants.Modifier.MALE);
    boolean hasFemaleModifier = dragon.configuration().hasModifier(DragonVariants.Modifier.FEMALE);
    if (!hasFemaleModifier && !hasMaleModifier) {
      dragon.configuration().addModifier(DragonVariants.Modifier.MALE);
      if (raiseError) {
        DragonMounts.loggerLimit.warn_once("Dragon unexpectedly had neither FEMALE nor MALE modifier.");
      }
    }

    if (!hasFertileModifier && !hasInfertileModifier) {
      updateFertilityModifiers();
      if (raiseError) {
        DragonMounts.loggerLimit.warn_once("Dragon unexpectedly had neither FERTILE nor INFERTILE modifier.");
      }
    }
  }

  public int getReproCount() {
    return entityDataManager.get(DATAPARAM_REPRO_COUNT);
  }

  public void setReproCount(int reproCount) {
    entityDataManager.set(DATAPARAM_REPRO_COUNT, reproCount);
  }

  public boolean isFertile() {
    return getReproCount() < reproductionLimit;
  }

  public void addReproduced() {
    setReproCount(getReproCount() + 1);
  }

  public boolean canReproduce() {
    return dragon.isTamed() && isFertile() && hasReachedReproductiveAge();
  }

  public boolean hasReachedReproductiveAge() {
    double emotionalMaturityThreshold = (double)dragon.configuration().getVariantTagValue(DragonVariants.Category.REPRODUCTION, REPRODUCTION_EMOTIONAL_MATURITY_THRESHOLD);
    return dragon.lifeStage().getEmotionalMaturity() >= emotionalMaturityThreshold;
  }

  public void updateFertilityModifiers() {
    boolean hasFertileModifier = dragon.configuration().hasModifier(DragonVariants.Modifier.FERTILE);
    boolean hasInfertileModifier = dragon.configuration().hasModifier(DragonVariants.Modifier.INFERTILE);
    if (isFertile()) {
      if (hasFertileModifier) return;
      dragon.configuration().addModifier(DragonVariants.Modifier.FERTILE);
    } else {
      if (hasInfertileModifier) return;
      dragon.configuration().addModifier(DragonVariants.Modifier.INFERTILE);
    }
  }

  public Optional<UUID> getBreederID() {
    return entityDataManager.get(DATAPARAM_BREEDER);
  }

  public void setBreederID(UUID breederID) {
    entityDataManager.set(DATAPARAM_BREEDER, Optional.fromNullable(breederID));
  }

  public EntityPlayer getBreeder() {
    Optional<UUID> breederID = getBreederID();
    if (breederID.isPresent()) {
      return dragon.world.getPlayerEntityByUUID(breederID.get());
    } else {
      return null;
    }
  }

  public void setBreeder(EntityPlayer breeder) {
    setBreederID(breeder != null ? breeder.getUniqueID() : null);
  }

  /**
   * Checks if the parameter is an item which this animal can be fed to breed it
   * (eg wheat, carrots or seeds etc depending on the animal type)
   */
  public boolean isBreedingItem(ItemStack item) {
    if (item.isEmpty()) return false;
    String itemName = item.getUnlocalizedName();
    boolean found = Arrays.asList(dragon.configuration().getVariantTagValue(DragonVariants.Category.REPRODUCTION, BREEDING_ITEMS)).contains(itemName);
    return found;
  }


  /**
   * is the dragon male?
   */
  public boolean isMale() {
    verifyModifiers(true);
    return dragon.configuration().hasModifier(DragonVariants.Modifier.MALE);
  }

  public void setMale(boolean male) {
    dragon.configuration().addModifier(male ? DragonVariants.Modifier.MALE : DragonVariants.Modifier.FEMALE);
  }

  /**
   * set in commands
   */
  public void setToOppositeGender() {
    this.setMale(!this.isMale());
  }

  // checks if both dragons can reproduce, are interbreeding compatible, are opposite gender, are are both in love
  public boolean canMateWith(EntityAnimal mate) {
    if (mate == dragon) {
      // No. Just... no.
      return false;
    } else if (!(mate instanceof EntityTameableDragon)) {
      return false;
    } else if (!canReproduce()) {
      return false;
    }

    EntityTameableDragon dragonMate = (EntityTameableDragon) mate;
    if (!dragonMate.reproduction().canReproduce()) return false;
    if (this.isMale() == dragonMate.reproduction().isMale()) return false;

    if (!canInterbreed(dragonMate)) return false;
    return dragon.isInLove() && dragonMate.isInLove();
  }

  // can these two dragon breeds interbreed with each other?
  private boolean canInterbreed(EntityTameableDragon mate) {
    String compatibility1 = (String)dragon.configuration().getVariantTagValue(DragonVariants.Category.REPRODUCTION, INTERBREEDING_COMPATABILITY);
    String compatibility2 = (String)mate.configuration().getVariantTagValue(DragonVariants.Category.REPRODUCTION, INTERBREEDING_COMPATABILITY);
    for (char c : compatibility1.toCharArray()) {
      if (compatibility2.indexOf(c) >= 0) return true;
    }
    return false;
  }

  /**
   * Vanilla createChild is not supported; custom AI is used instead
   */

  public EntityAgeable createChild(EntityAgeable mate) {
    throw new UnsupportedOperationException("vanilla createChild not supported");
//    if (!(mate instanceof EntityTameableDragon)) {
//      DragonMounts.loggerLimit.warn_once("Called createChild when mate wasn't a dragon");
//      return null;
//    }
//    EntityTameableDragon dragonMate = (EntityTameableDragon)mate;
//    if (!canMateWith(dragonMate)) return null;
//    return null;
  }

  public EntityDragonEgg createChild(EntityTameableDragon mate) {
    if (!canMateWith(mate)) {
      throw new IllegalArgumentException("Attempted to createChild but canMateWith() is false");
    }

    EntityTameableDragon parent1 = dragon;
    EntityTameableDragon parent2 = mate;
    DragonBreedNew dragonBreed = rand.nextBoolean() ? parent1.configuration().getDragonBreedNew()
                                                    : parent2.configuration().getDragonBreedNew();
    EntityDragonEgg baby = DragonFactory.getDefaultDragonFactory().createEgg(dragon.world, dragonBreed);

    // mix the custom names in case both parents have one
    if (parent1.hasCustomName() && parent2.hasCustomName()) {
      String p1Name = parent1.getCustomNameTag();
      String p2Name = parent2.getCustomNameTag();
      String babyName;

      if (p1Name.contains(" ") || p2Name.contains(" ")) {
        // combine two words with space
        // "Tempor Invidunt Dolore" + "Magna"
        // = "Tempor Magna" or "Magna Tempor"
        String[] p1Names = p1Name.split(" ");
        String[] p2Names = p2Name.split(" ");

        p1Name = fixChildName(p1Names[rand.nextInt(p1Names.length)]);
        p2Name = fixChildName(p2Names[rand.nextInt(p2Names.length)]);

        babyName = rand.nextBoolean() ? p1Name + " " + p2Name : p2Name + " " + p1Name;
      } else {
        // scramble two words
        // "Eirmod" + "Voluptua"
        // = "Eirvolu" or "Volueir" or "Modptua" or "Ptuamod" or ...
        if (rand.nextBoolean()) {
          p1Name = p1Name.substring(0, (p1Name.length() - 1) / 2);
        } else {
          p1Name = p1Name.substring((p1Name.length() - 1) / 2);
        }

        if (rand.nextBoolean()) {
          p2Name = p2Name.substring(0, (p2Name.length() - 1) / 2);
        } else {
          p2Name = p2Name.substring((p2Name.length() - 1) / 2);
        }

        p2Name = fixChildName(p2Name);

        babyName = rand.nextBoolean() ? p1Name + p2Name : p2Name + p1Name;
      }

      baby.setCustomNameTag(babyName);
    }

//    // inherit the baby's breed from its parents
//    baby.configuration().inheritBreed(parent1, parent2);

    // increase reproduction counter
    parent1.reproduction().addReproduced();
    parent2.reproduction().addReproduced();

    return baby;
  }

  private String fixChildName(String nameOld) {
    if (nameOld == null || nameOld.isEmpty()) {
      return nameOld;
    }

    // create all lower-case char array
    char[] chars = nameOld.toLowerCase().toCharArray();

    // convert first char to upper-case
    chars[0] = Character.toUpperCase(chars[0]);

    String nameNew = new String(chars);

    if (!nameOld.equals(nameNew)) {
      L.debug("Fixed child name {} -> {}");
    }

    return nameNew;
  }

  /**
   * Validates the following aspects of the tags:
   * 1) reproduction upper limit >= lower limit
   * If any errors are found, revert to the defaults and throw an error
   */
  public static class DragonReproductionValidator implements DragonVariants.VariantTagValidator {
    @Override
    public void validateVariantTags(DragonVariants dragonVariants, DragonVariants.ModifiedCategory modifiedCategory) throws IllegalArgumentException {
      DragonVariantsException.DragonVariantsErrors dragonVariantsErrors = new DragonVariantsException.DragonVariantsErrors();
      if (!modifiedCategory.getCategory().equals(DragonVariants.Category.REPRODUCTION)) return;

      if ((double)dragonVariants.getValueOrDefault(modifiedCategory, REPRODUCTION_LOWER_LIMIT)
          >= (double)dragonVariants.getValueOrDefault(modifiedCategory, REPRODUCTION_UPPER_LIMIT)) {
        dragonVariants.removeTags(DragonVariants.Category.REPRODUCTION, REPRODUCTION_LOWER_LIMIT, REPRODUCTION_UPPER_LIMIT);
        dragonVariantsErrors.addError("\"" + REPRODUCTION_LOWER_LIMIT.getTextname() + "\" must be <= \"" + REPRODUCTION_UPPER_LIMIT.getTextname() + "\"");
      }

      String [] breedingItemNames = (String [])dragonVariants.getValueOrDefault(modifiedCategory, BREEDING_ITEMS);
      ArrayList<String> badItemNames = new ArrayList<>();
      for (String name : breedingItemNames) {
        Item item = Item.REGISTRY.getObject(new ResourceLocation(name));
        if (item == null) {
          badItemNames.add(name);
        }
      }
      if (badItemNames.size() > 0) {
        StringBuilder sb = new StringBuilder();
        sb.append("one or more items were not found:");
        boolean first = true;
        for (String badItemName : badItemNames) {
          if (!first) sb.append(",");
          sb.append("\"");
          sb.append(badItemName);
          sb.append("\"");
          first = false;
        }
        dragonVariants.removeTag(DragonVariants.Category.REPRODUCTION, BREEDING_ITEMS);
        dragonVariantsErrors.addError(sb.toString());
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

  private int reproductionLimit = 0; // max number of times this dragon can reproduce

  public static final String NBT_BREEDER = "HatchedByUUID";
  public static final String NBT_REPRO_COUNT = "ReproductionCount";
  public static final String NBT_REPRO_LIMIT = "ReproductionLimit";

  private static final DataParameter<Optional<UUID>> DATAPARAM_BREEDER = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.OPTIONAL_UNIQUE_ID);
  private static final DataParameter<Integer> DATAPARAM_REPRO_COUNT = EntityDataManager.createKey(EntityTameableDragon.class, DataSerializers.VARINT);

  private static final Logger L = LogManager.getLogger();

  private static final DragonVariantTag REPRODUCTION_LOWER_LIMIT = DragonVariantTag.addTag("reproductionlowerlimit", 1, 0, 100,
          "dragon can reproduce at least this many times (lower limit for random number generation)").categories(DragonVariants.Category.REPRODUCTION);
  private static final DragonVariantTag REPRODUCTION_UPPER_LIMIT = DragonVariantTag.addTag("reproductionupperlimit", 5, 0, 100,
          "dragon can reproduce at most this many times (upper limit for random number generation)").categories(DragonVariants.Category.REPRODUCTION);
  private static final DragonVariantTag REPRODUCTION_EMOTIONAL_MATURITY_THRESHOLD = DragonVariantTag.addTag("emotionalmaturitythreshold", 75, 0, 100,
          "dragon cannot mate until its emotional maturity (%) is equal to this value or higher").categories(DragonVariants.Category.REPRODUCTION);
  private static final String [] defaultBreedingCodes = {"all"};
  private static final DragonVariantTag INTERBREEDING_COMPATABILITY = DragonVariantTag.addTag("interbreedingcodes", defaultBreedingCodes,
          "different species of dragon can interbreed if they have one or more codes in common." +
          "eg if air has [\"one\",\"two\"], water has [\"three\"], and cloud has [\"one\",\"three\"], " +
          "then cloud can breed with air (both have \"one\"), cloud can breed with water (both have \"three\")," +
          "but air can't breed with water").categories(DragonVariants.Category.REPRODUCTION);
  private static final String [] defaultBreedingItems = {"item.fish"};
  private static final DragonVariantTag BREEDING_ITEMS = DragonVariantTag.addTag("breedingitems", defaultBreedingItems,
          "list of items which can be used on the dragon to make it breed").categories(DragonVariants.Category.REPRODUCTION);
}