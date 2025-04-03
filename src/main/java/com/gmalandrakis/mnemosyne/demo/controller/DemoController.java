package com.gmalandrakis.mnemosyne.demo.controller;


import com.gmalandrakis.mnemosyne.demo.model.Customer;
import com.gmalandrakis.mnemosyne.demo.model.Transaction;
import com.gmalandrakis.mnemosyne.demo.service.CustomerService;
import com.gmalandrakis.mnemosyne.demo.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class DemoController {

    private final CustomerService customerService;
    private final TransactionService transactionService;

    @Autowired
    public DemoController(CustomerService customerService, TransactionService transactionService) {
        this.customerService = customerService;
        this.transactionService = transactionService;
    }

    @PostMapping(path = "getTransactions", consumes = "application/json", produces = "application/json")
    public ResponseEntity<List<Transaction>> getTransactions(@RequestBody List<UUID> id) {
        return ResponseEntity.ok(transactionService.getTransactionByIds(new HashSet<>(id)));
    }

    @PostMapping(path = "createTransaction", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Void> createTransaction(@RequestBody Transaction transaction) {
        transactionService.addTransaction(transaction);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(path = "deleteTransactions", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Void> deleteTransaction(@RequestBody List<Transaction> transaction) {
        transaction.forEach(transactionService::deleteTransaction);
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "invalidateAll")
    public ResponseEntity<Void> invalidateAll() {
        transactionService.invalidateAll();
        return ResponseEntity.ok().build();
    }

    @PostMapping(path = "getBySellers", produces = "application/json")
    public ResponseEntity<List<Transaction>> getBySellers(@RequestBody List<String> users) {
        return ResponseEntity.ok(transactionService.getTransactionsBySellers(new HashSet<>(users)));
    }

    @GetMapping(path = "getBySeller/{sellerId}", produces = "application/json")
    public ResponseEntity<List<Transaction>> getBySeller(@PathVariable("sellerId") String USERNAME) {
        return ResponseEntity.ok(transactionService.getTransactionsBySeller(USERNAME));
    }

    @GetMapping(path = "getById/{id}", produces = "application/json")
    public ResponseEntity<Transaction> getById(@PathVariable("id") UUID ID) {
        return ResponseEntity.ok(transactionService.getById(ID));
    }

    @GetMapping(path = "getPending", produces = "application/json")
    public ResponseEntity<List<Transaction>> getPending() {
        return ResponseEntity.ok(transactionService.getPendingTransactions());
    }


}
