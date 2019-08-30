package com.TheRPGAdventurer.ROTD.client.model;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.util.math.MathX;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;

/**
 * Created by TGG on 28/08/2019.
 *
 * AnimatedTexture performs simple animation on a texture to be used for rendering.
 * Typical usage:
 * 1) Create AnimatedTexture with the path to the png file for the animation.
 * The frames in the image must be oriented vertically, and the number of frames is automatically calculated
 *   by assuming that the frame height equals the frame width.
 * For example, an image that is 16 pixels wide by 64 pixels tall has four frames (64/16)
 *   Partial frames are ignored eg 16 pixels wide by 40 pixels tall is two frames and the bottom 8 rows are ignored
 * Maximum number of frames is 20.
 * 2) Use setAnimation to set the time per frame and whether interpolation is used.  (If setAnimation is not called,
 *     there is no animation at all).
 *  ticksPerFrame is constant for all frames and the animation cycles in a continuous loop.
 *  if interpolation is true, the image is linearly interpolated between frames
 *  eg if ticksPerFrame = 4, and we are on tick 3 between frame A and frame B, then the image is 1/4 * frameA + 3/4 * frameB
 * 3) call load() to load the animation from disk
 * 4) before rendering:
 *    a) call updateAnimation() with the ticks-elapsed-since-start.  The origin is arbitrary.
 *   then
 *    b) call bindTexture()
 *
 * If the texture is invalid and bindTexture() is called, the texture is set to a blank texture (black and pink check)
 */
public class AnimatedTexture {

  public AnimatedTexture(ResourceLocation textureRL) {
    this.textureRL = textureRL;
  }

  /**
   * Set the parameters to be used for the animation
   * @param ticksPerFrame the number of ticks to spend on each frame
   * @param interpolate if true: linearly interpolate between frames
   */
  public void setAnimation(int ticksPerFrame, boolean interpolate) {
    if (ticksPerFrame < 1) return;
    this.ticksPerFrame = ticksPerFrame;
    this.interpolate = interpolate;
  }

  /**
   * Load the animation frames from the resource
   * @param resourceManager
   */
  public void load(IResourceManager resourceManager) {
    release();
    initialised = false;

    try {
      IResource iresource = resourceManager.getResource(textureRL);
      BufferedImage bufferedimage = TextureUtil.readBufferedImage(iresource.getInputStream());
      int width = bufferedimage.getWidth();
      int height = bufferedimage.getHeight();

      int framecount = height / width;
      if (framecount <= 0 || framecount > MAX_FRAMES_ALLOWED) {
        throw new IllegalArgumentException("Animated Texture should contain one or more vertical square frames (max 20).  Found instead: " +
                                           width + " pixels wide x " + height + " pixels high = " + framecount + " frames.");
      }
      frameWidth = width;
      frameHeight = frameWidth;
      numberOfFrames = framecount;

      frameData = new int[frameWidth * frameHeight * numberOfFrames];
      bufferedimage.getRGB(0, 0, frameWidth, frameHeight * numberOfFrames, frameData, 0, frameWidth);

      currentFrame = new DynamicTexture(frameWidth, frameHeight);
    } catch (Exception e) {
      String cause = e.toString();
      DragonMounts.loggerLimit.warn_once(String.format("Error while loading texture %s: %s", textureRL, cause));
    }
    initialised = true;
  }

  /**
   * Update the texture to the appropriate frame for the given tickCount
   * The tickCount has an arbitrary origin
   * @param tickCount ticks since minecraft started
   */
  public void updateAnimation(long tickCount) {
    if (!initialised) return;
    if (currentFrameIsValid && tickCount == currentFrameTicks) return;
    if (ticksPerFrame == TICKS_PER_FRAME_INFINITE) {
      setFrame(0);
    } else {
      long ticksPerCycle = numberOfFrames * ticksPerFrame;
      long cycleTicks = tickCount % ticksPerCycle;
      if (!interpolate) {
        long nearestFrame = Math.round(cycleTicks / (double)ticksPerFrame) % numberOfFrames;
        setFrame((int)nearestFrame);
      } else {
        int frameBefore = (int)Math.floorDiv(cycleTicks, ticksPerFrame);
        int frameAfter = (frameBefore + 1) % numberOfFrames;
        float frameFraction = (cycleTicks - frameBefore * ticksPerFrame) / (float)ticksPerFrame;
        interpolateFrame(frameBefore, frameAfter, frameFraction);
      }
    }

    currentFrameIsValid = true;
    currentFrameTicks = tickCount;
  }

