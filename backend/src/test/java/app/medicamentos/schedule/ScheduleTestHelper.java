package app.medicamentos.schedule;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.UUID;

@ApplicationScoped
public class ScheduleTestHelper {
    @Transactional
    public void setTakenCount(UUID id, int count) {
        TreatmentScheduleEntity s = TreatmentScheduleEntity.findById(id);
        s.takenCount = count;
    }
}
