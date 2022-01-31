import React from "react";
import {connect} from "react-redux";
import {getUserInformation} from "../selectors/userInfoSelectors";
import {hasAccess} from "../utils/utils";

/**
 * Component that renders the footer
 */
const version = {
    version: '8.03',
    buildNumber: '42'
};
const feedbackUrl = 'https://opencast.org/';

const Footer = ({ user }) => (
        <footer id="main-footer" >
            <div className="default-footer">
                {/* Only render if a version is set */}
                {!!version.version && (
                    <div className="meta">
                        Opencast {version.version}
                        {hasAccess("ROLE_ADMIN", user) && (
                            <span> - {version.buildNumber || 'undefined'}</span>
                        )}
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

// Getting state data out of redux store
const mapStateToProps = state => ({
    user: getUserInformation(state)
});


export default connect(mapStateToProps)(Footer);

