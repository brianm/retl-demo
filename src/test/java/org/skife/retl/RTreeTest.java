package org.skife.retl;

import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.internal.EntryDefault;
import com.google.common.collect.Sets;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.assertj.core.util.Lists;

import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class RTreeTest {

    private static final Point SEA_OFFICE = Geometries.pointGeographic(-122.3381338, 47.607148);
    private static final Point BLR_OFFICE = Geometries.pointGeographic(77.7277, 12.9796 );


    private static final List<Airport> AIRPORTS = Lists.newArrayList(Airport.loadKnownAirports()
                                                                            .toBlocking()
                                                                            .getIterator());

    private static final RTree<Airport, Point> INDEX = RTree.<Airport, Point>create()
            .add(AIRPORTS.stream()
                         .map(a -> EntryDefault.entry(a, a.point()))
                         .collect(Collectors.toList()));

    @Example
    public void example() {

        Airport nearest = INDEX.nearest(SEA_OFFICE, 360, 1)
                               .toBlocking()
                               .first()
                               .value();

        assertThat(nearest).isNotNull()
                           .hasFieldOrPropertyWithValue("iata", "BFI");
    }

    @Property
    public void propertyIndexFindsActualClosest(@ForAll Point p) {
        int count = 10;
        List<Airport> nearest = Lists.newArrayList(INDEX.nearest(p, 360, count)
                                                        .map(Entry::value)
                                                        .toBlocking()
                                                        .toIterable());

        assertThat(nearest).isEqualTo(bruteForceNearest(p, count));
    }


    @Example
    public void seattle() throws Exception {

        List<Airport> nearest = Lists.newArrayList(INDEX.nearest(SEA_OFFICE, 360, 2)
                                                        .map(Entry::value)
                                                        .toBlocking()
                                                        .toIterable());

        assertThat(nearest).isEqualTo(bruteForceNearest(SEA_OFFICE, 2));
    }

    @Example
    public void exampleBruteNearestSeattle() {
        List<Airport> rs = bruteForceNearest(SEA_OFFICE, 3);
        rs.forEach(a -> System.out.printf("%s\th=%f\td=%f\n",
                                          a.iata(),
                                          Geo.haversineDistance(SEA_OFFICE, a.point()),
                                          SEA_OFFICE.distance(a.point())));
    }

    @Example
    public void bruteForceBlr() {
        List<Airport> nearest = Lists.newArrayList(INDEX.nearest(BLR_OFFICE, 360, 3)
                                                        .map(Entry::value)
                                                        .toBlocking()
                                                        .toIterable());

        List<Airport> rs = bruteForceNearest(BLR_OFFICE, 3);
        assertThat(nearest).isEqualTo(rs);
        rs.forEach(a -> System.out.printf("%s\th=%f\td=%f\n",
                                          a.iata(),
                                          Geo.haversineDistance(BLR_OFFICE, a.point()),
                                          BLR_OFFICE.distance(a.point())));
    }

    private List<Airport> bruteForceNearest(final Point p, int count) {
        final SortedSet<Airport> all = Sets.newTreeSet((first, second) -> {
            double d1 = Geo.haversineDistance(p, Geometries.pointGeographic(first.longitude(), first.latitude()));
            double d2 = Geo.haversineDistance(p, Geometries.pointGeographic(second.longitude(), second.latitude()));
            return (int) (d1 - d2);
        });
        all.addAll(AIRPORTS);

        return all.stream()
                  .limit(count)
                  .collect(Collectors.toList());
    }
}
