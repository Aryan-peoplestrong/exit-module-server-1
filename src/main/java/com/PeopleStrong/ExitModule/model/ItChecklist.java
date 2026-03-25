package com.PeopleStrong.ExitModule.model;

import com.PeopleStrong.ExitModule.model.enums.ChecklistStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "it_checklists")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItChecklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long checklistId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private ExitRequest exitRequest;

    @Column(nullable = false)
    private boolean idCardReceived;

    @Column(nullable = false)
    private boolean accessCardReceived;

    @Column(nullable = false)
    private boolean laptopReceived;

    private String documentPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChecklistStatus status;

    @Column(nullable = false)
    private int iteration;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
