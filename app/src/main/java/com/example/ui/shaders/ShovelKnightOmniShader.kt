package com.example.ui.shaders

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.example.engine.ConvergedVisualParams

/**
 * Shovel Knight+ Unified 2D/3D Entity Shader (Android 13+ / API 33+ AGSL RuntimeShader).
 *
 * Compiles RuntimeShader ONCE per composable instance via Modifier.composed + remember {}
 * and updates only lightweight float uniforms inside graphicsLayer to maintain smooth 60fps.
 *
 * 1) Snaps both 3D renders and 2D sprites onto a shared virtual pixel grid
 * 2) Draws a crisp 1-virtual-pixel #0E1125 dark ink silhouette outline via 4-neighbor alpha check
 * 3) Quantizes shading into hue-shifted cel bands with 4x4 Bayer ordered dithering (pure arithmetic)
 * 4) Adds a 1-texel directional top-left rim light
 * 5) Adds pixel-snapped Terastal / PBR crystal glints
 *
 * On API 24-32, gracefully falls back to hardware-accelerated graphicsLayer rendering.
 */
object ShovelKnightOmniShader {
  // Strictly AGSL-compliant: zero non-constant array indexing!
  const val AGSL_SOURCE = """
    uniform shader content;
    uniform float2 resolution;
    uniform float virtualGrid;
    uniform float outlineWeight;
    uniform float celBands;
    uniform float bayerStrength;
    uniform float rimStrength;
    uniform float crystalGlint;
    uniform float vertexSnap;
    uniform float timeSec;
    uniform float hitFlash;

    float bayer4x4(float2 cell) {
      float x = mod(floor(cell.x), 4.0);
      float y = mod(floor(cell.y), 4.0);
      float x0 = mod(x, 2.0);
      float y0 = mod(y, 2.0);
      float x1 = floor(x * 0.5);
      float y1 = floor(y * 0.5);
      float v0 = mod(x0 + 2.0 * y0, 4.0);
      float v1 = mod(x1 + 2.0 * y1, 4.0);
      return (v0 * 4.0 + v1) / 16.0 - 0.5;
    }

    half4 main(float2 fragCoord) {
      float2 res = max(resolution, float2(1.0, 1.0));
      float baseGrid = clamp(virtualGrid, 48.0, 220.0);
      float snap = clamp(vertexSnap, 0.0, 1.0);
      float grid = mix(baseGrid, max(44.0, baseGrid * 0.65), snap);
      float2 stepPx = res / grid;
      float2 cell = floor(fragCoord / stepPx);
      float2 snapped = (cell + 0.5) * stepPx;

      half4 center = content.eval(snapped);
      float a = float(center.a);

      // 4-neighbor alpha check for crisp 1-virtual-pixel #0E1125 ink outline
      float aL = float(content.eval(snapped + float2(-stepPx.x, 0.0)).a);
      float aR = float(content.eval(snapped + float2(stepPx.x, 0.0)).a);
      float aU = float(content.eval(snapped + float2(0.0, -stepPx.y)).a);
      float aD = float(content.eval(snapped + float2(0.0, stepPx.y)).a);
      float maxNeighborA = max(max(aL, aR), max(aU, aD));

      if (a < 0.18) {
        if (outlineWeight > 0.12 && maxNeighborA > 0.42) {
          float inkAlpha = clamp(outlineWeight * 1.15, 0.0, 1.0);
          return half4(0.055 * inkAlpha, 0.067 * inkAlpha, 0.145 * inkAlpha, inkAlpha);
        }
        return half4(0.0);
      }

      // Unpremultiply RGB safely
      float3 rgb = float3(center.rgb) / max(a, 0.001);

      // 2-frame crisp white hit-flash override (Universal Game Feel)
      if (hitFlash > 0.5) {
        return half4(a, a, a, a);
      }

      // Hue-shifted cel ramp quantization with 4x4 Bayer ordered dither
      float luma = dot(rgb, float3(0.299, 0.587, 0.114));
      float dither = bayer4x4(cell) * bayerStrength * 0.22;
      float bands = max(celBands, 2.0);
      float qLuma = floor(clamp(luma + dither, 0.0, 0.999) * bands) / (bands - 1.0);
      float ratio = clamp((qLuma + 0.08) / (luma + 0.08), 0.55, 1.45);
      float3 celRgb = rgb * ratio;

      // Subtle cool shadow hue-shift & warm highlight lift (Shovel Knight palette discipline)
      float3 shadowTint = float3(0.90, 0.94, 1.12);
      float3 highlightTint = float3(1.06, 1.03, 0.95);
      celRgb *= mix(shadowTint, highlightTint, clamp(qLuma, 0.0, 1.0));

      // Subtle Tokusatsu clearcoat highlight sheen on bright cel highlights
      if (qLuma > 0.85) {
        float coatBand = smoothstep(0.85, 1.0, qLuma);
        float sheenWave = 0.72 + 0.28 * sin(timeSec * 2.0 + cell.x * 0.14 - cell.y * 0.10);
        celRgb += float3(0.18, 0.22, 0.25) * coatBand * sheenWave * (0.65 + rimStrength * 0.35);
      }

      // Angled 45-degree screen-space comic shadow halftone dots in deep shadow bands
      if (qLuma < 0.34) {
        float2 rotCell = float2(
          cell.x * 0.7071 - cell.y * 0.7071,
          cell.x * 0.7071 + cell.y * 0.7071
        );
        float2 dotUv = fract(rotCell * 0.5) - 0.5;
        float dotMask = smoothstep(0.26, 0.16, length(dotUv));
        float shadowDepth = (0.34 - qLuma) * 0.42 * (0.55 + bayerStrength * 0.45);
        celRgb *= (1.0 - dotMask * shadowDepth);
      }

      // 1-texel directional top-left rim light
      if (rimStrength > 0.05 && (aL < 0.30 || aU < 0.30)) {
        celRgb += float3(0.24, 0.42, 0.48) * rimStrength;
      }

      // Pixel-snapped Terastal / PBR crystal glint band
      if (crystalGlint > 0.05) {
        float diag = fract((cell.x * 0.12 - cell.y * 0.09) + timeSec * 0.45);
        float glint = step(0.92, diag) * crystalGlint;
        celRgb += float3(0.45, 0.92, 0.88) * glint * 0.45;
      }

      celRgb = clamp(celRgb, 0.0, 1.0);
      return half4(celRgb * a, a);
    }
  """

