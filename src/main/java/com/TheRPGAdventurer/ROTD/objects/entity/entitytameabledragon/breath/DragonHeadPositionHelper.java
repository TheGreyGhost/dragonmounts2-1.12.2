package com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath;

import com.TheRPGAdventurer.ROTD.client.model.dragon.DragonModel;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.DragonPhysicalModel;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.helper.SegmentSizePositionRotation;
import com.TheRPGAdventurer.ROTD.util.math.MathX;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Created by TGG on 24/06/2015.
 * Helps to specify the position & orientation of the head and neck segments.
 * Can be used on both client and server sides.
 *
 * Usage:
 * 1) Create an instance
 * 2) call calculateHeadAndNeck() to set the neck and head positions & angles
 * 3) call getThroatPosition() to get the [x,y,z] position of the throat, uses body pitch from the dragon.
 * 4) getHeadPositionSizeLocation, getNeckPositionSizeLocation, getNeckSegmentPositionSizeLocations are used to
 *    provide detailed position information for the model
 */
public class DragonHeadPositionHelper {

  public SegmentSizePositionRotation[] neckSegments;
  public SegmentSizePositionRotation head;
  public SegmentSizePositionRotation neck;  //not required?  not sure.

  private EntityTameableDragon dragon;
  private DragonPhysicalModel dragonPhysicalModel;
  private final int NUMBER_OF_NECK_SEGMENTS;

  public DragonHeadPositionHelper(EntityTameableDragon parent) {
    dragon = parent;
    dragonPhysicalModel = parent.getPhysicalModel();
    NUMBER_OF_NECK_SEGMENTS = dragonPhysicalModel.getNumberOfNeckSegments();
  }

  private final Vec3d FIRST_NECK_SEGMENT_OFFSET_FROM_BODY_MODEL_ORIGIN_MC = new Vec3d(0, 10, -16);
  private final Vec3d BODY_ORIGIN_TRANSLATE_FOR_BODY_PITCH = new Vec3d(0, 4, 8);

  /** calculate the position, rotation angles, and scale of the head and all segments in the neck
   * @param animRadians
   * @param flutter
   * @param sit
   * @param walk
   * @param speed
   * @param ground
   * @param netLookYaw
   * @param lookPitch
   * @param breath
   */
  public void calculateHeadAndNeck(float animRadians, float flutter, float sit, float walk, float speed, float ground, float netLookYaw, float lookPitch, float breath) {
    neckSegments = new SegmentSizePositionRotation[NUMBER_OF_NECK_SEGMENTS];
    head = new SegmentSizePositionRotation();
    SegmentSizePositionRotation currentSegment = new SegmentSizePositionRotation();

    final Vec3d firstNeckSegmentOrigin = FIRST_NECK_SEGMENT_OFFSET_FROM_BODY_MODEL_ORIGIN_MC.add(BODY_ORIGIN_TRANSLATE_FOR_BODY_PITCH);
    currentSegment.rotationPointX = (float)firstNeckSegmentOrigin.x;
    currentSegment.rotationPointY = (float)firstNeckSegmentOrigin.y;
    currentSegment.rotationPointZ = (float)firstNeckSegmentOrigin.z;

    currentSegment.rotateAngleX = 0;
    currentSegment.rotateAngleY = 0;
    currentSegment.rotateAngleZ = 0;

    dragon.getAnimator().setLook(netLookYaw, lookPitch);
    double health = dragon.getHealthRelative();
    for (int i = 0; i < NUMBER_OF_NECK_SEGMENTS; i++) {
      float vertMulti = (i + 1) / (float)NUMBER_OF_NECK_SEGMENTS;

      float baseRotX = MathX.cos((float) i * 0.45f + animRadians) * 0.15f;
      if(!dragon.isUsingBreathWeapon()) baseRotX *= MathX.lerp(0.2f, 1, flutter);
      baseRotX *= MathX.lerp(1, 0.2f, sit);
      float ofsRotX = MathX.sin(vertMulti * MathX.PI_F * 0.9f) * 0.63f; // 0.9

      // basic up/down movement
      currentSegment.rotateAngleX = baseRotX;
      // reduce rotation when on ground
      currentSegment.rotateAngleX *= MathX.slerp(1f, 0.5f, walk); // 1 != 0.8f
      // flex neck down when hovering
      currentSegment.rotateAngleX += (1 - speed) * vertMulti;
      // lower neck on low health
      currentSegment.rotateAngleX -= MathX.lerp(0, ofsRotX, ground * health);
      // use looking yaw
      currentSegment.rotateAngleY = MathX.toRadians(netLookYaw) * vertMulti * speed;

      // update size (scale)
      currentSegment.scaleX = currentSegment.scaleY = MathX.lerp(1.6f, 1, vertMulti);
      currentSegment.scaleZ = 0.6f;

      neckSegments[i] = currentSegment.getCopy();

      // move next segment behind the current one
      float neckSize = DragonModel.NECK_SIZE * currentSegment.scaleZ - DragonModel.NECK_SEGMENT_OVERLAP;
      currentSegment.rotationPointX -= MathX.sin(currentSegment.rotateAngleY) * MathX.cos(currentSegment.rotateAngleX) * neckSize;
      currentSegment.rotationPointY += MathX.sin(currentSegment.rotateAngleX) * neckSize;
      currentSegment.rotationPointZ -= MathX.cos(currentSegment.rotateAngleY) * MathX.cos(currentSegment.rotateAngleX) * neckSize;
    }
    neck = currentSegment.getCopy();  // might not be required, not sure, so do it anyway...

    final float HEAD_TILT_DURING_BREATH = -0.1F;
    head.rotateAngleX = MathX.toRadians(lookPitch) + (1 - speed); // + breath * HEAD_TILT_DURING_BREATH
    head.rotateAngleY = currentSegment.rotateAngleY;
    head.rotateAngleZ = currentSegment.rotateAngleZ * 0.2f;

    head.rotationPointX = currentSegment.rotationPointX;
    head.rotationPointY = currentSegment.rotationPointY;
    head.rotationPointZ = currentSegment.rotationPointZ;
  }

