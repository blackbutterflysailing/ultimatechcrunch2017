package autonomous.clone.mobile_remote;

import android.Manifest;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pubnub.api.*;
import com.pubnub.api.models.consumer.*;
import com.pubnub.api.callbacks.*;
import com.pubnub.api.enums.*;
import com.pubnub.api.models.consumer.pubsub.*;
import android.webkit.WebView;

import java.io.IOException;
import java.util.Arrays;
import android.media.MediaPlayer;
import android.media.AudioManager;
import com.google.gson.*;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import autonomous.sdk.Clone;
import autonomous.sdk.CustomVideoCapturer;
import autonomous.sdk.VideoSession;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import autonomous.sdk.Config;

public class MainActivity extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks,
        Session.SessionListener,
        Publisher.PublisherListener,
        Subscriber.VideoListener, MediaPlayer.OnPreparedListener {

    private static final String TAG = "mobile-remote " + MainActivity.class.getSimpleName();

    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;

    private PubNub pubnub;
    private MediaPlayer mediaPlayer;

    private WebView webView;

    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;

    private RelativeLayout mPublisherViewContainer;
    private RelativeLayout mSubscriberViewContainer;

    protected LinearLayout viewLltListActions;

    // Spinning wheel for loading subscriber view
    private ProgressBar mLoadingSub;

    private DatabaseReference mDatabase;

    //Control Move
    private RadioGroup viewControlParent;
    protected RadioButton viewDirectionLeft, viewDirectionRight, viewDirectionBottom, viewDirectionTop, viewDirectionStop;
    private CompoundButton currentActionView;

    private CustomVideoCapturer customVideoCapturer;

    private Clone clone;
    private VideoSession videoSession;

    private final CompoundButton.OnCheckedChangeListener onChangeDirection = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                Log.d(TAG, " onChangeDirection OK");
                String new_command = "";
                switch (buttonView.getId()) {
                    case R.id.btn_top:
                        new_command = Config.FORWARD;
                        break;
                    case R.id.btn_bottom:
                        new_command = Config.BACKWARD;
                        break;
                    case R.id.btn_left:
                        new_command = Config.LEFT;
                        break;
                    case R.id.btn_right:
                        Log.d(TAG, " onCheckedChanged camera");
                        new_command = Config.RIGHT;
                        break;
                    case R.id.btn_stop_action:
                        new_command = Config.STOP;

                        break;
                }
                if(currentActionView !=null){
                    currentActionView.setChecked(false);
                }
                currentActionView = buttonView;
                if (!TextUtils.isEmpty(new_command)) {

                    if (new_command.compareTo(Config.STOP) != 0) {
                        clone.move(new_command);
                    } else {
                        if(currentActionView != null){
                            currentActionView.setChecked(false);
                        }
                        clone.stop();
                    }
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPublisher = new Publisher(getApplicationContext(), "publisher");
        mPublisher.setPublisherListener(this);
        // use an external customer video capturer
        customVideoCapturer = new CustomVideoCapturer(getApplicationContext());
        mPublisher.setCapturer(customVideoCapturer);
        
        mPublisherViewContainer = (RelativeLayout) findViewById(R.id.publisherview);
        mSubscriberViewContainer = (RelativeLayout) findViewById(R.id.subscriberview);

        viewLltListActions = (LinearLayout) findViewById(R.id.layout_video_capture_llt_action);

        viewControlParent = (RadioGroup)  findViewById(R.id.control_parent);

        mLoadingSub = (ProgressBar) findViewById(R.id.loadingSpinner);


        webView = (WebView) findViewById(R.id.webview);
        webView.loadUrl("https://shrouded-plateau-31420.herokuapp.com");
        viewDirectionTop = (RadioButton)  findViewById(R.id.btn_top);
        viewDirectionLeft = (RadioButton)  findViewById(R.id.btn_left);
        viewDirectionBottom = (RadioButton) findViewById(R.id.btn_bottom);
        viewDirectionRight = (RadioButton)  findViewById(R.id.btn_right);
        viewDirectionStop = (RadioButton) findViewById(R.id.btn_stop_action);

        viewDirectionTop.setOnCheckedChangeListener(onChangeDirection);
        viewDirectionBottom.setOnCheckedChangeListener(onChangeDirection);
        viewDirectionLeft.setOnCheckedChangeListener(onChangeDirection);
        viewDirectionRight.setOnCheckedChangeListener(onChangeDirection);
        viewDirectionStop.setOnCheckedChangeListener(onChangeDirection);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        requestPermissions();

        clone = new Clone(Config.ROBOT_ID);
        videoSession = new VideoSession();


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

        pubnub = initPubNub();
        pubnub.addListener(new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {
                System.out.println("$$$$$$$$$$$$$$$$$$  pubnub status.getCategory()="+status.getCategory());

                if (status.getCategory() == PNStatusCategory.PNUnexpectedDisconnectCategory) {
                    // This event happens when radio / connectivity is lost
                }

                else if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {

                    // Connect event. You can do stuff like publish, and know you'll get it.
                    // Or just use the connected event to confirm you are subscribed for
                    // UI / internal notifications, etc

                    if (status.getCategory() == PNStatusCategory.PNConnectedCategory){
/*

                            pubnub.publish().channel("ultima3tts").message("hello I am working.").async(new PNCallback<PNPublishResult>() {
                                //pubnub.publish().channel("awesomeChannel").message("hello/").async(new PNCallback<PNPublishResult>() {
                                @Override
                                public void onResponse(PNPublishResult result, PNStatus status) {
                                    // Check whether request successfully completed or not.
                                    if (!status.isError()) {

                                        // Message successfully published to specified channel.
                                        System.out.println("Message successfully published to specified channel.");
                                    }
                                    // Request processing failed.
                                    else {
                                        System.out.println("Message error published to specified channel.");
                                        // Handle message publish error. Check 'category' property to find out possible issue
                                        // because of which request did fail.
                                        //
                                        // Request can be resent using: [status retry];
                                    }
                                }
                            });
*/
                    }
                }
                else if (status.getCategory() == PNStatusCategory.PNReconnectedCategory) {

                    // Happens as part of our regular operation. This event happens when
                    // radio / connectivity is lost, then regained.
                }
                else if (status.getCategory() == PNStatusCategory.PNDecryptionErrorCategory) {

                    // Handle messsage decryption error. Probably client configured to
                    // encrypt messages and on live data feed it received plain text.
                }
            }

            @Override
            public void message(PubNub pubnub, PNMessageResult message) {
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println(message.getMessage());
                JsonElement jsonElement = message.getMessage();
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                JsonElement speechElement = jsonObject.get("speech");
                System.out.println("speechElement="+speechElement.getAsString());
                streamAudio(speechElement.getAsString());
                // Handle new message stored in message.message
                if (message.getChannel() != null) {
                    // Message has been received on channel group stored in
                    // message.getChannel()
                }
                else {
                    // Message has been received on channel stored in
                    // message.getSubscription()
                }

            /*
                log the following items with your favorite logger
                    - message.getMessage()
                    - message.getSubscription()
                    - message.getTimetoken()
            */
            }

            @Override
            public void presence(PubNub pubnub, PNPresenceEventResult presence) {

            }
        });

        //pubnub.subscribe().channels(Arrays.asList("ClarifAI1Channel")).execute();
        pubnub.subscribe().channels(Arrays.asList("ttschannelultima3")).execute();


        clone = new Clone(Config.ROBOT_ID);
        //videoSession = new VideoSession();
        //=================================//


    }

    private void streamAudio(String inputURL) {
        //String url = "http://........"; // your URL here
        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            }
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            //mediaPlayer.reset();
            mediaPlayer.setDataSource(inputURL);
            mediaPlayer.prepareAsync();
            mediaPlayer.start();
        } catch (IOException e) {
            System.out.println("Error on streamAudio e="+ e.getLocalizedMessage());
        }
        mediaPlayer.setOnPreparedListener(this);
    }

    // You need to listen for when the Media Player is finished preparing and is ready
    public void onPrepared(MediaPlayer player) {
        // Start the player
        player.start();
    }

    public void play() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.prepareAsync();
        }
    }

    private void startSessionVideo(){

        mSession = new Session(MainActivity.this, Config.API_KEY, clone.session.sessionId);
        mSession.setSessionListener(this);
        mSession.connect(clone.session.token);

    }

    private PubNub initPubNub() {
        /* View the Full Documentation. */
        /* Instantiate PubNub */
        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setPublishKey("pub-c-bccad06e-7478-49f5-82ae-a9bc2fae5c37");
        pnConfiguration.setSubscribeKey("sub-c-22a33f1e-3884-11e7-a268-0619f8945a4f");
        PubNub pubnub = new PubNub(pnConfiguration);
        return pubnub;
    }


    private void initSession(){


        mDatabase.child("sessions/"+Config.ROBOT_ID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                videoSession = dataSnapshot.getValue(VideoSession.class);

                if(videoSession==null){

                    requestNewSession();
                }else{
                    Log.d("DATA", videoSession.sessionId);
                    Log.d("DATA", videoSession.token);
                    clone.session = videoSession;
                    startSessionVideo();

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void requestNewSession(){

        AppController app = AppController.getInstance();

        final String PRODUCT_UPDATE_URL = Config.HOST + "/session/create";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, PRODUCT_UPDATE_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject obj = new JSONObject(response);

                            Log.d("DATA", response);
                            videoSession= new VideoSession();
                            videoSession.sessionId = obj.getString("session_id");
                            videoSession.token = obj.getString("token");

                            clone.setSession(videoSession);
                            startSessionVideo();

                        } catch (JSONException e) {

                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("ERROR", error.toString());
                    }
                });
        app.addToRequestQueue(stringRequest);

    }

