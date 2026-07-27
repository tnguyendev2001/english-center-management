package com.englishcenter.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classroom.ClassroomStatus;
import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.student.Student;
import com.englishcenter.student.StudentRepository;
import com.englishcenter.student.StudentStatus;
import com.englishcenter.teacher.Teacher;
import com.englishcenter.teacher.TeacherRepository;
import com.englishcenter.teacher.TeacherStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.Instant;
import java.util.UUID;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthSecurityIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ClassroomRepository classroomRepository;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    private Student student;
    private Teacher teacher;
    private Classroom assignedClassroom;
    private Classroom otherClassroom;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();
        classSessionRepository.deleteAll();
        classroomRepository.deleteAll();
        studentRepository.deleteAll();
        teacherRepository.deleteAll();

        student = new Student();
        student.setStudentCode("ST00001");
        student.setFullName("Student One");
        student.setStatus(StudentStatus.ACTIVE);
        student = studentRepository.save(student);

        teacher = new Teacher();
        teacher.setTeacherCode("GV001");
        teacher.setFullName("Teacher One");
        teacher.setStatus(TeacherStatus.ACTIVE);
        teacher = teacherRepository.save(teacher);

        Teacher otherTeacher = new Teacher();
        otherTeacher.setTeacherCode("GV002");
        otherTeacher.setFullName("Teacher Two");
        otherTeacher.setStatus(TeacherStatus.ACTIVE);
        otherTeacher = teacherRepository.save(otherTeacher);

        assignedClassroom = classroom("CLS-A", "Class A", teacher.getId(), teacher.getFullName());
        otherClassroom = classroom("CLS-B", "Class B", otherTeacher.getId(), otherTeacher.getFullName());

        createAccount("admin", "Admin12345", AccountRole.ADMIN, null, null, false);
        createAccount("teacher1", "Teacher123", AccountRole.TEACHER, null, teacher.getId(), false);
        createAccount("student1", "Student123", AccountRole.STUDENT, student.getId(), null, false);
        createAccount("mustchange", "TempPass123", AccountRole.ADMIN, null, null, true);
    }

    @Test
    void loginReturnsJwtWithRole() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Admin12345"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"));
    }

    @Test
    void wrongPasswordReturnsGenericError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"wrong-pass"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng."));
    }

    @Test
    void fiveFailedLoginsLocksAccount() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"student1","password":"bad"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"student1","password":"Student123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(
                        "Tài khoản đang tạm khóa do đăng nhập sai nhiều lần. Vui lòng thử lại sau."
                ));
    }

    @Test
    void anonymousProtectedApiReturns401() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotAccessFinance() throws Exception {
        String token = login("student1", "Student123");
        mockMvc.perform(get("/api/finance/config").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotAccessFinance() throws Exception {
        String token = login("teacher1", "Teacher123");
        mockMvc.perform(get("/api/finance/config").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessFinance() throws Exception {
        String token = login("admin", "Admin12345");
        mockMvc.perform(get("/api/finance/config").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void teacherCanAccessAssignedClassroomOnly() throws Exception {
        String token = login("teacher1", "Teacher123");
        mockMvc.perform(get("/api/classrooms/" + assignedClassroom.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/classrooms/" + otherClassroom.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherMeClassroomsReturnOnlyAssigned() throws Exception {
        String token = login("teacher1", "Teacher123");
        mockMvc.perform(get("/api/me/classrooms").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(assignedClassroom.getId().intValue()));

        mockMvc.perform(get("/api/me/teacher-dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedClassroomCount").value(1));
    }

    @Test
    void teacherCannotCreateClassroom() throws Exception {
        String token = login("teacher1", "Teacher123");
        mockMvc.perform(post("/api/classrooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "classCode":"CLS-X",
                                  "className":"Unauthorized",
                                  "level":"A1",
                                  "room":"R1",
                                  "startDate":"2026-07-06",
                                  "daysOfWeek":["MONDAY"],
                                  "startTime":"18:00:00",
                                  "endTime":"19:00:00",
                                  "status":"PLANNED",
                                  "teacherId":1
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCanCreateSessionForAssignedClassroom() throws Exception {
        String token = login("teacher1", "Teacher123");
        LocalDate nextMonday = nextWeekday(ClassDayOfWeek.MONDAY);
        mockMvc.perform(post("/api/class-sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "classroomId": %d,
                                  "sessionDate": "%s",
                                  "startTime": "18:00:00",
                                  "endTime": "19:00:00",
                                  "note": "Buổi học thường"
                                }
                                """.formatted(assignedClassroom.getId(), nextMonday)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.classroomId").value(assignedClassroom.getId().intValue()));
    }

    @Test
    void teacherCannotCreateSessionForOtherClassroom() throws Exception {
        String token = login("teacher1", "Teacher123");
        LocalDate nextMonday = nextWeekday(ClassDayOfWeek.MONDAY);
        mockMvc.perform(post("/api/class-sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "classroomId": %d,
                                  "sessionDate": "%s",
                                  "startTime": "18:00:00",
                                  "endTime": "19:00:00"
                                }
                                """.formatted(otherClassroom.getId(), nextMonday)))
                .andExpect(status().isForbidden());
    }

    private LocalDate nextWeekday(ClassDayOfWeek dayOfWeek) {
        LocalDate cursor = LocalDate.now();
        for (int i = 0; i < 14; i++) {
            if (cursor.getDayOfWeek() == dayOfWeek.toJavaDayOfWeek()) {
                return cursor;
            }
            cursor = cursor.plusDays(1);
        }
        return cursor;
    }

    @Test
    void studentCanAccessOwnMeInvoices() throws Exception {
        String token = login("student1", "Student123");
        mockMvc.perform(get("/api/me/invoices").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void studentCanAccessStudentDashboard() throws Exception {
        String token = login("student1", "Student123");
        mockMvc.perform(get("/api/me/student-dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeClassCount").isNumber())
                .andExpect(jsonPath("$.data.totalOutstandingDebt").exists());
    }

    @Test
    void teacherCannotAccessStudentDashboard() throws Exception {
        String token = login("teacher1", "Teacher123");
        mockMvc.perform(get("/api/me/student-dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotModifyAttendance() throws Exception {
        String token = login("student1", "Student123");
        mockMvc.perform(post("/api/attendance/mark")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sessionId":1,"items":[{"studentId":1,"status":"PRESENT"}]}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreatesStudentAccountOnce() throws Exception {
        Student fresh = new Student();
        fresh.setStudentCode("ST00099");
        fresh.setFullName("Fresh Student");
        fresh.setStatus(StudentStatus.ACTIVE);
        fresh = studentRepository.save(fresh);

        String token = login("admin", "Admin12345");
        MvcResult result = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ST00099","role":"STUDENT","studentId":%d,"teacherId":null}
                                """.formatted(fresh.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.temporaryPassword").isNotEmpty())
                .andExpect(jsonPath("$.data.user.mustChangePassword").value(true))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String temporaryPassword = body.path("data").path("temporaryPassword").asText();
        assertThat(temporaryPassword).hasSizeGreaterThanOrEqualTo(12);

        UserAccount saved = userAccountRepository.findByNormalizedUsername("st00099").orElseThrow();
        assertThat(saved.getPasswordHash()).doesNotContain(temporaryPassword);
        assertThat(passwordEncoder.matches(temporaryPassword, saved.getPasswordHash())).isTrue();

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ST00099b","role":"STUDENT","studentId":%d,"teacherId":null}
                                """.formatted(fresh.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Học viên này đã có tài khoản."));
    }

    @Test
    void disableAccountRejectsExistingToken() throws Exception {
        String token = login("student1", "Student123");
        UserAccount account = userAccountRepository.findByNormalizedUsername("student1").orElseThrow();
        String adminToken = login("admin", "Admin12345");

        mockMvc.perform(patch("/api/admin/users/" + account.getId() + "/disable")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminCannotManageUsers() throws Exception {
        String token = login("teacher1", "Teacher123");
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void mustChangePasswordBlocksBusinessApis() throws Exception {
        String token = login("mustchange", "TempPass123");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/finance/config").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void changePasswordInvalidatesOldToken() throws Exception {
        String token = login("mustchange", "TempPass123");
        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword":"TempPass123",
                                  "newPassword":"NewPass1234",
                                  "confirmPassword":"NewPass1234"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"mustchange","password":"NewPass1234"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.mustChangePassword").value(false));
    }

    @Test
    void invalidIssuerIsRejected() throws Exception {
        UserAccount admin = userAccountRepository.findByNormalizedUsername("admin").orElseThrow();
        String badToken = encodeToken(admin, "wrong-issuer", "school-management-web", admin.getTokenVersion());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + badToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenVersionMismatchIsRejected() throws Exception {
        UserAccount admin = userAccountRepository.findByNormalizedUsername("admin").orElseThrow();
        String stale = encodeToken(admin, "school-management", "school-management-web", admin.getTokenVersion() + 1);
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + stale))
                .andExpect(status().isUnauthorized());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
    }

    private UserAccount createAccount(
            String username,
            String password,
            AccountRole role,
            Long studentId,
            Long teacherId,
            boolean mustChangePassword
    ) {
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setNormalizedUsername(username.toLowerCase());
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setRole(role);
        account.setStatus(AccountStatus.ACTIVE);
        account.setStudentId(studentId);
        account.setTeacherId(teacherId);
        account.setMustChangePassword(mustChangePassword);
        account.setFailedLoginAttempts(0);
        account.setTokenVersion(0);
        account.setCreatedBy("test");
        account.setUpdatedBy("test");
        return userAccountRepository.save(account);
    }

    private Classroom classroom(String code, String name, Long teacherId, String teacherName) {
        Classroom classroom = new Classroom();
        classroom.setClassCode(code);
        classroom.setClassName(name);
        classroom.setLevel("Starter");
        classroom.setTeacherName(teacherName);
        classroom.setTeacherId(teacherId);
        classroom.setStartDate(LocalDate.of(2026, 7, 1));
        classroom.setDaysOfWeek(Set.of(ClassDayOfWeek.MONDAY));
        classroom.setStartTime(LocalTime.of(18, 0));
        classroom.setEndTime(LocalTime.of(19, 0));
        classroom.setStatus(ClassroomStatus.ONGOING);
        return classroomRepository.save(classroom);
    }

    private String encodeToken(UserAccount account, String issuer, String audience, int version) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .audience(java.util.List.of(audience))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .subject(String.valueOf(account.getId()))
                .claim("username", account.getUsername())
                .claim("role", account.getRole().name())
                .claim("ver", version)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
