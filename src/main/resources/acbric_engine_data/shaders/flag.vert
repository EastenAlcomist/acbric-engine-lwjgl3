#version 330 core
layout(location=0) in vec2 aPos;
layout(location=1) in vec4 aColor;
layout(location=2) in vec2 aTex;
layout(location=3) in vec4 aGen0;
layout(location=5) in vec4 aGen2;
uniform mat4 uProj;
uniform mat4 uModel;
out vec2 vTex;
out vec4 vColor;
out float vflagSize;
out vec2 vtexOffset;
out float wind;
out float t;
out float yShift;

void main() {
    gl_Position = uProj * uModel * vec4(aPos, 0.0, 1.0);
    vTex = aTex;
    vflagSize = aGen0.x;
    vtexOffset = aGen0.yz;
    wind = aGen0.w;
    t = aGen2.x;
    yShift = aGen2.y;
    vColor = vec4(1.0, 1.0, 1.0, 1.0);
}
