package com.TheRPGAdventurer.ROTD.common.entity.physicalmodel;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
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
 * Each category can also be "modified" to allow minor variations within the breed; for example male or female.
 * For example:
 * "physicalmodel": {
 *    "size": 2.0,
 *    "numberofnecksegments": 8
 * }
 * "physicalmodel:female": {
 *   "size": 1.6
 * }
 *
 * then if the dragon is female:
 * getValueOrDefault("size") returns 1.6
 * getValueOrDefault("numberofnecksegments") returns 8  (not defined in female, so falls back to unmodified category)
 *
 * Usage:
 * 1) During setup, optionally add VariantTagValidator functions to validate a collection of VariantTags.  These
 *    functions are called immediately after a DragonVariants config file has been parsed, and should be used to check whether
 *    particular combinations of tags are not permitted (eg three mutually exclusive tags have been selected, or
 *    if the dragon growth profile is positive but the final size is smaller than the initial size).
 *    The use of the validator allows the error message to be grouped with other errors during parsing, rather than
 *    during execution of the code which uses the tag values.
 *    The validator also has a function initaliseResources which be used to initialise data structures within objects which
 *    use the validator (for example: create ModelResourceLocations)
 * 2) Create a DragonVariants
 * 3) addTagAndValue() for all the tags in the config file
 * 4) call validateCollection() to apply all the registered VariantTagValidator functions on the collection.  checkForConflict is a
 *    helper function to help detect illegal combinations of options.  In case of validation errors, you can remove tags using
 *    removeTag() or removeTags()
 * 5) call initialiseResourcesForCollection() to initalise the resources required by the tags in this collection (eg ModelResources)
 * 6) call getValueOrDefault() to retrieve the value of a VariantTag, or isExplicitlyDefined to determine if the default is being used
 * 7) you can optionally call setComment() to add a comment; this is added to the start of the JSON used to store the DragonVariant
 *
 * You can optionally use DragonVariantsCategoryShortcut to access DragonVariants in a less cluttered way, eg
 *   DragonVariants.DragonVariantsCategoryShortcut dvc = dragonVariants.new DragonVariantsCategoryShortcut(Category.EGG);
 * then
 *  dvc.getValueOrDefault(TAGNAME)
 *  instead of
 *  dragonVariants.getValueOrDefault(Category.EGG, TAGNAME)
 *
 * Further notes on priority of the modifiers:
 * if the target has multiple modifiers applied to it (for example: female,albino):
 * 1) if there is an exact modified category match (egg:female,albino), use this
 * 2) if there is a category which contains some of the target tags, and doesn't contain any tags which aren't in the target,
 *     then take that.  If more than one category matches in this way, take the one with the lower priority
 * 3) otherwise, take the base category with no modifiers applied
 *
 */
public class DragonVariants {

