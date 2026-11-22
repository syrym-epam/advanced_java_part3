package advanced.java.part3.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table("sports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sport {
    @Id
    private Integer id;
    private String name;
}