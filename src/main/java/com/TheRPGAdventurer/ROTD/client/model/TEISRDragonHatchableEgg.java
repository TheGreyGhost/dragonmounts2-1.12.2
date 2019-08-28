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
    // the gem changes its appearance and animation as the player approaches.
    // When the player is a long distance away, the gem is dark, resting in the hopper, and does not rotate.
    // As the player approaches closer than 16 blocks, the gem first starts to glow brighter and to spin anti-clockwise
    // When the player gets closer than 4 blocks, the gem is at maximum speed and brightness, and starts to levitate above the pedestal
    // Once the player gets closer than 2 blocks, the gem reaches maximum height.

    // the appearance and animation of the gem is hence made up of several parts:
    // 1) the colour of the gem, which is contained in the tileEntity
    // 2) the brightness of the gem, which depends on player distance
    // 3) the distance that the gem rises above the pedestal, which depends on player distance
    // 4) the speed at which the gem is spinning, which depends on player distance.

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

    double relativeX = 0;
    double relativeY = 0;
    double relativeZ = 0;

//    final double pedestalCentreOffsetX = 0.5;
//    final double pedestalCentreOffsetY = 0.8;
//    final double pedestalCentreOffsetZ = 0.5;
//    Vec3d playerEye = new Vec3d(0.0, 0.0, 0.0);
//    Vec3d pedestalCentre = new Vec3d(relativeX + pedestalCentreOffsetX, relativeY + pedestalCentreOffsetY, relativeZ + pedestalCentreOffsetZ);
//    double playerDistance = playerEye.distanceTo(pedestalCentre);
//
//    final double DISTANCE_FOR_MIN_SPIN = 8.0;
//    final double DISTANCE_FOR_MAX_SPIN = 4.0;
//    final double DISTANCE_FOR_MIN_GLOW = 16.0;
//    final double DISTANCE_FOR_MAX_GLOW = 4.0;
//    final double DISTANCE_FOR_MIN_LEVITATE = 4.0;
//    final double DISTANCE_FOR_MAX_LEVITATE = 2.0;
//
//    final double MIN_LEVITATE_HEIGHT = 0.0;
//    final double MAX_LEVITATE_HEIGHT = 0.5;
//    double gemCentreOffsetX = pedestalCentreOffsetX;
//    double gemCentreOffsetY = pedestalCentreOffsetY + MathX.interpolate(playerDistance, DISTANCE_FOR_MIN_LEVITATE, DISTANCE_FOR_MAX_LEVITATE,
//            MIN_LEVITATE_HEIGHT, MAX_LEVITATE_HEIGHT);
//    double gemCentreOffsetZ = pedestalCentreOffsetZ;

//    final double MIN_GLOW = 0.0;
//    final double MAX_GLOW = 1.0;
////    double glowMultiplier = MathX.interpolate(playerDistance, DISTANCE_FOR_MIN_GLOW, DISTANCE_FOR_MAX_GLOW,
////            MIN_GLOW, MAX_GLOW);
//    double glowMultiplier = (MAX_GLOW +_MI)
//
//    GlStateManager.enableDepth();
//    GlStateManager.depthFunc(515);
//    GlStateManager.depthMask(true);

//    final double MIN_REV_PER_SEC = 0.0;
//    final double MAX_REV_PER_SEC = 0.5;
//    double revsPerSecond = MathX.interpolate(playerDistance, DISTANCE_FOR_MIN_SPIN, DISTANCE_FOR_MAX_SPIN,
//            MIN_REV_PER_SEC, MAX_REV_PER_SEC);
//    double angularPositionInDegrees = 0.0;  //tileEntityDragonHatchableEgg.getNextAngularPosition(revsPerSecond);

    try {
      // save the transformation matrix and the rendering attributes, so that we can restore them after rendering.  This
      //   prevents us disrupting any vanilla TESR that render after ours.
      //  using try..finally is not essential but helps make it more robust in case of exceptions
      // For further information on rendering using the Tessellator, see http://greyminecraftcoder.blogspot.co.at/2014/12/the-tessellator-and-worldrenderer-18.html
      GL11.glPushMatrix();
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT);

      // First we need to set up the translation so that we render our gem with the bottom point at 0,0,0
      // when the renderTileEntityAt method is called, the tessellator is set up so that drawing a dot at [0,0,0] corresponds to the player's eyes
      // This means that, in order to draw a dot at the TileEntity [x,y,z], we need to translate the reference frame by the difference between the
      // two points, i.e. by the [relativeX, relativeY, relativeZ] passed to the method.  If you then draw a cube from [0,0,0] to [1,1,1], it will
      // render exactly over the top of the TileEntity's block.
      // In this example, the zero point of our model needs to be in the middle of the block, not at the [x,y,z] of the block, so we need to
      // add an extra offset as well, i.e. [gemCentreOffsetX, gemCentreOffsetY, gemCentreOffsetZ]
      GlStateManager.translate(ITEM_ORIGIN_CORRECTION_X, ITEM_ORIGIN_CORRECTION_Y, ITEM_ORIGIN_CORRECTION_Z);
      GlStateManager.translate(relativeX, relativeY, relativeZ);

