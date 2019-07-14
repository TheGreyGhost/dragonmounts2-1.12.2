package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.physicalmodel;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.google.common.collect.Lists;
import com.google.gson.*;
import net.minecraft.client.audio.Sound;
import net.minecraft.client.audio.SoundList;
import net.minecraft.util.JsonUtils;
import org.apache.commons.lang3.Validate;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by TGG on 14/07/2019.
 */
public class DragonVariantsSerializer implements JsonDeserializer<DragonVariants> {
  public DragonVariants deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
    JsonObject jsonobject = JsonUtils.getJsonObject(element, "entry");
//    boolean flag = JsonUtils.getBoolean(jsonobject, "replace", false);
//    String s = JsonUtils.getString(jsonobject, "subtitle", (String) null);
    DragonVariants dragonVariants = this.deserializeTags(jsonobject);
    return dragonVariants;
  }

  private DragonVariants deserializeTags(JsonObject object) {
    DragonVariants dragonVariants = new DragonVariants();
    for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
      String tagName = entry.getKey();
      try {
        Optional<DragonVariantTag> tag = DragonVariantTag.getTagFromName(tagName);
        if (tag.isPresent()) {
          String s = JsonUtils.getString(entry.getValue(), tagName);
          dragonVariants.addTagAndValue(tag.get(), s);
        } else {
          DragonMounts.logger.error("DragonVariantTag not found:" + tagName);
        }
      } catch (Exception e) {
        DragonMounts.logger.error("Error parsing tag:" + tagName + "." + e);
      }
    }
    return dragonVariants;
  }
}
//  List<Sound> list = Lists.<Sound>newArrayList();
//
//  if (object.has("sounds")) {
//    JsonArray jsonarray = JsonUtils.getJsonArray(object, "sounds");
//
//    for (int i = 0; i < jsonarray.size(); ++i) {
//      JsonElement jsonelement = jsonarray.get(i);
//
//      if (JsonUtils.isString(jsonelement)) {
//        String s = JsonUtils.getString(jsonelement, "sound");
//        list.add(new Sound(s, 1.0F, 1.0F, 1, Sound.Type.FILE, false));
//      } else {
//        list.add(this.deserializeSound(JsonUtils.getJsonObject(jsonelement, "sound")));
//      }
//    }
//  }
//
//  private Sound deserializeSound(JsonObject object) {
//    String s = JsonUtils.getString(object, "name");
//    Sound.Type sound$type = this.deserializeType(object, Sound.Type.FILE);
//    float f = JsonUtils.getFloat(object, "volume", 1.0F);
//    Validate.isTrue(f > 0.0F, "Invalid volume");
//    float f1 = JsonUtils.getFloat(object, "pitch", 1.0F);
//    Validate.isTrue(f1 > 0.0F, "Invalid pitch");
//    int i = JsonUtils.getInt(object, "weight", 1);
//    Validate.isTrue(i > 0, "Invalid weight");
//    boolean flag = JsonUtils.getBoolean(object, "stream", false);
//    return new Sound(s, f, f1, i, sound$type, flag);
//  }
//
//  private Sound.Type deserializeType(JsonObject object, Sound.Type defaultValue) {
//    Sound.Type sound$type = defaultValue;
//
//    if (object.has("type")) {
//      sound$type = Sound.Type.getByName(JsonUtils.getString(object, "type"));
//      Validate.notNull(sound$type, "Invalid type");
//    }
//
//    return sound$type;
//  }
//}
