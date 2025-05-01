package com.nimbleways.springboilerplate.services.implementations;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import com.nimbleways.springboilerplate.dto.product.ProcessOrderResponse;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import org.springframework.web.bind.annotation.PathVariable;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {


    private final ProductRepository pr;
    private final NotificationService ns;
    private final OrderRepository or;


    public void notifyDelay(int leadTime, Product p) {
        p.setLeadTime(leadTime);
        pr.save(p);
        ns.sendDelayNotification(leadTime, p.getName());
    }

    private void handleSeasonalProduct(Product p) {
        if (LocalDate.now().plusDays(p.getLeadTime()).isAfter(p.getSeasonEndDate())) {
            ns.sendOutOfStockNotification(p.getName());
            p.setAvailable(0);
            pr.save(p);
        } else if (p.getSeasonStartDate().isAfter(LocalDate.now())) {
            ns.sendOutOfStockNotification(p.getName());
            pr.save(p);
        } else {
            notifyDelay(p.getLeadTime(), p);
        }
    }

    private void handleExpiredProduct(Product p) {
        if (p.getAvailable() > 0 && p.getExpiryDate().isAfter(LocalDate.now())) {
            p.setAvailable(p.getAvailable() - 1);
            pr.save(p);
        } else {
            ns.sendExpirationNotification(p.getName(), p.getExpiryDate());
            p.setAvailable(0);
            pr.save(p);
        }
    }

    private void processNormalProduct(Product p) {
        if (p.getAvailable() > 0) {
            p.setAvailable(p.getAvailable() - 1);
            pr.save(p);
        } else if (p.getLeadTime() > 0) {
            notifyDelay(p.getLeadTime(), p);
        }
    }

    private void processSeasonalProduct(Product p) {
        LocalDate now = LocalDate.now();
        if (now.isAfter(p.getSeasonStartDate()) &&
                now.isBefore(p.getSeasonEndDate()) &&
                p.getAvailable() > 0) {
            p.setAvailable(p.getAvailable() - 1);
            pr.save(p);
        } else {
            handleSeasonalProduct(p);
        }
    }

    private void processExpirableProduct(Product p) {
        if (p.getAvailable() > 0 && p.getExpiryDate().isAfter(LocalDate.now())) {
            p.setAvailable(p.getAvailable() - 1);
            pr.save(p);
        } else {
            handleExpiredProduct(p);
        }
    }

    public ProcessOrderResponse processOrder(Long orderId) {

        log.info("start service Processing order orderId {}", orderId);

        Optional<Order> orderOptional = or.findById(orderId);
        if (orderOptional.isPresent()) {
            Order order = orderOptional.get();
            Set<Product> products = order.getItems();
            for (Product p : products) {
                switch (p.getType()) {
                    case "NORMAL" -> processNormalProduct(p);
                    case "SEASONAL" -> processSeasonalProduct(p);
                    case "EXPIRABLE" -> processExpirableProduct(p);
                    default -> throw new IllegalArgumentException("Unknown product type: " + p.getType());
                }
            }
        }
        log.info("end service Processing order orderId {}", orderId);

        return new ProcessOrderResponse(orderId);

    }
}