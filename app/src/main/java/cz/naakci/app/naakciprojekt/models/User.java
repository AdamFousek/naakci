package cz.naakci.app.naakciprojekt.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by adamfousek on 28.11.17.
 * Třída pro uživatele
 */

public class User implements Serializable {

    @SerializedName("events")
    @Expose
    private ArrayList<Event> events = new ArrayList<Event>();

    private String name;

    private String password;

    public ArrayList<Event> getEvents() {
        return events;
    }

    public void setEvents(ArrayList<Event> events) {
        this.events = events;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
