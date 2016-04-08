package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 5;

    private VelocityTracker velocityTracker;

    private BrushType _randomBrush = _brushType;
    private boolean shuffle = false;
    private Random random;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));

        velocityTracker = VelocityTracker.obtain();
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }


    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        if(_offScreenCanvas != null) {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            _offScreenCanvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
        }

        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, null);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        //TODO
        //Basically, the way this works is to listen for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location
        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                } else {
                    velocityTracker.clear(); //reset the velocity tracker at the start of each touch event
                }

                velocityTracker.addMovement(motionEvent);

                if(shuffle) {
                    random = new Random();
                    while(_randomBrush == _brushType) {
                        /*While loop will ensure that the next randomly selected brush isn't the same as the current brush*/
                        _randomBrush = BrushType.values()[random.nextInt(4)];
                    }
                    _brushType = _randomBrush;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                velocityTracker.addMovement(motionEvent);

                velocityTracker.computeCurrentVelocity(1000);

                int historySize = motionEvent.getHistorySize();
                for (int i = 0; i < historySize; i++) {

                    float touchX = motionEvent.getHistoricalX(i);
                    float touchY = motionEvent.getHistoricalY(i);
                    _paint.setColor(getPixelColor(touchX, touchY));
                    _paint.setAlpha(_alpha);
                    //_offScreenCanvas.drawRect(touchX - 20.0f, touchY - 20.0f, touchX + 20.0f, touchY + 20.0f, _paint);
                    drawShape(_brushType, touchX, touchY);
                }

                // TODO: draw to the offscreen bitmap for current x,y point.
                float touchX = motionEvent.getX();
                float touchY = motionEvent.getY();
                _paint.setColor(getPixelColor(touchX, touchY));
                _paint.setAlpha(_alpha);
                //_offScreenCanvas.drawRect(touchX - 20.0f, touchY - 20.0f, touchX + 20.0f, touchY + 20.0f, _paint);
                drawShape(_brushType, touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        invalidate();

        return true;
    }

    public void drawShape (BrushType brush, float touchX, float touchY) {
        random = new Random();
        float x;
        float y;
        float width;

        switch (brush) {
            case Square:
                _offScreenCanvas.drawRect(touchX - 20.0f, touchY - 20.0f, touchX + 20.0f, touchY + 20.0f, _paint);
                break;
            case Circle:
                float xVelocity = velocityTracker.getXVelocity();
                float yVelocity = velocityTracker.getYVelocity();

                double magnitude = Math.sqrt(Math.pow(xVelocity, 2) + Math.pow(yVelocity, 2));

                int radius = (int) Math.ceil(magnitude/100);

                if(radius >= 150) {
                    radius = 150; //radius should cap at 50 so as not to get too wild...
                } else if (radius <= _defaultRadius) {
                    radius = _defaultRadius;
                }

                _offScreenCanvas.drawCircle(touchX, touchY, radius, _paint);
                break;
            case CircleSplatter:
                x = touchX - (random.nextFloat() * 50);
                y = touchY - (random.nextFloat() * 50);
                _paint.setColor(getPixelColor(x, y));
                _paint.setAlpha(_alpha);
                _offScreenCanvas.drawCircle(x, y, random.nextFloat() * 30, _paint);

                x = touchX + (random.nextFloat() * 50);
                y = touchY + (random.nextFloat() * 50);
                _paint.setColor(getPixelColor(x, y));
                _paint.setAlpha(_alpha);
                _offScreenCanvas.drawCircle(x, y, random.nextFloat() * 30, _paint);

                x = touchX + (random.nextFloat() * 50);
                y = touchY - (random.nextFloat() * 50);
                _paint.setColor(getPixelColor(x, y));
                _paint.setAlpha(_alpha);
                _offScreenCanvas.drawCircle(x, y, random.nextFloat() * 30, _paint);

                x = touchX - (random.nextFloat() * 50);
                y = touchY + (random.nextFloat() * 50);
                _paint.setColor(getPixelColor(x, y));
                _paint.setAlpha(_alpha);
                _offScreenCanvas.drawCircle(x, y, random.nextFloat() * 30, _paint);
                break;
            case SquareSplatter:
                width = random.nextFloat() * 20;

                x = touchX - (random.nextFloat() * 75);
                y = touchY - (random.nextFloat() * 75);
                _paint.setColor(getPixelColor(x, y));
                _paint.setAlpha(_alpha);

                _offScreenCanvas.drawRect(x - width, y - width, x + width, y + width, _paint);

                width = random.nextFloat() * 30;
                x = touchX + (random.nextFloat() * 75);
                y = touchY + (random.nextFloat() * 75);
                _paint.setColor(getPixelColor(x, y));
                _paint.setAlpha(_alpha);
                _offScreenCanvas.drawRect(x - width, y - width, x + width, y + width, _paint);

                width = random.nextFloat() * 30;
                x = touchX + (random.nextFloat() * 75);
                y = touchY - (random.nextFloat() * 75);
                _paint.setColor(getPixelColor(x, y));
                _paint.setAlpha(_alpha);
                _offScreenCanvas.drawRect(x - width, y - width, x + width, y + width, _paint);

                width = random.nextFloat() * 30;
                x = touchX - (random.nextFloat() * 75);
                y = touchY + (random.nextFloat() * 75);
                _paint.setColor(getPixelColor(x, y));
                _paint.setAlpha(_alpha);
                _offScreenCanvas.drawRect(x - width, y - width, x + width, y + width, _paint);
                break;
            default:
                break;
        }
    }


    private int getPixelColor(float x, float y) {
        if(_imageView == null) {
            return Color.WHITE;
        }

        Rect r = getBitmapPositionInsideImageView(_imageView);
        if(x < r.left || x > r.right || y < r.top || y > r.bottom) {
            return Color.WHITE;
        }

        BitmapDrawable drawable = ((BitmapDrawable) _imageView.getDrawable());
        if(drawable == null) {
            return Color.WHITE;
        }
        float imageHeight = r.height();
        float imageWidth = r.width();
        Bitmap bitmap = drawable.getBitmap();
        float bHeight = drawable.getIntrinsicHeight();
        float bWidth = drawable.getIntrinsicWidth();

        float xScale = bWidth / imageWidth;
        float yScale = bHeight / imageHeight;

        int scaledX = (int)Math.ceil((x-r.left)*xScale);
        int scaledY = (int)Math.ceil((y-r.top) * yScale);

        if(scaledX >= bWidth || scaledY >= bHeight || scaledX < 0 || scaledY < 0) {
            return Color.WHITE;
        }

        int color = bitmap.getPixel(scaledX, scaledY);
        return color;
    }

    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }

    public Bitmap getOffScreenBitmap() {
        return _offScreenBitmap;
    }

    public void toggleShuffle(boolean isChecked) {
        shuffle = isChecked;
    }
}

