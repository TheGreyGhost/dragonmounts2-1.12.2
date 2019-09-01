/*
 ** 2011 December 10
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.client.render.dragon;

import com.TheRPGAdventurer.ROTD.client.model.dragon.DragonModel;
import com.TheRPGAdventurer.ROTD.client.model.dragon.DragonModelMode;
import com.TheRPGAdventurer.ROTD.client.render.dragon.breeds.DefaultDragonBreedRenderer;
import com.TheRPGAdventurer.ROTD.common.blocks.BlockDragonBreedEgg;
import com.TheRPGAdventurer.ROTD.common.entity.EntityDragonEgg;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.EnumDragonBreed;
import com.TheRPGAdventurer.ROTD.common.entity.helper.DragonLifeStageHelper;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonPhysicalModel;
import com.TheRPGAdventurer.ROTD.util.debugging.CentrepointCrosshairRenderer;
import com.TheRPGAdventurer.ROTD.util.debugging.DebugSettings;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBanner;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

/**
 * Generic renderer for all dragons.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class DragonHatchableEggRenderer extends Render<EntityDragonEgg> {

  public DragonHatchableEggRenderer(RenderManager renderManager) {
    super(renderManager);
  }

  @Override
  public void doRender(EntityDragonEgg dragonEgg, double x, double y, double z, float yaw, float partialTicks) {
    // apply egg wiggle
    float tickX = dragonEgg.getEggWiggleX();
    float tickZ = dragonEgg.getEggWiggleZ();

    float rotX = 0;
    float rotZ = 0;

    if (tickX > 0) {
      rotX = (float) Math.sin(tickX - partialTicks) * 8;
    }
    if (tickZ > 0) {
      rotZ = (float) Math.sin(tickZ - partialTicks) * 8;
    }

/*		// Aether Egg Levitate
        float l = (float) (0.1 * Math.cos(dragon.ticksExisted / (Math.PI * 6.89)) + 0.307);
        boolean lev = false;
        if (dragon.getBreedType() == EnumDragonBreed.AETHER) lev = true;
*/

    // prepare GL states
    GlStateManager.pushMatrix();
    GlStateManager.translate(x, y /*+ (lev ? l : 0)*/, z);
    GlStateManager.rotate(rotX, 1, 0, 0);
    GlStateManager.rotate(rotZ, 0, 0, 1);
    GlStateManager.disableLighting();

    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

    // prepare egg rendering
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder vb = tessellator.getBuffer();
    vb.begin(GL_QUADS, DefaultVertexFormats.BLOCK);

    Block block = BlockDragonBreedEgg.DRAGON_BREED_EGG;
    IBlockState iblockstate = block.getDefaultState().withProperty(BlockDragonBreedEgg.BREED, EnumDragonBreed.FIRE);  //todo change later
    BlockPos blockpos = dragonEgg.getPosition();

    double tx = -blockpos.getX() - 0.5;
    double ty = -blockpos.getY();
    double tz = -blockpos.getZ() - 0.5;
    vb.setTranslation(tx, ty, tz);

    BlockRendererDispatcher brd = Minecraft.getMinecraft().getBlockRendererDispatcher();
    IBakedModel bakedModel = brd.getModelForState(iblockstate);

    // render egg
    brd.getBlockModelRenderer().renderModel(dragonEgg.world, bakedModel, iblockstate, blockpos, vb, false);
    vb.setTranslation(0, 0, 0);

    tessellator.draw();

    // restore GL state
    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
  }

  @Override
  protected ResourceLocation getEntityTexture(EntityDragonEgg entityDragonEgg) {
    return eggTexture;
  }

  private final Map<EnumDragonBreed, DefaultDragonBreedRenderer> breedRenderers = new EnumMap<>(EnumDragonBreed.class);
  private static final ResourceLocation eggTexture = new ResourceLocation("minecraftbyexample:textures/entities/dragon/fire/egg.png");
}

