package com.ethan.focusguard;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ComponentName;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class StandbyClockActivity extends Activity {
    private static final String[] STYLE_NAMES = {
            "Glow", "Clean", "Thin", "Mono", "Rounded", "Stack", "Wide", "Bold", "Soft", "Classic"
    };

    private FrameLayout rootFrame;
    private LinearLayout mainContent;
    private LinearLayout clockPane;
    private LinearLayout cardsPane;
    private LinearLayout overlayContainer;
    private LinearLayout menuShell;
    private LinearLayout expandedPanel;
    private LinearLayout stylePanel;
    private LinearLayout colourPanel;
    private TextView collapsedHandle;
    private Button secondsToggleButton;
    private GradientTextClock clock;
    private TextClock secondsClock;
    private TextView brandLabel;
    private TextView dateLine;
    private TextView statusLine;
    private TextView hintLine;

    private LinearLayout weatherCard;
    private TextView weatherTemp;
    private TextView weatherCondition;
    private TextView weatherCity;

    private LinearLayout batteryCard;
    private RingView batteryRing;
    private TextView batteryPercent;
    private TextView batteryLabel;

    private LinearLayout musicCard;
    private TextView musicEyebrow;
    private TextView musicTitle;
    private TextView musicArtist;
    private TextView musicMeta;
    private TextView musicPrevButton;
    private TextView musicPlayPauseButton;
    private TextView musicNextButton;

    private TextView colourHex;
    private View colourSwatch;
    private ColorWheelView colorWheel;

    private boolean autoStandby = false;
    private boolean menuExpanded = false;
    private int accentColor;
    private long lastWeatherRequest = 0L;

    private final Handler handler = new Handler();
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            updateLiveInfo();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        autoStandby = getIntent().getBooleanExtra("autoStandby", false);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        buildUi();
        hideSystemButtons();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemButtons();
    }

    private void hideSystemButtons() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        autoStandby = intent.getBooleanExtra("autoStandby", false);
        buildUi();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        buildUi();
        applySavedStyle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applySavedStyle();
        handler.post(refreshRunnable);
        requestWeatherIfNeeded(false);
        hideSystemButtons();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoStandby) FocusManager.noteStandbyDismissed(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (autoStandby && event.getAction() == MotionEvent.ACTION_DOWN) {
            FocusManager.noteStandbyDismissed(this);
            finish();
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    private void buildUi() {
        boolean landscape = isLandscape();

        rootFrame = new FrameLayout(this);
        rootFrame.setBackground(appBackground(Color.parseColor("#04070E"), Color.parseColor("#091429")));

        mainContent = new LinearLayout(this);
        mainContent.setOrientation(landscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        mainContent.setGravity(Gravity.CENTER);
        int pad = UiKit.dp(this, landscape ? 14 : 18);
        mainContent.setPadding(pad, UiKit.dp(this, landscape ? 10 : 18), pad, UiKit.dp(this, autoStandby ? 14 : (landscape ? 10 : 82)));

        buildClockPane(landscape);
        buildCardsPane(landscape);

        if (landscape) {
            mainContent.addView(clockPane, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.05f));
            mainContent.addView(cardsPane, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.05f));
        } else {
            mainContent.addView(clockPane, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
            mainContent.addView(cardsPane, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        rootFrame.addView(mainContent, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        if (!autoStandby) {
            buildOverlayControls();
            FrameLayout.LayoutParams overlayLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            overlayLp.setMargins(UiKit.dp(this, 12), UiKit.dp(this, 12), UiKit.dp(this, 12), UiKit.dp(this, 16));
            rootFrame.addView(overlayContainer, overlayLp);
            if (landscape) {
                overlayContainer.setVisibility(View.GONE);
                menuExpanded = false;
            }
        }

        setContentView(rootFrame);
        updateLiveInfo();
    }

    private void buildClockPane(boolean landscape) {
        clockPane = new LinearLayout(this);
        clockPane.setOrientation(LinearLayout.VERTICAL);
        clockPane.setGravity(Gravity.CENTER);
        clockPane.setPadding(UiKit.dp(this, 8), UiKit.dp(this, isLandscape() ? 4 : 6), UiKit.dp(this, 8), UiKit.dp(this, isLandscape() ? 4 : 6));
        clockPane.setOnClickListener(v -> {
            if (!autoStandby && isLandscape() && !menuExpanded) {
                setMenuExpanded(true);
            }
        });

        brandLabel = new TextView(this);
        brandLabel.setText("FocusGuard");
        brandLabel.setTextSize(landscape ? 18 : 17);
        brandLabel.setTypeface(Typeface.DEFAULT_BOLD);
        brandLabel.setLetterSpacing(0.03f);
        brandLabel.setTextColor(Color.WHITE);
        brandLabel.setAlpha(0.94f);
        clockPane.addView(brandLabel);

        clock = new GradientTextClock(this);
        clock.setGravity(Gravity.CENTER);
        clock.setIncludeFontPadding(false);
        clock.setShadowLayer(22f, 0f, 0f, Color.argb(80, 77, 148, 255));
        LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clockLp.setMargins(0, UiKit.dp(this, landscape ? 8 : 6), 0, 0);
        clock.setLayoutParams(clockLp);
        clockPane.addView(clock);

        secondsClock = new TextClock(this);
        secondsClock.setGravity(Gravity.CENTER);
        secondsClock.setIncludeFontPadding(false);
        secondsClock.setTextColor(Color.parseColor("#A8B4D6"));
        secondsClock.setTypeface(Typeface.MONOSPACE);
        secondsClock.setVisibility(View.GONE);
        clockPane.addView(secondsClock);

        dateLine = new TextView(this);
        dateLine.setGravity(Gravity.CENTER);
        dateLine.setTextColor(Color.WHITE);
        dateLine.setAlpha(0.88f);
        dateLine.setTextSize(landscape ? 15 : 14);
        dateLine.setPadding(0, UiKit.dp(this, isLandscape() ? 3 : 4), 0, 0);
        clockPane.addView(dateLine);

        statusLine = new TextView(this);
        statusLine.setGravity(Gravity.CENTER);
        statusLine.setTextColor(Color.parseColor("#C0C8E6"));
        statusLine.setTextSize(landscape ? 12 : 12);
        statusLine.setPadding(0, UiKit.dp(this, isLandscape() ? 2 : 3), 0, 0);
        clockPane.addView(statusLine);

        hintLine = new TextView(this);
        hintLine.setGravity(Gravity.CENTER);
        hintLine.setTextColor(Color.parseColor("#7A86A8"));
        hintLine.setTextSize(isLandscape() ? 11 : 12);
        hintLine.setPadding(0, UiKit.dp(this, isLandscape() ? 3 : 5), 0, 0);
        hintLine.setText(autoStandby ? "Tap anywhere to exit standby" : "Use the bottom handle to customise");
        clockPane.addView(hintLine);
    }

    private void buildCardsPane(boolean landscape) {
        cardsPane = new LinearLayout(this);
        cardsPane.setOrientation(LinearLayout.VERTICAL);
        cardsPane.setGravity(landscape ? Gravity.CENTER_VERTICAL : Gravity.CENTER_HORIZONTAL);

        weatherCard = cardContainer();
        LinearLayout weatherRow = new LinearLayout(this);
        weatherRow.setOrientation(LinearLayout.HORIZONTAL);
        weatherRow.setGravity(Gravity.CENTER_VERTICAL);
        weatherCard.addView(weatherRow);

        TextView weatherIcon = iconBubble("☁");
        int iconSize = UiKit.dp(this, landscape ? 44 : 56);
        weatherRow.addView(weatherIcon, new LinearLayout.LayoutParams(iconSize, iconSize));

        LinearLayout weatherTextCol = new LinearLayout(this);
        weatherTextCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams weatherTextLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        weatherTextLp.setMargins(UiKit.dp(this, landscape ? 10 : 14), 0, 0, 0);
        weatherRow.addView(weatherTextCol, weatherTextLp);

        weatherTemp = new TextView(this);
        weatherTemp.setTextSize(landscape ? 21 : 30);
        weatherTemp.setTypeface(Typeface.DEFAULT_BOLD);
        weatherTemp.setTextColor(Color.WHITE);
        weatherTextCol.addView(weatherTemp);

        weatherCondition = new TextView(this);
        weatherCondition.setTextSize(landscape ? 14 : 15);
        weatherCondition.setTextColor(Color.parseColor("#D6DDF6"));
        weatherTextCol.addView(weatherCondition);

        weatherCity = new TextView(this);
        weatherCity.setTextSize(13);
        weatherCity.setTextColor(Color.parseColor("#96A1C5"));
        weatherTextCol.addView(weatherCity);

        cardsPane.addView(weatherCard);

        batteryCard = cardContainer();
        LinearLayout batteryRow = new LinearLayout(this);
        batteryRow.setOrientation(LinearLayout.HORIZONTAL);
        batteryRow.setGravity(Gravity.CENTER_VERTICAL);
        batteryCard.addView(batteryRow);

        batteryRing = new RingView(this);
        LinearLayout.LayoutParams ringLp = new LinearLayout.LayoutParams(UiKit.dp(this, landscape ? 50 : 64), UiKit.dp(this, landscape ? 50 : 64));
        batteryRow.addView(batteryRing, ringLp);

        LinearLayout batteryTextCol = new LinearLayout(this);
        batteryTextCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams batteryTextLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        batteryTextLp.setMargins(UiKit.dp(this, landscape ? 10 : 14), 0, 0, 0);
        batteryRow.addView(batteryTextCol, batteryTextLp);

        batteryPercent = new TextView(this);
        batteryPercent.setTextSize(landscape ? 21 : 28);
        batteryPercent.setTypeface(Typeface.DEFAULT_BOLD);
        batteryPercent.setTextColor(Color.WHITE);
        batteryTextCol.addView(batteryPercent);

        batteryLabel = new TextView(this);
        batteryLabel.setTextSize(13);
        batteryLabel.setTextColor(Color.parseColor("#B8C3E6"));
        batteryTextCol.addView(batteryLabel);

        cardsPane.addView(batteryCard);

        musicCard = cardContainer();
        LinearLayout musicRow = new LinearLayout(this);
        musicRow.setOrientation(LinearLayout.HORIZONTAL);
        musicRow.setGravity(Gravity.CENTER_VERTICAL);
        musicCard.addView(musicRow);

        TextView musicArt = iconBubble("♪");
        musicArt.setTextSize(28);
        musicRow.addView(musicArt, new LinearLayout.LayoutParams(iconSize, iconSize));

        LinearLayout musicTextCol = new LinearLayout(this);
        musicTextCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams musicTextLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        musicTextLp.setMargins(UiKit.dp(this, landscape ? 10 : 14), 0, UiKit.dp(this, landscape ? 4 : 10), 0);
        musicRow.addView(musicTextCol, musicTextLp);

        musicEyebrow = new TextView(this);
        musicEyebrow.setTextSize(11);
        musicEyebrow.setLetterSpacing(0.08f);
        musicEyebrow.setTextColor(Color.parseColor("#8B96BB"));
        musicEyebrow.setTypeface(Typeface.DEFAULT_BOLD);
        musicTextCol.addView(musicEyebrow);

        musicTitle = new TextView(this);
        musicTitle.setTextSize(landscape ? 17 : 22);
        musicTitle.setTypeface(Typeface.DEFAULT_BOLD);
        musicTitle.setTextColor(Color.WHITE);
        setupMarquee(musicTitle);
        musicTextCol.addView(musicTitle);

        musicArtist = new TextView(this);
        musicArtist.setTextSize(landscape ? 12 : 14);
        musicArtist.setTextColor(Color.parseColor("#D6DDF6"));
        setupMarquee(musicArtist);
        musicTextCol.addView(musicArtist);

        musicMeta = new TextView(this);
        musicMeta.setTextSize(landscape ? 10 : 12);
        musicMeta.setTextColor(Color.parseColor("#8692B8"));
        musicMeta.setMaxLines(1);
        musicMeta.setEllipsize(TextUtils.TruncateAt.END);
        musicTextCol.addView(musicMeta);

        LinearLayout musicControls = new LinearLayout(this);
        musicControls.setOrientation(LinearLayout.HORIZONTAL);
        musicControls.setGravity(Gravity.CENTER_VERTICAL);
        musicRow.addView(musicControls);
        musicPrevButton = iconControl("‹");
        musicPlayPauseButton = iconControl("▶");
        musicNextButton = iconControl("›");
        musicPrevButton.setOnClickListener(v -> sendMediaCommand("previous"));
        musicPlayPauseButton.setOnClickListener(v -> sendMediaCommand("playpause"));
        musicNextButton.setOnClickListener(v -> sendMediaCommand("next"));
        musicControls.addView(musicPrevButton);
        musicControls.addView(musicPlayPauseButton);
        musicControls.addView(musicNextButton);

        cardsPane.addView(musicCard);
    }

    private void setupMarquee(TextView tv) {
        tv.setSingleLine(true);
        tv.setMaxLines(1);
        tv.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        tv.setHorizontallyScrolling(true);
        tv.setSelected(true);  // Required for marquee to start without focus
        tv.setFocusable(false);
        tv.setFocusableInTouchMode(false);
        tv.setMarqueeRepeatLimit(-1);
    }

    private void buildOverlayControls() {
        overlayContainer = new LinearLayout(this);
        overlayContainer.setOrientation(LinearLayout.VERTICAL);
        overlayContainer.setGravity(Gravity.CENTER_HORIZONTAL);

        expandedPanel = new LinearLayout(this);
        expandedPanel.setOrientation(LinearLayout.VERTICAL);
        expandedPanel.setVisibility(View.GONE);
        expandedPanel.setPadding(UiKit.dp(this, 10), 0, UiKit.dp(this, 10), UiKit.dp(this, isLandscape() ? 6 : 10));
        overlayContainer.addView(expandedPanel, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        stylePanel = new LinearLayout(this);
        stylePanel.setOrientation(LinearLayout.VERTICAL);
        stylePanel.setVisibility(View.GONE);
        expandedPanel.addView(stylePanel);
        buildStylePanel();

        colourPanel = new LinearLayout(this);
        colourPanel.setOrientation(LinearLayout.VERTICAL);
        colourPanel.setVisibility(View.GONE);
        expandedPanel.addView(colourPanel);
        buildColourPanel();

        menuShell = new LinearLayout(this);
        menuShell.setOrientation(LinearLayout.HORIZONTAL);
        menuShell.setGravity(Gravity.CENTER_VERTICAL);
        menuShell.setPadding(UiKit.dp(this, 10), UiKit.dp(this, 10), UiKit.dp(this, 10), UiKit.dp(this, 10));
        overlayContainer.addView(menuShell, new LinearLayout.LayoutParams(isLandscape() ? LinearLayout.LayoutParams.MATCH_PARENT : LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        collapsedHandle = new TextView(this);
        collapsedHandle.setText("⌃");
        collapsedHandle.setTextColor(Color.WHITE);
        collapsedHandle.setTextSize(22);
        collapsedHandle.setGravity(Gravity.CENTER);
        collapsedHandle.setPadding(UiKit.dp(this, isLandscape() ? 14 : 12), UiKit.dp(this, isLandscape() ? 8 : 9), UiKit.dp(this, isLandscape() ? 14 : 12), UiKit.dp(this, isLandscape() ? 8 : 9));
        collapsedHandle.setOnClickListener(v -> toggleMenu());
        menuShell.addView(collapsedHandle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        HorizontalScrollView buttonScroll = new HorizontalScrollView(this);
        buttonScroll.setHorizontalScrollBarEnabled(false);
        buttonScroll.setFillViewport(false);
        buttonScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        buttonScroll.setVisibility(View.GONE);
        buttonScroll.setTag("buttonScroll");
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        scrollLp.setMargins(UiKit.dp(this, isLandscape() ? 6 : 4), 0, 0, 0);
        menuShell.addView(buttonScroll, scrollLp);

        LinearLayout buttonsRow = new LinearLayout(this);
        buttonsRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonsRow.setGravity(Gravity.CENTER_VERTICAL);
        buttonsRow.setTag("buttonsRow");
        buttonScroll.addView(buttonsRow, new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        buttonsRow.addView(menuAction("Style", v -> {
            stylePanel.setVisibility(stylePanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            colourPanel.setVisibility(View.GONE);
            expandedPanel.setVisibility(stylePanel.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        }));
        secondsToggleButton = (Button) menuAction("Seconds", v -> {
            FocusManager.toggleClockSeconds(this);
            applySavedStyle();
        });
        buttonsRow.addView(secondsToggleButton);
        buttonsRow.addView(menuAction("Colour", v -> {
            colourPanel.setVisibility(colourPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            stylePanel.setVisibility(View.GONE);
            expandedPanel.setVisibility(colourPanel.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        }));
        buttonsRow.addView(menuAction("Dim", v -> {
            FocusManager.toggleClockDim(this);
            applySavedStyle();
        }));
        buttonsRow.addView(menuAction("Refresh", v -> requestWeatherIfNeeded(true)));
        buttonsRow.addView(menuAction("Close", v -> finish()));
    }

    private void buildStylePanel() {
        stylePanel.removeAllViews();
        stylePanel.addView(panelTitle("Clock style"));

        HorizontalScrollView styleScroll = new HorizontalScrollView(this);
        styleScroll.setHorizontalScrollBarEnabled(false);
        styleScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        stylePanel.addView(styleScroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        styleScroll.addView(row, new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < STYLE_NAMES.length; i++) {
            final int index = i;
            TextView card = new TextView(this);
            card.setText(previewTextForStyle(i));
            card.setTextColor(Color.WHITE);
            card.setTag("styleCard_" + index);
            card.setGravity(Gravity.CENTER);
            card.setPadding(UiKit.dp(this, 8), UiKit.dp(this, isLandscape() ? 8 : 10), UiKit.dp(this, 8), UiKit.dp(this, isLandscape() ? 8 : 10));
            card.setTextSize(isLandscape() ? 14 : 16);
            card.setTypeface(typefaceForStyle(i));
            card.setOnClickListener(v -> {
                FocusManager.setClockStyle(this, index);
                applySavedStyle();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(UiKit.dp(this, isLandscape() ? 106 : 118), UiKit.dp(this, isLandscape() ? 66 : 78));
            lp.setMargins(UiKit.dp(this, 4), UiKit.dp(this, 4), UiKit.dp(this, 4), UiKit.dp(this, 4));
            card.setLayoutParams(lp);
            row.addView(card);
        }
    }

    private void buildColourPanel() {
        colourPanel.removeAllViews();
        colourPanel.addView(panelTitle("Clock colour"));

        TextView help = new TextView(this);
        help.setText("Drag around the wheel to choose the exact colour");
        help.setTextColor(Color.parseColor("#B8C3E6"));
        help.setTextSize(13);
        help.setGravity(Gravity.CENTER);
        help.setPadding(0, 0, 0, UiKit.dp(this, 10));
        colourPanel.addView(help);

        FrameLayout wheelHolder = new FrameLayout(this);
        int wheelSize = UiKit.dp(this, isLandscape() ? 145 : 170);
        colourPanel.addView(wheelHolder, new LinearLayout.LayoutParams(wheelSize, wheelSize));
        colorWheel = new ColorWheelView(this);
        colorWheel.setOnColorChangedListener(color -> {
            FocusManager.setClockCustomColor(this, color);
            applySavedStyle();
        });
        wheelHolder.addView(colorWheel, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout swatchRow = new LinearLayout(this);
        swatchRow.setOrientation(LinearLayout.HORIZONTAL);
        swatchRow.setGravity(Gravity.CENTER);
        swatchRow.setPadding(0, UiKit.dp(this, 12), 0, 0);
        colourPanel.addView(swatchRow);

        colourSwatch = new View(this);
        LinearLayout.LayoutParams swatchLp = new LinearLayout.LayoutParams(UiKit.dp(this, 34), UiKit.dp(this, 34));
        swatchLp.setMargins(0, 0, UiKit.dp(this, 10), 0);
        swatchRow.addView(colourSwatch, swatchLp);

        colourHex = new TextView(this);
        colourHex.setTextColor(Color.WHITE);
        colourHex.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        colourHex.setTextSize(16);
        swatchRow.addView(colourHex);
    }

    private LinearLayout cardContainer() {
        boolean landscape = isLandscape();
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(UiKit.dp(this, landscape ? 11 : 16), UiKit.dp(this, landscape ? 8 : 14), UiKit.dp(this, landscape ? 11 : 16), UiKit.dp(this, landscape ? 8 : 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, UiKit.dp(this, landscape ? 4 : 7), 0, UiKit.dp(this, landscape ? 4 : 7));
        card.setLayoutParams(lp);
        return card;
    }

    private TextView iconBubble(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(24);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private TextView iconControl(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(isLandscape() ? 20 : 22);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        int size = UiKit.dp(this, isLandscape() ? 30 : 36);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(UiKit.dp(this, isLandscape() ? 2 : 3), 0, UiKit.dp(this, isLandscape() ? 2 : 3), 0);
        tv.setLayoutParams(lp);
        return tv;
    }

    private TextView panelTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextSize(16);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, UiKit.dp(this, 4), 0, UiKit.dp(this, 8));
        return tv;
    }

    private View menuAction(String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextSize(isLandscape() ? 11 : 12);
        b.setTextColor(Color.WHITE);
        b.setPadding(UiKit.dp(this, isLandscape() ? 8 : 7), UiKit.dp(this, isLandscape() ? 6 : 7), UiKit.dp(this, isLandscape() ? 8 : 7), UiKit.dp(this, isLandscape() ? 6 : 7));
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(UiKit.dp(this, 4), 0, UiKit.dp(this, 4), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void toggleMenu() {
        setMenuExpanded(!menuExpanded);
    }

    private void setMenuExpanded(boolean expanded) {
        menuExpanded = expanded;

        if (overlayContainer != null) {
            if (isLandscape()) {
                overlayContainer.setVisibility(menuExpanded ? View.VISIBLE : View.GONE);
            } else {
                overlayContainer.setVisibility(View.VISIBLE);
            }
        }

        View buttonScroll = menuShell == null ? null : menuShell.findViewWithTag("buttonScroll");
        if (buttonScroll != null) buttonScroll.setVisibility(menuExpanded ? View.VISIBLE : View.GONE);

        if (collapsedHandle != null) collapsedHandle.setText(menuExpanded ? "⌄" : "⌃");

        if (!menuExpanded) {
            if (expandedPanel != null) expandedPanel.setVisibility(View.GONE);
            if (stylePanel != null) stylePanel.setVisibility(View.GONE);
            if (colourPanel != null) colourPanel.setVisibility(View.GONE);
        }

        updateMainContentPadding();
        applySavedStyle();
    }

    private void updateMainContentPadding() {
        if (mainContent == null) return;
        int side = UiKit.dp(this, isLandscape() ? 14 : 18);
        int top = UiKit.dp(this, isLandscape() ? 10 : 18);
        int bottom;
        if (autoStandby) {
            bottom = UiKit.dp(this, 14);
        } else if (isLandscape()) {
            bottom = UiKit.dp(this, menuExpanded ? 62 : 8);
        } else {
            bottom = UiKit.dp(this, 82);
        }
        mainContent.setPadding(side, top, side, bottom);
    }

    private String previewTextForStyle(int index) {
        switch (index) {
            case 1: return "10:30\nCLEAN";
            case 2: return "10 30\nTHIN";
            case 3: return "10:30\nMONO";
            case 4: return "10:30\nROUND";
            case 5: return "10\n30\nSTACK";
            case 6: return "10:30\nWIDE";
            case 7: return "10:30\nBOLD";
            case 8: return "10:30\nSOFT";
            case 9: return "10:30\nCLASSIC";
            default: return "10\n30\nGLOW";
        }
    }

    private Typeface typefaceForStyle(int index) {
        switch (index) {
            case 1: return Typeface.create("sans-serif-light", Typeface.NORMAL);
            case 2: return Typeface.create("sans-serif-thin", Typeface.NORMAL);
            case 3: return Typeface.MONOSPACE;
            case 4: return Typeface.create("sans-serif-medium", Typeface.NORMAL);
            case 5: return Typeface.create("sans-serif-light", Typeface.NORMAL);
            case 6: return Typeface.create("sans-serif-condensed", Typeface.BOLD);
            case 7: return Typeface.DEFAULT_BOLD;
            case 8: return Typeface.create("sans-serif", Typeface.NORMAL);
            case 9: return Typeface.SERIF;
            default: return Typeface.create("sans-serif-light", Typeface.NORMAL);
        }
    }

    private void applySavedStyle() {
        int styleIndex = FocusManager.getClockStyle(this);
        accentColor = FocusManager.getClockCustomColor(this);
        boolean dim = FocusManager.isClockDim(this) || autoStandby;
        boolean secondsEnabled = FocusManager.isClockSecondsEnabled(this);

        int bgTop = dim ? Color.parseColor("#02050B") : blend(accentColor, Color.parseColor("#02050B"), 0.88f);
        int bgBottom = dim ? Color.parseColor("#08142C") : blend(accentColor, Color.parseColor("#08142C"), 0.82f);
        int panel = dim ? Color.parseColor("#121A2E") : blend(accentColor, Color.parseColor("#0B1330"), 0.68f);
        int panelSoft = dim ? Color.parseColor("#0D1224") : blend(accentColor, Color.parseColor("#091125"), 0.74f);
        int panelStroke = dim ? Color.parseColor("#24304F") : blend(accentColor, Color.parseColor("#88A3FF"), 0.48f);

        rootFrame.setBackground(appBackground(bgTop, bgBottom));
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(bgTop);
            getWindow().setNavigationBarColor(bgTop);
        }

        brandLabel.setTextColor(Color.WHITE);
        dateLine.setTextColor(Color.WHITE);
        statusLine.setTextColor(Color.parseColor("#D5DCF7"));
        hintLine.setTextColor(Color.parseColor("#95A0C3"));
        hintLine.setText(autoStandby ? "Tap anywhere to exit standby" : (isLandscape() ? "Tap clock for menu" : "Use the bottom handle to customise"));
        hintLine.setVisibility(isLandscape() && !menuExpanded && !autoStandby ? View.GONE : View.VISIBLE);
        statusLine.setVisibility(isLandscape() && !menuExpanded && !autoStandby ? View.GONE : View.VISIBLE);

        clock.setGradient(accentColor, blend(accentColor, Color.parseColor("#E879F9"), 0.45f));
        clock.setTextColor(accentColor);
        clock.setLetterSpacing(0f);
        clock.setLineSpacing(-UiKit.dp(this, isLandscape() ? 6 : 18), 0.84f);
        clock.setShadowLayer(styleIndex == 8 ? 34f : 22f, 0f, 0f, Color.argb(styleIndex == 8 ? 130 : 80, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)));
        secondsClock.setTextColor(blend(accentColor, Color.WHITE, 0.30f));
        secondsClock.setVisibility(secondsEnabled ? View.VISIBLE : View.GONE);
        secondsClock.setTextSize(isLandscape() ? 16 : 20);
        secondsClock.setFormat24Hour("ss");
        secondsClock.setFormat12Hour("ss");

        boolean landscape = isLandscape();
        int landscapeSize = menuExpanded ? 58 : 76;
        int portraitSize = secondsEnabled ? 88 : 100;

        switch (styleIndex) {
            case 1:
                clock.setTextSize(landscape ? landscapeSize - 4 : portraitSize - 6);
                clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                clock.setFormat24Hour(landscape ? "HH:mm" : "HH\nmm");
                clock.setFormat12Hour(landscape ? "h:mm" : "h\nmm");
                break;
            case 2:
                clock.setTextSize(landscape ? landscapeSize : portraitSize);
                clock.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
                clock.setFormat24Hour(landscape ? "HH mm" : "HH\nmm");
                clock.setFormat12Hour(landscape ? "h mm" : "h\nmm");
                break;
            case 3:
                clock.setTextSize(landscape ? landscapeSize - 8 : portraitSize - 10);
                clock.setTypeface(Typeface.MONOSPACE);
                clock.setFormat24Hour(landscape ? "HH:mm" : "HH\nmm");
                clock.setFormat12Hour(landscape ? "h:mm" : "h\nmm");
                break;
            case 4:
                clock.setTextSize(landscape ? landscapeSize - 4 : portraitSize - 4);
                clock.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                clock.setFormat24Hour(landscape ? "HH:mm" : "HH\nmm");
                clock.setFormat12Hour(landscape ? "h:mm" : "h\nmm");
                break;
            case 5:
                clock.setTextSize(landscape ? landscapeSize + 2 : portraitSize);
                clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                clock.setFormat24Hour(landscape ? "HH:mm" : "HH\nmm");
                clock.setFormat12Hour(landscape ? "h:mm" : "h\nmm");
                break;
            case 6:
                clock.setTextSize(landscape ? landscapeSize - 2 : portraitSize - 8);
                clock.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
                clock.setLetterSpacing(0.08f);
                clock.setFormat24Hour(landscape ? "HH:mm" : "HH\nmm");
                clock.setFormat12Hour(landscape ? "h:mm" : "h\nmm");
                break;
            case 7:
                clock.setTextSize(landscape ? landscapeSize - 8 : portraitSize - 12);
                clock.setTypeface(Typeface.DEFAULT_BOLD);
                clock.setFormat24Hour(landscape ? "HH:mm" : "HH\nmm");
                clock.setFormat12Hour(landscape ? "h:mm" : "h\nmm");
                break;
            case 8:
                clock.setTextSize(landscape ? landscapeSize - 2 : portraitSize - 4);
                clock.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                clock.setFormat24Hour(landscape ? "HH:mm" : "HH\nmm");
                clock.setFormat12Hour(landscape ? "h:mm" : "h\nmm");
                break;
            case 9:
                clock.setTextSize(landscape ? landscapeSize - 8 : portraitSize - 12);
                clock.setTypeface(Typeface.SERIF);
                clock.setFormat24Hour(landscape ? "HH:mm" : "HH\nmm");
                clock.setFormat12Hour(landscape ? "h:mm" : "h\nmm");
                break;
            default:
                clock.setTextSize(landscape ? landscapeSize : portraitSize);
                clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                clock.setFormat24Hour(landscape ? "HH:mm" : "HH\nmm");
                clock.setFormat12Hour(landscape ? "h:mm" : "h\nmm");
                break;
        }

        if (secondsToggleButton != null) {
            secondsToggleButton.setText(secondsEnabled ? "Seconds on" : "Seconds off");
        }

        styleCards(stylePanel, panelSoft, panelStroke, styleIndex);
        styleMenu(panel, panelStroke);
        styleColourPanel(panel, panelStroke);
        styleInfoCards(panel, panelStroke);
        updateMainContentPadding();
        updateLiveInfo();
    }

    private void styleMenu(int panel, int stroke) {
        if (overlayContainer == null) return;
        expandedPanel.setBackground(roundedPanel(panel, stroke, 26));
        menuShell.setBackground(roundedPanel(panel, stroke, 999));
        collapsedHandle.setBackground(roundedPanel(Color.TRANSPARENT, Color.TRANSPARENT, 999));

        View buttonsRow = menuShell.findViewWithTag("buttonsRow");
        if (buttonsRow instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) buttonsRow;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof Button) {
                    Button b = (Button) child;
                    b.setBackground(roundedPanel(blend(accentColor, Color.BLACK, 0.55f), stroke, 999));
                }
            }
        }
    }

    private void styleColourPanel(int panel, int stroke) {
        if (colourPanel == null) return;
        colourPanel.setPadding(UiKit.dp(this, 12), UiKit.dp(this, 12), UiKit.dp(this, 12), UiKit.dp(this, 12));
        colourPanel.setBackground(roundedPanel(panel, stroke, 24));
        if (colourHex != null) colourHex.setText(hex(accentColor));
        if (colourSwatch != null) colourSwatch.setBackground(roundedPanel(accentColor, Color.WHITE, 999));
        if (colorWheel != null) colorWheel.setSelectedColor(accentColor);
    }

    private void styleCards(LinearLayout panelView, int bg, int stroke, int selectedStyle) {
        if (panelView == null) return;
        panelView.setPadding(UiKit.dp(this, 12), UiKit.dp(this, 12), UiKit.dp(this, 12), UiKit.dp(this, 12));
        panelView.setBackground(roundedPanel(bg, stroke, 24));
        applyStyleSelection(panelView, bg, selectedStyle);
    }

    private void applyStyleSelection(View view, int bg, int selectedStyle) {
        if (view == null) return;
        Object tag = view.getTag();
        if (tag instanceof String && ((String) tag).startsWith("styleCard_")) {
            try {
                int position = Integer.parseInt(((String) tag).replace("styleCard_", ""));
                view.setBackground(roundedPanel(position == selectedStyle ? blend(accentColor, Color.BLACK, 0.28f) : blend(accentColor, Color.BLACK, 0.70f), accentColor, 18));
            } catch (Exception ignored) {
                view.setBackground(roundedPanel(bg, accentColor, 18));
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyStyleSelection(group.getChildAt(i), bg, selectedStyle);
            }
        }
    }

    private void styleInfoCards(int panel, int stroke) {
        int iconBg = blend(accentColor, Color.BLACK, 0.30f);
        styleCard(weatherCard, panel, stroke);
        styleCard(batteryCard, panel, stroke);
        styleCard(musicCard, panel, stroke);

        if (weatherCard != null && weatherCard.getChildCount() > 0) {
            View row = ((ViewGroup) weatherCard.getChildAt(0)).getChildAt(0);
            if (row instanceof TextView) row.setBackground(roundedPanel(iconBg, accentColor, 20));
        }
        if (musicCard != null && musicCard.getChildCount() > 0) {
            View row = ((ViewGroup) musicCard.getChildAt(0)).getChildAt(0);
            if (row instanceof TextView) row.setBackground(roundedPanel(iconBg, accentColor, 20));
        }
        batteryRing.setAccent(blend(accentColor, Color.parseColor("#39E37A"), 0.35f));
        styleMusicControls();
    }

    private void styleMusicControls() {
        int soft = blend(accentColor, Color.BLACK, 0.58f);
        int strong = blend(accentColor, Color.WHITE, 0.08f);
        if (musicPrevButton != null) {
            musicPrevButton.setBackground(roundedPanel(soft, blend(accentColor, Color.WHITE, 0.30f), 999));
            musicPrevButton.setTextColor(Color.WHITE);
        }
        if (musicNextButton != null) {
            musicNextButton.setBackground(roundedPanel(soft, blend(accentColor, Color.WHITE, 0.30f), 999));
            musicNextButton.setTextColor(Color.WHITE);
        }
        if (musicPlayPauseButton != null) {
            musicPlayPauseButton.setBackground(roundedPanel(strong, blend(accentColor, Color.WHITE, 0.45f), 999));
            musicPlayPauseButton.setTextColor(Color.WHITE);
        }
    }

    private void styleCard(View card, int bg, int stroke) {
        if (card != null) card.setBackground(roundedPanel(bg, stroke, 24));
    }

    private void updateLiveInfo() {
        dateLine.setText(new SimpleDateFormat("EEE, d MMM", Locale.UK).format(new Date()));
        statusLine.setText(FocusManager.statusText(this));
        updateBatteryCard(getBatteryText());
        updateWeatherCard(FocusManager.getWeatherSummary(this));
        updateMusicCard(FocusManager.getNowPlayingSummary(this));
        updateMusicButtonState();
        requestWeatherIfNeeded(false);
    }

    private String getBatteryText() {
        Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) return "Battery  •  0%";
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
        int percent = 0;
        if (level >= 0 && scale > 0) percent = Math.round(level * 100f / scale);
        return (charging ? "Charging" : "Battery") + "  •  " + percent + "%";
    }

    private void updateBatteryCard(String summary) {
        String[] parts = summary.split("\\s*•\\s*");
        String label = parts.length > 0 ? parts[0].trim() : "Battery";
        String percentText = parts.length > 1 ? parts[1].trim() : "0%";
        batteryPercent.setText(percentText);
        batteryLabel.setText(label);
        int percent = 0;
        try {
            percent = Integer.parseInt(percentText.replace("%", "").trim());
        } catch (Exception ignored) {}
        batteryRing.setPercent(percent);
    }

    private void updateWeatherCard(String summary) {
        String[] parts = summary.split("\\s*•\\s*");
        String city = parts.length > 0 ? parts[0].trim() : FocusManager.getWeatherCity(this);
        String temp = parts.length > 1 ? parts[1].trim() : "--°C";
        String condition = parts.length > 2 ? parts[2].trim() : "Weather unavailable";
        weatherTemp.setText(temp.replace("°C", "°"));
        weatherCondition.setText(condition);
        weatherCity.setText(city);
    }

    private void updateMusicCard(String summary) {
        String title = "No recent playback";
        String artist = "Enable notification access";
        String meta = "Music";
        String eyebrow = "NOW PLAYING";

        if (summary != null) {
            String[] lines = summary.split("\\n");
            String first = lines.length > 0 ? lines[0] : "Music";
            String second = lines.length > 1 ? lines[1] : "";
            String[] head = first.split("\\s*•\\s*");
            if (head.length >= 2) {
                meta = head[0].trim();
                title = head[1].trim();
            } else if (head.length == 1 && !head[0].trim().isEmpty()) {
                title = head[0].trim();
            }
            if (!second.trim().isEmpty()) {
                artist = second.trim();
            }
            if (summary.toLowerCase(Locale.UK).contains("enable notification access")) {
                title = "No playback connected";
                artist = "Enable notification access";
                meta = "Music";
            }
            if (summary.toLowerCase(Locale.UK).contains("no recent playback")) {
                title = "No recent playback";
                artist = "Start music to show it here";
            }
        }

        // updateLiveInfo() runs every second; avoid resetting the marquee when
        // the media metadata has not actually changed.
        if (!TextUtils.equals(musicEyebrow.getText(), eyebrow)) musicEyebrow.setText(eyebrow);
        if (!TextUtils.equals(musicTitle.getText(), title)) musicTitle.setText(title);
        if (!TextUtils.equals(musicArtist.getText(), artist)) musicArtist.setText(artist);
        if (!TextUtils.equals(musicMeta.getText(), meta)) musicMeta.setText(meta);
    }

    private void updateMusicButtonState() {
        if (musicPlayPauseButton == null) return;
        try {
            MediaSessionManager manager = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
            if (manager == null) return;
            List<MediaController> controllers = manager.getActiveSessions(new ComponentName(this, FocusMusicNotificationListener.class));
            if (controllers == null || controllers.isEmpty()) {
                musicPlayPauseButton.setText("▶");
                return;
            }
            for (MediaController controller : controllers) {
                PlaybackState state = controller.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                    musicPlayPauseButton.setText("Ⅱ");
                    return;
                }
            }
            musicPlayPauseButton.setText("▶");
        } catch (Exception ignored) {
            musicPlayPauseButton.setText("▶");
        }
    }

    private void sendMediaCommand(String command) {
        try {
            MediaSessionManager manager = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
            if (manager == null) {
                updateMusicCard("Music  •  enable notification access");
                return;
            }
            List<MediaController> controllers = manager.getActiveSessions(new ComponentName(this, FocusMusicNotificationListener.class));
            if (controllers == null || controllers.isEmpty()) {
                updateMusicCard("Music  •  start music first\nThen try the controls again");
                return;
            }

            MediaController chosen = controllers.get(0);
            for (MediaController controller : controllers) {
                PlaybackState state = controller.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                    chosen = controller;
                    break;
                }
            }

            MediaController.TransportControls controls = chosen.getTransportControls();
            if ("next".equals(command)) {
                controls.skipToNext();
            } else if ("previous".equals(command)) {
                controls.skipToPrevious();
            } else {
                PlaybackState state = chosen.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                    controls.pause();
                    if (musicPlayPauseButton != null) musicPlayPauseButton.setText("▶");
                } else {
                    controls.play();
                    if (musicPlayPauseButton != null) musicPlayPauseButton.setText("Ⅱ");
                }
            }
        } catch (SecurityException securityException) {
            updateMusicCard("Music  •  enable notification access\nSettings → Notification access");
        } catch (Exception ignored) {
            updateMusicCard("Music  •  controls unavailable\nTry Spotify or Samsung Music");
        }
    }

    private void requestWeatherIfNeeded(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastWeatherRequest < 30_000L) return;
        if (!force && !WeatherHelper.isStale(this)) return;
        lastWeatherRequest = now;
        WeatherHelper.refresh(this, success -> runOnUiThread(() -> updateWeatherCard(FocusManager.getWeatherSummary(this))));
    }

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private GradientDrawable roundedPanel(int color, int stroke, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setStroke(UiKit.dp(this, 1), stroke);
        d.setCornerRadius(UiKit.dp(this, radiusDp));
        return d;
    }

    private GradientDrawable appBackground(int top, int bottom) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{top, bottom});
        d.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        return d;
    }

    private int blend(int a, int b, float amountOfB) {
        float inverse = 1f - amountOfB;
        return Color.rgb(
                Math.round(Color.red(a) * inverse + Color.red(b) * amountOfB),
                Math.round(Color.green(a) * inverse + Color.green(b) * amountOfB),
                Math.round(Color.blue(a) * inverse + Color.blue(b) * amountOfB)
        );
    }

    private String hex(int color) {
        return String.format(Locale.UK, "#%06X", 0xFFFFFF & color);
    }

    public static class GradientTextClock extends TextClock {
        private int startColor = Color.parseColor("#60A5FA");
        private int endColor = Color.parseColor("#D946EF");

        public GradientTextClock(android.content.Context context) {
            super(context);
        }

        public void setGradient(int startColor, int endColor) {
            this.startColor = startColor;
            this.endColor = endColor;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            getPaint().setShader(new LinearGradient(0, 0, getWidth(), getHeight(), startColor, endColor, Shader.TileMode.CLAMP));
            super.onDraw(canvas);
        }
    }

    public static class RingView extends View {
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int percent = 0;
        private int accent = Color.parseColor("#39E37A");

        public RingView(android.content.Context context) {
            super(context);
            trackPaint.setStyle(Paint.Style.STROKE);
            trackPaint.setStrokeWidth(12f);
            trackPaint.setColor(Color.parseColor("#20314E"));
            progressPaint.setStyle(Paint.Style.STROKE);
            progressPaint.setStrokeCap(Paint.Cap.ROUND);
            progressPaint.setStrokeWidth(12f);
            progressPaint.setColor(accent);
        }

        public void setPercent(int percent) {
            this.percent = Math.max(0, Math.min(100, percent));
            invalidate();
        }

        public void setAccent(int accent) {
            this.accent = accent;
            progressPaint.setColor(accent);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float size = Math.min(getWidth(), getHeight());
            float radius = size / 2f - 10f;
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            canvas.drawCircle(cx, cy, radius, trackPaint);
            canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius, -90f, 360f * percent / 100f, false, progressPaint);

            Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            centerPaint.setColor(Color.WHITE);
            centerPaint.setTextAlign(Paint.Align.CENTER);
            centerPaint.setTypeface(Typeface.DEFAULT_BOLD);
            centerPaint.setTextSize(size * 0.26f);
            Paint.FontMetrics fm = centerPaint.getFontMetrics();
            float y = cy - (fm.ascent + fm.descent) / 2f;
            canvas.drawText("⚡", cx, y, centerPaint);
        }
    }

    public static class ColorWheelView extends View {
        public interface OnColorChangedListener {
            void onColorChanged(int color);
        }

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Bitmap bitmap;
        private int selectedColor = Color.CYAN;
        private float selectedX = -1;
        private float selectedY = -1;
        private OnColorChangedListener listener;

        public ColorWheelView(android.content.Context context) {
            super(context);
            markerPaint.setStyle(Paint.Style.STROKE);
            markerPaint.setStrokeWidth(4f);
            markerPaint.setColor(Color.WHITE);
        }

        public void setOnColorChangedListener(OnColorChangedListener listener) {
            this.listener = listener;
        }

        public void setSelectedColor(int color) {
            selectedColor = color;
            if (getWidth() <= 0 || getHeight() <= 0) return;
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            float radius = Math.min(getWidth(), getHeight()) / 2f * hsv[1];
            float angle = (float) Math.toRadians(hsv[0]);
            selectedX = getWidth() / 2f + (float) Math.cos(angle) * radius;
            selectedY = getHeight() / 2f + (float) Math.sin(angle) * radius;
            invalidate();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            createBitmap(w, h);
            setSelectedColor(selectedColor);
        }

        private void createBitmap(int w, int h) {
            if (w <= 0 || h <= 0) return;
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            float cx = w / 2f;
            float cy = h / 2f;
            float radius = Math.min(w, h) / 2f - 2f;
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    float dx = x - cx;
                    float dy = y - cy;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    int index = y * w + x;
                    if (dist <= radius) {
                        float hue = (float) Math.toDegrees(Math.atan2(dy, dx));
                        if (hue < 0) hue += 360f;
                        float sat = Math.min(1f, dist / radius);
                        pixels[index] = Color.HSVToColor(new float[]{hue, sat, 1f});
                    } else {
                        pixels[index] = Color.TRANSPARENT;
                    }
                }
            }
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (bitmap != null) canvas.drawBitmap(bitmap, 0, 0, paint);
            if (selectedX >= 0 && selectedY >= 0) {
                markerPaint.setColor(Color.BLACK);
                canvas.drawCircle(selectedX, selectedY, 16f, markerPaint);
                markerPaint.setColor(Color.WHITE);
                canvas.drawCircle(selectedX, selectedY, 12f, markerPaint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                pickColor(event.getX(), event.getY());
                return true;
            }
            return true;
        }

        private void pickColor(float x, float y) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) / 2f - 2f;
            float dx = x - cx;
            float dy = y - cy;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > radius && dist > 0) {
                dx = dx / dist * radius;
                dy = dy / dist * radius;
                dist = radius;
            }
            float hue = (float) Math.toDegrees(Math.atan2(dy, dx));
            if (hue < 0) hue += 360f;
            float sat = Math.min(1f, dist / radius);
            selectedColor = Color.HSVToColor(new float[]{hue, sat, 1f});
            selectedX = cx + dx;
            selectedY = cy + dy;
            invalidate();
            if (listener != null) listener.onColorChanged(selectedColor);
        }
    }
}
