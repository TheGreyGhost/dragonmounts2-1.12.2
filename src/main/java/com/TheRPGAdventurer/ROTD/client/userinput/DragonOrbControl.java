package com.TheRPGAdventurer.ROTD.client.userinput;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.breath.BreathWeaponTarget;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.inits.ModItems;
import com.TheRPGAdventurer.ROTD.common.network.MessageDragonTarget;
import com.TheRPGAdventurer.ROTD.util.DMUtils;
import com.TheRPGAdventurer.ROTD.util.RayTraceServer;
import com.TheRPGAdventurer.ROTD.util.debugging.DebugSettings;
import com.TheRPGAdventurer.ROTD.util.debugging.testclasses.DebugBreathFXSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

/**
 * This event handler is used to capture player input while the player is holding the dragon orb:
 * If the player is holding the dragon orb, records whether the player is holding down the
 * trigger and what the current target is (where the player is pointing the cursor)
 * Transmits this information to the server.
 * If the AutoLock option is selected, the orb will lock on to a target for as long as the trigger is held down.
 * If the autolock option isn't selected, the orb will change to whatever target is currently being looked at
 * <p>
 * Usage:
 * SETUP
 * (1) Register a server-side message handler for MessageDragonTarget
 * (2) Create the singleton in PostInit (client only) using DragonOrbControl.createSingleton(getNetwork());
 * (3) Initialise the keypress interception in PostInit (client only) using DragonOrbControl.initialiseInterceptors();
 * (4) Register the handler in PostInit(client only) using FMLCommonHandler.instance().bus()
 * .register(DragonOrbControl.getInstance());
 * <p>
 * MODIFYING
 * (1) Optionally: setKeyBreathState() to allow the rider of a dragon to breathe by pressing KEY_BREATH_PRIMARY, without holding a
 * dragon orb
 * <p>
 * POLLING
 * (1) get the singleton instance using getInstance()
 * (2) getTargetBeingLookedAt() returns the target being looked at, regardless of whether the trigger is held or not, and
 * regardless of whether there is an autolock target
 * (3) getTarget() returns the target of the orb while the trigger is being held.
 * (4) getTargetLockedOn() returns the target being breathed at (may be different to getTarget() if autolock is on).
 * Client side only.
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

public class DragonOrbControl {

  static public DragonOrbControl createSingleton(SimpleNetworkWrapper i_network) {
    instance = new DragonOrbControl(i_network);
    return instance;
  }

  static public DragonOrbControl getInstance() {
    return instance;
  }

  public static void initialiseInterceptors() {
    attackButtonInterceptor = new KeyBindingInterceptor(Minecraft.getMinecraft().gameSettings.keyBindAttack);
    Minecraft.getMinecraft().gameSettings.keyBindAttack = attackButtonInterceptor;
    attackButtonInterceptor.setInterceptionActive(false);

    useItemButtonInterceptor = new KeyBindingInterceptor(Minecraft.getMinecraft().gameSettings.keyBindUseItem);
    Minecraft.getMinecraft().gameSettings.keyBindUseItem = useItemButtonInterceptor;
    useItemButtonInterceptor.setInterceptionActive(false);
  }

  public static void enableClickInterception(boolean interception) {
    useItemButtonInterceptor.setInterceptionActive(interception);
    attackButtonInterceptor.setInterceptionActive(interception);
  }

  /**
   * Every tick, check if the player is holding the Dragon Orb, and if so, whether the player is targeting something with it
   * Additionally, check whether the player is riding the dragon and using the breath key to make the dragon breathe straight ahead
   * Send the target to the server at periodic intervals (if the target has changed significantly, or at least every x ticks)
   * <p>
   * Debug settings for freezing animation:
   * 1) hold the dragon orb
   * 2) hold either left or right mouse button to start the desired breath
   * 3) while holding the first button, click the other mouse button as well.
   * This will cause the dragon to stop updating on the client and the server, as well as any breathnodes
   * It will also stop any further target messages being sent
   * Once the animation has been frozen, it will stay frozen even after you release the mouse buttons
   * To cancel the freezing, use /dragon debug animationFrozen to toggle it off
   *
   * @param evt
   */
  @SubscribeEvent
  public void onTick(ClientTickEvent evt) {
    if (evt.phase != ClientTickEvent.Phase.START) return;
    EntityPlayerSP entityPlayerSP = Minecraft.getMinecraft().player;
    if (entityPlayerSP == null) return;

    boolean oldTriggerHeld = triggerHeld;

    boolean leftTriggerHeld = attackButtonInterceptor.isUnderlyingKeyDown();
    boolean rightTriggerHeld = useItemButtonInterceptor.isUnderlyingKeyDown();
    boolean orbTriggerHeld = leftTriggerHeld || rightTriggerHeld;

    if (!oldTriggerHeld) DebugBreathFXSettings.resetSpawnSuppressor();

    if (DebugSettings.isAnimationFreezeEnabled()) {
      if (leftTriggerHeld && rightTriggerHeld) {
        DebugSettings.setAnimationFreezeActive(true);
      }
      if (DebugSettings.isAnimationFrozen()) return;
    }

    BreathWeaponTarget.WeaponUsed weaponUsed = BreathWeaponTarget.WeaponUsed.NONE;
    if (leftTriggerHeld) {
      weaponUsed = BreathWeaponTarget.WeaponUsed.PRIMARY;
    } else if (rightTriggerHeld) {
      weaponUsed = BreathWeaponTarget.WeaponUsed.SECONDARY;
    }

    if (!DMUtils.hasEquipped(entityPlayerSP, ModItems.dragon_orb)) {
      enableClickInterception(false);
      orbTriggerHeld = false;
      targetBeingLookedAt = null;
    } else {
      enableClickInterception(true);
      final float MAX_ORB_RANGE = 20.0F;
      RayTraceResult mop = RayTraceServer.getMouseOver(entityPlayerSP.getEntityWorld(), entityPlayerSP, MAX_ORB_RANGE);
      targetBeingLookedAt = BreathWeaponTarget.fromMovingObjectPosition(mop, entityPlayerSP, weaponUsed);
      if (orbTriggerHeld) {
        breathWeaponTarget = BreathWeaponTarget.fromMovingObjectPosition(mop, entityPlayerSP, weaponUsed);
      }
    }
    if (!orbTriggerHeld && breathKeyHeld) {
      breathWeaponTarget = BreathWeaponTarget.targetDirection(dragonLookDirection, breathKeyWeaponUsed);
    }

    triggerHeld = orbTriggerHeld || breathKeyHeld;
    boolean needToSendMessage = false;
    if (!triggerHeld) {
      needToSendMessage = oldTriggerHeld;
    } else {
      if (!oldTriggerHeld) {
        needToSendMessage = true;
      } else {
        needToSendMessage = !breathWeaponTarget.approximatelyMatches(lastTargetSent);
      }
    }

    ++ticksSinceLastMessage;
    if (ticksSinceLastMessage >= MAX_TIME_NO_MESSAGE) {
      needToSendMessage = true;
    }

    if (needToSendMessage) {
      ticksSinceLastMessage = 0;
      lastTargetSent = breathWeaponTarget;
      MessageDragonTarget message = null;
      if (triggerHeld) {
        message = MessageDragonTarget.createTargetMessage(breathWeaponTarget);
      } else {
        message = MessageDragonTarget.createUntargetMessage();
      }
      network.sendToServer(message);
    }

    // if autolock is on, only change target when the player releases the button
    // (used on client side only, for rendering of Target Highlighting)  Server side AI is used for the real autolock
    boolean orbTargetAutoLock = DragonMounts.instance.getConfig().isOrbTargetAutoLock();
    if (breathWeaponTarget != null && orbTriggerHeld) {
      if (!orbTargetAutoLock || targetLockedOn == null) {
        targetLockedOn = breathWeaponTarget;
      }
    } else {
      targetLockedOn = null;
    }
  }

  /**
   * Get the block or entity being targeted by the dragon orb
   *
   * @return BreathWeaponTarget, or null for no target
   */
  public BreathWeaponTarget getTarget() {
    if (triggerHeld) {
      return breathWeaponTarget;
    } else {
      return null;
    }
  }

  /**
   * Used for breathing when riding without a Dragon Orb, i.e. holding down the KEY_BREATH_PRIMARY will cause the dragon to
   * breathe straight ahead where the dragon is looking.
   *
   * @param dragon      the dragon being ridden
   * @param newKeyState
   */
  public void setKeyBreathState(EntityTameableDragon dragon, boolean newKeyState, BreathWeaponTarget.WeaponUsed weaponUsed) {
    breathKeyHeld = newKeyState;
    dragonLookDirection = dragon.getLook(1.0F);
    breathKeyWeaponUsed = weaponUsed;
  }

  /**
   * Get the block or entity that the dragon orb cursor is currently pointing at
   *
   * @return BreathWeaponTarget, or null for none
   */
  public BreathWeaponTarget getTargetBeingLookedAt() {
    return targetBeingLookedAt;
  }

  public BreathWeaponTarget getTargetLockedOn() {
    return targetLockedOn;
  }

  private DragonOrbControl(SimpleNetworkWrapper i_network) {
    network = i_network;
    lastTargetSent = null;
  }
  private static DragonOrbControl instance = null;
  private static KeyBindingInterceptor attackButtonInterceptor;
  private static KeyBindingInterceptor useItemButtonInterceptor;
  private final int MAX_TIME_NO_MESSAGE = 20;  // send a message at least this many ticks or less
  private SimpleNetworkWrapper network;
  private int ticksSinceLastMessage = 0;
  private boolean triggerHeld = false;
  private boolean breathKeyHeld = false;
  private BreathWeaponTarget.WeaponUsed breathKeyWeaponUsed = BreathWeaponTarget.WeaponUsed.NONE;
  private Vec3d dragonLookDirection;
  private BreathWeaponTarget breathWeaponTarget;
  private BreathWeaponTarget lastTargetSent;
  private BreathWeaponTarget targetBeingLookedAt;
  private BreathWeaponTarget targetLockedOn;  // used client side only, for rendering.  server-side lockon is in AI

}
