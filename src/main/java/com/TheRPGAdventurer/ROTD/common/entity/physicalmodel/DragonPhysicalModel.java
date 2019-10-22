package com.TheRPGAdventurer.ROTD.common.entity.physicalmodel;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreedNew;
import com.TheRPGAdventurer.ROTD.common.entity.helper.DragonLifeStage;
import com.TheRPGAdventurer.ROTD.util.debugging.DebugSettings;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import net.minecraft.util.math.Vec3d;

/**
 * Created by TGG on 6/07/2019.
 * <p>
 * This class describes the physical configuration of the dragon
 * At the moment it only describes some parts which are necessary for code to know about
 * eg hitbox size, throat position, rider positions, banner positions
 *
 * It contains configuration information only, no state information.
 *
 * <p>
 * It's also just a "copy" of the DragonModel in easier-to-understand form
 * i.e. if DragonModel changes, this DragonPhysicalModel needs to be updated manually
 * <p>
 * The coordinate systems used are:
 * MC = model space coordinates (as used by DragonModel, ModelPart:
 * BC = Base space coordinates = model space scaled by 1/16, with x and y inverted, i.e. a 1:1 rendering of the Model Space, which
 * occurs when rendered with GLStateManager.scale(1.0))
 * WC = world space coordinates (i.e. full-size rendering using GLStateManager.scale(getAdultModelRenderScaleFactor()))
 * <p>
 * WC includes the dragon scale (0 to 1) where 1.0 is full-size dragon; scale is the vanilla scaling as used for EntityAgeable
 * <p>
 * The body origin in BC and WC is in the centre of the body, everything is referenced relative to that
 * The body origin in MC is not the same location; the MC origin is at [0, +0.75, 1.0] in BC
 * <p>
 * the y-axis is up/down (up is positive)
 * the x-axis is left/right
 * the z-axis is front/back (front = highest z)
 */
public class DragonPhysicalModel {

  public DragonPhysicalModel(DragonBreedNew dragonBreedNew, Modifiers modifiers) {
    this.dragonVariants = dragonBreedNew.getDragonVariants();
    this.modifiers = modifiers;
  }

  public float getHitboxWidthWC(float ageScale) {
    return ageScale * getHitboxWidthBC() * CONVERT_BC_TO_WC;
  }

  public float getHitboxHeightWC(float ageScale) {
    return ageScale * getHitboxHeightBC() * CONVERT_BC_TO_WC;
  }

  public float getHitboxWidthBC() {
    return 2.75F;
  }

  public float getHitboxHeightBC() {
    return 2.75F;
  }

  public float bodyOriginHeightAboveGroundBC(boolean sitting) {
    return BODY_HALF_HEIGHT_BC + (sitting ? BELLY_TO_GROUND_WHEN_SITTING_BC : BELLY_TO_GROUND_WHEN_STANDING_BC);
  }

  /**
   * Returns the origin of the entity's body from the entity origin (posX, posY, posZ), in WorldSpace coordinates
   *
   * @param ageScale the scale of the dragon (0 -> 1) , 1.0 is fully grown
   * @param sitting
   * @return
   */
  public Vec3d offsetOfOriginFromEntityPosWC(float ageScale, boolean sitting) {
    return new Vec3d(0, bodyOriginHeightAboveGroundBC(sitting), 0).scale(CONVERT_BC_TO_WC * ageScale);
  }

  /**
   * Returns the origin of the body pitch rotation point from the entity origin (posX, posY, posZ), in WorldSpace coordinates
   *
   * @param ageScale the scale of the dragon (0 -> 1) , 1.0 is fully grown
   * @param sitting
   * @return
   */
  public Vec3d offsetOfRotationPointFromEntityPosWC(float ageScale, boolean sitting) {
    Vec3d bodyOriginWC = offsetOfOriginFromEntityPosWC(ageScale, sitting);
    Vec3d rotationPointOffset = ROTATION_POINT_FOR_BODY_PITCH_BC.scale(CONVERT_BC_TO_WC * ageScale);
    return bodyOriginWC.add(rotationPointOffset);
  }

