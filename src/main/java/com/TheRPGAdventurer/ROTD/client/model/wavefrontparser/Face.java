package com.TheRPGAdventurer.ROTD.client.model.wavefrontparser;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.util.vector.Vector3f;

import java.awt.geom.FlatteningPathIterator;

public class Face {
  public Vertex[] vertices;
  public Vertex[] vertexNormals;
  public Vertex faceNormal;
  public TextureCoordinate[] textureCoordinates;
  // just a default to prevent Crashes
  private static TextureCoordinate[] DEFAULT_TEXTURE_COORDINATES = {new TextureCoordinate(0, 0), new TextureCoordinate(0, 1), new TextureCoordinate(1, 0)};

  public void addFaceForRender(BufferBuilder bufferBuilder) throws IndexOutOfBoundsException {
    addFaceForRender(bufferBuilder, 0.0005F);
  }

//  private void storeVertexData(int[] faceDataOut, int storeIndex, Vertex vertex, TextureCoordinate textureCoordinate, Vertex vertexNormal) {
//    int i = storeIndex * 7;
//    faceDataOut[i] = Float.floatToRawIntBits(vertex.x);
//    faceDataOut[i + 1] = Float.floatToRawIntBits(vertex.y);
//    faceDataOut[i + 2] = Float.floatToRawIntBits(vertex.z);
//    faceDataOut[i + 4] = Float.floatToRawIntBits(textureCoordinate.u);
//    faceDataOut[i + 4 + 1] = Float.floatToRawIntBits(textureCoordinate.v);
//    faceDataOut[i+ 6] = Float.
//
////    bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
////    bufferbuilder.pos(-0.5D, -0.25D, 0.0D).tex(0.0D, 1.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
////    bufferbuilder.pos(0.5D, -0.25D, 0.0D).tex(1.0D, 1.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
////    bufferbuilder.pos(0.5D, 0.75D, 0.0D).tex(1.0D, 0.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
////    bufferbuilder.pos(-0.5D, 0.75D, 0.0D).tex(0.0D, 0.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
////    tessellator.draw();
////
//  }

  /**
   * Tesselate the face.  Nudge the texture by the given amount (prevents visual artefacts)
   * @param bufferBuilder
   * @param textureNudgeOffset
   */
  public void addFaceForRender(BufferBuilder bufferBuilder, float textureNudgeOffset) throws IndexOutOfBoundsException {
    if (faceNormal == null) {
      faceNormal = this.calculateFaceNormal();
    }

    if (textureCoordinates == null || textureCoordinates.length <= 0) throw new IndexOutOfBoundsException("obj file doesn't contain any texture coordinates");

    float averageU = 0F;
    float averageV = 0F;
    for (int i = 0; i < textureCoordinates.length; ++i) {
      averageU += textureCoordinates[i].u;
      averageV += textureCoordinates[i].v;
    }
    averageU = averageU / textureCoordinates.length;
    averageV = averageV / textureCoordinates.length;

    float offsetU, offsetV;
    for (int i = 0; i < vertices.length; ++i) {
      if ((textureCoordinates != null) && (textureCoordinates.length > 0)) {
        offsetU = (textureCoordinates[i].u > averageU) ? -textureNudgeOffset : textureNudgeOffset;
        offsetV = (textureCoordinates[i].v > averageV) ? -textureNudgeOffset : textureNudgeOffset;

        bufferBuilder.pos(vertices[i].x, vertices[i].y, vertices[i].z)
                .tex(textureCoordinates[i].u + offsetU, textureCoordinates[i].v + offsetV)
                .normal(faceNormal.x, faceNormal.y, faceNormal.z)
                .endVertex();
      }
    }
  }

  public Vertex calculateFaceNormal() {
    Vec3d v1 = new Vec3d(vertices[1].x - vertices[0].x, vertices[1].y - vertices[0].y, vertices[1].z - vertices[0].z);
    Vec3d v2 = new Vec3d(vertices[2].x - vertices[0].x, vertices[2].y - vertices[0].y, vertices[2].z - vertices[0].z);
    Vec3d normalVector = null;

    normalVector = v1.crossProduct(v2).normalize();

    return new Vertex((float) normalVector.x, (float) normalVector.y, (float) normalVector.z);
  }
}