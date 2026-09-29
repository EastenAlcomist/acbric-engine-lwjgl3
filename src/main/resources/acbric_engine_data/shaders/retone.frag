#version 330 core
in vec2 vTex;
in vec4 vColor;
layout(location=0) out vec4 fragColor;

uniform sampler2D tex;
uniform float texSize;
uniform vec3 darkest;
uniform vec3 dark;
uniform vec3 light;

void main(void) {
	 vec4 original = texture(tex, floor(vTex) / texSize);
	 float amt = (original.x / 3.0 + original.y / 3.0 + original.z / 3.0);
	 if (amt > 0.5) {
    	 fragColor = vec4(light.x, light.y, light.z, original.a);
	 } else if (amt > 0.08) {
    	 fragColor = vec4(dark.x, dark.y, dark.z, original.a);
	 } else {
    	 fragColor = vec4(darkest.x, darkest.y, darkest.z, original.a);
	 }
}
