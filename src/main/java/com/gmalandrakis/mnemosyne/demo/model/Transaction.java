package com.gmalandrakis.mnemosyne.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Proxy;

import java.util.UUID;

@Data
@Getter
@Setter
@Entity
@Table(name = "transaction")
@Proxy(lazy = false)
public class Transaction {

    @Id
    private UUID id;

    @Column(name = "iscompleted")
    private boolean completed;
    private Double amount;
    private String buyerid;
    private String sellerid;

}
