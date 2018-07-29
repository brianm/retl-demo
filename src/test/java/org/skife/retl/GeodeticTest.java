package org.skife.retl;

import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Point;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.assertj.core.data.Offset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skife.retl.Geodetic.Earth;

public class GeodeticTest {

    private static final double EARTH_CIRCUMFERENCE_KM = 40075;

    @Example
    public void spotCheck() {
        final Point marriotWhitefield = Geometries.pointGeographic(77.7277, 12.9796);
        final Point seattleOffice = Geometries.pointGeographic(-122.3381, 47.6071);

        double distance = Earth.distance(seattleOffice, marriotWhitefield);
        assertThat(distance).isCloseTo(12990, Offset.offset(1.0));
    }

    @Property
    public void distanceAlwaysLessThanOrEqualToHalfCircumference(@ForAll Point from,
                                                                 @ForAll Point to) {
        assertThat(Earth.distance(from, to)).isLessThanOrEqualTo(EARTH_CIRCUMFERENCE_KM)
                                            .isPositive();
    }
}
