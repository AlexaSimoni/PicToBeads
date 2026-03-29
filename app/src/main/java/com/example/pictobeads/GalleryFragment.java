package com.example.pictobeads;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment to display saved images from the app's gallery.
 */
public class GalleryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);
        RecyclerView rv = view.findViewById(R.id.rv_gallery);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 3));
        
        List<Uri> images = loadImages();
        rv.setAdapter(new GalleryAdapter(images));
        
        return view;
    }

    /**
     * Queries MediaStore for images saved in the PicToBeads folder.
     * Input: None.
     * Output: List of Uris pointing to the images.
     * Algorithm: Queries the MediaStore images collection with a filter for the specific relative path.
     */
    private List<Uri> loadImages() {
        List<Uri> uriList = new ArrayList<>();
        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{MediaStore.Images.Media._ID};
        String selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
        String[] selectionArgs = new String[]{"%Pictures/PicToBeads%"};

        try (Cursor cursor = getContext().getContentResolver().query(collection, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                do {
                    long id = cursor.getLong(idColumn);
                    Uri contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
                    uriList.add(contentUri);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) { e.printStackTrace(); }
        return uriList;
    }

    private static class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
        private final List<Uri> images;

        GalleryAdapter(List<Uri> images) { this.images = images; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 300));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new ViewHolder(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ((ImageView)holder.itemView).setImageURI(images.get(position));
        }

        @Override
        public int getItemCount() { return images.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View v) { super(v); }
        }
    }
}
