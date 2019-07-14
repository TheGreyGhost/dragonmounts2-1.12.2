package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.physicalmodel;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Created by TGG on 14/07/2019.
 */
public class DragonVariantsReader {

  public DragonVariantsReader(IResourceManager manager) {
    this.iResourceManager = manager;
  }

  public DragonVariants readVariants() {
    boolean foundAtLeastOne = false;
    for (String s : iResourceManager.getResourceDomains()) {
      try {
        for (IResource iresource : iResourceManager.getAllResources(new ResourceLocation(s, "dragonvariants.json"))) {
          foundAtLeastOne = true;
          InputStream inputStream = iresource.getInputStream();
          try {
            DragonVariants dragonVariants = JsonUtils.fromJson(GSON,
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8), TYPE);
            return dragonVariants;
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
    return new DragonVariants();  // just return the default if we had trouble.
  }

  private final IResourceManager iResourceManager;

  private static final Gson GSON = new GsonBuilder()
          .registerTypeAdapter(DragonVariants.class, new DragonVariantsSerializer())
          .create();

  private static final ParameterizedType TYPE = new ParameterizedType() {
    public Type[] getActualTypeArguments() {
      return new Type[]{String.class, DragonVariants.class};
    }

    public Type getRawType() {
      return Map.class;
    }

    public Type getOwnerType() {
      return null;
    }
  };

  //  for (File file1 : FileUtils.listFiles(this.advancementsDir, new String[] {"json"}, true))
//  {
//    String s = FilenameUtils.removeExtension(this.advancementsDir.toURI().relativize(file1.toURI()).toString());
//    String[] astring = s.split("/", 2);
//
//    if (astring.length == 2)
//    {
//      ResourceLocation resourcelocation = new ResourceLocation(astring[0], astring[1]);
//
//      try
//      {
//        Advancement.Builder advancement$builder = (Advancement.Builder) JsonUtils.gsonDeserialize(GSON, FileUtils.readFileToString(file1, StandardCharsets.UTF_8), Advancement.Builder.class);
//
//        if (advancement$builder == null)
//        {
//          LOGGER.error("Couldn't load custom advancement " + resourcelocation + " from " + file1 + " as it's empty or null");
//        }
//        else
//        {
//          map.put(resourcelocation, advancement$builder);
//        }
//      }
//      catch (IllegalArgumentException | JsonParseException jsonparseexception)
//      {
//        LOGGER.error("Parsing error loading custom advancement " + resourcelocation, (Throwable)jsonparseexception);
//        this.hasErrored = true;
//      }
//      catch (IOException ioexception)
//      {
//        LOGGER.error("Couldn't read custom advancement " + resourcelocation + " from " + file1, (Throwable)ioexception);
//        this.hasErrored = true;
//      }
//    }
//
//}

}
