/*
 * This file is part of Doodle Android.
 *
 * Doodle Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Doodle Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Doodle Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019-2023 by Patrick Zedler
 */

package xyz.zedler.patrick.doodle.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.text.HtmlCompat;
import androidx.core.widget.TextViewCompat;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.divider.MaterialDivider;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;
import xyz.zedler.patrick.doodle.Constants.DEF;
import xyz.zedler.patrick.doodle.Constants.PREF;
import xyz.zedler.patrick.doodle.R;
import xyz.zedler.patrick.doodle.util.PrefsUtil;
import xyz.zedler.patrick.doodle.util.ResUtil;
import xyz.zedler.patrick.doodle.util.UiUtil;
import xyz.zedler.patrick.doodle.util.ViewUtil;

public class FormattedTextView extends LinearLayout {

  private final Context context;

  public FormattedTextView(Context context) {
    super(context);
    this.context = context;
    init();
  }

  public FormattedTextView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    this.context = context;
    init();
  }

  private void init() {
    setOrientation(VERTICAL);
    int padding = UiUtil.dpToPx(context, 16);
    setPadding(0, padding, 0, 0);
  }

  public void setText(String text, String... highlights) {
    removeAllViews();

    String[] parts = text.split("\n\n");
    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
      String partNext = i < parts.length - 1 ? parts[i + 1] : "";

      for (String highlight : highlights) {
        part = part.replaceAll(highlight, "<b>" + highlight + "</b>");
      }
      part = part.replaceAll("\n", "<br/>");

      if (part.startsWith("#")) {
        String[] h = part.split(" ");
        addView(getHeadline(h[0].length(), part.substring(h[0].length() + 1)));
      } else if (part.startsWith("- ")) {
        String[] bullets = part.trim().split("- ");
        for (int index = 1; index < bullets.length; index++) {
          addView(getBullet(bullets[index], index == bullets.length - 1));
        }
      } else if (part.startsWith("? ")) {
        addView(getMessage(part.substring(2), false));
      } else if (part.startsWith("! ")) {
        addView(getMessage(part.substring(2), true));
      } else if (part.startsWith("> ") || part.startsWith("=> ")) {
        String[] link = part.substring(part.startsWith("> ") ? 2 : 3).trim().split(" ");
        addView(link.length == 1 ? getLink(link[0], link[0]) : getLink(link[0], link[1]));
      } else if (part.startsWith("---")) {
        addView(getDivider());
      } else if (part.startsWith("OPTION_USE_SLIDING")) {
        View optionTransition = inflate(context, R.layout.partial_option_transition, null);
        optionTransition.setBackground(ViewUtil.getRippleBgListItemSurface(context));
        optionTransition.setLayoutParams(getVerticalLayoutParams(0, 16));
        MaterialSwitch toggle = optionTransition.findViewById(R.id.switch_other_transition);
        optionTransition.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        SharedPreferences sharedPrefs = new PrefsUtil(context).getSharedPrefs();
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
          ViewUtil.startIcon(optionTransition.findViewById(R.id.image_other_transition));
          sharedPrefs.edit().putBoolean(PREF.USE_SLIDING, isChecked).apply();
        });
        toggle.setChecked(sharedPrefs.getBoolean(PREF.USE_SLIDING, DEF.USE_SLIDING));
        addView(optionTransition);
      } else {
        boolean keepDistance = !partNext.startsWith("=> ");
        addView(getParagraph(part, keepDistance));
      }
    }
  }

  private MaterialTextView getParagraph(String text, boolean keepDistance) {
    MaterialTextView textView = new MaterialTextView(
        new ContextThemeWrapper(context, R.style.Widget_Doodle_TextView_Paragraph),
        null,
        0
    );
    textView.setLayoutParams(getVerticalLayoutParams(16, keepDistance ? 16 : 0));
    textView.setTextColor(ResUtil.getColorAttr(context, R.attr.colorOnBackground));
    textView.setText(HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY));
    return textView;
  }

  private MaterialTextView getHeadline(int h, String title) {
    MaterialTextView textView = new MaterialTextView(
        new ContextThemeWrapper(context, R.style.Widget_Doodle_TextView), null, 0
    );
    textView.setLayoutParams(getVerticalLayoutParams(16, 16));
    textView.setText(HtmlCompat.fromHtml(title, HtmlCompat.FROM_HTML_MODE_LEGACY));
    int resId;
    switch (h) {
      case 1:
        resId = R.style.TextAppearance_Doodle_HeadlineLarge;
        break;
      case 2:
        resId = R.style.TextAppearance_Doodle_HeadlineMedium;
        break;
      case 3:
        resId = R.style.TextAppearance_Doodle_HeadlineSmall;
        break;
      case 4:
        resId = R.style.TextAppearance_Doodle_TitleLarge;
        break;
      default:
        resId = R.style.TextAppearance_Doodle_TitleMedium;
        break;
    }
    TextViewCompat.setTextAppearance(textView, resId);
    textView.setTextColor(ResUtil.getColorAttr(context, R.attr.colorOnBackground));
    return textView;
  }

  private MaterialTextView getLink(String text, String link) {
    MaterialTextView textView = new MaterialTextView(
        new ContextThemeWrapper(context, R.style.Widget_Doodle_TextView_LabelLarge),
        null,
        0
    );
    textView.setLayoutParams(getVerticalLayoutParams(16, 16));
    textView.setTextColor(ResUtil.getColorAttr(context, R.attr.colorPrimary));
    textView.setText(text);
    textView.setOnClickListener(
        v -> context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)))
    );
    return textView;
  }

  private View getDivider() {
    MaterialDivider divider = new MaterialDivider(context);
    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
        UiUtil.dpToPx(context, 56), ViewGroup.LayoutParams.WRAP_CONTENT
    );
    layoutParams.setMargins(
        0, UiUtil.dpToPx(context, 8), 0, UiUtil.dpToPx(context, 24)
    );
    divider.setLayoutParams(layoutParams);
    return divider;
  }

  private LinearLayout getBullet(String text, boolean isLast) {
    int bulletSize = UiUtil.dpToPx(context, 4);

    View viewBullet = new View(context);
    FrameLayout.LayoutParams paramsBullet = new FrameLayout.LayoutParams(bulletSize, bulletSize);
    paramsBullet.rightMargin = UiUtil.dpToPx(context, 6);
    paramsBullet.leftMargin = UiUtil.dpToPx(context, 6);
    paramsBullet.gravity = Gravity.CENTER_VERTICAL;
    viewBullet.setLayoutParams(paramsBullet);

    GradientDrawable shape = new GradientDrawable();
    shape.setShape(GradientDrawable.OVAL);
    shape.setSize(bulletSize, bulletSize);
    shape.setColor(ResUtil.getColorAttr(context, R.attr.colorOnBackground));
    viewBullet.setBackground(shape);

    MaterialTextView textViewHeight = new MaterialTextView(
        new ContextThemeWrapper(context, R.style.Widget_Doodle_TextView), null, 0
    );
    textViewHeight.setLayoutParams(
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
    );
    textViewHeight.setText("E");
    textViewHeight.setVisibility(INVISIBLE);

    FrameLayout frameLayout = new FrameLayout(context);
    frameLayout.setLayoutParams(
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
    );
    frameLayout.addView(viewBullet);
    frameLayout.addView(textViewHeight);

    MaterialTextView textView = new MaterialTextView(
        new ContextThemeWrapper(context, R.style.Widget_Doodle_TextView), null, 0
    );
    LinearLayout.LayoutParams paramsText = new LinearLayout.LayoutParams(
        0, ViewGroup.LayoutParams.WRAP_CONTENT
    );
    paramsText.weight = 1;
    textView.setLayoutParams(paramsText);

    if (text.trim().endsWith("<br/>")) {
      text = text.trim().substring(0, text.length() - 5);
    }
    textView.setText(HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY));


    LinearLayout linearLayout = new LinearLayout(context);
    linearLayout.setLayoutParams(
        getVerticalLayoutParams(16, isLast ? 16 : 8)
    );

    linearLayout.addView(frameLayout);
    linearLayout.addView(textView);
    return linearLayout;
  }

  private MaterialCardView getMessage(String text, boolean useErrorColors) {
    int colorSurface = ResUtil.getColorAttr(
        context, useErrorColors ? R.attr.colorErrorContainer : R.attr.colorSurfaceVariant
    );
    int colorOnSurface = ResUtil.getColorAttr(
        context, useErrorColors ? R.attr.colorOnErrorContainer : R.attr.colorOnSurfaceVariant
    );
    MaterialCardView cardView = new MaterialCardView(context);
    cardView.setLayoutParams(getVerticalLayoutParams(16, 16));
    int padding = UiUtil.dpToPx(context, 16);
    cardView.setContentPadding(padding, padding, padding, padding);
    cardView.setCardBackgroundColor(colorSurface);
    cardView.setStrokeWidth(0);
    cardView.setRadius(padding);

    MaterialTextView textView = getParagraph(text, false);
    textView.setLayoutParams(getVerticalLayoutParams(0, 0));
    textView.setTextColor(colorOnSurface);
    cardView.addView(textView);
    return cardView;
  }

  private LinearLayout.LayoutParams getVerticalLayoutParams(int side, int bottom) {
    int pxSide = UiUtil.dpToPx(context, side);
    int pxBottom = UiUtil.dpToPx(context, bottom);
    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    );
    layoutParams.setMargins(pxSide, 0, pxSide, pxBottom);
    return layoutParams;
  }
}
