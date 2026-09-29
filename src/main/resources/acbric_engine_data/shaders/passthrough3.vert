#version 330 core
layout(location=0) in vec2 aPos;
layout(location=1) in vec4 aColor;
layout(location=2) in vec2 aTex;
uniform mat4 uProj;
uniform mat4 uModel;
uniform vec4 tint;
out vec2 vTex;
out vec4 vColor;

void main() {
    gl_Position = uProj * uModel * vec4(aPos, 0.0, 1.0);
    vTex = aTex;
    vColor = tint;
}