//      GlStateManager.rotate((float)angularPositionInDegrees, 0, 1, 0);   // rotate around the vertical axis

//      final double GEM_HEIGHT = 0.5;        // desired render height of the gem
//      final double MODEL_HEIGHT = 1.0;      // actual height of the gem in the vertexTable
//      final double SCALE_FACTOR = GEM_HEIGHT / MODEL_HEIGHT;
//      GlStateManager.scale(SCALE_FACTOR, SCALE_FACTOR, SCALE_FACTOR);

      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferBuilder = tessellator.getBuffer();
      //this.bindTexture(eggTexture);         // texture for the gem appearance

      ResourceLocation resourceLocation = EggModels.getInstance().getTexture(breed);
      if (resourceLocation != null) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(resourceLocation);
      }

//      // set the key rendering flags appropriately...
//      GL11.glDisable(GL11.GL_LIGHTING);     // turn off "item" lighting (face brightness depends on which direction it is facing)
//      GL11.glDisable(GL11.GL_BLEND);        // turn off "alpha" transparency blending
//      GL11.glDepthMask(true);               // gem is hidden behind other objects

      // set the rendering colour as the gem base colour
      Color fullBrightnessColor = Color.WHITE;//  tileEntityDragonHatchableEgg.getGemColour();
      float red = 0, green = 0, blue = 0;
      if (fullBrightnessColor != Color.BLACK) {
        red = (float) (fullBrightnessColor.getRed() / 255.0);
        green = (float) (fullBrightnessColor.getGreen() / 255.0);
        blue = (float) (fullBrightnessColor.getBlue() / 255.0);
      }
      GlStateManager.color(red, green, blue);     // change the rendering colour

//        // change the "multitexturing" lighting value (default value is the brightness of the tile entity's block)
//        // - this will make the gem "glow" brighter than the surroundings if it is dark.
//      final int SKY_LIGHT_VALUE = (int)(15 * glowMultiplier);
//      final int BLOCK_LIGHT_VALUE = 0;
//      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, SKY_LIGHT_VALUE * 16.0F, BLOCK_LIGHT_VALUE * 16.0F);

      bufferBuilder.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_NORMAL);
      WavefrontObject wavefrontObject = EggModels.getInstance().getModel(breed, EggModels.EggModelState.INCUBATING);
      wavefrontObject.tessellateAll(bufferBuilder);
      tessellator.draw();

//      ModelResourceLocation mrl = new ModelResourceLocation("dragonmounts:dragon_hatchable_egg.obj", "inventory");  // todo refactor
//
//      IBakedModel ibm = Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getModelManager().getModel(mrl);
//      addGemVertices(bufferBuilder);
//      tessellator.draw();

    } finally {
      GL11.glPopAttrib();
      GL11.glPopMatrix();
    }
  }

//  public ResourceLocation getEggTexture() {return eggTexture;}
//
//   private static final ResourceLocation eggTexture = new ResourceLocation("minecraftbyexample:textures/entities/dragon/fire/egg.png");

  /**
   * Copied from RenderItem, removing the tint index stuff
   * @param renderer
   * @param quads
   * @param colour
   */
  private void renderQuads(BufferBuilder renderer, java.util.List<BakedQuad> quads, int colour)
  {
    int i = 0;
    for (int j = quads.size(); i < j; ++i) {
      BakedQuad bakedquad = quads.get(i);
      net.minecraftforge.client.model.pipeline.LightUtil.renderQuadColor(renderer, bakedquad, colour);
    }
  }


}
