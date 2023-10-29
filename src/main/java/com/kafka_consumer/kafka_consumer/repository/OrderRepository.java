package com.kafka_consumer.kafka_consumer.repository;

import com.kafka_consumer.kafka_consumer.models.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
