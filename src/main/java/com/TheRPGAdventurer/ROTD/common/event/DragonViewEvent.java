package com.TheRPGAdventurer.ROTD.common.event;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.gui.DragonMountsConfig;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.inits.ModKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DragonViewEvent {

  /**
   * Credit to AlexThe666 : iceandfire
   *
   * @param event
   */
  @SubscribeEvent
  public void thirdPersonCameraFix(EntityViewRenderEvent.CameraSetup event) {
    EntityPlayer player = Minecraft.getMinecraft().player;
    int currentView = DragonMounts.proxy.getDragon3rdPersonView();

    if (player.getRidingEntity() instanceof EntityTameableDragon) {
      EntityTameableDragon dragon = (EntityTameableDragon) player.getRidingEntity();
      if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
        GlStateManager.translate(0F, -0.6F * dragon.getAgeScale(), 0);
      }

      if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 1) {
        if (currentView == 0) {
          GlStateManager.translate(0F, -1.3F * dragon.getAgeScale(), -DragonMountsConfig.ThirdPersonZoom * dragon.getAgeScale());
        } else if (currentView == 1) {
          GlStateManager.translate(-4.7F, -0.08F * dragon.getAgeScale(), -DragonMountsConfig.ThirdPersonZoom * dragon.getAgeScale());
        } else if (currentView == 2) {
          GlStateManager.translate(4.7F, -0.08F * dragon.getAgeScale(), -DragonMountsConfig.ThirdPersonZoom * dragon.getAgeScale());
        }
      }

      if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 2) {
        if (currentView == 0) {
          GlStateManager.translate(0F, -1.3F * dragon.getAgeScale(), DragonMountsConfig.ThirdPersonZoom * dragon.getAgeScale());
        } else if (currentView == 1) {
          GlStateManager.translate(-4.7F, -0.08F * dragon.getAgeScale(), DragonMountsConfig.ThirdPersonZoom * dragon.getAgeScale());
        } else if (currentView == 2) {
          GlStateManager.translate(4.7F, -0.08F * dragon.getAgeScale(), DragonMountsConfig.ThirdPersonZoom * dragon.getAgeScale());
        }
      }
    }
  }

//    @SubscribeEvent
//    public void rideDragonGameOverlay(RenderGameOverlayEvent.Pre event) {
//        EntityPlayer player = Minecraft.getMinecraft().player;
//        if (player.getRidingEntity() instanceof EntityTameableDragon) {
//            EntityTameableDragon dragon = (EntityTameableDragon) player.getRidingEntity();
//            if (event.getType() == RenderGameOverlayEvent.ElementType.EXPERIENCE) event.setCanceled(true);
//                GuiDragonRide rideGui = new GuiDragonRide(dragon);
//                rideGui.renderDragonBoostHotbar();
//        }
//    }

  @SubscribeEvent
  public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
    if (event.getEntityLiving() instanceof EntityPlayer) {
      EntityPlayer player = (EntityPlayer) event.getEntityLiving();
      if (player.getRidingEntity() instanceof EntityTameableDragon) {
        if (ModKeys.dragon_change_view.isPressed()) {
          int currentView = DragonMounts.proxy.getDragon3rdPersonView();
          if (currentView + 1 > 2) {
            currentView = 0;
          } else {
            currentView++;
          }

          DragonMounts.proxy.setDragon3rdPersonView(currentView);
        }
      }
    }
  }
}