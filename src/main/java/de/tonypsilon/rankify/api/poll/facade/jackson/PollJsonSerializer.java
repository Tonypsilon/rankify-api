package de.tonypsilon.rankify.api.poll.facade.jackson;

import de.tonypsilon.rankify.api.poll.business.Option;
import de.tonypsilon.rankify.api.poll.business.Poll;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Explicit JSON serializer for domain Poll to keep business layer free from JSON concerns.
 * Produces facade-friendly shape with flattened options.
 */
class PollJsonSerializer extends ValueSerializer<Poll> {

    @Override
    public void serialize(Poll poll, JsonGenerator gen, SerializationContext serializers) {
        gen.writeStartObject();
        gen.writeStringProperty("id", poll.id().value().toString());
        gen.writeStringProperty("title", poll.title().value());

        // options array (order preserved as returned by domain ballot copy)
        gen.writeArrayPropertyStart("options");
        for (Option option : poll.ballot().options()) {
            gen.writeString(option.text());
        }
        gen.writeEndArray();

        // schedule object
        gen.writeObjectPropertyStart("schedule");
        if (poll.schedule().start() != null) {
            gen.writePOJOProperty("start", poll.schedule().start());
        } else {
            gen.writeNullProperty("start");
        }
        if (poll.schedule().end() != null) {
            gen.writePOJOProperty("end", poll.schedule().end());
        } else {
            gen.writeNullProperty("end");
        }
        gen.writeEndObject();

        gen.writePOJOProperty("created", poll.created());
        gen.writeEndObject();
    }


}

