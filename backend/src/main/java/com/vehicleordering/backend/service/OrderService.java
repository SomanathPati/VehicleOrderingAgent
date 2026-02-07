package com.vehicleordering.backend.service;

import com.vehicleordering.backend.entity.Order;
import com.vehicleordering.backend.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final MeterRegistry meterRegistry;

    // Metrics
    private final Counter orderCreatedCounter;
    private final Counter orderProcessedCounter;
    private final Counter orderErrorCounter;
    private final Timer orderProcessingTimer;

    @Autowired
    public OrderService(OrderRepository orderRepository, MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.meterRegistry = meterRegistry;

        // Initialize metrics
        this.orderCreatedCounter = Counter.builder("orders.created")
                .description("Number of orders created")
                .register(meterRegistry);

        this.orderProcessedCounter = Counter.builder("orders.processed")
                .description("Number of orders processed")
                .register(meterRegistry);

        this.orderErrorCounter = Counter.builder("orders.errors")
                .description("Number of order processing errors")
                .register(meterRegistry);

        this.orderProcessingTimer = Timer.builder("orders.processing.duration")
                .description("Time taken to process orders")
                .register(meterRegistry);
    }

    public Order createOrder(Order order) {
        logger.info("Creating new order for customer: {}", order.getCustomerName());

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Order savedOrder = orderRepository.save(order);
            orderCreatedCounter.increment();

            logger.info("Order created successfully with ID: {}", savedOrder.getOrderId());
            return savedOrder;

        } catch (Exception e) {
            orderErrorCounter.increment();
            logger.error("Error creating order: {}", e.getMessage(), e);
            throw e;
        } finally {
            sample.stop(orderProcessingTimer);
        }
    }

    public Optional<Order> getOrderById(String orderId) {
        return orderRepository.findByOrderId(orderId);
    }

    public List<Order> getOrdersByEmail(String email) {
        return orderRepository.findByEmail(email);
    }

    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public Order updateOrderStatus(String orderId, Order.OrderStatus status) {
        logger.info("Updating order {} status to {}", orderId, status);

        Optional<Order> orderOptional = orderRepository.findByOrderId(orderId);
        if (orderOptional.isPresent()) {
            Order order = orderOptional.get();
            order.setStatus(status);
            Order updatedOrder = orderRepository.save(order);

            if (status == Order.OrderStatus.COMPLETED) {
                orderProcessedCounter.increment();
            }

            logger.info("Order {} status updated to {}", orderId, status);
            return updatedOrder;
        } else {
            logger.error("Order not found: {}", orderId);
            throw new RuntimeException("Order not found: " + orderId);
        }
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public void deleteOrder(String orderId) {
        logger.info("Deleting order: {}", orderId);

        Optional<Order> orderOptional = orderRepository.findByOrderId(orderId);
        if (orderOptional.isPresent()) {
            orderRepository.delete(orderOptional.get());
            logger.info("Order deleted: {}", orderId);
        } else {
            logger.error("Order not found for deletion: {}", orderId);
            throw new RuntimeException("Order not found: " + orderId);
        }
    }
}
