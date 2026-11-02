package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService transactionAuditService;

    @GetMapping("/")
    public String dashboard(
            @RequestParam(required = false) String keyword,
            Model model
    ) {
        List<Customer> customers = bankService.searchCustomers(keyword);
        model.addAttribute("customers", customers);
        model.addAttribute("allCustomers", customers);
        model.addAttribute("transferRequest", new TransferRequest());
        model.addAttribute("customer", new Customer());
        model.addAttribute("totalCustomers", customers.size());
        model.addAttribute("totalBalance", bankService.getTotalBalance());
        model.addAttribute("successCount", bankService.getSuccessCount());
        model.addAttribute("failedCount", bankService.getFailedCount());
        model.addAttribute("transactions", bankService.getLatestTransactions());
        model.addAttribute("keyword", keyword);
        return "bank";
    }

    @PostMapping("/customers")
    public String addCustomer(
            @Valid @ModelAttribute("customer") Customer customer,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            loadDashboardModel(model, customer, null);
            return "bank";
        }

        try {
            bankService.addCustomer(customer);
            redirectAttributes.addFlashAttribute("success", "Customer added successfully.");
            return "redirect:/";
        } catch (BankException e) {
            model.addAttribute("error", e.getMessage());
            loadDashboardModel(model, customer, null);
            return "bank";
        }
    }

    @PostMapping("/transfer")
    public String transfer(
            @Valid @ModelAttribute("transferRequest") TransferRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            loadDashboardModel(model, new Customer(), request);
            return "bank";
        }

        try {
            bankService.transfer(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    request.isSimulateFailure()
            );
            redirectAttributes.addFlashAttribute("success", "Transfer completed successfully.");
            return "redirect:/";
        } catch (Exception e) {
            transactionAuditService.recordFailure(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    e.getMessage()
            );
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/deposit")
    public String showDeposit(Model model) {
        model.addAttribute("customers", bankService.getAllCustomers());
        return "deposit";
    }

    @PostMapping("/deposit")
    public String deposit(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        try {
            bankService.deposit(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success", "Deposit completed successfully.");
            return "redirect:/deposit";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("customers", bankService.getAllCustomers());
            return "deposit";
        }
    }

    @GetMapping("/withdraw")
    public String showWithdraw(Model model) {
        model.addAttribute("customers", bankService.getAllCustomers());
        return "withdraw";
    }

    @PostMapping("/withdraw")
    public String withdraw(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        try {
            bankService.withdraw(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success", "Withdrawal completed successfully.");
            return "redirect:/withdraw";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("customers", bankService.getAllCustomers());
            return "withdraw";
        }
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(@PathVariable String accountNumber, Model model) {
        Customer customer = bankService.findCustomerByAccountNumber(accountNumber)
                .orElseThrow(() -> new BankException("Account " + accountNumber + " was not found"));

        List<BankTransaction> transactions = bankService.getHistoryForAccount(accountNumber);
        BigDecimal totalSent = transactions.stream()
                .filter(t -> t.getFromAccount() != null && t.getFromAccount().equals(accountNumber))
                .filter(t -> t.getStatus() == com.example.bankapp.model.TransactionStatus.SUCCESS)
                .map(BankTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("customer", customer);
        model.addAttribute("transactions", transactions);
        model.addAttribute("totalSent", totalSent);
        return "history";
    }

    @GetMapping("/large-transfers")
    public String largeTransfers(@RequestParam(defaultValue = "10000") BigDecimal min, Model model) {
        model.addAttribute("min", min);
        model.addAttribute("transfers", bankService.getLargeTransfers(min));
        return "large-transfers";
    }

    private void loadDashboardModel(Model model, Customer customer, TransferRequest transferRequest) {
        List<Customer> allCustomers = bankService.getAllCustomers();
        model.addAttribute("customers", allCustomers);
        model.addAttribute("allCustomers", allCustomers);
        model.addAttribute("customer", customer == null ? new Customer() : customer);
        model.addAttribute("transferRequest", transferRequest == null ? new TransferRequest() : transferRequest);
        model.addAttribute("totalCustomers", allCustomers.size());
        model.addAttribute("totalBalance", bankService.getTotalBalance());
        model.addAttribute("successCount", bankService.getSuccessCount());
        model.addAttribute("failedCount", bankService.getFailedCount());
        model.addAttribute("transactions", bankService.getLatestTransactions());
    }
}
