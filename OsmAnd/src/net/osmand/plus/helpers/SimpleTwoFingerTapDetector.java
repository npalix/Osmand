package net.osmand.plus.helpers;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Created by Barsik on 24.06.2014.
 */
public abstract class SimpleTwoFingerTapDetector {
	private static final int TIMEOUT = ViewConfiguration.getTapTimeout() + 100;
	private long mFirstDownTime = 0;
	private byte mTwoFingerTapCount = 0;

	private void reset(long time) {
		mFirstDownTime = time;
		mTwoFingerTapCount = 0;
	}


	public boolean onTouchEvent(MotionEvent event) {
		//this is workaround
		// because we support android 2.2
		int action = event.getAction();
		//action code will have value same as event.getActionMasked()
		int actionCode = action & MotionEvent.ACTION_MASK;
		switch (actionCode) {
			case MotionEvent.ACTION_DOWN:
				if (mFirstDownTime == 0 || event.getEventTime() - mFirstDownTime > TIMEOUT){
					reset(event.getDownTime());
				}
				break;
			case MotionEvent.ACTION_POINTER_UP:
				if (event.getPointerCount() == 2) {
					mTwoFingerTapCount++;
				}
				else{
					mFirstDownTime = 0;
				}
				break;
			case MotionEvent.ACTION_UP:
				if (mTwoFingerTapCount == 1 && event.getEventTime() - mFirstDownTime < TIMEOUT) {
					onTwoFingerTap();
					mFirstDownTime = 0;
					return true;
				}
		}

		return false;
	}

	public abstract void onTwoFingerTap();
}
