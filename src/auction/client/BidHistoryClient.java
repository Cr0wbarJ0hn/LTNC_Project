package auction.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ✅ Client để gọi REST API + WebSocket
 */
public class BidHistoryClient {
    private static final String BASE_URL = "http://localhost:8080/api";
    private static final String WS_BASE_URL = "ws://localhost:8080/ws";

    private final HttpClient httpClient;
    private final Gson gson;
    private Consumer<BidUpdate> bidUpdateCallback;

    public BidHistoryClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
                .create();
    }

    /**
     * Lấy lịch sử bid từ server
     */
    public List<BidUpdate> getBidHistory(String auctionId) throws Exception {
        String url = BASE_URL + "/auctions/" + auctionId + "/bids";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        List<BidUpdate> bids = new ArrayList<>();
        JsonArray jsonArray = gson.fromJson(response.body(), JsonArray.class);

        for (var element : jsonArray) {
            JsonObject obj = element.getAsJsonObject();
            bids.add(new BidUpdate(
                    obj.get("auctionId").getAsString(),
                    obj.get("bidderId").getAsString(),
                    obj.get("price").getAsDouble(),
                    obj.get("timestamp").getAsString()
            ));
        }

        return bids;
    }

    /**
     * Lấy thống kê phiên
     */
    public AuctionStats getAuctionStats(String auctionId) throws Exception {
        String url = BASE_URL + "/auctions/" + auctionId + "/stats";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        JsonObject stats = gson.fromJson(response.body(), JsonObject.class);

        return new AuctionStats(
                stats.get("totalBids").getAsLong(),
                stats.get("autoBids").getAsLong(),
                stats.get("maxPrice").getAsDouble(),
                stats.get("avgPrice").getAsDouble(),
                stats.get("minPrice").getAsDouble()
        );
    }

    /**
     * Set callback cho bid updates
     */
    public void setBidUpdateCallback(Consumer<BidUpdate> callback) {
        this.bidUpdateCallback = callback;
    }

    /**
     * DTO for Bid Update
     */
    public static class BidUpdate {
        public String auctionId;
        public String bidderId;
        public double price;
        public String timestamp;

        public BidUpdate(String auctionId, String bidderId, double price, String timestamp) {
            this.auctionId = auctionId;
            this.bidderId = bidderId;
            this.price = price;
            this.timestamp = timestamp;
        }
    }

    /**
     * DTO for Auction Stats
     */
    public static class AuctionStats {
        public long totalBids;
        public long autoBids;
        public double maxPrice;
        public double avgPrice;
        public double minPrice;

        public AuctionStats(long totalBids, long autoBids, double maxPrice,
                            double avgPrice, double minPrice) {
            this.totalBids = totalBids;
            this.autoBids = autoBids;
            this.maxPrice = maxPrice;
            this.avgPrice = avgPrice;
            this.minPrice = minPrice;
        }
    }

    /**
     * Gson Serializer for LocalDateTime
     */
    private static class LocalDateTimeSerializer implements
            com.google.gson.JsonSerializer<LocalDateTime> {
        @Override
        public com.google.gson.JsonElement serialize(LocalDateTime src,
                                                     java.lang.reflect.Type typeOfSrc,
                                                     com.google.gson.JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(src.toString());
        }
    }
}