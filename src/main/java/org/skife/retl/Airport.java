package org.skife.retl;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Point;
import com.google.common.io.Resources;
import io.vavr.Tuple2;
import org.immutables.value.Value;
import org.tukaani.xz.XZInputStream;
import rx.Observable;
import rx.Observer;
import rx.observables.SyncOnSubscribe;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Value.Immutable
@JsonSerialize(as = ImmutableAirport.class)
@JsonDeserialize(as = ImmutableAirport.class)
public interface Airport {

    String name();
    String iata();
    String city();
    String icao();

    float latitude();
    float longitude();

    default Point point() {
        return Geometries.pointGeographic(longitude(), latitude());
    }

    static Observable<Airport> loadKnownAirports() {
        return Observable.create(new SyncOnSubscribe<Tuple2<AutoCloseable, Iterator<Airport>>, Airport>() {
            @Override
            protected Tuple2<AutoCloseable, Iterator<Airport>> generateState() {
                try {
                    CsvSchema schema = CsvSchema.builder()
                                                .setEscapeChar('\\')
                                                .addColumn("airportId")
                                                .addColumn("name")
                                                .addColumn("city")
                                                .addColumn("country")
                                                .addColumn("iata")
                                                .addColumn("icao")
                                                .addColumn("latitude")
                                                .addColumn("longitude")
                                                .addColumn("altitude")
                                                .addColumn("timezoneOffset")
                                                .addColumn("dst")
                                                .addColumn("timezoneName")
                                                .addColumn("type")
                                                .addColumn("source")
                                                .build();

                    ObjectReader mapper = new CsvMapper().disable(FAIL_ON_UNKNOWN_PROPERTIES)
                                                         .readerFor(Airport.class)
                                                         .with(schema);

                    InputStream in = new XZInputStream(Resources.getResource("airports.dat.xz")
                                                                .openStream());
                    Iterator<Airport> itty = mapper.readValues(in);
                    return new Tuple2<>(in, itty);

                } catch (IOException e) {
                    throw new IllegalStateException("unable to loadKnownAirports airports.dat.xz from classpath", e);
                }
            }

            @Override
            protected Tuple2<AutoCloseable, Iterator<Airport>> next(Tuple2<AutoCloseable, Iterator<Airport>> state,
                                                                    Observer<? super Airport> observer) {
                if (state._2.hasNext()) {
                    observer.onNext(state._2.next());
                }
                else {
                    observer.onCompleted();
                }
                return state;
            }

            @Override
            protected void onUnsubscribe(Tuple2<AutoCloseable, Iterator<Airport>> state) {
                try {
                    state._1.close();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }).filter(a -> a.iata().length() == 3); // strip non-iata airports
    }

}
