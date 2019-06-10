package com.TheRPGAdventurer.ROTD.client.render.dragon.breathweaponFX;

import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.DragonBreathMode;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.nodes.BreathNodeFire;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.breath.nodes.BreathNodeP;
import com.TheRPGAdventurer.ROTD.objects.entity.entitytameabledragon.helper.util.RotatingQuad;
import com.TheRPGAdventurer.ROTD.util.debugging.testclasses.DebugBreathFXSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.Random;

/**
 * Created by TGG on 21/06/2015.
 * EntityFX that makes up the flame breath weapon; client side.
 *
 * Usage:
 * (1) create a new BreathFXFire using createBreathFXIce
 * (2) spawn it as per normal
 *
 */
public class BreathFXFire extends BreathFX {
  private final ResourceLocation fireballRL = new ResourceLocation("dragonmounts:entities/breathweapon/breath_fire");

  private final float SMOKE_CHANCE = 0.1f;
  private final float LARGE_SMOKE_CHANCE = 0.3f;

  private static final float MAX_ALPHA = 0.99F;

  /**
   * creates a single EntityFX from the given parameters.  Applies some random spread to direction.
   * @param world
   * @param x world [x,y,z] to spawn at (coordinates are the centre point of the fireball)
   * @param y
   * @param z
   * @param directionX initial world direction [x,y,z] - will be normalised.
   * @param directionY
   * @param directionZ
   * @param power the power of the ball
   * @param partialTicksHeadStart if spawning multiple EntityFX per tick, use this parameter to spread the starting
   *                              location in the direction
   * @return the new BreathFXFire
   */
  public static BreathFXFire createBreathFXFire(World world, double x, double y, double z,
                                                  double directionX, double directionY, double directionZ,
                                                  BreathNodeP.Power power,
                                                  float partialTicksHeadStart,
                                                  Optional<DebugBreathFXSettings> debugBreathFXSettings)
  {
    Vec3d direction = new Vec3d(directionX, directionY, directionZ).normalize();

    Random rand = new Random();
    BreathNodeP breathNode = new BreathNodeFire(power, DragonBreathMode.DEFAULT);
    breathNode.randomiseProperties(rand);
    Vec3d actualMotion = breathNode.getRandomisedStartingMotion(direction, rand);

    x += actualMotion.x * partialTicksHeadStart;
    y += actualMotion.y * partialTicksHeadStart;
    z += actualMotion.z * partialTicksHeadStart;
    BreathFXFire newBreathFXFire = new BreathFXFire(world, x, y, z, actualMotion, breathNode, debugBreathFXSettings);
    return newBreathFXFire;
  }

  private BreathFXFire(World world, double x, double y, double z, Vec3d motion,
                       BreathNodeP i_breathNode, Optional<DebugBreathFXSettings> debugBreathFXSettings) {
    super(world, x, y, z, motion.x, motion.y, motion.z, debugBreathFXSettings);

    breathNode = i_breathNode;
    particleGravity = Blocks.FIRE.blockParticleGravity;  /// arbitrary block!  maybe not even required.
    particleMaxAge = (int)breathNode.getMaxLifeTime(); // not used, but good for debugging
    this.particleAlpha = MAX_ALPHA;  // a value less than 1 turns on alpha blending

    //undo random velocity variation of vanilla EntityFX constructor
    motionX = motion.xCoord;
    motionY = motion.yCoord;
    motionZ = motion.zCoord;
    if (debugBreathFXSettings.isPresent() && debugBreathFXSettings.get().freezeMotion) {
      motionX = 0; motionY = 0; motionZ = 0;
    }

    // set the texture to the flame texture, which we have previously added using TextureStitchEvent
    TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(fireballRL.toString());
    setParticleTexture(sprite);
//    entityMoveAndResizeHelper = new EntityMoveAndResizeHelper(this);
  }

  /**
   * Returns 1, which means "use a texture from the blocks + items texture sheet"
   * @return
   */
  @Override
  public int getFXLayer() {
    return 1;
  }

