package com.aiavatar.app.view;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aiavatar.app.R;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class EtaCountDownView extends androidx.appcompat.widget.AppCompatTextView {
  private static final String TAG = EtaCountDownView.class.getSimpleName();

  private long countDownToTime;
  @Nullable
  private Listener listener;

  private final Handler mHandler;

  public EtaCountDownView(Context context) {
    super(context);
  }

  public EtaCountDownView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public EtaCountDownView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  /* init */ {
     mHandler = new Handler(Looper.getMainLooper());
  }

  /**
   * Starts a count down to the specified {@param time}.
   */
  public void startCountDownTo(long time) {
    if (time > 0) {
      this.countDownToTime = time;
      stopCountDown();
      updateCountDown();
    }
  }

  public void stopCountDown() {
    mHandler.removeCallbacksAndMessages(null);
  }

  public void setCallEnabled() {
    // setText(R.string.RegistrationActivity_call);
    setEnabled(true);
    // setAlpha(1.0f);
  }

  private void updateCountDown() {
    final long remainingMillis = countDownToTime - System.currentTimeMillis();

    if (remainingMillis > 0) {
      setEnabled(false);
      // setAlpha(0.5f);

      int totalRemainingSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(remainingMillis);
      int hoursRemaining        = totalRemainingSeconds % 60;
      int minutesRemaining      = totalRemainingSeconds / 60;
      int secondsRemaining      = totalRemainingSeconds % 60;

      final String formattedTime = getFormattedTime(remainingMillis);
      Log.d(TAG, String.format("updateCountDown: remaining = %ss formatted = %s", remainingMillis / 1000, formattedTime));

      setText(getResources().getString(R.string.Eta_text, formattedTime));

      if (listener != null) {
        listener.onRemaining(this, totalRemainingSeconds);
      }

      mHandler.postDelayed(this::updateCountDown, 1000);
    } else {
      // setCallEnabled();
    }
  }

  public void setListener(@Nullable Listener listener) {
    this.listener = listener;
  }

  private String getFormattedTime(long etaMillis) {
    long millisUntilFinished  = etaMillis;
    long secondInMillis       = millisUntilFinished / 60;
    long minuteInMillis       = secondInMillis * 60;
    long hourInMillis         = minuteInMillis * 60;

    long elapsedHours     = TimeUnit.MILLISECONDS.toHours(millisUntilFinished) % 60;
    long elapsedMinutes   = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60;
    long elapsedSeconds   = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;

      return String.format(Locale.ENGLISH,
              "%02d:%02d:%02d",
              elapsedHours,
              elapsedMinutes,
              elapsedSeconds
      );
  }

  public interface Listener {
    void onRemaining(@NonNull EtaCountDownView view, int secondsRemaining);
  }
}
