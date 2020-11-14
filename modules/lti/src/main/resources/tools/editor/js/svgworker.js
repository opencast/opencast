var params = {
  width: 1000,
  height: 64,
  targetSamples: 50000
}

onmessage = function(e) {
  switch(e.data.constructor) {
    case Float32Array:
      let totalLen = e.data.length;
      let data = e.data;
      if (totalLen > params.targetSamples * 1.1) {
        let divisor = 1.0 * e.data.length / params.targetSamples >> 0;
        data = data.filter((sample, i) => i % divisor === 0);
        totalLen = data.length;
      }
      postMessage({type: 'path', path: generatePath(data), length: totalLen});
      break;

    case Object:
      switch (e.data.cmd) {
        case 'init':
          setParameters(e.data);
      }
      break;

    default:
      console.log('unknown');
  }
}

function setParameters(opts) {
  for (var key in opts) {
    if (params.hasOwnProperty(key)) {
      params[key] = opts[key];
    }
  }
}

function generatePath(peaks) {
  var line = `M0,0`;
  line += peaks.reduce((d, current, index) => {
           d += ` L${index},${current}`;
           return d;
         }, '');
  return line;
}
