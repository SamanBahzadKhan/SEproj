package com.fridge.caps.models;

/**
 * Student.java
 * Represents a student user with academic information (department, year of study, phone).
 * Extends User with student-specific fields and maps to Firestore "students" collection.
 * Used for login, profile management, and appointment booking.
 */
public class Student extends User {

    private String phone;
    private String department;
    private String yearOfStudy;
    /** When false, student account is disabled; null/missing treated as active. */
    private Boolean isActive;

    public Student() {}

    public Student(String userId, String name, String email,
                   String phone, String department,
                   String yearOfStudy, String createdAt) {
        super(userId, name, email, UserRole.STUDENT, createdAt);
        this.phone       = phone;
        this.department  = department;
        this.yearOfStudy = yearOfStudy;
    }

    @Override
    public void getProfile() {}

    public String getPhone()       { return phone; }
    public String getDepartment()  { return department; }
    public String getYearOfStudy() { return yearOfStudy; }

    public Boolean getIsActive() { return isActive; }

    /** UI: treat missing field as active. */
    public boolean isAccountActive() {
        return isActive == null || isActive;
    }

    public void setPhone(String phone)             { this.phone = phone; }
    public void setDepartment(String department)   { this.department = department; }
    public void setYearOfStudy(String yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    public void setIsActive(Boolean active) { this.isActive = active; }
}