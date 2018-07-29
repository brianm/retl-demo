package org.skife.retl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import net.jqwik.api.Example;
import org.assertj.core.util.Lists;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AirportTest {

    @Example
    public void csvParsing() throws Exception {
        ObjectMapper mapper = new CsvMapper();

        try (InputStream in = AirportTest.class.getResourceAsStream("/airports.csv")) {
            final Iterator<Airport> itty = mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                                 .readerFor(Airport.class)
                                                 .with(CsvSchema.builder().setUseHeader(true).build())
                                                 .readValues(in);
            while (itty.hasNext()) {
                Airport a = itty.next();
                assertThat(a).hasFieldOrProperty("name")
                             .hasFieldOrProperty("city")
                             .hasFieldOrProperty("country")
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

        assertThat(all).hasSize(5652);
        assertThat(all).allSatisfy(a -> assertThat(a).hasFieldOrProperty("name")
                                                     .hasFieldOrProperty("city")
                                                     .hasFieldOrProperty("country")
                                                     .hasFieldOrProperty("latitude")
                                                     .hasFieldOrProperty("longitude")
                                                     .hasFieldOrProperty("iata"));

        assertThat(all.stream().filter(a -> a.iata().equals("SEA")).findFirst()).isNotEmpty();
    }
}
