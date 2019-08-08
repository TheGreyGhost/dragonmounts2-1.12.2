package com.TheRPGAdventurer.ROTD.util.debugging;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.gui.DragonMountsConfig;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.helper.DragonLifeStageHelper;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantsReader;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import java.util.Map;

/**
 * User: The Grey Ghost
 * Date: 24/12/2014
 * <p>
 * The Startup classes for this example are called during startup, in the following order:
 * preInitCommon
 * preInitClientOnly
 * initCommon
 * initClientOnly
 * postInitCommon
 * postInitClientOnly
 * See MinecraftByExample class for more information
 */
public class StartupDebugCommon {
  public static void preInitCommon() {
    if (!DragonMountsConfig.isDebug()) return;
    testDragonVariantsReader(); //todo comment back out again when testing finished
    testDragonLifeStageHelperTags();
  }

  public static void initCommon() {
    if (!DragonMountsConfig.isDebug()) return;
  }

  public static void postInitCommon() {
    if (!DragonMountsConfig.isDebug()) return;
    MinecraftForge.EVENT_BUS.register(new DebugSpawnInhibitor());
  }

  public static void testDragonVariantsReader() {
    String testfiles[] = {"testdata/test1.json", "testdata/test2.json", "testdata/test3.json", "testdata/test4.json",
            "testdata/test5.json", "testdata/test6.json", "testdata/test7.json", "testdata/test8.json"};

    for (String filename : testfiles) {
      DragonMounts.logger.info("DragonVariantsReader Test:" + filename);
      DragonVariantsReader dragonVariantsReader = new DragonVariantsReader(
              Minecraft.getMinecraft().getResourceManager(), new ResourceLocation("dragonmounts", filename));
      Map<String, DragonVariants> allVariants = dragonVariantsReader.readVariants();
      int i = 1; // put breakpoint here and inspect allVariants; compare to the json file, which contain comments
    }
  }

  public static void testDragonLifeStageHelperTags() {
    DragonLifeStageHelper.registerConfigurationTags();
    String testfiles[] = {"testdata/testdlsv1.json", "testdata/testdlsv2.json", "testdata/testdlsv3.json", "testdata/testdlsv4.json",
            "testdata/testdlsv5.json", "testdata/testdlsv6.json"};

    for (String filename : testfiles) {
      DragonMounts.logger.info("DragonVariants Test:" + filename);
      DragonVariantsReader dragonVariantsReader = new DragonVariantsReader(
              Minecraft.getMinecraft().getResourceManager(), new ResourceLocation("dragonmounts", filename));
      Map<String, DragonVariants> allVariants = dragonVariantsReader.readVariants();
      int i = 1; // put breakpoint here and inspect allVariants; compare to the json file, which contain comments
      try {
        allVariants.get("fire").validateCollection();  // parse errors should revert to default tags
      } catch (Exception e) {
        DragonMounts.logger.info("Failed test due to exception " + e);
      }
    }

    DragonVariantsReader dragonVariantsReader = new DragonVariantsReader(
            Minecraft.getMinecraft().getResourceManager(), new ResourceLocation("dragonmounts", "testdata/testdlsv7.json"));
    Map<String, DragonVariants> allVariants = dragonVariantsReader.readVariants();
    DragonLifeStageHelper.testClass(allVariants.get("fire"), 0);

  }

}
