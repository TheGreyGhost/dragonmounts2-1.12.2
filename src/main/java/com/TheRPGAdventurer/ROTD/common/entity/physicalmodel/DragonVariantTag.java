package com.TheRPGAdventurer.ROTD.common.entity.physicalmodel;

import com.TheRPGAdventurer.ROTD.DragonMounts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants.Category;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

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
 *
 * Usage:
 * 1) During setup, all classes which need to use a tag should add their desired tags
 *     eg DragonVariantTag NUMBER_OF_NECK_SEGMENTS = DragonVariantTag.addTag("numberofnecksegments", 5);
 *     Tags can have one of four types:
 *     Flag (true or false)
 *     String
 *     Long
 *     Double
 *     String, Long and Double must have a default value
 *     Long and Double can have optional minimum and maximum  (Inclusive i.e. [min = 3, max = 6] -> 3, 4, 5, 6 ok)
 *     The Comment is intended to be human-readable to help people configure the parameters correctly
 *
 * 2) The tag parser uses the following methods:
 *     getTagFromName() to find the tag corresponding to a string
 *     convertValue() to convert a tag value to the format expected by the tag
 *     getDefaultValue() - for use if the tag doesn't exist in the config file
 */
public class DragonVariantTag implements Comparable<DragonVariantTag> {

  /*
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


*/

  public static DragonVariantTag addTag(String textname, boolean defaultValue, String comment) {
    return addTag(textname, defaultValue, Optional.empty(), Optional.empty(), comment);
  }

  public static DragonVariantTag addTag(String textname, String defaultValue, String comment) {
    return addTag(textname, defaultValue, Optional.empty(), Optional.empty(), comment);
  }

  public static DragonVariantTag addTag(String textname, long defaultValue, String comment) {
    return addTag(textname, defaultValue, Optional.empty(), Optional.empty(), comment);
  }

  public static DragonVariantTag addTag(String textname, double defaultValue, String comment) {
    return addTag(textname, defaultValue, Optional.empty(), Optional.empty(), comment);
  }

  public static DragonVariantTag addTag(String textname, long defaultValue, long minValue, long maxValue, String comment) {
    return addTag(textname, defaultValue, Optional.of(minValue), Optional.of(maxValue), comment);
  }

  public static DragonVariantTag addTag(String textname, double defaultValue, double minValue, double maxValue, String comment) {
    return addTag(textname, defaultValue, Optional.of(minValue), Optional.of(maxValue), comment);
  }

  /**
   * Adds these categories as an expected place to find this tag
   * @param categories one or more categories
   * @return returns the same tag to allow chaining
   */
  public DragonVariantTag categories(Category... categories) {
    for (Category category : categories)
        expectedCategories.add(category);
    return this;
  }

  /**
   * For string tags: Adds these values as permissible values for the tag
   * @param values
   * @return returns the same tag to allow chaining
   */
  public DragonVariantTag values(String... values) {
    for (String value : values)
      permissibleValues.add(value);
    return this;
  }

  /**
   * Checks if the given name has a corresponding tag
   *
   * @param nameToFind the text name to be looked for
   * @return the corresponding tag, or throw IllegalArgumentException if not found
   */
  static public DragonVariantTag getTagFromName(String nameToFind) throws DragonVariantTagNotFoundException {
    DragonVariantTag retval = allTagNames.get(nameToFind);
    if (retval == null) throw new DragonVariantTagNotFoundException("Unknown tag:" + nameToFind);
    return retval;
  }

  public String getTextname() {
    return textname;
  }

  public Object getDefaultValue() {
    return defaultValue;
  }

  public String getComment() {
    String permissibleValues = getPermissibleValuesAsText("\", \"");
    if (permissibleValues.length() > 0) {
      permissibleValues = " Permissible values are: \"" + permissibleValues + "\".";
    }
    if (minValue.isPresent()) {
      permissibleValues += " MinVal:" + minValue.get().toString() + " ";
    }
    if (maxValue.isPresent()) {
      permissibleValues += " MaxVal:" + maxValue.get().toString() + " ";
    }
    return comment + permissibleValues;

  }

