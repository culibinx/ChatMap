package su.idev.chatmap.span;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

/**
 * Created by culibinl on 18.07.17.
 */

public class RoundedBackgroundSpan extends ReplacementSpan {


    @Override
    public  void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint)
    {
        RectF rect = new RectF(x, top, x + measureText(paint, text, start, end), bottom);
        paint.setColor(Color.BLUE);
        canvas.drawRoundRect(rect, 100f, 30f, paint);
        paint.setColor(Color.WHITE);
        canvas.drawText(text, start, end, x, y, paint);
    }
    @Override
    public  int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm)
    {
        return Math.round(measureText(paint, text, start, end));
    }

    private float measureText(Paint paint, CharSequence text, int start, int end)
    {
        return paint.measureText(text, start, end);
    }

}
