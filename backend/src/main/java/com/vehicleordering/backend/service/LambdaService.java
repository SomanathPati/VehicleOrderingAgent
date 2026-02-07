package com.vehicleordering.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class LambdaService {

    private static final Logger logger = LoggerFactory.getLogger(LambdaService.class);

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;
    private final String paymentFunctionArn;
    private final String emailFunctionArn;

    public LambdaService(LambdaClient lambdaClient,
                        ObjectMapper objectMapper,
                        @Value("${aws.lambda.payment-function}") String paymentFunctionArn,
                        @Value("${aws.lambda.email-function}") String emailFunctionArn) {
        this.lambdaClient = lambdaClient;
        this.objectMapper = objectMapper;
        this.paymentFunctionArn = paymentFunctionArn;
        this.emailFunctionArn = emailFunctionArn;
    }

    @Async
    public void triggerPaymentProcessing(com.vehicleordering.backend.entity.Order order) {
        logger.info("Triggering payment processing Lambda for order: {}", order.getOrderId());

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderId", order.getOrderId());
            payload.put("customerEmail", order.getEmail());
            payload.put("amount", calculateOrderAmount(order)); // Simplified calculation
            payload.put("currency", "USD");

            invokeLambda(paymentFunctionArn, payload);

            logger.info("Payment processing Lambda triggered successfully for order: {}", order.getOrderId());

        } catch (Exception e) {
            logger.error("Error triggering payment processing Lambda for order {}: {}",
                        order.getOrderId(), e.getMessage(), e);
            // Don't throw exception as payment processing is asynchronous
        }
    }

    @Async
    public void triggerEmailNotification(com.vehicleordering.backend.entity.Order order) {
        logger.info("Triggering email notification Lambda for order: {}", order.getOrderId());

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderId", order.getOrderId());
            payload.put("customerEmail", order.getEmail());
            payload.put("customerName", order.getCustomerName());
            payload.put("pdfUrl", order.getPdfUrl());
            payload.put("orderDetails", createOrderDetailsMap(order));

            invokeLambda(emailFunctionArn, payload);

            logger.info("Email notification Lambda triggered successfully for order: {}", order.getOrderId());

        } catch (Exception e) {
            logger.error("Error triggering email notification Lambda for order {}: {}",
                        order.getOrderId(), e.getMessage(), e);
            // Don't throw exception as email notification is asynchronous
        }
    }

    private void invokeLambda(String functionArn, Map<String, Object> payload) throws Exception {
        String payloadJson = objectMapper.writeValueAsString(payload);

        InvokeRequest invokeRequest = InvokeRequest.builder()
                .functionName(functionArn)
                .payload(StandardCharsets.UTF_8.encode(payloadJson))
                .invocationType("Event") // Asynchronous invocation
                .build();

        InvokeResponse invokeResponse = lambdaClient.invoke(invokeRequest);

        if (invokeResponse.statusCode() != 202) { // 202 = Accepted for async invocations
            logger.warn("Lambda invocation returned status code: {}", invokeResponse.statusCode());
        }

        logger.debug("Lambda function invoked successfully: {}", functionArn);
    }

    private Map<String, Object> createOrderDetailsMap(com.vehicleordering.backend.entity.Order order) {
        Map<String, Object> details = new HashMap<>();
        details.put("model", order.getModel());
        details.put("color", order.getColor());
        details.put("wheels", order.getWheels());
        details.put("features", order.getFeatures());
        details.put("specialRequests", order.getSpecialRequests());
        details.put("orderDate", order.getCreatedAt().toString());
        return details;
    }

    // Simplified order amount calculation - in real implementation, this would be more complex
    private double calculateOrderAmount(com.vehicleordering.backend.entity.Order order) {
        double basePrice = 25000.0; // Base price for any vehicle

        // Add feature costs
        double featureCost = 0.0;
        if (order.getFeatures() != null) {
            featureCost = order.getFeatures().size() * 1500.0; // $1500 per feature
        }

        // Add premium wheels cost
        double wheelsCost = 0.0;
        if ("Premium Alloy".equals(order.getWheels()) || "Chrome".equals(order.getWheels())) {
            wheelsCost = 2000.0;
        } else if ("Carbon Fiber".equals(order.getWheels())) {
            wheelsCost = 5000.0;
        }

        return basePrice + featureCost + wheelsCost;
    }
}
