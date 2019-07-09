package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.helper.DragonLifeStage;
import com.TheRPGAdventurer.ROTD.util.debugging.DebugSettings;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
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
 * The coordinate systems used are:
 * MC = model space coordinates (as used by DragonModel, ModelPart:
 * BC = Base space coordinates = model space scaled by 1/16, with x and y inverted, i.e. a 1:1 rendering of the Model Space, which
 *          occurs when rendered with GLStateManager.scale(1.0))
 * WC = world space coordinates (i.e. full-size rendering using GLStateManager.scale(getAdultModelRenderScaleFactor()))
 *
 * WC includes the dragon scale (0 to 1) where 1.0 is full-size dragon; scale is the vanilla scaling as used for EntityAgeable
 *
 * The body origin in BC and WC is in the centre of the body, everything is referenced relative to that
 * The body origin in MC is not the same location; the MC origin is at [0, +0.75, 1.0] in BC
 *
 * the y-axis is up/down (up is positive)
 * the x-axis is left/right
 * the z-axis is front/back (front = highest z)
 *
 */
public class DragonPhysicalModel {

  float getHitboxWidthWC(float scale) {return scale * getHitboxWidthBC() * RENDER_SCALE_FACTOR;}

  float getHitboxHeightWC(float scale) {return scale * getHitboxHeightBC() * RENDER_SCALE_FACTOR;}

  float getHitboxWidthBC() {return 2.75F;}

  float getHitboxHeightBC() {return 2.75F;}

  public float bodyOriginHeightAboveGroundBC(boolean sitting)
  {
    return BODY_HALF_HEIGHT_BC + (sitting ? BELLY_TO_GROUND_WHEN_SITTING_BC : BELLY_TO_GROUND_WHEN_STANDING_BC);
  }

  /**
   * Returns the origin of the entity's body from the entity origin (posX, posY, posZ), in WorldSpace coordinates
   * @param scale the scale of the dragon (0 -> 1) , 1.0 is fully grown
   * @param sitting
   * @return
   */
  public Vec3d offsetOfOriginFromEntityPosWC(float scale, boolean sitting)
  {
    return new Vec3d(0, bodyOriginHeightAboveGroundBC(sitting), 0).scale(RENDER_SCALE_FACTOR * scale);
  }

  /**
   * used when rendering; scale up the base model by this factor for a fully-grown adult
   * @return the relative render scale factor (1.0 = no scaling)
   */
  public float getFullSizeRenderScaleFactor()
  {
    return RENDER_SCALE_FACTOR;
  }

  /**
   * used when rendering; scale up the base model by this factor
   * @param scale the scale of the dragon (0 -> 1) , 1.0 is fully grown
   * @return the render scale factor to use
   */
  public float getRenderScaleFactor(float scale) { return RENDER_SCALE_FACTOR * scale; }

  /**
   * The factor used to convert from model space coordinates to worldspace coordinates
   * @return
   */
  public float getConversionFactorMCtoWC(float scale) {return CONVERSION_FACTOR_MC_TO_BC * RENDER_SCALE_FACTOR * scale;}

  /** gets the position offset to use for a passenger
   * i.e. the position of the rider relative to the entity posX, posY, posZ
   * @param scale the scale of the dragon (0 -> 1) , 1.0 is fully grown
   * @param pitch the body pitch of the dragon (as per vanilla convention)
   * @param passengerNumber the number (0.. max) of the passenger
   * @return the [x, y, z] of the mounting position relative to the dragon entity origin [posX, posY, posZ]
   */
  public Vec3d getRiderPositionOffsetWC(float scale, float pitch, boolean sitting, int passengerNumber)
  {
    Vec3d riderPositionOffsetFromBO = getRiderPositionOffsetBC(passengerNumber);
    Vec3d riderPositionOffsetFromRP = riderPositionOffsetFromBO.subtract(ROTATION_POINT_FOR_BODY_PITCH_BC);
    Vec3d riderPitchedPositionFromRP = riderPositionOffsetFromRP.rotatePitch((float)Math.toRadians(pitch));
    Vec3d riderPitchedPositionFromBO = ROTATION_POINT_FOR_BODY_PITCH_BC.add(riderPitchedPositionFromRP);
    return offsetOfOriginFromEntityPosWC(scale, sitting)
            .add(riderPitchedPositionFromBO.scale(scale * RENDER_SCALE_FACTOR));
  }

