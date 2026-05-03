package com.fridge.caps.views;


/**
 * Purpose: Handles screen flow, UI state coordination, and user interactions.
 * Depends on: Android UI toolkit, app controllers/viewmodels, and navigation intents.
 * Notes: Focuses on presentation logic while delegating business rules to controllers.
 */
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.fridge.caps.R;

public final class BottomNavUi {

    private BottomNavUi() {}

    public static void applyStudentNav(AppCompatActivity activity, int selectedNavItemId) {
        int[] ids = {
                R.id.navHome, R.id.navCounsel, R.id.navAppts, R.id.navAlerts, R.id.navProfile
        };
        for (int id : ids) {
            View v = activity.findViewById(id);
            if (v instanceof LinearLayout) {
                styleRow((LinearLayout) v, id == selectedNavItemId);
            }
        }
    }

    public static void applyCounselorNav(AppCompatActivity activity, int selectedNavItemId) {
        int[] ids = {
                R.id.navHome, R.id.navCounsel, R.id.navAppts, R.id.navAlerts, R.id.navProfile
        };
        for (int id : ids) {
            View v = activity.findViewById(id);
            if (v instanceof LinearLayout) {
                styleRow((LinearLayout) v, id == selectedNavItemId);
            }
        }
    }

    private static void styleRow(LinearLayout row, boolean selected) {
        int dark = ContextCompat.getColor(row.getContext(), R.color.caps_palette_neutral_dark);
        int muted = ContextCompat.getColor(row.getContext(), R.color.caps_palette_grey_warm);
        int color = selected ? dark : muted;
        for (int i = 0; i < row.getChildCount(); i++) {
            View c = row.getChildAt(i);
            if (c instanceof TextView) {
                TextView t = (TextView) c;
                t.setTextColor(color);
                t.setTypeface(Typeface.SANS_SERIF, selected ? Typeface.BOLD : Typeface.NORMAL);
            } else if (c instanceof ImageView) {
                ((ImageView) c).setImageTintList(ColorStateList.valueOf(color));
            } else if (c instanceof ViewGroup) {
                tintImageViewsDeep((ViewGroup) c, color);
            }
        }
    }

    private static void tintImageViewsDeep(ViewGroup group, int color) {
        ColorStateList list = ColorStateList.valueOf(color);
        for (int i = 0; i < group.getChildCount(); i++) {
            View c = group.getChildAt(i);
            if (c instanceof ImageView) {
                ((ImageView) c).setImageTintList(list);
            } else if (c instanceof ViewGroup) {
                tintImageViewsDeep((ViewGroup) c, color);
            }
        }
    }
}
