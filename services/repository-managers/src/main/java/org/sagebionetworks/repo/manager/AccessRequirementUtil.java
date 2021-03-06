package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public class AccessRequirementUtil {
	
	private static final List<Long> EMPTY_LIST = Arrays.asList(new Long[]{});
	
	private static final String FILE_TYPE_NAME = EntityType.getNodeTypeForClass(FileEntity.class).name();
	
	public static List<Long> unmetAccessRequirementIdsForEntity(
			UserInfo userInfo, 
			String entityId,
			List<String> entityAncestorIds,
			NodeDAO nodeDao, 
			AccessRequirementDAO accessRequirementDAO
			) throws NotFoundException {
		List<ACCESS_TYPE> accessTypes = Collections.singletonList(ACCESS_TYPE.DOWNLOAD);
		List<String> entityIds = new ArrayList<String>();
		entityIds.add(entityId);
		// if the user is the owner of the entity (and the entity is a File), then she automatically 
		// has access to the object and therefore has no unmet access requirements
		Long principalId = userInfo.getId();
		Node node = nodeDao.getNode(entityId);
		if (node.getCreatedByPrincipalId().equals(principalId) && node.getNodeType().equals(FILE_TYPE_NAME)) {
			return EMPTY_LIST;
		}
		// per PLFM-2477, we inherit the restrictions of the node's ancestors
		entityIds.addAll(entityAncestorIds);

		Set<Long> principalIds = new HashSet<Long>();
		for (Long ug : userInfo.getGroups()) {
			principalIds.add(ug);
		}
		
		return accessRequirementDAO.unmetAccessRequirements(entityIds, RestrictableObjectType.ENTITY, principalIds, accessTypes);
	}

	public static List<Long> unmetAccessRequirementIdsForEvaluation(
			UserInfo userInfo, 
			String evaluationId,
			AccessRequirementDAO accessRequirementDAO
			) throws NotFoundException {
		List<String> evaluationIds = Collections.singletonList(evaluationId);
		List<ACCESS_TYPE> accessTypes = new ArrayList<ACCESS_TYPE>();
		accessTypes.add(ACCESS_TYPE.DOWNLOAD);
		accessTypes.add(ACCESS_TYPE.PARTICIPATE);
		Set<Long> principalIds = new HashSet<Long>();
		for (Long ug : userInfo.getGroups()) {
			principalIds.add(ug);
		}
		return accessRequirementDAO.unmetAccessRequirements(evaluationIds, RestrictableObjectType.EVALUATION, principalIds, accessTypes);
	}
	
	public static List<Long> unmetAccessRequirementIdsForTeam(
			UserInfo userInfo, 
			String teamId,
			AccessRequirementDAO accessRequirementDAO
			) throws NotFoundException {
		List<String> teamIds = Collections.singletonList(teamId);
		List<ACCESS_TYPE> accessTypes = new ArrayList<ACCESS_TYPE>();
		accessTypes.add(ACCESS_TYPE.DOWNLOAD);
		accessTypes.add(ACCESS_TYPE.PARTICIPATE);
		Set<Long> principalIds = new HashSet<Long>();
		for (Long ug : userInfo.getGroups()) {
			principalIds.add(ug);
		}	
		return accessRequirementDAO.unmetAccessRequirements(teamIds, RestrictableObjectType.TEAM, principalIds, accessTypes);
	}
	
	public static List<String> getNodeAncestorIds(NodeDAO nodeDao, String nodeId, boolean includeNode) throws NotFoundException {
		List<String> nodeAncestorIds = new ArrayList<String>();
		for (EntityHeader ancestorHeader : nodeDao.getEntityPath(nodeId)) {
			// we omit 'subjectId' itself from the ancestor list
			if (includeNode || !ancestorHeader.getId().equals(nodeId)) 
				nodeAncestorIds.add(ancestorHeader.getId());
		}
		return nodeAncestorIds;
	}

}