  // this function is used by EffectRenderer.addEffect() to determine whether depthmask writing should be on or not.
  // BreathFXWater uses alphablending but we want depthmask writing on, otherwise translucent objects (such as water)
  //   render over the top of our breath.
  @Override
  public boolean isTransparent()
  {
    return true;
  }

  @Override
  public int getBrightnessForRender(float partialTick)
  {
    return 0xf000f0;
  }

  /**
   * Render the EntityFX onto the screen.
   * The EntityFX is rendered as a two-dimensional object (Quad) in the world (three-dimensional coordinates).
   *   The corners of the quad are chosen so that the EntityFX is drawn directly facing the viewer (or in other words,
   *   so that the quad is always directly face-on to the screen.)
   * In order to manage this, it needs to know two direction vectors:
   * 1) the 3D vector direction corresponding to left-right on the viewer's screen (edgeLRdirection)
   * 2) the 3D vector direction corresponding to up-down on the viewer's screen (edgeURdirection)
   * These two vectors are calculated by the caller.
   * For example, the top right corner of the quad on the viewer's screen is equal to the centre point of the quad (x,y,z)
   *   plus the edgeLRdirection vector multiplied by half the quad's width, plus the edgeUDdirection vector multiplied
   *   by half the quad's height.
   * NB edgeLRdirectionY is not provided because it's always 0, i.e. the top of the viewer's screen is always directly
   *    up so moving left-right on the viewer's screen doesn't affect the y coordinate position in the world
   * @param vertexBuffer
   * @param entity
   * @param partialTick
   * @param edgeLRdirectionX edgeLRdirection[XYZ] is the vector direction pointing left-right on the player's screen
   * @param edgeUDdirectionY edgeUDdirection[XYZ] is the vector direction pointing up-down on the player's screen
   * @param edgeLRdirectionZ edgeLRdirection[XYZ] is the vector direction pointing left-right on the player's screen
   * @param edgeUDdirectionX edgeUDdirection[XYZ] is the vector direction pointing up-down on the player's screen
   * @param edgeUDdirectionZ edgeUDdirection[XYZ] is the vector direction pointing up-down on the player's screen
   */
  @Override
  public void renderParticle(VertexBuffer vertexBuffer, Entity entity, float partialTick,
                             float edgeLRdirectionX, float edgeUDdirectionY, float edgeLRdirectionZ,
                             float edgeUDdirectionX, float edgeUDdirectionZ)
  {
    double minU = this.particleTexture.getMinU();
    double maxU = this.particleTexture.getMaxU();
    double minV = this.particleTexture.getMinV();
    double maxV = this.particleTexture.getMaxV();
    RotatingQuad tex = new RotatingQuad(minU, minV, maxU, maxV);
    Random random = new Random();
    if (random.nextBoolean()) {
      tex.mirrorLR();
    }
    tex.rotate90(random.nextInt(4));

    // "lightmap" changes the brightness of the particle depending on the local illumination (block light, sky light)
    //  in this example, it's held constant, but we still need to add it to each vertex anyway.
    int combinedBrightness = this.getBrightnessForRender(partialTick);
    int skyLightTimes16 = combinedBrightness >> 16 & 65535;
    int blockLightTimes16 = combinedBrightness & 65535;

    double scale = 0.1F * this.particleScale;
    final double scaleLR = scale;
    final double scaleUD = scale;
    double x = this.prevPosX + (this.posX - this.prevPosX) * partialTick - interpPosX;
    double y = this.prevPosY + (this.posY - this.prevPosY) * partialTick - interpPosY + this.height / 2.0F;
    // centre of rendering is now y midpt not ymin
    double z = this.prevPosZ + (this.posZ - this.prevPosZ) * partialTick - interpPosZ;

    vertexBuffer.pos(x - edgeLRdirectionX * scaleLR - edgeUDdirectionX * scaleUD,
                     y - edgeUDdirectionY * scaleUD,
                     z - edgeLRdirectionZ * scaleLR - edgeUDdirectionZ * scaleUD)
                .tex(tex.getU(0), tex.getV(0))
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
            .lightmap(skyLightTimes16, blockLightTimes16)
                .endVertex();
    vertexBuffer.pos(x - edgeLRdirectionX * scaleLR + edgeUDdirectionX * scaleUD,
                                 y + edgeUDdirectionY * scaleUD,
                                 z - edgeLRdirectionZ * scaleLR + edgeUDdirectionZ * scaleUD)
            .tex(tex.getU(1), tex.getV(1))
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
            .lightmap(skyLightTimes16, blockLightTimes16)
            .endVertex();
    vertexBuffer.pos(x + edgeLRdirectionX * scaleLR + edgeUDdirectionX * scaleUD,
            y + edgeUDdirectionY * scaleUD,
            z + edgeLRdirectionZ * scaleLR + edgeUDdirectionZ * scaleUD)
            .tex(tex.getU(2), tex.getV(2))
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
            .lightmap(skyLightTimes16, blockLightTimes16)
            .endVertex();
    vertexBuffer.pos(x + edgeLRdirectionX * scaleLR - edgeUDdirectionX * scaleUD,
            y - edgeUDdirectionY * scaleUD,
            z + edgeLRdirectionZ * scaleLR - edgeUDdirectionZ * scaleUD)
            .tex(tex.getU(3),  tex.getV(3))
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
            .lightmap(skyLightTimes16, blockLightTimes16)
            .endVertex();
  }

