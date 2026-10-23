package com.support.tickets.repository;

import com.support.tickets.domain.Ticket;
import com.support.tickets.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatus(TicketStatus status);

    @Query("SELECT t FROM Ticket t WHERE " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Ticket> searchByTitleOrDescription(@Param("search") String search);

    @Query("SELECT t FROM Ticket t WHERE " +
           "t.status = :status AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Ticket> searchByTitleOrDescriptionAndStatus(
            @Param("search") String search,
            @Param("status") TicketStatus status);
}
