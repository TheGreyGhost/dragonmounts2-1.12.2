package com.TheRPGAdventurer.ROTD.common.entity.physicalmodel;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.util.Minify;
import com.google.common.io.CharStreams;
import com.google.gson.*;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by TGG on 14/07/2019.
 * This class reads the dragonvariants.json file to load in the DragonVariantTag information for all breeds
 * Usage:
 * 1) Create with DragonVariantsReader, giving it the Minecraft resource manager and the resourcelocation of the
 * the config file (dragonmounts.json)
 * 2) In common preinit(), call readVariants() to get the DragonVariants for all breeds
 */
public class DragonVariantsReader {

  public DragonVariantsReader(IResourceManager manager, ResourceLocation configFileLocation) {
    this.iResourceManager = manager;
    this.configFileLocation = configFileLocation;
    final int MAX_ERROR_LENGTH = 1000;
    dragonVariantsErrors = new DragonVariantsException.DragonVariantsErrors(MAX_ERROR_LENGTH);
  }

  /**
   * Read all variant information for all breeds from the dragonvariants.json file
   *
   * @return Map of breed names (from json) to DragonVariants
   */
  public Map<String, DragonVariants> readVariants() {
    dragonVariantsErrors.clear();

    try {
      IResource iResource = iResourceManager.getResource(configFileLocation);
      InputStream inputStream = iResource.getInputStream();
      try {
        String inputString = CharStreams.toString(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String stripped = Minify.minify(inputString);
        Map<String, DragonVariants> allBreeds = deserialiseAllBreeds(new StringReader(stripped));
        if (dragonVariantsErrors.hasErrors()) {
          DragonMounts.logger.warn("One or more errors occurred parsing " + configFileLocation + ":\n" + dragonVariantsErrors.toString());
        }
        return allBreeds;
      } catch (RuntimeException runtimeexception) {
        DragonMounts.logger.warn("Invalid " + configFileLocation + "; " + runtimeexception.getMessage());
      } finally {
        IOUtils.closeQuietly(inputStream);
      }

    } catch (IOException e) {  // didn't find the file in this resourcedomain
      DragonMounts.logger.warn("Couldn't find " + configFileLocation);
    }

    return new HashMap<>();  // just return empty if we had trouble.
  }

//  // old - search all domains
//  /**
//   * Read all variant information for all breeds from the dragonvariants.json file
//   * @return Map of breed names (from json) to DragonVariants
//   */
//  public Map<String, DragonVariants> readVariants() {
//    invalidSyntaxFound = false;
//    invalidSyntaxFields = "";
//
//
//
//    boolean foundAtLeastOne = false;
//    for (String s : iResourceManager.getResourceDomains()) {
//      try {
//        for (IResource iresource : iResourceManager.getAllResources(new ResourceLocation(s, configFilename))) {
//          foundAtLeastOne = true;
//          InputStream inputStream = iresource.getInputStream();
//          try {
//            String inputString = CharStreams.toString(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
//            String stripped = Minify.minify(inputString);
//            Map<String, DragonVariants> allBreeds = deserialiseAllBreeds(new StringReader(stripped));
//            if (invalidSyntaxFound) {
//              DragonMounts.logger.warn("One or more errors occurred parsing " + configFilename + ":\n" + invalidSyntaxFields);
//            }
//            return allBreeds;
//          } catch (RuntimeException runtimeexception) {
//            DragonMounts.logger.warn("Invalid " + configFilename, (Throwable) runtimeexception);
//          } finally {
//            IOUtils.closeQuietly(inputStream);
//          }
//
//        }
//      } catch (IOException e) {  // didn't find the file in this resourcedomain
//      }
//    }
//    if (!foundAtLeastOne) {
//      DragonMounts.logger.warn("Couldn't find " + configFilename);
//    }
//
//    return new HashMap<>();  // just return empty if we had trouble.
//  }

  /**
   * Deserialise all tags for all breeds
   *
   * @param input
   * @return
   * @throws JsonSyntaxException
   */
  private Map<String, DragonVariants> deserialiseAllBreeds(Reader input) throws JsonSyntaxException {

    // ensure that the validators are also called for the default breed; otherwise resources aren't registered for the default breed..
    try {
      DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getDefaultBreed().getDragonVariants().validateCollection();
    } catch (DragonVariantsException dve) {
      dragonVariantsErrors.addError(dve);
    }

    Map<String, DragonVariants> retval = new HashMap<>();
    JsonParser parser = new JsonParser();
    JsonElement entireFile = parser.parse(input);
    if (!entireFile.isJsonObject()) throw new JsonSyntaxException("Malformed file");
    JsonObject obj = entireFile.getAsJsonObject();
    for (Map.Entry<String, JsonElement> entryForBreed : obj.entrySet()) {
      currentBreed = entryForBreed.getKey();
      JsonObject breedVariantData = entryForBreed.getValue().getAsJsonObject();

      DragonVariants dragonVariants = deserializeAllTagsForOneBreed(breedVariantData);
      try {
        retval.put(currentBreed, dragonVariants);
        DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().createDragonBreedNew(currentBreed, dragonVariants);
        dragonVariants.validateCollection();
      } catch (IllegalArgumentException iae) {
        dragonVariantsErrors.addError(iae);
      }
    }

    return retval;
  }

  /**
   * Deserialise all the tags for the specified breed's json object
   *
   * @param jsonObject the object for this breed
   * @return
   */
  private DragonVariants deserializeAllTagsForOneBreed(JsonObject jsonObject) {
    DragonVariants dragonVariants = new DragonVariants();
    if (!jsonObject.isJsonObject()) {
      dragonVariantsErrors.addError("Malformed entry");
      return dragonVariants;
    }

    for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
      String categoryName = entry.getKey();
      try {
        DragonVariants.Category category = DragonVariants.Category.getCategoryFromName(categoryName);
        deserializeAllTagsForOneCategory(dragonVariants, category, entry.getValue());
      } catch (IllegalArgumentException iae) {
        dragonVariantsErrors.addError(iae);
      }
    }
    return dragonVariants;
  }

