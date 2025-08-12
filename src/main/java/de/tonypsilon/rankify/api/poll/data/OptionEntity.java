package de.tonypsilon.rankify.api.poll.data;

import de.tonypsilon.rankify.api.poll.business.PollId;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "options")
public class OptionEntity {
    
    @EmbeddedId
    private OptionId id;

    public OptionEntity() {
    }

    public OptionEntity(PollId pollId, String text) {
        this.id = new OptionId(pollId.value(), text);
    }

    public PollId getPollId() {
        return new PollId(id.pollId);
    }

    public String getText() {
        return id.text;
    }

    public OptionId getId() {
        return id;
    }

    public void setId(OptionId id) {
        this.id = id;
    }

    @Embeddable
    public static class OptionId implements Serializable {

        @Column(name = "poll_id")
        private UUID pollId;

        @Column(name = "text")
        private String text;

        public OptionId() {
        }

        public OptionId(UUID pollId, String text) {
            this.pollId = pollId;
            this.text = text;
        }

        public UUID getPollId() {
            return pollId;
        }

        public void setPollId(UUID pollId) {
            this.pollId = pollId;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OptionId optionId = (OptionId) o;
            return Objects.equals(pollId, optionId.pollId) && Objects.equals(text, optionId.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pollId, text);
        }
    }
}
