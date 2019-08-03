/*
 ** 2014 March 19
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.common.network;

import com.TheRPGAdventurer.ROTD.common.entity.breath.BreathWeaponTarget;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Message to tell dragon what to target with their ranged breath weapon.
 * Sent from client to server only (server to client is by datawatcher)
 *
 * @author TGG
 */
public class MessageDragonTarget implements IMessage {

  // create a new message (used by SimpleNetworkWrapper)
  public MessageDragonTarget() {
    packetIsValid = false;
  }

  /**
   * Creates a message saying 'nothing is targeted'
   *
   * @return the message for sending
   */
  public static MessageDragonTarget createUntargetMessage() {
    MessageDragonTarget retval = new MessageDragonTarget();
    retval.targeting = false;
    retval.packetIsValid = true;
    return retval;
  }

  /**
   * Creates a message specifying the current target
   *
   * @param i_target the target
   * @return the message for sending
   */
  public static MessageDragonTarget createTargetMessage(BreathWeaponTarget i_target) {
    MessageDragonTarget retval = new MessageDragonTarget();
    retval.targeting = true;
    retval.target = i_target;
    retval.packetIsValid = true;
    return retval;
  }

  public boolean isPacketIsValid() {
    return packetIsValid;
  }

  public boolean isTargeting() {
    return targeting;
  }

  public BreathWeaponTarget getTarget() {
    return target;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    packetIsValid = false;
    try {
      targeting = buf.readBoolean();
      if (targeting) {
        target = BreathWeaponTarget.fromBytes(buf);
      }
    } catch (IndexOutOfBoundsException ioe) {
      if (printedError) return;
      printedError = true;
      System.err.println("Exception while reading MessageDragonTarget: " + ioe);
      return;
    } catch (IllegalArgumentException ioe) {
      if (printedError) return;
      printedError = true;
      System.err.println("Exception while reading MessageDragonTarget: " + ioe);
      return;
    }
    packetIsValid = true;
  }


  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeBoolean(targeting);
    if (!targeting) return;
    target.toBytes(buf);
  }
  private static boolean printedError = false;
  private BreathWeaponTarget target;
  private boolean targeting;
  private boolean packetIsValid = false;
}
