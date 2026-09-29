#version 330 core
in vec2 vTex;
in vec4 vColor;
layout(location=0) out vec4 fragColor;

uniform vec4 ambient;

in float vflagSize;
in float wind;
in float t;
in float yShift;
in vec2 coord;

void main(void) {
    vec2 texCoord = coord / vflagSize;
    texCoord = texCoord + vec2(0, cos(texCoord.x * 9.0 + t) * 0.07 * texCoord.x + 1.0 / (wind * 0.4 + 0.01) * texCoord.x + yShift * texCoord.x);
    texCoord = texCoord / vec2(min(0.95 + cos(t * wind) * 0.01, max(-0.95 + cos(t * wind) * 0.01, wind * 0.2)), 1.0);
    if (texCoord.x < 0.0 || texCoord.y - texCoord.x * 0.5 < -1.0 || texCoord.x > 1.0 || texCoord.y + texCoord.x * 0.5 > 0.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 0.0);
    } else {
	    fragColor = vColor * ambient;
    }
}
