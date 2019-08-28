package com.TheRPGAdventurer.ROTD.client.model.wavefrontparser;

import com.TheRPGAdventurer.ROTD.DragonMounts;
import com.TheRPGAdventurer.ROTD.common.entity.breeds.DragonBreed;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL11;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Taken from an earlier version of Forge, simplified and ported to 1.12.2
 * Doesn't force you to use TextureAtlasSprite, unlike Forge OBJloader, so you can supply your own arbitrary texture before render, similar to TileEntitySpecialRenderer
 * <p>
 * Wavefront Object importer
 * Based heavily off of the specifications found at http://en.wikipedia.org/wiki/Wavefront_.obj_file
 */
public class WavefrontObject {

  public WavefrontObject(ResourceLocation resource) throws ModelFormatException {
    this.fileName = resource.toString();

    try {
      IResource res = Minecraft.getMinecraft().getResourceManager().getResource(resource);
      loadObjModel(res.getInputStream());
    } catch (IOException e) {
      throw new ModelFormatException("IO Exception reading model format", e);
    }
  }

  public WavefrontObject(String filename, InputStream inputStream) throws ModelFormatException {
    this.fileName = filename;
    loadObjModel(inputStream);
  }

  /**
   * Render the entire object; gets its own Tessellator, starts and finished draw
   */
  public void renderAll() {
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder bufferBuilder = tessellator.getBuffer();
    bufferBuilder.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_NORMAL);
    tessellateAll(bufferBuilder);
    tessellator.draw();
  }

  /**
   * Adds the quads to the buffer (doesn't start or end tesselating)
   * @param bufferBuilder
   */
  public void tessellateAll(BufferBuilder bufferBuilder)  {
    try {
      if (vertexCache == null) {
        generateVertexCache();
      }
      bufferBuilder.addVertexData(vertexCache);
    } catch (IndexOutOfBoundsException ioobe) {
      DragonMounts.loggerLimit.error_once("Error rendering WavefrontObject:" + ioobe.getMessage());
    }
  }

  /**
   * Adds the quads to the buffer (doesn't start or end tesselating)
   * @param bufferBuilder
   */
  public void tessellateAllUncached(BufferBuilder bufferBuilder)  {
    try {
      for (GroupObject groupObject : groupObjects) {
        groupObject.render(bufferBuilder);
      }
    } catch (IndexOutOfBoundsException ioobe) {
      DragonMounts.loggerLimit.error_once("Error rendering WavefrontObject:" + ioobe.getMessage());
    }
  }
  /**
   * Returns a default model that can be used if the desired model wasn't found
   * @return
   */
  public static WavefrontObject getDefaultFallback() {
    if (defaultFallBack == null ) {
      try {
        defaultFallBack = new WavefrontObject("WavefrontObjectDefault:default cube", IOUtils.toInputStream(defaultCube, StandardCharsets.UTF_8));
      } catch (Exception e) {
        DragonMounts.loggerLimit.error_once("Couldn't parse default WavefrontObject, this will probably cause a crash later" + e.getMessage());
      }

    }
    return defaultFallBack;
  }

  public ArrayList<Vertex> vertices = new ArrayList<>();
  public ArrayList<Vertex> vertexNormals = new ArrayList<>();
  public ArrayList<TextureCoordinate> textureCoordinates = new ArrayList<>();
  public ArrayList<GroupObject> groupObjects = new ArrayList<>();

  private static final String defaultCube =
          "g cube\n" +
                  "v 0.0 0.0 0.0\nv 0.0 0.0 1.0\nv 0.0 1.0 0.0\nv 0.0 1.0 1.0\nv 1.0 0.0 0.0\nv 1.0 0.0 1.0\nv 1.0 1.0 0.0\nv 1.0 1.0 1.0\n" +
                  "vt 0.0 0.0\nvt 1.0 0.0\nvt 0.0 1.0\nvt 1.0 1.0\n" +
                  "f 1/2 7/3 5/1\nf 1/2 3/4 7/3\nf 1/1 4/4 3/3\nf 1/1 2/2 4/4\nf 3/3 8/2 7/4\nf 3/3 4/1 8/2\n" +
                  "f 5/2 7/4 8/3\nf 5/2 8/3 6/1\nf 1/1 5/2 6/4\nf 1/1 6/4 2/3\nf 2/1 6/2 8/4\nf 2/1 8/4 4/3\n";
  private static Pattern vertexPattern = Pattern.compile("(v( (\\-){0,1}\\d+\\.\\d+){3,4} *\\n)|(v( (\\-){0,1}\\d+\\.\\d+){3,4} *$)");
  private static Pattern vertexNormalPattern = Pattern.compile("(vn( (\\-){0,1}\\d+\\.\\d+){3,4} *\\n)|(vn( (\\-){0,1}\\d+\\.\\d+){3,4} *$)");
  private static Pattern textureCoordinatePattern = Pattern.compile("(vt( (\\-){0,1}\\d+\\.\\d+){2,3} *\\n)|(vt( (\\-){0,1}\\d+\\.\\d+){2,3} *$)");
  private static Pattern face_V_VT_VN_Pattern = Pattern.compile("(f( \\d+/\\d+/\\d+){3,4} *\\n)|(f( \\d+/\\d+/\\d+){3,4} *$)");
  private static Pattern face_V_VT_Pattern = Pattern.compile("(f( \\d+/\\d+){3,4} *\\n)|(f( \\d+/\\d+){3,4} *$)");
  private static Pattern face_V_VN_Pattern = Pattern.compile("(f( \\d+//\\d+){3,4} *\\n)|(f( \\d+//\\d+){3,4} *$)");
  private static Pattern face_V_Pattern = Pattern.compile("(f( \\d+){3,4} *\\n)|(f( \\d+){3,4} *$)");
  private static Pattern groupObjectPattern = Pattern.compile("([go]( [\\w\\d]+) *\\n)|([go]( [\\w\\d]+) *$)");
  private static Matcher vertexMatcher, vertexNormalMatcher, textureCoordinateMatcher;
  private static Matcher face_V_VT_VN_Matcher, face_V_VT_Matcher, face_V_VN_Matcher, face_V_Matcher;
  private static Matcher groupObjectMatcher;
  private GroupObject currentGroupObject;
  private String fileName;
  private static WavefrontObject defaultFallBack;

  private int [] vertexCache;

  private void generateVertexCache() {
    int vertexDataSize = DefaultVertexFormats.POSITION_TEX_NORMAL.getIntegerSize() * vertices.size();
    BufferBuilder bufferBuilder = new BufferBuilder(vertexDataSize);
    bufferBuilder.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_NORMAL);
    tessellateAllUncached(bufferBuilder);
    bufferBuilder.finishDrawing();
    IntBuffer rawVertexData = bufferBuilder.getByteBuffer().asIntBuffer();
    vertexCache = new int[rawVertexData.remaining()];
    rawVertexData.get(vertexCache);
  }

  /***
   * Verifies that the given line from the model file is a valid vertex
   *
   * @param line the line being validated
   * @return true if the line is a valid vertex, false otherwise
   */
  private static boolean isValidVertexLine(String line) {
    if (vertexMatcher != null) {
      vertexMatcher.reset();
    }

    vertexMatcher = vertexPattern.matcher(line);
    return vertexMatcher.matches();
  }

  /***
   * Verifies that the given line from the model file is a valid vertex normal
   *
   * @param line the line being validated
   * @return true if the line is a valid vertex normal, false otherwise
   */
  private static boolean isValidVertexNormalLine(String line) {
    if (vertexNormalMatcher != null) {
      vertexNormalMatcher.reset();
    }

    vertexNormalMatcher = vertexNormalPattern.matcher(line);
    return vertexNormalMatcher.matches();
  }

  /***
   * Verifies that the given line from the model file is a valid texture coordinate
   *
   * @param line the line being validated
   * @return true if the line is a valid texture coordinate, false otherwise
   */
  private static boolean isValidTextureCoordinateLine(String line) {
    if (textureCoordinateMatcher != null) {
      textureCoordinateMatcher.reset();
    }

    textureCoordinateMatcher = textureCoordinatePattern.matcher(line);
    return textureCoordinateMatcher.matches();
  }

  /***
   * Verifies that the given line from the model file is a valid face that is described by vertices, texture coordinates, and vertex normals
   *
   * @param line the line being validated
   * @return true if the line is a valid face that matches the format "f v1/vt1/vn1 ..." (with a minimum of 3 points in the face, and a maximum of 4), false otherwise
   */
  private static boolean isValidFace_V_VT_VN_Line(String line) {
    if (face_V_VT_VN_Matcher != null) {
      face_V_VT_VN_Matcher.reset();
    }

    face_V_VT_VN_Matcher = face_V_VT_VN_Pattern.matcher(line);
    return face_V_VT_VN_Matcher.matches();
  }

  /***
   * Verifies that the given line from the model file is a valid face that is described by vertices and texture coordinates
   *
   * @param line the line being validated
   * @return true if the line is a valid face that matches the format "f v1/vt1 ..." (with a minimum of 3 points in the face, and a maximum of 4), false otherwise
   */
  private static boolean isValidFace_V_VT_Line(String line) {
    if (face_V_VT_Matcher != null) {
      face_V_VT_Matcher.reset();
    }

    face_V_VT_Matcher = face_V_VT_Pattern.matcher(line);
    return face_V_VT_Matcher.matches();
  }

  /***
   * Verifies that the given line from the model file is a valid face that is described by vertices and vertex normals
   *
   * @param line the line being validated
   * @return true if the line is a valid face that matches the format "f v1//vn1 ..." (with a minimum of 3 points in the face, and a maximum of 4), false otherwise
   */
  private static boolean isValidFace_V_VN_Line(String line) {
    if (face_V_VN_Matcher != null) {
      face_V_VN_Matcher.reset();
    }

    face_V_VN_Matcher = face_V_VN_Pattern.matcher(line);
    return face_V_VN_Matcher.matches();
  }

  /***
   * Verifies that the given line from the model file is a valid face that is described by only vertices
   *
   * @param line the line being validated
   * @return true if the line is a valid face that matches the format "f v1 ..." (with a minimum of 3 points in the face, and a maximum of 4), false otherwise
   */
  private static boolean isValidFace_V_Line(String line) {
    if (face_V_Matcher != null) {
      face_V_Matcher.reset();
    }

    face_V_Matcher = face_V_Pattern.matcher(line);
    return face_V_Matcher.matches();
  }

  /***
   * Verifies that the given line from the model file is a valid face of any of the possible face formats
   *
   * @param line the line being validated
   * @return true if the line is a valid face that matches any of the valid face formats, false otherwise
   */
  private static boolean isValidFaceLine(String line) {
    return isValidFace_V_VT_VN_Line(line) || isValidFace_V_VT_Line(line) || isValidFace_V_VN_Line(line) || isValidFace_V_Line(line);
  }

  /***
   * Verifies that the given line from the model file is a valid group (or object)
   *
   * @param line the line being validated
   * @return true if the line is a valid group (or object), false otherwise
   */
  private static boolean isValidGroupObjectLine(String line) {
    if (groupObjectMatcher != null) {
      groupObjectMatcher.reset();
    }

    groupObjectMatcher = groupObjectPattern.matcher(line);
    return groupObjectMatcher.matches();
  }

  private void loadObjModel(InputStream inputStream) throws ModelFormatException {
    BufferedReader reader = null;

    String currentLine = null;
    int lineCount = 0;

    try {
      reader = new BufferedReader(new InputStreamReader(inputStream));

      while ((currentLine = reader.readLine()) != null) {
        lineCount++;
        currentLine = currentLine.replaceAll("\\s+", " ").trim();

        if (currentLine.startsWith("#") || currentLine.length() == 0) {
          continue;
        } else if (currentLine.startsWith("v ")) {
          Vertex vertex = parseVertex(currentLine, lineCount);
          if (vertex != null) {
            vertices.add(vertex);
          }
        } else if (currentLine.startsWith("vn ")) {
          Vertex vertex = parseVertexNormal(currentLine, lineCount);
          if (vertex != null) {
            vertexNormals.add(vertex);
          }
        } else if (currentLine.startsWith("vt ")) {
          TextureCoordinate textureCoordinate = parseTextureCoordinate(currentLine, lineCount);
          if (textureCoordinate != null) {
            textureCoordinates.add(textureCoordinate);
          }
        } else if (currentLine.startsWith("f ")) {

          if (currentGroupObject == null) {
            currentGroupObject = new GroupObject("Default");
          }

          Face face = parseFace(currentLine, lineCount);

          if (face != null) {
            currentGroupObject.faces.add(face);
          }
        } else if (currentLine.startsWith("g ") | currentLine.startsWith("o ")) {
          GroupObject group = parseGroupObject(currentLine, lineCount);

          if (group != null) {
            if (currentGroupObject != null) {
              groupObjects.add(currentGroupObject);
            }
          }

          currentGroupObject = group;
        }
      }

      groupObjects.add(currentGroupObject);
    } catch (IOException e) {
      throw new ModelFormatException("IO Exception reading model format", e);
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        // hush
      }

      try {
        inputStream.close();
      } catch (IOException e) {
        // hush
      }
    }
  }

  private Vertex parseVertex(String line, int lineCount) throws ModelFormatException {
    Vertex vertex = null;

    if (isValidVertexLine(line)) {
      line = line.substring(line.indexOf(" ") + 1);
      String[] tokens = line.split(" ");

      try {
        if (tokens.length == 2) {
          return new Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]));
        } else if (tokens.length == 3) {
          return new Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
        }
      } catch (NumberFormatException e) {
        throw new ModelFormatException(String.format("Number formatting error at line %d", lineCount), e);
      }
    } else {
      throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Incorrect format");
    }

    return vertex;
  }

  private Vertex parseVertexNormal(String line, int lineCount) throws ModelFormatException {
    Vertex vertexNormal = null;

    if (isValidVertexNormalLine(line)) {
      line = line.substring(line.indexOf(" ") + 1);
      String[] tokens = line.split(" ");

      try {
        if (tokens.length == 3)
          return new Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
      } catch (NumberFormatException e) {
        throw new ModelFormatException(String.format("Number formatting error at line %d", lineCount), e);
      }
    } else {
      throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Incorrect format");
    }

    return vertexNormal;
  }

  private TextureCoordinate parseTextureCoordinate(String line, int lineCount) throws ModelFormatException {
    TextureCoordinate textureCoordinate = null;

    if (isValidTextureCoordinateLine(line)) {
      line = line.substring(line.indexOf(" ") + 1);
      String[] tokens = line.split(" ");

      try {
        if (tokens.length == 2)
          return new TextureCoordinate(Float.parseFloat(tokens[0]), 1 - Float.parseFloat(tokens[1]));
        else if (tokens.length == 3)
          return new TextureCoordinate(Float.parseFloat(tokens[0]), 1 - Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
      } catch (NumberFormatException e) {
        throw new ModelFormatException(String.format("Number formatting error at line %d", lineCount), e);
      }
    } else {
      throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Incorrect format");
    }

    return textureCoordinate;
  }

  private Face parseFace(String line, int lineCount) throws ModelFormatException {
    Face face = null;

    if (isValidFaceLine(line)) {
      face = new Face();

      String trimmedLine = line.substring(line.indexOf(" ") + 1);
      String[] tokens = trimmedLine.split(" ");
      String[] subTokens = null;

      if (tokens.length == 3) {
        if (currentGroupObject.glDrawingMode == -1) {
          currentGroupObject.glDrawingMode = GL11.GL_TRIANGLES;
        } else if (currentGroupObject.glDrawingMode != GL11.GL_TRIANGLES) {
          throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Invalid number of points for face (expected 4, found " + tokens.length + ")");
        }
      } else if (tokens.length == 4) {
        if (currentGroupObject.glDrawingMode == -1) {
          currentGroupObject.glDrawingMode = GL11.GL_QUADS;
        } else if (currentGroupObject.glDrawingMode != GL11.GL_QUADS) {
          throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Invalid number of points for face (expected 3, found " + tokens.length + ")");
        }
      }

      // f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3 ...
      if (isValidFace_V_VT_VN_Line(line)) {
        face.vertices = new Vertex[tokens.length];
        face.textureCoordinates = new TextureCoordinate[tokens.length];
        face.vertexNormals = new Vertex[tokens.length];

        for (int i = 0; i < tokens.length; ++i) {
          subTokens = tokens[i].split("/");

          face.vertices[i] = vertices.get(Integer.parseInt(subTokens[0]) - 1);
          face.textureCoordinates[i] = textureCoordinates.get(Integer.parseInt(subTokens[1]) - 1);
          face.vertexNormals[i] = vertexNormals.get(Integer.parseInt(subTokens[2]) - 1);
        }

        face.faceNormal = face.calculateFaceNormal();
      }
      // f v1/vt1 v2/vt2 v3/vt3 ...
      else if (isValidFace_V_VT_Line(line)) {
        face.vertices = new Vertex[tokens.length];
        face.textureCoordinates = new TextureCoordinate[tokens.length];

        for (int i = 0; i < tokens.length; ++i) {
          subTokens = tokens[i].split("/");

          face.vertices[i] = vertices.get(Integer.parseInt(subTokens[0]) - 1);
          face.textureCoordinates[i] = textureCoordinates.get(Integer.parseInt(subTokens[1]) - 1);
        }

        face.faceNormal = face.calculateFaceNormal();
      }
      // f v1//vn1 v2//vn2 v3//vn3 ...
      else if (isValidFace_V_VN_Line(line)) {
        face.vertices = new Vertex[tokens.length];
        face.vertexNormals = new Vertex[tokens.length];

        for (int i = 0; i < tokens.length; ++i) {
          subTokens = tokens[i].split("//");

          face.vertices[i] = vertices.get(Integer.parseInt(subTokens[0]) - 1);
          face.vertexNormals[i] = vertexNormals.get(Integer.parseInt(subTokens[1]) - 1);
        }

        face.faceNormal = face.calculateFaceNormal();
      }
      // f v1 v2 v3 ...
      else if (isValidFace_V_Line(line)) {
        face.vertices = new Vertex[tokens.length];

        for (int i = 0; i < tokens.length; ++i) {
          face.vertices[i] = vertices.get(Integer.parseInt(tokens[i]) - 1);
        }

        face.faceNormal = face.calculateFaceNormal();
      } else {
        throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Incorrect format");
      }
    } else {
      throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Incorrect format");
    }

    return face;
  }

  private GroupObject parseGroupObject(String line, int lineCount) throws ModelFormatException {
    GroupObject group = null;

    if (isValidGroupObjectLine(line)) {
      String trimmedLine = line.substring(line.indexOf(" ") + 1);

      if (trimmedLine.length() > 0) {
        group = new GroupObject(trimmedLine);
      }
    } else {
      throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Incorrect format");
    }

    return group;
  }

}