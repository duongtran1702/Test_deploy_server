package com.production.order.repository;

import com.production.order.model.OrderEntity;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class OrderRepository {

    private final ConcurrentMap<String, OrderEntity> orders = new ConcurrentHashMap<>();

    public OrderEntity save(OrderEntity order) {
        orders.put(order.getId(), order);
        return order;
    }

    public Optional<OrderEntity> findById(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    public List<OrderEntity> findAll() {
        List<OrderEntity> list = new ArrayList<>(orders.values());
        list.sort(Comparator.comparing(OrderEntity::getCreatedAt).reversed());
        return list;
    }

    public long count() {
        return orders.size();
    }

    public void deleteById(String id) {
        orders.remove(id);
    }

    public void clear() {
        orders.clear();
    }
}
