package com.TheRPGAdventurer.ROTD.common.entity.breeds;

import com.TheRPGAdventurer.ROTD.client.render.dragon.breeds.DragonBreedRenderer;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.google.common.collect.ImmutableList;
import com.sun.istack.internal.NotNull;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.text.translation.I18n;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by TGG on 9/08/2019.
 * Represents the breed of the dragon.  Currently doesn't do much!
 *
 * Usage:
 * 1) Create a DragonBreedsRegistry or use getDefaultRegistry() for the default registry
 * 2) DragonBreedsRegistry.createDragonBreedNew for each breed; optionally validate the name beforehand using validateName
 * 3) When creating an entity of this breed:
 *     registerDataParameter and setDataParameter to set the entity's breed DataParameter to this breed
 *     writeToNBT to write the NBT tag for this breed
 * 4) DragonBreedRegistry is used to retrieve the DragonBreedNew from serialised information, i.e.
 *      from internalname, from NBT, or from DataParameter
 * The registry always has a default breed in it, registered under DEFAULT_NAME (currently "default")
 *
 */
public class DragonBreedNew {

  public DragonVariants getDragonVariants() {
    return dragonVariants;
  }

  public String getInternalName() {
    return internalName;
  }

  public String getLocalisedName() {
    return I18n.translateToLocal("dragon." + internalName);
  }

  public void registerDataParameter(EntityDataManager entityDataManager, DataParameter<String> dataParameter) {
    entityDataManager.register(dataParameter, internalName);
  }

  public void setDataParameter(EntityDataManager entityDataManager, DataParameter<String> dataParameter) {
    entityDataManager.set(dataParameter, internalName);
  }

  public void writeToNBT(NBTTagCompound nbt) {
    nbt.setString(NBT_BREED_NEW, internalName);
  }

  /**
   * Checks to make sure this is a valid internal name for a breed
   * @param internalName
   * @throws IllegalArgumentException
   */
  public static void validateName(@NotNull String internalName) throws IllegalArgumentException {
    if (internalName.length() < 4) throw new IllegalArgumentException("internal name too short (< 4)");
    if (internalName.length() < 12) throw new IllegalArgumentException("internal name too long (> 12)");
    if (!internalName.matches("[a-zA-Z0-9]+")) throw new IllegalArgumentException("internal name must contain only letters and/or digits");
  }

  DragonBreedNew(String internalName, DragonVariants dragonVariants) throws IllegalArgumentException {
    validateName(internalName);
    this.internalName = internalName;
    this.dragonVariants = dragonVariants;
  }

  /**
   * Maintains a list of all the different DragonBreeds
   */
  public static class DragonBreedsRegistry {

    public static final String DEFAULT_NAME = "default";

    public static DragonBreedsRegistry getDefaultRegistry() {
      return defaultRegistry;
    }

    public DragonBreedsRegistry() {
      createDragonBreedNew(DEFAULT_NAME, new DragonVariants());
    }

    /**
     * Creates a new DragonBreedNew and adds it to the registry.  Don't add duplicates
     * @param internalName
     * @param dragonVariants
     * @return
     * @throws IllegalArgumentException if the name has a problem or the same name is registered twice
     */
    public DragonBreedNew createDragonBreedNew(String internalName, DragonVariants dragonVariants) throws IllegalArgumentException {
      DragonBreedNew dragonBreedNew = new DragonBreedNew(internalName, dragonVariants);
      addBreed(dragonBreedNew);
      return dragonBreedNew;
    }

    /**
     * Add a breed to the registry.  Duplicate breed leads to error!
     * @param dragonBreedNew
     * @throws IllegalArgumentException
     */
    private void addBreed(DragonBreedNew dragonBreedNew) throws IllegalArgumentException {
      if (allDragonBreeds.containsKey(dragonBreedNew.getInternalName())) {
        throw new IllegalArgumentException("breed " + dragonBreedNew.getInternalName() + " is already in the registry");
      }
      allDragonBreeds.put(dragonBreedNew.getInternalName(), dragonBreedNew);
    }

    /** fetches the breed corresponding to the given internal name
     * @param internalName
     * @return
     * @throws IllegalArgumentException if name is not valid
     */
    public DragonBreedNew getBreed(String internalName) throws IllegalArgumentException {
      if (allDragonBreeds.containsKey(internalName)) {
        return allDragonBreeds.get(internalName);
      } else {
        throw new IllegalArgumentException("breed " + internalName + " does not exist");
      }
    }

    /**
     * Returns the default breed (fallback in case of configuration problems)
     * @return
     */
    public DragonBreedNew getDefaultBreed() {
      return getBreed(DEFAULT_NAME);
    }

    /** fetches the breed corresponding to the given internal name
     * @return
     * @throws IllegalArgumentException if name is not valid
     */
    public DragonBreedNew getBreed(EntityDataManager entityDataManager, DataParameter<String> dataParameter) throws IllegalArgumentException {
      String name = entityDataManager.get(dataParameter);
      return getBreed(name);
    }

    /** fetches the breed corresponding to the given internal name
     * @return
     * @throws IllegalArgumentException if the nbt doesn't contain the tag or the tag isn't a valid breed
     */
    public DragonBreedNew getBreed(NBTTagCompound nbt) throws IllegalArgumentException {
      String name = nbt.getString(NBT_BREED_NEW);
      return getBreed(name);
    }

    /** returns a list of all defined breeds
     *
     * @return
     */
    public ImmutableList<DragonBreedNew> getAllBreeds() {
      return ImmutableList.copyOf(allDragonBreeds.values());
    }

    private Map<String, DragonBreedNew> allDragonBreeds = new HashMap<>();
    private static DragonBreedsRegistry defaultRegistry = new DragonBreedsRegistry();
  }

  private DragonVariants dragonVariants;
  private String internalName;

  private static final String NBT_BREED_NEW = "BreedNew";   //todo change
}
