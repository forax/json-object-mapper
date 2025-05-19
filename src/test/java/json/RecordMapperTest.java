package json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.json.Json;
import java.util.json.JsonObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class RecordMapperTest {

  @Test
  @DisplayName("Should map a JsonObject to a simple record")
  public void testSimpleRecordMapping() {
    record SimpleRecord(String name, int age) {}

    var recordMapper = RecordMapper.of(MethodHandles.lookup());

    JsonObject json = (JsonObject) Json.fromUntyped(Map.of(
        "name", Json.fromUntyped("Alice"),
        "age", Json.fromUntyped(30)
    ));

    SimpleRecord result = recordMapper.fromTyped(json, SimpleRecord.class);

    assertNotNull(result);
    assertEquals("Alice", result.name());
    assertEquals(30, result.age());
  }

  @Test
  @DisplayName("Should match a JsonObject to a simple record")
  public void testSimpleRecordMatching() {
    record SimpleRecord(String name, int age) {}

    var recordMapper = RecordMapper.of(MethodHandles.lookup());

    JsonObject json = (JsonObject) Json.fromUntyped(Map.of(
        "name", Json.fromUntyped("Alice"),
        "age", Json.fromUntyped(30)
    ));

    if (recordMapper.match(json, SimpleRecord.class) instanceof SimpleRecord(String name, int age)) {
      assertEquals("Alice", name);
      assertEquals(30, age);
    } else {
      fail("Should match the record");
    }
  }

  @Test
  @DisplayName("Should map a JsonObject to a record with a long field")
  public void testLongIdRecordMapping() {
    record LongIdRecord(long id, String description) {}

    var recordMapper = RecordMapper.of(MethodHandles.lookup());

    JsonObject json = (JsonObject) Json.fromUntyped(Map.of(
        "id", Json.fromUntyped(1234567890123L),
        "description", Json.fromUntyped("A test item")
    ));

    LongIdRecord result = recordMapper.fromTyped(json, LongIdRecord.class);

    assertNotNull(result);
    assertEquals(1234567890123L, result.id());
    assertEquals("A test item", result.description());
  }


  @Test
  @DisplayName("Should map a JsonObject with nested JsonObject to a nested record")
  public void testNestedRecordMapping() {
    record AddressRecord(String street, String city) {}
    record PersonRecord(String name, int age, AddressRecord address) {}

    var recordMapper = RecordMapper.of(MethodHandles.lookup());

    JsonObject addressJson = (JsonObject) Json.fromUntyped(Map.of(
        "street", Json.fromUntyped("123 Main St"),
        "city", Json.fromUntyped("Anytown")
    ));
    JsonObject personJson = (JsonObject) Json.fromUntyped(Map.of(
        "name", Json.fromUntyped("Bob"),
        "age", Json.fromUntyped(25),
        "address", addressJson
    ));

    PersonRecord result = recordMapper.fromTyped(personJson, PersonRecord.class);

    assertNotNull(result);
    assertEquals("Bob", result.name());
    assertEquals(25, result.age());
    assertNotNull(result.address());
    assertEquals("123 Main St", result.address().street());
    assertEquals("Anytown", result.address().city());
  }

  @Test
  @DisplayName("Should throw UnsupportedOperationException for unhandled types in record components")
  public void testUnsupportedTypeInRecord() {
    record UnsupportedTypeRecord(String name, double salary) {}

    var recordMapper = RecordMapper.of(MethodHandles.lookup());

    JsonObject json = (JsonObject) Json.fromUntyped(Map.of(
        "name", Json.fromUntyped("Test"),
        "salary", Json.fromUntyped(50000.0) // double, which is not explicitly handled
    ));

    UnsupportedOperationException exception = assertThrows(
        UnsupportedOperationException.class,
        () -> recordMapper.fromTyped(json, UnsupportedTypeRecord.class)
    );
    assertTrue(exception.getMessage().contains("Unsupported type: double"));
  }

  @Test
  @DisplayName("Should throw exception if a required key (int) is missing")
  public void testMissingKeyForInt() {
    record SimpleRecord(String name, int age) {}

    var recordMapper = RecordMapper.of(MethodHandles.lookup());

    JsonObject json = (JsonObject) Json.fromUntyped(Map.of(
        "name", Json.fromUntyped("Charlie")
        // "age" is missing
    ));

    assertThrows(
        IllegalStateException.class,
        () -> recordMapper.fromTyped(json, SimpleRecord.class),
        "Expected an exception when a key is missing from JSON"
    );
  }

  @Test
  @DisplayName("Should throw exception if a required key (String) is missing")
  public void testMissingKeyForString() {
    record SimpleRecord(String name, int age) {}

    var recordMapper = RecordMapper.of(MethodHandles.lookup());
    JsonObject json = (JsonObject) Json.fromUntyped(Map.of(
        "age", Json.fromUntyped(40)
        // "name" is missing
    ));

    assertThrows(
        IllegalStateException.class,
        () -> recordMapper.fromTyped(json, SimpleRecord.class),
        "Expected an exception when a key is missing from JSON"
    );
  }

  @Test
  @DisplayName("Should map correctly when component names match JSON keys (standard case)")
  public void testProductRecordMapping() {
    record ProductRecord(String productId, String productName, int stockQuantity) {}

    var recordMapper = RecordMapper.of(MethodHandles.lookup());

    JsonObject json = (JsonObject) Json.fromUntyped(Map.of(
        "productId", Json.fromUntyped("P123"),
        "productName", Json.fromUntyped("Test Product"),
        "stockQuantity", Json.fromUntyped(100)
    ));

    ProductRecord result = recordMapper.fromTyped(json, ProductRecord.class);

    assertNotNull(result);
    assertEquals("P123", result.productId());
    assertEquals("Test Product", result.productName());
    assertEquals(100, result.stockQuantity());
  }

  @Test
  @DisplayName("Mapping should build decoder for each distinct local record class")
  public void testDecoderCachingWithLocalRecords() {
    record DataRecord(String data) {}

    var recordMapper = RecordMapper.of(MethodHandles.lookup());

    JsonObject json1 = (JsonObject) Json.fromUntyped(Map.of("data", Json.fromUntyped("Data1")));
    DataRecord dataRecord1 = recordMapper.fromTyped(json1, DataRecord.class);
    assertEquals("Data1", dataRecord1.data());

    JsonObject json2 = (JsonObject) Json.fromUntyped(Map.of("data", Json.fromUntyped("Data2")));
    DataRecord dataRecord2 = recordMapper.fromTyped(json2, DataRecord.class);
    assertEquals("Data2", dataRecord2.data());
  }

  @Test
  @DisplayName("Mapping a record with no components (empty record)")
  public void testEmptyRecord() {
    record EmptyRecord() {}

    var recordMapper = RecordMapper.of(MethodHandles.lookup());

    JsonObject json = (JsonObject) Json.fromUntyped(Map.of("someKey", Json.fromUntyped("someValue")));
    EmptyRecord result = recordMapper.fromTyped(json, EmptyRecord.class);
    assertNotNull(result); // Should successfully create an instance
  }
}
