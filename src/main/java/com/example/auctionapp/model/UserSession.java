package com.example.auctionapp.model;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class UserSession {
    // 🌟 Unified polymorphic object tracking (handles both User and Admin)
    private static User currentUser;
    private static PrintWriter out;
    private static BufferedReader in;
    private static String currentCategory = "All";


    public static void setSession(User user, PrintWriter outputStream) {
        currentUser = user;
        out = outputStream;
    }


    public static String getUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }


    public static boolean isAdmin() {
        return currentUser instanceof Admin;
    }

    public static User getCurrentUser() {
        return currentUser;
    }


    public static void cleanUserSession() {
        currentUser = null;       // 🌟 FIXED: Erases the active session model completely!
        currentCategory = "All";   // Reset browsing filters back to default

        try {
            if (out != null) {
                out.close();
                out = null;
            }
            if (in != null) {
                in.close();
                in = null;
            }
        } catch (Exception e) {
            System.err.println("Error cleaning up network streams on logout.");
        }
    }

    // --- Stream Getters & Setters ---
    public static PrintWriter getOut() { return out; }
    public static void setOut(PrintWriter out) { UserSession.out = out; }

    public static BufferedReader getIn() { return in; }
    public static void setIn(BufferedReader in) { UserSession.in = in; }

    // --- State Category Getters & Setters ---
    public static String getCurrentCategory() {
        return currentCategory;
    }

    public static void setCurrentCategory(String category) {
        currentCategory = category;
    }
}

