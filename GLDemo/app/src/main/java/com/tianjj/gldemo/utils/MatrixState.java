package com.tianjj.gldemo.utils;

import android.opengl.Matrix;

/**
 * 存储矩阵状态的类
 * 
 * @author octopus
 *
 */
public class MatrixState {

	private static float[] mProjMatrix = new float[16]; // 4x4矩阵 投影用
	private static float[] mVMatrix = new float[16]; // 摄像机位置朝向9参数矩阵
	private static float[] mMVPMatrix; // 最后起作用的总变换矩阵

	/**
	 * 设置摄像机的参数
	 *
	 * 摄像机本身处于的位置坐标：
	 * @param cx
	 *            摄像机位置的 x 坐标
	 * @param cy
	 *            摄像机位置的 y 坐标
	 * @param cz
	 *            摄像机位置的 z 坐标
	 *
	 * 摄像机的朝向（看的方向）坐标：
	 * @param tx
	 *            摄像机朝向 x 坐标
	 * @param ty
	 *            摄像机朝向 y 坐标
	 * @param tz
	 *            摄像机朝向 z 坐标
	 *
	 * 摄像机向上的方向上的朝向坐标：
	 * @param upx
	 *            摄像机上方朝向 x 坐标
	 * @param upy
	 *            摄像机上方朝向 y 坐标
	 * @param upz
	 *            摄像机上方朝向 z 坐标
	 */
	public static void setCamera(float cx, float cy, float cz, float tx,
			float ty, float tz, float upx, float upy, float upz) {
		Matrix.setLookAtM(mVMatrix, 0, cx, cy, cz, tx, ty, tz, upx, upy, upz);
	}

	/**
	 * 设置透视投影参数
	 * 
	 * @param left
	 *            近平面的 left
	 * @param right
	 *            近平面的 right
	 * @param bottom
	 *            近平面的 bottom
	 * @param top
	 *            近平面的 top
	 * @param near
	 *            近平面与视点的距离
	 * @param far
	 *            远平面与视点的距离
	 */
	public static void setProjectFrustum(float left, float right, float bottom,
			float top, float near, float far) {
		Matrix.frustumM(mProjMatrix, 0, left, right, bottom, top, near, far);
	}

	/**
	 * 设置正交投影的参数
	 * 
	 * @param left
	 *            近平面的 left
	 * @param right
	 *            近平面的 right
	 * @param bottom
	 *            近平面的 bottom
	 * @param top
	 *            近平面的 top
	 * @param near
	 *            近平面的距离
	 * @param far
	 *            远平面的距离
	 */
	public static void setProjectOrtho(float left, float right, float bottom,
			float top, float near, float far) {
		Matrix.orthoM(mProjMatrix, 0, left, right, bottom, top, near, far);
	}

	/**
	 *
	 * @param fovy y轴方向上的视角角度(一般是45°)
	 * @param aspect 宽高比
	 * @param zNear near 和 far， 是指距离视点的距离， 视点指的是相机的坐标为位置。
	 * @param zFar near 和 far， 是指距离视点的距离， 视点指的是相机的坐标为位置。
	 */
	public static void setProjectPerspective(float fovy, float aspect, float zNear, float zFar) {
		Matrix.perspectiveM(mProjMatrix, 0, fovy, aspect, zNear, zFar);
	}

	/**
	 * 获取物体的总变换矩阵
	 * 
	 * @param spec
	 * @return
	 */
	public static float[] getFinalMatrix(float[] spec) {
		mMVPMatrix = new float[16];
		Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, spec, 0);
		Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);
		return mMVPMatrix;
	}
}
