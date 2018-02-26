package com.ambulance.rider.Model;

/**
 * Created by sumit on 21-Jan-18.
 */

public class Riders {

    private String username, name, email, phone, password;

    // Default Constructor
    public Riders() {
    }

    // Parameterized Constructor
    public Riders(String username, String name, String email, String phone, String password) {
        this.username = username;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

    // Functions

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
