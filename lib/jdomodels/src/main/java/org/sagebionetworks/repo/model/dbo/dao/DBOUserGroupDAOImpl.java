package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_PROFILE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.ids.UuidETagGenerator;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupInt;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBOUserGroupDAOImpl implements UserGroupDAO {

	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private IdGenerator idGenerator;	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	private List<UserGroupInt> bootstrapUsers;
	
	private static final String ID_PARAM_NAME = "id";
	private static final String NAME_PARAM_NAME = "name";
	private static final String IS_INDIVIDUAL_PARAM_NAME = "isIndividual";
	private static final String ETAG_PARAM_NAME = "etag";
	
	private static final String SELECT_BY_NAME_AND_IS_INDIVID_SQL = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_NAME+"=:"+NAME_PARAM_NAME+
			" AND "+SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL+"=:"+IS_INDIVIDUAL_PARAM_NAME;
	
	private static final String SELECT_BY_NAME_SQL = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_NAME+"=:"+NAME_PARAM_NAME;
	
	private static final String SELECT_MULTI_BY_NAME_SQL = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_NAME+" IN (:"+NAME_PARAM_NAME+")";

	private static final String SELECT_MULTI_BY_PRINCIPAL_IDS = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_ID+" IN (:"+ID_PARAM_NAME+")";
	
	private static final String SELECT_BY_IS_INDIVID_SQL = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL+"=:"+IS_INDIVIDUAL_PARAM_NAME;
	
	private static final String SELECT_BY_IS_INDIVID_SQL_PAGINATED = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL+"=:"+IS_INDIVIDUAL_PARAM_NAME+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_BY_IS_INDIVID_OMITTING_SQL = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL+"=:"+IS_INDIVIDUAL_PARAM_NAME+
			" AND "+SqlConstants.COL_USER_GROUP_NAME+" NOT IN (:"+NAME_PARAM_NAME+")";
	
	private static final String SELECT_BY_IS_INDIVID_OMITTING_SQL_PAGINATED = 
			SELECT_BY_IS_INDIVID_OMITTING_SQL+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_ALL = 
		"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP;
	
	private static final String UPDATE_ETAG_LIST = 
			"UPDATE "+SqlConstants.TABLE_USER_GROUP+
			" SET "+SqlConstants.COL_USER_GROUP_E_TAG+"=:"+ETAG_PARAM_NAME+
			" WHERE "+SqlConstants.COL_USER_GROUP_ID+"=:"+ID_PARAM_NAME;
	
	private static final String SQL_COUNT_UNMODIFIED_USER_GROUPS = 
			"SELECT COUNT(*) FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_ID+"=:"+ID_PARAM_NAME+
			" AND "+SqlConstants.COL_USER_GROUP_E_TAG+"=:"+ETAG_PARAM_NAME;

	// the pattern is: select x.id, y.etag from g x LEFT OUTER JOIN p y on x.id=y.owner order by x.id
	// the query is: select g.id, p.etag from user_group g LEFT OUTER JOIN user_profile p on g.id=p.owner_id order by g.id limit l offset o 
	private static final String SELECT_ALL_PAGINATED_WITH_ETAG = 
		"SELECT g."+COL_USER_GROUP_ID+", p."+COL_USER_PROFILE_ETAG+" FROM "+
		TABLE_USER_GROUP+" g LEFT OUTER JOIN "+TABLE_USER_PROFILE+
		" p ON g."+COL_USER_GROUP_ID+" = p."+COL_USER_PROFILE_ID+
		" ORDER BY g."+COL_USER_GROUP_ID+" LIMIT :"+LIMIT_PARAM_NAME+
		" OFFSET :"+OFFSET_PARAM_NAME;
	
	// the query above is an outer join. For non-individual groups there is no UserProfile and
	// hence no etag.  The group is immutable and so it's Etag should always be 0.  The following
	// is the default etag used in such a case.
	public static final String DEFAULT_ETAG = UuidETagGenerator.ZERO_E_TAG;

	private static final String SQL_COUNT_USER_GROUPS = "SELECT COUNT("+COL_USER_GROUP_ID+") FROM "+TABLE_USER_GROUP + " WHERE "+COL_USER_GROUP_ID+"=:"+ID_PARAM_NAME;

	private static final RowMapper<DBOUserGroup> userGroupRowMapper = (new DBOUserGroup()).getTableMapping();
	
	
	/**
	 * This is injected by Spring
	 * @param bootstrapUsers
	 */
	public void setBootstrapUsers(List<UserGroupInt> bootstrapUsers) {
		this.bootstrapUsers = bootstrapUsers;
	}

	@Override
	public List<UserGroupInt> getBootstrapUsers() {
		return bootstrapUsers;
	}

	@Override
	public UserGroup findGroup(String name, boolean isIndividual)
			throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(NAME_PARAM_NAME, name);
		param.addValue(IS_INDIVIDUAL_PARAM_NAME, isIndividual);		
		List<DBOUserGroup> ugs = simpleJdbcTemplate.query(SELECT_BY_NAME_AND_IS_INDIVID_SQL, userGroupRowMapper, param);
		if (ugs.size()>1) throw new DatastoreException("Expected 0-1 UserGroups but found "+ugs.size());
		if (ugs.size()==0) return null;
		UserGroup dto = new UserGroup();
		UserGroupUtils.copyDboToDto(ugs.iterator().next(), dto);
		return dto;
	}

	@Override
	public Map<String, UserGroup> getGroupsByNames(Collection<String> groupName)
			throws DatastoreException {
		Map<String, UserGroup> dtos = new HashMap<String, UserGroup>();
		if (groupName.isEmpty()) return dtos;
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(NAME_PARAM_NAME, groupName);	
		try {
			List<DBOUserGroup> dbos = simpleJdbcTemplate.query(SELECT_MULTI_BY_NAME_SQL, userGroupRowMapper, param);
			for (DBOUserGroup dbo : dbos) {
				UserGroup dto = new UserGroup();
				UserGroupUtils.copyDboToDto(dbo, dto);
				dtos.put(dto.getName(), dto);
			}
			return dtos;
		} catch (Exception e) {
			throw new DatastoreException("'getGroupsByNames' failed for group list: "+groupName, e);
		}
	}

	@Override
	public Collection<UserGroup> getAll(boolean isIndividual)
			throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(IS_INDIVIDUAL_PARAM_NAME, isIndividual);		
		List<DBOUserGroup> dbos = simpleJdbcTemplate.query(SELECT_BY_IS_INDIVID_SQL, userGroupRowMapper, param);
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		for (DBOUserGroup dbo : dbos) {
			UserGroup dto = new UserGroup();
			UserGroupUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}
	
	@Override
	public long getCount()  throws DatastoreException {
		return basicDao.getCount(DBOUserGroup.class);
	}

	@Override
	public Collection<UserGroup> getAllExcept(boolean isIndividual, Collection<String> groupNamesToOmit) throws DatastoreException {
		// the SQL will be invalid for an empty list, so we 'divert' that case:
		if (groupNamesToOmit.isEmpty()) return getAll(isIndividual);
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(IS_INDIVIDUAL_PARAM_NAME, isIndividual);		
		param.addValue(NAME_PARAM_NAME, groupNamesToOmit);
		List<DBOUserGroup> dbos = simpleJdbcTemplate.query(SELECT_BY_IS_INDIVID_OMITTING_SQL, userGroupRowMapper, param);
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		for (DBOUserGroup dbo : dbos) {
			UserGroup dto = new UserGroup();
			UserGroupUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}
	
	@Override
	public List<UserGroup> getInRange(long fromIncl, long toExcl,
			boolean isIndividual) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(IS_INDIVIDUAL_PARAM_NAME, isIndividual);		
		param.addValue(OFFSET_PARAM_NAME, fromIncl);
		long limit = toExcl - fromIncl;
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		List<DBOUserGroup> dbos = simpleJdbcTemplate.query(SELECT_BY_IS_INDIVID_SQL_PAGINATED, userGroupRowMapper, param);
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		for (DBOUserGroup dbo : dbos) {
			UserGroup dto = new UserGroup();
			UserGroupUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}

	@Override
	public List<UserGroup> getInRangeExcept(long fromIncl, long toExcl,
			boolean isIndividual, Collection<String> groupNamesToOmit) throws DatastoreException {
		// the SQL will be invalid for an empty list, so we 'divert' that case:
		if (groupNamesToOmit.isEmpty()) return getInRange(fromIncl, toExcl, isIndividual);
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(IS_INDIVIDUAL_PARAM_NAME, isIndividual);		
		param.addValue(OFFSET_PARAM_NAME, fromIncl);
		long limit = toExcl - fromIncl;
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		param.addValue(NAME_PARAM_NAME, groupNamesToOmit);
		List<DBOUserGroup> dbos = simpleJdbcTemplate.query(SELECT_BY_IS_INDIVID_OMITTING_SQL_PAGINATED, userGroupRowMapper, param);
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		for (DBOUserGroup dbo : dbos) {
			UserGroup dto = new UserGroup();
			UserGroupUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}

	public DBOUserGroup findGroup(String name) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(NAME_PARAM_NAME, name);	
		List<DBOUserGroup> ugs = simpleJdbcTemplate.query(SELECT_BY_NAME_SQL, userGroupRowMapper, param);
		if (ugs.size()>1) throw new DatastoreException("Expected 0-1 UserGroups but found "+ugs.size());
		if (ugs.size()==0) return null;
		return ugs.iterator().next();
	}
	
	@Override
	public boolean doesPrincipalExist(String name) {
		try {
			return null!=findGroup(name);
		} catch (DatastoreException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean deletePrincipal(String name) {
		try {
			DBOUserGroup ug = findGroup(name);
			if (ug==null) return false;
			delete(ug.getId().toString());
			return true;
		} catch (DatastoreException e) {
			throw new RuntimeException(e);
		} catch (NotFoundException e) {
			return false;
		}
	}

	@Override
	public String create(UserGroup dto) throws DatastoreException,
			InvalidModelException {
		DBOUserGroup dbo = new DBOUserGroup();
		UserGroupUtils.copyDtoToDbo(dto, dbo);
		
		// If the create is successful, it should have a new etag
		dbo.setEtag(UUID.randomUUID().toString());
		
		if(dbo.getId() == null){
			dbo.setId(idGenerator.generateNewId());
		}else{
			// If an id was provided then it must not exist
			if(doesIdExist(dbo.getId())) throw new IllegalArgumentException("The id: "+dbo.getId()+" already exists, so a UserGroup cannot be created using that id.");
			// Make sure the ID generator has reserved this ID.
			idGenerator.reserveId(dbo.getId(), TYPE.DOMAIN_IDS);
		}
		try {
			dbo = basicDao.createNew(dbo);
			return dbo.getId().toString();
		} catch (Exception e) {
			throw new DatastoreException("id="+dbo.getId()+" name="+dto.getName(), e);
		}
	}

	public boolean doesIdExist(Long id) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(ID_PARAM_NAME, id);
		try{
			long count = simpleJdbcTemplate.queryForLong(SQL_COUNT_USER_GROUPS, parameters);
			return count > 0;
		}catch(Exception e){
			// Can occur when the schema does not exist.
			return false;
		}
	}
	
	@Override
	public UserGroup get(String id) throws DatastoreException,
			NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, id);		
		DBOUserGroup dbo = basicDao.getObjectByPrimaryKey(DBOUserGroup.class, param);
		UserGroup dto = new UserGroup();
		UserGroupUtils.copyDboToDto(dbo, dto);
		return dto;
	}
	
	@Override
	public List<UserGroup> get(List<String> ids) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, ids);
		List<DBOUserGroup> dbos = simpleJdbcTemplate.query(SELECT_MULTI_BY_PRINCIPAL_IDS, userGroupRowMapper, param);
		List<UserGroup> dtos = new ArrayList<UserGroup>();

		for (DBOUserGroup dbo : dbos) {
			UserGroup dto = new UserGroup();
			UserGroupUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}

	@Override
	public Collection<UserGroup> getAll() throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		List<DBOUserGroup> dbos = simpleJdbcTemplate.query(SELECT_ALL, userGroupRowMapper, param);
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		for (DBOUserGroup dbo : dbos) {
			UserGroup dto = new UserGroup();
			UserGroupUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;

	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void update(UserGroup dto) throws DatastoreException,
			InvalidModelException, NotFoundException,
			ConflictingUpdateException {
		DBOUserGroup dbo = new DBOUserGroup();
		UserGroupUtils.copyDtoToDbo(dto, dbo);
		
		// If the update is successful, it should have a new etag
		dbo.setEtag(UUID.randomUUID().toString());

		basicDao.update(dbo);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, id);
		basicDao.deleteObjectByPrimaryKey(DBOUserGroup.class, param);
	}

	// initialization of UserGroups
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void bootstrapUsers() throws Exception {
		// Boot strap all users and groups
		if(this.bootstrapUsers == null) throw new IllegalArgumentException("bootstrapUsers cannot be null");
		// For each one determine if it exists, if not create it
		for(UserGroupInt ug: this.bootstrapUsers){
			if(ug.getId() == null) throw new IllegalArgumentException("Bootstrap users must have an id");
			if(ug.getName() == null) throw new IllegalArgumentException("Bootstrap users must have a name");
			Long id = Long.parseLong(ug.getId());
			if(!this.doesIdExist(id)){
				UserGroup newUg = new UserGroup();
				newUg.setId(ug.getId());
				newUg.setName(ug.getName());
				newUg.setIsIndividual(ug.getIsIndividual());
				this.create(newUg);
				// Make sure the ID generator has reserved this ID.
				idGenerator.reserveId(id, TYPE.DOMAIN_IDS);
			}
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void touchList(List<String> ids) {
		MapSqlParameterSource params[] = new MapSqlParameterSource[ids.size()];
		for (int i = 0; i < params.length; i++) {
			params[i] = new MapSqlParameterSource();
			params[i].addValue(ID_PARAM_NAME, ids.get(i));
			params[i].addValue(ETAG_PARAM_NAME, UUID.randomUUID().toString());
		}
		simpleJdbcTemplate.batchUpdate(UPDATE_ETAG_LIST, params);
	}

	@Override
	public boolean hasChanged(UserGroup dto) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, dto.getId());
		param.addValue(ETAG_PARAM_NAME, dto.getEtag());
		Long count = simpleJdbcTemplate.queryForLong(SQL_COUNT_UNMODIFIED_USER_GROUPS, param);
		
		// No results -> etag has changed
		return count == 0;
	}
}
