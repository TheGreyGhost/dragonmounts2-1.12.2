package com.TheRPGAdventurer.ROTD.common.entity.physicalmodel;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.model.EggModels;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializer;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Created by TGG on 3/10/2019.
 * Keeps track of all modifiers applied to this dragon (eg male, female, etc)
 * Typical usage:
 * 0) During proxy.preInitialise, call preInitialise()
 * 1) call registerDataParameter() during the registerDataParameters() of the Entity which uses the Modifiers
 * 2) Create an empty Modifiers() or populate from an array of Modifier

 * 3) getStateFromDataParam() and setDataParameter() to synchronise between client and server
 * 4) getStateFromNBT() and writeToNBT() to load & save the entity
 *
 * 5) add() or remove() Modifier
 * 6) getModifierList() to get an explicit list of the discrete Modifiers
 *
 */
public class Modifiers implements Cloneable {

  public Modifiers() {
  }

  public Modifiers(DragonVariants.Modifier... modifiers) {
    if (DragonVariants.Modifier.validateMutexes(modifiers)) {
      for (DragonVariants.Modifier modifier : modifiers) {
        appliedModifiers.set(modifier.getBitIndex());
      }
    }
  }

  public void add(DragonVariants.Modifier modifier) {
    List<DragonVariants.Modifier> flagsToClear = modifier.getSameMutex();
    for (DragonVariants.Modifier modifierToClear : flagsToClear) {
      appliedModifiers.clear(modifierToClear.getBitIndex());
    }
    appliedModifiers.set(modifier.getBitIndex());
  }

  public void remove(DragonVariants.Modifier modifier) {
    appliedModifiers.clear(modifier.getBitIndex());
  }

  public Modifiers createCopy() {
    Modifiers retval = new Modifiers();
    retval.appliedModifiers = (BitSet)this.appliedModifiers.clone();
    return retval;
  }

  // gets the list of applied modifiers as discrete modifiers
  public List<DragonVariants.Modifier> getModifierList() {
    List<DragonVariants.Modifier> retval = new ArrayList<>();
    for (DragonVariants.Modifier modifier : DragonVariants.Modifier.getAllModifiers()) {
      if (appliedModifiers.get(modifier.getBitIndex())) {
        retval.add(modifier);
      }
    }
    return retval;
  }

  // gets the list of applied modifiers as discrete modifiers - for debugging purposes
  public List<DragonVariants.Modifier> getModifierListIncludingDebugs() {
    List<DragonVariants.Modifier> retval = new ArrayList<>();
    for (DragonVariants.Modifier modifier : DragonVariants.Modifier.values()) {
      if (appliedModifiers.get(modifier.getBitIndex())) {
        retval.add(modifier);
      }
    }
    return retval;
  }

  // returns true if Modifiers contains the given modifier
  public boolean hasModifier(DragonVariants.Modifier modifier) {
    boolean retval = appliedModifiers.get(modifier.getBitIndex());
    return retval;
  }

  public static void preInitialise() {
    DataSerializers.registerSerializer(MODIFIERS_DATA_SERIALIZER);
  }

  public static Modifiers getStateFromDataParam(EntityDataManager entityDataManager, DataParameter<Modifiers> dataParameter) throws IllegalArgumentException {
    try {
      Modifiers newModifiers = entityDataManager.get(dataParameter);
      return newModifiers;
    } catch (Exception e) {
      throw new IllegalArgumentException("Error getting Modifiers state from DataParameter:" + e.getMessage());
    }
  }

  public static Modifiers getStateFromNBT(NBTTagCompound nbtTagCompound) throws IllegalArgumentException {
    try {
      byte [] bitSet = nbtTagCompound.getByteArray(NBT_MODIFIERS);
      Modifiers newModifiers = new Modifiers();
      newModifiers.appliedModifiers = BitSet.valueOf(bitSet);
      newModifiers.checkValidity();
      return newModifiers;
    } catch (Exception e) {
      throw new IllegalArgumentException("Error reading NBT tag for Modifiers:" + e.getMessage());
//      DragonMounts.loggerLimit.warn_once("Error reading NBT tag for Modifiers:" + e.getMessage());
//      return new Modifiers();
    }
  }

  public static void registerDataParameter(EntityDataManager entityDataManager, DataParameter<Modifiers> dataParameter) {
    entityDataManager.register(dataParameter, new Modifiers()); // just a default
  }

  public void setDataParameter(EntityDataManager entityDataManager, DataParameter<Modifiers> dataParameter) {
    entityDataManager.set(dataParameter, this);
  }

  public void writeToNBT(NBTTagCompound nbt) {
    nbt.setByteArray(NBT_MODIFIERS, this.appliedModifiers.toByteArray());
  }

  // checks if the combination of modifiers is valid
  private void checkValidity() throws IllegalArgumentException {
    if (appliedModifiers.size() > DragonVariants.Modifier.MAX_BIT_INDEX) {
      throw new IllegalArgumentException("Modifier bit index " + appliedModifiers.size() +" greater than maximum:" + DragonVariants.Modifier.MAX_BIT_INDEX);
    }
    BitSet copy = (BitSet)appliedModifiers.clone();
    List<DragonVariants.Modifier> modifiersMatched = new ArrayList<>();
    for (DragonVariants.Modifier modifier : DragonVariants.Modifier.getAllModifiers()) {
      if (copy.get(modifier.getBitIndex())) {
        copy.clear(modifier.getBitIndex());
        modifiersMatched.add(modifier);
      }
    }
    if (!copy.isEmpty()) {
      throw new IllegalArgumentException("Invalid modifier bit index " + copy.nextSetBit(0));
    }
    DragonVariants.Modifier[] modifiersMatchedArray = new DragonVariants.Modifier[0];
    if (!DragonVariants.Modifier.validateMutexes(modifiersMatched.toArray(modifiersMatchedArray))) {
      throw new IllegalArgumentException("Two or more modifiers are mutually exclusive.");
    }
  }

  private static final String NBT_MODIFIERS = "Modifiers";

  public static final DataSerializer<Modifiers> MODIFIERS_DATA_SERIALIZER = new DataSerializer<Modifiers>() {
    public void write(PacketBuffer buf, Modifiers value) {
      byte [] bitSet = value.appliedModifiers.toByteArray();
      buf.writeByteArray(bitSet);
    }

    public Modifiers read(PacketBuffer buf) throws IOException {
      try {
        byte [] bitSet = buf.readByteArray(DragonVariants.Modifier.MAX_BIT_INDEX / 8 + 1);
        Modifiers newModifiers = new Modifiers();
        newModifiers.appliedModifiers = BitSet.valueOf(bitSet);
        newModifiers.checkValidity();
        return newModifiers;
      } catch (Exception e) {
        DragonMounts.loggerLimit.warn_once(e.getMessage());
        return new Modifiers();
      }
    }

    public DataParameter<Modifiers> createKey(int id) {
      return new DataParameter<Modifiers>(id, this);
    }

    public Modifiers copyValue(Modifiers value) {
      return value.createCopy();
    }
  };

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || !(o instanceof Modifiers)) return false;
    return this.appliedModifiers.equals(((Modifiers) o).appliedModifiers);
  }

  @Override
  public int hashCode() {
    return appliedModifiers.hashCode();
  }

  private BitSet appliedModifiers = new BitSet(DragonVariants.Modifier.MAX_BIT_INDEX + 1);

}