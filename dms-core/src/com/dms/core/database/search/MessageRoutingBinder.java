package com.dms.core.database.search;

import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;

import com.dms.core.database.tables.Message;
import com.dms.core.structures.ViewStatus;

public class MessageRoutingBinder implements RoutingBinder {

	@Override
	public void bind(RoutingBindingContext context) {
		context.dependencies().use("updateType").use("viewStatus");
		context.bridge(Message.class, new Bridge());
	}

	private static class Bridge implements RoutingBridge<Message> {

		@Override
		public void route(DocumentRoutes routes, Object entityIdentifier, Message indexedEntity,
				RoutingBridgeRouteContext context) {
			if (indexedEntity.getUpdateType() == null && indexedEntity.getViewStatus() != ViewStatus.DELETED) {
				routes.addRoute();
			} else {
				routes.notIndexed();
			}
		}

		@Override
		public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, Message indexedEntity,
				RoutingBridgeRouteContext context) {
			routes.addRoute();
		}

	}

}
