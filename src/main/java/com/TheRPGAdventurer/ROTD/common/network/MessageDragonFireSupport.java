package com.TheRPGAdventurer.ROTD.common.network;

import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.inits.ModSounds;
import io.netty.buffer.ByteBuf;
import net.ilexiconn.llibrary.server.network.AbstractMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.UUID;

public class MessageDragonFireSupport extends AbstractMessage<MessageDragonFireSupport> {

  public UUID dragonId;
  public MessageDragonFireSupport(UUID dragonId) {
    this.dragonId = dragonId;
  }

  public MessageDragonFireSupport() {
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    PacketBuffer packetBuf = new PacketBuffer(buf);
    dragonId = packetBuf.readUniqueId();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    PacketBuffer packetBuf = new PacketBuffer(buf);
    packetBuf.writeUniqueId(dragonId);

  }

  @Override
  @SideOnly(Side.CLIENT)
  public void onClientReceived(Minecraft client, MessageDragonFireSupport message, EntityPlayer player, MessageContext messageContext) {
  }

  @Override
  public void onServerReceived(MinecraftServer server, MessageDragonFireSupport message, EntityPlayer player, MessageContext messageContext) {
    clientWhistleSound(player);
    if (!player.world.isRemote) {
      Entity entity = server.getEntityFromUuid(dragonId);
      EntityTameableDragon dragon = (EntityTameableDragon) entity;
      if (entity != null) {
        if (entity instanceof EntityTameableDragon && dragon.isOwner(player)) {
          dragon.setfiresupport(!dragon.firesupport());
        }
      } else player.sendStatusMessage(new TextComponentTranslation("whistle.msg.fail"), true);
    }
  }

  /**
   * Play Sound on the client only; dont let anyone else hear!
   * <p>
   * Doesnt seem to work in {@code onClientRecieved()}...
   *
   * @param player
   */
  @SideOnly(Side.CLIENT)
  private void clientWhistleSound(EntityPlayer player) {
    player.world.playSound(null, player.getPosition(), ModSounds.DRAGON_WHISTLE, SoundCategory.PLAYERS, 4, 1);
  }
  EntityTameableDragon dragon;
}