  public SegmentSizePositionRotation getHeadPositionSizeLocation() {
    if (head == null) {
      throw new IllegalStateException("DragonHeadPositionHelper.calculateHeadAndNeck() must be called first");
    }
    return head.getCopy();
  }

  public SegmentSizePositionRotation getNeckPositionSizeLocation() {
    if (neck == null) {
      throw new IllegalStateException("DragonHeadPositionHelper.calculateHeadAndNeck() must be called first");
    }
    return neck.getCopy();
  }

  public SegmentSizePositionRotation[] getNeckSegmentPositionSizeLocations() {
    if (neckSegments == null) {
      throw new IllegalStateException("DragonHeadPositionHelper.calculateHeadAndNeck() must be called first");
    }
    SegmentSizePositionRotation[] retval = new SegmentSizePositionRotation[neckSegments.length];
    for (int i = 0; i < neckSegments.length; ++i) {
      retval[i] = neckSegments[i].getCopy();
    }
    return retval;
  }

  /** Calculate the position of the dragon's throat
   * Must have previously called calculateHeadAndNeck()
   * @return the world [x,y,z] of the throat
   */
  public Vec3d getThroatPosition() {
    if (head == null) {
      throw new IllegalStateException("DragonHeadPositionHelper.calculateHeadAndNeck() must be called first");
    }

    // algorithm is:
    // rotate and pitch the head->throat vector around the origin of the head
    // rotate the result around the body rotation point (for body pitch)
    // yaw this around the entity origin
    // add the entity origin in world coordinates

    float renderYawOffset = dragon.renderYawOffset;
    float ageScale = dragon.getAgeScale();

    Vec3d throatOffsetWC = dragonPhysicalModel.getThroatOffsetFromHeadOriginWC(ageScale, head.rotateAngleX, head.rotateAngleY);

    // the position of the head in MC is relative to the body pitch rotation point
    Vec3d headOffsetFromPitchOriginMC = new Vec3d(head.rotationPointX, head.rotationPointY, head.rotationPointZ);
    Vec3d headOffsetFromPitchOriginWC = dragonPhysicalModel.convertMCtoWC(ageScale, headOffsetFromPitchOriginMC);
    Vec3d headPlusThroatOffsetWC = headOffsetFromPitchOriginWC.add(throatOffsetWC);

    float bodyPitchRadians = (float)Math.toRadians(dragon.getBodyPitch());
    //pitch up to match the body
    headOffsetFromPitchOriginWC = headPlusThroatOffsetWC.rotatePitch(bodyPitchRadians);

    Vec3d rotationPointOffset = dragonPhysicalModel.offsetOfRotationPointFromEntityPosWC(ageScale, dragon.isSitting());
    Vec3d throatOffsetFromEntityPosWC = rotationPointOffset.add(headOffsetFromPitchOriginWC);
    throatOffsetFromEntityPosWC = throatOffsetFromEntityPosWC.rotateYaw((float)Math.toRadians(-renderYawOffset));

    Vec3d throatPosWC = dragon.getPositionVector().add(throatOffsetFromEntityPosWC);

    return throatPosWC;
  }

  /**Get the relative head size - depends on the age of the dragon.
   * Baby dragon has a relatively larger head compared to its body size (makes it look cuter)
   * @return the relative size of the head.  1.0 means normal size (adult).  Baby head is larger (eg 1.5)
   */
  public float getRelativeHeadSize() {
    return dragonPhysicalModel.getRelativeHeadSize(dragon.getAgeScale());
  }

  /**
   * rotate a vector around the X axis
   * @param angle in radians
   * @return
   */
  public Vec3d rotateX(Vec3d source, float angle) {
    float cosAngle = MathHelper.cos(angle);
    float sinAngle = MathHelper.sin(angle);
    double d0 = source.x;
    double d1 = source.y * (double)cosAngle + source.z * (double)sinAngle;
    double d2 = source.z * (double)cosAngle - source.y * (double)sinAngle;
    return new Vec3d(d0, d1, d2);
  }

  public Vec3d rotateY(Vec3d source, float angle) {
    float cosAngle = MathHelper.cos(angle);
    float sinAngle = MathHelper.sin(angle);
    double d0 = source.x * (double)cosAngle + source.z * (double)sinAngle;
    double d1 = source.y;
    double d2 = source.z * (double)cosAngle - source.x * (double)sinAngle;
    return new Vec3d(d0, d1, d2);
  }

  public Vec3d rotateZ(Vec3d source, float angle) {
    float cosAngle = MathHelper.cos(angle);
    float sinAngle = MathHelper.sin(angle);
    double d0 = source.x * (double)cosAngle + source.y * (double)sinAngle;
    double d1 = source.y * (double)cosAngle - source.x * (double)sinAngle;
    double d2 = source.z;
    return new Vec3d(d0, d1, d2);
  }

}
