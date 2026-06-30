package com.airline.user.entity;

import jakarta.persistence.*;

/**
 * FrequentTraveler entity - stores saved travelers for quick booking.
 * Many-to-one relationship with User (each user can have multiple travelers).
 */
@Entity
@Table(name = "frequent_travelers")
public class FrequentTraveler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name")
    private String fullName;

    private String phone;

    private String email;

    private String relationship; // SELF, SPOUSE, CHILD, PARENT, OTHER

    public FrequentTraveler() {
    }

    public FrequentTraveler(Long userId, String fullName, String relationship) {
        this.userId = userId;
        this.fullName = fullName;
        this.relationship = relationship;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }
}
