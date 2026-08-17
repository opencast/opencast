/*
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

/** Methods which carry their parameters in the query string rather than in a request body. */
const QUERY_METHODS = ['GET', 'HEAD', 'DELETE'];

const CLOSERS = {'{': '}', '[': ']'};

/** Index just past the closing quote of the JSON string literal starting at `start`. */
function stringEnd(src, start) {
  for (let i = start + 1; i < src.length; i++) {
    if (src[i] === '\\') {
      i += 1;
    } else if (src[i] === '"') {
      return i + 1;
    }
  }
  return src.length;
}

/**
 * Re-indent a JSON document without parsing it. A JSON.parse()/JSON.stringify() round trip would
 * round every number above 2^53, and Opencast payloads do contain such identifiers. Input that does
 * not look like JSON (XML, plain text, an error page) is returned unchanged.
 */
function prettify(text) {
  const src = text.trim();
  if (src[0] !== '{' && src[0] !== '[') {
    return text;
  }
  let out = '';
  let depth = 0;
  for (let i = 0; i < src.length; i++) {
    const char = src[i];
    if (char === '"') {
      // copy string literals verbatim so their contents are never reformatted
      const end = stringEnd(src, i);
      out += src.slice(i, end);
      i = end - 1;
    } else if (/\s/.test(char)) {
      continue;
    } else if (CLOSERS[char] !== undefined) {
      let next = i + 1;
      while (next < src.length && /\s/.test(src[next])) {
        next += 1;
      }
      if (src[next] === CLOSERS[char]) {
        // keep empty objects and arrays on a single line
        out += char + CLOSERS[char];
        i = next;
      } else {
        depth += 1;
        out += char + '\n' + '  '.repeat(depth);
      }
    } else if (char === '}' || char === ']') {
      depth -= 1;
      out += '\n' + '  '.repeat(depth) + char;
    } else if (char === ',') {
      out += char + '\n' + '  '.repeat(depth);
    } else if (char === ':') {
      out += ': ';
    } else {
      out += char;
    }
  }
  return out;
}

function escapeRegExp(text) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/** The value a form field contributes to the request. */
function fieldValue(input) {
  return input.type === 'checkbox' ? String(input.checked) : input.value;
}

/**
 * Percent-encode a path parameter value. Slashes are left alone so that values for catch-all path
 * parameters such as {path: .*} keep working.
 */
function encodePathValue(value) {
  return encodeURIComponent(value).replace(/%2F/g, '/');
}

/**
 * Substitute the path parameter values into the endpoint path. Placeholders whose field is empty or
 * whose value violates the parameter's pattern are left in place, and the offending field is marked
 * invalid so that the browser reports it on submit.
 */
function resolvePath(form) {
  let path = form.getAttribute('action');
  for (const input of form.querySelectorAll('.form_param_path')) {
    input.setCustomValidity('');
    const value = fieldValue(input);
    if (value === '') {
      continue;
    }
    // A path may constrain a parameter with a regular expression, as in /episode.{format:xml|json}.
    const placeholder = new RegExp('\\{' + escapeRegExp(input.name) + '(?::([^}]*))?\\}');
    const match = placeholder.exec(path);
    if (match === null) {
      continue;
    }
    if (match[1] !== undefined && !new RegExp('^(?:' + match[1] + ')$').test(value)) {
      input.setCustomValidity('This value does not match the expected pattern: ' + match[1]);
      continue;
    }
    // a replacer function keeps $-sequences in the value from being expanded
    path = path.replace(placeholder, () => encodePathValue(value));
  }
  return path;
}

/** Resolve the request URL and show it below the form. */
function refreshPath(form) {
  const path = resolvePath(form);
  form.querySelector('.form_path').textContent = path;
  return path;
}

/** The parameters to send, as [name, value] pairs. Untouched optional fields are left out. */
function collectParams(form) {
  const params = [];
  for (const input of form.querySelectorAll('.form_param_submit')) {
    if (input.type === 'file') {
      for (const file of input.files) {
        params.push([input.name, file]);
      }
    } else if (input.type === 'checkbox' || input.value !== '') {
      params.push([input.name, fieldValue(input)]);
    }
  }
  return params;
}

function hasUpload(params) {
  return params.some(([, value]) => value instanceof File);
}

async function submitForm(event, form) {
  event.preventDefault();

  const path = refreshPath(form);
  if (!form.reportValidity()) {
    return;
  }

  const method = form.querySelector('.form_method').value;
  const url = new URL(path, window.location.href);
  const params = collectParams(form);
  let body;
  if (QUERY_METHODS.includes(method)) {
    for (const [name, value] of params) {
      url.searchParams.append(name, value);
    }
  } else {
    body = hasUpload(params) ? new FormData() : new URLSearchParams();
    for (const [name, value] of params) {
      body.append(name, value);
    }
  }

  const working = form.parentElement.querySelector('.test_form_working');
  const response = form.parentElement.querySelector('.test_form_response');
  const output = response.querySelector('pre');
  output.textContent = '';
  response.classList.add('hidden');
  working.classList.remove('hidden');

  let message;
  try {
    // FormData sets its own Content-Type, including the multipart boundary. Never set it by hand.
    const result = await fetch(url, {method: method, body: body, credentials: 'same-origin'});
    const text = await result.text();
    message = `Status: ${result.status} (${result.statusText})\n\n${prettify(text)}`;
  } catch (error) {
    message = `The request failed: ${error.message}\n\n`
      + 'Note that the form submits to the server URL Opencast is configured with. If you are '
      + 'browsing these docs through a different host or port, the request is cross-origin and '
      + 'will be blocked.';
  }

  working.classList.add('hidden');
  output.textContent = message;
  response.classList.remove('hidden');
}

function init() {
  for (const form of document.querySelectorAll('form.form_test_form')) {
    for (const input of form.querySelectorAll('.form_param_path')) {
      input.addEventListener('input', () => refreshPath(form));
      input.addEventListener('change', () => refreshPath(form));
    }
    refreshPath(form);
    form.addEventListener('submit', (event) => submitForm(event, form));
  }
}

addEventListener('DOMContentLoaded', init);
