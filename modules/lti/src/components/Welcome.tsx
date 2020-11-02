import React from "react";

export function Welcome() {
    return <>
        <h1>Welcome to the LTI Module</h1>
        <ul>
            <li>More information about configuring LTI is available in the Administration Guide at <a href="https://docs.opencast.org">docs.opencast.org</a></li>
            <li>Here is <a href="/lti">what we know about you and your context</a> from the tool consumer.</li>
            <li>Here is <a href="/info/me.json">your organization, role and user information</a>.</li>
            <li>Published videos can be found in the <a href="/engage/ui/">Media Module</a>.</li>
            <li>For course integration, use the <a href="series/">Series LTI Tool</a>. Note that you can use the <code>?series=[series-id]</code> URL parameter to show just a single series.</li>
        </ul>
    </>;
}
