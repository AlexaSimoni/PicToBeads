package com.example.b;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity is now the "Front Door" of your app.
 * It shows the selection menu (Bracelet vs Picture).
 */
public class MainActivity extends AppCompatActivity {

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
