package cz.naakci.app.naakciprojekt.models;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Created by adamfousek on 09.12.17.
 */

public class Tickets {

    @SerializedName("codes")
    private Map<String, Code> codes;
    private int eventId;

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public Map<String, Code> getCodes() {
        return codes;
    }

    public void setCodes(Map<String, Code> codes) {
        this.codes = codes;
    }
}
