package com.TheRPGAdventurer.ROTD.server.handler;

import com.TheRPGAdventurer.ROTD.server.entity.EntityTameableDragon;

import net.minecraft.entity.Entity;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class MountHandler {
	@SubscribeEvent
	public void canMountEvent( EntityMountEvent event ) {
		if( event.isDismounting() ) {
			if( event.getWorldObj().isRemote )
				return;
			Entity ent = event.getEntityBeingMounted();
			if( ent instanceof EntityTameableDragon ) {
				EntityTameableDragon dragon = (EntityTameableDragon)ent;
				if( !dragon.onGround ) {
					event.setCanceled(true);
				}					
			}
		}
	}
}
