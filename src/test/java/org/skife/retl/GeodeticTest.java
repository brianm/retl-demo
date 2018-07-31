package org.skife.retl;

import com.github.davidmoten.rtree.geometry.Point;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.assertj.core.api.Condition;
import org.assertj.core.data.Offset;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.skife.retl.Geodetic.Earth;

public class GeodeticTest {

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

    @Example
    public void stuff() {
        Point b = Geodetic.latLong(0.9796, 280.7277);
        Point s = Geodetic.latLong(90, -179.3381);

        assertThat(Earth.distance(b, s)).isLessThanOrEqualTo(Earth.circumference() / 2);
    }

    @Property
    public void badLatLongExplodes(@ForAll float lat, @ForAll float lon) {
        assumeThat(lat).isNot(condition(f -> f >= -90 && f <= 90, "lat not in -90,90"));
        assumeThat(lon).isNot(condition(f -> f >= -180 && f <= 180, "lon not in -180,180"));
        assertThatThrownBy(() -> Geodetic.latLong(lat, lon)).isInstanceOf(IllegalArgumentException.class);
    }

    public static <T> Condition<T> condition(Predicate<T> p, String desc) {
        return new Condition<>(p, desc);
    }
}
