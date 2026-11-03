package vn.mpl.one.core.filter;

import java.util.Locale;
import vn.mpl.one.core.exception.InvalidFilterException;

public enum FilterOperator {
  EQ, NEQ, GT, GTE, LT, LTE, LIKE, IN, IS_NULL, IS_NOT_NULL;

  public boolean unary() { return this == IS_NULL || this == IS_NOT_NULL; }

  public static FilterOperator parse(String value) {
    return switch (value.toLowerCase(Locale.ROOT)) {
      case "isnull", "is_null" -> IS_NULL;
      case "notnull", "isnotnull", "is_not_null" -> IS_NOT_NULL;
      default -> {
        try { yield valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new InvalidFilterException("Unknown filter operator"); }
      }
    };
  }
}
