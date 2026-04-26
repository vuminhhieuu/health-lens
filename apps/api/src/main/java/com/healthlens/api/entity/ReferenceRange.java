package com.healthlens.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "reference_ranges")
public class ReferenceRange {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "metric_id", nullable = false)
    private ReferenceMetric metric;

    @Column(name = "min_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal minValue;

    @Column(name = "max_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal maxValue;

    @Column(name = "attention_min", nullable = false, precision = 10, scale = 4)
    private BigDecimal attentionMin;

    @Column(name = "attention_max", nullable = false, precision = 10, scale = 4)
    private BigDecimal attentionMax;

    @Column(length = 10)
    private String gender;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(nullable = false, length = 20)
    private String status;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = "active";
        }
    }
}
