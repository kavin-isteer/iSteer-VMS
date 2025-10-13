package com.isteer.vms.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.isteer.vms.security.TokenAuthenticationFilter;
import com.isteer.vms.security.VMSUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {
	private TokenAuthenticationFilter authenticationFilter;
	public SecurityConfiguration(TokenAuthenticationFilter authenticationFilter) {
		this.authenticationFilter = authenticationFilter;
	}
	
	@Bean
	public SecurityFilterChain configure(HttpSecurity http) throws Exception {
		http.csrf(csrf->csrf.disable());
		http.authorizeHttpRequests(req->{
		    req.requestMatchers("/login","/logout").permitAll();
		    req.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
		    req.requestMatchers("/applications/unresolved","/computers","/getComputer/**").permitAll();
			req.anyRequest().authenticated();
		});
		http.sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.logout(logout -> logout.disable());
		http.addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);
		http.formLogin(form->form.disable());
		http.httpBasic(basic->basic.disable());
		return http.build();	
	}
	
	@Bean
	public BCryptPasswordEncoder getPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
}
