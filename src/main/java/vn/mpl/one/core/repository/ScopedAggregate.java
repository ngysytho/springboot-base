package vn.mpl.one.core.repository;

import org.springframework.data.jpa.domain.Specification;

/**
 * Implement per domain using Criteria API and a closed enum of permitted metrics. Never accept raw
 * property names or SQL.
 */
public interface ScopedAggregate<T, M extends Enum<M>, R> {
  R aggregate(Specification<T> mandatoryScope, M metric);
}
