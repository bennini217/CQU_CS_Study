package com.rbac.model;

/**
 * Permission (权限) 实体类
 * 映射数据库中的 "permission" 表
 */
public class Permission {

    private int id;
    private String name;        // 权限的唯一标识 (e.g., "PERM_USER_DELETE")
    private String description;

    public Permission() {
    }

    public Permission(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // --- Getters and Setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Permission{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}