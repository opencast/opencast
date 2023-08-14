/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.mediapackage;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.net.URI;

public class ChainingMediaPackageSerializerTest {

  @Test
  public void testEncodeURI() throws Exception {
    final URI uri1 = new URI("http://test.host/path/file.ext");
    MediaPackageSerializer serializer1 = createNiceMock(MediaPackageSerializer.class);
    expect(serializer1.getRanking()).andStubReturn(10);
    expect(serializer1.encodeURI(anyObject(URI.class))).andStubReturn(uri1);
    expect(serializer1.decodeURI(anyObject(URI.class))).andStubReturn(uri1);
    replay(serializer1);

    final URI uri2 = new URI("https://demo.com/something/else");
    MediaPackageSerializer serializer2 = createNiceMock(MediaPackageSerializer.class);
    expect(serializer2.getRanking()).andStubReturn(5);
    expect(serializer2.encodeURI(anyObject(URI.class))).andStubReturn(uri2);
    expect(serializer2.decodeURI(anyObject(URI.class))).andStubReturn(uri2);
    replay(serializer2);

    ChainingMediaPackageSerializer chain = new ChainingMediaPackageSerializer();
    chain.addMediaPackageSerializer(serializer1);
    chain.addMediaPackageSerializer(serializer2);

    final URI uri = new URI("file:///tmp/file");
    assertEquals(uri2, chain.encodeURI(uri));
    assertEquals(uri1, chain.decodeURI(uri));
  }

}
