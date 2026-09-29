#version 330 core
in vec2 vTex;
in vec4 vColor;
layout(location=0) out vec4 fragColor;

uniform sampler2D tex;

in float vflagSize;
in vec2 vtexOffset;
in float wind;
in float t;
in float yShift;

void main(void) {
    vec2 texCoord = vTex / vflagSize;
    texCoord = texCoord + vec2(0, cos((texCoord.x + t) * wind) * 0.07 * texCoord.x + 1.0 / (wind * 0.4 + 0.18) * texCoord.x + yShift * texCoord.x);
    texCoord = texCoord / vec2(min(1.0, max(-1.0, wind * 0.2 + 0.15)), 1.0);
    if (texCoord.x < 0.0 || texCoord.y < -1.0 || texCoord.x > 1.0 || texCoord.y > 0.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 0.0);
    } else {
        fragColor = texture(tex, texCoord / 2.0 + vtexOffset);
    }
}
