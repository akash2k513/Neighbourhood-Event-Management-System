package com.neighborhood.eventmanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_events_status",     columnList = "status"),
    @Index(name = "idx_events_organizer",  columnList = "organizer_id"),
    @Index(name = "idx_events_zone",       columnList = "zone_id"),
    @Index(name = "idx_events_start_time", columnList = "start_time")
})
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // ==========================
    // BASIC DETAILS
    // ==========================

    @NotBlank(message = "Event title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    @Column(nullable = false)
    private String title;


    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    @Column(length = 2000)
    private String description;


    @NotNull(message = "Category is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCategory category;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.DRAFT;



    // ==========================
    // DATE & TIME
    // ==========================

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    @Column(nullable = false)
    private LocalDateTime startTime;


    @NotNull(message = "End time is required")
    @Column(nullable = false)
    private LocalDateTime endTime;



    // ==========================
    // CAPACITY
    // ==========================

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be greater than zero")
    @Column(nullable = false)
    private Integer capacity;


    @Column(nullable = false)
    private Integer registeredCount = 0;



    // ==========================
    // LOCATION
    // ==========================

    @NotBlank(message = "Location is required")
    @Column(nullable = false)
    private String location;



    // ==========================
    // RECURRING EVENT
    // ==========================

    @Column(nullable = false)
    private Boolean recurring = false;


    private String recurrenceType;


    private LocalDate recurrenceEndDate;



    // ==========================
    // RELATIONSHIPS
    // ==========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id")
    private User organizer;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;



    // ==========================
    // AUDIT FIELDS
    // ==========================

    @Column(nullable = false)
    private LocalDateTime createdAt;


    private LocalDateTime updatedAt;



    public Event() {
    }



    @PrePersist
    public void onCreate() {

        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (registeredCount == null) {
            registeredCount = 0;
        }
    }



    @PreUpdate
    public void onUpdate() {

        updatedAt = LocalDateTime.now();
    }



    // ==========================
    // GETTERS & SETTERS
    // ==========================


    public Long getId() {
        return id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public String getTitle() {
        return title;
    }


    public void setTitle(String title) {
        this.title = title;
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public EventCategory getCategory() {
        return category;
    }


    public void setCategory(EventCategory category) {
        this.category = category;
    }


    public EventStatus getStatus() {
        return status;
    }


    public void setStatus(EventStatus status) {
        this.status = status;
    }


    public LocalDateTime getStartTime() {
        return startTime;
    }


    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }


    public LocalDateTime getEndTime() {
        return endTime;
    }


    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }


    public Integer getCapacity() {
        return capacity;
    }


    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }


    public Integer getRegisteredCount() {
        return registeredCount;
    }


    public void setRegisteredCount(Integer registeredCount) {
        this.registeredCount = registeredCount;
    }


    public String getLocation() {
        return location;
    }


    public void setLocation(String location) {
        this.location = location;
    }


    public Boolean getRecurring() {
        return recurring;
    }


    public void setRecurring(Boolean recurring) {
        this.recurring = recurring;
    }


    public String getRecurrenceType() {
        return recurrenceType;
    }


    public void setRecurrenceType(String recurrenceType) {
        this.recurrenceType = recurrenceType;
    }


    public LocalDate getRecurrenceEndDate() {
        return recurrenceEndDate;
    }


    public void setRecurrenceEndDate(LocalDate recurrenceEndDate) {
        this.recurrenceEndDate = recurrenceEndDate;
    }


    public User getOrganizer() {
        return organizer;
    }


    public void setOrganizer(User organizer) {
        this.organizer = organizer;
    }


    public Zone getZone() {
        return zone;
    }


    public void setZone(Zone zone) {
        this.zone = zone;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}