  /**
   * used when rendering; scale up the base model by this factor for a fully-grown adult
   *
   * @return the relative render scale factor (1.0 = no scaling)
   */
  public float getFullSizeRenderScaleFactor() {
    return CONVERT_BC_TO_WC;
  }

  /**
   * used when rendering; scale up the base model by this factor
   *
   * @param ageScale the scale of the dragon (0 -> 1) , 1.0 is fully grown
   * @return the render scale factor to use
   */
  public float getRenderScaleFactor(float ageScale) {
    return CONVERT_BC_TO_WC * ageScale;
  }

  /**
   * The factor used to convert from model space coordinates to worldspace coordinates
   *
   * @return
   */
  public float getConversionFactorMCtoWC(float ageScale) {
    return CONVERT_MC_TO_BC * CONVERT_BC_TO_WC * ageScale;
  }

  /**
   * gets the position offset to use for a passenger
   * i.e. the position of the rider relative to the entity posX, posY, posZ
   *
   * @param ageScale        the scale of the dragon (0 -> 1) , 1.0 is fully grown
   * @param pitchDegrees    the body pitch of the dragon (as per vanilla convention)
   * @param passengerNumber the number (0.. max) of the passenger
   * @return the [x, y, z] of the mounting position relative to the dragon entity origin [posX, posY, posZ]
   */
  public Vec3d getRiderPositionOffsetWC(float ageScale, float pitchDegrees, boolean sitting, int passengerNumber) {
    Vec3d riderPositionOffsetFromBO = getRiderPositionOffsetBC(passengerNumber);
    Vec3d riderPositionOffsetFromRP = riderPositionOffsetFromBO.subtract(ROTATION_POINT_FOR_BODY_PITCH_BC);
    Vec3d riderPitchedPositionFromRP = riderPositionOffsetFromRP.rotatePitch((float) Math.toRadians(pitchDegrees));
    Vec3d riderPitchedPositionFromBO = ROTATION_POINT_FOR_BODY_PITCH_BC.add(riderPitchedPositionFromRP);
    return offsetOfOriginFromEntityPosWC(ageScale, sitting)
            .add(riderPitchedPositionFromBO.scale(ageScale * CONVERT_BC_TO_WC));
  }

  /**
   * how many passengers can ride on this breed?
   *
   * @return
   */
  public int getMaxNumberOfPassengers(DragonLifeStage dragonLifeStage) {
    return 3;
  }

  /**
   * what is the eye height of the dragon?
   *
   * @param ageScale  the scale of the dragon (0 -> 1) , 1.0 is fully grown
   * @param isSitting true if the dragon is sitting
   * @return the height of the eye above the feet, in WorldSpace Coordinates (normal minecraft coordinates)
   * todo later: use the actual position of the head
   */
  public float getEyeHeightWC(float ageScale, boolean isSitting) {
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

    double offsetEyeFromBodyOriginWCY = ageScale * CONVERT_BC_TO_WC * HEAD_OFFSET_FROM_BODY_BC.y;
    double originBodyWCY = offsetOfOriginFromEntityPosWC(ageScale, isSitting).y;
    return (float) (offsetEyeFromBodyOriginWCY + originBodyWCY);
  }

