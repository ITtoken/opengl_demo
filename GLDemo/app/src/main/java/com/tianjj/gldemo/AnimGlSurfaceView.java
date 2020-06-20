package com.tianjj.gldemo;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.PathInterpolator;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES10.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.*;

public class AnimGlSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final String TAG = "AnimGlSurfaceView";
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private GestureDetector mGestureDetector;
    private WeakReference<Context> mContext;

    public static final int MATRIX_SIZE = 16;
    private static final float[] mViewModel = new float[MATRIX_SIZE];
    private static final float[] mProjectModel = new float[MATRIX_SIZE];
    private static float[] mMVPProject = null;

    private static final float[] mMMatrix = new float[MATRIX_SIZE];
    private float mX = 0f;
    private volatile boolean mbTranslate = true;
    private FloatBuffer mMesh = null;
    private int mTex = -1;
    private int mProgram = -1;

    private boolean toLeft = true;

    private BlurFilter mBlurFilter = null;
    private int attribPosition;
    private int attribTexCoords;
    private int uniformTexture;
    private int uniformProjection;

    public AnimGlSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public AnimGlSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mBlurFilter = new BlurFilter(context);

        mGestureDetector = new GestureDetector(context, new MyGestureListener(this));

        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(1, 1, 1, 1);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    public static float[] getFinalMatrix(float[] spec) {
        mMVPProject = new float[MATRIX_SIZE];
        /*
         *  矩阵乘法计算, 将两个矩阵相乘, 并存入到第三个矩阵中
         *  六个参数 :
         *  ①② 参数 : 结果矩阵, 结果矩阵起始位移
         *  ③④ 参数 : 左矩阵, 结果矩阵起始位移
         *  ⑤⑥ 参数 : 右矩阵, 结果矩阵起始位移
         */
        Matrix.multiplyMM(mMVPProject, 0, mViewModel, 0, spec, 0);
        Matrix.multiplyMM(mMVPProject, 0, mProjectModel, 0, mMVPProject, 0);
        return mMVPProject;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        float ratio = (float) width / height;
        Matrix.setLookAtM(mViewModel, 0, 0, 0, 3, 0, 0, 0, 0, 1, 0);
        Matrix.orthoM(mProjectModel, 0, -ratio, ratio, -1, 1, 2, 10);

        /**
         * near 和 far， 是指距离视点的距离， 视点指的是相机的坐标为位置。
         * ***注意： 相机的正视方向为Z轴负方向。
         *
         * 比如， 相机位置在（0, 0, 3的位置， 那么正视的方向就是Z轴上3向负方向的所有图象）
         * 不过要想看到头像， 就需要投影矩阵的near和far， 只要头像在near和far之间， 就可以看到绘制的图象。
         *
         * 比如，图像绘制的Z轴坐标在Z轴的0平面，相机位置在（0, 0, 3)的位置， 那near就必须 >=0 并且 <3(距离视点的距离不能超过图象距离视点的距离)，
         * 向同， far，必须是 >3，这样near和fra就会将图象包含在视点可看见的范围内。
         *
         * 上述对于正交投影同样是必须的， 只是正交投影在视觉上near变化的话，不会有什么变化。
         * 在透视投影下，near距离图象左边越近，图象胡看起来越大, far只是规定了图像最远能看到的距离，图象位置不变的情况下， far再增大也不会改变图象视觉上的大小。
         */
        Matrix.frustumM(mProjectModel,
                0,
                -ratio,
                ratio,
                -1,
                1,
                1, //near 和 far， 是指距离视点的距离，在ortho正交投影下，因为正交投影的特性，视觉上不会有什么变化, 在透视投影下，
                6);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Log.d(TAG, "onDrawFrame: ..." + getWidth() + " x " + getHeight());
        glViewport(0, 0, getWidth(), getHeight());
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
        glClearColor(0.6f, 0.6f, 0.6f, 1.0f);

        mBlurFilter.init();

        if (mMesh == null) {
            mMesh = createMesh(0, 1, 1, 0);
        }

        if (-1 == mTex) {
            mTex = getTextTextureWith("田佳佳", 800f);
        }

        if (-1 == mProgram) {
            String vertShader = getShaderSource("vert.glsl");
            String fragShader = getShaderSource("frag.glsl");
            mProgram = buildProgram(vertShader, fragShader);
        }

        checkGlError();

        glBindTexture(GL_TEXTURE_2D, mTex);

        prepareMesh();

        mBlurFilter.setAsDrawTarget(5, getWidth(), getHeight());
        glDrawArrays(GL_TRIANGLE_STRIP ,0, 4);
        mBlurFilter.preprare();
        int renderTexture = mBlurFilter.getRenderTexture();

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        prepareMesh();
        glBindTexture(GL_TEXTURE_2D, renderTexture);

        glDrawArrays(GL_TRIANGLE_STRIP,0, 4);

        glDisableVertexAttribArray(attribPosition);
        glDisableVertexAttribArray(attribTexCoords);
        glBindTexture(GL_TEXTURE_2D,0);
    }

    private void prepareMesh() {
        glUseProgram(mProgram);//绑定program到当前的opengl版本环境
        attribPosition = glGetAttribLocation(mProgram, "position");
        attribTexCoords = glGetAttribLocation(mProgram, "texCoords");
        uniformTexture = glGetUniformLocation(mProgram, "texture");
        uniformProjection = glGetUniformLocation(mProgram, "projection");
        glEnableVertexAttribArray(attribPosition);
        glEnableVertexAttribArray(attribTexCoords);
        glUniform1i(uniformTexture, 0);

        if (toLeft) {
            mX -= 0.05f;
            if (mX < -1) {
                toLeft = false;
            }
        } else {
            mX += 0.05f;
            if (mX >= 0) {
                toLeft = true;
            }
        }

        Matrix.setRotateM(mMMatrix, 0, 0, 0, 1.0f, 0);
        Matrix.translateM(mMMatrix, 0, mX, -0.5f, 0);
        float[] finalMatrix = getFinalMatrix(mMMatrix);

        glUniformMatrix4fv(uniformProjection, 1, false, finalMatrix, 0);

        mMesh.position(0);
        glVertexAttribPointer(attribPosition, 3, GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mMesh);

        mMesh.position(3);
        glVertexAttribPointer(attribTexCoords, 2, GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mMesh);
    }

    private String getShaderSource(String s) {
        AssetManager assets = getContext().getAssets();
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

    private int getTextTextureWith(String text, float size) {
        int[] textures = new int[1];

        glActiveTexture(GL_TEXTURE0);
        glGenTextures(1, textures, 0);
        checkGlError();

        int texture = textures[0];
        glBindTexture(GL_TEXTURE_2D, texture);
        checkGlError();

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);


        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setTextSize(size);
        //paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));

        Paint.FontMetricsInt metrics = paint.getFontMetricsInt();
        int height = Math.abs(metrics.bottom - metrics.top);

        Rect rect = new Rect();
        paint.getTextBounds(text, 0, text.length(), rect);
        int width = Math.abs(rect.width());

        Bitmap bitmap = Bitmap.createBitmap(width * 2, height * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawColor(Color.DKGRAY);

        canvas.drawText(text, width / 2, height / 2 + size, paint);

//            Drawable vectorDrawable = getContext().getDrawable(R.mipmap.ic_launcher);
//            bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
//                    vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
//            Canvas canvas = new Canvas(bitmap);
//            vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
//            vectorDrawable.draw(canvas);


        GLUtils.texImage2D(GL_TEXTURE_2D, 0, GL_RGBA, bitmap, GL_UNSIGNED_BYTE, 0);
//        glGenerateMipmap(GL_TEXTURE_2D);

        return texture;
    }

    void onDoubleTap() {
        Log.d(TAG, "onDoubleTap: ...requestRender");
        requestRender();
    }

    private void checkGlError() {
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            Throwable throwable = new Throwable("GLES error: " + error);
            Log.e(TAG, "", throwable);
        }
    }

    private FloatBuffer createMesh(int left, int top, float right, float bottom) {
        final float[] verticesData = {
                // X, Y, Z, U, V
                left, bottom, 0.0f, 0.0f, 1.0f,
                right, bottom, 0.0f, 1.0f, 1.0f,
                left, top, 0.0f, 0.0f, 0.0f,
                right, top, 0.0f, 1.0f, 0.0f,
        };

        final int bytes = verticesData.length * FLOAT_SIZE_BYTES;
        final FloatBuffer triangleVertices = ByteBuffer.allocateDirect(bytes).order(
                ByteOrder.nativeOrder()).asFloatBuffer();
        triangleVertices.put(verticesData).position(0);
        return triangleVertices;
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private final WeakReference<AnimGlSurfaceView> mView;

        private MyGestureListener(AnimGlSurfaceView view) {
            mView = new WeakReference<>(view);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            mView.get().onDoubleTap();
            Log.d(TAG, "onDoubleTap: ...");
            return true;
        }
    }

    public void onCreate() {
        mContext = new WeakReference<Context>(getContext());
    }

    public void onPause() {
        super.onPause();
    }

    public void onDestroy() {
    }


}
