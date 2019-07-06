package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon;

import com.TheRPGAdventurer.ROTD.util.debugging.DebugSettings;
import com.sun.javafx.geom.Vec3f;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

/**
 * Created by TGG on 6/07/2019.
 *
 * This class describes the physical configuration of the dragon
 * At the moment it only describes some parts which are necessary for code to know about
 * eg hitbox size, throat position, rider positions, banner positions
 *
 * It's also just a "copy" of the DragonModel in easier-to-understand form
 *   i.e. if DragonModel changes, this DragonPhysicalModel needs to be updated manually
 *
 * All units are in minecraft coordinates (metres) and in the minecraft coordinate reference system
 * The model is for a dragon at BASE size.
 *
 * The body origin is in the centre of the body, everything is referenced relative to that
 *
 * the y-axis is up/down (up is positive)
 * the x-axis is left/right
 * the z-axis is front/back (front = highest z)
 *
 */
public class DragonPhysicalModel {


  public AxisAlignedBB hitBox(boolean sitting)
  {

  }

  float getHitboxWidth() {return 4.8F;}

  float getHitboxHeight() {return 4.2F;}

  public float bodyOriginHeightAboveGround(boolean sitting)
  {
    return BODY_HALF_HEIGHT + (sitting ? BELLY_TO_GROUND_WHEN_SITTING : BELLY_TO_GROUND_WHEN_STANDING);
  }

  public Vec3f offsetOriginFromEntityPos(boolean sitting)
  {
    return new Vec3f(0, bodyOriginHeightAboveGround(sitting), 0);
  }

  /** gets the position offset to use for a passenger at BASE size
   * i.e. the position of the rider relative to the origin of the body
  * @param passengerNumber the number (0.. max) of the passenger
  * @return the [x, y, z] of the mounting position relative to the dragon [posX, posY, posZ]
  */
  public Vec3f getRiderPositionOffset(int passengerNumber)
  {
    float yoffset = BODY_HALF_HEIGHT + SADDLE_THICKNESS;  // sit the rider directly on top of the saddle on the back

    // dragon position is the middle of the model and the saddle is on
    // the shoulders, so move player forwards on Z axis relative to the
    // dragon's rotation to fix that
    //

    Vec3f offset = new Vec3f(0,0,0); // default

    switch (passengerNumber) {
      case 0:
        offset = new Vec3f(0, yoffset, BODY_HALF_LENGTH * 0.75F); break;
      case 1:
        offset = new Vec3f(+0.6F, yoffset, 0); break;
      case 2:
        offset = new Vec3f(-0.6F, yoffset, 0); break;
    }

    if (DebugSettings.isRiderPositionTweak()) {
      offset = DebugSettings.getRiderPositionOffset(passengerNumber, offset);
    }
    return offset;
  }

  private float BODY_HALF_HEIGHT = 0.75F;
  private float BODY_HALF_LENGTH = 2.0F;
  private float SADDLE_THICKNESS = 2.0F/16.0F;

  // relative to body origin:
  // 0.25 metres above the top of the back but through the middle of the body
  private Vec3f rotationPointForPitch = new Vec3f(0, BODY_HALF_HEIGHT + 0.25F, 0);

  private float BELLY_TO_GROUND_WHEN_SITTING = 0.9F;
  private float BELLY_TO_GROUND_WHEN_STANDING = 1.5F;

}
