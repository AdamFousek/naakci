package cz.naakci.app.naakciprojekt.models;

import java.util.Date;

/**
 * Created by adamfousek on 28.11.17.
 * Třída pro daný kód
 */

public class Code {

    private String code;
    private Date used;
    private String line_first;
    private String line_second;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Date getUsed() {
        return used;
    }

    public void setUsed(Date used) {
        this.used = used;
    }

    public String getLine_first() {
        return line_first;
    }

    public void setLine_first(String line_first) {
        this.line_first = line_first;
    }

    public String getLine_second() {
        return line_second;
    }

    public void setLine_second(String line_second) {
        this.line_second = line_second;
    }
}
