package com.TheRPGAdventurer.ROTD.common.items;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

/**
 * Created by TGG on 17/08/2016.
 */
public class IItemDragonOrbColour implements IItemColor {

  /**
   * cycling glow effect for the orb jewel by varying the layer brightness
   * Returns the colour for rendering, based on
   * 1) the itemstack
   * 2) the "tintindex" (layer in the item model json)
   * For example:
   * bottle_drinkable.json contains
   * "layer0": "items/potion_overlay",
   * "layer1": "items/potion_bottle_drinkable"
   * layer0 = layerNumber 0 = for the bottle outline, whose colour doesn't change
   * layer1 = layerNumber 1 = for the bottle contents, whose colour changes depending on the type of potion
   *
   * @param stack
   * @param layerNumber
   * @return an RGB colour (to be multiplied by the texture colours)
   */
  @Override
  public int colorMultiplier(ItemStack stack, int layerNumber) {
    switch (layerNumber) {
      case 0: {  // claw
        return Color.WHITE.getRGB();
      }
      case 1: {   // orb jewel
        final long GLOW_CYCLE_PERIOD_SECONDS = 4;
        final float MIN_GLOW_BRIGHTNESS = 0.4F;
        final float MAX_GLOW_BRIGHTNESS = 1.0F;
        final long NANO_SEC_PER_SEC = 1000L * 1000L * 1000L;

        long cyclePosition = System.nanoTime() % (GLOW_CYCLE_PERIOD_SECONDS * NANO_SEC_PER_SEC);
        double cyclePosRadians = 2 * Math.PI * cyclePosition / (double) (GLOW_CYCLE_PERIOD_SECONDS * NANO_SEC_PER_SEC);
        final float BRIGHTNESS_MIDPOINT = (MIN_GLOW_BRIGHTNESS + MAX_GLOW_BRIGHTNESS) / 2.0F;
        final float BRIGHTNESS_AMPLITUDE = (MAX_GLOW_BRIGHTNESS - BRIGHTNESS_MIDPOINT);
        float brightness = BRIGHTNESS_MIDPOINT + BRIGHTNESS_AMPLITUDE * MathX.sin((float) cyclePosRadians);
        int brightnessInt = MathHelper.clamp((int) (255 * brightness), 0, 255);
        Color orbBrightness = new Color(brightnessInt, brightnessInt, brightnessInt);
        return orbBrightness.getRGB();
      }
      default: {
        DragonMounts.loggerLimit.error_once("Invalid layer number:" + layerNumber);
        return Color.WHITE.getRGB();
      }
    }
  }

  static boolean errorPrinted = false;  // for debugging purposes


}
