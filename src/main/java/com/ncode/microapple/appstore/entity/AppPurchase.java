package com.ncode.microapple.appstore.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "app_purchases")
public class AppPurchase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "app_id", nullable = false)
    private String appId;
    
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(name = "purchase_date", nullable = false)
    private LocalDateTime purchaseDate;
    
    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;

    // Default constructor for JPA
    protected AppPurchase() {}
    
    public AppPurchase(String userId, String appId, BigDecimal price, String transactionId) {
        this.userId = userId;
        this.appId = appId;
        this.price = price;
        this.transactionId = transactionId;
        this.purchaseDate = LocalDateTime.now();
    }
    
    // Getters
    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getAppId() { return appId; }
    public BigDecimal getPrice() { return price; }
    public LocalDateTime getPurchaseDate() { return purchaseDate; }
    public String getTransactionId() { return transactionId; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppPurchase that = (AppPurchase) o;
        return Objects.equals(transactionId, that.transactionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }
    
    @Override
    public String toString() {
        return "AppPurchase{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", appId='" + appId + '\'' +
                ", price=" + price +
                ", purchaseDate=" + purchaseDate +
                ", transactionId='" + transactionId + '\'' +
                '}';
    }
}