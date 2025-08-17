package de.tonypsilon.rankify.api.poll.facade.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import de.tonypsilon.rankify.api.poll.business.Option;
import de.tonypsilon.rankify.api.poll.business.Poll;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Explicit JSON serializer for domain Poll to keep business layer free from JSON concerns.
 * Produces facade-friendly shape with derived state and flattened options.
 */
class PollJsonSerializer extends JsonSerializer<Poll> {

    @Override
    public void serialize(Poll poll, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("id", poll.id().value().toString());
        gen.writeStringField("title", poll.title().value());

        // options array (order preserved as returned by domain ballot copy)
        gen.writeArrayFieldStart("options");
        for (Option option : poll.ballot().options()) {
            gen.writeString(option.text());
        }
        gen.writeEndArray();

        // schedule object
        gen.writeObjectFieldStart("schedule");
        if (poll.schedule().start() != null) {
            gen.writeObjectField("start", poll.schedule().start());
        } else {
            gen.writeNullField("start");
        }
        if (poll.schedule().end() != null) {
            gen.writeObjectField("end", poll.schedule().end());
        } else {
            gen.writeNullField("end");
        }
        gen.writeEndObject();

        gen.writeObjectField("created", poll.created());
        gen.writeStringField("state", deriveState(poll));
        gen.writeEndObject();
    }

    private String deriveState(Poll poll) {
        LocalDateTime start = poll.schedule().start();
        LocalDateTime end = poll.schedule().end();
        LocalDateTime now = LocalDateTime.now();
        if (end != null && end.isBefore(now)) {
            return "FINISHED";
        }
        if (start == null || start.isAfter(now)) {
            return "IN_PREPARATION";
        }
        return "ONGOING";
    }
}

