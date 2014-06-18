/**
 * 
 */
package org.sagebionetworks.repo.manager.team;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.MembershipRqstSubmissionDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dbo.dao.AuthorizationUtils;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author brucehoff
 *
 */
public class MembershipRequestManagerImpl implements MembershipRequestManager {
	
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired 
	private MembershipRqstSubmissionDAO membershipRqstSubmissionDAO;
	@Autowired
	private NotificationManager notificationManager;
	@Autowired
	private UserProfileDAO userProfileDAO;
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	@Autowired
	private TeamDAO teamDAO;
	
	
	public MembershipRequestManagerImpl() {}
	
	// for testing
	public MembershipRequestManagerImpl(
			AuthorizationManager authorizationManager,
			MembershipRqstSubmissionDAO membershipRqstSubmissionDAO,
			NotificationManager notificationManager,
			PrincipalAliasDAO principalAliasDAO,
			TeamDAO teamDAO
			) {
		this.authorizationManager=authorizationManager;
		this.membershipRqstSubmissionDAO=membershipRqstSubmissionDAO;
		this.notificationManager = notificationManager;
		this.principalAliasDAO = principalAliasDAO;
		this.teamDAO=teamDAO;
	}
	
	private static final String TEAM_MEMBERSHIP_REQUEST_CREATED_TEMPLATE = "message/teamMembershipRequestCreatedTemplate.txt";
	private static final String TREAM_MEMBERSHIP_REQUEST_MESSAGE_SUBJECT = "someone has requested to join your team";

	public static void validateForCreate(MembershipRqstSubmission mrs, UserInfo userInfo) {
		if (mrs.getCreatedBy()!=null) throw new InvalidModelException("'createdBy' field is not user specifiable.");
		if (mrs.getCreatedOn()!=null) throw new InvalidModelException("'createdOn' field is not user specifiable.");
		if (mrs.getId()!=null) throw new InvalidModelException("'id' field is not user specifiable.");
		if (!userInfo.isAdmin() && mrs.getUserId()!=null && !mrs.getUserId().equals(userInfo.getId().toString())) 
			throw new InvalidModelException("May not specify a user id other than yourself.");
		if (mrs.getTeamId()==null) throw new InvalidModelException("'teamId' field is required.");
	}

