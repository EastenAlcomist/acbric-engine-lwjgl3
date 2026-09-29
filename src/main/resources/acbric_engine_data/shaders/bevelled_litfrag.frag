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
uniform float strength;
uniform vec2 lightSize;
uniform float screenHeight;
uniform vec4 ambient;
uniform float ambientSaturation;
uniform float texSize;

in float vflipped;

in vec4 vbevel; // top, bottom, left, right

void main(void) {
    vec4 bumpLookup = texture(bump, floor(vTex) / texSize);
    float bly = bumpLookup.y;
    float blx = bumpLookup.x;

    float localY = mod(vTex.y, 16.0);
    float localX = mod(vTex.x, 16.0);

    if (localY <= 2.0 && vbevel[0] > 0.9) {
        blx = (blx - 0.5) * 0.5 + 0.75;
    } else if (localY >= 14.0 && vbevel[1] > 0.9) {
        blx = (blx - 0.5) * 0.5 + 0.25;
    } else if (localX <= 2.0 && vbevel[2] > 0.9) {
        bly = (bly - 0.5) * 0.5 + 0.75;
    } else if (localX >= 14.0 && vbevel[3] > 0.9) {
        bly = (bly - 0.5) * 0.5 + 0.25;
    }

    float leftM = max(bly - 0.3, 0.0) * 2.0 * bumpLookup.z * strength;
    float rightM = max(0.7 - bly, 0.0) * 2.0 * bumpLookup.z * strength;
    float topM = max(blx - 0.3, 0.0) * 2.0 * bumpLookup.z * strength;
    float bottomM = max(0.7 - blx, 0.0) * 2.0 * bumpLookup.z * strength;

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

	if (vflipped > 0.9) {
	    lightLeft = vec4(lightLeft.x * rightM, lightLeft.y * rightM, lightLeft.z * rightM, 0);
	    lightRight = vec4(lightRight.x * leftM, lightRight.y * leftM, lightRight.z * leftM, 0);
	} else {
	    lightLeft = vec4(lightLeft.x * leftM, lightLeft.y * leftM, lightLeft.z * leftM, 0);
	    lightRight = vec4(lightRight.x * rightM, lightRight.y * rightM, lightRight.z * rightM, 0);
	}

	float am = max(0.85, min(1.1, 164.0 / 256.0 + bumpLookup.z));
	vec4 ambientMult = vec4(am, am, am, 1.0);

	fragColor = texture(tex, floor(vTex) / texSize) * ambient * satur * ambientMult + lightLeft + lightRight + lightTop + lightBottom;
}
