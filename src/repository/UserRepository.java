
package auction.repository;

import auction.model.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class UserRepository {

    private final String filePath;
    private Map<String, User> store; // id -> User

    public UserRepository(String filePath) {
        this.filePath = filePath;
        this.store = loadFromFile();
    }

    public void save(User user) {
        store.put(user.getId(), user);
        persistToFile();
    }

    public Optional<User> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public Optional<User> findByUsername(String username) {
        return store.values().stream()
                .filter(u -> u.getUserName().equalsIgnoreCase(username))
                .findFirst();
    }

    public Optional<User> findByEmail(String email) {
        return store.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    public List<User> findAll() {
        return new ArrayList<>(store.values());
    }

    public List<User> findByRole(String role) {
        return store.values().stream()
                .filter(u -> u.getRole().equalsIgnoreCase(role))
                .collect(Collectors.toList());
    }

    public boolean delete(String id) {
        if (!store.containsKey(id)) return false;
        store.remove(id);
        persistToFile();
        return true;
    }

    public boolean existsByEmail(String email) {
        return store.values().stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
    }

    public boolean existsByUsername(String username) {
        return store.values().stream()
                .anyMatch(u -> u.getUserName().equalsIgnoreCase(username));
    }

    public int count() { return store.size(); }

    // ── File I/O ─────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, User> loadFromFile() {
        File file = new File(filePath);
        if (!file.exists()) return new HashMap<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (Map<String, User>) ois.readObject();
        } catch (Exception e) {
            System.err.println("Warning: Could not load data from " + filePath + ". Starting fresh.");
            return new HashMap<>();
        }
    }

    private void persistToFile() {
        File file = new File(filePath);
        file.getParentFile().mkdirs(); // create /data dir if needed
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(store);
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }
}