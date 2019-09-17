package com.TheRPGAdventurer.ROTD.common.entity.physicalmodel;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.util.DMUtils;
import com.TheRPGAdventurer.ROTD.util.Minify;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import com.google.gson.*;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by TGG on 14/07/2019.
 * This class reads the dragon variants json configuration files
 * Usage:
 * 1) Create with DragonVariantsReader, giving it the Minecraft resource manager and the assets folder which contains the
 *     config files folder - eg if the various .json files are in assets.dragonmounts.variants.configfiles then the
 *     configFileFolder is variants/configfiles
 * 2) To read all variants (to be done in common preinit():
 *   readAllVariants()
 * 3) To read a single variants file:
 *    readOneVariantFile
 *
 * The VariantTagValidator.validateVariantTags is called for each DragonVariants
 * The class does not register the breeds with DragonBreedsRegistry, this must be done by the caller
 * Likewise, the caller should call DragonVariants.initialiseResourcesForCollection() to ensure that VariantTagValidator.initaliseResources()
 *   is called for each tag.
 */
public class DragonVariantsReader {

  public DragonVariantsReader(IResourceManager manager, String configFilesFolder) {
    this.iResourceManager = manager;
    this.configFilesFolder = configFilesFolder;
    final int MAX_ERROR_LENGTH = 1000;
    dragonVariantsErrors = new DragonVariantsException.DragonVariantsErrors(MAX_ERROR_LENGTH);
  }

  /**
   * Read all variant information for all breeds from the dragonvariants folder

   * @return Map of breed names (from json) to DragonVariants
   */
  public Map<String, DragonVariants> readAllVariants() {
    dragonVariantsErrors.clear();
    // this ensures that the validators are also called for the default breed; just as a sanity check
    try {
      DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getDefaultBreed().getDragonVariants().validateCollection();
    } catch (DragonVariantsException dve) {
      dragonVariantsErrors.addError(dve);
      DragonMounts.logger.error("One or more errors occurred validating the default breed data- indicates an internal program error.  Msg:\n" + dragonVariantsErrors.toString());
    }
    dragonVariantsErrors.clear();

    Map<String, DragonVariants> allBreedVariants = new HashMap<>();
    try {
      List<String> allFilenames = DMUtils.listAssetsFolderContents(configFilesFolder);
      for (String filename : allFilenames) {
        ResourceLocation resourceLocation = new ResourceLocation("dragonmounts", configFilesFolder + "/" + filename);
        DragonVariants newVariant = readOneVariantFile(resourceLocation);
        if (newVariant != null) {
          allBreedVariants.put(newVariant.getBreedInternalName(), newVariant);
        }
      }
    } catch (IOException e) {  // didn't find the file in this resourcedomain
      ResourceLocation resourceLocation = new ResourceLocation("dragonmounts", configFilesFolder);
      DragonMounts.logger.warn("A problem occurred trying to read the list of config files from folder " + resourceLocation + ":\n" + e.getMessage());
    }

    return allBreedVariants;  //return empty if we had trouble
  }

  /**
   * Reads the given config file into a DragonVariants
   * @param configFileLocation
   * @return the DragonVariants or null in case of failure
   */
  public DragonVariants readOneVariantFile(ResourceLocation configFileLocation) {
    try {
      IResource iResource = iResourceManager.getResource(configFileLocation);
      DragonVariants dragonVariant = readOneVariantFile(iResource);
      return dragonVariant;
    } catch (IOException e) {  // didn't find the file in this resourcedomain
      DragonMounts.logger.warn("Couldn't find config file " + configFileLocation);
    } catch (RuntimeException runtimeexception) {
      DragonMounts.logger.error("A problem occurred reading the config file " + configFileLocation + "; " + runtimeexception.getMessage());
    }

    return null;
  }

