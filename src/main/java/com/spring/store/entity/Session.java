package com.spring.store.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    @Id
    private String sessionId;
    @ManyToOne
    @JoinColumn(name = "personId")
    private Person person;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;

}
