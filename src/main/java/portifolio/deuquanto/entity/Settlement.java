package portifolio.deuquanto.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tb_settlement")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Group group;

    @ManyToOne
    @JoinColumn(name = "payer_id")
    private Users payer;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private Users receiver;

    @Column(nullable = false)
    private BigDecimal amount;

    @CreationTimestamp
    private Instant date;
}
