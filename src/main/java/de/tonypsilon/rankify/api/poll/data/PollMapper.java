package de.tonypsilon.rankify.api.poll.data;

import de.tonypsilon.rankify.api.poll.business.Ballot;
import de.tonypsilon.rankify.api.poll.business.Option;
import de.tonypsilon.rankify.api.poll.business.Poll;
import de.tonypsilon.rankify.api.poll.business.PollId;
import de.tonypsilon.rankify.api.poll.business.PollTitle;
import de.tonypsilon.rankify.api.poll.business.Schedule;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashSet;
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
        // preserve provided ballot order when persisting (position reflects insertion order)
        for (Option option : poll.ballot().options()) {
            OptionEntity optionEntity = new OptionEntity(entity, option.text());
            entity.addOption(optionEntity);
        }
        return entity;
    }

    public Poll toDomain(PollEntity entity) {
        PollId pollId = new PollId(entity.getId());
        PollTitle title = new PollTitle(entity.getTitle());
        Schedule schedule = new Schedule(entity.getStart(), entity.getEnd());

        SequencedSet<Option> options = new LinkedHashSet<>();
        entity.getOptions().stream()
                .sorted(Comparator.comparing(OptionEntity::getText))
                .forEach(o -> options.add(new Option(o.getText())));

        Ballot ballot = new Ballot(options);

        return new Poll(pollId, title, ballot, schedule, entity.getCreated());
    }
}
