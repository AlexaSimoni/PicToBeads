package com.example.pictobeads;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import java.util.List;

import com.example.pictobeads.R;

/**
 * Activity for designing bead bracelets with different patterns.
 */
public class BraceletActivity extends AppCompatActivity {

    private b_gradle currentGridView;
    private FrameLayout gridContainer;
    private ImageView previewImage;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private float braceletWidthMm = 20f;
    private float braceletLengthMm = 160f;
    private float zoomFactor = 15f; 
    
    private int currentPatternType = 0; 
    private Bead selectedBead = null;

    /**
     * Initializes the bracelet design activity and UI components.
     * Input: savedInstanceState - Bundle with saved state.
     * Output: None.
     * Algorithm: Sets the layout, initializes views, inflates partial layouts, and sets up listeners for pattern selection and width adjustment.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView title = findViewById(R.id.tv_header_title);
        title.setText("Bracelet Making");
        gridContainer = findViewById(R.id.grid_container);
        previewImage = findViewById(R.id.preview_image);
        findViewById(R.id.btn_header_back).setOnClickListener(v -> finish());

        LinearLayout headerActions = findViewById(R.id.header_actions_container);
        View headerView = LayoutInflater.from(this).inflate(R.layout.partial_picture_header, headerActions, true);
        
        ImageButton btnHeart = headerView.findViewById(R.id.btn_header_heart);
        ImageButton btnUpload = headerView.findViewById(R.id.btn_header_upload);

        FrameLayout patternContainer = findViewById(R.id.pattern_toolbar_container);
        View patternView = LayoutInflater.from(this).inflate(R.layout.partial_bracelet_patterns, patternContainer, true);
        
        patternView.findViewById(R.id.btn_math_grid).setOnClickListener(v -> { currentPatternType = 0; updateGrid(); });
        patternView.findViewById(R.id.btn_vertical_brick).setOnClickListener(v -> { currentPatternType = 1; updateGrid(); });
        patternView.findViewById(R.id.btn_missing_brick).setOnClickListener(v -> { currentPatternType = 2; updateGrid(); });

        FrameLayout sliderContainer = findViewById(R.id.bottom_controls_container);
        View sliderView = LayoutInflater.from(this).inflate(R.layout.partial_width_slider, sliderContainer, true);
        TextView label = sliderView.findViewById(R.id.slider_label);
        label.setText("Bracelet Width (mm)");
        SeekBar seek = sliderView.findViewById(R.id.seek_control);
        seek.setMax(100);
        seek.setProgress((int)braceletWidthMm);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean f) { if(f) { braceletWidthMm = Math.max(5, p); updateGrid(); } }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) handleSelectedImage(result.getData().getData());
        });
        btnUpload.setOnClickListener(v -> imagePickerLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)));
        btnHeart.setOnClickListener(v -> updateGridWithBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.heart)));

        List<Bead> beads = Bead.getStandardTypes();
        selectedBead = beads.get(0);
        int[] beadIds = {R.id.btn_bead_1, R.id.btn_bead_2, R.id.btn_bead_3, R.id.btn_bead_4, R.id.btn_bead_5, R.id.btn_bead_6, R.id.btn_bead_7};
        for (int i = 0; i < beadIds.length; i++) {
            if (i < beads.size()) {
                final Bead b = beads.get(i);
                findViewById(beadIds[i]).setOnClickListener(v -> { selectedBead = b; updateGrid(); });
            }
        }

        updateGrid();
    }

    /**
     * Rebuilds the bracelet grid pattern.
     * Input: None.
     * Output: None.
     * Algorithm: Calculates columns and rows based on fixed length and adjustable width, instantiates the appropriate custom view, and applies selected bead and image data.
     */
    private void updateGrid() {
        if (selectedBead == null) return;
        float bSize = selectedBead.getSize();
        int cols = Math.max(1, (int) (braceletWidthMm / bSize));
        int rows = Math.max(1, (int) (braceletLengthMm / bSize));
        int res = (int) (bSize * zoomFactor);

        gridContainer.removeAllViews();
        if (currentPatternType == 0) currentGridView = new math_gradle(this, cols, rows, res, 1);
        else if (currentPatternType == 1) currentGridView = new brick_gradle(this, cols, rows, res, 1, true);
        else currentGridView = new missing_brick_gradle(this, cols, rows, res, 1);
        
        currentGridView.setBead(selectedBead);
        gridContainer.addView(currentGridView);

        Bitmap b = (previewImage.getDrawable() instanceof BitmapDrawable) ? ((BitmapDrawable) previewImage.getDrawable()).getBitmap() : null;
        if (b != null) currentGridView.setImageData(b);
    }

    /**
     * Updates the grid with a new image bitmap.
     * Input: bitmap - The source image for the bracelet.
     * Output: None.
     * Algorithm: Updates the preview image view and triggers a grid update.
     */
    private void updateGridWithBitmap(Bitmap bitmap) {
        if (bitmap == null) return;
        previewImage.setImageBitmap(bitmap);
        previewImage.setVisibility(View.VISIBLE);
        if (currentGridView != null) currentGridView.setImageData(bitmap);
    }

    /**
     * Handles image selection from URI.
     * Input: uri - image URI.
     * Output: None.
     * Algorithm: Decodes bitmap from stream and updates the grid.
     */
    private void handleSelectedImage(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap b = BitmapFactory.decodeStream(is);
            updateGridWithBitmap(b);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
