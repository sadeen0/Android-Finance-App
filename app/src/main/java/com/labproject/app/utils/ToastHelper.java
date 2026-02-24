package com.labproject.app.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.labproject.app.R;

public class ToastHelper {

    public static void showSuccess(Context context, String message) {
        showCustomToast(context, message, R.layout.custom_toast_success);
    }

    public static void showError(Context context, String message) {
        showCustomToast(context, message, R.layout.custom_toast_error);
    }

    public static void showInfo(Context context, String message) {
        showCustomToast(context, message, R.layout.custom_toast_info);
    }

    private static void showCustomToast(Context context, String message, int layoutId) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(layoutId, null);

        TextView text = layout.findViewById(R.id.toast_message);
        text.setText(message);

        Toast toast = new Toast(context);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
}
