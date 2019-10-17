package com.audiofetch.aflib.bll.app;

import android.annotation.TargetApi;
import android.support.annotation.Nullable;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.audiofetch.afaudiolib.bll.helpers.LG;
import com.audiofetch.aflib.uil.activity.MainActivity;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.NoSuchElementException;


public class ApplicationBase extends Application {
    public static final String TAG = ApplicationBase.class.getSimpleName();

    /**
     * Weak reference to mContext
     */
    private static WeakReference<Context> mContext;

    /**
     * Holds a weak ref to this app instance.
     */
    private static WeakReference<ApplicationBase> mInstance;

    /**
     * Default uncaught exception handler
     */
    private Thread.UncaughtExceptionHandler mDefaultUEH;

    /**
     * Uncaught exception handler
     */
    private Thread.UncaughtExceptionHandler _unCaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {

            Log.e(TAG, "===================================");
            Log.e(TAG, "An uncaught exception has occurred!");
            Log.e(TAG, "===================================");
            ex.printStackTrace();
            Log.e(TAG, "===================================");
            Log.e(TAG, "===================================");

            // re-throw critical exception further to the os (important)
            mDefaultUEH.uncaughtException(thread, ex);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            mDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(_unCaughtExceptionHandler);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                registerLifecycleHandler();
            }
        } catch(Exception ex) {
            Log.e(TAG, "Error", ex);
        } finally {
            ApplicationBase.mInstance = new WeakReference<>(this);
            ApplicationBase.mContext = new WeakReference<>(getApplicationContext());
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void registerLifecycleHandler() {
        registerActivityLifecycleCallbacks(new ApplicationLifecycleHandler());
    }

    /**
     * Kills the app, used by service class, notification media controls.
     */
    public void finish() {
        boolean handled = false;
        try {
            MainActivity ma = (MainActivity) MainActivity.getInstance();
            if (null != ma) {
                handled = true;
                ma.exitApplicationClearHistory();
            }

        } catch(ClassCastException ex) {
            Log.e(TAG, "Cannot close main activity", ex);
        } catch(Exception ex) {
            Log.e(TAG, "Cannot close main activity", ex);
        } finally {
            if (!handled) {
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }
    }

    /**
     * Returns the application instance
     *
     * @return
     */
    @Nullable
    public static ApplicationBase getInstance() {
        return (null != mInstance) ? mInstance.get() : null;
    }

    /**
     * Allows application mContext to be retrieved anywhere in the app
     *
     * @return
     */
    @Nullable
    public static Context getAppContext() {
        Context ctx = null;
        if (null != ApplicationBase.mContext) {
            ctx = ApplicationBase.mContext.get();
        }
        return ctx;
    }


    public static boolean supportsTelephony() {
        PackageManager pm = getAppContext().getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    /**
     * Retrieves the users current coarse location
     */
    public static double[] getUsersLocation() {
        double lat = 0, lng = 0;
        try {
            LocationManager lm = (LocationManager) getAppContext().getSystemService(Context.LOCATION_SERVICE);
            Criteria c = new Criteria();
            c.setAccuracy(Criteria.ACCURACY_COARSE);

            // new requirement as of SDK 23 to check for permission before asking for location
            boolean permissionGranted = true;
            final Context context = ApplicationBase.getAppContext();
            if ( Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionGranted = false;
            }
            if (permissionGranted) {
                Location location = lm.getLastKnownLocation(lm.getBestProvider(c, true));
                if (null != location) {
                    lng = location.getLongitude();
                    lat = location.getLatitude();
                }
            }
        }
        catch (SecurityException ex) {
            ex.printStackTrace();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return new double[]{lat, lng};
    }

    @SuppressWarnings("deprecation")
    public static synchronized String getLocalAddress() {
        Context cntxt = ApplicationBase.getAppContext();
        ConnectivityManager connectivityManager = (ConnectivityManager) cntxt.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (null != activeNetworkInfo) {
            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                WifiManager myWifiManager = (WifiManager) cntxt.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo myWifiInfo = myWifiManager.getConnectionInfo();
                int ipAddress = myWifiInfo.getIpAddress();

                return android.text.format.Formatter.formatIpAddress(ipAddress);
            } else if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                try {
                    Enumeration<NetworkInterface> interfs = NetworkInterface.getNetworkInterfaces();
                    NetworkInterface activeInterf = null;
                    String hostName = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getName();
                    InetAddress ret = null;

                    while (interfs.hasMoreElements()) {
                        try {
                            activeInterf = interfs.nextElement();
                            if (activeInterf.isLoopback()) {
                                continue;
                            } else if (!hostName.equalsIgnoreCase(activeInterf.getDisplayName())) {
                                ret = activeInterf.getInetAddresses().nextElement();
                                break;
                            }
                        } catch (NoSuchElementException e) {
                            e.printStackTrace();
                            continue;
                        }
                    }
                    return ret.getHostAddress();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     *
     * @param ctx
     */
    public static void  restartApp(Context ctx){
        if (null != ctx) {
            Intent intent = new Intent(ctx, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent, 0);
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if(Build.VERSION.SDK_INT >= 19 ) {
                am.setExact(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 500, pi);
            }else{
                am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 500, pi);
            }
            
            killApp();
        }
    }

    public static void restartApp() {
        restartApp(getAppContext());
    }

    /**
     * Does what it says
     */
    public static void killApp() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }


    public static void showDeviceHomeScreen() {
        final Context ctx = getAppContext();
        if (null != ctx) {
            final Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            ctx.startActivity(startMain);
        }
    }
}

