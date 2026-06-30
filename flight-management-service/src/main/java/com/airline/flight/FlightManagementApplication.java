package com.airline.flight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import com.airline.flight.entity.Flight;
import com.airline.flight.repository.FlightRepository;

@SpringBootApplication
@EnableDiscoveryClient
public class FlightManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlightManagementApplication.class, args);
    }

    @Bean
    public CommandLineRunner demo(FlightRepository repository) {
        return (args) -> {
            repository.save(new Flight("AI101", "Air India", "Delhi", "Mumbai", "2023-10-10", 150, 5000.0));
            repository.save(new Flight("indigo-6E", "Indigo", "Bangalore", "Goa", "2023-11-15", 180, 3500.0));
            repository.save(new Flight("Vistara-99", "Vistara", "Delhi", "London", "2023-12-01", 300, 45000.0));
        };
    }
}
