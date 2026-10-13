package user.adapter.out.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "country")
public class CountryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", nullable = false, unique = true, length = 2)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    protected CountryJpaEntity() {
    }

    public CountryJpaEntity(Integer id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
