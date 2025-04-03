package com.gmalandrakis.mnemosyne.demo.repository;

import com.gmalandrakis.mnemosyne.demo.model.Customer;
import org.hibernate.annotations.Proxy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
@Proxy(lazy=false)
public interface CustomerRepo extends JpaRepository<Customer, String> {

    Customer getById(String Id);
}
