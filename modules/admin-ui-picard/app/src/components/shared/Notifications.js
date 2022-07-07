import React from "react";
import {getNotifications, getGlobalPositions} from "../../selectors/notificationSelector";
import {connect} from "react-redux";
import {useTranslation} from "react-i18next";
import cn from 'classnames';
import {setHidden} from "../../actions/notificationActions";
import {NOTIFICATION_CONTEXT, NOTIFICATION_CONTEXT_ACCESS} from "../../configs/modalConfig";

/**
 * This component renders notifications about occurred errors, warnings and info
 */
const Notifications = ({ setNotificationHidden, notifications, globalPosition, context }) => {
    const { t } = useTranslation();

    const closeNotification = id => {
        setNotificationHidden(id, true);
    }

    const renderNotification = (notification, key) => (
      <li key={key}>
        <div className={cn(notification.type, 'alert sticky')}>
          <a onClick={() => closeNotification(notification.id)}
             className="fa fa-times close"/>
          <p>
            {t(notification.message)}
          </p>
        </div>
      </li>
    );

    return (
        // if context is not_corner then render notification without consider global notification position
        context === 'not_corner' ? (
            <ul>{notifications.map((notification, key) => (
                !notification.hidden && (notification.context === NOTIFICATION_CONTEXT ||
                    notification.context === NOTIFICATION_CONTEXT_ACCESS) && (
                    renderNotification(notification, key)
                )
            ))}
            </ul>
        ) : context === 'above_table' ? (
          <ul>
            {notifications.map((notification, key) => (
              !notification.hidden && (notification.context === 'global'
                && notification.type === 'error') && (
                renderNotification(notification, key)
              )
            ))}
          </ul>
        ) : (
            <ul className={cn({'global-notifications' : true,
                'notifications-top-left': globalPosition === 'top-left',
                'notifications-top-right': globalPosition === 'top-right',
                'notification-top-center': globalPosition === 'top-center',
                'notifications-bottom-left': globalPosition === 'bottom-left',
                'notifications-bottom-right': globalPosition === 'bottom-right',
                'notifications-bottom-center': globalPosition === 'bottom-center'})}>
                {notifications.map((notification, key) => (
                    (!notification.hidden && notification.context === 'global') && (
                        renderNotification(notification, key)
                    )
                ))

                }
            </ul>
        )

    )
}

// Getting state data out of redux store
const mapStateToProps = state => ({
    notifications: getNotifications(state),
    globalPosition: getGlobalPositions(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    setNotificationHidden: (id, isHidden) => dispatch(setHidden(id, isHidden))
});

export default connect(mapStateToProps, mapDispatchToProps)(Notifications);
