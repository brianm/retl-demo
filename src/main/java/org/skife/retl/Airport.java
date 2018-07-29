package org.skife.retl;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.github.davidmoten.rtree.geometry.Point;
import io.vavr.Tuple2;
import org.immutables.value.Value;
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

    String country();

    float latitude();

    float longitude();

    default Point point() {
        return Geodetic.latLong(latitude(), longitude());
    }

    static Observable<Airport> loadKnownAirports() {
        return Observable.create(new SyncOnSubscribe<Tuple2<AutoCloseable, Iterator<Airport>>, Airport>() {
            @Override
            protected Tuple2<AutoCloseable, Iterator<Airport>> generateState() {
                try {
                    CsvSchema schema = CsvSchema.builder()
                                                .setUseHeader(true)
                                                .build();

                    ObjectReader mapper = new CsvMapper().disable(FAIL_ON_UNKNOWN_PROPERTIES)
                                                         .readerFor(Airport.class)
                                                         .with(schema);

                    InputStream in = Airport.class.getResourceAsStream("/airports.csv");
                    Iterator<Airport> itty = mapper.readValues(in);
                    return new Tuple2<>(in, itty);

                } catch (IOException e) {
                    throw new IllegalStateException("unable to loadKnownAirports airports.csv from classpath", e);
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
        });
    }

}
