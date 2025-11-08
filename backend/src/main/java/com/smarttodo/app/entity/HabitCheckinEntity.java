package com.smarttodo.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "day"})
@Entity
@Table(name = "habit_checkins", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"habit_id", "day"}, name = "uk_habit_day")
})
public class HabitCheckinEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_checkin_habit"))
    @NonNull
    private final HabitEntity habit;

    @Column(name = "day", nullable = false)
    @NonNull
    private final LocalDate day;
}