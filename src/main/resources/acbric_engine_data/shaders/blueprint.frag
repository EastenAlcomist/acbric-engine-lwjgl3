#version 330 core
in vec2 vTex;
in vec4 vColor;
layout(location=0) out vec4 fragColor;

uniform sampler2D tex;
uniform float intensity;
uniform float texSize;

void main(void) {
	 vec4 original = texture(tex, floor(vTex) / texSize);
	 float amt = (original.x / 2.0 + original.y / 2.0 + original.z / 2.0 + original.a / 4.0) * intensity;
	 fragColor = vec4(1.0, 1.0, 1.0, min(1.0, amt));
}
