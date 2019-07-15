package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.physicalmodel;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.util.Minify;
import com.google.common.io.CharStreams;
import com.google.gson.*;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by TGG on 14/07/2019.
 * This class reads the dragonvariants.json file to load in the DragonVariantTag information for all breeds
 * Usage:
 * 1) Create with DragonVariantsReader, giving it the Minecraft resource manager.  dragonvariants.json should be
 *      located in resources\assets\dragonmounts, same as sounds.json
 * 2) In common preinit(), call readVariants() to get the DragonVariants for all breeds
 */
public class DragonVariantsReader {

  public DragonVariantsReader(IResourceManager manager, String configFilename) {
    this.iResourceManager = manager;
    this.configFilename = configFilename;
  }

  /**
   * Read all variant information for all breeds from the dragonvariants.json file
   * @return Map of breed names (from json) to DragonVariants
   */
  public Map<String, DragonVariants> readVariants() {
    invalidSyntaxFound = false;
    invalidSyntaxFields = "";

    boolean foundAtLeastOne = false;
    for (String s : iResourceManager.getResourceDomains()) {
      try {
        for (IResource iresource : iResourceManager.getAllResources(new ResourceLocation(s, configFilename))) {
          foundAtLeastOne = true;
          InputStream inputStream = iresource.getInputStream();
          try {
            String inputString = CharStreams.toString(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String stripped = Minify.minify(inputString);
            Map<String, DragonVariants> allBreeds = deserialiseAllBreeds(new StringReader(stripped));
            if (invalidSyntaxFound) {
              DragonMounts.logger.warn("One or more errors occurred parsing " + configFilename + ":\n" + invalidSyntaxFields);
            }
            return allBreeds;
          } catch (RuntimeException runtimeexception) {
            DragonMounts.logger.warn("Invalid " + configFilename, (Throwable) runtimeexception);
          } finally {
            IOUtils.closeQuietly(inputStream);
          }

        }
      } catch (IOException e) {  // didn't find the file in this resourcedomain
      }
    }
    if (!foundAtLeastOne) {
      DragonMounts.logger.warn("Couldn't find " + configFilename);
    }

    return new HashMap<>();  // just return empty if we had trouble.
  }

  /**
   * Deserialise all tags for all breeds
   * @param input
   * @return
   * @throws JsonSyntaxException
   */
  private Map<String, DragonVariants> deserialiseAllBreeds(Reader input) throws JsonSyntaxException {
    Map<String, DragonVariants> retval = new HashMap<>();
    JsonParser parser = new JsonParser();
    JsonElement entireFile = parser.parse(input);
    if (!entireFile.isJsonObject()) throw new JsonSyntaxException("Malformed file");
    JsonObject obj = entireFile.getAsJsonObject();
    for (Map.Entry<String, JsonElement> entryForBreed : obj.entrySet()) {
      currentBreed = entryForBreed.getKey();
      JsonObject breedVariantData = entryForBreed.getValue().getAsJsonObject();
      DragonVariants dragonVariants = deserializeAllTagsForOneBreed(breedVariantData);
      retval.put(currentBreed, dragonVariants);
    }
    return retval;
  }

  /**
   * Deserialise all the tags for the specified breed's json object
   * @param jsonObject the object for this breed
   * @return
   */
  private DragonVariants deserializeAllTagsForOneBreed(JsonObject jsonObject) {
    DragonVariants dragonVariants = new DragonVariants();
    if (!jsonObject.isJsonObject()) {
      syntaxError("Malformed entry");
      return dragonVariants;
    }

    for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
      String categoryName = entry.getKey();
      try {
        DragonVariants.Category category = DragonVariants.Category.getCategoryFromName(categoryName);
        deserializeAllTagsForOneCategory(dragonVariants, category, entry.getValue());
      } catch (IllegalArgumentException iae) {
        syntaxError(iae.getMessage());
      }
    }
    return dragonVariants;
  }

  /**
   * Deserialise all the tags within the supplied category
   * @param jsonElement the object comprising the category
   * @return
   */
  private DragonVariants deserializeAllTagsForOneCategory(DragonVariants dragonVariants, DragonVariants.Category category, JsonElement jsonElement) {
    if (!jsonElement.isJsonObject()) throw new IllegalArgumentException("malformed entry for category " + category);
    JsonObject object = jsonElement.getAsJsonObject();
    for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
      String tagName = entry.getKey();
      if (tagName == "flags" && entry.getValue().isJsonArray()) {
        deserializeFlagTags(dragonVariants, category, entry.getValue().getAsJsonArray());
      } else {
        try {
          deserialiseTagWithValue(dragonVariants, category, tagName, entry.getValue());
        } catch (IllegalArgumentException iae) {
          syntaxError(iae.getMessage());
        }
      }
    }
    return dragonVariants;
  }

  /** Parse all the tags in the flags array
   * @param dragonVariants
   * @param jsonArray
   */
  private void deserializeFlagTags(DragonVariants dragonVariants, DragonVariants.Category category, JsonArray jsonArray) {
    // Iterator to traverse the list
    Iterator<JsonElement> flagIterator = jsonArray.iterator();
    while (flagIterator.hasNext()) {
      JsonElement element = flagIterator.next();
      try {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
          syntaxError("problem with tag " + category + ":flags");
        } else {
          deserialiseFlagTag(dragonVariants, category, element.getAsJsonPrimitive().getAsString());
        }
      } catch (IllegalArgumentException iae) {
        syntaxError(iae.getMessage());
      }
    }
  }

  private void deserialiseTagWithValue(DragonVariants dragonVariants, DragonVariants.Category category,
                                       String tagName, JsonElement tagValue) throws IllegalArgumentException {
    DragonVariantTag tag = DragonVariantTag.getTagFromName(tagName);
    String value;
    try {
      value = tagValue.getAsJsonPrimitive().getAsString();
    } catch (Exception e) {
      throw new IllegalArgumentException("problem with tag " + category + ":" + tagName);
    }
    dragonVariants.addTagAndValue(category, tag, value);
  }

  private void deserialiseFlagTag(DragonVariants dragonVariants, DragonVariants.Category category,
                                  String tagName) throws IllegalArgumentException {
    DragonVariantTag tag = DragonVariantTag.getTagFromName(tagName);
    dragonVariants.addTagAndValue(category, tag, "");
  }

  /** Raises a flag that an error occurred during parsing, and adds a message
   * @param msg error message
   */
  private void syntaxError(String msg) {
    invalidSyntaxFound = true;
    msg = currentBreed + "::" + msg;
    int spaceLeft = MAX_ERROR_LENGTH - invalidSyntaxFields.length();
    if (spaceLeft < 0) return;
    if (spaceLeft < msg.length()) {
      invalidSyntaxFields += msg.substring(0, Math.min(spaceLeft, msg.length())) + " {..more..}";
    } else {
      invalidSyntaxFields += msg + "\n";
    }
  }

  final int MAX_ERROR_LENGTH = 1000;
  private boolean invalidSyntaxFound;
  private String invalidSyntaxFields;
  private String currentBreed;

  private final IResourceManager iResourceManager;
  private final String configFilename;
}
