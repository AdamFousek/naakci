package cz.naakci.app.naakciprojekt.models;

import java.io.Serializable;

/**
 * Created by adamfousek on 28.11.17.
 * Třída pro údálosti (akce)
 */

public class Event implements Serializable {

    private int id;
    private String name;
    private int used_tickets;
    private int total_tickets;

    public Event(){

    }

    public Event(int id, String name, int used_tickets, int total_tickets){
        this.id = id;
        this.name = name;
        this.used_tickets = used_tickets;
        this.total_tickets = total_tickets;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUsed_tickets() {
        return used_tickets;
    }

    public void setUsed_tickets(int used_tickets) {
        this.used_tickets = used_tickets;
    }

    public int getTotal_tickets() {
        return total_tickets;
    }

    public void setTotal_tickets(int total_tickets) {
        this.total_tickets = total_tickets;
    }
}
