package com.TheRPGAdventurer.ROTD.util.debugging;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.gui.DragonMountsConfig;
import com.TheRPGAdventurer.ROTD.common.entity.helper.DragonLifeStageHelper;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantsReader;
import net.minecraft.client.Minecraft;

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
public class StartupDebugClientOnly {
  public static void preInitClientOnly() {
    if (!DragonMounts.instance.getConfig().isDebug()) return;
//    testDragonVariantsReader();
//    testDragonLifeStageHelperTags();
  }

  public static void initClientOnly() {
    if (!DragonMounts.instance.getConfig().isDebug()) return;
  }

  public static void postInitClientOnly() {
    if (!DragonMounts.instance.getConfig().isDebug()) return;
  }

  public static void testDragonVariantsReader() {
    final String TEST_FOLDER = "testdata/testdvr1";

    DragonMounts.logger.info("DragonVariantsReader Test1" + TEST_FOLDER);
    DragonVariantsReader dragonVariantsReader = new DragonVariantsReader(
            Minecraft.getMinecraft().getResourceManager(), TEST_FOLDER);
    Map<String, DragonVariants> allVariants = dragonVariantsReader.readAllVariants();
    int i = 1; // put breakpoint here and inspect allVariants; compare to the json file, which contain comments
    for (DragonVariants dragonVariants : allVariants.values()) {
      String json = DragonVariantsReader.outputAsJSON(dragonVariants, true);
      System.out.print(json);
    }

    String json = DragonVariantsReader.outputAllTagsAsJSON(true);
    System.out.print(json);
    json = DragonVariantsReader.outputAllTagsAsJSON(false);
    System.out.print(json);
  }

  public static void testDragonLifeStageHelperTags() {
    DragonLifeStageHelper.registerConfigurationTags();
//    String testfiles[] = {"testdata/testdlsv1.json", "testdata/testdlsv2.json", "testdata/testdlsv3.json", "testdata/testdlsv4.json",
//            "testdata/testdlsv5.json", "testdata/testdlsv6.json"};
    final String TEST_FOLDER = "testdata/testdlsv1";

//    for (String filename : testfiles) {
    {
      DragonMounts.logger.info("testDragonLifeStageHelperTags Test1");
      DragonVariantsReader dragonVariantsReader = new DragonVariantsReader(
              Minecraft.getMinecraft().getResourceManager(), TEST_FOLDER);
      Map<String, DragonVariants> allVariants = dragonVariantsReader.readAllVariants();
      int i = 1; // put breakpoint here and inspect allVariants; compare to the json files, which contain comments
      for (DragonVariants dragonVariants : allVariants.values()) {
        String json = DragonVariantsReader.outputAsJSON(dragonVariants, true);
        System.out.print(json);
      }
    }

    DragonVariantsReader dragonVariantsReader = new DragonVariantsReader(
            Minecraft.getMinecraft().getResourceManager(),  "testdata/testdlsv2");
    Map<String, DragonVariants> allVariants = dragonVariantsReader.readAllVariants();
    DragonLifeStageHelper.testClass(allVariants.get("testdlsv7"), 0);
    DragonLifeStageHelper.testClass(allVariants.get("testdlsv7"), 1);
//    DragonLifeStageHelper.testClass(allVariants.get("fire"), 2);    // prints the default curves to console
  }


}
