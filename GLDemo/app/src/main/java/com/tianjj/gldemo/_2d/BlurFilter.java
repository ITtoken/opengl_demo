package com.tianjj.gldemo._2d;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.tianjj.gldemo.gl.GLFrameBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.*;

public class BlurFilter {
    private static final String TAG = "BlurFilter";
    private final Context mContext;
    private GLFrameBuffer mPingFbo;
    private GLFrameBuffer mPongFbo;
    private GLFrameBuffer mCompositionFbo;
    private int mBlurProgram;

    private static final float kFboScale = 0.25f;
    private static final int kMaxPasses = 4;
    private int mRadius;
    private int mBPosLoc;
    private int mBUvLoc;
    private int mBTextureLoc;
    private int mBOffsetLoc;
    private int mVbo;
    private GLFrameBuffer mLastDrawTarget;

    private boolean inited = false;

    public BlurFilter(Context context) {
        mContext = context;
    }

    public void init() {
        if (!inited) {
            initShader();
            initVBO();

            mCompositionFbo = new GLFrameBuffer();
            mPingFbo = new GLFrameBuffer();
            mPongFbo = new GLFrameBuffer();
            inited = true;
        }
    }

    private void initVBO() {
        float size = 2.0f;
        float translation = 1.0f;
        float vboData[] = {
                // Vertex data
                translation - size, -translation - size,
                translation - size, -translation + size,
                translation + size, -translation + size,
                // UV data
                0.0f, 0.0f - translation,
                0.0f, size - translation,
                size, size - translation
        };

        int[] tmp = new int[1];
        glGenBuffers(1, tmp, 0);
        checkGlError();
        mVbo = tmp[0];

        FloatBuffer triangleVertices = ByteBuffer.allocateDirect(vboData.length * 4).order(
                ByteOrder.nativeOrder()).asFloatBuffer();
        triangleVertices.put(vboData).position(0);
        glBindBuffer(GL_ARRAY_BUFFER, mVbo);
        checkGlError();
        glBufferData(GL_ARRAY_BUFFER, vboData.length*4, triangleVertices, GL_STATIC_DRAW);
        checkGlError();
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        checkGlError();
    }

    private void initShader() {
        String vs = getShaderSource("blur_vs.glsl");
        String fs = getShaderSource("blur_fs.glsl");
        mBlurProgram = buildProgram(vs, fs);

        mBPosLoc = glGetAttribLocation(mBlurProgram, "aPosition");
        mBUvLoc = glGetAttribLocation(mBlurProgram, "aUV");
        mBTextureLoc = glGetUniformLocation(mBlurProgram, "uTexture");
        mBOffsetLoc = glGetUniformLocation(mBlurProgram, "uOffset");
    }

    private void useBlurProgram() {
        glUseProgram(mBlurProgram);
    }

    private void viewPort(int x, int y, int width, int height) {
        glViewport(x, y, width, height);
    }

