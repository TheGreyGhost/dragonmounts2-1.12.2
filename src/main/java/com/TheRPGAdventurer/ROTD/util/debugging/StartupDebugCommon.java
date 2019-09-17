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
    if (!DragonMounts.instance.getConfig().isDebug()) return;
    testDragonVariantsReader(); //todo comment back out again when testing finished
//    testDragonLifeStageHelperTags();
  }

  public static void initCommon() {
    if (!DragonMounts.instance.getConfig().isDebug()) return;
  }

  public static void postInitCommon() {
    if (!DragonMounts.instance.getConfig().isDebug()) return;
    MinecraftForge.EVENT_BUS.register(new DebugSpawnInhibitor());
  }

  public static void testDragonVariantsReader() {
//    String testfiles[] = {"testdata/test1.json", "testdata/test2.json", "testdata/test3.json", "testdata/test4.json",
//            "testdata/test5.json", "testdata/test6.json", "testdata/test7.json", "testdata/test8.json"};
    final String TEST_FOLDER = "testdata/testdvr1";

//    for (String filename : testfiles) {
      DragonMounts.logger.info("DragonVariantsReader Test1" + TEST_FOLDER);
      DragonVariantsReader dragonVariantsReader = new DragonVariantsReader(
              Minecraft.getMinecraft().getResourceManager(), TEST_FOLDER);
      Map<String, DragonVariants> allVariants = dragonVariantsReader.readAllVariants();
      int i = 1; // put breakpoint here and inspect allVariants; compare to the json file, which contain comments
      for (DragonVariants dragonVariants : allVariants.values()) {
        String json = DragonVariantsReader.outputAsJSON(dragonVariants, true);
        System.out.print(json);
      }
//    }
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
      try {
        allVariants.get("fire").validateCollection();  // parse errors should revert to default tags
      } catch (Exception e) {
        DragonMounts.logger.info("Failed test due to exception " + e);
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
