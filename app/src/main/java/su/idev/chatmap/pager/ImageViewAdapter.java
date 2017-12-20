package su.idev.chatmap.pager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.github.chrisbanes.photoview.OnPhotoTapListener;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import su.idev.utils.FileUtils;

/**
 * Created by culibinl on 22.07.17.
 */

public class ImageViewAdapter extends RecyclingPagerAdapter<ImageViewAdapter.ImageViewHolder> {

    private Context context;
    private Toolbar toolbar;
    private ArrayList<File> files = new ArrayList<>();
    private File directoryPath;

    private HashSet<ImageViewHolder> holders;

    public ImageViewAdapter(Context context, File directoryPath, Toolbar toolbar) {
        this.context = context;
        this.toolbar = toolbar;
        this.directoryPath = directoryPath;
        this.holders = new HashSet<>();
        this.files = FileUtils.listFilesForFolder(directoryPath, false,
                ".*?\\.preview$");
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
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        PhotoView photoView = new PhotoView(context);
        photoView.setOnPhotoTapListener(new OnPhotoTapListener() {
            @Override
            public void onPhotoTap(ImageView view, float x, float y) {
                if (toolbar.getHeight() > 0)
                    FileUtils.collapseView(toolbar, 1000, 0);
                else
                    FileUtils.expandView(toolbar, 1000, 85);
            }
        });
        ImageViewHolder holder = new ImageViewHolder(photoView);
        holders.add(holder);

        return holder;
    }

    class ImageViewHolder extends ViewHolder {

        private int position = -1;
        private PhotoView photoView;


        ImageViewHolder(View itemView) {
            super(itemView);
            photoView = (PhotoView) itemView;
        }

        void bind(int position) {
            this.position = position;
            String filePath = sourceFromPosition(position); //files.get(position).getPath();
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
        }



    }
}
