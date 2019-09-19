package com.TheRPGAdventurer.ROTD.common.entity.physicalmodel;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

import java.util.*;

import static com.google.common.base.Preconditions.checkElementIndex;

/**
 * Created by TGG on 14/07/2019.
 * Holds all the variant tags applied to this dragon
 * Each DragonVariantTag represents a feature or variant that can be applied to a dragon
 * eg  NUMBER_OF_NECK_SEGMENTS = 4
 * See DragonVariantTag for more information
 * The advantage of using tags is that the different dragons can be easily configured without changing any code,
 * and all the different combinations are located in a single file, rather than being scattered throughout the code
 *
 * Each tag is grouped into a category (allows re-use of the same tag for different categories eg primary and secondary
 * breath weapon)
 *
 * Usage:
 * 1) During setup, optionally add VariantTagValidator functions to validate a collection of VariantTags.  These
 *    functions are called immediately after a DragonVariants config file has been parsed, and should be used to check whether
 *    particular combinations of tags are not permitted (eg three mutually exclusive tags have been selected, or
 *    if the dragon growth profile is positive but the final size is smaller than the initial size).
 *    The use of the validator allows the error message to be grouped with other errors during parsing, rather than
 *    during execution of the code which uses the tag values.
 *    The validator may also be used to initialise data structures within objects which use the validator (for example: create
 *       ModelResourceLocations)
 * 2) Create a DragonVariants
 * 3) addTagAndValue() for all the tags in the config file
 * 4) call validateCollection() to apply all the registered VariantTagValidator functions on the collection
 * 5) call initialiseResourcesForCollection() to initalise the resources required by the tags in this collection (eg ModelResources)
 * 6) call getValueOrDefault() to retrieve the value of a VariantTag
 *
 * You can optionally use DragonVariantsCategoryShortcut to access DragonVariants in a less cluttered way, eg
 *   DragonVariants.DragonVariantsCategoryShortcut dvc = dragonVariants.new DragonVariantsCategoryShortcut(Category.EGG);
 * then
 *  dvc.getValueOrDefault(TAGNAME)
 *  instead of
 *  dragonVariants.getValueOrDefault(Category.EGG, TAGNAME)
 *
 */
public class DragonVariants {

  public enum Category {
    BREATH_WEAPON_PRIMARY("breathweaponprimary", 0, "This section is used to configure the primary breath weapon"),
    BREATH_WEAPON_SECONDARY("breathweaponsecondary", 1, "This section is used to configure the secondary breath weapon"),
    PHYSICAL_MODEL("physicalmodel", 2, "Physical characteristics of the dragon model"),
    LIFE_STAGE("lifestage", 3,
          "The physical attributes of the dragon change with its age.  The dragon follows development stages similar to a human:\n" +
          "HATCHLING (newly born), INFANT, CHILD, EARLY TEEN, LATE TEEN, ADULT\n" +
          "The age corresponding to stage is given by the ageXXXXX tags.\n" +
          "The age of the dragon affects the following physical aspects:\n" +
          " 1) PhysicalSize (metres) - for the base dragon, this is the height of the top of the back\n" +
          " 2) PhysicalMaturity (0->100%) - the physical abilities of the dragon such as being able to fly,\n" +
          " 3) EmotionalMaturity (0->100%) - the behaviour of the dragon eg sticking close to parent, running away from mobs\n" +
          " 4) BreathWeaponMaturity (0->100%) - the strength of the breath weapon\n" +
          " 5) AttackDamageMultiplier (0->100%) - physical attack damage\n" +
          " 6) HealthMultiplier (0->100%) - health\n" +
          " 7) ArmourMultiplier (0->100%) - armour\n" +
          " 8) ArmourToughnessMultiplier (0->100%) - armour toughness\n" +
          " For each of these, the shape of the curve is given by corresponding tags, linearly interpolated.\n" +
          " e.g. if ageinfant is 1.0 days, healthpercentinfant = 20.0, agechild is 2.0 days, and healthpercentchild = 50.0,\n" +
          "   then healthpercent at 1.5 days of age is 35.0\n"
    ),
    EGG("egg", 4, "This section is used to configure the dragon's egg");

    /**
     * Checks if the given name has a corresponding Category
     *
     * @param nameToFind the text name to be looked for
     * @return the corresponding Category, or throw IllegalArgumentException if not found
     */
    static public Category getCategoryFromName(String nameToFind) throws IllegalArgumentException {
      for (Category category : Category.values()) {
        if (category.getTextName().equals(nameToFind)) {
          return category;
        }
      }
      throw new IllegalArgumentException("Category not valid:" + nameToFind);
    }

