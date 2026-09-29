#version 330 core
in vec2 vTex;
in vec4 vColor;
layout(location=0) out vec4 fragColor;

uniform sampler2D tex;

void main(void) {
	fragColor = texture(tex, vTex);
}
