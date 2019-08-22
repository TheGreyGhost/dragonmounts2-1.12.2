package com.TheRPGAdventurer.ROTD.client.model;

import com.TheRPGAdventurer.ROTD.common.entity.helper.util.Pair;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantTag;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariantsException;
import com.TheRPGAdventurer.ROTD.util.math.Interpolation;

/**
 * Created by TGG on 22/08/2019.
 * Holds the various models needed for rendering DragonHatchableEggs
 * Usage:
 * 1) Create the EggModels()
 * 2) During initial setup, call eggModels.registerConfigurationTags() to register all the tags that are
 *    used to configure this, and to set up the variant tag validation
 */

public class EggModels {

  /**
   * Initialise all the configuration tags used by this helper
   */
  public void registerConfigurationTags()
  {
    // dummy method for the tags themselves -the initialisation is all done in static initialisers
    DragonVariants.addVariantTagValidator(new EggModelsValidator());
  }

  /**
   * Validates the following aspects of the tags:
   * 1) life stage ages are in the correct order
   * 2) growth rate curve matches the relative size of sizehatchling and sizeadult and doesn't produce a dragon smaller
   *    than minimum_size or bigger than maximum_size
   * If any errors are found, revert to the defaults and throw an error
   */
  public class EggModelsValidator implements DragonVariants.VariantTagValidator {
    @Override
    public void validateVariantTags(DragonVariants dragonVariants) throws IllegalArgumentException {
      DragonVariantsException.DragonVariantsErrors dragonVariantsErrors = new DragonVariantsException.DragonVariantsErrors();

      double [] lifeStageAgesConfig = getLifeStageAges(dragonVariants);
      if (!Interpolation.isValidInterpolationArray(lifeStageAgesConfig)) {
        DragonVariantTag[] tagsToRemove = {AGE_INFANT, AGE_CHILD, AGE_EARLY_TEEN, AGE_LATE_TEEN, AGE_ADULT};
        dragonVariants.removeTags(DragonVariants.Category.LIFE_STAGE, tagsToRemove);
        lifeStageAgesConfig = getLifeStageAges(dragonVariants);  // read defaults
        dragonVariantsErrors.addError(DragonVariants.Category.LIFE_STAGE.getTextName()
                + " age values invalid (each age must be bigger than the previous age)");
      }

      double [] growthRatePointsConfig = getGrowthRatePoints(dragonVariants);
      try {
        Pair<double[], double []> curves = calculatePhysicalSizeCurve(dragonVariants, lifeStageAgesConfig, growthRatePointsConfig);
      } catch (DragonVariantsException dve) {
        DragonVariantTag [] tagsToRemove = {GROWTHRATE_HATCHLING, GROWTHRATE_INFANT, GROWTHRATE_CHILD,
                GROWTHRATE_EARLY_TEEN, GROWTHRATE_LATE_TEEN, SIZE_HATCHLING, SIZE_ADULT};
        dragonVariants.removeTags(DragonVariants.Category.EGG, tagsToRemove);
        dragonVariantsErrors.addError(dve);
      }

      if (dragonVariantsErrors.hasErrors()) {
        throw new DragonVariantsException(dragonVariantsErrors);
      }
    }
  }


  private static final DragonVariantTag EGG_ITEM_MODEL_JSON = DragonVariantTag.addTag("eggmodeljson", );
  private static final DragonVariantTag EGG_ITEM_MODEL_OBJ = DragonVariantTag.addTag("eggmodelobj", );
  private static final DragonVariantTag EGG_ITEM_MODEL_TEXTURE = DragonVariantTag.addTag("eggmodeltexture", );

}
