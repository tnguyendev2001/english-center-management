package com.englishcenter.auth;

import com.englishcenter.auth.dto.CreateUserRequest;
import com.englishcenter.auth.dto.CreateUserResponse;
import com.englishcenter.auth.dto.ResetPasswordResponse;
import com.englishcenter.auth.dto.UserAccountResponse;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.security.SecurityUtils;
import com.englishcenter.student.Student;
import com.englishcenter.student.StudentRepository;
import com.englishcenter.teacher.Teacher;
import com.englishcenter.teacher.TeacherRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
    private static final int MAX_PAGE_SIZE = 100;

    private final UserAccountRepository userAccountRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final UserAccountMapper userAccountMapper;
    private final AuthAuditLogger authAuditLogger;

    public AdminUserService(
            UserAccountRepository userAccountRepository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            PasswordEncoder passwordEncoder,
            TemporaryPasswordGenerator temporaryPasswordGenerator,
            UserAccountMapper userAccountMapper,
            AuthAuditLogger authAuditLogger
    ) {
        this.userAccountRepository = userAccountRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
        this.userAccountMapper = userAccountMapper;
        this.authAuditLogger = authAuditLogger;
    }

    @Transactional(readOnly = true)
    public Page<UserAccountResponse> search(String username, AccountRole role, AccountStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        String usernameFilter = username == null || username.isBlank() ? null : username.trim();
        Page<UserAccount> accounts = usernameFilter == null
                ? userAccountRepository.searchByRoleAndStatus(role, status, pageable)
                : userAccountRepository.search(usernameFilter, role, status, pageable);
        return accounts.map(userAccountMapper::toUserAccountResponse);
    }

    @Transactional(readOnly = true)
    public UserAccountResponse getById(Long id) {
        return userAccountMapper.toUserAccountResponse(findAccount(id));
    }

    @Transactional
    public CreateUserResponse create(CreateUserRequest request) {
        validateRoleProfile(request.role(), request.studentId(), request.teacherId());

        String username = request.username().trim();
        String normalized = AuthService.normalizeUsername(username);
        if (normalized.isBlank()) {
            throw new BusinessException("Tên đăng nhập là bắt buộc.");
        }
        if (userAccountRepository.existsByNormalizedUsername(normalized)) {
            throw new BusinessException("Tên đăng nhập đã tồn tại.");
        }

        if (request.role() == AccountRole.STUDENT) {
            Student student = studentRepository.findById(request.studentId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy học viên."));
            if (userAccountRepository.existsByStudentId(student.getId())) {
                throw new BusinessException("Học viên này đã có tài khoản.");
            }
        }
        if (request.role() == AccountRole.TEACHER) {
            Teacher teacher = teacherRepository.findById(request.teacherId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy giáo viên."));
            if (userAccountRepository.existsByTeacherId(teacher.getId())) {
                throw new BusinessException("Giáo viên này đã có tài khoản.");
            }
        }

        String temporaryPassword = temporaryPasswordGenerator.generate();
        String actor = SecurityUtils.currentUsernameOrSystem();

        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setNormalizedUsername(normalized);
        account.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        account.setRole(request.role());
        account.setStatus(AccountStatus.ACTIVE);
        account.setStudentId(request.role() == AccountRole.STUDENT ? request.studentId() : null);
        account.setTeacherId(request.role() == AccountRole.TEACHER ? request.teacherId() : null);
        account.setMustChangePassword(true);
        account.setFailedLoginAttempts(0);
        account.setTokenVersion(0);
        account.setCreatedBy(actor);
        account.setUpdatedBy(actor);

        UserAccount saved = userAccountRepository.save(account);
        authAuditLogger.accountCreated(saved.getId(), saved.getUsername(), saved.getRole(), actor);

        return new CreateUserResponse(userAccountMapper.toUserAccountResponse(saved), temporaryPassword);
    }

    @Transactional
    public UserAccountResponse enable(Long id) {
        UserAccount account = findAccount(id);
        if (account.getStatus() == AccountStatus.ACTIVE
                && (account.getLockedUntil() == null || !account.getLockedUntil().isAfter(java.time.LocalDateTime.now()))) {
            return userAccountMapper.toUserAccountResponse(account);
        }
        account.setStatus(AccountStatus.ACTIVE);
        account.setLockedUntil(null);
        account.setFailedLoginAttempts(0);
        account.incrementTokenVersion();
        account.setUpdatedBy(SecurityUtils.currentUsernameOrSystem());
        UserAccount saved = userAccountRepository.save(account);
        authAuditLogger.accountEnabled(saved.getId(), saved.getUsername(), SecurityUtils.currentUsernameOrSystem());
        return userAccountMapper.toUserAccountResponse(saved);
    }

    @Transactional
    public UserAccountResponse disable(Long id) {
        UserAccount account = findAccount(id);
        if (account.getStatus() == AccountStatus.DISABLED) {
            return userAccountMapper.toUserAccountResponse(account);
        }
        account.setStatus(AccountStatus.DISABLED);
        account.incrementTokenVersion();
        account.setUpdatedBy(SecurityUtils.currentUsernameOrSystem());
        UserAccount saved = userAccountRepository.save(account);
        authAuditLogger.accountDisabled(saved.getId(), saved.getUsername(), SecurityUtils.currentUsernameOrSystem());
        return userAccountMapper.toUserAccountResponse(saved);
    }

    @Transactional
    public ResetPasswordResponse resetPassword(Long id) {
        UserAccount account = findAccount(id);
        String temporaryPassword = temporaryPasswordGenerator.generate();
        account.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        account.setMustChangePassword(true);
        account.setFailedLoginAttempts(0);
        account.setLockedUntil(null);
        if (account.getStatus() == AccountStatus.LOCKED) {
            account.setStatus(AccountStatus.ACTIVE);
        }
        account.incrementTokenVersion();
        account.setUpdatedBy(SecurityUtils.currentUsernameOrSystem());
        UserAccount saved = userAccountRepository.save(account);
        authAuditLogger.passwordReset(saved.getId(), saved.getUsername(), SecurityUtils.currentUsernameOrSystem());
        return new ResetPasswordResponse(userAccountMapper.toUserAccountResponse(saved), temporaryPassword);
    }

    private UserAccount findAccount(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản."));
    }

    private void validateRoleProfile(AccountRole role, Long studentId, Long teacherId) {
        if (role == AccountRole.ADMIN) {
            if (studentId != null || teacherId != null) {
                throw new BusinessException("Vai trò và hồ sơ liên kết không hợp lệ.");
            }
            return;
        }
        if (role == AccountRole.STUDENT) {
            if (studentId == null || teacherId != null) {
                throw new BusinessException("Vai trò và hồ sơ liên kết không hợp lệ.");
            }
            return;
        }
        if (role == AccountRole.TEACHER) {
            if (teacherId == null || studentId != null) {
                throw new BusinessException("Vai trò và hồ sơ liên kết không hợp lệ.");
            }
            return;
        }
        throw new BusinessException("Vai trò và hồ sơ liên kết không hợp lệ.");
    }
}
