package com.TheRPGAdventurer.ROTD.util;

/**
 * Created by TGG on 8/11/2019.
 * Help to document the magic numbers used by SPacketEntityStatus
 * eg for knockback
 * world.setEntityState
 *
 */
public enum EntityState {
  KNOCKBACK((byte)30);


  public byte getMagicNumber() {return magicNumber;}

  private final byte magicNumber;
  private EntityState(byte magicNumber) {
    this.magicNumber = magicNumber;
  }
}
