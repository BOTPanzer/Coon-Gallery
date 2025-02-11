package com.botpa.turbophotos.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import com.botpa.turbophotos.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.snackbar.Snackbar.SnackbarLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;

public class Orion {

    //Get attribute color
    public static int getColor(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr,  typedValue, true);
        return typedValue.data;
    }

    //Snack bar
    public static void snack(String msg, Activity activity) {
        snack(msg, "ok", null, activity);
    }

    public static void snack(String msg, String btn, Activity activity) {
        snack(msg, btn, null, activity);
    }

    public static void snack(String msg, String btn, Runnable runnable, Activity activity) {
        LinearLayout.LayoutParams objLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        final Snackbar snackbar = Snackbar.make(rootView, "nepe", Snackbar.LENGTH_LONG);

        @SuppressLint("RestrictedApi")
        SnackbarLayout layout = (SnackbarLayout) snackbar.getView();
        layout.setPadding(0,0, 0,0);
        layout.setBackgroundColor(0x00000000);

        LayoutInflater inflter = (LayoutInflater.from(activity));
        View snackView = inflter.inflate(R.layout.snackbar_one, null);

        TextView snackText = snackView.findViewById(R.id.textView);
        snackText.setText(msg);

        TextView textViewOne = snackView.findViewById(R.id.txtOne);
        textViewOne.setText(btn.toUpperCase());
        textViewOne.setOnClickListener(view -> {
            if (runnable != null) runnable.run();
            snackbar.dismiss();
        });

        layout.addView(snackView, objLayoutParams);
        snackbar.show();
    }

    public static void snack2(String msg, String btnCancel, String btnConfirm, Runnable runnable, Activity activity) {
        LinearLayout.LayoutParams objLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        final Snackbar snackbar = Snackbar.make(rootView, "nepe", Snackbar.LENGTH_LONG);

        @SuppressLint("RestrictedApi")
        SnackbarLayout layout = (SnackbarLayout) snackbar.getView();
        layout.setPadding(0,0, 0,0);
        layout.setBackgroundColor(0x00000000);

        LayoutInflater inflter = (LayoutInflater.from(activity));
        View snackView = inflter.inflate(R.layout.snackbar_two, null);

        TextView snackText = snackView.findViewById(R.id.textView);
        snackText.setText(msg);

        TextView textViewOne = snackView.findViewById(R.id.txtOne);
        textViewOne.setText(btnCancel.toUpperCase());
        textViewOne.setOnClickListener(view -> snackbar.dismiss());

        TextView textViewTwo = snackView.findViewById(R.id.txtTwo);
        textViewTwo.setText(btnConfirm.toUpperCase());
        textViewTwo.setOnClickListener(view -> {
            runnable.run();
            snackbar.dismiss();
        });

        layout.addView(snackView, objLayoutParams);
        snackbar.show();
    }

    //Keyboard
    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) view = new View(activity.getApplicationContext());
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void showKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) view = new View(activity.getApplicationContext());
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    //Animations
    public static void hideAnim(View view) {
        hideAnim(view, 150, null);
    }

    public static void hideAnim(View view, int duration) {
        hideAnim(view, duration, null);
    }

    public static void hideAnim(View view, Runnable runnable) {
        hideAnim(view, 150, runnable);
    }

    public static void hideAnim(View view, int duration, Runnable runnable) {
        if (view.getVisibility() == View.GONE) return;

        if (view.getAnimation() != null && !view.getAnimation().hasEnded()) return;

        AlphaAnimation alpha = new AlphaAnimation(1, 0);
        alpha.setDuration(duration);
        view.startAnimation(alpha);
        alpha.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
                if (runnable != null) runnable.run();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }

    public static void showAnim(View view) {
        showAnim(view, 150, null);
    }

    public static void showAnim(View view, int duration) {
        showAnim(view, duration, null);
    }

    public static void showAnim(View view, Runnable runnable) {
        showAnim(view, 150, runnable);
    }

    public static void showAnim(View view, int duration, Runnable runnable) {
        if (view.getVisibility() == View.VISIBLE) return;

        AlphaAnimation alpha = new AlphaAnimation(0, 1);
        alpha.setDuration(duration);
        view.startAnimation(alpha);
        view.setVisibility(View.VISIBLE);
        if (runnable != null) runnable.run();
    }

    public static class ResizeWidthAnimation extends Animation {
        private final int mWidth;
        private final int mStartWidth;
        private final View mView;

        public ResizeWidthAnimation(View view, int width) {
            mView = view;
            mWidth = width;
            mStartWidth = view.getWidth();
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            mView.getLayoutParams().width = mStartWidth + (int) ((mWidth - mStartWidth) * interpolatedTime);
            mView.requestLayout();
        }

        @Override
        public void initialize(int width, int height, int parentWidth, int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }
    }

    public static class ResizeHeightAnimation extends Animation {
        private final int mHeight;
        private final int mStartHeight;
        private final View mView;

        public ResizeHeightAnimation(View view, int height) {
            mView = view;
            mHeight = height;
            mStartHeight = view.getHeight();
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            mView.getLayoutParams().height = mStartHeight + (int) ((mHeight - mStartHeight) * interpolatedTime);
            mView.requestLayout();
        }

        @Override
        public void initialize(int width, int height, int parentWidth, int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }
    }

    //Clipboard
    public static void copyToClip(String str, Activity activity) {
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("", str);
        clipboard.setPrimaryClip(clip);
        Orion.snack("Copied to Clipboard", "Ok", activity);
    }

    //Colors
    public static int lighten(int color, double fraction) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        red = lightenColor(red, fraction);
        green = lightenColor(green, fraction);
        blue = lightenColor(blue, fraction);
        int alpha = Color.alpha(color);
        return Color.argb(alpha, red, green, blue);
    }

    public static int darken(int color, double fraction) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        red = darkenColor(red, fraction);
        green = darkenColor(green, fraction);
        blue = darkenColor(blue, fraction);
        int alpha = Color.alpha(color);

        return Color.argb(alpha, red, green, blue);
    }

    private static int darkenColor(int color, double fraction) {
        return (int) Math.max(color - (color * fraction), 0);
    }

    private static int lightenColor(int color, double fraction) {
        return (int) Math.min(color + (color * fraction), 255);
    }

    //Files
    private static void createFile(String path) {
        int lastSep = path.lastIndexOf(File.separator);
        if (lastSep > 0) {
            String dirPath = path.substring(0, lastSep);
            makeDir(dirPath);
        }

        File file = new File(path);

        try {
            if (!file.exists()) file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void makeDir(String path) {
        if (!existsFile(path)) {
            File file = new File(path);
            file.mkdirs();
        }
    }

    public static String readFile(String path) {
        createFile(path);

        StringBuilder sb = new StringBuilder();
        FileReader fr = null;
        try {
            fr = new FileReader(new File(path));

            char[] buff = new char[1024];
            int length = 0;

            while ((length = fr.read(buff)) > 0) {
                sb.append(new String(buff, 0, length));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();
    }

    public static void writeFile(String path, String str) {
        createFile(path);
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(new File(path), false);
            fileWriter.write(str);
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileWriter != null)
                    fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean writeJSON(File file, JSONObject json) {
        createFile(file.getAbsolutePath());
        FileWriter fileWriter = null;
        boolean success = true;

        try {
            fileWriter = new FileWriter(file, false);
            fileWriter.write(json.toString(2));
            fileWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        } finally {
            try {
                if (fileWriter != null) fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return success;
    }

    public static void deleteFile(File file) {
        if (!file.exists()) return;

        if (file.isFile()) {
            file.delete();
            return;
        }

        File[] fileArr = file.listFiles();

        if (fileArr != null) {
            for (File subFile : fileArr) {
                if (subFile.isDirectory()) {
                    deleteFile(subFile.getAbsolutePath());
                }

                if (subFile.isFile()) {
                    subFile.delete();
                }
            }
        }

        file.delete();
    }

    public static void deleteFile(String path) {
        deleteFile(new File(path));
    }

    public static boolean moveFile(String oldPath, String newPath) {
        File oldFile = new File(oldPath);
        File newFile = new File(newPath);
        return moveFile(oldFile, newFile);
    }

    public static boolean moveFile(File oldFile, File newFile) {
        return oldFile.renameTo(newFile);
    }

    public static boolean existsFile(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static JSONObject loadJSON(String path) {
        try {
            return new JSONObject(readFile(path));
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    //Files: directories
    public static String getAppDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/OrionAssistant/";
    }

    public static String getExternalStorageDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
    }

    //Files: URIs
    public static String convertUriToFilePath(final Context context, final Uri uri) {
        String path = null;

        //IDK, I copied this lol
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    path = Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);

                if (!TextUtils.isEmpty(id)) {
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "");
                    }
                }

                if (id.startsWith("msf:")) {
                    final String[] split = id.split(":");
                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[] { split[1] };
                    path = getDataColumn(context, MediaStore.Downloads.EXTERNAL_CONTENT_URI, selection, selectionArgs);
                } else {
                    final Uri contentUri = ContentUris
                            .withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));

                    path = getDataColumn(context, contentUri, null, null);
                }
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = MediaStore.Audio.Media._ID + "=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                path = getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
            path = getDataColumn(context, uri, null, null);
        } else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
        }

        //Decode path
        if (path != null) {
            try {
                return URLDecoder.decode(path, "UTF-8");
            } catch (Exception e) {
                return null;
            }
        }

        //Didn't work
        path = uri.getPath();
        if (path.contains(":")) return getExternalStorageDir() + path.substring(path.indexOf(":") + 1);

        //All failed
        return null;
    }

    public static Uri getUriFromFile(final Context context, final File file) {
        return FileProvider.getUriForFile(context, "com.com.botpa.turbophotos.FileProvider", file);
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;

        final String column = MediaStore.Images.Media.DATA;
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    //Files: bitmaps
    private static void saveBitmap(Bitmap bitmap, String destPath) {
        FileOutputStream out = null;
        createFile(destPath);
        try {
            out = new FileOutputStream(destPath);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Bitmap getScaledBitmap(String path, int max) {
        Bitmap src = BitmapFactory.decodeFile(path);

        int width = src.getWidth();
        int height = src.getHeight();
        float rate = 0.0f;

        if (width > height) {
            rate = max / (float) width;
            height = (int) (height * rate);
            width = max;
        } else {
            rate = max / (float) height;
            width = (int) (width * rate);
            height = max;
        }

        return Bitmap.createScaledBitmap(src, width, height, true);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int width = options.outWidth;
        final int height = options.outHeight;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampleBitmapFromPath(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    //Focus
    public static void clearFocus(Activity activity) {
        View focus = activity.getCurrentFocus();
        if (focus != null) focus.clearFocus();
    }
}