    public void setAsDrawTarget(int radius, int width, int height) {
        mRadius = radius;

        mCompositionFbo.allocateBuffers(width, height);

        int fboWidth = (int) (width /** kFboScale*/);
        int fboHeight = (int) (height /** kFboScale*/);
        mPingFbo.allocateBuffers(fboWidth, fboHeight);
        mPongFbo.allocateBuffers(fboWidth, fboHeight);

        if (mPingFbo.getStatus() != GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "Invalid ping buffer");
            return;
        }
        if (mPongFbo.getStatus() != GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "Invalid pong buffer");
            return;
        }
        if (mCompositionFbo.getStatus() != GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "Invalid composition buffer");
            return;
        }

        mCompositionFbo.bind();
        checkGlError();
        viewPort(0, 0, mCompositionFbo.getWidth(), mCompositionFbo.getHeight());
    }

    public void preprare() {
        float radius = mRadius / 6.0f;

        int passes = Math.min(kMaxPasses, (int) Math.ceil(radius));

        float radiusByPasses = radius / (float) passes;
        float stepX = radiusByPasses / (float) mCompositionFbo.getWidth();
        float stepY = radiusByPasses / (float) mCompositionFbo.getHeight();


        useBlurProgram();
        checkGlError();
        glActiveTexture(GL_TEXTURE0);
        checkGlError();
        glUniform1i(mBTextureLoc, 0);
        checkGlError();
        glBindTexture(GL_TEXTURE_2D, mCompositionFbo.getTextureName());
        checkGlError();
        glUniform2f(mBOffsetLoc, stepX, stepY);
        checkGlError();
        glViewport(0, 0, mPingFbo.getWidth(), mPingFbo.getHeight());
        mPingFbo.bind();
        checkGlError();
        drawMesh(mBUvLoc, mBPosLoc);

        int[] get0 = new int[1];
        glGetIntegerv(GL_FRAMEBUFFER_BINDING, get0, 0);

        glGetIntegerv(GL_TEXTURE_BINDING_2D, get0, 0);

        checkGlError();

        GLFrameBuffer read = mPingFbo;
        GLFrameBuffer draw = mPongFbo;
        glViewport(0, 0, draw.getWidth(), draw.getHeight());
        for (int i = 1; i < passes; i++) {
            draw.bind();

            glBindTexture(GL_TEXTURE_2D, read.getTextureName());
            glUniform2f(mBOffsetLoc, stepX * i, stepY * i);

            int[] get = new int[1];
            glGetIntegerv(GL_FRAMEBUFFER_BINDING, get, 0);

            glGetIntegerv(GL_TEXTURE_BINDING_2D, get, 0);

            drawMesh(mBUvLoc, mBPosLoc);
            checkGlError();

            // Swap buffers for next iteration
            GLFrameBuffer tmp = draw;
            draw = read;
            read = tmp;
        }
        mLastDrawTarget = read;
    }

    public int getRenderTexture() {
        return mLastDrawTarget.getTextureName();
    }

    void drawMesh(int uv, int position) {
        glEnableVertexAttribArray(uv);
        glEnableVertexAttribArray(position);
        glBindBuffer(GL_ARRAY_BUFFER, mVbo);
        checkGlError();
        glVertexAttribPointer(position, 2 /* size */, GL_FLOAT, false,
                0 /* stride */, 0 /* offset */);
        checkGlError();
        glVertexAttribPointer(uv, 2 /* size */, GL_FLOAT, false, 0 /* stride */,
                6*4 /* offset */);
        checkGlError();
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        // draw mesh
        glDrawArrays(GL_TRIANGLE_STRIP, 0 /* first */, 3 /* count */);
    }

    private void checkGlError() {
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            Throwable throwable = new Throwable("GLES error: " + error);
            Log.e(TAG, "", throwable);
            throw new IllegalStateException();
        }
    }

    private String getShaderSource(String s) {
        AssetManager assets = mContext.getAssets();
        try {
            InputStream open = assets.open(s);
            int len = open.available();
            byte[] buffer = new byte[len];
            open.read(buffer);
            String result = new String(buffer, "utf8");
//            Log.d(TAG, "getShaderSource: " + result);
            return result;
        } catch (IOException e) {
            return null;
        }
    }

    private int buildShader(String source, int type) {
        int shader = glCreateShader(type);

        glShaderSource(shader, source);
        checkGlError();

        glCompileShader(shader);
        checkGlError();

        int[] status = new int[1];
        glGetShaderiv(shader, GL_COMPILE_STATUS, status, 0);
        if (status[0] != GL_TRUE) {
            String error = glGetShaderInfoLog(shader);
            Log.d(TAG, "Error while compiling shader: " + error);
            glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    private int buildProgram(String vertex, String fragment) {
        int vertexShader = buildShader(vertex, GL_VERTEX_SHADER);
        if (vertexShader == 0) return 0;

        int fragmentShader = buildShader(fragment, GL_FRAGMENT_SHADER);
        if (fragmentShader == 0) return 0;

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        checkGlError();

        //glDeleteShader(vertexShader);
        //glDeleteShader(fragmentShader);

        int[] status = new int[1];
        glGetProgramiv(program, GL_LINK_STATUS, status, 0);
        if (status[0] != GL_TRUE) {
            String error = glGetProgramInfoLog(program);
            Log.d(TAG, "Error while linking program:\n" + error);
            glDeleteProgram(program);
            return 0;
        }

        return program;
    }


}
