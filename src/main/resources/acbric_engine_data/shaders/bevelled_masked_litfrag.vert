#version 330 core
layout(location=0) in vec2 aPos;
layout(location=1) in vec4 aColor;
layout(location=2) in vec2 aTex;
layout(location=3) in vec4 aGen0;
layout(location=4) in vec4 aGen1;
layout(location=6) in vec4 aGen3;
layout(location=7) in vec4 aGen4;
layout(location=8) in vec4 aGen5;
layout(location=9) in vec4 aGen6;
layout(location=10) in vec4 aGen7;
layout(location=11) in vec4 aGen8;
uniform mat4 uProj;
uniform mat4 uModel;
out vec2 vTex;
out vec4 vColor;
out vec2 vflipped_concave;
out vec4 vbevel; // top, bottom, left, right
out vec3 vt; // tl, tm, tr
out vec3 vm; // ml, mm, mr
out vec3 vb; // bl, bm, br
out vec4 vpaint;
out vec4 vmaskOffsetAndEnabled;
out vec2 vglobalTexCoord;

void main() {
    gl_Position = uProj * uModel * vec4(aPos, 0.0, 1.0);
    vTex = aTex;
    vflipped_concave = aGen0.xy;
    vglobalTexCoord = aGen0.zw;
    vbevel = aGen3;
    vt = aGen4.xyz;
    vm = aGen5.xyz;
    vb = aGen6.xyz;
    vColor = aGen1; // tint
    vpaint = aGen7;
    vmaskOffsetAndEnabled = aGen8;
}
