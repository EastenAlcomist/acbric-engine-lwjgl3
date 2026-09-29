#version 330 core
in vec2 vTex;
in vec4 vColor;
layout(location=0) out vec4 fragColor;

uniform sampler2D lightFromLeft;
uniform sampler2D lightFromRight;
uniform vec2 lightSize;
uniform float screenHeight;
uniform vec4 ambient;
uniform float ambientSaturation;
uniform float strength;

in float vflagSize;
in float wind;
in float t;
in float yShift;
in vec2 coord;

void main(void) {
    vec2 texCoord = coord / vflagSize;

    float leftM = max(floor(0.5 + (sin(texCoord.x * 9.0 + t) * min(1.0, wind * 0.1) * 0.5 + 0.5) * 4.0) * 0.25 - 0.3, 0.0) * 2.0 * 0.36 * strength;
    float rightM = max(floor(0.5 + (-sin(texCoord.x * 9.0 + t) * min(1.0, wind * 0.1) * 0.5 + 0.5) * 4.0) * 0.25 - 0.3, 0.0) * 2.0 * 0.36 * strength;

    vec4 lightLeft = texture(lightFromLeft, vec2(gl_FragCoord.x, screenHeight - gl_FragCoord.y) / lightSize / 4.0);
	vec4 lightRight = texture(lightFromRight, vec2(gl_FragCoord.x, screenHeight - gl_FragCoord.y) / lightSize / 4.0);
	lightLeft = vec4(lightLeft.x * leftM, lightLeft.y * leftM, lightLeft.z * leftM, 0);
	lightRight = vec4(lightRight.x * rightM, lightRight.y * rightM, lightRight.z * rightM, 0);

    texCoord = texCoord + vec2(0, cos(texCoord.x * 9.0 + t) * 0.07 * texCoord.x + 1.0 / (wind * 0.4 + 0.01) * texCoord.x + yShift * texCoord.x);
    texCoord = texCoord / vec2(min(0.95 + cos(t * wind) * 0.01, max(-0.95 + cos(t * wind) * 0.01, wind * 0.2)), 1.0);
    if (texCoord.x < 0.0 || texCoord.y - texCoord.x * 0.5 < -1.0 || texCoord.x > 1.0 || texCoord.y + texCoord.x * 0.5 > 0.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 0.0);
    } else {
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

	    fragColor = vColor * ambient * satur + lightLeft + lightRight;
    }
}
