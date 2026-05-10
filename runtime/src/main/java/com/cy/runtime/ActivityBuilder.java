package com.cy.runtime;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;


public class ActivityBuilder {

    public final static ActivityBuilder INSTANCE = new ActivityBuilder();

    private ActivityBuilder() {}

    public final static String BUILDER_NAME_POSIX = "Builder";

    private Application application;

    public void startActivity(Context context, Intent intent) {
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        context.startActivity(intent);
    }

    Application.ActivityLifecycleCallbacks callbacks = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            performInject(activity, savedInstanceState);
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {

        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {

        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            performSaveState(activity, outState);
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {

        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {

        }
    };

    public void init(Context context) {
        if (this.application != null) return;
        this.application = (Application) context.getApplicationContext();
        this.application.registerActivityLifecycleCallbacks(callbacks);
    }

    private void performInject(Activity activity, Bundle bundle) {

        try {
            // 通过反射调用对应方法完成自动 Inject 和 Get
            Class.forName(activity.getClass().getName() + BUILDER_NAME_POSIX).getDeclaredMethod("inject", Activity.class, Bundle.class).invoke(null, activity, bundle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void performSaveState(Activity activity, Bundle outState) {
        try {
            Class.forName(activity.getClass().getName() + BUILDER_NAME_POSIX).getDeclaredMethod("saveState", Activity.class, Bundle.class).invoke(null, activity, outState);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
