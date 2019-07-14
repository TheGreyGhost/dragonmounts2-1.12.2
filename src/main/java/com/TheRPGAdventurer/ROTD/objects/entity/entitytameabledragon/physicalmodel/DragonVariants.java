package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.physicalmodel;

import com.TheRPGAdventurer.ROTD.DragonMounts;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by TGG on 14/07/2019.
 * Holds all the variant tags applied to this dragon
 * Each DragonVariantTag represents a feature or variant that can be applied to a dragon
 * eg  NUMBER_OF_NECK_SEGMENTS = 4
 * See DragonVariantTag for more information
 * The advantage of using tags is that the different dragons can be easily configured without changing any code,
 *   and all the different combinations are located in a single file, rather than being scattered throughout the code
 */
public class DragonVariants {

  public void addTagAndValue(DragonVariantTag tag, String tagValue) throws IllegalArgumentException
  {
    Object convertedValue = tag.convertValue(tagValue);
    allAppliedTags.put(tag, convertedValue);
  }

  /**
   * Gets the value of a particular tag applied to this dragon, or the default value if the tag hasn't been applied
   * @param tag
   * @return
   */
  public Object getValueOrDefault(DragonVariantTag tag)
  {
    return allAppliedTags.getOrDefault(tag, tag.getDefaultValue());
  }

  private HashMap<DragonVariantTag, Object> allAppliedTags = new HashMap();
}
