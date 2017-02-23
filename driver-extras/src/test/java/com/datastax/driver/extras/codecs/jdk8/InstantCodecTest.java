/*
 *      Copyright (C) 2012-2017 DataStax Inc.
 *
 *      This software can be used solely with DataStax Enterprise. Please consult the license at
 *      http://www.datastax.com/terms/datastax-dse-driver-license-terms
 */
package com.datastax.driver.extras.codecs.jdk8;

import com.datastax.driver.core.Assertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Instant;
import java.time.ZonedDateTime;

import static com.datastax.driver.core.ProtocolVersion.V4;
import static org.assertj.core.api.Assertions.assertThat;

public class InstantCodecTest {

    @DataProvider(name = "InstantCodecTest.parse")
    public Object[][] parseParameters() {
        return new Object[][]{
                {null, null},
                {"", null},
                {"NULL", null},
                {"0", Instant.ofEpochMilli(0)},
                {"1277860847999", Instant.ofEpochMilli(1277860847999L)},
                {"'2010-06-30T01:20'", Instant.parse("2010-06-30T01:20:00.000Z")},
                {"'2010-06-30T01:20Z'", Instant.parse("2010-06-30T01:20:00.000Z")},
                {"'2010-06-30T01:20:47'", Instant.parse("2010-06-30T01:20:47.000Z")},
                {"'2010-06-30T01:20:47+01:00'", Instant.parse("2010-06-30T00:20:47.000Z")},
                {"'2010-06-30T01:20:47.999'", Instant.parse("2010-06-30T01:20:47.999Z")},
                {"'2010-06-30T01:20:47.999+01:00'", Instant.parse("2010-06-30T00:20:47.999Z")}
        };
    }

    @DataProvider(name = "InstantCodecTest.format")
    public Object[][] formatParameters() {
        return new Object[][]{
                {null, "NULL"},
                {Instant.ofEpochMilli(0), "'1970-01-01T00:00:00.000+00:00'"},
                {ZonedDateTime.parse("2010-06-30T02:20:47.999+01:00").toInstant(), "'2010-06-30T01:20:47.999+00:00'"}
        };
    }

    @Test(groups = "unit", dataProvider = "InstantCodecTest.parse")
    public void should_parse_valid_formats(String input, Instant expected) {
        // when
        Instant actual = InstantCodec.instance.parse(input);
        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test(groups = "unit", dataProvider = "InstantCodecTest.format")
    public void should_serialize_and_format_valid_object(Instant input, String expected) {
        // when
        String actual = InstantCodec.instance.format(input);
        // then
        Assertions.assertThat(InstantCodec.instance).withProtocolVersion(V4).canSerialize(input);
        assertThat(actual).isEqualTo(expected);
    }

}
