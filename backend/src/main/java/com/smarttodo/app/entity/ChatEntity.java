package com.smarttodo.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "chats")
public class ChatEntity {
    @Id
    private Long id;

    @Column(name = "state")
    private ChatState state
}
