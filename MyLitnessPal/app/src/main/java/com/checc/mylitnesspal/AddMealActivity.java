package com.checc.mylitnesspal;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.UUID;

public class AddMealActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private SurfaceView surfaceView;
    private Button takePicBtn;
    private Camera camera;
    private SurfaceHolder surfaceHolder;
    private Camera.PictureCallback pictureCallback;

    private FirebaseDatabase db;
    private FirebaseStorage storage;

    private ProgressBar pg;

    private long mealNo;
    private int hasInitiatedName = 0;
    private int hasInitiatedMass = 0;

    private int kCal;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_meal);

        kCal = 0;

        db = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        db.getReference().child("Meal Number").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mealNo = (long) dataSnapshot.getValue();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        surfaceView = findViewById(R.id.camera_view);
        takePicBtn = findViewById(R.id.take_pic_btn);
        pg = findViewById(R.id.uploading_pic_prog);

        pg.setVisibility(View.INVISIBLE);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback( this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        takePicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                camera.takePicture(null, null, pictureCallback);
                pg.setVisibility(View.VISIBLE);
                takePicBtn.setVisibility(View.INVISIBLE);
            }
        });

        pictureCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                //rotate and scale bitmap otherwise it would be 90 degrees anticlockwise for some reason
                final Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap rotatedBitmap = Bitmap.createBitmap(bmp , 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, 800, 1280, true);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                //upload jpeg to firebase storage
                final StorageReference ref = storage.getReference().child("images/" + UUID.randomUUID() + ".jpg");
                UploadTask uploadTask = ref.putBytes(byteArray);
                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        // Continue with the task to get the download URL
                        return ref.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            pg.setVisibility(View.INVISIBLE);
                            takePicBtn.setVisibility(View.VISIBLE);

                            final Uri downloadUri = task.getResult();
                            //upload image download url to firebase database
                            db.getReference().child("Ingredient").child("Photo").setValue(downloadUri.toString());

                            //open pop up window and check if server has computed the name and weight of the food
                            //display name and weight in pop up window if server is done computing
                            final AlertDialog.Builder popupBuilder = new AlertDialog.Builder(AddMealActivity.this);
                            final View popupView = getLayoutInflater().inflate(R.layout.confirm_ingredient, null);

                            final ProgressBar popupPgName = popupView.findViewById(R.id.pop_prog_name);
                            final ProgressBar popupPgMass = popupView.findViewById(R.id.pop_prog_mass);

                            popupPgName.setVisibility(View.VISIBLE);
                            popupPgMass.setVisibility(View.VISIBLE);

                            final TextView foodName = popupView.findViewById(R.id.pop_food_name);
                            final TextView foodMass = popupView.findViewById(R.id.pop_food_mass);
                            final Button addIngredientBtn = popupView.findViewById(R.id.add_ingredient_btn);
                            final Button finishMealBtn = popupView.findViewById(R.id.finish_meal_btn);

                            foodName.setVisibility(View.INVISIBLE);
                            foodMass.setVisibility(View.INVISIBLE);
                            addIngredientBtn.setVisibility(View.INVISIBLE);
                            finishMealBtn.setVisibility(View.INVISIBLE);

                            popupBuilder.setView(popupView);
                            final AlertDialog showPopup = popupBuilder.create();
                            showPopup.show();

                            db.getReference().child("Ingredient").child("name").addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    hasInitiatedName++;

                                    if(hasInitiatedName == 2){
                                        popupPgName.setVisibility(View.INVISIBLE);
                                        foodName.setVisibility(View.VISIBLE);
                                        addIngredientBtn.setVisibility(View.VISIBLE);
                                        finishMealBtn.setVisibility(View.VISIBLE);

                                        foodName.setText(dataSnapshot.getValue().toString());
                                        hasInitiatedName = 0;
                                    }

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                            db.getReference().child("Ingredient").child("kCal").addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    hasInitiatedMass++;

                                    if(hasInitiatedMass == 2){
                                        kCal = kCal + Integer.parseInt(dataSnapshot.getValue().toString());

                                        popupPgMass.setVisibility(View.INVISIBLE);
                                        foodMass.setVisibility(View.VISIBLE);

                                        foodMass.setText(dataSnapshot.getValue().toString());
                                        hasInitiatedMass = 0;
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                            addIngredientBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    showPopup.dismiss();
                                }
                            });

                            finishMealBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    db.getReference().child("Meals").child("Meal " + mealNo).child("name").setValue("Meal " + mealNo);
                                    db.getReference().child("Meals").child("Meal " + mealNo).child("kCal").setValue(Integer.toString(kCal));

                                    db.getReference().child("Meal Number").setValue(mealNo + 1);

                                    Intent goBack = new Intent(getApplicationContext(), MainActivity.class);
                                    startActivity(goBack);
                                }
                            });

                        } else {
                            // Handle failures
                            // ...
                            pg.setVisibility(View.INVISIBLE);
                            takePicBtn.setVisibility(View.VISIBLE);
                        }
                    }
                });

                AddMealActivity.this.camera.startPreview();
            }
        };
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            camera = Camera.open();
        }catch (Exception e){

        }

        Camera.Parameters parameters;
        parameters = camera.getParameters();
        parameters.setPreviewFrameRate(20);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();

        // You need to choose the most appropriate previewSize for your app
        Camera.Size previewSize = previewSizes.get(0);
        parameters.setPreviewSize(previewSize.width, previewSize.height);

        camera.setParameters(parameters);
        camera.setDisplayOrientation(90);
        try{
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        camera.stopPreview();
        camera.release();
        camera = null;
    }
}