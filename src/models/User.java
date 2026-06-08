package models;

public class User {
    private int    userId;
    private String name;
    private String email;
    private String passwordHash;
    private String phone;
    private String role;          // admin | doctor | patient
    private Integer createdBy;   // NULL for self-registered root admins
    private int    groupId;      // root admin's user_id — shared by whole group

    public User() {}

    public User(String name, String email, String passwordHash, String phone, String role) {
        this.name         = name;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.phone        = phone;
        this.role         = role;
    }

    // ── Getters & Setters ──

    public int getUserId()                  { return userId; }
    public void setUserId(int userId)       { this.userId = userId; }

    public String getName()                 { return name; }
    public void setName(String name)        { this.name = name; }

    public String getEmail()                { return email; }
    public void setEmail(String email)      { this.email = email; }

    public String getPasswordHash()                        { return passwordHash; }
    public void setPasswordHash(String passwordHash)       { this.passwordHash = passwordHash; }

    public String getPhone()                { return phone; }
    public void setPhone(String phone)      { this.phone = phone; }

    public String getRole()                 { return role; }
    public void setRole(String role)        { this.role = role; }

    public Integer getCreatedBy()                   { return createdBy; }
    public void setCreatedBy(Integer createdBy)     { this.createdBy = createdBy; }

    public int getGroupId()                 { return groupId; }
    public void setGroupId(int groupId)     { this.groupId = groupId; }
}