package com.github.zachdeibert.microscopecamera;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;

public class CameraWrapper extends CameraDevice.StateCallback {
    private static final String TAG = "CameraWrapper";
    private final Activity activity;
    private final CameraManager manager;
    private final List<CameraSurfaceRenderer> renderers;
    private CameraDevice device;

    public boolean hasDevice() {
        return device != null;
    }

    public CameraDevice getDevice() {
        return device;
    }

    public CameraSurfaceRenderer createSurfaceRenderer(Surface surface) {
        CameraSurfaceRenderer renderer = new CameraSurfaceRenderer(this, surface);
        if (hasDevice()) {
            renderer.startRendering();
        }
        return renderer;
    }

    @Override
    public void onOpened(@NonNull CameraDevice camera) {
        if (device == null) {
            device = camera;
            for (CameraSurfaceRenderer renderer : renderers) {
                renderer.startRendering();
            }
        } else {
            Log.wtf(TAG, "Multiple cameras were opened.");
        }
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice camera) {
        if (device == null) {
            Log.wtf(TAG, "The camera that does not exist was disconnected.");
        } else {
            device = null;
        }
    }

    @Override
    public void onError(@NonNull CameraDevice camera, int error) {
        Log.w(TAG, String.format("Camera had error code %d.", error));
    }

    public void tryLoadCamera(int requestCode) {
        try {
            String[] cameras = manager.getCameraIdList();
            if (cameras.length > 0) {
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity, new String[] {
                            Manifest.permission.CAMERA
                    }, requestCode);
                    return;
                }
                manager.openCamera(cameras[0], this, null);
            } else {
                Log.e(TAG, "No cameras found.");
            }
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Unable to access camera.", ex);
        }
    }

    public CameraWrapper(Activity activity) {
        this.activity = activity;
        manager = (CameraManager) activity.getSystemService(Activity.CAMERA_SERVICE);
        renderers = new ArrayList<>();
    }
}
