package com.example.camera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST_CODE = 102;
    ImageView displayImageView;
    Button cameraBtn,galleryBtn;
    private static final int CAMERA_REQUEST =101;
    String currentPhotoPath;
    private static final int GALLERY_REQUEST_CODE=103;
    StorageReference storageReference;
    private Context context;
     private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        displayImageView=(ImageView)findViewById(R.id.imageView);
        cameraBtn =(Button)findViewById(R.id.cameraBtn);
        galleryBtn=(Button)findViewById(R.id.galleryBtn);
        context=this;
        progressDialog=new ProgressDialog(context);
        storageReference= FirebaseStorage.getInstance().getReference(); //To initialise the ref


        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askCameraPermissions(); //To check if user given the permission or not

            }
        });
        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI); //Need to pick action from mediastore
                startActivityForResult(intent,GALLERY_REQUEST_CODE);
            }
        });

    }

    private void askCameraPermissions() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) //To check if permission is given or not
        {
            //inside if it means if permission not given then
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},CAMERA_REQUEST); //To request abt camera permission
        }
        else
        {
            dispatchTakePictureIntent(); //For opening camera and saving the photos by creating image file and then the Uri of it
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==CAMERA_REQUEST)
        {
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) //If both this condition fulfill
            //this means user given camera permission
            //grade.length>0 means there is something in grantResults array,2nd one to check permission
            { //openCamera
                dispatchTakePictureIntent();
            }
            else
            {
                Toast.makeText(this,"Camera permissions is required",Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) { //By this we decide what to do
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE) { //To show clicked images we use thumbnail which comes with this data
            //for that
            /*Bitmap image = (Bitmap) data.getExtras().get("data");
            displayImageView.setImageBitmap(image); //This wil capture the image and show image to our image view */

            if (resultCode == Activity.RESULT_OK)
            {
                File f =new File(currentPhotoPath);  //Creating new file from currentPhotoPath
                displayImageView.setImageURI(Uri.fromFile(f));  //Set ImageView by image by Using Uri-setImageUri
                //Uri.fromFile(f) getting the Uri from file f which is creating by using currentPhotoPath(Absolute Path)
                //Its better to use Uri bcoz it helps to save the picture without degrading its resolution
                Toast.makeText(this,"Saved to " + currentPhotoPath,Toast.LENGTH_SHORT).show();

                Intent mediaScanIntent=new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);


                uploadToFirebase(f.getName(),contentUri); /*Need two parameters first the file name to store in firebase and second the
                Uri show we exactly know the location of file that we have to upload*/

            }
        }
        if(requestCode==GALLERY_REQUEST_CODE)
        {
            if(resultCode==Activity.RESULT_OK)
            {
                Uri contentUri = data.getData(); //Creating content Uri from the data passed by intent data here
                //Next we need to create the file name just like in createImageFile
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()); //Using this to create a file name also can create by another name
                //Here we creating a SimpleDateFormat to get  TimeStamp
                String imageFileName = "JPEG_" + timeStamp + "_" +getFileExt(contentUri); //Creating image file here, We use getFileExt to get the extension of image(jpg,png etc.)
                displayImageView.setImageURI(contentUri);

                uploadToFirebase(imageFileName,contentUri);
            }

        }
    }

    private void uploadToFirebase(String imageFileName, Uri contentUri)
    {
        progressDialog.setMessage("Uploading");
        progressDialog.show();
        final StorageReference image=storageReference.child("images/" + imageFileName);  //Need to give a path to store
        //Here we create a folder name with images in which our images will store
        //Now to upload the file to firebase
        //Requires the uri of the image that will be uploaded
        image.putFile(contentUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) { //If upload succesful we need to get the Uri
                //To show it in the image view
                image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) { //Here uri is he download Url that we get from server after uploading
                        Log.d("tag","On Success: Image Url is " + uri);
                        //Picasso.get().load(uri).into(displayImageView); // We r getting the image uri from firebase and loading into imageview by Picasso
                    }

                });
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this,"Image Uploaded Successfully",Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this,"Upload failed",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getFileExt(Uri contentUri) { //It will get type of image that user have selected
        Context context;
        ContentResolver c = getContentResolver(); //By this we can get the extension
        MimeTypeMap mime=MimeTypeMap.getSingleton(); //By using getSingleton method of Mimetypemap we can list out all the supported type of images
        return mime.getExtensionFromMimeType(c.getType(contentUri)); //Using this map we can get the extension of the mime type from the Url that we selected from gallery
        //Content Uri is the absolute Uri from this we are getting the image ext.
    }


    private File createImageFile() throws IOException { //The file_path.xml will specify where our image gonna save
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()); //Using this to create a file name also can create by another name
        //Here we creating a SimpleDateFormat to get  TimeStamp
        String imageFileName = "JPEG_" + timeStamp + "_"; //Creating image file here

        File storageDir =getExternalFilesDir(Environment.DIRECTORY_PICTURES); //Storage directory to save our file
        //File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile( //Here we creating image file
                imageFileName,  /* prefix,image name */
                ".jpg",         /* suffix ,Extension*/
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath(); //From this we are a absolute path where image is saved and use it to upload to firebase or imageview
        return image; //Finally returning the image
    }

    private void dispatchTakePictureIntent() {  //Open Camera and save image file to directory by first creating our images  into file

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); //For camera opening
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) { //Check if camera present or not
            // Create the File where the photo should go
            File photoFile = null; //As createImageFile can throw the IO exception we r using try and catch
            try {
                photoFile = createImageFile(); //Now we call the above files in this to create the image file
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,  //Now it converts the photofile(file of the image) into Uri
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI); //Putting uri into MediaStore.Extra_Output which will be the input in intent
                //By that we can get the data and start activity by camera request code
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }



}

