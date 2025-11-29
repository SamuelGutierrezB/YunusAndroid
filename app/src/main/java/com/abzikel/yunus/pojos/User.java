package com.abzikel.yunus.pojos;

import java.util.Date;

public class User {
    private String id;
    private String phone;
    private String firstName;
    private String lastName;
    private Double yunus;
    private Date creation;
    private Date lastLogin;

    // Constructor, getters, and setters

    public User() {
        // Default constructor required for Firebase
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Double getYunus() {
        return yunus;
    }

    public void setYunus(Double yunus) {
        this.yunus = yunus;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Date getCreation() {
        return creation;
    }

    public void setCreation(Date creation) {
        this.creation = creation;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

}
