package com.TheRPGAdventurer.ROTD.util.debugging;

import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.EntityTameableDragon;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Created by TGG on 27/06/2019.
 *
 * While active, prevents any non-dragon entities from spawning.  Debugging purposes.
 * also useful: /kill @e[type=!Player]
 */
public class DebugSpawnInhibitor {

  @SubscribeEvent
  public void checkForSpawnDenial(LivingSpawnEvent.CheckSpawn event)
  {
    if (!DebugSettings.isSpawningInhibited() || event.getEntityLiving() instanceof EntityTameableDragon) {
      event.setResult(Event.Result.DEFAULT);
    } else {
      event.setResult(Event.Result.DENY);
    }
  }


}
