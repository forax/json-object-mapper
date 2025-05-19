# Java JSON RecordMapper

A simple Java interface for mapping `java.util.json.JsonObject` instances to Java record instances.
The mapping relies on matching keys in the `JsonObject` to the component names of the target record.

## Key Features

*   **Type-Safe Mapping:** Converts `JsonObject` to a specific record class.
*   **Nested Record Support:** Capable of mapping JSON objects that contain nested JSON objects to records with components that are themselves records.
*   **Mandatory Fields:** By default, all record components are considered mandatory. If a corresponding key is missing in the `JsonObject`, an `IllegalStateException` is thrown during mapping.
*   **Reflection-based:** Uses `MethodHandles.Lookup` to access record constructors, enabling mapping to public and non-public records (if the lookup has sufficient access rights).

## Prerequisites

Java 25 (`java.util.json` must be present)

## Getting Started

Define your record.

```java
// Simple record
record User(String name, int age) {}
```

Create an instance of `RecordMapper` using the `of` factory method.
You need to provide a `MethodHandles.Lookup` instance.
It's recommended to store the mapper as a `static final` constant.

```java
import java.lang.invoke.MethodHandles;
import json.RecordMapper;

static final RecordMapper MAPPER = RecordMapper.of(MethodHandles.lookup());

public class Main {
    public static void main(String[] args) {
       JsonObject userJson = ...
    
       User user = MAPPER.fromTyped(userJson, User.class);
       System.out.println("Mapped User: " + user);
    }
}
```

## How It Works

1.  **Component Discovery:** Retrieves the record components of the target `recordClass`.
2.  **Key-Value Matching:** For each component, it extracts the corresponding value from the `JsonObject`
    using the component's name as the key (case-sensitive).
3.  **Type Conversion:** Converts JSON values to the types of the record components.
4.  **Constructor Invocation:** Invokes the canonical constructor of the `recordClass`
    with the extracted and converted values.

## Supported Data Types for Record Components

The mapper implementation directly supports:

- `String` from JSON string
- `int` or `long ` from JSON number
- other `Record` types from nested JSON objects, mapped recursively
- *(TODO add more types)*

If a record component type is not supported, an `UnsupportedOperationException` may be thrown.

## What's missing

It's just a prototype so

- recursive records of the same type are not cached correctly
- not all primitive types are supported
- `java.util.List` of records (or primitives) are not supported
