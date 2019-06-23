package com.TheRPGAdventurer.ROTD.client.render;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Created by TGG on 19/06/2015.
 * Used to stitch the textures used by the breath weapon EntityFX into the blocks+items texture sheet, so that the
 *   EntityFX renderer can use them.
 *   I should probably have used some sort of registry, or asked each BreathFX for its RL, but this was easier...
 */
public class TextureStitcherBreathFX
{
  @SubscribeEvent
  public void stitcherEventPre(TextureStitchEvent.Pre event) {
    ResourceLocation flameRL = new ResourceLocation("dragonmounts:entities/breathweapon/breath_fire");
    event.getMap().registerSprite(flameRL);
    ResourceLocation iceRL = new ResourceLocation("dragonmounts:entities/breathweapon/breath_ice");
    event.getMap().registerSprite(iceRL);
    ResourceLocation waterRL = new ResourceLocation("dragonmounts:entities/breathweapon/breath_water");
    event.getMap().registerSprite(waterRL);
    ResourceLocation airRL = new ResourceLocation("dragonmounts:entities/breathweapon/breath_air");
    event.getMap().registerSprite(airRL);
    ResourceLocation forestGasCloudRL = new ResourceLocation("dragonmounts:entities/breathweapon/breath_forest");
    event.getMap().registerSprite(forestGasCloudRL);
  }
}
