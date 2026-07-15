package com.cartify.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CartifyApiApplication {

	public static void main(String[] args) {
		// Avoid accidental Spring debug mode from unrelated host variables such as DEBUG=release.
		System.setProperty("debug", System.getProperty("debug", "false"));
		SpringApplication.run(CartifyApiApplication.class, args);
	}

}
