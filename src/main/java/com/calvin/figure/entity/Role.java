package com.calvin.figure.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.UniqueConstraint;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import com.calvin.figure.FigureApplication;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table
@EntityListeners(AuditingEntityListener.class)
public class Role implements Serializable {

    private static final long serialVersionUID = FigureApplication.SERIAL_VERSION_UID;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column
    private String remark;

    @JsonIgnoreProperties({ "role", "detail", "action", "metaTable", "createTime", "updateTime" })
    @ManyToMany
    @JoinTable(uniqueConstraints = @UniqueConstraint(columnNames = { "role_id", "permission_id" }))
    private List<Permission> permission; // 不要设置成 = new ArrayList<>();因为修改的时候要区分前端传过来的是空数组还是没有传。

    @JsonIgnore
    // @ManyToMany(mappedBy = "role")
    // 参考Permission实体里的注释
    @ManyToMany
    @JoinTable(name = "user_role")
    private List<User> user; // 不要设置成 = new ArrayList<>();因为修改的时候要区分前端传过来的是空数组还是没有传。

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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<Permission> getPermission() {
        return permission;
    }

    public void setPermission(List<Permission> permission) {
        this.permission = permission;
    }

    public List<User> getUser() {
        return user;
    }

    public void setUser(List<User> user) {
        this.user = user;
    }
}
