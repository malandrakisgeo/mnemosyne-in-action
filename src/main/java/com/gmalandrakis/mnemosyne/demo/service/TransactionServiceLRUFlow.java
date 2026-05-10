package com.gmalandrakis.mnemosyne.demo.service;

import com.gmalandrakis.mnemosyne.annotations.*;
import com.gmalandrakis.mnemosyne.cache.LRUCache;
import com.gmalandrakis.mnemosyne.demo.model.Transaction;
import com.gmalandrakis.mnemosyne.demo.repository.TransactionRepo;
import com.gmalandrakis.mnemosyne.structures.AddMode;
import com.gmalandrakis.mnemosyne.structures.RemoveMode;
import org.hibernate.annotations.Proxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

//@Service
@Proxy(lazy = false)
public class TransactionServiceLRUFlow {

    private final TransactionRepo repository;

    @Autowired
    public TransactionServiceLRUFlow(TransactionRepo repository) {
        this.repository = repository;
    }

   /* @Cached(cacheName = "transactionsBySellers", allowSeparateHandlingForKeyCollections = false,
            addMode = AddMode.ADD_TO_COLLECTION, removeMode = RemoveMode.REMOVE_FROM_ALL_COLLECTIONS) //TODO: ADD TO ALL COLLECTIONS?
    public List<Transaction> getTransactionsBySellers(Set<String> username) {
        System.out.println("Cache miss for Ssellers!");
        var ret = username.stream().map(repository::getTransactionBySellerid).flatMap(List::stream).toList();
        return ret;
    }*/

    @Cached(cacheName = "transactionsBySellerLRU", targetObjectKeys = "sellerid", cacheType = LRUCache.class,
            addMode = AddMode.ADD_TO_COLLECTION, removeMode = RemoveMode.REMOVE_FROM_COLLECTION)
    public List<Transaction> getTransactionsBySeller(String username) {
        System.out.println("Cache miss for seller!");
        var ret = repository.getTransactionBySellerid(username);
        return ret;
    }

    @Cached(cacheName = "transactionsByIdLRU", capacity = 300,  cacheType = LRUCache.class,
            targetObjectKeys = "id", addMode = AddMode.SINGLE_VALUE, removeMode = RemoveMode.SINGLE_VALUE)
    public Transaction getById(UUID id) {
        System.out.println("Cache miss!");

        var ret = repository.getById(id);
        return ret;
    }

    @Cached(cacheName = "getPendingTransactionsLRU",  cacheType = LRUCache.class,
            addOnCondition = "!completed", addMode = AddMode.ADD_TO_COLLECTION,
            removeOnCondition = "completed", removeMode = RemoveMode.REMOVE_FROM_ALL_COLLECTIONS)
    public List<Transaction> getPendingTransactions() {
        System.out.println("Cache miss!");

        return repository.getTransactionByCompleted(false);
    }

    @Cached(cacheName = "transactionByIdsLRU", cacheType = LRUCache.class,
            allowSeparateHandlingForKeyCollections = true,
            targetObjectKeys = "id", addMode = AddMode.ADD_TO_COLLECTION, removeMode = RemoveMode.REMOVE_FROM_ALL_COLLECTIONS)
    public List<Transaction> getTransactionByIds(Set<UUID> transactionIds) {
        System.out.println("Cache miss!");
        return transactionIds.stream().map(repository::getById).toList();
    }

    @Cached(cacheName = "completedTransactionCacheLRU",  cacheType = LRUCache.class,capacity = 1000, addMode = AddMode.ADD_TO_COLLECTION, removeMode = RemoveMode.REMOVE_FROM_COLLECTION)
    public List<Transaction> getTransactionsBySellerAndCompletion(@Key String sellerId, boolean completed) {
        System.out.println("Cache miss!");

        return repository.getTransactionBySellerAndCompleted(sellerId, false);
    }

    //@UpdatesCache(name = "getPendingTransactionsBySeller", removeMode = RemoveMode.INVALIDATE_CACHE) //TODO: Test with condition
    @UpdatesCache(name = "getPendingTransactionsLRU", removeMode = RemoveMode.INVALIDATE_CACHE)
    //leaks. Create an extra object and you see the leak.
    @UpdatesCache(name = "transactionsBySellerLRU", removeMode = RemoveMode.INVALIDATE_CACHE) //same.
    @UpdatesCache(name = "transactionsByIdLRU", removeMode = RemoveMode.INVALIDATE_CACHE)
    @UpdatesCache(name = "transactionByIdsLRU", removeMode = RemoveMode.INVALIDATE_CACHE)
    public void invalidateAll() {
        System.out.println("invalidated.");
    }


    @UpdatesValuePool(addIfAbsent = true) //TODO: an einai false kai to remove einai true se allo pool?
    public void saveTransaction(@UpdatedValue Transaction transaction) {
        this.repository.save(transaction);
    }

    // @UpdatesCache(name = "getPendingTransactions", removeMode = RemoveMode.REMOVE_VALUE_FROM_COLLECTION) //TODO: REMOVE_VALUE_FROM_ALL_COLLECTIONS should also do the trick here.
    // @UpdatesCache(name = "transactionsBySeller", targetObjectKeys = "sellerid", removeMode = RemoveMode.REMOVE_VALUE_FROM_COLLECTION)
    //@UpdatesCache(name = "transactionByIds", targetObjectKeys = "id", removeMode = RemoveMode.REMOVE_VALUE_FROM_COLLECTION)
    //@UpdatesCache(name = "transactionsById", targetObjectKeys = "id", removeMode = RemoveMode.DEFAULT, addMode = AddMode.NONE)
    @UpdatesValuePool(remove = true)
    public void deleteTransaction(@UpdatedValue Transaction transaction) {
        this.repository.deleteById(transaction.getId());
    }

    @UpdatesCache(name = "getPendingTransactionsLRU",
            addOnCondition = "!completed", addMode = AddMode.ADD_TO_COLLECTION, removeOnCondition = "completed",
            removeMode = RemoveMode.REMOVE_FROM_COLLECTION)
    @UpdatesCache(name = "transactionsBySellerLRU", targetObjectKeys = "sellerid")
    @UpdatesCache(name = "transactionsByIdLRU", targetObjectKeys = "id")
    @UpdatesCache(name = "transactionByIdsLRU", targetObjectKeys = "id")
    public void saveTransactionWithUpdatesCache(@UpdatedValue Transaction transaction) {
        this.repository.save(transaction);
    }

    @UpdatesCache(name = "getPendingTransactionsLRU", addMode = AddMode.NONE,
            removeMode = RemoveMode.REMOVE_FROM_COLLECTION)
    //TODO: REMOVE_VALUE_FROM_ALL_COLLECTIONS should also do the trick here.
    @UpdatesCache(name = "transactionsBySellerLRU", targetObjectKeys = "sellerid", removeMode = RemoveMode.REMOVE_FROM_ALL_COLLECTIONS) //TODO: Important note: These have to be specified whenever a function removes a value instead of adding or updating it, otherwise discrepancies will occur
    @UpdatesCache(name = "transactionByIdsLRU", targetObjectKeys = "id", removeMode = RemoveMode.REMOVE_FROM_ALL_COLLECTIONS)
    @UpdatesCache(name = "transactionsByIdLRU", targetObjectKeys = "id", removeMode = RemoveMode.SINGLE_VALUE, addMode = AddMode.NONE)
    public void deleteTransactionWithUpdatesCache(@UpdatedValue Transaction transaction) {
        this.repository.deleteById(transaction.getId());
    }


}

