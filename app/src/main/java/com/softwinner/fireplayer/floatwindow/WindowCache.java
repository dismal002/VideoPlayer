package com.softwinner.fireplayer.floatwindow;

import android.util.SparseArray;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WindowCache {
    public Map<Class<? extends FloatWindowService>, SparseArray<Window>> sWindows = new HashMap();

    public boolean isCached(int id, Class<? extends FloatWindowService> cls) {
        return getCache(id, cls) != null;
    }

    public Window getCache(int id, Class<? extends FloatWindowService> cls) {
        SparseArray<Window> l2 = this.sWindows.get(cls);
        if (l2 == null) {
            return null;
        }
        return l2.get(id);
    }

    public void putCache(int id, Class<? extends FloatWindowService> cls, Window window) {
        SparseArray<Window> l2 = this.sWindows.get(cls);
        if (l2 == null) {
            l2 = new SparseArray<>();
            this.sWindows.put(cls, l2);
        }
        l2.put(id, window);
    }

    public void removeCache(int id, Class<? extends FloatWindowService> cls) {
        SparseArray<Window> l2 = this.sWindows.get(cls);
        if (l2 != null) {
            l2.remove(id);
            if (l2.size() == 0) {
                this.sWindows.remove(cls);
            }
        }
    }

    public int getCacheSize(Class<? extends FloatWindowService> cls) {
        SparseArray<Window> l2 = this.sWindows.get(cls);
        if (l2 == null) {
            return 0;
        }
        return l2.size();
    }

    public Set<Integer> getCacheIds(Class<? extends FloatWindowService> cls) {
        SparseArray<Window> l2 = this.sWindows.get(cls);
        if (l2 == null) {
            return new HashSet();
        }
        Set<Integer> keys = new HashSet<>();
        for (int i = 0; i < l2.size(); i++) {
            keys.add(Integer.valueOf(l2.keyAt(i)));
        }
        return keys;
    }

    public int size() {
        return this.sWindows.size();
    }
}
