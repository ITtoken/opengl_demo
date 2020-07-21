package com.tianjj.gldemo.gl;

import android.util.Log;

import static android.opengl.GLES20.*;

public class GLFrameBuffer {
    private final int mFBOName;
    private final int mTextureName;
    private int mWidth;
    private int mHeight;

    public GLFrameBuffer() {
        int[] tmp = new int[1];
        glGenFramebuffers(1, tmp, 0);
        int status = getStatus();
        mFBOName = tmp[0];

        tmp = new int[1];
        glGenTextures(1, tmp, 0);
        mTextureName = tmp[0];
    }

    public int getWidth () {
        return mWidth;
    }

    public  int getHeight() {
        return mHeight;
    }

    public void allocateBuffers(int width, int height) {
        mWidth = width;
        mHeight = height;

        bind();
        checkGlError();

        glBindTexture(GL_TEXTURE_2D, mTextureName);
        checkGlError();
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        checkGlError();
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        checkGlError();
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        checkGlError();
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        checkGlError();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
        checkGlError();

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mTextureName, 0);

        glBindTexture(GL_TEXTURE_2D, 0);

        unbind();
        checkGlError();
    }

    private void checkGlError() {
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            Throwable throwable = new Throwable("GLES error: " + error);
            Log.e("FB", "", throwable);
            throw new IllegalStateException();
        }
    }

    public int getStatus() {
        return glCheckFramebufferStatus(GL_FRAMEBUFFER);
    }

    public int getTextureName() {
        return mTextureName;
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, mFBOName);
        int bindSt = getStatus();
        if (bindSt != GL_FRAMEBUFFER_COMPLETE) {
            Log.e("bind", "bind: failed," + bindSt, new Throwable());
        }
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
}
