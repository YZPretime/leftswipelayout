package com.example.retime.leftswipelayoutdemo.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Scroller;

/**
 * Created by retime on 2017/9/14.
 */

public class LeftSwipeLayout extends LinearLayout {

    public static final int STATE_CLOSED = 0;
    public static final int STATE_OPEN = 1;
//    public static final int STATE_SCROLL = 2;

    private int state = STATE_CLOSED;

    private boolean isDragging;

    private View rightLayout;

    private Scroller mScroller;

    private int mTouchSlop;

    public LeftSwipeLayout(Context context) {
        this(context, null);
    }

    public LeftSwipeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LeftSwipeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mScroller = new Scroller(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() == 2 && getOrientation() == HORIZONTAL) {
            View mainLayout = getChildAt(0);
            LayoutParams mainLayoutParams = (LayoutParams) mainLayout.getLayoutParams();
            if (mainLayoutParams != null) {
                mainLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            } else {
                mainLayout.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            rightLayout = getChildAt(1);
        }
    }

    public int getState() {
        return state;
    }

    private float dispatchDownX;
    private float dispatchDownY;
    private float interceptDownX;
    private float interceptDownY;
    private float lastX;
    private float lastY;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                dispatchDownX = ev.getX();
                dispatchDownY = ev.getY();
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                float offsetXAbs = Math.abs(ev.getX() - dispatchDownX);
                float offsetYAbs = Math.abs(ev.getY() - dispatchDownY);
                if (offsetXAbs <= offsetYAbs && offsetXAbs > mTouchSlop) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercepted = false;
        //只有子View个数为2且当前线性布局方向为横向时，才做事件拦截处理
        if (getChildCount() != 2 || getOrientation() != HORIZONTAL) {
            return false;
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                interceptDownX = ev.getX();
                interceptDownY = ev.getY();
                intercepted = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float offsetXAbs = Math.abs(ev.getX() - interceptDownX);
                float offsetYAbs = Math.abs(ev.getY() - interceptDownY);
                intercepted = offsetXAbs > offsetYAbs && offsetXAbs > mTouchSlop;
                break;
            case MotionEvent.ACTION_UP:
                intercepted = false;
                break;
        }
        return intercepted;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //只有子View个数为2且当前线性布局方向为横向时，才做事件处理
        if (getChildCount() != 2 || getOrientation() != HORIZONTAL) {
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float offsetX = event.getX() - lastX;
                    //滑动逻辑
                    if ((getScrollX() >= rightLayout.getWidth() && offsetX <= 0)
                            || (getScrollX() <= 0 && offsetX >= 0)) {
                        return super.onTouchEvent(event);
                    }
                    int scrollToX = (int) (getScrollX() - offsetX);
                    if (scrollToX < 0) {
                        scrollToX = 0;
                    } else if (scrollToX > rightLayout.getWidth()) {
                        scrollToX = rightLayout.getWidth();
                    }
                    scrollTo(scrollToX, 0);
                    lastX = event.getX();
                    return true;
                } else {
                    float offsetXAbs = Math.abs(event.getX() - lastX);
                    float offsetYAbs = Math.abs(event.getY() - lastY);
                    if (offsetXAbs > mTouchSlop || offsetYAbs > mTouchSlop) { //滑动超过一定距离
                        if (offsetXAbs > offsetYAbs) { //x轴偏移大于y轴偏移，处理并消费事件
                            isDragging = true;
                            lastX = event.getX();
                            return true;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    //复位逻辑，展开或关闭
                    if (state == STATE_OPEN) {
                        if (getScrollX() < rightLayout.getWidth() * 2 / 3) {
                            smoothClose();
                        } else {
                            smoothOpen();
                        }
                    } else {
                        if (getScrollX() < rightLayout.getWidth() / 3) {
                            smoothClose();
                        } else {
                            smoothOpen();
                        }
                    }
                    isDragging = false;
                    return true;
                }
                break;
        }
        return true;
    }

    public void quickClose() {
        scrollTo(0, 0);
        state = STATE_CLOSED;
    }

    public void smoothClose() {
        mScroller.startScroll(getScrollX(), 0, -getScrollX(), 0);
        state = STATE_CLOSED;
        postInvalidate();
    }

    public void smoothOpen() {
        int rightLayoutWidth = rightLayout == null ? 0 : rightLayout.getWidth();
        mScroller.startScroll(getScrollX(), 0, rightLayoutWidth - getScrollX(), 0);
        state = STATE_OPEN;
        postInvalidate();
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), 0);
            postInvalidate();
        }
    }
}
