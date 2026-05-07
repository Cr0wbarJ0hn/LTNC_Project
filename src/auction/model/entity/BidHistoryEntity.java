package auction.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ✅ JPA Entity cho Bid History - Persist vào Database
 */
@Entity
@Table(name = "bid_history", indexes = {
        @Index(name = "idx_auction_id", columnList = "auction_id"),
        @Index(name = "idx_bidder_id", columnList = "bidder_id"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class BidHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auction_id", nullable = false)
    private String auctionId;

    @Column(name = "bidder_id", nullable = false)
    private String bidderId;

    @Column(name = "price", nullable = false)
    private double price;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "is_auto_bid")
    private boolean isAutoBid;

    // Constructors
    public BidHistoryEntity() {}

    public BidHistoryEntity(String auctionId, String bidderId, double price,
                            LocalDateTime timestamp, boolean isAutoBid) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.price = price;
        this.timestamp = timestamp;
        this.isAutoBid = isAutoBid;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    public String getBidderId() { return bidderId; }
    public void setBidderId(String bidderId) { this.bidderId = bidderId; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isAutoBid() { return isAutoBid; }
    public void setAutoBid(boolean autoBid) { isAutoBid = autoBid; }

    @Override
    public String toString() {
        return String.format("BidHistoryEntity{auction=%s, bidder=%s, price=%.2f, time=%s, auto=%b}",
                auctionId, bidderId, price, timestamp, isAutoBid);
    }
}