package com.TheRPGAdventurer.ROTD.common.entity.physicalmodel;

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
 * 5) call getValueOrDefault() to retrieve the value of a VariantTag
 */
public class DragonVariants {

  public enum Category {
    BREATH_WEAPON_PRIMARY("breathweaponprimary", 0, "This section is used to configure the primary breath weapon"),
    BREATH_WEAPON_SECONDARY("breathweaponsecondary", 1, "This section is used to configure the secondary breath weapon"),
    PHYSICAL_MODEL("physicalmodel", 2,
    * The age of the dragon affects the following aspects:
            * 1) PhysicalSize (metres) - for the base dragon, this is the height of the top of the back
    * 2) PhysicalMaturity (0->100%) - the physical abilities of the dragon such as being able to fly,
    * 3) EmotionalMaturity (0->100%) - the behaviour of the dragon eg sticking close to parent, running away from mobs
    * 4) BreathWeaponMaturity (0->100%) - the strength of the breath weapon
    * 5) AttackDamageMultiplier (0->100%) - physical attack damage
    * 6) HealthMultiplier (0->100%) - health
    * 7) ArmourMultiplier (0->100%) - armour
    * 8) ArmourToughnessMultiplier (0->100%) - armour toughness

    * lifeStageAges is the age corresponding to each ageLabel, in minecraft days
    * breathMaturityPoints, physicalMaturityPoints, emotionalMaturityPoints are the corresponding curve points, to be
    *    linearly interpolated
    * growthratePoints is the growth rate curve points which are then integrated to give the physical size
    * physicalSizePoints is the integral of the growth rate
    */



            "This section is used to configure the physical appearance of the dragon as well as its physical traits such as health and armour.  " +
            "The properties are scaled with the age of the dragon"

    ),
    LIFE_STAGE("lifestage", 3),
    EGG("lifestage", 4);

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
   * The VariantTagValidator is used to validate a group of variant tags
   * for example - to check if two incompatible tags have both been selected
   * If an incompatible combination is found, the validator may correct it or return it to defaults
   *
   * The validator may also initialise its internal structures in response to the calls
   * eg create ModelResourceLocations based on tags linked to models
   */
  public interface VariantTagValidator {
    void validateVariantTags(DragonVariants dragonVariants) throws IllegalArgumentException;
  }

  /**
   * Registers a VariantTagValidator to be called once the DragonVariants config file has been parsed
   */
  public static void addVariantTagValidator(VariantTagValidator variantTagValidator) {
    variantTagValidators.add(variantTagValidator);
  }

  public DragonVariants() {
    int categoryCount = Category.values().length;
    allAppliedTags = new ArrayList<>(categoryCount);
    for (int i = 0; i < categoryCount; ++i) {
      allAppliedTags.add(i, null);
    }
    for (Category category : Category.values()) {
      checkElementIndex(category.getIdx(), categoryCount);
      allAppliedTags.set(category.getIdx(), new HashMap<>());
    }
  }

  public void addTagAndValue(Category category, DragonVariantTag tag, Object tagValue) throws IllegalArgumentException {
    Object convertedValue = tag.convertValue(tagValue);
    allAppliedTags.get(category.getIdx()).put(tag, convertedValue);
  }

  /**
   * Check the collection of tags for validity according to the registered VariantTagValidators
   * @throws IllegalArgumentException if errors found
   */
  public void validateCollection() throws DragonVariantsException
  {
    DragonVariantsException.DragonVariantsErrors dragonVariantsErrors = new DragonVariantsException.DragonVariantsErrors();

    for (VariantTagValidator variantTagValidator : variantTagValidators) {
      try {
        variantTagValidator.validateVariantTags(this);
      } catch (DragonVariantsException dve) {
        dragonVariantsErrors.addError(dve);
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
    return allAppliedTags.get(category.getIdx()).containsValue(tag);
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

  public void removeTags(Category category, DragonVariantTag [] tagsToRemove) {
    for (DragonVariantTag dragonVariantTag : tagsToRemove)
      allAppliedTags.get(category.getIdx()).remove(dragonVariantTag);
  }

  private ArrayList<HashMap<DragonVariantTag, Object>> allAppliedTags;

  private static Set<VariantTagValidator> variantTagValidators = new HashSet<>();
}
