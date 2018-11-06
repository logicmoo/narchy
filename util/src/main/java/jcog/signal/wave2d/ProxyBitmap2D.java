package jcog.signal.wave2d;

import org.jetbrains.annotations.Nullable;

/**
 * exposes a buffered image as a camera video source
 * TODO integrate with Tensor API
 */
public class ProxyBitmap2D implements Bitmap2D {

    private Bitmap2D src;
    private int w,h;

    public ProxyBitmap2D() {
        set(null);
    }

    public ProxyBitmap2D(Bitmap2D src) {
        set(src);
    }

    public synchronized <P extends ProxyBitmap2D> P set(@Nullable Bitmap2D src) {
        if (this.src!=src) {
            this.w = this.h = 1; //set to zero while switching
            this.src = src;
            if (src!=null) {
                this.w = src.width();
                this.h = src.height();
            }
        }
        return (P) this;
    }


    @Override
    public void update() {
        src.update();
    }

    @Override
    public int width() {
        return w;
    }

    @Override
    public int height() {
        return h;
    }

    @Override
    public float brightness(int x, int y) {
        return src.brightness(x, y);
    }
}