  /** call once per tick to update the EntityFX size, position, collisions, etc
   */
  @Override
  public void onUpdate() {
    final float YOUNG_AGE = 0.25F;
    final float OLD_AGE = 0.75F;

    float lifetimeFraction = breathNode.getLifetimeFraction();
    if (lifetimeFraction < YOUNG_AGE) {
      particleAlpha = MAX_ALPHA;
    } else if (lifetimeFraction < OLD_AGE) {
      particleAlpha = MAX_ALPHA;
    } else {
      particleAlpha = MAX_ALPHA * (1 - lifetimeFraction);
    }

    final float PARTICLE_SCALE_RELATIVE_TO_SIZE = 5.0F; // factor to convert from particleSize to particleScale
    float currentParticleSize = breathNode.getCurrentRenderDiameter();
    particleScale = PARTICLE_SCALE_RELATIVE_TO_SIZE * currentParticleSize;

    // spawn a smoke trail after some time
    if (SMOKE_CHANCE != 0 && rand.nextFloat() < lifetimeFraction && rand.nextFloat() <= SMOKE_CHANCE) {
      world.spawnParticle(getSmokeParticleID(), posX, posY, posZ, motionX * 0.5, motionY * 0.5, motionZ * 0.5);
    }

    // smoke / steam when hitting water.  node is responsible for aging to death
    if (isInWater()) {
      world.spawnParticle(getSmokeParticleID(), posX, posY, posZ, 0, 0, 0);
    }

    float newAABBDiameter = breathNode.getCurrentAABBcollisionSize();

    prevPosX = posX;
    prevPosY = posY;
    prevPosZ = posZ;
    moveAndResizeParticle(motionX, motionY, motionZ, newAABBDiameter, newAABBDiameter);

    if (isCollided && onGround) {
        motionY -= 0.01F;         // ensure that we hit the ground next time too
    }
    breathNode.updateAge(this);
    particleAge = (int)breathNode.getAgeTicks();  // not used, but good for debugging
    if (breathNode.isDead()) {
      setExpired();
    }
  }

  protected EnumParticleTypes getSmokeParticleID() {
    if (LARGE_SMOKE_CHANCE != 0 && rand.nextFloat() <= LARGE_SMOKE_CHANCE) {
      return EnumParticleTypes.SMOKE_LARGE;
    } else {
      return EnumParticleTypes.SMOKE_NORMAL;
    }
  }

  /** Vanilla moveEntity does a pile of unneeded calculations, and also doesn't handle resize around the centre properly,
   * so replace with a custom one
   * @param dx the amount to move the entity in world coordinates [dx, dy, dz]
   * @param dy
   * @param dz
   */
  @Override
  public void moveEntity(double dx, double dy, double dz) {
    moveAndResizeParticle(dx, dy, dz, this.width, this.height);
  }

//  private EntityMoveAndResizeHelper entityMoveAndResizeHelper;
}
