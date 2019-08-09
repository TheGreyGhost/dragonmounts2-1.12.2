/*
 ** 2013 October 27
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.common.entity.helper;

import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.EntityDataManager;

import java.util.Random;

/**
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public abstract class DragonHelper {

  public DragonHelper(EntityTameableDragon dragon) {
    this.dragon = dragon;
    this.entityDataManager = dragon.getDataManager();
    this.rand = dragon.getRNG();
  }

  public void writeToNBT(NBTTagCompound nbt) {
  }

  public void readFromNBT(NBTTagCompound nbt) {
  }

  public void applyEntityAttributes() {
  }

  public void onLivingUpdate() {
  }

  public void onDeathUpdate() {
  }

  public void onDeath() {
  }

  // for testing only
  protected DragonHelper() {
    dragon = null;
    entityDataManager = null;
    rand = null;
  }


  protected final EntityTameableDragon dragon;
  protected final EntityDataManager entityDataManager;
  protected final Random rand;
}