  /**
   * Get the position of the eye relative to the entity Position
   *
   * @param ageScale           the scale of the dragon (0 -> 1) , 1.0 is fully grown
   * @param rotationYawDegrees the rotation (yaw) of the entity - as per vanilla value
   * @param pitchDegrees       of the entity - as per vanilla convention
   * @param isSitting
   * @return the world coordinates of the eye relative to the entity position [posX, posY, posZ]
   * todo: currently does an approximation for the head position.
   */
  public Vec3d getEyePositionWC(float ageScale, float rotationYawDegrees, float pitchDegrees, boolean isSitting) {
    Vec3d headPitchedOffsetFromRP = HEAD_OFFSET_FROM_ROTATION_POINT_BC.rotatePitch((float) Math.toRadians(pitchDegrees));
    Vec3d headPitchedOffsetFromBO = ROTATION_POINT_FOR_BODY_PITCH_BC.add(headPitchedOffsetFromRP);
    Vec3d headRotatedOffset = headPitchedOffsetFromBO.rotateYaw(
            -(float) Math.toRadians(rotationYawDegrees)).scale(ageScale * CONVERT_BC_TO_WC);
    return offsetOfOriginFromEntityPosWC(ageScale, isSitting).add(headRotatedOffset);
  }

  /**
   * Get the relative head size - depends on the age of the dragon.
   * Baby dragon has a relatively larger head compared to its body size (makes it look cuter)
   *
   * @param ageScale the scale of the dragon (0 -> 1) , 1.0 is fully grown
   * @return the relative size of the head.  1.0 means normal size (adult).  Baby head is larger (eg 1.5)
   */
  public float getRelativeHeadSize(float ageScale) {
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

    ageScale = MathX.clamp(ageScale, SCALE_OF_BABY, SCALE_OF_ADULT);
    float relativeHeadSize = A / (ageScale + B);
    return relativeHeadSize;
  }

  /**
   * Get the position of the throat relative to the origin of the head
   *
   * @param ageScale         the scale of the dragon (0 -> 1) , 1.0 is fully grown
   * @param headPitchRadians pitch of the head - as per vanilla convention, in radians not degrees
   * @param headYawRadians   yaw of the head - as per vanilla convention, in radians not degrees
   * @return the vector offset of the throat from the head centre point
   */
  public Vec3d getThroatOffsetFromHeadOriginWC(float ageScale, float headPitchRadians, float headYawRadians) {
    double scaleupFactor = ageScale * CONVERT_BC_TO_WC * getRelativeHeadSize(ageScale);
    Vec3d throatOffsetWC = THROAT_OFFSET_FROM_HEAD_CENTRE_BC.scale(scaleupFactor);
    Vec3d pitchedThroatOffsetWC = throatOffsetWC.rotatePitch(-headPitchRadians);
    Vec3d pitchedYawedThroatOffsetWC = pitchedThroatOffsetWC.rotateYaw(-headYawRadians);
    return pitchedYawedThroatOffsetWC;
  }

  /**
   * convert a vector in MC (Modelspace coordinates) into World coordinates (including inversion of the z, y axes)
   *
   * @param ageScale the scale of the dragon (0 -> 1) , 1.0 is fully grown
   * @param vecMC
   * @return
   */
  public Vec3d convertMCtoWC(float ageScale, Vec3d vecMC) {
    return new Vec3d(vecMC.x * CONVERT_MC_TO_WC * ageScale,
            vecMC.y * CONVERT_MC_TO_WC * ageScale * -1,
            vecMC.z * CONVERT_MC_TO_WC * ageScale * -1);
  }

  /**
   * How far does the dragon move for one full cycle of the walk animation
   *
   * @param ageScale the scale of the dragon (0 -> 1) , 1.0 is fully grown
   * @return the number of blocks moved in one full cycle of the walk legs
   */
  public float getMoveDistancePerWalkAnimationCycleWC(float ageScale) {
    if (DebugSettings.existsDebugParameter("walkdistancepercycle")) {
      return (float) DebugSettings.getDebugParameter("walkdistancepercycle");
    }
    return ageScale * MOVE_DISTANCE_PER_WALK_ANIMATION_CYCLE_BC * CONVERT_BC_TO_WC;
  }

  public int getNumberOfTailSegments() {
    return NUMBER_OF_TAIL_SEGMENTS;
  }

  public int getNumberOfNeckSegments() {
    return NUMBER_OF_NECK_SEGMENTS;
  }

