package com.example.bankapp.config;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Inserts three sample customers the first time the application starts (empty database). */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            return;
        }

        customerRepository.save(new Customer(null, "Nuwan Jayasinghe", "nuwan@example.com", "100001", new BigDecimal("25000.00")));
        customerRepository.save(new Customer(null, "Sajini Perera", "sajini@example.com", "100002", new BigDecimal("18000.00")));
        customerRepository.save(new Customer(null, "Kasun Silva", "kasun@example.com", "100003", new BigDecimal("32000.00")));
        customerRepository.save(new Customer(null, "Bank Fees Account", "bank@example.com", "999999", BigDecimal.ZERO));
    }
}
