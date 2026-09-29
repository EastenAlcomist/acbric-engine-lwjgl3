#version 330 core
in vec2 vTex;
in vec4 vColor;
layout(location=0) out vec4 fragColor;

uniform sampler2D tex;
uniform float texSize;

void main(void) {
    vec4 here = texture(tex, floor(vTex) / texSize);
    if (here.a > 0.0) {
        fragColor = vec4(1.0, 0.1, 0.1, 1.0);
    } else {
        fragColor = vec4(1.0, 0.1, 0.1, 0.0);
    }
}
