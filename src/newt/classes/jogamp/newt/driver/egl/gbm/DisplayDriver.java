/**
 * Copyright 2019 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package jogamp.newt.driver.egl.gbm;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.opengl.GLProfile;

import jogamp.nativewindow.drm.DRMLib;
import jogamp.nativewindow.drm.DRMUtil;
import jogamp.newt.DisplayImpl;
import jogamp.newt.NEWTJNILibLoader;
import jogamp.opengl.egl.EGLDisplayUtil;

public class DisplayDriver extends DisplayImpl {

    static {
        NEWTJNILibLoader.loadNEWT();
        GLProfile.initSingleton();

        if (!DisplayDriver.initIDs()) {
            throw new NativeWindowException("Failed to initialize egl.gbm Display jmethodIDs");
        }
        if (!ScreenDriver.initIDs()) {
            throw new NativeWindowException("Failed to initialize egl.gbm Screen jmethodIDs");
        }
        if (!WindowDriver.initIDs()) {
            throw new NativeWindowException("Failed to initialize egl.gbm Window jmethodIDs");
        }
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }

    public DisplayDriver() {
        gbmHandle = 0;
    }

    @Override
    protected void createNativeImpl() {
        final int drmFd = DRMUtil.getDrmFd();
        if( 0 > drmFd ) {
            throw new NativeWindowException("Failed to initialize DRM");
        }
        gbmHandle = DRMLib.gbm_create_device(drmFd);
        aDevice = EGLDisplayUtil.eglCreateEGLGraphicsDevice(gbmHandle, AbstractGraphicsDevice.DEFAULT_CONNECTION, AbstractGraphicsDevice.DEFAULT_UNIT);
        aDevice.open();
    }

    @Override
    protected void closeNativeImpl(final AbstractGraphicsDevice aDevice) {
        aDevice.close();
        DRMLib.gbm_device_destroy(gbmHandle);
        gbmHandle = 0;
    }

    /* pp */ final long getGBMHandle() { return gbmHandle; }

    @Override
    protected void dispatchMessagesNative() {
        DispatchMessages0();
    }

    //----------------------------------------------------------------------
    // Internals only
    //
    private static native boolean initIDs();

    private static native void DispatchMessages0();

    private long gbmHandle;
}