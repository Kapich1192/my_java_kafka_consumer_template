package com.kafka_consumer.kafka_consumer.models.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String items;
    private Double amount;

    public Order(){

    }

    public Order(Long id, String items, Double amount) {
        this.id = id;
        this.items = items;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public String getItems() {
        return items;
    }

    public Double getAmount() {
        return amount;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setItems(String items) {
        this.items = items;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
