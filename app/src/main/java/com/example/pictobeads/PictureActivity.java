package com.example.pictobeads;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.example.pictobeads.R;

/**
 * Activity for creating mosaic patterns from images.
 */
public class PictureActivity extends AppCompatActivity {

    private b_gradle currentGridView;
    private FrameLayout gridContainer;
    private ImageView previewImage;
    private EditText etWidth, etHeight;
    private SeekBar seekZoom;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private float pictureWidthMm = 200f;
    private float pictureHeightMm = 200f;
    private float zoomFactor = 10f; 
    
    private int currentPatternType = 0; 
    private Bead selectedBead = null;
    private boolean isRemoveBgActive = false;
    private int colorLimit = 20;

    private final LinkedList<PictureState> undoStack = new LinkedList<>();
    private final LinkedList<PictureState> redoStack = new LinkedList<>();
    private boolean isInternalChange = false;

    private LinearLayout colorVarietyToolbar;
    private TextView tvVarietyValue;
    private RecyclerView rvPalette;

    private static class PictureState {
        float width, height, zoom;
        int patternType, beadIndex, colorLimit;
        boolean removeBg;
        Bitmap bitmap;

        PictureState(float width, float height, float zoom, int patternType, int beadIndex, int colorLimit, boolean removeBg, Bitmap bitmap) {
            this.width = width; this.height = height; this.zoom = zoom;
            this.patternType = patternType; this.beadIndex = beadIndex;
            this.colorLimit = colorLimit; this.removeBg = removeBg; this.bitmap = bitmap;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridContainer = findViewById(R.id.grid_container);
        previewImage = findViewById(R.id.preview_image);
        findViewById(R.id.btn_header_back).setOnClickListener(v -> finish());

        // Header and Sidebar Setup
        View headerView = LayoutInflater.from(this).inflate(R.layout.partial_picture_header, (LinearLayout)findViewById(R.id.header_actions_container), true);
        headerView.findViewById(R.id.btn_header_undo).setOnClickListener(v -> undo());
        headerView.findViewById(R.id.btn_header_redo).setOnClickListener(v -> redo());
        headerView.findViewById(R.id.btn_header_save_image).setOnClickListener(v -> saveScreenshotToGallery());
        headerView.findViewById(R.id.btn_header_save_pattern).setOnClickListener(v -> saveProjectForEditing());

        // COLOR VARIETY SIDEBAR LOGIC
        colorVarietyToolbar = findViewById(R.id.color_variety_toolbar);
        tvVarietyValue = findViewById(R.id.tv_variety_value);
        rvPalette = findViewById(R.id.rv_color_palette);
        rvPalette.setLayoutManager(new LinearLayoutManager(this));
        
        findViewById(R.id.btn_color_settlement).setOnClickListener(v -> {
            colorVarietyToolbar.setVisibility(View.VISIBLE);
            tvVarietyValue.setText(String.valueOf(colorLimit));
            updatePaletteList();
        });

        findViewById(R.id.btn_variety_inc).setOnClickListener(v -> {
            if (colorLimit < 64) { colorLimit++; tvVarietyValue.setText(String.valueOf(colorLimit)); updateGrid(); updatePaletteList(); }
        });
        findViewById(R.id.btn_variety_dec).setOnClickListener(v -> {
            if (colorLimit > 0) { colorLimit--; tvVarietyValue.setText(String.valueOf(colorLimit)); updateGrid(); updatePaletteList(); }
        });

        // Hide toolbar when grid is tapped
        gridContainer.setOnClickListener(v -> {
            if (colorVarietyToolbar.getVisibility() == View.VISIBLE) {
                colorVarietyToolbar.setVisibility(View.GONE);
            }
        });

        // Pattern and Controls Setup
        View patternView = LayoutInflater.from(this).inflate(R.layout.partial_picture_patterns, (FrameLayout)findViewById(R.id.pattern_toolbar_container), true);
        patternView.findViewById(R.id.btn_math_grid).setOnClickListener(v -> { saveCurrentState(); currentPatternType = 0; updateGrid(); updatePaletteList(); });
        patternView.findViewById(R.id.btn_vertical_grid).setOnClickListener(v -> { saveCurrentState(); currentPatternType = 1; updateGrid(); updatePaletteList(); });
        patternView.findViewById(R.id.btn_horizontal_grid).setOnClickListener(v -> { saveCurrentState(); currentPatternType = 2; updateGrid(); updatePaletteList(); });
        patternView.findViewById(R.id.btn_math_grid_2).setOnClickListener(v -> { saveCurrentState(); currentPatternType = 3; updateGrid(); updatePaletteList(); });
        patternView.findViewById(R.id.btn_remove_background).setOnClickListener(v -> {
            saveCurrentState(); isRemoveBgActive = !isRemoveBgActive;
            if (currentGridView != null) currentGridView.setRemoveBackground(isRemoveBgActive);
            updatePaletteList();
        });

        View controlsView = LayoutInflater.from(this).inflate(R.layout.partial_picture_controls, (FrameLayout)findViewById(R.id.bottom_controls_container), true);
        etWidth = controlsView.findViewById(R.id.et_picture_width);
        etHeight = controlsView.findViewById(R.id.et_picture_height);
        seekZoom = controlsView.findViewById(R.id.seek_control);

        TextWatcher dw = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isInternalChange) { try { saveCurrentState(); pictureWidthMm = Float.parseFloat(etWidth.getText().toString()); pictureHeightMm = Float.parseFloat(etHeight.getText().toString()); updateGrid(); updatePaletteList(); } catch (Exception e) {} }
            }
        };
        etWidth.addTextChangedListener(dw); etHeight.addTextChangedListener(dw);
        seekZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) { if (f) { saveCurrentState(); zoomFactor = 5f + (p / 100f) * 25f; updateGrid(); updatePaletteList(); } }
            @Override public void onStartTrackingTouch(SeekBar s) {} @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) { saveCurrentState(); handleSelectedImage(result.getData().getData()); }
        });
        headerView.findViewById(R.id.btn_header_upload).setOnClickListener(v -> imagePickerLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)));
        headerView.findViewById(R.id.btn_header_heart).setOnClickListener(v -> { saveCurrentState(); updateGridWithBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.heart)); });

        List<Bead> beads = Bead.getStandardTypes();
        selectedBead = beads.get(0);
        int[] bIds = {R.id.btn_bead_1, R.id.btn_bead_2, R.id.btn_bead_3, R.id.btn_bead_4, R.id.btn_bead_5, R.id.btn_bead_6, R.id.btn_bead_7};
        for (int i = 0; i < bIds.length; i++) {
            final int idx = i;
            if (i < beads.size()) findViewById(bIds[i]).setOnClickListener(v -> { saveCurrentState(); selectedBead = beads.get(idx); updateGrid(); updatePaletteList(); });
        }

        String loadPath = getIntent().getStringExtra("LOAD_PATH");
        if (loadPath != null) loadProject(loadPath);
        else updateGrid();
    }

    private void updatePaletteList() {
        if (currentGridView != null) {
            rvPalette.setAdapter(new PaletteAdapter(currentGridView.getCurrentPalette()));
        }
    }

    private void saveCurrentState() {
        if (isInternalChange) return;
        Bitmap currentBmp = (previewImage.getDrawable() instanceof BitmapDrawable) ? ((BitmapDrawable) previewImage.getDrawable()).getBitmap() : null;
        undoStack.push(new PictureState(pictureWidthMm, pictureHeightMm, zoomFactor, currentPatternType, Bead.getStandardTypes().indexOf(selectedBead), colorLimit, isRemoveBgActive, currentBmp));
        redoStack.clear(); if (undoStack.size() > 20) undoStack.removeLast();
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        isInternalChange = true;
        Bitmap currentBmp = (previewImage.getDrawable() instanceof BitmapDrawable) ? ((BitmapDrawable) previewImage.getDrawable()).getBitmap() : null;
        redoStack.push(new PictureState(pictureWidthMm, pictureHeightMm, zoomFactor, currentPatternType, Bead.getStandardTypes().indexOf(selectedBead), colorLimit, isRemoveBgActive, currentBmp));
        restoreState(undoStack.pop()); isInternalChange = false;
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        isInternalChange = true;
        Bitmap currentBmp = (previewImage.getDrawable() instanceof BitmapDrawable) ? ((BitmapDrawable) previewImage.getDrawable()).getBitmap() : null;
        undoStack.push(new PictureState(pictureWidthMm, pictureHeightMm, zoomFactor, currentPatternType, Bead.getStandardTypes().indexOf(selectedBead), colorLimit, isRemoveBgActive, currentBmp));
        restoreState(redoStack.pop()); isInternalChange = false;
    }

    private void restoreState(PictureState state) {
        this.pictureWidthMm = state.width; this.pictureHeightMm = state.height; this.zoomFactor = state.zoom;
        this.currentPatternType = state.patternType; this.selectedBead = Bead.getStandardTypes().get(Math.max(0, state.beadIndex));
        this.colorLimit = state.colorLimit; this.isRemoveBgActive = state.removeBg;
        etWidth.setText(String.valueOf((int)state.width)); etHeight.setText(String.valueOf((int)state.height));
        seekZoom.setProgress((int)((state.zoom - 5f) / 25f * 100f));
        if (state.bitmap != null) { previewImage.setImageBitmap(state.bitmap); previewImage.setVisibility(View.VISIBLE); }
        else { previewImage.setImageDrawable(null); previewImage.setVisibility(View.GONE); }
        updateGrid();
        updatePaletteList();
    }

    private void saveScreenshotToGallery() {
        View view = findViewById(R.id.main_root);
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, "PicToBeads_Mosaic_" + System.currentTimeMillis() + ".png");
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Screenshots");
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
        if (uri != null) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                Toast.makeText(this, "Screenshot Saved", Toast.LENGTH_SHORT).show();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void saveProjectForEditing() {
        try {
            File dir = new File(getExternalFilesDir(null), "SavedPatterns");
            if (!dir.exists()) dir.mkdirs();
            String id = "PICTURE_" + System.currentTimeMillis();
            if (gridContainer.getChildCount() > 0) {
                View grid = gridContainer.getChildAt(0);
                Bitmap snapshot = Bitmap.createBitmap(grid.getWidth(), grid.getHeight(), Bitmap.Config.ARGB_8888);
                grid.draw(new Canvas(snapshot));
                File thumbFile = new File(dir, id + "_thumb.png");
                try (FileOutputStream fos = new FileOutputStream(thumbFile)) { snapshot.compress(Bitmap.CompressFormat.PNG, 100, fos); }
            }
            File dataFile = new File(dir, id + ".txt");
            try (FileOutputStream fos = new FileOutputStream(dataFile)) {
                String data = "PICTURE|" + pictureWidthMm + "|" + pictureHeightMm + "|" + zoomFactor + "|" + currentPatternType + "|" + Bead.getStandardTypes().indexOf(selectedBead) + "|" + isRemoveBgActive + "|" + colorLimit;
                fos.write(data.getBytes());
            }
            if (previewImage.getDrawable() instanceof BitmapDrawable) {
                File imgFile = new File(dir, id + ".png");
                try (FileOutputStream fos = new FileOutputStream(imgFile)) { ((BitmapDrawable) previewImage.getDrawable()).getBitmap().compress(Bitmap.CompressFormat.PNG, 100, fos); }
            }
            Toast.makeText(this, "Project Saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show(); }
    }

    private void loadProject(String id) {
        isInternalChange = true;
        try {
            File dir = new File(getExternalFilesDir(null), "SavedPatterns");
            File dataFile = new File(dir, id + ".txt");
            if (dataFile.exists()) {
                FileInputStream fis = new FileInputStream(dataFile);
                byte[] buffer = new byte[(int) dataFile.length()]; fis.read(buffer); fis.close();
                String[] parts = new String(buffer).split("\\|");
                if (parts[0].equals("PICTURE")) {
                    this.pictureWidthMm = Float.parseFloat(parts[1]); this.pictureHeightMm = Float.parseFloat(parts[2]);
                    this.zoomFactor = Float.parseFloat(parts[3]); this.currentPatternType = Integer.parseInt(parts[4]);
                    this.selectedBead = Bead.getStandardTypes().get(Integer.parseInt(parts[5]));
                    this.isRemoveBgActive = Boolean.parseBoolean(parts[6]);
                    if (parts.length > 7) this.colorLimit = Integer.parseInt(parts[7]);
                    etWidth.setText(String.valueOf((int)this.pictureWidthMm)); etHeight.setText(String.valueOf((int)this.pictureHeightMm));
                    seekZoom.setProgress((int)((this.zoomFactor - 5f) / 25f * 100f));
                }
            }
            File imgFile = new File(dir, id + ".png");
            if (imgFile.exists()) { previewImage.setImageBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath())); previewImage.setVisibility(View.VISIBLE); }
            updateGrid();
            updatePaletteList();
        } catch (Exception e) { e.printStackTrace(); }
        isInternalChange = false;
    }

    private void updateGrid() {
        if (selectedBead == null) return;
        float bSize = selectedBead.getSize();
        int cols = Math.max(1, (int) (pictureWidthMm / bSize));
        int rows = Math.max(1, (int) (pictureHeightMm / bSize));
        int res = (int) (bSize * zoomFactor);
        gridContainer.removeAllViews();
        if (currentPatternType == 0) currentGridView = new math_gradle(this, cols, rows, res, 1);
        else if (currentPatternType == 1) currentGridView = new brick_gradle(this, cols, rows, res, 1, true);
        else if (currentPatternType == 2) currentGridView = new brick_gradle(this, cols, rows, res, 1, false);
        else if (currentPatternType == 3) currentGridView = new vertical_staggered_missing_gradle(this, cols, rows, res, 1);
        currentGridView.setBead(selectedBead);
        currentGridView.setRemoveBackground(isRemoveBgActive);
        currentGridView.setMaxColors(colorLimit);
        gridContainer.addView(currentGridView);
        Bitmap b = (previewImage.getDrawable() instanceof BitmapDrawable) ? ((BitmapDrawable) previewImage.getDrawable()).getBitmap() : null;
        if (b != null) currentGridView.setImageData(b);
    }

    private void updateGridWithBitmap(Bitmap bitmap) {
        if (bitmap == null) return;
        previewImage.setImageBitmap(bitmap); previewImage.setVisibility(View.VISIBLE);
        updateGrid();
        updatePaletteList();
    }

    private void handleSelectedImage(Uri uri) {
        try { InputStream is = getContentResolver().openInputStream(uri); updateGridWithBitmap(BitmapFactory.decodeStream(is)); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private static class PaletteAdapter extends RecyclerView.Adapter<PaletteAdapter.ViewHolder> {
        private final List<Integer> colors;
        PaletteAdapter(List<Integer> colors) { this.colors = colors; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_color_circle, p, false); return new ViewHolder(v);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) { h.viewCircle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colors.get(pos))); }
        @Override public int getItemCount() { return colors != null ? colors.size() : 0; }
        static class ViewHolder extends RecyclerView.ViewHolder {
            View viewCircle;
            ViewHolder(View v) { super(v); viewCircle = v.findViewById(R.id.view_color_circle); }
        }
    }
}
