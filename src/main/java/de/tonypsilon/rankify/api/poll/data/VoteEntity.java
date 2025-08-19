package de.tonypsilon.rankify.api.poll.data;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "votes")
public class VoteEntity {

    @Id
    private UUID id;

    @Column(name = "poll_id")
    private UUID pollId;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "vote", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Ranking> rankings = new LinkedHashSet<>();

    public VoteEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getPollId() {
        return pollId;
    }

    public void setPollId(final UUID pollId) {
        this.pollId = pollId;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(final LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Set<Ranking> getRankings() {
        return rankings;
    }

    public void setRankings(Set<Ranking> rankings) {
        this.rankings = rankings;
    }

    public void addRanking(Ranking ranking) {
        rankings.add(ranking);
        ranking.setVote(this);
    }
}
