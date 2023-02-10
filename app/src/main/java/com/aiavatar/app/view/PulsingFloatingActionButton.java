package com.aiavatar.app.view;


import android.animation.Animator;
import android.content.Context;
import android.util.AttributeSet;

import com.aiavatar.app.view.animation.AnimationCompleteListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class PulsingFloatingActionButton extends FloatingActionButton {

  private boolean pulsing;

  public PulsingFloatingActionButton(Context context) {
    super(context);
  }

  public PulsingFloatingActionButton(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public PulsingFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public void startPulse(long periodMillis) {
    if (!pulsing) {
      pulsing = true;
      pulse(periodMillis);
    }
  }

  public void stopPulse() {
    pulsing = false;
  }

  private void pulse(long periodMillis) {
    if (!pulsing) return;

    this.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).setListener(new AnimationCompleteListener() {
      @Override
      public void onAnimationEnd(Animator animation) {
        clearAnimation();
        animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).setListener(new AnimationCompleteListener() {
          @Override
          public void onAnimationEnd(Animator animation) {
            PulsingFloatingActionButton.this.postDelayed(() -> pulse(periodMillis), periodMillis);
          }
        }).start();
      }
    }).start();
  }

}
