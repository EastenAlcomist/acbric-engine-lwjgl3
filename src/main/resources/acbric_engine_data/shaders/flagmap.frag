#version 330 core
in vec2 vTex;
in vec4 vColor;
layout(location=0) out vec4 fragColor;

uniform sampler2D tex;
uniform sampler2D map;
uniform sampler2D arms;

void main(void) {
    vec2 pos = vTex;
    vec4 base = texture(tex, pos);
    vec4 mapLookup = texture(map, pos);
    vec4 armsClr = texture(arms, mapLookup.xy);
    float mixAmt = mapLookup.a;
    fragColor = base * (1.0 - mixAmt) + armsClr * mixAmt * vColor;
}
