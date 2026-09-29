#version 330 core
layout(location=0) in vec2 aPos;
layout(location=1) in vec4 aColor;
layout(location=2) in vec2 aTex;
layout(location=3) in vec4 aGen0;
layout(location=4) in vec4 aGen1;
layout(location=10) in vec4 aGen7;
layout(location=11) in vec4 aGen8;
uniform mat4 uProj;
uniform mat4 uModel;
out vec2 vTex;
out vec4 vColor;
out vec3 vmaskOffsetAndEnabled;
out vec2 vglobalTexCoord;
out vec4 vpaint;

void main() {
    gl_Position = uProj * uModel * vec4(aPos, 0.0, 1.0);
    vTex = aTex;
    vColor = aGen1; // tint
    vmaskOffsetAndEnabled = aGen8.xyz;
    vglobalTexCoord = aGen0.zw;
    vpaint = aGen7;
}
