package org.sagebionetworks.repo.model.dbo.v2.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ATTACHMENT_RESERVATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ATTACHMENT_RESERVATION_TIMESTAMP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_DDL_FILE_WIKI_ATTATCHMENT_RESERVATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_ATTACHMENT_RESERVATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_PAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ID;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.persistence.DBOProjectStat;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class V2DBOWikiAttachmentReservation implements MigratableDatabaseObject<V2DBOWikiAttachmentReservation, V2DBOWikiAttachmentReservation> {
//	private static final FieldColumn[] FIELDS = new FieldColumn[] {
//		new FieldColumn("wikiId", V2_COL_WIKI_ATTACHMENT_RESERVATION_ID, true).withIsBackupId(true),
//		new FieldColumn("fileHandleId", V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID, true),
//		new FieldColumn("timeStamp", V2_COL_WIKI_ATTACHMENT_RESERVATION_TIMESTAMP),
//	};

	@Field(name = V2_COL_WIKI_ATTACHMENT_RESERVATION_ID, primary = true, backupId = true, nullable = false)	// TODO: size 20?
	@ForeignKey(name = "V2_WIKI_ATTACH_RESERVE_FK", table = V2_TABLE_WIKI_PAGE, field = V2_COL_WIKI_ID, cascadeDelete = true)
	private Long wikiId;
	
	// TODO: Size 20?
	@Field(name = V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID, primary = true, nullable = false)	// TODO: UNIQUE KEY `V2_WIKI_UNIQUE_FILE_HANDLE_ID` (`WIKI_ID`,`FILE_HANDLE_ID`)
	@ForeignKey(name = "V2_WIKI_FILE_HAND_RESERVE_FK", table = TABLE_FILES, field = COL_FILES_ID, cascadeDelete = true)
	private Long fileHandleId;
	
	@Field(name = V2_COL_WIKI_ATTACHMENT_RESERVATION_TIMESTAMP, nullable = false)
	private Timestamp timeStamp;
	
	private static TableMapping<V2DBOWikiAttachmentReservation> tableMapping = AutoTableMapping.create(V2DBOWikiAttachmentReservation.class);
	
	@Override
	public TableMapping<V2DBOWikiAttachmentReservation> getTableMapping() {
		return tableMapping;
	}

	public Long getWikiId() {
		return wikiId;
	}

	public void setWikiId(Long wikiId) {
		this.wikiId = wikiId;
	}
	
	public Long getFileHandleId() {
		return fileHandleId;
	}

	public void setFileHandleId(Long fileHandleId) {
		this.fileHandleId = fileHandleId;
	}

	public Timestamp getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Timestamp timeStamp) {
		this.timeStamp = timeStamp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result
				+ ((timeStamp == null) ? 0 : timeStamp.hashCode());
		result = prime * result + ((wikiId == null) ? 0 : wikiId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		V2DBOWikiAttachmentReservation other = (V2DBOWikiAttachmentReservation) obj;
		if (fileHandleId == null) {
			if (other.fileHandleId != null)
				return false;
		} else if (!fileHandleId.equals(other.fileHandleId))
			return false;
		if (wikiId == null) {
			if (other.wikiId != null)
				return false;
		} else if (!wikiId.equals(other.wikiId))
			return false;
		if (timeStamp == null) {
			if (other.timeStamp != null)
				return false;
		} else if (!timeStamp.equals(other.timeStamp))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOWikiAttachmentReservation [wikiId=" + wikiId + ", fileHandleId="
				+ fileHandleId + ", timeStamp=" + timeStamp + "]";
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.V2_WIKI_ATTACHMENT_RESERVATION;
	}

	@Override
	public MigratableTableTranslation<V2DBOWikiAttachmentReservation, V2DBOWikiAttachmentReservation> getTranslator() {
		return new MigratableTableTranslation<V2DBOWikiAttachmentReservation, V2DBOWikiAttachmentReservation>(){

			@Override
			public V2DBOWikiAttachmentReservation createDatabaseObjectFromBackup(
					V2DBOWikiAttachmentReservation backup) {
				return backup;
			}

			@Override
			public V2DBOWikiAttachmentReservation createBackupFromDatabaseObject(
					V2DBOWikiAttachmentReservation dbo) {
				return dbo;
			}};
	}

	@Override
	public Class<? extends V2DBOWikiAttachmentReservation> getBackupClass() {
		return V2DBOWikiAttachmentReservation.class;
	}

	@Override
	public Class<? extends V2DBOWikiAttachmentReservation> getDatabaseObjectClass() {
		return V2DBOWikiAttachmentReservation.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}

}

//public class V2DBOWikiAttachmentReservation implements MigratableDatabaseObject<V2DBOWikiAttachmentReservation, V2DBOWikiAttachmentReservation> {
//	private static final FieldColumn[] FIELDS = new FieldColumn[] {
//		new FieldColumn("wikiId", V2_COL_WIKI_ATTACHMENT_RESERVATION_ID, true).withIsBackupId(true),
//		new FieldColumn("fileHandleId", V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID, true),
//		new FieldColumn("timeStamp", V2_COL_WIKI_ATTACHMENT_RESERVATION_TIMESTAMP),
//	};
//
//	private Long wikiId;
//	private Long fileHandleId;
//	private Timestamp timeStamp;
//	
//	@Override
//	public TableMapping<V2DBOWikiAttachmentReservation> getTableMapping() {
//		return new TableMapping<V2DBOWikiAttachmentReservation>(){
//
//			@Override
//			public V2DBOWikiAttachmentReservation mapRow(ResultSet rs, int rowNum)throws SQLException {
//				V2DBOWikiAttachmentReservation result = new V2DBOWikiAttachmentReservation();
//				result.setWikiId(rs.getLong(V2_COL_WIKI_ATTACHMENT_RESERVATION_ID));
//				result.setFileHandleId(rs.getLong(V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID));
//				result.setTimeStamp(rs.getTimestamp(V2_COL_WIKI_ATTACHMENT_RESERVATION_TIMESTAMP));
//				return result;
//			}
//
//			@Override
//			public String getTableName() {
//				return V2_TABLE_WIKI_ATTACHMENT_RESERVATION;
//			}
//
//			@Override
//			public String getDDLFileName() {
//				return V2_DDL_FILE_WIKI_ATTATCHMENT_RESERVATION;
//			}
//
//			@Override
//			public FieldColumn[] getFieldColumns() {
//				return FIELDS;
//			}
//
//			@Override
//			public Class<? extends V2DBOWikiAttachmentReservation> getDBOClass() {
//				return V2DBOWikiAttachmentReservation.class;
//			}
//			
//		};
//	}
//
//	public Long getWikiId() {
//		return wikiId;
//	}
//
//	public void setWikiId(Long wikiId) {
//		this.wikiId = wikiId;
//	}
//	
//	public Long getFileHandleId() {
//		return fileHandleId;
//	}
//
//	public void setFileHandleId(Long fileHandleId) {
//		this.fileHandleId = fileHandleId;
//	}
//
//	public Timestamp getTimeStamp() {
//		return timeStamp;
//	}
//
//	public void setTimeStamp(Timestamp timeStamp) {
//		this.timeStamp = timeStamp;
//	}
//
//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result
//				+ ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
//		result = prime * result
//				+ ((timeStamp == null) ? 0 : timeStamp.hashCode());
//		result = prime * result + ((wikiId == null) ? 0 : wikiId.hashCode());
//		return result;
//	}
//
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		V2DBOWikiAttachmentReservation other = (V2DBOWikiAttachmentReservation) obj;
//		if (fileHandleId == null) {
//			if (other.fileHandleId != null)
//				return false;
//		} else if (!fileHandleId.equals(other.fileHandleId))
//			return false;
//		if (wikiId == null) {
//			if (other.wikiId != null)
//				return false;
//		} else if (!wikiId.equals(other.wikiId))
//			return false;
//		if (timeStamp == null) {
//			if (other.timeStamp != null)
//				return false;
//		} else if (!timeStamp.equals(other.timeStamp))
//			return false;
//		return true;
//	}
//
//	@Override
//	public String toString() {
//		return "DBOWikiAttachmentReservation [wikiId=" + wikiId + ", fileHandleId="
//				+ fileHandleId + ", timeStamp=" + timeStamp + "]";
//	}
//
//	@Override
//	public MigrationType getMigratableTableType() {
//		return MigrationType.V2_WIKI_ATTACHMENT_RESERVATION;
//	}
//
//	@Override
//	public MigratableTableTranslation<V2DBOWikiAttachmentReservation, V2DBOWikiAttachmentReservation> getTranslator() {
//		return new MigratableTableTranslation<V2DBOWikiAttachmentReservation, V2DBOWikiAttachmentReservation>(){
//
//			@Override
//			public V2DBOWikiAttachmentReservation createDatabaseObjectFromBackup(
//					V2DBOWikiAttachmentReservation backup) {
//				return backup;
//			}
//
//			@Override
//			public V2DBOWikiAttachmentReservation createBackupFromDatabaseObject(
//					V2DBOWikiAttachmentReservation dbo) {
//				return dbo;
//			}};
//	}
//
//	@Override
//	public Class<? extends V2DBOWikiAttachmentReservation> getBackupClass() {
//		return V2DBOWikiAttachmentReservation.class;
//	}
//
//	@Override
//	public Class<? extends V2DBOWikiAttachmentReservation> getDatabaseObjectClass() {
//		return V2DBOWikiAttachmentReservation.class;
//	}
//
//	@Override
//	public List<MigratableDatabaseObject> getSecondaryTypes() {
//		return null;
//	}
//
//}
