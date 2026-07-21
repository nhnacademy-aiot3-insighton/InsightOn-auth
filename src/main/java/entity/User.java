package entity;

import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @NonNull
    @Column(name = "user_id", nullable = false, length = 26)
    private String userId;

    @NonNull
    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @NonNull
    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Setter
    @Column(name = "last_login_at", nullable = true)
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private User(String email, String userName, Status status, Role role) {
        this.userId = UlidCreator.getUlid().toString();
        this.email = email;
        this.userName = userName;
        this.status = status;
        this.role = role;
        this.lastLoginAt = null;
        this.createdAt = LocalDateTime.now();
    }

    public static User createMember(String email, String userName) {
        return new User(email, userName, Status.ACTIVE, Role.MEMBER);
    }

    public static User createAdmin(String email, String userName) {
        return new User(email, userName, Status.ACTIVE, Role.ADMIN);
    }
}
