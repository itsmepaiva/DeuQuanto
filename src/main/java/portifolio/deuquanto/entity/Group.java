package portifolio.deuquanto.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_group")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    @CreationTimestamp
    private Instant createdAt;

    private Instant expiresAt;


    @Formula("(SELECT COUNT(*) FROM group_members gm WHERE gm.group_id = id)")
    private Integer totalMembers;

    @Column(unique = true)
    private String inviteToken;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupMember> groupMembers = new ArrayList<>();

    @PrePersist
    public void prePersist(){
        if(this.inviteToken == null){
            this.inviteToken = UUID.randomUUID().toString();
        }
    }

    @Transient
    public boolean isExpired(){
        if(this.expiresAt == null){
            return false;
        }
        return Instant.now().isAfter(this.expiresAt);
    }
}