//
//    private void firebaseAuthLogin() {
//
//        try {
//
//            final FirebaseAuth auth = FirebaseAuth.getInstance();
//            //Login
//            auth.signInWithEmailAndPassword(Config.FBASE_EMAIL, Config.FBASE_PASSWORD)
//                    .addOnCompleteListener(new OnCompleteListener() {
//                        @Override
//                        public void onComplete(Task task) {
//                            mDatabase = FirebaseDatabase.getInstance().getReference();
//
//                        }
//                    });
//
//        } catch (Exception e) {
//
//        }
//
//    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");

        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "onRestart");

        super.onRestart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");

        super.onResume();

        if (mSession == null) {
            return;
        }
        mSession.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");

        super.onPause();

        if (mSession == null) {
            return;
        }
        mSession.onPause();

        if (isFinishing()) {
            disconnectSession();
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onPause");

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");

        disconnectSession();

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this, getString(R.string.rationale_ask_again))
                    .setTitle(getString(R.string.title_settings_dialog))
                    .setPositiveButton(getString(R.string.setting))
                    .setNegativeButton(getString(R.string.cancel), null)
                    .setRequestCode(RC_SETTINGS_SCREEN_PERM)
                    .build()
                    .show();
        }
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {
        String[] perms = { android.Manifest.permission.INTERNET, android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE };
        if (EasyPermissions.hasPermissions(this, perms)) {
            Log.d(TAG,"done request permissions");
            //All Start From Here
            initSession();

        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), RC_VIDEO_APP_PERM, perms);
        }
    }


    private void attachSubscriberView(Subscriber subscriber) {

//        if (subscriber != null) {
//            if (mSubscriberViewContainer != null && mSubscriberViewContainer.indexOfChild(mSubscriber.getView()) == -1) {
//                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//                mSubscriberViewContainer.addView(mSubscriber.getView(), layoutParams);
//
//                subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
//                        BaseVideoRenderer.STYLE_VIDEO_FILL);
//            }
//        }
        mSubscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        mSubscriberViewContainer.addView(mSubscriber.getView());

    }

    private void attachPublisherView(Publisher publisher) {
        if (publisher != null) {
            publisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                    BaseVideoRenderer.STYLE_VIDEO_FILL);

            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    320, 240);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP,
                    RelativeLayout.TRUE);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
                    RelativeLayout.TRUE);

            layoutParams.topMargin = dpToPx(getApplicationContext(), 20);
            layoutParams.rightMargin = dpToPx(getApplicationContext(), 20);

            mPublisherViewContainer.addView(publisher.getView(), layoutParams);
        }
    }


    @Override
    public void onConnected(Session session) {
        Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());
