package com.botpa.turbophotos.util;

import android.animation.ValueAnimator;
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
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
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
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;

import com.botpa.turbophotos.R;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.snackbar.Snackbar.SnackbarLayout;

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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class Orion {

    private static final ObjectMapper objectMapper = new ObjectMapper();


    //Math
    public static float lerp(float a, float b, float t) {
        return a * (1 - t) + b * t;
    }

    //Get attribute color
    public static int getColor(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr,  typedValue, true);
        return typedValue.data;
    }

    //Snack bar
    public static void snack(Activity activity, String message) {
        snack(activity, message, "ok", null);
    }

    public static void snack(Activity activity, String message, String button) {
        snack(activity, message, button, null);
    }

    public static void snack(Activity activity, String message, String button, Runnable runnable) {
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
        snackText.setText(message);

        TextView textViewOne = snackView.findViewById(R.id.txtOne);
        textViewOne.setText(button.toUpperCase());
        textViewOne.setOnClickListener(view -> {
            if (runnable != null) runnable.run();
            snackbar.dismiss();
        });

        layout.addView(snackView, objLayoutParams);
        snackbar.show();
    }

    public static void snackTwo(Activity activity, String message, String cancel, String confirm, Runnable runnable) {
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
        snackText.setText(message);

        TextView textViewOne = snackView.findViewById(R.id.txtOne);
        textViewOne.setText(cancel.toUpperCase());
        textViewOne.setOnClickListener(view -> snackbar.dismiss());

        TextView textViewTwo = snackView.findViewById(R.id.txtTwo);
        textViewTwo.setText(confirm.toUpperCase());
        textViewTwo.setOnClickListener(view -> {
            runnable.run();
            snackbar.dismiss();
        });

        layout.addView(snackView, objLayoutParams);
        snackbar.show();
    }

    public static void snackTwo(Activity activity, String message, String button1, Runnable runnable1, String button2, Runnable runnable2) {
        snackTwo(activity, message, button1, runnable1, button2, runnable2, Snackbar.LENGTH_LONG);
    }

    public static void snackTwo(Activity activity, String message, String button1, Runnable runnable1, String button2, Runnable runnable2, int length) {
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
        snackText.setText(message);

        TextView textViewOne = snackView.findViewById(R.id.txtOne);
        textViewOne.setText(button1.toUpperCase());
        textViewOne.setOnClickListener(view -> {
            runnable1.run();
            snackbar.dismiss();
        });

        TextView textViewTwo = snackView.findViewById(R.id.txtTwo);
        textViewTwo.setText(button2.toUpperCase());
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

        private final View view;
        private final int width, startWidth;

        public ResizeWidthAnimation(View view, int width) {
            this.view = view;
            this.width = width;
            startWidth = view.getWidth();
        }

        public ResizeWidthAnimation(View view, int width, int startWidth) {
            this.view = view;
            this.width = width;
            this.startWidth = startWidth;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            view.getLayoutParams().width = startWidth + (int) ((width - startWidth) * interpolatedTime);
            view.requestLayout();
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

        private final View view;
        private final int height, startHeight;

        public ResizeHeightAnimation(View view, int height) {
            this.view = view;
            this.height = height;
            startHeight = view.getHeight();
        }

        public ResizeHeightAnimation(View view, int height, int startHeight) {
            this.view = view;
            this.height = height;
            this.startHeight = startHeight;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            view.getLayoutParams().height = startHeight + (int) ((height - startHeight) * interpolatedTime);
            view.requestLayout();
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

    //Insets
    public interface OnInsetsChanged {

        void run(View view, Insets insets, float percent);

    }

    public static final OnInsetsChanged onInsetsChangedDefault = (view, insets, duration) -> view.setPadding(insets.left, insets.top, insets.right, insets.bottom);

    public static void addInsetsChangedListener(View view, int type) {
        addInsetsChangedListener(view, new int[] { type }, 0, onInsetsChangedDefault);
    }

    public static void addInsetsChangedListener(View view, int[] types) {
        addInsetsChangedListener(view, types, 0, onInsetsChangedDefault);
    }

    public static void addInsetsChangedListener(View view, int[] types, float duration) {
        addInsetsChangedListener(view, types, duration, onInsetsChangedDefault);
    }

    public static void addInsetsChangedListener(View view, int[] types, OnInsetsChanged onInsetsChanged) {
        addInsetsChangedListener(view, types, 0, onInsetsChanged);
    }

    public static void addInsetsChangedListener(View view, int[] types, float duration, OnInsetsChanged onInsetsChanged) {
        if (types.length == 0) return;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            //Get insets
            Insets insets = Insets.of(0, 0, 0, 0);
            for (int type : types) insets = Insets.add(insets, windowInsets.getInsets(type)); //WindowInsetsCompat.Type.systemBars()

            //Run on insets changed
            //Check if animate
            if (duration <= 0) {
                //No animation -> Run on insets changed as if it finished
                onInsetsChanged.run(view, insets, 1.0f);
            } else {
                //Calculate start & end values
                float leftStart = view.getPaddingLeft();
                float leftEnd = insets.left;
                float topStart = view.getPaddingTop();
                float topEnd = insets.top;
                float rightStart = view.getPaddingRight();
                float rightEnd = insets.right;
                float botStart = view.getPaddingBottom();
                float botEnd = insets.bottom;

                //Animate
                ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
                animator.setDuration((long) duration);
                animator.addUpdateListener(animation -> {
                    //Calculate animation percent
                    float percent = animation.getAnimatedFraction();

                    //Run on insets changed
                    onInsetsChanged.run(
                            view,
                            Insets.of(
                                    (int) Orion.lerp(leftStart, leftEnd, percent),
                                    (int) Orion.lerp(topStart, topEnd, percent),
                                    (int) Orion.lerp(rightStart, rightEnd, percent),
                                    (int) Orion.lerp(botStart, botEnd, percent)
                            ),
                            percent
                    );
                });
                animator.start();
            }

            //Done
            return windowInsets;
        });
    }

    //Clipboard
    public static void copyToClip(Context context, String string) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("", string);
        clipboard.setPrimaryClip(clip);
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
    public static List<File> listFiles(File parent) {
        //Create folders list
        List<File> folders = new ArrayList<>();

        //Get files array
        File[] files = parent.listFiles();
        if (files == null) return folders;

        //Add folders to list
        for (File file : files) if (file.isDirectory()) folders.add(file);
        return folders;
    }

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

    public static String getExtension(String path) {
        int dotIndex = path.lastIndexOf(".");
        return dotIndex == -1 ? "" : path.substring(dotIndex + 1);
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

    public static boolean writeFile(File file, String data) {
        Writer writer = null;
        boolean success = true;

        try {
            writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8));
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

    public static boolean deleteFile(File file) {
        if (!file.exists() || !file.isFile()) return false;

        return file.delete();
    }

    public static boolean emptyFolder(File directory, boolean deleteSelf) {
        //Invalid folder
        if (!directory.exists() || !directory.isDirectory()) return false;

        //List folder files
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) return true;

        //Empty folder
        boolean allDeleted = true;
        for (File file : files) {
            if (file.isDirectory()) {
                //Delete folder
                if (!emptyFolder(file, true)) allDeleted = false;
            } else {
                //Delete file
                if (!file.delete()) allDeleted = false;
            }
        }

        //Delete self
        if (deleteSelf && !directory.delete()) allDeleted = false;

        //Return success
        return allDeleted;
    }

    public static boolean moveFile(File oldFile, File newFile) {
        return oldFile.renameTo(newFile);
    }

    public static boolean cloneFile(Context context, File sourceFile, File destFile) {
        try {
            //Get parent
            File parent = destFile.getParentFile();
            if (parent == null) return false;

            //Create parent folder
            if (!parent.exists() && !parent.mkdirs()) {
                Log.i("ORION", parent.getAbsolutePath());
                Log.i("ORION", "FOLDER");
                return false;
            }

            //Copy file
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            scanFile(context, destFile);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void scanFile(Context context, File file) {
        MediaScannerConnection.scanFile(context, new String[] { file.getAbsolutePath() }, null, null);
    }

    //Files: JSON
    public static ObjectNode getEmptyJson() {
        return objectMapper.createObjectNode();
    }

    public static ArrayNode getEmptyJsonArray() {
        return objectMapper.createArrayNode();
    }

    public static boolean writeJson(File file, ObjectNode json) {
        return writeFile(file, json.toString());
    }

    public static boolean writeJsonPretty(File file, ObjectNode json) {
        return writeFile(file, json.toPrettyString());
    }

    public static ObjectNode loadJson(File file) {
        //Check if file exists and has content before trying to parse
        if (file == null || !file.exists() || file.length() == 0) return objectMapper.createObjectNode();

        try {
            JsonNode rootNode = objectMapper.readTree(file);

            //Check if it's a valid json object
            if (rootNode != null && rootNode.isObject()) {
                //Cast and return
                return (ObjectNode) rootNode;
            } else {
                //Not an object -> Return empty
                return objectMapper.createObjectNode();
            }
        } catch (IOException e) {
            //Return empty on error
            return objectMapper.createObjectNode();
        }
    }

    public static ObjectNode loadJson(String json) {
        //Basic check for null or empty string
        if (json == null || json.trim().isEmpty()) return objectMapper.createObjectNode();

        try {
            JsonNode rootNode = objectMapper.readTree(json);

            //Check if it's a valid json object
            if (rootNode != null && rootNode.isObject()) {
                //Cast and return
                return (ObjectNode) rootNode;
            } else {
                //Not an object -> Return empty
                return objectMapper.createObjectNode();
            }
        } catch (JsonProcessingException e) {
            //Return empty on error
            return objectMapper.createObjectNode();
        }
    }

    public static ArrayNode arrayToJson(String[] array) {
        ArrayNode jsonArray = objectMapper.createArrayNode();
        if (array == null) return jsonArray;
        for (String obj : array) jsonArray.add(obj);
        return jsonArray;
    }

    //Files: directories
    public static String getExternalStorageDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
    }

    //Files: URIs
    public static Uri getFileUriFromFilePath(Context context, String filePath) {
        //Build query args
        Bundle queryArgs = new Bundle();
        queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, MediaStore.Files.FileColumns.DATA + "=? ");
        queryArgs.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, new String[] { filePath });
        queryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE);

        //Create cursor
        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                new String[] { MediaStore.Files.FileColumns._ID },
                queryArgs,
                null
        )) {
            //Search
            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID));
                return Uri.withAppendedPath(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), String.valueOf(id));
            }
        } catch (Exception e) {
            //Error
            Log.e("Orion.getFileUriFromFilePath", "Error: " + e.getMessage());
        }

        //Failed
        return null;
    }

    public static Uri getMediaUriFromFilePath(Context context, String filePath) {
        //Ger mime type
        String extension = getExtension(filePath);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        //Determine if the file is an image or video to select the correct table
        Uri contentUri;
        if (mimeType != null && mimeType.startsWith("video")) {
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else {
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        //Build query args
        Bundle queryArgs = new Bundle();
        queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, MediaStore.Files.FileColumns.DATA + "=? ");
        queryArgs.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, new String[] { filePath });
        queryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE);

        //Create cursor
        try (Cursor cursor = context.getContentResolver().query(
                contentUri,
                new String[] { MediaStore.MediaColumns._ID },
                queryArgs,
                null
        )) {
            //Search
            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID));
                return Uri.withAppendedPath(contentUri, String.valueOf(id));
            }
        } catch (Exception e) {
            //Error
            Log.e("Orion.getMediaUriFromFilePath", "Error: " + e.getMessage());
        }

        //Failed
        return null;
    }

    public static String getFilePathFromMediaUri(Context context, Uri uri) {
        //Build query args
        Bundle queryArgs = new Bundle();
        queryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE);

        //Data column (file path)
        final String column = MediaStore.MediaColumns.DATA;

        //Create cursor
        try (Cursor cursor = context.getContentResolver().query(
                uri,
                new String[] { column },
                queryArgs,
                null
        )) {
            //Search
            if (cursor != null && cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            //Error
            Log.e("Orion.getPathFromUri", "Error querying for path: " + e.getMessage());
        }

        //Failed
        return null;
    }

    public static String getFilePathFromDocumentProviderUri(Context context, Uri uri) {
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
