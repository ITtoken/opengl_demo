attribute vec4 position;
attribute vec2 texCoords;
varying vec2 outTexCoords;
uniform mat4 projection;
void main() {
    outTexCoords = texCoords;
    gl_Position = projection * position;
}