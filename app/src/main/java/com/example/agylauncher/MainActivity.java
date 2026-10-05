package com.example.agylauncher;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity implements ChatStore.Listener {

    private static final String TERMUX_PKG = "com.termux";
    private static final String PERM = "com.termux.permission.RUN_COMMAND";
    private static final String BASH = "/data/data/com.termux/files/usr/bin/bash";
    private static final String HOME = "/data/data/com.termux/files/home";
    private static final String INSTALL_URL =
            "https://raw.githubusercontent.com/wallentx/antigravity-cli-termux/dev/install.sh";
    private static final int REQ_VOICE = 77;

    private static final String SETUP_CMD =
            "mkdir -p ~/.termux && grep -q '^allow-external-apps' ~/.termux/termux.properties 2>/dev/null"
            + " || echo 'allow-external-apps=true' >> ~/.termux/termux.properties";

    // Script arguments: $1 = folder, $2 = prompt, $3 = "1" to continue, $4 = "1" auto-approve, $5 = model
    private static final String PATH_FIX =
            "[ -f ~/.bashrc ] && . ~/.bashrc; "
            + "export PATH=\"$HOME/.local/bin:$HOME/.agy/bin:$HOME/.antigravity/bin:$PATH\"; ";

    // ----- background (chat) scripts
    private static final String BG_GOTO =
            "mkdir -p \"$1\" && cd \"$1\" || { echo \"Cannot open folder: $1\"; exit 3; }; ";

    private static final String BG_NEED =
            "if ! command -v agy >/dev/null 2>&1; then echo 'NH is not installed. Use the menu > Setup.'; exit 2; fi; ";

    private static final String S_ASK =
            PATH_FIX + BG_GOTO + BG_NEED
            + "command -v script >/dev/null 2>&1 || pkg install -y util-linux >/dev/null 2>&1; "
            + "export AGY_PROMPT=\"$2\"; export AGY_MODEL=\"$5\"; "
            + "if [ \"$3\" = 1 ]; then export F=--continue; else export F=; fi; "
            + "if [ \"$4\" = 1 ]; then export F=\"$F --dangerously-skip-permissions\"; fi; "
            + "if command -v script >/dev/null 2>&1; then "
            + "timeout 900 script -qec 'agy -p \"$AGY_PROMPT\" ${AGY_MODEL:+--model \"$AGY_MODEL\"} $F' /dev/null; "
            + "else timeout 900 agy -p \"$AGY_PROMPT\" ${AGY_MODEL:+--model \"$AGY_MODEL\"} $F; fi";

    private static final String S_MODELS_BG =
            PATH_FIX + BG_NEED
            + "command -v script >/dev/null 2>&1 || pkg install -y util-linux >/dev/null 2>&1; "
            + "if command -v script >/dev/null 2>&1; then timeout 60 script -qec 'agy models' /dev/null; "
            + "else timeout 60 agy models; fi";

    // ----- foreground (Termux window) scripts
    private static final String FG_GOTO =
            "mkdir -p \"$1\" && cd \"$1\" || { echo \"Cannot open folder: $1\"; exec bash -l; }; ";

    private static final String FG_NEED =
            "if ! command -v agy >/dev/null 2>&1; then "
            + "echo 'NH is not installed. Use the menu > Setup.'; exec bash -l; fi; ";

    private static final String S_INSTALL =
            "pkg update -y; pkg install -y glibc-repo; pkg install -y glibc curl; "
            + "curl -fsSL " + INSTALL_URL + " | bash; "
            + "echo; echo '== Install finished. You can go back to the app. =='; exec bash -l";

    private static final String S_STORAGE =
            "termux-setup-storage; sleep 2; echo; echo '== Storage setup done =='; exec bash -l";

    private static final String S_INTERACTIVE =
            PATH_FIX + FG_GOTO + FG_NEED + "agy -c || agy; exec bash -l";

    private static final String S_ZIP =
            "pkg install -y zip >/dev/null 2>&1; "
            + "if [ ! -d \"$HOME/storage/downloads\" ]; then echo 'Run Setup > Storage permission first.'; exec bash -l; fi; "
            + "if [ ! -d \"$1\" ]; then echo \"Folder not found: $1\"; exec bash -l; fi; "
            + "d=\"$(dirname \"$1\")\"; b=\"$(basename \"$1\")\"; cd \"$d\" && "
            + "zip -r \"$b.zip\" \"$b\" -x \"*/node_modules/*\" \"*/build/*\" \"*/.gradle/*\" && "
            + "cp \"$b.zip\" \"$HOME/storage/downloads/\" && echo \"Saved to Download/$b.zip\"; exec bash -l";

    // ----- install check and install steps (run inside Termux)
    private static final String S_CHECK =
            "[ -f ~/.bashrc ] && . ~/.bashrc; "
            + "export PATH=\"$HOME/.local/bin:$HOME/.agy/bin:$HOME/.antigravity/bin:$PATH\"; "
            + "g=0; [ -e \"$PREFIX/glibc/lib/ld-linux-aarch64.so.1\" ] && g=1; "
            + "c=0; command -v curl >/dev/null 2>&1 && c=1; "
            + "a=0; command -v agy >/dev/null 2>&1 && a=1; "
            + "st=0; [ -d \"$HOME/storage/downloads\" ] && st=1; "
            + "sc=0; command -v script >/dev/null 2>&1 && sc=1; "
            + "z=0; command -v zip >/dev/null 2>&1 && z=1; "
            + "echo \"glibc=$g\"; echo \"curl=$c\"; echo \"agy=$a\"; echo \"storage=$st\"; "
            + "echo \"script=$sc\"; echo \"zip=$z\"; echo \"check=done\"";

    private static final String S_STEP_UPDATE =
            "pkg update -y; DEBIAN_FRONTEND=noninteractive apt-get -y -o Dpkg::Options::=--force-confold upgrade; "
            + "[ -d \"$HOME/storage/downloads\" ] || termux-setup-storage; sleep 2; "
            + "echo; echo '== Done: update and storage. Go back to the app and tap Recheck. =='; exec bash -l";

    private static final String S_STEP_GLIBC =
            "pkg install -y glibc-repo; pkg install -y glibc; "
            + "echo; echo '== Done: glibc. Go back to the app and tap Recheck. =='; exec bash -l";

    private static final String S_STEP_TOOLS =
            "pkg install -y curl util-linux zip nano; "
            + "echo; echo '== Done: tools. Go back to the app and tap Recheck. =='; exec bash -l";

    private static final String S_STEP_AGY =
            PATH_FIX + "curl -fsSL " + INSTALL_URL + " | bash; "
            + "echo; echo '== Done. If you saw no [ERR] line, go back to the app and tap Recheck. =='; exec bash -l";

    private static final String S_FULL_INSTALL =
            "echo '== 1/5 Updating server packages =='; pkg update -y; "
            + "DEBIAN_FRONTEND=noninteractive apt-get -y -o Dpkg::Options::=--force-confold upgrade; "
            + "echo; echo '== 2/5 Storage access (tap Allow if Android asks) =='; "
            + "[ -d \"$HOME/storage/downloads\" ] || termux-setup-storage; sleep 3; "
            + "echo; echo '== 3/5 Installing glibc =='; pkg install -y glibc-repo; pkg install -y glibc; "
            + "echo; echo '== 4/5 Installing tools =='; pkg install -y curl util-linux zip nano; "
            + "echo; echo '== 5/5 Installing Antigravity CLI =='; curl -fsSL " + INSTALL_URL + " | bash; "
            + PATH_FIX
            + "echo; if command -v agy >/dev/null 2>&1; then "
            + "echo '== DONE. Type agy and press Enter to log in. =='; "
            + "else echo '== NH was not found. Scroll up and read the first [ERR] line. =='; fi; exec bash -l";

    private static final String FULL_COMMANDS_TEXT =
            "termux-setup-storage\n"
            + "pkg update -y && pkg install -y glibc-repo && pkg install -y glibc curl util-linux zip nano "
            + "&& curl -fsSL " + INSTALL_URL + " | bash";

    // ------------------------------------------------------------ state

    private SharedPreferences prefs;
    private Palette pal;

    private FrameLayout rootFrame;
    private LinearLayout chatList;
    private ScrollView chatScroll;
    private EditText input;
    private TextView folderLabel;
    private TextView modelPill;
    private FrameLayout sendBtn;
    private IconView sendIcon;
    private GradientDrawable sendBg;

    private View scrim;
    private LinearLayout drawer;
    private LinearLayout drawerList;
    private int drawerW;
    private boolean drawerOpen = false;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private int renderedCount = -1;
    private String renderedChatId = null;
    private boolean wasBusyHere = false;
    private boolean tickerOn = false;
    private boolean pendingModelPicker = false;

    private TextView busyStatus;
    private TextView busyTime;
    private TextView busyNote;
    private ShimmerBar busyBar;

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private String pendingSpeech = null;
    private final Map<String, Integer> feedback = new HashMap<>();

    private boolean freshLaunch = false;
    private FrameLayout settingsOverlay;
    private LinearLayout settingsContent;
    private ScrollView settingsScroll;
    private SpotlightLayout spotlight;
    private List<View> welcomeBlur = new ArrayList<>();
    private FrameLayout guideOverlay;
    private LinearLayout guideContent;
    private ScrollView guideScroll;
    private TextView welcomeChip;
    private TextView guideChip;
    private boolean guideFirst = false;
    private FrameLayout welcomeOverlay;
    private LinearLayout welcomeChips;

    private Dialog sheetDialog;
    private LinearLayout sheetRoot;
    private LinearLayout sheetList;

    private static final class Palette {
        final boolean dark;
        final int bg, card, text, sub, subText, border, userBubble, grey, strong, onStrong,
                accent, codeBg, codeFg, green, red;

        static final String[] ACC_LIGHT = {"#C6613F", "#2F6FED", "#1F9D6B", "#7A4DE0", "#D6457A"};
        static final String[] ACC_DARK = {"#D97757", "#5B93FF", "#35C58B", "#9C7BFF", "#F06A9C"};
        static final String[] ACC_NAMES = {"Terracotta", "Ocean", "Emerald", "Violet", "Rose"};

        Palette(boolean d, int accentIdx) {
            dark = d;
            int ai = (accentIdx < 0 || accentIdx >= ACC_LIGHT.length) ? 0 : accentIdx;
            bg = Color.parseColor(d ? "#262624" : "#FAF9F5");
            card = Color.parseColor(d ? "#30302E" : "#FFFFFF");
            text = Color.parseColor(d ? "#F2F1EC" : "#1F1E1D");
            sub = Color.parseColor(d ? "#8D8B82" : "#A09E96");
            subText = Color.parseColor(d ? "#A7A59B" : "#6F6D66");
            border = Color.parseColor(d ? "#4A4944" : "#E6E3D9");
            userBubble = Color.parseColor(d ? "#3A3935" : "#F0EEE6");
            grey = Color.parseColor(d ? "#3A3935" : "#EFEDE5");
            strong = Color.parseColor(d ? "#F2F1EC" : "#141413");
            onStrong = Color.parseColor(d ? "#1F1E1D" : "#FFFFFF");
            accent = Color.parseColor(d ? ACC_DARK[ai] : ACC_LIGHT[ai]);
            codeBg = Color.parseColor(d ? "#353B47" : "#ECF0F7");
            codeFg = Color.parseColor(d ? "#8AB4F8" : "#2B5C9E");
            green = Color.parseColor(d ? "#3DBB6A" : "#2E9E57");
            red = Color.parseColor(d ? "#E5534B" : "#D64545");
        }
    }

    private static final class MaxHeightScrollView extends ScrollView {
        private final int maxH;

        MaxHeightScrollView(Context c, int maxH) {
            super(c);
            this.maxH = maxH;
        }

        @Override
        protected void onMeasure(int w, int h) {
            super.onMeasure(w, MeasureSpec.makeMeasureSpec(maxH, MeasureSpec.AT_MOST));
        }
    }

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!ChatStore.isBusyHere() || busyStatus == null) {
                tickerOn = false;
                return;
            }
            updateBusyCard();
            ui.postDelayed(this, 250);
        }
    };

    // ------------------------------------------------------------ lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("agy", MODE_PRIVATE);
        String mode = prefs.getString("theme", "system");
        boolean sysDark = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        boolean dark = "dark".equals(mode) || ("system".equals(mode) && sysDark);
        setTheme(dark ? R.style.ThemeDark : R.style.ThemeLight);
        super.onCreate(savedInstanceState);
        freshLaunch = savedInstanceState == null;
        ChatStore.init(this);
        pal = new Palette(dark, prefs.getInt("accent", 0));
        setupWindow();
        buildUi();
        ChatStore.setListener(this);
        render();
        if (prefs.getBoolean("reopenSettings", false)) {
            prefs.edit().putBoolean("reopenSettings", false).apply();
            showSettings(false);
        }
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacks(ticker);
        ChatStore.clearListener(this);
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        dismissSheet();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        ui.removeCallbacks(ticker);
        tickerOn = false;
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ChatStore.setListener(this);
        render();
        if (welcomeOverlay != null || guideOverlay != null) checkInstall();
    }

    @Override
    public void onChanged() {
        render();
        updateInstallChips();
        if (guideOverlay != null) renderGuide();
        if (spotlight != null && installState() == 1) clearSpotlight();
        if (drawerOpen) rebuildDrawerList();
        if (pendingModelPicker && !ChatStore.modelsLoading()) {
            pendingModelPicker = false;
            showModelSheet();
        }
    }

    @Override
    public void onBackPressed() {
        if (guideOverlay != null) {
            closeInstallGuide();
            return;
        }
        if (settingsOverlay != null) {
            closeSettings();
            return;
        }
        if (spotlight != null) {
            clearSpotlight();
            return;
        }
        if (welcomeOverlay != null) {
            dismissWelcome(false);
            return;
        }
        if (drawerOpen) {
            closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    // ------------------------------------------------------------ helpers

    @SuppressWarnings("deprecation")
    private void setupWindow() {
        getWindow().setStatusBarColor(pal.bg);
        getWindow().setNavigationBarColor(pal.bg);
        int f = getWindow().getDecorView().getSystemUiVisibility();
        if (!pal.dark) {
            f |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) f |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(f);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable shape(int fill, int stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        if (stroke != 0) g.setStroke(Math.max(1, dp(1)), stroke);
        return g;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    private void ripple(View v, boolean borderless) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(borderless
                ? android.R.attr.selectableItemBackgroundBorderless
                : android.R.attr.selectableItemBackground, tv, true);
        v.setBackgroundResource(tv.resourceId);
    }

    private IconView iconBtn(int type, int iconDp, int sizeDp, View.OnClickListener l) {
        IconView v = new IconView(this, type);
        v.setColors(pal.text, pal.bg);
        v.setIconDp(iconDp);
        ripple(v, true);
        v.setOnClickListener(l);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return v;
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_LONG).show();
    }

    private void copyText(String label, String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, text));
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private String folder() {
        return prefs.getString("folder", "~/projects");
    }

    private String modelLabel() {
        String m = prefs.getString("model", "");
        return m.isEmpty() ? "Default model" : m;
    }

    // ------------------------------------------------------------ UI build

    private void buildUi() {
        rootFrame = new FrameLayout(this);
        rootFrame.setBackgroundColor(pal.bg);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);

        // ----- top bar
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(6), dp(8), dp(6));

        bar.addView(iconBtn(IconView.MENU, 26, 48, v -> openDrawer()));

        folderLabel = new TextView(this);
        folderLabel.setTextSize(13);
        folderLabel.setGravity(Gravity.CENTER);
        folderLabel.setSingleLine(true);
        folderLabel.setEllipsize(TextUtils.TruncateAt.END);
        folderLabel.setOnClickListener(v -> changeFolder());
        bar.addView(folderLabel, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        bar.addView(iconBtn(IconView.SETTINGS, 26, 48, v -> showSettings(true)));
        bar.addView(iconBtn(IconView.NEWCHAT, 28, 48, v -> ChatStore.newChat()));
        bar.addView(iconBtn(IconView.DOTS, 26, 48, v -> showCommandsSheet()));
        main.addView(bar);

        // ----- messages
        chatScroll = new ScrollView(this);
        chatScroll.setFillViewport(true);
        chatScroll.setVerticalScrollBarEnabled(false);
        chatList = new LinearLayout(this);
        chatList.setOrientation(LinearLayout.VERTICAL);
        chatList.setPadding(dp(18), dp(6), dp(18), dp(16));
        chatScroll.addView(chatList);
        main.addView(chatScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        // ----- input card
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(12), dp(4), dp(12), dp(12));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(shape(pal.card, pal.border, 28));
        card.setPadding(dp(20), dp(12), dp(12), dp(12));
        card.setElevation(dp(2));

        input = new EditText(this);
        input.setBackground(null);
        input.setHint("Message NH");
        input.setHintTextColor(pal.sub);
        input.setTextColor(pal.text);
        input.setTextSize(18);
        input.setMinHeight(dp(52));
        input.setMaxLines(6);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setPadding(0, dp(8), dp(8), dp(8));
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void afterTextChanged(Editable e) {
                updateSendState();
                input.post(() -> input.bringPointIntoView(input.getSelectionEnd()));
            }
        });
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) scrollChatToEnd();
        });
        input.setOnClickListener(v -> scrollChatToEnd());
        chatScroll.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, orr, ob) -> {
            int newH = b - t;
            int oldH = ob - ot;
            if (welcomeChips != null) {
                welcomeChips.setVisibility(newH < dp(440) ? View.GONE : View.VISIBLE);
            }
            if (oldH > 0 && newH != oldH && input != null && input.hasFocus()) {
                scrollChatToEnd();
            }
        });
        card.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout plus = roundButton(pal.grey);
        IconView plusIcon = new IconView(this, IconView.PLUS);
        plusIcon.setColors(pal.text, pal.grey);
        plusIcon.setIconDp(24);
        plus.addView(plusIcon, new FrameLayout.LayoutParams(dp(48), dp(48)));
        plus.setOnClickListener(v -> showQuickSheet());
        row.addView(plus, new LinearLayout.LayoutParams(dp(48), dp(48)));

        modelPill = new TextView(this);
        modelPill.setTextSize(16);
        modelPill.setTextColor(pal.text);
        modelPill.setSingleLine(true);
        modelPill.setEllipsize(TextUtils.TruncateAt.END);
        modelPill.setGravity(Gravity.CENTER);
        modelPill.setPadding(dp(16), 0, dp(16), 0);
        modelPill.setBackground(shape(pal.grey, 0, 24));
        modelPill.setOnClickListener(v -> openModelPicker());
        // The pill lives in a flexible holder, so it can never push the mic/send buttons off screen.
        FrameLayout pillHolder = new FrameLayout(this);
        pillHolder.addView(modelPill, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, dp(48), Gravity.START | Gravity.CENTER_VERTICAL));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        hp.setMargins(dp(8), 0, dp(8), 0);
        row.addView(pillHolder, hp);

        FrameLayout mic = roundButton(pal.grey);
        IconView micIcon = new IconView(this, IconView.MIC);
        micIcon.setColors(pal.text, pal.grey);
        micIcon.setIconDp(24);
        mic.addView(micIcon, new FrameLayout.LayoutParams(dp(48), dp(48)));
        mic.setOnClickListener(v -> startVoice());
        row.addView(mic, new LinearLayout.LayoutParams(dp(48), dp(48)));

        sendBg = circle(pal.strong);
        sendBtn = new FrameLayout(this);
        sendBtn.setBackground(sendBg);
        sendIcon = new IconView(this, IconView.WAVE);
        sendIcon.setColors(pal.onStrong, pal.strong);
        sendIcon.setIconDp(24);
        sendBtn.addView(sendIcon, new FrameLayout.LayoutParams(dp(48), dp(48)));
        sendBtn.setOnClickListener(v -> {
            if (input.getText().toString().trim().isEmpty()) startVoice();
            else send();
        });
        sendBtn.setOnTouchListener((v, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(90).start();
            } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            }
            return false;
        });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(48), dp(48));
        sp.setMargins(dp(8), 0, 0, 0);
        row.addView(sendBtn, sp);

        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, dp(6), 0, 0);
        card.addView(row, rp);
        wrap.addView(card);
        main.addView(wrap);

        rootFrame.addView(main, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        buildDrawer();
        setContentView(rootFrame);
        if (freshLaunch) showWelcome();
    }

    private FrameLayout roundButton(int color) {
        FrameLayout f = new FrameLayout(this);
        f.setBackground(circle(color));
        return f;
    }

    // ------------------------------------------------------------ drawer (all chats)

    private void buildDrawer() {
        drawerW = (int) (getResources().getDisplayMetrics().widthPixels * 0.84f);

        scrim = new View(this);
        scrim.setBackgroundColor(0x66000000);
        scrim.setAlpha(0f);
        scrim.setVisibility(View.GONE);
        scrim.setOnClickListener(v -> closeDrawer());
        rootFrame.addView(scrim, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setBackgroundColor(pal.bg);
        drawer.setClickable(true);
        drawer.setElevation(dp(16));
        drawer.setVisibility(View.GONE);
        drawer.setTranslationX(-drawerW);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(20), dp(14), dp(8), dp(8));
        TextView title = new TextView(this);
        title.setText("Chats");
        title.setTextSize(24);
        title.setTypeface(Typeface.SERIF, Typeface.BOLD);
        title.setTextColor(pal.text);
        head.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(iconBtn(IconView.NEWCHAT, 28, 48, v -> {
            ChatStore.newChat();
            closeDrawer();
        }));
        drawer.addView(head);

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        drawerList = new LinearLayout(this);
        drawerList.setOrientation(LinearLayout.VERTICAL);
        drawerList.setPadding(dp(8), dp(4), dp(8), dp(8));
        sv.addView(drawerList);
        drawer.addView(sv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView cmds = drawerFooter("All commands");
        cmds.setOnClickListener(v -> {
            closeDrawer();
            ui.postDelayed(this::showCommandsSheet, 220);
        });
        drawer.addView(cmds);
        TextView clear = drawerFooter("Clear all chats");
        clear.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Clear all chats?")
                .setMessage("This removes the chat list from this app. NH's own history is not touched.")
                .setPositiveButton("Clear", (d, w) -> {
                    ChatStore.clearAll();
                    closeDrawer();
                })
                .setNegativeButton("Cancel", null)
                .show());
        drawer.addView(clear);

        rootFrame.addView(drawer, new FrameLayout.LayoutParams(
                drawerW, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private TextView drawerFooter(String label) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(15);
        t.setTextColor(pal.subText);
        t.setPadding(dp(24), dp(14), dp(24), dp(14));
        ripple(t, false);
        return t;
    }

    private void rebuildDrawerList() {
        drawerList.removeAllViews();
        List<ChatStore.Info> items = ChatStore.chats();
        String cur = ChatStore.currentId();
        if (items.isEmpty()) {
            TextView e = new TextView(this);
            e.setText("No chats yet.\nYour conversations will show up here.");
            e.setTextColor(pal.sub);
            e.setTextSize(14);
            e.setPadding(dp(16), dp(24), dp(16), dp(16));
            drawerList.addView(e);
            return;
        }
        long now = System.currentTimeMillis();
        for (final ChatStore.Info c : items) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(14), dp(10), dp(14), dp(10));
            if (c.id.equals(cur)) row.setBackground(shape(pal.userBubble, 0, 12));
            else ripple(row, false);

            TextView t = new TextView(this);
            t.setText(c.title);
            t.setTextSize(16);
            t.setTextColor(pal.text);
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.END);
            row.addView(t);

            TextView s = new TextView(this);
            String when = DateUtils.getRelativeTimeSpanString(
                    c.ts, now, DateUtils.MINUTE_IN_MILLIS).toString();
            s.setText(c.folder.isEmpty() ? when : when + "  \u00B7  " + c.folder);
            s.setTextSize(12);
            s.setTextColor(pal.subText);
            s.setSingleLine(true);
            s.setEllipsize(TextUtils.TruncateAt.END);
            row.addView(s);

            row.setOnClickListener(v -> {
                ChatStore.openChat(c.id);
                if (!c.folder.isEmpty()) prefs.edit().putString("folder", c.folder).apply();
                closeDrawer();
            });
            row.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Delete this chat?")
                        .setMessage(c.title)
                        .setPositiveButton("Delete", (d, w) -> ChatStore.deleteChat(c.id))
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(2), 0, dp(2));
            drawerList.addView(row, lp);
        }
    }

    private void openDrawer() {
        hideKeyboard();
        rebuildDrawerList();
        drawerOpen = true;
        scrim.setVisibility(View.VISIBLE);
        drawer.setVisibility(View.VISIBLE);
        scrim.animate().alpha(1f).setDuration(220).start();
        drawer.animate().translationX(0f).setDuration(240)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    private void closeDrawer() {
        if (!drawerOpen) return;
        drawerOpen = false;
        scrim.animate().alpha(0f).setDuration(200).withEndAction(() -> scrim.setVisibility(View.GONE)).start();
        drawer.animate().translationX(-drawerW).setDuration(220)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> drawer.setVisibility(View.GONE)).start();
    }

    // ------------------------------------------------------------ render

    private void updateSendState() {
        boolean has = !input.getText().toString().trim().isEmpty();
        boolean busy = ChatStore.isBusy();
        if (has) {
            sendBg.setColor(pal.accent);
            sendIcon.setType(IconView.UP);
            sendIcon.setColors(Color.WHITE, pal.accent);
        } else {
            sendBg.setColor(pal.strong);
            sendIcon.setType(IconView.WAVE);
            sendIcon.setColors(pal.onStrong, pal.strong);
        }
        sendBtn.setAlpha(busy ? 0.5f : 1f);
    }

    private void render() {
        List<ChatStore.Msg> msgs = ChatStore.snapshot();
        String curId = ChatStore.currentId();
        boolean busyHere = ChatStore.isBusyHere();

        boolean yolo = prefs.getBoolean("yolo", false);
        folderLabel.setText((yolo ? "\u25CF " : "") + folder());
        folderLabel.setTextColor(yolo ? pal.accent : pal.subText);
        modelPill.setText(modelLabel());

        chatList.removeAllViews();
        welcomeChips = null;
        busyStatus = null;
        busyTime = null;
        busyNote = null;
        busyBar = null;

        boolean chatChanged = !(curId == null ? renderedChatId == null : curId.equals(renderedChatId));
        if (chatChanged) {
            renderedCount = -1;
            wasBusyHere = busyHere;
            renderedChatId = curId;
        }

        if (msgs.isEmpty() && !busyHere) addWelcome();

        int animFrom;
        if (renderedCount < 0 || renderedCount > msgs.size()) animFrom = msgs.size();
        else animFrom = renderedCount;

        View newestAssistant = null;
        for (int i = 0; i < msgs.size(); i++) {
            ChatStore.Msg m = msgs.get(i);
            View v;
            if (m.user) {
                v = userBubble(m.text);
            } else {
                boolean last = i == msgs.size() - 1;
                v = assistantBlock(m.text, (curId == null ? "" : curId) + ":" + i, last && !busyHere);
            }
            if (i >= animFrom) {
                animateIn(v);
                if (!m.user) newestAssistant = v;
            }
        }
        if (busyHere) {
            View c = busyCard();
            if (!wasBusyHere) animateIn(c);
        } else if (!msgs.isEmpty()) {
            TextView foot = new TextView(this);
            foot.setText("NH is AI and can make mistakes.");
            foot.setTextSize(15);
            foot.setTextColor(pal.sub);
            foot.setPadding(dp(2), dp(26), dp(2), dp(10));
            chatList.addView(foot);
        }

        renderedCount = msgs.size();
        wasBusyHere = busyHere;
        updateSendState();

        final View target = newestAssistant;
        chatScroll.post(() -> {
            if (target != null) {
                chatScroll.smoothScrollTo(0, Math.max(0, target.getTop() - dp(8)));
            } else {
                chatScroll.smoothScrollTo(0, chatList.getHeight());
            }
        });

        if (busyHere && !tickerOn) {
            tickerOn = true;
            ui.post(ticker);
        }
    }

    private void scrollChatToEnd() {
        chatScroll.post(() -> chatScroll.scrollTo(0, Math.max(0, chatList.getHeight())));
    }

    private void animateIn(View v) {
        v.setAlpha(0f);
        v.setTranslationY(dp(14));
        v.animate().alpha(1f).translationY(0f).setDuration(280)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    private void addWelcome() {
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setGravity(Gravity.CENTER_HORIZONTAL);
        w.setPadding(0, dp(48), 0, dp(8));

        AntigravityView spark = new AntigravityView(this);
        w.addView(spark, new LinearLayout.LayoutParams(dp(132), dp(88)));

        TextView hi = new TextView(this);
        hi.setText("How can I help you\ntoday?");
        hi.setTextSize(28);
        hi.setTypeface(Typeface.SERIF);
        hi.setTextColor(pal.text);
        hi.setGravity(Gravity.CENTER);
        hi.setPadding(0, dp(14), 0, dp(6));
        w.addView(hi);

        TextView sub = new TextView(this);
        sub.setText("Working in " + folder());
        sub.setTextSize(13);
        sub.setTextColor(pal.subText);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 0, 0, dp(18));
        w.addView(sub);

        String[] chips = {
                "Create a new Android project",
                "Explain the files in this folder",
                "Find and fix errors in my code"
        };
        LinearLayout chipsBox = new LinearLayout(this);
        chipsBox.setOrientation(LinearLayout.VERTICAL);
        chipsBox.setGravity(Gravity.CENTER_HORIZONTAL);
        for (final String c : chips) {
            TextView chip = new TextView(this);
            chip.setText(c);
            chip.setTextSize(14);
            chip.setTextColor(pal.text);
            chip.setPadding(dp(16), dp(10), dp(16), dp(10));
            chip.setBackground(shape(pal.card, pal.border, 20));
            chip.setOnClickListener(v -> {
                input.setText(c);
                input.setSelection(c.length());
                input.requestFocus();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            lp.setMargins(0, dp(5), 0, dp(5));
            chipsBox.addView(chip, lp);
        }
        w.addView(chipsBox, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        welcomeChips = chipsBox;
        if (chatScroll.getHeight() > 0 && chatScroll.getHeight() < dp(440)) {
            chipsBox.setVisibility(View.GONE);
        }
        chatList.addView(w);
    }

    private View userBubble(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(17);
        tv.setTextColor(pal.text);
        tv.setTextIsSelectable(true);
        tv.setPadding(dp(16), dp(11), dp(16), dp(11));
        tv.setBackground(shape(pal.userBubble, 0, 20));
        tv.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.82f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.END;
        lp.setMargins(dp(40), dp(12), 0, dp(8));
        chatList.addView(tv, lp);
        return tv;
    }

    private View assistantBlock(final String text, final String key, boolean isLast) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        TextView tv = new TextView(this);
        tv.setText(Markdown.render(text, pal.codeBg, pal.codeFg, pal.accent));
        tv.setTextSize(17);
        tv.setTextColor(pal.text);
        tv.setLineSpacing(0f, 1.38f);
        tv.setTextIsSelectable(true);
        tv.setPadding(dp(2), dp(6), dp(2), dp(2));
        box.addView(tv);

        // action row: copy, share, read aloud, like, dislike, retry
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        actions.addView(actionIcon(IconView.COPY, v -> {
            copyText("agy", text);
            toast("Copied.");
        }));
        actions.addView(actionIcon(IconView.SHARE, v -> {
            Intent s = new Intent(Intent.ACTION_SEND);
            s.setType("text/plain");
            s.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(s, "Share"));
        }));
        actions.addView(actionIcon(IconView.PLAY, v -> speak(text)));

        final IconView up = actionIcon(IconView.THUMB_UP, null);
        final IconView down = actionIcon(IconView.THUMB_DOWN, null);
        Integer fb = feedback.get(key);
        up.setFilled(fb != null && fb == 1);
        down.setFilled(fb != null && fb == -1);
        up.setOnClickListener(v -> {
            boolean on = !(feedback.containsKey(key) && feedback.get(key) == 1);
            if (on) feedback.put(key, 1); else feedback.remove(key);
            up.setFilled(on);
            down.setFilled(false);
            if (on) toast("Thanks for the feedback.");
        });
        down.setOnClickListener(v -> {
            boolean on = !(feedback.containsKey(key) && feedback.get(key) == -1);
            if (on) feedback.put(key, -1); else feedback.remove(key);
            down.setFilled(on);
            up.setFilled(false);
            if (on) toast("Thanks for the feedback.");
        });
        actions.addView(up);
        actions.addView(down);
        if (isLast) actions.addView(actionIcon(IconView.RETRY, v -> retry()));

        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ap.setMargins(0, dp(4), 0, 0);
        box.addView(actions, ap);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(10), 0, dp(6));
        chatList.addView(box, lp);
        return box;
    }

    private IconView actionIcon(int type, View.OnClickListener l) {
        IconView v = new IconView(this, type);
        v.setColors(pal.subText, pal.bg);
        v.setIconDp(21);
        ripple(v, true);
        if (l != null) v.setOnClickListener(l);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(46), dp(42)));
        return v;
    }

    private View busyCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(shape(pal.card, pal.border, 16));
        card.setPadding(dp(14), dp(14), dp(14), dp(12));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        AntigravityView spark = new AntigravityView(this);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(54), dp(38));
        sp.setMargins(0, 0, dp(12), 0);
        row.addView(spark, sp);

        busyStatus = new TextView(this);
        busyStatus.setTextSize(15);
        busyStatus.setTextColor(pal.text);
        row.addView(busyStatus, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        busyTime = new TextView(this);
        busyTime.setTextSize(13);
        busyTime.setTextColor(pal.subText);
        row.addView(busyTime);
        card.addView(row);

        busyBar = new ShimmerBar(this);
        busyBar.setColors(pal.border, pal.accent);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(6));
        bp.setMargins(0, dp(12), 0, dp(6));
        card.addView(busyBar, bp);

        busyNote = new TextView(this);
        busyNote.setTextSize(11);
        busyNote.setTextColor(pal.subText);
        card.addView(busyNote);

        TextView stop = new TextView(this);
        stop.setText("Stop waiting");
        stop.setTextSize(13);
        stop.setTextColor(pal.accent);
        stop.setPadding(0, dp(10), dp(12), dp(2));
        stop.setOnClickListener(v -> ChatStore.finish(
                "Stopped waiting. NH may still be running on the server.", false));
        card.addView(stop);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(10), 0, dp(6));
        chatList.addView(card, lp);

        updateBusyCard();
        return card;
    }

    private void updateBusyCard() {
        if (busyStatus == null) return;
        long ms = Math.max(0L, System.currentTimeMillis() - ChatStore.busySince());
        long s = ms / 1000L;
        String status;
        if (s < 4) status = "Reading your request...";
        else if (s < 20) status = "Thinking...";
        else if (s < 60) status = "Working on it...";
        else if (s < 150) status = "Still working. Bigger tasks take longer...";
        else status = "Almost there. Complex tasks can take several minutes...";
        float p = (float) (0.95 * (1.0 - Math.exp(-ms / 60000.0)));
        busyStatus.setText(status);
        busyTime.setText(String.format(Locale.US, "%d:%02d", s / 60, s % 60));
        busyBar.setProgress(p);
        busyNote.setText("~" + Math.round(p * 100) + "% (estimate, NH does not report real progress)");
    }

    // ------------------------------------------------------------ welcome screen

    private View featureRow(int iconType, String title, String sub, int tier, List<View> subsOut) {
        int padV = tier == 0 ? 11 : (tier == 1 ? 9 : 8);
        int badgeDp = tier == 2 ? 38 : 44;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(padV), dp(14), dp(padV));
        row.setBackground(shape(pal.card, pal.border, 18));

        FrameLayout badge = new FrameLayout(this);
        badge.setBackground(circle(pal.accent));
        IconView ic = new IconView(this, iconType);
        ic.setColors(Color.WHITE, pal.accent);
        ic.setIconDp(tier == 2 ? 19 : 21);
        badge.addView(ic, new FrameLayout.LayoutParams(dp(badgeDp), dp(badgeDp)));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(badgeDp), dp(badgeDp));
        bp.setMargins(0, 0, dp(12), 0);
        row.addView(badge, bp);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(tier == 2 ? 15 : 16);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(pal.text);
        col.addView(t);
        if (tier < 2) {
            TextView s = new TextView(this);
            s.setText(sub);
            s.setTextSize(13);
            s.setTextColor(pal.subText);
            col.addView(s);
            subsOut.add(s);
        }
        row.addView(col, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    /**
     * After the first layout: if the middle part is taller than the screen allows, shrink the logo,
     * then hide the card sub-lines, then the tagline, until everything fits.
     */
    private void fitWelcome(final ScrollView sv, final View logo, final LinearLayout.LayoutParams lgp,
                            final List<View> subs, final TextView title, final TextView tag, final int level) {
        sv.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (sv.getViewTreeObserver().isAlive()) {
                    sv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                if (welcomeOverlay == null || sv.getChildCount() == 0 || sv.getHeight() == 0) return;
                boolean overflow = sv.getChildAt(0).getHeight() > sv.getHeight() + dp(2);
                if (!overflow || level >= 3) return;
                if (level == 0) {
                    lgp.width = (int) (lgp.width * 0.72f);
                    lgp.height = (int) (lgp.height * 0.72f);
                    logo.setLayoutParams(lgp);
                } else if (level == 1) {
                    for (View v : subs) v.setVisibility(View.GONE);
                } else {
                    title.setTextSize(30);
                    tag.setVisibility(View.GONE);
                }
                fitWelcome(sv, logo, lgp, subs, title, tag, level + 1);
            }
        });
    }

    /** Shown on every app start. Sizes adapt to the screen height so nothing is cut off. */
    private void showWelcome() {
        if (welcomeOverlay != null) return;
        hideKeyboard();
        final boolean returning = prefs.getBoolean("welcomed", false);
        float hDp = getResources().getDisplayMetrics().heightPixels
                / getResources().getDisplayMetrics().density;
        final int tier = hDp >= 760 ? 0 : (hDp >= 640 ? 1 : 2);
        int logoDp = tier == 2 ? Math.max(110, (int) (hDp * 0.18f)) : Math.min(165, (int) (hDp * 0.21f));

        final FrameLayout o = new FrameLayout(this);
        o.setClickable(true);
        o.setClipChildren(false);

        WelcomeBackdrop bd = new WelcomeBackdrop(this);
        bd.setColors(pal.dark ? 0xFF1F1E1D : blend(0xFFFFFFFF, pal.accent, 0.06f),
                pal.dark ? blend(0xFF1F1E1D, pal.accent, 0.28f) : blend(0xFFFFFFFF, pal.accent, 0.22f),
                pal.accent,
                0xFFFFFFFF,
                pal.dark ? 0xFFFFFFFF : pal.accent);
        o.addView(bd, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // page = top bar (install chip left, Skip right) / scrollable middle / fixed bottom (button)
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setClipChildren(false);
        page.setClipToPadding(false);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setPadding(dp(16), dp(10), dp(8), dp(4));
        topRow.setClipChildren(false);
        topRow.setClipToPadding(false);

        welcomeChip = makeChip();
        welcomeChip.setOnClickListener(v -> showInstallGuide());
        topRow.addView(welcomeChip, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        View topSpacer = new View(this);
        topRow.addView(topSpacer, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView skip = new TextView(this);
        skip.setText("Skip");
        skip.setTextSize(15);
        skip.setTextColor(pal.subText);
        skip.setPadding(dp(14), dp(10), dp(14), dp(10));
        skip.setOnClickListener(v -> dismissWelcome(false));
        topRow.addView(skip, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        page.addView(topRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setPadding(dp(24), dp(2), dp(24), dp(10));
        sv.addView(col, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(sv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        List<View> steps = new ArrayList<>();
        final List<View> subs = new ArrayList<>();

        LogoView logo = new LogoView(this);
        logo.setAccent(0xFFC6613F);
        final LinearLayout.LayoutParams lgp = new LinearLayout.LayoutParams(dp(logoDp), dp(logoDp));
        lgp.gravity = Gravity.CENTER_HORIZONTAL;
        col.addView(logo, lgp);
        steps.add(logo);

        TextView kicker = new TextView(this);
        kicker.setText(returning ? "WELCOME BACK TO" : "WELCOME TO");
        kicker.setTextSize(12);
        kicker.setTypeface(Typeface.DEFAULT_BOLD);
        kicker.setLetterSpacing(0.22f);
        kicker.setTextColor(pal.accent);
        kicker.setGravity(Gravity.CENTER);
        col.addView(kicker, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        steps.add(kicker);

        final TextView title = new TextView(this);
        title.setText("NH Chat");
        title.setTextSize(tier == 2 ? 32 : 38);
        title.setTypeface(Typeface.SERIF, Typeface.BOLD);
        title.setTextColor(pal.text);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(2), 0, dp(4));
        col.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        steps.add(title);

        final TextView tag = new TextView(this);
        tag.setText("Google Antigravity in your pocket.\nChat, build and ship from your phone.");
        tag.setTextSize(15);
        tag.setTextColor(pal.subText);
        tag.setGravity(Gravity.CENTER);
        tag.setLineSpacing(0f, 1.15f);
        tag.setPadding(0, 0, 0, dp(tier == 2 ? 8 : 14));
        col.addView(tag, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        steps.add(tag);

        String[][] feats = {
                {"Chat with NH anywhere", "Build and fix code in chat."},
                {"Projects that stay organised", "One folder, every chat saved."},
                {"Your model, your voice", "Pick a model, type or speak."}
        };
        int[] icons = {IconView.NEWCHAT, IconView.DOC, IconView.MIC};
        for (int i = 0; i < feats.length; i++) {
            View f = featureRow(icons[i], feats[i][0], feats[i][1], tier, subs);
            LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            fp.setMargins(0, dp(4), 0, dp(4));
            col.addView(f, fp);
            steps.add(f);
        }

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setPadding(dp(24), dp(8), dp(24), dp(16));
        bottom.setClipChildren(false);
        bottom.setClipToPadding(false);

        FrameLayout btnHolder = new FrameLayout(this);
        btnHolder.setClipChildren(false);
        final TextView btn = new TextView(this);
        btn.setText(returning ? "Continue" : "Get started");
        btn.setTextSize(17);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setTextColor(Color.WHITE);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(shape(pal.accent, 0, 28));
        btn.setOnClickListener(v -> onContinueClicked());
        btn.setOnTouchListener((v, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).start();
            } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            }
            return false;
        });
        btnHolder.addView(btn, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(56)));
        ScaleAnimation breathe = new ScaleAnimation(1f, 1.02f, 1f, 1.02f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        breathe.setDuration(1300);
        breathe.setRepeatMode(Animation.REVERSE);
        breathe.setRepeatCount(Animation.INFINITE);
        breathe.setInterpolator(new AccelerateDecelerateInterpolator());
        btnHolder.startAnimation(breathe);
        bottom.addView(btnHolder, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView note = new TextView(this);
        note.setText("Needs the server app with NH installed. Setup takes about a minute.");
        note.setTextSize(12);
        note.setTextColor(pal.sub);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, dp(10), 0, 0);
        bottom.addView(note, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        page.addView(bottom, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        steps.add(bottom);

        o.addView(page, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        rootFrame.addView(o, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        welcomeOverlay = o;
        updateInstallChips();
        checkInstall();
        fitWelcome(sv, logo, lgp, subs, title, tag, 0);
        welcomeBlur = new ArrayList<>();
        welcomeBlur.add(sv);
        welcomeBlur.add(bottom);
        welcomeBlur.add(skip);

        for (int i = 0; i < steps.size(); i++) {
            View v = steps.get(i);
            v.setAlpha(0f);
            v.setTranslationY(dp(22));
            if (i == 0) {
                v.setScaleX(0.6f);
                v.setScaleY(0.6f);
                v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                        .setStartDelay(80).setDuration(600)
                        .setInterpolator(new OvershootInterpolator(1.1f)).start();
            } else {
                v.animate().alpha(1f).translationY(0f)
                        .setStartDelay(160 + i * 70L).setDuration(420)
                        .setInterpolator(new DecelerateInterpolator()).start();
            }
        }
    }

    private void dismissWelcome(final boolean checkSetup) {
        final FrameLayout o = welcomeOverlay;
        if (o == null) return;
        welcomeOverlay = null;
        welcomeChip = null;
        spotlight = null;
        welcomeBlur = new ArrayList<>();
        prefs.edit().putBoolean("welcomed", true).apply();
        o.animate().alpha(0f).scaleX(1.05f).scaleY(1.05f).setDuration(320)
                .withEndAction(() -> {
                    rootFrame.removeView(o);
                    if (checkSetup && (!termuxInstalled()
                            || checkSelfPermission(PERM) != PackageManager.PERMISSION_GRANTED)) {
                        showSetupSheet();
                    }
                }).start();
    }

    private void showSetupSheet() {
        LinearLayout l = beginSheet("Quick setup");
        sheetNote(l, "Do these once, in this order. The server app must be installed from F-Droid.");
        sheetRow(l, "Open the full install guide", installStatusText(), false, this::showInstallGuide);
        sheetRow(l, "2. Copy server setup command", "Paste it in the server app, then fully restart it", false,
                this::copySetupCommand);
        sheetRow(l, "1. Grant server permission", "", false, this::requestTermuxPermission);
        sheetRow(l, "3. Storage permission", "", false, () -> runForeground(S_STORAGE));
        sheetRow(l, "4. Install NH (first time)", "", false, () -> runForeground(S_INSTALL));
        sheetRow(l, "Open server", "", false, this::openTermux);
        showSheet();
    }

    // ------------------------------------------------------------ install status + guide

    private static final class InstallInfo {
        boolean termux, perm, known, glibc, curl, agy, storage, script, zip;
    }

    private InstallInfo installInfo() {
        InstallInfo i = new InstallInfo();
        i.termux = termuxInstalled();
        i.perm = i.termux && checkSelfPermission(PERM) == PackageManager.PERMISSION_GRANTED;
        String raw = ChatStore.installRaw();
        if (raw.contains("check=done")) {
            i.known = true;
            i.glibc = raw.contains("glibc=1");
            i.curl = raw.contains("curl=1");
            i.agy = raw.contains("agy=1");
            i.storage = raw.contains("storage=1");
            i.script = raw.contains("script=1");
            i.zip = raw.contains("zip=1");
        }
        return i;
    }

    /** 1 = complete install (green), 0 = not installed (red), 2 = still checking (grey). */
    private int installState() {
        InstallInfo i = installInfo();
        if (i.termux && i.perm && i.known && i.glibc && i.agy) return 1;
        if (i.perm && !i.known && ChatStore.installChecking()) return 2;
        return 0;
    }

    private String installStatusText() {
        int st = installState();
        if (st == 1) return "Installed";
        if (st == 2) return "Checking...";
        return "Not installed - tap to install";
    }

    /** Asks Termux (silently) which install steps are already done. */
    private void checkInstall() {
        if (!termuxInstalled()) return;
        if (checkSelfPermission(PERM) != PackageManager.PERMISSION_GRANTED) return;
        if (ChatStore.installChecking()) return;
        try {
            Intent i = baseIntent(S_CHECK, "", false, true);
            i.putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", makePending("status"));
            ChatStore.setInstallChecking(true);
            startService(i);
        } catch (SecurityException e) {
            ChatStore.setInstall("Blocked by the server app (allow-external-apps).", false);
        } catch (Exception e) {
            ChatStore.setInstall(String.valueOf(e.getMessage()), false);
        }
        updateInstallChips();
    }

    private void forceCheckInstall() {
        ChatStore.setInstallChecking(false);
        checkInstall();
    }

    private TextView makeChip() {
        TextView c = new TextView(this);
        c.setTextSize(13);
        c.setTypeface(Typeface.DEFAULT_BOLD);
        c.setTextColor(Color.WHITE);
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(14), dp(8), dp(14), dp(8));
        c.setBackground(shape(pal.subText, 0, 18));
        c.setTag(Integer.valueOf(pal.subText));
        return c;
    }

    private void tintChip(TextView chip, int color) {
        final GradientDrawable bg = (GradientDrawable) chip.getBackground();
        Object old = chip.getTag();
        if (old instanceof Integer && ((Integer) old).intValue() != color) {
            ValueAnimator va = ValueAnimator.ofArgb(((Integer) old).intValue(), color);
            va.setDuration(300);
            va.addUpdateListener(a -> bg.setColor(((Integer) a.getAnimatedValue()).intValue()));
            va.start();
        } else {
            bg.setColor(color);
        }
        chip.setTag(Integer.valueOf(color));
    }

    private void styleChip(TextView chip, int st, boolean compact) {
        if (chip == null) return;
        String label = compact
                ? (st == 1 ? "Installed \u2713" : (st == 2 ? "Checking..." : "Not installed"))
                : (st == 1 ? "Installed \u2713  \u00B7  Guide" : (st == 2 ? "Checking..." : "Install & Guide"));
        int color = st == 1 ? pal.green : (st == 2 ? pal.subText : pal.red);
        if (label.contentEquals(chip.getText())) return;
        chip.setText(label);
        tintChip(chip, color);
        chip.clearAnimation();
        if (st == 0) {
            ScaleAnimation pulse = new ScaleAnimation(1f, 1.06f, 1f, 1.06f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            pulse.setDuration(900);
            pulse.setRepeatMode(Animation.REVERSE);
            pulse.setRepeatCount(Animation.INFINITE);
            pulse.setInterpolator(new AccelerateDecelerateInterpolator());
            chip.startAnimation(pulse);
        }
    }

    private void updateInstallChips() {
        int st = installState();
        styleChip(welcomeChip, st, false);
        styleChip(guideChip, st, true);
    }

    private TextView makeButton(String label, boolean primary, View.OnClickListener l) {
        TextView b = new TextView(this);
        b.setText(label);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setTextColor(primary ? Color.WHITE : pal.text);
        b.setBackground(shape(primary ? pal.accent : pal.grey, 0, 22));
        b.setPadding(dp(14), dp(10), dp(14), dp(10));
        b.setOnClickListener(l);
        b.setOnTouchListener((v, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start();
            } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(110).start();
            }
            return false;
        });
        return b;
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            toast("No browser found. Open: " + url);
        }
    }

    private void openOverlaySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + TERMUX_PKG)));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + TERMUX_PKG)));
            } catch (Exception e2) {
                toast("Open Android Settings > Apps > (the server app) > Display over other apps.");
            }
        }
    }

    private void runCompleteInstall() {
        if (!ready()) return;
        runForeground(S_FULL_INSTALL);
        toast("Installing on the server. Come back here when it says DONE, then tap Recheck.");
    }

    private void showInstallGuide() {
        if (guideOverlay != null) return;
        hideKeyboard();
        final FrameLayout g = new FrameLayout(this);
        g.setBackgroundColor(pal.bg);
        g.setClickable(true);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(4), dp(8), dp(14), dp(8));
        head.setClipToPadding(false);
        head.setClipChildren(false);
        IconView back = iconBtn(IconView.BACK, 24, 48, v -> closeInstallGuide());
        head.addView(back);
        TextView ttl = new TextView(this);
        ttl.setText("Install guide");
        ttl.setTextSize(20);
        ttl.setTypeface(Typeface.SERIF, Typeface.BOLD);
        ttl.setTextColor(pal.text);
        ttl.setSingleLine(true);
        ttl.setEllipsize(TextUtils.TruncateAt.END);
        head.addView(ttl, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        guideChip = makeChip();
        guideChip.setOnClickListener(v -> {
            forceCheckInstall();
            toast("Checking...");
        });
        head.addView(guideChip, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        page.addView(head);

        View divider = new View(this);
        divider.setBackgroundColor(pal.border);
        page.addView(divider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));

        guideScroll = new ScrollView(this);
        guideScroll.setFillViewport(true);
        guideScroll.setVerticalScrollBarEnabled(false);
        guideContent = new LinearLayout(this);
        guideContent.setOrientation(LinearLayout.VERTICAL);
        guideContent.setPadding(dp(16), dp(14), dp(16), dp(30));
        guideScroll.addView(guideContent);
        page.addView(guideScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        g.addView(page, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        rootFrame.addView(g, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        guideOverlay = g;
        guideFirst = true;

        g.setTranslationX(getResources().getDisplayMetrics().widthPixels);
        g.animate().translationX(0f).setDuration(260)
                .setInterpolator(new DecelerateInterpolator()).start();

        updateInstallChips();
        renderGuide();
        checkInstall();
    }

    private void closeInstallGuide() {
        final FrameLayout g = guideOverlay;
        if (g == null) return;
        guideOverlay = null;
        guideChip = null;
        guideContent = null;
        guideScroll = null;
        hideKeyboard();
        g.animate().translationX(getResources().getDisplayMetrics().widthPixels).setDuration(220)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> rootFrame.removeView(g)).start();
    }

    private void renderGuide() {
        if (guideOverlay == null || guideContent == null || guideScroll == null) return;
        final ScrollView sc = guideScroll;
        final int oldY = sc.getScrollY();
        guideContent.removeAllViews();

        InstallInfo info = installInfo();
        int state = installState();
        int stateColor = state == 1 ? pal.green : (state == 2 ? pal.subText : pal.red);

        int checks = 0;
        if (info.termux) checks++;
        if (info.known) checks++;
        if (info.perm) checks++;
        if (info.storage) checks++;
        if (info.glibc) checks++;
        if (info.curl && info.script) checks++;
        if (info.agy) checks++;
        final int total = 7;

        // ----- summary card
        LinearLayout sum = new LinearLayout(this);
        sum.setOrientation(LinearLayout.VERTICAL);
        sum.setBackground(shape(pal.card, pal.border, 18));
        sum.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = new TextView(this);
        badge.setText(state == 1 ? "\u2713" : (state == 2 ? "\u2026" : "!"));
        badge.setTextSize(24);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setTextColor(Color.WHITE);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(circle(stateColor));
        LinearLayout.LayoutParams bgp = new LinearLayout.LayoutParams(dp(52), dp(52));
        bgp.setMargins(0, 0, dp(14), 0);
        top.addView(badge, bgp);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView st = new TextView(this);
        st.setText(state == 1 ? "Antigravity is installed"
                : (state == 2 ? "Checking your setup..." : "Antigravity is not installed yet"));
        st.setTextSize(18);
        st.setTypeface(Typeface.SERIF, Typeface.BOLD);
        st.setTextColor(pal.text);
        col.addView(st);
        TextView sb = new TextView(this);
        sb.setText(checks + " of " + total + " checks passed"
                + (state == 1 ? "  \u00B7  you are ready to chat" : "  \u00B7  follow the steps below"));
        sb.setTextSize(13);
        sb.setTextColor(pal.subText);
        col.addView(sb);
        top.addView(col, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        sum.addView(top);

        ShimmerBar bar = new ShimmerBar(this);
        bar.setColors(pal.border, stateColor);
        bar.setProgress(checks / (float) total);
        LinearLayout.LayoutParams barp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(6));
        barp.setMargins(0, dp(14), 0, 0);
        sum.addView(bar, barp);

        TextView run = makeButton(state == 1 ? "Run install again" : "Run complete install",
                state != 1, v -> runCompleteInstall());
        LinearLayout.LayoutParams runp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        runp.setMargins(0, dp(14), 0, 0);
        sum.addView(run, runp);

        LinearLayout two = new LinearLayout(this);
        two.setOrientation(LinearLayout.HORIZONTAL);
        TextView copyAll = makeButton("Copy commands", false, v -> {
            copyText("agy install", FULL_COMMANDS_TEXT);
            toast("Copied. Paste it in the server app.");
        });
        copyAll.setMinHeight(dp(46));
        LinearLayout.LayoutParams c1 = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        c1.setMargins(0, 0, dp(6), 0);
        two.addView(copyAll, c1);
        TextView recheck = makeButton("Recheck", false, v -> {
            forceCheckInstall();
            toast("Checking...");
        });
        recheck.setMinHeight(dp(46));
        LinearLayout.LayoutParams c2 = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        c2.setMargins(dp(6), 0, 0, 0);
        two.addView(recheck, c2);
        LinearLayout.LayoutParams twop = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        twop.setMargins(0, dp(10), 0, 0);
        sum.addView(two, twop);

        guideContent.addView(sum, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView hdr = new TextView(this);
        hdr.setText("STEP BY STEP");
        hdr.setTextSize(12);
        hdr.setTypeface(Typeface.DEFAULT_BOLD);
        hdr.setLetterSpacing(0.12f);
        hdr.setTextColor(pal.sub);
        hdr.setPadding(dp(4), dp(20), dp(4), dp(8));
        guideContent.addView(hdr);

        // ----- steps (status: 1 done, 0 to do, 2 manual)
        addStepCard(1, "Install the server app",
                "Install the F-Droid version (the Play Store one is outdated). Open it once, then come back here.",
                info.termux ? 1 : 0, null,
                "Open F-Droid page", () -> openUrl("https://f-droid.org/packages/com.termux/"),
                "Open server", this::openTermux);

        addStepCard(2, "Let other apps send commands",
                "Paste this in the server app, then fully close it and open it again.",
                info.known ? 1 : 0,
                new String[][]{{"Paste in the server app", SETUP_CMD}},
                "Copy and open server", this::copySetupCommand, null, null);

        addStepCard(3, "Give this app permission",
                "Tap the button and allow the permission that Android asks for.",
                info.perm ? 1 : 0, null,
                "Grant permission", this::requestTermuxPermission, null, null);

        addStepCard(4, "Allow the server to display over other apps",
                "Android Settings > Apps > (the server app) > Display over other apps > Allow. "
                        + "This lets the server window open when this app starts a command.",
                2, null,
                "Open settings", this::openOverlaySettings, null, null);

        addStepCard(5, "Update packages and allow storage",
                "Update the server, then tap Allow when Android asks for storage access.",
                info.storage ? 1 : 0,
                new String[][]{{"", "pkg update -y"}, {"", "pkg upgrade -y"},
                        {"Allow storage", "termux-setup-storage"}},
                "Run on server", () -> runForegroundChecked(S_STEP_UPDATE), null, null);

        addStepCard(6, "Install glibc",
                "NH needs the glibc runtime. Do this before step 8, otherwise the installer stops with a "
                        + "\"Missing ... glibc loader\" error.",
                info.glibc ? 1 : 0,
                new String[][]{{"", "pkg install glibc-repo -y"}, {"", "pkg install glibc -y"}},
                "Run on server", () -> runForegroundChecked(S_STEP_GLIBC), null, null);

        addStepCard(7, "Install helper tools",
                "curl downloads the installer, util-linux lets the chat run NH in the background, "
                        + "zip saves projects.",
                (info.curl && info.script) ? 1 : 0,
                new String[][]{{"", "pkg install curl util-linux zip nano -y"}},
                "Run on server", () -> runForegroundChecked(S_STEP_TOOLS), null, null);

        addStepCard(8, "Install Antigravity CLI (NH)",
                "A community installer (not made by Google) that patches the official CLI for the server.",
                info.agy ? 1 : 0,
                new String[][]{
                        {"Install", "curl -fsSL " + INSTALL_URL + " | bash"},
                        {"Or read the script first, then run it",
                                "curl -fsSL " + INSTALL_URL + " -o install.sh\ncat install.sh\nbash install.sh"}},
                "Run on server", () -> runForegroundChecked(S_STEP_AGY), null, null);

        addStepCard(9, "Log in with Google",
                "Open NH once. Open the link it shows in your browser, sign in, then paste the code back on the server.",
                2,
                new String[][]{{"", "agy"}},
                "Open NH on server", () -> runForegroundChecked(S_INTERACTIVE), null, null);

        addStepCard(10, "Check that it works",
                "You should see the path of the NH command and its version. Then use the chat screen.",
                info.agy ? 1 : 0,
                new String[][]{{"", "which agy"}, {"", "agy --version"}},
                "Recheck", () -> {
                    forceCheckInstall();
                    toast("Checking...");
                }, null, null);

        // ----- troubleshooting
        TextView th = new TextView(this);
        th.setText("IF SOMETHING FAILS");
        th.setTextSize(12);
        th.setTypeface(Typeface.DEFAULT_BOLD);
        th.setLetterSpacing(0.12f);
        th.setTextColor(pal.sub);
        th.setPadding(dp(4), dp(14), dp(4), dp(8));
        guideContent.addView(th);

        LinearLayout tb = new LinearLayout(this);
        tb.setOrientation(LinearLayout.VERTICAL);
        tb.setBackground(shape(pal.card, pal.border, 16));
        tb.setPadding(dp(14), dp(12), dp(14), dp(12));
        String[] tips = {
                "\"Missing ... glibc loader\" error: run step 6 (glibc), then step 8 again.",
                "If the terminal says the command was not found: close the server app completely, open it again, then run: which agy",
                "This app says blocked or nothing happens: finish steps 2 and 3, then restart the server app.",
                "The server window does not open: finish step 4.",
                "Update NH later with this command: agy update"
        };
        for (String tip : tips) {
            TextView tv = new TextView(this);
            tv.setText("\u2022  " + tip);
            tv.setTextSize(14);
            tv.setTextColor(pal.subText);
            tv.setPadding(0, dp(3), 0, dp(3));
            tb.addView(tv);
        }
        guideContent.addView(tb, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (guideFirst) {
            guideFirst = false;
            for (int i = 0; i < guideContent.getChildCount(); i++) {
                View v = guideContent.getChildAt(i);
                v.setAlpha(0f);
                v.setTranslationY(dp(18));
                v.animate().alpha(1f).translationY(0f)
                        .setStartDelay(120 + Math.min(i, 8) * 55L).setDuration(380)
                        .setInterpolator(new DecelerateInterpolator()).start();
            }
        } else {
            sc.post(() -> sc.scrollTo(0, oldY));
        }
    }

    private void runForegroundChecked(String script) {
        if (!ready()) return;
        runForeground(script);
    }

    private void addStepCard(int index, String title, String desc, int status, String[][] cmds,
                             String actLabel, final Runnable act, String act2Label, final Runnable act2) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(shape(pal.card, pal.border, 16));
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        TextView nb = new TextView(this);
        nb.setText(status == 1 ? "\u2713" : String.valueOf(index));
        nb.setTextSize(14);
        nb.setTypeface(Typeface.DEFAULT_BOLD);
        nb.setGravity(Gravity.CENTER);
        nb.setTextColor(status == 1 ? Color.WHITE : pal.text);
        nb.setBackground(circle(status == 1 ? pal.green : pal.grey));
        LinearLayout.LayoutParams nbp = new LinearLayout.LayoutParams(dp(30), dp(30));
        nbp.setMargins(0, 0, dp(12), 0);
        h.addView(nb, nbp);
        TextView tt = new TextView(this);
        tt.setText(title);
        tt.setTextSize(16);
        tt.setTypeface(Typeface.DEFAULT_BOLD);
        tt.setTextColor(pal.text);
        h.addView(tt, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView stl = new TextView(this);
        stl.setText(status == 1 ? "Done" : (status == 2 ? "Manual" : "To do"));
        stl.setTextSize(12);
        stl.setTypeface(Typeface.DEFAULT_BOLD);
        stl.setTextColor(status == 1 ? pal.green : (status == 2 ? pal.sub : pal.red));
        stl.setPadding(dp(8), 0, 0, 0);
        h.addView(stl);
        card.addView(h);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextSize(14);
        d.setTextColor(pal.subText);
        d.setLineSpacing(0f, 1.15f);
        d.setPadding(0, dp(8), 0, dp(2));
        card.addView(d);

        if (cmds != null) {
            for (String[] c : cmds) {
                card.addView(commandBox(c[0], c[1]));
            }
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        if (actLabel != null && act != null) {
            TextView b1 = makeButton(actLabel, status != 1, v -> act.run());
            LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(42));
            p1.setMargins(0, 0, dp(8), 0);
            actions.addView(b1, p1);
        }
        if (act2Label != null && act2 != null) {
            TextView b2 = makeButton(act2Label, false, v -> act2.run());
            actions.addView(b2, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(42)));
        }
        if (actions.getChildCount() > 0) {
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ap.setMargins(0, dp(12), 0, 0);
            card.addView(actions, ap);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        guideContent.addView(card, lp);
    }

    /** A monospace command with its own Copy button. */
    private View commandBox(String caption, final String cmd) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        if (caption != null && !caption.isEmpty()) {
            TextView c = new TextView(this);
            c.setText(caption);
            c.setTextSize(12);
            c.setTextColor(pal.subText);
            c.setPadding(dp(2), dp(10), dp(2), dp(4));
            wrap.addView(c);
        } else {
            wrap.setPadding(0, dp(8), 0, 0);
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setBackground(shape(pal.codeBg, 0, 12));

        TextView t = new TextView(this);
        t.setText(cmd);
        t.setTypeface(Typeface.MONOSPACE);
        t.setTextSize(13);
        t.setTextColor(pal.text);
        t.setTextIsSelectable(true);
        t.setPadding(dp(12), dp(10), dp(6), dp(10));
        box.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final TextView cp = new TextView(this);
        cp.setText("Copy");
        cp.setTextSize(14);
        cp.setTypeface(Typeface.DEFAULT_BOLD);
        cp.setTextColor(pal.accent);
        cp.setPadding(dp(12), dp(10), dp(14), dp(10));
        ripple(cp, false);
        cp.setOnClickListener(v -> {
            copyText("agy install", cmd);
            cp.setText("Copied \u2713");
            ui.postDelayed(() -> cp.setText("Copy"), 1500);
        });
        box.addView(cp);
        wrap.addView(box, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return wrap;
    }

    // ------------------------------------------------------------ welcome: install first

    private static int blend(int base, int over, float t) {
        int r = Math.round(Color.red(base) * (1f - t) + Color.red(over) * t);
        int g = Math.round(Color.green(base) * (1f - t) + Color.green(over) * t);
        int b = Math.round(Color.blue(base) * (1f - t) + Color.blue(over) * t);
        return Color.rgb(r, g, b);
    }

    /** Continue: opens the chat only when the install is complete; otherwise spotlights the install button. */
    private void onContinueClicked() {
        int st = installState();
        if (st == 1) {
            dismissWelcome(true);
            return;
        }
        if (st == 2) {
            toast("Checking your setup... try again in a moment.");
            return;
        }
        highlightInstallChip();
    }

    private void blurWelcome(boolean on) {
        for (View v : welcomeBlur) {
            if (Build.VERSION.SDK_INT >= 31) {
                v.setRenderEffect(on
                        ? RenderEffect.createBlurEffect(18f, 18f, Shader.TileMode.CLAMP)
                        : null);
            }
            v.animate().alpha(on ? 0.45f : 1f).setDuration(240).start();
        }
    }

    private void highlightInstallChip() {
        final FrameLayout o = welcomeOverlay;
        if (o == null || welcomeChip == null || spotlight != null) return;
        int[] a = new int[2];
        int[] b = new int[2];
        welcomeChip.getLocationInWindow(a);
        o.getLocationInWindow(b);
        float pad = dp(6);
        float l = a[0] - b[0] - pad;
        float t = a[1] - b[1] - pad;
        float r = l + welcomeChip.getWidth() + 2 * pad;
        float bt = t + welcomeChip.getHeight() + 2 * pad;

        final SpotlightLayout sp = new SpotlightLayout(this);
        sp.setHole(l, t, r, bt, (bt - t) / 2f);
        sp.setRingColor(Color.WHITE);

        View arrow = new View(this);
        arrow.setBackground(shape(pal.card, 0, 3));
        arrow.setRotation(45f);
        FrameLayout.LayoutParams ap = new FrameLayout.LayoutParams(dp(16), dp(16));
        ap.leftMargin = (int) ((l + r) / 2f) - dp(8);
        ap.topMargin = (int) bt + dp(8);
        sp.addView(arrow, ap);

        final LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setBackground(shape(pal.card, 0, 16));
        bubble.setPadding(dp(16), dp(14), dp(16), dp(14));
        TextView h1 = new TextView(this);
        h1.setText("Install NH first");
        h1.setTextSize(17);
        h1.setTypeface(Typeface.DEFAULT_BOLD);
        h1.setTextColor(pal.text);
        bubble.addView(h1);
        TextView h2 = new TextView(this);
        h2.setText("NH is not installed yet. Tap the glowing button, follow the steps, "
                + "then press Continue. You can also tap Skip.");
        h2.setTextSize(14);
        h2.setTextColor(pal.subText);
        h2.setPadding(0, dp(4), 0, 0);
        bubble.addView(h2);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
                Math.min(o.getWidth() - dp(32), dp(330)), FrameLayout.LayoutParams.WRAP_CONTENT);
        bp.leftMargin = dp(16);
        bp.topMargin = (int) bt + dp(16);
        sp.addView(bubble, bp);

        sp.setListeners(() -> {
            clearSpotlight();
            showInstallGuide();
        }, this::clearSpotlight);
        o.addView(sp, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        spotlight = sp;
        blurWelcome(true);
        sp.setAlpha(0f);
        sp.animate().alpha(1f).setDuration(240).start();
        bubble.setTranslationY(dp(10));
        bubble.animate().translationY(0f).setDuration(320)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    private void clearSpotlight() {
        final SpotlightLayout sp = spotlight;
        if (sp == null) return;
        spotlight = null;
        blurWelcome(false);
        sp.animate().alpha(0f).setDuration(200).withEndAction(() -> {
            if (sp.getParent() instanceof FrameLayout) {
                ((FrameLayout) sp.getParent()).removeView(sp);
            }
        }).start();
    }

    // ------------------------------------------------------------ settings page

    private void copyWholeChat() {
        String all = ChatStore.exportCurrent();
        if (all.isEmpty()) {
            toast("Nothing to copy yet.");
        } else {
            copyText("NH chat", all);
            toast("Whole chat copied.");
        }
    }

    private Bitmap avatarBitmap;
    private boolean avatarTried = false;
    private boolean settingsFirst = false;

    private Bitmap loadAvatar() {
        if (avatarTried) return avatarBitmap;
        avatarTried = true;
        try {
            java.io.InputStream in = getResources().openRawResource(R.raw.dev_profile);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            byte[] data = out.toByteArray();
            if (NhCore.imageOk(data)) {
                avatarBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            }
        } catch (Exception e) {
            avatarBitmap = null;
        }
        return avatarBitmap;
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    private void chooseTheme(String mode) {
        if (mode.equals(prefs.getString("theme", "system"))) return;
        prefs.edit().putString("theme", mode).putBoolean("reopenSettings", true).apply();
        recreate();
    }

    private void chooseAccent(int idx) {
        if (idx == prefs.getInt("accent", 0)) return;
        prefs.edit().putInt("accent", idx).putBoolean("reopenSettings", true).apply();
        recreate();
    }

    private void showSettings(boolean animate) {
        if (settingsOverlay != null) return;
        hideKeyboard();
        final FrameLayout g = new FrameLayout(this);
        g.setBackgroundColor(pal.bg);
        g.setClickable(true);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(4), dp(8), dp(14), dp(8));
        head.addView(iconBtn(IconView.BACK, 24, 48, v -> closeSettings()));
        TextView ttl = new TextView(this);
        ttl.setText("Settings");
        ttl.setTextSize(20);
        ttl.setTypeface(Typeface.SERIF, Typeface.BOLD);
        ttl.setTextColor(pal.text);
        head.addView(ttl, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        page.addView(head);

        View divider = new View(this);
        divider.setBackgroundColor(pal.border);
        page.addView(divider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));

        settingsScroll = new ScrollView(this);
        settingsScroll.setFillViewport(true);
        settingsScroll.setVerticalScrollBarEnabled(false);
        settingsContent = new LinearLayout(this);
        settingsContent.setOrientation(LinearLayout.VERTICAL);
        settingsContent.setPadding(dp(16), dp(8), dp(16), dp(30));
        settingsScroll.addView(settingsContent);
        page.addView(settingsScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        g.addView(page, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        rootFrame.addView(g, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        settingsOverlay = g;
        settingsFirst = animate;

        if (animate) {
            g.setTranslationX(getResources().getDisplayMetrics().widthPixels);
            g.animate().translationX(0f).setDuration(260)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
        renderSettings();
    }

    private void closeSettings() {
        final FrameLayout g = settingsOverlay;
        if (g == null) return;
        settingsOverlay = null;
        settingsContent = null;
        settingsScroll = null;
        g.animate().translationX(getResources().getDisplayMetrics().widthPixels).setDuration(220)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> rootFrame.removeView(g)).start();
    }

    private TextView settingsSection(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(12);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLetterSpacing(0.12f);
        t.setTextColor(pal.sub);
        t.setPadding(dp(4), dp(20), dp(4), dp(8));
        return t;
    }

    private LinearLayout settingsCard() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(shape(pal.card, pal.border, 18));
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        return c;
    }

    private View themeTile(final String mode, int previewMode, String label, boolean selected) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(pal.card);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(selected ? 2 : 1), selected ? pal.accent : pal.border);
        tile.setBackground(bg);
        ThemePreviewView pv = new ThemePreviewView(this, previewMode);
        pv.setAccent(pal.accent);
        tile.addView(pv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(74)));
        TextView t = new TextView(this);
        t.setText(selected ? label + " \u2713" : label);
        t.setTextSize(13);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(selected ? pal.accent : pal.text);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(8), 0, dp(2));
        tile.addView(t);
        tile.setOnClickListener(v -> chooseTheme(mode));
        return tile;
    }

    private void toolRow(LinearLayout parent, String label, String sub, final Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(4), dp(12), dp(4), dp(12));
        ripple(row, false);
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(16);
        l.setTextColor(pal.text);
        row.addView(l);
        TextView s = new TextView(this);
        s.setText(sub);
        s.setTextSize(12);
        s.setTextColor(pal.subText);
        row.addView(s);
        row.setOnClickListener(v -> action.run());
        parent.addView(row);
    }

    private void detailRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(2), dp(10), dp(2), dp(10));
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(12);
        l.setTextColor(pal.subText);
        row.addView(l);
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(16);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setTextColor(pal.text);
        v.setTextIsSelectable(true);
        row.addView(v);
        parent.addView(row);
    }

    private void renderSettings() {
        if (settingsOverlay == null || settingsContent == null) return;
        settingsContent.removeAllViews();

        // ----- appearance
        settingsContent.addView(settingsSection("APPEARANCE"));
        LinearLayout theme = settingsCard();
        TextView th = new TextView(this);
        th.setText("Theme");
        th.setTextSize(16);
        th.setTypeface(Typeface.DEFAULT_BOLD);
        th.setTextColor(pal.text);
        theme.addView(th);
        TextView ths = new TextView(this);
        ths.setText("Choose how NH Chat looks.");
        ths.setTextSize(13);
        ths.setTextColor(pal.subText);
        ths.setPadding(0, dp(2), 0, dp(12));
        theme.addView(ths);

        String cur = prefs.getString("theme", "system");
        LinearLayout tiles = new LinearLayout(this);
        tiles.setOrientation(LinearLayout.HORIZONTAL);
        String[] modes = {"system", "light", "dark"};
        String[] labels = {"System", "Light", "Dark"};
        for (int i = 0; i < 3; i++) {
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tp.setMargins(i == 0 ? 0 : dp(4), 0, i == 2 ? 0 : dp(4), 0);
            tiles.addView(themeTile(modes[i], i, labels[i], modes[i].equals(cur)), tp);
        }
        theme.addView(tiles);

        TextView ac = new TextView(this);
        ac.setText("Accent colour");
        ac.setTextSize(16);
        ac.setTypeface(Typeface.DEFAULT_BOLD);
        ac.setTextColor(pal.text);
        ac.setPadding(0, dp(20), 0, dp(10));
        theme.addView(ac);

        int curAccent = prefs.getInt("accent", 0);
        LinearLayout dots = new LinearLayout(this);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(Gravity.CENTER_VERTICAL);
        String[] hexes = pal.dark ? Palette.ACC_DARK : Palette.ACC_LIGHT;
        for (int i = 0; i < hexes.length; i++) {
            final int idx = i;
            FrameLayout dot = new FrameLayout(this);
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(Color.parseColor(hexes[i]));
            if (i == curAccent) gd.setStroke(dp(3), pal.text);
            dot.setBackground(gd);
            if (i == curAccent) {
                TextView ck = new TextView(this);
                ck.setText("\u2713");
                ck.setTextSize(18);
                ck.setTypeface(Typeface.DEFAULT_BOLD);
                ck.setTextColor(Color.WHITE);
                ck.setGravity(Gravity.CENTER);
                dot.addView(ck, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            }
            dot.setOnClickListener(v -> chooseAccent(idx));
            LinearLayout.LayoutParams dp1 = new LinearLayout.LayoutParams(dp(42), dp(42));
            dp1.setMargins(0, 0, dp(12), 0);
            dots.addView(dot, dp1);
        }
        theme.addView(dots);
        TextView an = new TextView(this);
        an.setText(Palette.ACC_NAMES[curAccent < Palette.ACC_NAMES.length ? curAccent : 0]);
        an.setTextSize(13);
        an.setTextColor(pal.subText);
        an.setPadding(0, dp(8), 0, 0);
        theme.addView(an);
        settingsContent.addView(theme);

        // ----- tools
        settingsContent.addView(settingsSection("TOOLS"));
        LinearLayout tools = settingsCard();
        toolRow(tools, "Copy whole chat", "Copies the current chat as text", this::copyWholeChat);
        toolRow(tools, "Install guide", "Step-by-step setup with copy buttons", this::showInstallGuide);
        toolRow(tools, "Show welcome screen", "Replay the intro", this::showWelcome);
        settingsContent.addView(tools);

        // ----- developer
        settingsContent.addView(settingsSection("DEVELOPER"));
        LinearLayout dev = settingsCard();
        dev.setGravity(Gravity.CENTER_HORIZONTAL);
        final String name = NhCore.text(NhCore.NAME);
        final String tg = NhCore.text(NhCore.TELEGRAM);
        String exp = NhCore.text(NhCore.EXPERIENCE);
        String role = NhCore.text(NhCore.ROLE);
        String handle = NhCore.text(NhCore.HANDLE);
        boolean ok = !name.isEmpty() && !tg.isEmpty() && !exp.isEmpty();
        if (!ok) {
            TextView warn = new TextView(this);
            warn.setText("Developer details could not be verified. Please install the original NH Chat app.");
            warn.setTextSize(14);
            warn.setTextColor(pal.red);
            warn.setGravity(Gravity.CENTER);
            warn.setPadding(dp(8), dp(8), dp(8), dp(8));
            dev.addView(warn);
        } else {
            AvatarView av = new AvatarView(this);
            av.setColors(pal.accent, pal.card);
            av.setInitials("NH");
            av.setBitmap(loadAvatar());
            dev.addView(av, new LinearLayout.LayoutParams(dp(112), dp(112)));

            TextView nm = new TextView(this);
            nm.setText(name);
            nm.setTextSize(28);
            nm.setTypeface(Typeface.SERIF, Typeface.BOLD);
            nm.setTextColor(pal.text);
            nm.setGravity(Gravity.CENTER);
            nm.setPadding(0, dp(12), 0, 0);
            dev.addView(nm);
            TextView rl = new TextView(this);
            rl.setText(role);
            rl.setTextSize(14);
            rl.setTextColor(pal.subText);
            rl.setGravity(Gravity.CENTER);
            rl.setPadding(0, dp(2), 0, dp(10));
            dev.addView(rl);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setBackground(shape(pal.grey, 0, 14));
            info.setPadding(dp(14), dp(4), dp(14), dp(4));
            detailRow(info, "Name", name);
            detailRow(info, "Experience", exp);
            detailRow(info, "Telegram", handle);
            dev.addView(info, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            LinearLayout btns = new LinearLayout(this);
            btns.setOrientation(LinearLayout.HORIZONTAL);
            TextView open = makeButton("Open Telegram", true, v -> openUrl(tg));
            LinearLayout.LayoutParams o1 = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            o1.setMargins(0, 0, dp(6), 0);
            open.setMinHeight(dp(46));
            btns.addView(open, o1);
            TextView cp = makeButton("Copy link", false, v -> {
                copyText("Telegram", tg);
                toast("Telegram link copied.");
            });
            LinearLayout.LayoutParams o2 = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            o2.setMargins(dp(6), 0, 0, 0);
            cp.setMinHeight(dp(46));
            btns.addView(cp, o2);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            bp.setMargins(0, dp(14), 0, 0);
            dev.addView(btns, bp);
        }
        settingsContent.addView(dev);

        // ----- about
        settingsContent.addView(settingsSection("ABOUT"));
        LinearLayout about = settingsCard();
        detailRow(about, "App", "NH Chat");
        String ver = versionName();
        if (!ver.isEmpty()) detailRow(about, "Version", ver);
        String credit = NhCore.text(NhCore.CREDIT);
        if (!credit.isEmpty()) {
            TextView cr = new TextView(this);
            cr.setText(credit);
            cr.setTextSize(13);
            cr.setTextColor(pal.subText);
            cr.setPadding(dp(2), dp(8), dp(2), 0);
            about.addView(cr);
        }
        settingsContent.addView(about);

        if (settingsFirst) {
            settingsFirst = false;
            for (int i = 0; i < settingsContent.getChildCount(); i++) {
                View v = settingsContent.getChildAt(i);
                v.setAlpha(0f);
                v.setTranslationY(dp(18));
                v.animate().alpha(1f).translationY(0f)
                        .setStartDelay(100 + Math.min(i, 8) * 55L).setDuration(380)
                        .setInterpolator(new DecelerateInterpolator()).start();
            }
        }
    }

    // ------------------------------------------------------------ bottom sheets

    private LinearLayout beginSheet(String title) {
        sheetRoot = new LinearLayout(this);
        sheetRoot.setOrientation(LinearLayout.VERTICAL);
        float r = dp(24);
        GradientDrawable g = new GradientDrawable();
        g.setColor(pal.card);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        sheetRoot.setBackground(g);
        sheetRoot.setPadding(dp(8), dp(10), dp(8), dp(16));

        View handle = new View(this);
        handle.setBackground(shape(pal.border, 0, 3));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(dp(40), dp(5));
        hp.gravity = Gravity.CENTER_HORIZONTAL;
        hp.setMargins(0, 0, 0, dp(10));
        sheetRoot.addView(handle, hp);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(18);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(pal.text);
        t.setPadding(dp(14), dp(2), dp(14), dp(8));
        sheetRoot.addView(t);

        int maxH = (int) (getResources().getDisplayMetrics().heightPixels * 0.72f);
        MaxHeightScrollView sv = new MaxHeightScrollView(this, maxH);
        sv.setVerticalScrollBarEnabled(false);
        sheetList = new LinearLayout(this);
        sheetList.setOrientation(LinearLayout.VERTICAL);
        sv.addView(sheetList);
        sheetRoot.addView(sv);
        return sheetList;
    }

    private void showSheet() {
        dismissSheet();
        hideKeyboard();
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(sheetRoot);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
            w.setDimAmount(0.35f);
        }
        final View root = sheetRoot;
        root.setTranslationY(dp(700));
        sheetDialog = d;
        d.show();
        root.post(() -> root.animate().translationY(0f).setDuration(240)
                .setInterpolator(new DecelerateInterpolator()).start());
    }

    private void dismissSheet() {
        if (sheetDialog != null && sheetDialog.isShowing()) {
            try {
                sheetDialog.dismiss();
            } catch (Exception ignored) {
                // window already gone
            }
        }
        sheetDialog = null;
    }

    private void sheetSection(LinearLayout list, String text) {
        TextView t = new TextView(this);
        t.setText(text.toUpperCase(Locale.US));
        t.setTextSize(12);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(pal.sub);
        t.setPadding(dp(14), dp(16), dp(14), dp(4));
        list.addView(t);
    }

    private void sheetNote(LinearLayout list, String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(13);
        t.setTextColor(pal.subText);
        t.setPadding(dp(14), dp(6), dp(14), dp(6));
        list.addView(t);
    }

    private void sheetRow(LinearLayout list, String label, String sub, boolean checked,
                          final Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(11), dp(14), dp(11));
        ripple(row, false);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(16);
        l.setTextColor(pal.text);
        col.addView(l);
        if (sub != null && !sub.isEmpty()) {
            TextView s = new TextView(this);
            s.setText(sub);
            s.setTextSize(12);
            s.setTextColor(pal.subText);
            col.addView(s);
        }
        row.addView(col, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (checked) {
            TextView c = new TextView(this);
            c.setText("\u2713");
            c.setTextSize(18);
            c.setTypeface(Typeface.DEFAULT_BOLD);
            c.setTextColor(pal.accent);
            row.addView(c);
        }
        row.setOnClickListener(v -> {
            dismissSheet();
            if (action != null) ui.post(action);
        });
        list.addView(row);
    }

    /** The three-dot menu: every command and action in one list. */
    private void showCommandsSheet() {
        LinearLayout l = beginSheet("All commands");
        boolean yolo = prefs.getBoolean("yolo", false);

        sheetSection(l, "Project");
        sheetRow(l, "Change project folder", folder(), false, this::changeFolder);
        sheetRow(l, "Continue previous chat", "Next message continues NH's last chat in this folder", false, () -> {
            ChatStore.setContinue(true);
            toast("Next message continues the previous chat.");
        });
        sheetRow(l, "Copy whole chat", "Copies the current chat as text", false, this::copyWholeChat);
        sheetRow(l, "Save folder as ZIP to Download", "Zips the project folder", false, () -> runForeground(S_ZIP));
        sheetRow(l, "Open interactive NH on the server", "Full NH screen (login, /model, /resume)", false,
                () -> runForeground(S_INTERACTIVE));

        sheetSection(l, "Model");
        sheetRow(l, "Select model", modelLabel(), false, this::openModelPicker);
        sheetRow(l, "Show models list", "Prints the NH model list in the chat", false,
                () -> runBackground(S_MODELS_BG, "", false, false));

        sheetSection(l, "Safety");
        sheetRow(l, "Auto-approve all actions", yolo ? "ON (tap to turn off)" : "OFF (tap to turn on)", yolo,
                this::toggleYolo);

        sheetSection(l, "App");
        sheetRow(l, "Settings", "Theme and developer details", false, () -> showSettings(true));
        sheetRow(l, "Show welcome screen", "Replay the intro", false, this::showWelcome);

        sheetSection(l, "Setup (first time)");
        sheetRow(l, "Install guide (step by step)", installStatusText(), false, this::showInstallGuide);
        sheetRow(l, "1. Grant server permission", "", false, this::requestTermuxPermission);
        sheetRow(l, "2. Copy server setup command", "Paste it in the server app, then restart it", false,
                this::copySetupCommand);
        sheetRow(l, "3. Storage permission", "", false, () -> runForeground(S_STORAGE));
        sheetRow(l, "4. Install NH (first time)", "", false, () -> runForeground(S_INSTALL));
        sheetRow(l, "Open server", "", false, this::openTermux);

        sheetSection(l, "Commands inside NH (tap to copy)");
        String[][] refs = {
                {"agy", "Start NH on the server"},
                {"agy -c", "Continue the last chat"},
                {"agy models", "List models"},
                {"agy update", "Update NH"},
                {"agy --version", "Show version"},
                {"/model", "Change model"},
                {"/effort", "Change reasoning effort"},
                {"/resume", "Pick an older chat"},
                {"/plan", "Plan before working"},
                {"/diff", "See what changed"},
                {"/config", "Open settings"},
                {"/help", "List all commands"},
                {"/logout", "Log out"},
                {"/quit", "Close NH"}
        };
        for (final String[] r : refs) {
            sheetRow(l, r[0], r[1], false, () -> {
                copyText("command", r[0]);
                toast("Copied: " + r[0]);
            });
        }
        showSheet();
    }

    /** The + button: quick actions. */
    private void showQuickSheet() {
        LinearLayout l = beginSheet("Quick actions");
        sheetRow(l, "Project folder", folder(), false, this::changeFolder);
        sheetRow(l, "Voice input", "Speak your message", false, this::startVoice);
        sheetRow(l, "Save folder as ZIP to Download", "", false, () -> runForeground(S_ZIP));
        boolean yolo = prefs.getBoolean("yolo", false);
        sheetRow(l, "Auto-approve all actions", yolo ? "ON" : "OFF", yolo, this::toggleYolo);
        showSheet();
    }

    // ------------------------------------------------------------ models

    private List<String> parseModels(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String line : raw.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            t = t.replaceFirst("^[\\-*\u2022\u203A>\u2713\u2714\\s]+", "");
            String[] cols = t.split("\\s{2,}");
            if (cols.length > 1) t = cols[0].trim();
            if (t.isEmpty() || t.endsWith(":") || t.length() > 80) continue;
            if (t.matches("^[=\\-_\\s]+$")) continue;
            if (!out.contains(t)) out.add(t);
            if (out.size() >= 40) break;
        }
        return out;
    }

    private void openModelPicker() {
        if (ChatStore.modelsRaw().isEmpty()) {
            if (fetchModels()) {
                pendingModelPicker = true;
                toast("Loading models...");
            }
        } else {
            showModelSheet();
        }
    }

    private void showModelSheet() {
        LinearLayout l = beginSheet("Select model");
        final String cur = prefs.getString("model", "");
        sheetRow(l, "Default model", "Use whatever NH has selected", cur.isEmpty(), () -> {
            prefs.edit().remove("model").apply();
            render();
        });
        List<String> models = parseModels(ChatStore.modelsRaw());
        String err = ChatStore.modelsError();
        if (models.isEmpty() && !err.isEmpty()) {
            sheetNote(l, "Could not load the list: " + (err.length() > 160 ? err.substring(0, 160) : err));
        }
        for (final String m : models) {
            sheetRow(l, m, "", m.equals(cur), () -> {
                prefs.edit().putString("model", m).apply();
                render();
            });
        }
        sheetRow(l, "Type a model name...", "If your model is not in the list", false, this::askModelName);
        sheetRow(l, ChatStore.modelsLoading() ? "Refreshing..." : "Refresh list from NH",
                "Asks NH for its model list", false, () -> {
                    if (fetchModels()) {
                        pendingModelPicker = true;
                        toast("Loading models...");
                    }
                });
        showSheet();
    }

    private void askModelName() {
        final EditText et = new EditText(this);
        et.setSingleLine(true);
        et.setText(prefs.getString("model", ""));
        new AlertDialog.Builder(this)
                .setTitle("Model name")
                .setMessage("Exactly as NH expects it. See the models list.")
                .setView(et)
                .setPositiveButton("Save", (d, w) -> {
                    String m = et.getText().toString().trim();
                    if (m.isEmpty()) prefs.edit().remove("model").apply();
                    else prefs.edit().putString("model", m).apply();
                    render();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean fetchModels() {
        if (!ready()) return false;
        if (ChatStore.modelsLoading()) return true;
        try {
            Intent i = baseIntent(S_MODELS_BG, "", false, true);
            i.putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", makePending("models"));
            ChatStore.setModelsLoading(true);
            startService(i);
            return true;
        } catch (SecurityException e) {
            ChatStore.setModelsLoading(false);
            toast("Blocked. Finish Setup 2 (allow-external-apps) and restart the server app.");
        } catch (Exception e) {
            ChatStore.setModelsLoading(false);
            toast("Failed: " + e.getMessage());
        }
        return false;
    }

    // ------------------------------------------------------------ actions

    private void toggleYolo() {
        if (prefs.getBoolean("yolo", false)) {
            prefs.edit().putBoolean("yolo", false).apply();
            render();
            toast("Auto-approve OFF.");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Turn on auto-approve?")
                .setMessage("NH will run commands and edit or delete files in your project "
                        + "folder without asking. Use a dedicated folder like ~/projects.")
                .setPositiveButton("Turn on", (d, w) -> {
                    prefs.edit().putBoolean("yolo", true).apply();
                    render();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void changeFolder() {
        final EditText et = new EditText(this);
        et.setSingleLine(true);
        et.setText(folder());
        new AlertDialog.Builder(this)
                .setTitle("Project folder")
                .setMessage("Examples: ~/projects, ~/CalcApp, ~/storage/downloads")
                .setView(et)
                .setPositiveButton("Save", (d, w) -> {
                    prefs.edit().putString("folder", et.getText().toString().trim()).apply();
                    render();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void send() {
        String t = input.getText().toString().trim();
        if (t.isEmpty()) return;
        if (ChatStore.isBusy()) {
            toast("NH is still working.");
            return;
        }
        if (!ready()) return;
        boolean cont = ChatStore.shouldContinue();
        ChatStore.addUser(t, folder());
        String ctx = cont ? "" : ChatStore.contextForNewSession(6000);
        String prompt = ctx.isEmpty() ? t : ctx + "\n\nNew message from the user:\n" + t;
        input.setText("");
        runBackground(S_ASK, prompt, cont, true);
    }

    private void retry() {
        if (ChatStore.isBusy()) {
            toast("NH is still working.");
            return;
        }
        if (!ready()) return;
        String u = ChatStore.lastUserText();
        if (u == null) return;
        ChatStore.popLastAgent();
        boolean cont = ChatStore.shouldContinue();
        String ctx = cont ? "" : ChatStore.contextForNewSession(6000);
        String prompt = ctx.isEmpty() ? u : ctx + "\n\nNew message from the user:\n" + u;
        runBackground(S_ASK, prompt, cont, true);
    }

    private void startVoice() {
        try {
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to NH");
            startActivityForResult(i, REQ_VOICE);
        } catch (ActivityNotFoundException e) {
            toast("Voice input is not available on this phone.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_VOICE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> r = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (r != null && !r.isEmpty()) {
                String cur = input.getText().toString();
                String add = r.get(0);
                String all = cur.isEmpty() ? add : cur + " " + add;
                input.setText(all);
                input.setSelection(all.length());
            }
        }
    }

    private void speak(String raw) {
        String text = raw.replaceAll("[*`#|]", " ");
        if (text.length() > 3900) text = text.substring(0, 3900);
        if (tts != null && ttsReady) {
            if (tts.isSpeaking()) {
                tts.stop();
                return;
            }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "agy");
            return;
        }
        pendingSpeech = text;
        if (tts == null) {
            tts = new TextToSpeech(this, status -> {
                ttsReady = status == TextToSpeech.SUCCESS;
                if (ttsReady && pendingSpeech != null) {
                    tts.speak(pendingSpeech, TextToSpeech.QUEUE_FLUSH, null, "agy");
                    pendingSpeech = null;
                } else if (!ttsReady) {
                    toast("Text-to-speech is not available.");
                }
            });
        }
    }

    // ------------------------------------------------------------ Termux bridge

    private boolean termuxInstalled() {
        try {
            getPackageManager().getPackageInfo(TERMUX_PKG, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean ready() {
        if (!termuxInstalled()) {
            toast("Install the server app (F-Droid) first.");
            return false;
        }
        if (checkSelfPermission(PERM) != PackageManager.PERMISSION_GRANTED) {
            toast("Grant permission first: menu > Setup 1.");
            return false;
        }
        return true;
    }

    private String expand(String p) {
        p = p.trim();
        if (p.isEmpty() || p.equals("~")) return HOME;
        if (p.startsWith("~/")) return HOME + p.substring(1);
        if (!p.startsWith("/")) return HOME + "/" + p;
        return p;
    }

    private Intent baseIntent(String script, String prompt, boolean cont, boolean background) {
        Intent i = new Intent();
        i.setClassName(TERMUX_PKG, "com.termux.app.RunCommandService");
        i.setAction("com.termux.RUN_COMMAND");
        i.putExtra("com.termux.RUN_COMMAND_PATH", BASH);
        i.putExtra("com.termux.RUN_COMMAND_ARGUMENTS",
                new String[]{"-c", script, "agy-chat", expand(folder()), prompt,
                        cont ? "1" : "0",
                        prefs.getBoolean("yolo", false) ? "1" : "0",
                        prefs.getString("model", "")});
        i.putExtra("com.termux.RUN_COMMAND_WORKDIR", HOME);
        i.putExtra("com.termux.RUN_COMMAND_BACKGROUND", background);
        if (!background) i.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        return i;
    }

    private PendingIntent makePending(String kind) {
        Intent ri = new Intent(this, ResultReceiver.class);
        ri.putExtra("kind", kind);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
        return PendingIntent.getBroadcast(
                this, (int) (System.currentTimeMillis() & 0xFFFFFFF), ri, flags);
    }

    private void runBackground(String script, String prompt, boolean cont, boolean markContinue) {
        if (!ready()) return;
        if (ChatStore.isBusy()) {
            toast("NH is still working.");
            return;
        }
        try {
            Intent i = baseIntent(script, prompt, cont, true);
            i.putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", makePending("chat"));
            ChatStore.setBusy(true, markContinue);
            startService(i);
        } catch (SecurityException e) {
            ChatStore.finish("Blocked. Finish Setup 2 (allow-external-apps) "
                    + "and restart the server app.", false);
        } catch (Exception e) {
            ChatStore.finish("Failed to start: " + e.getMessage(), false);
        }
    }

    private void runForeground(String script) {
        if (!ready()) return;
        try {
            startService(baseIntent(script, "", false, false));
        } catch (SecurityException e) {
            toast("Blocked: finish Setup 2 (allow-external-apps) and restart the server app.");
        } catch (Exception e) {
            toast("Failed: " + e.getMessage());
        }
    }

    private void requestTermuxPermission() {
        if (!termuxInstalled()) {
            toast("Install the server app (F-Droid) first.");
            return;
        }
        requestPermissions(new String[]{PERM}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        boolean ok = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
        toast(ok ? "Permission granted." : "Permission denied. Open the server app once, then try again.");
        if (ok) checkInstall();
        updateInstallChips();
        renderGuide();
    }

    private void copySetupCommand() {
        copyText("termux-setup", SETUP_CMD);
        toast("Copied. Paste it in the server app, then restart it.");
        openTermux();
    }

    private void openTermux() {
        Intent i = getPackageManager().getLaunchIntentForPackage(TERMUX_PKG);
        if (i == null) {
            toast("Server app not installed.");
            return;
        }
        startActivity(i);
    }
}
