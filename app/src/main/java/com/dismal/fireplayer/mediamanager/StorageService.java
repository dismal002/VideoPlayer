package com.dismal.fireplayer.mediamanager;

import android.content.Context;
import android.os.Environment;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.storage.IMountService;
import android.os.storage.StorageManager;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class StorageService {
    public static final String INTERNAL_DISK = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final String TAG = "StorageService";
    private static StorageService mInstance;
    private final ArrayList<String> mAllDeviceList = new ArrayList<>();
    private final Context mContext;
    private final IMountService mMountService;

    public static StorageService getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new StorageService(context);
        }
        return mInstance;
    }

    private StorageService(Context context) {
        this.mContext = context.getApplicationContext();
        // Try to get IMountService, but it may not be available on non-system apps
        IMountService mountService = null;
        try {
            mountService = IMountService.Stub.asInterface(ServiceManager.getService("mount"));
        } catch (Exception e) {
            Log.w(TAG, "Could not access IMountService (not a system app): " + e.getMessage());
        }
        this.mMountService = mountService;
        try {
            StorageManager storageManager = (StorageManager) this.mContext.getSystemService("storage");
            if (storageManager != null) {
                String[] volumePaths = null;
                try {
                    java.lang.reflect.Method getVolumePaths = storageManager.getClass().getMethod("getVolumePaths");
                    volumePaths = (String[]) getVolumePaths.invoke(storageManager);
                } catch (Exception e) {
                    Log.w(TAG, "getVolumePaths not found, using reflection fallback: " + e.getMessage());
                }
                if (volumePaths != null) {
                    for (String dev : volumePaths) {
                        this.mAllDeviceList.add(dev);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting volume paths: " + e.getMessage());
        }
        if (this.mAllDeviceList.size() == 0) {
            Log.w(TAG, "No volume paths found, using default external storage");
            this.mAllDeviceList.add(INTERNAL_DISK);
        }
    }

    public boolean isDevicePath(String path) {
        if (path == null) {
            return false;
        }
        Iterator<String> it = this.mAllDeviceList.iterator();
        while (it.hasNext()) {
            if (path.equalsIgnoreCase(it.next())) {
                return true;
            }
        }
        return false;
    }


    public boolean isPartitionPath(String path) {
        if (path == null) {
            return false;
        }
        Log.d(TAG, "isPartitionPath():" + path);
        int index = path.lastIndexOf('/');
        if (!isDevicePath(path.substring(0, index))) {
            return false;
        }
        String name = path.substring(index + 1, path.length());
        index = name.lastIndexOf(":");
        if (index == -1 || index == name.length() - 1) {
            return false;
        }
        String major = name.substring(0, index);
        String minor = name.substring(index + 1, name.length());
        try {
            Integer.valueOf(major);
            Integer.valueOf(minor);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isMountedDevice(String path) {
        if (!isDevicePath(path)) {
            return false;
        }
        try {
            String state = this.mMountService.getVolumeState(path);
            if (state != null) {
                return "mounted".equals(state);
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    public ArrayList<String> getAllStorageList() {
        return this.mAllDeviceList;
    }

    public ArrayList<String> getMountedStorageList() {
        ArrayList<String> mountedDevices = new ArrayList<>();
        try {
            Iterator<String> it = this.mAllDeviceList.iterator();
            while (it.hasNext()) {
                String device = it.next();
                // Try to check mount state via IMountService if available
                if (this.mMountService != null) {
                    try {
                        String state = this.mMountService.getVolumeState(device);
                        if ("mounted".equals(state)) {
                            mountedDevices.add(device);
                        }
                    } catch (RemoteException e) {
                        // Fallback: check if directory exists and is readable
                        File deviceFile = new File(device);
                        if (deviceFile.exists() && deviceFile.canRead()) {
                            mountedDevices.add(device);
                        }
                    }
                } else {
                    // Fallback: check if directory exists and is readable
                    File deviceFile = new File(device);
                    if (deviceFile.exists() && deviceFile.canRead()) {
                        mountedDevices.add(device);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting mounted storage list: " + e.getMessage());
            // Fallback: at least return external storage if accessible
            File externalStorage = new File(INTERNAL_DISK);
            if (externalStorage.exists() && externalStorage.canRead()) {
                mountedDevices.add(INTERNAL_DISK);
            }
        }
        return mountedDevices;
    }

    public ArrayList<String> getMountedRootList() {
        ArrayList<String> mountedRoots = new ArrayList<>();
        ArrayList<String> mountedDevices = getMountedStorageList();
        if (mountedDevices == null || mountedDevices.size() <= 0) {
            return null;
        }
        Iterator<String> it = mountedDevices.iterator();
        while (it.hasNext()) {
            String dev = it.next();
            ArrayList<String> partList = getMountedPartitionList(dev);
            if (partList != null) {
                Iterator<String> it2 = partList.iterator();
                while (it2.hasNext()) {
                    mountedRoots.add(String.valueOf(dev) + "/" + it2.next());
                }
            } else {
                mountedRoots.add(dev);
            }
        }
        return mountedRoots;
    }

    public String getRootPath(String path) {
        String rootPath = null;
        if (path != null) {
            Iterator<String> it = getMountedStorageList().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                String device = it.next();
                if (path.contains(device)) {
                    rootPath = device;
                    ArrayList<String> partitionList = getMountedPartitionList(device);
                    if (partitionList != null) {
                        Iterator<String> it2 = partitionList.iterator();
                        while (it2.hasNext()) {
                            String partitionPath = String.valueOf(device) + "/" + it2.next();
                            if (path.contains(partitionPath)) {
                                rootPath = partitionPath;
                            }
                        }
                    }
                }
            }
        }
        return rootPath;
    }

    public ArrayList<String> getPartitionList(String devPath) {
        if (!isMountedDevice(devPath)) {
            return null;
        }
        return getMountedPartitionList(devPath);
    }

    public long[] getStorageMemInfo(String path) {
        if (!isMountedDevice(path) && !isPartitionPath(path)) {
            return null;
        }
        StatFs sf = new StatFs(path);
        long blockSize = (long) sf.getBlockSize();
        return new long[] { blockSize * ((long) sf.getBlockCount()), blockSize * ((long) sf.getAvailableBlocks()) };
    }

    /* access modifiers changed from: package-private */
    public ArrayList<String> getMountedPartitionList(String devPath) {
        ArrayList<String> partitionList = null;
        String[] list = new File(devPath).list();
        if (list != null && list.length > 0 && list.length <= 10) {
            int length = list.length;
            int i = 0;
            while (true) {
                if (i < length) {
                    String listItem = list[i];
                    int index = listItem.lastIndexOf(":");
                    if (index == -1 || index == listItem.length() - 1) {
                        break;
                    }
                    String major = listItem.substring(0, index);
                    String minor = listItem.substring(index + 1, listItem.length());
                    try {
                        Integer.valueOf(major);
                        Integer.valueOf(minor);
                        i++;
                    } catch (NumberFormatException e) {
                    }
                } else {
                    partitionList = new ArrayList<>();
                    for (String listItem2 : list) {
                        partitionList.add(listItem2);
                    }
                }
            }
        }
        return partitionList;
    }
}
