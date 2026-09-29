#version 330 core
in vec2 vTex;
in vec4 vColor;
layout(location=0) out vec4 fragColor;

uniform sampler2D tex;
uniform float texSize;

void main(void) {
	fragColor = texture(tex, floor(vTex) / texSize) * vColor;
}
