/*
 ** 2012 Januar 21
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.client.model.dragon.anim;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.client.model.dragon.DragonModel;
import com.TheRPGAdventurer.ROTD.common.entity.breath.DragonBreathHelperP;
import com.TheRPGAdventurer.ROTD.common.entity.breath.DragonHeadPositionHelper;
import com.TheRPGAdventurer.ROTD.common.entity.EntityTameableDragon;
import com.TheRPGAdventurer.ROTD.common.entity.helper.SegmentSizePositionRotation;
import com.TheRPGAdventurer.ROTD.common.entity.helper.util.Spline;
import com.TheRPGAdventurer.ROTD.common.entity.physicalmodel.DragonPhysicalModel;
import com.TheRPGAdventurer.ROTD.util.debugging.DebugSettings;
import com.TheRPGAdventurer.ROTD.util.math.Interpolation;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import net.minecraft.util.math.Vec3d;

/**
 * Animation control class to put useless reptiles in motion.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class DragonAnimator {

  public CircularBuffer yTrail = new CircularBuffer(8);
  public CircularBuffer yawTrail = new CircularBuffer(16);
  public CircularBuffer pitchTrail = new CircularBuffer(16);

  /*    // final X rotation angles for ground
      private float[] xGround = {0, 0, 0, 0};

      // X rotation angles for ground
      // 1st dim - front, hind
      // 2nd dim - thigh, crus, foot, toe
      private float[][] xGroundStand = {
              {0.8f, -1.5f, 1.3f, 0},
              {-0.3f, 1.5f, -0.2f, 0},
      };
      private float[][] xGroundSit = {
              {0.3f, -1.8f, 1.8f, 0},
              {-0.8f, 1.8f, -0.9f, 0},
      };

      // X rotation angles for walking
      // 1st dim - animation keyframe
      // 2nd dim - front, hind
      // 3rd dim - thigh, crus, foot, toe
      private float[][][] xGroundWalk = {{
              {0.4f, -1.4f, 1.3f, 0},    // move down and forward
              {0.1f, 1.2f, -0.5f, 0}     // move back
      }, {
              {1.2f, -1.6f, 1.3f, 0},    // move back
              {-0.3f, 2.1f, -0.9f, 0.6f} // move up and forward
      }, {
              {0.9f, -2.1f, 1.8f, 0.6f}, // move up and forward
              {-0.7f, 1.4f, -0.2f, 0}    // move down and forward
      }};

      // final X rotation angles for walking
      private float[] xGroundWalk2 = {0, 0, 0, 0};

      // Y rotation angles for ground, thigh only
      private float[] yGroundStand = {-0.25f, 0.25f};
      private float[] yGroundSit = {0.1f, 0.35f};
      private float[] yGroundWalk = {-0.1f, 0.1f};

      // final X rotation angles for air
      private float[] xAir;

      // X rotation angles for air
      // 1st dim - front, hind
      // 2nd dim - thigh, crus, foot, toe
      private float[][] xAirAll = {{0, 0, 0, 0}, {0, 0, 0, 0}};

      // Y rotation angles for air, thigh only
      private float[] yAirAll = {-0.1f, 0.1f};
  */
  public DragonAnimator(EntityTameableDragon dragon) {
    this.dragon = dragon;
    this.dragonPhysicalModel = dragon.getPhysicalModel();
    WING_FINGERS = dragonPhysicalModel.getNumberOfWingFingers();
    NECK_SEGMENTS = dragonPhysicalModel.getNumberOfNeckSegments();
    TAIL_SEGMENTS = dragonPhysicalModel.getNumberOfTailSegments();

    wingFingerRotateX = new float[WING_FINGERS];
    wingFingerRotateY = new float[WING_FINGERS];
    tailSegments = new SegmentSizePositionRotation[TAIL_SEGMENTS];
    dragonHeadPositionHelper = new DragonHeadPositionHelper(dragon);
  }

  public DragonHeadPositionHelper getDragonHeadPositionHelper() {
    return dragonHeadPositionHelper;
  }

  public void setPartialTicks(float partialTicks) {
    if (DebugSettings.isAnimationFrozen()) {
      partialTicks = DebugSettings.animationFrozenPartialTicks();
    }
    this.partialTicks = partialTicks;
  }

  public void setMovement(float moveDistanceBlocksX4, float moveSpeedBlocksPer200ms) {
    this.moveDistanceBlocksX4 = moveDistanceBlocksX4;
    this.moveSpeedBlocksPer200ms = moveSpeedBlocksPer200ms;
  }

  public void setLook(float lookYaw, float lookPitch) {
    // don't twist the neck
    this.lookYaw = MathX.clamp(lookYaw, -120, 120); // 120
    this.lookPitch = MathX.clamp(lookPitch, -90, 90); // 90
  }

  /**
   * Updates the dragon component parts - position, angles, scale. Called
   * every frame.
   */
  public void animate() {
    if (DebugSettings.isAnimationFrozen()) return;
    haveCalculatedAnimations = true;
    anim = animTimer.get(partialTicks);
    ground = groundTimer.get(partialTicks);
    flutter = FlutterTimer.get(partialTicks);
    walk = walkTimer.get(partialTicks);
    sit = sitTimer.get(partialTicks);
    bite = biteTimer.get(partialTicks);
    breath = breathTimer.get(partialTicks);
    speed = speedTimer.get(partialTicks);
    roar = roarTimer.get(partialTicks);

    animRadians = anim * MathX.PI_F * 2;
    cycleOfs = MathX.sin(animRadians - 1) + 1;

    // check if the wings are moving down and trigger the event
    boolean newWingsDown = cycleOfs > 1;
    if (newWingsDown && !wingsDown && flutter != 0) {
      dragon.onWingsDown(speed);
    }
    wingsDown = newWingsDown;

    cycleOfs = (cycleOfs * cycleOfs + cycleOfs * 2) * 0.05f;

    // reduce up/down amplitude
    cycleOfs *= MathX.lerp(0.5f, 1, flutter);
    cycleOfs *= MathX.lerp(1, 0.5f, ground);

    // updateFromAnimator body parts
    animHeadAndNeck();
    animTail();
    animWings();
    animLegs();
  }

  /**
   * Updates the animation state. Called on every tick.
   */
  public void tickingUpdate() {
    if (!dragon.isEgg()) {
      setOnGround(!dragon.isFlying());
    }

    if (DebugSettings.isAnimationFrozen()) {
      return;
    }

    // init trails
    if (initTrails) {
      yTrail.fill((float) dragon.posY);
      yawTrail.fill(dragon.renderYawOffset);
      pitchTrail.fill(getBodyPitch());
      initTrails = false;
    }

    // don't move anything during death sequence
    if (dragon.getHealth() <= 0) {
      animTimer.sync();
      groundTimer.sync();
      FlutterTimer.sync();
      biteTimer.sync();
      walkTimer.sync();
      sitTimer.sync();
      roarTimer.sync();
      return;
    }

    float speedMax = 0.05f;
    float speedEnt = (float) (dragon.motionX * dragon.motionX + dragon.motionZ * dragon.motionZ);
    float speedMulti = MathX.clamp(speedEnt / speedMax, 0, 1);

    // update main animation timer
    float animAdd = 0.035f;

    // depend timing speed on movement
    if (!onGround) {
      animAdd += (1 - speedMulti) * animAdd;
    }

    animTimer.add(animAdd);

    // update ground transition
    float groundVal = groundTimer.get();
    if (onGround) {
      groundVal *= 0.95f;
      groundVal += 0.08f;
    } else {
      groundVal -= 0.1f;
    }
    groundTimer.set(groundVal);

    // update Flutter transition
    boolean FlutterFlag = !onGround && (dragon.collided
            || dragon.motionY > -0.1 || speedEnt < speedMax);
    FlutterTimer.add(FlutterFlag ? 0.1f : -0.1f);

    // update walking transition
    boolean walkFlag = moveSpeedBlocksPer200ms > 0.1 && !dragon.isSitting();
    float walkVal = 0.1f;
    walkTimer.add(walkFlag ? walkVal : -walkVal);

    // update sitting transisiton
    float sitVal = sitTimer.get();
    sitVal += dragon.isSitting() ? 0.1f : -0.1f;
    sitVal *= 0.95f;
    sitTimer.set(sitVal);


    // update bite opening transition and breath transitions
    DragonBreathHelperP.BreathState breathState = dragon.getBreathHelperP().getCurrentBreathState();
    switch (breathState) {
      case IDLE: {  // breath is idle, handle bite attack
        int ticksSinceLastAttack = dragon.getTicksSinceLastAttack();
        final int JAW_OPENING_TIME_FOR_ATTACK = 5;
        boolean jawFlag = (ticksSinceLastAttack >= 0 && ticksSinceLastAttack < JAW_OPENING_TIME_FOR_ATTACK);
        biteTimer.add(jawFlag ? 0.2f : -0.2f);
        breathTimer.set(0.0F);

        int roarticks = dragon.roarTicks;
        final int JAW_OPENING_TIME_FOR_ROAR = 20;
        boolean jawFlag1 = (roarticks >= 0 && roarticks < JAW_OPENING_TIME_FOR_ROAR);
        roarTimer.add(jawFlag1 ? 0.2f : -0.2f);
        break;
      }
      case STARTING: {
        biteTimer.set(0.0F);
        breathTimer.set(dragon.getBreathHelperP().getBreathStateFractionComplete());
        break;
      }
      case STOPPING: {
        float breathStateFractionComplete = dragon.getBreathHelperP().getBreathStateFractionComplete();
        breathTimer.set(1.0F - breathStateFractionComplete);
        break;
      }
      case SUSTAIN: {
        breathTimer.set(1.0F);
        break;
      }
      default: {
        DragonMounts.loggerLimit.error_once("unexpected breathstate:" + breathState);
        return;
      }
    }

    // update speed transition
    boolean nearGround = dragon.getAltitude() < dragon.height * 2;
    boolean speedFlag = speedEnt > speedMax || onGround || nearGround || dragon.getPassengers().size() > 1 || dragon.isUnHovered() || (speedEnt > speedMax && dragon.boosting());
    float speedValue = 0.05f;
    speedTimer.add(speedFlag ? speedValue : -speedValue);

    // update trailers
    double yawDiff = dragon.renderYawOffset - prevRenderYawOffset;
    prevRenderYawOffset = dragon.renderYawOffset;

    // filter out 360 degrees wrapping
    if (yawDiff < 180 && yawDiff > -180) {
      yawAbs += yawDiff;
    }

    // TODO: where's yOffset?
    //yTrail.update(entity.posY - entity.yOffset);
    yTrail.update((float) dragon.posY);
    yawTrail.update((float) yawAbs);
    pitchTrail.update(getBodyPitch());
  }

  public float getFlutterTime() {
    return flutter;
  }

  public float getWalkTime() {
    return walk;
  }

  public Vec3d getThroatPosition() {
    if (!haveCalculatedAnimations) {
      animate();
    }
    return dragonHeadPositionHelper.getThroatPosition();
  }

  public SegmentSizePositionRotation getTail() {
    return tail.getCopy();
  }

  public SegmentSizePositionRotation[] getTailSegments() {
    SegmentSizePositionRotation[] retval = new SegmentSizePositionRotation[tailSegments.length];
    for (int i = 0; i < tailSegments.length; ++i) {
      retval[i] = tailSegments[i].getCopy();
    }
    return retval;
  }

  public void splineArraysOld(float x, boolean shift, float[] result, float[]... nodes) {
    // uncomment to disable interpolation
//        if (true) {
//            if (shift) {
//                System.arraycopy(nodes[(int) (x + 1) % nodes.length], 0, result, 0, nodes.length);
//            } else {
//                System.arraycopy(nodes[(int) x % nodes.length], 0, result, 0, nodes.length);
//            }
//            return;
//        }

    int i1 = (int) x % nodes.length;
    int i2 = (i1 + 1) % nodes.length;
    int i3 = (i1 + 2) % nodes.length;

    float[] a1 = nodes[i1];
    float[] a2 = nodes[i2];
    float[] a3 = nodes[i3];

    float xn = x % nodes.length - i1;

    if (shift) {
      Spline.interp(xn, result, a2, a3, a1, a2);
    } else {
      Spline.interp(xn, result, a1, a2, a3, a1);
    }
  }

  /**
   * Interpolate (cubic spline smoothing) between the keyFrames of the animation
   *
   * @param numberOfCycles the number of full cycles of the animation
   * @param phaseShift     if true, apply a phase shift to the cycle
   * @param result         the interpolated result
   * @param keyFrames
   */
  public void interpolateBetweenFrames(float numberOfCycles, boolean phaseShift, float[] result, float[]... keyFrames) {
    float cycleFraction = numberOfCycles % 1.0F;
    if (cycleFraction < 0) cycleFraction += 1.0F;
    float keyFrameIndex = cycleFraction * keyFrames.length;
    // find the frames that we are interpolating between (using splines)

    int frameIndex0 = (int) keyFrameIndex;
    frameIndex0 = frameIndex0 % keyFrames.length;   // in case keyFrameIndex == keyFrames.length
    int frameIndex1 = (frameIndex0 + 1) % keyFrames.length;
    int frameIndex2 = (frameIndex0 + 2) % keyFrames.length;

    float[] frame0 = keyFrames[frameIndex0];
    float[] frame1 = keyFrames[frameIndex1];
    float[] frame2 = keyFrames[frameIndex2];

    float fractionOfFrame = keyFrameIndex - frameIndex0;

    if (phaseShift) {
      Spline.interp(fractionOfFrame, result, frame1, frame2, frame0, frame1);
    } else {
      Spline.interp(fractionOfFrame, result, frame0, frame1, frame2, frame0);
    }
  }

  /**
   * Smoothed linear interpolation between a and b, using x
   * Performed on an array
   *
   * @param a
   * @param b
   * @param result return value
   * @param x
   */
  public void slerpArrays(float[] a, float[] b, float[] result, float x) {
    if (a.length != b.length || b.length != result.length) {
      throw new IllegalArgumentException();
    }

    if (x <= 0) {
      System.arraycopy(a, 0, result, 0, a.length);
      return;
    }
    if (x >= 1) {
      System.arraycopy(b, 0, result, 0, a.length);
      return;
    }

    for (int i = 0; i < result.length; i++) {
      result[i] = MathX.slerp(a[i], b[i], x);
    }
  }

  public float getBodyPitch() {
    return getBodyPitch(partialTicks);
  }

  public float getBodyPitch(float pt) {
    if (DebugSettings.existsDebugParameter("dragonpitch")) {
      return (float) DebugSettings.getDebugParameter("dragonpitch");
    }

    float pitchMovingMax = 90;
    float pitchMoving = MathX.clamp(yTrail.get(pt, 5, 0) * 10, -pitchMovingMax, pitchMovingMax);
    float pitchHoverMax = 60;
    boolean unhover = dragon.dragonInv.getStackInSlot(33) != null || dragon.dragonInv.getStackInSlot(34) != null
            || dragon.getPassengers().size() > 1 || dragon.isUnHovered() || dragon.boosting();
    return Interpolation.smoothStep(pitchHoverMax, unhover ? 0 : pitchMoving, speed);
  }

  public float getModelOffsetX() {
    return 0;
  }

  public float getModelOffsetY() {
    return -1.5f + (sit * 0.6f);
  }

  public float getModelOffsetZ() {
    return -1.5f;
  }

  public boolean isOnGround() {
    return onGround;
  }

  public void setOnGround(boolean onGround) {
    this.onGround = onGround;
  }

  public boolean isOpenJaw() {
    return openJaw;
  }

  public void setOpenJaw(boolean openJaw) {
    this.openJaw = openJaw;
  }

  public float getJawRotateAngleX() {
    return jawRotateAngleX;
  }

  public float getMoveDistanceBlocksX4() {
    return moveDistanceBlocksX4;
  }

  public float getSpeed() {
    return speed;
  }

  public float getAnimTime() {
    return anim;
  }

  public float getGroundTime() {
    return ground;
  }

  public float getSitTime() {
    return sit;
  }

  public float getCycleOfs() {
    return cycleOfs;
  }

  public float getWingFingerRotateX(int index) {
    return wingFingerRotateX[index];
  }

  public float getWingFingerRotateY(int index) {
    return wingFingerRotateY[index];
  }

  public float getWingArmRotateAngleX() {
    return wingArmRotateAngleX;
  }

  public float getWingArmRotateAngleY() {
    return wingArmRotateAngleY;
  }

  public float getWingArmRotateAngleZ() {
    return wingArmRotateAngleZ;
  }

  public float getWingArmPreRotateAngleX() {
    return wingArmPreRotateAngleX;
  }

  public float getWingForearmRotateAngleX() {
    return wingForearmRotateAngleX;
  }

  public float getWingForearmRotateAngleY() {
    return wingForearmRotateAngleY;
  }

  public float getWingForearmRotateAngleZ() {
    return wingForearmRotateAngleZ;
  }

  public float getLookYaw() {
    return lookYaw;
  }

  protected void animHeadAndNeck() {
    dragonHeadPositionHelper.calculateHeadAndNeck(animRadians, flutter, sit, walk, speed, ground, lookYaw, lookPitch, breath);
    final float BITE_ANGLE = 0.72F;
    final float ROAR_ANGLE = 0.58F;
    final float BREATH_ANGLE = 0.67F;
    jawRotateAngleX = (bite * BITE_ANGLE + breath * BREATH_ANGLE + roar * ROAR_ANGLE);
    jawRotateAngleX += (1 - MathX.sin(animRadians)) * 0.1f * flutter;
    if (DebugSettings.existsDebugParameter("dragonjawangle")) {
      jawRotateAngleX = (float) DebugSettings.getDebugParameter("dragonjawangle");
    }
  }

  protected void animWings() {
    // move wings slower while sitting
    float aSpeed = sit > 0 ? 0.6f : 1;

    // animation speeds
    float a1 = animRadians * aSpeed * 0.35f;
    float a2 = animRadians * aSpeed * 0.5f;
    float a3 = animRadians * aSpeed * 0.75f;

    if (ground < 1) {
      // Hovering
      wingArmFlutter[0] = 0.125f - MathX.cos(animRadians) * 0.2f;
      wingArmFlutter[1] = 0.25f;
      wingArmFlutter[2] = (MathX.sin(animRadians) + 0.125f) * 0.8f;

      wingForearmFlutter[0] = 0;
      wingForearmFlutter[1] = -wingArmFlutter[1] * 2;
      wingForearmFlutter[2] = -(MathX.sin(animRadians + 2) + 0.5f) * 0.75f;

      // gliding
      wingArmGlide[0] = -0.25f - MathX.cos(animRadians * 2) * MathX.cos(animRadians * 1.5f) * 0.04f;
      wingArmGlide[1] = 0.25f;
      wingArmGlide[2] = 0.35f + MathX.sin(animRadians) * 0.05f;

      wingForearmGlide[0] = 0;
      wingForearmGlide[1] = -wingArmGlide[1] * 2;
      wingForearmGlide[2] = -0.25f + (MathX.sin(animRadians + 2) + 0.5f) * 0.05f;
    }

    if (ground > 0) {
      // standing
      wingArmGround[0] = 0;
      wingArmGround[1] = 1.4f - MathX.sin(a1) * MathX.sin(a2) * 0.02f;
      wingArmGround[2] = 0.8f + MathX.sin(a2) * MathX.sin(a3) * 0.05f;

      // walking
      wingArmGround[1] += MathX.sin(moveDistanceBlocksX4 * 0.5f) * 0.02f * walk;
      wingArmGround[2] += MathX.cos(moveDistanceBlocksX4 * 0.5f) * 0.05f * walk;

      wingForearmGround[0] = 0;
      wingForearmGround[1] = -wingArmGround[1] * 2;
      wingForearmGround[2] = 0;
    }

    // interpolate between Fluttering and gliding
    slerpArrays(wingArmGlide, wingArmFlutter, wingArm, flutter);
    slerpArrays(wingForearmGlide, wingForearmFlutter, wingForearm, flutter);

    // interpolate between flying and grounded
    slerpArrays(wingArm, wingArmGround, wingArm, ground);
    slerpArrays(wingForearm, wingForearmGround, wingForearm, ground);

    // apply angles
    wingArmRotateAngleX = wingArm[0];
    wingArmRotateAngleY = wingArm[1];
    wingArmRotateAngleZ = wingArm[2];
    wingArmPreRotateAngleX = 1 - speed;
    wingForearmRotateAngleX = wingForearm[0];
    wingForearmRotateAngleY = wingForearm[1];
    wingForearmRotateAngleZ = wingForearm[2];

    // interpolate between folded and unfolded wing angles
    float[] yFold = new float[]{2.7f, 2.8f, 2.9f, 3.0f};
    float[] yUnfold = new float[]{0.1f, 0.9f, 1.7f, 2.5f};

    // set wing finger angles
    float rotX = 0;
    float rotYOfs = MathX.sin(a1) * MathX.sin(a2) * 0.03f;
    float rotYMulti = 1;

    for (int i = 0; i < WING_FINGERS; i++) {
      wingFingerRotateX[i] = rotX += 0.005f; // reduce Z-fighting
      wingFingerRotateY[i] = MathX.slerp(yUnfold[i], yFold[i] + rotYOfs * rotYMulti, ground);
      rotYMulti -= 0.2f;
    }
  }

  protected void animTail() {
    tail.rotationPointX = 0;
    tail.rotationPointY = 16;
    tail.rotationPointZ = 62;
    tail.rotateAngleX = 0;
    tail.rotateAngleY = 0;
    tail.rotateAngleZ = 0;

    float rotXStand = 0;
    float rotYStand = 0;
    float rotXSit = 0;
    float rotYSit = 0;
    float rotXAir = 0;
    float rotYAir = 0;

    for (int i = 0; i < TAIL_SEGMENTS; i++) {
      float vertMulti = (i + 1) / (float) TAIL_SEGMENTS;

      // idle
      float amp = 0.1f + i / (TAIL_SEGMENTS * 2f);

      rotXStand = (i - TAIL_SEGMENTS * 0.6f) * -amp * 0.4f;
      rotXStand += (MathX.sin(animRadians * 0.2f) * MathX.sin(animRadians * 0.37f) * 0.4f * amp - 0.1f) * (1 - sit);
      rotXSit = rotXStand * 0.8f;

      rotYStand = (rotYStand + MathX.sin(i * 0.45f + animRadians * 0.5f)) * amp * 0.4f;
      rotYSit = MathX.sin(vertMulti * MathX.PI_F) * MathX.PI_F * 1.2f - 0.5f; // curl to the left

      rotXAir -= MathX.sin(i * 0.45f + animRadians) * 0.04f * MathX.lerp(0.3f, 1, flutter);

      // interpolate between sitting and standing
      tail.rotateAngleX = MathX.lerp(rotXStand, rotXSit, sit);
      tail.rotateAngleY = MathX.lerp(rotYStand, rotYSit, sit);

      // interpolate between flying and grounded
      tail.rotateAngleX = MathX.lerp(rotXAir, tail.rotateAngleX, ground);
      tail.rotateAngleY = MathX.lerp(rotYAir, tail.rotateAngleY, ground);

      // body movement
      float angleLimit = 160 * vertMulti;
      float pitchAngleLimit = 160 * vertMulti;
      float yawOfs = MathX
              .clamp((float) yawTrail.getChangeInValue(partialTicks, 0, i + 1) * 2, -angleLimit, angleLimit);
      float pitchOfs = MathX
              .clamp((float) pitchTrail.getChangeInValue(partialTicks, 0, i + 1) * 2, -pitchAngleLimit, pitchAngleLimit);

      tail.rotateAngleX += MathX.toRadians(pitchOfs);
      tail.rotateAngleX -= (1 - speed) * vertMulti * 2;
      tail.rotateAngleY += MathX.toRadians(180 - yawOfs);

      // update scale
      float neckScale = MathX.lerp(1.5f, 0.3f, vertMulti);
      tail.setScale(neckScale);

      // update proxy
      tailSegments[i] = tail.getCopy();

      // move next proxy behind the current one
      float tailSize = DragonModel.TAIL_SIZE * tail.scaleZ - 0.7f;
      tail.rotationPointY += MathX.sin(tail.rotateAngleX) * tailSize;
      tail.rotationPointZ -= MathX.cos(tail.rotateAngleY) * MathX.cos(tail.rotateAngleX) * tailSize;
      tail.rotationPointX -= MathX.sin(tail.rotateAngleY) * MathX.cos(tail.rotateAngleX) * tailSize;
    }
  }

  protected void animLegs() {
    // do nothing - server doesn't need any of these positions so the DragonModel can do it all
  }
  private final DragonHeadPositionHelper dragonHeadPositionHelper;
  // entity parameters
  private final EntityTameableDragon dragon;
  private final int WING_FINGERS;
  private final int NECK_SEGMENTS;
  private final int TAIL_SEGMENTS;
  private SegmentSizePositionRotation[] tailSegments;
  private SegmentSizePositionRotation tail = new SegmentSizePositionRotation();  //not required?  not sure.
  private boolean haveCalculatedAnimations = false;
  private float partialTicks;
  private float moveDistanceBlocksX4;       // the distance moved along the ground in blocks, multiplied by four
  private float moveSpeedBlocksPer200ms;  // the ground movement speed, in blocks per 200 milliseconds
  private float lookYaw;
  private float lookPitch;
  private double prevRenderYawOffset;
  private double yawAbs;
  // timing vars
  private float animRadians;  // animation timer converted to radians
  private float cycleOfs;
  private float anim;
  private float ground;
  private float flutter;
  private float walk;
  private float sit;
  private float bite;

