package study.shop.member.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "members", uniqueConstraints =
        @UniqueConstraint(name = "uk_members_login_id", columnNames = "login_id"))
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, length = 30, updatable = false)
    private String loginId;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    protected Member() { }

    public static Member withPasswordHash(String loginId, String displayName, String passwordHash) {
        Member member = new Member();
        member.loginId = LoginIds.normalize(loginId);
        if (displayName == null || displayName.strip().isEmpty() || displayName.strip().length() > 50) {
            throw new IllegalArgumentException("표시 이름은 공백을 제외하고 1~50자입니다.");
        }
        member.displayName = displayName.strip();
        if (passwordHash == null || !passwordHash.matches("\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}")) {
            throw new IllegalArgumentException("BCrypt 형식의 해시가 필요합니다.");
        }
        member.passwordHash = passwordHash;
        return member;
    }

    public Long getId() { return id; }
    public String getLoginId() { return loginId; }
    public String getDisplayName() { return displayName; }
    @JsonIgnore
    public String getPasswordHash() { return passwordHash; }

    @Override
    public String toString() {
        return "Member[id=" + id + ", loginId=" + loginId + "]";
    }
}
