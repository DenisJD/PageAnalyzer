package hexlet.code.domain;

import io.ebean.Model;
import io.ebean.annotation.NotNull;
import io.ebean.annotation.WhenCreated;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import java.time.Instant;

@Entity
public final class UrlCheck extends Model {
    @Id
    private long id;
    @WhenCreated
    private Instant createdAt;
    private final int statusCode;
    private final String title;
    private final String h1;
    @Lob
    @Column(columnDefinition = "varchar(255)")
    private final String description;
    @ManyToOne
    @NotNull
    private final Url url;

    public UrlCheck(int pStatusCode, String pTitle, String pH1, String pDescription, Url pUrl) {
        this.statusCode = pStatusCode;
        this.title = pTitle;
        this.h1 = pH1;
        this.description = pDescription;
        this.url = pUrl;
    }

    public long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getTitle() {
        return title;
    }

    public String getH1() {
        return h1;
    }

    public String getDescription() {
        return description;
    }

    public Url getUrl() {
        return url;
    }
}
