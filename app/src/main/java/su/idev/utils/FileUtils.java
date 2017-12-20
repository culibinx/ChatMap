package su.idev.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * Created by culibinl on 23.06.17.
 */

public abstract class FileUtils {

    private static final String TAG = "FileUtils";

    public static void saveObjectToSharedPreference(Context context, String preferenceFileName, String serializedObjectKey, Object object) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(preferenceFileName, 0);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        final Gson gson = new Gson();
        String serializedObject = gson.toJson(object);
        sharedPreferencesEditor.putString(serializedObjectKey, serializedObject);
        sharedPreferencesEditor.apply();
    }

    public static <GenericClass> GenericClass getSavedObjectFromPreference(Context context, String preferenceFileName, String preferenceKey, Class<GenericClass> classType) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(preferenceFileName, 0);
        if (sharedPreferences.contains(preferenceKey)) {
            final Gson gson = new Gson();
            return gson.fromJson(sharedPreferences.getString(preferenceKey, ""), classType);
        }
        return null;
    }

    public static String getMimeType(String fileUrl)
            throws java.io.IOException
    {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String type = fileNameMap.getContentTypeFor(fileUrl);

        return type;
    }

    public static void expandView(final View v, int duration, int targetHeight) {

        int prevHeight  = v.getHeight();

        v.setVisibility(View.VISIBLE);
        ValueAnimator valueAnimator = ValueAnimator.ofInt(prevHeight, targetHeight);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                v.getLayoutParams().height = (int) animation.getAnimatedValue();
                v.requestLayout();
            }
        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    public static void animateVisibility(final View view, boolean isVisible) {
        animateVisibility(view, isVisible, 0);
    }
    public static void animateVisibility(final View view, boolean isVisible, float alpha) {
        if ((isVisible && view.getVisibility() == View.VISIBLE) ||
                (!isVisible && !(view.getVisibility() == View.VISIBLE))) {
            return;
        }
        float to = isVisible ? alpha > 0 ? alpha : 1.0f : 0.0f,
                from = isVisible ? 0.0f : alpha > 0 ? alpha : 1.0f;

        ObjectAnimator animation = ObjectAnimator.ofFloat(view, "alpha", from, to);
        animation.setDuration(ViewConfiguration.getDoubleTapTimeout());

        if (!isVisible) {
            animation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(View.GONE);
                }
            });
        } else view.setVisibility(View.VISIBLE);

        animation.start();
    }

    public static String getDeviceName() {
        final String manufacturer = Build.MANUFACTURER, model = Build.MODEL;
        return model.startsWith(manufacturer) ? capitalizePhrase(model) : capitalizePhrase(manufacturer) + " " + model;
    }

    public static String capitalizePhrase(String s) {
        if (s == null || s.length() == 0)
            return s;
        else {
            StringBuilder phrase = new StringBuilder();
            boolean next = true;
            for (char c : s.toCharArray()) {
                if (next && Character.isLetter(c) || Character.isWhitespace(c))
                    next = Character.isWhitespace(c = Character.toUpperCase(c));
                phrase.append(c);
            }
            return phrase.toString();
        }
    }

    public static void collapseView(final View v, int duration, int targetHeight) {
        int prevHeight  = v.getHeight();
        ValueAnimator valueAnimator = ValueAnimator.ofInt(prevHeight, targetHeight);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                v.getLayoutParams().height = (int) animation.getAnimatedValue();
                v.requestLayout();
            }
        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    public static Bitmap decodeBitmapFromResource(String source,
                                                  int reqWidth, int reqHeight)
    {
        return decodeBitmapFromResource(source, reqWidth, reqHeight, true);
    }

    public static Bitmap decodeBitmapFromResource(String source,
                                                  int reqWidth, int reqHeight, boolean async) {

        final File optimized = new File(source + ".optimized");
        if (optimized.exists()) {
            return BitmapFactory.decodeFile(optimized.getPath());
        }
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(source, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        final Bitmap bitmap = BitmapFactory.decodeFile(source, options);

        if (async) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                }

                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        //bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        byte[] byteArray = stream.toByteArray();

                        // Write preview file
                        if (!optimized.exists()) { optimized.createNewFile(); }
                        FileOutputStream out = new FileOutputStream(optimized.getPath());
                        out.write(byteArray);
                        out.flush();
                        out.close();
                    } catch (Exception ex) {
                        if (optimized.exists()) {
                            optimized.delete();
                        }
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void v) {
                    super.onPostExecute(v);
                }
            }.execute();
        } else {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                //bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                // Write preview file
                if (!optimized.exists()) { optimized.createNewFile(); }
                FileOutputStream out = new FileOutputStream(optimized.getPath());
                out.write(byteArray);
                out.flush();
                out.close();
            } catch (Exception ex) {
                if (optimized.exists()) {
                    optimized.delete();
                }
            }
        }

        return bitmap;
    }

    public static Bitmap BitmapFromURL(Context context, String source) {
        Bitmap bm = null;
        if (source != null && source.trim().length() > 0) {
            try {
                String hash = source.indexOf("@") > 0 ? md5(source) : source;
                File cacheMapDirectory = FileUtils.getBaseCacheDir(context,"avatars");
                File file = new File(cacheMapDirectory, hash.toLowerCase());
                if (file.exists()) {
                    bm = BitmapFactory.decodeFile(file.getAbsolutePath());
                } else {
                    String sourceURL = "https://gravatar.com/avatar/" + hash.toLowerCase() + "?size=80&default=identicon";
                    URL url = new URL(sourceURL);
                    URLConnection connection = url.openConnection();
                    connection.setConnectTimeout(1000*3);
                    connection.connect();
                    int fileLength = connection.getContentLength();
                    // Stop if filesize is zero
                    if (fileLength > 0) {
                        InputStream input = new BufferedInputStream(url.openStream(), 8192);
                        OutputStream output = new FileOutputStream(file);
                        byte data[] = new byte[8192];
                        int count = -1;
                        while ((count = input.read(data)) != -1) {
                            //while ((count = input.read(data)) != -1) {
                            output.write(data, 0, count);
                        }
                        // flushing output
                        output.flush();
                        // closing streams
                        output.close();
                        input.close();

                        bm = BitmapFactory.decodeFile(file.getAbsolutePath());
                    }
                }
            } catch (Exception ex) {
                //
            }
        }
        return bm;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap tintImage(Bitmap bitmap, int color) {
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        Bitmap bitmapResult = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapResult);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return bitmapResult;
    }

    public static byte[] convertStreamToByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[10240];
        int i = Integer.MAX_VALUE;
        while ((i = is.read(buff, 0, buff.length)) > 0) {
            baos.write(buff, 0, i);
        }

        return baos.toByteArray(); // be sure to close InputStream in calling function
    }

    public static byte[] compress(final byte[] input) throws IOException
    {
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream();
             GZIPOutputStream gzipper = new GZIPOutputStream(bout))
        {
            gzipper.write(input, 0, input.length);
            gzipper.close();

            return bout.toByteArray();
        }
    }

    public static byte[] decompress(final byte[] input) throws IOException
    {
        try (ByteArrayInputStream bin = new ByteArrayInputStream(input);
             GZIPInputStream gzipper = new GZIPInputStream(bin))
        {
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            int len;
            while ((len = gzipper.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }

            gzipper.close();
            out.close();
            return out.toByteArray();
        }
    }

    public static byte[] readFile(File file)
    {
        if (file != null && file.exists()) {
            try {
                InputStream in = new BufferedInputStream(new FileInputStream(file));
                byte[] buffer = new byte[1024];
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                in.close();
                out.close();
                return out.toByteArray();
            } catch (Exception ex) {}
        }
        return null;
    }
    public static String readFileString(File file)
    {
        if (file != null && file.exists()) {
            StringBuilder contents = new StringBuilder();
            try {
                InputStream is = new BufferedInputStream(new FileInputStream(file));
                InputStreamReader rdr = new InputStreamReader(is,"UTF-8");
                char[] buffer = new char[1024];
                int len;
                while ((len = rdr.read(buffer)) > 0) {
                    contents.append(buffer, 0, len);
                }
                rdr.close();
                if (is != null) { is.close(); }
            } catch (Exception ex) {}
            return contents.toString();
        }
        return null;
    }

    public static String toString(byte[] buffer)
    {
        try {
            if (buffer != null)
                return new String(buffer, "UTF-8");
        } catch (Exception ex) {}

        return null;
    }

    public static String readBufferedString(String fileName) throws IOException
    {
        InputStream is = new BufferedInputStream(new FileInputStream(fileName));
        try {
            InputStreamReader rdr = new InputStreamReader(is, "UTF-8");
            StringBuilder contents = new StringBuilder();
            char[] buff = new char[4096];
            int len = rdr.read(buff);
            while (len >= 0) {
                contents.append(buff, 0, len);
            }
            return buff.toString();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                // log error in closing the file
            }
        }
    }

    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            StringBuilder hexString = new StringBuilder();
            for (byte digestByte : md.digest(input.getBytes()))
                hexString.append(String.format("%02X", digestByte));
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            //e.printStackTrace();
            Log.e(TAG, "NoSuchAlgorithmException");
            return null;
        }
    }

    public static boolean copyFile(Context context, Uri sourceUri, File targetFile) {

        // Check file
        if (targetFile.exists())
            return true;
        // Copy file
        boolean success = false;
        try {
            FileOutputStream outputStream = new FileOutputStream(targetFile);
            InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
            if (inputStream != null) {
                byte[] data = new byte[1024];
                while (inputStream.read(data) != -1) {
                    outputStream.write(data);
                }
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            success = true;
        } catch (Exception ex) {
            Log.i("XZ", ex.getMessage());
            success = false;
            if (targetFile.exists())
                targetFile.delete();
        }
        return success;
    }

    public static Bitmap optimizedBitmap(File sourceFile) {
        // Only < 1 Mb for preview
        if (!sourceFile.exists())
            return null;
        if (sourceFile.length() < 1*1024*1024)
            return BitmapFactory.decodeFile(sourceFile.getPath());
        else
            return FileUtils.decodeBitmapFromResource(sourceFile.getPath(), 1024, 1024);
    }

    public static boolean writeToFile(Context context, String filename, String content) {
        boolean success = true;
        FileOutputStream outputStream;
        try {
            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(content.getBytes());
            outputStream.close();
        } catch (Exception e) {
            //e.printStackTrace();
            success = false;
        }
        return success;
    }

    public static boolean writeToFile(Context context, String filename, byte[] content) {
        boolean success = true;
        FileOutputStream outputStream;
        try {
            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(content);
            outputStream.close();
        } catch (Exception e) {
            //e.printStackTrace();
            success = false;
        }
        return success;
    }

    public static File getFile(Context context, String filename) {
        return new File(context.getFilesDir(), filename);
    }

    public static void deleteFile(File file) {
        file.delete();
    }

    public static void deleteFile(Context context, String filename) {
        context.deleteFile(filename);
    }

    public static File getTempFile(Context context, String url) {
        File file = null;
        try {
            String fileName = Uri.parse(url).getLastPathSegment();
            file = File.createTempFile(fileName, null, context.getCacheDir());
        } catch (IOException e) {
            // Error while creating file
        }
        return file;
    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public static void addImageToGallery(final String filePath, final Context context) {

        try {
            ContentValues values = new ContentValues();

            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.MediaColumns.DATA, filePath);

            context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception ex) {
            Log.e(TAG, "Not saved");
        }
    }

    public static void addPhotoToGallery(Context context, Uri contentUri) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    public static File getAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }

    public static File getAlbumStorageDir(Context context, String albumName) {
        // Get the directory for the app's private pictures directory.
        File file = new File(context.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }

    public static File getFilesDir(Context context, String name) {
        return getFilesDir(context, name, false);
    }
    public static File getFilesDir(Context context, String name, boolean external) {
        // Get the directory for the app's private root directory.
        File file = new File(external ? context.getExternalFilesDir(null) : context.getFilesDir(), name);
        if (!file.exists() && !file.isDirectory() && !file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }

    public static File getCacheDir(Context context, String name) {
        return getCacheDir(context, name, false);
    }
    public static File getCacheDir(Context context, String name, boolean external) {
        // Get the directory for the app's private root directory.
        File file = new File(external ? context.getExternalCacheDir() : context.getCacheDir(), name);
        if (!file.exists() && !file.isDirectory() && !file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }
    public static File getExternalCacheDir(String path, String name) {
        // Get the directory for the app's private root directory.
        File file = new File(path, name);
        if (!file.exists() && !file.isDirectory() && !file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }

    public static File getBaseCacheDir(Context context, String folder) {
        File cacheDirectory;
        if (context == null || folder == null) {
            return null;
        }
        boolean isExternal = FileUtils.isExternalStorageWritable();
        if (isExternal) {
            try {
                cacheDirectory = FileUtils.getExternalCacheDir("/storage/sdcard1/Android/data/su.idev.chatmap/cache", folder);
                if (!cacheDirectory.exists() && !cacheDirectory.isDirectory()) {
                    cacheDirectory = FileUtils.getCacheDir(context, folder, isExternal);
                }
            } catch (Exception ex) {
                cacheDirectory = FileUtils.getCacheDir(context, folder, isExternal);
            }
        } else {
            cacheDirectory = FileUtils.getCacheDir(context, folder, isExternal);
        }
        return cacheDirectory;
    }

    private static boolean isMatchUrl(String s) {
        String pattern = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        // String pattern = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        return isMatch(s, pattern);
    }

    private static boolean isMatch(String s, String pattern) {
        try {
            Pattern patt = Pattern.compile(pattern);
            Matcher matcher = patt.matcher(s);
            return matcher.matches();
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static ArrayList<File> listFilesForFolder(final File folder,
                                                     final boolean recursivity,
                                                     final String patternFileFilter) {

        // Inputs
        boolean filteredFile = false;

        // Ouput
        final ArrayList<File> output = new ArrayList<File> ();

        // Foreach elements
        for (final File fileEntry : folder.listFiles()) {

            // If this element is a directory, do it recursivly
            if (fileEntry.isDirectory()) {
                if (recursivity) {
                    output.addAll(listFilesForFolder(fileEntry, recursivity, patternFileFilter));
                }
            }
            else {
                // If there is no pattern, the file is correct
                if (patternFileFilter.length() == 0) {
                    filteredFile = true;
                }
                // Otherwise we need to filter by pattern
                else {
                    filteredFile = Pattern.matches(patternFileFilter, fileEntry.getName());
                }

                // If the file has a name which match with the pattern, then add it to the list
                if (filteredFile) {
                    output.add(fileEntry);
                }
            }
        }

        return output;
    }
}
