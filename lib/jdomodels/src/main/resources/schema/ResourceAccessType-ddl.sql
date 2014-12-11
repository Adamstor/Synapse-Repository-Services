CREATE TABLE IF NOT EXISTS `JDORESOURCEACCESS_ACCESSTYPE` (
  `ID_OID` bigint(20) NOT NULL,
  `OWNER_ID` bigint(20) NOT NULL,
  `STRING_ELE` varchar(256) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
  PRIMARY KEY (`ID_OID`,`STRING_ELE`),
  KEY `JDORESOURCEACCESS_ACCESSTYPE_N49` (`ID_OID`),
  CONSTRAINT `RES_ACCESS_TYP_OWNER_FK` FOREIGN KEY (`OWNER_ID`) REFERENCES `ACL` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `JDORESOURCEACCESS_ACCESSTYPE_FK1` FOREIGN KEY (`ID_OID`) REFERENCES `JDORESOURCEACCESS` (`ID`) ON DELETE CASCADE
)
