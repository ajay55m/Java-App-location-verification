package com.app.fourscontracting;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.ViewGroup;

public class LoadingAlert {
    private Activity activity;
    private AlertDialog dialog;

    LoadingAlert(Activity myActivity) {
        this.activity = myActivity;
    }

    /* access modifiers changed from: package-private */
    public void startAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.activity);
        builder.setView(this.activity.getLayoutInflater().inflate(R.layout.dialog_layout, (ViewGroup) null));
        builder.setCancelable(false);
        AlertDialog create = builder.create();
        this.dialog = create;
        create.show();
    }

    /* access modifiers changed from: package-private */
    public void closeAlertDialog() {
        this.dialog.dismiss();
    }
}
