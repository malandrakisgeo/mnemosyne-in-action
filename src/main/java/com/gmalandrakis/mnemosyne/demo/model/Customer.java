package com.gmalandrakis.mnemosyne.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Proxy;

@Data
@Getter
@Setter
@Entity
@Table(name = "customer")
@Proxy(lazy=false)
public class Customer {
    @Id
    private String id;

    private String name;
    private String surname;
    private String email;

    @Override
    public String toString() {
        return "{" +
                " id='" + id + "'" +
                ", name='" + name + "'" +
                ", surname='" + surname + "'" +
                ", email='" + email + "'" +
                "}";
    }

    public Customer() {
    }

    public Customer(String id, String name, String surname, String email) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.email = email;
    }
}
