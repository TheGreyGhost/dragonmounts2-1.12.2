/*
** 2016 April 26
**
** The author disclaims copyright to this source code. In place of
** a legal notice, here is a blessing:
**    May you do good and not evil.
**    May you find forgiveness for yourself and forgive others.
**    May you share freely, never taking more than you give.
 */
<<<<<<< HEAD:src/main/java/com/TheRPGAdventurer/ROTD/entity/ai/air/EntityAIDragonFollowOwnerElytraFlying.java
package com.TheRPGAdventurer.ROTD.entity.ai.air;
=======
package com.TheRPGAdventurer.ROTD.entity.entitytameabledragon.ai.air;

import com.TheRPGAdventurer.ROTD.entity.entitytameabledragon.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.entity.entitytameabledragon.ai.EntityAIDragonBase;
>>>>>>> 487f066b... changes:src/main/java/com/TheRPGAdventurer/ROTD/entity/entitytameabledragon/ai/air/EntityAIDragonFollowOwnerElytraFlying.java

import com.TheRPGAdventurer.ROTD.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.entity.ai.EntityAIDragonBase;
import net.minecraft.entity.player.EntityPlayer;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class EntityAIDragonFollowOwnerElytraFlying extends EntityAIDragonBase {
    
    protected EntityPlayer owner;

    public EntityAIDragonFollowOwnerElytraFlying(EntityTameableDragon dragon) {
        super(dragon);
    }

    @Override
    public boolean shouldExecute() {
    	
    	if (!dragon.canFly()) {
    		return false;
    	}
        
        // don't follow if sitting
        if (dragon.isSitting()) {
        	return false;
        }
        
        if(dragon.getLeashed()) {
           return false;
        }
        
        owner = (EntityPlayer) dragon.getOwner();
        
        // don't follow if ownerless 
        if (owner == null) {
            return false;
        }
        
        // don't follow if already being ridden
        if (dragon.isPassenger(owner)) {
            return false;
        }
        
        // follow only if the owner is using an Elytra
        return owner.isElytraFlying();
    }
    
    @Override
    public void updateTask() {
        dragon.getNavigator().tryMoveToXYZ(owner.posX, owner.posY, owner.posZ - 25, 1);
        dragon.setBoosting(dragon.getDistanceToEntity(owner) > 18);
    }
}
