package com.gmalandrakis.mnemosyne.demo;

import com.gmalandrakis.mnemosyne.core.MnemoService;
import com.gmalandrakis.mnemosyne.demo.model.Transaction;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

@SpringBootTest(classes = {MnemosyneSpringConf.class})
@EnableAspectJAutoProxy()
@EnableAutoConfiguration
@ComponentScan("com.gmalandrakis")
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
public class TransactionTests {
    private static String DEFAULT_SELLER = "John";
    private static String DEFAULT_BUYER = "George";

    /*
     * !IMPORTANT!
     * The configuration below (the annotations @MockBean, the @Autowired etc)
     * are what I resorted to using after many trial and error attempts with other configurations,
     * and an adjustment of a ChatGPT generated suggestion (and believe me -I don't often resort to LLMs for solving my programming problems).
     * It works, but the @MockBean is deprecated.
     * <p>
     * It is more than important as TODO to come up with a better configuration.
     */
    @MockBean
    TransactionRepo repository;
    @MockBean
    CustomerRepo repositoryy;


    @Autowired
    TransactionService transactionServiceTest;
    @Autowired
    CustomerService customerService;

    List<Transaction> availableTransactions;

    @BeforeEach
    void contextLoads() throws Throwable {
        availableTransactions = new ArrayList<>();
    }

    @Test
    void generalTests() throws Throwable {
        availableTransactions.add(getPendingTransaction());
        when(repository.getTransactionByCompleted(false)).thenReturn(availableTransactions);
        when(repository.save(any())).thenReturn(getPendingTransaction());
        var result = transactionServiceTest.getPendingTransactions();
        assert (result.size() == 1);

        verify(repository, atLeast(1)).getTransactionByCompleted(anyBoolean());
        verify(repository, atMost(1)).getTransactionByCompleted(anyBoolean());

        //Add a new transaction:
        transactionServiceTest.saveTransaction(getPendingTransaction());
        var result2 = transactionServiceTest.getPendingTransactions();
        assert (result2.size() == result.size() + 1);
        verify(repository, atLeast(1)).getTransactionByCompleted(anyBoolean());
        verify(repository, atMost(1)).getTransactionByCompleted(anyBoolean());

        //Mark existing transaction as completed (thereby removing it from pendingTransactions):
        this.availableTransactions.get(0).setCompleted(true);
        transactionServiceTest.saveTransaction(this.availableTransactions.get(0));
        this.availableTransactions.remove(this.availableTransactions.get(0));
        var result3 = transactionServiceTest.getPendingTransactions();
        assert (result3.size() == result2.size() - 1);

        //Create a new transaction:
        var tr = getTransaction();
        transactionServiceTest.saveTransaction(tr);
        var r = transactionServiceTest.getById(tr.getId());
        verify(repository, atMost(0)).getReferenceById(any());
        assertEquals(r.getId(), tr.getId());
    }

    @Test
    void testPreemptiveFetchingAndUpdate() {
        /*
            The database contains two transactions of the seller A.
            Create a third one.
            getTransactionBySellerid should have been called even though getTransactionsBySellers() was not directly called by the user.
         */
        availableTransactions.add(getPendingTransaction());
        availableTransactions.add(getTransaction());
        when(repository.getTransactionBySellerid(any())).thenReturn(availableTransactions);

        var newTransaction = getPendingTransaction();
        transactionServiceTest.saveTransaction(newTransaction);

        verify(repository, atLeast(1)).getTransactionBySellerid(any()); //Fetched preemptively by mnemosyne!
        verify(repository, atMost(1)).getTransactionBySellerid(any());

        var result = transactionServiceTest.getTransactionsBySeller(DEFAULT_SELLER);
        verify(repository, atMost(1)).getTransactionBySellerid(any());  //Fetched from the cache -not the DB
        assertTrue(result.size() == 3);
        assertTrue(result.contains(newTransaction));
        var pending = transactionServiceTest.getPendingTransactions(); //should be present in other caches too
        assertTrue(pending.contains(newTransaction));

        //Should update the cache:
        newTransaction.setCompleted(true);
        transactionServiceTest.saveTransaction(newTransaction);

        result = transactionServiceTest.getTransactionsBySeller(DEFAULT_SELLER);

        assertTrue(result.size() == 3); //No new values
        assertTrue(result.stream().anyMatch(f -> {
            return f.getId() == newTransaction.getId() && newTransaction.isCompleted();
        }));
        verify(repository, atMost(1)).getTransactionBySellerid(any());  //Fetched from the cache -not the DB

        //TestDeletion:
        transactionServiceTest.deleteTransaction(newTransaction);
        result = transactionServiceTest.getTransactionsBySeller(DEFAULT_SELLER);
        assertTrue(result.size() == 2); //value removed
        verify(repository, atMost(1)).getTransactionBySellerid(any());  //Fetched from the cache -not the DB

        //verify update on other caches:
        pending = transactionServiceTest.getPendingTransactions();
        assertTrue(pending == null || !pending.contains(newTransaction));
    }

