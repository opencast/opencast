import assert from 'node:assert/strict';
import test from 'node:test';

import {
  EventConversor,
  opencastSearchResultToOpencastPaellaEvent,
} from '@asicupv/paella-opencast-core';

test('converts an Opencast audio-only delivery track into a Paella stream', () => {
  const searchResult = {
    mediapackage: {
      id: 'audio-only-event',
      duration: 597008,
      title: 'audio only presentation',
      media: {
        track: {
          id: 'audio-track',
          type: 'presentation/delivery',
          mimetype: 'audio/m4a',
          tags: { tag: ['engage-download', 'engage-streaming'] },
          url: 'https://example.invalid/presentation.m4a',
          duration: 597008,
          live: false,
          master: false,
          audio: { channels: 2, samplingrate: 44100 },
        },
      },
      attachments: { attachment: [] },
      metadata: { catalog: [] },
    },
    acl: [],
  };

  const event = opencastSearchResultToOpencastPaellaEvent(searchResult);
  const streams = new EventConversor({}, {}).getStreams(event);

  assert.equal(streams.length, 1);
  assert.equal(streams[0].content, 'presentation');
  assert.equal(streams[0].role, 'mainAudio');
  assert.deepEqual(streams[0].sources.audio, [
    {
      src: 'https://example.invalid/presentation.m4a',
      mimetype: 'audio/m4a',
      res: { w: 0, h: 0 },
    },
  ]);
});
