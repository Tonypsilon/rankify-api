package de.tonypsilon.rankify.api.poll.facade.jackson;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.tonypsilon.rankify.api.poll.business.Poll;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PollJacksonConfig {

    @Bean
    Module pollJacksonModule() {
        SimpleModule module = new SimpleModule("PollJsonModule");
        module.addSerializer(Poll.class, new PollJsonSerializer());
        return module;
    }
}

