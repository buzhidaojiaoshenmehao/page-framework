package com.usernav.pagedemo;

import android.app.Activity;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.usernav.page.PageBundle;
import com.usernav.page.PageNavigator;
import com.usernav.page.ViewPage;

public final class DemoPages {
    static final String PICK_ACTION = "form.pickCategory";
    private static final String CATEGORY = "category";
    private static final String REVIEWED = "reviewed";
    private static final int PICK_REQUEST = 101;

    private DemoPages() {}

    public abstract static class DemoPage extends ViewPage {
        abstract String title();

        final LinearLayout content(String description) {
            LinearLayout content = DemoUi.content(context());
            DemoUi.title(content, title());
            DemoUi.body(content, description);
            return content;
        }
    }

    public static final class HomePage extends DemoPage {
        private String category = "设计";
        private TextView selection;

        @Override String title() { return "首页"; }

        @Override protected View createView(LayoutInflater inflater, ViewGroup container) {
            LinearLayout content = DemoUi.content(context());
            content.setFocusableInTouchMode(true);
            DemoUi.title(content, "Page 框架");
            DemoUi.body(content, "在一个 Activity 中组织原生 View 页面，统一管理页面栈、返回和生命周期。");

            LinearLayout form = DemoUi.card(content);
            DemoUi.section(form, "表单选择与结果回传");
            DemoUi.body(form, "先编辑草稿，再打开选择页。返回后草稿仍在，选中的分类会更新到表单。");
            EditText draft = new EditText(context());
            draft.setText("周末计划");
            draft.setHint("草稿标题");
            draft.setSingleLine();
            draft.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            DemoUi.add(form, draft, 4);
            selection = DemoUi.body(form, selectionText());
            DemoUi.button(form, "选择分类", () -> {
                PageBundle arguments = new PageBundle();
                arguments.putString(CATEGORY, category);
                startPageForResult(PICK_ACTION, arguments, PICK_REQUEST);
            });

            LinearLayout panel = DemoUi.card(content);
            DemoUi.section(panel, "透明面板");
            DemoUi.body(panel, "用透明 Page 展示筛选说明。面板打开时，底层页面仍可见。");
            DemoUi.button(panel, "打开透明面板", () -> startPage(FilterPanelPage.class, null));

            LinearLayout flow = DemoUi.card(content);
            DemoUi.section(flow, "多步流程与页面复用");
            DemoUi.body(flow, "从编辑进入确认，再回到已有的编辑页。底部页面栈会显示中间页何时出栈。");
            DemoUi.button(flow, "体验多步流程", () -> startPage(EditorPage.class, null));
            content.requestFocus();
            return DemoUi.page(content);
        }

        @Override protected void onResult(int requestCode, int resultCode, PageBundle data) {
            if (requestCode == PICK_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
                category = data.getString(CATEGORY, category);
                selection.setText(selectionText());
            }
        }

        private String selectionText() {
            return "已选分类 · " + category;
        }
    }

    public static final class OptionPickerPage extends DemoPage {
        @Override String title() { return "分类选择"; }

        @Override protected View createView(LayoutInflater inflater, ViewGroup container) {
            String current = arguments().getString(CATEGORY, "设计");
            LinearLayout content = content("当前分类 · " + current + "\n选择新分类后返回表单。");
            LinearLayout options = DemoUi.card(content);
            for (String option : new String[]{"设计", "开发", "生活"}) {
                DemoUi.button(options, option, () -> {
                    PageBundle result = new PageBundle();
                    result.putString(CATEGORY, option);
                    setResult(Activity.RESULT_OK, result);
                    finish();
                });
            }
            DemoUi.button(content, "取消", this::finish);
            return DemoUi.page(content);
        }
    }

    public static final class FilterPanelPage extends DemoPage {
        @Override String title() { return "透明筛选面板"; }

        @Override protected View createView(LayoutInflater inflater, ViewGroup container) {
            FrameLayout root = new FrameLayout(context());
            root.setBackgroundColor(0x520d241e);
            root.setOnClickListener(view -> finish());

            LinearLayout card = content("首页仍保留在这张面板下方。点击关闭、外部区域或系统返回键即可退出。");
            card.setBackground(DemoUi.rounded(context(), Color.WHITE, 18));
            card.setClickable(true);
            DemoUi.button(card, "关闭面板", this::finish);
            ScrollView sheet = new ScrollView(context());
            sheet.setClickable(true);
            sheet.addView(card, new ScrollView.LayoutParams(-1, -2));

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
            int margin = DemoUi.dp(context(), 12);
            params.setMargins(margin, margin, margin, margin);
            root.addView(sheet, params);
            return root;
        }
    }

    public static final class EditorPage extends DemoPage {
        private int quantity = 1;
        private int reviews;

        private TextView status;
        private TextView count;

        @Override String title() { return "编辑订单"; }

        @Override protected View createView(LayoutInflater inflater, ViewGroup container) {
            LinearLayout content = content("调整数量，进入确认页，再选择复用此页。");
            LinearLayout card = DemoUi.card(content);
            DemoUi.section(card, "订单草稿");
            count = DemoUi.body(card, quantityText());
            status = DemoUi.body(card, "接收确认参数 · 0 次");
            DemoUi.button(card, "数量 +1", () -> {
                quantity++;
                count.setText(quantityText());
            });
            DemoUi.button(card, "进入确认页", () -> {
                PageBundle args = new PageBundle();
                args.putInt("quantity", quantity);
                PageNavigator.useDefaultAnimation(args);
                startPage(ReviewPage.class, args);
            });
            DemoUi.button(content, "返回首页", this::finish);
            return DemoUi.page(content);
        }

        @Override protected void onNewArguments(PageBundle args) {
            if (args.getBoolean(REVIEWED, false)) {
                reviews++;
                status.setText("接收确认参数 · " + reviews + " 次");
            }
        }

        private String quantityText() {
            return "商品数量 · " + quantity;
        }
    }

    public static final class ReviewPage extends DemoPage {
        @Override String title() { return "确认订单"; }

        @Override protected View createView(LayoutInflater inflater, ViewGroup container) {
            LinearLayout content = content("SingleTask 会把已有的编辑页带回栈顶，并移除它上方的确认页。");
            LinearLayout card = DemoUi.card(content);
            DemoUi.body(card, "本次确认数量 · " + arguments().getInt("quantity", 1));
            TextView timer = DemoUi.body(card, "计时任务由页面 View 管理，离开后自动取消。");
            viewResources().postDelayed(new Runnable() {
                private int seconds;

                @Override public void run() {
                    timer.setText("已打开 " + (++seconds) + " 秒 · " + state());
                    viewResources().postDelayed(this, 1000);
                }
            }, 1000);
            DemoUi.button(card, "确认并复用编辑页", () -> {
                PageBundle args = new PageBundle();
                args.setFlags(PageBundle.FLAG_LAUNCH_MODE_SINGLE_TASK);
                args.putBoolean(REVIEWED, true);
                startPage(EditorPage.class, args);
            });
            DemoUi.button(content, "返回编辑页（退场动画）", this::finish);
            return DemoUi.page(content);
        }
    }
}
