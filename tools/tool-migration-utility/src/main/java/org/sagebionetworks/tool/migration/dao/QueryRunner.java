package org.sagebionetworks.tool.migration.dao;

import java.util.List;

import org.json.JSONException;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;

/**
 * By making this an interface, we can have a real implementation that is well
 * tested with integration tests, an stub implementation to unit test all 
 * dependencies.
 * 
 * 
 * @author jmhill
 *
 */
public interface QueryRunner {
	
	/**
	 * Get the root entity.
	 * @param client
	 * @return
	 * @throws SynapseException
	 * @throws JSONException
	 */
	public EntityData getRootEntity(Synapse client) throws SynapseException, JSONException;
	
	/**
	 * Get all entity data from a given repository.
	 * @param client
	 * @return
	 * @throws SynapseException
	 * @throws IllegalAccessException
	 * @throws JSONException 
	 * @throws InterruptedException 
	 */
	public List<EntityData> getAllEntityData(Synapse client, BasicProgress progress) throws SynapseException, JSONException, InterruptedException;
	
	/**
	 * Get all entity data for entities of a given type.
	 * @param client
	 * @param type
	 * @param progress
	 * @return
	 * @throws SynapseException
	 * @throws JSONException
	 * @throws InterruptedException
	 */
	public List<EntityData> getAllEntityDataOfType(Synapse client, EntityType type, BasicProgress progress) throws SynapseException, JSONException, InterruptedException;
	
	/**
	 * Get all child entities of a given parent.
	 * @param client
	 * @param parentId
	 * @return
	 * @throws SynapseException
	 * @throws IllegalAccessException
	 * @throws JSONException 
	 * @throws InterruptedException 
	 */
	public List<EntityData> getAllAllChildrenOfEntity(Synapse client, String parentId) throws SynapseException, IllegalAccessException, JSONException, InterruptedException;
	
	/**
	 * Get the total entity count.
	 * @param client
	 * @return
	 * @throws SynapseException 
	 * @throws JSONException 
	 */
	public long getTotalEntityCount(Synapse client) throws SynapseException, JSONException;
	
	/**
	 * Get a count for an entity type
	 * @param type
	 * @param client
	 * @return
	 * @throws SynapseException
	 * @throws JSONException
	 */
	public long getCountForType(EntityType type, Synapse client) throws SynapseException, JSONException;

}
