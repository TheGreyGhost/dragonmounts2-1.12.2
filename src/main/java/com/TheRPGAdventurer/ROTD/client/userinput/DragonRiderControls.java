package com.TheRPGAdventurer.ROTD.client.userinput;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.breath.BreathWeaponTarget;
import com.TheRPGAdventurer.ROTD.common.inits.ModItems;
import com.TheRPGAdventurer.ROTD.common.inits.ModKeys;
import com.TheRPGAdventurer.ROTD.common.network.MessageDragonRiderControls;
import com.TheRPGAdventurer.ROTD.common.network.MessageDragonTarget;
import com.TheRPGAdventurer.ROTD.util.DMUtils;
import com.TheRPGAdventurer.ROTD.util.RayTraceServer;
import com.TheRPGAdventurer.ROTD.util.debugging.DebugSettings;
import com.TheRPGAdventurer.ROTD.util.debugging.testclasses.DebugBreathFXSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

/**
 * This event handler is used to capture player input while the player is riding the dragon.
 * Usage:
 * SETUP
 * (1) Register a server-side message handler for MessageDragonRiderControls
 * (2) Create the singleton in PostInit (client only) using DragonRiderControls.createSingleton(getNetwork());
 * (3) Register the tick handler in PostInit(client only) using FMLCommonHandler.instance().bus()
 * .register(DragonOrbControl.getInstance());
 * <p>
 * POLLING
 * (1) get the singleton instance using getInstance()
 */

/*
 ** 2013 October 27
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */

public class DragonRiderControls {

  static public DragonRiderControls createSingleton(SimpleNetworkWrapper i_network) {
    instance = new DragonRiderControls(i_network);
    return instance;
  }

  static public DragonRiderControls getInstance() {
    return instance;
  }

  /**
   * Every tick, check if the client player is riding a dragon; if so, check for key presses which control the dragon:
   * - flight commands
   * - breathweapon commands
   *
   * Send the key press information to the server at periodic intervals (if the keys have changed, or at least every x ticks)
   */
  @SubscribeEvent
  public void onTick(ClientTickEvent evt) {
    if (evt.phase != ClientTickEvent.Phase.START) return;
    EntityPlayerSP entityPlayerSP = Minecraft.getMinecraft().player;
    if (entityPlayerSP == null) return;

    Entity entityBeingRiddenByPlayer = entityPlayerSP.getRidingEntity();
    if (!(entityBeingRiddenByPlayer instanceof EntityTameableDragon)) return;

    EntityTameableDragon dragonBeingRiddenByPlayer = (EntityTameableDragon)entityBeingRiddenByPlayer;
    if (!dragonBeingRiddenByPlayer.riding().isThisTheControllingPlayer(entityPlayerSP)) return;
//    if ( (hasControllingPlayer(mc.player) && getControllingPlayer() != null)
//        || (dragon.getRidingEntity() instanceof EntityPlayer && this.getRidingEntity() != null && this.getRidingEntity().equals(mc.player)) || (getOwner() != null && firesupport())) {

    // check for breathweapon controls
    boolean breathKeyHeldDownPrimary = ModKeys.KEY_BREATH_PRIMARY.isKeyDown();
    boolean breathKeyHeldDownSecondary = ModKeys.KEY_BREATH_SECONDARY.isKeyDown();
    BreathWeaponTarget.WeaponUsed breathWeaponUsed = BreathWeaponTarget.WeaponUsed.NONE;
    boolean breathKeyHeldDownEither = breathKeyHeldDownPrimary || breathKeyHeldDownSecondary;
    if (breathKeyHeldDownPrimary) {
      breathWeaponUsed = BreathWeaponTarget.WeaponUsed.PRIMARY;
    } else if (breathKeyHeldDownSecondary) {
      breathWeaponUsed = BreathWeaponTarget.WeaponUsed.SECONDARY;
    }
    DragonOrbControl.getInstance().setKeyBreathState(dragonBeingRiddenByPlayer, breathKeyHeldDownEither, breathWeaponUsed);

    // check for rider flight controls
    boolean isBoosting = ModKeys.BOOST.isKeyDown();
    boolean isDown = ModKeys.DOWN.isKeyDown();
    boolean unhover = ModKeys.KEY_HOVERCANCEL.isPressed();
    boolean followyaw = ModKeys.FOLLOW_YAW.isPressed();
    boolean locky = ModKeys.KEY_LOCKEDY.isPressed();

    MessageDragonRiderControls msg = new MessageDragonRiderControls(dragonBeingRiddenByPlayer.getEntityId(), unhover, followyaw, locky, isBoosting, isDown);

    boolean needToSendMessage = false;
    needToSendMessage = !msg.equals(lastMessageSent);

    ++ticksSinceLastMessage;
    if (ticksSinceLastMessage >= MAX_TIME_NO_MESSAGE) {
      needToSendMessage = true;
    }

    if (needToSendMessage) {
      ticksSinceLastMessage = 0;
      DragonMounts.NETWORK_WRAPPER.sendToServer(msg);
      lastMessageSent = msg;
    }
  }

  private DragonRiderControls(SimpleNetworkWrapper i_network) {
    network = i_network;
  }
  private static DragonRiderControls instance = null;
  private final int MAX_TIME_NO_MESSAGE = 20;  // send a message at least this many ticks or less
  private SimpleNetworkWrapper network;
  private int ticksSinceLastMessage = 0;
  private MessageDragonRiderControls lastMessageSent = null;
}
