package com.TheRPGAdventurer.ROTD.client.other;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Created by TGG on 29/08/2019.
 * Keeps track of the number of ticks since the client started
 * Useful for animation, doesn't get saved (i.e. reset to zero when the game is restarted)
 */
public class ClientTickCounter {

  @SubscribeEvent
  public static void clientTicker(TickEvent.ClientTickEvent tickEvent) {
    ++ticksSinceStart;
  }

  public static long getTicksSinceStart() {return ticksSinceStart;}
  private static long ticksSinceStart = 0;
}
