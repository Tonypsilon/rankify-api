package de.tonypsilon.rankify.api.poll.data;

import de.tonypsilon.rankify.api.poll.business.*;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;

@Component
class PollMapper {

    public PollEntity toEntity(Poll poll) {
        PollEntity entity = new PollEntity();
        entity.setId(poll.id().value());
        entity.setTitle(poll.title().value());
        entity.setStart(poll.schedule().start());
        entity.setEnd(poll.schedule().end());
        entity.setCreated(poll.created());
        return entity;
    }

    public Poll toDomain(PollEntity entity, List<OptionEntity> optionEntities) {
        PollId pollId = new PollId(entity.getId());
        PollTitle title = new PollTitle(entity.getTitle());
        Schedule schedule = new Schedule(entity.getStart(), entity.getEnd());

        SequencedSet<Option> options = new LinkedHashSet<>();
        for (OptionEntity optionEntity : optionEntities) {
            options.add(new Option(optionEntity.getText()));
        }

        Ballot ballot = new Ballot(options);

        return new Poll(pollId, title, ballot, schedule, entity.getCreated());
    }

    public List<OptionEntity> toOptionEntities(PollId pollId, Ballot ballot) {
        return ballot.options().stream()
                .map(option -> new OptionEntity(pollId, option.text()))
                .toList();
    }
}