  public int getNumberOfWingFingers() {
    return NUMBER_OF_WING_FINGERS;
  }

  /**
   * gets the position offset to use for a passenger at BASE size
   * i.e. the position of the rider relative to the origin of the body
   *
   * @param passengerNumber the number (0.. max) of the passenger
   * @return the [x, y, z] of the mounting position relative to the dragon origin; in Basespace Coordinates
   */
  private Vec3d getRiderPositionOffsetBC(int passengerNumber) {
    float yoffset = BODY_HALF_HEIGHT_BC + SADDLE_THICKNESS_BC;  // sit the rider directly on top of the saddle on the back

    // dragon position is the middle of the model and the saddle is on
    // the shoulders, so move player forwards on Z axis relative to the
    // dragon's rotation to fix that
    //

    Vec3d offset = new Vec3d(0, yoffset, 0); // default

    switch (passengerNumber) {
      case 0:
        offset = new Vec3d(0, yoffset, 1.1F);
        break;  // determined by tweaking
      case 1:
        offset = new Vec3d(+0.6F, yoffset, 0);
        break;
      case 2:
        offset = new Vec3d(-0.6F, yoffset, 0);
        break;
      default:
        DragonMounts.loggerLimit.error_once("Illegal passengerNumber:" + passengerNumber);
    }

    if (DebugSettings.isRiderPositionTweak()) {
      offset = DebugSettings.getRiderPositionOffset(passengerNumber, offset);
    }
    return offset;
  }
  private float MOVE_DISTANCE_PER_WALK_ANIMATION_CYCLE_BC = 4.2F;  //theoretical is 4.0 but 4.2 looks a bit better
  private float BODY_HEIGHT_BC = 1.5F;
  private float BODY_HALF_HEIGHT_BC = BODY_HEIGHT_BC / 2.0F;
  private float BODY_HALF_LENGTH_BC = 2.0F;
  private float SADDLE_THICKNESS_BC = 2.0F / 16.0F;
  private float BELLY_TO_GROUND_WHEN_SITTING_BC = 0.65F;
  private float BELLY_TO_GROUND_WHEN_STANDING_BC = 1.25F;

  private float DESIRED_BODY_HEIGHT_WC = 1.5F * 1.6F;
  private float CONVERT_BC_TO_WC = DESIRED_BODY_HEIGHT_WC / BODY_HEIGHT_BC;

  private float CONVERT_MC_TO_BC = 1.0F / 16.0F;  // is the vanilla value i.e. 0.0625F
  private float CONVERT_MC_TO_WC = CONVERT_MC_TO_BC * CONVERT_BC_TO_WC;

  // relative to body origin:
  // 0.25 metres above the top of the back, 1.5 metres forward from body centre
  private Vec3d ROTATION_POINT_FOR_BODY_PITCH_BC = new Vec3d(0, BODY_HALF_HEIGHT_BC + 0.25F, 1.5F);
  // the origin of the head relative to the body origin
  private Vec3d HEAD_OFFSET_FROM_BODY_BC = new Vec3d(0, 1.0F, 4.0F);
  private Vec3d HEAD_OFFSET_FROM_ROTATION_POINT_BC = HEAD_OFFSET_FROM_BODY_BC.subtract(ROTATION_POINT_FOR_BODY_PITCH_BC);

  private Vec3d THROAT_OFFSET_FROM_HEAD_CENTRE_BC = new Vec3d(0, -6.0 / 16.0, 12 / 16.0);
  //Throat position relative to head centrepoint is [y=10, z=-12] in MC -  to be 2 MC beyond main head

  private int NUMBER_OF_NECK_SEGMENTS = 7;
  private int NUMBER_OF_WING_FINGERS = 4;
  private int NUMBER_OF_TAIL_SEGMENTS = 12;

  private final DragonVariants dragonVariants;
  private final Modifiers modifiers;

}
