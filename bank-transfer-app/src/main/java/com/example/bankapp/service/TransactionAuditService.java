package com.example.bankapp.service;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionAuditService {

    private final BankTransactionRepository bankTransactionRepository;

    /**
     * Saves a FAILED record after an account operation was rolled back.
     * REQUIRES_NEW = always start a brand-new, independent transaction,
     * so this record is committed even though the account operation was rolled back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String fromAccount, String toAccount, BigDecimal amount, String message) {
        BankTransaction transaction = new BankTransaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount == null ? BigDecimal.ZERO : amount);
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setDescription(message == null ? "Account operation failed and was rolled back." : message);
        transaction.setCreatedAt(LocalDateTime.now());
        bankTransactionRepository.save(transaction);
    }
}
