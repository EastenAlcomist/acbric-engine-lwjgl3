#version 330 core
in vec2 vTex;
in vec4 vColor;
layout(location=0) out vec4 fragColor;

uniform sampler2D tex;
uniform sampler2D refTex;
uniform float texSize;
uniform vec4 ambient;

in vec3 vSrcA;
in vec3 vTrgA;

void main(void) {
    vec4 base = texture(tex, floor(vTex) / texSize);
    vec4 ref = texture(refTex, floor(vTex) / texSize);
    if ((ref.x - vSrcA.x) * (ref.x - vSrcA.x) + (ref.y - vSrcA.y) * (ref.y - vSrcA.y) + (ref.z - vSrcA.z) * (ref.z - vSrcA.z) < 0.000256) {
        base.xyz = clamp(base.xyz + (vTrgA - vSrcA) * ambient.xyz, vec3(0.0, 0.0, 0.0), vec3(1.0, 1.0, 1.0));
    }
	fragColor = base * vColor;
}