	public static void populateCreationFields(UserInfo userInfo, MembershipRqstSubmission mrs, Date now) {
		mrs.setCreatedBy(userInfo.getId().toString());
		mrs.setCreatedOn(now);
		if (mrs.getUserId()==null) mrs.setUserId(userInfo.getId().toString());
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipRequestManager#create(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.MembershipRqstSubmission)
	 */
	@Override
	public MembershipRqstSubmission create(UserInfo userInfo,
			MembershipRqstSubmission mrs) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException {
		if (AuthorizationUtils.isUserAnonymous(userInfo)) 
			throw new UnauthorizedException("anonymous user cannot create membership request.");
		validateForCreate(mrs, userInfo);
		Date now = new Date();
		populateCreationFields(userInfo, mrs, now);
		MembershipRqstSubmission created = membershipRqstSubmissionDAO.create(mrs);
		Set<String> teamAdmins = new HashSet<String>(teamDAO.getAdminTeamMembers(mrs.getTeamId()));
		sendMembershipRequestMessage(userInfo, teamAdmins, mrs.getTeamId());
		return created;
	}

	private void sendMembershipRequestMessage(UserInfo requester, Set<String> adminPrincipalIds, String teamId) throws NotFoundException {
		String requesterUserName = principalAliasDAO.getUserName(requester.getId());
		UserProfile userProfile = userProfileDAO.get(requester.getId().toString());
		String displayName = EmailUtils.getDisplayName(requesterUserName, userProfile);
		Map<String,String> fieldValues = new HashMap<String,String>();
		fieldValues.put("#userName#", displayName);
		fieldValues.put("#teamName#", teamDAO.get(teamId).getName());
		fieldValues.put("#teamId#", teamId);
		String messageContent = EmailUtils.readMailTemplate(TEAM_MEMBERSHIP_REQUEST_CREATED_TEMPLATE, fieldValues);
		
		notificationManager.sendNotification(
				requester, 
				adminPrincipalIds, 
				TREAM_MEMBERSHIP_REQUEST_MESSAGE_SUBJECT, 
				messageContent, 
				NotificationManager.TEXT_PLAIN_MIME_TYPE);
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipRequestManager#get(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public MembershipRqstSubmission get(UserInfo userInfo, String id)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		MembershipRqstSubmission mrs = membershipRqstSubmissionDAO.get(id);
		if (!userInfo.isAdmin() && !userInfo.getId().toString().equals(mrs.getUserId()))
			throw new UnauthorizedException("Cannot retrieve membership request for another user.");
		return mrs;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipRequestManager#delete(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public void delete(UserInfo userInfo, String id) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		MembershipRqstSubmission mrs = null;
		try {
			mrs = membershipRqstSubmissionDAO.get(id);
		} catch (NotFoundException e) {
			return;
		}
		if (!userInfo.isAdmin() && !userInfo.getId().toString().equals(mrs.getUserId()))
			throw new UnauthorizedException("Cannot delete membership request for another user.");
		membershipRqstSubmissionDAO.delete(id);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipRequestManager#getOpenByTeamInRange(java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<MembershipRequest> getOpenByTeamInRange(UserInfo userInfo, 
			String teamId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)) throw new UnauthorizedException("Cannot retrieve membership requests.");
		Date now = new Date();
		long teamIdAsLong = Long.parseLong(teamId);
		List<MembershipRequest> mrList = membershipRqstSubmissionDAO.getOpenByTeamInRange(teamIdAsLong, now.getTime(), limit, offset);
		long count = membershipRqstSubmissionDAO.getOpenByTeamCount(teamIdAsLong, now.getTime());
		PaginatedResults<MembershipRequest> results = new PaginatedResults<MembershipRequest>();
		results.setResults(mrList);
		results.setTotalNumberOfResults(count);
		return results;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.MembershipRequestManager#getOpenByTeamAndRequestorInRange(java.lang.String, java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<MembershipRequest> getOpenByTeamAndRequesterInRange(UserInfo userInfo, 
			String teamId, String requestorId, long limit, long offset)
			throws DatastoreException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)) throw new UnauthorizedException("Cannot retrieve membership requests.");
		Date now = new Date();
		long teamIdAsLong = Long.parseLong(teamId);
		long requestorIdAsLong = Long.parseLong(requestorId);
		List<MembershipRequest> mrList = membershipRqstSubmissionDAO.getOpenByTeamAndRequesterInRange(teamIdAsLong, requestorIdAsLong, now.getTime(), limit, offset);
		long count = membershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(teamIdAsLong, requestorIdAsLong, now.getTime());
		PaginatedResults<MembershipRequest> results = new PaginatedResults<MembershipRequest>();
		results.setResults(mrList);
		results.setTotalNumberOfResults(count);
		return results;
	}


	@Override
	public PaginatedResults<MembershipRqstSubmission> getOpenSubmissionsByRequesterInRange(
			UserInfo userInfo, String requesterId, long limit, long offset) throws DatastoreException, NotFoundException {
		if (!userInfo.getId().toString().equals(requesterId)) throw new UnauthorizedException("Cannot retrieve another's membership requests.");
		Date now = new Date();
		long requesterIdAsLong = Long.parseLong(requesterId);
		List<MembershipRqstSubmission> mrList = membershipRqstSubmissionDAO.getOpenSubmissionsByRequesterInRange(requesterIdAsLong, now.getTime(), limit, offset);
		long count = membershipRqstSubmissionDAO.getOpenByRequesterCount(requesterIdAsLong, now.getTime());
		PaginatedResults<MembershipRqstSubmission> results = new PaginatedResults<MembershipRqstSubmission>();
		results.setResults(mrList);
		results.setTotalNumberOfResults(count);
		return results;
	}

	@Override
	public PaginatedResults<MembershipRqstSubmission> getOpenSubmissionsByTeamAndRequesterInRange(
			UserInfo userInfo, String teamId, String requesterId, long limit,
			long offset) throws DatastoreException, NotFoundException {
		if (!userInfo.getId().toString().equals(requesterId)) throw new UnauthorizedException("Cannot retrieve another's membership requests.");
		Date now = new Date();
		long teamIdAsLong = Long.parseLong(teamId);
		long requestorIdAsLong = Long.parseLong(requesterId);
		List<MembershipRqstSubmission> mrList = membershipRqstSubmissionDAO.getOpenSubmissionsByTeamAndRequesterInRange(teamIdAsLong, requestorIdAsLong, now.getTime(), limit, offset);
		long count = membershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(teamIdAsLong, requestorIdAsLong, now.getTime());
		PaginatedResults<MembershipRqstSubmission> results = new PaginatedResults<MembershipRqstSubmission>();
		results.setResults(mrList);
		results.setTotalNumberOfResults(count);
		return results;
	}

}
