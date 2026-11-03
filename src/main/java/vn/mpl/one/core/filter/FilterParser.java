package vn.mpl.one.core.filter;

import java.util.*;
import vn.mpl.one.core.exception.InvalidFilterException;

public final class FilterParser {
  private FilterParser() {}

  /** Parse syntax only; persistence must additionally enforce its field/operator policy. */
  public static FilterCriteria parse(List<String> filters) {
    List<FilterRule> rules = new ArrayList<>();
    for (String group : filters == null ? List.<String>of() : filters) {
      if (group == null) throw new InvalidFilterException("Invalid filter");
      for (String text : group.split("&&", -1)) {
        String[] parts = text.split(":", 3);
        if (parts.length < 2 || !parts[0].matches("[A-Za-z][A-Za-z0-9_]*"))
          throw new InvalidFilterException("Invalid filter field or format");
        FilterOperator operator = FilterOperator.parse(parts[1]);
        if (operator.unary() ? parts.length != 2 : parts.length != 3 || parts[2].isBlank())
          throw new InvalidFilterException("Invalid filter value");
        rules.add(new FilterRule(parts[0], operator, operator.unary() ? null : parts[2]));
        if (rules.size() > 20) throw new InvalidFilterException("At most 20 filters are allowed");
      }
    }
    return new FilterCriteria(rules);
  }

  public static FilterCriteria parse(List<String> filters, Map<String, FilterField> fields) {
    FilterCriteria criteria = parse(filters);
    validate(criteria, fields);
    return criteria;
  }

  public static void validate(FilterCriteria criteria, Map<String, FilterField> fields) {
    if (criteria.rules().size() > 20) throw new InvalidFilterException("At most 20 filters are allowed");
    for (FilterRule rule : criteria.rules()) {
      if (rule.field() == null || !rule.field().matches("[A-Za-z][A-Za-z0-9_]*"))
        throw new InvalidFilterException("Invalid filter field");
      FilterField field = fields.get(rule.field());
      if (field == null) throw new InvalidFilterException("Unknown filter field");
      if (rule.operator() == null || !field.operators().contains(rule.operator()))
        throw new InvalidFilterException("Operator is not allowed for this field");
      if (rule.operator().unary() ? rule.value() != null : rule.value() == null || rule.value().isBlank())
        throw new InvalidFilterException("Invalid filter value");
    }
  }
}
