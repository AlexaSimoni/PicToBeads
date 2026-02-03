package com.example.pictobeads;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity serves as the entry point of the application, providing navigation to different design modules.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Initializes the activity, sets the content view, and configures navigation buttons.
     * Input: savedInstanceState - Bundle containing the activity's previously saved state.
     * Output: None.
     * Algorithm: Inflates the activity_start layout, finds the bracelet and picture buttons by their IDs, and attaches click listeners that start their respective activities.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        ImageButton btnBracelet = findViewById(R.id.btn_bracelet);
        ImageButton btnPicture = findViewById(R.id.btn_picture);

        btnBracelet.setOnClickListener(v -> {
            startActivity(new Intent(this, BraceletActivity.class));
        });

        btnPicture.setOnClickListener(v -> {
            startActivity(new Intent(this, PictureActivity.class));
        });
    }
}
