package org.sagebionetworks.evaluation.manager;

import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_PERMISSIONS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE_SUBMISSION;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.PARTICIPATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ_PRIVATE_SUBMISSION;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.SUBMIT;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE_SUBMISSION;

import java.util.List;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.repo.manager.AccessRequirementUtil;
import org.sagebionetworks.repo.manager.PermissionsManagerUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.AuthorizationUtils;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class EvaluationPermissionsManagerImpl implements EvaluationPermissionsManager {

	@Autowired
	private AccessControlListDAO aclDAO;
	@Autowired
	private EvaluationDAO evaluationDAO;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private AccessRequirementDAO  accessRequirementDAO;
	@Autowired
	private UserManager userManager;

	@Override
	public AccessControlList createAcl(UserInfo userInfo, AccessControlList acl)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException {

		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null.");
		}
		if (acl == null) {
			throw new IllegalArgumentException("ACL cannot be null.");
		}

		final String evalId = acl.getId();
		if (evalId == null || evalId.isEmpty()) {
			throw new IllegalArgumentException("ACL's evaluation ID must not be null or empty.");
		}

		final Evaluation eval = getEvaluation(evalId);
		if (!isEvalOwner(userInfo, eval)) {
			throw new UnauthorizedException("Only the owner of evaluation " + evalId + " can create ACL.");
		}

		final String evalOwerId = eval.getOwnerId();
		PermissionsManagerUtils.validateACLContent(acl, userInfo, Long.parseLong(evalOwerId));

		final String aclId = aclDAO.create(acl, ObjectType.EVALUATION);
		acl = aclDAO.get(aclId, ObjectType.EVALUATION);
		return acl;
	}

	@Override
	public AccessControlList updateAcl(UserInfo userInfo, AccessControlList acl)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException {

		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null.");
		}
		if (acl == null) {
			throw new IllegalArgumentException("ACL cannot be null.");
		}

		final String evalId = acl.getId();
		if (evalId == null || evalId.isEmpty()) {
			throw new IllegalArgumentException("ACL's evaluation ID must not be null or empty.");
		}

		final Evaluation eval = getEvaluation(evalId);
		if (!hasAccess(userInfo, evalId, CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("User " + userInfo.getId().toString()
					+ " not authorized to change permissions on evaluation " + evalId);
		}

		final Long evalOwnerId = KeyFactory.stringToKey(eval.getOwnerId());
		PermissionsManagerUtils.validateACLContent(acl, userInfo, evalOwnerId);

		aclDAO.update(acl, ObjectType.EVALUATION);
		return aclDAO.get(evalId, ObjectType.EVALUATION);
	}

	@Override
	public void deleteAcl(UserInfo userInfo, String evalId)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException, ConflictingUpdateException {
		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null.");
		}
		if (evalId == null || evalId.isEmpty()) {
			throw new IllegalArgumentException("Evaluation Id cannot be null or empty.");
		}
		if (!hasAccess(userInfo, evalId, CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("User " + userInfo.getId().toString()
					+ " not authorized to change permissions on evaluation " + evalId);
		}
		aclDAO.delete(evalId, ObjectType.EVALUATION);
	}

	@Override
	public AccessControlList getAcl(UserInfo userInfo, String evalId)
			throws NotFoundException, DatastoreException {
		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null.");
		}
		if (evalId == null || evalId.isEmpty()) {
			throw new IllegalArgumentException("Evaluation ID cannot be null or empty.");
		}

		AccessControlList acl = aclDAO.get(evalId, ObjectType.EVALUATION);
		return acl;
	}

	@Override
	public boolean hasAccess(UserInfo userInfo, String evalId, ACCESS_TYPE accessType)
			throws NotFoundException, DatastoreException {
		try {
			validateHasAccess(userInfo, evalId, accessType);
		} catch (UnauthorizedException e) {
			return false;
		}
		return true;
	}

	/**
	 * Whether the user has the access to the specified evaluation.
	 * Has the same logic as 'hasAccess' but throws informative exception if the answer is false.
	 */
	@Override
	public void validateHasAccess(UserInfo userInfo, String evalId, ACCESS_TYPE accessType)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null.");
		}
		if (evalId == null || evalId.isEmpty()) {
			throw new IllegalArgumentException("Evaluation ID cannot be null or empty.");
		}
		if (accessType == null) {
			throw new IllegalArgumentException("Access type cannot be null.");
		}
		if (userInfo.isAdmin()) return;

		if (isAnonymousWithNonReadAccess(userInfo, accessType))
			throw new UnauthorizedException("Anonymous user is not allowed to access Evaluation.");
		
		if (!aclDAO.canAccess(userInfo.getGroups(), evalId, ObjectType.EVALUATION, accessType))
			throw new UnauthorizedException("User lacks "+accessType+" access to Evaluation "+evalId);
		
		if (hasUnmetAccessRequirements(userInfo, evalId, accessType))
			throw new UnauthorizedException("User has unmet access restrictions for Evaluation "+evalId);
	}

	@Override
	public UserEvaluationPermissions getUserPermissionsForEvaluation(UserInfo userInfo, String evalId)
			throws NotFoundException, DatastoreException {

		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null.");
		}
		if (evalId == null || evalId.isEmpty()) {
			throw new IllegalArgumentException("Evaluation ID cannot be null or empty.");
		}

		UserEvaluationPermissions permission = new UserEvaluationPermissions();

		final Evaluation eval = getEvaluation(evalId);
		permission.setOwnerPrincipalId(KeyFactory.stringToKey(eval.getOwnerId()));

		// Public read
		UserInfo anonymousUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		permission.setCanPublicRead(hasAccess(anonymousUser, evalId, READ));

		// Other permissions
		permission.setCanView(hasAccess(userInfo, evalId, READ));
		permission.setCanEdit(hasAccess(userInfo, evalId, UPDATE));
		permission.setCanDelete(hasAccess(userInfo, evalId, DELETE));
		permission.setCanChangePermissions(hasAccess(userInfo, evalId, CHANGE_PERMISSIONS));
		permission.setCanParticipate(hasAccess(userInfo, evalId, PARTICIPATE));
		permission.setCanSubmit(hasAccess(userInfo, evalId, SUBMIT));
		permission.setCanViewPrivateSubmissionStatusAnnotations(hasAccess(userInfo, evalId, READ_PRIVATE_SUBMISSION));
		permission.setCanEditSubmissionStatuses(hasAccess(userInfo, evalId, UPDATE_SUBMISSION));
		permission.setCanDeleteSubmissions(hasAccess(userInfo, evalId, DELETE_SUBMISSION));

		return permission;
	}
	
	private static boolean isAnonymousWithNonReadAccess(UserInfo userInfo, ACCESS_TYPE accessType) {
		return AuthorizationUtils.isUserAnonymous(userInfo) && !READ.equals(accessType);
	}
	
	private boolean hasUnmetAccessRequirements(UserInfo userInfo, String evalId, ACCESS_TYPE accessType) throws NotFoundException {
		if ((PARTICIPATE.equals(accessType) || SUBMIT.equals(accessType))) {
			List<Long> unmetRequirements = AccessRequirementUtil.unmetAccessRequirementIdsForEvaluation(
					userInfo, evalId, accessRequirementDAO);
			if (!unmetRequirements.isEmpty()) return true;
		}
		return false;
		
	}

	private boolean isEvalOwner(final UserInfo userInfo, final Evaluation eval) {
		String userId = userInfo.getId().toString();
		String evalOwnerId = eval.getOwnerId();
		if (userId != null && evalOwnerId != null && userId.equals(evalOwnerId)) {
			return true;
		}
		return false;
	}

	private Evaluation getEvaluation(final String evalId) throws NotFoundException {
		try {
			return evaluationDAO.get(evalId);
		}
		catch (NotFoundException e) {
			// Rethrow with a more specific message
			throw new NotFoundException("Evaluation of ID " + evalId + " does not exist yet.");
		}
	}
}
