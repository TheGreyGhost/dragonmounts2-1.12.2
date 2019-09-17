package com.TheRPGAdventurer.ROTD.common.entity.physicalmodel;

import com.TheRPGAdventurer.ROTD.DragonMounts;

/**
 * Created by TGG on 17/09/2019.
 */
public class DragonVariantTagNotFoundException extends IllegalArgumentException {
  public DragonVariantTagNotFoundException(String msg) {
    super(msg);
  }

  // if true, ignore this error (probably caused by a client tag on dedicated server)
  public static boolean shouldIgnore() {
    return DragonMounts.proxy.isDedicatedServer();
  }
}
