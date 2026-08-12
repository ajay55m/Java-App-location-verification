package com.app.fourscontracting;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CaptureActivity extends AppActivity {
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null && extras.containsKey("data")) {
                        Bitmap bitmap = (Bitmap) extras.get("data");
                        File outputFile = writeBitmapToFile(bitmap);
                        if (outputFile != null) {
                            Intent response = new Intent();
                            response.putExtra("photo_path", outputFile.getAbsolutePath());
                            response.putExtra("eid", getIntent().getStringExtra("eid"));
                            response.putExtra("action", getIntent().getStringExtra("action"));
                            response.putExtra("uid", getIntent().getStringExtra("uid"));
                            response.putExtra("project_id", getIntent().getStringExtra("project_id"));
                            response.putExtra("location_id", getIntent().getStringExtra("location_id"));
                            response.putExtra("lat", getIntent().getDoubleExtra("lat", 0.0));
                            response.putExtra("lng", getIntent().getDoubleExtra("lng", 0.0));
                            response.putExtra("attendance_submitted", false);
                            response.putExtra("attendance_message", "Photo captured");
                            setResult(Activity.RESULT_OK, response);
                            finish();
                            return;
                        }
                    }
                }
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(this, "No camera available", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private File writeBitmapToFile(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        File dir = new File(getCacheDir(), "attendance");
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        File outputFile = new File(dir, System.currentTimeMillis() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.flush();
            return outputFile;
        } catch (IOException e) {
            return null;
        }
    }
}
