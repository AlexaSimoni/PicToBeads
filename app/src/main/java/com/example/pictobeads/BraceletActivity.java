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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
 * Activity for designing bead bracelets. Supports movement (panning), undo/redo, 
 * and color adjustments.
 */
public class BraceletActivity extends AppCompatActivity {

    private b_gradle currentGridView;
    private FrameLayout gridContainer;
    private ImageView previewImage;
    private SeekBar seekWidth;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private float braceletWidthMm = 20f;
    private final float braceletLengthMm = 160f;
    private float zoomFactor = 15f; 
    
    private int currentPatternType = 0; 
    private Bead selectedBead = null;
    private int colorLimit = 20;

    private final LinkedList<BraceletState> undoStack = new LinkedList<>();
    private final LinkedList<BraceletState> redoStack = new LinkedList<>();
    private boolean isInternalChange = false;

    private LinearLayout colorVarietyToolbar;
    private TextView tvVarietyValue;
    private RecyclerView rvPalette;

    // Panning variables
    private float posX = 0, posY = 0;
    private float lastX, lastY;

    private static class BraceletState {
        float width;
        int patternType, beadIndex, colorLimit;
        Bitmap bitmap;

        BraceletState(float width, int patternType, int beadIndex, int colorLimit, Bitmap bitmap) {
            this.width = width; this.patternType = patternType;
            this.beadIndex = beadIndex; this.colorLimit = colorLimit;
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
        headerView.findViewById(R.id.btn_header_save_image).setOnClickListener(v -> saveScreenshotToGallery());
        headerView.findViewById(R.id.btn_header_save_pattern).setOnClickListener(v -> saveProjectForEditing());

        // Setup Color Variety Sidebar
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

        // Setup Panning Logic on the workspace
        View workspace = findViewById(R.id.grid_workspace);
        View.OnTouchListener panListener = (v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getRawX();
                    lastY = event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - lastX;
                    float deltaY = event.getRawY() - lastY;
                    posX += deltaX;
                    posY += deltaY;
                    gridContainer.setTranslationX(posX);
                    gridContainer.setTranslationY(posY);
                    lastX = event.getRawX();
                    lastY = event.getRawY();
                    break;
                case MotionEvent.ACTION_UP:
                    if (Math.abs(event.getRawX() - lastX) < 10 && Math.abs(event.getRawY() - lastY) < 10) {
                        if (colorVarietyToolbar.getVisibility() == View.VISIBLE) colorVarietyToolbar.setVisibility(View.GONE);
                    }
                    break;
            }
            return true;
        };

        // Attach panning to workspace AND the container itself so the design is movable
        workspace.setOnTouchListener(panListener);
        gridContainer.setOnTouchListener(panListener);

        FrameLayout patternContainer = findViewById(R.id.pattern_toolbar_container);
        View patternView = LayoutInflater.from(this).inflate(R.layout.partial_bracelet_patterns, patternContainer, true);
        
        patternView.findViewById(R.id.btn_math_grid).setOnClickListener(v -> { saveCurrentState(); currentPatternType = 0; updateGrid(); });
        patternView.findViewById(R.id.btn_vertical_brick).setOnClickListener(v -> { saveCurrentState(); currentPatternType = 1; updateGrid(); });
        patternView.findViewById(R.id.btn_missing_brick).setOnClickListener(v -> { saveCurrentState(); currentPatternType = 2; updateGrid(); });
        patternView.findViewById(R.id.btn_math_grid_2).setOnClickListener(v -> { saveCurrentState(); currentPatternType = 3; updateGrid(); });

