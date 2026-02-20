package com.example.pictobeads;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import com.example.pictobeads.R;

/**
 * Activity for designing bead bracelets with different patterns.
 * Supports undo/redo, saving images, and saving editable patterns.
 */
public class BraceletActivity extends AppCompatActivity {

    private b_gradle currentGridView;
    private FrameLayout gridContainer;
    private ImageView previewImage;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private float braceletWidthMm = 20f;
    private final float braceletLengthMm = 160f;
    private float zoomFactor = 15f; 
    
    private int currentPatternType = 0; 
    private Bead selectedBead = null;

    // Undo/Redo Stacks
    private final LinkedList<BraceletState> undoStack = new LinkedList<>();
    private final LinkedList<BraceletState> redoStack = new LinkedList<>();
    private boolean isInternalChange = false;

    /**
     * State object for undo/redo and pattern persistence.
     */
    private static class BraceletState {
        float width;
        int patternType;
        int beadIndex;
        Bitmap bitmap;

        BraceletState(float width, int patternType, int beadIndex, Bitmap bitmap) {
            this.width = width;
            this.patternType = patternType;
            this.beadIndex = beadIndex;
            this.bitmap = bitmap;
        }
    }

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
        
        headerView.findViewById(R.id.btn_header_undo).setOnClickListener(v -> undo());
        headerView.findViewById(R.id.btn_header_redo).setOnClickListener(v -> redo());
        headerView.findViewById(R.id.btn_header_save_image).setOnClickListener(v -> saveImageToGallery());
        headerView.findViewById(R.id.btn_header_save_pattern).setOnClickListener(v -> savePatternForEditing());

        FrameLayout patternContainer = findViewById(R.id.pattern_toolbar_container);
        View patternView = LayoutInflater.from(this).inflate(R.layout.partial_bracelet_patterns, patternContainer, true);
        
        patternView.findViewById(R.id.btn_math_grid).setOnClickListener(v -> { saveCurrentState(); currentPatternType = 0; updateGrid(); });
        patternView.findViewById(R.id.btn_vertical_brick).setOnClickListener(v -> { saveCurrentState(); currentPatternType = 1; updateGrid(); });
        patternView.findViewById(R.id.btn_missing_brick).setOnClickListener(v -> { saveCurrentState(); currentPatternType = 2; updateGrid(); });
        patternView.findViewById(R.id.btn_math_grid_2).setOnClickListener(v -> { saveCurrentState(); currentPatternType = 3; updateGrid(); });

