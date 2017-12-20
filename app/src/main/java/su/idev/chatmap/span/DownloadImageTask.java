package su.idev.chatmap.span;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.pitt.library.fresh.FreshDownloadView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

import su.idev.utils.FileUtils;

import static su.idev.chatmap.ImageViewActivity.fileFromSource;
import static su.idev.chatmap.ImageViewActivity.previewFile;
import static su.idev.chatmap.ImageViewActivity.historyFile;
import static su.idev.chatmap.ImageViewActivity.previewFileExists;
import static su.idev.chatmap.ImageViewActivity.replaceDownloadSpan;
import static su.idev.chatmap.ImageViewActivity.startImageViewActivity;
import static su.idev.chatmap.ImageViewActivity.startUrlViewActivity;

/**
 * Created by culibinl on 18.07.17.
 */

public class DownloadImageTask extends AsyncTask<Void, Integer, String> {

    private Context context;
    private TextView textView;
    private int start;
    private int end;
    private FreshDownloadView freshDownloadView;
    private String source;
    private File sourceFile;
    private File previewFile;
    private File historyFile;

    public DownloadImageTask(Context context,
                             TextView textView, int start, int end,
                             FreshDownloadView freshDownloadView,
                             String source) {
        this.context = context;
        this.textView = textView;
        this.start = start;
        this.end = end;
        this.freshDownloadView = freshDownloadView;
        this.source = source;
        this.sourceFile = fileFromSource(context, source);
        this.previewFile = previewFile(sourceFile);
        this.historyFile = historyFile(sourceFile);
    }

    @Override
    protected void onPreExecute() {
        freshDownloadView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle cancel downloading
                cancel(true);
                freshDownloadView.setVisibility(View.GONE);

            }
        });
        // Start visual progress
        freshDownloadView.startDownload();
        freshDownloadView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        // Update visual progress
        if (progress[0] == progress[1]) {
            if (progress[1] == 0) {
                freshDownloadView.showDownloadError();
            } else {
                freshDownloadView.showDownloadOk();
            }
        } else {
            if (progress[1] > 0) {
                float uProgress = ((float)progress[0]/(float)progress[1]);
                freshDownloadView.upDateProgress(uProgress);
            } else {
                //freshDownloadView.startDownload();
            }
        }
    }

    @Override
    protected void onPostExecute(String filePath) {

        // Stop visual progress
        if (filePath == null) {
            freshDownloadView.setVisibility(View.GONE);
            if (previewFileExists(sourceFile)) {
                startImageViewActivity(context, sourceFile.getPath());
            } else {
                startUrlViewActivity(context, source);
            }
        } else {
            freshDownloadView.showDownloadOk();
            replaceDownloadSpan(context, textView, start, end, sourceFile);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    freshDownloadView.setVisibility(View.GONE);
                }
            }, 1000);
        }
    }

    @Override
    protected String doInBackground(Void... params) {
        try {

            // Sleep for visual progress
            // TimeUnit.MILLISECONDS.sleep(1000);

            // Return on already prepared or unknown files
            if (sourceFile == null || previewFile == null || historyFile == null ||
                    previewFileExists(sourceFile))
                return null;

            // Check content type
            FileNameMap fileNameMap = URLConnection.getFileNameMap();
            String contentType = fileNameMap.getContentTypeFor(source);

            // Open connection
            URL url = new URL(source);
            URLConnection connection = url.openConnection();
            if (contentType == null || !contentType.startsWith("image")) {
                contentType = connection.getHeaderField("Content-Type");
            }
            // Return on not image
            if (contentType == null || !contentType.startsWith("image"))
                return null;

            connection.setConnectTimeout(1000*5);
            connection.connect();

            // Determine content length
            int contentLength = connection.getContentLength();

            // Return on empty file or timeout
            if (contentLength == 0) {
                return null;
            }

            // Write source file
            InputStream input = new BufferedInputStream(url.openStream(), 8192);
            OutputStream output = new FileOutputStream(sourceFile);
            byte data[] = new byte[8192];
            long total = 0;
            int count = -1;
            while ((count = input.read(data)) != -1) {
                total += count;
                if (contentLength > 0) {
                    // Normal progress
                    publishProgress((int)total, contentLength);
                } else {
                    // Fake progress on content length equal -1
                    publishProgress((int)total, (int)total*2-count);
                }
                output.write(data, 0, count);

                // Break on cancel
                if (isCancelled()) break;

            }
            output.flush();
            output.close();
            input.close();

            // Generate preview file
            boolean success = false;
            if (!isCancelled() && total > 0) {
                Bitmap bitmap = BitmapFactory.decodeFile(sourceFile.getPath());
                if (bitmap != null) {
                    float aspectRatio = bitmap.getWidth() / (float) bitmap.getHeight();
                    int width = 250;
                    int height = Math.round(width / aspectRatio);
                    bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    //bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();

                    // Write preview file
                    if (!previewFile.exists()) { previewFile.createNewFile(); }
                    FileOutputStream outPreview = new FileOutputStream(previewFile.getPath());
                    outPreview.write(byteArray);
                    outPreview.flush();
                    outPreview.close();

                    // Write history file
                    if (!historyFile.exists()) { historyFile.createNewFile(); }
                    FileOutputStream outHistory = new FileOutputStream(historyFile.getPath());
                    outHistory.write(source.getBytes());
                    outHistory.flush();
                    outHistory.close();

                    // Write optimized
                    if (sourceFile.length() >= 1*1024*1024)
                        FileUtils.decodeBitmapFromResource(sourceFile.getPath(), 1024, 1024, false);

                    success = true;
                }
            }


            // Remove source and preview files on cancel or empty file
            if (!success) {
                if (sourceFile.exists()) { sourceFile.delete(); }
                if (previewFile.exists()) { previewFile.delete(); }
                if (historyFile.exists()) { historyFile.delete(); }
                return null;
            }

            // Sleep for visual progress
            // TimeUnit.MILLISECONDS.sleep(1000);

            // Return source file path
            return sourceFile.getPath();

        } catch (Exception e) {
            // Remove source and preview files on error
            if (previewFile.exists()) { previewFile.delete(); }
            if (sourceFile.exists()) { sourceFile.delete(); }
            Log.e("DownloadImageTask", e.getMessage());

        }
        return null;
    }
}