  /** gets the position offset to use for a passenger at BASE size
   * i.e. the position of the rider relative to the origin of the body
  * @param passengerNumber the number (0.. max) of the passenger
  * @return the [x, y, z] of the mounting position relative to the dragon origin; in Basespace Coordinates
  */
  private Vec3d getRiderPositionOffsetBC(int passengerNumber)
  {
    float yoffset = BODY_HALF_HEIGHT_BC + SADDLE_THICKNESS_BC;  // sit the rider directly on top of the saddle on the back

    // dragon position is the middle of the model and the saddle is on
    // the shoulders, so move player forwards on Z axis relative to the
    // dragon's rotation to fix that
    //

    Vec3d offset = new Vec3d(0, yoffset, 0); // default

    switch (passengerNumber) {
      case 0:
        offset = new Vec3d(0, yoffset, 1.1F); break;  // determined by tweaking
      case 1:
        offset = new Vec3d(+0.6F, yoffset, 0); break;
      case 2:
        offset = new Vec3d(-0.6F, yoffset, 0); break;
      default:
        DragonMounts.loggerLimit.error_once("Illegal passengerNumber:" + passengerNumber);
    }

    if (DebugSettings.isRiderPositionTweak()) {
      offset = DebugSettings.getRiderPositionOffset(passengerNumber, offset);
    }
    return offset;
  }

  /** how many passengers can ride on this breed?
   * @return
   */
  public int getMaxNumberOfPassengers(DragonLifeStage dragonLifeStage)
  {
    return 3;
  }

  /** what is the eye height of the dragon?
   *  @param scale the scale of the dragon (0 -> 1) , 1.0 is fully grown
   *  @param isSitting true if the dragon is sitting
   *  @return the height of the eye above the feet, in WorldSpace Coordinates (normal minecraft coordinates)
   *  todo later: use the actual position of the head
   */
  public float getEyeHeightWC(float scale, boolean isSitting)
  {
    if (DebugSettings.existsDebugParameter("headbodyx")) {
      HEAD_OFFSET_FROM_BODY_BC = new Vec3d(DebugSettings.getDebugParameter("headbodyx"),
                                           HEAD_OFFSET_FROM_BODY_BC.y, HEAD_OFFSET_FROM_BODY_BC.z);
    }
    if (DebugSettings.existsDebugParameter("headbodyy")) {
      HEAD_OFFSET_FROM_BODY_BC = new Vec3d(HEAD_OFFSET_FROM_BODY_BC.x,
              DebugSettings.getDebugParameter("headbodyy"),
              HEAD_OFFSET_FROM_BODY_BC.z);
    }
    if (DebugSettings.existsDebugParameter("headbodyz")) {
      HEAD_OFFSET_FROM_BODY_BC = new Vec3d(HEAD_OFFSET_FROM_BODY_BC.x, HEAD_OFFSET_FROM_BODY_BC.y,
              DebugSettings.getDebugParameter("headbodyz"));
    }

    double offsetEyeFromBodyOriginWCY = scale * RENDER_SCALE_FACTOR * HEAD_OFFSET_FROM_BODY_BC.y;
    double originBodyWCY = offsetOfOriginFromEntityPosWC(scale, isSitting).y;
    return (float)(offsetEyeFromBodyOriginWCY + originBodyWCY);
  }
  /** Entity rotation Yaw */
  /**
   * Get the position of the eye relative
   *  @param scale the scale of the dragon (0 -> 1) , 1.0 is fully grown
   * @param rotationYaw the rotation (yaw) of the entity - as per vanilla value
   * @param pitch of the entity - as per vanilla convention
   * @param isSitting
   * @return the world coordinates of the eye relative to the entity position [posX, posY, posZ]
   * todo: currently does an approximation for the head position.
   */
  public Vec3d getEyePositionWC(float scale, float rotationYaw, float pitch, boolean isSitting)
  {
    Vec3d headPitchedOffsetFromRP = HEAD_OFFSET_FROM_ROTATION_POINT_BC.rotatePitch((float)Math.toRadians(pitch));
    Vec3d headPitchedOffsetFromBO = ROTATION_POINT_FOR_BODY_PITCH_BC.add(headPitchedOffsetFromRP);
    Vec3d headRotatedOffset = headPitchedOffsetFromBO.rotateYaw(-(float)Math.toRadians(rotationYaw)).scale(scale * RENDER_SCALE_FACTOR);
    return offsetOfOriginFromEntityPosWC(scale, isSitting).add(headRotatedOffset);
  }

