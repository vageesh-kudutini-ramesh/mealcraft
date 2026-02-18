package com.mealcraft.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Tracks which notifications the user has dismissed.
 * Used to hide individual notifications (e.g. per pantry item) or aggregate alerts.
 */
@Entity
@Table(name = "dismissed_notifications", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "notification_type", "reference_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DismissedNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "notification_type", nullable = false, length = 60)
    private String notificationType;

    /** Reference for this notification (e.g. pantry_item_id, "meal_plan_tomorrow", "streak") */
    @Column(name = "reference_id", nullable = false, length = 120)
    private String referenceId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime dismissedAt;
}
