package com.tianjj.gldemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import android.util.Log;

import static android.opengl.GLES10.GL_RGBA;
import static android.opengl.GLES20.*;

public class GLFrameBuffer {
    private final int mFBOName;
    private final int mTextureName;
    private int mWidth;
    private int mHeight;
//    private final int mRenderBuffer;

    public GLFrameBuffer() {
        int[] tmp = new int[1];
        glGenFramebuffers(1, tmp, 0);
        int status = getStatus();
        mFBOName = tmp[0];

        tmp = new int[1];
        glGenTextures(1, tmp, 0);
        mTextureName = tmp[0];
//        glGenRenderbuffers(1, tmp, 2);
//        mRenderBuffer = tmp[2];
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

        glBindTexture(GL_TEXTURE_2D, mTextureName);
        checkGlError();
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        //Bitmap bitmap = Bitmap.createBitmap(width * 2, height * 2, Bitmap.Config.ARGB_8888);
        //GLUtils.texImage2D(GL_TEXTURE_2D, 0, GL_RGBA, bitmap, GL_UNSIGNED_BYTE, 0);

        int[] rb = new int[1];
        glGenRenderbuffers(1, rb, 0);
        checkGlError();
        glBindRenderbuffer(GL_RENDERBUFFER, rb[0]);//绑定renderbuffer
        checkGlError();
        //设置renderbuffer的缓冲大小
        glRenderbufferStorage(GL_RENDERBUFFER, GL_RGBA4, width, height);
        checkGlError();

        bind();
        int st = getStatus();
        if (st == GL_FRAMEBUFFER_COMPLETE) {
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, rb[0]);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mTextureName, 0);
        } else {
            Log.d("FB", "No completed status " + st);
        }
        st = getStatus();
        unbind();
        st = getStatus();
    }

    private void checkGlError() {
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            Throwable throwable = new Throwable("GLES error: " + error);
            Log.e("FB", "", throwable);
            throw new IllegalStateException();
        }
    }

    int getStatus() {
        return glCheckFramebufferStatus(GL_FRAMEBUFFER);
    }

    int getTextureName() {
        return mTextureName;
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, mFBOName);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
}
