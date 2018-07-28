package org.skife.retl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.io.Resources;
import net.jqwik.api.Example;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.tukaani.xz.XZInputStream;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AirportTest {

    @Test
    public void csvParsing() throws Exception {
        CsvSchema schema = CsvSchema.builder()
                                    .setEscapeChar('\\')
                                    .addColumn("airportId")
                                    .addColumn("name")
                                    .addColumn("city")
                                    .addColumn("country")
                                    .addColumn("iata")
                                    .addColumn("icao")
                                    .addNumberColumn("latitude")
                                    .addNumberColumn("longitude")
                                    .addNumberColumn("altitude")
                                    .addColumn("timezoneOffset")
                                    .addColumn("dst")
                                    .addColumn("timezoneName")
                                    .addColumn("type")
                                    .addColumn("source")
                                    .build();

        ObjectMapper mapper = new CsvMapper();
        try (InputStream in = new XZInputStream(Resources.getResource("airports.dat.xz").openStream())) {
            final Iterator<Airport> itty = mapper.enable(JsonParser.Feature.ALLOW_MISSING_VALUES)
                                                 .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                                 .readerFor(Airport.class)
                                                 .with(schema)
                                                 .readValues(in);
            while (itty.hasNext()) {
                Airport a = itty.next();
                assertThat(a).hasFieldOrProperty("name")
                             .hasFieldOrProperty("icao")
                             .hasFieldOrProperty("city")
                             .hasFieldOrProperty("latitude")
                             .hasFieldOrProperty("longitude")
                             .hasFieldOrProperty("iata");
            }

        }
    }

    @Example
    public void loadKnownAirports() {
        List<Airport> all = Lists.newArrayList(Airport.loadKnownAirports()
                                                      .toBlocking()
                                                      .getIterator());

        assertThat(all).hasSize(7184);
        assertThat(all).allSatisfy(a -> assertThat(a).hasFieldOrProperty("name")
                                                     .hasFieldOrProperty("icao")
                                                     .hasFieldOrProperty("city")
                                                     .hasFieldOrProperty("latitude")
                                                     .hasFieldOrProperty("longitude")
                                                     .hasFieldOrProperty("iata"));

        assertThat(all.stream().filter(a -> a.iata().equals("SEA")).findFirst()).isNotEmpty();
    }
}
