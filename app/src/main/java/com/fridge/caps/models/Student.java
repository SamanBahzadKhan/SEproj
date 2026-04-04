package com.fridge.caps.models;

/**
 * Student.java
 * Represents a student user. Extends User with student-specific fields.
 * Maps to the Firestore "students" collection.
 */
public class Student extends User {

    private String phone;
    private String department;
    private String yearOfStudy;

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

    public void setPhone(String phone)             { this.phone = phone; }
    public void setDepartment(String department)   { this.department = department; }
    public void setYearOfStudy(String yearOfStudy) { this.yearOfStudy = yearOfStudy; }
}
