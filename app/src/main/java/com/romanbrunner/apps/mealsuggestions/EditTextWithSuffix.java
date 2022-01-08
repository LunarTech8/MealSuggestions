package com.romanbrunner.apps.mealsuggestions;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;


public class EditTextWithSuffix extends androidx.appcompat.widget.AppCompatEditText
{
    private final TextPaint textPaint = new TextPaint();
    private final TextPaint hintTextPaint = new TextPaint();
    private String suffix = "";

    public EditTextWithSuffix(Context context)
    {
        super(context);
    }
    public EditTextWithSuffix(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        getAttributes(context, attrs, 0);
    }
    public EditTextWithSuffix(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        getAttributes(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();
        textPaint.setColor(getCurrentTextColor());
        textPaint.setTextSize(getTextSize());
        textPaint.setTextAlign(Paint.Align.LEFT);
        hintTextPaint.setColor(getCurrentHintTextColor());
        hintTextPaint.setTextSize(getTextSize());
        hintTextPaint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        if (getText() != null && !getText().toString().isEmpty())
        {
            canvas.drawText(suffix, (int)textPaint.measureText(getText().toString()) + getPaddingLeft(), getBaseline(), textPaint);
        }
        else if (getHint() != null && !getHint().toString().isEmpty())
        {
            canvas.drawText(suffix, (int)textPaint.measureText(getHint().toString()) + getPaddingLeft(), getBaseline(), hintTextPaint);
        }
    }

    private void getAttributes(Context context, AttributeSet attrs, int defStyleAttr)
    {
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.EditTextWithSuffix, defStyleAttr, 0);
        suffix = typedArray.getString(R.styleable.EditTextWithSuffix_suffix);
        if(suffix == null)
        {
            suffix = "";
        }
        typedArray.recycle();
    }
}