  /**
   * Reads the given config resource into a DragonVariants
   * @param iResource
   * @return the DragonVariants or null in case of failure
   */
  private DragonVariants readOneVariantFile(IResource iResource) {
    dragonVariantsErrors.clear();

    try {
      InputStream inputStream = iResource.getInputStream();
      try {
        String inputString = CharStreams.toString(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String stripped = Minify.minifyCommentsOnly(inputString);
        DragonVariants dragonVariants = deserialiseEntireFile(new StringReader(stripped));
        if (dragonVariantsErrors.hasErrors()) {
          DragonMounts.logger.warn("One or more errors occurred parsing config file " + iResource.getResourceLocation() + ":\n"
                                   + dragonVariantsErrors.toString());
        }
        return dragonVariants;
      } catch (RuntimeException runtimeexception) {
        DragonMounts.logger.warn("Invalid config file " + iResource.getResourceLocation() + "; " + runtimeexception.getMessage());
      } finally {
        IOUtils.closeQuietly(inputStream);
      }
    } catch (IOException e) {  // didn't find the file in this resourcedomain
      DragonMounts.logger.warn("Couldn't find config file " + iResource.getResourceLocation() + ":" + e.getMessage());
    }

    return null;
  }

  /**
   * Convert the given DragonVariants into JSON
   * @param dragonVariants
   * @return the JSON output
   */
  public static String outputAsJSON(DragonVariants dragonVariants, boolean includeComments) {
    Map<DragonVariants.Category, ImmutableMap<DragonVariantTag, Object>> allTags = new HashMap<>();
    for (DragonVariants.Category category : DragonVariants.Category.values()) {
      allTags.put(category, dragonVariants.getAllAppliedTagsForCategory(category));
    }

    StringBuilder json = new StringBuilder();
    json.append("{\"");
    json.append(BREED_INTERNAL_NAME_JSON);
    json.append("\": \"");
      json.append(dragonVariants.getBreedInternalName());
      json.append("\",\n");
    boolean isFirst = true;
    for (Map.Entry<DragonVariants.Category, ImmutableMap<DragonVariantTag, Object>> entry : allTags.entrySet() ) {
      if (!entry.getValue().isEmpty()) {
        if (!isFirst) {
          json.append(",\n");
        }
        isFirst = false;
        outputCategoryAsJSON(json, entry.getKey(), entry.getValue(), includeComments);
      }
    }

    json.append("\n}");
    return json.toString();
  }

  private static void outputCategoryAsJSON(StringBuilder json,
                                           DragonVariants.Category category,  ImmutableMap<DragonVariantTag, Object> tags, boolean includeComments) {
    json.append("\"");
      json.append(category.getTextName());
    json.append("\": {\n");
    if (includeComments) {
      json.append("\\* ");
      json.append(category.getComment());
      json.append(" *\\");
    }
    boolean isFirst = true;
    for (ImmutableMap.Entry<DragonVariantTag, Object> entry : tags.entrySet()) {
      if (!(entry.getValue() instanceof Boolean)) { // save the flags for later
        if (!isFirst) {
          json.append(",\n");
        }
        isFirst = false;
        outputSingleTagAsJSON(json, entry.getKey(), entry.getValue(), includeComments);
      }
    }
    boolean atLeastOneFlag = false;
    for (ImmutableMap.Entry<DragonVariantTag, Object> entry : tags.entrySet()) {
      if ((entry.getValue() instanceof Boolean)) { // save the flags for later
        if (!isFirst) {
          json.append(",\n");
        }
        isFirst = false;
        if (!atLeastOneFlag) {
          atLeastOneFlag = true;
          json.append("\"");
          json.append(FLAGS_JSON);
          json.append("\": [\n");
        }
        outputSingleTagAsJSON(json, entry.getKey(), entry.getValue(), includeComments);
      }
    }
    json.append("\n");
    if (atLeastOneFlag) {
      json.append("]\n");
    }
    json.append("}");
  }

  /**
   * Output a DragonVariants JSON which contains all the possible tag options
   * @return the JSON output
   */
  public static String outputAllTagsAsJSON(boolean includeComments) {
    DragonVariants dragonVariants = new DragonVariants("example");
    ImmutableSet<DragonVariantTag> allDefinedTags = DragonVariantTag.getAllDragonVariantTags();
    for (DragonVariantTag tag : allDefinedTags) {
      for (DragonVariants.Category category : tag.getExpectedCategories()) {
        dragonVariants.addTagAndValue(category, tag, tag.getDefaultValue());
      }
    }
    return outputAsJSON(dragonVariants, includeComments);
  }

  /** write the tag suitable for json reader
   * @param json
   */
  public static void outputSingleTagAsJSON(StringBuilder json, DragonVariantTag tag, Object value, boolean includeComments) {
    json.append("\"");
    json.append(tag.getTextname());
    json.append("\"");
    if (value instanceof Boolean) {
    } else if (value instanceof String) {
      json.append(": \"");
      json.append(value);
      json.append("\"");
    } else if (value instanceof Number) {
      json.append(": ");
      json.append(value);
    } else {
      throw new IllegalArgumentException("Unexpected object type");
    }
    if (includeComments) {
      json.append("/* ");
      json.append(tag.getComment());
      json.append(" */");
    }
  }

  /**
   * Deserialise the JSON file for a single breed
   * @param input
   * @return
   * @throws JsonSyntaxException
   */
  private DragonVariants deserialiseEntireFile(Reader input) throws JsonSyntaxException {
    JsonParser parser = new JsonParser();
    JsonElement entireFile = parser.parse(input);
    if (!entireFile.isJsonObject()) throw new JsonSyntaxException("Malformed file");
    JsonObject breedVariantData = entireFile.getAsJsonObject();
    DragonVariants dragonVariants = deserializeAllTagsForOneBreed(breedVariantData);
    if (dragonVariants == null) return null;
    try {
      dragonVariants.validateCollection();
    } catch (IllegalArgumentException iae) {
      dragonVariantsErrors.addError(iae);
    }

    return dragonVariants;
  }

  /**
   * Deserialise all the tags for the specified breed's json object
   *
   * @param jsonObject the object for this breed
   * @return The new DragonVariants, or null if it didn't work
   */
  private DragonVariants deserializeAllTagsForOneBreed(JsonObject jsonObject) {
    if (!jsonObject.isJsonObject()) {
      dragonVariantsErrors.addError("Malformed entry");
      return null;
    }
    JsonElement breedInternalNameJSON = jsonObject.get(BREED_INTERNAL_NAME_JSON);
    if (breedInternalNameJSON == null) {
      dragonVariantsErrors.addError("Didn't find the required field " + BREED_INTERNAL_NAME_JSON);
      return null;
    }
    if (!breedInternalNameJSON.isJsonPrimitive() || !breedInternalNameJSON.getAsJsonPrimitive().isString()) {
      dragonVariantsErrors.addError("The required field " + BREED_INTERNAL_NAME_JSON + " has the wrong type (should be a String).");
      return null;
    }

    DragonVariants dragonVariants = new DragonVariants(breedInternalNameJSON.getAsString());

    for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
      String categoryName = entry.getKey();
      if (!categoryName.equals(BREED_INTERNAL_NAME_JSON)) {
        try {
          DragonVariants.Category category = DragonVariants.Category.getCategoryFromName(categoryName);
          deserializeAllTagsForOneCategory(dragonVariants, category, entry.getValue());
        } catch (IllegalArgumentException iae) {
          dragonVariantsErrors.addError(iae);
        }
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
        } catch (DragonVariantTagNotFoundException dvtnfe) {
          // if we're in a dedicated server, ignore this error
          if (!DragonVariantTagNotFoundException.shouldIgnore()) {
            dragonVariantsErrors.addError(dvtnfe);
          }
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
          dragonVariantsErrors.addError("problem with tag " + category.getTextName() + ":flags - not correctly formatted");
        } else {
          deserialiseFlagTag(dragonVariants, category, element.getAsJsonPrimitive().getAsString());
        }
      } catch (DragonVariantTagNotFoundException dvtnfe) {
        // if we're in a dedicated server, ignore this error
        if (!DragonVariantTagNotFoundException.shouldIgnore()) {
          dragonVariantsErrors.addError(dvtnfe);
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
      throw new IllegalArgumentException("problem with tag " + category.getTextName() + ":" + tagName + "-" + e.getMessage());
    }
  }

  private void deserialiseFlagTag(DragonVariants dragonVariants, DragonVariants.Category category,
                                  String tagName) throws IllegalArgumentException {
    DragonVariantTag tag = DragonVariantTag.getTagFromName(tagName);
    dragonVariants.addTagAndValue(category, tag, "");
  }

  private final IResourceManager iResourceManager;
  private final String configFilesFolder;
  private DragonVariantsException.DragonVariantsErrors dragonVariantsErrors;

  private static final String BREED_INTERNAL_NAME_JSON = "breedinternalname";
  private static final String FLAGS_JSON = "flags";

}
