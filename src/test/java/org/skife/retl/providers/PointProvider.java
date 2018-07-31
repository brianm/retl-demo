package org.skife.retl.providers;

import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Point;
import com.google.auto.service.AutoService;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.arbitraries.FloatArbitrary;
import net.jqwik.api.providers.ArbitraryProvider;
import net.jqwik.api.providers.TypeUsage;
import org.skife.retl.Geodetic;

import java.util.Collections;
import java.util.Set;

@AutoService(ArbitraryProvider.class)
public class PointProvider implements ArbitraryProvider {

    @Override
    public boolean canProvideFor(TypeUsage targetType) {
        return targetType.isOfType(Point.class);
    }

    @Override
    public Set<Arbitrary<?>> provideFor(TypeUsage _t, SubtypeProvider _s) {
        FloatArbitrary lats = Arbitraries.floats().between(-90f, 90f);
        FloatArbitrary lons = Arbitraries.floats().between(-180f, 180f);
        return Collections.singleton(Combinators.combine(lats, lons)
                                                .as(Geodetic::latLong));
    }
}
