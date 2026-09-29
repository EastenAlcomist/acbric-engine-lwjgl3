#version 330 core
in vec2 vTex;
in vec4 vColor;
layout(location=0) out vec4 fragColor;

uniform sampler2D tex;
uniform sampler2D mask;
uniform vec2 texSize;

in vec3 vmaskOffsetAndEnabled;
in vec2 vglobalTexCoord;
in vec4 vpaint;

void main(void) {
    vec4 maskValue = texture(mask, floor(vglobalTexCoord + vmaskOffsetAndEnabled.xy) / texSize.y);
    if (vmaskOffsetAndEnabled.z > 0.0 && maskValue.a < 0.1) {
        fragColor = vec4(0.0, 0.0, 0.0, 0.0);
        return;
    }

	fragColor = (texture(tex, floor(vTex) / texSize.x) * vec4(1.0 - vpaint.w / 2.0, 1.0 - vpaint.w / 2.0, 1.0 - vpaint.w / 2.0, 1.0) + vec4(vpaint.x, vpaint.y, vpaint.z, 0.0) / 2.0) * vColor;
}