  /**
   * Deserialise all the tags within the supplied category
   *
   * @param jsonElement the object comprising the category
   * @return
   */
  private DragonVariants deserializeAllTagsForOneCategory(DragonVariants dragonVariants, DragonVariants.Category category, JsonElement jsonElement) {
    if (!jsonElement.isJsonObject()) throw new IllegalArgumentException("malformed entry for category " + category);
    JsonObject object = jsonElement.getAsJsonObject();
    for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
      String tagName = entry.getKey();
      if (tagName.equals("flags") && entry.getValue().isJsonArray()) {
        deserializeFlagTags(dragonVariants, category, entry.getValue().getAsJsonArray());
      } else {
        try {
          deserialiseTagWithValue(dragonVariants, category, tagName, entry.getValue());
        } catch (IllegalArgumentException iae) {
          dragonVariantsErrors.addError(iae);
        }
      }
    }
    return dragonVariants;
  }

  /**
   * Parse all the tags in the flags array
   *
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
          dragonVariantsErrors.addError("problem with tag " + category + ":flags");
        } else {
          deserialiseFlagTag(dragonVariants, category, element.getAsJsonPrimitive().getAsString());
        }
      } catch (IllegalArgumentException iae) {
        dragonVariantsErrors.addError(iae);
      }
    }
  }

  private void deserialiseTagWithValue(DragonVariants dragonVariants, DragonVariants.Category category,
                                       String tagName, JsonElement tagValue) throws IllegalArgumentException {
    DragonVariantTag tag = DragonVariantTag.getTagFromName(tagName);
    String value;
    try {
      JsonPrimitive jsonPrimitive = tagValue.getAsJsonPrimitive();
      if (jsonPrimitive.isNumber()) {
        dragonVariants.addTagAndValue(category, tag, jsonPrimitive.getAsNumber());
      } else if (jsonPrimitive.isString()) {
        dragonVariants.addTagAndValue(category, tag, jsonPrimitive.getAsString());
      } else {
        throw new IllegalArgumentException("value has an unexpected type");
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("problem with tag " + category + ":" + tagName + "-" + e.getMessage());
    }
  }

  private void deserialiseFlagTag(DragonVariants dragonVariants, DragonVariants.Category category,
                                  String tagName) throws IllegalArgumentException {
    DragonVariantTag tag = DragonVariantTag.getTagFromName(tagName);
    dragonVariants.addTagAndValue(category, tag, "");
  }

  private final IResourceManager iResourceManager;
  private final ResourceLocation configFileLocation;
  private DragonVariantsException.DragonVariantsErrors dragonVariantsErrors;
  private String currentBreed;
}
