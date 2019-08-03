package com.TheRPGAdventurer.ROTD.common.event;

import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class EventLiving {

  @SubscribeEvent
  public void onEntityMount(EntityMountEvent event) {
    if (event.getEntityBeingMounted() instanceof EntityTameableDragon) {
      EntityTameableDragon dragon = (EntityTameableDragon) event.getEntityBeingMounted();
      if (event.isDismounting() && event.getEntityMounting() instanceof EntityPlayer && !event.getEntityMounting().world.isRemote) {
        EntityPlayer player = (EntityPlayer) event.getEntityMounting();

        double extraY = dragon.getAgeScale() * 0.2D;
        player.setPositionAndUpdate(dragon.posX, dragon.posY - extraY, dragon.posZ);
      }
    }
  }
}

