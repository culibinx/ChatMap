package su.idev.chatmap;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.chrisbanes.photoview.OnPhotoTapListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.pitt.library.fresh.FreshDownloadView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import su.idev.chatmap.pager.HackyViewPager;
import su.idev.chatmap.pager.ImageViewAdapter;
import su.idev.chatmap.span.CenteredImageSpan;
import su.idev.chatmap.span.DownloadImageTask;
import su.idev.utils.FileUtils;

import static android.text.style.DynamicDrawableSpan.ALIGN_BOTTOM;
import static android.webkit.ConsoleMessage.MessageLevel.LOG;
import static su.idev.chatmap.MapsActivity.REFRESH_ACTION_FILTER;
import static su.idev.utils.FileUtils.getDeviceName;
import static su.idev.utils.FileUtils.tintImage;

/**
 * Created by culibinl on 17.07.17.
 */

public class ImageViewActivity extends AppCompatActivity {

    private String source;
    private Button showAll;
    private ImageButton deletePhoto;
    private PhotoView mPhotoView;
    private Toolbar toolbar;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        //toolbar.setTitle("Simple Sample");
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        mPhotoView = (PhotoView) findViewById(R.id.image);
        mPhotoView.setOnPhotoTapListener(new OnPhotoTapListener() {
            @Override
            public void onPhotoTap(ImageView view, float x, float y) {
                if (toolbar.getHeight() > 0)
                    FileUtils.collapseView(toolbar, 1000, 0);
                else
                    FileUtils.expandView(toolbar, 1000, 85);
            }
        });

        viewPager = (HackyViewPager) findViewById(R.id.view_pager);
        viewPager.setVisibility(View.GONE);
        setAdapter();

