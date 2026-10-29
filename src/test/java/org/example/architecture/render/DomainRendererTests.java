package org.example.architecture.render;

import java.util.List;

import org.example.architecture.design.ArchitectureModel.Attribute;
import org.example.architecture.design.ArchitectureModel.Entity;
import org.example.architecture.design.ArchitectureModel.EntityRelation;
import org.example.architecture.design.Cardinality;
import org.example.architecture.design.Samples;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainRendererTests {

	@Test
	void rendersTheSampleDomainModel() {
		assertThat(DomainRenderer.render(Samples.model())).isEqualTo("""
				erDiagram
				    Order {
				        UUID id
				        OrderStatus status
				    }
				    Refund {
				        UUID id
				        Money amount
				    }
				    Order ||--o{ Refund : "is refunded by"
				""");
	}

	@Test
	void sanitizesNamesAndMapsEveryCardinality() {
		ModelBuilder model = new ModelBuilder();
		model.entities = List.of(new Entity("E-1", "Order line", List.of(new Attribute("unit price", null))),
				new Entity("E-2", "Order line!", List.of()), new Entity("E-3", "Order line?", List.of()),
				new Entity("E-4", "2nd", List.of()));
		model.entityRelations = List.of(new EntityRelation("E-1", "E-2", Cardinality.ONE_TO_ONE, "is \"like\""),
				new EntityRelation("E-1", "E-3", Cardinality.ONE_TO_MANY, null),
				new EntityRelation("E-2", "E-3", Cardinality.MANY_TO_ONE, "a\nb"),
				new EntityRelation("E-3", "E-4", Cardinality.MANY_TO_MANY, "x"),
				new EntityRelation("E-4", "E-1", null, "y"), new EntityRelation("E-1", "E-9", null, "ghost"),
				new EntityRelation("E-9", "E-1", null, "ghost"));

		assertThat(DomainRenderer.render(model.build())).isEqualTo("""
				erDiagram
				    Order_line {
				        string unit_price
				    }
				    Order_line_2
				    Order_line_3
				    Entity_2nd
				    Order_line ||--|| Order_line_2 : "is 'like'"
				    Order_line ||--o{ Order_line_3 : ""
				    Order_line_2 }o--|| Order_line_3 : "a b"
				    Order_line_3 }o--o{ Entity_2nd : "x"
				    Entity_2nd }o--o{ Order_line : "y"
				""");
	}

}
