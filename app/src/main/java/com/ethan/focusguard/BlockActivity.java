package com.ethan.focusguard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BlockActivity extends Activity {
    private TextView status;
    private final Handler handler = new Handler();

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            status.setText(FocusManager.statusText(BlockActivity.this));
            if (!FocusManager.isFocusActive(BlockActivity.this)) {
                finish();
                return;
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(UiKit.dp(this, 28), UiKit.dp(this, 28), UiKit.dp(this, 28), UiKit.dp(this, 28));
        UiKit.page(this, root);

        LinearLayout card = UiKit.card(this);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(card);

        TextView emoji = UiKit.text(this, "🛡️", 58, UiKit.color(this, R.color.accent), false);
        emoji.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(emoji);

        TextView title = UiKit.text(this, "Focus mode is on", 30, UiKit.color(this, R.color.text_primary), true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, UiKit.dp(this, 16), 0, UiKit.dp(this, 8));
        card.addView(title);

        TextView message = UiKit.text(this, "This app is blocked right now. Go back to the thing you said mattered.", 18, UiKit.color(this, R.color.text_secondary), false);
        message.setGravity(Gravity.CENTER_HORIZONTAL);
        message.setPadding(0, 0, 0, UiKit.dp(this, 22));
        card.addView(message);

        status = UiKit.text(this, "", 18, UiKit.color(this, R.color.success), true);
        status.setGravity(Gravity.CENTER_HORIZONTAL);
        status.setPadding(0, 0, 0, UiKit.dp(this, 26));
        card.addView(status);

        Button openFocusGuard = UiKit.button(this, "Open FocusGuard");
        openFocusGuard.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
        card.addView(openFocusGuard);

        setContentView(root);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
