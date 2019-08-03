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

import com.TheRPGAdventurer.ROTD.common.entity.breath.DragonOrbTargets;
import io.netty.channel.ChannelHandler.Sharable;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Network handler for dragon targeting messages (client tells the server what the dragon should target).
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
@Sharable
public class MessageDragonTargetHandlerServer implements IMessageHandler<MessageDragonTarget, IMessage> {

  @Override
  public IMessage onMessage(MessageDragonTarget message, MessageContext ctx) {
    if (ctx.side != Side.SERVER) {
      L.warn("MessageDragonTarget received on wrong side:" + ctx.side);
      return null;
    }

    // we know for sure that this handler is only used on the server side, so it is ok to assume
    //  that the ctx handler is a serverhandler, and that WorldServer exists.
    // Packets received on the client side must be handled differently!  See MessageHandlerOnClient

    final EntityPlayerMP sendingPlayer = ctx.getServerHandler().player;
    if (sendingPlayer == null) {
      L.warn("EntityPlayerMP was null when MessageDragonTarget was received");
      return null;
    }

    if (!message.isPacketIsValid()) {
      L.warn("Invalid MessageDragonTarget received");
      return null;
    }

    if (message.isTargeting()) {
      DragonOrbTargets.getInstance().setPlayerTarget(sendingPlayer, message.getTarget());
    } else {
      DragonOrbTargets.getInstance().clearPlayerTarget(sendingPlayer);
    }

    return null;
  }
  private static final Logger L = LogManager.getLogger();

}
