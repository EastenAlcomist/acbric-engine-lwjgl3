#version 330 core
layout(location=0) in vec2 aPos;
layout(location=1) in vec4 aColor;
layout(location=2) in vec2 aTex;
layout(location=3) in vec4 aGen0;
layout(location=5) in vec4 aGen2;
layout(location=6) in vec4 aGen3;
layout(location=7) in vec4 aGen4;
layout(location=8) in vec4 aGen5;
uniform mat4 uProj;
uniform mat4 uModel;
out vec2 vTex;
out vec4 vColor;
out float vflipped;
out float vangle;
out vec3 vSrcA;
out vec3 vTrgA;
out vec3 vSrcB;
out vec3 vTrgB;

void main() {
    gl_Position = uProj * uModel * vec4(aPos, 0.0, 1.0);
    vTex = aTex;
    vflipped = aGen0.x;
    vangle = aGen0.y;
    vSrcA = aGen2.xyz;
    vTrgA = aGen3.xyz;
    vSrcB = aGen4.xyz;
    vTrgB = aGen5.xyz;
    vColor = vec4(1.0, 1.0, 1.0, 1.0);
}
