package com.gmalandrakis.mnemosyne.demo.service;

import com.gmalandrakis.mnemosyne.annotations.*;
import com.gmalandrakis.mnemosyne.annotations.UpdatesCache.RemoveMode;

import com.gmalandrakis.mnemosyne.demo.model.Customer;
import com.gmalandrakis.mnemosyne.demo.repository.CustomerRepo;
import org.hibernate.annotations.Proxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

//@Service
//@Proxy(lazy = false)
public class CustomerService {
    private final CustomerRepo repository;

    //  private final RedisTemplate<String, String> redisTemplate;


    @Autowired
    public CustomerService(CustomerRepo repository) {
        this.repository = repository;
        //  this.redisTemplate = redisTemplate;
    }

    //@/UpdatesCache(name = "multiCustomerCacheSeparate", removeMode = RemoveMode.REMOVE_VALUE_FROM_ALL_COLLECTIONS)
    //@UpdatesCache(name = "customerCache", targetObjectKeys = "id", removeMode = RemoveMode.REMOVE_KEY)
   // @Cached(cacheName="moofes")
    public void deleteMe(@UpdatedValue Customer id, @UpdateKey(keyId = "KEY") List<Integer> ids) {
//bug 1: not recognized without @Cached (FIXED)
        //bug2: after deleting, it does not re-add for some reason!
    }


    @Cached(cacheName = "multiCustomerCache", capacity = 500, timeToLive = 150 * 1000, countdownFromCreation = true)
    public List<Customer> getCustomerByIdTest(@Key List<Integer> ids, String ignore) {
        System.out.println("method called");

        var list = new ArrayList<Customer>();
        ids.forEach(id -> {
            list.add(this.getCustomerByIdTest(String.valueOf(id)));
        });

        return list;
    }


    @Cached(cacheName = "multiCustomerCacheSeparate", capacity = 500, timeToLive = 3000 * 1000, countdownFromCreation = true, allowSeparateHandlingForKeyCollections = true)
   // @UpdatesCache(name = "multiCustomerCache", annotatedKeys = "ids")
    public List<Customer> getCustomerByIdTestSpecial(@UpdateKey(keyId = "ids") List<Integer> ids) {
        System.out.println("called by a thread");
        var list = new ArrayList<Customer>();
        ids.forEach(id -> {
            list.add(this.getCustomerByIdTest(String.valueOf(id)));
        });

        return list;
    }


    @Cached(cacheName = "customerCache", capacity = 500, timeToLive = 3000 * 1000, countdownFromCreation = true)
    public Customer getCustomerByIdTest(String id) {
        System.out.println("method called");

        var c = repository.getById(id);
        if (c != null) {
            return c;
        }
        return new Customer();
    }

    public List<Customer> getCustomerByIdRowToUncached(Integer maxId) {
        // int r = Integer.parseInt(maxId);
        var lst = new ArrayList<Customer>();
        for (int i = 1; i <= maxId; i++) {
            lst.add(repository.getById(String.valueOf(i)));
        }

        return lst;
    }


    @Cached(cacheName = "customersCache", capacity = 500, timeToLive = 3000 * 1000, countdownFromCreation = true)
    public List<Customer> getCustomerByIdRowTo(Integer maxId) {
        System.out.println("method called");

        // int r = Integer.parseInt(maxId);
        var lst = new ArrayList<Customer>();
        for (int i = 1; i <= maxId; i++) {
            lst.add(repository.getById(String.valueOf(i)));
        }

        return lst;
    }


}
