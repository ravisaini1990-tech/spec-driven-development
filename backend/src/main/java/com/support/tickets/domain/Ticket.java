package com.support.tickets.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Priority priority = Priority.MEDIUM;

    @Column(length = 255)
    private String assignee;

    @Column(length = 255)
    private String category;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Produces a human-readable text representation of this ticket for RAG ingestion.
     * All key fields are included so semantic search can match on any of them.
     *
     * @param comments the ticket's comments in chronological order, used as the
     *                 resolution-notes/history portion of the document since there is
     *                 no separate resolution-notes field
     */
    public String toDocumentText(List<Comment> comments) {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(title).append("\n");
        sb.append("Description: ").append(description).append("\n");
        sb.append("Status: ").append(status != null ? status.name() : "OPEN").append("\n");
        sb.append("Priority: ").append(priority != null ? priority.name() : "MEDIUM").append("\n");
        sb.append("Assignee: ").append(assignee != null ? assignee : "Unassigned").append("\n");
        sb.append("Category: ").append(category != null ? category : "General");

        if (comments != null && !comments.isEmpty()) {
            sb.append("\nComments:");
            for (Comment comment : comments) {
                sb.append("\n- ").append(comment.getAuthor()).append(": ").append(comment.getBody());
            }
        }

        return sb.toString();
    }
}
