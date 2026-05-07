package auction.controller;

import auction.model.entity.BidHistoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import auction.repository.BidHistoryRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**ZS
 * ✅ REST API Controller - Provide endpoints for Bid History
 */
@RestController
@RequestMapping("/api/auctions")
@CrossOrigin(origins = "*")
public class AuctionController {

    @Autowired
    private BidHistoryRepository bidHistoryRepository;

    /**
     * GET /api/auctions/{auctionId}/bids - Lịch sử bid
     */
    @GetMapping("/{auctionId}/bids")
    public ResponseEntity<List<BidHistoryEntity>> getBidHistory(@PathVariable String auctionId) {
        List<BidHistoryEntity> bids = bidHistoryRepository.findByAuctionId(auctionId);
        return ResponseEntity.ok(bids);
    }

    /**
     * GET /api/auctions/{auctionId}/stats - Thống kê giá
     */
    @GetMapping("/{auctionId}/stats")
    public ResponseEntity<Map<String, Object>> getAuctionStats(@PathVariable String auctionId) {
        Map<String, Object> stats = new HashMap<>();

        List<BidHistoryEntity> bids = bidHistoryRepository.findByAuctionId(auctionId);
        Double maxPrice = bidHistoryRepository.findMaxPriceByAuctionId(auctionId);
        long totalBids = bidHistoryRepository.countByAuctionId(auctionId);
        long autoBids = bidHistoryRepository.countByAuctionIdAndIsAutoBid(auctionId, true);

        stats.put("totalBids", totalBids);
        stats.put("autoBids", autoBids);
        stats.put("maxPrice", maxPrice != null ? maxPrice : 0.0);
        stats.put("bidHistory", bids);

        if (!bids.isEmpty()) {
            stats.put("avgPrice", bids.stream()
                    .mapToDouble(BidHistoryEntity::getPrice)
                    .average()
                    .orElse(0.0));
            stats.put("minPrice", bids.stream()
                    .mapToDouble(BidHistoryEntity::getPrice)
                    .min()
                    .orElse(0.0));
        }

        return ResponseEntity.ok(stats);
    }

    /**
     * POST /api/auctions/{auctionId}/bids - Thêm bid
     */
    @PostMapping("/{auctionId}/bids")
    public ResponseEntity<BidHistoryEntity> addBid(
            @PathVariable String auctionId,
            @RequestBody BidRequest bidRequest) {

        BidHistoryEntity bid = new BidHistoryEntity(
                auctionId,
                bidRequest.getBidderId(),
                bidRequest.getPrice(),
                LocalDateTime.now(),
                bidRequest.isAutoBid()
        );

        BidHistoryEntity saved = bidHistoryRepository.save(bid);
        return ResponseEntity.ok(saved);
    }

    /**
     * DTO for Bid Request
     */
    public static class BidRequest {
        private String bidderId;
        private double price;
        private boolean autoBid;

        public BidRequest() {}
        public BidRequest(String bidderId, double price, boolean autoBid) {
            this.bidderId = bidderId;
            this.price = price;
            this.autoBid = autoBid;
        }

        public String getBidderId() { return bidderId; }
        public void setBidderId(String bidderId) { this.bidderId = bidderId; }

        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }

        public boolean isAutoBid() { return autoBid; }
        public void setAutoBid(boolean autoBid) { this.autoBid = autoBid; }
    }
}