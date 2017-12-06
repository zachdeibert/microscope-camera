package com.github.zachdeibert.microscopecamera;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CameraWrapper extends CameraDevice.StateCallback {
    private static final String TAG = "CameraWrapper";
    private final Activity activity;
    public final CameraManager manager;
    private final List<CameraSurfaceRenderer> renderers;
    private CameraDevice device;

    public void takePicture(int format, final IPhotoCallback callback) {
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(device.getId());
            int width = 480;
            int height = 640;
            if (characteristics != null) {
                Size[] sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(format);
                if (sizes != null && sizes.length > 0) {
                    width = sizes[0].getWidth();
                    height = sizes[0].getHeight();
                }
            }
            ImageReader reader = ImageReader.newInstance(width, height, format, 1);
            final CameraSurfaceRenderer renderer = createSurfaceRenderer(reader.getSurface(), false);
            reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] data = new byte[buffer.capacity()];
                    buffer.get(data);
                    callback.photoTaken(data);
                    image.close();
                    renderer.close();
                }
            }, null);
            renderer.startRendering();
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Unable to access camera.", ex);
        }
    }

    public boolean hasDevice() {
        return device != null;
    }

    public CameraDevice getDevice() {
        return device;
    }

    private CameraSurfaceRenderer createSurfaceRenderer(Surface surface, boolean autoStart) {
        CameraSurfaceRenderer renderer = new CameraSurfaceRenderer(this, surface);
        if (autoStart && hasDevice()) {
            renderer.startRendering();
        }
        return renderer;
    }

    public CameraSurfaceRenderer createSurfaceRenderer(Surface surface) {
        return createSurfaceRenderer(surface, true);
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
