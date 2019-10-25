package com.TheRPGAdventurer.ROTD.common.network;

import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.util.DMUtils;
import io.netty.buffer.ByteBuf;
import net.ilexiconn.llibrary.server.network.AbstractMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageDragonRiderControls extends AbstractMessage<MessageDragonRiderControls> {

  public boolean isHoverCancel;
  public boolean isFollowYaw;
  public boolean locky;
  public boolean isBoosting;
  public boolean down;

  public MessageDragonRiderControls(int dragonId, boolean isHoverCancel, boolean isFollowYaw, boolean locky, boolean isBoosting, boolean down) {
    this.dragonId = dragonId;
    this.isHoverCancel = isHoverCancel;
    this.isFollowYaw = isFollowYaw;
    this.locky = locky;
    this.isBoosting = isBoosting;
    this.down = down;
  }

  public MessageDragonRiderControls() {
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    dragonId = buf.readInt();
    isHoverCancel = buf.readBoolean();
    isFollowYaw = buf.readBoolean();
    locky = buf.readBoolean();
    isBoosting = buf.readBoolean();
    down = buf.readBoolean();

  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeInt(dragonId);
    buf.writeBoolean(isHoverCancel);
    buf.writeBoolean(isFollowYaw);
    buf.writeBoolean(locky);
    buf.writeBoolean(isBoosting);
    buf.writeBoolean(down);

  }

  @Override
  @SideOnly(Side.CLIENT)
  public void onClientReceived(Minecraft client, MessageDragonRiderControls message, EntityPlayer player, MessageContext messageContext) {
  }

  @Override
  public void onServerReceived(MinecraftServer server, MessageDragonRiderControls message, EntityPlayer player, MessageContext messageContext) {
    Entity entity = player.world.getEntityByID(message.dragonId);
    if (entity instanceof EntityTameableDragon) {
      EntityTameableDragon dragon = (EntityTameableDragon) entity;

      if (message.isHoverCancel) {
        dragon.setUnHovered(!dragon.isUnHovered());
        player.sendStatusMessage(new TextComponentTranslation(DMUtils.translateToLocal("msg.dragon.toggleHover") + (dragon.isUnHovered() ? ": On" : ": Off")), false);
      }

      if (message.isFollowYaw) {
        dragon.setFollowYaw(!dragon.followYaw());
        player.sendStatusMessage(new TextComponentTranslation(DMUtils.translateToLocal("msg.dragon.toggleFollowYaw") + (dragon.followYaw() ? ": On" : ": Off")), false);
      }

      if (message.locky) {
        dragon.setYLocked(!dragon.isYLocked());
        player.sendStatusMessage(new TextComponentTranslation(DMUtils.translateToLocal("msg.dragon.toggleYLock") + (dragon.isYLocked() ? ": On" : ": Off")), false);
      }

      if (message.down) dragon.setGoingDown(true);
      else dragon.setGoingDown(false);

      if (message.isBoosting) dragon.setBoosting(true);
      else dragon.setBoosting(false);
    }
  }
  private int dragonId;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof MessageDragonRiderControls)) return false;
    MessageDragonRiderControls msg2 = (MessageDragonRiderControls)obj;
    return (isHoverCancel == msg2.isHoverCancel) && (isFollowYaw == msg2.isFollowYaw) && (locky == msg2.locky) && (isBoosting == msg2.isBoosting && down == msg2.down);
  }

}
