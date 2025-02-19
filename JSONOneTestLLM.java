package org.sfj;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class JSONOneTest {

    @Test
    public void testParseEmptyString() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("\"\"");
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.STRING));
        assertThat(obj.stringValue(), is(""));
    }

    @Test
    public void testParseEscapedCharactersInString() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("\"Hello \\\"World\\\"\"");
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.STRING));
        assertThat(obj.stringValue(), is("Hello \"World\""));
    }

    @Test
    public void testParseUnicodeInString() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("\"\\u0041\\u0042\\u0043\"");
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.STRING));
        assertThat(obj.stringValue(), is("ABC"));
    }



    @Test(expected = ParseException.class)
    public void testParseUnclosedString() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("\"Hello");
        p.singleObject(); // Should throw ParseException
    }

    @Test(expected = ParseException.class)
    public void testParseUnclosedArray() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("[1, 2, 3");
        p.singleObject(); // Should throw ParseException
    }

    @Test(expected = ParseException.class)
    public void testParseUnclosedObject() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("{\"key\": 10");
        p.singleObject(); // Should throw ParseException
    }


    @Test(expected = ParseException.class)
    public void testParseTrailingCommaInArray() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("[1, 2,]");
        p.singleObject(); // Should throw ParseException
    }

    @Test(expected = ParseException.class)
    public void testParseTrailingCommaInObject() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("{\"a\": 1,}");
        p.singleObject(); // Should throw ParseException
    }




    @Test
    public void testAbstractJSONObjectEdgeCases() {
        JSONOne.JString str1 = new JSONOne.JString("value1");
        JSONOne.JString str2 = new JSONOne.JString("value1");
        JSONOne.JString str3 = new JSONOne.JString("value2");

        // Test equals
        assertThat(str1.equals(str2), is(true));
        assertThat(str1.equals(str3), is(false));

        // Test hashCode
        assertThat(str1.hashCode(), is(str2.hashCode()));

        // Test toString
        assertThat(str1.toString(), is("value1"));
    }

    @Test
    public void testJMapEdgeCases() {
        JSONOne.JMap map = new JSONOne.JMap();
        map.put("key1", new JSONOne.JString("value1"));
        map.put("key2", new JSONOne.JNumber(123));

        // Test containsKey and containsValue
        assertThat(map.containsKey("key1"), is(true));
        assertThat(map.containsKey("nonexistent"), is(false));
        assertThat(map.containsValue(new JSONOne.JString("value1")), is(true));

        // Test remove
        map.remove("key1");
        assertThat(map.containsKey("key1"), is(false));

        // Test clear
        map.clear();
        assertThat(map.size(), is(0));
    }


    @Test
    public void testJArrayEdgeCases() {
        JSONOne.JArray array = new JSONOne.JArray();
        array.add(new JSONOne.JString("value1"));
        array.add(new JSONOne.JNumber(123));

        // Test set
        array.set(0, new JSONOne.JString("newValue"));
        assertThat(array.get(0).stringValue(), is("newValue"));

        // Test remove
        array.remove(0);
        assertThat(array.size(), is(1));

        // Test clear
        array.clear();
        assertThat(array.size(), is(0));
    }




    @Test
    public void testUnicodeAndEscapedCharacters() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("\"\\u0041\\u0042\\u0043\""); // ABC
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.STRING));
        assertThat(obj.stringValue(), is("ABC"));

        p = new JSONOne.Parser("\"\\\"\\\\\\/\\b\\f\\n\\r\\t\""); // Escaped characters
        obj = p.singleObject();
        assertThat(obj.stringValue(), is("\"\\/\b\f\n\r\t"));
    }

    @Test
    public void testEdgeCaseNumbers() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("1.7976931348623157E308"); // Max double value
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.NUMBER));
        assertThat(obj.numberValue().doubleValue(), is(1.7976931348623157E308));

        p = new JSONOne.Parser("4.9E-324"); // Min double value
        obj = p.singleObject();
        assertThat(obj.numberValue().doubleValue(), is(4.9E-324));


    }

    @Test
    public void testNullValues() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("{\"key\": null}");
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.MAP));
        assertThat(obj.mapValue().get("key").nullValue(), is(true));

        p = new JSONOne.Parser("[null, null]");
        obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.ARRAY));
        assertThat(obj.arrayValue().get(0).nullValue(), is(true));
        assertThat(obj.arrayValue().get(1).nullValue(), is(true));
    }

    @Test
    public void testPrettyPrinting() throws IOException {
        JSONOne.JMap map = new JSONOne.JMap();
        map.putBoolean("bool", true);
        map.putNumber("num", 123);
        map.putString("str", "value");
        map.putNull("null");

        String prettyJson = map.print(2, false);
        String expected = "{\n" +
                "  \"bool\": true,\n" +
                "  \"num\": 123,\n" +
                "  \"str\": \"value\",\n" +
                "  \"null\": null\n" +
                "}";
        assertThat(prettyJson, is(expected));

        String compactJson = map.print(0, true);
        assertThat(compactJson, is("{\"bool\":true,\"num\":123,\"str\":\"value\",\"null\":null}"));
    }

    @Test(expected = ClassCastException.class)
    public void testTypeMismatchNumber() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("\"123\"");
        JSONOne.JObject obj = p.singleObject();
        obj.numberValue(); // Should throw ClassCastException
    }

    @Test(expected = ClassCastException.class)
    public void testTypeMismatchString() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("123");
        JSONOne.JObject obj = p.singleObject();
        obj.stringValue(); // Should throw ClassCastException
    }

    @Test(expected = ParseException.class)
    public void testInvalidEscapeSequence() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("\"\\x\""); // Invalid escape sequence
        p.singleObject();
    }

    @Test(expected = ParseException.class)
    public void testInvalidNumberFormat() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("1.2.3"); // Invalid number format
        p.singleObject();
    }

    @Test
    public void testNestedMixedTypes() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("{\"array\": [1, \"two\", true, null]}");
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.MAP));

        JSONOne.JArray nestedArray = obj.mapValue().getArray("array", null);
        assertThat(nestedArray.size(), is(4));
        assertThat(nestedArray.get(0).numberValue().intValue(), is(1));
        assertThat(nestedArray.get(1).stringValue(), is("two"));
        assertThat(nestedArray.get(2).boolValue(), is(true));
        assertThat(nestedArray.get(3).nullValue(), is(true));
    }

    @Test(expected = ParseException.class)
    public void testEmptyInput() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("");
        p.singleObject();
    }

    @Test(expected = ParseException.class)
    public void testInvalidInput() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("invalid");
        p.singleObject();
    }

    @Test(expected = ParseException.class)
    public void testParseInvalidJSON() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("{");
        p.singleObject(); // Should throw ParseException
    }

    @Test(expected = ParseException.class)
    public void testParseInvalidNumber() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("5.3.3");
        p.singleObject(); // Should throw ParseException
    }

    @Test(expected = ParseException.class)
    public void testParseInvalidBoolean() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("tru");
        p.singleObject(); // Should throw ParseException
    }

    @Test(expected = ParseException.class)
    public void testParseInvalidString() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("\"foo");
        p.singleObject(); // Should throw ParseException
    }

    @Test
    public void testParseEmptyObject() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("{}");
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.MAP));
        assertThat(obj.mapValue().size(), is(0));
    }

    @Test
    public void testParseEmptyArray() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("[]");
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.ARRAY));
        assertThat(obj.arrayValue().size(), is(0));
    }

    @Test
    public void testParseNestedObject() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("{\"nested\": {\"key\": \"value\"}}");
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.MAP));

        // Ensure the value associated with "nested" is a JMap
        JSONOne.JObject nestedObj = obj.mapValue().get("nested");
        assertThat(nestedObj.getType(), is(JSONOne.Type.MAP));

        // Safely cast to JMap
        JSONOne.JMap nestedMap = nestedObj.mapValue();
        assertThat(nestedMap.getString("key", null), is("value"));
    }

    @Test
    public void testParseNestedArray() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("{\"nested\": [1, 2, 3]}");
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.MAP));

        // Ensure the value associated with "nested" is a JArray
        JSONOne.JObject nestedObj = obj.mapValue().get("nested");
        assertThat(nestedObj.getType(), is(JSONOne.Type.ARRAY));

        // Safely cast to JArray
        JSONOne.JArray nestedArray = nestedObj.arrayValue();
        assertThat(nestedArray.size(), is(3));
        assertThat(nestedArray.get(0).numberValue().intValue(), is(1));
        assertThat(nestedArray.get(1).numberValue().intValue(), is(2));
        assertThat(nestedArray.get(2).numberValue().intValue(), is(3));
    }

    @Test
    public void testParseEscapedString() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("\"foo\\\"bar\"");
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.STRING));
        assertThat(obj.stringValue(), is("foo\"bar"));

        p = new JSONOne.Parser("\"foo\\\\bar\"");
        obj = p.singleObject();
        assertThat(obj.stringValue(), is("foo\\bar"));

        p = new JSONOne.Parser("\"foo\\/bar\"");
        obj = p.singleObject();
        assertThat(obj.stringValue(), is("foo/bar"));

        p = new JSONOne.Parser("\"foo\\nbar\"");
        obj = p.singleObject();
        assertThat(obj.stringValue(), is("foo\nbar"));

        p = new JSONOne.Parser("\"foo\\u0041bar\"");
        obj = p.singleObject();
        assertThat(obj.stringValue(), is("fooAbar"));
    }

    @Test
    public void testParseMixedArray() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("[1, \"two\", true, null]");
        JSONOne.JArray arr = (JSONOne.JArray) p.singleObject();
        assertThat(arr.size(), is(4));
        assertThat(arr.get(0).numberValue().intValue(), is(1));
        assertThat(arr.get(1).stringValue(), is("two"));
        assertThat(arr.get(2).boolValue(), is(true));
        assertThat(arr.get(3).nullValue(), is(true));
    }

    @Test
    public void testParseMixedObject() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("{\"num\": 1, \"str\": \"two\", \"bool\": true, \"null\": null}");
        JSONOne.JMap map = (JSONOne.JMap) p.singleObject();
        assertThat(map.size(), is(4));
        assertThat(map.getNumber("num", null).intValue(), is(1));
        assertThat(map.getString("str", null), is("two"));
        assertThat(map.getBoolean("bool", null), is(true));
        assertThat(map.get("null").nullValue(), is(true));
    }

    @Test
    public void testSerializeObject() throws IOException {
        JSONOne.JMap map = new JSONOne.JMap();
        map.putBoolean("bool", true);
        map.putNumber("num", 123);
        map.putString("str", "value");
        map.putNull("null");

        String jsonString = map.print(0, true);
        assertThat(jsonString, is("{\"bool\":true,\"num\":123,\"str\":\"value\",\"null\":null}"));
    }

    @Test
    public void testSerializeArray() throws IOException {
        JSONOne.JArray arr = new JSONOne.JArray();
        arr.addBoolean(true);
        arr.addNumber(123);
        arr.addString("value");
        arr.addNull();

        String jsonString = arr.print(0, true);
        assertThat(jsonString, is("[true,123,\"value\",null]"));
    }

    @Test
    public void testParseLargeNumber() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("1.7976931348623157E308"); // Max double value
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.NUMBER));
        assertThat(obj.numberValue().doubleValue(), is(1.7976931348623157E308));
    }

    @Test
    public void testParseSmallNumber() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("4.9E-324"); // Min double value
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.NUMBER));
        assertThat(obj.numberValue().doubleValue(), is(4.9E-324));
    }

    @Test(expected = ClassCastException.class)
    public void testInvalidTypeAccess() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("123");
        JSONOne.JObject obj = p.singleObject();
        obj.stringValue(); // Should throw ClassCastException
    }

    @Test(expected = ClassCastException.class)
    public void testInvalidTypeAccess2() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("\"123\"");
        JSONOne.JObject obj = p.singleObject();
        obj.numberValue(); // Should throw ClassCastException
    }


    @Test
    public void testParseUnicodeString() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("\"\\u0041\\u0042\\u0043\"");
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.STRING));
        assertThat(obj.stringValue(), is("ABC"));
    }

    @Test
    public void testParseUnicodeStringInObject() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("{\"key\": \"\\u0041\\u0042\\u0043\"}");
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.MAP));
        assertThat(obj.mapValue().getString("key", null), is("ABC"));
    }
    @Test
    public void testParseBoolean() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("true");
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.BOOLEAN));
        assertThat(obj.boolValue(), is(true));

        p = new JSONOne.Parser("tRUe");
        obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.BOOLEAN));
        assertThat(obj.boolValue(), is(true));

        p = new JSONOne.Parser("false");
        obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.BOOLEAN));
        assertThat(obj.boolValue(), is(false));
    }

    @Test
    public void testParseNumber() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("5");
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.NUMBER));
        assertThat(obj.numberValue().longValue(), is(5L));

        p = new JSONOne.Parser("5.3");
        obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.NUMBER));
        assertThat(obj.numberValue().doubleValue(), is(5.3d));

        p = new JSONOne.Parser("-5.3");
        obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.NUMBER));
        assertThat(obj.numberValue().doubleValue(), is(-5.3d));

        p = new JSONOne.Parser("5.3E4");
        obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.NUMBER));
        assertThat(obj.numberValue().doubleValue(), is(5.3e4d));

        p = new JSONOne.Parser("5.3E-4");
        obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.NUMBER));
        assertThat(obj.numberValue().doubleValue(), is(5.3e-4d));
    }

    @Test
    public void testParseString() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("\"foo\"");
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.STRING));
        assertThat(obj.stringValue(), is("foo"));

        p = new JSONOne.Parser("\"foo is a bar\"");
        obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.STRING));
        assertThat(obj.stringValue(), is("foo is a bar"));

        p = new JSONOne.Parser("\"fo\\'o \\ni\\\"s a bar\"");
        obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.STRING));
        assertThat(obj.stringValue(), is("fo'o \ni\"s a bar"));
    }

    @Test
    public void testParseNull() throws ParseException {
        JSONOne.Parser p = new JSONOne.Parser("null");
        JSONOne.JObject obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.NULL));
        assertThat(obj.nullValue(), is(true));

        p = new JSONOne.Parser("nUll");
        obj = p.singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.NULL));
        assertThat(obj.nullValue(), is(true));
    }

    @Test
    public void testSimpleArrays() throws ParseException, IOException {
        JSONOne.JArray arr = new JSONOne.JArray();
        arr.addBoolean(true);
        arr.addNumber(1003);
        arr.addString("foobar");
        assertThat(arr.size(), is(3));
        assertThat(arr.get(0).getType(), is(JSONOne.Type.BOOLEAN));
        assertThat(arr.get(1).getType(), is(JSONOne.Type.NUMBER));
        assertThat(arr.get(2).getType(), is(JSONOne.Type.STRING));

        JSONOne.Parser p = new JSONOne.Parser("[ true, 1003, \"foobar\"]");
        JSONOne.JArray arr2 = (JSONOne.JArray) p.singleObject();
        assertThat(arr, is(arr2));
    }

    @Test
    public void testSimpleMaps() throws ParseException, IOException {
        JSONOne.JMap map = new JSONOne.JMap();
        map.putBoolean("one", false);
        map.putNull("two");
        map.putNumber("three", 102.4d);
        map.putString("four", "taffeta");

        assertThat(map.size(), is(4));
        assertThat(map.getBoolean("one", null), is(false));
        assertThat(map.get("two").nullValue(), is(true));
        assertThat(map.getNumber("three", null), is(102.4d));
        assertThat(map.getString("four", null), is("taffeta"));

        JSONOne.Parser
                p =
                new JSONOne.Parser("{ \"one\" : false, \"two\": null, \"three\":102.4d, \"four\": \"taffeta\" }");
        JSONOne.JMap map2 = (JSONOne.JMap) p.singleObject();
        assertThat(map, is(map2));
    }

    @Test
    public void testParseColors() throws ParseException, IOException {
        InputStream stream = JSONOneTest.class.getResourceAsStream("colors.json");
        StringBuilder sb = new StringBuilder();
        new BufferedReader(new InputStreamReader(stream)).lines().forEach(l -> {
            sb.append(l);
            sb.append('\n');
        });
        JSONOne.JObject obj = new JSONOne.Parser(sb.toString()).singleObject();
        assertThat(obj.getType(), is(JSONOne.Type.MAP));
        JSONOne.JMap map = obj.mapValue();
        assertThat(map.size(), is(1));
        JSONOne.JArray arr = map.getArray("colors", null);
        assertThat(arr.size(), is(6));

        // check something random
        assertThat(arr.get(4).mapValue().getString("type", null), is("primary"));

    }

    @Test
    public void testRoundTrip() throws ParseException, IOException {
        roundTrip("colors.json", "mockCrud.json", "twitter.json");
    }

    private void roundTrip(String... resources) throws ParseException, IOException {
        for (String res : resources) {
            InputStream stream = JSONOneTest.class.getResourceAsStream(res);
            StringBuilder sb = new StringBuilder();
            new BufferedReader(new InputStreamReader(stream)).lines().forEach(l -> {
                sb.append(l);
                sb.append(System.lineSeparator());
            });
            JSONOne.JObject obj1 = new JSONOne.Parser(sb.toString()).singleObject();
            if (obj1.getType().equals(JSONOne.Type.MAP) || obj1.getType().equals(JSONOne.Type.ARRAY)) {
                String cString = obj1.print(0, true);
                String nocString = obj1.print(0, false);
                JSONOne.JObject obj3 = new JSONOne.Parser(nocString).singleObject();
                JSONOne.JObject obj2 = new JSONOne.Parser(cString).singleObject();
                assertThat(obj1, is(obj2));
                assertThat(obj1, is(obj3));
                assertThat(obj2, is(obj3));
            }
        }
    }
}
