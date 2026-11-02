package com.example.bankapp.repository;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    long countByStatus(TransactionStatus status);

    List<BankTransaction> findByStatusAndAmountGreaterThanOrderByCreatedAtDesc(
            TransactionStatus status,
            BigDecimal minAmount
    );

    List<BankTransaction> findByFromAccountOrToAccountOrderByCreatedAtDesc(
            String fromAccount,
            String toAccount
    );
}