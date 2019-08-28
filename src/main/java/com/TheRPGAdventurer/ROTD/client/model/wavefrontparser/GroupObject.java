package com.TheRPGAdventurer.ROTD.client.model.wavefrontparser;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;


public class GroupObject {
  public String name;
  public ArrayList<Face> faces = new ArrayList<Face>();
  public int glDrawingMode;

  public GroupObject() {
    this("");
  }

  public GroupObject(String name) {
    this(name, GL11.GL_TRIANGLES);
  }

  public GroupObject(String name, int glDrawingMode) {
    this.name = name;
    this.glDrawingMode = glDrawingMode;
  }

  public void render(BufferBuilder bufferBuilder) throws IndexOutOfBoundsException{
    if (faces.size() > 0) {
      for (Face face : faces) {
        face.addFaceForRender(bufferBuilder);
      }
    }
  }
}