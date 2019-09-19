package com.TheRPGAdventurer.ROTD.util.debugging;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.gui.DragonMountsConfig;
import com.TheRPGAdventurer.ROTD.common.entity.helper.DragonLifeStageHelper;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantsException;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantsReader;
import com.sun.org.apache.xerces.internal.util.DraconianErrorHandler;
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

  static DragonVariantTag TESTTAG1;
  static DragonVariantTag TESTTAG2;
  static DragonVariantTag TESTTAG3;

  public static void testDragonVariantsReader() {

    TESTTAG1 = DragonVariantTag.addTag("testtag1", "one", "testtag1").categories(DragonVariants.Category.EGG).values("one", "two", "three");
    TESTTAG2 = DragonVariantTag.addTag("testtag2flag", "testtag2 flag").categories(DragonVariants.Category.EGG);
    TESTTAG3 = DragonVariantTag.addTag("testtag3", 2.0, "testtag3 number").categories(DragonVariants.Category.EGG);
    DragonVariants.addVariantTagValidator(new TestValidator());

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

  public static class TestValidator implements DragonVariants.VariantTagValidator {
    @Override
    public void validateVariantTags(DragonVariants dragonVariants) throws IllegalArgumentException {
      DragonVariantsException.DragonVariantsErrors errors = new DragonVariantsException.DragonVariantsErrors();
      DragonVariants.DragonVariantsCategoryShortcut dvc = dragonVariants.new DragonVariantsCategoryShortcut(DragonVariants.Category.EGG);
      dvc.checkForConflict(errors, TESTTAG2, false, true, TESTTAG3);  // 2 isn't defined and 3 is .
      dvc.checkForConflict(errors, TESTTAG1, "two", true, TESTTAG3);  // 1 is "two" and 3 is defined
      dvc.checkForConflict(errors, TESTTAG2, true, true, TESTTAG1);  // 2 is defined and 1 is also defined
      dvc.checkForConflict(errors, TESTTAG1, "three", false, TESTTAG3);  // 1 is "one" and 3 isn't defined
      if (errors.hasErrors()) {
        throw new DragonVariantsException(errors);
      }
    }

    @Override
    public void initaliseResources(DragonVariants dragonVariants) throws IllegalArgumentException {

    }
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
            Minecraft.getMinecraft().getResourceManager(), "testdata/testdlsv2");
    Map<String, DragonVariants> allVariants = dragonVariantsReader.readAllVariants();
    DragonLifeStageHelper.testClass(allVariants.get("testdlsv7"), 0);
    DragonLifeStageHelper.testClass(allVariants.get("testdlsv7"), 1);
    //    DragonLifeStageHelper.testClass(allVariants.get("fire"), 2);    // prints the default curves to console
  }
}
