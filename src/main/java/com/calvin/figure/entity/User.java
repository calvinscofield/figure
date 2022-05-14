package com.calvin.figure.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import com.calvin.figure.FigureApplication;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

@Entity
@Table(name = "[user]")
@EntityListeners(AuditingEntityListener.class)
public class User implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = FigureApplication.SERIAL_VERSION_UID;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 14, unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 10, unique = true)
    private String name;

    @Column(unique = true)
    private String email;

    @Column(length = 14, unique = true)
    private String phone;

    @Column
    private Instant expiryTime; // 账户的过期时间，null表示永不过期

    @Column
    private Instant credentialsExpiryTime; // 凭证的过期时间，null表示永不过期

    @Column
    private Boolean locked;

    @Column
    private Boolean disabled;

    @Column
    private String remark;

    @JsonIgnoreProperties({ "modifier", "creator", "permission", "createTime", "updateTime" })
    @ManyToMany
    @JoinTable(uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "role_id" }))
    private List<Role> role; // 不要设置成 = new ArrayList<>();因为修改的时候要区分前端传过来的是空数组还是没有传。

    @JsonIgnore
    @ManyToOne
    @JoinColumn(updatable = false)
    @CreatedBy
    private User creator;

    @JsonIgnore
    @ManyToOne
    @JoinColumn
    @LastModifiedBy
    private User modifier;

    @Column(updatable = false)
    @CreatedDate
    private Instant createTime;

    @Column
    @LastModifiedDate
    private Instant updateTime;

    @JsonIgnore
    @Version
    private Integer version;

    @Transient
    @JsonIgnore
    private Set<GrantedAuthority> authorities;

    @Transient
    private ObjectNode perms;

    public User() {
    }

    public User(String username, Boolean locked, Boolean disabled) {
        this.username = username;
        this.locked = locked;
        this.disabled = disabled;
    }

    public User(String username) {
        this(username, false, false);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Instant getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(Instant expiryTime) {
        this.expiryTime = expiryTime;
    }

    public Instant getCredentialsExpiryTime() {
        return credentialsExpiryTime;
    }

    public void setCredentialsExpiryTime(Instant credentialsExpiryTime) {
        this.credentialsExpiryTime = credentialsExpiryTime;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public User getModifier() {
        return modifier;
    }

    public void setModifier(User modifier) {
        this.modifier = modifier;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Instant createTime) {
        this.createTime = createTime;
    }

    public Instant getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Instant updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<Role> getRole() {
        return role;
    }

    public void setRole(List<Role> role) {
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
        this.authorities = Collections.unmodifiableSet(sortAuthorities(authorities));
    }

    private static SortedSet<GrantedAuthority> sortAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Assert.notNull(authorities, "Cannot pass a null GrantedAuthority collection");
        // Ensure array iteration order is predictable (as per
        // UserDetails.getAuthorities() contract and SEC-717)
        SortedSet<GrantedAuthority> sortedAuthorities = new TreeSet<>(new AuthorityComparator());
        for (GrantedAuthority grantedAuthority : authorities) {
            Assert.notNull(grantedAuthority, "GrantedAuthority list cannot contain any null elements");
            sortedAuthorities.add(grantedAuthority);
        }
        return sortedAuthorities;
    }

    private static class AuthorityComparator implements Comparator<GrantedAuthority>, Serializable {

        private static final long serialVersionUID = FigureApplication.SERIAL_VERSION_UID;

        @Override
        public int compare(GrantedAuthority g1, GrantedAuthority g2) {
            // Neither should ever be null as each entry is checked before adding it to
            // the set. If the authority is null, it is a custom authority and should
            // precede others.
            if (g2.getAuthority() == null) {
                return -1;
            }
            if (g1.getAuthority() == null) {
                return 1;
            }
            return g1.getAuthority().compareTo(g2.getAuthority());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User) {
            return this.username.equals(((User) obj).username);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.username.hashCode();
    }

    @Override
    public boolean isAccountNonExpired() {
        if (this.expiryTime != null && this.expiryTime.isBefore(Instant.now()))
            return false;
        else
            return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.locked == null ? true : !this.locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        if (this.credentialsExpiryTime != null && this.credentialsExpiryTime.isBefore(Instant.now()))
            return false;
        else
            return true;
    }

    @Override
    public boolean isEnabled() {
        return this.disabled == null ? true : !this.disabled;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }

    public ObjectNode getPerms() {
        return perms;
    }

    public void setPerms(ObjectNode perms) {
        this.perms = perms;
    }

}
