package com.example.bankapp.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

/** Inserts three sample customers the first time the application starts (empty database). */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE bank_transactions MODIFY COLUMN from_account VARCHAR(255) NULL");
            jdbcTemplate.execute("ALTER TABLE bank_transactions MODIFY COLUMN to_account VARCHAR(255) NULL");
        } catch (Exception ignored) {
            // Ignored if table doesn't exist yet or DB does not support this syntax
        }

        if (customerRepository.count() > 0) {
            return;
        }

        customerRepository.save(new Customer(null, "Nuwan Jayasinghe", "nuwan@example.com", "100001", new BigDecimal("25000.00")));
        customerRepository.save(new Customer(null, "Sajini Perera", "sajini@example.com", "100002", new BigDecimal("28000.00")));
        customerRepository.save(new Customer(null, "Kasun Silva", "kasun@example.com", "100003", new BigDecimal("32000.00")));
        customerRepository.save(new Customer(null, "Bank Fees Account", "bank@example.com", "999999", BigDecimal.ZERO));
    }
}
