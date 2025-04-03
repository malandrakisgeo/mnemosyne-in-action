package com.gmalandrakis.mnemosyne.demo.service;

import com.gmalandrakis.mnemosyne.annotations.Cached;
import com.gmalandrakis.mnemosyne.annotations.UpdatedValue;
import com.gmalandrakis.mnemosyne.annotations.UpdatesCache;
import com.gmalandrakis.mnemosyne.annotations.UpdatesCache.AddMode;
import com.gmalandrakis.mnemosyne.annotations.UpdatesCache.RemoveMode;

import com.gmalandrakis.mnemosyne.demo.customcache.FIFOCache;
import com.gmalandrakis.mnemosyne.demo.model.Transaction;
import com.gmalandrakis.mnemosyne.demo.repository.TransactionRepo;
import org.hibernate.annotations.Proxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Proxy(lazy = false)
public class TransactionService {

    private final TransactionRepo repository;

    @Autowired
    public TransactionService(TransactionRepo repository) {
        this.repository = repository;
    }

    @Cached(cacheName = "transactionsBySellers", allowSeparateHandlingForKeyCollections = false)
    public List<Transaction> getTransactionsBySellers(Set<String> username) {
        System.out.println("Cache miss for Ssellers!");
        // var a = repository.getTransactionBySellerid(username.get(0));
        // a.size();
        var ret = username.stream().map(repository::getTransactionBySellerid).flatMap(List::stream).toList();
        return ret;
    }

    @Cached(cacheName = "transactionsBySeller", cacheType = FIFOCache.class)
    public List<Transaction> getTransactionsBySeller(String username) {
        System.out.println("Cache miss for seller!");
        var ret = repository.getTransactionBySellerid(username);
        return ret;
    }

    @Cached(cacheName = "transactionsById", cacheType = FIFOCache.class, capacity = 3)
    public Transaction getById(UUID id) {
        System.out.println("Cache miss!");

        var ret = repository.getReferenceById(id);
        return ret;
    }

    @Cached(cacheName = "getPendingTransactions", cacheType = FIFOCache.class)
    public List<Transaction> getPendingTransactions() {
        System.out.println("Cache miss!");

        return repository.getTransactionByCompleted(false);
    }

    @Cached(cacheName = "transactionByIds", allowSeparateHandlingForKeyCollections = true)
    public List<Transaction> getTransactionByIds(Set<UUID> transactionIds) {
        System.out.println("Cache miss!");
        return transactionIds.stream().map(repository::getById).toList();
    }

    //@UpdatesCache(name = "getPendingTransactionsBySeller", removeMode = RemoveMode.INVALIDATE_CACHE) //TODO: Test with condition
    @UpdatesCache(name = "getPendingTransactions", removeMode = RemoveMode.INVALIDATE_CACHE) //leaks. Create an extra object and you see the leak.
    @UpdatesCache(name = "transactionsBySeller",  removeMode = RemoveMode.INVALIDATE_CACHE) //same.
    @UpdatesCache(name = "transactionsById", removeMode = RemoveMode.INVALIDATE_CACHE)
    @UpdatesCache(name = "transactionByIds",  removeMode = RemoveMode.INVALIDATE_CACHE)
    public void invalidateAll(){
        System.out.println("invalidated.");
    }

    /*
        tested: ADD_VALUES_TO_COLLECTION for collection-cache without arguments and with condition
                ADD_VALUES_TO_COLLECTION for collection-cache with arguments
                DEFAULT adding for 1-1 cache with arguments
                DEFAULT adding for separate-handling cache
                REMOVE_VALUE_FROM_COLLECTION for collection-cache without arguments and with condition
                REMOVE_VALUE_FROM_ALL_COLLECTIONS for collection cache with arguments (re-test with multiple collections instead of 1-2?)
                DEFAULT removal for 1-1 cache with arguments
                REMOVE_VALUE_FROM_COLLECTION for collection-cache with arguments
                INVALIDATE_CACHE

                NOT TESTED:
                ADD_VALUES_TO_ALL_COLLECTIONS
                REPLACE_EXISTING_COLLECTION
                REMOVE_VALUE_FROM_ALL_COLLECTIONS (perhaps should not exist)


     */
    //@UpdatesCache(name = "getPendingTransactionsBySeller", conditionalAdd = "!completed", targetObjectKeys = "sellerid", addMode = AddMode.ADD_VALUES_TO_COLLECTION)
    @UpdatesCache(name = "getPendingTransactions", addOnCondition = "!completed", addMode = AddMode.ADD_VALUES_TO_COLLECTION)
    @UpdatesCache(name = "transactionsBySeller", targetObjectKeys = "sellerid", addMode = AddMode.ADD_VALUES_TO_COLLECTION)
    @UpdatesCache(name = "transactionsById", targetObjectKeys = "id", removeMode = RemoveMode.NONE, addMode = AddMode.DEFAULT)
    @UpdatesCache(name = "transactionByIds", targetObjectKeys = "id", addMode = AddMode.DEFAULT) //works
    public void addTransaction(@UpdatedValue Transaction transaction) {
        this.repository.save(transaction);
    }

    @UpdatesCache(name = "getPendingTransactions", removeOnCondition = "!completed", removeMode = RemoveMode.REMOVE_VALUE_FROM_COLLECTION)
    @UpdatesCache(name = "transactionsBySeller", targetObjectKeys = "sellerid", removeMode = RemoveMode.REMOVE_VALUE_FROM_COLLECTION)
    @UpdatesCache(name = "transactionByIds", targetObjectKeys = "id", removeMode = RemoveMode.REMOVE_VALUE_FROM_COLLECTION)
    @UpdatesCache(name = "transactionsById", targetObjectKeys = "id", removeMode = RemoveMode.DEFAULT, addMode = AddMode.NONE)
    public void deleteTransaction(@UpdatedValue Transaction transaction) {
        this.repository.deleteById(transaction.getId());
    }


}

