package com.tesco.isearch;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    String mCurrentPhotoPath;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    private static int RESULT_LOAD_IMAGE = 1;

    private boolean isCaptureImage;
    private boolean isUploadImage;

    private TextView mTextMessage;

    private Button uploadButton;
    private Button captureButton;

    /*private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.description);
                    return true;
                case R.id.capture_image:
                    isCaptureImage = true;
                    isUploadImage = false;
                    dispatchTakePictureIntent();
                    return true;
                case R.id.upload_image:
                    isCaptureImage = false;
                    isUploadImage = true;
                    Intent i = new Intent(
                            Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i, RESULT_LOAD_IMAGE);
                    return true;
            }
            return false;
        }

    };*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextMessage = (TextView) findViewById(R.id.message);
        uploadButton = (Button) findViewById(R.id.upload_image);
        captureButton = (Button) findViewById(R.id.capture_image);
        int hasExternalStorageAccess = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        if (hasExternalStorageAccess != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_ASK_PERMISSIONS);
        }
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isUploadImage = true;
                isCaptureImage = false;
                dispatchUploadPicture();
            }
        });

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isCaptureImage = true;
                isUploadImage = false;
                dispatchTakePictureIntent();
            }
        });
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void dispatchUploadPicture() {
        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mTextMessage.setText("");
        RemoteCall remoteCall = new RemoteCall();
        List<Product> products = new ArrayList<>();
        if (isCaptureImage && requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            File file = null;
            try {
                //products = (List<Product>) remoteCall.execute().get();
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                file = new File(Environment.getExternalStorageDirectory() + File.separator + "temporary_file.jpg");
                FileOutputStream fo = new FileOutputStream(file);
                fo.write(bytes.toByteArray());
                fo.flush();
                fo.close();
                remoteCall.setSourceFile(file);
                products = (List<Product>) remoteCall.execute().get();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (file != null) {
                    file.delete();
                }
            }

        }

        if (isUploadImage && requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            try {
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(data.getData(),
                        filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();

                File sourceFile = new File(picturePath);
                remoteCall.setSourceFile(sourceFile);
                products = (List<Product>) remoteCall.execute().get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        if (products.size() > 0) {
            ListView listView = (ListView) findViewById(R.id.list_item);
            listView.setAdapter(new CustomAdapter(this, products));
        } else {
            mTextMessage.setText("No Records found.");
        }
    }


    class RemoteCall extends AsyncTask {

        private File sourceFile;

        public void setSourceFile(File sourceFile) {
            this.sourceFile = sourceFile;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            List<Product> products = new ArrayList<>();

            if (sourceFile.isFile()) {
                DataOutputStream dos = null;
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1 * 1024 * 1024;
                try {
                    FileInputStream fileInputStream = new FileInputStream(sourceFile);
                    URL url = new URL("http://9cd0d725.ngrok.io/search");
                    HttpURLConnection conn = null;
                    // Open a HTTP connection to the URL
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true); // Allow Inputs
                    conn.setDoOutput(true); // Allow Outputs
                    conn.setUseCaches(false); // Don't use a Cached Copy
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("ENCTYPE",
                            "multipart/form-data");
                    conn.setRequestProperty("Content-Type",
                            "multipart/form-data;boundary=" + boundary);
                    conn.setRequestProperty("image", this.sourceFile.getAbsolutePath());

                    dos = new DataOutputStream(conn.getOutputStream());

                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"image\";filename=\""
                            + this.sourceFile.getAbsolutePath() + "\"" + lineEnd);

                    dos.writeBytes(lineEnd);

                    // create a buffer of maximum size
                    bytesAvailable = fileInputStream.available();

                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];

                    // read file and write it into form...
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    while (bytesRead > 0) {

                        dos.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math
                                .min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0,
                                bufferSize);

                    }

                    // send multipart form data necesssary after file
                    // data...
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + twoHyphens
                            + lineEnd);

                    // Responses from the server (code and message)
                    int serverResponseCode = conn.getResponseCode();
                    if (serverResponseCode == 202) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuffer sb = new StringBuffer();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        System.out.println(sb.toString());
                        JSONArray jsonArray = new JSONArray(sb.toString());
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = new JSONObject(jsonArray.get(i).toString());
                            Product product = new Product();
                            product.setName(jsonObject.get("name").toString());
                            product.setPrice(jsonObject.get("price").toString());
                            product.setImageUrl(jsonObject.get("imageUrl").toString());
                            product.setLabels(jsonObject.get("labels").toString());
                            products.add(product);
                        }
                    }
                    // close the streams //
                    fileInputStream.close();
                    dos.flush();
                    dos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return products;
        }
    }

}
