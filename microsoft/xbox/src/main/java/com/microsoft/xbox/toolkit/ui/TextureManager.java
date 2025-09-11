package com.microsoft.xbox.toolkit.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import com.microsoft.xbox.toolkit.*;
import com.microsoft.xbox.toolkit.network.HttpClientFactory;
import com.microsoft.xbox.toolkit.network.XLEHttpStatusAndStream;
import com.microsoft.xbox.toolkit.network.XLEThreadPool;
import com.microsoft.xboxtcui.R;
import com.microsoft.xboxtcui.XboxTcuiSdk;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class TextureManager {
    private static final int ANIM_TIME = 100;
    private static final int BITMAP_CACHE_MAX_FILE_SIZE_IN_BYTES = 5242880;
    private static final String BMP_FILE_CACHE_DIR_NAME = "texture";
    private static final int BMP_FILE_CACHE_SIZE = 2000;
    private static final int DECODE_THREAD_WAIT_TIMEOUT_MS = 3000;
    private static final int TEXTURE_TIMEOUT_MS = 15000;
    private static final long TIME_TO_RETRY_MS = 300000;
    public static TextureManager instance = new TextureManager();
    public final Object listLock = new Object();
    private final TimeMonitor stopwatch = new TimeMonitor();
    public XLEMemoryCache<TextureManagerScaledNetworkBitmapRequest, XLEBitmap> bitmapCache = new XLEMemoryCache<>(Math.min(getNetworkBitmapCacheSizeInMB(), 50) * 1048576, BITMAP_CACHE_MAX_FILE_SIZE_IN_BYTES);
    public XLEFileCache bitmapFileCache = XLEFileCacheManager.createCache(BMP_FILE_CACHE_DIR_NAME, BMP_FILE_CACHE_SIZE);
    public HashSet<TextureManagerScaledNetworkBitmapRequest> inProgress = new HashSet<>();
    public HashMap<TextureManagerScaledNetworkBitmapRequest, RetryEntry> timeToRetryCache = new HashMap<>();
    public ThreadSafePriorityQueue<TextureManagerDownloadRequest> toDecode = new ThreadSafePriorityQueue<>();
    public MultiMap<TextureManagerScaledNetworkBitmapRequest, ImageView> waitingForImage = new MultiMap<>();
    private Thread decodeThread = null;
    private HashMap<TextureManagerScaledResourceBitmapRequest, XLEBitmap> resourceBitmapCache = new HashMap<>();

    public TextureManager() {
        this.stopwatch.start();
        XLEThread xLEThread = new XLEThread(new TextureManagerDecodeThread(), "XLETextureDecodeThread");
        this.decodeThread = xLEThread;
        xLEThread.setDaemon(true);
        this.decodeThread.setPriority(4);
        this.decodeThread.start();
    }

    public static TextureManager Instance() {
        return instance;
    }

    private static boolean invalidUrl(String str) {
        return str == null || str.isEmpty();
    }

    private static boolean validResizeDimention(int i, int i2) {
        if (i != 0 && i2 != 0) {
            return i > 0 && i2 > 0;
        }
        throw new UnsupportedOperationException();
    }

    public void logMemoryUsage() {
    }

    public void preload(int i) {
    }

    public void preload(URI uri) {
    }

    public void preloadFromFile(String str) {
    }

    public void unsafeClearBitmapCache() {
    }

    private int getNetworkBitmapCacheSizeInMB() {
        return (Math.max(0, MemoryMonitor.instance().getMemoryClass() - 64) / 2) + 12;
    }

    private void load(@NotNull TextureManagerScaledNetworkBitmapRequest textureManagerScaledNetworkBitmapRequest) {
        if (!invalidUrl(textureManagerScaledNetworkBitmapRequest.url)) {
            XLEThreadPool.textureThreadPool.run(new TextureManagerDownloadThreadWorker(new TextureManagerDownloadRequest(textureManagerScaledNetworkBitmapRequest)));
        }
    }

    public XLEBitmap.XLEBitmapDrawable loadScaledResourceDrawable(int i) {
        XLEBitmap loadResource = loadResource(i);
        if (loadResource == null) {
            return null;
        }
        return loadResource.getDrawable();
    }

    public BitmapFactory.Options computeInSampleSizeOptions(int desiredw, int desiredh, BitmapFactory.Options options) {
        boolean z = true;
        BitmapFactory.Options scaleoptions = new BitmapFactory.Options();
        int scale = 1;
        if (validResizeDimention(desiredw, desiredh) && options.outWidth > desiredw && options.outHeight > desiredh) {
            scale = (int) Math.pow(2.0d, Math.min((int) Math.floor(Math.log(((float) options.outWidth) / ((float) desiredw)) / Math.log(2.0d)), (int) Math.floor(Math.log(((float) options.outHeight) / ((float) desiredh)) / Math.log(2.0d))));
            if (scale < 1) {
                z = false;
            }
            XLEAssert.assertTrue(z);
        }
        scaleoptions.inSampleSize = scale;
        return scaleoptions;
    }


    public XLEBitmap loadResource(int i) {
        TextureManagerScaledResourceBitmapRequest textureManagerScaledResourceBitmapRequest = new TextureManagerScaledResourceBitmapRequest(i);
        XLEBitmap xLEBitmap = this.resourceBitmapCache.get(textureManagerScaledResourceBitmapRequest);
        if (xLEBitmap == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(XboxTcuiSdk.getResources(), textureManagerScaledResourceBitmapRequest.resourceId, options);
            xLEBitmap = XLEBitmap.decodeResource(XboxTcuiSdk.getResources(), textureManagerScaledResourceBitmapRequest.resourceId);
            this.resourceBitmapCache.put(textureManagerScaledResourceBitmapRequest, xLEBitmap);
        }
        XLEAssert.assertNotNull(xLEBitmap);
        return xLEBitmap;
    }

    public void bindToView(int i, ImageView imageView, int i2, int i3) {
        bindToView(i, imageView, i2, i3, null);
    }

    public void bindToView(int i, ImageView imageView, int i2, int i3, OnBitmapSetListener onBitmapSetListener) {
        boolean z = true;
        XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
        XLEBitmap loadResource = loadResource(i);
        if (loadResource == null) {
            z = false;
        }
        XLEAssert.assertTrue(z);
        if (imageView instanceof XLEImageView) {
            ((XLEImageView) imageView).TEST_loadingOrLoadedImageUrl = Integer.toString(i);
        }
        setImage(imageView, loadResource);
    }

    public void bindToViewFromFile(String str, ImageView imageView, TextureBindingOption textureBindingOption) {
        XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
        bindToViewInternal(str, imageView, textureBindingOption);
    }

    public void bindToViewFromFile(String str, ImageView imageView, int i, int i2) {
        XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
        if (i == 0 || i2 == 0) {
            throw new UnsupportedOperationException();
        }
        bindToViewInternal(str, imageView, new TextureBindingOption(i, i2));
    }

    public void bindToView(URI uri, ImageView imageView, int i, int i2) {
        String str;
        XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
        if (i == 0 || i2 == 0) {
            throw new UnsupportedOperationException();
        }
        if (uri == null) {
            str = null;
        } else {
            str = uri.toString();
        }
        bindToViewInternal(str, imageView, new TextureBindingOption(i, i2));
    }

    public void bindToView(URI uri, ImageView imageView, TextureBindingOption textureBindingOption) {
        String str;
        XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
        if (uri == null) {
            str = null;
        } else {
            str = uri.toString();
        }
        bindToViewInternal(str, imageView, textureBindingOption);
    }

    public void setCachingEnabled(boolean z) {
        this.bitmapCache = new XLEMemoryCache<>(z ? getNetworkBitmapCacheSizeInMB() : 0, BITMAP_CACHE_MAX_FILE_SIZE_IN_BYTES);
        this.bitmapFileCache = XLEFileCacheManager.createCache(BMP_FILE_CACHE_DIR_NAME, BMP_FILE_CACHE_SIZE, z);
        this.resourceBitmapCache = new HashMap<>();
    }

    public boolean isBusy() {
        boolean z;
        synchronized (listLock) {
            z = !this.inProgress.isEmpty();
        }
        return z;
    }

    private void bindToViewInternal(String str, ImageView imageView, TextureBindingOption textureBindingOption) {
        XLEBitmap xLEBitmap;
        TextureManagerScaledNetworkBitmapRequest textureManagerScaledNetworkBitmapRequest = new TextureManagerScaledNetworkBitmapRequest(str, textureBindingOption);
        synchronized (listLock) {
            if (this.waitingForImage.containsValue(imageView)) {
                this.waitingForImage.removeValue(imageView);
            }
            boolean z = true;
            if (!invalidUrl(str)) {
                xLEBitmap = this.bitmapCache.get(textureManagerScaledNetworkBitmapRequest);
                if (xLEBitmap == null) {
                    RetryEntry retryEntry = this.timeToRetryCache.get(textureManagerScaledNetworkBitmapRequest);
                    if (retryEntry != null) {
                        if (!retryEntry.isExpired()) {
                            if (textureBindingOption.resourceIdForError != -1) {
                                xLEBitmap = loadResource(textureBindingOption.resourceIdForError);
                            }
                        }
                    }
                    if (textureBindingOption.resourceIdForLoading != -1) {
                        xLEBitmap = loadResource(textureBindingOption.resourceIdForLoading);
                        if (xLEBitmap == null) {
                            z = false;
                        }
                        XLEAssert.assertTrue(z);
                    }
                    this.waitingForImage.put(textureManagerScaledNetworkBitmapRequest, imageView);
                    if (!this.inProgress.contains(textureManagerScaledNetworkBitmapRequest)) {
                        this.inProgress.add(textureManagerScaledNetworkBitmapRequest);
                        load(textureManagerScaledNetworkBitmapRequest);
                    }
                }
            } else if (textureBindingOption.resourceIdForError != -1) {
                xLEBitmap = loadResource(textureBindingOption.resourceIdForError);
                XLEAssert.assertNotNull(xLEBitmap);
            } else {
                xLEBitmap = null;
            }
        }
        setImage(imageView, xLEBitmap);
        if (imageView instanceof XLEImageView) {
            ((XLEImageView) imageView).TEST_loadingOrLoadedImageUrl = str;
        }
    }

    public XLEBitmap createScaledBitmap(XLEBitmap xLEBitmap, int i, int i2) {
        if (!validResizeDimention(i, i2) || xLEBitmap.getBitmap() == null) {
            return xLEBitmap;
        }
        float height = ((float) xLEBitmap.getBitmap().getHeight()) / ((float) xLEBitmap.getBitmap().getWidth());
        float f = (float) i2;
        float f2 = (float) i;
        if (f / f2 < height) {
            i = Math.max(1, (int) (f / height));
        } else {
            i2 = Math.max(1, (int) (f2 * height));
        }
        return XLEBitmap.createScaledBitmap8888(xLEBitmap, i, i2, true);
    }

    public void drainWaitingForImage(TextureManagerScaledNetworkBitmapRequest textureManagerScaledNetworkBitmapRequest, XLEBitmap xLEBitmap) {
        if (this.waitingForImage.containsKey(textureManagerScaledNetworkBitmapRequest)) {
            Iterator<ImageView> it = this.waitingForImage.get(textureManagerScaledNetworkBitmapRequest).iterator();
            while (it.hasNext()) {
                ImageView next = it.next();
                if (next != null) {
                    if (next instanceof XLEImageView) {
                        setXLEImageView(textureManagerScaledNetworkBitmapRequest, (XLEImageView) next, xLEBitmap);
                    } else {
                        setView(textureManagerScaledNetworkBitmapRequest, next, xLEBitmap);
                    }
                }
            }
        }
    }

    private void setView(final TextureManagerScaledNetworkBitmapRequest textureManagerScaledNetworkBitmapRequest, final ImageView imageView, final XLEBitmap xLEBitmap) {
        ThreadManager.UIThreadPost(() -> {
            boolean keyValueMatches;
            XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
            synchronized (TextureManager.this.listLock) {
                keyValueMatches = TextureManager.this.waitingForImage.keyValueMatches(textureManagerScaledNetworkBitmapRequest, imageView);
            }
            if (keyValueMatches) {
                TextureManager.this.setImage(imageView, xLEBitmap);
                synchronized (TextureManager.this.listLock) {
                    TextureManager.this.waitingForImage.removeValue(imageView);
                }
            }
        });
    }

    private void setXLEImageView(final TextureManagerScaledNetworkBitmapRequest textureManagerScaledNetworkBitmapRequest, final XLEImageView xLEImageView, final XLEBitmap xLEBitmap) {
        ThreadManager.UIThreadPost(() -> {
            boolean keyValueMatches;
            XLEAssert.assertTrue(Thread.currentThread() == ThreadManager.UIThread);
            synchronized (listLock) {
                keyValueMatches = waitingForImage.keyValueMatches(textureManagerScaledNetworkBitmapRequest, xLEImageView);
            }
            if (keyValueMatches) {
                final float alpha = xLEImageView.getAlpha();
                if (xLEImageView.getShouldAnimate()) {
                    xLEImageView.animate().alpha(0.0f).setDuration(100).setListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animator) {
                            xLEImageView.setFinal(true);
                            setImage(xLEImageView, xLEBitmap);
                            xLEImageView.animate().alpha(alpha).setDuration(100).setListener(null);
                        }
                    });
                } else {
                    setImage(xLEImageView, xLEBitmap);
                }
                synchronized (listLock) {
                    waitingForImage.removeValue(xLEImageView);
                }
            }
        });
    }

    public void purgeResourceBitmapCache() {
        this.resourceBitmapCache.clear();
    }

    public void setImage(@NotNull ImageView imageView, XLEBitmap xLEBitmap) {
        Bitmap bitmap = xLEBitmap == null ? null : xLEBitmap.getBitmap();
        OnBitmapSetListener onBitmapSetListener = (OnBitmapSetListener) imageView.getTag(R.id.image_callback);
        if (onBitmapSetListener != null) {
            onBitmapSetListener.onBeforeImageSet(imageView, bitmap);
        }
        imageView.setImageBitmap(bitmap);
        imageView.setTag(R.id.image_bound, true);
        if (onBitmapSetListener != null) {
            onBitmapSetListener.onAfterImageSet(imageView, bitmap);
        }
    }

    private static class RetryEntry {
        private static final long SEC = 1000;
        private static final long[] TIMES_MS = {5000, 9000, 19000, 37000, 75000, 150000, 300000};
        private int curIdx = 0;
        private long currStart = System.currentTimeMillis();

        public boolean isExpired() {
            return this.currStart + TIMES_MS[this.curIdx] < System.currentTimeMillis();
        }

        public void startNext() {
            int i = this.curIdx;
            if (i < TIMES_MS.length - 1) {
                this.curIdx = i + 1;
            }
            this.currStart = System.currentTimeMillis();
        }
    }

    private class TextureManagerDecodeThread implements Runnable {
        private TextureManagerDecodeThread() {
        }

        public void run() {
            while (true) {
                TextureManagerDownloadRequest request = toDecode.pop();
                XLEBitmap resultBitmap = null;

                if (request.stream != null) {
                    BackgroundThreadWaitor.getInstance().waitForReady(TextureManager.DECODE_THREAD_WAIT_TIMEOUT_MS);

                    try {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        StreamUtil.CopyStream(outputStream, request.stream);
                        byte[] imageData = outputStream.toByteArray();

                        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
                        boundsOptions.inJustDecodeBounds = true;
                        BitmapFactory.decodeStream(new ByteArrayInputStream(imageData), null, boundsOptions);

                        BitmapFactory.Options decodeOptions = computeInSampleSizeOptions(
                                request.key.bindingOption.width,
                                request.key.bindingOption.height,
                                boundsOptions
                        );

                        XLEBitmap decodedBitmap = XLEBitmap.decodeStream(
                                new ByteArrayInputStream(imageData),
                                decodeOptions
                        );

                        if (request.key.bindingOption.useFileCache &&
                                !bitmapFileCache.contains(request.key)) {
                            bitmapFileCache.save(request.key, new ByteArrayInputStream(imageData));
                        }

                        resultBitmap = TextureManager.this.createScaledBitmap(
                                decodedBitmap,
                                request.key.bindingOption.width,
                                request.key.bindingOption.height
                        );

                    } catch (Exception ignored) {
                    }
                }

                BackgroundThreadWaitor.getInstance().waitForReady(TextureManager.DECODE_THREAD_WAIT_TIMEOUT_MS);

                synchronized (listLock) {
                    if (resultBitmap != null) {
                        bitmapCache.add(request.key, resultBitmap, resultBitmap.getByteCount());
                        timeToRetryCache.remove(request.key);
                    } else if (request.key.bindingOption.resourceIdForError != -1) {
                        resultBitmap = loadResource(request.key.bindingOption.resourceIdForError);

                        RetryEntry retryEntry = timeToRetryCache.get(request.key);
                        if (retryEntry != null) {
                            retryEntry.startNext();
                        } else {
                            timeToRetryCache.put(request.key, new RetryEntry());
                        }
                    }

                    drainWaitingForImage(request.key, resultBitmap);
                    inProgress.remove(request.key);
                }
            }
        }
    }

    private class TextureManagerDownloadThreadWorker implements Runnable {
        private final TextureManagerDownloadRequest request;

        public TextureManagerDownloadThreadWorker(TextureManagerDownloadRequest textureManagerDownloadRequest) {
            request = textureManagerDownloadRequest;
        }

        public void run() {
            XLEAssert.assertTrue(request.key != null && request.key.url != null);
            request.stream = null;
            try {
                String url = request.key.url;
                if (!url.startsWith(HttpHost.DEFAULT_SCHEME_NAME)) {
                    request.stream = downloadFromAssets(url);
                } else if (request.key.bindingOption.useFileCache) {
                    request.stream = bitmapFileCache.getInputStreamForRead(request.key);
                    if (request.stream == null) {
                        request.stream = downloadFromWeb(url);
                    }
                } else {
                    request.stream = downloadFromWeb(url);
                }
            } catch (Exception ignored) {
            }
            synchronized (listLock) {
                toDecode.push(request);
            }
        }

        private @Nullable InputStream downloadFromWeb(String str) {
            try {
                XLEHttpStatusAndStream httpStatusAndStreamInternal = HttpClientFactory.textureFactory.getHttpClient(TextureManager.TEXTURE_TIMEOUT_MS).getHttpStatusAndStreamInternal(new HttpGet(URI.create(str)), false);
                if (httpStatusAndStreamInternal.statusCode == 200) {
                    return httpStatusAndStreamInternal.stream;
                }
                return null;
            } catch (Exception unused) {
                return null;
            }
        }

        private @Nullable InputStream downloadFromAssets(String str) {
            try {
                return XboxTcuiSdk.getAssetManager().open(str);
            } catch (IOException unused) {
                return null;
            }
        }
    }
}