  public enum Category {
    BREATH_WEAPON_PRIMARY("breathweaponprimary", 0, "This section is used to configure the primary breath weapon"),
    BREATH_WEAPON_SECONDARY("breathweaponsecondary", 1, "This section is used to configure the secondary breath weapon"),
    PHYSICAL_MODEL("physicalmodel", 2, "Physical characteristics of the dragon model (appearance)"),
    LIFE_STAGE("lifestage", 3,
          "The physical attributes of the dragon change with its age.  The dragon follows development stages similar to a human:\n" +
          "HATCHLING (newly born), INFANT, CHILD, EARLY TEEN, LATE TEEN, ADULT\n" +
          "The age corresponding to stage XXXXX is given by the ageXXXXX tags.\n" +
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

//    public int getIdx() {
//      return idx;
//    }
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
   *
   * The function is called repeatedly, once for each ModifiedCategory in the config file
   */
  public interface VariantTagValidator {
    void validateVariantTags(DragonVariants dragonVariants, ModifiedCategory modifiedCategory) throws IllegalArgumentException;
    void initaliseResources(DragonVariants dragonVariants, ModifiedCategory modifiedCategory) throws IllegalArgumentException;
  }

  /**
   * Registers a VariantTagValidator to be called once the DragonVariants config file has been parsed
   */
  public static void addVariantTagValidator(VariantTagValidator variantTagValidator) {
    variantTagValidators.add(variantTagValidator);
  }

  public DragonVariants(String breedInternalName) {
    allAppliedTags = new HashMap<>();
    this.breedInternalName = breedInternalName;
  }

  public void addTagAndValue(ModifiedCategory modifiedCategory, DragonVariantTag tag, Object tagValue) throws IllegalArgumentException {
    if (!tag.getExpectedCategories().contains(modifiedCategory.getCategory())) {
      throw new IllegalArgumentException(
              "Tag " + tag.getTextname() + " was found in unexpected category " + modifiedCategory.getCategory().getTextName()
              + ".  Valid categories for this tag are: " + tag.getExpectedCategoriesAsText(", ")
              );
    }
    Object convertedValue = tag.convertValue(tagValue);
    if (!allAppliedTags.containsKey(tag)) {
      allAppliedTags.put(tag, new HashMap<>());
    }
    allAppliedTags.get(tag).put(modifiedCategory, convertedValue);
  }

  /**
   * Check the collection of tags for validity according to the registered VariantTagValidators
   * @throws DragonVariantsException if errors found
   */
  public void validateCollection() throws DragonVariantsException
  {
    DragonVariantsException.DragonVariantsErrors dragonVariantsErrors = new DragonVariantsException.DragonVariantsErrors();

    Set<ModifiedCategory> allModifiedCategories = getAllModifiedCategories();
    for (VariantTagValidator variantTagValidator : variantTagValidators) {
      for (ModifiedCategory modifiedCategory : allModifiedCategories) {
        try {
          variantTagValidator.validateVariantTags(this, modifiedCategory);
        } catch (IllegalArgumentException iae) {
          dragonVariantsErrors.addError(iae);
        }
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

    Set<ModifiedCategory> allModifiedCategories = getAllModifiedCategories();
    for (VariantTagValidator variantTagValidator : variantTagValidators) {
      for (ModifiedCategory modifiedCategory : allModifiedCategories) {
        try {
          variantTagValidator.initaliseResources(this, modifiedCategory);
        } catch (IllegalArgumentException iae) {
          dragonVariantsErrors.addError(iae);
        }
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
  public Object getValueOrDefault(ModifiedCategory modifiedCategory, DragonVariantTag tag) {
    // algorithm:
    // if the tag hasn't been applied at all, return the default value
    // otherwise, find the best-matching modifiedcategory for the target modifiedCategory
    //   if the match is acceptable (same category, and doesn't contain any modifiers not in the target

    if (!allAppliedTags.containsKey(tag)) return tag.getDefaultValue();
    HashMap<ModifiedCategory, Object> mco = allAppliedTags.get(tag);
    ModifiedCategoryRanker mcr = new ModifiedCategoryRanker(modifiedCategory);
    ModifiedCategory bestMatch = Collections.min(mco.keySet(), mcr);
    if (mcr.isAcceptableMatch(bestMatch)) {
      return mco.get(bestMatch);
    } else {
      return tag.getDefaultValue();
    }
  }

  /**
   * Does the config file contain this tag in the given category?
   * @param category - the category for the tag, modifiers not relevant
   * @param tag
   * @return true if the tag has been explicitly applied to this category; false if using the default.
   */
  public boolean tagIsExplictlyApplied(Category category, DragonVariantTag tag) {
    if (!allAppliedTags.containsKey(tag)) return false;
    HashMap<ModifiedCategory, Object> mco = allAppliedTags.get(tag);
    for (ModifiedCategory mc : mco.keySet()) {
      if (mc.getCategory().equals(category)) return true;
    }
    return false;
  }

  /**
   * Does the config file contain this tag in the given modifiedcategory?
   * @param modifiedCategory - the specific modified category for the tag
   * @param tag
   * @return true if the tag has been explicitly applied to this modifiedcategory; false if using the default.
   */
  public boolean tagIsExplictlyApplied(ModifiedCategory modifiedCategory, DragonVariantTag tag) {
    if (!allAppliedTags.containsKey(tag)) return false;
    HashMap<ModifiedCategory, Object> mco = allAppliedTags.get(tag);
    return mco.containsKey(modifiedCategory);
  }


  /** remove one or more tags (set back to default)
   */
  public void removeTag(Category category, DragonVariantTag tagToRemove) {
    if (!allAppliedTags.containsKey(tagToRemove)) return;
    HashMap<ModifiedCategory, Object> mco = allAppliedTags.get(tagToRemove);
    mco.entrySet().removeIf(e->e.getKey().getCategory().equals(category));
  }

  public void removeTags(Category category, Collection<DragonVariantTag> tagsToRemove) {
    for (DragonVariantTag dragonVariantTag : tagsToRemove)
      removeTag(category, dragonVariantTag);
  }

  public void removeTags(Category category, DragonVariantTag... tagsToRemove) {
    for (DragonVariantTag dragonVariantTag : tagsToRemove)
      removeTag(category, dragonVariantTag);
  }

  /** remove one or more tags (set back to default)
   */
  public void removeTag(ModifiedCategory category, DragonVariantTag tagToRemove) {
    if (!allAppliedTags.containsKey(tagToRemove)) return;
    HashMap<ModifiedCategory, Object> mco = allAppliedTags.get(tagToRemove);
    mco.remove(tagToRemove);
  }

  public void removeTags(ModifiedCategory category, Collection<DragonVariantTag> tagsToRemove) {
    for (DragonVariantTag dragonVariantTag : tagsToRemove)
      removeTag(category, dragonVariantTag);
  }

  public void removeTags(ModifiedCategory category, DragonVariantTag... tagsToRemove) {
    for (DragonVariantTag dragonVariantTag : tagsToRemove)
      removeTag(category, dragonVariantTag);
  }

  public String getBreedInternalName() {
    return breedInternalName;
  }

  public ImmutableSortedMap<DragonVariantTag, Object> getAllAppliedTagsForCategory(ModifiedCategory modifiedCategory) {
    Map<DragonVariantTag, Object> allTags = new HashMap<>();
    for (Map.Entry<DragonVariantTag, HashMap<ModifiedCategory, Object>> entry : allAppliedTags.entrySet()) {
      HashMap<ModifiedCategory, Object> mco = entry.getValue();
      if (mco.containsKey(modifiedCategory)) {
        allTags.put(entry.getKey(), mco.get(modifiedCategory));
      }
    }

    return ImmutableSortedMap.copyOf(allTags);
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  // shortcut to access DragonVariants with a particular category, without having to provide the category every time
  //  (visually less cluttered)
  // For information about the methods, see the corresponding methods in DragonVariants
  public class DragonVariantsCategoryShortcut {
    public DragonVariantsCategoryShortcut(ModifiedCategory modifiedCategory) {
      this.modifiedCategory = modifiedCategory;
    }

    public Object getValueOrDefault(DragonVariantTag tag) {
      return DragonVariants.this.getValueOrDefault(modifiedCategory, tag);
    }

    public boolean tagIsExplictlyApplied(DragonVariantTag tag) {
      return DragonVariants.this.tagIsExplictlyApplied(modifiedCategory, tag);
    }

    public boolean checkForConflict(DragonVariantsException.DragonVariantsErrors errors,
                                    DragonVariantTag masterTag, Object masterConflictState,
                                    boolean slaveConflictState, DragonVariantTag... slaveTags) {
      return DragonVariants.this.checkForConflict(errors, modifiedCategory, masterTag, masterConflictState,
                                                  slaveConflictState, slaveTags);
    }

      private ModifiedCategory modifiedCategory;
  }

  public Set<ModifiedCategory> getAllModifiedCategories() {
    HashSet<ModifiedCategory> allModifiedCategories = new HashSet<>();
    for (Map.Entry<DragonVariantTag, HashMap<ModifiedCategory, Object>> entry : allAppliedTags.entrySet()) {
      allModifiedCategories.addAll(entry.getValue().keySet());
    }
    return allModifiedCategories;
  }

  /** check whether there is a conflict between a "master" tag and "slave" tags
   * eg if there is a flag for "egg should spin", and two parameters "spin speed" and "spin direction":
   *   it's an error if the "spin speed" or "spin direction" are defined, but the "egg should spin" flag is false
   *   In this case, the master tag is the flag and the two slaves are the parameters
   *   masterConflictState is false and slaveConflictState is true
   * @param errors    the errors log to populate with an error message if any
   * @param modifiedCategory the category to look in.  Does not filter through to other modifiedcategories below this one
   * @param masterTag the master tag to check
   * @param masterConflictState if the master tag is in this state, check for slave conflicts.
   * @param slaveConflictState if any of the slave tags are in this state, and master matches masterConflictState, then we have a conflict
   * @param slaveTags one or more slaveTags to check for conflicts
   * @return true if a conflict was discovered
   */

  public boolean checkForConflict(DragonVariantsException.DragonVariantsErrors errors, ModifiedCategory modifiedCategory,
                                  DragonVariantTag masterTag, Object masterConflictState,
                                  boolean slaveConflictState, DragonVariantTag... slaveTags) {
    String masterConditionText = "";
    if (masterTag.getDefaultValue() instanceof Boolean) {
      if ((Boolean)masterConflictState != getValueOrDefault(modifiedCategory, masterTag)) return false;
      masterConditionText = (Boolean)masterConflictState ? " is true" : " is false";
    } else {
      if (!masterConflictState.equals(getValueOrDefault(modifiedCategory, masterTag))) return false;
      if (tagIsExplictlyApplied(modifiedCategory, masterTag)) {
        masterConditionText = " has the value \"" + masterConflictState.toString() + "\"";
      } else {
        masterConditionText = " has the default value \"" + masterConflictState.toString() + "\"";
      }
    }

    List<DragonVariantTag> conflictSlaveBooleans = new ArrayList<>();
    List<DragonVariantTag> conflictSlaveNonBooleans = new ArrayList<>();
    for (DragonVariantTag slave : slaveTags) {
      if (slave.getDefaultValue() instanceof Boolean) {
        if ((Boolean)slaveConflictState == getValueOrDefault(modifiedCategory, slave)) {
          conflictSlaveBooleans.add(slave);
        }
      } else if (slaveConflictState == tagIsExplictlyApplied(modifiedCategory, slave)) {
        conflictSlaveNonBooleans.add(slave);
      }
    }
    if (conflictSlaveNonBooleans.isEmpty() && conflictSlaveBooleans.isEmpty()) return false;

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("When ");
    stringBuilder.append(masterTag.getTextname());
    stringBuilder.append(masterConditionText);
    if (!conflictSlaveBooleans.isEmpty()) {
      stringBuilder.append(" then the following tags must be ");
      stringBuilder.append(slaveConflictState ? "false:" : "true:");
      boolean first = true;
      for (DragonVariantTag dragonVariantTag : conflictSlaveBooleans) {
        if (!first) {
          stringBuilder.append(", ");
        }
        first = false;
        stringBuilder.append(dragonVariantTag.getTextname());
      }
      errors.addError(stringBuilder.toString());
    }
    if (!conflictSlaveNonBooleans.isEmpty()) {
      stringBuilder.append(conflictSlaveBooleans.isEmpty() ? " then" : " and");
      stringBuilder.append(" the following tags must ");
      stringBuilder.append(slaveConflictState ? "not be defined:" : "be defined:");
      boolean first = true;
      for (DragonVariantTag dragonVariantTag : conflictSlaveNonBooleans) {
        if (!first) {
          stringBuilder.append(", ");
        }
        first = false;
        stringBuilder.append(dragonVariantTag.getTextname());
      }
      errors.addError(stringBuilder.toString());
    }

    return true;
  }

  public enum Modifier implements Comparable<Modifier> {
    // define in decreasing order of priority.  In case of ambiguity, the highest priority modifier is used, eg
    //   the earliest in the list
    // Mutex = mutually exclusive flags.  If two flags have any mutex bits in common, they cannot both be applied at the same time
    MALE("male", 0x01, 0),
    FEMALE("female", 0x01, 1),
    DEBUG1("debug1", 0x02, 2, true),
    DEBUG2("debug2", 0x04, 3, true);
    ; // may add other modifiers in future

    private String textname;
    private int mutexFlags;
    private int bitIndex;
    private boolean debugOnly;

    private Modifier(String textname, int mutexFlags, int bitIndex) {
      this.textname = textname;
      this.mutexFlags = mutexFlags;
      this.bitIndex = bitIndex;
      if (bitIndex < 0 || bitIndex > Modifier.MAX_BIT_INDEX) throw new AssertionError("Illegal Modifier.bitindex:"+bitIndex);
      this.debugOnly = false;
    }
    private Modifier(String textname, int mutexFlags, int bitIndex, boolean debugOnly) {
      this.textname = textname;
      this.mutexFlags = mutexFlags;
      this.bitIndex = bitIndex;
      if (bitIndex < 0 || bitIndex > Modifier.MAX_BIT_INDEX) throw new AssertionError("Illegal Modifier.bitindex:"+bitIndex);
      this.debugOnly = debugOnly;
    }

    public String getTextname() {return textname;}
    public int getBitIndex() {return bitIndex;}

    public static Modifier getModifierFromText(String text) throws IllegalArgumentException {
      for (Modifier modifier : Modifier.values()) {
        if (text.equals(modifier.textname)) return modifier;
      }
      throw new IllegalArgumentException("Invalid modifier specified:" + text);
    }

    // returns a list of all modifiers, excluding any used for debugging
    public static List<Modifier> getAllModifiers() {
      ArrayList<Modifier> allModifiers = new ArrayList<>();
      for (Modifier modifier : Modifier.values()) {
        if (!modifier.debugOnly) allModifiers.add(modifier);
      }
      return allModifiers;
    }

    // return true if all modifiers can co-exist, false if any mutexes clash
    public static boolean validateMutexes(Modifier... modifiers) {
      int appliedMutex = 0;
      for (Modifier modifier : modifiers) {
        if (0 != (appliedMutex & modifier.mutexFlags)) {
          return false;
        }
        appliedMutex |= modifier.mutexFlags;
      }
      return true;
    }

    // return all other Modifiers which share the same mutex
    public List<Modifier> getSameMutex() {
      List<Modifier> retval = new ArrayList<>();
      for (Modifier modifier : Modifier.values()) {
        if (modifier.mutexFlags == this.mutexFlags) {
          retval.add(modifier);
        }
      }
      return retval;
    }

    static final public int MAX_BIT_INDEX = 31;
  }

  public static class ModifiedCategory {
    public ModifiedCategory(Category category, Modifier... modifiers) throws IllegalArgumentException {
      initialise(category, modifiers);
    }

    public ModifiedCategory(Category category, Modifiers modifiers) throws IllegalArgumentException {
      List<Modifier> modifierList = modifiers.getModifierList();
      Modifier [] modifierArray = new Modifier[modifierList.size()];
      modifierArray = modifierList.toArray(modifierArray);
      initialise(category, modifierArray);
    }

    public Category getCategory() {
      return category;
    }

    public static ModifiedCategory parseFromString(String text) throws IllegalArgumentException {
      String[] split = text.split(":");
      Category category;
      if (split.length == 0) {
        category = Category.getCategoryFromName("");  // will almost certainly throw IAE
      } else {
        category = Category.getCategoryFromName(split[0]);
      }
      if (split.length == 1) return new ModifiedCategory(category);
      if (split.length != 2) {
        throw new IllegalArgumentException("Syntax error with category:modifiers value (" + text + ")");
      }
      split = split[1].split(",");
      Modifier[] modifiers = new Modifier[split.length];
      int i = 0;
      for (String modText : split) {
        modifiers[i++] = Modifier.getModifierFromText(modText.trim());
      }
      return new ModifiedCategory(category, modifiers);
    }

    private void initialise(Category category, Modifier... modifiers) throws IllegalArgumentException {
      this.category = category;
      this.appliedModifiers = modifiers;
      Arrays.sort(this.appliedModifiers);
      int appliedMutex = 0;
      int clashingMutex = 0;
      for (Modifier modifier : modifiers) {
        if (0 != (appliedMutex & modifier.mutexFlags)) {
          clashingMutex |= modifier.mutexFlags;
        }
        appliedMutex |= modifier.mutexFlags;
      }
      if (clashingMutex == 0) return;

      String errorMsg = "The requested category:modifiers contains the following mutually-exclusive modifiers - ";
      errorMsg += category.getTextName() + ":";
      boolean first = true;
      for (Modifier modifier : modifiers) {
        if (0 != (clashingMutex & modifier.mutexFlags)) {
          if (!first) errorMsg += ", ";
          errorMsg += modifier.getTextname();
          first = false;
        }
      }
      throw new IllegalArgumentException(errorMsg);

    }

    private Category category;
    private Modifier[] appliedModifiers;  // sorted in order

    @Override
    public boolean equals(Object other) {
      if (other == this) return true;
      if (!(other instanceof ModifiedCategory)) return false;
      ModifiedCategory otherMC = (ModifiedCategory) other;
      if (!category.equals(otherMC.category)) return false;
      return Arrays.equals(appliedModifiers, otherMC.appliedModifiers);
    }

    @Override
    public int hashCode() {
      int hc1 = category.hashCode();
      int hc2 = appliedModifiers.hashCode();
      int hc3 = hc1 ^ hc2;
      return category.hashCode() ^ Arrays.deepHashCode(appliedModifiers);
    }

    @Override
    public String toString() {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(category.getTextName());
      if (appliedModifiers.length > 0) {
        stringBuilder.append(":");
        boolean first = true;
        for (Modifier modifier : appliedModifiers) {
          if (!first) stringBuilder.append(", ");
          first = false;
          stringBuilder.append(modifier.getTextname());
        }
      }
      return stringBuilder.toString();
    }
  }

  // Create the Ranker specifying the target category
  // This will then allow sorting on the ModifiedCategory which is the best match for the target
  // The sorting ignores category, only pays attention to the modifiers
  public static class ModifiedCategoryRanker implements Comparator<ModifiedCategory> {

    public ModifiedCategoryRanker(ModifiedCategory target) {
      this.target = target;
    }

    @Override
    public int compare(ModifiedCategory o1, ModifiedCategory o2) {
      if (o1.equals(o2)) return 0;

      ArrayList<Modifier> o1Matches = findMatchingModifiers(o1);
      if (o1Matches != null && o1Matches.size() == target.appliedModifiers.length) return -1;
      ArrayList<Modifier> o2Matches = findMatchingModifiers(o2);
      if (o1Matches == null && o2Matches == null) return 0;
      if (o1Matches == null && o2Matches != null) return 1;
      if (o1Matches != null && o2Matches == null) return -1;

      if (o1Matches.size() > o2Matches.size()) {
        return -1;
      } else if (o1Matches.size() < o2Matches.size()) {
        return  1;
      }

      for (int i = 0; i < o1Matches.size(); ++i) {
        int compare = o1Matches.get(i).compareTo(o2Matches.get(i));
        if (compare != 0) return compare;
      }
      return 0;
    }

    // Is the given ModifiedCategory an acceptable match for the target?
    // i.e. it belongs to the same category, and it doesn't contain any
    //   modifiers which are applied to the target?
    public boolean isAcceptableMatch(ModifiedCategory mc) {
      List<Modifier> matched = findMatchingModifiers(mc);
      return (matched != null);
    }

    // returns the matching modifiers, or null if incompatible (mc contains modifiers which aren't in the target)
    private ArrayList<Modifier> findMatchingModifiers(ModifiedCategory mc) {
      ArrayList<Modifier> matchedModifers = new ArrayList<>();
      int srcIdx = 0;
      int dstIdx = 0;

      while (srcIdx < target.appliedModifiers.length && dstIdx < mc.appliedModifiers.length) {
        int compare = target.appliedModifiers[srcIdx].compareTo(mc.appliedModifiers[dstIdx]);
        if (compare == 0) {
          matchedModifers.add(target.appliedModifiers[srcIdx]);
          ++srcIdx;
          ++dstIdx;
        } else if (compare < 0) {
          ++srcIdx;
        } else {
          return null;
        }
      }
      if (dstIdx < mc.appliedModifiers.length) return null;
      return matchedModifers;
    }

    private ModifiedCategory target;
  }

  // Sort the modified category alphabetically, first on category, then on the list of modifiers
  public static class ModifiedCategoryAlphabeticSorter implements Comparator<ModifiedCategory> {

    public ModifiedCategoryAlphabeticSorter() {
    }

    @Override
    public int compare(ModifiedCategory o1, ModifiedCategory o2) {
      // algorithm:
      // 1: compare categories
      // 2: find the first tag which isn't identical in both categories (modifiers are sorted
      //    in alphabetical order already
      if (o1.equals(o2)) return 0;
      int cmp = o1.getCategory().getTextName().compareTo(o2.getCategory().getTextName());
      if (cmp != 0) return cmp;

      for (int i = 0; i < o1.appliedModifiers.length; ++i) {
        Modifier m1 = o1.appliedModifiers[i];
        if (o2.appliedModifiers.length < i+1) return -1;
        Modifier m2 = o2.appliedModifiers[i];
        cmp = m1.getTextname().compareTo(m2.getTextname());
        if (cmp != 0) return cmp;
      }
      return 0;
    }
  }

  private String comment = "";

  private HashMap<DragonVariantTag, HashMap<ModifiedCategory, Object>> allAppliedTags;
  private String breedInternalName;

  private static Set<VariantTagValidator> variantTagValidators = new HashSet<>();
}
