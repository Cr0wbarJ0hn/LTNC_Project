package auction.Demo;

import auction.model.*;
import auction.repository.UserRepository;
import auction.service.UserService;

import java.util.List;

public class Main {
    public static void main(String[] args) {

        // Setup
        UserRepository repo    = new UserRepository("data/users.dat");
        UserService    service = new UserService(repo);

        // Register users
        Admin  admin  = service.registerAdmin("adminRoot", "admin@auction.com", "admin123", "ADMIN-XYZ");
        Bidder bidder = service.registerBidder("john_bidder", "john@mail.com", "pass123"); // ✅ Bidder
        Seller seller = service.registerSeller("alice_store", "alice@mail.com", "pass456", "Alice's Antiques");

        // Login test
        System.out.println("\n--- Login Tests ---");
        service.login("john_bidder", "pass123");
        service.login("john_bidder", "wrongpass");

        // Bidder: add funds
        System.out.println("\n--- Bidder Actions ---");
        service.addFundsToBidder(bidder.getId(), 500.00); //
        System.out.println("Wallet: $" +
                ((Bidder) service.findById(bidder.getId()).get()).getWalletBalance()); //

        // Seller: verify
        System.out.println("\n--- Seller Actions ---");
        service.verifySeller(seller.getId());

        // Admin: deactivate
        System.out.println("\n--- Admin Actions ---");
        service.deactivateUser(bidder.getId());
        service.login("john_bidder", "pass123");

        // List all users
        System.out.println("\n--- All Users ---");
        List<User> all = service.getAllUsers();
        all.forEach(System.out::println);

        System.out.println("\nTotal users: " + service.getTotalUsers());
    }
}