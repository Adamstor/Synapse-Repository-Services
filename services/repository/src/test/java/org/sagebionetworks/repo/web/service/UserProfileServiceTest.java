package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Most of the tests in this suite only worked when using illegal principal IDs, so @Ignore were added when it was refactored.
 *
 */

public class UserProfileServiceTest {
	
	private static final Long EXTRA_USER_ID = 2398475L;
	private static final Long NONEXISTENT_USER_ID = 827634L;
	private static UserProfile extraProfile;
	private static UserInfo userInfo;
	
	private UserProfileService userProfileService = new UserProfileServiceImpl();
	
	private EntityPermissionsManager mockPermissionsManager;
	private UserProfileManager mockUserProfileManager;
	private UserManager mockUserManager;
	private EntityManager mockEntityManager;
	private PrincipalAliasDAO mockPrincipalAlaisDAO;
	
	@Before
	public void before() throws Exception {
		mockPermissionsManager = mock(EntityPermissionsManager.class);
		mockUserProfileManager = mock(UserProfileManager.class);
		mockUserManager = mock(UserManager.class);
		mockEntityManager = mock(EntityManager.class);
		mockPrincipalAlaisDAO = mock(PrincipalAliasDAO.class);
		
		
		// Create UserGroups
		List<PrincipalAlias> groups = new LinkedList<PrincipalAlias>();
		for (int i = 0; i < 10; i++) {
			PrincipalAlias alias = new PrincipalAlias();
			alias.setPrincipalId(new Long(i));
			alias.setType(AliasType.TEAM_NAME);
			alias.setAlias("g"+i);
			groups.add(alias);
		}
		
		// Create UserProfiles
		List<UserProfile> list = new ArrayList<UserProfile>();
		for (int i = 0; i < 10; i++) {
			UserProfile p = new UserProfile();
			p.setOwnerId("" + i);
			list.add(p);
		}
		// extra profile with duplicated name
		UserProfile p = new UserProfile();
		p.setOwnerId("-100");
		list.add(p);
		QueryResults<UserProfile> profiles = new QueryResults<UserProfile>(list, list.size());
		
		extraProfile = new UserProfile();
		extraProfile.setOwnerId(EXTRA_USER_ID.toString());
		userInfo = new UserInfo(false, EXTRA_USER_ID);

		when(mockUserProfileManager.getInRange(any(UserInfo.class), anyLong(), anyLong())).thenReturn(profiles);
		when(mockUserProfileManager.getInRange(any(UserInfo.class), anyLong(), anyLong())).thenReturn(profiles);
		when(mockUserProfileManager.getUserProfile(any(UserInfo.class), eq(EXTRA_USER_ID.toString()))).thenReturn(extraProfile);
		when(mockUserProfileManager.getUserProfile(any(UserInfo.class), eq(NONEXISTENT_USER_ID.toString()))).thenThrow(new NotFoundException());
		when(mockUserManager.getUserInfo(EXTRA_USER_ID)).thenReturn(userInfo);
		when(mockPrincipalAlaisDAO.listPrincipalAliases(AliasType.TEAM_NAME)).thenReturn(groups);

		userProfileService.setPermissionsManager(mockPermissionsManager);
		userProfileService.setUserProfileManager(mockUserProfileManager);
		userProfileService.setUserManager(mockUserManager);
		userProfileService.setEntityManager(mockEntityManager);
		userProfileService.setPrincipalAlaisDAO(mockPrincipalAlaisDAO);
	}
	
	@Test
	public void testGetUserGroupHeadersById() throws DatastoreException, NotFoundException {
		List<Long> ids = new ArrayList<Long>();
		ids.add(0L);
		ids.add(1l);
		ids.add(2L);
		
		UserGroupHeaderResponsePage response = userProfileService.getUserGroupHeadersByIds(null, ids);
		Map<String, UserGroupHeader> headers = new HashMap<String, UserGroupHeader>();
		for (UserGroupHeader ugh : response.getChildren())
			headers.put(ugh.getOwnerId(), ugh);
		assertEquals(3, headers.size());
		assertTrue(headers.containsKey("0"));
		assertTrue(headers.containsKey("1"));
		assertTrue(headers.containsKey("2"));
	}
	
