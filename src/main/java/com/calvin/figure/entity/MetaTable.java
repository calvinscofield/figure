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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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
public class MetaTable implements Serializable {

	private static final long serialVersionUID = FigureApplication.SERIAL_VERSION_UID;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(length = 45, unique = true, nullable = false)
	private String name;

	@Column(length = 45)
	private String displayName;

	@JsonIgnoreProperties({ "metaTable", "createTime", "updateTime" })
	@OneToMany(mappedBy = "metaTable")
	private List<MetaField> metaField;

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

	public MetaTable() {
	}

	public MetaTable(String name, String displayName) {
		this.name = name;
		this.displayName = displayName;
	}

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

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
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

	public List<MetaField> getMetaField() {
		return metaField;
	}

	public void setMetaField(List<MetaField> metaField) {
		this.metaField = metaField;
	}
}
