package com.TheRPGAdventurer.ROTD.common.entity.physicalmodel;

import java.util.ArrayList;
import java.util.HashMap;

import static com.google.common.base.Preconditions.checkElementIndex;

/**
 * Created by TGG on 14/07/2019.
 * Holds all the variant tags applied to this dragon
 * Each DragonVariantTag represents a feature or variant that can be applied to a dragon
 * eg  NUMBER_OF_NECK_SEGMENTS = 4
 * See DragonVariantTag for more information
 * The advantage of using tags is that the different dragons can be easily configured without changing any code,
 * and all the different combinations are located in a single file, rather than being scattered throughout the code
 * <p>
 * Each tag is grouped into a category (allows re-use of the same tag for different categories eg primary and secondary
 * breath weapon)
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
   * Gets the value of a particular tag applied to this dragon, or the default value if the tag hasn't been applied
   *
   * @param tag
   * @return
   */
  public Object getValueOrDefault(Category category, DragonVariantTag tag) {
    return allAppliedTags.get(category.getIdx()).getOrDefault(tag, tag.getDefaultValue());
  }
  private ArrayList<HashMap<DragonVariantTag, Object>> allAppliedTags;
}
