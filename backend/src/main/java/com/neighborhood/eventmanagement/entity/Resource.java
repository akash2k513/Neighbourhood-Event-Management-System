package com.neighborhood.eventmanagement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "resources")
public class Resource {

    public enum ResourceType { EQUIPMENT, FACILITY, SERVICE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType type;

    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    public Resource() {}

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ResourceType getType() { return type; }
    public void setType(ResourceType type) { this.type = type; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Venue getVenue() { return venue; }
    public void setVenue(Venue venue) { this.venue = venue; }
}