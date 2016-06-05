package me.geso.esmapper;

import me.geso.esmapper.entity.IdSettable;
import me.geso.esmapper.entity.ScoreSettable;

public class EntryBean implements ScoreSettable, IdSettable {
    private String id;
    private String title;
    private String body;
    private int i;
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

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public float getScore() {
        return score;
    }

    @Override
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
