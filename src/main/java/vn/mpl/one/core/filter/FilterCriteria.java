package vn.mpl.one.core.filter;

import java.util.List;

public record FilterCriteria(List<FilterRule> rules) {
  public FilterCriteria {
    rules = List.copyOf(rules);
  }
}
