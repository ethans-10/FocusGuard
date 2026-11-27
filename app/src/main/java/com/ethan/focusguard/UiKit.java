package com.ethan.focusguard;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class UiKit {
    private UiKit() {}

    public static int color(Activity activity, int resourceId) {
        return activity.getColor(resourceId);
    }

    public static int dp(Activity activity, int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static void page(Activity activity, View root) {
        int background = color(activity, R.color.page_background);
        boolean dark = (activity.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        root.setBackgroundColor(background);
        Window window = activity.getWindow();
        window.setStatusBarColor(background);
        window.setNavigationBarColor(Build.VERSION.SDK_INT >= 26 || dark
                ? background : Color.parseColor("#202124"));

        int systemUiVisibility = dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26 && !dark) {
            systemUiVisibility |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        window.getDecorView().setSystemUiVisibility(systemUiVisibility);
    }

    public static GradientDrawable rounded(int color, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusDp);
        return drawable;
    }

    public static GradientDrawable roundedStroke(Activity activity, int color, int strokeColor, int strokeDp, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(activity, radiusDp));
        drawable.setStroke(dp(activity, strokeDp), strokeColor);
        return drawable;
    }

    public static TextView text(Activity activity, String value, int sp, int color, boolean bold) {
        TextView tv = new TextView(activity);
        tv.setText(value);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setLineSpacing(dp(activity, 2), 1.0f);
        if (bold) tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    public static LinearLayout card(Activity activity) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(activity, 18), dp(activity, 18), dp(activity, 18), dp(activity, 18));
        card.setBackground(roundedStroke(activity, color(activity, R.color.surface),
                color(activity, R.color.border), 1, 24));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, dp(activity, 12), 0, dp(activity, 12));
        card.setLayoutParams(lp);
        return card;
    }

    public static LinearLayout pill(Activity activity, int color) {
        LinearLayout pill = new LinearLayout(activity);
        pill.setOrientation(LinearLayout.VERTICAL);
        pill.setPadding(dp(activity, 14), dp(activity, 10), dp(activity, 14), dp(activity, 10));
        pill.setBackground(rounded(color, dp(activity, 18)));
        return pill;
    }

    public static Button button(Activity activity, String label) {
        Button button = new Button(activity);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setBackground(rounded(color(activity, R.color.accent), dp(activity, 18)));
        button.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, dp(activity, 8), 0, dp(activity, 8));
        button.setLayoutParams(lp);
        return button;
    }

    public static Button secondaryButton(Activity activity, String label) {
        Button button = button(activity, label);
        button.setTextColor(color(activity, R.color.text_primary));
        button.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        button.setBackground(roundedStroke(activity, color(activity, R.color.surface),
                color(activity, R.color.border), 1, 18));
        return button;
    }

    public static Button dangerButton(Activity activity, String label) {
        Button button = button(activity, label);
        button.setBackground(rounded(color(activity, R.color.danger), dp(activity, 18)));
        return button;
    }

    public static TextView sectionHeader(Activity activity, String title, String subtitle) {
        TextView tv = text(activity, title + (subtitle == null || subtitle.isEmpty() ? "" : "\n" + subtitle),
                18, color(activity, R.color.text_primary), true);
        tv.setPadding(0, 0, 0, dp(activity, 12));
        return tv;
    }

    public static LinearLayout row(Activity activity) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(activity, 4), 0, dp(activity, 4));
        return row;
    }

    public static void topMargin(Activity activity, View view, int dp) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) view.getLayoutParams();
        if (lp == null) {
            lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }
        lp.setMargins(lp.leftMargin, dp(activity, dp), lp.rightMargin, lp.bottomMargin);
        view.setLayoutParams(lp);
    }
}
