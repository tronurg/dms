package com.ogya.dms.core.database.search;

import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.database.tables.Dgroup;
import com.ogya.dms.core.database.tables.Message;

public class DmsMappingConfigurer implements HibernateOrmSearchMappingConfigurer {

	@Override
	public void configure(HibernateOrmMappingConfigurationContext context) {

		ProgrammaticMappingConfigurationContext mapping = context.programmaticMapping();

		TypeMappingStep messageMapping = mapping.type(Message.class);
		messageMapping.indexed().routingBinder(new MessageRoutingBinder());
		messageMapping.property("id").genericField().sortable(Sortable.YES);
		messageMapping.property("content").fullTextField().analyzer("messageContentAnalyzer");
		messageMapping.property("viewStatus").genericField();
		messageMapping.property("contact").indexedEmbedded().includePaths("id").indexingDependency()
				.reindexOnUpdate(ReindexOnUpdate.NO);
		messageMapping.property("dgroup").indexedEmbedded().includePaths("id").indexingDependency()
				.reindexOnUpdate(ReindexOnUpdate.NO);

		TypeMappingStep contactMapping = mapping.type(Contact.class);
		contactMapping.indexed();
		contactMapping.property("id").genericField();

		TypeMappingStep dgroupMapping = mapping.type(Dgroup.class);
		dgroupMapping.indexed();
		dgroupMapping.property("id").genericField();

	}

}
