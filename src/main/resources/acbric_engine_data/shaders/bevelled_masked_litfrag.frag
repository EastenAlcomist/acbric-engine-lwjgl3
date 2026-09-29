#version 330 core
in vec2 vTex;
in vec4 vColor;
layout(location=0) out vec4 fragColor;

uniform sampler2D tex;
uniform sampler2D lightFromLeft;
uniform sampler2D lightFromTop;
uniform sampler2D lightFromRight;
uniform sampler2D lightFromBottom;
uniform sampler2D bump;
uniform sampler2D mask;
uniform sampler2D maskBump;
uniform float strength;
uniform vec2 lightSize;
uniform float screenHeight;
uniform vec4 ambient;
uniform float ambientSaturation;
uniform vec2 texSize;

in vec2 vflipped_concave;

in vec4 vbevel; // top, bottom, left, right

in vec3 vt; // tl, tm, tr
in vec3 vm; // ml, mm, mr
in vec3 vb; // bl, bm, br

in vec4 vpaint;

in vec2 vglobalTexCoord;
in vec4 vmaskOffsetAndEnabled;

void main(void) {
    vec4 maskValue = texture(mask, floor(vglobalTexCoord + vmaskOffsetAndEnabled.xy) / texSize.y);
    if (vmaskOffsetAndEnabled.z > 0.0 && maskValue.a < 0.1) {
        fragColor = vec4(0.0, 0.0, 0.0, 0.0);
        return;
    }

    vec4 bumpLookup = texture(bump, floor(vTex) / texSize.x);

    float shiny = bumpLookup.z * ((1.0 - vpaint.a) + vmaskOffsetAndEnabled.w * vpaint.a / 0.36);

    float bly = bumpLookup.y; // NB bly is left-right and blx is up-down, argh.
    float blx = bumpLookup.x;

    if (vmaskOffsetAndEnabled.z > 0.0) {
        vec4 maskBumpLookup = texture(maskBump, floor(vglobalTexCoord + vmaskOffsetAndEnabled.xy) / texSize.y);
        blx = blx + maskBumpLookup.x - 0.5;
        bly = bly + (maskBumpLookup.y - 0.5) * vflipped_concave.x;
    }

    float localY = mod(vglobalTexCoord.y, 16.0);
    float localX = mod(vglobalTexCoord.x, 16.0);

    if (
        ((localY <= 4.0) &&
            ((localX <= 4.0 && vt[0] < 0.1) ||
            (localX >= 12.0 && vt[2] < 0.1) ||
            (localX > 4.0 && localX < 12.0 && vt[1] < 0.1)))
        ||
        ((localY >= 12.0) &&
            ((localX <= 4.0 && vb[0] < 0.1) ||
            (localX >= 12.0 && vb[2] < 0.1) ||
            (localX > 4.0 && localX < 12.0 && vb[1] < 0.1)))
        ||
        ((localY > 4.0 && localY < 12.0) &&
            ((localX <= 4.0 && vm[0] < 0.1) ||
            (localX >= 12.0 && vm[2] < 0.1) ||
            (localX > 4.0 && localX < 12.0 && vm[1] < 0.1)))
    ) {
        fragColor = vec4(0.0, 0.0, 0.0, 0.0);
        return;
    }

    // Bevels up
    if (vflipped_concave.y > 0.5) {
        if (localY <= 2.0 && vbevel[0] > 0.9) {
            blx = (blx - 0.5) * 0.5 + 0.75;
        } else if (localY >= 14.0 && vbevel[1] > 0.9) {
            blx = (blx - 0.5) * 0.5 + 0.25;
        } else if (localX <= 2.0 && vbevel[2] > 0.9) {
            bly = (bly - 0.5) * 0.5 + 0.25;
        } else if (localX >= 14.0 && vbevel[3] > 0.9) {
            bly = (bly - 0.5) * 0.5 + 0.75;
        }
    } else {
        if (localY <= 2.0 && vbevel[0] > 0.9) {
            blx = (blx - 0.5) * 0.5 + 0.75;
        } else if (localY >= 14.0 && vbevel[1] > 0.9) {
            blx = (blx - 0.5) * 0.5 + 0.25;
        } else if (localX <= 2.0 && vbevel[2] > 0.9) {
            bly = (bly - 0.5) * 0.5 + 0.75;
        } else if (localX >= 14.0 && vbevel[3] > 0.9) {
            bly = (bly - 0.5) * 0.5 + 0.25;
        }
    }

    float leftM = max(bly - 0.3, 0.0) * 2.0 * shiny * strength;
    float rightM = max(0.7 - bly, 0.0) * 2.0 * shiny * strength;
    float topM = max(blx - 0.3, 0.0) * 2.0 * shiny * strength;
    float bottomM = max(0.7 - blx, 0.0) * 2.0 * shiny * strength;

	vec4 lightTop = texture(lightFromTop, vec2(gl_FragCoord.x, screenHeight - gl_FragCoord.y) / lightSize / 4.0);
	lightTop = vec4(lightTop.x * topM, lightTop.y * topM, lightTop.z * topM, 0);

	vec4 lightBottom = texture(lightFromBottom, vec2(gl_FragCoord.x, screenHeight - gl_FragCoord.y) / lightSize / 4.0);
	lightBottom = vec4(lightBottom.x * bottomM, lightBottom.y * bottomM, lightBottom.z * bottomM, 0);

    vec4 lightLeft = texture(lightFromLeft, vec2(gl_FragCoord.x, screenHeight - gl_FragCoord.y) / lightSize / 4.0);
	vec4 lightRight = texture(lightFromRight, vec2(gl_FragCoord.x, screenHeight - gl_FragCoord.y) / lightSize / 4.0);

	mat4 satur = mat4(
	    (1.0 - ambientSaturation) / 3.0 + ambientSaturation,
	    (1.0 - ambientSaturation) / 3.0,
	    (1.0 - ambientSaturation) / 3.0,
	    0.0,

	    (1.0 - ambientSaturation) / 3.0,
	    (1.0 - ambientSaturation) / 3.0 + ambientSaturation,
	    (1.0 - ambientSaturation) / 3.0,
	    0.0,

	    (1.0 - ambientSaturation) / 3.0,
	    (1.0 - ambientSaturation) / 3.0,
	    (1.0 - ambientSaturation) / 3.0 + ambientSaturation,
	    0.0,

	    0.0,
	    0.0,
	    0.0,
	    1.0
	);

	float am = max(0.85, min(1.1, 164.0 / 256.0 + bumpLookup.z));
	vec4 ambientMult = vec4(am, am, am, 1.0);

	lightLeft = vec4(lightLeft.x * leftM, lightLeft.y * leftM, lightLeft.z * leftM, 0);
	lightRight = vec4(lightRight.x * rightM, lightRight.y * rightM, lightRight.z * rightM, 0);

	fragColor = (texture(tex, floor(vTex) / texSize.x) * vec4(1.0 - vpaint.w, 1.0 - vpaint.w, 1.0 - vpaint.w, 1.0) + vec4(vpaint.x, vpaint.y, vpaint.z, 0.0)) * vColor * ambient * satur * ambientMult + lightLeft + lightRight + lightTop + lightBottom;
}