  /**Get the relative head size - depends on the age of the dragon.
   * Baby dragon has a relatively larger head compared to its body size (makes it look cuter)
   *
   * @param scale the scale of the dragon (0 -> 1) , 1.0 is fully grown
   * @return the relative size of the head.  1.0 means normal size (adult).  Baby head is larger (eg 1.5)
   */
  public float getRelativeHeadSize(float scale) {
    final float RELATIVE_SIZE_OF_ADULT_HEAD = 1.0F;
    final float RELATIVE_SIZE_OF_BABY_HEAD = 2.0F;
    final float SCALE_OF_BABY = 0.2F;
    final float SCALE_OF_ADULT = 1.0F;

    // used to be 1.4F / (scale + 0.4F) i.e. a rational function of the form head_size = A / (scale + B)
    // We want the headsize of the adult to be RELATIVE_SIZE_OF_ADULT_HEAD at SCALE_OF_ADULT, and
    //    headsize of the baby to be RELATIVE_SIZE_OF_BABY_HEAD at SCALE_OF_BABY
    //  we can rearrange to solve for A and B
    final float B = (RELATIVE_SIZE_OF_ADULT_HEAD * SCALE_OF_ADULT - RELATIVE_SIZE_OF_BABY_HEAD * SCALE_OF_BABY) /
            (RELATIVE_SIZE_OF_BABY_HEAD - RELATIVE_SIZE_OF_ADULT_HEAD);
    final float A = RELATIVE_SIZE_OF_ADULT_HEAD * (SCALE_OF_ADULT + B);

    scale = MathX.clamp(scale, SCALE_OF_BABY, SCALE_OF_ADULT);
    float relativeHeadSize = A / (scale + B);
    return relativeHeadSize;
  }

  public int getNumberOfTailSegments() {return  NUMBER_OF_TAIL_SEGMENTS;}
  public int getNumberOfNeckSegments() {return  NUMBER_OF_NECK_SEGMENTS;}
  public int getNumberOfWingFingers() {return  NUMBER_OF_WING_FINGERS;}

  private float BODY_HEIGHT_BC = 1.5F;
  private float BODY_HALF_HEIGHT_BC = BODY_HEIGHT_BC / 2.0F;
  private float BODY_HALF_LENGTH_BC = 2.0F;
  private float SADDLE_THICKNESS_BC = 2.0F/16.0F;
  private float BELLY_TO_GROUND_WHEN_SITTING_BC = 0.65F;
  private float BELLY_TO_GROUND_WHEN_STANDING_BC = 1.25F;

  private float DESIRED_BODY_HEIGHT_WC = 1.5F * 1.6F;
  private float RENDER_SCALE_FACTOR = DESIRED_BODY_HEIGHT_WC / BODY_HEIGHT_BC;

  private float CONVERSION_FACTOR_MC_TO_BC = 1.0F/16.0F;  // is the vanilla value i.e. 0.0625F

  // relative to body origin:
  // 0.25 metres above the top of the back, 1.5 metres forward from body centre
  private Vec3d ROTATION_POINT_FOR_BODY_PITCH_BC = new Vec3d(0, BODY_HALF_HEIGHT_BC + 0.25F, 1.5F);
  // the origin of the head relative to the body origin
  private Vec3d HEAD_OFFSET_FROM_BODY_BC = new Vec3d(0, 1.0F, 4.0F);
  private Vec3d HEAD_OFFSET_FROM_ROTATION_POINT_BC = HEAD_OFFSET_FROM_BODY_BC.subtract(ROTATION_POINT_FOR_BODY_PITCH_BC);


  private int NUMBER_OF_NECK_SEGMENTS = 7;
  private int NUMBER_OF_WING_FINGERS = 4;
  private int NUMBER_OF_TAIL_SEGMENTS = 12;

}
