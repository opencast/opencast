// React imports
import React, {Component} from "react";
import {withTranslation} from "react-i18next";

/**
 * Component that renders the footer
 */
class Footer extends  Component {
    constructor(props) {
        super(props);
    }
    render() {
        // Get the version and feedback URL from the App-Component
        const {version, feedbackUrl} = this.props;
        return (
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
    }
}

export default withTranslation()(Footer)
