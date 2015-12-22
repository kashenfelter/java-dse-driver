/*
 *      Copyright (C) 2012-2015 DataStax Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.datastax.driver.geometry.codecs;

import com.datastax.driver.core.utils.DseVersion;
import com.datastax.driver.geometry.LineString;
import org.testng.collections.Lists;

import static com.datastax.driver.geometry.Utils.p;

@DseVersion(major = 5.0)
public class LineStringCodecIntegrationTest extends GeometryCodecIntegrationTest<LineString> {
    public LineStringCodecIntegrationTest() {
        super("LineStringType", Lists.newArrayList(
                new LineString(p(0, 10), p(10, 0)),
                new LineString(p(30, 10), p(10, 30), p(40, 40)),
                new LineString(p(-5, 0), p(0, 10), p(10, 5)))
        );
    }
}
