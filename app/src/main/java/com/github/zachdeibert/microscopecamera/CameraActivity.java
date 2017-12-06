package com.github.zachdeibert.microscopecamera;

import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final int CAMERA_REQUEST_CODE = 1;
    private CameraWrapper camera;
    private Surface surface;
    private CameraSurfaceRenderer renderer;

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
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        camera = new CameraWrapper(this);
        camera.tryLoadCamera(CAMERA_REQUEST_CODE);
        SurfaceView view = (SurfaceView) findViewById(R.id.camera_preview);
        view.getHolder().addCallback(this);
    }
}
