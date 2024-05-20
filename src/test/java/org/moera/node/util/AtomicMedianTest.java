package org.moera.node.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AtomicMedianTest {

    @Test
    void medianCalculation() {
        AtomicMedian median = new AtomicMedian(5, 27, 5);
        median.add(0); // 5
        median.add(11); // 10
        median.add(15); // 15
        median.add(17); // 15
        median.add(22); // 20
        median.add(31); // 25
        Assertions.assertEquals(median.getMedian(), 17);
    }

}
