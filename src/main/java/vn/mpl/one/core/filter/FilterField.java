package vn.mpl.one.core.filter;

import java.util.Set;

/** A module-owned property type and explicit operator policy. Never derive this from HTTP input. */
public record FilterField(Class<?> type, Set<FilterOperator> operators) {
  public FilterField { java.util.Objects.requireNonNull(type); operators = Set.copyOf(operators); }
}
