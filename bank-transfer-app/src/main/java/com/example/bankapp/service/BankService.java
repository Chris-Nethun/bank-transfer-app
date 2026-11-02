package com.example.bankapp.service;

import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private static final BigDecimal TRANSFER_FEE_RATE = new BigDecimal("0.01");
    private static final String BANK_ACCOUNT_NUMBER = "999999";

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository bankTransactionRepository;

    public List<Customer> getAllCustomers() {
        return customerRepository.findAllByOrderByFullNameAsc();
    }

    public List<Customer> searchCustomers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllCustomers();
        }

        String term = keyword.trim().toLowerCase();
        return customerRepository.findAllByOrderByFullNameAsc().stream()
                .filter(c -> c.getFullName().toLowerCase().contains(term)
                        || c.getEmail().toLowerCase().contains(term)
                        || c.getAccountNumber().contains(term))
                .toList();
    }

    public List<BankTransaction> getLatestTransactions() {
        return bankTransactionRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(20)
                .toList();
    }

    public Optional<Customer> findCustomerByAccountNumber(String accountNumber) {
        return customerRepository.findByAccountNumber(accountNumber);
    }

    public BigDecimal getTotalBalance() {
        return customerRepository.findAll().stream()
                .map(Customer::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long getSuccessCount() {
        return bankTransactionRepository.countByStatus(TransactionStatus.SUCCESS);
    }

    public long getFailedCount() {
        return bankTransactionRepository.countByStatus(TransactionStatus.FAILED);
    }

    public List<BankTransaction> getHistoryForAccount(String accountNumber) {
        return bankTransactionRepository.findByFromAccountOrToAccountOrderByCreatedAtDesc(accountNumber, accountNumber);
    }

    public List<BankTransaction> getLargeTransfers(BigDecimal minAmount) {
        if (minAmount == null) {
            minAmount = new BigDecimal("10000");
        }
        return bankTransactionRepository.findByStatusAndAmountGreaterThanOrderByCreatedAtDesc(
                TransactionStatus.SUCCESS,
                minAmount
        );
    }

    @Transactional
    public Customer addCustomer(Customer customer) {
        if (customer == null) {
            throw new BankException("Customer data is required");
        }
        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new BankException("Email already exists");
        }
        if (customerRepository.existsByAccountNumber(customer.getAccountNumber())) {
            throw new BankException("Account number already exists");
        }
        return customerRepository.save(customer);
    }

    @Transactional
    public BankTransaction deposit(String accountNumber, BigDecimal amount) {
        Customer customer = findCustomerByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        validatePositiveAmount(amount);

        customer.setBalance(customer.getBalance().add(amount));
        customerRepository.save(customer);

        return saveTransaction(null, accountNumber, amount, TransactionStatus.SUCCESS,
                "Deposit completed");
    }

    @Transactional
    public BankTransaction withdraw(String accountNumber, BigDecimal amount) {
        Customer customer = findCustomerByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        validatePositiveAmount(amount);

        if (customer.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(accountNumber);
        }

        customer.setBalance(customer.getBalance().subtract(amount));
        customerRepository.save(customer);

        return saveTransaction(accountNumber, null, amount, TransactionStatus.SUCCESS,
                "Withdrawal completed");
    }

    @Transactional
    public BankTransaction transfer(String fromAccount, String toAccount, BigDecimal amount, boolean simulateFailure) {
        validateTransferAccounts(fromAccount, toAccount);
        validatePositiveAmount(amount);

        Customer sender = findCustomerByAccountNumber(fromAccount)
                .orElseThrow(() -> new AccountNotFoundException(fromAccount));
        Customer receiver = findCustomerByAccountNumber(toAccount)
                .orElseThrow(() -> new AccountNotFoundException(toAccount));

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(fromAccount);
        }

        BigDecimal fee = amount.multiply(TRANSFER_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDebited = amount.add(fee);

        if (sender.getBalance().compareTo(totalDebited) < 0) {
            throw new InsufficientBalanceException(fromAccount);
        }

        if (simulateFailure) {
            throw new BankException("Simulated system failure after withdrawal");
        }

        sender.setBalance(sender.getBalance().subtract(totalDebited));
        receiver.setBalance(receiver.getBalance().add(amount));

        Customer bankAccount = findCustomerByAccountNumber(BANK_ACCOUNT_NUMBER)
                .orElseThrow(() -> new AccountNotFoundException(BANK_ACCOUNT_NUMBER));
        bankAccount.setBalance(bankAccount.getBalance().add(fee));

        customerRepository.saveAll(List.of(sender, receiver, bankAccount));

        saveTransaction(fromAccount, toAccount, amount, TransactionStatus.SUCCESS,
                "Transfer completed. Fee: " + fee);

        BankTransaction transaction = saveTransaction(fromAccount, toAccount, amount, TransactionStatus.SUCCESS,
                "Transfer completed. Fee: " + fee);
        return transaction;
    }

    private void validateTransferAccounts(String fromAccount, String toAccount) {
        if (fromAccount == null || fromAccount.isBlank()) {
            throw new BankException("Sender account is required");
        }
        if (toAccount == null || toAccount.isBlank()) {
            throw new BankException("Receiver account is required");
        }
        if (fromAccount.equals(toAccount)) {
            throw new BankException("Sender and receiver cannot be the same account");
        }
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BankException("Amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Amount must be greater than zero");
        }
    }

    private BankTransaction saveTransaction(String fromAccount, String toAccount, BigDecimal amount,
                                           TransactionStatus status, String description) {
        BankTransaction transaction = new BankTransaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount == null ? BigDecimal.ZERO : amount);
        transaction.setStatus(status);
        transaction.setDescription(description);
        transaction.setCreatedAt(LocalDateTime.now());
        return bankTransactionRepository.save(transaction);
    }
}
