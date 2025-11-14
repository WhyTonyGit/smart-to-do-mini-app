package com.smarttodo.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "chatId", "displayName"})
@Entity
@Table(name = "users")
public class UserEntity {
    @Id //Сюда кладем userId, так как он уникален
    @NonNull
    private Long id;

    @Column(name = "chat_id", nullable = false, unique = true, updatable = false)
    @NonNull
    private final Long chatId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskEntity> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HabitEntity> habits = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}