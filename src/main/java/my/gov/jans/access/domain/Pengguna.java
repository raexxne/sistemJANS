package my.gov.jans.access.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class Pengguna {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    @Column(name = "password_hash")
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    private Role role;
    private boolean enabled = true;
    private String phone;
    private String address;
    @Column(name = "profile_picture", columnDefinition = "LONGBLOB")
    private byte[] profilePicture;
    @Column(name = "profile_picture_type")
    private String profilePictureType;

    public Pengguna() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public String getName() {
        return name;
    }

    public void setName(String v) {
        name = v;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        email = v;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String v) {
        passwordHash = v;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role v) {
        role = v;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean v) {
        enabled = v;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String v) {
        phone = v;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String v) {
        address = v;
    }

    public byte[] getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(byte[] v) {
        profilePicture = v;
    }

    public String getProfilePictureType() {
        return profilePictureType;
    }

    public void setProfilePictureType(String v) {
        profilePictureType = v;
    }
}
