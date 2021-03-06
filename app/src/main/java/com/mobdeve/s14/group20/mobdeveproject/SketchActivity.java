package com.mobdeve.s14.group20.mobdeveproject;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class SketchActivity extends AppCompatActivity {

    private Context context;

    private RecyclerView rvTags;
    private RecyclerView.LayoutManager tagsManager;
    private TagsAdapter tagsAdapter;

    private Button clearButton;
    private Button saveButton;
    private CanvasView canvas;
    private ProgressBar pbProgress;

    private String title, subtitle, noteType, newNoteId, userId;
    private ArrayList<String> tags;

    private EditText etTitle, etSubtitle;
    private TextView tvNoteId, tvCanvasUrl;

    private FirebaseDatabase database;
    private DatabaseReference reference;
    private FirebaseUser user;

    private Uri fileUri, selectedImageUri;

    HashMap<String, Object> noteData = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sketch);

        context = getApplicationContext();
        clearButton = findViewById(R.id.sketch_clear_screen);
        saveButton = findViewById(R.id.sketch_save_button);
        canvas = findViewById(R.id.sketch_canvas_view);

        this.initFirebase();
        this.loadData();
        this.initEssentials();

        clearButton.setOnClickListener(v -> {
            canvas.clearScreen();
            }
        );
        saveButton.setOnClickListener(v -> {
            this.saveCanvas();
            this.saveNote();
        });
    }

    private void initFirebase() {
        this.database = FirebaseDatabase.getInstance();
        this.reference = this.database.getReference().child(Collection.users.name());
    }

    private void loadData(){

        this.title = getIntent().getStringExtra(Keys.TITLE.name());
        this.subtitle = getIntent().getStringExtra(Keys.SUBTITLE.name());
        this.tags = getIntent().getStringArrayListExtra(Keys.TAGS.name());
        this.noteType = getIntent().getStringExtra(Keys.NOTETYPE.name());
    }

    private static final String default_url = "https://firebasestorage.googleapis.com/v0/b/tous-les-journal.appspot.com/o/sketchbook_background.jpg?alt=media&token=5b754840-8160-4537-8f66-7c99ea906753";

    private void initEssentials(){

        this.etTitle = findViewById(R.id.etml_sketch_title);
        this.etSubtitle = findViewById(R.id.etml_sketch_subtitle);
        this.pbProgress = findViewById(R.id.pb_sketch_progress);
        this.tvNoteId = findViewById(R.id.sketch_tv_noteid);
        this.tvCanvasUrl = findViewById(R.id.sketch_tv_url);

        Log.d("SKETCH NOTE ID", tvNoteId.getText().toString());
        this.etTitle.setText(this.title);
        this.etSubtitle.setText(this.subtitle);
        this.tvCanvasUrl.setText(default_url);

        this.rvTags = findViewById(R.id.rv_sketch_tags);
        this.tagsManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        this.rvTags.setLayoutManager(tagsManager);

        this.tagsAdapter = new TagsAdapter(this.tags);
        this.rvTags.setAdapter(this.tagsAdapter);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Unsaved changes will be lost.\nAre you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SketchActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        this.pbProgress.setVisibility(View.VISIBLE);
//        this.saveNote();
//        this.pbProgress.setVisibility(View.GONE);
//    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void saveCanvas() {
        this.user = FirebaseAuth.getInstance().getCurrentUser();
        try {
            this.fileUri = Uri.fromFile(canvas.saveScreen(etTitle.getText()));
            this.selectedImageUri = this.fileUri;
            uploadImage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveNote() {
        userId = user.getUid();

        this.reference.child((userId)).child(Collection.notes.name())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        newNoteId = reference.push().getKey();
                        System.out.println("Current note id: " + newNoteId);

                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date date = new Date();
                        String dateString = formatter.format(date);

                        title = String.valueOf(etTitle.getText());
                        subtitle = String.valueOf(etSubtitle.getText());

                        if(title.equals(""))
                            title = "Title";

                        if(subtitle.equals(""))
                            subtitle = "Subtitle";

                        String sketchURL = default_url;

                        noteData.put("title", title);
                        noteData.put("subtitle", subtitle);
                        noteData.put("noteType", noteType);
                        noteData.put("dateModified", dateString);
                        noteData.put("tags", tags);
                        noteData.put("sketchLink", sketchURL);
                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {

                    }
                });
    }

    private void uploadImage() {
        final ProgressDialog pdUpload = new ProgressDialog(this);
        pdUpload.setMessage("Uploading photo");
        pdUpload.show();

        StorageReference fileReference;

        if(selectedImageUri != null) {
            if(getFileExtension(selectedImageUri) == null)
                fileReference = FirebaseStorage.getInstance().getReference()
                        .child("uploads").child(user.getUid().toString())
                        .child(System.currentTimeMillis() + ".png");
            else
                fileReference = FirebaseStorage.getInstance().getReference()
                        .child("uploads").child(user.getUid().toString())
                        .child(System.currentTimeMillis() + "." + getFileExtension(selectedImageUri));

            fileReference.putFile(selectedImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull @NotNull Task<UploadTask.TaskSnapshot> task) {
                    fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String imgUrl = uri.toString();

                            Log.d("Download url: ", imgUrl);

                            noteData.put("sketchLink", imgUrl);

                            reference.child((userId)).child(Collection.notes.name()).child(newNoteId).setValue(noteData);
                            pdUpload.dismiss();

                            Toast.makeText(SketchActivity.this, "Image upload successful", Toast.LENGTH_SHORT).show();

                        }
                    });
                }
            });
            Log.d("Download path: ", fileReference.getPath().toString());
        }
    }

    private String getFileExtension(Uri imgUri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();

        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(imgUri));
    }
}