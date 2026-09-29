#version 330 core
in vec2 vTex;
in vec4 vColor;
layout(location=0) out vec4 fragColor;

uniform sampler2D tex;
uniform float texSize;

void main(void) {
    float here = texture(tex, floor(vTex) / texSize).a;
    float topLeft = texture(tex, floor(vTex - vec2(-1.0, -1.0)) / texSize).a;
    float top = texture(tex, floor(vTex - vec2(0.0, -1.0)) / texSize).a;
    float topRight = texture(tex, floor(vTex - vec2(1.0, -1.0)) / texSize).a;
    float right = texture(tex, floor(vTex - vec2(1.0, 0.0)) / texSize).a;
    float bottomRight = texture(tex, floor(vTex - vec2(1.0, 1.0)) / texSize).a;
    float bottom = texture(tex, floor(vTex - vec2(0.0, 1.0)) / texSize).a;
    float bottomLeft = texture(tex, floor(vTex - vec2(-1.0, 1.0)) / texSize).a;
    float left = texture(tex, floor(vTex - vec2(-1.0, 0.0)) / texSize).a;
    float sum = here + topLeft + top + topRight + right + bottomRight + bottom + bottomLeft + left;
    if (sum < 8.99 && sum > 0.01 && here > 0.01) {
        fragColor = vColor;//vec4(1.0, 0.1, 0.1, 1.0);
    } else {
        fragColor = vec4(1.0, 0.1, 0.1, 0.0);
    }
}
