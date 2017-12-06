package com.github.zachdeibert.microscopecamera;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.io.Closeable;
import java.util.Arrays;

public class CameraSurfaceRenderer extends CameraCaptureSession.StateCallback implements Closeable {
    private static final String TAG = "CameraSurfaceRenderer";
    private final CameraWrapper camera;
    private final Surface surface;
    private HandlerThread thread;
    private Handler handler;
    private CameraCaptureSession session;

    @Override
    public void onConfigured(@NonNull CameraCaptureSession session) {
        try {
            this.session = session;
            CameraDevice device = camera.getDevice();
            CaptureRequest.Builder req = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            req.addTarget(surface);
            req.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            session.setRepeatingRequest(req.build(), null, handler);
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Unable to access camera.", ex);
        }
    }

    @Override
    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        Log.e(TAG, "Camera configuration failed.");
    }

    public void startRendering() {
        try {
            thread = new HandlerThread("Camera Thread");
            thread.start();
            handler = new Handler(thread.getLooper());
            CameraDevice device = camera.getDevice();
            device.createCaptureSession(Arrays.asList(surface), this, null);
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Unable to access camera.", ex);
        }
    }

    @Override
    public void close() {
        if (thread != null) {
            Log.d(TAG, "Closing renderer.");
            thread.quitSafely();
            try {
                thread.join();
            } catch (InterruptedException ex) {
                Log.wtf(TAG, "Interrupted while waiting for thread to die.", ex);
            }
            thread = null;
        }
        if (session != null) {
            session.close();
        }
    }

    public CameraSurfaceRenderer(CameraWrapper camera, Surface surface) {
        this.camera = camera;
        this.surface = surface;
    }
}
