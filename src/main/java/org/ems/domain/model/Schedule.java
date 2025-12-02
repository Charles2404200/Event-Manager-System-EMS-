package org.ems.domain.model;

import java.time.LocalDateTime;

public class Schedule {

    public static boolean overlaps(LocalDateTime aStart, LocalDateTime aEnd,
                                   LocalDateTime bStart, LocalDateTime bEnd) {

        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }
}
