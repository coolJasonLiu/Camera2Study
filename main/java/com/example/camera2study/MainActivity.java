package com.example.camera2study;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final String[] Permission_need =
            {Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO,
                    Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION};
    //后置摄像头信息
    private String backCameraId;
    private CameraCharacteristics backCharacteristics;
    //前置摄像头信息
    private String frontCameraId;
    private CameraCharacteristics frontCharacteristics;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession mCaptureSession;
    private com.example.camera2study.textureView camera_preview;
    private List<Surface> surfaceList = new ArrayList<>();
    private CaptureRequest.Builder requestBuilder;
    //private CameraCharacteristics cameraCharacteristics;
    private Surface previewSurface;//用于预览的surface
    private SurfaceTexture mSurfaceTexture;
    private Button btn_take;
    private TextView take;
    private TextView recode;
    private TextView bph;
    private TextView dj;
    private TextView kmsj;
    private TextView iso;
    private TextView seek_dj_tx;
    private TextView seek_kmsj_tx;
    private TextView seek_iso_tx;
    private TextView open;
    private boolean flag = false;
    private boolean btn_flag = false;
    private Size size;
    private ImageReader imageReader;
    private Surface jpegPreviewSurface;//拍照时用于获取数据并保存的surface
    private CaptureRequest.Builder captureImageRequestBuilder;//用于拍照的 CaptureRequest.Builder对象
    private String[] km_time = {"1/500", "1/250", "1/125", "1/60", "1/30", "1/15", "1/8", "1/4", "1/2", "1", "2", "4", "8", "16"};
    private long[] km_time_in = {2000000, 4000000, 8000000, 10000000, 20000000, 40000000, 125000000, 250000000, 500000000, 1000000000, 2000000000, 4000000000L, 8000000000L, 16000000000L};
    private CaptureRequest.Builder setBuilder;
    private boolean isOpen = false;
    private Surface previewDataSurface;//用于防止卡顿的surface
    private Surface mediaSurface;
    private MediaRecorder mMediaRecorder = new MediaRecorder();
    private CaptureRequest.Builder mPreviewBuilder;
    private SeekBar seek_dj;
    private SeekBar seek_kmsj;
    private SeekBar seek_iso;
    private LinearLayout dd;
    private LinearLayout zidong;
    private LinearLayout bcd;
    private LinearLayout yy;
    private LinearLayout qt;
    private LinearLayout yt;
    private LinearLayout ly_setAll;
    View ly_bph;
    View ly_dj;
    View ly_kmsj;
    View ly_iso;

    /**
     * 初始化数据, initView, initEvent
     * */
    private void initView() {
        camera_preview = findViewById(R.id.camera_preview);
        btn_take = findViewById(R.id.btn_take);
        open = findViewById(R.id.open);
        take = findViewById(R.id.tx_take);
        recode = findViewById(R.id.tx_recode);

        kmsj = findViewById(R.id.kmsj);
        ly_kmsj = View.inflate(this,R.layout.layout_kmsj,null);
        seek_kmsj_tx = ly_kmsj.findViewById(R.id.seek_kmsj_tx);//kmsj
        iso = findViewById(R.id.iso);
        ly_iso = View.inflate(this,R.layout.layout_iso,null);
        seek_iso_tx = ly_iso.findViewById(R.id.seek_iso_tx);//iso
        dj = findViewById(R.id.dj);
        ly_dj = View.inflate(this,R.layout.layout_dj,null);
        seek_dj_tx = ly_dj.findViewById(R.id.seek_dj_tx);//dj
        bph = findViewById(R.id.bph);
        ly_bph = View.inflate(this,R.layout.layout_bph,null);//bph

        ly_setAll = findViewById(R.id.cc);
        dd = findViewById(R.id.dd);
    }
    private void initEvent() {
        btn_take.setOnClickListener(this);
        open.setOnClickListener(this);
        take.setOnClickListener(this);
        recode.setOnClickListener(this);
        findViewById(R.id.ln_kmsj).setOnClickListener(this);
        findViewById(R.id.ln_iso).setOnClickListener(this);
        findViewById(R.id.ln_dj).setOnClickListener(this);
        findViewById(R.id.ln_bph).setOnClickListener(this);
        findViewById(R.id.btn_reset).setOnClickListener(this);
    }

    /**
     * * */
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkRequiredPermissions();
        initCamera();
        openCamera();
        initView();
        initEvent();
        camera_preview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                mSurfaceTexture = surfaceTexture;
            }
            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            }
            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                return false;
            }
            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
            }
        });
    }
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.open) {
            if (!isOpen) {
                startPreview();
                isOpen = true;
                open.setText("开始预览");
            } else {
                //stopPreview();
                isOpen = false;
                open.setText("停止预览");
            }
        } else if (id == R.id.tx_take) {
            take.setText("拍照中");
            recode.setText("录像");
            ly_setAll.setVisibility(View.VISIBLE);
            btn_flag = false;
        } else if (id == R.id.tx_recode) {
            take.setText("拍照");
            recode.setText("录像中");
            ly_setAll.setVisibility(View.INVISIBLE);
            btn_flag = true;
        } else if (id == R.id.btn_take) {
            if (btn_flag) {
                if (flag) {
                    Log.d("TAG","停止录像");
                    stopRecode();
                    flag = false;
                } else {
                    Log.d("TAG","开始录像");
                    startRecode();
                    flag = true;
                }
            } else {
                System.out.println("开拍！");
                takePic();
            }
        } else if (id == R.id.ln_kmsj) {
            seekbarkmsj();
        } else if (id == R.id.ln_iso) {
            seekbariso();
        } else if (id == R.id.ln_dj) {
            seekbardj();
        } else if (id == R.id.ln_bph) {
            setofbph();
        } else if (id == R.id.btn_reset) {
            bph.setText("自动");
            dj.setText("自动");
            kmsj.setText("自动");
            iso.setText("自动");
            reset();
            dd.removeAllViews();
        }
    }
