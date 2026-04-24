package portifolio.deuquanto.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import portifolio.deuquanto.entity.enums.GroupRole;

import java.time.Instant;

@Entity
@Table(name = "group_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupMember {

    @EmbeddedId
    private GroupMemberId id = new GroupMemberId();

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private Users user;

    @ManyToOne
    @MapsId("groupId")
    @JoinColumn(name = "group_id")
    private Group group;

    @Enumerated(EnumType.STRING)
    private GroupRole role;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant joinedAt;
}
