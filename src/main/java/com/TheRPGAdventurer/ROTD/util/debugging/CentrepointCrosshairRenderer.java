package com.TheRPGAdventurer.ROTD.util.debugging;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.DragonMountsConfig;
import com.TheRPGAdventurer.ROTD.client.userinput.DragonOrbControl;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.BreathWeaponTarget;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * For debugging purposes:
 * renders a 3D crosshair at a given location (currently used for throat location and BreathFX rendering)
 * Usage:
 * (1) Register the handler during PostInit client only
 * (2) Each frame, add all desired centrepoints with addCentrepointToRender()
 * Created by TGG on 29/06/2019
 */
public class CentrepointCrosshairRenderer
{
  /**
   * render this first (before any other DrawBlockHighlightEvents which might cancel it)
   * @param event the event
   */
  @SubscribeEvent(priority = EventPriority.HIGHEST)
  public void centrepointCrosshairRenderer(DrawBlockHighlightEvent event)
  {
    for (Vec3d cp : pointsToRenderNextFrame) {
      draw3DCrosshair(cp, event.getPlayer(), event.getPartialTicks());
    }
    pointsToRenderNextFrame.clear();
  }

  /** draw an oscillating outlined bounding box around the indicated aabb
   *
   * @param centrepoint the location of the centrepoint of the 3D crosshair
   * @param entityPlayer used to offset based on player's eye position
   * @param partialTick
   */
  private void draw3DCrosshair(Vec3d centrepoint, EntityPlayer entityPlayer, double partialTick)
  {

    // copied from DebugRendererCollisionBox

    GlStateManager.enableBlend();
    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                                        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    GlStateManager.glLineWidth(2.0F);
    GlStateManager.disableTexture2D();
    GlStateManager.depthMask(false);
    final float MAX_BYTE = 255;
//    GlStateManager.color(colour.getRed()/MAX_BYTE, colour.getGreen()/MAX_BYTE, colour.getBlue()/MAX_BYTE, colour.getAlpha()/MAX_BYTE);

    double px = entityPlayer.lastTickPosX + (entityPlayer.posX - entityPlayer.lastTickPosX) * partialTick;
    double py = entityPlayer.lastTickPosY + (entityPlayer.posY - entityPlayer.lastTickPosY) * partialTick;
    double pz = entityPlayer.lastTickPosZ + (entityPlayer.posZ - entityPlayer.lastTickPosZ) * partialTick;

    Color dummyDrawColour = Color.WHITE;
    double cx = centrepoint.x - px;
    double cy = centrepoint.y - py;
    double cz = centrepoint.z - pz;

    final double CROSSHAIR_RADIUS = 1.0;
    drawLine(cx - CROSSHAIR_RADIUS, cy, cz, cx + CROSSHAIR_RADIUS, cy, cz);
    drawLine(cx, cy - CROSSHAIR_RADIUS, cz, cx, cy + CROSSHAIR_RADIUS, cz);
    drawLine(cx, cy, cz - CROSSHAIR_RADIUS, cx, cy, cz + CROSSHAIR_RADIUS);

    GlStateManager.depthMask(true);
    GlStateManager.enableTexture2D();
    GlStateManager.disableBlend();
  }

  private static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2) {
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder bufferbuilder = tessellator.getBuffer();
    bufferbuilder.begin(3, DefaultVertexFormats.POSITION_COLOR);
    bufferbuilder.pos(x1, y1, z1).color(0, 0, 255, 255).endVertex();
    bufferbuilder.pos(x2, y2, z2).color(0, 0, 255, 255).endVertex();
    tessellator.draw();
  }
  /**
   * add a centrepoint to be rendered (as a crosshair) on the next frame.
   * @param x
   * @param y
   * @param z
   */
  public static void addCentrepointToRender(double x, double y, double z)
  {
    if (pointsToRenderNextFrame.size() < MAX_POINTS) {
      pointsToRenderNextFrame.add(new Vec3d(x, y, z));
    }
  }

  private static  final int MAX_POINTS = 500;
  private static List<Vec3d> pointsToRenderNextFrame = new ArrayList<>(MAX_POINTS);

}
