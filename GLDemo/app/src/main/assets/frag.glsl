precision mediump float;
varying vec2 outTexCoords;
uniform sampler2D texture;
void main() {
    gl_FragColor = texture2D(texture, outTexCoords);
}