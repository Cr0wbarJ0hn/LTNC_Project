package auction.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ WebSocket Handler - Realtime Bid Updates
 */
@Component
public class AuctionWebSocketHandler extends AbstractWebSocketHandler {

    private final Map<String, Set<WebSocketSession>> auctionSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String auctionId = getAuctionIdFromSession(session);
        auctionSessions.computeIfAbsent(auctionId, k -> ConcurrentHashMap.newKeySet())
                .add(session);

        System.out.println("[WebSocket] ✅ Client connected to auction: " + auctionId);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String auctionId = getAuctionIdFromSession(session);

        // Parse incoming bid
        Map<String, Object> bidData = objectMapper.readValue(message.getPayload(), Map.class);

        // Broadcast to all clients
        broadcastToAuction(auctionId, bidData);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String auctionId = getAuctionIdFromSession(session);
        Set<WebSocketSession> sessions = auctionSessions.get(auctionId);
        if (sessions != null) {
            sessions.remove(session);
        }

        System.out.println("[WebSocket] ❌ Client disconnected from auction: " + auctionId);
    }

    /**
     * ✅ Broadcast bid update to all connected clients
     */
    public void broadcastToAuction(String auctionId, Map<String, Object> bidData) {
        Set<WebSocketSession> sessions = auctionSessions.get(auctionId);
        if (sessions == null) return;

        TextMessage message = null;
        try {
            message = new TextMessage(objectMapper.writeValueAsString(bidData));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    System.err.println("[WebSocket] ❌ Error sending message: " + e.getMessage());
                }
            }
        }
    }

    /**
     * ✅ Broadcast extension event
     */
    public void broadcastExtensionEvent(String auctionId, String bidderId, LocalDateTime newEndTime) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "AUCTION_EXTENDED");
        event.put("auctionId", auctionId);
        event.put("bidderId", bidderId);
        event.put("newEndTime", newEndTime.toString());
        event.put("timestamp", LocalDateTime.now().toString());

        broadcastToAuction(auctionId, event);
    }

    private String getAuctionIdFromSession(WebSocketSession session) {
        String uri = session.getUri().toString();
        return uri.substring(uri.lastIndexOf("/") + 1);
    }
}