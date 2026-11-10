package com.example.bankapp.service;

import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BankServiceTest {

    @Test
    void transferCreatesOneTransaction() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        BankTransactionRepository bankTransactionRepository = mock(BankTransactionRepository.class);

        when(customerRepository.debit("100001", new BigDecimal("1010.00"))).thenReturn(1);
        when(customerRepository.credit("100002", new BigDecimal("1000.00"))).thenReturn(1);
        when(customerRepository.credit("999999", new BigDecimal("10.00"))).thenReturn(1);
        when(bankTransactionRepository.save(any(BankTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankService bankService = new BankService(customerRepository, bankTransactionRepository);

        BankTransaction transaction = bankService.transfer(
                "100001", "100002", new BigDecimal("1000.00"), false);

        assertNotNull(transaction);
        assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());
        verify(customerRepository).debit("100001", new BigDecimal("1010.00"));
        verify(customerRepository).credit("100002", new BigDecimal("1000.00"));
        verify(customerRepository).credit("999999", new BigDecimal("10.00"));
        verify(bankTransactionRepository, times(1)).save(any(BankTransaction.class));
    }

    @Test
    void withdrawalCreatesTransactionWithoutToAccount() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        BankTransactionRepository bankTransactionRepository = mock(BankTransactionRepository.class);

        when(customerRepository.debit("100001", new BigDecimal("1000.00"))).thenReturn(1);
        when(bankTransactionRepository.save(any(BankTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankService bankService = new BankService(customerRepository, bankTransactionRepository);

        BankTransaction transaction = bankService.withdraw("100001", new BigDecimal("1000.00"));

        assertNotNull(transaction);
        assertEquals("100001", transaction.getFromAccount());
        assertNull(transaction.getToAccount());
        assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());
        assertEquals("Withdrawal completed", transaction.getDescription());
        verify(customerRepository).debit("100001", new BigDecimal("1000.00"));
        verify(bankTransactionRepository, times(1)).save(any(BankTransaction.class));
    }

    @Test
    void depositCreatesTransactionWithoutFromAccount() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        BankTransactionRepository bankTransactionRepository = mock(BankTransactionRepository.class);

        when(customerRepository.credit("100001", new BigDecimal("1000.00"))).thenReturn(1);
        when(bankTransactionRepository.save(any(BankTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankService bankService = new BankService(customerRepository, bankTransactionRepository);

        BankTransaction transaction = bankService.deposit("100001", new BigDecimal("1000.00"));

        assertNotNull(transaction);
        assertNull(transaction.getFromAccount());
        assertEquals("100001", transaction.getToAccount());
        assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());
        assertEquals("Deposit completed", transaction.getDescription());
        verify(customerRepository).credit("100001", new BigDecimal("1000.00"));
        verify(bankTransactionRepository, times(1)).save(any(BankTransaction.class));
    }

    @Test
    void withdrawalRejectsInsufficientBalanceWithoutSavingTransaction() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        BankTransactionRepository bankTransactionRepository = mock(BankTransactionRepository.class);
        when(customerRepository.debit("100001", new BigDecimal("1000.00"))).thenReturn(0);
        when(customerRepository.countAccounts("100001")).thenReturn(1L);

        BankService bankService = new BankService(customerRepository, bankTransactionRepository);

        assertThrows(InsufficientBalanceException.class,
                () -> bankService.withdraw("100001", new BigDecimal("1000.00")));
        verify(bankTransactionRepository, times(0)).save(any(BankTransaction.class));
    }

}
