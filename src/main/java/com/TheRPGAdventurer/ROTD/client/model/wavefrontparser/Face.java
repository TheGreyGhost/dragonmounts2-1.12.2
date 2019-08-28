package com.TheRPGAdventurer.ROTD.client.model.wavefrontparser;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.util.vector.Vector3f;

public class Face {
  public Vertex[] vertices;
  public Vertex[] vertexNormals;
  public Vertex faceNormal;
  public TextureCoordinate[] textureCoordinates;

  public void addFaceForRender(BufferBuilder bufferBuilder) {
    addFaceForRender(bufferBuilder, 0.0005F);
  }

  private void storeVertexData(int[] faceDataOut, int storeIndex, int vertexIndex, Vector3f position, int shadeColor, TextureAtlasSprite sprite, BlockFaceUV faceUV) {
    int i = storeIndex * 7;
    faceDataOut[i] = Float.floatToRawIntBits(position.x);
    faceDataOut[i + 1] = Float.floatToRawIntBits(position.y);
    faceDataOut[i + 2] = Float.floatToRawIntBits(position.z);
    faceDataOut[i + 3] = shadeColor;
    faceDataOut[i + 4] = Float.floatToRawIntBits(sprite.getInterpolatedU((double) faceUV.getVertexU(vertexIndex) * .999 + faceUV.getVertexU((vertexIndex + 2) % 4) * .001));
    faceDataOut[i + 4 + 1] = Float.floatToRawIntBits(sprite.getInterpolatedV((double) faceUV.getVertexV(vertexIndex) * .999 + faceUV.getVertexV((vertexIndex + 2) % 4) * .001));

//    bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
//    bufferbuilder.pos(-0.5D, -0.25D, 0.0D).tex(0.0D, 1.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
//    bufferbuilder.pos(0.5D, -0.25D, 0.0D).tex(1.0D, 1.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
//    bufferbuilder.pos(0.5D, 0.75D, 0.0D).tex(1.0D, 0.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
//    bufferbuilder.pos(-0.5D, 0.75D, 0.0D).tex(0.0D, 0.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
//    tessellator.draw();
//
  }

  public void addFaceForRender(BufferBuilder bufferBuilder, float textureOffset) {
    if (faceNormal == null) {
      faceNormal = this.calculateFaceNormal();
    }

//    bufferBuilder.setNormal(faceNormal.x, faceNormal.y, faceNormal.z);

    float averageU = 0F;
    float averageV = 0F;

    if ((textureCoordinates != null) && (textureCoordinates.length > 0)) {
      for (int i = 0; i < textureCoordinates.length; ++i) {
        averageU += textureCoordinates[i].u;
        averageV += textureCoordinates[i].v;
      }

      averageU = averageU / textureCoordinates.length;
      averageV = averageV / textureCoordinates.length;
    }

    float offsetU, offsetV;

    for (int i = 0; i < vertices.length; ++i) {

      if ((textureCoordinates != null) && (textureCoordinates.length > 0)) {
        offsetU = textureOffset;
        offsetV = textureOffset;

        if (textureCoordinates[i].u > averageU) {
          offsetU = -offsetU;
        }
        if (textureCoordinates[i].v > averageV) {
          offsetV = -offsetV;
        }

//        bufferBuilder.addVertexWithUV(vertices[i].x, vertices[i].y, vertices[i].z, textureCoordinates[i].u + offsetU,
//                                    textureCoordinates[i].v + offsetV);
        bufferBuilder.pos(vertices[i].x, vertices[i].y, vertices[i].z)
                .tex(textureCoordinates[i].u + offsetU, textureCoordinates[i].v + offsetV)
                .normal(faceNormal.x, faceNormal.y, faceNormal.z)
                .endVertex();
      }
//      else
//      {
//        bufferBuilder.addVertex(vertices[i].x, vertices[i].y, vertices[i].z);
//      }
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