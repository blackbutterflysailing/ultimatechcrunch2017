package autonomous.sdk;

import android.graphics.Bitmap;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import autonomous.clone.example.AppController;

/**
 * Created by binhpro on 5/11/17.
 */

public class Clone {

    public String productId;

    public VideoSession session;

    private DatabaseReference mDatabase;

    private AppController app = AppController.getInstance();

    private String currentArchiveId;

    public Clone(String productId){
        this.productId = productId;
        currentArchiveId = "";

        mDatabase = FirebaseDatabase.getInstance().getReference();
        //==init session here /====
        session =new VideoSession("","");

    }

    public void writeFireBaseSession(){

        HashMap<String, Object> result = new HashMap<>();

        result.put("data", session.date );
        result.put("sessionId", session.sessionId);
        result.put("token", session.token);

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("sessions/"+ productId , result);
        mDatabase.updateChildren(childUpdates);
    }

    private void executeCommand(String value){

        String package_data = "{\"source\":\"WebApp\",\"type\":\"clone_control\",\"data\":\"{\\\"action\\\":\\\"MOVE\\\",\\\"name\\\":\\\"" + value + "\\\"}\"}";
        if(mDatabase!=null){
            mDatabase.child(productId).push().setValue(package_data);
        }else {
            Log.d("Move"," MDatabase is Null");
        }

    }

    public void autocharge() {

        //var data = {"action": "CHARGE"}
        //var packages = {"source": "WebApp", "type":"autodocking", "data": JSON.stringify(data)}
        String package_data = "{\"source\":\"WebApp\",\"type\":\"autodocking\",\"data\":\"{\\\"action\\\":\\\"CHARGE\\\"}";
        if(mDatabase!=null){
            mDatabase.child(productId).push().setValue(package_data);
        }else {
            Log.d("Charge"," MDatabase is Null");
        }
    }
    public void move(String value){
        executeCommand(value);
    }
    public void moveForward(){
        executeCommand(Config.FORWARD);
    }
    public void moveBackward(){
        executeCommand(Config.BACKWARD);
    }
    public void turnLeft(){
        executeCommand(Config.LEFT);
    }
    public void turnRight(){
        executeCommand(Config.RIGHT);
    }
    public void stop(){
        executeCommand(Config.STOP);
    }

    public Bitmap capture(){

        return null;
    }

    /** Example call RecordVideo
     * startRecording
     * return
     */
    public void startRecording(){

        final String PRODUCT_UPDATE_URL = Config.HOST + "/session/start-archive?session_id="+this.session.sessionId;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, PRODUCT_UPDATE_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            Log.d("DATA", response);
                            currentArchiveId = obj.getString("id");


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

    /** Example stop RecordVideo
     * stopRecording
     * @param archiveId
     */
    public void stopRecording(){

        final String PRODUCT_UPDATE_URL = Config.HOST + "/session/stop-archive?archive_id=" + currentArchiveId;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, PRODUCT_UPDATE_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            //
                            JSONObject obj = new JSONObject(response);
                            Log.d("DATA", response);
                            currentArchiveId = obj.getString("id");

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

    /** Example get List History
     * get List History of Archives
     */
    public void history(){

        final String PRODUCT_UPDATE_URL = Config.HOST + "/history";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, PRODUCT_UPDATE_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            //
                            JSONObject obj = new JSONObject(response);
                            Log.d("DATA", response);
                            //Continue...

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



    public void setSession(VideoSession session) {
        this.session = session;
        writeFireBaseSession();
    }
}