    @Test
    void testSeparateHandlingWithoutPreemptiveAddAndWithValuePool() {
        /*
            1. The DB has 1 transaction A
            2. Create 2 new transactions B,C and store them in the DB via the service.
            3. Call getTransactionByIds with the IDs of A,B,C.
            4. The repository should have been called only once, with the ID of A.
         */
        var tran1 = getTransaction();
        when(repository.getById(tran1.getId())).thenReturn(tran1);

        var tran2 = getTransaction();
        var tran3 = getPendingTransaction();
        transactionServiceTest.saveTransaction(tran2);
        transactionServiceTest.saveTransaction(tran3);


        var result = transactionServiceTest.getTransactionByIds(Set.of(tran1.getId(), tran2.getId(), tran3.getId()));
        verify(repository, times(1)).getById(tran1.getId());
        assertTrue(result.size() == 3);

        var tran4 = getPendingTransaction();
        transactionServiceTest.saveTransaction(tran4);

        transactionServiceTest.deleteTransaction(tran3);
        result = transactionServiceTest.getTransactionByIds(Set.of(tran1.getId(), tran2.getId(), tran3.getId()));
        verify(repository, times(2)).getById(any()); //In separate-collection caches, if one less than expected was found, mnemosyne queries the underlying method for the objects one-by-one
        assertTrue(result.size() == 2);
        result = transactionServiceTest.getTransactionByIds(Set.of(tran1.getId(), tran2.getId(), tran4.getId()));
        verify(repository, times(2)).getById(any());
        assertTrue(result.size() == 3);

        assertTrue(transactionServiceTest.getById(tran3.getId()) == null);

    }

    @Test
    void testSeparateHandlingWithoutPreemptiveAddAndWithUpdatesCache() {
        /*
            1. The DB has 1 transaction A
            2. Create 2 new transactions B,C and store them in the DB via the service.
            3. Call getTransactionByIds with the IDs of A,B,C.
            4. The repository should have been called only once, with the ID of A.
         */
        var tran1 = getTransaction();
        when(repository.getById(tran1.getId())).thenReturn(tran1);

        var tran2 = getTransaction();
        var tran3 = getPendingTransaction();
        transactionServiceTest.saveTransactionWithUpdatesCache(tran2);
        transactionServiceTest.saveTransactionWithUpdatesCache(tran3);


        var result = transactionServiceTest.getTransactionByIds(Set.of(tran1.getId(), tran2.getId(), tran3.getId()));
        verify(repository, times(1)).getById(tran1.getId());
        assertTrue(result.size() == 3);

        var tran4 = getPendingTransaction();
        transactionServiceTest.saveTransactionWithUpdatesCache(tran4);

        transactionServiceTest.deleteTransactionWithUpdatesCache(tran3);
        result = transactionServiceTest.getTransactionByIds(Set.of(tran1.getId(), tran2.getId(), tran3.getId()));
        verify(repository, times(2)).getById(any()); //In separate-collection caches, if one less than expected was found, mnemosyne queries the underlying method for the objects one-by-one
        assertTrue(result.size() == 2);
        result = transactionServiceTest.getTransactionByIds(Set.of(tran1.getId(), tran2.getId(), tran4.getId()));
        verify(repository, times(2)).getById(any());
        assertTrue(result.size() == 3);

        assertTrue(transactionServiceTest.getById(tran3.getId()) == null);

    }


    @Test
    void testFIFOFlow() {
        var tran1 = getTransaction();
        when(repository.getById(tran1.getId())).thenReturn(tran1);

        var tran2 = getTransaction();
        var tran3 = getPendingTransaction();
        transactionServiceTest.saveTransactionWithUpdatesCache(tran1);
        transactionServiceTest.saveTransactionWithUpdatesCache(tran2);
        transactionServiceTest.saveTransactionWithUpdatesCache(tran3);
        verify(repository, times(0)).getById(any());

        var result = transactionServiceTest.getById(tran1.getId());
        assert (result != null);
        verify(repository, times(0)).getById(tran1.getId());
        result = transactionServiceTest.getById(tran2.getId());
        assert (result != null);
        verify(repository, times(0)).getById(tran2.getId());
        result = transactionServiceTest.getById(tran3.getId());
        assert (result != null);
        verify(repository, times(0)).getById(tran3.getId());

        //TIME TO FIFO!!!
        when(repository.getById(tran1.getId())).thenReturn(tran1);

        var tran4 = getPendingTransaction();
        transactionServiceTest.saveTransactionWithUpdatesCache(tran4);
        result = transactionServiceTest.getById(tran1.getId());
        assert (result != null); //TODO: Allakse to capacity se 5 kai ksanatrexto.
        verify(repository, times(1)).getById(tran1.getId());


    }



    /*
TODO    test compound key getTransactionsBySellerAndCompletion
     */

    @Test
    void deleteFromDifferentCaches_shouldOnlyAffectOne() {

    /*
       1. Add a transaction to caches A and B. Remove from B. Should still be in A.
     */
        var tran1 = getPendingTransaction();
        var tran2 = getPendingTransaction();
        transactionServiceTest.saveTransaction(tran1);
        transactionServiceTest.saveTransaction(tran2);

        var result = transactionServiceTest.getPendingTransactions();
        assert (result.size() == 2);

        verify(repository, times(1)).getTransactionByCompleted(anyBoolean()); //preemptive fetching.

        tran2.setCompleted(true);
        transactionServiceTest.saveTransaction(tran2);

        result = transactionServiceTest.getPendingTransactions();
        assertTrue(result.size() == 1);
        assertTrue(!result.contains(tran2));
        var single = transactionServiceTest.getById(tran2.getId());
        assertTrue(single != null);
        verify(repository, times(0)).getReferenceById(tran2.getId()); //fetched from cache, and not the repository
    }


    private Transaction getPendingTransaction() {
        Transaction t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setCompleted(false);
        t.setBuyerid(DEFAULT_BUYER);
        t.setSellerid(DEFAULT_SELLER);
        t.setAmount(10.0); //todo: randomize
        return t;
    }

    private Transaction getTransaction() {
        Transaction t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setCompleted(true);
        t.setBuyerid(DEFAULT_BUYER);
        t.setSellerid(DEFAULT_SELLER);
        t.setAmount(10.0); //todo: randomize
        return t;
    }

}
