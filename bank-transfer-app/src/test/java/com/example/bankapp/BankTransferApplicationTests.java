package com.example.bankapp;

import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.service.BankService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class BankTransferApplicationTests {

    @Autowired
    private BankService bankService;

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
}