        FrameLayout sliderContainer = findViewById(R.id.bottom_controls_container);
        View sliderView = LayoutInflater.from(this).inflate(R.layout.partial_width_slider, sliderContainer, true);
        seekWidth = sliderView.findViewById(R.id.seek_control);
        seekWidth.setProgress((int)braceletWidthMm);
        seekWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) { if(f) { saveCurrentState(); braceletWidthMm = Math.max(5, p); updateGrid(); } }
            @Override public void onStartTrackingTouch(SeekBar s) {} @Override public void onStopTrackingTouch(SeekBar s) {}
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
            if (i < beads.size()) findViewById(beadIds[i]).setOnClickListener(v -> { saveCurrentState(); selectedBead = beads.get(index); updateGrid(); });
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
        int beadIdx = Bead.getStandardTypes().indexOf(selectedBead);
        undoStack.push(new BraceletState(braceletWidthMm, currentPatternType, Bead.getStandardTypes().indexOf(selectedBead), colorLimit, currentBmp));
        redoStack.clear(); if (undoStack.size() > 20) undoStack.removeLast();
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        isInternalChange = true;
        Bitmap currentBmp = (previewImage.getDrawable() instanceof BitmapDrawable) ? ((BitmapDrawable) previewImage.getDrawable()).getBitmap() : null;
        redoStack.push(new BraceletState(braceletWidthMm, currentPatternType, Bead.getStandardTypes().indexOf(selectedBead), colorLimit, currentBmp));
        restoreState(undoStack.pop()); isInternalChange = false;
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        isInternalChange = true;
        Bitmap currentBmp = (previewImage.getDrawable() instanceof BitmapDrawable) ? ((BitmapDrawable) previewImage.getDrawable()).getBitmap() : null;
        undoStack.push(new BraceletState(braceletWidthMm, currentPatternType, Bead.getStandardTypes().indexOf(selectedBead), colorLimit, currentBmp));
        restoreState(redoStack.pop()); isInternalChange = false;
    }

    private void restoreState(BraceletState state) {
        this.braceletWidthMm = state.width; this.currentPatternType = state.patternType;
        this.colorLimit = state.colorLimit; this.selectedBead = Bead.getStandardTypes().get(Math.max(0, state.beadIndex));
        seekWidth.setProgress((int)state.width);
        tvVarietyValue.setText(String.valueOf(colorLimit));
        if (state.bitmap != null) { previewImage.setImageBitmap(state.bitmap); previewImage.setVisibility(View.VISIBLE); }
        else { previewImage.setImageDrawable(null); previewImage.setVisibility(View.GONE); }
        updateGrid(); updatePaletteList();
    }

    private void saveScreenshotToGallery() {
        View view = findViewById(R.id.main_root);
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, "PicToBeads_Screenshot_" + System.currentTimeMillis() + ".png");
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Screenshots");
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
        if (uri != null) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                Toast.makeText(this, "Screenshot saved", Toast.LENGTH_SHORT).show();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void saveProjectForEditing() {
        try {
            File dir = new File(getExternalFilesDir(null), "SavedPatterns");
            if (!dir.exists()) dir.mkdirs();
            String id = "BRACELET_" + System.currentTimeMillis();
            if (gridContainer.getChildCount() > 0) {
                View grid = gridContainer.getChildAt(0);
                Bitmap snapshot = Bitmap.createBitmap(grid.getWidth(), grid.getHeight(), Bitmap.Config.ARGB_8888);
                grid.draw(new Canvas(snapshot));
                File thumbFile = new File(dir, id + "_thumb.png");
                try (FileOutputStream fos = new FileOutputStream(thumbFile)) { snapshot.compress(Bitmap.CompressFormat.PNG, 100, fos); }
            }
            File dataFile = new File(dir, id + ".txt");
            try (FileOutputStream fos = new FileOutputStream(dataFile)) {
                String data = "BRACELET|" + braceletWidthMm + "|" + currentPatternType + "|" + Bead.getStandardTypes().indexOf(selectedBead) + "|" + colorLimit;
                fos.write(data.getBytes());
            }
            if (previewImage.getDrawable() instanceof BitmapDrawable) {
                File imgFile = new File(dir, id + ".png");
                try (FileOutputStream fos = new FileOutputStream(imgFile)) { ((BitmapDrawable) previewImage.getDrawable()).getBitmap().compress(Bitmap.CompressFormat.PNG, 100, fos); }
            }
            Toast.makeText(this, "Project saved", Toast.LENGTH_SHORT).show();
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
                if (parts[0].equals("BRACELET")) {
                    this.braceletWidthMm = Float.parseFloat(parts[1]); this.currentPatternType = Integer.parseInt(parts[2]);
                    this.selectedBead = Bead.getStandardTypes().get(Integer.parseInt(parts[3]));
                    if (parts.length > 4) this.colorLimit = Integer.parseInt(parts[4]);
                    seekWidth.setProgress((int)this.braceletWidthMm);
                    tvVarietyValue.setText(String.valueOf(this.colorLimit));
                }
            }
            File imgFile = new File(dir, id + ".png");
            if (imgFile.exists()) {
                Bitmap b = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                previewImage.setImageBitmap(b); previewImage.setVisibility(View.VISIBLE);
            }
            updateGrid(); updatePaletteList();
        } catch (Exception e) { e.printStackTrace(); }
        isInternalChange = false;
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
        currentGridView.setBead(selectedBead); currentGridView.setMaxColors(colorLimit);
        gridContainer.addView(currentGridView);
        
        // Ensure starting position is centered but movable
        posX = 0; posY = 0;
        gridContainer.setTranslationX(0); gridContainer.setTranslationY(0);

        Bitmap b = (previewImage.getDrawable() instanceof BitmapDrawable) ? ((BitmapDrawable) previewImage.getDrawable()).getBitmap() : null;
        if (b != null) currentGridView.setImageData(b);
    }

    private void updateGridWithBitmap(Bitmap bitmap) {
        if (bitmap == null) return;
        previewImage.setImageBitmap(bitmap); previewImage.setVisibility(View.VISIBLE);
        updateGrid();
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