        FrameLayout sliderContainer = findViewById(R.id.bottom_controls_container);
        View sliderView = LayoutInflater.from(this).inflate(R.layout.partial_width_slider, sliderContainer, true);
        SeekBar seek = sliderView.findViewById(R.id.seek_control);
        seek.setProgress((int)braceletWidthMm);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) { if(f) { saveCurrentState(); braceletWidthMm = Math.max(5, p); updateGrid(); } }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) { saveCurrentState(); handleSelectedImage(result.getData().getData()); }
        });
        headerView.findViewById(R.id.btn_header_upload).setOnClickListener(v -> imagePickerLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)));
        headerView.findViewById(R.id.btn_header_heart).setOnClickListener(v -> { saveCurrentState(); updateGridWithBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.heart)); });

        List<Bead> beads = Bead.getStandardTypes();
        selectedBead = beads.get(0);
        int[] beadIds = {R.id.btn_bead_1, R.id.btn_bead_2, R.id.btn_bead_3, R.id.btn_bead_4, R.id.btn_bead_5, R.id.btn_bead_6, R.id.btn_bead_7};
        for (int i = 0; i < beadIds.length; i++) {
            final int index = i;
            if (i < beads.size()) {
                findViewById(beadIds[i]).setOnClickListener(v -> { saveCurrentState(); selectedBead = beads.get(index); updateGrid(); });
            }
        }

        updateGrid();
    }

    private void saveCurrentState() {
        if (isInternalChange) return;
        Bitmap currentBmp = (previewImage.getDrawable() instanceof BitmapDrawable) ? ((BitmapDrawable) previewImage.getDrawable()).getBitmap() : null;
        int beadIdx = Bead.getStandardTypes().indexOf(selectedBead);
        undoStack.push(new BraceletState(braceletWidthMm, currentPatternType, beadIdx, currentBmp));
        redoStack.clear();
        if (undoStack.size() > 20) undoStack.removeLast();
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        isInternalChange = true;
        Bitmap currentBmp = (previewImage.getDrawable() instanceof BitmapDrawable) ? ((BitmapDrawable) previewImage.getDrawable()).getBitmap() : null;
        redoStack.push(new BraceletState(braceletWidthMm, currentPatternType, Bead.getStandardTypes().indexOf(selectedBead), currentBmp));
        restoreState(undoStack.pop());
        isInternalChange = false;
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        isInternalChange = true;
        Bitmap currentBmp = (previewImage.getDrawable() instanceof BitmapDrawable) ? ((BitmapDrawable) previewImage.getDrawable()).getBitmap() : null;
        undoStack.push(new BraceletState(braceletWidthMm, currentPatternType, Bead.getStandardTypes().indexOf(selectedBead), currentBmp));
        restoreState(redoStack.pop());
        isInternalChange = false;
    }

    private void restoreState(BraceletState state) {
        this.braceletWidthMm = state.width;
        this.currentPatternType = state.patternType;
        this.selectedBead = Bead.getStandardTypes().get(Math.max(0, state.beadIndex));
        if (state.bitmap != null) { previewImage.setImageBitmap(state.bitmap); previewImage.setVisibility(View.VISIBLE); }
        else { previewImage.setImageDrawable(null); previewImage.setVisibility(View.GONE); }
        updateGrid();
    }

    /**
     * Saves the visual grid as a PNG image in the gallery.
     */
    private void saveImageToGallery() {
        if (gridContainer.getChildCount() == 0) return;
        View view = gridContainer.getChildAt(0);
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, "PicToBeads_" + System.currentTimeMillis() + ".png");
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        v.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PicToBeads");
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
        if (uri != null) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                Toast.makeText(this, "Image Saved", Toast.LENGTH_SHORT).show();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    /**
     * Saves the current design parameters and source image to a private app directory for future editing.
     */
    private void savePatternForEditing() {
        try {
            File dir = new File(getExternalFilesDir(null), "SavedPatterns");
            if (!dir.exists()) dir.mkdirs();
            String id = "pattern_" + System.currentTimeMillis();
            File dataFile = new File(dir, id + ".txt");
            try (FileOutputStream fos = new FileOutputStream(dataFile)) {
                String data = braceletWidthMm + "|" + currentPatternType + "|" + Bead.getStandardTypes().indexOf(selectedBead);
                fos.write(data.getBytes());
            }
            if (previewImage.getDrawable() instanceof BitmapDrawable) {
                File imgFile = new File(dir, id + ".png");
                try (FileOutputStream fos = new FileOutputStream(imgFile)) {
                    ((BitmapDrawable) previewImage.getDrawable()).getBitmap().compress(Bitmap.CompressFormat.PNG, 100, fos);
                }
            }
            Toast.makeText(this, "Pattern Saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show(); }
    }

    private void updateGrid() {
        if (selectedBead == null) return;
        float bSize = selectedBead.getSize();
        int cols = Math.max(1, (int) (braceletWidthMm / bSize));
        int rows = Math.max(1, (int) (braceletLengthMm / bSize));
        int res = (int) (bSize * zoomFactor);
        gridContainer.removeAllViews();
        if (currentPatternType == 0) currentGridView = new math_gradle(this, cols, rows, res, 1);
        else if (currentPatternType == 1) currentGridView = new brick_gradle(this, cols, rows, res, 1, true);
        else if (currentPatternType == 2) currentGridView = new missing_brick_gradle(this, cols, rows, res, 1);
        else if (currentPatternType == 3) currentGridView = new vertical_staggered_missing_gradle(this, cols, rows, res, 1);
        currentGridView.setBead(selectedBead);
        gridContainer.addView(currentGridView);
        Bitmap b = (previewImage.getDrawable() instanceof BitmapDrawable) ? ((BitmapDrawable) previewImage.getDrawable()).getBitmap() : null;
        if (b != null) currentGridView.setImageData(b);
    }

    private void updateGridWithBitmap(Bitmap bitmap) {
        if (bitmap == null) return;
        previewImage.setImageBitmap(bitmap);
        previewImage.setVisibility(View.VISIBLE);
        if (currentGridView != null) currentGridView.setImageData(bitmap);
    }

    private void handleSelectedImage(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            updateGridWithBitmap(BitmapFactory.decodeStream(is));
        } catch (Exception e) { e.printStackTrace(); }
    }
}
