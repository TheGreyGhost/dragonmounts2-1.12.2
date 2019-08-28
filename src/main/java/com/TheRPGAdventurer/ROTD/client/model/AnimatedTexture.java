package com.TheRPGAdventurer.ROTD.client.model;

import com.TheRPGAdventurer.ROTD.util.math.MathX;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.TextureMetadataSection;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

import java.awt.image.BufferedImage;
import java.io.Closeable;

/**
 * Created by TGG on 28/08/2019.
 */
public class AnimatedTexture {

  public AnimatedTexture(ResourceLocation textureRL) {
    this.textureRL = textureRL;
  }

  public void setAnimation(int ticksPerFrame, boolean interpolate) {
    if (ticksPerFrame < 1) return;
    this.ticksPerFrame = ticksPerFrame;
    this.interpolate = interpolate;
  }

  private static final int TICKS_PER_FRAME_INFINITE = -1;
  private ResourceLocation textureRL;
  private DynamicTexture currentFrame;
  private int numberOfFrames;
  private int maxFrames;
  private int [] frameData;
  private int frameWidth;
  private int frameHeight;
  private boolean initialised = false;
  private int ticksPerFrame = TICKS_PER_FRAME_INFINITE;
  private boolean interpolate = false;
  private long currentFrameTicks;
  private boolean currentFrameIsValid = false;

  private final int MAX_FRAMES_ALLOWED = 20;  // arbitrary

  public void load(IResourceManager resourceManager ) {
    release();
    initialised = false;

    try {
      IResource iresource = resourceManager.getResource(textureRL);
      BufferedImage bufferedimage = TextureUtil.readBufferedImage(iresource.getInputStream());
      int width = bufferedimage.getWidth();
      int height = bufferedimage.getHeight();

      int framecount = height / width;
      if (framecount <= 0 || framecount > MAX_FRAMES_ALLOWED) {
        throw new IllegalArgumentException("Animated Texture should contain of one or more vertical square frames (max 20).  Found instead: " +
                                           width + " pixels wide x " + height + " pixels high = " + framecount + " frames.");
      }
      frameWidth = width;
      frameHeight = frameWidth;
      numberOfFrames = framecount;

      frameData = new int[frameWidth * frameHeight * numberOfFrames];
      bufferedimage.getRGB(0, 0, frameWidth, frameHeight, frameData, 0, frameWidth);

      currentFrame = new DynamicTexture(frameWidth, frameHeight);
    } catch (Exception e) {

    }
    initialised = true;
  }

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
        float frameFraction = (cycleTicks - frameBefore * ticksPerFrame) / ticksPerFrame;
        interpolateFrame(frameBefore, frameAfter, frameFraction);
      }
    }

    currentFrameIsValid = true;
    currentFrameTicks = tickCount;
  }

  public void bindTexture() {
    if (!initialised) return;
    GlStateManager.bindTexture(currentFrame.getGlTextureId());
  }

  public void release() {
    if (currentFrame != null)  {
      currentFrame.deleteGlTexture();
    }
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
    int frameBeforeStart = frameBeforeIndex * pixelsPerFrame;
    int frameAfterStart = frameAfterIndex * pixelsPerFrame;
    for (int i = 0; i < pixelsPerFrame; ++i) {
      int colourBefore = frameData[frameBeforeStart+i];
      int colourAfter = frameData[frameAfterStart+i];
      int colourR = this.interpolateColor(fractionOfFrame, colourBefore >> 16 & 255, colourAfter >> 16 & 255);
      int colourG = this.interpolateColor(fractionOfFrame, colourBefore >> 8 & 255, colourAfter >> 8 & 255);
      int colourB = this.interpolateColor(fractionOfFrame, colourBefore & 255, colourAfter & 255);
      currentFrame.getTextureData()[i] = colourBefore & 0xff000000 | colourR << 16 | colourG << 8 | colourB;
    }
  }

  private int interpolateColor(float cycleFraction, int previousValue, int nextValue)
  {
    return (int)(cycleFraction * nextValue + (1.0F - cycleFraction) * previousValue);
  }
}