/*else if (id == R.id.btn_reset) {
        Log.d("TAG","重置");
    }*/

    private void reset(){
        seek_dj_tx.setText("自动");
        seek_kmsj_tx.setText("自动");
        seek_iso_tx.setText("自动");
        if (setBuilder != null){
            try {
                setBuilder = null;
                setBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        setBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        setBuilder.addTarget(previewSurface);
        setBuilder.addTarget(previewDataSurface);
        try {
            mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    /**
     * 参数设计
     * 1.快门时间, seekbarkmsj()
     * 2.iso感光度, seekbariso()
     * 3.对焦, seekbardj()
     * 4.白平衡, seekbarbph()
     * */
    private void set_mode(){
        try {
            setBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
        if (setBuilder != null){
            setBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_OFF_KEEP_STATE);
            setBuilder.addTarget(previewSurface);
            setBuilder.addTarget(previewDataSurface);
            try {
                mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }
    //快门时间， 1/500，，，，16.
    private void seekbarkmsj(){
        dd.removeAllViews();
        set_mode();
        seek_kmsj = ly_kmsj.findViewById(R.id.seek_kmsj);
        seek_kmsj.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                seek_kmsj_tx.setText(km_time[i]);
                kmsj.setText(km_time[i]);
                if (setBuilder != null){
                    setBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_OFF);
                    setBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME,km_time_in[i]);
                    try {
                        mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        dd.addView(ly_kmsj);
    }
    //感光度， 100，，，，3200.
    private void seekbariso(){
        dd.removeAllViews();
        set_mode();
        seek_iso = ly_iso.findViewById(R.id.seek_iso);
        seek_iso.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                seek_iso_tx.setText(""+i*100);
                iso.setText(""+i*100);
                if (setBuilder != null){
                    setBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_OFF);
                    setBuilder.set(CaptureRequest.SENSOR_SENSITIVITY,i*100);
                    try {
                        mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        dd.addView(ly_iso);
    }
    //对焦， 1，，，，11.
    private void seekbardj(){
        dd.removeAllViews();
        set_mode();
        seek_dj = ly_dj.findViewById(R.id.seek_dj);
        seek_dj.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                seek_dj_tx.setText(""+i);
                dj.setText(""+i);
                if (setBuilder != null){
                    setBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_OFF_KEEP_STATE);
                    setBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE,(float)i);
                    Log.d("TAG","点击了对焦");
                    try {
                        mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        dd.addView(ly_dj);
    }
    private void setofbph() {
        dd.removeAllViews();
        set_mode();
        zidong = ly_bph.findViewById(R.id.zidong);
        bcd = ly_bph.findViewById(R.id.bcd);
        yy = ly_bph.findViewById(R.id.yy);
        qt = ly_bph.findViewById(R.id.qt);
        yt = ly_bph.findViewById(R.id.yt);
        setBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF_KEEP_STATE);
        try {
            mCaptureSession.setRepeatingRequest(setBuilder.build(), null, null);
            Log.d("TAG","点击了白平衡");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        zidong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (setBuilder != null){
                    setBuilder.set(CaptureRequest.CONTROL_AWB_MODE,CaptureRequest.CONTROL_AWB_MODE_AUTO);
                    Log.d("TAG","开启自动");
                    try {
                        mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        bcd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (setBuilder != null){
                    setBuilder.set(CaptureRequest.CONTROL_AWB_MODE,2);
                    try {
                        mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        yy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (setBuilder != null){
                    setBuilder.set(CaptureRequest.CONTROL_AWB_MODE,8);
                    try {
                        mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        qt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (setBuilder != null){
                    setBuilder.set(CaptureRequest.CONTROL_AWB_MODE,5);
                    try {
                        mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        yt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (setBuilder != null){
                    setBuilder.set(CaptureRequest.CONTROL_AWB_MODE,6);
                    try {
                        mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        dd.addView(ly_bph);
    }
    /**
     * 参数设计
     * 1.快门时间
     * 2.iso感光度
     * 3.对焦
     * 4.白平衡
     * */



    /**
     * 开启预览
     * 1.startPreview()
     * 2.previewDataSurface用于接受数据、接受预览, getPreviewSurface()
     * 3.jpegPreviewSurface用于获取数据、保存预览, getImgReader()
     * 4.释放相机会话，closeSession()
     * */
    //创建一个用于接收预览数据的surface，防止用于预览的surface在拍照后卡顿
    private void getPreviewSurface() {
        ImageReader previewDataImageReader = ImageReader.newInstance(camera_preview.getWidth(), camera_preview.getHeight(), ImageFormat.YUV_420_888, 5);
        previewDataImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image image = imageReader.acquireNextImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    Image.Plane yPlane = planes[0];
                    Image.Plane uPlane = planes[1];
                    Image.Plane vPlane = planes[2];
                    ByteBuffer yBuffer = yPlane.getBuffer(); // Data from Y channel
                    ByteBuffer uBuffer = uPlane.getBuffer(); // Data from U channel
                    ByteBuffer vBuffer = vPlane.getBuffer(); // Data from V channel
                }
                if (image != null) {
                    image.close();
                }
            }
        }, null);
        previewDataSurface = previewDataImageReader.getSurface();
    }
    //使用ImageReader创建一个用于保存照片的surface
    private void getImgReader() {
        //创建ImageReader，用于创建保存预览的surface
        imageReader = ImageReader.newInstance(camera_preview.getWidth(), camera_preview.getHeight(), ImageFormat.JPEG, 5);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image image = imageReader.acquireLatestImage();
                new Thread(new ImageSaver(image)).start();//在子线程保存照片
            }
        }, null);
        jpegPreviewSurface = imageReader.getSurface();
    }
    //保存照片
    private static class ImageSaver implements Runnable {
        private Image mImage;
        public ImageSaver(Image image) {
            mImage = image;
        }
        @Override
        public void run() {
            System.out.println("wdawdawdad" + mImage);
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            File mImageFile = new File(Environment.getExternalStorageDirectory() + "/DCIM/myPicture.jpg");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mImageFile);
                fos.write(data, 0, data.length);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImageFile = null;
                if (fos != null) {
                    try {
                        fos.close();
                        fos = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    private void closeSession(){
        if (mCaptureSession != null){
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }//释放相机会话
    /*private void stopPreview(){
        surfaceList = null;
    }*/
    private void startPreview(){
        closeSession();
        getPreviewSurface();
        getImgReader();
        //摄像输入输出
        try {
            setUpMediaRecorder();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaSurface = mMediaRecorder.getSurface();
        surfaceList.add(mediaSurface);
        //设置预览页面1,获取相机支持的size并和surfaceTexture比较选出适合的,用于预览流
        size = new Size(1920,1080);
        mSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());//设置surface的大小，控件texture大小要与surface匹配
        previewSurface = new Surface(mSurfaceTexture);
        //
        surfaceList.add(previewSurface);
        surfaceList.add(previewDataSurface);
        surfaceList.add(jpegPreviewSurface);
        try {
            requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //CaptureRequest必须指定一个或多个surface,可以多次调用方法添加
            requestBuilder.addTarget(previewSurface);
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
            requestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE);
            requestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            requestBuilder.addTarget(previewDataSurface);
            //创建CaptureSession
            cameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCaptureSession = cameraCaptureSession;
                    try {
                        mCaptureSession.setRepeatingRequest(requestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        if (setBuilder != null){
            try {
                setBuilder = null;
                setBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 开启预览
     * */

    /**
     * 拍照
     * */
    private static final SparseIntArray ORIENTATION = new SparseIntArray();
    //设置屏幕方向
    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }
    private void takePic() {
        try {
            //获取屏幕方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            try{
                captureImageRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            }catch(CameraAccessException e){
                e.printStackTrace();
            }
            captureImageRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
            captureImageRequestBuilder.addTarget(previewDataSurface);
            captureImageRequestBuilder.addTarget(jpegPreviewSurface);
            mCaptureSession.capture(captureImageRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.d("TAG","拍照");
    }
    /**
     * 拍照
     * */

    /**
     * 录像
     * */
    private void setUpMediaRecorder() throws IOException {
        size = new Size(1920,1080);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); //设置用于录制的音源
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);//开始捕捉和编码数据到setOutputFile（指定的文件）
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); //设置在录制过程中产生的输出文件的格式
        mMediaRecorder.setOutputFile(Environment.getExternalStorageDirectory() + "/DCIM/myRadio2.mp4");//设置输出文件的路径
        mMediaRecorder.setVideoEncodingBitRate(10000000);//设置录制的视频编码比特率
        mMediaRecorder.setVideoFrameRate(30);//设置要捕获的视频帧速率
        mMediaRecorder.setVideoSize(size.getWidth(), size.getHeight());//设置要捕获的视频的宽度和高度
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);//设置视频编码器，用于录制
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);//设置audio的编码格式
        int rotation = MainActivity.this.getWindowManager().getDefaultDisplay().getRotation();
        mMediaRecorder.setOrientationHint(ORIENTATION.get(rotation));
        mMediaRecorder.prepare();
    }
    private void startRecode(){
        try {
            try {
                mPreviewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            mPreviewBuilder.addTarget(previewSurface);
            mPreviewBuilder.addTarget(mediaSurface);
            mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //开启录像
                mMediaRecorder.start();
            }
        });
    }
    private void stopRecode(){
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        try {
            mCaptureSession.setRepeatingRequest(requestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    /**
     * 录像
     * */

    /**
     * 初始化相机
     * 1.获取实例,筛选前后摄像头,获取设备列表, initCamera()
     * 2.获取各个摄像头可控等级, isHardwareLevelSupported()
     * 3.申请检查相机权限, checkRequiredPermissions(), isPermission()
     */
    //初始化摄像头
    private void initCamera(){
        //创建CameraManager实例
        cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        //获取相机设备ID列表
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            for (String cameraId : cameraIdList) { //检查设备可控等级和筛选前后摄像头
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (isHardwareLevelSupported(characteristics)) {
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                        backCameraId = cameraId;
                        backCharacteristics = characteristics;
                        break;
                    } else if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                        frontCameraId = cameraId;
                        frontCharacteristics = characteristics;
                        break;
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    //检查该cameraID的可控等级是否达到INFO_SUPPORTED_HARDWARE_LEVEL_FULL或以上,initCamera()里使用
    private boolean isHardwareLevelSupported(CameraCharacteristics characteristics) {
        int requiredLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        int[] levels = {CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3};
        for (int i = 0; i < 5; i++) {
            if (requiredLevel == levels[i]) {
                if (i > 2) {
                    return true;
                }
            }
        }
        return false;
    }
    //申请相机权限
    private boolean checkRequiredPermissions() {
        if (!isPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(Permission_need, 1);//如果权限未授权，则申请授权
            shouldShowRequestPermissionRationale("该权限将用于手机拍照录像和存储功能，若拒绝则运行像地狱");//显示权限信息
        }
        return isPermission();
    }
    //检查相机权限
    public boolean isPermission() {
        for (String permission : Permission_need) {
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }
    /**
     * 初始化相机
     * 1.获取实例,筛选前后摄像头,获取设备列表, initCamera()
     * 2.获取各个摄像头可控等级, isHardwareLevelSupported()
     * 3.申请检查相机权限, checkRequiredPermissions(), isPermission()
     */

    /**
     * 打开相机
     * 1.handler开启相机
     * 2.相机回调
     * 3.
     */
    //在handler中开启相机
    Handler cameraHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        checkRequiredPermissions();
                        Toast.makeText(MainActivity.this, "开个相机权限行不行？", Toast.LENGTH_SHORT).show();
                    } else {
                        cameraManager.openCamera(backCameraId, mStateCallback, null);
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
            if (msg.what == 0) {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        checkRequiredPermissions();
                        Toast.makeText(MainActivity.this, "开个相机权限行不行？", Toast.LENGTH_SHORT).show();
                    } else {
                        cameraManager.openCamera(frontCameraId, mStateCallback, null);
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    };
    private void openCamera() {
         if (frontCameraId != null) {
            Message message_front = Message.obtain();
            message_front.what = 0;
            cameraHandler.sendMessage(message_front);
         } else if (backCameraId != null) {
            Message message_back = Message.obtain();
            message_back.what = 1;
            cameraHandler.sendMessage(message_back);
         } else {
            Toast.makeText(this, "宁的摄像头有、问题", Toast.LENGTH_SHORT).show();
        }
    }
    private void openFrontCamera(){
        if (frontCameraId != null) {
            Message message_front = Message.obtain();
            message_front.what = 0;
            cameraHandler.sendMessage(message_front);
        }
    }
    private void openBackCamera(){
        if (backCameraId != null) {
            Message message_back = Message.obtain();
            message_back.what = 1;
            cameraHandler.sendMessage(message_back);
        }
    }
    private void switchCamera() {
    }
    //创建相机回调
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            //打开成功，可以获取CameraDevice对象
            cameraDevice = camera;
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            //断开连接
        }
        @Override
        public void onError(@NonNull CameraDevice camera, final int error) {
            //发生异常
        }
    };
    /**
     * 打开相机
     * 1.handler开启相机
     * 2.相机回调
     */



}