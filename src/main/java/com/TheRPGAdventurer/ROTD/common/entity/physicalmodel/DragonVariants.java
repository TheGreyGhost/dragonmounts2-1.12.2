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
 * 2) Create a DragonVariants
 * 3) addTagAndValue() for all the tags in the config file
 * 4) call validateCollection() to apply all the registered VariantTagValidator functions on the collection
 * 5) call getValueOrDefault() to retrieve the value of a VariantTag
 */
public class DragonVariants {

  public enum Category {
    BREATH_WEAPON_PRIMARY("breathweaponprimary", 0),
    BREATH_WEAPON_SECONDARY("breathweaponsecondary", 1),
    PHYSICAL_MODEL("physicalmodel", 2),
    LIFE_STAGE("lifestage",3);

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
    Category(String textName, int idx) {
      this.textName = textName;
      this.idx = idx;
    }
  }

  /**
   * The VariantTagValidator is used to validate a group of variant tags
   * for example - to check if two incompatible tags have both been selected
   * If an incompatible combination is found, the validator may correct it or return it to defaults
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
