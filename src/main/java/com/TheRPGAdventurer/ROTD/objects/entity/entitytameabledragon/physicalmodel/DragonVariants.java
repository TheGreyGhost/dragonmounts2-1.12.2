package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.physicalmodel;

import com.TheRPGAdventurer.ROTD.DragonMounts;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by TGG on 14/07/2019.
 * Holds all the variant tags applied to this dragon
 */
public class DragonVariants {

  public void addTagAndValue(DragonVariantTag tag, String tagValue)
  {
    Optional convertedValue = tag.convertValue(tagValue);
    if (convertedValue.isPresent()) {
      allAppliedTags.put(tag, convertedValue.get());
    } else {
      DragonMounts.logger.error("DragonVariantTag is incompatible with assigned value:" + tag + " --> " + tagValue);
    }
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
