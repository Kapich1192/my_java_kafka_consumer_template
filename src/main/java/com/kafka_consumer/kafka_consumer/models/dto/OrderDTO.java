package com.kafka_consumer.kafka_consumer.models.dto;

import java.util.Objects;

public class OrderDTO {
    private String item;
    private Double amount;

    public OrderDTO() {

    }

    public OrderDTO(String item, Double amount) {
        this.item = item;
        this.amount = amount;
    }

    public String getItem() {
        return item;
    }

    public Double getAmount() {
        return amount;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "OrderDTO{" +
                "item='" + item + '\'' +
                ", amount=" + amount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderDTO orderDTO = (OrderDTO) o;
        return Objects.equals(item, orderDTO.item) && Objects.equals(amount, orderDTO.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, amount);
    }
}
