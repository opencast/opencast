import React from "react";

export function Welcome() {
    return <>
        <h1>Welcome to the LTI Module</h1>
        <ul>
            <li>More information about configuring LTI is available in the Administration Guide at <a href="https://docs.opencast.org">docs.opencast.org</a></li>
            <li>Here is <a href="/lti">what we know about you and your context</a> from the tool consumer.</li>
            <li>Here is <a href="/info/me.json">your organization, role and user information</a>.</li>
            <li>Published videos can be found in the <a href="/engage/ui/">Media Module</a>.</li>
            <li>For course integration, use the <a href="index.html?subtool=series">Series LTI Tool</a>. Note that you can specify these parameters:</li>
            <ul>
                <li>Use the <code>?series=[series-id]</code> URL parameter to show just a single series.</li>
                <li>Use the <code>?series_name=[series-name]</code> URL parameter to show just a single series. The series name has to be unique.</li>
                <li>Use the <code>?deletion=true</code> URL parameter to show a deletion button next to each episode.</li>
                <li>Use the <code>?edit=true</code> URL parameter to show an edit button next to each episode.</li>
            </ul>
            <li>For upload integration, use the <a href="index.html?subtool=upload">Upload LTI Tool</a>. Note that you can specify these parameters:</li>
            <ul>
                <li>Use the <code>?series=[series-id]</code> URL parameter to set the series ID the upload belongs to.</li>
                <li>Use the <code>?series_name=[series-name]</code> URL parameter to set the series name the upload belongs to. The series name has to be unique.</li>
                <li>Use the <code>?episode_id=[episode-id]</code> URL parameter to set an episode ID (do not use other than for testing).</li>
            </ul>
        </ul>
    </>;
}
