/*
 * Copyright (c) 2013 Menny Even-Danan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anysoftkeyboard.keyboards.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.Animation;

import com.anysoftkeyboard.AskPrefs.AnimationsLevel;
import com.anysoftkeyboard.addons.AddOn;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.keyboardextensions.KeyboardExtension;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.AnyKeyboard.AnyKey;
import com.anysoftkeyboard.keyboards.ExternalAnyKeyboard;
import com.anysoftkeyboard.keyboards.GenericKeyboard;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.Keyboard.Key;
import com.anysoftkeyboard.keyboards.Keyboard.Row;
import com.anysoftkeyboard.keyboards.KeyboardSwitcher;
import com.anysoftkeyboard.theme.KeyboardTheme;
import com.anysoftkeyboard.utils.Log;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.R;

public class AnyKeyboardView extends AnyKeyboardBaseView {

    private static final int DELAY_BEFORE_POPPING_UP_EXTENSION_KBD = 35;// milliseconds
    private final static String TAG = "AnyKeyboardView";
    private static final int TEXT_POP_OUT_ANIMATION_DURATION = 1200;

    private boolean mExtensionVisible = false;
    private final int mExtensionKeyboardYActivationPoint;
    private final int mExtensionKeyboardPopupOffset;
    private final int mExtensionKeyboardYDismissPoint;
    private Key mExtensionKey;
    private Key mUtilityKey;
    private Key mSpaceBarKey = null;
    private Point mFirstTouchPoint = new Point(0, 0);
    private boolean mIsFirstDownEventInsideSpaceBar = false;
    private Animation mInAnimation;

    private float mGesturePreviewTextSize;
    private int mGesturePreviewTextColor, mGesturePreviewTextColorRed,
            mGesturePreviewTextColorGreen, mGesturePreviewTextColorBlue;

    /** Whether we've started dropping move events because we found a big jump */
    // private boolean mDroppingEvents;
    /**
     * Whether multi-touch disambiguation needs to be disabled if a real
     * multi-touch event has occured
     */
    // private boolean mDisableDisambiguation;
    /**
     * The distance threshold at which we start treating the touch session as a
     * multi-touch
     */
    // private int mJumpThresholdSquare = Integer.MAX_VALUE;

    /**
     * The y coordinate of the last row
     */
    // private int mLastRowY;
    public AnyKeyboardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnyKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mExtensionKeyboardPopupOffset = 0;
        mExtensionKeyboardYActivationPoint = -5;
        mExtensionKeyboardYDismissPoint = getThemedKeyboardDimens()
                .getNormalKeyHeight();

        mInAnimation = null;

        mGesturePreviewTextColorRed = (mGesturePreviewTextColor & 0x00FF0000) >> 16;
        mGesturePreviewTextColorGreen = (mGesturePreviewTextColor & 0x0000FF00) >> 8;
        mGesturePreviewTextColorBlue = mGesturePreviewTextColor & 0x000000FF;
    }

    @Override
    protected KeyDetector createKeyDetector(final float slide) {
        return new ProximityKeyDetector();
    }

    public void setKeyboardSwitcher(KeyboardSwitcher switcher) {
        mSwitcher = switcher;
    }

    @Override
    public void setKeyboard(AnyKeyboard newKeyboard, float verticalCorrection) {
        mExtensionKey = null;
        mExtensionVisible = false;

        mUtilityKey = null;
        super.setKeyboard(newKeyboard, verticalCorrection);
        if (newKeyboard != null && newKeyboard instanceof GenericKeyboard
                && ((GenericKeyboard) newKeyboard).disableKeyPreviews()) {
            // Phone keyboard never shows popup preview (except language
            // switch).
            setPreviewEnabled(false);
        } else {
            setPreviewEnabled(AnyApplication.getConfig().getShowKeyPreview());
        }
        // TODO: For now! should be a calculated value
        // lots of key : true
        // some keys: false
        setProximityCorrectionEnabled(true);
        // One-seventh of the keyboard width seems like a reasonable threshold
        // mJumpThresholdSquare = newKeyboard.getMinWidth() / 7;
        // mJumpThresholdSquare *= mJumpThresholdSquare;
        // Assuming there are 4 rows, this is the coordinate of the last row
        // mLastRowY = (newKeyboard.getHeight() * 3) / 4;
        // setKeyboardLocal(newKeyboard);

        // looking for the space-bar, so I'll be able to detect swipes starting
        // at it
        mSpaceBarKey = null;
        if (newKeyboard != null) {
            for (Key aKey : newKeyboard.getKeys()) {
                if (aKey.getPrimaryCode() == KeyCodes.SPACE) {
                    mSpaceBarKey = aKey;
                    break;
                }
            }
        }
    }

    @Override
    public boolean setValueFromTheme(TypedArray remoteTypedArray, int[] padding, int localAttrId, int remoteTypedArrayIndex) {
        switch (localAttrId) {
            case R.attr.previewGestureTextSize:
                mGesturePreviewTextSize = remoteTypedArray.getDimensionPixelSize(remoteTypedArrayIndex, 0);
                Log.d(TAG, "AnySoftKeyboardTheme_previewGestureTextSize %f", mGesturePreviewTextSize);
                break;
            case R.attr.previewGestureTextColor:
                mGesturePreviewTextColor = remoteTypedArray.getColor(remoteTypedArrayIndex, 0xFFF);
                Log.d(TAG, "AnySoftKeyboardTheme_previewGestureTextColor %d", mGesturePreviewTextColor);
            default:
                return super.setValueFromTheme(remoteTypedArray, padding, localAttrId, remoteTypedArrayIndex);
        }
        return true;
    }

    @Override
    protected int getKeyboardStyleResId(KeyboardTheme theme) {
        return theme.getThemeResId();
    }

    @Override
    final protected boolean isFirstDownEventInsideSpaceBar() {
        return mIsFirstDownEventInsideSpaceBar;
    }

    public boolean setShiftLocked(boolean shiftLocked) {
        AnyKeyboard keyboard = getKeyboard();
        if (keyboard != null) {
            if (keyboard.setShiftLocked(shiftLocked)) {
                invalidateAllKeys();
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean onLongPress(AddOn keyboardAddOn, Key key, boolean isSticky, boolean requireSlideInto) {
        if (mAnimationLevel == AnimationsLevel.None) {
            mMiniKeyboardPopup.setAnimationStyle(0);
        } else if (mExtensionVisible && mMiniKeyboardPopup.getAnimationStyle() != R.style.ExtensionKeyboardAnimation) {
            mMiniKeyboardPopup.setAnimationStyle(R.style.ExtensionKeyboardAnimation);
        } else if (!mExtensionVisible && mMiniKeyboardPopup.getAnimationStyle() != R.style.MiniKeyboardAnimation) {
            mMiniKeyboardPopup.setAnimationStyle(R.style.MiniKeyboardAnimation);
        }

        return super.onLongPress(keyboardAddOn, key, isSticky, requireSlideInto);
    }

    private long mExtensionKeyboardAreaEntranceTime = -1;

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent me) {
        if (getKeyboard() == null)//I mean, if there isn't any keyboard I'm handling, what's the point?
            return false;

        if (areTouchesDisabled()) return super.onTouchEvent(me);

        if (me.getAction() == MotionEvent.ACTION_DOWN) {
            mFirstTouchPoint.x = (int) me.getX();
            mFirstTouchPoint.y = (int) me.getY();
            mIsFirstDownEventInsideSpaceBar =
                    mSpaceBarKey != null && mSpaceBarKey.isInside(mFirstTouchPoint.x, mFirstTouchPoint.y);
        } else if (mIsFirstDownEventInsideSpaceBar) {
            if (me.getAction() == MotionEvent.ACTION_MOVE) {
                setGesturePreviewText(mSwitcher, me);
                return true;
            } else if (me.getAction() == MotionEvent.ACTION_UP) {
                final int slide = getSlideDistance(me);
                final int distance = slide & 0x00FF;// removing direction
                if (distance > SLIDE_RATIO_FOR_GESTURE) {
                    //cancelling the touch (since we handle this)
                    disableTouchesTillFingersAreUp();
                    //handling the gesture
                    switch (slide & 0xFF00) {
                        case DIRECTION_DOWN:
                            mKeyboardActionListener.onSwipeDown(true);
                            break;
                        case DIRECTION_UP:
                            mKeyboardActionListener.onSwipeUp(true);
                            break;
                        case DIRECTION_LEFT:
                            mKeyboardActionListener.onSwipeLeft(true, isAtTwoFingersState());
                            break;
                        case DIRECTION_RIGHT:
                            mKeyboardActionListener.onSwipeRight(true, isAtTwoFingersState());
                            break;
                    }
                }
                super.onTouchEvent(me);
                return true;//handled
            }

        }
        // If the motion event is above the keyboard and it's a MOVE event
        // coming even before the first MOVE event into the extension area
        if (!mIsFirstDownEventInsideSpaceBar
                && me.getY() < mExtensionKeyboardYActivationPoint
                && !mMiniKeyboardPopup.isShowing()
                && !mExtensionVisible
                && me.getAction() == MotionEvent.ACTION_MOVE) {
            if (mExtensionKeyboardAreaEntranceTime <= 0)
                mExtensionKeyboardAreaEntranceTime = System.currentTimeMillis();

            if (System.currentTimeMillis() - mExtensionKeyboardAreaEntranceTime > DELAY_BEFORE_POPPING_UP_EXTENSION_KBD) {
                KeyboardExtension extKbd = ((ExternalAnyKeyboard) getKeyboard()).getExtensionLayout();
                if (extKbd == null || extKbd.getKeyboardResId() == -1) {
                    return super.onTouchEvent(me);
                } else {
                    // telling the main keyboard that the last touch was
                    // canceled
                    MotionEvent cancel = MotionEvent.obtain(me.getDownTime(),
                            me.getEventTime(), MotionEvent.ACTION_CANCEL,
                            me.getX(), me.getY(), 0);
                    super.onTouchEvent(cancel);
                    cancel.recycle();

                    mExtensionVisible = true;
                    dismissAllKeyPreviews();
                    if (mExtensionKey == null) {
                        mExtensionKey = new AnyKey(new Row(getKeyboard()), getThemedKeyboardDimens());
                        mExtensionKey.edgeFlags = 0;
                        mExtensionKey.height = 1;
                        mExtensionKey.width = 1;
                        mExtensionKey.popupResId = extKbd.getKeyboardResId();
                        mExtensionKey.externalResourcePopupLayout = mExtensionKey.popupResId != 0;
                        mExtensionKey.x = getWidth() / 2;
                        mExtensionKey.y = mExtensionKeyboardPopupOffset;
                    }
                    // so the popup will be right above your finger.
                    mExtensionKey.x = (int) me.getX();

                    onLongPress(extKbd, mExtensionKey,
                            AnyApplication.getConfig().isStickyExtensionKeyboard(),
                            !AnyApplication.getConfig().isStickyExtensionKeyboard());
                    // it is an extension..
                    mMiniKeyboard.setPreviewEnabled(true);
                    return true;
                }
            } else {
                return super.onTouchEvent(me);
            }
        } else if (mExtensionVisible && me.getY() > mExtensionKeyboardYDismissPoint) {
            // closing the popup
            dismissPopupKeyboard();
            return true;
        } else {
            return super.onTouchEvent(me);
        }
    }

    private static final int SLIDE_RATIO_FOR_GESTURE = 250;

    private void setGesturePreviewText(KeyboardSwitcher switcher, MotionEvent me) {
        /*
        if (mPreviewText == null)
            return;
        // started at SPACE, so I stick with the position. This is used
        // for showing gesture info on the spacebar.
        // we'll also add the current gesture, with alpha [0...200,255].
        // if any
        final int slide = getSlideDistance(me);
        final int slideDistance = slide & 0x00FF;// removing direction

        if (slideDistance >= 20) {
            final boolean isGesture = slideDistance > SLIDE_RATIO_FOR_GESTURE;

            final int alpha = isGesture ? 255 : slideDistance / 2;
            mPreviewText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    mGesturePreviewTextSize);
            int color = Color
                    .argb(alpha, mGesturePreviewTextColorRed,
                            mGesturePreviewTextColorGreen,
                            mGesturePreviewTextColorBlue);
            mPreviewText.setTextColor(color);
            final int swipeKeyTarget;
            final AskPrefs cfg = AnyApplication.getConfig();
            switch (slide & 0xFF00) {// removing distance
                case DIRECTION_UP:
                    swipeKeyTarget = cfg.getGestureSwipeUpKeyCode(true);
                    break;
                case DIRECTION_DOWN:
                    swipeKeyTarget = cfg.getGestureSwipeDownKeyCode();
                    break;
                case DIRECTION_LEFT:
                    swipeKeyTarget = cfg.getGestureSwipeLeftKeyCode(true, false);
                    break;
                case DIRECTION_RIGHT:
                    swipeKeyTarget = cfg.getGestureSwipeRightKeyCode(true, false);
                    break;
                default:
                    swipeKeyTarget = KeyCodes.SPACE;
                    break;
            }

            String tooltip;
            switch (swipeKeyTarget) {
                case KeyCodes.MODE_ALPHABET:
                    // printing the next alpha keyboard name
                    tooltip = switcher!=null? switcher.peekNextAlphabetKeyboard() : "";
                    break;
                case KeyCodes.MODE_SYMOBLS:
                    // printing the next alpha keyboard name
                    tooltip = switcher!=null? switcher.peekNextSymbolsKeyboard() : "";
                    break;
                default:
                    tooltip = "";
                    break;
            }
            mPreviewText.setText(tooltip);
        } else {
            mPreviewText.setText("");
        }
        */
    }

    private final static int DIRECTION_UP = 0x0100;
    private final static int DIRECTION_DOWN = 0x0200;
    private final static int DIRECTION_LEFT = 0x0400;
    private final static int DIRECTION_RIGHT = 0x0800;

    private int getSlideDistance(MotionEvent me) {
        final int horizontalSlide = ((int) me.getX()) - mFirstTouchPoint.x;
        final int horizontalSlideAbs = Math.abs(horizontalSlide);
        final int verticalSlide = ((int) me.getY()) - mFirstTouchPoint.y;
        final int verticalSlideAbs = Math.abs(verticalSlide);

        final int direction;
        final int slide;
        final int maxSlide;

        if (horizontalSlideAbs > verticalSlideAbs) {
            if (horizontalSlide > 0) {
                direction = DIRECTION_RIGHT;
            } else {
                direction = DIRECTION_LEFT;
            }
            maxSlide = mSwipeSpaceXDistanceThreshold;
            slide = Math.min(horizontalSlideAbs, maxSlide);
        } else {
            if (verticalSlide > 0) {
                direction = DIRECTION_DOWN;
            } else {
                direction = DIRECTION_UP;
            }
            maxSlide = mSwipeYDistanceThreshold;
            slide = Math.min(verticalSlideAbs, maxSlide);
        }

        final int slideRatio = (255 * slide) / maxSlide;

        return direction + slideRatio;
    }

    @Override
    protected void onUpEvent(PointerTracker tracker, int x, int y,
                             long eventTime) {
        super.onUpEvent(tracker, x, y, eventTime);
        mIsFirstDownEventInsideSpaceBar = false;
    }

    protected void onCancelEvent(PointerTracker tracker, int x, int y,
                                 long eventTime) {
        super.onCancelEvent(tracker, x, y, eventTime);
        mIsFirstDownEventInsideSpaceBar = false;
    }

    @Override
    public boolean dismissPopupKeyboard() {
        mExtensionKeyboardAreaEntranceTime = -1;
        mExtensionVisible = false;
        return super.dismissPopupKeyboard();
    }

    public void openUtilityKeyboard() {
        dismissAllKeyPreviews();
        if (mUtilityKey == null) {
            mUtilityKey = new AnyKey(new Row(getKeyboard()), getThemedKeyboardDimens());
            mUtilityKey.edgeFlags = Keyboard.EDGE_BOTTOM;
            mUtilityKey.height = 0;
            mUtilityKey.width = 0;
            mUtilityKey.popupResId = R.xml.ext_kbd_utility_utility;
            mUtilityKey.externalResourcePopupLayout = false;
            mUtilityKey.x = getWidth() / 2;
            mUtilityKey.y = getHeight() - getThemedKeyboardDimens().getSmallKeyHeight();
        }
        super.onLongPress(mDefaultAddOn, mUtilityKey, true, false);
        mMiniKeyboard.setPreviewEnabled(true);
    }

    public void requestInAnimation(Animation animation) {
        if (mAnimationLevel != AnimationsLevel.None)
            mInAnimation = animation;
        else
            mInAnimation = null;
    }

    @Override
    public void onDraw(Canvas canvas) {
        final boolean keyboardChanged = mKeyboardChanged;
        super.onDraw(canvas);
        // switching animation
        if (mAnimationLevel != AnimationsLevel.None && keyboardChanged
                && (mInAnimation != null)) {
            startAnimation(mInAnimation);
            mInAnimation = null;
        }
        // text pop out animation
        if (mPopOutText != null && mAnimationLevel != AnimationsLevel.None) {
            final int maxVerticalTravel = getHeight() / 2;
            final long currentAnimationTime = SystemClock.elapsedRealtime() - mPopOutTime;
            if (currentAnimationTime > TEXT_POP_OUT_ANIMATION_DURATION) {
                mPopOutText = null;
            } else {
                final float popOutPositionProgress = ((float) currentAnimationTime) / ((float) TEXT_POP_OUT_ANIMATION_DURATION);
                final float animationProgress = mPopOutTextReverting? 1f-popOutPositionProgress : popOutPositionProgress;
                final float animationInterpolatorPosition = getPopOutAnimationInterpolator(false, animationProgress);
                final int y =
                        mPopOutStartPoint.y - (int) (maxVerticalTravel * animationInterpolatorPosition);
                final int x = mPopOutStartPoint.x;
                final int alpha = mPopOutTextReverting?
                        (int) (255 * animationProgress)
                        : 255 - (int) (255 * animationProgress);
                // drawing
                setPaintToKeyText(mPaint);
                // will disappear over time
                mPaint.setAlpha(alpha);
                mPaint.setShadowLayer(5, 0, 0, Color.BLACK);
                // will grow over time
                mPaint.setTextSize(
                        mPaint.getTextSize() * (1.0f + animationInterpolatorPosition));
                canvas.translate(x, y);
                canvas.drawText(mPopOutText, 0, mPopOutText.length(), 0, 0, mPaint);
                canvas.translate(-x, -y);
                //we're doing reverting twice much faster
                if (mPopOutTextReverting) {
                    mPopOutTime = mPopOutTime - (int)(60*popOutPositionProgress);
                }
                // next frame
                postInvalidateDelayed(1000 / 60);// doing 60 frames per second;
            }
        }
    }

    /*
     * Taken from Android's DecelerateInterpolator.java and AccelerateInterpolator.java
     */
    private static float getPopOutAnimationInterpolator(final boolean isAccelerating, final float input) {
        return isAccelerating?
                input * input :
                (1.0f - (1.0f - input) * (1.0f - input));
    }

    private boolean mPopOutTextReverting = false;
    private CharSequence mPopOutText = null;
    private long mPopOutTime = 0;
    private final Point mPopOutStartPoint = new Point();

    public void revertPopTextOutOfKey() {
        if (TextUtils.isEmpty(mPopOutText)) return;

        if (!mPopOutTextReverting) {
            mPopOutTextReverting = true;
            //re-setting the mPopOutTime to reflect the time required to revert back
            final long currentAnimationTime = SystemClock.elapsedRealtime() - mPopOutTime;
            final long animationTimeLeft = TEXT_POP_OUT_ANIMATION_DURATION - currentAnimationTime;
            mPopOutTime = SystemClock.elapsedRealtime() - animationTimeLeft;
        }
    }

    public void popTextOutOfKey(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            Log.w(TAG, "Call for popTextOutOfKey with missing text argument!");
            return;
        }
        if (!AnyApplication.getConfig().workaround_alwaysUseDrawText())
            return;// not doing it with StaticLayout

        mPopOutTextReverting = false;
        //performing "toString" so we'll have a separate copy of the CharSequence,
        // and not the original object which I fear is a reference copy (hence may be changed).
        mPopOutText = text.toString();
        mPopOutTime = SystemClock.elapsedRealtime();
        mPopOutStartPoint.x = mFirstTouchPoint.x;
        mPopOutStartPoint.y = mFirstTouchPoint.y;
        // it is ok to wait for the next loop.
        postInvalidate();
    }
}
