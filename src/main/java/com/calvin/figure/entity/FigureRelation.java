package com.calvin.figure.entity;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.calvin.figure.FigureApplication;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table
@EntityListeners(AuditingEntityListener.class)
public class FigureRelation {

	private static final long serialVersionUID = FigureApplication.SERIAL_VERSION_UID;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(length = 45, nullable = false)
	private String name;

	@ManyToOne
	@JoinColumn
	private Figure from;

	@ManyToOne
	@JoinColumn
	private Figure to;

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

	public Figure getFrom() {
		return from;
	}

	public void setFrom(Figure from) {
		this.from = from;
	}

	public Figure getTo() {
		return to;
	}

	public void setTo(Figure to) {
		this.to = to;
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

}