  fun applyUniformsAndBuildEffect(
    shader: RuntimeShader,
    widthPx: Float,
    heightPx: Float,
    visuals: ConvergedVisualParams,
    timeSec: Float,
    crystalGlint: Float = 0.25f,
    hitFlash: Boolean = false,
  ): androidx.compose.ui.graphics.RenderEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    if (widthPx <= 1f || heightPx <= 1f) return null
    return runCatching {
      shader.setFloatUniform("resolution", widthPx, heightPx)
      shader.setFloatUniform("virtualGrid", visuals.virtualPixelGrid.coerceIn(64f, 208f))
      shader.setFloatUniform("outlineWeight", visuals.outlineWeight.coerceIn(0f, 1f))
      shader.setFloatUniform("celBands", visuals.celBands.toFloat().coerceIn(2f, 6f))
      shader.setFloatUniform("bayerStrength", visuals.bayerDither.coerceIn(0f, 1f))
      shader.setFloatUniform("rimStrength", visuals.rimLight.coerceIn(0f, 1f))
      shader.setFloatUniform("crystalGlint", crystalGlint.coerceIn(0f, 1f))
      shader.setFloatUniform("vertexSnap", visuals.vertexSnap.coerceIn(0f, 1f))
      shader.setFloatUniform("timeSec", timeSec)
      shader.setFloatUniform("hitFlash", if (hitFlash) 1f else 0f)
      RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }.getOrNull()
  }
}

fun Modifier.shovelKnightEntityEffect(
  widthPx: Float,
  heightPx: Float,
  visuals: ConvergedVisualParams,
  timeSec: Float,
  crystalGlint: Float = 0.25f,
  hitFlash: Boolean = false,
): Modifier = composed {
  val cachedShader = remember {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      runCatching { RuntimeShader(ShovelKnightOmniShader.AGSL_SOURCE) }.getOrNull()
    } else {
      null
    }
  }
  this.graphicsLayer {
    renderEffect = if (cachedShader != null) {
      ShovelKnightOmniShader.applyUniformsAndBuildEffect(
        shader = cachedShader,
        widthPx = if (widthPx > 1f) widthPx else size.width,
        heightPx = if (heightPx > 1f) heightPx else size.height,
        visuals = visuals,
        timeSec = timeSec,
        crystalGlint = crystalGlint,
        hitFlash = hitFlash,
      )
    } else {
      null
    }
  }
}
