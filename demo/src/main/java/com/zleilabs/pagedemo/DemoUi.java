package com.zleilabs.pagedemo;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class DemoUi {
    static final int SURFACE = 0xfff3f6f4;
    static final int DIAGNOSTIC = 0xffe1eeea;
    static final int INK = 0xff16302b;
    private static final int MUTED = 0xff455f57;
    private static final int ACCENT = 0xff116450;

    private DemoUi() {}

    static int dp(Context context, int size) {
        return Math.round(size * context.getResources().getDisplayMetrics().density);
    }

    static LinearLayout column(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    static LinearLayout content(Context context) {
        LinearLayout layout = column(context);
        int inset = dp(context, 20);
        layout.setPadding(inset, inset, inset, inset);
        return layout;
    }

    static ScrollView page(LinearLayout content) {
        ScrollView scroll = new ScrollView(content.getContext());
        scroll.setBackgroundColor(SURFACE);
        scroll.setFillViewport(true);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        return scroll;
    }

    static LinearLayout card(LinearLayout parent) {
        Context context = parent.getContext();
        LinearLayout card = column(context);
        int inset = dp(context, 16);
        card.setPadding(inset, inset, inset, inset);
        card.setBackground(rounded(context, Color.WHITE, 14));
        add(parent, card, 14);
        return card;
    }

    static TextView text(Context context, String value, int size, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLineSpacing(dp(context, 2), 1f);
        return view;
    }

    static void title(LinearLayout parent, String value) {
        TextView title = text(parent.getContext(), value, 26, INK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        add(parent, title, 0);
    }

    static void section(LinearLayout parent, String value) {
        TextView title = text(parent.getContext(), value, 17, INK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        add(parent, title, 0);
    }

    static TextView body(LinearLayout parent, String value) {
        TextView text = text(parent.getContext(), value, 15, MUTED);
        add(parent, text, 6);
        return text;
    }

    static void button(LinearLayout parent, String label, Runnable action) {
        Context context = parent.getContext();
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextColor(ACCENT);
        button.setTextSize(15);
        button.setMinHeight(dp(context, 48));
        button.setMinimumHeight(dp(context, 48));
        button.setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x24116450),
                rounded(context, DIAGNOSTIC, 10), null));
        button.setOnClickListener(view -> action.run());
        add(parent, button, 8);
    }

    static void add(LinearLayout parent, View view, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(parent.getContext(), top);
        parent.addView(view, params);
    }

    static GradientDrawable rounded(Context context, int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(context, radius));
        return shape;
    }
}
