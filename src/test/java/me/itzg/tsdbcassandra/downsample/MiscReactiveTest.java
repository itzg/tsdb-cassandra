package me.itzg.tsdbcassandra.downsample;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import me.itzg.tsdbcassandra.downsample.TemporalNormalizer;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class MiscReactiveTest {

  @Test
  void windowingInstants() {
    final TemporalNormalizer normalizer = new TemporalNormalizer(Duration.ofMinutes(5));

    StepVerifier.create(
        Flux.just(
            Instant.parse("2007-12-03T10:02:32.08Z"),
            Instant.parse("2007-12-03T10:03:32.08Z"),
            // skip 10:10
            Instant.parse("2007-12-03T10:15:32.08Z"),
            Instant.parse("2007-12-03T10:16:32.08Z"),
            Instant.parse("2007-12-03T10:17:33.08Z"),
            Instant.parse("2007-12-03T10:22:34.08Z"),
            Instant.parse("2007-12-03T10:23:36.08Z")
        )
            .windowUntilChanged(instant -> instant.with(normalizer))
            .map(Flux::collectList)
            .concatMap(listMono -> listMono)
    )
        .expectNext(List.of(
            Instant.parse("2007-12-03T10:02:32.08Z"),
            Instant.parse("2007-12-03T10:03:32.08Z")
        ))
        .expectNext(List.of(
            Instant.parse("2007-12-03T10:15:32.08Z"),
            Instant.parse("2007-12-03T10:16:32.08Z"),
            Instant.parse("2007-12-03T10:17:33.08Z")
        ))
        .expectNext(List.of(
            Instant.parse("2007-12-03T10:22:34.08Z"),
            Instant.parse("2007-12-03T10:23:36.08Z")
        ))
        .verifyComplete();
  }

  /**
   * Confirm behavior similar to three granularities, such as 5m, 15m, 1h, of aggregation where
   * we want all of the aggregated values at each granularity. At the same time we want to
   * feed each granularity's aggregation into the next starting with the raw data.
   */
  @Test
  void chainAndSubscribeMultipleFlux() {
    final Flux<Integer> fluxSrc = Flux.just(
        1, 2,
        3, 4,
        5, 6,
        7, 8,
        9, 10,
        11, 12,
        13, 14,
        15, 16);
    final Flux<Integer> flux1 = windowAndSum(fluxSrc, 2);
    final Flux<Integer> flux2 = windowAndSum(flux1, 2);
    final Flux<Integer> flux3 = windowAndSum(flux2, 2);

    final Flux<Integer> merged = Flux.concat(flux1, flux2, flux3);

    StepVerifier.create(merged)
        .expectNext(
            3, 7,
            11, 15,
            19, 23,
            27, 31)
        .expectNext(
            10, 26,
            42, 58)
        .expectNext(36, 100)
        .verifyComplete();
  }

  /**
   * Perform a very simplified version of windowing and aggregation.
   */
  Flux<Integer> windowAndSum(Flux<Integer> flux, int windowSize) {
    return flux.window(windowSize)
        .concatMap(integerFlux -> integerFlux.reduce(Integer::sum));
  }
}
