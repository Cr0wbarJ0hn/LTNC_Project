-- ✅ Flyway Migration Script - Initialize Database

-- Drop existing tables if exist (optional)
-- DROP TABLE IF EXISTS anti_sniping_events;
-- DROP TABLE IF EXISTS auto_bid_requests;
-- DROP TABLE IF EXISTS bid_history;
-- DROP TABLE IF EXISTS auctions;
-- DROP TABLE IF EXISTS users;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
                                     user_id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create auctions table
CREATE TABLE IF NOT EXISTS auctions (
                                        auction_id VARCHAR(50) PRIMARY KEY,
    item_name VARCHAR(255) NOT NULL,
    item_category VARCHAR(100),
    initial_price DECIMAL(15,2) NOT NULL,
    current_price DECIMAL(15,2) NOT NULL,
    leading_bidder VARCHAR(50),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    seller_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_status (status),
    INDEX idx_end_time (end_time),
    INDEX idx_seller_id (seller_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create bid_history table
CREATE TABLE IF NOT EXISTS bid_history (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           auction_id VARCHAR(50) NOT NULL,
    bidder_id VARCHAR(50) NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_auto_bid BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions(auction_id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_auction_id (auction_id),
    INDEX idx_bidder_id (bidder_id),
    INDEX idx_timestamp (timestamp)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create auto_bid_requests table
CREATE TABLE IF NOT EXISTS auto_bid_requests (
                                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                 auction_id VARCHAR(50) NOT NULL,
    bidder_id VARCHAR(50) NOT NULL,
    max_bid DECIMAL(15,2) NOT NULL,
    increment DECIMAL(15,2) NOT NULL,
    extension_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions(auction_id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_auction_id (auction_id),
    INDEX idx_bidder_id (bidder_id),
    INDEX idx_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create anti_sniping_events table
CREATE TABLE IF NOT EXISTS anti_sniping_events (
                                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                   auction_id VARCHAR(50) NOT NULL,
    bidder_id VARCHAR(50) NOT NULL,
    extension_count INT NOT NULL,
    old_end_time TIMESTAMP NOT NULL,
    new_end_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions(auction_id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_auction_id (auction_id),
    INDEX idx_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert sample data
INSERT INTO users (user_id, username, email, password, role) VALUES
                                                                 ('SELLER-001', 'seller1', 'seller1@auction.com', 'hashed_password_1', 'SELLER'),
                                                                 ('SELLER-002', 'seller2', 'seller2@auction.com', 'hashed_password_2', 'SELLER'),
                                                                 ('BIDDER-001', 'bidder1', 'bidder1@auction.com', 'hashed_password_3', 'BIDDER'),
                                                                 ('BIDDER-002', 'bidder2', 'bidder2@auction.com', 'hashed_password_4', 'BIDDER'),
                                                                 ('BIDDER-003', 'bidder3', 'bidder3@auction.com', 'hashed_password_5', 'BIDDER'),
                                                                 ('ADMIN-001', 'admin', 'admin@auction.com', 'hashed_password_6', 'ADMIN');

-- Insert sample auctions
INSERT INTO auctions (auction_id, item_name, item_category, initial_price, current_price, leading_bidder, start_time, end_time, status, seller_id) VALUES
                                                                                                                                                       ('AUCTION-001', 'Picasso Painting', 'Art', 50000000, 55000000, 'BIDDER-001', NOW(), DATE_ADD(NOW(), INTERVAL 5 MINUTE), 'RUNNING', 'SELLER-001'),
                                                                                                                                                       ('AUCTION-002', 'iPhone 15 Pro', 'Electronics', 25000000, 28000000, 'BIDDER-002', NOW(), DATE_ADD(NOW(), INTERVAL 3 MINUTE), 'RUNNING', 'SELLER-002'),
                                                                                                                                                       ('AUCTION-003', 'Vintage Watch', 'Collectibles', 10000000, 12000000, 'BIDDER-003', NOW(), DATE_ADD(NOW(), INTERVAL 2 MINUTE), 'RUNNING', 'SELLER-001');

-- Insert sample bid history
INSERT INTO bid_history (auction_id, bidder_id, price, timestamp, is_auto_bid) VALUES
                                                                                   ('AUCTION-001', 'BIDDER-001', 50000000, DATE_SUB(NOW(), INTERVAL 5 MINUTE), FALSE),
                                                                                   ('AUCTION-001', 'BIDDER-002', 51000000, DATE_SUB(NOW(), INTERVAL 4 MINUTE), FALSE),
                                                                                   ('AUCTION-001', 'BIDDER-001', 52000000, DATE_SUB(NOW(), INTERVAL 3 MINUTE), TRUE),
                                                                                   ('AUCTION-001', 'BIDDER-002', 53000000, DATE_SUB(NOW(), INTERVAL 2 MINUTE), FALSE),
                                                                                   ('AUCTION-001', 'BIDDER-001', 55000000, DATE_SUB(NOW(), INTERVAL 1 MINUTE), TRUE),
                                                                                   ('AUCTION-002', 'BIDDER-002', 25000000, DATE_SUB(NOW(), INTERVAL 3 MINUTE), FALSE),
                                                                                   ('AUCTION-002', 'BIDDER-003', 26000000, DATE_SUB(NOW(), INTERVAL 2 MINUTE), TRUE),
                                                                                   ('AUCTION-002', 'BIDDER-002', 28000000, DATE_SUB(NOW(), INTERVAL 1 MINUTE), FALSE);