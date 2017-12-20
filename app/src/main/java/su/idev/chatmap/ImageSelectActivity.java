package su.idev.chatmap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.chrisbanes.photoview.OnPhotoTapListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pitt.library.fresh.FreshDownloadView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import su.idev.chatmap.camera.CameraActivity;
import su.idev.utils.FileUtils;

import static su.idev.chatmap.MapsActivity.REFRESH_ACTION_FILTER;


/**
 * Created by culibinl on 21.07.17.
 */

public class ImageSelectActivity extends AppCompatActivity {

    public static final int REQUEST_PICK_IMAGE = 1000;
    private String uid;

    private ImageButton cameraPhoto;
    private ImageButton galleryPhoto;
    private ImageButton deletePhoto;
    private ImageButton uploadPhoto;
    private ImageButton selectPhoto;


    private PhotoView mPhotoView;
    private Toolbar toolbar;

    private UploadTask uploadTask;
    private FreshDownloadView freshDownloadView;

    private TextView hrefPhoto;

    private File sourceFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_select);

        Bundle extras = getIntent().getExtras();
        if (extras != null) uid = extras.getString("uid");

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_accent_24);
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
                if (toolbar.getHeight() > 0) {
                    FileUtils.collapseView(toolbar, 1000, 0);
                    hrefPhoto.setVisibility(View.GONE);
                } else {
                    FileUtils.expandView(toolbar, 1000, 85);
                    if (hrefPhoto.getText().length() > 0) {
                        hrefPhoto.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        cameraPhoto = (ImageButton) findViewById(R.id.cameraPhoto);
        cameraPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCameraPicker();
            }
        });

        galleryPhoto = (ImageButton) findViewById(R.id.galleryPhoto);
        galleryPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startImagePicker();
            }
        });

        deletePhoto = (ImageButton) findViewById(R.id.deletePhoto);
        deletePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sourceFile.exists()) {
                    if (uploadTask == null || uploadTask.isComplete()) {
                        File linkFile = linkFile(sourceFile);
                        if (linkFile.exists())
                            linkFile.delete();
                        File sessionFile = sessionFile(sourceFile);
                        if (sessionFile.exists())
                            linkFile.delete();

                        new File(sourceFile.getPath() + ".optimized").delete();
                        sourceFile.delete();
                        sourceFile = null;

                        mPhotoView.setImageBitmap(null);
                        hrefPhoto.setText("");
                        enableControls(false);
                    }
                }
            }
        });
        deletePhoto.setVisibility(View.GONE);

        uploadPhoto = (ImageButton) findViewById(R.id.uploadPhoto);
        uploadPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sourceFile.exists()) {
                    uploadImage();
                }
            }
        });

        hrefPhoto = (TextView) findViewById(R.id.hrefPhoto);
        hrefPhoto.setVisibility(View.GONE);



        selectPhoto = (ImageButton) findViewById(R.id.selectPhoto);
        selectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPhoto();
            }
        });

        freshDownloadView = (FreshDownloadView) findViewById(R.id.pitt);
        freshDownloadView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle cancel downloading
                if (uploadTask != null) {
                    uploadTask.cancel();
                }
                freshDownloadView.setVisibility(View.GONE);
            }
        });
        freshDownloadView.setVisibility(View.GONE);
        freshDownloadView.setRadius(52f);
        freshDownloadView.setProgressTextSize(52f);

        enableControls(false);

    }

    private static File targetForUpload(Context context, Uri sourceUri, String uid)
    {
        return new File(FileUtils.getBaseCacheDir(context, "upload"),
                FileUtils.md5(sourceUri.getPath() + uid));
    }

    private static File sessionFile(File sourceFile)
    {
        return new File(sourceFile.getPath() + ".session");
    }

    private static File linkFile(File sourceFile)
    {
        return new File(sourceFile.getPath() + ".link");
    }

    // Camera image
    private void startCameraPicker()
    {
        Intent intent = new Intent(ImageSelectActivity.this, CameraActivity.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        //intent.putExtra("uid", UID());
        //ImageSelectActivity.this.startActivity(intent);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    // Pick image
    private void startImagePicker()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // May be other content type
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    // Show selected image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_IMAGE:

                    if (data.getStringExtra("CAMERA") == null && data.getData() == null)
                        return;

                    final Context context = ImageSelectActivity.this;
                    final Uri sourceUri = data.getStringExtra("CAMERA") != null ?
                            Uri.fromFile(new File(data.getStringExtra("CAMERA"))) : data.getData();
                    final File targetFile = targetForUpload(context, sourceUri, uid);

                    new AsyncTask<Void, Void, Bitmap>() {
                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                        }

                        @Override
                        protected Bitmap doInBackground(Void... params) {
                            if (FileUtils.copyFile(context, sourceUri, targetFile)) {
                                sourceFile = targetFile;
                                // Write optimized
                                // if (sourceFile.length() >= 1*1024*1024)
                                //    FileUtils.decodeBitmapFromResource(sourceFile.getPath(), 1024, 1024, false);
                                return FileUtils.optimizedBitmap(sourceFile);
                            } else {
                                return null;
                            }
                        }

                        @Override
                        protected void onPostExecute(Bitmap result) {
                            if (result != null) {
                                //Log.i("XZ", sourceFile.getPath());
                                mPhotoView.setImageBitmap(result);
                                File linkFile = sourceFile != null ? linkFile(sourceFile) : null;
                                if (linkFile != null && linkFile.exists()) {
                                    String href = FileUtils.toString(FileUtils.readFile(linkFile));
                                    hrefPhoto.setText(href);
                                } else {
                                    hrefPhoto.setText("");
                                }
                            }
                            enableControls(false);
                        }
                    }.execute();
                    break;
                default:
                    break;
            }
        }
    }

    // Upload image
    private void uploadImage() {

        if (uploadTask != null) {
            if (uploadTask.isPaused()) {
                uploadTask.resume();
                return;
            }
            if (uploadTask.isInProgress()) {
                uploadTask.pause();
                return;
            }
            return;
        }

        // Check content type
        String contentType;
        try {
            contentType = FileUtils.getMimeType(sourceFile.getPath());
        } catch (Exception ex) {
            contentType = "image/jpeg";
        }

        // File or Blob
        final Uri file = Uri.fromFile(sourceFile);
        // Session file
        final File sessionFile = sessionFile(sourceFile);
        String sessionContent = FileUtils.toString(FileUtils.readFile(sessionFile));
        //String sessionContent = FileUtils.readFileString(sessionFile);
        Uri sessionUri = sessionContent != null && sessionContent.length() > 0 ?
                Uri.parse(sessionContent) : null;
        // Link file
        final File linkFile = linkFile(sourceFile);

        // Create the file metadata
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(contentType)
                .build();

        freshDownloadView.startDownload();
        freshDownloadView.setVisibility(View.VISIBLE);

        // Upload file and metadata to the path 'images/blabla.jpg'
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        if (sessionUri != null) {
            uploadTask = storageRef.child("images/" + file.getLastPathSegment())
                    .putFile(file, metadata, sessionUri);
        } else {
            uploadTask = storageRef.child("images/" + file.getLastPathSegment())
                    .putFile(file, metadata);
        }

        enableControls(true);

        // Listen for state changes, errors, and completion of the upload.
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                // Update progress
                float uProgress = ((float)taskSnapshot.getBytesTransferred()/(float)taskSnapshot.getTotalByteCount());
                freshDownloadView.upDateProgress(uProgress);

                // Save session for resume pause
                Uri sessionUri = taskSnapshot.getUploadSessionUri();
                if (sessionUri != null && !sessionFile.exists()) {
                    try {
                        FileOutputStream outSession = new FileOutputStream(sessionFile.getPath());
                        outSession.write(sessionUri.toString().getBytes());
                        outSession.flush();
                        outSession.close();
                    } catch (Exception ex) { }
                }

            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                //Toast.makeText(ImageSelectActivity.this, "Upload is paused", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                // Toast.makeText(ImageSelectActivity.this, "Upload is failure", Toast.LENGTH_SHORT).show();
                try { uploadTask.cancel(); } catch (Exception ex) {}
                if (sessionFile.exists()) sessionFile.delete();
                uploadTask = null;
                freshDownloadView.showDownloadError();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        freshDownloadView.setVisibility(View.GONE);
                    }
                }, 1000);
                enableControls(false);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Handle successful uploads on complete
                successUpload(linkFile, sessionFile, taskSnapshot);
                enableControls(false);
            }
        });
    }

    void enableControls(boolean onUpload) {
        if (onUpload) {
            galleryPhoto.setVisibility(View.GONE);
            deletePhoto.setVisibility(View.GONE);
            uploadPhoto.setVisibility(View.VISIBLE);
            selectPhoto.setVisibility(View.GONE);
            hrefPhoto.setVisibility(View.GONE);
        } else {
            galleryPhoto.setVisibility(View.VISIBLE);
            if (sourceFile != null) {
                deletePhoto.setVisibility(View.VISIBLE);
                uploadPhoto.setVisibility(View.VISIBLE);
                if (linkFile(sourceFile).exists()) {
                    selectPhoto.setVisibility(View.VISIBLE);
                    hrefPhoto.setVisibility(View.VISIBLE);
                } else {
                    selectPhoto.setVisibility(View.GONE);
                    hrefPhoto.setVisibility(View.GONE);
                }
            } else {
                deletePhoto.setVisibility(View.GONE);
                uploadPhoto.setVisibility(View.GONE);
                selectPhoto.setVisibility(View.GONE);
                hrefPhoto.setVisibility(View.GONE);
            }
        }

    }

    // Save uploadTask on background
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If there's an upload in progress, save the reference so you can query it later
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        if (storageRef != null) {
            outState.putString("reference", storageRef.toString());
        }
    }

    // Restore uploadTask on background
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // If there was an upload in progress, get its reference and create a new StorageReference
        final String stringRef = savedInstanceState.getString("reference");
        if (stringRef == null) {
            return;
        }
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(stringRef);

        // Find all UploadTasks under this StorageReference (in this example, there should be one)
        List<UploadTask> tasks = storageRef.getActiveUploadTasks();
        if (tasks.size() > 0) {
            // Get the task monitoring the upload
            UploadTask task = tasks.get(0);
            enableControls(true);
            if (sourceFile != null) {
                // Add new listeners to the task using an Activity scope
                task.addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        successUpload(linkFile(sourceFile), sessionFile(sourceFile), taskSnapshot);
                        enableControls(false);
                    }
                });
            } else {
                task.cancel();
                enableControls(false);
            }
        }
    }

    // Success upload
    private void successUpload(File linkFile, File sessionFile, UploadTask.TaskSnapshot taskSnapshot) {
        Uri sourceFileUri = taskSnapshot.getMetadata().getDownloadUrl();
        try {
            // Save link
            FileOutputStream outSession = new FileOutputStream(linkFile.getPath());
            outSession.write(sourceFileUri.toString().getBytes());
            outSession.flush();
            outSession.close();

            hrefPhoto.setVisibility(View.VISIBLE);
            hrefPhoto.setText(sourceFileUri.toString());

            //Toast.makeText(ImageSelectActivity.this, "Upload success!", Toast.LENGTH_SHORT).show();
            freshDownloadView.showDownloadOk();
        } catch (Exception ex) {
            //Toast.makeText(ImageSelectActivity.this, "Upload is failure", Toast.LENGTH_SHORT).show();
            freshDownloadView.showDownloadError();
        } finally {
            if (sessionFile.exists()) sessionFile.delete();
            uploadTask = null;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                freshDownloadView.setVisibility(View.GONE);
            }
        }, 1000);
    }

    // Return uploaded url
    private void selectPhoto() {
        File linkFile = sourceFile != null ? linkFile(sourceFile) : null;
        if (linkFile != null && linkFile.exists()) {
            String href = FileUtils.toString(FileUtils.readFile(linkFile));
            // Send refresh
            Intent intent = new Intent();
            intent.setAction(REFRESH_ACTION_FILTER);
            intent.putExtra("source", "ImageSelectActivity");
            intent.putExtra("target", "MapsActivity");
            intent.putExtra("body", href);
            sendBroadcast(intent);
        }
        onBackPressed();
    }

    // Cancel uploadTask onBackPressed
    @Override
    public void onBackPressed()
    {
        if (uploadTask != null) {
            if (!uploadTask.isComplete()) {
                uploadTask.cancel();
            }
            uploadTask = null;
        }
        if (sourceFile != null) {
            File sessionFile = sessionFile(sourceFile);
            if (sessionFile.exists()) {
                sessionFile.delete();
            }
            new File(sourceFile.getPath() + ".optimized").delete();
            sourceFile.delete();
            sourceFile = null;
        }
        super.onBackPressed();
    }

}
