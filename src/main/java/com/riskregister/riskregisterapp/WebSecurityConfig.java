package com.riskregister.riskregisterapp;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import com.riskregister.riskregisterapp.services.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {


	@Autowired
	private DataSource dataSource;
	
	@Autowired
	private CustomUserDetailsService customUserDetailsService;
	
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(customUserDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}


	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authenticationProvider(authenticationProvider())
			.authorizeHttpRequests((requests) -> {
				requests
				.requestMatchers( "/home", "/signup", "/forgot-password", 
				"signin", "/create-user", "/login", "/favicon.ico", "/assets/**",
				"/create-user-magic", "/login-magic", "create-user-or-login-magic",
				"/verify-token-and-login",
				"/privacy", "/terms",
				"/public/**", "/resources/**"
				)
				.permitAll()
				.anyRequest().authenticated();
				// requests.requestMatchers(HttpMethod.GET, "/product/**", "/category/**").permitAll();
			}

			)
			.formLogin((form) -> form
				.loginPage("/login")
				.usernameParameter("email")
				.passwordParameter("password")
				.defaultSuccessUrl("/", false)
				.permitAll()
			)
			.logout((logout) -> logout.permitAll())
			.csrf().disable()
			// .rememberMe((rememberMe) -> rememberMe.key("victor-detail-oriented"))
			// .securityContextRepository(securityContextRepository())
			// .securityContext(
			// 	(securityContext) -> securityContext.securityContextRepository(new RequestAttributeSecurityContextRepository())
			// )
			;

		return http.build();
	} 

	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new DelegatingSecurityContextRepository(
			new RequestAttributeSecurityContextRepository(),
			new HttpSessionSecurityContextRepository()
		);
	}


}
