package su.idev.chatmap.camera;

/**
 * Created by culibinl on 01.10.17.
 */

import android.app.Activity;
import android.os.Bundle;

import java.io.File;

import su.idev.chatmap.R;

public class CameraActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String source = extras.getString("source");
            File sourceFile = new File(source);
        }

        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, CameraFragment.newInstance())
                    .commit();
        }
    }

}