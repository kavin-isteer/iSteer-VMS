package com.isteer.vms.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfiguration {
	@Bean
	  public OpenAPI configOpenAPI() {
	      return new OpenAPI()
	              .info(new Info().title("Vulnerability Management System API")
	              .description("Rest API endpoints for isteer Vulnerability Management System")
	              .version("v0.0.1"));
	  }
}
