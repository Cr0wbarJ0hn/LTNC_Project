// src/auction/service/UserService.java
package auction.service;

import auction.model.*;
import auction.repository.UserRepository;

import java.util.List;
import java.util.Optional;

public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    // ── Registration ─────────────────────────────────────────

    public Bidder registerBidder(String username, String email, String password) {
        validateNewUser(username, email);
        Bidder bidder = new Bidder(username, email, hashPassword(password));
        repo.save(bidder);
        System.out.println("✔ Bidder registered: " + username);
        return bidder;
    }

    public Seller registerSeller(String username, String email, String password, String storeName) {
        validateNewUser(username, email);
        Seller seller = new Seller(username, email, hashPassword(password), storeName);
        repo.save(seller);
        System.out.println("✔ Seller registered: " + username + " | Store: " + storeName);
        return seller;
    }

    public Admin registerAdmin(String username, String email, String password, String adminCode) {
        validateNewUser(username, email);
        Admin admin = new Admin(username, email, hashPassword(password), adminCode);
        repo.save(admin);
        System.out.println("✔ Admin registered: " + username);
        return admin;
    }

    // ── Authentication ────────────────────────────────────────

    public Optional<User> login(String username, String password) {
        Optional<User> userOpt = repo.findByUsername(username);
        if (userOpt.isEmpty()) return Optional.empty();
        User user = userOpt.get();
        if (!user.isActive()) {
            System.out.println("✖ Account is deactivated.");
            return Optional.empty();
        }
        if (!user.getPasswordHash().equals(hashPassword(password))) {
            System.out.println("✖ Invalid password.");
            return Optional.empty();
        }
        System.out.println("✔ Login successful: " + user.getRole() + " — " + username);
        return Optional.of(user);
    }

    // ── Profile Updates ───────────────────────────────────────

    public void updateEmail(String userId, String newEmail) {
        User user = findOrThrow(userId);
        if (repo.existsByEmail(newEmail)) throw new IllegalArgumentException("Email already in use.");
        user.setEmail(newEmail);
        repo.save(user);
    }

    public void changePassword(String userId, String oldPassword, String newPassword) {
        User user = findOrThrow(userId);
        if (!user.getPasswordHash().equals(hashPassword(oldPassword)))
            throw new IllegalArgumentException("Old password is incorrect.");
        user.setPasswordHash(hashPassword(newPassword));
        repo.save(user);
    }

    public void deactivateUser(String userId) {
        User user = findOrThrow(userId);
        user.setActive(false);
        repo.save(user);
        System.out.println("✔ User deactivated: " + user.getUserName());
    }

    public void reactivateUser(String userId) {
        User user = findOrThrow(userId);
        user.setActive(true);
        repo.save(user);
        System.out.println("✔ User reactivated: " + user.getUserName());
    }

    // ── Seller-specific ───────────────────────────────────────

    public void verifySeller(String sellerId) {
        User user = findOrThrow(sellerId);
        if (!(user instanceof Seller)) throw new IllegalArgumentException("User is not a Seller.");
        ((Seller) user).setVerified(true);
        repo.save(user);
        System.out.println("✔ Seller verified: " + user.getUserName());
    }

    // ── Bidder-specific ────────────────────────────────────────

    public void addFundsToBidder(String bidderId, double amount) {
        User user = findOrThrow(bidderId);
        if (!(user instanceof Bidder)) throw new IllegalArgumentException("User is not a Bidder.");
        ((Bidder) user).addFunds(amount);
        repo.save(user);
        System.out.printf("✔ Added $%.2f to %s's wallet.%n", amount, user.getUserName());
    }

    // ── Queries ───────────────────────────────────────────────

    public List<User> getAllUsers()                   { return repo.findAll(); }
    public List<User> getUsersByRole(String role)     { return repo.findByRole(role); }
    public Optional<User> findById(String id)         { return repo.findById(id); }
    public Optional<User> findByUsername(String name) { return repo.findByUsername(name); }
    public boolean deleteUser(String id)              { return repo.delete(id); }
    public int getTotalUsers()                        { return repo.count(); }

    // ── Helpers ───────────────────────────────────────────────

    private void validateNewUser(String username, String email) {
        if (repo.existsByUsername(username))
            throw new IllegalArgumentException("Username already taken: " + username);
        if (repo.existsByEmail(email))
            throw new IllegalArgumentException("Email already registered: " + email);
    }

    private User findOrThrow(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }


    private String hashPassword(String password) {
        return Integer.toHexString(password.hashCode());
    }
}