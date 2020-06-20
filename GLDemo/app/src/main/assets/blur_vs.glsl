precision mediump float;

attribute vec2 aPosition;
attribute highp vec2 aUV;
varying highp vec2 vUV;

void main() {
    vUV = aUV;
    gl_Position = vec4(aPosition, 0.0, 1.0);
}