# Node Source URI Utility Design

## Goal

Add a handwritten `NodeSourceUri` utility to Java, Python, and TypeScript moeralib. The utility parses, modifies, and
serializes node source URI strings while exposing an idiomatic public interface in each language.

## Wire format

The serialized form is:

```text
<uri>;<parameter-name>=<parameter-value>;...
```

The substring before the first `;` is the URI. An `=` before the first `;` is an ordinary URI character. A `;` always
starts the parameter block, so a parsed URI cannot itself contain `;`.

Each substring after a `;` is one parameter segment. A segment is split at its first `=`. If there is no `=`, the
parameter value is the empty string. An empty parameter name is ignored. If the same decoded parameter name occurs
more than once, the last value replaces the previous value without changing the parameter's original position.

Parameter names and values use UTF-8 percent-encoding according to RFC 3986. Only unreserved ASCII characters
(`A-Z`, `a-z`, `0-9`, `-`, `.`, `_`, and `~`) remain unescaped. In particular, a space is `%20`, while `+` is encoded
as `%2B` during serialization and is never interpreted as a space during parsing. Hexadecimal digits emitted by the
serializer are uppercase.

A valid `%HH` sequence contributes its byte to UTF-8 decoding. A malformed escape, such as `%`, `%2`, or `%ZZ`, is
preserved literally. A sequence of valid escaped bytes that is not valid UTF-8 is decoded with the Unicode replacement
character, consistently across the three implementations.

Serialization always emits parameters as `;name=value`, including `=` for an empty value. Unknown parameters are
preserved. Parameter insertion order is preserved. Updating an existing parameter does not move it.

`via_email` is currently the only parameter with a published constant, but it has no special parsing or serialization
behavior.

## Object behavior

A default-constructed object contains an empty URI and no parameters, and serializes to the empty string. URI setters
accept any non-null string that does not contain `;`; they perform no other URI validation. Passing a URI containing
`;` raises the language's normal argument/value exception. `parse` accepts the complete non-null serialized string and
sets the URI directly from the substring before the first delimiter.

Replacing all parameters makes a defensive copy. Java returns a stable read-only live view. Python creates a read-only
view of the current dictionary without copying it. TypeScript returns the current mutable parameter object directly.
Replacing all parameters in Python or TypeScript replaces the underlying collection, so previously returned objects
remain attached to the old collection. Parameter names and values supplied through the programmatic API are non-null
strings. Empty parameter names supplied programmatically are ignored, matching the parser. Reading a missing parameter
returns the language's customary missing value.

## Java API

Create `org.moera.lib.node.NodeSourceUri` with this interface:

```java
public class NodeSourceUri {
    public static final String VIA_EMAIL = "via_email";

    public NodeSourceUri();
    public static NodeSourceUri parse(String sourceUri);
    public String getUri();
    public void setUri(String uri);
    public Map<String, String> getParameters();
    public void setParameters(Map<String, String> parameters);
    public boolean hasParameter(String name);
    public String getParameter(String name);
    public void setParameter(String name, String value);
    @Override public String toString();
}
```

Use `LinkedHashMap` internally. `getParameters()` returns a stable unmodifiable view of that map, so obtaining the view
does not copy the parameters and later changes through `NodeSourceUri` are visible in it. Put RFC 3986 percent-encoding
and tolerant percent-decoding in the public `org.moera.lib.util.PercentEncoding` utility. Null arguments are rejected
with `NullPointerException`, and `setUri()` throws `IllegalArgumentException` when the URI contains `;`.

## Python API

Create `moeralib.node_source_uri.NodeSourceUri`. Use an optional `uri: str = ""` constructor argument, a `parse`
class method, mutable `uri` property, read-only `parameters` mapping for the current dictionary, `set_parameters()`,
`get_parameter()`, `set_parameter()`, and `__str__()`. Return `MappingProxyType(self._parameters)` directly without
copying the dictionary. `set_parameters()` replaces the internal dictionary with its validated copy. `get_parameter()`
returns `None` when absent. Invalid argument types raise `TypeError`; a URI containing `;` raises `ValueError`. Export
the class from the top-level `moeralib` package.

## TypeScript API

Create and export `NodeSourceUri` from the package root. Its constructor accepts `uri: string = ""`. Provide static
`parse()`, mutable `uri`, the mutable current `Record<string, string>` through `parameters`, `setParameters()`,
`hasParameter()`, `getParameter()`, `setParameter()`, and `toString()`. Store parameters in an ordinary object rather
than a `Map`; no proxy or read-only facade is used. `getParameter()` returns `undefined` when absent. The URI setter
throws `TypeError` when it contains `;`.

## Documentation

Add a protocol page under `development/protocols/` documenting the wire format, parsing rules, percent-encoding, and
the `via_email` parameter. Add links from the development documentation navigation where protocol pages are listed.
API comments in each implementation document the language-specific surface and exceptions.

## Testing

Each library receives behavior tests covering:

- default construction and a URI containing `=`;
- parsing and serialization of multiple parameters;
- a segment without `=`, an empty name, and duplicate names;
- UTF-8 and reserved-character percent-encoding, including literal `+`;
- preservation of malformed percent escapes;
- rejection of `;` by the programmatic URI setter;
- defensive copying on parameter input; Java's stable read-only view; Python's uncopied read-only view; and
  TypeScript's mutable object;
- missing parameter lookup and the `via_email` constant.

The implementations are handwritten and are not added to the generated Node API schema. Existing `sourceUri` fields,
database storage, and client applications are unchanged.
