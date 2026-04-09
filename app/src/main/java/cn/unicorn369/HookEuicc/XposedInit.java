package cn.unicorn369.HookEuicc;

import android.app.Activity;
import android.app.AndroidAppHelper;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.pm.ResolveInfo;

import android.telephony.euicc.DownloadableSubscription;
import android.telephony.euicc.EuiccManager;
import android.telephony.TelephonyManager;

import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedInit implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static final String Title = "eSIM Code";

    private static String initActivationCode = "";
    private static Activity activity;

    private static final String PREF_NAME = "conf";
    private static final String KEY_ENABLE_HOOK = "enable_hook";
    private static final String KEY_NO_EUICC = "no_euicc";
    private static final String KEY_BYPASS_OMAPI = "bypass_omapi";

    private XSharedPreferences prefs;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, "conf");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        
        boolean enableHook = prefs.getBoolean(KEY_ENABLE_HOOK, true);
        boolean noEuicc = prefs.getBoolean(KEY_NO_EUICC, false);
        boolean bypassOmapi = prefs.getBoolean(KEY_BYPASS_OMAPI, false);

        if (noEuicc && lpparam.packageName.equals("com.android.phone")) {
            XposedHelpers.findAndHookMethod(
                "com.android.internal.telephony.uicc.UiccSlot", 
                lpparam.classLoader, "isEuicc", 
                XC_MethodReplacement.returnConstant(false)
            );
            XposedBridge.log("HookEuicc-bypassOmapi 已启用: " + lpparam.packageName);
        }

        if (bypassOmapi && lpparam.packageName.equals("com.android.se")) {
            XposedHelpers.findAndHookMethod("com.android.se.security.AccessControlEnforcer",
            lpparam.classLoader, "readSecurityProfile",
            new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    XposedHelpers.setBooleanField(param.thisObject, "mUseArf", false);
                    XposedHelpers.setBooleanField(param.thisObject, "mUseAra", false);
                    XposedHelpers.setBooleanField(param.thisObject, "mFullAccess", true);
                    return null;
                }
            });
            XposedBridge.log("HookEuicc-bypassOmapi 已启用: " + lpparam.packageName);
        }

        if (!enableHook) {
            XposedBridge.log("HookEuicc 已禁用: " + lpparam.packageName);
            return;
        }
        //Class
        Class<?> packageManagerClass = XposedHelpers.findClass("android.app.ApplicationPackageManager", lpparam.classLoader);

        //Activity
        XposedHelpers.findAndHookMethod(
            Activity.class, "onCreate", "android.os.Bundle",
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    activity = (Activity) param.thisObject;
                }
            }
        );

        //伪装支持eSIM
        XposedHelpers.findAndHookMethod(
            packageManagerClass, "hasSystemFeature", String.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String feature = (String) param.args[0];
                    if (feature.equals(PackageManager.FEATURE_TELEPHONY_EUICC)) {
                        param.setResult(true);
                    }
                }
            }
        );
        XposedHelpers.findAndHookMethod(
            packageManagerClass, "hasSystemFeature", String.class, int.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String feature = (String) param.args[0];
                    if (feature.equals(PackageManager.FEATURE_TELEPHONY_EUICC)) {
                        param.setResult(true);
                    }
                }
            }
        );
        XposedHelpers.findAndHookMethod(
            EuiccManager.class, "isEnabled",
            XC_MethodReplacement.returnConstant(true)
        );

        //获取eSIM激活码
        XposedHelpers.findAndHookMethod(
            DownloadableSubscription.class, "forActivationCode", String.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String activationCode = (String) param.args[0];
                    if (activationCode != null) {
                        XposedBridge.log("HookEuicc-eSIM Code: " + activationCode);
                        shareCode(activationCode);
                    }
                }
            }
        );

        //Hook LPA
        XposedHelpers.findAndHookMethod(
            DownloadableSubscription.class, "getEncodedActivationCode",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String activationCode = (String) param.getResult();
                    if (activationCode != null) {
                        XposedBridge.log("HookEuicc-eSIM Code: " + activationCode);
                        shareCode(activationCode);
                    }
                }
            }
        );

        //其他检测
        XposedHelpers.findAndHookMethod(
            packageManagerClass, "queryIntentServices", Intent.class, int.class,
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Intent intent = (Intent) param.args[0];
                    if (intent != null && intent.getAction().equals("android.service.euicc.EuiccService")) {
                        List<ResolveInfo> originalList = (List<ResolveInfo>) param.getResult();
                        if (originalList == null || originalList.isEmpty()) {
                            //注入伪造的ResolveInfo
                            List<ResolveInfo> fakeList = new ArrayList<>();
                            fakeList.add(createFakeResolveInfo());
                            param.setResult(fakeList);
                        }
                    }
                }
            }
        );
        XposedHelpers.findAndHookMethod(
            TelephonyManager.class, "getCardIdForDefaultEuicc",
            XC_MethodReplacement.returnConstant(0)
        );
    }

    private void shareCode(String activationCode) {
        Context context = AndroidAppHelper.currentApplication().getApplicationContext();
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        //复制到剪切板
        if (clipboardManager != null) {
            ClipData clipdata = ClipData.newPlainText(Title, activationCode);
            clipboardManager.setPrimaryClip(clipdata);
        }
        //发送激活码
        if (initActivationCode != activationCode) {
            initActivationCode = activationCode;
            //
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, activationCode);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shareIntent = Intent.createChooser(shareIntent, Title);
            //部分应用可能会出现错误(捂脸)
            try {
                context.startActivity(shareIntent);
            } catch (Exception e) {
                activity.startActivity(shareIntent);
            }
            try {
                Toast.makeText(context, "已复制到剪切板\neSIM激活码：" + activationCode, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(activity, "已复制到剪切板\neSIM激活码：" + activationCode, Toast.LENGTH_LONG).show();
            }
        }
    }

    //构造伪造的ResolveInfo
    private ResolveInfo createFakeResolveInfo() {
        ResolveInfo fakeInfo = new ResolveInfo();
        fakeInfo.serviceInfo = new ServiceInfo();
        fakeInfo.serviceInfo.packageName = BuildConfig.APPLICATION_ID;
        fakeInfo.serviceInfo.name = "HookEuiccService";
        fakeInfo.serviceInfo.permission = "android.permission.BIND_EUICC_SERVICE";
        return fakeInfo;
    }
}
