package com.example.auctionapp.model;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class UserSession {
    // The static variable that holds the username globally
    private static String loggedInUsername;
    private static PrintWriter out;
    private static BufferedReader in;
    private static String currentCategory = "All";

    // Call this when the user successfully logs in
    public static void setUsername(String username) {
        loggedInUsername = username;
    }


    // Call this from any screen when you need to know who is logged in
    public static String getUsername() {
        return loggedInUsername;
    }

    // Call this when the user clicks "Logout"
    public static void cleanUserSession() {
        loggedInUsername = null;
    }
    public static PrintWriter getOut() { return out; }
    public static void setOut(PrintWriter out) { UserSession.out = out; }

    public static BufferedReader getIn() { return in; }
    public static void setIn(BufferedReader in) { UserSession.in = in; }

    public static String getCurrentCategory() {
        return currentCategory;
    }

    public static void setCurrentCategory(String category) {
        currentCategory = category;
    }


}

