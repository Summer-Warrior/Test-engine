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
 * Full-Viewport Multi-Hardware Display Post-FX Shader (Android 13+ / API 33+ AGSL).
 *
 * Compiles RuntimeShader ONCE per composable instance via Modifier.composed + remember {}
 * and updates only lightweight float uniforms inside graphicsLayer.
 *
 * - GBA hardware mosaic battle-entry dissolve (mosaicFactor 10f -> 1f)
 * - GBC ordered 4x4 Bayer dither quantization
 * - DMG 4-shade dot-matrix LCD sub-pixel grid
 * - Wii / PS2 soft highlight bloom lift
 * - PokeCRT-RTX Sony PVM RGB aperture grille + soft vignette + crisp black-masked CRT curvature
 */
object OmniDisplayPostShader {
  const val AGSL_SOURCE = """
    uniform shader content;
    uniform float2 resolution;
    uniform float mosaicFactor;
    uniform float crtIntensity;
    uniform float dotMatrixLcd;
    uniform float bloomLift;
    uniform float bayerDither;
    uniform float crtCurvature;

    float bayer4x4(float2 p) {
      float x = mod(floor(p.x), 4.0);
      float y = mod(floor(p.y), 4.0);
      float v0 = mod(mod(x, 2.0) + 2.0 * mod(y, 2.0), 4.0);
      float v1 = mod(floor(x * 0.5) + 2.0 * floor(y * 0.5), 4.0);
      return (v0 * 4.0 + v1) / 16.0 - 0.5;
    }

    half4 main(float2 fragCoord) {
      float2 res = max(resolution, float2(1.0, 1.0));
      float2 uv = fragCoord / res;

      // Optional analog CRT barrel curvature with clean black bezel outside [0.0, 1.0] (zero edge smearing!)
      if (crtCurvature > 0.5) {
        float2 c = uv * 2.0 - 1.0;
        float r2 = dot(c, c);
        c *= (1.0 + 0.055 * r2);
        uv = c * 0.5 + 0.5;
        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
          return half4(0.0, 0.0, 0.0, 1.0);
        }
      }

      float2 sampleCoord = uv * res;

      // GBA Hardware Mosaic Dissolve (1.0 = crisp, up to 10.0 = chunky transition blocks)
      if (mosaicFactor > 1.05) {
        float stepSize = clamp(mosaicFactor * 1.8, 1.0, 24.0);
        sampleCoord = (floor(sampleCoord / stepSize) + 0.5) * stepSize;
      }

      half4 base = content.eval(sampleCoord);
      float3 rgb = float3(base.rgb);

      // Wii / PS2 Soft Highlight Bloom Lift (4-tap bright halo)
      if (bloomLift > 0.15) {
        float2 off = float2(3.5, 3.5);
        float3 bSum = float3(content.eval(sampleCoord + float2(off.x, 0.0)).rgb)
                    + float3(content.eval(sampleCoord - float2(off.x, 0.0)).rgb)
                    + float3(content.eval(sampleCoord + float2(0.0, off.y)).rgb)
                    + float3(content.eval(sampleCoord - float2(0.0, off.y)).rgb);
        float3 bAvg = bSum * 0.25;
        float bright = max(0.0, dot(bAvg, float3(0.299, 0.587, 0.114)) - 0.52);
        rgb += bAvg * bright * bloomLift * 0.65;
      }

      // GBC Subtle Bayer Dither
      if (bayerDither > 0.18) {
        rgb += bayer4x4(sampleCoord * 0.5) * bayerDither * 0.055;
      }

      // DMG 4-Shade Dot-Matrix LCD Grid (active when DMG / Purist LCD selected)
      if (dotMatrixLcd > 0.18) {
        float gx = step(0.16, fract(sampleCoord.x * 0.25));
        float gy = step(0.16, fract(sampleCoord.y * 0.25));
        float lcdMask = mix(1.0, 0.80 + 0.20 * (gx * gy), dotMatrixLcd);
        float luma = dot(rgb, float3(0.299, 0.587, 0.114));
        float3 dmgOlive = mix(float3(0.06, 0.22, 0.06), float3(0.61, 0.74, 0.06), clamp(luma, 0.0, 1.0));
        rgb = mix(rgb, dmgOlive, dotMatrixLcd * 0.55) * lcdMask;
      }

      // PokeCRT-RTX Sony PVM RGB Aperture Grille + Scanline + Soft Vignette
      if (crtIntensity > 0.08) {
        float scan = 0.5 + 0.5 * sin(sampleCoord.y * 1.5708);
        float scanMask = 1.0 - crtIntensity * 0.22 * (1.0 - scan);
        float triad = mod(floor(sampleCoord.x), 3.0);
        float3 grille = float3(
          triad < 0.5 ? 1.05 : 0.94,
          (triad >= 0.5 && triad < 1.5) ? 1.05 : 0.94,
          triad >= 1.5 ? 1.05 : 0.94
        );
        rgb *= mix(float3(1.0), grille, crtIntensity * 0.55) * scanMask;
        float2 centered = uv - 0.5;
        float vig = 1.0 - dot(centered, centered) * (0.35 + crtIntensity * 0.45);
        rgb *= clamp(vig, 0.62, 1.0);
      }

      return half4(clamp(rgb, 0.0, 1.0), base.a);
    }
  """

  fun applyUniformsAndBuildEffect(
    shader: RuntimeShader,
    widthPx: Float,
    heightPx: Float,
    visuals: ConvergedVisualParams,
    mosaicFactor: Float = 1f,
  ): androidx.compose.ui.graphics.RenderEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    if (widthPx <= 1f || heightPx <= 1f) return null
    return runCatching {
      shader.setFloatUniform("resolution", widthPx, heightPx)
      shader.setFloatUniform("mosaicFactor", mosaicFactor.coerceIn(1f, 10f))
      shader.setFloatUniform("crtIntensity", visuals.crtScanline.coerceIn(0f, 1f))
      shader.setFloatUniform("dotMatrixLcd", visuals.dotMatrixLcd.coerceIn(0f, 1f))
      shader.setFloatUniform("bloomLift", visuals.bloom.coerceIn(0f, 1f))
      shader.setFloatUniform("bayerDither", visuals.bayerDither.coerceIn(0f, 1f))
      shader.setFloatUniform("crtCurvature", if (visuals.crtCurvatureEnabled) 1f else 0f)
      RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }.getOrNull()
  }
}

fun Modifier.omniDisplayPostEffect(
  visuals: ConvergedVisualParams,
  mosaicFactor: Float = 1f,
): Modifier = composed {
  val cachedShader = remember {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      runCatching { RuntimeShader(OmniDisplayPostShader.AGSL_SOURCE) }.getOrNull()
    } else {
      null
    }
  }
  this.graphicsLayer {
    renderEffect = if (cachedShader != null) {
      OmniDisplayPostShader.applyUniformsAndBuildEffect(
        shader = cachedShader,
        widthPx = size.width,
        heightPx = size.height,
        visuals = visuals,
        mosaicFactor = mosaicFactor,
      )
    } else {
      null
    }
  }
}