	@Test
	public void testGetUserGroupHeadersByIdNotInCache() throws DatastoreException, NotFoundException {
		List<Long> ids = new ArrayList<Long>();
		ids.add(0L);
		ids.add(1l);
		ids.add(2L);
		ids.add(EXTRA_USER_ID); // should require fetch from repo
		
		UserGroupHeaderResponsePage response = userProfileService.getUserGroupHeadersByIds(null, ids);
		Map<String, UserGroupHeader> headers = new HashMap<String, UserGroupHeader>();
		for (UserGroupHeader ugh : response.getChildren()) {
			headers.put(ugh.getOwnerId(), ugh);
		}
		assertEquals(4, headers.size());
		assertTrue(headers.containsKey("0"));
		assertTrue(headers.containsKey("1"));
		assertTrue(headers.containsKey("2"));
		assertTrue(headers.containsKey(EXTRA_USER_ID.toString()));
		
		verify(mockUserProfileManager).getUserProfile(any(UserInfo.class), eq(EXTRA_USER_ID.toString()));
	}
	
	public void testGetUserGroupHeadersByIdDoesNotExist() throws DatastoreException, NotFoundException {
		List<Long> ids = new ArrayList<Long>();
		ids.add(0L);
		ids.add(1l);
		ids.add(2L);
		ids.add(NONEXISTENT_USER_ID); // should not exist
		
		UserGroupHeaderResponsePage response = userProfileService.getUserGroupHeadersByIds(null, ids);
		Map<String, UserGroupHeader> headers = new HashMap<String, UserGroupHeader>();
		for (UserGroupHeader ugh : response.getChildren())
			headers.put(ugh.getOwnerId(), ugh);
		assertEquals(3, headers.size());
		assertTrue(headers.containsKey("g0"));
		assertTrue(headers.containsKey("g1"));
		assertTrue(headers.containsKey("g2"));
		assertFalse(headers.containsKey("g10"));
	}


	@Test
	public void testAddFavorite() throws Exception {
		String entityId = "syn123";
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, userInfo)).thenReturn(true);		
		Favorite fav = new Favorite();
		fav.setEntityId(entityId);
		fav.setPrincipalId(EXTRA_USER_ID.toString());
		when(mockUserProfileManager.addFavorite(any(UserInfo.class), anyString())).thenReturn(fav);

		userProfileService.addFavorite(EXTRA_USER_ID, entityId);
		
		verify(mockUserProfileManager).addFavorite(userInfo, entityId);
		verify(mockEntityManager).getEntityHeader(userInfo, entityId, null);
	}

	@Test(expected=UnauthorizedException.class)
	public void testAddFavoriteUnauthorized() throws Exception {
		String entityId = "syn123";
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, userInfo)).thenReturn(false);		
		Favorite fav = new Favorite();
		fav.setEntityId(entityId);
		fav.setPrincipalId(EXTRA_USER_ID.toString());
		when(mockUserProfileManager.addFavorite(any(UserInfo.class), anyString())).thenReturn(fav);

		userProfileService.addFavorite(EXTRA_USER_ID, entityId);		
		fail();
	}
	
	@Test
	public void testPrivateFieldCleaning() throws Exception {
		String profileId = "someOtherProfileid";
		String ownerId = "ownerId";
		String email = "test@example.com";
		UserProfile userProfile = new UserProfile();
		userProfile.setOwnerId(ownerId);
		when(mockUserManager.getUserInfo(EXTRA_USER_ID)).thenReturn(userInfo);
		when(mockUserProfileManager.getUserProfile(userInfo, profileId)).thenReturn(userProfile);
		
		UserProfile someOtherUserProfile = userProfileService.getUserProfileByOwnerId(EXTRA_USER_ID, profileId);
		assertNull(someOtherUserProfile.getEtag());
	}

	@Test
	public void testPrivateFieldCleaningAdmin() throws Exception {
		String profileId = "someOtherProfileid";
		String ownerId = "ownerId";
		String email = "test@example.com";
		UserProfile userProfile = new UserProfile();
		userProfile.setOwnerId(ownerId);

		userInfo = new UserInfo(true, EXTRA_USER_ID);
		when(mockUserManager.getUserInfo(EXTRA_USER_ID)).thenReturn(userInfo);
		when(mockUserProfileManager.getUserProfile(userInfo, profileId)).thenReturn(userProfile);
		
		UserProfile someOtherUserProfile = userProfileService.getUserProfileByOwnerId(EXTRA_USER_ID, profileId);
		assertNull(someOtherUserProfile.getEtag());
	}


}