//    public Vec3d getRiderPosition()  //todo update various position helpers such as rider location, etc
//    {
//        return new Vec3d(00)
//    }
  private float breath;
  private float speed;
  private float roar;
  // timing interp vars
  private TickFloat animTimer = new TickFloat();
  private TickFloat groundTimer = new TickFloat(1).setLimit(0, 1);
  private TickFloat FlutterTimer = new TickFloat().setLimit(0, 1);
  private TickFloat walkTimer = new TickFloat().setLimit(0, 1);
  private TickFloat sitTimer = new TickFloat().setLimit(0, 1);
  private TickFloat biteTimer = new TickFloat().setLimit(0, 1);
  private TickFloat breathTimer = new TickFloat().setLimit(0, 1);
  private TickFloat speedTimer = new TickFloat(1).setLimit(0, 1);
  private TickFloat roarTimer = new TickFloat().setLimit(0, 1);
  // trails
  private boolean initTrails = true;
  // model flags
  private boolean onGround;
  private boolean openJaw;
  private boolean wingsDown;
  private float jawRotateAngleX;
  private float[] wingFingerRotateX;
  private float[] wingFingerRotateY;
  // animation parameters
  private float[] wingArm = new float[3];
  private float[] wingForearm = new float[3];
  private float[] wingArmFlutter = new float[3];
  private float[] wingForearmFlutter = new float[3];
  private float[] wingArmGlide = new float[3];
  private float[] wingForearmGlide = new float[3];
  private float[] wingArmGround = new float[3];
  private float[] wingForearmGround = new float[3];
  private float wingArmRotateAngleX;
  private float wingArmRotateAngleY;
  private float wingArmRotateAngleZ;
  private float wingArmPreRotateAngleX;
  private float wingForearmRotateAngleX;
  private float wingForearmRotateAngleY;
  private float wingForearmRotateAngleZ;
  private DragonPhysicalModel dragonPhysicalModel;


}