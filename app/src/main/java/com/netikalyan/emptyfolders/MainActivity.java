package com.netikalyan.emptyfolders;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "EmptyFolders";
    private final String[] STORAGE_PERMISSION = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final String USER_STORAGE = Environment.getExternalStorageDirectory().getAbsolutePath();
    private final ArrayList<String> mFileList = new ArrayList<>();
    private SimpleListAdapter mListAdapter;
    private CheckBox mSelectAllCheckBox;
    private int selectedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSelectAllCheckBox = findViewById(R.id.selectAll);
        mSelectAllCheckBox.setText(String.format(getString(R.string.selected), selectedCount));
        mSelectAllCheckBox.setOnClickListener(v -> {
            mListAdapter.notifyDataSetChanged();
            if (mSelectAllCheckBox.isChecked()) {
                selectedCount = mListAdapter.getItemCount();
            } else {
                selectedCount = 0;
            }
            mSelectAllCheckBox.setText(String.format(getString(R.string.selected), selectedCount));
        });

        mListAdapter = new SimpleListAdapter();
        RecyclerView mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mListAdapter);

        checkStoragePermissions();
    }

    private void checkStoragePermissions() {
        if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermissions(STORAGE_PERMISSION, 1001);
        } else {
            new FolderScanTask().execute();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (1001 == requestCode) {
            for (int i = 0; i < STORAGE_PERMISSION.length; i++) {
                if (Objects.equals(STORAGE_PERMISSION[i], permissions[i]) && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            new FolderScanTask().execute();
        }
    }

    public void deleteSelected(View view) {
        // TODO: start deleting folders and execute FolderScanTask
    }

    class SimpleViewHolder extends RecyclerView.ViewHolder {
        final CheckBox folderNameCheckBox;

        SimpleViewHolder(@NonNull View itemView) {
            super(itemView);
            folderNameCheckBox = itemView.findViewById(R.id.checkBox);
            folderNameCheckBox.setOnClickListener(v -> {
                if (folderNameCheckBox.isChecked())
                    ++selectedCount;
                else
                    --selectedCount;
                mSelectAllCheckBox.setText(String.format(getString(R.string.selected), selectedCount));
                mSelectAllCheckBox.setChecked(selectedCount == mListAdapter.getItemCount());
            });
        }
    }

    class SimpleListAdapter extends RecyclerView.Adapter<SimpleViewHolder> {

        @NonNull
        @Override
        public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recycler_item, viewGroup, false);
            return new SimpleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SimpleViewHolder simpleViewHolder, int position) {
            simpleViewHolder.folderNameCheckBox.setText(mFileList.get(position));
            simpleViewHolder.folderNameCheckBox.setChecked(mSelectAllCheckBox.isChecked());
        }

        @Override
        public int getItemCount() {
            return mFileList.size();
        }
    }

    // TODO: make class static to avoid leaks
    class FolderScanTask extends AsyncTask<Void, Void, ArrayList<String>> {

        @Nullable
        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            Log.d(TAG, "Root = " + USER_STORAGE);
            File parentFolder = new File(USER_STORAGE);
            File[] subFolders = parentFolder.listFiles();
            ArrayList<String> fileList = new ArrayList<>();
            if (subFolders != null && subFolders.length != 0) {
                for (File folder : subFolders)
                    fileList.addAll(isFolderEmpty(folder));
            }
            return fileList;
        }

        @Override
        protected void onPostExecute(ArrayList<String> list) {
            super.onPostExecute(list);
            mFileList.addAll(list);
            mListAdapter.notifyDataSetChanged();
        }

        private ArrayList<String> isFolderEmpty(File folder) {
            ArrayList<String> fileList = new ArrayList<>();
            if (folder.isDirectory()) {
                File[] childFiles = folder.listFiles();
                if (childFiles == null || childFiles.length == 0) {
                    Log.e(TAG, "Empty Folder - " + folder.getAbsolutePath());
                    fileList.add(folder.getAbsolutePath().substring(USER_STORAGE.length()));
                } else {
                    for (File file : childFiles) {
                        if (file.isFile()) {
                            break;
                        } else if (file.isDirectory()) {
                            fileList.addAll(isFolderEmpty(file));
                        }
                    }
                }
            }
            return fileList;
        }
    }
}
