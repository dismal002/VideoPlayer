package com.softwinner.fireplayer.util;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

public class SimpleNotifyDialog extends Dialog {
    Context mContext;
    int mViewId;

    public SimpleNotifyDialog(Context mContext2) {
        super(mContext2);
        this.mContext = mContext2;
    }

    public SimpleNotifyDialog(Context mContext2, int theme) {
        super(mContext2, theme);
        this.mContext = mContext2;
    }

    public void setView(int id) {
        this.mViewId = id;
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(this.mViewId);
    }
}
