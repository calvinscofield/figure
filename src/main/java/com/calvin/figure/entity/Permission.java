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
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import com.calvin.figure.FigureApplication;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table
@EntityListeners(AuditingEntityListener.class)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class Permission implements Serializable {

	private static final long serialVersionUID = FigureApplication.SERIAL_VERSION_UID;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(length = 45, unique = true, nullable = false)
	private String name;

	@JsonIgnoreProperties({ "createTime", "updateTime" })
	@ManyToOne
	@JoinColumn(nullable = false)
	private MetaTable metaTable;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private JsonNode field;

	@Column
	private String remark;

	@JsonIncludeProperties({ "id", "name" })
	// @ManyToMany(mappedBy = "permission")
	/*
	 * mappedBy = "permission" 的作用是把中间表role_permission的维护责任交给role表去维护，使自己不参与。
	 * 这样的多对多关系是一种双向关系，一方是拥有方，另一方是被拥有方，只有拥有方才具有维护中间表的权利。
	 */
	@ManyToMany
	@JoinTable(name = "role_permission")
	/*
	 * 不指定mappedBy 实际上是两个单向的多对多关系，如果没指定上面的@JoinTable，默认是生成中间表permission_role
	 * Permission拥有维护中间表permission_role的权利
	 * Role拥有维护中间表role_permission的权利
	 * Permission和Role各自维护者对应的中间表
	 * 当按上面@JoinTable的方式指定了中间表，让两个中间表合二为一了，从而使两个单向的多对多关系通过一个中间表发生了联系，
	 * 并且同时让Permission和Role两者都拥有了维护中间表的权利，秒啊！
	 */
	/*
	 * 我想要的需求是1.删除Permission时自动删除中间表中的对应记录，2.删除Role时也要自动删除中间表中的对应记录，
	 * 如果设置mappedBy = "permission"需求1就无法满足。
	 */
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

	public MetaTable getMetaTable() {
		return metaTable;
	}

	public void setMetaTable(MetaTable metaTable) {
		this.metaTable = metaTable;
	}

	public JsonNode getField() {
		return field;
	}

	public void setField(JsonNode field) {
		this.field = field;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public User getModifier() {
		return modifier;
	}

	public void setModifier(User modifier) {
		this.modifier = modifier;
	}

	public User getCreator() {
		return creator;
	}

	public void setCreator(User creator) {
		this.creator = creator;
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

}
