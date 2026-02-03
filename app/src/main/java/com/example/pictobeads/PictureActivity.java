package com.example.pictobeads;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
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

/**
 * Activity for creating mosaic patterns from images.
 */
public class PictureActivity extends AppCompatActivity {

    private b_gradle currentGridView;
    private FrameLayout gridContainer;
    private ImageView previewImage;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private float pictureWidthMm = 200f;
    private float pictureHeightMm = 200f;
    private float zoomFactor = 10f; // pixels per mm
    
    private int currentPatternType = 0; 
    private Bead selectedBead = null;
    private boolean isRemoveBgActive = false;

    /**
     * Initializes the activity, sets up UI components and event listeners.
     * Input: savedInstanceState - Bundle containing the activity's previously saved state.
     * Output: None.
     * Algorithm: Sets the layout, initializes views, inflates partial layouts, and attaches listeners for dimensions, zoom, and image selection.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Common UI setup
        TextView title = findViewById(R.id.tv_header_title);
        title.setText("Picture Mosaic");
        gridContainer = findViewById(R.id.grid_container);
        previewImage = findViewById(R.id.preview_image);
        findViewById(R.id.btn_header_back).setOnClickListener(v -> finish());

        // 2. Inflate Header Actions
        LinearLayout headerActions = findViewById(R.id.header_actions_container);
        View headerView = LayoutInflater.from(this).inflate(R.layout.partial_picture_header, headerActions, true);
        
        ImageButton btnUpload = headerView.findViewById(R.id.btn_header_upload);
        ImageButton btnHeart = headerView.findViewById(R.id.btn_header_heart);

        // 3. Inflate Pattern Selection Buttons
        FrameLayout patternContainer = findViewById(R.id.pattern_toolbar_container);
        View patternView = LayoutInflater.from(this).inflate(R.layout.partial_picture_patterns, patternContainer, true);
        
        patternView.findViewById(R.id.btn_vertical_grid).setOnClickListener(v -> { currentPatternType = 1; updateGrid(); });
        patternView.findViewById(R.id.btn_horizontal_grid).setOnClickListener(v -> { currentPatternType = 2; updateGrid(); });
        patternView.findViewById(R.id.btn_remove_background).setOnClickListener(v -> {
            isRemoveBgActive = !isRemoveBgActive;
            if (currentGridView != null) currentGridView.setRemoveBackground(isRemoveBgActive);
        });

        // 4. Inflate Picture Controls (Dimension bars + Zoom)
        FrameLayout sliderContainer = findViewById(R.id.bottom_controls_container);
        View controlsView = LayoutInflater.from(this).inflate(R.layout.partial_picture_controls, sliderContainer, true);
        
        EditText etWidth = controlsView.findViewById(R.id.et_picture_width);
        EditText etHeight = controlsView.findViewById(R.id.et_picture_height);
        SeekBar seekZoom = controlsView.findViewById(R.id.seek_control);

        TextWatcher dimensionWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    pictureWidthMm = Float.parseFloat(etWidth.getText().toString());
                    pictureHeightMm = Float.parseFloat(etHeight.getText().toString());
                    updateGrid();
                } catch (Exception e) {}
            }
        };
        etWidth.addTextChangedListener(dimensionWatcher);
        etHeight.addTextChangedListener(dimensionWatcher);

        seekZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean f) {
                if (f) {
                    zoomFactor = 5f + (p / 100f) * 25f; // Scale pixels per mm
                    updateGrid();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // 5. Image Picker & Bead Selectors
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) handleSelectedImage(result.getData().getData());
        });
        btnUpload.setOnClickListener(v -> imagePickerLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)));
        btnHeart.setOnClickListener(v -> updateGridWithBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.heart)));

        List<Bead> beads = Bead.getStandardTypes();
        selectedBead = beads.get(0);
        int[] bIds = {R.id.btn_bead_1, R.id.btn_bead_2, R.id.btn_bead_3, R.id.btn_bead_4, R.id.btn_bead_5, R.id.btn_bead_6, R.id.btn_bead_7};
        for (int i = 0; i < bIds.length; i++) {
            if (i < beads.size()) {
                final Bead b = beads.get(i);
                findViewById(bIds[i]).setOnClickListener(v -> { selectedBead = b; updateGrid(); });
            }
        }
        updateGrid();
    }

    /**
     * Recalculates and redraws the grid based on current dimensions, bead type, and pattern.
     * Input: None.
     * Output: None.
     * Algorithm: Calculates columns and rows from mm inputs, replaces the grid container content with a new specialized view, and processes the image bitmap for the new grid.
     */
    private void updateGrid() {
        if (selectedBead == null) return;
        float bSize = selectedBead.getSize();
        
        // Calculate Grid size based on physical mm input
        int cols = Math.max(1, (int) (pictureWidthMm / bSize));
        int rows = Math.max(1, (int) (pictureHeightMm / bSize));
        
        // Visual resolution scaled by zoom
        int res = (int) (bSize * zoomFactor);

        gridContainer.removeAllViews();
        if (currentPatternType == 0) currentGridView = new math_gradle(this, cols, rows, res, 1);
        else if (currentPatternType == 1) currentGridView = new brick_gradle(this, cols, rows, res, 1, true);
        else currentGridView = new brick_gradle(this, cols, rows, res, 1, false);
        
        currentGridView.setBead(selectedBead);
        currentGridView.setRemoveBackground(isRemoveBgActive);
        gridContainer.addView(currentGridView);
        
        Bitmap b = (previewImage.getDrawable() instanceof BitmapDrawable) ? ((BitmapDrawable) previewImage.getDrawable()).getBitmap() : null;
        if (b != null) currentGridView.setImageData(b);
    }

    /**
     * Updates the grid with a new bitmap image.
     * Input: bitmap - The source image for the mosaic pattern.
     * Output: None.
     * Algorithm: Sets the bitmap to the preview image view and triggers a grid update.
     */
    private void updateGridWithBitmap(Bitmap bitmap) {
        if (bitmap == null) return;
        previewImage.setImageBitmap(bitmap);
        previewImage.setVisibility(View.VISIBLE);
        updateGrid();
    }

    /**
     * Handles the image selected from the gallery.
     * Input: uri - The URI of the selected image.
     * Output: None.
     * Algorithm: Opens an input stream for the URI, decodes it into a bitmap, and updates the grid.
     */
    private void handleSelectedImage(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap b = BitmapFactory.decodeStream(is);
            updateGridWithBitmap(b);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
