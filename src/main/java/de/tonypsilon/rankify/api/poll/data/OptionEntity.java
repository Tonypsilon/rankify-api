package de.tonypsilon.rankify.api.poll.data;

import de.tonypsilon.rankify.api.poll.business.PollId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "options", uniqueConstraints = {
        @UniqueConstraint(name = "uk_options_poll_text", columnNames = {"poll_id", "text"})
})
public class OptionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false, foreignKey = @ForeignKey(name = "fk_options_poll_id"))
    private PollEntity poll;

    @Column(name = "text", nullable = false)
    private String text;

    // position column managed via @OrderColumn on PollEntity.options list

    public OptionEntity() { /* for JPA */ }

    public OptionEntity(PollEntity poll, String text) {
        this.id = UUID.randomUUID();
        this.poll = poll;
        this.text = text;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PollEntity getPoll() {
        return poll;
    }

    public void setPoll(PollEntity poll) {
        this.poll = poll;
    }

    public PollId getPollId() {
        return poll == null ? null : new PollId(poll.getId());
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
