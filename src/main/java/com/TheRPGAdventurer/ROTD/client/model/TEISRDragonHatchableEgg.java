package com.TheRPGAdventurer.ROTD.client.model;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.model.wavefrontparser.WavefrontObject;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.*;

/**
 * Renders the model of the DragonHatchable Egg - as an item
  */
public class TEISRDragonHatchableEgg extends TileEntityItemStackRenderer
{
  @Override
  public void renderByItem(ItemStack itemStackIn) {
    DragonBreedNew breed = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getDefaultBreed();
    try {
      breed = DragonBreedNew.DragonBreedsRegistry.getDefaultRegistry().getBreed(itemStackIn.getTagCompound());
    } catch (IllegalArgumentException iae) {
      DragonMounts.loggerLimit.error_once("Unknown breed in TEISRDragonHatchableEgg:" + iae.getMessage());
    }

    // vanilla item are expected to extend from [0,0,0] to [1,1,1]
    //  the item renderer called has therefore applied
    //    GlStateManager.translate(-0.5F, -0.5F, -0.5F)
    //  to centre it around [0,0,0]
    // the eggmodel (similar to entities) extends from [-0.5, 0, -0.5] to [0.5, 1, 0.5] hence the appropriate translation
    //  would have been
    //    GlStateManager.translate(0F, -0.5F, 0F)
    //  so we need to correct to that by translating
    //    GlStateManager.translate(0.5F, 0, 0.5F)
    final double ITEM_ORIGIN_CORRECTION_X = 0.5;
    final double ITEM_ORIGIN_CORRECTION_Y = 0.0;
    final double ITEM_ORIGIN_CORRECTION_Z = 0.5;

    try {
      // save the transformation matrix and the rendering attributes, so that we can restore them after rendering.  This
      //   prevents us disrupting any vanilla TESR that render after ours.
      //  using try..finally is not essential but helps make it more robust in case of exceptions
      // For further information on rendering using the Tessellator, see http://greyminecraftcoder.blogspot.co.at/2014/12/the-tessellator-and-worldrenderer-18.html
      GL11.glPushMatrix();
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT);

      GlStateManager.translate(ITEM_ORIGIN_CORRECTION_X, ITEM_ORIGIN_CORRECTION_Y, ITEM_ORIGIN_CORRECTION_Z);

      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferBuilder = tessellator.getBuffer();

      ResourceLocation resourceLocation = EggModels.getInstance().getTexture(breed);
      if (resourceLocation != null) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(resourceLocation);
      }

      bufferBuilder.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_NORMAL);
      WavefrontObject wavefrontObject = EggModels.getInstance().getModel(breed, EggModels.EggModelState.INCUBATING);
      wavefrontObject.tessellateAll(bufferBuilder);
      tessellator.draw();
    } finally {
      GL11.glPopAttrib();
      GL11.glPopMatrix();
    }
  }

}
