package com.nathanhaze.gifcreator;

import android.graphics.Bitmap;

import com.zomato.photofilters.imageprocessors.Filter;

public class JavaUtil {

    // private field that refers to the object
    private static JavaUtil singleObject;

    private JavaUtil() {
        // constructor of the SingletonExample class
    }

    public static JavaUtil getInstance() {
        return singleObject;
    }

    public static Bitmap doFilter(Filter filter, Bitmap bitmap) {
        return filter.processFilter(bitmap);
    }
}
