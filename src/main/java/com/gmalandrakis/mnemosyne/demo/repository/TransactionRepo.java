package com.gmalandrakis.mnemosyne.demo.repository;

import com.gmalandrakis.mnemosyne.demo.model.Customer;
import com.gmalandrakis.mnemosyne.demo.model.Transaction;
import org.hibernate.annotations.Proxy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@Transactional
@Proxy(lazy = false)
public interface TransactionRepo extends JpaRepository<Transaction, UUID> {


    List<Transaction> getTransactionBySellerid(String sellerid);

    List<Transaction> getTransactionByCompleted(boolean completed);
}
