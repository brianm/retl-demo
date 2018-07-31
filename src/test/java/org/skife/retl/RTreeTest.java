package org.skife.retl;

import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.internal.EntryDefault;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.assertj.core.util.Lists;

import java.util.List;
import java.util.SortedSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skife.retl.Geodetic.Earth;

public class RTreeTest {

    private static final Point SEA_OFFICE = Geodetic.latLong(47.607148, -122.3381338);
    private static final Point BLR_OFFICE = Geodetic.latLong(12.9796, 77.7277);


    private static final List<Airport> AIRPORTS = ImmutableList.copyOf(Airport.loadKnownAirports()
                                                                              .toBlocking()
                                                                              .getIterator());

    private static final RTree<Airport, Point> INDEX = RTree.<Airport, Point>create()
            .add(AIRPORTS.stream()
                         .map(a -> EntryDefault.entry(a, a.point()))
                         .collect(Collectors.toList()));

    @Example
    public void seattle() {

        Airport nearest = INDEX.nearest(SEA_OFFICE, 1, 1)
                               .toBlocking()
                               .first()
                               .value();

        assertThat(nearest).hasFieldOrPropertyWithValue("iata", "BFI");
    }

    @Example
    public void bengaluru() {
        Airport nearest = INDEX.nearest(BLR_OFFICE, 1, 1)
                               .toBlocking()
                               .first()
                               .value();

        assertThat(nearest).hasFieldOrPropertyWithValue("iata", "BLR");
    }

    @Property
    public void propertyIndexFindsActualClosest(@ForAll Point p) {
        int count = 1;
        int maxDistance = 1000;
        List<Airport> nearest = Lists.newArrayList(INDEX.nearest(p, maxDistance, count)
                                                        .map(Entry::value)
                                                        .toBlocking()
                                                        .toIterable());

        assertThat(nearest).isEqualTo(bruteForceHaversineNearest(p, maxDistance, count));
    }

    @Property
    public void propertyIndexFindsActualClosest2(@ForAll Point p) {
        int count = 1;
        int maxDistance = 1000;
        List<Airport> nearest = Lists.newArrayList(INDEX.nearest(p, maxDistance, count)
                                                        .map(Entry::value)
                                                        .toBlocking()
                                                        .toIterable());
        List<Airport> expected = bruteForceHaversineNearest(p, maxDistance, count);
        assertThat(nearest).isEqualTo(expected);
    }


    @Example
    public void seattle2() {
        List<Airport> nearest = Lists.newArrayList(INDEX.nearest(SEA_OFFICE, 100, 2)
                                                        .map(Entry::value)
                                                        .toBlocking()
                                                        .toIterable());

        nearest.forEach(a -> System.out.printf("%s\th=%f\td=%f\n",
                                               a.iata(),
                                               Earth.distance(SEA_OFFICE, a.point()),
                                               SEA_OFFICE.distance(a.point())));
        System.out.println();
        List<Airport> expected = bruteForceHaversineNearest(SEA_OFFICE, 100, 2);
        expected.forEach(a -> System.out.printf("%s\th=%f\td=%f\n",
                                                a.iata(),
                                                Earth.distance(SEA_OFFICE, a.point()),
                                                SEA_OFFICE.distance(a.point())));

        assertThat(nearest).isEqualTo(expected);
    }

    @Example
    public void seattleNaive() throws Exception {

        List<Airport> nearest = Lists.newArrayList(INDEX.nearest(SEA_OFFICE, 100, 2)
                                                        .map(Entry::value)
                                                        .toBlocking()
                                                        .toIterable());

        nearest.forEach(a -> System.out.printf("AIRPORT_IDX\t%s\th=%f\td=%f\n",
                                               a.iata(),
                                               Earth.distance(SEA_OFFICE, a.point()),
                                               SEA_OFFICE.distance(a.point())));
        List<Airport> naive = bruteForceNaiveNearest(SEA_OFFICE, 100, 2);
        naive.forEach(a -> System.out.printf("brute\t%s\th=%f\td=%f\n",
                                             a.iata(),
                                             Earth.distance(SEA_OFFICE, a.point()),
                                             SEA_OFFICE.distance(a.point())));

        assertThat(nearest).isEqualTo(naive);
    }

    @Example
    public void exampleBruteNearestSeattle() {
        List<Airport> rs = bruteForceHaversineNearest(SEA_OFFICE, 100, 3);
        rs.forEach(a -> System.out.printf("%s\th=%f\td=%f\n",
                                          a.iata(),
                                          Earth.distance(SEA_OFFICE, a.point()),
                                          SEA_OFFICE.distance(a.point())));
    }

