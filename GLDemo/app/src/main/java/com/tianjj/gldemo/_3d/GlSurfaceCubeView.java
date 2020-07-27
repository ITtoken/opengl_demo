package com.tianjj.gldemo._3d;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.tianjj.gldemo.R;
import com.tianjj.gldemo.utils.MatrixState;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES10.GL_RGBA;
import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_NO_ERROR;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_REPEAT;
import static android.opengl.GLES20.GL_SRC_ALPHA;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_TRUE;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

public class GlSurfaceCubeView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final String TAG = "GlSurfaceCubeView";
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

    private int attribPosition;
    private int attribTexCoords;
    private int uniformTexture;
    private int uniformProjection;
    private int mBlurRadius;
    private int mTex0 = -1;

    public GlSurfaceCubeView(Context context) {
        super(context);
        init(context);
    }

    public GlSurfaceCubeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mGestureDetector = new GestureDetector(context, new MyGestureListener(this));

        Matrix.setIdentityM(mMMatrix, 0);

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
        //Matrix.orthoM(mProjectModel, 0, -ratio, ratio, -1, 1, 1, 10);

        /**
         * perspectiveM 和 frustumM 一样， 都是设置正交投影， 用来替代perspectiveM的一些缺陷
         *
         * 参数：
         * 1. 需要变换投影矩阵
         * 2. 矩阵偏移
         * 3. y轴防线上的视角
         * 4. 宽高比
         * 5. near 和 far， 是指距离视点的距离， 视点指的是相机的坐标为位置。
         *
         */
        Matrix.perspectiveM(mProjectModel, 0, 45.0f, ratio, 2f, 100);

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
/*        Matrix.frustumM(mProjectModel,
                0,
                -ratio,
                ratio,
                -1,
                1,
                2f, //near 和 far， 是指距离视点的距离，在ortho正交投影下，因为正交投影的特性，视觉上不会有什么变化, 在透视投影下，
                100);*/
    }

    private void save() {
        float[] tmpMtrix = new float[MATRIX_SIZE];

        Matrix.setIdentityM(tmpMtrix, 0);
        /**
         * 这里需要想Z轴正方向(垂直屏幕向外)移动4~6个距离才能看见， 因为
         * 矩阵设置成单位矩阵后， 没有了M V P效果， 所以屏幕显示的区域范围都变成了-1~1，
         * 但是mesh的设置是Z轴为-5， 所以需要将Z轴转换到-1~1的范围内绘制结果才能显示出来
         * */
        Matrix.translateM(tmpMtrix, 0, 0, 0, 4);
        Matrix.rotateM(tmpMtrix, 0, 180, 0, 0, 1);
        glUniformMatrix4fv(uniformProjection, 1, false, tmpMtrix, 0);
    }

    private void restore() {
        glUniformMatrix4fv(uniformProjection, 1, false, mMVPProject, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //Log.d(TAG, "onDrawFrame: ..." + getWidth() + " x " + getHeight());
        /**
         * 设置绘制绘制视角范围, 片段着色器在处理的时候会根据glViewport设置的范围来确定处理哪些像素
         * (所以在可以的时候,适当缩小glViewPort的范围可以进行优化)
         * 单位是像素, 左下角为(0,0)点, x向右递增, y向上递增.
         */
        glViewport(0, 0, getWidth(), getHeight());
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
        glClearColor(0.6f, 0.6f, 0.6f, 0.0f);

        if (-1 == mTex0) {
            mTex0 = getRoundRectTexture();
        }

        if (-1 == mTex) {
            mTex = getImageTexture();//getTextTextureWith("TianJJ GL Test", 800f);
        }

        if (mMesh == null) {
            /**
             * opengl的坐标中心为(0,0)点,左下角(-1, -1),右下角(1, -1), 右上角(1, 1), 左上角(-1, 1).
             * 所以参数+1/-1代表坐标向右/向左移动半个屏幕单位.
             **/
            mMesh = createMesh();
        }

        if (-1 == mProgram) {
            String vertShader = getShaderSource("cube_vert.glsl");
            String fragShader = getShaderSource("cube_frag.glsl");
            mProgram = buildProgram(vertShader, fragShader);

            glUseProgram(mProgram);

            attribPosition = glGetAttribLocation(mProgram, "position");
            attribTexCoords = glGetAttribLocation(mProgram, "texCoords");
            uniformTexture = glGetUniformLocation(mProgram, "texture");
            uniformProjection = glGetUniformLocation(mProgram, "projection");
        }

        checkGlError();

        prepareMesh(false);
        glEnable(GL_DEPTH_TEST);
        glBindTexture(GL_TEXTURE_2D, mTex);
        glDrawArrays(GL_TRIANGLES, 0, 36);


        prepareMesh(true);
//        glEnable(GL_BLEND);
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBindTexture(GL_TEXTURE_2D, mTex0);
        glDrawArrays(GL_TRIANGLES, 0, 36);

//        glDisable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);

        glDisableVertexAttribArray(attribPosition);
        glDisableVertexAttribArray(attribTexCoords);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private int getRoundRectTexture() {
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
        paint.setColor(Color.parseColor("#333333"));
        paint.setAntiAlias(true);

        Bitmap bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawColor(Color.TRANSPARENT);

        canvas.drawRoundRect(0, 0, 200, 200, 50 ,50, paint);


        GLUtils.texImage2D(GL_TEXTURE_2D, 0, GL_RGBA, bitmap, GL_UNSIGNED_BYTE, 0);
//        glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);


        return texture;
    }

    private void prepareMesh(boolean copy) {
        glUseProgram(mProgram);//绑定program到当前的opengl版本环境
        glEnableVertexAttribArray(attribPosition);
        glEnableVertexAttribArray(attribTexCoords);
        glUniform1i(uniformTexture, 0);

        mX += 0.5f;
        if (mX > 360) {
            mX = 0;
        }

        float[] rotateM = rotateM(mMMatrix, mX);

        if (copy) {
            /*float[] T3 = new float[MATRIX_SIZE];
            Matrix.setIdentityM(T3, 0);
            Matrix.translateM(T3, 0, 1, -1, -2);
            Matrix.multiplyMM(rotateM, 0, T3, 0, rotateM, 0);*/

            MatrixState.translate(rotateM, 1, -1, -2);
        }

        float[] finalMatrix = getFinalMatrix(rotateM);

        glUniformMatrix4fv(uniformProjection, 1, false, finalMatrix, 0);

        mMesh.position(0);
        glVertexAttribPointer(attribPosition, 3, GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mMesh);

        mMesh.position(3);
        glVertexAttribPointer(attribTexCoords, 2, GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mMesh);
    }

    private float[] rotateM(float[] outMatrix, float angle) {
        /**
         * 注意需要先初始化为单位矩阵在操作， 否则会有问题
         */
        float[] T1 = new float[MATRIX_SIZE];
        float[] T2 = new float[MATRIX_SIZE];
        float[] R = new float[MATRIX_SIZE];
        Matrix.setIdentityM(T1, 0);
        Matrix.setIdentityM(T2, 0);
        Matrix.setIdentityM(R, 0);

        //Matrix.translateM(T1, 0, 0, 0, 5);
        Matrix.setRotateM(R, 0, angle, 1.0f, 0.5f, 0.5f);
        Matrix.translateM(T2, 0, 0, 0, -3);

        /**
         * Opengl是列变换矩阵， 需要从右往左乘
         *
         * 先T1变换， 将图像移动到原点（之前Z在-5位置）
         * 再进行R变换，旋转
         * 最后T变换， 将图像移回到原来的位置（Z轴-5位置）
         * T2 * R * T1 * modelMatrix
         *
         * 这样就是原地做旋转操作的效果
         */
        Matrix.setIdentityM(outMatrix, 0);
        Matrix.multiplyMM(outMatrix, 0, T1, 0, outMatrix, 0);
        Matrix.multiplyMM(outMatrix, 0, R, 0, outMatrix, 0);
        Matrix.multiplyMM(outMatrix, 0, T2, 0, outMatrix, 0);
        return outMatrix;
    }

    private float[] rotateM2(float[] outMatrix, float angle) {
        MatrixState.rotate(outMatrix, angle, 1.0f, 0.5f, 0.5f);
        MatrixState.translate(outMatrix, 0, 0, -3);

        return outMatrix;
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

    private int getImageTexture() {
        int[] textures = new int[1];

        glActiveTexture(GL_TEXTURE0);
        glGenTextures(1, textures, 0);
        checkGlError();

        int texture = textures[0];
        glBindTexture(GL_TEXTURE_2D, texture);
        checkGlError();

        /**
         * GL_TEXTURE_MIN_FILTER 和 GL_TEXTURE_MAG_FILTER分别指定了当纹理缩小（纹理内容比顶点坐标区域大， 会缩放纹理）
         * 和纹理放大（纹理内容比顶点坐标区域小， 会拉伸纹理）的内容处理方式
         *
         * GL_TEXTURE_WRAP_S 和 GL_TEXTURE_WRAP_T
         * 分表指定了，当纹理坐标大于纹理本身内容(大于1)的时候， S 和 T坐标方向（相当于X 和 Y轴）上多出来的区域怎么填充
         */
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.timg);
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, GL_RGBA, bitmap, GL_UNSIGNED_BYTE, 0);
        return texture;
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

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);


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

    private FloatBuffer createMesh() {
        final float[] vertices = {
                -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,
                0.5f, -0.5f, -0.5f, 1.0f, 0.0f,
                0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
                0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
                -0.5f, 0.5f, -0.5f, 0.0f, 1.0f,
                -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,

                -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
                0.5f, -0.5f, 0.5f, 1.0f, 0.0f,
                0.5f, 0.5f, 0.5f, 1.0f, 1.0f,
                0.5f, 0.5f, 0.5f, 1.0f, 1.0f,
                -0.5f, 0.5f, 0.5f, 0.0f, 1.0f,
                -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,

                -0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
                -0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
                -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
                -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
                -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
                -0.5f, 0.5f, 0.5f, 1.0f, 0.0f,

                0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
                0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
                0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
                0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
                0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
                0.5f, 0.5f, 0.5f, 1.0f, 0.0f,

                -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
                0.5f, -0.5f, -0.5f, 1.0f, 1.0f,
                0.5f, -0.5f, 0.5f, 1.0f, 0.0f,
                0.5f, -0.5f, 0.5f, 1.0f, 0.0f,
                -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
                -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,

                -0.5f, 0.5f, -0.5f, 0.0f, 1.0f,
                0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
                0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
                0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
                -0.5f, 0.5f, 0.5f, 0.0f, 0.0f,
                -0.5f, 0.5f, -0.5f, 0.0f, 1.0f
        };

        final int bytes = vertices.length * FLOAT_SIZE_BYTES;
        final FloatBuffer triangleVertices = ByteBuffer.allocateDirect(bytes).order(
                ByteOrder.nativeOrder()).asFloatBuffer();
        triangleVertices.put(vertices).position(0);
        return triangleVertices;
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private final WeakReference<GlSurfaceCubeView> mView;

        private MyGestureListener(GlSurfaceCubeView view) {
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
