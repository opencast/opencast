import React from "react";

/**
 * Component that renders the footer
 */
const version = {
    version: '8.03',
    buildNumber: '42'
};
const feedbackUrl = 'https://opencast.org/';

const Footer = () => (
        <footer id="main-footer" >
            <div className="default-footer">
                {/* Only render if a version is set */}
                {!!version.version && (
                    <div className="meta">
                        Opencast {version.version}
                        {/*Todo: Only if user is admin*/}
                        <span> - {version.buildNumber || 'undefined'}</span>
                    </div>
                )}
                {/* Only render if a feedback URL is set*/}
                {!!feedbackUrl && (
                    <div className="feedback-btn" id="feedback-btn">
                        <a href={feedbackUrl}>Feedback</a>
                    </div>
                )}
            </div>
        </footer>
);

export default Footer;

