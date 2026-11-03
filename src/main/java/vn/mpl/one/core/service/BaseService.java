package vn.mpl.one.core.service;

import java.util.*;
import java.util.function.Consumer;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import vn.mpl.one.core.entity.BaseEntity;
import vn.mpl.one.core.exception.*;
import vn.mpl.one.core.repository.BaseRepository;

/**
 * Internal persistence primitive. Tenant modules must compose this behind an authorized application
 * service.
 */
@Transactional(readOnly = true)
public class BaseService<T extends BaseEntity> {
  private final BaseRepository<T, UUID> repository;

  public BaseService(BaseRepository<T, UUID> repository) {
    this.repository = repository;
  }

  public Optional<T> findById(UUID id) {
    return repository.findById(id);
  }

  public List<T> findByCriteria(Specification<T> criteria, Sort sort) {
    return repository.findAll(criteria, sort);
  }

  public Optional<T> findOneByCriteria(Specification<T> criteria) {
    return repository.findOne(criteria);
  }

  public T findOneOrFailByCriteria(Specification<T> criteria) {
    return findOneByCriteria(criteria)
        .orElseThrow(() -> new NotFoundException("Resource not found"));
  }

  public Page<T> paginate(Specification<T> criteria, Pageable page) {
    return repository.findAll(criteria, page);
  }

  public boolean exists(Specification<T> criteria) {
    return repository.exists(criteria);
  }

  public long count(Specification<T> criteria) {
    return repository.count(criteria);
  }

  @Transactional
  public T create(T entity) {
    if (entity.getId() != null) throw new ConflictException("Entity already exists");
    return repository.saveAndFlush(entity);
  }

  @Transactional
  public T update(Specification<T> scope, Consumer<T> change) {
    T entity = findOneOrFailByCriteria(scope);
    change.accept(entity);
    return repository.saveAndFlush(entity);
  }

  @Transactional
  public void delete(Specification<T> scope) {
    repository.delete(findOneOrFailByCriteria(scope));
    repository.flush();
  }

  @Transactional
  public List<T> bulkCreate(List<T> entities) {
    if (entities.stream().anyMatch(e -> e.getId() != null))
      throw new ConflictException("Entity already exists");
    return repository.saveAllAndFlush(entities);
  }

  @Transactional
  public List<T> bulkUpdate(List<Update<T>> updates) {
    return updates.stream().map(u -> update(u.scope(), u.change())).toList();
  }

  public record Update<T>(Specification<T> scope, Consumer<T> change) {}
}
