/*
** 2016 March 07
**
** The author disclaims copyright to this source code. In place of
** a legal notice, here is a blessing:
**    May you do good and not evil.
**    May you find forgiveness for yourself and forgive others.
**    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.common.entity.breeds;

import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonVariants;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.Modifiers;

/**
 * Just a convenience class
 */
public class DragonBreedWithModifiers {
  public DragonBreedWithModifiers(DragonBreedNew dragonBreedNew, Modifiers modifiers) {
    this.dragonBreedNew = dragonBreedNew;
    this.modifiers = modifiers;
  }

  public DragonBreedNew dragonBreedNew;
  public Modifiers modifiers;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || !(o instanceof DragonBreedWithModifiers)) return false;
    DragonBreedWithModifiers dbwm2 = (DragonBreedWithModifiers)o;
    return this.dragonBreedNew.equals(dbwm2.dragonBreedNew) && this.modifiers.equals(dbwm2.modifiers);
  }
  @Override
  public int hashCode() {
    return dragonBreedNew.hashCode() * 37  + modifiers.hashCode(); // simple but doesn't need to be optimised.
  }
}
