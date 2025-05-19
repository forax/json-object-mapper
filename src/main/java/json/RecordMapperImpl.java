package json;

import netscape.javascript.JSObject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.json.JsonNumber;
import java.util.json.JsonObject;
import java.util.json.JsonString;
import java.util.json.JsonValue;
import java.util.stream.IntStream;

import static java.lang.invoke.MethodType.methodType;

record RecordMapperImpl(MethodHandles.Lookup lookup, ClassValue<MethodHandle> mhs, ClassValue<Decoder> decoders)
    implements RecordMapper {

  public interface Decoder {  // hide it here
    Record decode(JsonObject object);
  }

  private static final Object INVALID = new Object();

  private static Object getOrFail(Map<String,Object> map, String key) {
    var value = map.getOrDefault(key, INVALID);
    if (value == INVALID) {
      throw new IllegalStateException("no key " + key + " defined");
    }
    return value;
  }

  private static final MethodHandle OBJECT_MEMBERS, MAP_GET,
      STRING_VALUE, LONG_VALUE, INT_VALUE;
  static {
    var lookup = MethodHandles.lookup();
    try {
      OBJECT_MEMBERS = lookup.findVirtual(JsonObject.class, "members", methodType(Map.class));
      MAP_GET = lookup.findStatic(lookup.lookupClass(), "getOrFail", methodType(Object.class, Map.class, String.class));
      STRING_VALUE = lookup.findVirtual(JsonString.class, "value", methodType(String.class))
          .asType(methodType(String.class, JsonValue.class));
      var numberValue =lookup.findVirtual(JsonNumber.class, "toNumber", methodType(Number.class));
      var longValue = numberValue
          .asType(methodType(Long.class, JsonValue.class))
          .asType(methodType(long.class, JsonValue.class));
      LONG_VALUE = longValue;
      INT_VALUE = MethodHandles.explicitCastArguments(longValue, methodType(int.class, JsonValue.class));

    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }


  @Override
  @SuppressWarnings("unchecked")
  public <T extends Record> T fromTyped(JsonObject object, Class<T> recordClass) {
    Objects.requireNonNull(object);
    Objects.requireNonNull(recordClass);
    return (T) decoders.get(recordClass).decode(object);
  }

  @Override
  public Object match(JsonObject object, Class<? extends Record> recordClass) {
    Objects.requireNonNull(object);
    Objects.requireNonNull(recordClass);
    try {
      return decoders.get(recordClass).decode(object);
    } catch (IllegalStateException | ClassCastException e) {
      return null;
    }
  }

  static RecordMapper of(MethodHandles.Lookup lookup) {
    var mhs = new ClassValue<MethodHandle>() {
      private MethodHandle filter(Class<?> type) {
        if (type == String.class) {
          return STRING_VALUE;
        }
        if (type.isRecord()) {
          return get(type)
              .asType(methodType(type, JsonValue.class));
        }
        return switch (type.getName()) {
          case "long" -> LONG_VALUE;
          case "int" -> INT_VALUE;
          // FIXME implements the other primitive types
          default -> throw new UnsupportedOperationException("Unsupported type: " + type);
        };
      }

      @Override
      protected MethodHandle computeValue(Class<?> type) {
        var components = type.getRecordComponents();
        var types = Arrays.stream(components)
            .map(RecordComponent::getType)
            .toArray(Class<?>[]::new);
        var names = Arrays.stream(components)
            .map(RecordComponent::getName)
            .toArray(String[]::new);

        MethodHandle constructor;
        try {
          constructor = lookup.findConstructor(type, methodType(void.class, types));
        } catch (NoSuchMethodException e) {
          throw (NoSuchMethodError) new NoSuchMethodError().initCause(e);
        } catch (IllegalAccessException e) {
          throw (IllegalAccessError) new IllegalAccessError().initCause(e);
        }
        constructor = constructor.asType(methodType(Record.class, types));

        var filters = IntStream.range(0, types.length)
            .mapToObj(i -> {
              var parameterType = types[i];
              var name = names[i];
              var mapGet = MethodHandles.insertArguments(MAP_GET, 1, name)
                  .asType(methodType(JsonValue.class, Map.class));
              return MethodHandles.filterReturnValue(mapGet, filter(parameterType));
            })
            .toArray(MethodHandle[]::new);
        var mh = MethodHandles.filterArguments(constructor, 0, filters);
        mh = MethodHandles.permuteArguments(mh, methodType(Record.class, Map.class), new int[types.length]);
        mh = MethodHandles.filterReturnValue(OBJECT_MEMBERS, mh);
        return mh;
      }
    };
    var decoders = new ClassValue<Decoder>() {
      @Override
      protected Decoder computeValue(Class<?> type) {
        var mh = mhs.get(type);
        return MethodHandleProxies.asInterfaceInstance(Decoder.class, mh);
      }
    };

    return new RecordMapperImpl(lookup, mhs, decoders);
  }
}
