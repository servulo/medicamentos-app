package app.medicamentos.medication;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "medications")
public class MedicationEntity extends PanacheEntityBase {
    @Id public UUID id;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(nullable = false, length = 120) public String name;
    @Column(nullable = false) public String unit;
    @Column(name = "stock_quantity", nullable = false) public int stockQuantity;
    @Column(name = "purchase_threshold_units", nullable = false) public int purchaseThresholdUnits;
    @Column(name = "created_at", nullable = false) public OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) public OffsetDateTime updatedAt;

    public boolean purchaseNeeded() {
        return stockQuantity <= purchaseThresholdUnits;
    }
}
