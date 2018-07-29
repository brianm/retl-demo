package org.skife.retl;

import com.github.davidmoten.rtree.geometry.Point;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.assertj.core.data.Offset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skife.retl.Geodetic.Earth;

public class StepOneTest {

    @Example
    public void spotCheck() {
        final Point bengaluru = Geodetic.latLong(12.9796, 77.7277);
        final Point seattle = Geodetic.latLong(47.6071, -122.3381);

        double distance = Earth.distance(seattle, bengaluru);
        assertThat(distance).isCloseTo(12990, Offset.offset(1.0));
    }

    @Property
    public void nothingFurtherThanHalfWorldAway(@ForAll Point from,
                                                @ForAll Point to) {
        double distance = Earth.distance(from, to);
        double halfWorldAway = Earth.circumference() / 2;

        assertThat(distance).isLessThanOrEqualTo(halfWorldAway)
                            .isPositive();
    }
}
