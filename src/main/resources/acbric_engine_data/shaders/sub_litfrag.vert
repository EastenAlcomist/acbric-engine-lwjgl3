#version 330 core
layout(location=0) in vec2 aPos;
layout(location=1) in vec4 aColor;
layout(location=2) in vec2 aTex;
layout(location=3) in vec4 aGen0;
layout(location=4) in vec4 aGen1;
layout(location=5) in vec4 aGen2;
layout(location=6) in vec4 aGen3;
uniform mat4 uProj;
uniform mat4 uModel;
out vec2 vTex;
out vec4 vColor;
out float vflipped;
out float vstrength;
out vec3 vSrcA;
out vec4 vTrgA;

void main() {
    gl_Position = uProj * uModel * vec4(aPos, 0.0, 1.0);
    vTex = aTex;
    vflipped = aGen0.x;
    vstrength = aGen0.z;
    vColor = aGen1; // tint
    vSrcA = aGen2.xyz;
    vTrgA = aGen3;
}
