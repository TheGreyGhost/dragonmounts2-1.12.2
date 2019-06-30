package com.TheRPGAdventurer.ROTD.client.other;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.DragonMountsConfig;
import com.TheRPGAdventurer.ROTD.client.userinput.DragonOrbControl;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.BreathWeaponTarget;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;

/**
 * Highlights the current target of the dragon orb.
 * Usage:
 * (1) Register the handler during PostInit client only
 * Created by TGG on 28/08/2015.
 */
public class TargetHighlighter
{
  /**
   * If the player is holding the dragon orb but not currently holding the attack button down, override the normal
   *   highlighting with a custom highlighting. Otherwise, revert to the default.
   *   This event should be called last because it cancels any further rendering (to prevent vanilla highlighting)
   * @param event the event
   */
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void blockHighlightDecider(DrawBlockHighlightEvent event)
  {
    BreathWeaponTarget targetBeingLookedAt = DragonOrbControl.getInstance().getTargetBeingLookedAt();
    if (targetBeingLookedAt == null) {
      return;
    }

    event.setCanceled(true);

    if (!DragonMountsConfig.isOrbHighlightTarget()) {
      return;
    }

    BreathWeaponTarget targetToHighlight = targetBeingLookedAt;
    BreathWeaponTarget targetLockedOn = DragonOrbControl.getInstance().getTargetLockedOn();
    if (targetLockedOn != null) {
      targetToHighlight = targetLockedOn;
    }

    switch (targetToHighlight.getTypeOfTarget()) {
      case ENTITY: {
        highlightEntity(targetToHighlight.getTargetEntity(event.getPlayer().getEntityWorld()), event.getPlayer(), event.getPartialTicks());
        break;
      }
      case LOCATION: {
        highlightBlock(targetToHighlight.getTargetedLocation(), event.getPlayer(), event.getPartialTicks());
        break;
      }
      case DIRECTION: {
        return;
      }
      default: {
          DragonMounts.loggerLimit.error_once(
                  "Unknown target type in blockHighlightDecider : " + targetToHighlight.getTypeOfTarget());
          return;
      }
    }
  }

  private void highlightEntity(Entity entity, EntityPlayer entityPlayer, double partialTick)
  {
    if (entity == null || entityPlayer == null) return;

    AxisAlignedBB entityAABB = entity.getEntityBoundingBox();

    int timeMS = (int) System.currentTimeMillis();
    drawAABB(entityAABB, entityPlayer, partialTick, timeMS, Color.RED);
  }

  private void highlightBlock(Vec3d worldPos, EntityPlayer entityPlayer, double partialTick)
  {
    if (worldPos == null || entityPlayer == null) return;
    BlockPos blockPos = new BlockPos(worldPos);
    AxisAlignedBB blockAABB = new AxisAlignedBB(blockPos, blockPos.add(1, 1, 1));

    int timeMS = (int)System.currentTimeMillis();
    drawAABB(blockAABB, entityPlayer, partialTick, timeMS, Color.RED);
  }

  /** draw an oscillating outlined bounding box around the indicated aabb
   *
   * @param aabb the aabb to draw
   * @param entityPlayer used to offset based on player's eye position
   * @param partialTick
   * @param animationTimerMS a timer used to updateFromAnimator the bounding box (expands & contracts). 0 = no effect.  Elapsed
   *                         time in milliseconds.
   */
  private void drawAABB(AxisAlignedBB aabb, EntityPlayer entityPlayer, double partialTick, int animationTimerMS,
                        Color colour)
  {
    final double EXPANSION_MIN = 0.002; // amount to expand the AABB by
    final double EXPANSION_MAX = 0.100;
    double expansionamount = EXPANSION_MIN;
    if (animationTimerMS != 0) {
      final int CYCLE_TIME_MS = 1000;
      double cyclePos = MathX.modulus(animationTimerMS, CYCLE_TIME_MS) / (double)CYCLE_TIME_MS;
      if (cyclePos < 0.5) {
        expansionamount = MathX.terp(EXPANSION_MIN, EXPANSION_MAX, cyclePos * 2.0);
      } else {
        expansionamount = MathX.terp(EXPANSION_MAX, EXPANSION_MIN, 2 * (cyclePos - 0.5));
      }
    }
    AxisAlignedBB expandedBox = aabb.grow(expansionamount);

    // copied from DebugRendererCollisionBox

    GlStateManager.enableBlend();
    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                                        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    GlStateManager.glLineWidth(2.0F);
    GlStateManager.disableTexture2D();
    GlStateManager.depthMask(false);
    final float MAX_BYTE = 255;
    GlStateManager.color(colour.getRed()/MAX_BYTE, colour.getGreen()/MAX_BYTE, colour.getBlue()/MAX_BYTE, colour.getAlpha()/MAX_BYTE);

    double px = entityPlayer.lastTickPosX + (entityPlayer.posX - entityPlayer.lastTickPosX) * partialTick;
    double py = entityPlayer.lastTickPosY + (entityPlayer.posY - entityPlayer.lastTickPosY) * partialTick;
    double pz = entityPlayer.lastTickPosZ + (entityPlayer.posZ - entityPlayer.lastTickPosZ) * partialTick;

    Color dummyDrawColour = Color.WHITE;
    RenderGlobal.drawSelectionBoundingBox(expandedBox.offset(-px, -py, -pz),
            dummyDrawColour.getRed()/MAX_BYTE, dummyDrawColour.getGreen()/MAX_BYTE,
            dummyDrawColour.getBlue()/MAX_BYTE, dummyDrawColour.getAlpha()/MAX_BYTE);

    GlStateManager.depthMask(true);
    GlStateManager.enableTexture2D();
    GlStateManager.disableBlend();
  }
}