    @Example
    public void bruteForceBlr() {
        List<Airport> nearest = Lists.newArrayList(INDEX.nearest(BLR_OFFICE, 100, 3)
                                                        .map(Entry::value)
                                                        .toBlocking()
                                                        .toIterable());

        List<Airport> rs = bruteForceHaversineNearest(BLR_OFFICE, (int) Earth.circumference(), 3);
        assertThat(nearest).isEqualTo(rs);
        rs.forEach(a -> System.out.printf("%s\th=%f\td=%f\n",
                                          a.iata(),
                                          Earth.distance(BLR_OFFICE, a.point()),
                                          BLR_OFFICE.distance(a.point())));
    }

    private List<Airport> bruteForceHaversineNearest(final Point p, int maxDistance, int count) {
        final SortedSet<Airport> all = Sets.newTreeSet((first, second) -> {
            double d1 = Earth.distance(p, Geodetic.latLong(first.latitude(), first.longitude()));
            double d2 = Earth.distance(p, Geodetic.latLong(second.latitude(), second.longitude()));
            return Double.compare(d1, d2);
        });
        all.addAll(AIRPORTS);

        return all.stream()
                  .filter(a -> Earth.distance(p, a.point()) < maxDistance)
                  .limit(count)
                  .collect(Collectors.toList());
    }

    private List<Airport> bruteForceNearest(final Point p,
                                            int maxDistance,
                                            int count,
                                            BiFunction<Point, Point, Double> distanceFunction) {
        final SortedSet<Airport> all = Sets.newTreeSet((first, second) -> {
            double d1 = distanceFunction.apply(p, first.point());
            double d2 = distanceFunction.apply(p, second.point());
            return Double.compare(d1, d2);
        });
        all.addAll(AIRPORTS);
        return all.stream()
                  .filter(a -> distanceFunction.apply(p, a.point()) < maxDistance)
                  .limit(count)
                  .collect(Collectors.toList());
    }

    private List<Airport> bruteForceNaiveNearest(final Point p, int maxDistance, int count) {
        final SortedSet<Airport> all = Sets.newTreeSet((first, second) -> {
            double d1 = p.distance(Geodetic.latLong(first.latitude(), first.longitude()));
            double d2 = p.distance(Geodetic.latLong(second.latitude(), second.longitude()));
            return Double.compare(d1, d2);
        });
        all.addAll(AIRPORTS);

        return all.stream()
                  .filter(a -> p.distance(a.point()) < maxDistance)
                  .limit(count)
                  .collect(Collectors.toList());
    }


    RTree<Airport, Point> AIRPORT_IDX = RTree.<Airport, Point>create()
            .add(AIRPORTS.stream()
                         .map(a -> EntryDefault.entry(a, a.point()))
                         .collect(Collectors.toList()));

    @Example
    public void exampleNearBengaluru() throws Exception {
        double maxDistance = 10;
        int resultSize = 1;
        Point b = Geodetic.latLong(12.9796, 77.7277);

        Airport blr = AIRPORT_IDX.nearest(b, maxDistance, resultSize)
                                 .toBlocking()
                                 .first()
                                 .value();
        assertThat(blr.iata()).isEqualTo("BLR");
    }

    @Example
    public void exampleNearSeattle() {

        Point sea = Geodetic.latLong(47.607148, -122.3381338);

        Airport bfi = AIRPORT_IDX.nearest(sea, 10, 1)
                                 .toBlocking()
                                 .first()
                                 .value();

        assertThat(bfi.iata()).isEqualTo("BFI");
    }

    @Property
    public void nearestMatchesHaversine(@ForAll Point p) {
        Airport bfi = AIRPORT_IDX.nearest(p, 360, 1)
                                 .toBlocking()
                                 .first()
                                 .value();
        Airport expected = haversineNearest(p, AIRPORTS);
        assertThat(bfi).isEqualTo(expected);
    }

    private static Airport haversineNearest(Point p, List<Airport> airports) {
        final SortedSet<Airport> all = Sets.newTreeSet((first, second) -> {
            double d1 = Earth.distance(p, Geodetic.latLong(first.latitude(),
                                                           first.longitude()));
            double d2 = Earth.distance(p, Geodetic.latLong(second.latitude(),
                                                           second.longitude()));
            return Double.compare(d1, d2);
        });
        all.addAll(airports);

        return all.iterator().next();
    }
}
