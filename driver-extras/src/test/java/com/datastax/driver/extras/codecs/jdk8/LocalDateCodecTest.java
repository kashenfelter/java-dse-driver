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

import java.time.LocalDate;

import static com.datastax.driver.core.ProtocolVersion.V4;
import static org.assertj.core.api.Assertions.assertThat;

public class LocalDateCodecTest {

    private static final LocalDate EPOCH = LocalDate.ofEpochDay(0);
    private static final LocalDate MIN = LocalDate.parse("-5877641-06-23");

    @DataProvider(name = "LocalDateCodecTest.parse")
    public Object[][] parseParameters() {
        return new Object[][]{
                {null, null},
                {"", null},
                {"NULL", null},
                {"0", MIN},
                {"'2147483648'", EPOCH},
                {"'-5877641-06-23'", MIN},
                {"'1970-01-01'", EPOCH},
                {"'2014-01-01'", LocalDate.parse("2014-01-01")}
        };
    }

    @DataProvider(name = "LocalDateCodecTest.format")
    public Object[][] formatParameters() {
        return new Object[][]{
                {null, "NULL"},
                {LocalDate.ofEpochDay(0), "'1970-01-01'"},
                {LocalDate.parse("2010-06-30"), "'2010-06-30'"}
        };
    }

    @Test(groups = "unit", dataProvider = "LocalDateCodecTest.parse")
    public void should_parse_valid_formats(String input, LocalDate expected) {
        // when
        LocalDate actual = LocalDateCodec.instance.parse(input);
        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test(groups = "unit", dataProvider = "LocalDateCodecTest.format")
    public void should_serialize_and_format_valid_object(LocalDate input, String expected) {
        // when
        String actual = LocalDateCodec.instance.format(input);
        // then
        Assertions.assertThat(LocalDateCodec.instance).withProtocolVersion(V4).canSerialize(input);
        assertThat(actual).isEqualTo(expected);
    }

}
