package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@UnitTest
public class MyUnitTests {

    @Mock
    private NotificationService notificationService;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderRepository orderRepository;
    @InjectMocks
    private ProductService productService;

    private Product normalProduct;
    private Product seasonalProduct;
    private Product expirableProduct;
    private Order order;

    @BeforeEach
    void setUp() {
        normalProduct = new Product();
        normalProduct.setType("NORMAL");
        normalProduct.setAvailable(5);
        normalProduct.setLeadTime(3);
        normalProduct.setName("NormalProduct");

        seasonalProduct = new Product();
        seasonalProduct.setType("SEASONAL");
        seasonalProduct.setAvailable(5);
        seasonalProduct.setSeasonStartDate(LocalDate.now().minusDays(1));
        seasonalProduct.setSeasonEndDate(LocalDate.now().plusDays(5));
        seasonalProduct.setLeadTime(3);
        seasonalProduct.setName("SeasonalProduct");

        expirableProduct = new Product();
        expirableProduct.setType("EXPIRABLE");
        expirableProduct.setAvailable(5);
        expirableProduct.setExpiryDate(LocalDate.now().plusDays(5));
        expirableProduct.setName("ExpirableProduct");

        order = new Order();
        order.setId(1L);
        order.setItems(Set.of(normalProduct, seasonalProduct, expirableProduct));
    }



    @Test
    void testNotifyDelay() {
        productService.notifyDelay(5, normalProduct);

        verify(productRepository).save(normalProduct);
        verify(notificationService).sendDelayNotification(5, "NormalProduct");
    }

    @Test
    void testHandleSeasonalProduct_endOfSeason() {
        seasonalProduct.setSeasonEndDate(LocalDate.now().plusDays(2));
        seasonalProduct.setLeadTime(5);

        // invoke private method via reflection (si besoin)
        ReflectionTestUtils.invokeMethod(productService, "handleSeasonalProduct", seasonalProduct);

        verify(notificationService).sendOutOfStockNotification(eq("SeasonalProduct"));
        verify(productRepository).save(seasonalProduct);
    }

    @Test
    void testHandleExpiredProduct_whenExpired() {
        expirableProduct.setAvailable(1);
        expirableProduct.setExpiryDate(LocalDate.now().minusDays(1));

        ReflectionTestUtils.invokeMethod(productService, "handleExpiredProduct", expirableProduct);

        verify(notificationService).sendExpirationNotification(eq("ExpirableProduct"), any(LocalDate.class));
        verify(productRepository).save(expirableProduct);
    }

    @Test
    public void test() {
        // GIVEN
        Product product =new Product(null, 15, 0, "NORMAL", "RJ45 Cable", null, null, null);

        Mockito.when(productRepository.save(product)).thenReturn(product);

        // WHEN
        productService.notifyDelay(product.getLeadTime(), product);

        // THEN
        assertEquals(0, product.getAvailable());
        assertEquals(15, product.getLeadTime());
        verify(productRepository, Mockito.times(1)).save(product);
        verify(notificationService, Mockito.times(1)).sendDelayNotification(product.getLeadTime(), product.getName());
    }
}

