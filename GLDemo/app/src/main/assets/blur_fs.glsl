precision mediump float;

uniform sampler2D uTexture;
uniform vec2 uOffset;

varying highp vec2 vUV;
vec4 fragColor;

void main() {
    fragColor  = texture2D(uTexture, vUV);
    fragColor += texture2D(uTexture, vUV + vec2( uOffset.x,  uOffset.y));
    fragColor += texture2D(uTexture, vUV + vec2( uOffset.x, -uOffset.y));
    fragColor += texture2D(uTexture, vUV + vec2(-uOffset.x,  uOffset.y));
    fragColor += texture2D(uTexture, vUV + vec2(-uOffset.x, -uOffset.y));

    gl_FragColor = vec4(fragColor.rgb * 0.2, 1.0);
}