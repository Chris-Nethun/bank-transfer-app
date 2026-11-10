package com.example.bankapp.repository;

import com.example.bankapp.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByEmail(String email);

    @Modifying
    @Query("""
            UPDATE Customer c
            SET c.balance = c.balance - :amount
            WHERE c.accountNumber = :accountNumber
              AND c.balance >= :amount
            """)
    int debitBalance(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("""
            UPDATE Customer c
            SET c.balance = c.balance + :amount
            WHERE c.accountNumber = :accountNumber
            """)
    int creditBalance(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);

    @Query("SELECT c FROM Customer c ORDER BY c.fullName ASC")
    List<Customer> findAllByOrderByFullNameAsc();
}