        deletePhoto = (ImageButton) findViewById(R.id.deletePhoto);
        deletePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewPager.isShown()) {
                    int position = viewPager.getCurrentItem();
                    String _source = getSourceFromPosition(position);
                    if (_source != null) {

                        // Disable current
                        if (_source.replace(".preview", "").equals(source)) {
                            source = null;
                            showAll.setVisibility(View.GONE);
                            mPhotoView.setVisibility(View.GONE);
                        }

                        // Send refresh
                        Intent intent = new Intent();
                        intent.setAction(REFRESH_ACTION_FILTER);
                        //intent.putExtra("source", "ImageViewActivity");
                        intent.putExtra("target", "MapsActivity");
                        //intent.putExtra("body", "delete:" + source);
                        //intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                        sendBroadcast(intent);

                        // Delete
                        new File(_source).delete();
                        new File(_source.replace(".preview", "")).delete();
                        new File(_source.replace(".preview", ".optimized")).delete();

                        // Refresh
                        viewPager.removeAllViews();
                        setAdapter();
                        int count = viewPager.getAdapter().getCount();
                        if (count == 0) {
                            // Back on empty
                            onBackPressed();
                        } else {
                            // Set position
                            position--;
                            if (position >= 0 && position < count)
                                viewPager.setCurrentItem(position);
                        }
                    }
                } else {
                    new File(source).delete();
                    new File(source + ".preview").delete();
                    // Send refresh
                    Intent intent = new Intent();
                    intent.setAction(REFRESH_ACTION_FILTER);
                    intent.putExtra("source", "ImageViewActivity");
                    intent.putExtra("target", "MapsActivity");
                    intent.putExtra("body", "delete:" + source);
                    //intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    sendBroadcast(intent);
                    // Back
                    onBackPressed();
                }

            }
        });
        deletePhoto.setVisibility(View.GONE);

        showAll = (Button) findViewById(R.id.showAll);
        showAll.setText(getString(R.string.all_photo));
        showAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewPager.isShown()) {
                    showAll.setText(getString(R.string.all_photo));
                    mPhotoView.setVisibility(View.VISIBLE);
                    //deletePhoto.setVisibility(View.VISIBLE);
                    viewPager.setVisibility(View.GONE);
                } else {
                    showAll.setText(getString(R.string.current_photo));
                    mPhotoView.setVisibility(View.GONE);
                    //deletePhoto.setVisibility(View.GONE);
                    viewPager.setVisibility(View.VISIBLE);
                }
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            source = extras.getString("source");
            File sourceFile = new File(source);
            if (sourceFile.exists()) {
                Bitmap bitmap;
                // Only < 1 Mb for preview
                if (getDeviceName().contains("Vertex")) {
                    if (sourceFile.length() < 1*1024*1024)
                        bitmap = BitmapFactory.decodeFile(sourceFile.getPath());
                    else
                        bitmap = FileUtils.decodeBitmapFromResource(sourceFile.getPath(), 1024, 1024);
                } else {
                    bitmap = BitmapFactory.decodeFile(sourceFile.getPath());
                }
                mPhotoView.setImageBitmap(bitmap);
            }
            deletePhoto.setVisibility(View.VISIBLE);
        }

        FileUtils.collapseView(toolbar, 1, 0);
    }

    private void setAdapter() {
        viewPager.setAdapter(new ImageViewAdapter(ImageViewActivity.this,
                photoDirectory(ImageViewActivity.this), toolbar));
        //viewPager.setAdapter(new ImagePagerAdapter(ImageViewActivity.this,
        //        photoDirectory(ImageViewActivity.this), toolbar));
    }

    private String getSourceFromPosition(int position) {
        //return ((ImagePagerAdapter)viewPager.getAdapter())
        //        .sourceFromPosition(position);
        return ((ImageViewAdapter)viewPager.getAdapter())
                .sourceFromPosition(position);
    }

    static class ImagePagerAdapter extends PagerAdapter {

        private Context context;
        private Toolbar toolbar;
        private ArrayList<File> files = new ArrayList<>();
        private File directoryPath;

        public ImagePagerAdapter(Context context, File directoryPath, Toolbar toolbar) {
            this.context = context;
            this.toolbar = toolbar;
            this.directoryPath = directoryPath;
            refreshList();
        }

        public void refreshList() {
            files = FileUtils.listFilesForFolder(directoryPath, false,
                    ".*?\\.preview$");
            this.notifyDataSetChanged();
        }

        public String sourceFromPosition(int position) {
            if (position > -1 && position < files.size()) {
                return files.get(position).getPath();
            }
            return null;
        }

        @Override
        public int getCount() {
            return files.size();
        }

        @Override
        public View instantiateItem(ViewGroup container, int position) {
            PhotoView photoView = new PhotoView(context);
            //PhotoView photoView = new PhotoView(container.getContext());
            photoView.setOnPhotoTapListener(new OnPhotoTapListener() {
                @Override
                public void onPhotoTap(ImageView view, float x, float y) {
                    if (toolbar.getHeight() > 0)
                        FileUtils.collapseView(toolbar, 1000, 0);
                    else
                        FileUtils.expandView(toolbar, 1000, 85);
                }
            });
            String filePath = files.get(position).getPath();
            File sourceFile = new File(filePath.replace(".preview", ""));
            Bitmap bitmap;
            if (sourceFile.exists()) {
                // Only < 1 Mb for preview
                if (sourceFile.exists() && sourceFile.length() < 1*1024*1024)
                    bitmap = BitmapFactory.decodeFile(sourceFile.getPath());
                else
                    bitmap = FileUtils.decodeBitmapFromResource(sourceFile.getPath(), 1024, 1024);
            } else {
                bitmap = BitmapFactory.decodeFile(filePath);
            }
            photoView.setImageBitmap(bitmap);
            container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            return photoView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }


    }

    public static File photoDirectory(Context context) {
        return FileUtils.getBaseCacheDir(context,"download");
    }

    public static void setTextViewHTML(final Context context,
                                       final FreshDownloadView freshDownloadView,
                                       final TextView textView, final String html,
                                       final boolean incoming)
    {
        if (context == null/* || html.indexOf("<http") < 0*/) {
            textView.setText(html);
        } else {
            //urlsHTML
            //CharSequence sequence = Html.fromHtml(attachmentHTML(html));
            String updated = urlsHTML(html);
            updated = updated.replace("<<a href=", "<a href=");
            updated = updated.replace("</a>>", "</a>");
            CharSequence sequence = Html.fromHtml(updated);
            final SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
            URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
            for(final URLSpan span : urls)
            {
                final int start = strBuilder.getSpanStart(span);
                final int end = strBuilder.getSpanEnd(span);
                final int flags = strBuilder.getSpanFlags(span);
                final String source = span.getURL().replace("\\","").replace("\"","");

                if (source.contains("firebasestorage")
                        || source.contains("png")
                        || source.contains("jpeg")
                        || source.contains("jpg")) {
                    final File sourceFile = fileFromSource(context, source);
                    Bitmap bitmap = previewFileExists(sourceFile) ?
                            BitmapFactory.decodeFile(previewFile(sourceFile).getPath()) : null;
                    if (bitmap != null) {
                        ImageSpan imgSpan = new ImageSpan(context, bitmap, ALIGN_BOTTOM);
                        strBuilder.setSpan(imgSpan, start, end, flags);
                        // Set new clickable
                        ClickableSpan clickable = new ClickableSpan() {
                            public void onClick(View view) {
                                startImageViewActivity(context, sourceFile.getPath());
                            }
                        };
                        strBuilder.setSpan(clickable, start, end, flags);
                        // Remove old
                        strBuilder.removeSpan(span);
                    } else {
                        // Set downloadable span
                        bitmap = BitmapFactory
                                .decodeResource(context.getResources(), R.drawable.ic_photo);
                        if (incoming) { bitmap = tintImage(bitmap, R.color.colorPrimary); }
                        CenteredImageSpan imgSpan = new CenteredImageSpan(context, bitmap);
                        strBuilder.setSpan(imgSpan, start, end, flags);
                        ClickableSpan clickable = new ClickableSpan() {
                            public void onClick(View view) {
                                if (freshDownloadView.isShown()) return;
                                DownloadImageTask task = new
                                        DownloadImageTask(context,
                                        textView, start, end,
                                        freshDownloadView,
                                        source);
                                task.execute();
                            }
                        };
                        strBuilder.setSpan(clickable, start, end, flags);
                        // Remove old
                        strBuilder.removeSpan(span);
                    }
                }
            }
            textView.setText(strBuilder);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    private static String attachmentHTML(String html) {
        String result = html;
        String pattern = "<\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]>";
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(html);
            while (m.find()) {
                String s = m.group(0);
                String extracted = s.substring(s.indexOf('<')+1,s.lastIndexOf('>'));
                result = extracted; //result.replace(s, "<a href=\"inline:" + extracted + "\">attachment</a>");
            }
        } catch (Exception ex) {
            //
        }
        return result;
    }

    private static String urlsHTML(String html) {
        String result = html;
        String pattern = "(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(html);
            while (m.find()) {
                String s = m.group(0);
                result = result.replace(s, "<a href=\"" + s + "\">" + s + "</a>");
            }
        } catch (Exception ex) {
            //
        }
        return result;
    }

    public static void startImageViewActivity(Context context, String source) {
        Intent intent = new Intent().setClass(context, ImageViewActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra("source", source);
        context.startActivity(intent);
    }

    public static void startUrlViewActivity(Context context, String source) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(source));
        context.startActivity(intent);
    }

    private static void startFileViewActivity(Context context, String filePath, String contentType) {
        if (filePath == null || contentType == null) return;
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        File file = new File(filePath);
        intent.setDataAndType(Uri.fromFile(file), contentType);
        context.startActivity(Intent.createChooser(intent, "Choose a viewer"));
    }

    public static File fileFromSource(Context context, String source) {
        if (source == null) return null;
        return new File(photoDirectory(context), FileUtils.md5(source));
    }

    public static boolean previewFileExists(File sourceFile) {
        return sourceFile != null &&
                sourceFile.exists() &&
                new File(sourceFile.getPath() + ".preview").exists();
    }

    public static File previewFile(File sourceFile) {
        return sourceFile != null ? new File(sourceFile.getPath() + ".preview") : null;
    }

    public static File historyFile(File sourceFile) {
        return sourceFile != null ? new File(sourceFile.getPath() + ".history") : null;
    }

    public static void replaceDownloadSpan(final Context context,
                                           TextView textView,
                                           int start, int end, final File sourceFile) {
        Bitmap bitmap = previewFileExists(sourceFile) ?
                BitmapFactory.decodeFile(previewFile(sourceFile).getPath()) : null;
        if (bitmap != null) {
            boolean replaced = false;
            // Replace image on load
            CharSequence sequence = textView.getText();
            final SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
            CenteredImageSpan[] urls = strBuilder.getSpans(0, sequence.length(), CenteredImageSpan.class);
            for(CenteredImageSpan centered_span : urls)
            {
                int centered_start = strBuilder.getSpanStart(centered_span);
                int centered_end = strBuilder.getSpanEnd(centered_span);
                int centered_flags = strBuilder.getSpanFlags(centered_span);

                if (centered_start == start && centered_end == end) {
                    replaced = true;
                    // Set new imgSpan
                    ImageSpan imgSpan = new ImageSpan(context, bitmap, ALIGN_BOTTOM);
                    strBuilder.setSpan(imgSpan, centered_start, centered_end, centered_flags);
                    // Set new clickable
                    ClickableSpan clickable = new ClickableSpan() {
                        public void onClick(View view) {
                            startImageViewActivity(context, sourceFile.getPath());
                        }
                    };
                    strBuilder.setSpan(clickable, centered_start, centered_end, centered_flags);

                    // Remove old
                    strBuilder.removeSpan(centered_span);
                    // Remove old clickable
                    ClickableSpan[] clicks = strBuilder.getSpans(0, sequence.length(), ClickableSpan.class);
                    for(ClickableSpan click : clicks)
                    {
                        int clicks_start = strBuilder.getSpanStart(click);
                        int clicks_end = strBuilder.getSpanEnd(click);
                        if (clicks_start == centered_start && clicks_end == centered_end) {
                            strBuilder.removeSpan(click);
                        }
                        break;
                    }
                    break;
                }
            }
            if (replaced) {
                textView.setText(strBuilder);
                textView.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }

    }


}







