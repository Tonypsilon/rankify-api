package de.tonypsilon.rankify.api.poll.facade.jackson;

import de.tonypsilon.rankify.api.poll.business.Poll;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

@Configuration
class PollJacksonConfig {

    @Bean
    JacksonModule pollJacksonModule() {
        SimpleModule module = new SimpleModule("PollJsonModule");
        module.addSerializer(Poll.class, new PollJsonSerializer());
        return module;
    }
}

