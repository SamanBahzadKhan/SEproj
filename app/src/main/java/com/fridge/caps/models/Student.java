package com.fridge.caps.models;



/**
 * Purpose: Defines core domain data structures and status values.
 * Depends on: Firebase timestamp types and Java/Kotlin data accessors.
 * Notes: Used as transfer objects between controllers and screens.
 */
/**
 * Purpose: Defines core domain data structures and status values.
 * Depends on: Firebase timestamp types and Java/Kotlin data accessors.
 * Notes: Used as transfer objects between controllers and screens.
 */
import com.google.firebase.firestore.PropertyName;

public class Student extends User {

    private String phone;
    private String department;
    private String yearOfStudy;
    
    private String campusStudentId;
    
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

    
    @PropertyName("studentId")
    public String getCampusStudentId() {
        return campusStudentId != null ? campusStudentId.trim() : "";
    }

    @PropertyName("studentId")
    public void setCampusStudentId(String campusStudentId) {
        this.campusStudentId = campusStudentId;
    }

    public Boolean getIsActive() { return isActive; }

    
    public boolean isAccountActive() {
        return isActive == null || isActive;
    }

    public void setPhone(String phone)             { this.phone = phone; }
    public void setDepartment(String department)   { this.department = department; }
    public void setYearOfStudy(String yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    public void setIsActive(Boolean active) { this.isActive = active; }
}