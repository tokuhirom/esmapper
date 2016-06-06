package me.geso.esmapper;

import me.geso.esmapper.annotation.Id;
import me.geso.esmapper.annotation.Score;

public class EntryBean {
    @Id
    private String id;
    private String title;
    private String body;
    private int i;
    @Score
    private float score;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }
}
