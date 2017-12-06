package com.github.zachdeibert.microscopecamera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.ImageFormat;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String TAG = "CameraActivity";
    private static final int CAMERA_REQUEST_CODE = 1;
    private static final int STORAGE_REQUEST_CODE = 2;
    private CameraWrapper camera;
    private Surface surface;
    private CameraSurfaceRenderer renderer;
    private boolean isStreaming;
    private boolean isVideoing;
    private File saveDirectory;

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surface = holder.getSurface();
        renderer = camera.createSurfaceRenderer(surface);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (surface != holder.getSurface()) {
            renderer.close();
            surface = holder.getSurface();
            renderer = camera.createSurfaceRenderer(surface);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        renderer.close();
        surface = null;
        renderer = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    camera.tryLoadCamera(CAMERA_REQUEST_CODE);
                }
                break;
            case STORAGE_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveDirectory.mkdirs();
                }
                break;
        }
    }

    private void enterImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        camera = new CameraWrapper(this);
        camera.tryLoadCamera(CAMERA_REQUEST_CODE);
        SurfaceView view = (SurfaceView) findViewById(R.id.camera_preview);
        view.getHolder().addCallback(this);
        enterImmersiveMode();
        saveDirectory = new File(Environment.getExternalStorageDirectory(), "Microscope Camera");
        if (!saveDirectory.exists()) {
            if (!saveDirectory.mkdirs()) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[] {
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, STORAGE_REQUEST_CODE);
                    return;
                }
            }
        }
        Log.d(TAG, String.format("Saving files to %s.", saveDirectory));
    }

    @Override
    protected void onResume() {
        super.onResume();
        enterImmersiveMode();
    }

    public void toggleStream(View v) {
        v.setBackgroundTintList((isStreaming = !isStreaming) ? ColorStateList.valueOf(getResources().getColor(R.color.colorRecording)) : null);
    }

    public void takePhoto(View v) {
        camera.takePicture(ImageFormat.JPEG, new IPhotoCallback() {
            @Override
            public void photoTaken(byte[] data) {
                File file = new File(saveDirectory, String.format("%s.jpg", new Date()));
                FileOutputStream stream = null;
                try {
                    stream = new FileOutputStream(file);
                    stream.write(data);
                } catch (IOException ex) {
                    Log.e(TAG, "Unable to save picture.", ex);
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Unable to save picture.", e);
                        }
                    }
                }
                if (file.exists()) {
                    MediaScannerConnection.scanFile(CameraActivity.this, new String[] {
                            file.getAbsolutePath()
                    }, null, null);
                }
            }
        });
    }

    public void toggleVideo(View v) {
        v.setBackgroundTintList((isVideoing = !isVideoing) ? ColorStateList.valueOf(getResources().getColor(R.color.colorRecording)) : null);
    }
}
