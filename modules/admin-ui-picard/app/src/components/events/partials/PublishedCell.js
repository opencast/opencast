import React, {useEffect, useState} from 'react';
import styled from 'styled-components';
import {useTranslation} from "react-i18next";

import playIconOn from '../../../img/play-icon-on.png';
import playIcon from '../../../img/play-icon.png';

// Style for list items in popup listing publications of an event
const PublicationLink = styled.a`
    background-image: url(${props => (props.enabled ? playIconOn : playIcon)});
    background-position-y: center;
    margin: 6px 0;
    padding-left: 30px;
    line-height: 22px;
    background-repeat: no-repeat;
    display: block;
`;

// References for detecting a click outside of the container of the popup listing publications of an event
const containerPublications = React.createRef();

/**
 * This component renders the published cells of events in the table view
 */
const PublishCell = ({ row }) => {
    const { t } = useTranslation();

    // State of popup listing publications of an event
    const [showPopup, setShowPopup] = useState(false);

    useEffect(() => {

        // Function for handling clicks outside of popup
        const handleClickOutside = e => {
            if (containerPublications.current && !containerPublications.current.contains(e.target)) {
                setShowPopup(false);
            }
        }

        // Event listener for handle a click outside of popup
        window.addEventListener('mousedown', handleClickOutside);

        return () => {
            window.removeEventListener('mousedown', handleClickOutside);
        }


    }, [])

    return (
        <div className="popover-wrapper" ref={containerPublications}>
            {row.publications.length && (
                <>
                    <a className="popover-wrapper__trigger" >
                        <span onClick={() => setShowPopup(!showPopup)}>{t('YES')}</span>
                    </a>
                    {showPopup && (
                        <div className="js-popover popover">
                            <div className="popover__header"/>
                            <div className="popover__content">
                                {/* Show a list item for each publication of an event that isn't hidden*/}
                                {row.publications.map((publication, key) => (
                                    !publication.hiding ? (
                                        // Check if publications is enabled and choose icon according
                                        publication.enabled ? (
                                            <PublicationLink href={publication.url}
                                                             target="_blank"
                                                             enabled>
                                                {t(publication.name)}
                                            </PublicationLink>
                                        ) : (
                                            <PublicationLink>
                                                {t(publication.name)}
                                            </PublicationLink>
                                        )
                                    ) : null
                                ))
                                }
                            </div>
                        </div>
                    )}

                </>

            )}

        </div>
    )
}


export default PublishCell;
