package autonomous.sdk;
import java.util.Date;
/**
 * Created by binhpro on 5/11/17.
 */

public class VideoSession {
    public String sessionId;
    public String token;
    public Long date;
    public VideoSession(){
        Date datetime = new Date();
        this.date = datetime.getTime();
    }
    public VideoSession(String sessionId, String token){
        sessionId = sessionId;
        token=token;
        Date datetime = new Date();
        this.date = datetime.getTime();

    }
}
