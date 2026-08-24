#version 150
#moj_import <fog.glsl>

const float CloudFadeAlpha = 0.0;   // 0=no fade, 1=fully transparent at top
const float Brightness       = 1.0;   // fullbright
const float Transparency     = 0.8;   // base alpha
const float CloudHeight      = 2.5;   // vertical scaling
const float CloudScale       = 1.0;   // horizontal scaling
const float CloudYOffset     = 0.0;   // global vertical offset
const float CloudFogFactor   = 0.75;

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ModelOffset;
uniform int FogShape;
uniform vec4 ColorModulator;

out float vertexDistance;
out vec4 vertexColor;

float lerp(float d, float e, float f) {
    return e + d * (f - e);
}

void main() {
    vec3 pos = vec3(
        Position.x * CloudScale,
        Position.y * CloudHeight + CloudYOffset,
        Position.z * CloudScale
    );

    pos += ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
    vertexDistance = fog_distance(pos * CloudFogFactor, FogShape);

    vec3 rgb = vec3(Brightness);
    float baseA = Transparency;

    float vertexY = pos.y - ModelOffset.y;
    float normalizedY = clamp(vertexY / CloudHeight, 0.0, 1.0);
    float dir = clamp((ModelOffset.y + CloudHeight / 2.0) / CloudHeight, -1.0, 1.0);
    float fadeBelow = lerp(normalizedY, 1.0, CloudFadeAlpha);
    float fadeAbove = lerp(1.0 - normalizedY, 1.0, CloudFadeAlpha);
    float mixFactor = (dir + 1.0) * 0.5;
    float fade = mix(fadeBelow, fadeAbove, mixFactor);

    vertexColor = vec4(rgb, baseA * (1.0 - fade)) * ColorModulator;
}
