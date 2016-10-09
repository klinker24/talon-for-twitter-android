package com.afollestad.materialcamera;

public interface ICallback {
    /**
     * It is called when the background operation completes.
     * If the operation is successful, {@code exception} will be {@code null}.
     */
    void done(Exception exception);
}