package com.gmalandrakis.mnemosyne.demo;

import com.gmalandrakis.mnemosyne.demo.model.Customer;
import com.gmalandrakis.mnemosyne.demo.repository.CustomerRepo;
import com.gmalandrakis.mnemosyne.demo.repository.TransactionRepo;
import com.gmalandrakis.mnemosyne.demo.service.CustomerService;
import com.gmalandrakis.mnemosyne.demo.service.TransactionService;
import com.gmalandrakis.mnemosyne.spring.MnemosyneSpringConf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

@SpringBootTest(classes = {MnemosyneSpringConf.class})
@EnableAspectJAutoProxy()
@EnableAutoConfiguration
@ComponentScan("com.gmalandrakis")
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
public class CustomerTests {

    @MockBean
    CustomerRepo repository;
    @MockBean
    TransactionRepo transactionRepo;

    @Autowired
    CustomerService customerService;
    @Autowired
    TransactionService transactionServiceTest;

    List<Customer> availableCustomers;

    @BeforeEach
    void contextLoads() throws Throwable {
        availableCustomers = new ArrayList<>();
    }

    @Test
    void testSaving(){
        var cust1 = getCustomer(false, false);
        var cust2 = getCustomer(true, true);
        customerService.saveActiveUserDetails(cust1);
        customerService.saveActiveUserDetails(cust2);
        var r1 = customerService.getCustomerByIdTest(cust1.getId());
        var r2 = customerService.getCustomerByIdTest(cust2.getId());
        assert (r1 != null);
        assert (r2 != null);
        verify(repository, times(0)).getById(any());

        var r3 = customerService.getActiveUsers();
        assert(r3 != null && !r3.isEmpty() && r3.contains(cust2));
        cust2.setActive(false);
       // cust2.setVerified(false);
        customerService.saveActiveUserDetails(cust2);
        r3 = customerService.getActiveUsers();
        assert(r3 == null || r3.isEmpty());
        //TODO: eprepe na isxuei to prohgoumeno kai xwris to verified = false


    }


    private Customer getCustomer(boolean active, boolean verified) {
        var cust = new Customer();
        cust.setId(String.valueOf(UUID.randomUUID()));
        cust.setActive(active);
        cust.setVerified(verified);
        cust.setName("Test");
        cust.setSurname("Testopoulos");
        cust.setEmail("test@test.se");
        return cust;
    }


}
