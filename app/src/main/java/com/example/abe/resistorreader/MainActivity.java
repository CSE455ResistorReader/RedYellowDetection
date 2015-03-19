package com.example.abe.resistorreader;

//ADD PACKAGE HERE

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2HSV;
import static org.opencv.imgproc.Imgproc.MORPH_ELLIPSE;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.dilate;
import static org.opencv.imgproc.Imgproc.erode;
import static org.opencv.imgproc.Imgproc.getStructuringElement;

public class MainActivity extends Activity implements OnClickListener {

    //keep track of camera capture intent
    final int CAMERA_CAPTURE = 1;
    //keep track of cropping intent
    final int PIC_CROP = 2;
    //captured picture uri
    private Uri picUri;
    //declares the bitmap to store the pic
    Bitmap thePic;
    //The image we want to display
    Bitmap displayImage;
    ImageView picView;
    //Load OpenCV libraries.
    static {
        if(!OpenCVLoader.initDebug()) {
            Log.d("ERROR", "Unable to load OpenCV");
        } else {
            Log.d("SUCCESS", "OpenCV loaded");
        }
    }


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button cameraButton = (Button) findViewById(R.id.button_camera);
        Button resultsButton = (Button) findViewById(R.id.results_btn);
        cameraButton.setOnClickListener(this);
        resultsButton.setOnClickListener(this);


    }

    /**
     * Click method to handle user pressing button to launch camera
     */
    public void onClick(View v) {
        if (v.getId() == R.id.button_camera) {
            try {
                //use standard intent to capture an image
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //we will handle the returned data in onActivityResult
                startActivityForResult(captureIntent, CAMERA_CAPTURE);
            }
            catch(ActivityNotFoundException anfe){
                //display an error message
                String errorMessage = "Whoops - your device doesn't support capturing images!";
                Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    /**
     * Handle user returning from both capturing and cropping the image
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            //user is returning from capturing an image using the camera
            if(requestCode == CAMERA_CAPTURE){
                //get the Uri for the captured image
                picUri = data.getData();
                Log.v("CSUSB", data.getDataString());
                //carry out the crop operation
                performCrop();
                Log.v("CSUSB", data.getDataString());
                //colorDetect();
            }
            //user is returning from cropping the image
            else if(requestCode == PIC_CROP){
                //get the returned data
                Bundle extras = data.getExtras();
                //get the cropped bitmap
                thePic = extras.getParcelable("data");
                //change content view
                setContentView(R.layout.activity_results);
                //display the returned cropped image
                picView =  (ImageView)findViewById(R.id.resultsView);
                colorDetect();
                //colorDetectBlue();
                picView.setImageBitmap(displayImage);

            }
        }
    }

    /**
     * Helper method to carry out crop operation
     */
    private void performCrop(){
        //take care of exceptions
        try {
            //call the standard crop action intent (the user device may not support it)
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            //indicate image type and Uri
            cropIntent.setDataAndType(picUri, "image/*");
            //set crop properties
            cropIntent.putExtra("crop", "true");
            //indicate aspect of desired crop
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            //indicate output X and Y
            cropIntent.putExtra("outputX", 256);
            cropIntent.putExtra("outputY", 256);
            //retrieve data on return
            cropIntent.putExtra("return-data", true);
            //start the activity - we handle returning in onActivityResult
            startActivityForResult(cropIntent, PIC_CROP);
        }
        //respond to users whose devices do not support the crop action
        catch(ActivityNotFoundException anfe){
            //display an error message
            String errorMessage = "Whoops - your device doesn't support the crop action!";
            Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private void colorDetect() {
        Mat temp = new Mat (thePic.getWidth(), thePic.getHeight(), CvType.CV_8UC1);
        //Mat PicHSV = new Mat();
        displayImage = Bitmap.createBitmap(temp.cols(), temp.rows(), Bitmap.Config.ARGB_8888);
        //Bitmap bitmapPic = MediaStore.Images.Media.getBitmap(this.getContentResolver(), temp);
        Utils.bitmapToMat(thePic, temp);

        /*
        r=red
        y=yellow
        bk=black
        */
        double rLastX = -1;
        double rLastY = -1;

        double yLastX = -1;
        double yLastY = -1;

       // double bkLastX = -1;
       // double bkLastY = -1;
         Mat imgHSV = new Mat();
        Mat imgHSV2 = new Mat();
        //Mat imgHSV3 = new Mat();

        cvtColor(temp, imgHSV, COLOR_BGR2HSV);//RED
        cvtColor(temp, imgHSV2, COLOR_BGR2HSV);//YELLOW
        //cvtColor(temp, imgHSV3, COLOR_BGR2HSV);
        //Search for colors between these HSV values.
        //RED
        //                              Lows HSV                 Highs HSV
        Core.inRange(imgHSV, new Scalar(40,57,184), new Scalar(179,231,255), imgHSV );
        //YELLOW
        Core.inRange(imgHSV2, new Scalar(87,207,166), new Scalar(123,250 ,222), imgHSV2 );
        //BLACK
       // Core.inRange(imgHSV3, new Scalar (48,48,48), new Scalar(0,0,0), imgHSV3);

        erode(imgHSV, imgHSV, getStructuringElement(MORPH_ELLIPSE, new Size(5, 5)));
        dilate(imgHSV, imgHSV, getStructuringElement(MORPH_ELLIPSE, new Size(5,5)));

        erode(imgHSV2,imgHSV2, getStructuringElement(MORPH_ELLIPSE, new Size(5,5)));
        dilate(imgHSV2, imgHSV2, getStructuringElement(MORPH_ELLIPSE, new Size(5,5)));

      //  erode(imgHSV3,imgHSV3, getStructuringElement(MORPH_ELLIPSE, new Size(5,5)));
        //dilate(imgHSV3, imgHSV3, getStructuringElement(MORPH_ELLIPSE, new Size(5,5)));


        dilate(imgHSV, imgHSV, getStructuringElement(MORPH_ELLIPSE, new Size(5,5)));
        erode(imgHSV,imgHSV, getStructuringElement(MORPH_ELLIPSE, new Size(5,5)));

        dilate(imgHSV2, imgHSV2, getStructuringElement(MORPH_ELLIPSE, new Size(5,5)));
        erode(imgHSV2,imgHSV2, getStructuringElement(MORPH_ELLIPSE, new Size(5,5)));

        //dilate(imgHSV3, imgHSV3, getStructuringElement(MORPH_ELLIPSE, new Size(5,5)));
        //erode(imgHSV3,imgHSV3, getStructuringElement(MORPH_ELLIPSE, new Size(5,5)));


        Moments rMoments = Imgproc.moments(imgHSV);
        double rdM01 = rMoments.get_m01();
        double rdM10 = rMoments.get_m10();
        double rdArea = rMoments.get_m00();

        Moments yMoments = Imgproc.moments(imgHSV2);
        double ydM01 = yMoments.get_m01();
        double ydM10 = yMoments.get_m10();
        double ydArea = yMoments.get_m00();

     /*   Moments bkMoments= Imgproc.moments(imgHSV3);
        double bkdM01 = bkMoments.get_m01();
        double bkdM10 = bkMoments.get_m10();
        double bkdArea = bkMoments.get_m00();
*/

        double rposX = rdM10 / rdArea;
        double rposY = rdM01 / rdArea;

        double yposX = ydM10/ydArea;
        double yposY = ydM01/ydArea;

     //   double bkposX = bkdM10/bkdArea;
       // double bkposY = bkdM01/bkdArea;

        if(rLastX >= 0 && rLastY >= 0 && rposX >= 0 && rposY >= 0)
        {
            //displays the dot in the corner if red is not detected
            Core.line(temp,new Point(rposX, rposY), new Point(rLastX, rLastY), new Scalar(255,255,100), 10);
        }

        if(yLastX >= 0 && yLastY >= 0 && yposX >= 0 && yposY >= 0)
        {
            //displays the dot in corner if yellow not detected
            Core.line(temp,new Point(yposX, yposY), new Point(yLastX, yLastY), new Scalar(255,255,100), 15);
        }

        /*if(bkLastX >= 0 && bkLastY >= 0 && bkposX >= 0 && bkposY >= 0)
        {
            //displys the dot on the picture
            Core.line(temp,new Point(bkposX, bkposY), new Point(bkLastX, bkLastY), new Scalar(255,255,100), 10);
        }*/
        rLastX = rposX;
        rLastY = rposY;

        yLastX = yposX;
        yLastY = yposY;

        //bkLastX = bkposX;
        //bkLastY = bkposY;
        //}

        //     Core.line(temp,new Point(100, 100), new Point(200, 200), new Scalar(255,255,100), 20);
        //displays the dot on the picture if red is detected
        Core.line(temp,new Point(rposX, rposY), new Point(rLastX, rLastY), new Scalar(255,255,100), 10);
       //displays dot on picture if yellow is detected
        Core.line(temp,new Point(yposX, yposY), new Point(yLastX, yLastY), new Scalar(255,255,100), 10);
       // Core.line(temp,new Point(bkposX, bkposY), new Point(bkLastX, bkLastY), new Scalar(255,255,100), 10);



        Utils.matToBitmap(temp, displayImage);
        picView.setImageBitmap(displayImage);

    }
   /* private void colorDetectBlue() {
        Mat temp = new Mat (thePic.getWidth(), thePic.getHeight(), CvType.CV_8UC1);
        //Mat PicHSV = new Mat();
        displayImage = Bitmap.createBitmap(temp.cols(), temp.rows(), Bitmap.Config.ARGB_8888);
        //Bitmap bitmapPic = MediaStore.Images.Media.getBitmap(this.getContentResolver(), temp);
        Utils.bitmapToMat(thePic, temp);


        //cvtColor(temp, PicHSV, COLOR_RGB2GRAY);
        double iLastX = -1;
        double iLastY = -1;


        Mat imgHSV = new Mat();
        Mat imgHSV2 = new Mat();


        cvtColor(temp, imgHSV, COLOR_BGR2HSV);
        cvtColor(temp,imgHSV2,COLOR_BGR2HSV);
        //Search for Blue
        Core.inRange(imgHSV, new Scalar(0,232,41), new Scalar(40,255,94), imgHSV );

        erode(imgHSV,imgHSV, getStructuringElement(MORPH_ELLIPSE, new Size(5,5)));
        dilate(imgHSV, imgHSV, getStructuringElement(MORPH_ELLIPSE, new Size(5,5)));

        dilate(imgHSV, imgHSV, getStructuringElement(MORPH_ELLIPSE, new Size(5,5)));
        erode(imgHSV,imgHSV, getStructuringElement(MORPH_ELLIPSE, new Size(5,5)));


        Moments oMoments = Imgproc.moments(imgHSV);
        double dM01 = oMoments.get_m01();
        double dM10 = oMoments.get_m10();
        double dArea = oMoments.get_m00();

        //  if(dArea > 10000)
        // {
        double posX = dM10 / dArea;
        double posY = dM01 / dArea;

        if(iLastX >= 0 && iLastY >= 0 && posX >= 0 && posY >= 0)
        {
            //displys the dot on the picture
            Core.line(temp,new Point(posX, posY), new Point(iLastX, iLastY), new Scalar(255,255,100), 10);
        }
        iLastX = posX;
        iLastY = posY;
        //}

        //     Core.line(temp,new Point(100, 100), new Point(200, 200), new Scalar(255,255,100), 20);
        //displays the dot on the picture
        Core.line(temp,new Point(posX, posY), new Point(iLastX, iLastY), new Scalar(255,255,100), 10);



        Utils.matToBitmap(temp, displayImage);
        picView.setImageBitmap(displayImage);

    }*/

}//Do color detection of each color, figure out a way to do it in a loop, go through the
//x values of the displayed dots to determine the order in which the colors appear
