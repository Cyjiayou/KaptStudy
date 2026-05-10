package com.cy.runtime.utils;

import android.os.Bundle;

public class BundleUtils {

    @SuppressWarnings("unchecked")
    public static <T> T get(Bundle bundle, String key) {
        return (T) bundle.get(key);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Bundle bundle, String key, T defaultValue) {
        Object value = bundle.get(key);
        return (value != null) ? (T) value : defaultValue;
    }
}
