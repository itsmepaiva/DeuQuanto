package portifolio.deuquanto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import portifolio.deuquanto.entity.Group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    Optional<Group> findByInviteToken(String inviteToken);

    @Query("SELECT g FROM Group g JOIN g.groupMembers gm WHERE gm.user.id = :userId")
    List<Group> findAllByUserId(UUID userId);
}
