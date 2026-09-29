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
in float vangle;

void main(void) {
    vec4 bumpLookup = texture(bump, floor(vTex) / texSize);

    float topM = max(bumpLookup.x - 0.3, 0.0) * 2.0 * bumpLookup.z * strength;
    float bottomM = max(0.7 - bumpLookup.x, 0.0) * 2.0 * bumpLookup.z * strength;
    float leftM = max(bumpLookup.y - 0.3, 0.0) * 2.0 * bumpLookup.z * strength;
    float rightM = max(0.7 - bumpLookup.y, 0.0) * 2.0 * bumpLookup.z * strength;

    /*float m0 = max(0.0, cos(vangle));
    float m1 = max(0.0, cos(vangle + 0.785398163));
    float m2 = max(0.0, cos(vangle + 1.570796327));
    float m3 = max(0.0, cos(vangle + 2.35619449));*/

    /*float m0 = max(0.0, cos(vangle));
    float m1 = max(0.0, cos(vangle - 1.570796327));
    float m2 = max(0.0, cos(vangle - 3.141592654));
    float m3 = max(0.0, cos(vangle - 4.71238898));*/

    float m0 = max(0.0, 1.0 - abs(mod(vangle / 1.570796327 + 9.0, 4.0) - 1.0));
    float m1 = max(0.0, 1.0 - abs(mod(vangle / 1.570796327 + 8.0, 4.0) - 1.0));
    float m2 = max(0.0, 1.0 - abs(mod(vangle / 1.570796327 + 7.0, 4.0) - 1.0));
    float m3 = max(0.0, 1.0 - abs(mod(vangle / 1.570796327 + 6.0, 4.0) - 1.0));

	vec4 lightTop = texture(lightFromTop, vec2(gl_FragCoord.x, screenHeight - gl_FragCoord.y) / lightSize / 4.0);
	vec4 lightBottom = texture(lightFromBottom, vec2(gl_FragCoord.x, screenHeight - gl_FragCoord.y) / lightSize / 4.0);
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

	if (vflipped > 0.9) {
        vec4 totalLight =
            topM * (lightTop * m0 + lightRight * m1 + lightBottom * m2 + lightLeft * m3) + // top
            leftM * (lightTop * m3 + lightRight * m0 + lightBottom * m1 + lightLeft * m2) + // left
            bottomM * (lightTop * m2 + lightRight * m3 + lightBottom * m0 + lightLeft * m1) + // bottom
            rightM * (lightTop * m1 + lightRight * m2 + lightBottom * m3 + lightLeft * m0); // right

	    fragColor = texture(tex, floor(vTex) / texSize) * vColor * ambient * satur * ambientMult + vec4(totalLight.x, totalLight.y, totalLight.z, 0);
	} else {
	    vec4 totalLight =
            topM * (lightTop * m0 + lightRight * m1 + lightBottom * m2 + lightLeft * m3) + // top
            rightM * (lightTop * m3 + lightRight * m0 + lightBottom * m1 + lightLeft * m2) + // left
            bottomM * (lightTop * m2 + lightRight * m3 + lightBottom * m0 + lightLeft * m1) + // bottom
            leftM * (lightTop * m1 + lightRight * m2 + lightBottom * m3 + lightLeft * m0); // right

	    fragColor = texture(tex, floor(vTex) / texSize) * vColor * ambient * satur * ambientMult + vec4(totalLight.x, totalLight.y, totalLight.z, 0);
	}
}