  public ImmutableList<Category> getExpectedCategories() {
    return ImmutableList.copyOf(expectedCategories);
  }

  public ImmutableList<String> getPermissibleValues() {
    return ImmutableList.copyOf(permissibleValues);
  }

  /**
   * List all the category names expected for this tag
   * @return a concatenated list of all expected category names with the separator between the entries
   */
  public String getExpectedCategoriesAsText(String separator) {
    StringBuilder stringBuilder = new StringBuilder();
    boolean first = true;
    for (Category category : getExpectedCategories()) {
      if (!first) stringBuilder.append(separator);
      stringBuilder.append(category.getTextName());
      first = false;
    }
    return stringBuilder.toString();
  }

  /**
   * List all the category names expected for this tag
   * @return a concatenated list of all expected category names with the separator between the entries
   */
  public String getPermissibleValuesAsText(String separator) {
    StringBuilder stringBuilder = new StringBuilder();
    boolean first = true;
    for (String value : getPermissibleValues()) {
      if (!first) stringBuilder.append(separator);
      stringBuilder.append(value);
      first = false;
    }
    return stringBuilder.toString();
  }

  /**
   * Convert the given input value to the suitable type for this tag
   *
   * @param value the value to be converted
   * @return the converted value, or throws if the value can't be converted
   */
  public Object convertValue(Object value) throws IllegalArgumentException {
    if (defaultValue instanceof Boolean) {
      if (!(value instanceof Boolean)) {
        throw new IllegalArgumentException("Expected a true or false (no quotes)");
      }
      return (Boolean)value;
    }
    if (defaultValue instanceof String) {
      if (!(value instanceof String)) {
        throw new IllegalArgumentException("Expected a string");
      }
      if (permissibleValues.size() > 0 && !permissibleValues.contains((String)value)) {
        throw new IllegalArgumentException("The tag did not have one of the permissible values (\"" + getPermissibleValuesAsText("\", \"") + "\")");
      }
      return value;
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
      throw new IllegalArgumentException("Expected a number with format:" + defaultValue.getClass());
    }

    if (minValue.isPresent() && minValue.get().compareTo(numberValue) > 0) {
      throw new IllegalArgumentException("Number out of range:" + numberValue + " < min (" + minValue.get() + ")");
    }
    if (maxValue.isPresent() && maxValue.get().compareTo(numberValue) < 0) {
      throw new IllegalArgumentException("Number out of range:" + numberValue + " > max (" + maxValue.get() + ")");
    }
    return numberValue;
  }

  /**
   * Return a list of all the defined DragonVariantTags
   * @return
   */
  public static ImmutableSet<DragonVariantTag> getAllDragonVariantTags() {
    return ImmutableSet.copyOf(allTagNames.values());
  }

  private static DragonVariantTag addTag(String textname, Object defaultValue, Optional<Comparable> minValue, Optional<Comparable> maxValue, String comment)
  {
    if (allTagNames.containsKey(textname)) {
      DragonMounts.loggerLimit.warn_once("DragonVariantTag already contains:"+textname);
      return allTagNames.get(textname);
    }
    DragonVariantTag newTag = new DragonVariantTag(textname, defaultValue, minValue, maxValue, comment);
    allTagNames.put(textname, newTag);
    return newTag;
  }

  private DragonVariantTag(String textname, Object defaultValue, Optional<Comparable> minValue, Optional<Comparable> maxValue, String comment) {
    this.textname = textname;
    this.defaultValue = defaultValue;
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.comment = comment;
  }

  static private HashMap<String, DragonVariantTag> allTagNames = new HashMap<>();
  private final String textname;
  private final Object defaultValue;
  private final Optional<Comparable> minValue;
  private final Optional<Comparable> maxValue;
  private final String comment; // a comment for the config file

  private ArrayList<Category> expectedCategories = new ArrayList<>(); // which categories do we expect to find this tag in?
  private ArrayList<String> permissibleValues = new ArrayList<>(); // which values can this tag take (for String tags only)

  @Override
  public int compareTo(DragonVariantTag other) {
    return this.textname.compareTo(other.textname);
  }
}
