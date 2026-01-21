package android.os.storage;

import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

public interface IMountService extends IInterface {
    public static abstract class Stub {
        public static IMountService asInterface(IBinder obj) {
            return null;
        }
    }

    String getVolumeState(String mountPoint) throws RemoteException;
}
