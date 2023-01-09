package hexlet.code.domain;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.time.Instant;
import java.util.List;

@Entity
public final class Url extends Model {
    @Id
    private long id;
    private final String name;
    @WhenCreated
    private Instant createdAt;
    @OneToMany(cascade = CascadeType.ALL)
    private List<UrlCheck> listChecks;

    public Url(String urlName) {
        this.name = urlName;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<UrlCheck> getListChecks() {
        return listChecks;
    }

    public Instant getTimeOfLastCheck() {
        if (!listChecks.isEmpty()) {
            return listChecks.get(listChecks.size() - 1).getCreatedAt();
        }
        return null;
    }

    public int getStatusCodeOfLastCheck() {
        if (!listChecks.isEmpty()) {
            return listChecks.get(listChecks.size() - 1).getStatusCode();
        }
        return -1;
    }
}
