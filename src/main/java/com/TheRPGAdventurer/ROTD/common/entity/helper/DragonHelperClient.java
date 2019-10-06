package com.TheRPGAdventurer.ROTD.common.entity.helper;

import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;

/**
 * Created by TGG on 6/10/2019.
 * Just a convenience class to retrieve ClientHelpers more easily
 */
public abstract class DragonHelperClient extends DragonHelper {
  public DragonHelperClient(EntityTameableDragon dragon) {
    super(dragon);
  }
}