  /**
   * Bind the texture ready for rendering
   * If the texture has an error, bind a pink+black check default texture instead
   */
  public void bindTexture() {
    if (!initialised) {
       bindMissingTexture();
    } else {
      GlStateManager.bindTexture(currentFrame.getGlTextureId());
    }
  }

  /**
   * Bind the missing texture (pink + black check)
   */
  public static void bindMissingTexture() {
    createMissingTexture();
    GlStateManager.bindTexture(missingTexture.getGlTextureId());
  }

  /**
   * Release the resources associated with this texture
   * Not essential provided we don't make too many textures
   */
  public void release() {
    if (currentFrame != null)  {
      currentFrame.deleteGlTexture();
      currentFrame = null;
    }
    if (missingTexture != null) {
      missingTexture.deleteGlTexture();
      missingTexture = null;
    }
  }

  private static void createMissingTexture() {
    if (missingTexture != null) return;
    missingTexture = new DynamicTexture(2, 2);
    int [] frame = missingTexture.getTextureData();
    frame[0] = 0xff000000; // black
    frame[1] = 0xffff00ff; // pink
    frame[2] = 0xff000000; // black
    frame[3] = 0xffff00ff; // pink
    missingTexture.updateDynamicTexture();
  }

  private void setFrame(int frameIndex) {
    if (!initialised) return;
    frameIndex = MathX.clamp(frameIndex, 0, numberOfFrames - 1);
    int pixelsPerFrame = frameWidth * frameHeight;
    System.arraycopy(frameData, frameIndex * pixelsPerFrame, currentFrame.getTextureData(), 0, pixelsPerFrame);
    currentFrame.updateDynamicTexture();
  }

  private void interpolateFrame(int frameBeforeIndex, int frameAfterIndex, float fractionOfFrame) {
    int pixelsPerFrame = frameWidth * frameHeight;
    int frameBeforeOffset = frameBeforeIndex * pixelsPerFrame;
    int frameAfterOffset = frameAfterIndex * pixelsPerFrame;
    for (int i = 0; i < pixelsPerFrame; ++i) {
      int colourBefore = frameData[frameBeforeOffset+i];
      int colourAfter = frameData[frameAfterOffset+i];
      int colourR = this.interpolateColor(fractionOfFrame, (colourBefore >> 16) & 255, (colourAfter >> 16) & 255);
      int colourG = this.interpolateColor(fractionOfFrame, (colourBefore >> 8) & 255, (colourAfter >> 8) & 255);
      int colourB = this.interpolateColor(fractionOfFrame, colourBefore & 255, colourAfter & 255);
      currentFrame.getTextureData()[i] = (colourBefore & 0xff000000) | (colourR << 16) | (colourG << 8) | colourB;
    }
    currentFrame.updateDynamicTexture();
  }

  private int interpolateColor(float cycleFraction, int previousValue, int nextValue)
  {
    return (int)(cycleFraction * nextValue + (1.0F - cycleFraction) * previousValue);
  }

  private static final int TICKS_PER_FRAME_INFINITE = -1;
  private final int MAX_FRAMES_ALLOWED = 20;  // arbitrary
  private ResourceLocation textureRL;
  private DynamicTexture currentFrame;
  private int numberOfFrames;
  private int [] frameData;
  private int frameWidth;
  private int frameHeight;
  private boolean initialised = false;
  private int ticksPerFrame = TICKS_PER_FRAME_INFINITE;
  private boolean interpolate = false;
  private long currentFrameTicks;
  private boolean currentFrameIsValid = false;
  private static DynamicTexture missingTexture;
}
