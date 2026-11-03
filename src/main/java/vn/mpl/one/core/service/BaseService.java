package vn.mpl.one.core.service;

import java.util.*;
import java.util.function.Consumer;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import vn.mpl.one.core.entity.BaseEntity;
import vn.mpl.one.core.tenant.TenantOwned;
import vn.mpl.one.core.exception.*;
import vn.mpl.one.core.repository.BaseRepository;

/** Persistence adapters only. Public adapter entry points own the transaction boundary. */
@Transactional(readOnly = true)
public abstract class BaseService<T extends BaseEntity> {
  private final BaseRepository<T, UUID> repository;

  protected BaseService(BaseRepository<T, UUID> repository) { this.repository = repository; }

  protected final Optional<T> findById(UUID id, Specification<T> mandatoryScope) {
    return findOneByCriteria(scopedId(id, mandatoryScope));
  }
  protected final List<T> findByCriteria(Specification<T> scope, Sort sort) {
    return repository.findAll(required(scope), sort);
  }
  protected final Optional<T> findOneByCriteria(Specification<T> scope) {
    return repository.findOne(required(scope));
  }
  protected final T findOneOrFailByCriteria(Specification<T> scope) {
    return findOneByCriteria(scope).orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
  }
  protected final Page<T> paginate(Specification<T> scope, Pageable pageable) {
    return repository.findAll(required(scope), pageable);
  }
  protected final boolean exists(Specification<T> scope) { return repository.exists(required(scope)); }
  protected final long count(Specification<T> scope) { return repository.count(required(scope)); }

  /** Saves managed entities only; detached updates must first be loaded through the authorized scope. */
  @Transactional
  protected T save(T entity, Specification<T> scope, UUID organizationId) {
    required(scope);
    if (entity instanceof TenantOwned owned && !Objects.equals(owned.getOrganizationId(), Objects.requireNonNull(organizationId)))
      throw new ResourceNotFoundException("Resource not found");
    if (entity.getId() != null) {
      T managed = findById(entity.getId(), scope).orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
      if (managed != entity) throw new ConflictException("Load the managed entity through its scope before updating");
    }
    return repository.saveAndFlush(entity);
  }
  @Transactional
  protected List<T> saveAll(List<T> entities, Specification<T> scope, UUID organizationId) {
    // Deliberately preserve entity callbacks/version checks; caller transaction rolls back every write on failure.
    return entities.stream().map(entity -> save(entity, scope, organizationId)).toList();
  }
  @Transactional
  protected void delete(Specification<T> scope) {
    repository.delete(findOneOrFailByCriteria(scope));
    repository.flush();
  }
  @Transactional
  protected void deleteById(UUID id, Specification<T> scope) { delete(scopedId(id, scope)); }

  // Retain useful pre-refactor conveniences internally, never as public domain CRUD.
  @Transactional
  protected T create(T entity, Specification<T> scope, UUID organizationId) {
    if (entity.getId() != null) throw new ConflictException("Entity already exists");
    return save(entity, scope, organizationId);
  }
  @Transactional
  protected T update(Specification<T> scope, UUID organizationId, Consumer<T> change) {
    T entity = findOneOrFailByCriteria(scope);
    change.accept(entity);
    return save(entity, scope, organizationId);
  }
  @Transactional
  protected List<T> bulkCreate(List<T> entities, Specification<T> scope, UUID organizationId) {
    if (entities.stream().anyMatch(e -> e.getId() != null)) throw new ConflictException("Entity already exists");
    return saveAll(entities, scope, organizationId);
  }
  @Transactional
  protected List<T> bulkUpdate(List<Update<T>> updates, UUID organizationId) {
    return updates.stream().map(u -> update(u.scope(), organizationId, u.change())).toList();
  }
  protected record Update<T>(Specification<T> scope, Consumer<T> change) {}

  private Specification<T> scopedId(UUID id, Specification<T> scope) {
    Objects.requireNonNull(id);
    return required(scope).and((root, query, cb) -> cb.equal(root.get("id"), id));
  }
  private Specification<T> required(Specification<T> scope) { return Objects.requireNonNull(scope, "Explicit scope is required"); }
}
