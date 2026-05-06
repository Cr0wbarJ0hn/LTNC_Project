module com.example.auctionapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires eu.hansolo.tilesfx;
    requires java.sql;
    requires com.google.gson;

    opens com.example.auctionapp to javafx.fxml;
    exports com.example.auctionapp;
    exports com.example.auctionapp.controller;
    opens com.example.auctionapp.controller to javafx.fxml;
    opens com.example.auctionapp.model to com.google.gson;
}


