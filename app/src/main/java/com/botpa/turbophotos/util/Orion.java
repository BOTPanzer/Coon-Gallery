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
import android.util.Log;
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
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Orion {

    //Get attribute color
    public static int getColor(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr,  typedValue, true);
        return typedValue.data;
    }

    //Snack bar
    public static void snack(Activity activity, String msg) {
        snack(activity, msg, "ok", null);
    }

    public static void snack(Activity activity, String msg, String btn) {
        snack(activity, msg, btn, null);
    }

    public static void snack(Activity activity, String msg, String btn, Runnable runnable) {
        LinearLayout.LayoutParams objLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        final Snackbar snackbar = Snackbar.make(rootView, "nepe", Snackbar.LENGTH_LONG);

        @SuppressLint("RestrictedApi")
        SnackbarLayout layout = (SnackbarLayout) snackbar.getView();
        layout.setPadding(0,0, 0,0);
        layout.setBackgroundColor(0x00000000);

        LayoutInflater inflater = (LayoutInflater.from(activity));
        View snackView = inflater.inflate(R.layout.snackbar_one, null);

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

    public static void snack2(Activity activity, String msg, String btnCancel, String btnConfirm, Runnable runnable) {
        LinearLayout.LayoutParams objLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        final Snackbar snackbar = Snackbar.make(rootView, "nepe", Snackbar.LENGTH_LONG);

        @SuppressLint("RestrictedApi")
        SnackbarLayout layout = (SnackbarLayout) snackbar.getView();
        layout.setPadding(0,0, 0,0);
        layout.setBackgroundColor(0x00000000);

        LayoutInflater inflater = (LayoutInflater.from(activity));
        View snackView = inflater.inflate(R.layout.snackbar_two, null);

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

    public static void snack2(Activity activity, String msg, String btn1, Runnable runnable1, String btn2, Runnable runnable2) {
        snack2(activity, msg, btn1, runnable1, btn2, runnable2, Snackbar.LENGTH_LONG);
    }

    public static void snack2(Activity activity, String msg, String btn1, Runnable runnable1, String btn2, Runnable runnable2, int length) {
        LinearLayout.LayoutParams objLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        final Snackbar snackbar = Snackbar.make(rootView, "nepe", length);

        @SuppressLint("RestrictedApi")
        SnackbarLayout layout = (SnackbarLayout) snackbar.getView();
        layout.setPadding(0,0, 0,0);
        layout.setBackgroundColor(0x00000000);

        LayoutInflater inflater = (LayoutInflater.from(activity));
        View snackView = inflater.inflate(R.layout.snackbar_two, null);

        TextView snackText = snackView.findViewById(R.id.textView);
        snackText.setText(msg);

        TextView textViewOne = snackView.findViewById(R.id.txtOne);
        textViewOne.setText(btn1.toUpperCase());
        textViewOne.setOnClickListener(view -> {
            runnable1.run();
            snackbar.dismiss();
        });

        TextView textViewTwo = snackView.findViewById(R.id.txtTwo);
        textViewTwo.setText(btn2.toUpperCase());
        textViewTwo.setOnClickListener(view -> {
            runnable2.run();
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
        Orion.snack(activity, "Copied to Clipboard");
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
    private static Charset detectCharset(Path path) {
        Charset charset = StandardCharsets.UTF_8;
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(path.toFile());
            byte[] bytes = new byte[4096]; // Read a chunk of bytes
            int bytesRead = fis.read(bytes);
            charset = bytesRead == -1 ? StandardCharsets.UTF_8 : detectCharsetFromBytes(bytes, bytesRead);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return charset;
    }

    private static Charset detectCharsetFromBytes(byte[] bytes, int length) {
        // Simple BOM detection (UTF-8, UTF-16, UTF-32)
        if (length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return StandardCharsets.UTF_8; // UTF-8 BOM
        } else if (length >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
            return StandardCharsets.UTF_16BE; // UTF-16BE BOM
        } else if (length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
            return StandardCharsets.UTF_16LE; // UTF-16LE BOM
        }/* else if (length >= 4 && bytes[0] == (byte) 0x00 && bytes[1] == (byte) 0x00 && bytes[2] == (byte) 0xFE && bytes[3] == (byte) 0xFF) {
            return "UTF-32BE"; // UTF-32BE BOM
        } else if (length >= 4 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE && bytes[2] == (byte) 0x00 && bytes[3] == (byte) 0x00) {
            return "UTF-32LE"; // UTF-32LE BOM
        }*/

        return StandardCharsets.UTF_8; //default if unable to detect.
    }

    public static String readFile(File file) {
        Path path = file.toPath();
        Charset charset = detectCharset(path);
        Reader reader = null;

        StringBuilder sb = new StringBuilder();

        try {
            reader = new BufferedReader(new InputStreamReader(Files.newInputStream(path), charset));

            char[] buff = new char[1024];
            int length;

            while ((length = reader.read(buff)) > 0) {
                sb.append(new String(buff, 0, length));
            }
        } catch (IOException e) {
            String message = e.getMessage();
            if (message != null) Log.e("Read file", message);
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (Exception e) {
                String message = e.getMessage();
                if (message != null) Log.e("Read file (2)", message);
            }
        }

        return sb.toString();
    }

    /*public static String readFile(File file) {
        StringBuilder sb = new StringBuilder();
        Reader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath())));

            char[] buff = new char[1024];
            int length;

            while ((length = reader.read(buff)) > 0) {
                sb.append(new String(buff, 0, length));
            }
        } catch (IOException e) {
            String message = e.getMessage();
            if (message != null) Log.e("Read file", message);
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (Exception e) {
                String message = e.getMessage();
                if (message != null) Log.e("Read file (2)", message);
            }
        }

        return sb.toString();
    }*/

    public static boolean writeFile(File file, String data) {
        Writer writer = null;
        boolean success = true;

        try {
            writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_16));
            writer.write(data);
        } catch (IOException e) {
            String message = e.getMessage();
            if (message != null) Log.e("Write file", message);
            success = false;
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (Exception e) {
                String message = e.getMessage();
                if (message != null) Log.e("Write file (2)", message);
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
                    deleteFile(new File(subFile.getAbsolutePath()));
                }

                if (subFile.isFile()) {
                    subFile.delete();
                }
            }
        }

        file.delete();
    }

    public static boolean moveFile(File oldFile, File newFile) {
        return oldFile.renameTo(newFile);
    }

    public static boolean existsFile(String path) {
        File file = new File(path);
        return file.exists();
    }

    //Files: JSON
    public static boolean writeJson(File file, JsonObject json) {
        return writeFile(file, json.toString());
    }

    public static JsonObject loadJson(File file) {
        return loadJson(readFile(file));
    }

    public static JsonObject loadJson(String json) {
        /*Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement gsonElem = gson.fromJson(readFile(path), JsonElement.class);
        return new gsonElem.getAsJsonObject();*/
        JsonObject obj = new Gson().fromJson(json, JsonObject.class);
        return obj == null ? new JsonObject() : obj;
    }

    public static JsonArray arrayToJson(String[] array) {
        JsonArray jsonArray = new JsonArray();
        for (String obj : array) jsonArray.add(obj);
        return jsonArray;
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
        return FileProvider.getUriForFile(context, "com.botpa.turbophotos.FileProvider", file);
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
