package com.example.pictobeads;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fragment that displays a gallery of saved projects (patterns) for editing.
 * Supports multi-selection and deletion of projects.
 * Displays the most recent projects first.
 */
public class ProjectsFragment extends Fragment {

    private RecyclerView rv;
    private List<ProjectItem> projects;
    private final Set<String> selectedProjectIds = new HashSet<>();
    private ImageButton btnDelete;
    private ProjectsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_projects, container, false);
        
        view.findViewById(R.id.btn_gallery_back).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).goToStart();
            }
        });

        btnDelete = view.findViewById(R.id.btn_delete_project);
        btnDelete.setOnClickListener(v -> deleteSelectedProjects());

        rv = view.findViewById(R.id.rv_projects);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 4));
        
        refreshProjects();
        
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshProjects();
    }

    private void refreshProjects() {
        if (rv == null) return;
        projects = loadProjectItems();
        
        // Sort projects by last modified date (descending) to show recent first
        Collections.sort(projects, (p1, p2) -> Long.compare(p2.lastModified, p1.lastModified));
        
        selectedProjectIds.clear();
        if (btnDelete != null) btnDelete.setVisibility(View.GONE);
        
        adapter = new ProjectsAdapter(projects, new ProjectsAdapter.OnProjectClickListener() {
            @Override
            public void onProjectClick(ProjectItem project) {
                if (!selectedProjectIds.isEmpty()) {
                    toggleSelection(project.id);
                } else {
                    openProject(project.id);
                }
            }

            @Override
            public void onProjectLongClick(ProjectItem project) {
                toggleSelection(project.id);
            }
        });
        rv.setAdapter(adapter);
    }

    private void toggleSelection(String id) {
        if (selectedProjectIds.contains(id)) {
            selectedProjectIds.remove(id);
        } else {
            selectedProjectIds.add(id);
        }
        
        btnDelete.setVisibility(selectedProjectIds.isEmpty() ? View.GONE : View.VISIBLE);
        adapter.setSelectedIds(selectedProjectIds);
    }

    private void openProject(String id) {
        Intent intent;
        if (id.startsWith("BRACELET")) {
            intent = new Intent(getContext(), BraceletActivity.class);
        } else {
            intent = new Intent(getContext(), PictureActivity.class);
        }
        intent.putExtra("LOAD_PATH", id);
        startActivity(intent);
    }

    private void deleteSelectedProjects() {
        File dir = new File(getContext().getExternalFilesDir(null), "SavedPatterns");
        for (String id : selectedProjectIds) {
            File txtFile = new File(dir, id + ".txt");
            File pngFile = new File(dir, id + ".png");
            File thumbFile = new File(dir, id + "_thumb.png");
            if (txtFile.exists()) txtFile.delete();
            if (pngFile.exists()) pngFile.delete();
            if (thumbFile.exists()) thumbFile.delete();
        }
        refreshProjects();
    }

    private List<ProjectItem> loadProjectItems() {
        List<ProjectItem> list = new ArrayList<>();
        File dir = new File(getContext().getExternalFilesDir(null), "SavedPatterns");
        if (dir.exists()) {
            // Filter by thumb files to represent the pattern snapshots
            File[] files = dir.listFiles((d, name) -> name.endsWith("_thumb.png"));
            if (files != null) {
                for (File f : files) {
                    String id = f.getName().replace("_thumb.png", "");
                    list.add(new ProjectItem(id, f.getAbsolutePath(), f.lastModified()));
                }
            }
        }
        return list;
    }

    private static class ProjectItem {
        String id, imagePath;
        long lastModified;
        ProjectItem(String id, String imagePath, long lastModified) { 
            this.id = id; 
            this.imagePath = imagePath; 
            this.lastModified = lastModified;
        }
    }

    private static class ProjectsAdapter extends RecyclerView.Adapter<ProjectsAdapter.ViewHolder> {
        private final List<ProjectItem> items;
        private final OnProjectClickListener listener;
        private final Set<String> selectedIds = new HashSet<>();

        interface OnProjectClickListener { 
            void onProjectClick(ProjectItem item);
            void onProjectLongClick(ProjectItem item);
        }

        ProjectsAdapter(List<ProjectItem> items, OnProjectClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        void setSelectedIds(Set<String> ids) {
            this.selectedIds.clear();
            this.selectedIds.addAll(ids);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProjectItem item = items.get(position);
            Bitmap bmp = BitmapFactory.decodeFile(item.imagePath);
            holder.ivThumbnail.setImageBitmap(bmp);
            
            boolean isSelected = selectedIds.contains(item.id);
            holder.ivCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            holder.itemView.setAlpha(isSelected ? 0.6f : 1.0f);
            
            holder.itemView.setOnClickListener(v -> listener.onProjectClick(item));
            holder.itemView.setOnLongClickListener(v -> {
                listener.onProjectLongClick(item);
                return true;
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivThumbnail, ivCheck;
            ViewHolder(View v) { 
                super(v); 
                ivThumbnail = v.findViewById(R.id.iv_project_thumbnail);
                ivCheck = v.findViewById(R.id.iv_selected_check);
            }
        }
    }
}
