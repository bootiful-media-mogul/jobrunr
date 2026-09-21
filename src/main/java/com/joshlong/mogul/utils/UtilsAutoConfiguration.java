package com.joshlong.mogul.utils;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration
class UtilsAutoConfiguration {

	@Bean
	JsonUtils jsonUtils(JsonMapper jsonMapper) {
		return new JsonUtils(jsonMapper);
	}

}