    public String getTextName() {
      return textName;
    }
    public String getComment() {return comment;}

    public int getIdx() {
      return idx;
    }
    private String textName;
    private int idx;
    private String comment;
    Category(String textName, int idx, String comment) {
      this.textName = textName;
      this.idx = idx;
      this.comment = comment;
    }
  }

  /**
   * The VariantTagValidator is used to validate a group of variant tags and initialise respective resources
   * for example - to check if two incompatible tags have both been selected
   * If an incompatible combination is found, the validator may correct it or return it to defaults
   * This checking is performed by validateVariantTags()
   * The VariantTagValidator may also initialise resources in response to a call to initialiseResources()
   * eg create ModelResourceLocations based on tags linked to models
   */
  public interface VariantTagValidator {
    void validateVariantTags(DragonVariants dragonVariants) throws IllegalArgumentException;
    void initaliseResources(DragonVariants dragonVariants) throws IllegalArgumentException;
  }

  /**
   * Registers a VariantTagValidator to be called once the DragonVariants config file has been parsed
   */
  public static void addVariantTagValidator(VariantTagValidator variantTagValidator) {
    variantTagValidators.add(variantTagValidator);
  }

  public DragonVariants(String breedInternalName) {
    int categoryCount = Category.values().length;
    allAppliedTags = new ArrayList<>(categoryCount);
    for (int i = 0; i < categoryCount; ++i) {
      allAppliedTags.add(i, null);
    }
    for (Category category : Category.values()) {
      checkElementIndex(category.getIdx(), categoryCount);
      allAppliedTags.set(category.getIdx(), new HashMap<>());
    }
    this.breedInternalName = breedInternalName;
  }

  public void addTagAndValue(Category category, DragonVariantTag tag, Object tagValue) throws IllegalArgumentException {
    if (!tag.getExpectedCategories().contains(category)) {
      throw new IllegalArgumentException(
              "Tag " + tag.getTextname() + " was found in unexpected category " + category.getTextName()
              + ".  Valid categories for this tag are: " + tag.getExpectedCategoriesAsText(", ")
              );
    }
    Object convertedValue = tag.convertValue(tagValue);
    allAppliedTags.get(category.getIdx()).put(tag, convertedValue);
  }

  /**
   * Check the collection of tags for validity according to the registered VariantTagValidators
   * @throws DragonVariantsException if errors found
   */
  public void validateCollection() throws DragonVariantsException
  {
    DragonVariantsException.DragonVariantsErrors dragonVariantsErrors = new DragonVariantsException.DragonVariantsErrors();

    for (VariantTagValidator variantTagValidator : variantTagValidators) {
      try {
        variantTagValidator.validateVariantTags(this);
      } catch (IllegalArgumentException iae) {
        dragonVariantsErrors.addError(iae);
      }
    }
    if (dragonVariantsErrors.hasErrors()) throw new DragonVariantsException(dragonVariantsErrors);
  }

  /**
   * Initialise all resources in the collection of tags according to the registered VariantTagValidators
   * @throws DragonVariantsException if a problem occurred
   */
  public void initialiseResourcesForCollection() throws DragonVariantsException
  {
    DragonVariantsException.DragonVariantsErrors dragonVariantsErrors = new DragonVariantsException.DragonVariantsErrors();

    for (VariantTagValidator variantTagValidator : variantTagValidators) {
      try {
        variantTagValidator.initaliseResources(this);
      } catch (IllegalArgumentException iae) {
        dragonVariantsErrors.addError(iae);
      }
    }
    if (dragonVariantsErrors.hasErrors()) throw new DragonVariantsException(dragonVariantsErrors);
  }


  /**
   * Gets the value of a particular tag applied to this dragon, or the default value if the tag hasn't been applied
   *
   * @param tag
   * @return
   */
  public Object getValueOrDefault(Category category, DragonVariantTag tag) {
    return allAppliedTags.get(category.getIdx()).getOrDefault(tag, tag.getDefaultValue());
  }

  /**
   * Does the config file contain this tag?
   * @param category
   * @param tag
   * @return true if the tag has been explicitly applied; false if using the default.
   */
  public boolean tagIsExplictlyApplied(Category category, DragonVariantTag tag) {
    return allAppliedTags.get(category.getIdx()).containsKey(tag);
  }

  /** remove one or more tags (set back to default)
   */
  public void removeTag(Category category, DragonVariantTag tagToRemove) {
    allAppliedTags.get(category.getIdx()).remove(tagToRemove);
  }

