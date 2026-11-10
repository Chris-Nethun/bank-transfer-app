package com.example.bankapp;

import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import com.example.bankapp.service.BankService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BankTransferApplicationTests {

    @Autowired
    private BankService bankService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @Test
    void testDepositAndWithdraw() {
        Customer customer = bankService.findCustomerByAccountNumber("100001").orElseThrow();
        BigDecimal initialBalance = customer.getBalance();

        BankTransaction depositTx = bankService.deposit("100001", new BigDecimal("100.00"));
        assertNotNull(depositTx);
        assertNull(depositTx.getFromAccount());
        assertEquals("100001", depositTx.getToAccount());
        assertEquals(TransactionStatus.SUCCESS, depositTx.getStatus());

        Customer afterDeposit = bankService.findCustomerByAccountNumber("100001").orElseThrow();
        assertEquals(0, initialBalance.add(new BigDecimal("100.00")).compareTo(afterDeposit.getBalance()));

        BankTransaction withdrawTx = bankService.withdraw("100001", new BigDecimal("50.00"));
        assertNotNull(withdrawTx);
        assertEquals("100001", withdrawTx.getFromAccount());
        assertNull(withdrawTx.getToAccount());
        assertEquals(TransactionStatus.SUCCESS, withdrawTx.getStatus());

        Customer afterWithdraw = bankService.findCustomerByAccountNumber("100001").orElseThrow();
        assertEquals(0, initialBalance.add(new BigDecimal("50.00")).compareTo(afterWithdraw.getBalance()));
    }

    @Test
    void testWithdrawInsufficientBalance() {
        assertThrows(InsufficientBalanceException.class, () ->
                bankService.withdraw("100001", new BigDecimal("999999999.00"))
        );
    }

    @Test
    void testTransferAndSimulateFailure() {
        Customer sender = bankService.findCustomerByAccountNumber("100001").orElseThrow();
        BigDecimal senderInitial = sender.getBalance();

        Customer receiver = bankService.findCustomerByAccountNumber("100002").orElseThrow();
        BigDecimal receiverInitial = receiver.getBalance();

        // Successful transfer of 1000.00 with 1% fee (10.00)
        BankTransaction tx = bankService.transfer("100001", "100002", new BigDecimal("1000.00"), false);
        assertNotNull(tx);
        assertEquals(TransactionStatus.SUCCESS, tx.getStatus());

        Customer senderAfter = bankService.findCustomerByAccountNumber("100001").orElseThrow();
        Customer receiverAfter = bankService.findCustomerByAccountNumber("100002").orElseThrow();
        assertEquals(0, senderInitial.subtract(new BigDecimal("1010.00")).compareTo(senderAfter.getBalance()));
        assertEquals(0, receiverInitial.add(new BigDecimal("1000.00")).compareTo(receiverAfter.getBalance()));

        // Simulated failure transfer must roll back balance changes
        assertThrows(Exception.class, () ->
                bankService.transfer("100001", "100002", new BigDecimal("500.00"), true)
        );

        Customer senderAfterFail = bankService.findCustomerByAccountNumber("100001").orElseThrow();
        Customer receiverAfterFail = bankService.findCustomerByAccountNumber("100002").orElseThrow();
        assertEquals(0, senderAfter.getBalance().compareTo(senderAfterFail.getBalance()));
        assertEquals(0, receiverAfter.getBalance().compareTo(receiverAfterFail.getBalance()));
    }

    @Test
    @Transactional
    void testCustomerRepositoryMethods() {
        assertTrue(customerRepository.getByAccountNumber("100001").isPresent());
        assertEquals(1, customerRepository.countAccounts("100001"));
        assertEquals(0, customerRepository.countAccounts("non_existing"));
        assertTrue(customerRepository.countEmails("nuwan@example.com") >= 1);
        assertEquals(0, customerRepository.countEmails("nobody@example.com"));

        List<Customer> ordered = customerRepository.findAllOrdered();
        assertFalse(ordered.isEmpty());

        List<Customer> search = customerRepository.searchByName("nuwan");
        assertFalse(search.isEmpty());

        BigDecimal totalBalance = customerRepository.getTotalBalance();
        assertNotNull(totalBalance);

        long countNative = customerRepository.countCustomersNative();
        assertTrue(countNative >= 3);
        assertEquals(countNative, bankService.getTotalCustomers());

        int debited = customerRepository.debit("100001", new BigDecimal("1.00"));
        assertEquals(1, debited);

        int credited = customerRepository.credit("100001", new BigDecimal("1.00"));
        assertEquals(1, credited);
    }

    @Test
    void testBankTransactionRepositoryMethods() {
        List<BankTransaction> allLatest = bankTransactionRepository.findAllLatest();
        assertNotNull(allLatest);

        List<BankTransaction> history = bankTransactionRepository.findHistory("100001");
        assertNotNull(history);

        List<BankTransaction> successTxs = bankTransactionRepository.findByStatusLatest(TransactionStatus.SUCCESS);
        assertNotNull(successTxs);

        long successCount = bankTransactionRepository.countByStatus(TransactionStatus.SUCCESS);
        assertTrue(successCount >= 0);
    }
}
