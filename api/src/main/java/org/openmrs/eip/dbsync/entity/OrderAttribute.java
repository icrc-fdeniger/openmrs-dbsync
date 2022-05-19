package org.openmrs.eip.dbsync.entity;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.openmrs.eip.dbsync.entity.light.OrderAttributeTypeLight;
import org.openmrs.eip.dbsync.entity.light.OrderLight;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "order_attribute")
@AttributeOverride(name = "id", column = @Column(name = "order_attribute_id"))
public class OrderAttribute extends Attribute<OrderAttributeTypeLight> {
	
	@NotNull
	@ManyToOne
	@JoinColumn(name = "order_id")
	private OrderLight referencedEntity;
}
