package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.physicalmodel;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.google.gson.*;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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

  public DragonVariantsReader(IResourceManager manager) {
    this.iResourceManager = manager;
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
        for (IResource iresource : iResourceManager.getAllResources(new ResourceLocation(s, "dragonvariants.json"))) {
          foundAtLeastOne = true;
          InputStream inputStream = iresource.getInputStream();
          try {
            Map<String, DragonVariants> allBreeds = deserialiseAllBreeds(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            if (invalidSyntaxFound) {
              DragonMounts.logger.warn("One or more errors occurred parsing dragonvariants.json:\n" + invalidSyntaxFields);
            }
            return allBreeds;
          } catch (RuntimeException runtimeexception) {
            DragonMounts.logger.warn("Invalid dragonvariants.json", (Throwable) runtimeexception);
          } finally {
            IOUtils.closeQuietly(inputStream);
          }

        }
      } catch (IOException e) {  // didn't find the file in this resourcedomain
      }
    }
    if (!foundAtLeastOne) {
      DragonMounts.logger.warn("Couldn't find dragonvariants.json");
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
   * @param object
   * @return
   */
  private DragonVariants deserializeAllTagsForOneBreed(JsonObject object) {
    DragonVariants dragonVariants = new DragonVariants();
    for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
      String tagName = entry.getKey();
      if (tagName == "flags" && entry.getValue().isJsonArray()) {
        deserializeFlagTags(dragonVariants, entry.getValue().getAsJsonArray());
      } else {
        try {
          deserialiseTagWithValue(dragonVariants, tagName, entry.getValue());
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
  private void deserializeFlagTags(DragonVariants dragonVariants, JsonArray jsonArray) {
    // Iterator to traverse the list
    Iterator<JsonElement> iterator = jsonArray.iterator();
    while (iterator.hasNext()) {
      JsonElement element = iterator.next();
      try {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
          syntaxError("flags");
        } else {
          deserialiseFlagTag(dragonVariants, element.getAsJsonPrimitive().getAsString());
        }
      } catch (IllegalArgumentException iae) {
        syntaxError(iae.getMessage());
      }
    }
  }

  private void deserialiseTagWithValue(DragonVariants dragonVariants, String tagName, JsonElement tagValue) throws IllegalArgumentException {
    DragonVariantTag tag = DragonVariantTag.getTagFromName(tagName);
    String value;
    try {
      value = tagValue.getAsJsonPrimitive().getAsString();
    } catch (Exception e) {
      throw new IllegalArgumentException("problem with tag " + tagName);
    }
    dragonVariants.addTagAndValue(tag, value);
  }

  private void deserialiseFlagTag(DragonVariants dragonVariants, String tagName) throws IllegalArgumentException {
    DragonVariantTag tag = DragonVariantTag.getTagFromName(tagName);
    dragonVariants.addTagAndValue(tag, "");
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
}
