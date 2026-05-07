package auction.repository;

import auction.model.entity.BidHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ Spring Data JPA Repository - Persist Bid History
 */
@Repository
public interface BidHistoryRepository extends JpaRepository<BidHistoryEntity, Long> {

    /**
     * Lấy tất cả bids của một phiên đấu giá
     */
    @Query("SELECT b FROM BidHistoryEntity b WHERE b.auctionId = :auctionId ORDER BY b.timestamp ASC")
    List<BidHistoryEntity> findByAuctionId(@Param("auctionId") String auctionId);

    /**
     * Lấy bids của một bidder
     */
    @Query("SELECT b FROM BidHistoryEntity b WHERE b.bidderId = :bidderId ORDER BY b.timestamp DESC")
    List<BidHistoryEntity> findByBidderId(@Param("bidderId") String bidderId);

    /**
     * Lấy bids trong khoảng thời gian
     */
    @Query("SELECT b FROM BidHistoryEntity b WHERE b.auctionId = :auctionId " +
            "AND b.timestamp BETWEEN :start AND :end ORDER BY b.timestamp ASC")
    List<BidHistoryEntity> findByAuctionIdAndTimeRange(
            @Param("auctionId") String auctionId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Lấy giá cao nhất của phiên
     */
    @Query(value = "SELECT MAX(b.price) FROM bid_history b WHERE b.auction_id = :auctionId",
            nativeQuery = true)
    Double findMaxPriceByAuctionId(@Param("auctionId") String auctionId);

    /**
     * Lấy số lượng bids
     */
    long countByAuctionId(String auctionId);

    /**
     * Lấy auto-bid count
     */
    long countByAuctionIdAndIsAutoBid(String auctionId, boolean isAutoBid);
}