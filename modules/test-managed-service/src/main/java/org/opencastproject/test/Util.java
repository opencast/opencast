package org.opencastproject.test;

import java.util.Dictionary;
import java.util.Enumeration;

public final class Util {
  private Util() {
  }


  public static <A, B> String mkString(Dictionary<A, B> d) {
    if (d != null) {
      final Enumeration<A> keys = d.keys();
      final StringBuffer s = new StringBuffer();
      while (keys.hasMoreElements()) {
        final A key = keys.nextElement();
        final B value = d.get(key);
        if (s.length() > 0) {
          s.append(",");
        }
        s.append(key).append("=").append(value);
      }
      return "[" + s.toString() + "]";
    } else {
      return "<null>";
    }
  }
}
