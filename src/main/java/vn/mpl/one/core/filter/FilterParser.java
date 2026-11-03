package vn.mpl.one.core.filter;

import java.util.*;
import vn.mpl.one.core.exception.BadRequestException;

public final class FilterParser {
  private FilterParser() {}

  public static FilterCriteria parse(List<String> filters, Set<String> fields) {
    List<FilterRule> rules = new ArrayList<>();
    for (String group : filters == null ? List.<String>of() : filters) {
      for (String text : group.split("&&", -1)) {
        String[] p = text.split(":", 3);
        if (p.length < 2 || !fields.contains(p[0]))
          throw new BadRequestException("Unknown filter field or invalid format");
        FilterOperator op;
        try {
          op = FilterOperator.valueOf(p[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
          throw new BadRequestException("Unknown filter operator");
        }
        boolean unary = op == FilterOperator.ISNULL || op == FilterOperator.NOTNULL;
        if (unary ? p.length != 2 : p.length != 3 || p[2].isBlank())
          throw new BadRequestException("Invalid filter value");
        rules.add(new FilterRule(p[0], op, unary ? null : p[2]));
      }
    }
    if (rules.size() > 20) throw new BadRequestException("At most 20 filters are allowed");
    return new FilterCriteria(rules);
  }
}
