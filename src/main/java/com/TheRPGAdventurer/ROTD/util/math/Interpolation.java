/*
** 2016 March 05
**
** The author disclaims copyright to this source code. In place of
** a legal notice, here is a blessing:
**    May you do good and not evil.
**    May you find forgiveness for yourself and forgive others.
**    May you share freely, never taking more than you give.
 */
package com.TheRPGAdventurer.ROTD.util.math;

/**
 * Interpolation utility class.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class Interpolation {


  /**
   * find the position of x within the array of xvalues
   * example:
   *   if xvalues is {0, 2, 4, 6, 8} and x is 2.5 then findIndex is 1 + (2.5 - 2)/(4 - 2) = 1.25
   * @param x       the value to find. values outside the array range are clipped
   * @param xvalues the values to be searched; assumed to be valid (isValidInterpolationArray is true)
   * @return the fractional index of x within the array of xvalues; 0 - xvalues.length - 1 inclusive
   */
  public static double findIndex(double x, double [] xvalues)
  {
    for (int i = 0; i < xvalues.length; ++i) {
      if (x < xvalues[i]) {
        if (i == 0) return 0.0F;
        return i -1 + (x - xvalues[i-1]) / (xvalues[i] - xvalues[i-1]);
      }
    }
    return xvalues.length - 1;
  }

  /**
   * Checks if this array is suitable for use with interpolation:
   *  - sorted ascending
   *  - the difference between two consecutive values is no less than 1e-5 of their value, eg 100,000 to 100,001
   * @param xvalues the array of xvalues
   * @return true if valid, false if not
   */
  public static boolean isValidInterpolationArray(double [] xvalues) {
    for (int i = 0; i < xvalues.length - 1; ++i) {
      double delta = xvalues[i + 1] - xvalues[i];
      double magnitude = Math.max(Math.abs(xvalues[i]), Math.abs(xvalues[i + 1]));
      if (delta < magnitude * 1E-5F) return false;
    }
    return true;
  }

  /**
   * Uses the fractional index to interpolate between the yvalues
   * @param index index of the desired element.  values outside the array range are clipped
   * @param yvalues
   * @return
   */
  public static double linearArray(double index, double[] yvalues) {
    if (index <= 0) return yvalues[0];
    if (index >= yvalues.length - 1) return yvalues[yvalues.length - 1];
    int idx = (int)Math.floor(index);
    double frac = index - idx;
    return linear(yvalues[idx], yvalues[idx+1], frac);
  }

  /**
   * Uses the x to interpolate between the yvalues
   * eg xvalues = [0,  2,  4,  6,  8]
   *    yvalues = [0, 10, 20, 30, 40]
   * if x = 2.5 then y = (2.5 - 2) / (4.0 - 2.0) * (20 - 10) + 10
   *
   * @param x the value of x to obtain the corresponding y-value for
   * @param xvalues the xvalues to use for interpolation: assumed to be valid (isValidInterpolationArray is true)
   * @param yvalues the yvalues to interpolate between
   * @return
   */
  public static double linearArray(double x, double[] xvalues, double[] yvalues) {
    if (xvalues.length != yvalues.length) throw new IllegalArgumentException("mismatch xvalues and yvalues size");
    double index = findIndex(x, xvalues);
    return linearArray(index, yvalues);
  }

  public static double linear(double a, double b, double x) {
    if (x <= 0) {
      return a;
    }
    if (x >= 1) {
      return b;
    }
    return a * (1 - x) + b * x;
  }


  public static float linear(float a, float b, float x) {
    if (x <= 0) {
      return a;
    }
    if (x >= 1) {
      return b;
    }
    return a * (1 - x) + b * x;
  }

  public static float smoothStep(float a, float b, float x) {
    if (x <= 0) {
      return a;
    }
    if (x >= 1) {
      return b;
    }
    x = x * x * (3 - 2 * x);
    return a * (1 - x) + b * x;
  }

  // http://www.java-gaming.org/index.php?topic=24122.0
  public static void catmullRomSpline(float x, float[] result, float[]... knots) {
    int nknots = knots.length;
    int nspans = nknots - 3;
    int knot = 0;
    if (nspans < 1) {
      throw new IllegalArgumentException("Spline has too few knots");
    }
    x = MathX.clamp(x, 0, 0.9999f) * nspans;

    int span = (int) x;
    if (span >= nknots - 3) {
      span = nknots - 3;
    }

    x -= span;
    knot += span;

    int dimension = result.length;
    for (int i = 0; i < dimension; i++) {
      float knot0 = knots[knot][i];
      float knot1 = knots[knot + 1][i];
      float knot2 = knots[knot + 2][i];
      float knot3 = knots[knot + 3][i];

      float c3 = CR[0][0] * knot0 + CR[0][1] * knot1 + CR[0][2] * knot2 + CR[0][3] * knot3;
      float c2 = CR[1][0] * knot0 + CR[1][1] * knot1 + CR[1][2] * knot2 + CR[1][3] * knot3;
      float c1 = CR[2][0] * knot0 + CR[2][1] * knot1 + CR[2][2] * knot2 + CR[2][3] * knot3;
      float c0 = CR[3][0] * knot0 + CR[3][1] * knot1 + CR[3][2] * knot2 + CR[3][3] * knot3;

      result[i] = ((c3 * x + c2) * x + c1) * x + c0;
    }
  }

  private Interpolation() {
  }
  private static final float[][] CR = {
          {-0.5f, 1.5f, -1.5f, 0.5f},
          {1.0f, -2.5f, 2.0f, -0.5f},
          {-0.5f, 0.0f, 0.5f, 0.0f},
          {0.0f, 1.0f, 0.0f, 0.0f}
  };
}
