package com.fridge.caps.model;

import com.fridge.caps.models.Student;
import com.fridge.caps.models.UserRole;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class StudentTest {

    private Student student;

    @Before
    public void setUp() {
        student = new Student();
        student.setUserId("student_001");
        student.setName("Ahmad Raza");
        student.setEmail("ahmad.raza@lums.edu.pk");
        student.setPhone("+92 301 1234567");
        student.setDepartment("Computer Science");
        student.setYearOfStudy("3rd Year");
        student.setRole(UserRole.STUDENT);
    }

    @Test
    public void testStudentFieldsSetCorrectly() {
        assertEquals("ahmad.raza@lums.edu.pk", student.getEmail());
        assertEquals("Ahmad Raza", student.getName());
        assertEquals("Computer Science", student.getDepartment());
        assertEquals("3rd Year", student.getYearOfStudy());
        assertEquals("+92 301 1234567", student.getPhone());
        assertEquals(UserRole.STUDENT, student.getRole());
    }

    @Test
    public void testStudentUidNotNull() {
        assertNotNull(student.getUserId());
        assertFalse(student.getUserId().isEmpty());
    }

    @Test
    public void testStudentDefaultConstructor() {
        Student emptyStudent = new Student();
        assertNull(emptyStudent.getName());
        assertNull(emptyStudent.getEmail());
    }
}
