precision mediump float;
varying vec2 outTexCoords;
uniform sampler2D texture;

void main() {
    vec4 color = texture2D(texture, outTexCoords);

    //当（圆角边角）透明度几乎透明的时候， 丢弃绘制， 这样模板也不会被更新
    //if(color.a < 0.05) {
    //    discard;
    //}
    /*if(gl_FragCoord.x < 600.0) {
        float r = color.r;
        float g = color.g;
        float b = color.b;

        if (r > 0.5 && r < 1.0) {
            r+=0.2;
        }

        if (g > 0.5 && g < 1.0) {
            g+=0.3;
        }

        if (b > 0.5 && b < 1.0) {
            b+=0.1;
        }

        color = vec4(r*0.3, g*0.4, b*0.2, 1.0);
    }*/
    gl_FragColor = color;
}