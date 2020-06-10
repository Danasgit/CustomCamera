package org.danas.customcamera;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import static android.hardware.Camera.getCameraInfo;

public class MainActivity extends Activity implements View.OnClickListener {
    private final String TAG = "MainActivity";
    private Camera mCamera;//相机
    private SurfaceView mPreview;
    private ImageView mPicture;
    private Button mTakePhotoBtn;
    private TextView mChangeCameraBtn;
    private int cameraType = Camera.CameraInfo.CAMERA_FACING_BACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initCamera();
        initView();
    }

    private void initView() {
        mPreview = findViewById(R.id.preview_surface);
        mPicture = findViewById(R.id.picture);
        mTakePhotoBtn = findViewById(R.id.take_photo_btn);
        mChangeCameraBtn = findViewById(R.id.change_camera);
        mChangeCameraBtn.setOnClickListener(this);
        mTakePhotoBtn.setOnClickListener(this);
        mPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                /**
                 * The SurfaceHolder must already contain a surface when this method is called.
                 * If you are using SurfaceView, you will need to register a SurfaceHolder.Callback
                 * with SurfaceHolder#addCallback(SurfaceHolder.Callback) and wait for
                 * SurfaceHolder.Callback#surfaceCreated(SurfaceHolder) before
                 * calling setPreviewDisplay() or starting preview.
                 * 相机的预览必须在surfaceCreated后调用，否则黑屏且没有任何提示哦
                 */
                try {
                    mCamera.setPreviewDisplay(mPreview.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mCamera.startPreview();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.e("TAG", "初始化surfaceChanged时的Holder对象" + mPreview.getHolder().toString());
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

    }

    private void initCamera() {
        mCamera = Camera.open();
        mCamera.setDisplayOrientation(90);
        setCameraSize(mCamera,
                ScreenUtils.getScreenHeight(this)
                , ScreenUtils.getScreenWidth(this));
    }

    public static Camera getCamera(int cameraType) {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == cameraType) {
                return Camera.open(i);
            }
        }
        return null;
    }

    private void setCameraSize(Camera camera, float needW, float needH) {
        if(null == camera ) return;
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> list = parameters.getSupportedPreviewSizes();
        /**
         * 这个返回的是所有camera支持的尺寸，需要注意的是并不是所有我们需要的尺寸摄像头都支持，
         * 比如我现在的画布尺寸是宽230高120，这个尺寸摄像头是绝对不支持的所以我们需要在摄像头
         * 支持的所有尺寸中选择以一个最接近我们目标的
         */
        float needRatio = needW/needH;
        Log.e("我需要的宽高比为", String.valueOf(needRatio));
        LinkedHashMap<Float, Camera.Size> map = new LinkedHashMap<>();
        float bestRatio = 0;
        for (Camera.Size size : list){
            Log.e("Camera.Size", size.width + "," + size.height + "," + (float)size.width/size.height);
            /**
             * 先把所有的尺寸打出来让大家有一个认识
             */
            /**
             * 将当前宽高比的首位，存入map
             */
            if(!map.containsKey((float)size.width/size.height))
                map.put((float)size.width/size.height, size);

            if(bestRatio == 0 || Math.abs(needRatio - (float)size.width/size.height) < Math.abs(needRatio - bestRatio)) {
                bestRatio = (float)size.width/size.height;
            }
        }
        Camera.Size beatSize = map.get(bestRatio);
        Log.e("最佳的Camera.Size", beatSize.width + "---" + beatSize.height);

        parameters.setPreviewSize(beatSize.width, beatSize.height);
        camera.setParameters(parameters);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(mCamera != null) {
            mCamera.startPreview();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mCamera != null) {
            mCamera.stopPreview();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mCamera != null) {
            mCamera.release();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.change_camera:
                if(null != mCamera) {
                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera = null;
                    Camera camera = getCamera(cameraType == Camera.CameraInfo.CAMERA_FACING_BACK
                    ? Camera.CameraInfo.CAMERA_FACING_FRONT
                            :Camera.CameraInfo.CAMERA_FACING_BACK);
                    if(null != camera) {
                        cameraType = cameraType == Camera.CameraInfo.CAMERA_FACING_BACK
                                ? Camera.CameraInfo.CAMERA_FACING_FRONT
                                :Camera.CameraInfo.CAMERA_FACING_BACK;
                        mCamera = camera;

                        mCamera.setDisplayOrientation(90);
                        setCameraSize(mCamera,
                                ScreenUtils.getScreenHeight(this)
                                , ScreenUtils.getScreenWidth(this));
                        try {
                            mCamera.setPreviewDisplay(mPreview.getHolder());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mCamera.startPreview();
                    } else {
                        initCamera();
                        try {
                            mCamera.setPreviewDisplay(mPreview.getHolder());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mCamera.startPreview();
                    }
                } else {
                    initCamera();
                }
                break;
            case R.id.take_photo_btn:
                if(mCamera != null) {
                    /**
                     *   相机必须执行在startView之后，并且takePicture
                     *   会导致预览停止
                     *    mTakePhotoBtn.setClickable(false);
                     *    和try{}catch(){}都是为了处理相机状态错误导致的异常
                     */
                    mTakePhotoBtn.setClickable(false);
                    try {
                        mCamera.takePicture(new Camera.ShutterCallback() {
                            @Override
                            public void onShutter() {
                                Log.e(TAG, "用户按下快门时调用");

                            }
                        }, new Camera.PictureCallback() {
                            @Override
                            public void onPictureTaken(byte[] data, Camera camera) {
                                Log.e(TAG, "raw" + data);
                            }
                        }, new Camera.PictureCallback() {
                            @Override
                            public void onPictureTaken(byte[] data, Camera camera) {
                                if(mCamera != null) mCamera.startPreview();
                                mTakePhotoBtn.setClickable(true);
                                /**
                                 * 此处可以将文件保存到sdCard
                                 */
                                /**
                                 * onPictureTaken所回调的data是相当大的，并且是未旋转的角度，不建议直接
                                 * 显示在ImageView上
                                 * 下面处理的Bitmap的角度和固定尺寸压缩
                                 */
                                mPicture.setImageBitmap(BitmapUtils.adjustPhotoRotation(
                                        BitmapUtils.compressScaleBitmap(
                                                BitmapFactory.decodeByteArray(data, 0, data.length),
                                                600, 600), cameraType == 0 ?90:270));
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        mTakePhotoBtn.setClickable(true);
                    }
                }
                break;


        }
    }
}