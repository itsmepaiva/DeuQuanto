package portifolio.deuquanto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import portifolio.deuquanto.entity.Group;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
}