//
//        mPublisher = new Publisher.Builder(MainActivity.this).name("publisher").build();
//        mPublisher.setPublisherListener(this);
//        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
//
//        mPublisherViewContainer.addView(mPublisher.getView());
//        if (mPublisher.getView() instanceof GLSurfaceView) {
//            ((GLSurfaceView)(mPublisher.getView())).setZOrderOnTop(true);
//        }
//
//        mSession.publish(mPublisher);
        if (mPublisher != null) {
            attachPublisherView(mPublisher);
            session.publish(mPublisher);

        }

    }

    @Override
    public void onDisconnected(Session session) {
        Log.d(TAG, "onDisconnected: disconnected from session " + session.getSessionId());

        mSession = null;
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.d(TAG, "onError: Error (" + opentokError.getMessage() + ") in session " + session.getSessionId());

        Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.d(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());

        if (Config.SUBSCRIBE_TO_SELF) {
            return;
        }
        if (mSubscriber != null) {
            return;
        }

        subscribeToStream(stream);
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.d(TAG, "onStreamDropped: Stream " + stream.getStreamId() + " dropped from session " + session.getSessionId());

        if (Config.SUBSCRIBE_TO_SELF) {
            return;
        }
        if (mSubscriber == null) {
            return;
        }

        if (mSubscriber.getStream().equals(stream)) {
            mSubscriberViewContainer.removeView(mSubscriber.getView());
            mSubscriber.destroy();
            mSubscriber = null;
        }
    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.d(TAG, "onStreamCreated: Own stream " + stream.getStreamId() + " created");

        if (!Config.SUBSCRIBE_TO_SELF) {
            return;
        }

        subscribeToStream(stream);
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        Log.d(TAG, "onStreamDestroyed: Own stream " + stream.getStreamId() + " destroyed");
    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        Log.d(TAG, "onError: Error (" + opentokError.getMessage() + ") in publisher");

        Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onVideoDataReceived(SubscriberKit subscriberKit) {

        viewLltListActions.setVisibility(View.VISIBLE);
        // stop loading spinning
        mLoadingSub.setVisibility(View.GONE);
        viewControlParent.setVisibility(View.VISIBLE);

        attachSubscriberView(mSubscriber);

    }

    @Override
    public void onVideoDisabled(SubscriberKit subscriberKit, String s) {

    }

    @Override
    public void onVideoEnabled(SubscriberKit subscriberKit, String s) {

    }

    @Override
    public void onVideoDisableWarning(SubscriberKit subscriberKit) {

    }

    @Override
    public void onVideoDisableWarningLifted(SubscriberKit subscriberKit) {

    }

    private void subscribeToStream(Stream stream) {

//        mSubscriber = new Subscriber(MainActivity.this, stream);
//        mSubscriber.setVideoListener(this);
//        mSession.subscribe(mSubscriber);

        mSubscriber = new Subscriber(MainActivity.this, stream);
        mSubscriber.setRenderer(new BasicCustomVideoRenderer(this, clone ));
        mSubscriber.setVideoListener(this);
        mSession.subscribe(mSubscriber);

    }

    private void disconnectSession() {
        if (mSession == null) {
            return;
        }

        if (mSubscriber != null) {
            mSubscriberViewContainer.removeView(mSubscriber.getView());
            mSession.unsubscribe(mSubscriber);
            mSubscriber.destroy();
            mSubscriber = null;
        }

        if (mPublisher != null) {
            mPublisherViewContainer.removeView(mPublisher.getView());
            mSession.unpublish(mPublisher);
            mPublisher.destroy();
            mPublisher = null;
        }
        mSession.disconnect();
    }

    private int dpToPx(Context context, int dp) {
        double screenDensity = context.getResources().getDisplayMetrics().density;
        return (int) (screenDensity * (double) dp);
    }
}
