package json;

// Benchmark                        Mode  Cnt   Score   Error  Units
// RecordMapperBench.mappingSimple  avgt    5  15,057 ± 0,050  ns/op
// RecordMapperBench.recordMapper   avgt    5  16,997 ± 0,044  ns/op

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.json.*;

// $JAVA_HOME/bin/java -jar target/benchmarks.jar -prof dtraceasm
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = { "--enable-preview" })
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class RecordMapperBench {
  public record Person(String name, int age) {}

  private static final RecordMapper RECORD_MAPPER = RecordMapper.of(MethodHandles.lookup());

  private JsonObject object = (JsonObject) Json.fromUntyped(Map.of(
      "name", Json.fromUntyped("John"),
      "age", Json.fromUntyped(32)
    ));

  @Benchmark
  public Person mappingSimple() {
    var members = object.members();
    var nameValue = (JsonString) members.get("name");
    var name = nameValue.value();
    var ageValue = (JsonNumber) members.get("age");
    var age = (int) (long) (Long) ageValue.toNumber();
    return new Person(name, age);
  }

  //@Benchmark
  public Person mappingInstanceof() {
    return object.members() instanceof Map<String, JsonValue> map &&
        map.get("name") instanceof JsonString jsonString && jsonString.value() instanceof String name &&
        map.get("age") instanceof JsonNumber jsonNumber && jsonNumber.toNumber() instanceof Long age
        ? new Person(name, (int) (long) age) : null;
  }

  @Benchmark
  public Person recordMapper() {
    return RECORD_MAPPER.fromTyped(object, Person.class);
  }
}