  public void removeTags(Category category, Collection<DragonVariantTag> tagsToRemove) {
    for (DragonVariantTag dragonVariantTag : tagsToRemove)
      allAppliedTags.get(category.getIdx()).remove(dragonVariantTag);
  }

  public void removeTags(Category category, DragonVariantTag... tagsToRemove) {
    for (DragonVariantTag dragonVariantTag : tagsToRemove)
      allAppliedTags.get(category.getIdx()).remove(dragonVariantTag);
  }

  public String getBreedInternalName() {
    return breedInternalName;
  }

  public ImmutableSortedMap<DragonVariantTag, Object> getAllAppliedTagsForCategory(Category category) {
    return ImmutableSortedMap.copyOf(allAppliedTags.get(category.getIdx()));
  }


  // shortcut to access DragonVariants with a particular category, without having to provide the category every time
  //  (visually less cluttered)
  // For information about the methods, see the corresponding methods in DragonVariants
  public class DragonVariantsCategoryShortcut {
    public DragonVariantsCategoryShortcut(Category category) {
      this.category = category;
    }

    public Object getValueOrDefault(DragonVariantTag tag) {
      return DragonVariants.this.getValueOrDefault(category, tag);
    }

    public boolean tagIsExplictlyApplied(DragonVariantTag tag) {
      return DragonVariants.this.tagIsExplictlyApplied(category, tag);
    }

    public boolean checkForConflict(DragonVariantsException.DragonVariantsErrors errors,
                                    DragonVariantTag masterTag, Object masterConflictState,
                                    boolean slaveConflictState, DragonVariantTag... slaveTags) {
      return DragonVariants.this.checkForConflict(errors, category, masterTag, masterConflictState,
                                                  slaveConflictState, slaveTags);
    }

      private Category category;
  }

  /** check whether there is a conflict between a "master" tag and "slave" tags
   * eg if there is a flag for "egg should spin", and two parameters "spin speed" and "spin direction":
   *   it's an error if the "spin speed" or "spin direction" are defined, but the "egg should spin" flag isn't
   *   In this case, the master tag is the flag and the two slaves are the parameters
   *   masterConflictState is false and slaveConflictState is true
   * @param errors    the errors log to populate with an error message if any
   * @param category the category to look in
   * @param masterTag the master tag to check
   * @param masterConflictState if the master tag is in this state, check for slave conflicts.
   * @param slaveConflictState if any of the slave tags are in this state, and master matches masterConflictState, then we have a conflict
   * @param slaveTags one or more slaveTags to check for conflicts
   * @return true if a conflict was discovered
   */
  public boolean checkForConflict(DragonVariantsException.DragonVariantsErrors errors, Category category,
                                  DragonVariantTag masterTag, Object masterConflictState,
                                  boolean slaveConflictState, DragonVariantTag... slaveTags) {
    String masterConditionText = "";
    if (masterTag.getDefaultValue() instanceof Boolean) {
      if ((Boolean)masterConflictState != tagIsExplictlyApplied(category, masterTag)) return false;
      masterConditionText = (Boolean)masterConflictState ? " is defined" : " is not defined";
    } else {
      if (!masterConflictState.equals(getValueOrDefault(category, masterTag))) return false;
      if (tagIsExplictlyApplied(category, masterTag)) {
        masterConditionText = " has the value \"" + masterConflictState.toString() + "\"";
      } else {
        masterConditionText = " has the default value \"" + masterConflictState.toString() + "\"";
      }
    }

    List<DragonVariantTag> conflictSlaves = new ArrayList<>();
    for (DragonVariantTag slave : slaveTags) {
      if (slaveConflictState == tagIsExplictlyApplied(category, slave)) {
        conflictSlaves.add(slave);
      }
    }
    if (conflictSlaves.isEmpty()) return false;

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("When ");
    stringBuilder.append(masterTag.getTextname());
    stringBuilder.append(masterConditionText);
    stringBuilder.append(" then the following tags must ");
    stringBuilder.append(slaveConflictState ? "not be defined:" : "be defined:");
    boolean first = true;
    for (DragonVariantTag dragonVariantTag : conflictSlaves) {
      if (!first) {
        stringBuilder.append(", ");
      }
      first = false;
      stringBuilder.append(dragonVariantTag.getTextname());
    }
    errors.addError(stringBuilder.toString());
    return true;
  }


/* todo
what I'm doing:
validation of possible tag values          '
validation of mutially exclusive tags - rules how?
particles spawnings - speeds, random exponential timing,
*/


  private ArrayList<HashMap<DragonVariantTag, Object>> allAppliedTags;
  private String breedInternalName;

  private static Set<VariantTagValidator> variantTagValidators = new HashSet<>();
}
