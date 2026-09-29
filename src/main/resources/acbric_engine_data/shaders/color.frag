#version 330 core
in vec2 vTex;
in vec4 vColor;
layout(location=0) out vec4 fragColor;

void main(void) {
	fragColor = vColor;
}
