package json;

import netscape.javascript.JSObject;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.json.JsonObject;

/**
 * The {@code RecordMapper} interface defines a contract for mapping {@link JsonObject}
 * instances to Java {@link Record} instances.
 * <p>
 * The mapping process relies on matching keys in the {@code JsonObject} to the
 * component names of the target record. It supports basic data types and nested
 * records.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><b>Type-Safe Mapping:</b> Converts {@code JsonObject} to a specific record class.</li>
 *   <li><b>Nested Record Support:</b> Capable of mapping JSON objects that contain
 *       nested JSON objects to records with components that are themselves records.</li>
 *   <li><b>Mandatory Fields:</b> By default, all record components are considered
 *       mandatory. If a corresponding key is missing in the {@code JsonObject},
 *       an {@link IllegalStateException} is thrown during mapping.</li>
 * </ul>
 *
 * <h3>Supported Data Types for Record Components:</h3>
 * Based on the provided tests, the mapper implementation directly supports:
 * <ul>
 *   <li>{@code String} (from JSON string)</li>
 *   <li>{@code int} (from JSON number)</li>
 *   <li>{@code long} (from JSON number)</li>
 *   <li>Other {@code Record} types (from nested JSON objects)</li>
 *   <li>TODO add more types</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Define a simple record
 * record UserRecord(String name, int age) {}
 *
 * // Obtain a RecordMapper instance (it must be a constant)
 * static final RecordMapper MAPPER = RecordMapper.of(MethodHandles.lookup());
 *
 * // Create a JsonObject (example using a hypothetical Json.fromUntyped)
 * JsonObject userJson = (JsonObject) Json.fromUntyped(Map.of(
 *     "name", Json.fromUntyped("Alice"),
 *     "age", Json.fromUntyped(30)
 * ));
 *
 * // Map the JsonObject to the UserRecord
 * UserRecord user = MAPPER.fromTyped(userJson, UserRecord.class);
 * }</pre>
 */
public interface RecordMapper {

  /**
   * Factory method to obtain an instance of {@code RecordMapper}.
   * <p>
   * The provided {@link MethodHandles.Lookup} encapsulates the access
   * rights of the caller, allowing the mapper to operate on non-public records.
   * </p>
   *
   * @param lookup A {@link MethodHandles.Lookup} instance that provides the
   *               context for reflective operations, for accessing
   *               record constructors. Must not be null.
   * @return A new instance of {@code RecordMapper}.
   * @throws NullPointerException if {@code lookup} is null.
   */
  static RecordMapper of(MethodHandles.Lookup lookup) {
    Objects.requireNonNull(lookup, "lookup is null");
    return RecordMapperImpl.of(lookup);
  }

  /**
   * Maps the given {@link JsonObject} to an instance of the specified record class.
   * <p>
   * The mapping process involves:
   * <ol>
   *   <li>Retrieving the record components of the {@code recordClass}.</li>
   *   <li>For each component, extracting the corresponding value from the
   *       {@code JsonObject} using the component's name as the key.
   *       JSON keys are expected to match record component names exactly
   *       (case-sensitively).</li>
   *   <li>Converting the JSON values to the types of the record components.
   *       Supported conversions include:
   *       <ul>
   *         <li>JSON string to {@code String}.</li>
   *         <li>JSON number to {@code int} or {@code long}.</li>
   *         <li>Nested JSON object to another {@code Record} type (recursive mapping).</li>
   *       </ul>
   *   </li>
   *   <li>Invoking the canonical constructor of the {@code recordClass} with the
   *       extracted and converted values.</li>
   * </ol>
   * </p>
   *
   * @param <T>         The type of the record to map to.
   * @param object      The {@link JsonObject} to map from. Must not be null.
   *                    Its structure should align with the target record's components.
   * @param recordClass The {@link Class} object representing the target record type.
   *                    Must not be null.
   * @return An instance the recordClass populated with data from the {@code JsonObject}.
   * @throws NullPointerException          if {@code object} or {@code recordClass} is null.
   * @throws IllegalStateException         if a required key is missing in the {@code JsonObject}
   *                                       for a record component.
   * @throws ClassCastException            if a value cannot be
   *                                       correctly obtained or cast (e.g., trying to get
   *                                       a number for a string field if the JsonObject
   *                                       contains an unexpected type).
   * @throws UnsupportedOperationException if a record component has a type that is
   *                                       not supported by this mapper's implementation.
   */
  <T extends Record> T fromTyped(JsonObject object, Class<T> recordClass);

  Object match(JsonObject object, Class<? extends Record> recordClass);
}
