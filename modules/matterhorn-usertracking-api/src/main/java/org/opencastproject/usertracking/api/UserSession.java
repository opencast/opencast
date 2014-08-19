package org.opencastproject.usertracking.api;

public interface UserSession {

  String getUserId();

  void setUserId(String userId);

  String getUserIp();

  void setUserIp(String userIp);

  String getSessionId();

  void setSessionId(String sessionId);

  String getUserAgent();

  void setUserAgent(String userAgent);
}
