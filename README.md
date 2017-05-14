# Clone SDK & Sample Code

## What is Clone?

[Clone Robot](https://www.autonomous.ai/clone-telepresence-smart-office-robot) is an Android-based robot designed by [Autonomous](https://www.autonomous.ai).  It is a 4-foot tall robot, capable of moving around, running Android apps, and being controlled by a smartphone.

Email Duy at duy@autonomous.ai if you have any questions :)


## Develop Apps for Clone

Clone is based on Android, so building a Clone app is the same with building an Android app.  

The Clone SDK lets developers:

* instruct Clone to move at a given speed, direction, and amount of time
* read Clone's internal sensors including velocity, battery level, bumper, and IMU
* see and hear what Clone sees & hears


![ScreenShot](https://github.com/duyhtq/HelloClone/blob/master/diagram.jpg)


## Build your first Clone app

Import **HelloClone** project into Android Studio.

```
/HelloClone/RobotApp/
/HelloClone/PhoneApp/
```

Update Config.java under /autonomous/sdk/ with the your ROBOT ID.

```
public static String ROBOT_ID ="YOUR_ROBOT_ID";
```

Install the **RobotApp** on the Android tablet on Clone.  

Install **PhoneApp** on your Android phone.

That's it.  You now can use your phone to remotely control Clone to move around, capture photos, or record videos.


## Clone SDK

Create a new Clone object with your ROBOT ID

```
clone = new Clone(Config.ROBOT_ID);
```

Drive forward

```
clone.moveForward();
```

Drive backward

```
clone.moveBackward();
```

Turn right

```
clone.turnRight();
```

Turn left

```
clone.turnLeft();
```

Stop

```
clone.stop();
```

Auto-charge

```
clone.autocharge();
```

Start recording a video

```
clone.startRecording();
```

Stop recording a video

```
clone.stopRecording();
```


## App Ideas & Examples

**Example 1:** Build your own Telepresence Robot!

```
// MainActivity.java

// Catch the button press action

private final CompoundButton.OnCheckedChangeListener onChangeDirection = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                String new_command = "";
                switch (buttonView.getId()) {
                    case R.id.btn_top:
                        clone.moveForward();
                        break;
                    case R.id.btn_bottom:
                        clone.moveBackward();
                        break;
                    case R.id.btn_left:
                        clone.turnLeft();
                        break;
                    case R.id.btn_right: 
                        clone.turnRight();
                        break;
                    case R.id.btn_stop_action:
                        if(currentActionView != null){
                            currentActionView.setChecked(false);
                        }
                        clone.stop();
                        break;
                }
                if(currentActionView !=null){
                    currentActionView.setChecked(false);
                }
                currentActionView = buttonView;
            }
        }
    };
    
```
**Example 2:**  Take a screenshot from thousands of miles away :)

```
// MainActivity.java

// Catch the button press action

final ImageView button = (ImageView) findViewById(R.id.btnCapture);

button.setOnClickListener(new View.OnClickListener() {
    public void onClick(View v) {
        if (mSubscriber == null) {
            return;
        }
        ((BasicCustomVideoRenderer) mSubscriber.getRenderer()).saveScreenshot(true);
        Toast.makeText(MainActivity.this, "Screenshot saved.", Toast.LENGTH_LONG).show();
    }
});
```

```
// BasicCustomVideoRender.java

// Return the image

decodeYUV420(intArray, yuv, width, height);

Bitmap bmp = Bitmap.createBitmap(intArray, width, height, Bitmap.Config.ARGB_8888);



// TODO: IMPLEMENT YOUR OWN ALGORITHM HERE TO ANALYZE THE CURRENT SCREENSHOT
// AND TELL THE ROBOT TO PERFORM ANY ACTION



```

**Example 3:** Write your own algorithm to analyze what Clone sees, frame by frame, and instruct Clone to move accordingly.  For example, autonomous navigation app, office security app, etc.

```
// BasicCustomVideoRender.java

public void displayFrame(Frame frame) {
    mFrameLock.lock();
    if (this.mCurrentFrame != null) {
        this.mCurrentFrame.recycle();
    }
    this.mCurrentFrame = frame;
    mFrameLock.unlock();
            

            
    // TODO: IMPLEMENT YOUR OWN ALGORITHM HERE TO ANALYZE FRAME BY FRAME
    // AND TELL THE ROBOT TO PERFORM ANY ACTION 


```


**Example 4:**  Send a file from your phone to Clone to present to the audience

**Example 5:**  Office security to notify your phone when a stranger enters the building or your home

**Example 6:**  Clone autonomously moves around a warehouse to count inventory


## Hardware specifications

* Battery time: 3 hours
* Charging time: 2 hours
* Maximum velocity: 27 inches per second
* Maximum rotational velocity: 180 degrees
* Materials: CNC Aluminum, PP Plastic
* Dimension: 14 in x 14 in x 43 in
* Weight: 16 lbs

![ScreenShot](https://github.com/duyhtq/HelloClone/blob/master/hardware.jpg)



# ultimatechcrunch2017
