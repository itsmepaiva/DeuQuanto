package portifolio.deuquanto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import portifolio.deuquanto.entity.GroupMember;
import portifolio.deuquanto.entity.GroupMemberId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    @Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.group WHERE gm.user.id = :userId")
    List<GroupMember> findAllByUserIdWithGroups(@Param("userId")UUID userId);

    boolean existsByUserIdAndGroupId(UUID userId, Long groupId);

    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.user.id != :userId ORDER BY gm.joinedAt ASC")
    List<GroupMember> findPotentialSuccessors(Long groupId, UUID userId);

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, UUID userId);

    boolean existsByUserId(UUID userId);
}
