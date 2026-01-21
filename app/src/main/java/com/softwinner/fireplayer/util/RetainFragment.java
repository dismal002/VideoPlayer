package com.softwinner.fireplayer.util;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.util.Log;

public class RetainFragment extends Fragment {
    private static final String TAG = "RetainFragment";
    private Object mObject = null;
    private Object mObjectFKM = null;
    private Object mObjectVFF = null;
    private Object mObjectVTF = null;

    public static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
        RetainFragment mRetainFragment = (RetainFragment) fm.findFragmentByTag(TAG);
        if (mRetainFragment != null) {
            return mRetainFragment;
        }
        Log.v(TAG, "new RetainFragment()");
        RetainFragment mRetainFragment2 = new RetainFragment();
        fm.beginTransaction().add((Fragment) mRetainFragment2, TAG).commit();
        return mRetainFragment2;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void setObject(Object object) {
        this.mObject = object;
    }

    public Object getObject() {
        return this.mObject;
    }
}
