package com.TheRPGAdventurer.ROTD.common.entity.physicalmodel;

import java.util.HashMap;
import java.util.Optional;

/**
 * Created by TGG on 14/07/2019.
 * Each of these tags represents a variation that can be applied to a dragon
 * For example -
 * NUMBER_OF_NECK_SEGMENTS = 4
 * TWIN_RIDGE_PLATES
 * <p>
 * The variants may have an associated value, eg
 * NUMBER_OF_NECK_SEGMENTS = 4
 * PRIMARY_BREATH_WEAPON = fire
 * or they may have just a tag itself (no value- just a flag), in which case 'true' is the associated value eg
 * TWIN_RIDGE_PLATES
 */
public enum DragonVariantTag {

  NUMBER_OF_NECK_SEGMENTS("numberofnecksegments", 7, 4, 12),
  NUMBER_OF_WING_FINGERS("numberofwingfingers", 4, 2, 6),
  NUMBER_OF_TAIL_SEGMENTS("numberoftailsegments", 12, 0, 20),
  MAX_NUMBER_OF_PASSENGERS("maxpassengers", 3, 0, 3),
  TWIN_RIDGE_PLATES("twinridgeplates"),
  TAIL_SPIKE("tailspike"),

  PARTICLE_FACTORY("particlefactory", "fire"),
  PARTICLE_RESOURCE_LOCATION("particleresourcelocation", ""),
  NODE_SPEED("nodespeed", 1.2, 0.0, 5.0),
  NODE_DIAMETER("nodediameter", 2.0, 0.1, 5.0),
  NODE_LIFETIME("nodelifetime", 40, 1, 200),
  NODE_INTENSITY("nodeintensity", 1.0, 0.0, 5.0);

  /**
   * Checks if the given name has a corresponding tag
   *
   * @param nameToFind the text name to be looked for
   * @return the corresponding tag, or throw IllegalArgumentException if not found
   */
  static public DragonVariantTag getTagFromName(String nameToFind) throws IllegalArgumentException {
    DragonVariantTag retval = allTagNames.get(nameToFind);
    if (retval == null) throw new IllegalArgumentException("Tag not valid:" + nameToFind);
    return retval;
  }

  public String getTextname() {
    return textname;
  }

  public Object getDefaultValue() {
    return defaultValue;
  }

  /**
   * Convert the given input string value to the suitable type for this tag
   *
   * @param value the value to be converted
   * @return the converted value, or throws if an error
   */
  public Object convertValue(Object value) throws IllegalArgumentException {
    if (defaultValue instanceof Boolean) {
      return true;
    }
    if (defaultValue instanceof String) {
      if (value instanceof String) return value;
      throw new IllegalArgumentException("Expected a string");
    }
    if (!(value instanceof Number)) {
      throw new IllegalArgumentException("Expected a number");
    }
    Number numberValue;
    try {
      if (defaultValue instanceof Long) {
        numberValue = Long.parseLong(value.toString());
      } else if (defaultValue instanceof Double) {
        numberValue = Double.parseDouble(value.toString());
      } else {
        throw new IllegalArgumentException("internal error:unknown tag format in DragonVariantTag");
      }
    } catch (NumberFormatException nfe) {
      throw new IllegalArgumentException("expected a number with format:" + defaultValue.getClass());
    }

    if (minValue.isPresent() && minValue.get().compareTo(numberValue) > 0) {
      throw new IllegalArgumentException("Number out of range:" + numberValue + " < min (" + minValue.get() + ")");
    }
    if (maxValue.isPresent() && maxValue.get().compareTo(numberValue) < 0) {
      throw new IllegalArgumentException("Number out of range:" + numberValue + " > max (" + maxValue.get() + ")");
    }
    return numberValue;
  }
  static private HashMap<String, DragonVariantTag> allTagNames;
  private final String textname;
  private final Object defaultValue;
  private final Optional<Comparable> minValue;
  private final Optional<Comparable> maxValue;

  // set up helper structure
  static { // guaranteed to run only after all enums have been created
    allTagNames = new HashMap<>(DragonVariantTag.values().length);
    for (DragonVariantTag dragonVariantTag : DragonVariantTag.values()) {
      allTagNames.put(dragonVariantTag.getTextname(), dragonVariantTag);
    }
  }

  DragonVariantTag(String textname) {
    this.textname = textname;
    this.defaultValue = false;
    this.minValue = Optional.empty();
    this.maxValue = Optional.empty();
  }
  DragonVariantTag(String textname, String defaultValue) {
    this.textname = textname;
    this.defaultValue = defaultValue;
    this.minValue = Optional.empty();
    this.maxValue = Optional.empty();
  }
  DragonVariantTag(String textname, long defaultValue) {
    this.textname = textname;
    this.defaultValue = defaultValue;
    this.minValue = Optional.empty();
    this.maxValue = Optional.empty();
  }
  DragonVariantTag(String textname, long defaultValue, long minValue, long maxValue) {
    this.textname = textname;
    this.defaultValue = defaultValue;
    this.minValue = Optional.of(minValue);
    this.maxValue = Optional.of(maxValue);
  }

  DragonVariantTag(String textname, double defaultValue) {
    this.textname = textname;
    this.defaultValue = defaultValue;
    this.minValue = Optional.empty();
    this.maxValue = Optional.empty();
  }

  DragonVariantTag(String textname, double defaultValue, double minValue, double maxValue) {
    this.textname = textname;
    this.defaultValue = defaultValue;
    this.minValue = Optional.of(minValue);
    this.maxValue = Optional.of(maxValue);
  }

}
